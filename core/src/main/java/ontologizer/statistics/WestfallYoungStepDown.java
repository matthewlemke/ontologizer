package ontologizer.statistics;

import java.util.Arrays;

public class WestfallYoungStepDown extends AbstractTestCorrection
    implements IResampling
{
    /** Specifies the number of resampling steps */
    private int numberOfResamplingSteps = 1000;

    @Override
    public String getDescription()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getName()
    {
        return "Westfall-Young-Step-Down";
    }

    /**
     * @author Sebastian Bauer Class models a double value entry and its index of a source array.
     */
    private class Entry implements Comparable<Entry>
    {
        public double value;

        public int index;

        @Override
        public int compareTo(Entry o)
        {
            if (this.value < o.value) {
                return -1;
            }
            if (this.value == o.value) {
                return 0;
            }
            return 1;
        }
    };

    @Override
    public PValue[] adjustPValues(IPValueCalculation pvalues)
    {
        int i;

        /* Calculate raw P-values */
        PValue[] rawP = pvalues.calculateRawPValues();

        double[] q = new double[rawP.length];
        int[] count = new int[rawP.length];

        /* Sort the raw P-values and remember their original index */
        int m = rawP.length;
        int r[] = new int[m];
        Entry[] sortedRawPValues = new Entry[m];

        for (i = 0; i < m; i++) {
            sortedRawPValues[i] = new Entry();
            sortedRawPValues[i].value = rawP[i].p;
            sortedRawPValues[i].index = i;
        }
        Arrays.sort(sortedRawPValues);

        /* build up r, this info is redundant but using r is more convenient */
        for (i = 0; i < m; i++) {
            r[i] = sortedRawPValues[i].index;
        }

        /* Now "permute" */
        System.out.println("Sampling " + this.numberOfResamplingSteps + " random study sets\nThis may take a while...");
        for (int b = 0; b < this.numberOfResamplingSteps; b++) {
            /* Compute raw p values of "permuted" data */
            PValue[] randomRawP = pvalues.calculateRandomPValues();

            assert(randomRawP.length == rawP.length);

            /* Compute the successive minima of raw p values */
            q[m - 1] = randomRawP[r[m - 1]].p;
            for (i = m - 2; i >= 0; i--) {
                q[i] = Math.min(q[i + 1], randomRawP[r[i]].p);
            }

            /* Count up */
            for (i = 0; i < m; i++) {
                if (q[i] <= rawP[r[i]].p) {
                    count[i]++;
                }
            }

            System.out.print(b + "\r");
        }
        System.out.println("Done!");

        /* Enforce monotony contraints */
        int c = count[0];
        for (i = 1; i < m; i++) {
            c = count[i] = Math.max(1, Math.max(c, count[i]));
        }

        /* Calculate the adjusted p values */
        for (i = 0; i < m; i++) {
            rawP[r[i]].p_adjusted = ((double) count[i]) / this.numberOfResamplingSteps;
        }
        return rawP;
    }

    @Override
    public void setNumberOfResamplingSteps(int n)
    {
        this.numberOfResamplingSteps = n;
    }

    @Override
    public int getNumberOfResamplingSteps()
    {
        return this.numberOfResamplingSteps;
    }

    @Override
    public void resetCache()
    {
        // no cache here, nothing to do

    }

    @Override
    public int getSizeTolerance()
    {
        return 0;
    }

    @Override
    public void setSizeTolerance(int t)
    {

    }
}
