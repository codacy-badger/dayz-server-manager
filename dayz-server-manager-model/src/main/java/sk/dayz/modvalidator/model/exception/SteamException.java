package sk.dayz.modvalidator.model.exception;

public class SteamException extends RuntimeException {

    public SteamException(String message) {
        super(message);
    }

    public SteamException(String message, Throwable cause) {
        super(message, cause);
    }
}
