package ontologizer.gui.swt.result;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;

import ontologizer.calculation.AbstractGOTermProperties;
import ontologizer.calculation.EnrichedGOTermsResult;
import ontologizer.go.TermID;
import ontologizer.gui.swt.support.GraphPaint;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Display;

import att.grappa.Element;
import att.grappa.Graph;
import att.grappa.GraphEnumeration;
import att.grappa.GrappaConstants;
import att.grappa.GrappaPoint;
import att.grappa.Node;
import att.grappa.Parser;
import att.grappa.Subgraph;

public class EnrichedGOTermsResultLatexWriter
{
	/**
	 * Converts the given double value to a proper latex formated
	 * version.
	 * 
	 * @param val
	 * @return
	 */
	private static String toLatex(double val)
	{
		double e = Math.log(val)/Math.log(10);
		if (Math.abs(e) > 3)
		{
			int exp = (int)Math.floor(e);

			double i = val / Math.pow(10,exp);
			
			return String.format(Locale.US,"%.3f \\times 10^{%d}",i,exp);	
		} else
		{
			return String.format(Locale.US,"%.5f",val);	
		}
	}

	/**
	 * @param terms 
	 * @param htmlFile
	 * @param dotFile 
	 * @param pngFile 
	 */
	public static void write(EnrichedGOTermsResult result, File texFile, Collection<TermID> terms) 
	{
		try
		{
			PrintWriter out = new PrintWriter(texFile);

			out.println("\\documentclass{article}");
			out.println("\\title{Ontologizer GO Term Overrepresentation Analysis}");
			out.println("\\begin{document}");
			out.println("\\maketitle");
			out.println("\\begin{table}[th]");
			out.println("\\begin{center}");
			out.println("\\begin{footnotesize}");
			out.println("\\begin{tabular}{llllll}");
			out.println("\\hline\\\\[-2ex]");
			out.println("ID & Name & p-Value & p-Value (Adj) & Study Count & Population Count \\\\");
			out.println("\\hline\\\\[-2ex]");

			AbstractGOTermProperties [] sortedProps = new AbstractGOTermProperties[result.getSize()];
			int i=0;
			for (AbstractGOTermProperties prop : result)
				sortedProps[i++] = prop;
			Arrays.sort(sortedProps);

			for (AbstractGOTermProperties props : sortedProps)
			{
				if (!terms.contains(props.goTerm.getID()))
					continue;

				out.print (props.goTerm.getIDAsString());
				out.print (" & ");
				
				String name = props.goTerm.getName();
				name = name.replaceAll("_", " ");
				out.print(name);
				
				out.print(" & $");
				out.print(toLatex(props.p));
				out.println("$ & $");
				out.print(toLatex(props.p_adjusted));
				out.println("$ & ");
				out.println(props.annotatedStudyGenes);
				out.println(" & ");
				out.println(props.annotatedPopulationGenes);
				out.println(" \\\\");
		
			}
			out.println("\\hline\\\\[-2ex]");
			
			out.println("\\end{tabular}");
			out.println("\\end{footnotesize}");
			out.println("\\end{center}");
			out.println(String.format("\\caption{" +
					                  "GO overrepresentation analysis using Ontologizer~\\cite{Ontologizer2008} with settings \"%s/%s\". " +
					                  "For this analysis, a total of %d genes were in the population set, of which a total of %d genes were in the study set.}",result.getCalculationName(),result.getCorrectionName(),result.getPopulationGeneCount(),result.getStudyGeneCount()));
			out.println("\\end{table}");
			out.println("\\begin{thebibliography}{1}");
			out.println("\\bibitem{Ontologizer2008} Sebastian Bauer, Steffen Grossmann, Martin Vingron, Peter N. Robinson. Ontologizer 2.0 -- a multifunctional tool for GO term enrichment analysis and data exploration. \\emph{Bioinformatics}, \\textbf{24}(14): 1650--1651.");
			out.println("\\end{thebibliography}");
			out.println("\\end{document}");

			
			
			out.flush();
			out.close();

		} catch (IOException e)
		{
			e.printStackTrace();
		} catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

