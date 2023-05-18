package p2.storage;

public interface DataEncoder<T> {

    byte[] encode(T data);
}
