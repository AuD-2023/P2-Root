package p2;

import p2.storage.AllocationStrategy;
import p2.btrfs.BtrfsFile;
import p2.storage.FileSystem;
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

        String fileName = "example.txt";
        StringEncoder encoder = StringEncoder.INSTANCE;
        FileSystem fileSystem = new FileSystem(AllocationStrategy.NEXT_FIT, 200);

        BtrfsFile file = fileSystem.createFile(fileName, "Helo", encoder);

        System.out.println(fileSystem.readFile(fileName, encoder)); // Helo!

        fileSystem.insertIntoFile(fileName, 4, " World!", encoder);

        System.out.println(fileSystem.readFile(fileName, encoder)); // Helo World!

        fileSystem.insertIntoFile(fileName, 3, "l", encoder);

        System.out.println(fileSystem.readFile(fileName, encoder)); // Hello World!

        fileSystem.insertIntoFile(fileName, 6, "beautiful and very very very nice and wonderful and i dont know what else ", encoder);

        System.out.println(fileSystem.readFile(fileName, encoder, 0, file.getSize())); // Hello beautiful and very very very nice and wonderful and i dont know what else World!

        fileSystem.removeFromFile(fileName, 6, 14);

        System.out.println(fileSystem.readFile(fileName, encoder)); // Hello very very very nice and wonderful and i dont know what else World

        fileSystem.removeFromFile(fileName, 6, 60);

        System.out.println(fileSystem.readFile(fileName, encoder)); // Hello World!

        fileSystem.removeFromFile(fileName, 0, file.getSize());

        System.out.println(fileSystem.readFile(fileName, encoder)); // <empty>
    }
}
