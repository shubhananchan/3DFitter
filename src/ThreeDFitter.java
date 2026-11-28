import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

public class ThreeDFitter {
    // Total clearance between hole and peg, in mm. Starting values, tuned later with test prints.
    static final double PRESS_GAP_MM = 0.05;
    static final double SNUG_GAP_MM = 0.10;
    static final double SLIDE_GAP_MM = 0.20;
    static final double LOOSE_GAP_MM = 0.40;

    static final String OFFSET_FILE = "printer_offset.properties";
    static final String PROFILE_FLAG = "--profile";

    public static void main(String[] args) {
        // An optional "--profile <name>" at the end picks which saved offset to use.
        String profile = null;
        if (args.length >= 2 && args[args.length - 2].equals(PROFILE_FLAG)) {
            profile = args[args.length - 1];
            if (!isValidProfileName(profile)) {
                System.out.println("Profile names use letters, numbers, - and _ only, got: " + profile);
                return;
            }
            args = Arrays.copyOf(args, args.length - 2);
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("calibrate")) {
            runCalibration(args, profile);
            return;
        }

        if (args.length != 2) {
            printUsage();
            return;
        }

        double nominalSizeMm;
        double gapMm;
        try {
            nominalSizeMm = parseSize(args[0]);
            gapMm = gapForFit(args[1]);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            return;
        }

        double offsetMm;
        try {
            offsetMm = loadOffset(profile);
        } catch (IOException | NumberFormatException e) {
            System.out.println("Could not read " + OFFSET_FILE + ". Run calibrate again to rewrite it.");
            return;
        }

        String fitType = args[1].toLowerCase();
        System.out.printf("3DFitter: %.3f mm, %s fit, %s, printer offset %.3f mm%n",
                nominalSizeMm, fitType, profileLabel(profile), offsetMm);
        System.out.printf("Draw hole at: %.3f mm%n", cadHoleSize(holeSize(nominalSizeMm, gapMm), offsetMm));
        System.out.printf("Draw peg at:  %.3f mm%n", cadPegSize(pegSize(nominalSizeMm, gapMm), offsetMm));
    }

    // Turns the size text into a positive, finite number, or throws with a message for the user.
    static double parseSize(String sizeText) {
        double sizeMm;
        try {
            sizeMm = Double.parseDouble(sizeText);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Size must be a number, got: " + sizeText);
        }

        // parseDouble accepts "NaN" and "Infinity", which the <= 0 check misses.
        if (!Double.isFinite(sizeMm)) {
            throw new IllegalArgumentException("Size must be a finite number, got: " + sizeText);
        }
        if (sizeMm <= 0) {
            throw new IllegalArgumentException("Size must be greater than 0");
        }
        return sizeMm;
    }

    // Returns the total gap for a fit type. Lowercased so "SLIDE" works the same as "slide".
    static double gapForFit(String fitType) {
        String fit = fitType.toLowerCase();
        if (fit.equals("press")) {
            return PRESS_GAP_MM;
        } else if (fit.equals("snug")) {
            return SNUG_GAP_MM;
        } else if (fit.equals("slide")) {
            return SLIDE_GAP_MM;
        } else if (fit.equals("loose")) {
            return LOOSE_GAP_MM;
        }
        throw new IllegalArgumentException("Unknown fit type: " + fitType + " (use press, snug, slide or loose)");
    }

    // The gap is split evenly, so the hole grows by half and the peg shrinks by half.
    static double holeSize(double nominalSizeMm, double gapMm) {
        return nominalSizeMm + gapMm / 2;
    }

    static double pegSize(double nominalSizeMm, double gapMm) {
        return nominalSizeMm - gapMm / 2;
    }

    // Holes print smaller by the offset, so draw them bigger to land on the target size.
    static double cadHoleSize(double targetHoleMm, double offsetMm) {
        return targetHoleMm - offsetMm;
    }

    // Assumes pegs print bigger by the same amount holes print smaller, so draw them smaller.
    static double cadPegSize(double targetPegMm, double offsetMm) {
        return targetPegMm + offsetMm;
    }

    static void printUsage() {
        System.out.println("Usage: java ThreeDFitter <size-mm> <fit-type>");
        System.out.println("       java ThreeDFitter calibrate <planned-mm> <measured-mm>");
        System.out.println("Add --profile <name> to either command to keep a separate offset per printer or filament.");
        System.out.println("Fit types: press, snug, slide, loose");
    }

    // Calibration mode: compare a test hole's CAD size with its caliper measurement and save the offset.
    static void runCalibration(String[] args, String profile) {
        if (args.length != 3) {
            printUsage();
            return;
        }

        double plannedMm;
        double measuredMm;
        try {
            plannedMm = parseSize(args[1]);
            measuredMm = parseSize(args[2]);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            return;
        }

        double offsetMm = calculateOffset(plannedMm, measuredMm);
        try {
            saveOffset(offsetMm, profile);
        } catch (IOException e) {
            System.out.println("Could not save " + OFFSET_FILE + ": " + e.getMessage());
            return;
        }
        System.out.printf("Saved offset %.3f mm for %s to %s%n", offsetMm, profileLabel(profile), OFFSET_FILE);
    }

    // Negative means holes print smaller than drawn (10.00 drawn, 9.85 measured gives -0.15).
    // Rounded to 0.001 mm because 9.85 - 10.00 in doubles gives -0.15000000000000036.
    static double calculateOffset(double plannedMm, double measuredMm) {
        return Math.round((measuredMm - plannedMm) * 1000) / 1000.0;
    }

    static boolean isValidProfileName(String profile) {
        return profile.matches("[A-Za-z0-9_-]+");
    }

    // No profile uses the plain "offset" key, so files saved before profiles existed still work.
    static String offsetKey(String profile) {
        if (profile == null) {
            return "offset";
        }
        return "offset." + profile;
    }

    static String profileLabel(String profile) {
        if (profile == null) {
            return "default profile";
        }
        return "profile " + profile;
    }

    // Reads every saved offset. A missing file means nothing is calibrated yet, so it returns an empty set.
    static Properties loadSettings() throws IOException {
        Properties settings = new Properties();
        try (FileReader reader = new FileReader(OFFSET_FILE)) {
            settings.load(reader);
        } catch (FileNotFoundException e) {
            return settings;
        }
        return settings;
    }

    // Loads the existing file first so saving one profile keeps the offsets of the others.
    static void saveOffset(double offsetMm, String profile) throws IOException {
        Properties settings = loadSettings();
        settings.setProperty(offsetKey(profile), String.valueOf(offsetMm));
        try (FileWriter writer = new FileWriter(OFFSET_FILE)) {
            settings.store(writer, "3DFitter printer offsets in mm (measured minus planned)");
        }
    }

    static double loadOffset(String profile) throws IOException {
        String value = loadSettings().getProperty(offsetKey(profile));
        if (value == null) {
            System.out.println("No saved offset for " + profileLabel(profile)
                    + ", using 0. Run calibrate first for printer-corrected sizes.");
            return 0;
        }

        double offsetMm = Double.parseDouble(value);
        if (!Double.isFinite(offsetMm)) {
            throw new NumberFormatException("offset is not a finite number");
        }
        return offsetMm;
    }
}
