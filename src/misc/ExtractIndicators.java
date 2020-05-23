package misc;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import dc.GP.Const;
import dc.io.FReader;
import dc.io.Logger;
import weka.classifiers.Classifier;
import weka.classifiers.CostMatrix;
import weka.classifiers.bayes.net.EditableBayesNet;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.functions.Logistic;
import weka.classifiers.functions.SMO;
import weka.classifiers.lazy.IBk;
import weka.classifiers.lazy.KStar;
import weka.classifiers.meta.CostSensitiveClassifier;
import weka.classifiers.rules.DecisionTable;
import weka.classifiers.rules.JRip;
import weka.classifiers.rules.PART;
import weka.classifiers.trees.DecisionStump;
import weka.classifiers.trees.J48;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.net.BayesNetGenerator;




public class ExtractIndicators {

	int numberOfThresholds;
	protected static double[] THRESHOLDS;



	double thresholdIncrement;
	
	public static FileWriter fileWriter;
	public static FileWriter fileWriterTemporalByTime;
	public static FileWriter fileWriterModelRules;
	Double[] training;
	
	DCCurveFeatureExtraction[] curves;
	
	public static Map<String, Evaluation[]> evaluationsMap = new LinkedHashMap<String, Evaluation[]>();
	public static Map<String, Classifier[]> modelsNoFilterMap = new LinkedHashMap<String, Classifier[]>();
	//DCCurveFeatureExtractionCostSensitiveClassifier [] curves;


	protected static Random random;
	static Map<Double, String> thresholdGPStringMap = new HashMap<Double, String>();
	public static Map<Double, String> thresholdGPStringUpwardMap = new HashMap<Double, String>();
	public static Map<Double, String> thresholdGPStringDownwardMap = new HashMap<Double, String>();

	protected static Logger log;
	
