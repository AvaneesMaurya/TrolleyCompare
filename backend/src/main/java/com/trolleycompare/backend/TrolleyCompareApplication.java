package com.trolleycompare.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TrolleyCompareApplication {
    public static void main(String[] args) {
        SpringApplication.run(TrolleyCompareApplication.class, args);
    }
}