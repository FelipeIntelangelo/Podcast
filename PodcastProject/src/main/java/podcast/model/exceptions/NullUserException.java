package podcast.model.exceptions;

public class NullUserException extends RuntimeException {
    public NullUserException(String message) {
        super(message);
    }
}
