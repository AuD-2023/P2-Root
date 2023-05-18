package p2.storage;

public interface Storage {

    StorageView createView(Interval... intervals);

    void write(int storagePos, byte[] source, int sourcePos, int length);

    void read(int storagePos, byte[] dest, int destPos, int length);

    int getSize();
}
