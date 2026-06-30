package mx.uumbal.solutions.palm_flow.common.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

    public NotFoundException(String resource, Object id) {
        super(String.format("%s not found with id: %s", resource, id), HttpStatus.NOT_FOUND);
    }
}
