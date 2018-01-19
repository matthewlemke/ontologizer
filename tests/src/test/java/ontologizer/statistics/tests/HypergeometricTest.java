package ontologizer.statistics.tests;

import org.junit.Assert;
import org.junit.Test;

import ontologizer.statistics.Hypergeometric;

public class HypergeometricTest
{
    private Hypergeometric hyper = new Hypergeometric();

    /*
     * Test method for 'ontologizer.Hypergeometric.dhyper(int, int, int, int)'
     */
    @Test
    public void testDhyper()
    {
        Assert.assertEquals(0.268, this.hyper.dhyper(4, 45, 20, 10), 0.001);
        Assert.assertEquals(1, this.hyper.dhyper(10, 10, 10, 10), 0.00001);
    }

    @Test
    public void testPhyper()
    {
        double result = this.hyper.phyper(2, 1526, 4, 190, false);

        Assert.assertTrue(result > 0.0069 && result < 0.0070);
        Assert.assertTrue(
            (this.hyper.phyper(22, 1526, 40, 190, false) + this.hyper.phyper(22, 1526, 40, 190, true)) == 1);
        Assert
            .assertTrue((this.hyper.phyper(3, 1526, 40, 190, false) + this.hyper.phyper(3, 1526, 40, 190, true)) == 1);

        /*
         * checking unreasonable numbers
         */
        // drawing more white than available
        Assert.assertTrue(this.hyper.phyper(4, 8, 3, 6, false) == 0);
        // drawing more white than drawn in total
        Assert.assertTrue(this.hyper.phyper(4, 8, 5, 3, false) == 0);
        // drawing more white than available in total
        Assert.assertTrue(this.hyper.phyper(10, 8, 5, 12, false) == 0);
    }
}
