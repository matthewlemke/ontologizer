package ontologizer.association;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AbstractByteLineScannerTest
{
    @Test
    public void testBigFile() throws FileNotFoundException, IOException
    {
        try (InputStream is = new GZIPInputStream(new FileInputStream("data/gene_ontology.1_2.obo.gz"));
            BufferedReader br = new BufferedReader(
                new InputStreamReader(new GZIPInputStream(new FileInputStream("data/gene_ontology.1_2.obo.gz"))))) {

            class TestByteLineScanner extends AbstractByteLineScanner
            {
                public int actualLineCount;

                public int expectedLineCount;

                public TestByteLineScanner(InputStream is)
                {
                    super(is);
                }

                @Override
                public boolean newLine(byte[] buf, int start, int len)
                {
                    this.actualLineCount++;

                    StringBuilder actualString = new StringBuilder();
                    for (int i = start; i < start + len; i++) {
                        actualString.append((char) buf[i]);
                    }
                    String expectedString;
                    try {
                        expectedString = br.readLine();
                        this.expectedLineCount++;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    Assert.assertEquals(expectedString, actualString.toString());
                    return true;
                }
            }

            TestByteLineScanner tbls = new TestByteLineScanner(is);
            tbls.scan();

            assertEquals(tbls.expectedLineCount, tbls.actualLineCount);
            assertNull(br.readLine());
        }
    }

    @Test
    public void testMissingNewLineAtLineEnd() throws IOException
    {
        ByteArrayInputStream bais = new ByteArrayInputStream("test\ntest2".getBytes());
        class TestByteLineScanner extends AbstractByteLineScanner
        {
            public int lines;

            public TestByteLineScanner(InputStream is)
            {
                super(is);
            }

            @Override
            public boolean newLine(byte[] buf, int start, int len)
            {
                this.lines++;
                return true;
            }
        }

        TestByteLineScanner tbls = new TestByteLineScanner(bais);
        tbls.scan();
        assertEquals(2, tbls.lines);
    }

    @Test
    public void testAvailable() throws IOException
    {
        try (ByteArrayInputStream bais = new ByteArrayInputStream("test\ntest2\n\test3\n".getBytes())) {
            class TestByteLineScanner extends AbstractByteLineScanner
            {
                public TestByteLineScanner(InputStream is)
                {
                    super(is);
                }

                @Override
                public boolean newLine(byte[] buf, int start, int len)
                {
                    return false;
                }
            }

            TestByteLineScanner tbls = new TestByteLineScanner(bais);
            tbls.scan();
            assertEquals(12, tbls.available());

            byte[] expected = "test2\n\test3\n".getBytes();
            byte[] actual = tbls.availableBuffer();
            assertEquals(12, expected.length);
            for (int i = 0; i < 12; i++) {
                assertEquals(expected[i], actual[i]);
            }
        }
    }
}
