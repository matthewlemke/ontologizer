package ontologizer.calculation.b2g;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import ontologizer.ByteString;
import ontologizer.GOTermEnumerator;
import ontologizer.go.TermID;

/**
 * A basic container representing a set of genes 
 * 
 * @author Sebastian Bauer
 */
class GeneIDs
{
	public int [] gid;
	
	public GeneIDs(int size)
	{
		gid = new int[size];
	}
}

/**
 * The base class of bayes2go Score.
 * 
 * For efficiency reasons terms and genes are represented by own ids.
 * 
 * @author Sebastian Bauer
 */
abstract class Bayes2GOScore
{
	/** Source of randomness */
	protected Random rnd;
	
	protected GOTermEnumerator populationEnumerator;
	protected Set<ByteString> population;
	
	/** Array of terms */
	protected TermID [] termsArray;

	/** Indicates the activation state of a term */
	protected boolean [] isActive;

	/** Contains active terms */
	protected LinkedHashSet<TermID> activeTerms = new LinkedHashSet<TermID>();

	/** Array indicating the genes that have been observed */
	protected boolean [] observedGenes;
	
	/** Array that indicate the activation counts of the genes */
	protected int [] activeHiddenGenes;
	
	/** Maps genes to an unique gene index */
	protected HashMap<ByteString,Integer> gene2GenesIdx = new HashMap<ByteString,Integer>();
	
	protected ByteString [] genes;
	
	/** Maps the term to the index in allTermsArray */
	protected HashMap<TermID,Integer> term2TermsIdx = new HashMap<TermID,Integer>();
	
	/** Maps a term id to the ids of the genes to that the term is annotated */
	protected GeneIDs [] termLinks;

	/** The current number of inactive terms */
	protected int numInactiveTerms;

	protected int numRecords;
	protected int [] termActivationCounts;

	/**
	 * An array representing the inactive terms.
	 * 
	 * Note that only the first elements as given
	 * by the attribute numInactiveTerms are the
	 * inactive terms. 
	 */
	protected TermID[] inactiveTermsArray;

	/**
	 * From a term to an index of the inactiveTermsArray.
	 */
	protected HashMap<TermID,Integer> term2InactiveTermsIdx = new HashMap<TermID,Integer>();

	protected boolean usePrior = true;
	protected double p = Double.NaN;

	public Bayes2GOScore(List<TermID> termList, GOTermEnumerator populationEnumerator, Set<ByteString> observedActiveGenes)
	{
		this(null,termList, populationEnumerator, observedActiveGenes);
	}
	
	public Bayes2GOScore(Random rnd, List<TermID> termList, GOTermEnumerator populationEnumerator, Set<ByteString> observedActiveGenes)
	{
		int i;

		this.rnd = rnd;

		/* Initialize basics of genes */
		population = populationEnumerator.getGenes();
		genes = new ByteString[population.size()];
		observedGenes = new boolean[genes.length];
		i=0;
		for (ByteString g : population)
		{
			gene2GenesIdx.put(g,i);
			genes[i] = g;
			observedGenes[i] = observedActiveGenes.contains(g);
			i++;
		}
		activeHiddenGenes = new int[population.size()];

		/* Initialize basics of terms */
		isActive = new boolean[termList.size()];
		termsArray = new TermID[termList.size()];
		inactiveTermsArray = new TermID[termList.size()];
		numInactiveTerms = termList.size();
		termActivationCounts = new int[termList.size()];
		termLinks = new GeneIDs[termList.size()];

		i=0;
		for (TermID tid : termList)
		{
			term2TermsIdx.put(tid,i);
			termsArray[i]=tid;

			inactiveTermsArray[i] = tid;
			term2InactiveTermsIdx.put(tid, i);

			/* Fill in the links */
			termLinks[i] = new GeneIDs(populationEnumerator.getAnnotatedGenes(tid).totalAnnotated.size());
			int j=0;
			for (ByteString gene : populationEnumerator.getAnnotatedGenes(tid).totalAnnotated)
			{
				termLinks[i].gid[j] = gene2GenesIdx.get(gene);
				j++;
			}

			i++;
		}
		
		this.populationEnumerator = populationEnumerator;

		activeTerms = new LinkedHashSet<TermID>();
	}

