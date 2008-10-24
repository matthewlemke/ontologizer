/*
 * Created on 29.10.2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.gui.swt;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import ontologizer.FileCache;
import ontologizer.OntologizerCore;
import ontologizer.PopulationSet;
import ontologizer.StudySet;
import ontologizer.StudySetList;
import ontologizer.FileCache.FileCacheUpdateCallback;
import ontologizer.calculation.CalculationRegistry;
import ontologizer.gui.swt.MainWindow.Set;
import ontologizer.gui.swt.images.Images;
import ontologizer.gui.swt.support.SWTUtil;
import ontologizer.gui.swt.threads.AnalyseThread;
import ontologizer.gui.swt.threads.SimilarityThread;
import ontologizer.statistics.TestCorrectionRegistry;
import ontologizer.worksets.WorkSet;
import ontologizer.worksets.WorkSetList;

import org.eclipse.swt.graphics.DeviceData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import tools.Sleak;

/**
 * 
 * This is the main class of the Ontologizer SWT Application
 * 
 * @author Sebastian Bauer
 *
 */
public class Ontologizer
{
	private static Logger logger = Logger.getLogger(Ontologizer.class.getName());

	private static MainWindow main;
	private static PreferencesWindow prefs;
	private static AboutWindow about;
	private static HelpWindow help;
	private static LogWindow log;
	private static WorkSetWindow workSet;
	private static NewProjectWizard newProjectWizard;
	private static GraphWindow graph;
	private static LinkedList<ResultWindow> resultWindowList;
	private static File workspace;

	public static ThreadGroup threadGroup;

	private static WorkSetList workSetList = new WorkSetList();
	
	static
	{
		/* Default */
		workSetList.add("C. elegans","http://www.geneontology.org/ontology/gene_ontology_edit.obo","http://cvsweb.geneontology.org/cgi-bin/cvsweb.cgi/go/gene-associations/gene_association.wb.gz?rev=HEAD");
		workSetList.add("Fruit Fly","http://www.geneontology.org/ontology/gene_ontology_edit.obo","http://cvsweb.geneontology.org/cgi-bin/cvsweb.cgi/go/gene-associations/gene_association.fb.gz?rev=HEAD");
		workSetList.add("Mouse","http://www.geneontology.org/ontology/gene_ontology_edit.obo","http://cvsweb.geneontology.org/cgi-bin/cvsweb.cgi/go/gene-associations/gene_association.mgi.gz?rev=HEAD");
		workSetList.add("Human","http://www.geneontology.org/ontology/gene_ontology_edit.obo","http://cvsweb.geneontology.org/cgi-bin/cvsweb.cgi/go/gene-associations/gene_association.goa_human.gz?rev=HEAD");
		workSetList.add("Protein Data Bank","http://www.geneontology.org/ontology/gene_ontology_edit.obo","http://cvsweb.geneontology.org/cgi-bin/cvsweb.cgi/go/gene-associations/gene_association.goa_pdb.gz?rev=HEAD");
		workSetList.add("Rice", "http://www.geneontology.org/ontology/gene_ontology_edit.obo", "http://cvsweb.geneontology.org/cgi-bin/cvsweb.cgi/go/gene-associations/gene_association.gramene_oryza.gz?rev=HEAD");
//		workSetList.add("UniProt", "http://www.geneontology.org/ontology/gene_ontology_edit.obo", "http://cvsweb.geneontology.org/cgi-bin/cvsweb.cgi/go/gene-associations/gene_association.goa_uniprot.gz?rev=HEAD");
		workSetList.add("Yeast","http://www.geneontology.org/ontology/gene_ontology_edit.obo","http://cvsweb.geneontology.org/cgi-bin/cvsweb.cgi/go/gene-associations/gene_association.sgd.gz?rev=HEAD");
	}

	/**
	 * This is the action which is executed on a download click within
	 * the workset window.
	 */
	private static ISimpleAction downloadAction =
		new ISimpleAction()
		{
			public void act()
			{
				try
				{
					if (workSet.getSelectedAddress() == null) return;
					FileCache.open(workSet.getSelectedAddress());
					
				} catch (IOException e)
				{
					logException(e);
				}
			}
		};

