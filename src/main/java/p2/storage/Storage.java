package p2.storage;

import p2.AllocationStrategy;

public interface Storage {

    StorageView createView(Interval... intervals);

    void write(int storagePos, byte[] source, int sourcePos, int length);

    void read(int storagePos, byte[] dest, int destPos, int length);

    AllocationStrategy getAllocationStrategy();
}
