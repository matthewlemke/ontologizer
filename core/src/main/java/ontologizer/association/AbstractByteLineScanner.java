package ontologizer.association;

import java.io.IOException;
import java.io.InputStream;

/**
 * This is a simple class that can be used to read an input stream in byte representation in a line-based manner.
 *
 * @author Sebastian Bauer
 */
abstract public class AbstractByteLineScanner
{
    private InputStream is;

    private final int BUF_SIZE = 65536;

    private int available;

    private int availableStart;

    byte[] byteBuf = new byte[2 * BUF_SIZE];

    public AbstractByteLineScanner(InputStream is)
    {
        this.is = is;
    }

    public void scan() throws IOException
    {
        int read;
        int read_offset = 0;

        outer: while ((read = is.read(byteBuf, read_offset, BUF_SIZE) + read_offset) > read_offset)
        {
            int line_start = 0;
            int pos = 0;

            while (pos < read)
            {
                if (byteBuf[pos] == '\n')
                {
                    if (!newLine(byteBuf, line_start, pos - line_start))
                    {
                        availableStart = pos + 1;
                        available = read - availableStart;
                        break outer;
                    }
                    line_start = pos + 1;
                }
                pos++;
            }

            System.arraycopy(byteBuf, line_start, byteBuf, 0, read - line_start);
            read_offset = read - line_start;
        }
        if (read_offset != 0) {
            newLine(byteBuf, 0, read_offset);
        }
    }

    /**
     * Returns the number of bytes that are still available in the buffer after the reading has been aborted.
     *
     * @return
     */
    public int available()
    {
        return available;
    }

    /**
     * Returns the bytes that are still available in the buffer after the reading has been aborted.
     *
     * @return
     */
    public byte[] availableBuffer()
    {
        byte[] b = new byte[available];
        System.arraycopy(byteBuf, availableStart, b, 0, available);
        return b;
    }

    /**
     * Called whenever a new line was encountered.
     *
     * @param buf
     * @param start
     * @param len
     * @return false for aborting the reading
     */
    abstract public boolean newLine(byte[] buf, int start, int len);
}
