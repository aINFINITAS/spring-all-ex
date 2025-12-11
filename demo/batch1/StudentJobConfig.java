package com.example.demo.batch1;

import com.example.demo.dto.Student;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class StudentJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final StudentProcessor processor;
    private final StudentJobListener studentJobListener;
    private final StudentItemWriter writer;

    @Bean
    public ItemReader<Student> studentReader() {
        List<Student> list = IntStream.rangeClosed(1, 10_000)
                .mapToObj(i -> new Student(
                        "Student" + i,
                        new BigDecimal(1000 + i),
                        i
                ))
                .collect(Collectors.toList());

        return new ListItemReader<>(list);
    }

    @Bean
    public Step studentStep() {
        return new StepBuilder("studentStep", jobRepository)
                .<Student, Student>chunk(1000, transactionManager)
                .reader(studentReader())
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    public Job studentJob() {
        return new JobBuilder("studentJob", jobRepository)
                .start(studentStep())
                .listener(studentJobListener)
                .build();
    }
}
