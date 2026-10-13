package com.aives.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

@Component
public class Pbkdf2CompatiblePasswordEncoder implements PasswordEncoder {

    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;
    private static final SecureRandom RANDOM = new SecureRandom();
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @Override
    public String encode(CharSequence rawPassword) {
        // Mặc định băm bằng PBKDF2 theo format chuẩn của cơ sở dữ liệu AIVES
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(rawPassword.toString().toCharArray(), salt, ITERATIONS);
        Base64.Encoder enc = Base64.getEncoder();
        return ITERATIONS + ":" + enc.encodeToString(salt) + ":" + enc.encodeToString(hash);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        // Hỗ trợ kiểm tra nếu dữ liệu là BCrypt
        if (encodedPassword.startsWith("$2a$") || encodedPassword.startsWith("$2b$")) {
            return bcrypt.matches(rawPassword, encodedPassword);
        }

        // Kiểm tra định dạng PBKDF2: iterations:salt:hash
        String[] parts = encodedPassword.split(":");
        if (parts.length != 3) {
            return false;
        }
        try {
            Base64.Decoder dec = Base64.getDecoder();
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = dec.decode(parts[1]);
            byte[] expected = dec.decode(parts[2]);
            byte[] actual = pbkdf2(rawPassword.toString().toCharArray(), salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_LENGTH);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Lỗi khởi tạo PBKDF2 algorithm", e);
        }
    }
}
