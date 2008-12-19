package ontologizer.go;

import java.io.*;
import java.util.*; /* HashMap */
import java.util.logging.Logger;

import ontologizer.myException;

/*
 * I gratefully acknowledge the help of John Richter Day, who provided the
 * source of DAGEdit on which I based this parser for the Ontologizer and also
 * sent several useful suggestions by email. Much of the code in this class was
 * adapated verbatim from several classes in DAGEdit.
 * 
 * 
 * Of course, any errors in the present program are my own.
 */

/**
 * OBOParser parses the Gene Ontology OBO term definition file. Please see
 * www.geneontology.org for background on this file format.
 * 
 * @author Peter N. Robinson, Sebastian Bauer
 */
public class OBOParser
{
	private static Logger logger = Logger.getLogger(OBOParser.class.getCanonicalName());

	private enum Stanza
	{
		TERM,
		TYPEDEF
	}
	
	public final static int PARSE_DEFINITIONS = 1 << 0;

	/**
	 * Escaped characters such as \\ in the gene_ontology.obo file.
	 */
	private static final HashMap<Character, Character> escapeChars = new HashMap<Character, Character>();

	/**
	 * Reverse direction
	 */
	private static final HashMap<Character, Character> unescapeChars = new HashMap<Character, Character>();

	static
	{
		escapeChars.put(new Character(':'), new Character(':'));
		escapeChars.put(new Character('W'), new Character(' '));
		escapeChars.put(new Character('t'), new Character('\t'));
		escapeChars.put(new Character(','), new Character(','));
		escapeChars.put(new Character('"'), new Character('"'));
		escapeChars.put(new Character('n'), new Character('\n'));
		escapeChars.put(new Character('\\'), new Character('\\'));
		escapeChars.put(new Character('{'), new Character('{'));
		escapeChars.put(new Character('}'), new Character('}'));
		escapeChars.put(new Character('['), new Character('['));
		escapeChars.put(new Character(']'), new Character(']'));
		escapeChars.put(new Character('!'), new Character('!'));

		Iterator<Character> it = escapeChars.keySet().iterator();
		while (it.hasNext())
		{
			Character key = it.next();
			Character value = escapeChars.get(key);
			unescapeChars.put(value, key);
		}
	}

	/** Name and path of OBO file, e.g. gene_ontology.obo */
	private String filename;

	/** The current parse options */
	private int options;

	/** Format version of the gene_ontology.obo file */
	private String format_version;

	/** Date of the gene_ontology.obo file */
	private String date;

	/** Collection of all terms */
	private HashSet<Term> terms = new HashSet<Term>();

	/** Collection of subsets */
	private HashMap<String,Integer> subsets = new HashMap<String, Integer>();
	
	/** This is the id of the next subset */
	private int nextSubsetId;

	/** Statistics */
	private int numberOfRelations;
	
	/** Contains all the ontology prefixes */
	private HashSet <Prefix> prefixes = new HashSet<Prefix>();

	/* Used for parsing */
	private String line;
	private int linenum = 0;
	private int bytesRead = 0;

	/** The Stanza currently being processed */
	private Stanza currentStanza;

	/** The id of the current Term in the stanza currently being parsed */
	private String currentID;

	/** The name of the GO Term currently being parsed */
	private String currentName;

	/** The namespace of the stanza currently being parsed */
	private String currentNamespace;

	/** The definition of the stanza currently being parsed */
	private String currentDefintion;

	/** Is current term obsolete? */
	private boolean currentObsolete;

	/** The parents of the term of the stanza currently being parsed */
	private ArrayList<ParentTermID> currentParents = new ArrayList<ParentTermID>();

	/** The alternative ids of the term */
	private ArrayList<TermID> currentAlternatives = new ArrayList<TermID>();

	/**
	 * @param filename
	 *            path and name of the gene_ontology.obo file
	 */
	public OBOParser(String filename)
	{
		this.filename = filename;
	}

	/**
	 * Options can be combined via logical or. Valid options are:
	 * <ul>
	 * <li>PARSE_DEFINITIONS - to gather the definition entry.
	 * </ul>
	 * 
	 * @param filename
	 *            defines the path and name of the gene_ontology.obo file
	 * @param options
	 *            defines some options.
	 */
	public OBOParser(String filename, int options)
	{
		this.filename = filename;
		this.options = options;
	}

	public Set<Term> getTermMap()
	{
		return this.terms;
	}

