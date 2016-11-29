public class Main {

    public static void main(String[] args) {
        final TravelingSalesmanAnneal sa = new TravelingSalesmanAnneal(1000, 400, 0.001);
        sa.run();
    }
}