	private static ISimpleAction invalidateAction =
		new ISimpleAction()
		{
			public void act()
			{
				if (workSet.getSelectedAddress() == null) return;
				FileCache.invalidate(workSet.getSelectedAddress());
				workSet.updateWorkSetList(workSetList);
			}
		};

	public static void main(String[] args)
	{
		boolean useSleak = false;
		String os = System.getProperty("os.name");
		if (os.contains("Linux"))
		{
			/* We are running on linux which requires the MOZILLA_FIVE_HOME
			 * variable set. If this is not done, we do it now.
			 */
			String mozHome = System.getenv("MOZILLA_FIVE_HOME");
			if (mozHome == null)
			{
				File xulRunnerDir = new File("/usr/lib/xulrunner");
				if (xulRunnerDir.exists()) env.SetEnv.setenv("MOZILLA_FIVE_HOME","/usr/lib/xulrunner");
				else env.SetEnv.setenv("MOZILLA_FIVE_HOME","/usr/lib/mozilla");
			}
		}

		/* Prepare threads */
		threadGroup = new ThreadGroup("Worker");

		/* Prepare logging */
		Logger rootLogger = Logger.getLogger("");
		rootLogger.addHandler(new Handler()
		{
			public void close() throws SecurityException { }
			public void flush() { }
			public void publish(LogRecord arg0)
			{
				log(arg0.getLevel().getName(),arg0.getMessage());
			}
		});
//		rootLogger.setLevel(Level.FINEST);

		/* Prepare the help system */
		File helpFolderFile = new File(System.getProperty("user.dir"),"src/ontologizer/help");//.getAbsolutePath();
		String helpFolder = "";

		try
		{
			if (!helpFolderFile.exists())
			{
				/* The help folder doesn't exists, so try to copy the files to a
				 * directory */

				/* TODO: Find out if we there is a possibility to list the files */
				File file = File.createTempFile("onto","");
				File imgDir = new File(file,"images");
				file.delete();
				file.mkdirs();
				imgDir.mkdirs();

				copyFileToTemp("help/1_overview.html",file.getCanonicalPath());
				copyFileToTemp("help/2_requirements.html",file.getCanonicalPath());
				copyFileToTemp("help/3_howto.html",file.getCanonicalPath());
				copyFileToTemp("help/4_results.html",file.getCanonicalPath());
				copyFileToTemp("help/5_tutorial.html",file.getCanonicalPath());

				copyFileToTemp("help/images/filesets.png",imgDir.getCanonicalPath());
				copyFileToTemp("help/images/main-with-project.png",imgDir.getCanonicalPath());
				copyFileToTemp("help/images/menu-preferences.png",imgDir.getCanonicalPath());
				copyFileToTemp("help/images/name-of-the-project.png",imgDir.getCanonicalPath());
				copyFileToTemp("help/images/new-project.png",imgDir.getCanonicalPath());
				copyFileToTemp("help/images/new-project-fileset.png",imgDir.getCanonicalPath());
				copyFileToTemp("help/images/population.png",imgDir.getCanonicalPath());
				copyFileToTemp("help/images/preferences.png",imgDir.getCanonicalPath());
				copyFileToTemp("help/images/result-graph.png",imgDir.getCanonicalPath());
				copyFileToTemp("help/images/result-initial.png",imgDir.getCanonicalPath());
				copyFileToTemp("help/images/study.png",imgDir.getCanonicalPath());
				
				helpFolder = file.getAbsolutePath();
				
			} else
			{
				helpFolder = helpFolderFile.getCanonicalPath();
			}
		}
		catch (IOException e) { }

		DeviceData data = new DeviceData();
		data.tracking = useSleak;
		Display display = new Display (data);
		Images.setDisplay(display);

		Sleak sleak = null;
		if (useSleak)
		{
			sleak = new Sleak();
			sleak.open ();
		}
		
		main = new MainWindow(display);
		prefs = new PreferencesWindow(display);
		about = new AboutWindow(display);
		help = new HelpWindow(display,helpFolder);
		log = new LogWindow(display);
		workSet = new WorkSetWindow(display);
		graph = new GraphWindow(display);
		newProjectWizard = new NewProjectWizard(display);
		resultWindowList = new LinkedList<ResultWindow>();

		/* When the analyze button is pressed */
		main.addAnalyseAction(new ISimpleAction(){public void act()
		{
			List<MainWindow.Set> list = main.getSetEntriesOfCurrentPopulation();
			if (list.size() > 1)
			{
				PopulationSet populationSet = getPopulationSetFromList(list);
				StudySetList studySetList = getStudySetListFromList(list);
	
				String defintionFile = main.getDefinitionFileString();
				String associationsFile = main.getAssociationsFileString();
				String mappingFile = main.getMappingFileString();
				String methodName = main.getSelectedMethodName();
				String mtcName = main.getSelectedMTCName();

				if (mappingFile != null && mappingFile.length() == 0)
					mappingFile = null;

				final Display display = main.getShell().getDisplay();
				final ResultWindow result = new ResultWindow(display);
				result.setBusyPointer(true);
				resultWindowList.add(result);

				Runnable calledWhenFinished = new Runnable()
				{
					public void run()
					{
						display.syncExec(new Runnable(){ public void run()
						{
							if (!main.getShell().isDisposed())
								main.enableAnalyseButton();
						}});
					}
				};
				/* Now let's start the task...TODO: Refactor! */
				final Thread newThread = new AnalyseThread(display,calledWhenFinished,
						result,defintionFile,associationsFile,mappingFile,
						populationSet,studySetList,methodName,mtcName,
						GlobalPreferences.getNumberOfPermutations());
				result.addCloseAction(new ISimpleAction(){public void act()
				{
					newThread.interrupt();
					resultWindowList.remove(result);
					result.dispose();
				}});
				newThread.start();
			}
		}});
		
		/* When the "Similarity" button is pressed */
		main.addSimilarityAction(new ISimpleAction()
		{
			public void act()
			{
				List<MainWindow.Set> list = main.getSetEntriesOfCurrentPopulation();
				if (list.size() > 1)
				{
					final Display display = main.getShell().getDisplay();

					final StudySetList studySetList = getStudySetListFromList(list);
					final WorkSet workSet = main.getSelectedWorkingSet();
					final ResultWindow result = new ResultWindow(display);

					result.open();
					result.setBusyPointer(true);
					resultWindowList.add(result);
					
					Runnable calledWhenFinished = new Runnable()
					{
						public void run()
						{
							display.syncExec(new Runnable(){ public void run()
							{
								if (!main.getShell().isDisposed())
									main.enableAnalyseButton();
							}});
						}
					};

					final SimilarityThread newThread = new SimilarityThread(display,calledWhenFinished,result,studySetList,workSet);
					result.addCloseAction(new ISimpleAction(){public void act()
					{
						newThread.interrupt();
						resultWindowList.remove(result);
						result.dispose();
					}});
					newThread.start();
				}
			}
		});
		
		/* On a new project event */
		main.addNewProjectAction(new ISimpleAction()
		{
			public void act()
			{
				newProjectWizard.open(workSetList);
			}
		});

		/* On a opening the preferences event */
		main.addOpenPreferencesAction(new ISimpleAction()
		{
			public void act() { prefs.open(); }
		});

		/* On opening the log window event */
		main.addOpenLogWindowAction(new ISimpleAction()
		{
			public void act() { log.open(); }
		});

		/* On a opening the help event */
		main.addOpenHelpContentsAction(new ISimpleAction()
		{
			public void act() { help.open(); }
		});

		/* On a about window opening event */
		main.addOpenAboutAction(new ISimpleAction()
		{
			public void act() { about.open(); }
		});

		/* On a workset window opening event */
		main.addOpenWorkSetAction(new ISimpleAction()
		{
			public void act()
			{
				workSet.updateWorkSetList(workSetList);
				workSet.open();
			}
		});

		/* Store the current settings on disposal */
		main.addDisposeAction(new ISimpleAction(){public void act()
		{
			Preferences p = Preferences.userNodeForPackage(Ontologizer.class);
			p.put("definitionFile",main.getDefinitionFileString());
			p.put("associationsFile",main.getAssociationsFileString());
			p.put("mtc", main.getSelectedMTCName());
			p.put("method", main.getSelectedMethodName());
			p.put("dotCMD",GlobalPreferences.getDOTPath());
			p.put("numberOfPermutations",Integer.toString(GlobalPreferences.getNumberOfPermutations()));
			p.put("wrapColumn", Integer.toString(GlobalPreferences.getWrapColumn()));
			if (GlobalPreferences.getProxyHost() != null)
			{
				p.put("proxyHost",GlobalPreferences.getProxyHost());
				p.put("proxyPort", Integer.toString(GlobalPreferences.getProxyPort()));
			}
		}});

		workSet.addDownloadAction(downloadAction);
		workSet.addInvalidateAction(invalidateAction);

		FileCache.addUpdateCallback(new FileCacheUpdateCallback(){
			public void update(String url)
			{
				main.getShell().getDisplay().asyncExec(new Runnable()
				{
					public void run()
					{
						workSet.updateWorkSetList(workSetList);
					}
				});
			}

			public void exception(Exception exception, String url)
			{
				Ontologizer.logException(exception);
			}
		});

		/* Remember the dot path, if it was accepted */
		prefs.addAcceptPreferencesAction(new ISimpleAction()
		{
			public void act()
			{
				GlobalPreferences.setDOTPath(prefs.getDOTPath());
				GlobalPreferences.setNumberOfPermutations(prefs.getNumberOfPermutations());
				GlobalPreferences.setProxyHost(prefs.getProxyHost());
				GlobalPreferences.setProxyPort(prefs.getProxyPort());
				GlobalPreferences.setWrapColumn(prefs.getWrapColumn());
			}
		});

		/* Set preferences */
		Preferences p = Preferences.userNodeForPackage(Ontologizer.class);
		main.setDefinitonFileString(p.get("definitionFile",""));
		main.setAssociationsFileString(p.get("associationsFile",""));
		main.setSelectedMTCName(p.get("mtc",TestCorrectionRegistry.getDefault().getName()));
		main.setSelectedMethodName(p.get("method",CalculationRegistry.getDefault().getName()));
		GlobalPreferences.setDOTPath(p.get("dotCMD","dot"));
		GlobalPreferences.setNumberOfPermutations(p.getInt("numberOfPermutations", 500));
		GlobalPreferences.setProxyPort(p.get("proxyPort", "8888"));
		GlobalPreferences.setProxyHost(p.get("proxyHost", ""));
		GlobalPreferences.setWrapColumn(p.getInt("wrapColumn", 30));

		/* Prepare workspace */
		workspace = new File(ontologizer.util.Util.getAppDataDirectory("ontologizer"),"workspace");
		if (!workspace.exists())
			workspace.mkdirs();
		logger.info("Workspace directory is \"" + workspace.getAbsolutePath() + "\"");

		main.setWorkspace(workspace);
		main.updateWorkSetList(workSetList);

		/* Prepare the file cache */
		FileCache.setCacheDirectory(new File(workspace,".cache").getAbsolutePath());

		Shell shell = main.getShell();
		shell.open();

		while (!shell.isDisposed())
		{
			try
			{
				if (!display.readAndDispatch())
					display.sleep();
			} catch(Exception ex)
			{
				logException(ex);
			}
		}

		/* Dispose the result windows but they have to be copied into a separate
		 * array before, as disposing the window implicates removing them from
		 * the list */
		ResultWindow [] resultWindowArray = new ResultWindow[resultWindowList.size()];
		int i=0;
		for (ResultWindow resultWindow : resultWindowList)
			resultWindowArray[i++] = resultWindow; 
		for (i = 0;i<resultWindowArray.length;i++)
			resultWindowArray[i].dispose();

		/* Dispose the rest */
		prefs.dispose();
		about.dispose();
		help.dispose();
		log.dispose();
		graph.dispose();
		workSet.dispose();

		Images.diposeImages();

		if (useSleak)
		{
			while (!sleak.shell.isDisposed ()) {
				if (!display.readAndDispatch ()) display.sleep ();
			}
		}

		FileCache.abortAllDownloads();

		/* Ensure that all threads are finished before the main thread
		 * disposes the device. */
		ThreadGroup group = threadGroup;
		group.interrupt();

		try
		{
			synchronized(group)
			{
				while (group.activeCount() > 0)
					group.wait( 10 );
			}
		
		} catch (InterruptedException e)
		{
			e.printStackTrace();
		}

		display.dispose();
	}

