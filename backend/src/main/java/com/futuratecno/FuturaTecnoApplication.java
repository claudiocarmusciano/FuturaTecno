package com.futuratecno;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync   // envío de emails fuera del hilo del request (ver EmailService#enviarHtmlAsync)
public class FuturaTecnoApplication {
    public static void main(String[] args) {
        SpringApplication.run(FuturaTecnoApplication.class, args);
    }
}