	/**
	 * This puts the results of the parse of a single OBO stanza into one Term
	 * object and stores that in the HashSet terms.
	 */
	private void enterNewTerm()
	{
		if (currentStanza != null)
		{
			if (currentStanza == Stanza.TYPEDEF)
				return;

			if (currentID == null || currentName == null
					|| currentNamespace == null) {
				
				logger.severe("Error parsing stanza: " + currentStanza.toString());
				throw new IllegalArgumentException("Missing ID, Name or Namespace of for current stanza!");

			}
			/* Create a Term object and put it in the HashMap terms. */
			Term t = new Term(currentID, currentName, currentNamespace, currentParents);
			t.setObsolete(currentObsolete);
			t.setDefinition(currentDefintion);
			t.setAlternatives(currentAlternatives);
			terms.add(t);
			numberOfRelations += currentParents.size();
		}

		/* Now reset... */
		currentID = null;
		currentName = null;
		currentNamespace = null;
		currentDefintion = null;
		currentObsolete = false;
		currentParents = new ArrayList<ParentTermID>();
		currentAlternatives.clear();
	}

	
	/**
	 * The main parsing routine for the gene_ontology.obo file
	 * 
	 * @return A string giving details about the parsed obo file
	 * @throws myException 
	 * @throws IOException 
	 */
	public String doParse() throws IOException, myException
	{
		return doParse(null);
	}

	/**
	 * The main parsing routine for the gene_ontology.obo file
	 *
	 * @param progress
	 * @return A string giving details about the parsed obo file
	 * @throws myException 
	 * @throws IOException 
	 */
	public String doParse(IOBOParserProgress progress) throws IOException, myException
	{
//		try
		{
			int currentTerm = 0;
			long millis = 0;

			File file = new File(filename);
			BufferedReader reader = new BufferedReader(new FileReader(file));

			if (progress != null)
				progress.init((int)file.length());

			for (linenum = 1; (line = reader.readLine()) != null; linenum++)
			{
				/* Progress support, call only every quarter second */
				if (progress != null)
				{
					long newMillis = System.currentTimeMillis();
					if (newMillis - millis > 250)
					{
						progress.update(bytesRead,currentTerm);
						millis = newMillis;
					}
				}

				bytesRead += line.length();
				
				line = stripSpecialCharacters(line);
				if (line.length() == 0)
					continue;
				/*
				 * The following takes care of multiline entries (lines
				 * terminated with "\")
				 */
				while (line.charAt(line.length() - 1) == '\\'
						&& line.charAt(line.length() - 2) != '\\')
				{
					String str = reader.readLine();
					linenum++;
					if (str == null)
						throw new myException("Unexpected end of file", line, linenum);
					line = line.substring(0, line.length() - 1) + str;
				}
				// When we get here we have one complete tag : value pair
				if (line.charAt(0) == '!')
					continue; /* skip "!" comments */
				// If the line starts with "[", we are at a new [Term] or
				// [Typedef]
				if (line.charAt(0) == '[')
				{
					// If we get here, all info for a term from the previous
					// stanza should be ready to be entered.
					
					enterNewTerm();
					currentTerm++;
					if (line.charAt(line.length() - 1) != ']')
						throw new myException("Unclosed stanza \"" + line
								+ "\"", line, linenum);

					String stanzaname = line.substring(1, line.length() - 1);
					if (stanzaname.length() < 1)
						throw new myException("Empty stanza", line, linenum);
					
					if (stanzaname.equalsIgnoreCase("term"))
						currentStanza = Stanza.TERM;
					else if (stanzaname.equalsIgnoreCase("typedef"))
						currentStanza = Stanza.TYPEDEF;
					else throw new IllegalArgumentException("Unknown stanza type: \""+stanzaname+"\" at line " + linenum);
				} else
				{
					try
					{
						SOPair pair;
						try
						{
							pair = unescape(line, ':', 0, true);
						} catch (myException ex)
						{
							System.err.println("ERROR FIX ME");
							break;
						}
	
						String name = pair.str;
						int lineEnd = findUnescaped(line, '!', 0, line.length());
						if (lineEnd == -1)
							lineEnd = line.length();
						int trailingStartIndex = -1;
						for (int i = lineEnd - 1; i >= 0; i--)
						{
							if (Character.isWhitespace(line.charAt(i)))
								continue;
							else
								break;
						}
						int stopIndex = trailingStartIndex;
						if (stopIndex == -1)
							stopIndex = lineEnd;
						String value = line.substring(pair.index + 1, stopIndex);
						if (value.length() == 0)
							throw new myException("Tag found with no value", line, linenum);
	
						if (currentStanza == null)
							readHeaderValue(name, value);
						else
							readTagValue(name, value);
					} catch (IllegalArgumentException iae)
					{
						logger.severe("Unable to parse line at " + linenum + " " + line);
						throw iae;
					}
				}
			} // for
			enterNewTerm(); // Get very last stanza after loop!
			reader.close();
			if (progress != null)
				progress.update((int)file.length(),currentTerm);

			
			logger.info("Got " + terms.size() + " terms and " + numberOfRelations + " relations");
		}/* catch (FileNotFoundException ex)
		{
			System.err.println("Could not find file " + filename);
			System.err.println(ex.getStackTrace());
		} catch (IOException ex)
		{
			System.err.println("IOException:");
			System.err.println(ex.getStackTrace());
		} catch (myException ontex)
		{
			System.err.println("Parse Exception: " + ontex.toString());
			System.err.println("This should never happen with a well-formed"
					+ " gene_ontology.obo file. Please check you "
					+ " are using the correct file.");
		}*/
		
		return this.getParseDiagnostics();
	}

