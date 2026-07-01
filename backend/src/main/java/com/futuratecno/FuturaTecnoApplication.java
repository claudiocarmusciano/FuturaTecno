package com.futuratecno;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FuturaTecnoApplication {
    public static void main(String[] args) {
        SpringApplication.run(FuturaTecnoApplication.class, args);
    }
}
