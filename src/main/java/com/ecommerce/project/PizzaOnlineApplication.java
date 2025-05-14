package com.ecommerce.project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class PizzaOnlineApplication {

    public static void main(String[] args) {
        SpringApplication.run(PizzaOnlineApplication.class, args);
    }

}
