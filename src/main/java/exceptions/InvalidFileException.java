package exceptions;

public class InvalidFileException extends RuntimeException {

    private int statusCode;
    private String responseCode;

    public InvalidFileException(String message) {
        super(message);
    }

    public InvalidFileException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidFileException(String message, int statusCode, String responseCode) {
        super(message);
        this.statusCode = statusCode;
        this.responseCode = responseCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseCode() {
        return responseCode;
    }
}

