package com.aives.service;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Kích hoạt AI chấm điểm khi buổi thi chuyển sang COMPLETED.
 * Chạy nền SAU khi transaction của sinh viên commit, để việc gọi Gemini (có thể mất tới 90 giây)
 * không làm sinh viên phải chờ và lỗi AI không làm hỏng bài nộp.
 */
@Component
@Slf4j
public class AiGradingTrigger {

    private final AiGradingService aiGradingService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ai-grading");
        t.setDaemon(true);
        return t;
    });

    public AiGradingTrigger(AiGradingService aiGradingService) {
        this.aiGradingService = aiGradingService;
    }

    public void gradeAfterCommit(Integer sessionId) {
        Runnable job = () -> {
            try {
                aiGradingService.evaluateSessionById(sessionId);
            } catch (Exception e) {
                log.warn("AI grading job failed for session {}: {}", sessionId, e.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    executor.submit(job);
                }
            });
        } else {
            executor.submit(job);
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
