package att.grappa;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestGradient
{
    @Test
    @SuppressWarnings("unchecked")
    public void testGradient() throws Exception
    {
        String gr = "digraph G { test [label=\"Test\",style=\"filled\",gradientangle=270,fillcolor=\"white:red\"]; }";
        ByteArrayInputStream bais = new ByteArrayInputStream(gr.getBytes());

        Parser parser = new Parser(bais, System.err);
        parser.parse();

        Node testNode = (Node) parser.getGraph().elements(GrappaConstants.NODE).nextElement();
        assertEquals(270, Integer.parseInt((String) testNode.getAttributeValue("gradientangle")));

        List<Color> colorList = (List<Color>) testNode.getAttributeValue(GrappaConstants.FILLCOLOR_ATTR);
        assertEquals(0xffffffff, colorList.get(0).getRGB());
        assertEquals(0xffff0000, colorList.get(1).getRGB());

    }
}
