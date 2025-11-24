package com.example.scheduler.config;

import com.example.scheduler.job.SampleJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    // ---------------------------
    // 1️⃣ Job Detail (chung cho 4 trigger)
    // ---------------------------
    @Bean
    public JobDetail sampleJobDetail() {
        return JobBuilder.newJob(SampleJob.class)
                .withIdentity("sampleJob")
                .storeDurably() // cho phép tồn tại mà không cần trigger
                .build();
    }

    // ---------------------------
    // 2️⃣ SimpleTrigger — chạy mỗi 10 giây
    // ---------------------------
    @Bean
    public Trigger simpleTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(sampleJobDetail())
                .withIdentity("simpleTrigger")
                .startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(10)
                        .repeatForever())
                .build();
    }

    // ---------------------------
    // 3️⃣ CronTrigger — chạy mỗi 5 phút
    // ---------------------------
    @Bean
    public Trigger cronTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(sampleJobDetail())
                .withIdentity("cronTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0/5 * * * ?"))
                .build();
    }

    // ---------------------------
    // 4️⃣ CalendarIntervalTrigger — chạy mỗi ngày
    // ---------------------------
    @Bean
    public Trigger calendarTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(sampleJobDetail())
                .withIdentity("calendarTrigger")
                .startNow()
                .withSchedule(CalendarIntervalScheduleBuilder.calendarIntervalSchedule()
                        .withIntervalInDays(1))
                .build();
    }

    // ---------------------------
    // 5️⃣ DailyTimeIntervalTrigger — chạy từ 9h đến 17h, mỗi 1 giờ
    // ---------------------------
    @Bean
    public Trigger dailyTimeTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(sampleJobDetail())
                .withIdentity("dailyTrigger")
                .withSchedule(DailyTimeIntervalScheduleBuilder.dailyTimeIntervalSchedule()
                        .startingDailyAt(TimeOfDay.hourAndMinuteOfDay(9, 0))
                        .endingDailyAt(TimeOfDay.hourAndMinuteOfDay(17, 0))
                        .withIntervalInHours(1))
                .build();
    }

}

package com.example.scheduler.job;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SampleJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(SampleJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        logger.info("✅ Job '{}' được kích hoạt bởi Trigger '{}', thời gian: {}",
                context.getJobDetail().getKey().getName(),
                context.getTrigger().getKey().getName(),
                context.getFireTime());
    }

}


``````````````````````````````````````````````````

@PostMapping("/run-now")
public String runJobWithoutTrigger() throws SchedulerException {
    JobDetail jobDetail = JobBuilder.newJob(SampleJob.class)
            .withIdentity("tempJob", "manualGroup")
            .build();

    Trigger trigger = TriggerBuilder.newTrigger()
            .startNow()
            .build();

    scheduler.scheduleJob(jobDetail, trigger);
    return "🚀 Đã tạo và chạy job tạm thời!";
}

@PostMapping("/run-with-data")
public String runJobWithData(@RequestParam String message) throws SchedulerException {
    JobDataMap dataMap = new JobDataMap();
    dataMap.put("message", message);

    JobKey jobKey = new JobKey("sampleJob");
    scheduler.triggerJob(jobKey, dataMap);
    return "📨 Job chạy với dữ liệu: " + message;
}


package com.example.scheduler.config;

import com.example.scheduler.job.SampleJob;
import jakarta.annotation.PostConstruct;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Autowired
    private Scheduler scheduler;

    @PostConstruct
    public void runJobOnStartup() throws SchedulerException {
        System.out.println("🚀 Ứng dụng khởi động — chạy SampleJob ngay lập tức!");

        // Tạo JobDetail
        JobDetail jobDetail = JobBuilder.newJob(SampleJob.class)
                .withIdentity("startupJob", "manualGroup")
                .build();

        // Tạo Trigger chỉ chạy 1 lần
        Trigger trigger = TriggerBuilder.newTrigger()
                .startNow()
                .build();

        // Chạy job
        scheduler.scheduleJob(jobDetail, trigger);
    }
}




import com.example.scheduler.job.SampleJob;
import org.quartz.*;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzStartupRunner {

    @Bean
    public ApplicationRunner runSampleJobOnStartup(Scheduler scheduler) {
        return args -> {
            System.out.println("🚀 ApplicationRunner — chạy SampleJob sau khi Spring Boot load xong!");

            JobDetail jobDetail = JobBuilder.newJob(SampleJob.class)
                    .withIdentity("appRunnerJob", "manualGroup")
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .startNow()
                    .build();

            scheduler.scheduleJob(jobDetail, trigger);
        };
    }
}






