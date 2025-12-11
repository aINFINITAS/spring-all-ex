package com.example.demo.auth;

import com.example.demo.comon.StacktraceUtil;
import com.example.demo.dto.Student;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DemoService {

    @Auth(
            permission = {"P1", "P2"},
            type = "#req.demoRequest.type",
            defaultType = "MANAGER"
    )
    public String check2(Demo2Request req) {

        List<Student> test = new ArrayList<>();
        for (int i = 0; i < 10000000; i++) {
            test.add(new Student("Name " + i, i));
        }
        Student a = test.get(100);

        log.info("Student info : {}", a.getName());
        log.info("BUSINESS RUNNING...");
        return "OK";
    }

    public String method3(Integer integer) {

        try {
            method4(integer);
        } catch (Exception e) {
            log.error("method3 exception: {}", StacktraceUtil.toSingleLine(e));
            log.info("method3 exception: {}", StacktraceUtil.toSingleLine(e));
        }


        return "method3";
    }


    private void method4(Integer i) {
        if (i > 100) {
            throw new IllegalArgumentException("illegal arg");
        }
    }
}