	/** Remove non-Latin characters */
	public static String stripSpecialCharacters(final String s)
	{
		StringBuilder out = null;
		int length = s.length();
		int i;

		for (i = 0; i < length; i++)
		{
			char c = s.charAt(i);
			if (c >= 128)
			{
				out = new StringBuilder(i + 32);
				if (i!=0)
					out.append(s.substring(0, i)); /* omits the current character */
				break;
			}
		}
		
		/* No buffer allocated? So there are no non-latin characters inside s */
		if (out == null) return s;

		for (;i < length; i++)
		{
			char c = s.charAt(i);
			if (c < 128)
				out.append(c);
		}
		return out.toString();
	}

	/** This static class stores a pair or <String,int> values */
	public static class SOPair
	{
		public String str = null;

		public int index = -1;

		public SOPair(String str, int index)
		{
			this.str = str;
			this.index = index;
		}
	}

	/**
	 * @param name
	 *            The tag of a Stanza in the header of the OBO file
	 * @param value
	 *            The value of the stanza This function is used to record the
	 *            version and date of the gene_ontology.obo file.
	 */

	private void readHeaderValue(String name, String value) throws myException
	{
		value = value.trim();
		if (name.equals("format-version"))
		{
			this.format_version = value;
			return;
		} else if (name.equals("date"))
		{
			this.date = value;
		} else if (name.equals("subsetdef"))
		{
			if (!subsets.containsKey(name))
			{
				subsets.put(new String(name.toCharArray()), nextSubsetId);
				nextSubsetId++;
			}
		}
	}

	protected void readTagValue(String name, String value) throws myException,
			IOException
	{
		value = value.trim();

		if (name.equals("import"))
		{
			if (currentStanza != null)
			{
				throw new myException("import tags may only occur "
						+ "in the header", line, linenum);
			}
			return;
		} else if (name.equals("id"))
		{
			readID(value);
		} else if (name.equals("name"))
		{
			readName(unescape(value));
		} else if (name.equals("is_a"))
		{
			readISA(unescape(value));
		} else if (name.equals("relationship"))
		{
			int typeIndex = findUnescaped(value, ' ', 0, value.length());
			String type = value.substring(0, typeIndex).trim();
			if (typeIndex == -1)
				throw new myException("No id specified for" + " relationship.", line, linenum);
			int endoffset = findUnescaped(value, '[',
					typeIndex + type.length(), value.length());
			String id;
			if (endoffset == -1)
				id = value.substring(typeIndex + 1, value.length()).trim();
			else
			{
				id = value.substring(typeIndex + 1, endoffset).trim();
			}

			if (id.length() == 0)
				throw new myException("Empty id specified for"
						+ " relationship.", line, linenum);
			readRelationship(type,id);
		} else if (name.equals("is_obsolete"))
		{
			currentObsolete = value.equalsIgnoreCase("true");
		} else if (name.equals("namespace"))
		{
			readNamespace(value);
		} else if (name.equals("alt_id"))
		{
			readAlternative(value);
		}
		else if ((options & PARSE_DEFINITIONS) != 0)
		{
			if (name.equals("def"))
			{
				if (value.startsWith("\""))
					currentDefintion = unescape(value, '\"', 1, value.length(),
							false).str;
			}
		}
		/*
			 * else if (name.equals("comment")) { return; } else if
			 * (name.equals("domain")) { return; } else if
			 * (name.equals("range")) { return; } else if
			 * (name.equals("xref_analog")) { return; } else if
			 * (name.equals("xref_unk")) { return; } else if
			 * (name.equals("subset")) { return; } else if
			 * (name.equals("synonym")) { return; } else if
			 * (name.equals("related_synonym")) { return; } else if
			 * (name.equals("exact_synonym")) { return; } else if
			 * (name.equals("narrow_synonym")) { return;
			 *  } else if (name.equals("broad_synonym")) { return; } else if
			 * (name.equals("relationship")) { return; }
			 */
	}

