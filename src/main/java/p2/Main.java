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

        FileSystem fileSystem = new FileSystem(AllocationStrategy.NEXT_FIT, 200);

        BtrfsFile file = fileSystem.createFile("Helo World!", new StringEncoder());

        System.out.println(fileSystem.readFile(file, new StringDecoder())); // Helo World!

        fileSystem.insertIntoFile(file, 3, "l", new StringEncoder());

        System.out.println(fileSystem.readFile(file, new StringDecoder())); // Hello World!

        fileSystem.insertIntoFile(file, 6, "beautiful and very very very nice and wonderful and i dont know what else ", new StringEncoder());

        System.out.println(fileSystem.readFile(file, new StringDecoder(), 0, file.getSize())); // Hello beautiful and very very very nice and wonderful and i dont know what else World!

        fileSystem.removeFromFile(file, 6, 14);

        System.out.println(fileSystem.readFile(file, new StringDecoder())); // Hello very very very nice and wonderful and i dont know what else World

        fileSystem.removeFromFile(file, 6, 60);

        System.out.println(fileSystem.readFile(file, new StringDecoder())); // Hello World!

        fileSystem.removeFromFile(file, 0, file.getSize());

        System.out.println(fileSystem.readFile(file, new StringDecoder())); // <empty>
    }
}
