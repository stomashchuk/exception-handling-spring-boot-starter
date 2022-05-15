package ru.setclub;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.setclub.handler.CustomExceptionHandler;

@Configuration
@ConditionalOnProperty(prefix = "custom.exceptions.handling", name = "enable", havingValue = "true")
@EnableConfigurationProperties(ExceptionHandlingProperties.class)
public class ExceptionHandlingConfiguration {

    @Bean
    CustomExceptionHandler customExceptionHandler(ExceptionHandlingProperties properties) {
        return new CustomExceptionHandler(properties);
    }
}