	private void readAlternative(String value)
	{
		try
		{
			currentAlternatives.add(new TermID(value));
		} catch (IllegalArgumentException e)
		{
			logger.warning("Unable to parse alternative ID: \""+value+"\"");
		}

	}

	private static String unescape(String str) throws myException
	{
		return unescape(str, '\0', 0, str.length(), false).str;
	}

	private static SOPair unescape(String str, char toChar, int startindex,
			boolean mustFindChar) throws myException
	{
		return unescape(str, toChar, startindex, str.length(), mustFindChar);
	}

	private static SOPair unescape(String str, char toChar, int startindex,
			int endindex, boolean mustFindChar) throws myException
	{
		StringBuilder out = new StringBuilder();
		int endValue = -1;
		for (int i = startindex; i < endindex; i++)
		{
			char c = str.charAt(i);
			if (c == '\\')
			{
				i++;
				c = str.charAt(i);
				Character mapchar = (Character) escapeChars.get(new Character(c));
				if (mapchar == null)
					throw new myException("Unrecognized escape" + " character "
							+ c + " found.", null, -1);
				out.append(mapchar);
			} else if (c == toChar)
			{
				endValue = i;
				break;
			} else
			{
				out.append(c);
			}
		}
		if (endValue == -1 && mustFindChar)
		{
			throw new myException("Expected " + toChar + ".", str, -1);
		}
		return new SOPair(out.toString(), endValue);
	}

	@SuppressWarnings("unused")
	private static int findUnescaped(String str, char toChar)
	{
		return findUnescaped(str, toChar, 0, str.length());
	}

	private static int findUnescaped(String str, char toChar, int startindex,
			int endindex)
	{
		for (int i = startindex; i < endindex; i++)
		{
			char c = str.charAt(i);
			if (c == '\\')
			{
				i++;
				continue;
			} else if (c == toChar)
			{
				return i;
			}
		}
		return -1;
	}

	public void readID(String value)
	{
		currentID = value;
	}

	public void readName(String value)
	{
		currentName = value;
	}

	public void readISA(String value)
	{
		if (currentStanza == Stanza.TERM)
			currentParents.add(new ParentTermID(new TermID(value),TermRelation.IS_A));
	}

	private void readRelationship(String type, String id)
	{
		if (currentStanza == Stanza.TERM)
		{
			TermRelation tr = TermRelation.UNKOWN;
	
			if (type.equals("part_of"))
				tr = TermRelation.PART_OF_A;
			else if (type.equals("regulates"))
				tr = TermRelation.REGULATES;
			else if (type.equals("negatively_regulates"))
				tr = TermRelation.NEGATIVELY_REGULATES;
			else if (type.equals("positively_regulates"))
				tr = TermRelation.POSITIVELY_REGULATES;

			currentParents.add(new ParentTermID(new TermID(id),tr));
		}
	}

	private void readNamespace(String value)
	{
		if (value.equalsIgnoreCase("cellular_component"))
		{
			currentNamespace = "C";
		} else if (value.equalsIgnoreCase("molecular_function"))
		{
			currentNamespace = "F";
		} else if (value.equalsIgnoreCase("biological_process"))
		{
			currentNamespace = "B";
		} else
		{
			throw new IllegalArgumentException("Encountered an unknown namespace: "
					+ value);
		}
	}

	public String getFormatVersion()
	{
		return format_version;
	}

	public String getDate()
	{
		return date;
	}

	/**
	 * Gives some diagnostics about the parsed obo file
	 * 
	 * @return A String telling you something about the parsed obo file
	 */
	private String getParseDiagnostics()
	{
		StringBuilder diag = new StringBuilder();

		diag.append("Details of parsed obo file:\n");
		diag.append("  filename:\t\t" + this.filename + "\n");
		diag.append("  date:\t\t\t" + this.date + "\n");
		diag.append("  format:\t\t" + this.format_version + "\n");
		diag.append("  term definitions:\t" + this.terms.size());
		
		return diag.toString();
	}
}