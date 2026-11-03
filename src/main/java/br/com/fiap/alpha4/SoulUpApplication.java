package br.com.fiap.alpha4;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import java.time.Clock;
import java.time.ZoneId;

@SpringBootApplication
public class SoulUpApplication {
    public static void main(String[] args) {
        SpringApplication.run(SoulUpApplication.class, args);
    }

    @Bean
    Clock clock() {
        return Clock.system(ZoneId.of("America/Sao_Paulo"));
    }
}
