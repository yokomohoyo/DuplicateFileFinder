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

package org.cabro.util;

import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.cabro.util.visitor.HashingFileVisitor;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Helps find duplicate files using a couple of different
 * hash types. Geared for CLI usage at this time.
 *
 * @author Phil
 */
public class DuplicateFileUtility {
    // Default amount of bytes to slurp from file to hash
    private static final int BYTES_TO_READ = 8192; // 8k

    // Exit Codes
    private static final Integer EXIT_CODE_NORMAL = 0;
    private static final Integer EXIT_CODE_ERROR = 1;

    // Date formatter
    private static final SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");

    // Default Hashing Algorithm & digest
    private static final String DEFAULT_HASH = "MD5";
    private static MessageDigest md = null;

    // print extra stuff
    public static boolean verbose = false;
    public static boolean debug = false;

    // Toggle full file hashing
    private static boolean READ_WHOLE = false;

    static {
        try {
            md = MessageDigest.getInstance(DEFAULT_HASH);
        } catch (NoSuchAlgorithmException nsae) {
            println("Unable to get requested algorithm");
            System.exit(EXIT_CODE_ERROR);
        }
    }

    /**
     * Meat and potatoes.
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        long startTime = System.currentTimeMillis();

        // Being initialize CLI
        Options options = new Options();
        options.addOption("a", false, "Read the whole file for hashing comparison");
        options.addOption("p", true, "The path to scan for duplicate files. Prints the nth file found to STDOUT");
        options.addOption("n", true, "Find duplicate of this file (needle)");
        options.addOption("h", true, "The path to find the needle file (haystack)");

        // Help and troubleshooting
        options.addOption("?", false, "Print some helpful text");
        options.addOption("t", false, "Display current time and exit");
        options.addOption("v", false, "Verbose");
        options.addOption("d", false, "Debug");

        // Cooy only unique files to target directory
        options.addOption("c", true ,"Copy a de-duplicated list files to this target directory");

        // Print unique files to stdout
        options.addOption("u", false ,"Print a list of unique files found in this path");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
            // Get the input from the CLI
            cmd = parser.parse(options, args);
        } catch (ParseException pe) {
            //Unable to parse this noise
            println("Unable to interpret the command ... exiting ...");
            System.exit(EXIT_CODE_ERROR);
        }

        // Make sure we have args
        assert cmd != null;

        verbose = cmd.hasOption("v");
        debug = cmd.hasOption("d");

        if (cmd.hasOption("?")) {
            //User wants some help
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("dupe", options);
            System.exit(EXIT_CODE_NORMAL);
        }

        // Print the timestamp and end
        if (cmd.hasOption("t")) {
            //User wants to print the date and end
            println("The time is: " + sdf.format(new Date()));
            System.exit(EXIT_CODE_NORMAL);
        }

        // Crawl a path to find duplicates
        if (cmd.hasOption("p")) {
            String p = cmd.getOptionValue("p");
            READ_WHOLE = cmd.hasOption("a");

            HashingFileVisitor visitor = new HashingFileVisitor();

            Files.walkFileTree(Paths.get(p), visitor);

            if (cmd.hasOption("u")) {
                for(Path u : visitor.getUniqueFiles()) {
                    println(u.toString());
                }
                System.exit(EXIT_CODE_NORMAL);
            }

            // Want to copy unique files found in this path to the target destination
            if (cmd.hasOption("c")) {
                String destBase = cmd.getOptionValue("c");
                for (Path u : visitor.getUniqueFiles()) {
                    try {
                        Path dest = Paths.get(destBase + u.toString().replace(p, ""));
                        if (verbose)
                            println("Copying: " + u.toString() + " to: " + dest.toString());
                        Files.copy(u, dest);
                    } catch (IOException ioe) {
                        println("Unable to copy: " + u.toString());
                    }
                }
            } else {
                println(visitor.printDuplicates());
                if (verbose) {
                    println("");
                    println("Number of duplicates files found: " + visitor.getDuplicates().size());
                    println("Number of files processed: " + visitor.fileCount);
                    println("Number of directories processed: " + visitor.directoryCount);
                }
            }
        }

        // Find a needle in a haystack
        if (cmd.hasOption("n")) {
            // Lets find a duplicate of this particular file
            Path f = Paths.get(cmd.getOptionValue("n"));
            Long size = f.toFile().length();
            String hash = getHash(f);
            List<Path> sizeDupe = Files.walk(Paths.get(cmd.getOptionValue("h")))
                    .filter(file -> size.equals(file.toFile().length()))
                    .collect(Collectors.toList());

            if (sizeDupe.size() > 0) {
                // Hash the remaining files to verify duplicity
                for (Path p : sizeDupe) {
                    if (!p.equals(f)) {
                        String h = getHash(p);
                        if (h.equals(hash))
                            println(p.toString());
                    }
                }
            }
        }

        // Print the run duration if desired
        Double elapsed = ((double) System.currentTimeMillis() - startTime) / 1000;
        if (verbose)
            println("Time elapsed: " + elapsed + "(s)");
        System.exit(EXIT_CODE_NORMAL);
    }

    /**
     * Hashing function. Would be better to move to it's own class
     *
     * @param file
     * @return
     * @throws IOException
     */
    public static String getHash(Path file) throws IOException {
        StringBuilder hash = new StringBuilder();

        // Just read the last 8k of the file
        FileInputStream fis = new FileInputStream(file.toFile());

        int fileLength = Math.toIntExact(file.toFile().length());
        int readLength;
        if (READ_WHOLE) {
            readLength = fileLength;
            if (debug)
                println("(getHash) Reading " + file.toString() + " file: " + fileLength);
        } else {
            readLength = Math.min(fileLength, BYTES_TO_READ);
        }

        int offset = fileLength - readLength;

        byte[] fReadBuffer = new byte[fileLength];
        IOUtils.read( fis, fReadBuffer, offset, readLength );
        byte[] h = md.digest(fReadBuffer);
        fis.close();
        for (byte aH : h) hash.append(Integer.toString((aH & 0xff) + 0x100, 16).substring(1));

        return hash.toString();
    }

    public static void println(String message) {
        System.out.println(message);
    }

}
