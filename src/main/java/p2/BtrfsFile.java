package p2;

import java.util.List;

public class BtrfsFile<T extends DiskData<T>> {

    BtrfsNode root;
    int size = 0;

    public BtrfsFile(List<T> data, int n) {
        root = new BtrfsNode(n);
    }

    public void write(int start, T data) {

    }

    public void insert(int start, T data) {

    }

    Integer[] read(int start, int length) {
        return null;
    }

    void remove(int start, int length) {

    }

}
