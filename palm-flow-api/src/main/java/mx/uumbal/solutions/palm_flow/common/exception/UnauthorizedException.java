package mx.uumbal.solutions.palm_flow.common.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }

    public UnauthorizedException() {
        super("Unauthorized", HttpStatus.UNAUTHORIZED);
    }
}
