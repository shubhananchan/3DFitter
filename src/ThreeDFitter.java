public class ThreeDFitter {
    // Total clearance between hole and peg, in mm. Placeholder until tuned on my printer.
    static final double SLIDE_GAP_MM = 0.20;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java ThreeDFitter <size-mm> <fit-type>");
            return;
        }
        double nominalSizeMm = Double.parseDouble(args[0]);
        String fitType = args[1];
        double halfGap = SLIDE_GAP_MM / 2;

        double holeSizeMm = nominalSizeMm + halfGap;
        double pegSizeMm = nominalSizeMm - halfGap;

        System.out.printf("3DFitter: %.2f mm, %s fit%n", nominalSizeMm, fitType);
        System.out.printf("Hole: %.2f mm%n", holeSizeMm);
        System.out.printf("Peg:  %.2f mm%n", pegSizeMm);
    }
}
