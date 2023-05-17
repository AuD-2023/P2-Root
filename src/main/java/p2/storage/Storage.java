package p2.storage;

import p2.AllocationStrategy;

public interface Storage {

    StorageView createView(Interval... intervals);

    void write(int start, byte[] data, int length);

    byte get(int index);

    AllocationStrategy getAllocationStrategy();
}
