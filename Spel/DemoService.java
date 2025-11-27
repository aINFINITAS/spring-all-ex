package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DemoService {

    @Auth(
            permission = {"P1", "P2"},
            type = "#req.demoRequest.type",
            defaultType = "MANAGER"
    )
    public String check2(Demo2Request req) {

        log.info("BUSINESS RUNNING...");
        return "OK";
    }
}
