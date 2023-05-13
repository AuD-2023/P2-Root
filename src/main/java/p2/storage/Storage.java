package p2.storage;

import p2.AllocationStrategy;

public interface Storage {

    StorageView createView(StorageInterval interval);

    void write(int start, byte[] data, int length);

    AllocationStrategy getAllocationStrategy();
}
