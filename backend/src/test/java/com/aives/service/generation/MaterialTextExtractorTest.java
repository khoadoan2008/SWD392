package com.aives.service.generation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MaterialTextExtractorTest {
    private final MaterialTextExtractor extractor = new MaterialTextExtractor();
    @Test public void extractsUtf8TextWithBomAndVietnamese() throws Exception {
        String text="\uFEFFGiao thức TCP bảo đảm truyền dữ liệu theo đúng thứ tự, có cơ chế xác nhận và truyền lại.";
        assertEquals(text.substring(1), extractor.extract("course.TXT", stream(text)));
    }
    @Test public void extractsRealPdfText() throws Exception {
        try (PDDocument pdf=new PDDocument(); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            PDPage page=new PDPage(); pdf.addPage(page);
            try (PDPageContentStream content=new PDPageContentStream(pdf,page)) {
                content.beginText(); content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA),12);
                content.newLineAtOffset(20,700); content.showText(GenerationFixtures.MATERIAL); content.endText();
            }
            pdf.save(out);
            assertTrue(extractor.extract("course.pdf",new ByteArrayInputStream(out.toByteArray())).contains("retransmission"));
        }
    }
    @Test public void extractsRealDocxText() throws Exception {
        try (XWPFDocument doc=new XWPFDocument(); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            doc.createParagraph().createRun().setText(GenerationFixtures.MATERIAL); doc.write(out);
            assertEquals(GenerationFixtures.MATERIAL,extractor.extract("course.docx",new ByteArrayInputStream(out.toByteArray())));
        }
    }
    @Test public void rejectsImageOnlyPdf() throws Exception {
        try (PDDocument pdf=new PDDocument(); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            pdf.addPage(new PDPage()); pdf.save(out);
            assertThrows(GenerationException.class,()->extractor.extract("scan.pdf",new ByteArrayInputStream(out.toByteArray())));
        }
    }
    @Test public void rejectsUnsupportedFilename() {
        assertThrows(GenerationException.class,()->extractor.extract("course.exe",stream(GenerationFixtures.MATERIAL)));
    }
    @Test public void rejectsFakePdfAndFakeDocx() {
        assertThrows(GenerationException.class,()->extractor.extract("fake.pdf",stream(GenerationFixtures.MATERIAL)));
        assertThrows(GenerationException.class,()->extractor.extract("fake.docx",stream(GenerationFixtures.MATERIAL)));
    }
    @Test public void rejectsEmptyText() {
        assertThrows(GenerationException.class,()->extractor.extract("empty.txt",stream("")));
    }
    @Test public void rejectsInvalidUtf8AndBinaryText() {
        assertThrows(GenerationException.class,()->extractor.extract("bad.txt",new ByteArrayInputStream(new byte[]{(byte)0xff,(byte)0xfe})));
        assertThrows(GenerationException.class,()->extractor.extract("binary.txt",stream(GenerationFixtures.MATERIAL+"\u0000")));
    }
    @Test public void rejectsOversizedStreamWithoutTrustingPartSize() {
        InputStream huge=new InputStream(){ int remaining=MaterialTextExtractor.MAX_FILE_BYTES+1;
            @Override public int read(){ return remaining-- > 0 ? 'a' : -1; }
            @Override public int read(byte[] b,int off,int len) {
                if(remaining<=0)return -1; int n=Math.min(len,remaining); java.util.Arrays.fill(b,off,off+n,(byte)'a'); remaining-=n; return n;
            }};
        assertThrows(GenerationException.class,()->extractor.extract("huge.txt",huge));
    }
    @Test public void rejectsTooMuchExtractedText() {
        String text=String.join("", java.util.Collections.nCopies(GenerationRequest.MAX_TEXT_CHARS+1,"x"));
        assertThrows(GenerationException.class,()->extractor.extract("long.txt",stream(text)));
    }
    private static InputStream stream(String text) { return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)); }
}
