package com.kyx.service.hr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HRApplication {

    public static void main(String[] args) {

        SpringApplication.run(HRApplication.class, args);

    }

}
