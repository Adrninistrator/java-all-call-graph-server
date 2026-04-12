package com.github.adrninistrator.jacgserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Java All Call Graph Server 启动类
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@SpringBootApplication
public class JacgServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(JacgServerApplication.class, args);
    }
}
