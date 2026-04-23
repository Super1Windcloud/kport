package com.example.demo;

import com.example.demo.config.ProcessKillProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ProcessKillProperties.class)
public class DemoSpringbootApplication {
  public static void main(String[] args) {
    SpringApplication.run(DemoSpringbootApplication.class, args);
  }
}
