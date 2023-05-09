package p2.storage;

public interface Storage {

    StorageView createView(StorageInterval interval);

    void write(int start, byte[] data);
}
