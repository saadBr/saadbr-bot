package net.saadbr.saadbrbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "net.saadbr.saadbrbot.clients")

public class SaadbrBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(SaadbrBotApplication.class, args);
    }

}
