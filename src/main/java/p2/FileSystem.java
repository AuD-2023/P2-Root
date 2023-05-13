package p2;

import p2.btrfs.BtrfsFile;

import java.util.List;

public class FileSystem {

    Integer[] data;
    List<BtrfsFile> files;

    AllocationStrategy allocator;

    public FileSystem(AllocationStrategy.Factory factory, int size) {
        this.allocator = factory.create(this);
        Integer[] data = new Integer[size];
    }

    public void garbageCollect() {

    }

}
