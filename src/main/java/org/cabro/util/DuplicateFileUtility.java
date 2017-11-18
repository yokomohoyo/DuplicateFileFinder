package org.cabro.util;

import org.apache.commons.cli.*;
import org.cabro.util.visitor.DuplicateFindingVisitor;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Helps find duplicate files using a couple of different
 * hash types. Geared for CLI usage at this time.
 *
 * @author Phil Miller <phil_miller@msn.com>
 */
public class DuplicateFileUtility {
    private static final Integer EXIT_CODE_NORMAL = 0;
    private static final Integer EXIT_CODE_ERROR = 1;

    // Date formatter
    private static final SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");

    // Default Hashing Algorithm
    private static  final String DEFAULT_HASH = "MD5";

    /**
     * Main Function
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        // Being initialize CLI
        Options options = new Options();
        options.addOption("p", true, "The path to scan for hashDuplicates");
        options.addOption("a", true ,"Select the hashing algorithm to use [SHA1|MD5|SHA256]");
        options.addOption("c", true ,"Copies the de-duplicated files to this target directory.");

        // Help and troubleshooting
        options.addOption("h", false, "Print some helpful text");
        options.addOption("t", false, "Display current time and exit");

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
        if (cmd.hasOption("h")) {
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

        // Get the path for traversing ready
        if (cmd.hasOption("p")) {
            String p = cmd.getOptionValue("p");
            //List<Path> files = listFiles(p);

            // Hasher
            String alg = cmd.getOptionValue("a");
            if (alg == null || alg.equalsIgnoreCase("")) {
                alg = DEFAULT_HASH;
            }

            DuplicateFindingVisitor visitor = new DuplicateFindingVisitor();
            Files.walkFileTree(Paths.get(p), visitor);
            println(visitor.printDuplicates());
            println("Number of files processed: " + visitor.fileCount);
            println("Number of directories processed: " + visitor.directoryCount);
            System.exit(EXIT_CODE_NORMAL);

        }
    }

    public static void println (String message) {
        System.out.println(message);
    }

}
