package az.code.tourapp.exceptions;

public class NoSuchRequestException extends RuntimeException{
    public NoSuchRequestException() {
        super("This request does not exists or it is cancelled.");
    }
}