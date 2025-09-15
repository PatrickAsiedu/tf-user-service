package app.tradeflows.api.user_service.exceptions;

public class ConflictException extends Exception{
    public ConflictException(String message) {
        super(message);
    }
}
