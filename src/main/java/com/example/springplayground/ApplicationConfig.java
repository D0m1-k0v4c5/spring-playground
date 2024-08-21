package com.example.springplayground;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * @author d0m1.k0v4c5 on 2024. 08. 19.
 */
@EnableJpaRepositories(basePackages = "com.example")
@EnableJpaAuditing
@Configuration
public class ApplicationConfig {


}
