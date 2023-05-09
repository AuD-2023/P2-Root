package p2;

import java.util.List;

public class Disk {

    Integer[] data;
    List<BtrfsFile<?>> files;

    AllocationStrategy allocator;

    public Disk(AllocationStrategy.Factory factory, int size) {
        this.allocator = factory.create(this);
        Integer[] data = new Integer[size];
    }

    public void garbageCollect() {

    }

}
