package com.campus.repair;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = { UserDetailsServiceAutoConfiguration.class })
@MapperScan("com.campus.repair.mapper")
@EnableScheduling
public class CampusRepairApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusRepairApplication.class, args);
    }
}
