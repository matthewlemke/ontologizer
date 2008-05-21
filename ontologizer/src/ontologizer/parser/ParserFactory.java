package ontologizer.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * This class tries to determine the format and returns a parser object
 * corresponding to one of the above options. Users who desire to add
 * support for new formats should create a class that implements the
 * interface IGeneNameParser. Currently Fasta and a basic list of
 * gene names is supported.
 * 
 * @author Peter Robinson, Sebastian Bauer
 */

public final class ParserFactory
{
	/**
	 * Hide the constructor.
	 */
	private ParserFactory()
	{
	}

	/**
	 * Returns an instance of a gene name parser. The file type is
	 * determined automatically.
	 * 
	 * @param file the input file.
	 * @return an object which can be queried for gene names.
	 * @throws IOException on an error
	 */
	public static IGeneNameParser getNewInstance(final File file) throws IOException
	{
		String type = getFileType(file);
		if (type.equals("fasta"))
		{
			return new FastaParser(file);
		} else
		{
			return new OneOnALineParser(file);
		}
	}

	/**
	 * Tries to determine the file type of the given file.
	 * 
	 * @param file specifies the file whose type should be identified.
	 * 
	 * @return currently eighter "plain" or "fasta"
	 * 
	 * @throws IOException when something fails.
	 */
	private static String getFileType(final File file) throws IOException
	{
		/* default: one gene name on a line */
		String type = "plain"; 
		String inputLine;
		BufferedReader is = new BufferedReader(new FileReader(file));

		int num = 0;
		while ((inputLine = is.readLine()) != null && num < 3)
		{
			if (inputLine.startsWith(">"))
			{
				type = "fasta";
				break;
			}
			num++;
		}
		is.close();
		return type;
	}

}
