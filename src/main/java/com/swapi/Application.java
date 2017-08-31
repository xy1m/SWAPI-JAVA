package com.swapi;

import com.swapi.sw.StarWars;
import com.swapi.sw.StarWarsApi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public StarWars starWarsApi() {
        StarWarsApi.init();
        return StarWarsApi.getApi();
    }
}