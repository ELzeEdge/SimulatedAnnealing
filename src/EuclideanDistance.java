
public class EuclideanDistance{

    public double calculate(final double[] position1, final double[] position2) {
        double sum = 0;
        for (int i = 0; i < position1.length; i++) {
            final double d = position1[i] - position2[i];
            sum += d * d;
        }
        return Math.sqrt(sum);
    }
}
