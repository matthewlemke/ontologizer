package ontologizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import ontologizer.set.StudySet;

/**
 * @author Sebastian Bauer
 * @param <Result>
 */
abstract public class AbstractStudySetResult<Result> implements Iterable<Result>
{
    protected List<Result> list = new ArrayList<Result>();

    private HashMap<String, Integer> goID2Index = new HashMap<String, Integer>();

    private int index = 0;

    @SuppressWarnings("unused")
    private int populationGeneCount;

    private StudySet studySet;

    /**
     * @param studySet the study set where this result should belong to.
     * @param populationGeneCount number of genes of the populations (FIXME: This infact is redundant)
     */
    public AbstractStudySetResult(StudySet studySet, int populationGeneCount)
    {
        this.studySet = studySet;
        this.populationGeneCount = populationGeneCount;
    }

    /**
     * Add new Term properties
     *
     * @param goTermID
     * @param prop
     */
    public void addGOTermProperties(String goTermID, Result prop)
    {
        this.list.add(prop);
        Integer integer = new Integer(this.index);
        this.goID2Index.put(goTermID, integer);
        this.index++;
    }

    /**
     * Return the studyset for these results.
     *
     * @return
     */
    public StudySet getStudySet()
    {
        return this.studySet;
    }

    /**
     * Return the property of the given TermID
     *
     * @param goID
     * @return
     */
    public Result getGOTermProperties(String goID)
    {
        Integer index = this.goID2Index.get(goID);
        if (index == null) {
            return null;
        }
        return this.list.get(index);
    }

    @Override
    public Iterator<Result> iterator()
    {
        return this.list.iterator();
    }
}
