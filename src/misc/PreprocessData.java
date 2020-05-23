package misc;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import dc.GP.AbstractNode;


import dc.io.FReader;
import dc.io.Logger;
import dc.io.FReader.FileMember;
import files.FWriter;

public class PreprocessData {

	int numberOfThresholds;
	protected static double[] THRESHOLDS;
	protected static AbstractNode[] GP_TREES;
	protected static String[] GP_TREES_STRING;
	public static int NUM_OF_PROCESSORS = 5;
	//public static boolean LINEAR_FUNCTIONALITY_ONLY = false;
	//public static int NEGATIVE_EXPRESSION_REPLACEMENT = 5;
	public static String FUNCTION_NODE_DEFINITION = null;
	double thresholdIncrement;
	
	
	int TrainingDay = -1;

	
	

	int currentGeneration;

	Double[] training;
	Double[] test;
	DCCurveRegression[] curves;

	static double[][] pop;
	double[][] newPop;

	static int nRuns;

	protected static Random random;
	static Map<Double, String> thresholdGPStringMap = new HashMap<Double, String>();
	public static Map<Double, String> thresholdGPStringUpwardMap = new HashMap<Double, String>();
	public static Map<Double, String> thresholdGPStringDownwardMap = new HashMap<Double, String>();

	protected static Logger log;
	public static boolean splitDatasetByTrendType = false; // Reads input
	public static boolean REUSE_EXISTING_TREE = true;
															// parameter
	// to decide whether to split dataset to upward and downward trend datasets

	
	public PreprocessData(String filename, String prefix, boolean isOpenClosePrice, String storedDirectory ) throws IOException, ParseException {
		
	

		//System.out.println("Loading directional changes data...");

		// loads the data
		HashMap<String, ArrayList<FReader.FileMember>> yearRecordInMap = FReader.loadDataObjectByDay(filename,isOpenClosePrice);

		//ArrayList<Double[]> numberOfDays = FReader.loadData(filename,10);
		

		for (Map.Entry<String, ArrayList<FReader.FileMember>> entry : yearRecordInMap.entrySet())
		{
			String s_ = entry.getKey();
			String s[] = s_.split("\\.");
			String fileName = storedDirectory+"//"+prefix+"_"+s[2]+s[1]+".txt";
			
			FWriter writer = new FWriter(fileName);
			writer.openToAppend(new File(fileName));
			
			ArrayList<FReader.FileMember> currentItem = entry.getValue();
			Iterator<FReader.FileMember> iterator = currentItem.iterator();
			while (iterator.hasNext()) {
				FReader.FileMember fileMember = iterator.next();
				String fileContent =  fileMember.Day + " " + fileMember.time + " " + 
						fileMember.bid + " " + fileMember.ask;
				if (isOpenClosePrice)
				{
					fileContent = fileContent + " " + fileMember.closeBid + " " + fileMember.closeAsk;
				}
				writer.write(fileContent);					
							
				System.out.println(fileContent);
			}
			writer.closeFile();
		}

		

		System.out.println("XXXXXX");
	}

	

	public static void main(String[] args) throws Exception {
		

		// Split the long parameter file , according to the delimiter
		String s[] = args[0].split(":");
		boolean isOpenClose = false;
		if (s[2].equalsIgnoreCase("true"))
			isOpenClose = true;
		
		File dir = new File(s[3]);
		
		boolean successful = dir.mkdir();
			
    	String storedDirectory = null;
	   		
	    if (!dir.exists())
		{	
		   	System.out.println("Direcrtory not set");
	    	System.exit(0);
	    }
	    else
	    {
	    	storedDirectory = dir.getAbsolutePath();
	    	System.setProperty("user.dir", storedDirectory);
	    	System.out.println("Files stored in :" + storedDirectory);
	    }
		
		PreprocessData ga = new PreprocessData(s[0], s[1], isOpenClose,  storedDirectory);
		System.out.println("XXXXXX");
		
		//TreeHelperClass.readFitnessFile(2);
	}
}
