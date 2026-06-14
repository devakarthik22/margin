package com.margin;

import com.margin.scm.github.GitHubProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(GitHubProperties.class)
public class MarginApplication {
    public static void main(String[] args) {
        SpringApplication.run(MarginApplication.class, args);
    }
}
