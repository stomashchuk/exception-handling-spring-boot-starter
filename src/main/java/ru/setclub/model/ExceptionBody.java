package ru.setclub.model;

import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public class ExceptionBody {

    private final UUID uuid;
    private final String message;
    private final HttpStatus httpStatus;
    private final String path;
    private final ZonedDateTime timestamp;

}
