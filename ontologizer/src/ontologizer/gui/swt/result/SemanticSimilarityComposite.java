package ontologizer.gui.swt.result;

import net.sourceforge.nattable.NatTable;
import ontologizer.ByteString;
import ontologizer.calculation.SemanticResult;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

/**
 * The GUI for semantic similarity measures.
 * 
 * @author Sebastian Bauer
 *
 */
public class SemanticSimilarityComposite extends Composite
{
	private SemanticResult result;

	private SemanticSimilarityNatModel semanticSimilarityNatModel;
	private NatTable natTable;
	private Point natTableLastSelected = new Point(-1,-1);

	private Text selectedSimilarityText;

	private ResultControls resultControls;
	private Browser browser;

	public SemanticSimilarityComposite(Composite parent, int style)
	{
		super(parent, style);
		
		setLayout(new FillLayout());
	
		resultControls = new ResultControls(this,0);
		browser = resultControls.getBrowser();
		Composite tableComposite = resultControls.getTableComposite();
		tableComposite.setLayout(new GridLayout());

		semanticSimilarityNatModel = new SemanticSimilarityNatModel();
		semanticSimilarityNatModel.setSingleCellSelection(true);
		semanticSimilarityNatModel.setEnableMoveColumn(false);
		
		natTable = new NatTable(tableComposite,
								SWT.NO_BACKGROUND | SWT.NO_REDRAW_RESIZE | SWT.DOUBLE_BUFFERED | SWT.V_SCROLL | SWT.H_SCROLL,
				                semanticSimilarityNatModel);
		natTable.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		natTable.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseUp(MouseEvent e)
			{
				updateSelectedText();
			}
		});
		natTable.addMouseMoveListener(new MouseMoveListener()
		{
			public void mouseMove(MouseEvent e)
			{
				updateSelectedText();
			}
		});
		
		selectedSimilarityText = new Text(tableComposite,SWT.BORDER);
		selectedSimilarityText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}

	public void updateSelectedText()
	{
		Point newNatTableLastSelected = natTable.getSelectionSupport().getLastSelectedCell();
		if (natTableLastSelected.x != newNatTableLastSelected.x ||
			natTableLastSelected.y != newNatTableLastSelected.y)
		{
			selectedSimilarityText.setText(Double.toString(getLastSelectedValue()));
			
			ByteString gene1 = result.names[newNatTableLastSelected.x];
			ByteString gene2 = result.names[newNatTableLastSelected.y];

			updateBrowser(gene1,gene2);
			newNatTableLastSelected.x = newNatTableLastSelected.x;
			newNatTableLastSelected.y = newNatTableLastSelected.y;
		}
			
//		natTableLastSelected = newNatTableLastSelected;
//		selectedSimilarityText.setText(Double.toString(getLastSelectedValue()));
	}
	
	public double getLastSelectedValue()
	{
		Point p;
		
		p = natTable.getSelectionSupport().getLastSelectedCell();
		if (p == null) return Double.NaN;
		
		int x = p.x;
		int y = p.y;
		
		return semanticSimilarityNatModel.getValue(x,y); 
	}

	public void updateBrowser(ByteString g1, ByteString g2)
	{
		StringBuilder str = new StringBuilder();
		str.append("<html>");
		str.append("<body>");
		str.append("<h1>");
		str.append(g1.toString());
		str.append(" ");
		str.append(g2.toString());
		str.append("</h1>");
		str.append("</body>");
		str.append("<html/>");
		
		browser.setText(str.toString());
	}
	
	public void setResult(SemanticResult result)
	{
		this.result = result;
		semanticSimilarityNatModel.setValues(result.mat);
		semanticSimilarityNatModel.setNames(result.names);
		natTable.updateResize();
	}
}
