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
    private static final int BYTES_TO_READ = 4096; // 4k

    // Exit Codes
    private static final Integer EXIT_CODE_NORMAL = 0;
    private static final Integer EXIT_CODE_ERROR = 1;

    // Date formatter
    private static final SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");

    // Default Hashing Algorithm & digest
    private static final String DEFAULT_HASH = "MD5";
    private static MessageDigest md = null;

    // print extra stuff
    private static boolean verbose = false;

    static {
        try {
            md = MessageDigest.getInstance(DEFAULT_HASH);
        } catch (NoSuchAlgorithmException nsae) {
            println("Unable to get requested algorithm");
            System.exit(EXIT_CODE_ERROR);
        }
    }

    /**
     * Main Function
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        long startTime = System.currentTimeMillis();

        // Being initialize CLI
        Options options = new Options();
        options.addOption("p", true, "The path to scan for duplicate files. Prints the nth file found to STDOUT");
        options.addOption("o", true, "Find duplicate of this file (Must indicate haystack)");
        options.addOption("s", true, "The path to find the needle file in -o");

        // Help and troubleshooting
        options.addOption("h", false, "Print some helpful text");
        options.addOption("t", false, "Display current time and exit");
        options.addOption("v", false, "Verbose");

        // Cooy only unique files to target directory
        options.addOption("c", true ,"Copy a de-duplicated list files to this target directory.");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
            // Get the input from the CLI
            cmd = parser.parse(options, args);
        } catch (ParseException pe) {
            //Unable to parse this noise
            println("Unable to parse the command");
            System.exit(EXIT_CODE_ERROR);
        }

        // See if the user wants some help
        assert cmd != null;

        verbose = cmd.hasOption("v");

        if (cmd.hasOption("h")) {
            //User wants some help
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("dupe", options);
        }

        // Print the timestamp and end
        if (cmd.hasOption("t")) {
            //User wants to print the date and end
            println("The time is: " + sdf.format(new Date()));
        }

        // Get the path for traversing ready
        if (cmd.hasOption("p")) {
            String p = cmd.getOptionValue("p");

            HashingFileVisitor visitor = new HashingFileVisitor();

            Files.walkFileTree(Paths.get(p), visitor);
            if (cmd.hasOption("c")) {
                String destBase = cmd.getOptionValue("c");
                // Want to copy unique files to the target destination
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
                    println("Number of files processed: " + visitor.fileCount);
                    println("Number of directories processed: " + visitor.directoryCount);
                }
            }
        }

        // With -o $FILE we need to have a path given with -s
        if (cmd.hasOption("o")) {
            // Lets find a duplicate of this particular file
            String o = cmd.getOptionValue("o");
            Path f = Paths.get(o);

            MessageDigest md = null;
            try {
                md = MessageDigest.getInstance(DEFAULT_HASH);
            } catch (NoSuchAlgorithmException nsae) {
                throw new IOException(nsae);
            }

            Long size = f.toFile().length();
            String hash = getHash(f, f.toFile().length());
            List<Path> sizeDupe = Files.walk(Paths.get(cmd.getOptionValue("s")))
                    .filter(fs -> size.equals(fs.toFile().length()))
                    .collect(Collectors.toList());

            if (sizeDupe.size() > 0) {
                // Hash the remaining files to verify duplicity
                for (Path p : sizeDupe) {
                    String h = getHash(p, p.toFile().length());
                    if (h.equals(hash))
                        println(p.toString());
                }
            }
        }

        // Print the run duration if desired
        Double elapsed = ((double) System.currentTimeMillis() - startTime) / 1000;
        if (verbose)
            println("Time elapsed: " + elapsed + "(s)");
        System.exit(EXIT_CODE_NORMAL);
    }

    public static String getHash(Path file, long bytes) throws IOException {
        StringBuilder hash = new StringBuilder();

        // Just read the smaller of 4k bytes or 25% of the total bytes whichever is smaller
        FileInputStream fis = new FileInputStream(file.toFile());
        byte[] h = md.digest(
                IOUtils.toByteArray(fis, Integer.min((int) (bytes), BYTES_TO_READ)));
        fis.close();
        for (byte aH : h) hash.append(Integer.toString((aH & 0xff) + 0x100, 16).substring(1));

        return hash.toString();
    }

    public static void println(String message) {
        System.out.println(message);
    }

}
