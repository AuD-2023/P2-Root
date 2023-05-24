package p2.storage;

import p2.btrfs.BtrfsFile;

import java.util.ArrayList;
import java.util.List;

/**
 * A file system that uses {@linkplain BtrfsFile BtrfsFiles} to represent its files.
 * It manages the allocation and files in a given storage.
 * It does not support directories or multiple data types in a file.
 */
public class FileSystem {

    /**
     * The storage that this file system uses to store the files.
     */
    private final Storage storage;

    /**
     * The files that are stored in this file system.
     */
    private final List<BtrfsFile> files = new ArrayList<>();

    /**
     * The array that is used to mark intervals as used.
     * This is necessary to allocate new intervals without overwriting existing files.
     */
    private final boolean[] used;

    /**
     * The allocation strategy that is used to allocate new intervals.
     */
    AllocationStrategy allocator;

    /**
     * Creates a new file system of a given size that uses the given allocation strategy.
     *
     * @param factory The factory that will be used to create the allocation strategy.
     * @param size The size of the storage that will be used to store the files.
     */
    public FileSystem(AllocationStrategy.Factory factory, int size) {
        storage = new ArrayStorage(size);
        used = new boolean[size];
        this.allocator = factory.create(used);
        allocator.setMaxIntervalSize(2); //for easier testing; can be removed
    }

    /**
     * Creates a new file in this file system that contains the given data.
     *
     * @param name The name of the file.
     * @param data The initial data of the file.
     * @param encoder The encoder that will be used to encode the data.
     * @param <T> The type of the data that is initially stored.
     * @return The new file.
     */
    public <T> BtrfsFile createFile(String name, T data, DataEncoder<T> encoder) {
        byte[] encoded = encoder.encode(data);
        List<Interval> intervals = allocator.allocate(encoded.length);
        BtrfsFile file = new BtrfsFile(name, storage, 3);
        file.insert(0, intervals, encoded);
        files.add(file);
        return file;
    }

    /**
     * Inserts data into a file at a given position.
     *
     * @param fileName the name of the file.
     * @param start The position (logical address) at which the data will be inserted.
     * @param data The data that will be inserted.
     * @param encoder The encoder that will be used to encode the data.
     * @param <T> The type of the data that will be inserted.
     * @throws NoSuchBtrfsFileException If there is no file with the given name.
     */
    public <T> void insertIntoFile(String fileName, int start, T data, DataEncoder<T> encoder) throws NoSuchBtrfsFileException {
        BtrfsFile file = getFile(fileName);

        if (!files.contains(file)) {
            throw new IllegalArgumentException("File not part of this fileSystem");
        }

        byte[] encoded = encoder.encode(data);
        List<Interval> intervals = allocator.allocate(encoded.length);
        file.insert(start, intervals, encoded);
    }

    /**
     * Writes data into a file at a given position. This will overwrite existing data.
     *
     * @param fileName the name of the file.
     * @param start The position (logical address) at which the data will be inserted.
     * @param data The data that will be inserted.
     * @param encoder The encoder that will be used to encode the data.
     * @param <T> The type of the data that will be inserted.
     * @throws NoSuchBtrfsFileException If there is no file with the given name.
     */
    public <T> void writeIntoFile(String fileName, int start, T data, DataEncoder<T> encoder) throws NoSuchBtrfsFileException {
        BtrfsFile file = getFile(fileName);

        if (!files.contains(file)) {
            throw new IllegalArgumentException("File not part of this fileSystem");
        }

        byte[] encoded = encoder.encode(data);
        List<Interval> intervals = allocator.allocate(encoded.length);
        file.write(start, intervals, encoded);
    }

    /**
     * Reads the whole data stored inside a file.
     *
     * @param fileName The name of the file.
     * @param encoder The encoder that will be used to decode the data.
     * @param <T> The type of the data that is stored inside the file.
     * @return The data that is stored inside the file.
     * @throws NoSuchBtrfsFileException If there is no file with the given name.
     */
    public <T> T readFile(String fileName, DataEncoder<T> encoder) throws NoSuchBtrfsFileException {
        BtrfsFile file = getFile(fileName);

        if (!files.contains(file)) {
            throw new IllegalArgumentException("File not part of this fileSystem");
        }

        StorageView data = file.read(0, file.getSize());
        return encoder.decode(data);
    }

    /**
     * Reads a portion of the data stored inside a file.
     *
     * @param fileName The name of the file.
     * @param decoder The decoder that will be used to decode the data.
     * @param start The start position (logical address) at which the reading will start.
     * @param length The amount of bytes that will be read.
     * @param <T> The type of the data that is stored inside the file.
     * @return The data that was read
     */
    public <T> T readFile(String fileName, DataEncoder<T> decoder, int start, int length) throws NoSuchBtrfsFileException {
        BtrfsFile file = getFile(fileName);

        if (!files.contains(file)) {
            throw new IllegalArgumentException("File not part of this fileSystem");
        }

        StorageView data = file.read(start, length);
        return decoder.decode(data);
    }

    /**
     * Removes a portion of the data stored inside a file.
     *
     * @param fileName The name of the file.
     * @param start The start position (logical address) at which the removal will start.
     * @param length The amount of bytes that will be removed.
     * @throws NoSuchBtrfsFileException If there is no file with the given name.
     */
    public void removeFromFile(String fileName, int start, int length) throws NoSuchBtrfsFileException {
        BtrfsFile file = getFile(fileName);

        if (!files.contains(file)) {
            throw new IllegalArgumentException("File not part of this fileSystem");
        }

        file.remove(start, length);
    }

    /**
     * Returns the size of the storage that is used by this file system.
     * This is equal to the amount of bytes that can be stored in this file system.
     *
     * @return The size of the storage that is used by this file system.
     */
    public int getSize() {
        return storage.getSize();
    }

    /**
     * Recycles unused intervals in the storage and marks them as free.
     */
    public void garbageCollect() {
        //TODO
    }

    private BtrfsFile getFile(String name) {
        for (BtrfsFile file : files) {
            if (file.getName().equals(name)) {
                return file;
            }
        }

        throw new NoSuchBtrfsFileException("name");
    }

}
