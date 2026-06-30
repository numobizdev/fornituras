package mx.uumbal.solutions.palm_flow.common.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }

    public ForbiddenException() {
        super("Access denied", HttpStatus.FORBIDDEN);
    }
}
