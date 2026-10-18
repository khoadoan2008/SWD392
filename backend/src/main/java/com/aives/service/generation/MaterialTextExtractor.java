package com.aives.service.generation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.io.Writer;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

/** No permanent upload directory, no user-controlled filesystem paths. */
public final class MaterialTextExtractor {
    public static final int MAX_FILE_BYTES = 10 * 1024 * 1024;
    static {
        ZipSecureFile.setMaxEntrySize(20L * 1024 * 1024);
        ZipSecureFile.setMaxTextSize(GenerationRequest.MAX_TEXT_CHARS);
    }

    public String extract(String filename, InputStream input) throws GenerationException {
        if (filename == null || input == null) throw new GenerationException("Hãy chọn tệp học liệu.");
        String lower = filename.toLowerCase(Locale.ROOT);
        if (!(lower.endsWith(".pdf") || lower.endsWith(".docx") || lower.endsWith(".txt"))) {
            throw new GenerationException("Chỉ hỗ trợ .pdf, .docx và .txt (UTF-8).");
        }
        try {
            byte[] bytes = readBounded(input, MAX_FILE_BYTES);
            if (bytes.length == 0) throw new GenerationException("Tệp học liệu rỗng.");
            String text;
            if (lower.endsWith(".pdf")) {
                if (bytes.length < 5 || bytes[0] != '%' || bytes[1] != 'P' || bytes[2] != 'D'
                        || bytes[3] != 'F' || bytes[4] != '-') {
                    throw new GenerationException("Nội dung tệp không phải PDF hợp lệ.");
                }
                try (PDDocument pdf = Loader.loadPDF(bytes)) {
                    if (pdf.isEncrypted() || !pdf.getCurrentAccessPermission().canExtractContent()) {
                        throw new GenerationException("Không đọc PDF được mã hóa hoặc cấm trích xuất văn bản.");
                    }
                    if (pdf.getNumberOfPages() > 100) throw new GenerationException("PDF tối đa 100 trang/lần.");
                    LimitedTextWriter writer = new LimitedTextWriter();
                    new PDFTextStripper().writeText(pdf, writer);
                    text = writer.toString();
                }
            } else if (lower.endsWith(".docx")) {
                if (bytes.length < 4 || bytes[0] != 'P' || bytes[1] != 'K') {
                    throw new GenerationException("Nội dung tệp không phải DOCX hợp lệ.");
                }
                validateDocxArchive(bytes);
                try (XWPFDocument doc = new XWPFDocument(new java.io.ByteArrayInputStream(bytes));
                     XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
                    text = extractor.getText();
                }
            } else {
                text = StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(bytes)).toString();
                for (int i = 0; i < text.length(); i++) {
                    char c = text.charAt(i);
                    if (Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t') {
                        throw new GenerationException("TXT chứa dữ liệu nhị phân, không phải văn bản UTF-8.");
                    }
                }
            }
            text = text.replace("\uFEFF", "").replace("\r\n", "\n").trim();
            if (text.length() < 40) {
                throw new GenerationException("Tệp không đủ văn bản. PDF scan/ảnh cần OCR trước khi tải lên.");
            }
            if (text.length() > GenerationRequest.MAX_TEXT_CHARS) {
                throw new GenerationException("Văn bản vượt 100.000 ký tự. Hãy chia nhỏ học liệu.");
            }
            return text;
        } catch (CharacterCodingException e) {
            throw new GenerationException("TXT phải được lưu bằng UTF-8.", e);
        } catch (IOException | RuntimeException e) {
            throw new GenerationException("Không đọc được tệp. Kiểm tra định dạng, mật khẩu hoặc tệp bị hỏng.", e);
        }
    }

    private static void validateDocxArchive(byte[] bytes) throws IOException, GenerationException {
        long expanded = 0;
        int entries = 0;
        boolean document = false;
        try (ZipInputStream zip = new ZipInputStream(new java.io.ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zip.getNextEntry()) != null) {
                if (++entries > 500) throw new GenerationException("DOCX có quá nhiều thành phần.");
                if ("word/document.xml".equals(entry.getName())) document = true;
                int n;
                while ((n = zip.read(buffer)) != -1) {
                    expanded += n;
                    if (expanded > 20L * 1024 * 1024) throw new GenerationException("DOCX giải nén vượt giới hạn 20 MB.");
                }
            }
        }
        if (!document) throw new GenerationException("Tệp không chứa tài liệu Word DOCX hợp lệ.");
    }

    private static final class LimitedTextWriter extends Writer {
        private final StringBuilder text = new StringBuilder();
        @Override public void write(char[] chars, int offset, int length) throws IOException {
            if (text.length() + length > GenerationRequest.MAX_TEXT_CHARS) throw new IOException("PDF text limit exceeded.");
            text.append(chars, offset, length);
        }
        @Override public void flush() { }
        @Override public void close() { }
        @Override public String toString() { return text.toString(); }
    }

    static byte[] readBounded(InputStream input, int limit) throws IOException, GenerationException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0, n;
        while ((n = input.read(buffer)) != -1) {
            total += n;
            if (total > limit) throw new GenerationException("Dữ liệu vượt dung lượng cho phép.");
            output.write(buffer, 0, n);
        }
        return output.toByteArray();
    }
}
