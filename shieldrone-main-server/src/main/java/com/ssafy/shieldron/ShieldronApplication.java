package com.ssafy.shieldron;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class ShieldronApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShieldronApplication.class, args);
    }

}
