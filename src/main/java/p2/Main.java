package p2;

import p2.btrfs.BtrfsFile;
import p2.storage.FileSystem;
import p2.storage.StringDecoder;
import p2.storage.StringEncoder;

/**
 * Main entry point in executing the program.
 */
public class Main {

    /**
     * Main entry point in executing the program.
     *
     * @param args program arguments, currently ignored
     */
    public static void main(String[] args) {

        FileSystem fileSystem = new FileSystem(AllocationStrategy.NEXT_FIT, 100);

        BtrfsFile file = fileSystem.createFile("Helo World!", new StringEncoder());

        fileSystem.insertIntoFile(file, 3, "l", new StringEncoder());

        System.out.println(fileSystem.readFile(file, new StringDecoder()));

        fileSystem.insertIntoFile(file, 6, "beautiful ", new StringEncoder());

        System.out.println(fileSystem.readFile(file, new StringDecoder(), 1, file.getSize() - 1));

    }
}
