package com.soldout.reservas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MsReservasApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsReservasApplication.class, args);
    }
}
