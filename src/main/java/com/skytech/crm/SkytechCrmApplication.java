package com.skytech.crm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication
public class SkytechCrmApplication {
  public static void main(String[] args) {
    SpringApplication.run(SkytechCrmApplication.class, args);
  }
}
