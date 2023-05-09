package p2;

public class BtrfsFile {

    BtrfsNode root;

    public BtrfsFile(char[] data, int n) {
        root = new BtrfsNode(n);
    }

    public void write(int start, char[] data) {
    }

    public void insert(int start, char[] data) {
    }

    char[] read(int start, int length) {
        return null;
    }

    void remove(int start, int length) {

    }
}
