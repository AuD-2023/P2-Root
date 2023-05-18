package p2.storage;

public interface DataDecoder<T> {

    T decode(StorageView data);

}
