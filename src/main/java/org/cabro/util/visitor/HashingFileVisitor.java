/*
Copyright 2017 cabro.org

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.cabro.util.visitor;

import org.cabro.util.DuplicateFileUtility;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Gets information about files it visits to build a list of duplicate files.
 * As it visits files, it first makes a list of file sizeSet, if it finds files with
 * similar file sizeSet, it will use a hash in order to distinguish between them. It only
 * md5Set part of the file to try to be as efficient as possible.
 *
 * @author Phil
 *
 */
public class HashingFileVisitor implements FileVisitor<Path> {

    public Integer fileCount;
    public Integer directoryCount;
    private final List<Path> sizeDuplicates;

    private final List<Path> hashDuplicates;
    private final Set<Long> sizeSet;
    private final Set<String> md5Set;
    private final Set<Path> uniqueFiles;

    public HashingFileVisitor() {
        fileCount = 0;
        directoryCount = 0;
        hashDuplicates = new ArrayList<>();
        sizeDuplicates = new ArrayList<>();
        md5Set = new HashSet<>();
        sizeSet = new HashSet<>();
        uniqueFiles = new HashSet<>();
    }

    public Set<Path> getUniqueFiles() {
        return uniqueFiles;
    }

    public List<Path> getHashDuplicates() {
        return hashDuplicates;
    }

    public String printDuplicates() {
        StringBuilder rv = new StringBuilder();

        if (hashDuplicates.size() > 0) {
            int counter = 1;
            for (Path dupe : hashDuplicates) {
                if (counter < hashDuplicates.size()) {
                    rv.append(dupe.toAbsolutePath()).append(System.lineSeparator());
                } else {
                    rv.append(dupe.toAbsolutePath());
                }
                counter++;
            }
        } else {
            rv.append("Nothing found");
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
        if (attrs.isRegularFile() && Files.isReadable(file)) {
            try {
                if (!sizeSet.add(attrs.size())) {
                    sizeDuplicates.add(file);

                    // Found a size duplicate let's see if it's hash is there and if not add it
                    String hash = DuplicateFileUtility.getHash(file);

                    if (!md5Set.add(hash)) {
                        hashDuplicates.add(file);
                    } else {
                        // Lets check a little better before we ignore the fact that this hash wasn't found
                        List<Path> o = sizeDuplicates.stream()
                                .filter( s -> s.toFile().length() == attrs.size() )
                                .collect(Collectors.toList());

                        // Existing file md5Set that match the size of this guy
                        for (Path p : o) {
                            if (!p.equals(file)) {
                                String tHash = DuplicateFileUtility.getHash(p);
                                if (tHash.equals(hash)) {
                                    // There was one in there that was duplicated so lets add it so
                                    // we don't have to do this again
                                    hashDuplicates.add(file);
                                }
                            }
                        }
                    }
                }
            } catch (FileNotFoundException e) {
                DuplicateFileUtility.println(file.toString() + ": Not Found");
            }

            uniqueFiles.add(file);
        }

        fileCount++;
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        DuplicateFileUtility.println("Unable to visit " + file.toString());
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
    }
}
