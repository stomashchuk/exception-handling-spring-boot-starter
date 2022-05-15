package ru.setclub;

import java.util.Map;
import java.util.Set;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "custom.exceptions.handling")
public class ExceptionHandlingProperties {

    private Map<String, Integer> exceptionToHttpStatusMap;
    private Set<String> exceptionsWithMessageForwarding;

}