	private static PopulationSet getPopulationSetFromList(List<Set> list)
	{
		if (list == null) return null;
		return new PopulationSet(list.get(0).name,list.get(0).entries);
	}
	
	private static StudySetList getStudySetListFromList(List<Set> list)
	{
		Iterator<MainWindow.Set> iter = list.iterator();

		/* Skip population */
		iter.next();

		StudySetList studySetList = new StudySetList("guiList");

		while (iter.hasNext())
		{
			MainWindow.Set sSet = iter.next();
			StudySet studySet = new StudySet(sSet.name, sSet.entries);
			studySetList.addStudySet(studySet);
		}
		return studySetList;
	}

	private static void copyFileToTemp(String file, String tempDir)
	{
		InputStream is = OntologizerCore.class.getResourceAsStream(file);
		
		try
		{
			byte [] buf = new byte[8192];
			int read;
			File destFile = new File(tempDir,new File(file).getName());
			BufferedOutputStream dest = new BufferedOutputStream(new FileOutputStream(destFile));

			while ((read = is.read(buf))>0)
				dest.write(buf,0,read);
	
			dest.close();
			destFile.deleteOnExit();
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns whether the project name is valid.
	 * 
	 * @return
	 */
	public static boolean isProjectNameValid(String name)
	{
		List<String> l = main.getProjectNames();
		for (String s : l)
		{
			if (s.equalsIgnoreCase(name))
				return false;
		}
		return true;
	}
	
	/**
	 * Shows the wait pointer on all application windows.
	 */
	public static void showWaitPointer()
	{
		main.showWaitPointer();
		prefs.showWaitPointer();
		about.showWaitPointer();
		help.showWaitPointer();
		for (ResultWindow r : resultWindowList)
			r.showWaitPointer();
	}
	
	/**
	 * Hides the wait pointer on all application windows.
	 */
	public static void hideWaitPointer()
	{
		for (ResultWindow r : resultWindowList)
			r.hideWaitPointer();
		help.hideWaitPointer();
		about.hideWaitPointer();
		prefs.hideWaitPointer();
		main.hideWaitPointer();
	}

	public static File getWorkspace()
	{
		return workspace;
	}
	
	public static void newProject(File project)
	{
		main.addProject(project);
	}
	
	public static void log(String name, String message)
	{
		SimpleDateFormat sdf = new SimpleDateFormat();
		String date = sdf.format(new Date(System.currentTimeMillis()));
		final String line = "(" + date + ") " + "[" + name + "] " + message + "\n";

		if (main != null && !main.getShell().isDisposed())
		{
			main.getShell().getDisplay().asyncExec(new Runnable()
			{
				public void run()
				{
					log.addToLog(line);
				}
			});
		}
	}

	/**
	 * Log and displays the given exception.
	 * 
	 * @param e
	 */
	public static void logException(final Exception e)
	{
		log("Exception",e.getLocalizedMessage());
		if (main != null && !main.getShell().isDisposed())
		{
			main.display.syncExec(new Runnable()
			{
				public void run() {SWTUtil.displayException(main.getShell(), e);};
			});
		}
	}
	
	/**
	 * Log at information level.
	 *
	 * @param txt
	 */
	public static void logInfo(String txt)
	{
		log("Info",txt);
	}
}