	public void setUsePrior(boolean usePrior)
	{
		this.usePrior = usePrior;
	}

	public boolean getUsePrior()
	{
		return usePrior;
	}

	public void setExpectedNumberOfTerms(double terms)
	{
		p = (double)terms / termsArray.length;
	}

	public double score(Collection<TermID> activeTerms)
	{
		ArrayList<TermID> oldTerms = new ArrayList<TermID>(this.activeTerms);

		/* Deactivate old terms */
		for (TermID tid : oldTerms)
			switchState(term2TermsIdx.get(tid));

		/* Enable new terms */
		for (TermID tid : activeTerms)
		{
			Integer idx = term2TermsIdx.get(tid);
			if (idx != null)
				switchState(idx);
		}
		
		double score = getScore();
		
		/* Disable new terms */
		for (TermID tid : activeTerms)
		{
			Integer idx = term2TermsIdx.get(tid);
			if (idx != null)
				switchState(idx);
		}

		/* Enable old terms */
		for (TermID tid : oldTerms)
			switchState(term2TermsIdx.get(tid));

		return score;
	}

	/**
	 * Returns the score of the current state.
	 * 
	 * @return
	 */
	public abstract double getScore();
	
	public abstract void proposeNewState(long rand);
	public void proposeNewState()
	{
		proposeNewState(rnd.nextLong());
	}

	public abstract void hiddenGeneActivated(int gid);
	public abstract void hiddenGeneDeactivated(int gid);
	
//	public long currentTime;
	
	public void switchState(int toSwitch)
	{
//		long enterTime = System.nanoTime();

		TermID t = termsArray[toSwitch];
		int [] geneIDs = termLinks[toSwitch].gid;

		isActive[toSwitch] = !isActive[toSwitch];
		if (isActive[toSwitch])
		{
			/* A term is added */
			activeTerms.add(t);

			for (int gid : geneIDs)
			{
				if (activeHiddenGenes[gid] == 0)
				{
					activeHiddenGenes[gid] = 1;
					hiddenGeneActivated(gid);
				} else
				{
					activeHiddenGenes[gid]++;
				}
			}

			int inactiveIndex = term2InactiveTermsIdx.get(t);

			if (inactiveIndex != (numInactiveTerms - 1))
			{
				inactiveTermsArray[inactiveIndex] = inactiveTermsArray[numInactiveTerms - 1];
				term2InactiveTermsIdx.put(inactiveTermsArray[inactiveIndex], inactiveIndex);
			}
			
			term2InactiveTermsIdx.remove(t);
			numInactiveTerms--;
		} else
		{
			/* Remove a term */
			activeTerms.remove(t);

			/* Update hiddenActiveGenes */
			for (int gid : geneIDs)
			{
				if (activeHiddenGenes[gid] == 1)
				{
					activeHiddenGenes[gid] = 0;
					hiddenGeneDeactivated(gid);
				} else
				{
					activeHiddenGenes[gid]--;
				}
			}

			/* Append the new term at the end of the index list */
			inactiveTermsArray[numInactiveTerms] = t;
			term2InactiveTermsIdx.put(t, numInactiveTerms);
			numInactiveTerms++;
		}
		
//		{
//			long ds = currentTime / 100000000;
//			currentTime += System.nanoTime() - enterTime;
//			if (currentTime / 100000000 != ds)
//				System.out.println(currentTime / 1000000);
//		}
	}

	public void exchange(TermID t1, TermID t2)
	{
		switchState(term2TermsIdx.get(t1));
		switchState(term2TermsIdx.get(t2));
	}


	public abstract void undoProposal();
	
	public abstract long getNeighborhoodSize();

	/**
	 * Records the current settings.
	 */
	public void record()
	{
		for (TermID tid : activeTerms)
			termActivationCounts[term2TermsIdx.get(tid)]++;

		numRecords++;
	}
}
