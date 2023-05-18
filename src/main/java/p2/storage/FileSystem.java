package p2.storage;

import p2.AllocationStrategy;
import p2.btrfs.BtrfsFile;

import java.util.ArrayList;
import java.util.List;

public class FileSystem {

    private final Storage storage;
    private final List<BtrfsFile> files = new ArrayList<>();

    AllocationStrategy allocator;

    public FileSystem(AllocationStrategy.Factory factory, int size) {
        storage = new ArrayStorage(size);
        this.allocator = factory.create(this);
    }

    public <T> BtrfsFile createFile(T data, DataEncoder<T> encoder) {
        byte[] encoded = encoder.encode(data);
        List<Interval> intervals = allocator.allocate(encoded.length);
        BtrfsFile file = new BtrfsFile(storage, 3);
        file.insert(0, intervals, encoded);
        files.add(file);
        return file;
    }

    public <T> void insertIntoFile(BtrfsFile file, int start, T data, DataEncoder<T> encoder) {
        if (!files.contains(file)) {
            throw new IllegalArgumentException("File not part of this fileSystem");
        }

        byte[] encoded = encoder.encode(data);
        List<Interval> intervals = allocator.allocate(encoded.length);
        file.insert(start, intervals, encoded);
    }

    public <T> T readFile(BtrfsFile file, DataDecoder<T> decoder) {
        if (!files.contains(file)) {
            throw new IllegalArgumentException("File not part of this fileSystem");
        }

        StorageView data = file.read(0, file.getSize());
        return decoder.decode(data);
    }

    public <T> T readFile(BtrfsFile file, DataDecoder<T> decoder, int start, int end) {
        if (!files.contains(file)) {
            throw new IllegalArgumentException("File not part of this fileSystem");
        }

        StorageView data = file.read(start, end);
        return decoder.decode(data);
    }

    public int getSize() {
        return storage.getSize();
    }

    public void garbageCollect() {

    }

}
