package com.aives.service;

import com.aives.model.entity.SystemConfig;
import com.aives.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private static final Set<String> SPEECH_LANGUAGES = Set.of("vi-VN", "en-US");
    private static final Map<String, int[]> INT_RANGES = Map.of(
            SystemConfig.ANSWER_TIME, new int[]{15, 900},
            SystemConfig.MAX_FOLLOWUPS, new int[]{0, 5},
            SystemConfig.MAIN_QUESTIONS, new int[]{1, 20});

    @Transactional
    public void updateConfigs(Map<String, String> configs) {
        configs.forEach(this::validate);
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

    private void validate(String key, String value) {
        if (value == null) return;
        String v = value.trim();
        if (SystemConfig.SPEECH_LANGUAGE.equals(key)) {
            if (!SPEECH_LANGUAGES.contains(v)) {
                throw new IllegalArgumentException("Ngôn ngữ STT/TTS chỉ hỗ trợ vi-VN hoặc en-US.");
            }
            return;
        }
        int[] range = INT_RANGES.get(key);
        if (range == null) {
            throw new IllegalArgumentException("Không hỗ trợ khoá cấu hình: " + key);
        }
        int number;
        try {
            number = Integer.parseInt(v);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Giá trị của " + key + " phải là số nguyên.");
        }
        if (number < range[0] || number > range[1]) {
            throw new IllegalArgumentException("Giá trị của " + key + " phải từ " + range[0] + " đến " + range[1] + ".");
        }
    }
}
