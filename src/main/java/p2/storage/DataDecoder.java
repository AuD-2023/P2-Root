package p2.storage;

public interface DataDecoder<T> {

    default T decode(StorageView data) {
        return decode(data.getData());
    }

    T decode(byte[] data);

}
