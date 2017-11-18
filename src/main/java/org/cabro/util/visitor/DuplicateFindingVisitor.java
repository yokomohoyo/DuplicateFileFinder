package org.cabro.util.visitor;

import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Gets information about a list of files
 */
public class DuplicateFindingVisitor implements FileVisitor<Path> {

    private static final String DEFAULT_HASH = "MD5";

    public Integer fileCount;
    public Integer directoryCount;
    public List<Path> hashDuplicates;
    public List<Path> sizeDuplicates;
    private Set<Long> fileSizes;
    private Set<byte[]> hashes;

    public DuplicateFindingVisitor() {
        fileCount = 0;
        directoryCount = 0;
        hashDuplicates = new ArrayList<>();
        sizeDuplicates = new ArrayList<>();
        fileSizes = new HashSet<>();
        hashes = new HashSet<>();
    }

    public String printDuplicates() {
        StringBuffer rv = new StringBuffer();
        List<Path> finalDuplicateList = new ArrayList<>(sizeDuplicates);
        finalDuplicateList.addAll(hashDuplicates);

        for (Path dupe : finalDuplicateList) {
            rv.append(dupe.toAbsolutePath() + System.lineSeparator());
        }

        return rv.toString();
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        // Increment our counters
        directoryCount++;
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Long size = Files.size(file);

        if (attrs.isRegularFile()) {
            if (!fileSizes.add(size)) {
                sizeDuplicates.add(file);

                // If false, it's a duplicate so let's hash it and see if it's a duplicate for real
                MessageDigest md = null;
                try {
                    md = MessageDigest.getInstance(DEFAULT_HASH);
                } catch (NoSuchAlgorithmException nsae) {
                    nsae.printStackTrace();
                    System.exit(1);
                }

                byte[] h = md.digest(IOUtils.toByteArray(new FileInputStream(file.toFile()), Integer.valueOf((int)(long)(attrs.size() * .10))));

                // Try adding it, and if it returns something then the key was used and it's returning the value
                if (!hashes.add(h)) {
                    hashDuplicates.add(file);
                }
            }
        }

        fileCount++;
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        System.out.print("Unable to visit " + file.toString());
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
    }
}
