
import java.util.Arrays;
public class TravelingSalesmanAnneal{
    /**
     * The current global best score.  The global best score is the best score that has been found over all of the
     * iterations.
     */
    private double globalBestScore = Double.POSITIVE_INFINITY;

    /**
     * The current score.
     */
    private double currentScore;

    /**
     * The maximum number of iterations to try.
     */
    private final int kMax;

    /**
     * The current iteration.
     */
    private int k;

    /**
     * The starting temperature.
     */
    private final double startingTemperature;

    /**
     * The ending temperature.
     */
    private final double endingTemperature;

    /**
     * The current temperature.
     */
    private double currentTemperature;

    /**
     * The number of cycles to try at each temperature.
     */
    private int cycles = 100;

    /**
     * The last probability of accepting a new non-improving move.
     */
    private double lastProbability;

    /**
     * The size of the map.
     */
    public static final double MAP_SIZE = 10;

    /**
     * The city count.
     */
    public static final int CITY_COUNT = 50;

    /**
     * The distance calculator.
     */
    private final EuclideanDistance distance = new EuclideanDistance();

    /**
     * The city coordinates.
     */
    private double[][] cities;

    /**
     * A random number generator.
     */
    private final MersenneTwisterGenerateRandom rnd = new MersenneTwisterGenerateRandom();

    /**
     * The current path being evaluated.
     */
    private int[] currentPath;

    /**
     * The backup path, in case the current is not kept.
     */
    private int[] backupPath;

    /**
     * The best path yet.
     */
    private int[] bestPath;

    /**
     * Construct the object.
     */
    public TravelingSalesmanAnneal(final int theKMax, final double theStartingTemperature, final double theEndingTemperature) {
        this.kMax = theKMax;
        this.startingTemperature = theStartingTemperature;
        this.endingTemperature = theEndingTemperature;
    }
    /**
     * Run the example.
     */
    public void run() {
        this.cities = new double[CITY_COUNT][2];
        this.currentPath = new int[CITY_COUNT];
        this.backupPath = new int[CITY_COUNT];
        this.bestPath = new int[CITY_COUNT];

        // place the cities in a circle
        final double ratio = (2 * Math.PI) / this.cities.length;

        for (int cityNumber = 0; cityNumber < cities.length; cityNumber++) {
            this.cities[cityNumber][0] = (int) (Math.cos(ratio * cityNumber) * (MAP_SIZE / 2) + (MAP_SIZE / 2));
            this.cities[cityNumber][1] = (int) (Math.sin(ratio * cityNumber) * (MAP_SIZE / 2) + (MAP_SIZE / 2));
        }

        // pick a random city order
        this.currentPath = new int[CITY_COUNT];
        for (int i = 0; i < this.currentPath.length; i++) {
            int city;
            boolean foundCity;

            do {
                city = this.rnd.nextInt(CITY_COUNT);
                foundCity = false;
                for (int j = 0; j < i; j++) {
                    if (city == this.currentPath[j]) {
                        foundCity = true;
                    }
                }
            } while (foundCity);

            this.currentPath[i] = city;
        }

        // now begin main loop, and find a minimum
        while (!done()) {
            this.iteration();
            System.out.println("Iteration #" + getK() + ", Best Score=" + this.getBestScore() + "," + getStatus());
        }

        System.out.println(Arrays.toString(this.bestPath));
    }
    /**
     * @return The correct temperature for the current iteration.
     */
    public double coolingSchedule() {
        final double ex = (double) k / (double) kMax;
        return this.startingTemperature * Math.pow(this.endingTemperature / this.startingTemperature, ex);
    }
    /**
     * Perform one training iteration.  This will execute the specified number of cycles at the current
     * temperature.
     */
    public void iteration() {

        // Is this the first time through, if so, then setup.
        if (k == 0) {
            this.currentScore = evaluate();
            foundNewBest();
            this.globalBestScore = this.currentScore;
        }

        // increment the current iteration counter
        k++;

        // obtain the correct temperature
        this.currentTemperature = coolingSchedule();

        // perform the specified number of cycles
        for (int cycle = 0; cycle < this.cycles; cycle++) {
            // backup current state
            backupState();

            // randomize the method
            moveToNeighbor();

            final double trialScore = evaluate();

            // was this iteration an improvement?  If so, always keep.
            boolean keep = false;

            if (trialScore < this.currentScore) {
                // it was better, so always keep it
                keep = true;
            } else {
                // it was worse, so we might keep it
                this.lastProbability = calcProbability(currentScore, trialScore, this.currentTemperature);
                if (this.lastProbability > this.rnd.nextDouble()) {
                    keep = true;
                }
            }

            // should we keep this position?
            if (keep) {
                this.currentScore = trialScore;
                // better than global error
                if (trialScore < this.globalBestScore) {
                    this.globalBestScore = trialScore;
                    foundNewBest();
                }
            } else {
                // do not keep this position
                restoreState();
            }
        }
    }

    /**
     * {@inheritDoc}
     */

    public void backupState() {
        System.arraycopy(this.currentPath, 0, this.backupPath, 0, this.currentPath.length);
    }

    /**
     * {@inheritDoc}
     */

    public void restoreState() {
        System.arraycopy(this.backupPath, 0, this.currentPath, 0, this.currentPath.length);
    }

    /**
     * {@inheritDoc}
     */

    public void foundNewBest() {
        System.arraycopy(this.currentPath, 0, this.bestPath, 0, this.currentPath.length);
    }

    /**
     * @return True, if training has reached the last iteration.
     */
    public boolean done() {
        return k >= kMax;
    }

    /**
     * @return The best score found so far.
     */
    public double getBestScore() {
        return this.globalBestScore;
    }

    /**
     * Calculate the probability that a worse solution will be accepted.  The higher the temperature the more likely
     * this will happen.
     *
     * @param ecurrent The current energy (or score/error).
     * @param enew     The new energy (or score/error).
     * @param t        The current temperature.
     * @return The probability of accepting a worse solution.
     */
    public double calcProbability(final double ecurrent, final double enew, final double t) {
        return Math.exp(-(Math.abs(enew - ecurrent) / t));
    }

    /**
     * @return The current iteration.
     */
    public int getK() {
        return this.k;
    }

    public void moveToNeighbor() {

        // pick the first point to swap
        final int pt1 = this.rnd.nextInt(this.currentPath.length);

        // pick the second point to swap, can't be the same as the first
        int pt2;

        do {
            pt2 = this.rnd.nextInt(this.currentPath.length);
        } while (pt1 == pt2);

        // swap them
        final int temp = this.currentPath[pt1];
        this.currentPath[pt1] = this.currentPath[pt2];
        this.currentPath[pt2] = temp;
    }

    /**
     * {@inheritDoc}
     */
    public double evaluate() {
        double result = 0;
        for (int i = 0; i < (cities.length - 1); i++) {
            // find current and next city
            final double[] city1 = this.cities[this.currentPath[i]];
            final double[] city2 = this.cities[this.currentPath[i + 1]];
            result += this.distance.calculate(city1, city2);
        }

        return result;
    }
    public String getStatus() {
        final StringBuilder result = new StringBuilder();
        result.append("k=");
        result.append(this.k);
        result.append(",kMax=");
        result.append(this.kMax);
        result.append(",t=");
        result.append(this.currentTemperature);
        result.append(",prob=");
        result.append(this.lastProbability);
        return result.toString();
    }
}

