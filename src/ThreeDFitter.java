public class ThreeDFitter {
    // Total clearance between hole and peg, in mm. Starting values, tuned later with test prints.
    static final double PRESS_GAP_MM = 0.05;
    static final double SNUG_GAP_MM = 0.10;
    static final double SLIDE_GAP_MM = 0.20;
    static final double LOOSE_GAP_MM = 0.40;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java ThreeDFitter <size-mm> <fit-type>");
            System.out.println("Fit types: press, snug, slide, loose");
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

        String fitType = args[1].toLowerCase();
        System.out.printf("3DFitter: %.3f mm, %s fit%n", nominalSizeMm, fitType);
        System.out.printf("Hole: %.3f mm%n", holeSize(nominalSizeMm, gapMm));
        System.out.printf("Peg:  %.3f mm%n", pegSize(nominalSizeMm, gapMm));
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
}
