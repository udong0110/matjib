package com.example.matjib;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.matjib.mapper")
public class MatjibApplication {
    public static void main(String[] args) {
        SpringApplication.run(MatjibApplication.class, args);
    }
}
