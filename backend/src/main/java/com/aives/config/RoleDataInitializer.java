package com.aives.config;

import com.aives.model.entity.Role;
import com.aives.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class RoleDataInitializer implements ApplicationRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(ApplicationArguments args) {
        initRoleIfMissing(Role.ADMIN_NAME, "Quản trị hệ thống");
        initRoleIfMissing(Role.LECTURER_NAME, "Giảng viên");
        initRoleIfMissing(Role.STUDENT_NAME, "Sinh viên");
    }

    private void initRoleIfMissing(String name, String description) {
        roleRepository.findByNameIgnoreCase(name).orElseGet(() -> {
            Role r = roleRepository.save(Role.builder().name(name).description(description).build());
            log.info("Initialized system role: {}", name);
            return r;
        });
    }
}

