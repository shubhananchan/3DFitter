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
        try {
            nominalSizeMm = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("Size must be a number, got: " + args[0]);
            return;
        }

        // parseDouble accepts "NaN" and "Infinity", which the <= 0 check misses.
        if (!Double.isFinite(nominalSizeMm)) {
            System.out.println("Size must be a finite number, got: " + args[0]);
            return;
        }

        if (nominalSizeMm <= 0) {
            System.out.println("Size must be greater than 0");
            return;
        }

        // Lowercase so "SLIDE" and "Slide" work the same as "slide".
        String fitType = args[1].toLowerCase();
        double gapMm;
        if (fitType.equals("press")) {
            gapMm = PRESS_GAP_MM;
        } else if (fitType.equals("snug")) {
            gapMm = SNUG_GAP_MM;
        } else if (fitType.equals("slide")) {
            gapMm = SLIDE_GAP_MM;
        } else if (fitType.equals("loose")) {
            gapMm = LOOSE_GAP_MM;
        } else {
            System.out.println("Unknown fit type: " + args[1] + " (use press, snug, slide or loose)");
            return;
        }

        double halfGap = gapMm / 2;
        double holeSizeMm = nominalSizeMm + halfGap;
        double pegSizeMm = nominalSizeMm - halfGap;

        // 3 decimals so the 0.025 mm half-gap of a press fit is not rounded away.
        System.out.printf("3DFitter: %.3f mm, %s fit%n", nominalSizeMm, fitType);
        System.out.printf("Hole: %.3f mm%n", holeSizeMm);
        System.out.printf("Peg:  %.3f mm%n", pegSizeMm);
    }
}
