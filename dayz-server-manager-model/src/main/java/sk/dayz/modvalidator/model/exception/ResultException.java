package sk.dayz.modvalidator.model.exception;

public class ResultException extends RuntimeException {

    public ResultException(String message) {
        super(message);
    }

    public ResultException(String message, Throwable cause) {
        super(message, cause);
    }
}
