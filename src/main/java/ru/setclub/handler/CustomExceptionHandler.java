package ru.setclub.handler;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import ru.setclub.ExceptionHandlingProperties;
import ru.setclub.exceptions.BusinessException;
import ru.setclub.model.ExceptionBody;
import ru.setclub.exceptions.ValidationException;

@Slf4j
@ControllerAdvice
public class CustomExceptionHandler {

    private final ExceptionHandlingProperties properties;
    private final Map<Class<? extends Throwable>, HttpStatus> httpStatusMap;
    private final Collection<Class<? extends Throwable>> exceptionsWithMessageForwarding;

    private static final String ERROR_TEXT = "Произошла системная ошибка. Обратитесь в службу поддержки. Код ошибки: %s.";

    public CustomExceptionHandler(ExceptionHandlingProperties properties) {
        this.properties = properties;
        this.httpStatusMap = constructHttpStatusMap();
        this.exceptionsWithMessageForwarding = constructExceptionWithMessageForwardingSet();
    }

    @ExceptionHandler
    public ResponseEntity<Object> handleException(Exception e, ServletWebRequest request) {
        return constructResponse(request.getRequest().getRequestURI(), e);
    }

    private ResponseEntity<Object> constructResponse(String requestUri, Exception e) {
        UUID uuid = UUID.randomUUID();
        log.error("Exception with id " + uuid, e);

        Throwable realException = determineException(e);
        Class<? extends Throwable> thrownExceptionClass = realException.getClass();
        String message;
        if (exceptionsWithMessageForwarding.contains(thrownExceptionClass)) {
            message = realException.getMessage();
        } else {
            message = String.format(ERROR_TEXT, uuid);
        }

        HttpStatus status = determineResponseStatus(thrownExceptionClass, e);
        ExceptionBody exceptionBody = new ExceptionBody(
            uuid,
            message,
            status,
            requestUri,
            ZonedDateTime.now(ZoneId.of("Z"))
        );
        return ResponseEntity
            .status(status)
            .body(exceptionBody);
    }

    private Throwable determineException(Exception e) {
        // Because BusinessException is a wrapper around real exception, we need to get cause if it exists
        if (e instanceof BusinessException) {
            return e.getCause() == null
                ? e
                : e.getCause();
        } else {
            return e;
        }
    }

    private HttpStatus determineResponseStatus(Class<? extends Throwable> thrownExceptionClass, Exception e) {
        // 'computeIfAbsent' method looks if there is a value exist in the map with such key.
        // If it exists then returns it. Else computes value with received function and puts it in the map.
        // When the next call will occur this key will have already been in the map.
        return httpStatusMap.computeIfAbsent(thrownExceptionClass, ignored -> {
            for (Entry<Class<? extends Throwable>, HttpStatus> entry : httpStatusMap.entrySet()) {
                // If thrown exception is a child object of a key from map (look at 'isInstance' method documentation)
                if (entry.getKey().isInstance(e)) {
                    return entry.getValue();
                }
            }
            return HttpStatus.INTERNAL_SERVER_ERROR;
        });
    }

    @SuppressWarnings("unchecked")
    private Map<Class<? extends Throwable>, HttpStatus> constructHttpStatusMap() {
        // Using IdentityHashMap because classes are comparing by link
        Map<Class<? extends Throwable>, HttpStatus> result = new IdentityHashMap<>();
        result.put(ValidationException.class, HttpStatus.BAD_REQUEST);
        // If properties in a client project contains additional values, then add them
        Map<String, Integer> statusMapFromProps =
            properties == null
                ? null
                : properties.getExceptionToHttpStatusMap();
        try {
            if (statusMapFromProps != null) {
                for (Map.Entry<String, Integer> exClassName : statusMapFromProps.entrySet()) {
                    Class<? extends Throwable> exClass =
                        (Class<? extends Throwable>) Class.forName(exClassName.getKey());
                    HttpStatus exStatus = HttpStatus.valueOf(exClassName.getValue());
                    result.put(exClass, exStatus);
                }
            }
        } catch (Exception e) {
            log.error("Can't add some of the exception classes and statuses from properties to httpStatus Map.", e);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Collection<Class<? extends Throwable>> constructExceptionWithMessageForwardingSet() {
        // Using IdentityHashMap because classes are comparing by link
        Set<Class<? extends Throwable>> result = Collections.newSetFromMap(new IdentityHashMap<>());
        result.add(ValidationException.class);
        // If properties in a client project contains additional values, then add them
        Set<String> exceptionClassNames =
            properties == null
                ? null
                : properties.getExceptionsWithMessageForwarding();
        try {
            if (exceptionClassNames != null) {
                for (String exClassName : exceptionClassNames) {
                    Class<? extends Throwable> exClass =
                        (Class<? extends Throwable>) Class.forName(exClassName);
                    result.add(exClass);
                }
            }
        } catch (Exception e) {
            log.error("Can't add some of the exception classes from properties "
                + "to exceptionsWithMessageForwarding Set.", e);
        }
        return result;
    }
}
