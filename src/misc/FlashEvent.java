package misc;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import dc.GP.Const;
import dc.io.FReader;
import dc.io.Logger;
import weka.classifiers.Classifier;
import weka.classifiers.CostMatrix;
import weka.classifiers.bayes.net.EditableBayesNet;
import weka.classifiers.functions.Logistic;
import weka.classifiers.meta.CostSensitiveClassifier;
import weka.classifiers.rules.DecisionTable;
import weka.classifiers.rules.JRip;
import weka.classifiers.rules.PART;
import weka.classifiers.trees.DecisionStump;
import weka.classifiers.trees.J48;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.net.BayesNetGenerator;




public class FlashEvent {

	int numberOfThresholds;
	protected static double[] THRESHOLDS;



	double thresholdIncrement;
	
	public static FileWriter fileWriter;
	public static FileWriter fileWriterTemporalByTime;
	Double[] training;
	
	//DCCurveFeatureExtraction[] curves;
	DCCurveFlashEvent [] curves;


	protected static Random random;
	static Map<Double, String> thresholdGPStringMap = new HashMap<Double, String>();
	public static Map<Double, String> thresholdGPStringUpwardMap = new HashMap<Double, String>();
	public static Map<Double, String> thresholdGPStringDownwardMap = new HashMap<Double, String>();

	protected static Logger log;
	
	// to decide whether to split dataset to upward and downward trend datasets


	
	public FlashEvent(String filename, double initial, double thresholdDelta) throws IOException, ParseException {
		//double initial =  0.05;
		THRESHOLDS = new double[Const.NUMBER_OF_THRESHOLDS];
		

		
		for (int i = 0; i < THRESHOLDS.length; i++) {  
			 //THRESHOLDS[i] = (initial * (i + 1)) / 100.0;
			//THRESHOLDS[i] = (initial + (0.0075 * i)) / 100.0; //Correct one /Right one, replace after test
			THRESHOLDS[i] = (initial + (thresholdDelta * i)) / 100.0; 
			//THRESHOLDS[i] = (0.01 + (0.0025 * i)) / 100.0;
			String thresholdStr = String.format ("%.8f",THRESHOLDS[i]);
			 System.out.println(thresholdStr);
			
		}

		
		ArrayList<Double[]> days = FReader.loadData(filename,false);
		//for (int i = 0; i < FReader.dataRecordInFileArray.size(); i++)
		//	System.out.println("Count " + i + " Data-"+ FReader.dataRecordInFileArray.get(i).Day + "   " + FReader.dataRecordInFileArray.get(i).time +  
		///			"   " + FReader.dataRecordInFileArray.get(i).price);
		

		// allow the creation of training & testing data sets that are longer
		// than 1 day
		ArrayList<Double[]> ar = new ArrayList<Double[]>();
		for (int i = 0; i < days.size(); i++)
			ar.add(days.get(i));
		int size = 0;
		for (Double[] d : ar)
			size += d.length;
		training = new Double[size];
		int counter = 0;
		for (Double[] d : ar) {
			for (double n : d) {
				training[counter] = n;
				counter++;
			}
		}
				//curves = new DCCurveFeatureExtraction[THRESHOLDS.length];
				curves = new DCCurveFlashEvent[THRESHOLDS.length];

		//System.out.println("DC curves:");
		
			for (int i = 0; i < curves.length; i++) {
				//curves[i] = new DCCurveFeatureExtraction(classifier,classifierNoFilter);
				curves[i] = new  DCCurveFlashEvent();
				curves[i].build(training, THRESHOLDS[i], "");
				String thresholdStr = String.format("%.8f", THRESHOLDS[i]);
				 System.out.println(thresholdStr);
				 
				 curves[i].findFlashEvent();
				 System.out.println(curves[i].events.length);
				 
				 DCCurveFlashEvent.flashEventString = DCCurveFlashEvent.flashEventString +curves[i].events.length + "," +filename+"\n";
				 
				 fileWriter.write(curves[i].events.length + "," +filename+"\n");
				
			}
			
			System.out.println( DCCurveFlashEvent.flashEventString );
			//fileWriter.write(DCCurveFlashEvent.flashEventString );
		
	}

	

	public static void main(String[] args) throws Exception {
		
		//String filename= args[0];
		String s[] = args[0].split(":");
		
		 String resultFileName = null;
		if (args[1] != null)
			resultFileName = args[1];
		else{
			System.out.println("Specify result file");
			System.exit(1);
		}
		
		File resultFile = new File(resultFileName);
		 // creates the file
		resultFile.createNewFile();
		
		
		
		
	
			fileWriter = new FileWriter(resultFile);
			fileWriter.write("Threshold,"+
					"% FlashEvent,"+
					"% FlashEventWithOvershoot," +
					"% FlashEventWithoutOvershoot," +
					"% FlashEventWithOvershootInAllDCEvents," +
					"% OverShootInAllDCEvents," +
					"Total Events," +
					"market");
			
			fileWriter.write("\n");
			
			double initial[] = {0.001, 0.01,0.05,0.1};
			double thresholdDelta[] = {0.0075,0.025,0.0020,0.025};
			
			for (String file : s) {
				if (initial.length == thresholdDelta.length){
					for (int b =0; b <initial.length ; b++){
						FlashEvent classify = new FlashEvent(file,initial[b],thresholdDelta[b]);
					}
				}
				
			}
			fileWriter.flush();
			fileWriter.close();
			
			
			
		
		//TreeHelperClass.readFitnessFile(2);
	}
}

