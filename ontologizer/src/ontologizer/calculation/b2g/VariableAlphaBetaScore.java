package ontologizer.calculation.b2g;

import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;

import ontologizer.ByteString;
import ontologizer.GOTermEnumerator;
import ontologizer.go.TermID;

/**
 * Score of a setting in which alpha and beta are known.
 *  
 * @author Sebastian Bauer
 */
class VariableAlphaBetaScore extends Bayes2GOScore
{
	private HashMap<ByteString, Double> llr = new HashMap<ByteString,Double>();
	private double alpha;
	private double beta;
	
	private double score;

	public VariableAlphaBetaScore(Random rnd, List<TermID> termList, GOTermEnumerator populationEnumerator, Set<ByteString> observedActiveGenes, double alpha, double beta)
	{
		super(rnd, termList, populationEnumerator, observedActiveGenes);
		
		this.alpha = alpha;
		this.beta = beta;
		
		calcLLR();
	}

	public void calcLLR()
	{
		for (ByteString g : population)
		{
			if (observedActiveGenes.contains(g))
				llr.put(g, Math.log(1-beta) - Math.log(alpha)); // P(oi=1|h=1) / P(oi=1|h=0)
			else
				llr.put(g, Math.log(beta) - Math.log(1-alpha)); // P(oi=0|h=1) / P(oi=0|h=0)
		}

	}

	private int proposalSwitch;
	private TermID proposalT1;
	private TermID proposalT2;

	public void hiddenGeneActivated(ByteString gene)
	{
		score += llr.get(gene);
	}
	
	public void hiddenGeneDeactivated(ByteString gene)
	{
		score -= llr.get(gene);
	}
	
	@Override
	public void proposeNewState(long rand)
	{
		long oldPossibilities = getNeighborhoodSize();

		proposalSwitch = -1;
		proposalT1 = null;
		proposalT2 = null;

		long choose = Math.abs(rand) % oldPossibilities;

		if (choose < termsArray.length)
		{
			/* on/off */
			proposalSwitch = (int)choose;
			switchState(proposalSwitch);
		}	else
		{
			long base = choose - termsArray.length;
			
			int activeTermPos = (int)(base / numInactiveTerms);
			int inactiveTermPos = (int)(base % numInactiveTerms);
			
			for (TermID tid : activeTerms)
				if (activeTermPos-- == 0) proposalT1 = tid;
			proposalT2 = inactiveTermsArray[inactiveTermPos];

			exchange(proposalT1, proposalT2);
		}
	}
	
	@Override
	public double getScore()
	{
		return score + activeTerms.size() * Math.log(p/(1.0-p));
	}
	
	@Override
	public void undoProposal()
	{
		if (proposalSwitch != -1)	switchState(proposalSwitch);
		else exchange(proposalT2, proposalT1);
	}

	public long getNeighborhoodSize()
	{
		return termsArray.length + activeTerms.size() * numInactiveTerms;
	}

}

