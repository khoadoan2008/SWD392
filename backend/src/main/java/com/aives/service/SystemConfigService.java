package com.aives.service;

import com.aives.model.entity.SystemConfig;
import com.aives.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigRepository configRepository;

    @Transactional(readOnly = true)
    public Map<String, String> getAllConfigs() {
        Map<String, String> map = new HashMap<>();
        List<SystemConfig> configs = configRepository.findAll();
        for (SystemConfig c : configs) {
            map.put(c.getConfigKey(), c.getConfigValue());
        }
        // Defaults if not in database
        map.putIfAbsent(SystemConfig.SPEECH_LANGUAGE, "vi-VN");
        map.putIfAbsent(SystemConfig.ANSWER_TIME, "120");
        map.putIfAbsent(SystemConfig.MAX_FOLLOWUPS, "2");
        map.putIfAbsent(SystemConfig.MAIN_QUESTIONS, "3");
        return map;
    }

    @Transactional(readOnly = true)
    public String getConfig(String key, String defaultValue) {
        return configRepository.findById(key)
                .map(SystemConfig::getConfigValue)
                .orElse(defaultValue);
    }

    @Transactional(readOnly = true)
    public int getIntConfig(String key, int defaultValue) {
        try {
            return Integer.parseInt(getConfig(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Transactional
    public void updateConfigs(Map<String, String> configs) {
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            if (val != null) {
                SystemConfig cfg = configRepository.findById(key)
                        .orElse(SystemConfig.builder().configKey(key).build());
                cfg.setConfigValue(val.trim());
                configRepository.save(cfg);
            }
        }
    }
}