	// to decide whether to split dataset to upward and downward trend datasets


	
	public ExtractIndicators(String filename,Classifier classifier, Classifier classifierNoFilter, String kfold, String classifierName) throws IOException, ParseException {
		double initial =  0.01;
		THRESHOLDS = new double[Const.NUMBER_OF_THRESHOLDS];
		

		
		for (int i = 0; i < THRESHOLDS.length; i++) {  
			 //THRESHOLDS[i] = (initial * (i + 1)) / 100.0;
			THRESHOLDS[i] = (initial + (0.0025 * i)) / 100.0; //Correct one /Right one, replace after test
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
				curves = new DCCurveFeatureExtraction[THRESHOLDS.length];
				//curves = new DCCurveFeatureExtractionCostSensitiveClassifier[THRESHOLDS.length];

		//System.out.println("DC curves:");
		
			for (int i = 0; i < curves.length; i++) {
				curves[i] = new DCCurveFeatureExtraction(classifier,classifierNoFilter);
				//curves[i] = new  DCCurveFeatureExtractionCostSensitiveClassifier(classifier,classifierNoFilter);
				
				curves[i].build(training, THRESHOLDS[i], "");
				String thresholdStr = String.format("%.8f", THRESHOLDS[i]);
				 System.out.println(thresholdStr);
				 
				 
				 System.out.println(curves[i].events.length);
				 
				 curves[i].extractDCfeatures();
				 
				 curves[i].extractDCDatasetTemporalFeacture(filename, curves[i].thresholdString,8);
				 
				 curves[i].printFeatures(filename, curves[i].thresholdString, 0.70);
				 
				 curves[i].classify(filename, curves[i].thresholdString , kfold, classifierName);
				 
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
				new J48(), // a decision tree
				new PART(), 
				new DecisionTable(),//decision table majority classifier
				new DecisionStump(), //one-level decision tree
				new JRip(),
				new Logistic(),
				new  KStar(),
				new  IBk(),
				new NaiveBayes(),
				new BayesNet(),
				new SMO(),
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
				new J48(), // a decision tree
				new PART(), 
				new DecisionTable(),//decision table majority classifier
				new DecisionStump(), //one-level decision tree
				new JRip(),
				new Logistic(),
				new KStar(),
				new  IBk(),
				new NaiveBayes(),
				new BayesNet(),
			new SMO(),
				//csc
		};
		
		
		for (int j = 0; j < models.length; j++){
		
			String modelName = models[j].getClass().getName();
			System.out.println(modelName);
		    
		      
			String kfold = "";
			if (args[2] != null)
				kfold = args[2];
			else{
				System.out.println("Specify type of testing");
				System.exit(1);
			}
			
			String modelNameArray[] = modelName.split("\\.");
			String classifierName = modelNameArray[modelNameArray.length-1]; 
			System.out.println(classifierName);
			fileWriter = new FileWriter(classifierName+"_"+ kfold+ "_"+resultFile);
			fileWriterTemporalByTime =   new FileWriter(classifierName+"_"+ kfold+ "_"+"_temporalByTime.txt");
		/*	fileWriter.write(" FileName,  threshold, Percentage minority class, Percentage majority class, sensitivity_Yes, specificity_yes, " +
					" recall_yes, F-score_yes, FPR_yes, TPR_yes, Accuracy_yes, precision_yes, sensitivity_no, specificity_no, recall_no, F-score_no, FPR_no, " +
					" TPR_no, Accuracy_no, precision_no");*/
			
			fileWriter.write("FileName, "+
			"threshold, "+
			"Percentage minority class before filter,"+
			"Percentage minority class after filter,"+
			"Percentage majority class before filter," +
			"Percentage majority class after filter, " +
			"recall_beforeFilter," + 
			"recall_afterFilter," + 
			"precision_beforeFilter," + 
			"precision_afterFilter," + 
			"fmeasure_beforeFilter," + 
			"fmeasure_afterFilter," + 
			"accuracy_beforeFilter," +
			"accuracy_afterFilter," +
			//"accuracy_beforeFilter No," +
			//"accuracy_afterFilter No," +
	/*		"Recall_yes_beforeFilter,Recall_yes_afterFilter," +
			"F-score_yes_beforeFilter,F-score_yes_afterFilter," +
			
			 "Accuracy_yes_beforeFilter,Accuracy_yes_afterFilter," +
			"Precision_yes_beforeFilter,Precision_yes_afterFilter,"+
			
			"Recall_no_beforeFilter,Recall_no_afterFilter,"+
			"F-score_no_beforeFilter,F-score_no_afterFilter,"
			
			+ "Accuracy_no_beforeFilter,Accuracy_no_afterFilter,"+
			"Precision_no_beforeFilter,Precision_no_afterFilter," + */
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
			
			fileWriterModelRules =   new FileWriter(classifierName+"_"+ kfold+ "_"+"_Rules.txt");
			fileWriterModelRules.write("FileName, "+
					"threshold, "+
					"Rule");
			
			
			for (String file : s) {
				ExtractIndicators classify = new ExtractIndicators(file,models[j],modelsNoFilter[j],kfold,classifierName);
				System.out.println(classify.numberOfThresholds);
			}
			fileWriter.flush();
			fileWriter.close();
			
			
			fileWriterTemporalByTime.flush();
			fileWriterTemporalByTime.close();
			
			fileWriterModelRules.flush();
			fileWriterModelRules.close();
						
			
		}
		ArrayList<String> modelPrecisionResultString = new ArrayList<String>();
		ArrayList<String> modelRecallResultString = new ArrayList<String>();
		ArrayList<String> modelAccuracyResultString = new ArrayList<String>();
		
		String algorithmName = null;
		ArrayList<String> algorithmNameArray = new ArrayList<String>();
		String algorithmPrecisionResult = null;
		String algorithmRecallResult = null;
		String algorithmAccuracyResult = null;
		boolean ifFirst = true;
		for (Map.Entry<String, Evaluation[]> entry : evaluationsMap.entrySet()) {
			//System.out.println("Key : " + entry.getKey() );
			Evaluation myEvaluation =  entry.getValue()[0];
			double filterPrecisionDbl =  (myEvaluation.confusionMatrix()[0][0])/(myEvaluation.confusionMatrix()[0][0] +myEvaluation.confusionMatrix()[1][0] );
 			if(Double.isNaN(filterPrecisionDbl))
 				filterPrecisionDbl = 0.0;		
 			
 			double filterAccuracyDbl = ((myEvaluation.confusionMatrix()[0][0]+ myEvaluation.confusionMatrix()[1][1])/(myEvaluation.confusionMatrix()[0][1]+ myEvaluation.confusionMatrix()[1][0]+ myEvaluation.confusionMatrix()[0][0]+ myEvaluation.confusionMatrix()[1][1])) ;
 			if(Double.isNaN(filterAccuracyDbl))
 				filterAccuracyDbl = 0.0;
 			
 			double filterRecallDbl = (myEvaluation.confusionMatrix()[0][0])/(myEvaluation.confusionMatrix()[0][0] +myEvaluation.confusionMatrix()[0][1] );
 			if(Double.isNaN(filterRecallDbl))
 				filterRecallDbl = 0.0;
 			
			String[] parts = entry.getKey().split("\\+");
			String key = parts[2];
			if (algorithmName == null )
			{
				algorithmName = parts[2];
				algorithmNameArray.add(algorithmName);
				algorithmPrecisionResult = parts[0]+","+parts[1]+","+filterPrecisionDbl+"\n";
				algorithmRecallResult = parts[0]+","+parts[1]+","+filterRecallDbl+"\n";
				algorithmAccuracyResult = parts[0]+","+parts[1]+","+filterAccuracyDbl+"\n";
				
			}
			else{
				if (algorithmName.compareToIgnoreCase(parts[2]) != 0){
					algorithmName = parts[2];
					algorithmNameArray.add(algorithmName);
					ifFirst = false;
					//Add content to arrayList
					modelPrecisionResultString.add(algorithmPrecisionResult);
					modelRecallResultString.add(algorithmRecallResult);
					modelAccuracyResultString.add(algorithmAccuracyResult);
					
					//modelRecallResultString.add
					//clear content
					algorithmPrecisionResult = "";
					algorithmPrecisionResult = ","+filterPrecisionDbl+"\n";
					
					algorithmRecallResult = "";
					algorithmRecallResult = ","+filterRecallDbl+"\n";
					
					algorithmAccuracyResult = "";
					algorithmAccuracyResult = ","+filterAccuracyDbl+"\n";
				
				}
				else{
					if (ifFirst){
						algorithmPrecisionResult = algorithmPrecisionResult+parts[0]+","+parts[1]+","+filterPrecisionDbl+"\n";
						algorithmRecallResult = algorithmRecallResult +parts[0]+","+parts[1]+","+filterRecallDbl+"\n";
						algorithmAccuracyResult = algorithmAccuracyResult +parts[0]+","+parts[1]+","+filterAccuracyDbl+"\n";
					
					}
					else{
						algorithmPrecisionResult = algorithmPrecisionResult+","+filterPrecisionDbl+"\n";
						algorithmRecallResult = algorithmRecallResult+","+filterRecallDbl+"\n";
						algorithmAccuracyResult = algorithmAccuracyResult+","+filterAccuracyDbl+"\n";
					}
				}
			}
		}
		
		//finally Add data for last algorithm in collection
		modelPrecisionResultString.add(algorithmPrecisionResult);
		modelRecallResultString.add(algorithmRecallResult);
		modelAccuracyResultString.add(algorithmAccuracyResult);
		
		
		//Print precision
		String[] myData = null;
		
		myData = modelPrecisionResultString.get(0).split("\\r?\\n");
		 
		for ( int k =1; k< modelPrecisionResultString.size();k++) {
			String element =  modelPrecisionResultString.get(k);
			String[] myDataTemp = element.split("\\r?\\n");
			for (int m=0; m<myDataTemp.length; m++)
			{
				myData[m] = myData[m]+myDataTemp[m];
				//System.out.println(myData[m]);
			}
			 //System.out.println("");
		}
		
		
		FileWriter fileWriterPrecision =    new FileWriter("AllAlgorithmPrecision.txt"); 
		
		fileWriterPrecision.write("FileName,threshold,");
		for (int m =0; m<algorithmNameArray.size();m++){
			fileWriterPrecision.write(algorithmNameArray.get(m)+ ",");
		}
		
		fileWriterPrecision.write("\n");
		
		for (int m=0; m<myData.length; m++){
			fileWriterPrecision.write(myData[m]);
			fileWriterPrecision.write("\n");
		}
		
		fileWriterPrecision.flush();
		fileWriterPrecision.close();
		
		
		//end print precision
		//start accuracy
		myData = null;
		myData = modelAccuracyResultString.get(0).split("\\r?\\n");
		 
		for ( int k =1; k< modelAccuracyResultString.size();k++) {
			String element =  modelAccuracyResultString.get(k);
			String[] myDataTemp = element.split("\\r?\\n");
			for (int m=0; m<myDataTemp.length; m++)
			{
				myData[m] = myData[m]+myDataTemp[m];
				//System.out.println(myData[m]);
			}
			 //System.out.println("");
		}
		
		
		FileWriter fileWriterAccuracy =    new FileWriter("AllAlgorithmAccuracy.txt"); 
		
		fileWriterAccuracy.write("FileName,threshold,");
		for (int m =0; m<algorithmNameArray.size();m++){
			fileWriterAccuracy.write(algorithmNameArray.get(m)+ ",");
		}
		
		fileWriterAccuracy.write("\n");
		
		for (int m=0; m<myData.length; m++){
			fileWriterAccuracy.write(myData[m]);
			fileWriterAccuracy.write("\n");
		}
		
		fileWriterAccuracy.flush();
		fileWriterAccuracy.close();
		// end print accuracy
		// start print recall
		
		myData = null;
		myData = modelRecallResultString.get(0).split("\\r?\\n");
		 
		for ( int k =1; k< modelRecallResultString.size();k++) {
			String element =  modelRecallResultString.get(k);
			String[] myDataTemp = element.split("\\r?\\n");
			for (int m=0; m<myDataTemp.length; m++)
			{
				myData[m] = myData[m]+myDataTemp[m];
				//System.out.println(myData[m]);
			}
		}
		
		
		FileWriter fileWriterRecall =    new FileWriter("AllAlgorithmRecall.txt"); 
		
		fileWriterRecall.write("FileName,threshold,");
		for (int m =0; m<algorithmNameArray.size();m++){
			fileWriterRecall.write(algorithmNameArray.get(m)+ ",");
		}
		
		fileWriterRecall.write("\n");
		
		for (int m=0; m<myData.length; m++){
			fileWriterRecall.write(myData[m]);
			fileWriterRecall.write("\n");
		}
		
		fileWriterRecall.flush();
		fileWriterRecall.close();

		
		// end print recall
		
		//TreeHelperClass.readFitnessFile(2);
	}
}

