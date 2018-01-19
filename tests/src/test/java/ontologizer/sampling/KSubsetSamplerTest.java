package ontologizer.sampling;

import java.util.ArrayList;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Test;

public class KSubsetSamplerTest
{
    @Test
    public void testKSubsetSampler() throws Exception
    {
        HashSet<String> objects = new HashSet<>();
        int nObj = 100;

        for (int i = 0; i < nObj; i++) {
            objects.add(String.valueOf(i));
        }

        KSubsetSampler<String> sampler = new KSubsetSampler<>(objects);

        int k = 3;
        int n = 1000;
        // sampling few
        ArrayList<String> oneSample = sampler.sampleOneOrdered(k);
        Assert.assertEquals(k, oneSample.size());

        // sampling all
        oneSample = sampler.sampleOneOrdered(nObj);
        Assert.assertEquals(nObj, oneSample.size());

        // sampling more
        oneSample = sampler.sampleOneOrdered(2 * nObj);
        Assert.assertEquals(nObj, oneSample.size());

        // sampling none
        oneSample = sampler.sampleOneOrdered(0);
        Assert.assertEquals(0, oneSample.size());

        HashSet<ArrayList<String>> manySamples = sampler.sampleManyOrderedWithoutReplacement(k, n);
        Assert.assertEquals(n, manySamples.size());
        for (ArrayList<String> sample : manySamples) {
            Assert.assertEquals(k, sample.size());
        }
    }
}
