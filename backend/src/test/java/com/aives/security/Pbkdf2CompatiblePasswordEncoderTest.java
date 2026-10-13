package com.aives.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class Pbkdf2CompatiblePasswordEncoderTest {

    private final Pbkdf2CompatiblePasswordEncoder encoder = new Pbkdf2CompatiblePasswordEncoder();

    @Test
    void testVerifyAdminPasswordFromDemoData() {
        String adminHash = "65536:kfTNw/rdoXXxzRGQ0Y0Z2g==:gbVuZTCHO759D4Ro6V/opitW/x27fUOAIQ2MGhrAa24=";
        assertTrue(encoder.matches("admin123", adminHash), "Mật khẩu admin123 phải khớp với hash trong demo data");
    }

    @Test
    void testVerifyUserPasswordFromDemoData() {
        String userHash = "65536:svZ+c3lZ4+XK6sWTdHNtdQ==:0svGRpt9mLJNAthHviv1HgZn30rF5ANNYsens3GXX4E=";
        assertTrue(encoder.matches("123456", userHash), "Mật khẩu 123456 phải khớp với hash của lecturer1 / student1");
    }

    @Test
    void testEncodeAndMatchNewPassword() {
        String raw = "NewPassword@2026";
        String encoded = encoder.encode(raw);
        assertTrue(encoder.matches(raw, encoded), "Mật khẩu mới băm phải khớp");
    }
}
