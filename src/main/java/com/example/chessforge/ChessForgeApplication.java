package com.example.chessforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ChessForgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChessForgeApplication.class, args);
    }

}
