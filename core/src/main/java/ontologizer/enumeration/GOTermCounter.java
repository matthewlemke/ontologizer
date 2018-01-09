package ontologizer.enumeration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ontologizer.go.Namespace;
import ontologizer.go.Ontology;
import ontologizer.go.Ontology.IVisitingGOVertex;
import ontologizer.go.Term;
import ontologizer.go.TermID;

/**
 * This class encapsulates the counting of explicit and implicit annotations for an entire study set. You can iterate
 * conveniently over all GO terms where genes have been annotated to.
 *
 * @author Peter N. Robinson, Sebastian Bauer
 */

public class GOTermCounter implements Iterable<TermID>
{
    private static Logger logger = LoggerFactory.getLogger(GOTermCounter.class.getCanonicalName());

    /**
     * Explicit and total annotations to a given term in the biological process namespace; key: a GO:id, value: an
     * Association Counter object.
     */
    private HashMap<TermID, AssociationCounter> processHashMap;

    /** Molecular function */
    private HashMap<TermID, AssociationCounter> functionHashMap;

    /** Cellular component */
    private HashMap<TermID, AssociationCounter> componentHashMap;

    /**
     * The graph of the ontology
     */
    private Ontology graph;

    public GOTermCounter(Ontology g)
    {
        this.processHashMap = new HashMap<TermID, AssociationCounter>();
        this.functionHashMap = new HashMap<TermID, AssociationCounter>();
        this.componentHashMap = new HashMap<TermID, AssociationCounter>();

        this.graph = g;
    }

    /**
     * @param ids A list of all the GO terms to which <B>an individual gene</B> is annotated directly.
     */
    public void add(ArrayList<TermID> ids)
    {
        Iterator<TermID> it;

        /* First, add direct counts for ids */
        it = ids.iterator();
        while (it.hasNext()) {
            TermID id = it.next();
            addDirect(id);
        }

        /* Second, add the indirect counts for ids */
        final HashSet<TermID> allTerms = new HashSet<TermID>();
        this.graph.walkToSource(ids, new IVisitingGOVertex()
        {
            @Override
            public boolean visited(Term term)
            {
                allTerms.add(term.getID());
                return true;
            }
        });

        /*
         * When we get here, allTerms has a list of all terms that are directly or indirectly annotated to the present
         * gene. Each of these terms now needs to be incremented by "1" and "1" only (i.e., avoid duplicate because of
         * diamond shaped paths or two children of one term being annotated).
         */
        Iterator<TermID> it2 = allTerms.iterator();
        while (it2.hasNext()) {
            TermID id = it2.next();
            addTotal(id);
        }
    }

    /** Add direct annotation */
    private void addDirect(TermID id)
    {
        Term gt = this.graph.getTerm(id);
        AssociationCounter ac = null;
        if (gt == null) {
            System.err.println("Error: GOTermCounter.addDirect"
                + " Could not find " + id);
            System.exit(1);
        }

        switch (Namespace.getNamespaceEnum(gt.getNamespace())) {
            case BIOLOGICAL_PROCESS:
                if (this.processHashMap.containsKey(id)) {
                    ac = this.processHashMap.get(id);
                    ac.incrementDirectCount();
                } else {
                    ac = new AssociationCounter(id);
                    ac.incrementDirectCount();
                    this.processHashMap.put(id, ac);
                }
                break;

            case MOLECULAR_FUNCTION:
                if (this.functionHashMap.containsKey(id)) {
                    ac = this.functionHashMap.get(id);
                    ac.incrementDirectCount();
                } else {
                    ac = new AssociationCounter(id);
                    ac.incrementDirectCount();
                    this.functionHashMap.put(id, ac);
                }
                break;

            case CELLULAR_COMPONENT:
                if (this.componentHashMap.containsKey(id)) {
                    ac = this.componentHashMap.get(id);
                    ac.incrementDirectCount();
                } else {
                    ac = new AssociationCounter(id);
                    ac.incrementDirectCount();
                    this.componentHashMap.put(id, ac);
                }
                break;
        }
    }

    /** Add to total (direct + indirect) annotation */
    private void addTotal(TermID id)
    {
        Term gt = this.graph.getTerm(id);
        AssociationCounter ac = null;
        if (gt == null) {
            System.err.println("Error: GOTermCounter.addDirect"
                + " Could not find " + id);
            System.exit(1);
        }

        switch (Namespace.getNamespaceEnum(gt.getNamespace())) {
            case BIOLOGICAL_PROCESS:
                if (this.processHashMap.containsKey(id)) {
                    ac = this.processHashMap.get(id);
                    ac.incrementCount();
                } else {
                    ac = new AssociationCounter(id);
                    ac.incrementCount();
                    this.processHashMap.put(id, ac);
                }
                break;

            case MOLECULAR_FUNCTION:
                if (this.functionHashMap.containsKey(id)) {
                    ac = this.functionHashMap.get(id);
                    ac.incrementCount();
                } else {
                    ac = new AssociationCounter(id);
                    ac.incrementCount();
                    this.functionHashMap.put(id, ac);
                }
                break;

            case CELLULAR_COMPONENT:
                if (this.componentHashMap.containsKey(id)) {
                    ac = this.componentHashMap.get(id);
                    ac.incrementCount();
                } else {
                    ac = new AssociationCounter(id);
                    ac.incrementCount();
                    this.componentHashMap.put(id, ac);
                }
                break;
        }
    }

    private AssociationCounter find(TermID id)
    {
        AssociationCounter ac = null;
        ac = this.processHashMap.get(id);
        if (ac == null) {
            ac = this.functionHashMap.get(id);
        }
        if (ac == null) {
            ac = this.componentHashMap.get(id);
        }

        return ac;

    }

    /**
     * @return the count of direct annotations for GO term id.
     * @param id a GO:id, e.g., GO:0001234
     */
    public int getDirectCount(TermID id)
    {
        AssociationCounter ac = find(id);
        if (ac == null) {
            System.out.println("Error: Could not find Association counts for "
                + id);
            return 0;

        }
        return ac.getDirectCount();
    }

    /**
     * @param id a GO:id, e.g., GO:0001234
     * @return the count of total annotations for the given GO term id.
     */
    public int getCount(TermID id)
    {
        AssociationCounter ac = find(id);
        if (ac == null) {
            return 0;
        }
        return ac.getCount();
    }

    public int getTotalNumberOfAnnotatedTerms()
    {
        int sum = this.processHashMap.size() + this.componentHashMap.size() + this.functionHashMap.size();
        return sum;

    }

    @Override
    public Iterator<TermID> iterator()
    {
        /*
         * TODO: This list built up is only temporarily because we have currently three separeted ontologies
         */
        LinkedList<TermID> nameList = new LinkedList<TermID>();

        for (TermID term : this.processHashMap.keySet()) {
            nameList.add(term);
        }

        for (TermID term : this.functionHashMap.keySet()) {
            nameList.add(term);
        }

        for (TermID term : this.componentHashMap.keySet()) {
            nameList.add(term);
        }

        return nameList.iterator();
    }
}
