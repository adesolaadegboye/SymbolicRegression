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




public class ExtractTickIndicators {

	int numberOfThresholds;
	protected static double[] THRESHOLDS;



	double thresholdIncrement;
	
	public static FileWriter fileWriter;
	public static FileWriter fileWriterTemporalByTime;
	Double[] training;
	
	//DCCurveFeatureExtraction[] curves;
	DCCurveFeatureExtractCostSensClassifier5Indicators [] curves;


	protected static Random random;
	static Map<Double, String> thresholdGPStringMap = new HashMap<Double, String>();
	public static Map<Double, String> thresholdGPStringUpwardMap = new HashMap<Double, String>();
	public static Map<Double, String> thresholdGPStringDownwardMap = new HashMap<Double, String>();

	protected static Logger log;
	
	// to decide whether to split dataset to upward and downward trend datasets


	
	public ExtractTickIndicators(String filename,Classifier classifier, Classifier classifierNoFilter) throws IOException, ParseException {
		double initial =  0.01;
		THRESHOLDS = new double[Const.NUMBER_OF_THRESHOLDS];
		

		
		for (int i = 0; i < THRESHOLDS.length; i++) {  
			 //THRESHOLDS[i] = (initial * (i + 1)) / 100.0;
			THRESHOLDS[i] = (initial + (0.0025 * i)) / 100.0; //Correct one /Right one, replace after test
			//THRESHOLDS[i] = (0.01 + (0.0025 * i)) / 100.0;
			String thresholdStr = String.format ("%.8f",THRESHOLDS[i]);
			 System.out.println(thresholdStr);
			
		}

		
		ArrayList<Double[]> days = FReader.loadDataHasOnePrice(filename);
		String tickFileName = filename.replaceAll("10min", "hqtick");
		if (FReader.globalfileName == null || !FReader.globalfileName.equalsIgnoreCase(tickFileName) ){
			FReader.loadDataTickData(tickFileName);
		}
		
		
		
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
				curves = new DCCurveFeatureExtractCostSensClassifier5Indicators[THRESHOLDS.length];

		//System.out.println("DC curves:");
		
			for (int i = 0; i < curves.length; i++) {
				//curves[i] = new DCCurveFeatureExtraction(classifier,classifierNoFilter);
				curves[i] = new  DCCurveFeatureExtractCostSensClassifier5Indicators(classifier,classifierNoFilter);
				curves[i].build(training, THRESHOLDS[i], "");
				String thresholdStr = String.format("%.8f", THRESHOLDS[i]);
				 System.out.println(thresholdStr);
				 
				 
				 System.out.println(curves[i].events.length);
				 
				 curves[i].extractDCfeatures();
				 
				 curves[i].extractDCDatasetTemporalFeacture(filename, curves[i].thresholdString,8);
				 
				 curves[i].extractFeacturesFromTickData();
				 
				 curves[i].printFeatures(filename, curves[i].thresholdString, 0.70);
				 
				 curves[i].classify(filename, curves[i].thresholdString);
				 
				 curves[i].deleteTemporaryFiles(filename, curves[i].thresholdString);
				 
				
			}
		
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
		
		CostMatrix cm1 = new CostMatrix(2);
		cm1.normalize(); // set diaginals to 0
		// cm.setCell(rowIndex, int columnIndex, java.lang.Object value)
		cm1.setCell(0, 1, -10);
		cm1.setCell(1, 0, -2);

		// set up the cost sensitive classifier
		CostSensitiveClassifier csc1 = new CostSensitiveClassifier();
		csc1.setCostMatrix(cm1);
		
		Classifier[] models = { 
//				new J48(), // a decision tree
//				new PART(), 
//				new DecisionTable(),//decision table majority classifier
//				new DecisionStump(), //one-level decision tree
//				new JRip(),
				new Logistic(),
				new EditableBayesNet(),
				new BayesNetGenerator(),
				new NaiveBayes(),
				new BayesNet(),
				//csc1
		};
		
		CostMatrix cm = new CostMatrix(2);
		cm.normalize(); // set diaginals to 0
		// cm.setCell(rowIndex, int columnIndex, java.lang.Object value)
		cm.setCell(0, 1, -10);
		cm.setCell(1, 0, -2);

		// set up the cost sensitive classifier
		CostSensitiveClassifier csc = new CostSensitiveClassifier();
		csc.setCostMatrix(cm);
		Classifier[] modelsNoFilter = { 
//				new J48(), // a decision tree
//				new PART(), 
//				new DecisionTable(),//decision table majority classifier
//				new DecisionStump(), //one-level decision tree
//				new JRip(),
				new Logistic(),
				new EditableBayesNet(),
				new BayesNetGenerator(),
				new NaiveBayes(),
				new BayesNet(),
				//csc
		};
		
		for (int j = 0; j < models.length; j++){
		
			String modelName = models[j].getClass().getName();
			System.out.println(modelName);
		    
		      
			
			
			String modelNameArray[] = modelName.split("\\.");
			String classifierName = modelNameArray[modelNameArray.length-1]; 
			System.out.println(classifierName);
			fileWriter = new FileWriter(classifierName+"_"+resultFile);
			fileWriterTemporalByTime =   new FileWriter(classifierName+"_temporalByTime.txt");
		/*	fileWriter.write(" FileName,  threshold, Percentage minority class, Percentage majority class, sensitivity_Yes, specificity_yes, " +
					" recall_yes, F-score_yes, FPR_yes, TPR_yes, Accuracy_yes, precision_yes, sensitivity_no, specificity_no, recall_no, F-score_no, FPR_no, " +
					" TPR_no, Accuracy_no, precision_no");*/
			
			fileWriter.write("FileName, "+
			"threshold, "+
			"Percentage minority class before filter,"+
			"Percentage minority class after filter,"+
			"Percentage majority class before filter," +
			"Percentage majority class after filter, " +
			
			"Recall_yes_beforeFilter,Recall_yes_afterFilter," +
			"F-score_yes_beforeFilter,F-score_yes_afterFilter," +
			/*"FPR_yes_beforeFilter, FPR_yes_afterFilter,"+		
			"TPR_yes_beforeFilter,TPR_yes_afterFilter," +*/
			 "Accuracy_yes_beforeFilter,Accuracy_yes_afterFilter," +
			"Precision_yes_beforeFilter,Precision_yes_afterFilter,"+
			
			"Recall_no_beforeFilter,Recall_no_afterFilter,"+
			"F-score_no_beforeFilter,F-score_no_afterFilter,"
			/*+ "FPR_no_beforeFilter,FPR_no_afterFilter,"+
			"TPR_no_beforeFilter,TPR_no_afterFilter," */
			+ "Accuracy_no_beforeFilter,Accuracy_no_afterFilter,"+
			"Precision_no_beforeFilter,Precision_no_afterFilter," +
			"Root_mean_square_error_beforeFilter," +
			"Root_mean_square_error_afterFilter");
			fileWriter.write("\n");
			
			
			fileWriterTemporalByTime.write("FileName, "+
			"threshold, "+
			"00:00:00-03:00:00 - % overshoot," +
			"00:00:00-03:00:00 - % number of DC events," +
			"03:00:01-06:00:00 - % overshoot," +
			"03:00:01-06:00:00 - % number of DC events," +
			"06:00:01-09:00:00 - % overshoot," +
			"06:00:01-09:00:00 - % numer of DC events" +
			"09:00:01-12:00:00 - % overshoot," +
			"09:00:01-12:00:00 - % number of DC events," +
			"12:00:01-15:00:00 - % overshoot," +
			"12:00:01-15:00:00 - % number of DC events," +
			"15:00:01-18:00:00 - % overshoot," +
			"15:00:01-18:00:00 - % number of DC events," +
			"18:00:01-21:00:00 - % overshoot," +
			"18:00:01-21:00:00 - % number of DC events," +
			"21:00:01-24:00:00 - % overshoot," +
			"21:00:01-24:00:00 - % number of DC events," );
			fileWriterTemporalByTime.write("\n");
			
			
			for (String file : s) {
				ExtractTickIndicators classify = new ExtractTickIndicators(file,models[j],modelsNoFilter[j]);
				System.out.println(classify.numberOfThresholds);
			}
			fileWriter.flush();
			fileWriter.close();
			
			
			fileWriterTemporalByTime.flush();
			fileWriterTemporalByTime.close();
		}
		//TreeHelperClass.readFitnessFile(2);
	}
}

