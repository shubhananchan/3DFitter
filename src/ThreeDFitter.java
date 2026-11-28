import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

public class ThreeDFitter {
    // Total clearance between hole and peg, in mm. Starting values, tuned later with test prints.
    static final double PRESS_GAP_MM = 0.05;
    static final double SNUG_GAP_MM = 0.10;
    static final double SLIDE_GAP_MM = 0.20;
    static final double LOOSE_GAP_MM = 0.40;

    static final String OFFSET_FILE = "printer_offset.properties";

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("calibrate")) {
            runCalibration(args);
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
            offsetMm = loadOffset();
        } catch (FileNotFoundException e) {
            System.out.println("No " + OFFSET_FILE + " found, using offset 0. Run calibrate first for printer-corrected sizes.");
            offsetMm = 0;
        } catch (IOException | NumberFormatException e) {
            System.out.println("Could not read " + OFFSET_FILE + ". Run calibrate again to rewrite it.");
            return;
        }

        String fitType = args[1].toLowerCase();
        System.out.printf("3DFitter: %.3f mm, %s fit, printer offset %.3f mm%n", nominalSizeMm, fitType, offsetMm);
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
        System.out.println("Fit types: press, snug, slide, loose");
    }

    // Calibration mode: compare a test hole's CAD size with its caliper measurement and save the offset.
    static void runCalibration(String[] args) {
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
            saveOffset(offsetMm);
        } catch (IOException e) {
            System.out.println("Could not save " + OFFSET_FILE + ": " + e.getMessage());
            return;
        }
        System.out.printf("Saved offset %.3f mm to %s%n", offsetMm, OFFSET_FILE);
    }

    // Negative means holes print smaller than drawn (10.00 drawn, 9.85 measured gives -0.15).
    // Rounded to 0.001 mm because 9.85 - 10.00 in doubles gives -0.15000000000000036.
    static double calculateOffset(double plannedMm, double measuredMm) {
        return Math.round((measuredMm - plannedMm) * 1000) / 1000.0;
    }

    static void saveOffset(double offsetMm) throws IOException {
        Properties settings = new Properties();
        settings.setProperty("offset", String.valueOf(offsetMm));
        try (FileWriter writer = new FileWriter(OFFSET_FILE)) {
            settings.store(writer, "3DFitter printer offset in mm (measured minus planned)");
        }
    }

    static double loadOffset() throws IOException {
        Properties settings = new Properties();
        try (FileReader reader = new FileReader(OFFSET_FILE)) {
            settings.load(reader);
        }
        // A missing key becomes "" so parseDouble throws NumberFormatException instead of crashing.
        double offsetMm = Double.parseDouble(settings.getProperty("offset", ""));
        if (!Double.isFinite(offsetMm)) {
            throw new NumberFormatException("offset is not a finite number");
        }
        return offsetMm;
    }
}
