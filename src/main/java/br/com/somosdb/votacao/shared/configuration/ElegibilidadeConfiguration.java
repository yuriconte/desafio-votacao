package br.com.somosdb.votacao.shared.configuration;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ElegibilidadeConfiguration {

    @Bean
    RandomGenerator randomGenerator() {
        return new SecureRandom();
    }
}
