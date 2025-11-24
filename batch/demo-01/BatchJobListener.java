package com.example.scheduler.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Component
public class BatchJobListener implements JobExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(BatchJobListener.class);

    private long startTime;

    // Trước khi job bắt đầu
    @Override
    public void beforeJob(JobExecution jobExecution) {
        startTime = System.currentTimeMillis();
        logger.info("🚀 [Before Job] Bắt đầu chạy job: {}", jobExecution.getJobInstance().getJobName());
        logger.info("📅 Start time: {}", jobExecution.getStartTime());
    }

    // Sau khi job hoàn thành
    @Override
    public void afterJob(JobExecution jobExecution) {
        long endTime = System.currentTimeMillis();
        long duration = (endTime - startTime) / 1000;

        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            logger.info("✅ [After Job] Job '{}' hoàn thành thành công!", jobExecution.getJobInstance().getJobName());
            logger.info("📦 Tổng thời gian chạy: {} giây", duration);
            logger.info("📊 Đã đọc: {} | Ghi: {} | Bỏ qua: {}",
                    jobExecution.getStepExecutions().stream().mapToLong(s -> s.getReadCount()).sum(),
                    jobExecution.getStepExecutions().stream().mapToLong(s -> s.getWriteCount()).sum(),
                    jobExecution.getStepExecutions().stream().mapToLong(s -> s.getSkipCount()).sum()
            );
        } else if (jobExecution.getStatus() == BatchStatus.FAILED) {
            logger.error("❌ [After Job] Job '{}' thất bại!", jobExecution.getJobInstance().getJobName());
            jobExecution.getAllFailureExceptions()
                    .forEach(e -> logger.error("Lỗi: {}", e.getMessage()));
        }

        logger.info("🏁 End time: {}", jobExecution.getEndTime());
    }
}
