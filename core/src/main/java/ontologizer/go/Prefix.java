package ontologizer.go;

import ontologizer.types.ByteString;

/**
 * This class implements a ontology prefix which is used before the colon in the specification of the term. I.e., for
 * gene ontology this would be "GO".
 *
 * @author Sebastian Bauer
 */
public class Prefix
{
    /** The prefix as byte string */
    private ByteString prefix;

    /**
     * Constructs a new prefix from a string.
     *
     * @param newPrefix
     */
    public Prefix(String newPrefix)
    {
        this.prefix = new ByteString(newPrefix);
    }

    /**
     * Constructs a new prefix from a string which can be limited in length.
     *
     * @param newPrefix
     * @param length
     */
    public Prefix(String newPrefix, int length)
    {
        this.prefix = new ByteString(newPrefix, length);
    }

    /**
     * Constructs a new prefix from a byte string.
     *
     * @param newPrefix
     */
    public Prefix(ByteString newPrefix)
    {
        this.prefix = newPrefix;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof Prefix) {
            return equals((Prefix) obj);
        }
        if (obj instanceof ByteString) {
            return equals((ByteString) obj);
        }

        return super.equals(obj);
    }

    public boolean equals(ByteString obj)
    {
        return this.prefix.equals(obj);
    }

    public boolean equals(Prefix obj)
    {
        return this.prefix.equals(obj.prefix);
    }

    @Override
    public int hashCode()
    {
        return this.prefix.hashCode();
    }

    @Override
    public String toString()
    {
        return this.prefix.toString();
    }
}
