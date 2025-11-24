package com.example.scheduler.job;

import com.example.scheduler.listener.BatchJobListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BatchJobConfig {

    @Bean
    public Step userStep(StepBuilderFactory stepBuilderFactory,
            DBReader reader,
            DataProcessor processor,
            DBWriter writer) {
        return stepBuilderFactory.get("userStep")
                .<User, User>chunk(5)
                .reader(reader.reader(null))
                .processor(processor)
                .writer(writer.writer(null))
                .build();
    }

    @Bean
    public Job userJob(JobBuilderFactory jobBuilderFactory,
            Step userStep,
            BatchJobListener listener) {
        return jobBuilderFactory.get("userJob")
                .listener(listener) // 🔥 Gắn listener vào đây
                .start(userStep)
                .build();
    }
}

