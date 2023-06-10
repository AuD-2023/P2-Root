package p2.storage;

public class NoSuchBtrfsFileException extends RuntimeException {

    public NoSuchBtrfsFileException(String message) {
        super(message);
    }

}
