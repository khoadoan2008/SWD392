package com.aives.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AIVES - AI Interactive Viva Examination System API")
                        .description("Tài liệu RESTful API hệ thống thi vấn đáp tự động tương tác AI (SWD392)")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("SWD392 Team")
                                .email("contact@aives.edu.vn"))
                        .license(new License().name("Apache 2.0").url("https://springdoc.org")))
                // Hỗ trợ nút Authorize Bearer JWT ngay trên Swagger UI
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Nhập Bearer Token (JWT) được cấp sau khi login để test các API yêu cầu xác thực")));
    }
}
