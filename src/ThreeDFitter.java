public class ThreeDFitter {
    // Total clearance between hole and peg, in mm. Starting values, tuned later with test prints.
    static final double PRESS_GAP_MM = 0.05;
    static final double SNUG_GAP_MM = 0.10;
    static final double SLIDE_GAP_MM = 0.20;
    static final double LOOSE_GAP_MM = 0.40;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java ThreeDFitter <size-mm> <fit-type>");
            return;
        }

        double nominalSizeMm;
        try {
            nominalSizeMm = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("Size must be a number, got: " + args[0]);
            return;
        }

        if (nominalSizeMm <= 0) {
            System.out.println("Size must be greater than 0");
            return;
        }

        String fitType = args[1];
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
            System.out.println("Unknown fit type: " + fitType);
            return;

        }
        double halfGap = gapMm / 2;
        double holeSizeMm = nominalSizeMm + halfGap;
        double pegSizeMm = nominalSizeMm - halfGap;

        System.out.printf("3DFitter: %.2f mm, %s fit%n", nominalSizeMm, fitType);
        System.out.printf("Hole: %.2f mm%n", holeSizeMm);
        System.out.printf("Peg:  %.2f mm%n", pegSizeMm);
    }
}
