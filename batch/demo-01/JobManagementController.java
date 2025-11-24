package com.example.scheduler.controller;

import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/jobs")
public class JobManagementController {

    @Autowired
    private Scheduler scheduler;

    // ✅ 1. Chạy job ngay lập tức
    @PostMapping("/start/{jobName}")
    public String startJob(@PathVariable String jobName) throws SchedulerException {
        JobKey jobKey = new JobKey(jobName);
        if (scheduler.checkExists(jobKey)) {
            scheduler.triggerJob(jobKey);
            return "🚀 Job '" + jobName + "' đã được kích hoạt!";
        } else {
            return "⚠️ Job '" + jobName + "' không tồn tại!";
        }
    }

    // ⏸️ 2. Tạm dừng job
    @PostMapping("/pause/{jobName}")
    public String pauseJob(@PathVariable String jobName) throws SchedulerException {
        JobKey jobKey = new JobKey(jobName);
        scheduler.pauseJob(jobKey);
        return "⏸️ Job '" + jobName + "' đã bị tạm dừng.";
    }

    // 🔁 3. Resume job
    @PostMapping("/resume/{jobName}")
    public String resumeJob(@PathVariable String jobName) throws SchedulerException {
        JobKey jobKey = new JobKey(jobName);
        scheduler.resumeJob(jobKey);
        return "🔁 Job '" + jobName + "' đã được kích hoạt lại.";
    }

    // 🗑️ 4. Xóa job
    @DeleteMapping("/{jobName}")
    public String deleteJob(@PathVariable String jobName) throws SchedulerException {
        JobKey jobKey = new JobKey(jobName);
        boolean deleted = scheduler.deleteJob(jobKey);
        return deleted ? "🗑️ Job '" + jobName + "' đã bị xóa."
                : "⚠️ Không tìm thấy job '" + jobName + "'.";
    }

    // 👀 5. Xem danh sách job đang có
    @GetMapping
    public List<String> listJobs() throws SchedulerException {
        List<String> jobs = new ArrayList<>();
        for (String group : scheduler.getJobGroupNames()) {
            for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(group))) {
                jobs.add(jobKey.getName() + " (" + group + ")");
            }
        }
        return jobs;
    }
}
