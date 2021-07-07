package az.code.tourapp.exceptions;

public class MissingFirstQuestionException extends RuntimeException{
    public MissingFirstQuestionException() {
        super("You need a fist question to start. Fist question should have id of 1.");
    }
}
