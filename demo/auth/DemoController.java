package com.example.demo.auth;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/demo")
@RequiredArgsConstructor
public class DemoController {

    private final DemoService demoService;
    private static final Logger log = LogManager.getLogger(DemoController.class);

    @PostMapping("/check")
    public String check(@RequestBody Demo2Request request) {
        return demoService.check2(request);
    }

    @PostMapping("/check3")
    public String check3(@RequestParam Integer id) {
        log.info("Test log http");
        return demoService.method3(id);
    }
}
