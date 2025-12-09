package com.example.demo.batch1;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StudentJobListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("🔥 Student Job STARTED at {}", jobExecution.getStartTime());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("✅ Student Job COMPLETED at {}", jobExecution.getEndTime());
        } else {
            log.warn("❌ Student Job FAILED with status: {}", jobExecution.getStatus());
        }
    }
}
