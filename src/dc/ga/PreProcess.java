package dc.ga;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.Map.Entry;

import dc.ga.DCCurve.Event;
import dc.io.FReader;
import dc.io.FReader.FileMember2;
import misc.myAutoWeka;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.CostMatrix;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.functions.Logistic;
import weka.classifiers.functions.SMO;
import weka.classifiers.lazy.IBk;
import weka.classifiers.lazy.KStar;
import weka.classifiers.rules.DecisionTable;
import weka.classifiers.rules.JRip;
import weka.classifiers.rules.PART;
import weka.classifiers.trees.DecisionStump;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.classifiers.meta.AutoWEKAClassifier;
import weka.core.converters.ConverterUtils.DataSource;
public class PreProcess {

	Classifier[] classifierArray;
	String[] bestAutoWekaExperimentPaths;
	double thresholdDbl;
	String thresholdString = "";
	public Event lastTrainingEvent = null;
	public Event lastTestingEvent = null;
	public Event[] trainingEventArray = null;
	public Event[] testEventArray = null;
	public ArrayList<FileMember2> dataRecordInFileArrayTest = new ArrayList<FileMember2>();
	boolean useAuto = true;

	static public ArrayList<String[]> eventFeatures = new ArrayList<String[]>();

	Instances trainingInstancesAuto = null;
	Instances trainingInstancesManual = null;
	Instances testInstances = null;
	Instances data = null;
	public myAutoWeka autoWEKAClassifier = null;
	ArrayList<myAutoWeka> autoWEKAClassifierList = null;
	String autoWEKAClassifierListEvalString = "";
	double AutoWekafmeasure = 0.0;

	int trainingCount = -1;
	Classifier currentModel = null;

	public String filename;
	public String regressionAlgoName;
	public Map<Classifier[], Evaluation[]> classifierEvaluationMap = new LinkedHashMap<Classifier[], Evaluation[]>();
	public Classifier DCCurveClassifier[] = null;
	public Evaluation DCCurveEvaluation[] = null;
	String comparisonResult;
	public ArrayList<String> tempFilePath = new ArrayList<String>();
	String bestPrecision = "";
	String bestRecall = "";
	double bestFMeasure = 0.0;
	double bestAccuracy = 0.0;
	int processTestDataCount = 0;

	public PreProcess(double delta, String filename, String regressionAlgoName) {

		// this.trainingData = Arrays.copyOf(trainingDate, trainingDate.length);
		// this.testingData = Arrays.copyOf(testingData, testingData.length);
		processTestDataCount = 0;
		Classifier[] classifiers = { new J48(), // a decision tree
				new PART(), new DecisionTable(), // decision table majority
													// classifier
				new DecisionStump(), // one-level decision tree
				new JRip(), new Logistic(), new KStar(), new IBk(), new NaiveBayes(), new BayesNet(), new SMO()

				// csc
		};

		classifierArray = Arrays.copyOf(classifiers, classifiers.length);
		thresholdDbl = delta;
		this.thresholdString = String.format("%.8f", delta);
		this.filename = filename;
		this.regressionAlgoName = regressionAlgoName;
		//deleteTemporaryFiles(filename+regressionAlgoName, thresholdString);
	}

	public void buildTraining(Event[] events) {

		extractTrainingDCfeatures(events);
		printTrainingFeatures(filename, thresholdString, events);

		/* no longer in user now doing autoWeka instead of manual
		 * for (int classifierCount = 0; classifierCount < classifierArray.length; classifierCount++) {
			currentModel = classifierArray[classifierCount];
			classifyTraining(filename, thresholdString, "kfold");

		}*/
		trainingEventArray =  new Event[events.length];
		System.arraycopy( events, 0, trainingEventArray, 0, events.length );
	}

	public void buildTest(Event[] testEvents) {

		// this.testEvents = events.toArray(new Event[events.size()]);

		int lengthOfDataset = (lastTestingEvent.end + lastTrainingEvent.end+1) == FReader.dataRecordInFileArray.size()
				? lastTestingEvent.end + lastTrainingEvent.end : lastTestingEvent.end + lastTrainingEvent.end + 1;
		for (int testIt = lastTrainingEvent.end + 1; testIt < lengthOfDataset; testIt++) // we
																							// only
																							// want
																							// records
																							// in
																							// the
																							// testing
																							// data.
		{

			// System.out.println(FReader.dataRecordInFileArray.get(testIt).Day
			// + " " + FReader.dataRecordInFileArray.get(testIt).time);

			if (testIt >= FReader.dataRecordInFileArray.size())
				continue;

			dataRecordInFileArrayTest.add(FReader.dataRecordInFileArray.get(testIt));

		}

	}

	public void processTestData(Event[] testEvents) {
		Event[] copiedArray = Arrays.copyOf(testEvents, testEvents.length);

		extractTestDCfeatures(copiedArray);
		printTestFeatures(filename, thresholdString, copiedArray);
		testEventArray = new Event[testEvents.length];
		System.arraycopy(copiedArray,0 , testEventArray, 0 ,copiedArray.length);

	}

	public void loadTestData(Event[] testEvents) {

		File f = new File(filename);

		String absolutePath = "";
		try {
			absolutePath = f.getCanonicalPath();
		} catch (IOException e3) {

			e3.printStackTrace();
		}
		// System.out.println("File path : " + absolutePath);

		String folderName = absolutePath.substring(0, absolutePath.lastIndexOf(File.separator));
		String fileName = absolutePath.substring(absolutePath.lastIndexOf(File.separator) + 1,
				absolutePath.length() - 4);

		BufferedReader breader = null;
		try {
			breader = new BufferedReader(
					new FileReader(folderName + "/" + fileName+regressionAlgoName + "_Features_" + thresholdString + "_test.arff"));
		} catch (FileNotFoundException e2) {

			e2.printStackTrace();
		}

		try {
			System.out.println("About to read "+ folderName + "/" + fileName+regressionAlgoName + "_Features_" + thresholdString + "_test.arff");
			//DataSource source = new DataSource(folderName + "/" + fileName+regressionAlgoName + "_Features_" + thresholdString + "_test.arff");
			testInstances = new Instances(breader);
		//	testInstances = new Instances(source.getDataSet());
			testInstances.setClassIndex(testInstances.numAttributes() - 1);
			System.out.println("Read "+ folderName + "/" + fileName+regressionAlgoName + "_Features_" + thresholdString + "_test.arff");
		} catch ( Exception e1) {
			testInstances.setClassIndex(1);
		}
		

		System.out.println("Instance " + testInstances.numInstances() + " event count " + testEvents.length);

	}
	public void loadTestData() {

		File f = new File(filename);

		String absolutePath = "";
		try {
			absolutePath = f.getCanonicalPath();
		} catch (IOException e3) {

			e3.printStackTrace();
		}
		// System.out.println("File path : " + absolutePath);

		String folderName = absolutePath.substring(0, absolutePath.lastIndexOf(File.separator));
		String fileName = absolutePath.substring(absolutePath.lastIndexOf(File.separator) + 1,
				absolutePath.length() - 4);

		BufferedReader breader = null;
		try {
			breader = new BufferedReader(
					new FileReader(folderName + "/" + fileName+regressionAlgoName + "_Features_" + thresholdString + "_test.arff"));
		} catch (FileNotFoundException e2) {

			e2.printStackTrace();
			System.exit(-1);
		}

		try {
			System.out.println("About to read "+ folderName + "/" + fileName+regressionAlgoName + "_Features_" + thresholdString + "_test.arff");
			//DataSource source = new DataSource(folderName + "/" + fileName+regressionAlgoName + "_Features_" + thresholdString + "_test.arff");
			testInstances = new Instances(breader);
		//	testInstances = new Instances(source.getDataSet());
			testInstances.setClassIndex(testInstances.numAttributes() - 1);
			System.out.println("Read "+ folderName + "/" + fileName+regressionAlgoName + "_Features_" + thresholdString + "_test.arff");
		} catch ( Exception e1) {
			testInstances.setClassIndex(1);
		}
		

		System.out.println("Instance " + testInstances.numInstances() );

	}

	public void loadTrainingData(Event[] events) {

		File f = new File(filename);
		Instances trainingInstances = null;
		String absolutePath = "";
		try {
			absolutePath = f.getCanonicalPath();
		} catch (IOException e2) {

			e2.printStackTrace();
		}
		// System.out.println("File path : " + absolutePath);

		String folderName = absolutePath.substring(0, absolutePath.lastIndexOf(File.separator));
		String fileName = absolutePath.substring(absolutePath.lastIndexOf(File.separator) + 1,
				absolutePath.length() - 4);
		// System.out.println("File path : " + folderName);

		BufferedReader breader = null;

		try {
			String trainingDataset = null;

			trainingDataset = folderName + "/" + fileName+regressionAlgoName + "_CompleteEventFeatures_" + thresholdString + ".arff";

			breader = new BufferedReader(new FileReader(trainingDataset));
			trainingInstances = new Instances(breader);
			trainingInstances.setClassIndex(trainingInstances.numAttributes() - 1);
			breader.close();

		} catch (Exception e1) {

			e1.printStackTrace();
		}

		trainingInstances.setClassIndex(trainingInstances.numAttributes() - 1);
		trainingInstancesManual = new Instances(trainingInstances, 0, trainingInstances.size());
		trainingInstancesManual.setClassIndex(trainingInstancesManual.numAttributes() - 1);

		trainingInstancesAuto = new Instances(trainingInstances, 0, trainingInstances.size());
		trainingInstancesAuto.setClassIndex(trainingInstancesAuto.numAttributes() - 1);
		System.out.println("Instance " + trainingInstances.numInstances() + " event count " + events.length);

	}

	public void extractTestDCfeatures(Event[] testEvents) {

		for (int k = 0 /* testIndexStart */; k < testEvents.length  /* testIndexEnd */; k++) {

			int start = testEvents[k].start;
			int end = testEvents[k].end;
			boolean isEnd = false;

			/*
			 * if (start == dataRecordInFileArrayTest.size()){ //just use the
			 * last element in dataRecordInFileArrayTest start =
			 * testEvents[k-1].start; end = testEvents[k-1].end;
			 * testEvents[k].endDate = dataRecordInFileArrayTest.get(end).Day;
			 * testEvents[k].endPrice =
			 * dataRecordInFileArrayTest.get(end).price; testEvents[k].endTime =
			 * dataRecordInFileArrayTest.get(end).time; isEnd = true;
			 * 
			 * } else
			 */// if (end == dataRecordInFileArrayTest.size()){
				// end = start;
				// testEvents[k].endDate =
				// dataRecordInFileArrayTest.get(start).Day;
				// testEvents[k].endPrice =
				// dataRecordInFileArrayTest.get(start).price;
			// testEvents[k].endTime =
			// dataRecordInFileArrayTest.get(start).time;
			// }
			// else{
			// System.out.println("End");

			if (start >= dataRecordInFileArrayTest.size() || end >= dataRecordInFileArrayTest.size())
				continue;

			processTestDataCount = k;
			testEvents[k].endDate = dataRecordInFileArrayTest.get(end).Day;
			testEvents[k].endPrice = dataRecordInFileArrayTest.get(end).price;
			testEvents[k].endTime = dataRecordInFileArrayTest.get(end).time;
			// }

			testEvents[k].startDate = dataRecordInFileArrayTest.get(start).Day;
			testEvents[k].startPrice = dataRecordInFileArrayTest.get(start).price;
			testEvents[k].startTime = dataRecordInFileArrayTest.get(start).time;

			if (k > 0) {
				if (testEvents[k - 1].overshoot != null && testEvents[k - 1].overshoot.length() > 0)
					testEvents[k].previousDChadOvershoot = "yes";

				testEvents[k].PreviousDCPrice = testEvents[k - 1].startPrice;
			} else {
				testEvents[k].previousDChadOvershoot = "no";
				testEvents[k].PreviousDCPrice = "0.0";
			}

			/*
			 * if (k>1) { if (testEvents[k-2].overshoot != null &&
			 * testEvents[k-2].overshoot.length()>0)
			 * testEvents[k]._2previousDChadOvershoot= "yes"; } else {
			 * testEvents[k]._2previousDChadOvershoot ="no"; }
			 */

			if (testEvents[k].start == testEvents[k].end)
				testEvents[k].isFlashEvent = "yes";

			double endPrice = Double.parseDouble(dataRecordInFileArrayTest.get(end).price);
			double startPrice = Double.parseDouble(dataRecordInFileArrayTest.get(start).price);

			double price = Math.abs(((endPrice - startPrice) / startPrice) / thresholdDbl);
			double percentagePriceDisplacement = price; // (price / endPrice)
														// *100.0;
			DecimalFormat df = new DecimalFormat("#.#################");
			String value = Double.toString(Double.parseDouble(df.format(percentagePriceDisplacement)));

			double osPrice = 0.0;
			double osEndPriceDbl = 0.0;
			String osEndPrice = "";
			double currentTmv = 0.0;

			String string[] = testEvents[k].endDate.split("\\.");
			String endYear = string[2].replaceFirst("^0+(?!$)", "");
			String endMonth = string[1].replaceFirst("^0+(?!$)", "");
			String endDay = string[0].replaceFirst("^0+(?!$)", "");
			Arrays.fill(string, null);
			string = testEvents[k].startDate.split("\\.");

			String startYear = string[2].replaceFirst("^0+(?!$)", "");
			String startMonth = string[1].replaceFirst("^0+(?!$)", "");
			String startDay = string[0].replaceFirst("^0+(?!$)", "");

			Arrays.fill(string, null);
			string = testEvents[k].startTime.split(":");

			String startHour = string[2].replaceFirst("^0+(?!$)", "");
			String startMinute = string[1].replaceFirst("^0+(?!$)", "");
			String startSeconds = string[0].replaceFirst("^0+(?!$)", "");

			Arrays.fill(string, null);
			string = testEvents[k].endTime.split(":");

			String endHour = string[2].replaceFirst("^0+(?!$)", "");
			String endMinute = string[1].replaceFirst("^0+(?!$)", "");
			String endSeconds = string[0].replaceFirst("^0+(?!$)", "");

			Calendar endCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			endCalendar.clear();
			endCalendar.set(Integer.parseInt(endYear), Integer.parseInt(endMonth) - 1, Integer.parseInt(endDay),
					Integer.parseInt(endHour), Integer.parseInt(endMinute), Integer.parseInt(endSeconds));
			long endSecondsSinceEpoch = endCalendar.getTimeInMillis() / 1000L;

			Calendar startCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			startCalendar.clear();
			startCalendar.set(Integer.parseInt(startYear), Integer.parseInt(startMonth) - 1, Integer.parseInt(startDay),
					Integer.parseInt(startHour), Integer.parseInt(startMinute), Integer.parseInt(startSeconds));
			long startSecondsSinceEpoch = startCalendar.getTimeInMillis() / 1000L;

			double timeDiff = Math.abs(endSecondsSinceEpoch - startSecondsSinceEpoch);
			// double percentageTimeDisplacement = (
			// timeDiff/endSecondsSinceEpoch) * 100;
			double _10MinsTimeDifference = Math.round(timeDiff / 60.0);

			double _TT10MinsTimeDifference = 0.0;
			if (testEvents[k].overshoot != null && testEvents[k].overshoot.length() > 0) {
				int osStart = testEvents[k].overshoot.start;
				String osStartPrice = FReader.dataRecordInFileArray.get(osStart).price;

				int osEnd = testEvents[k].overshoot.end;
				osEndPrice = FReader.dataRecordInFileArray.get(osEnd).price;
				osEndPriceDbl = Double.parseDouble(osEndPrice);
				double osStartPriceDbl = Double.parseDouble(osStartPrice);

				osPrice = Math.abs(((osEndPriceDbl - osStartPriceDbl) / osStartPriceDbl) / thresholdDbl);
				currentTmv = Math.abs(((osEndPriceDbl - startPrice) / startPrice) / thresholdDbl);

				int overshootEnd = testEvents[k].overshoot.end;
				testEvents[k].overshoot.endDate = FReader.dataRecordInFileArray.get(overshootEnd).Day;

				testEvents[k].overshoot.endPrice = FReader.dataRecordInFileArray.get(overshootEnd).price;

				testEvents[k].overshoot.endTime = FReader.dataRecordInFileArray.get(overshootEnd).time;
				Arrays.fill(string, null);
				string = testEvents[k].overshoot.endDate.split("\\.");

				String overshootEndYear = string[2].replaceFirst("^0+(?!$)", "");
				String overshootEndMonth = string[1].replaceFirst("^0+(?!$)", "");
				String overshootEndDay = string[0].replaceFirst("^0+(?!$)", "");

				Arrays.fill(string, null);
				string = testEvents[k].overshoot.endTime.split(":");

				String overshootEndHour = string[2].replaceFirst("^0+(?!$)", "");
				String overshootEndMinute = string[1].replaceFirst("^0+(?!$)", "");
				String overshootEndSeconds = string[0].replaceFirst("^0+(?!$)", "");

				Calendar overshootEndCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
				overshootEndCalendar.clear();
				overshootEndCalendar.set(Integer.parseInt(overshootEndYear), Integer.parseInt(overshootEndMonth) - 1,
						Integer.parseInt(overshootEndDay), Integer.parseInt(overshootEndHour),
						Integer.parseInt(overshootEndMinute), Integer.parseInt(overshootEndSeconds));
			} else {
				currentTmv = Math.abs(((endPrice - startPrice) / startPrice) / thresholdDbl);

			}
			_TT10MinsTimeDifference = _10MinsTimeDifference;
			testEvents[k].osv_ext = Double.toString(Double.parseDouble(df.format(osPrice)));
			testEvents[k].percentageDeltaPrice = value;

			testEvents[k].tmv = Double.toString(Double.parseDouble(df.format(currentTmv)));
			testEvents[k].sigma = Double.toString((currentTmv * thresholdDbl) / _TT10MinsTimeDifference);

			if (testEvents[k].sigma.compareToIgnoreCase("NaN") == 0) {
				// System.out.println(this.events[k].sigma);
				testEvents[k].sigma = "-1";
			}

			if (testEvents[k].overshoot != null)
				testEvents[k].hasOverShoot = "yes";

			// if (testEvents[k].overshoot != null && value == "0.0")
			// System.out.println("X");

			if (isEnd) {
				testEvents[k].datapoints = new ArrayList<FReader.FileMember2>(
						dataRecordInFileArrayTest.subList(testEvents[k].start, testEvents[k].end));
			} else if (start == end) {
				testEvents[k].datapoints = new ArrayList<FReader.FileMember2>();
				testEvents[k].datapoints.add(dataRecordInFileArrayTest.get(testEvents[k].start));

			} else {
				testEvents[k].datapoints = new ArrayList<FReader.FileMember2>(
						dataRecordInFileArrayTest.subList(testEvents[k].start, testEvents[k].end + 1));
			}
			String timeValue = Double.toString(Double.parseDouble(df.format(_10MinsTimeDifference / 10)));

			testEvents[k].percentageDeltaDuration = timeValue;

			// System.out.println((endSecondsSinceEpoch -
			// startSecondsSinceEpoch) + " " + timeValue + " priceValue " +
			// value );
			// System.out.println("StartTime " + " " + events[k].startTime+ "
			// endTime " + events[k].endTime);
			// System.out.println(events[k].startDate + " " +events[k].endDate +
			// " " + events[k].startPrice + "" + events[k].endPrice + " " +
			// events[k].DCValue );
		}

	}

	void getBestAutoClassifier() {
		// Using same record unchanged
		Instances trainingInstancesAutoTemp = new Instances(trainingInstancesAuto, 0, trainingInstancesAuto.size());
		trainingInstancesAutoTemp.setClassIndex(trainingInstancesAutoTemp.numAttributes() - 1);
		

		for (int i = 0; i < autoWEKAClassifierList.size(); i++) {

			String detailResult = autoWEKAClassifierList.get(i).detailString();
			String lines[] = detailResult.split("\\r?\\n");
			String precision = "";
			String recall = "";
			double fmeasure = 0.0;
			for (int detailClassResultCount = 0 ; detailClassResultCount< lines.length; detailClassResultCount++){
				if (detailClassResultCount == 0 || detailClassResultCount ==1 
						|| detailClassResultCount==2 || detailClassResultCount==4 ||
						detailClassResultCount==5)
					continue;
				
				if (detailClassResultCount==3){
					String after = lines[detailClassResultCount].trim().replaceAll(" +", ",");
					String DCWithOSClassification[] = after.split(",");
					precision = DCWithOSClassification[2];
					recall = DCWithOSClassification[3];
					fmeasure = 2*((Double.parseDouble(precision) * Double.parseDouble(recall))
							/ (Double.parseDouble(precision) + Double.parseDouble(recall) ));
				}
			}
			if (autoWEKAClassifierList.isEmpty())
				return;
			if (autoWEKAClassifier == null) {
				if (autoWEKAClassifier == null)
					autoWEKAClassifier = new myAutoWeka();

			}
			if (bestRecall.isEmpty()) {
				try {
					autoWEKAClassifier = (myAutoWeka) AbstractClassifier.makeCopy(autoWEKAClassifierList.get(i));
					bestRecall = recall;
					bestPrecision = precision;
					bestFMeasure = fmeasure; 
					Evaluation eval = null;
					try {
						eval = new Evaluation(trainingInstancesAuto);
						eval.evaluateModel(autoWEKAClassifier, trainingInstancesAuto);
						
						
				
					} catch (Exception e) {
						e.printStackTrace();
					}
					
					bestAccuracy =  eval.pctCorrect();
				} catch (Exception e) {
					
					e.printStackTrace();
				}
			} 
			//else if (Double.parseDouble(precision) > Double.parseDouble(bestPrecision)){
			else if (fmeasure > bestFMeasure){
				try {
					autoWEKAClassifier = (myAutoWeka) AbstractClassifier.makeCopy(autoWEKAClassifierList.get(i));
					bestRecall = recall;
					bestPrecision = precision;
					bestFMeasure = fmeasure; 
					Evaluation eval = null;
					try {
						eval = new Evaluation(trainingInstancesAuto);
						eval.evaluateModel(autoWEKAClassifier, trainingInstancesAuto);
						
						
				
					} catch (Exception e) {
						e.printStackTrace();
					}
					
					bestAccuracy =  eval.pctCorrect();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		autoWEKAClassifierList.clear();
		autoWEKAClassifierList.add(autoWEKAClassifier);
		bestAutoWekaExperimentPaths = Arrays.copyOf(autoWEKAClassifier.getmsExperimentPathsArray(),
				autoWEKAClassifier.getmsExperimentPathsArray().length);
		
	}
	
	void getAutoWekaClassifierDetailResult(){
		
		String detailResult = autoWEKAClassifier.detailString();

		//BufferedReader bufReader = new BufferedReader(new StringReader(detailResult));
		String lines[] = detailResult.split("\\r?\\n");
		
		String DCWithOSClassificationDetail = "TrendWithOS";
		String DCWithoutOSClassificationDetail= "TrendWithoutOS";
		String AverageDCClassificationDetail = "WeightedAverage";
		
		 
		for (int i = 0 ; i< lines.length; i++){
			if (i == 0 || i ==1)
				continue;
			if (i==2){
				String after = lines[i].trim().replaceAll(" +", ",");
				//String commas = ",";
				//after = after.substring(commas.length());
				//autoWEKAClassifierListEvalString = autoWEKAClassifierListEvalString+ "," + filename+ "," + this.thresholdString + "," + after+ "," + "\n";
			}
			if (i==3){
				String after = lines[i].trim().replaceAll(" +", "\t");
				
				autoWEKAClassifierListEvalString = autoWEKAClassifierListEvalString + filename+ "\t" + this.thresholdString + "\t"+ DCWithOSClassificationDetail+ "\t" +  autoWEKAClassifier.accuracy()+ "\t" + after  +  "\n";
			}
			if (i==4){
				String after = lines[i].trim().trim().replaceAll(" +", "\t");
			
				autoWEKAClassifierListEvalString = autoWEKAClassifierListEvalString + filename+ "\t" + this.thresholdString + "\t" + DCWithoutOSClassificationDetail+ "\t" +  autoWEKAClassifier.accuracy()+"\t" + after  + "\n";
			}
			if (i==5){
				String prefix ="Weighted Avg.    ";
				String substr = lines[i].substring(prefix.length());
				String after = substr.trim().trim().replaceAll(" +", "\t");
				
				autoWEKAClassifierListEvalString = autoWEKAClassifierListEvalString  + filename + "\t" + this.thresholdString+ "\t"  + AverageDCClassificationDetail+ "\t"  + autoWEKAClassifier.accuracy()+ "\t" + after;
			}
		}
		return;
	}

	boolean autoClassifyTraining(int counter) {

		File f = new File(filename);
		boolean rtCode = false;
		String absolutePath = "";
		try {
			absolutePath = f.getCanonicalPath();
		} catch (IOException e2) {

			e2.printStackTrace();
		}
		// System.out.println("File path : " + absolutePath);

		String fileNames = absolutePath.substring(absolutePath.lastIndexOf(File.separator) + 1,
				absolutePath.length() - 4);

		myAutoWeka autoWEKAClassifierTemp = new myAutoWeka();

		Instances trainingInstancesAutoTemp = new Instances(trainingInstancesAuto, 0, trainingInstancesAuto.size());
		trainingInstancesAutoTemp.setClassIndex(trainingInstancesAutoTemp.numAttributes() - 1);
	//	if (trainingInstancesAutoTemp.size() <15)
		//	return rtCode;
		try {
			
			autoWEKAClassifierTemp.setParallelRuns(1);
			autoWEKAClassifierTemp.setMemLimit(1024);
			// autoWEKAClassifierTemp.setMemLimit(5);
			autoWEKAClassifierTemp.setTimeLimit(1);
			autoWEKAClassifierTemp.setSeed(123);
			 // Default K-fold =  10 . Sample size is smaller we set to sample size
			  if (trainingInstancesAutoTemp.size() < 3) {
				  autoWEKAClassifierTemp.setResamplingArgs(trainingInstancesAutoTemp.size()-3);
			  }
			 
			// System.out.println(autoWEKAClassifierTemp.getOptions().toString());
			autoWEKAClassifierTemp.setDoNotCheckCapabilities(false);
			autoWEKAClassifierTemp.setBespokePath(fileNames+"_" + regressionAlgoName + "_" + thresholdString + counter + "_");
			autoWEKAClassifierTemp.buildClassifier(trainingInstancesAutoTemp);

			autoWEKAClassifierList.add(autoWEKAClassifierTemp);

			tempFilePath.add(autoWEKAClassifierTemp.getmsExperimentPaths());
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("An error occured");
			rtCode = false;
		}

		String[] msExperimentPath = Arrays.copyOf(autoWEKAClassifierTemp.getmsExperimentPathsArray(),
				autoWEKAClassifierTemp.getmsExperimentPathsArray().length);
		System.out.println("msExperimentPath[0]" + msExperimentPath[0]);
		return rtCode;
	}

	public void classifyTraining(String filename, String thresholdString, String kfold) {

		File f = new File(filename);
		Instances trainingInstances = null;
		String modelName = currentModel.getClass().getName();
		String modelNameArray[] = modelName.split("\\.");
		String cname = modelNameArray[modelNameArray.length - 1];

		// System.out.println(cname);

		String absolutePath = "";
		try {
			absolutePath = f.getCanonicalPath();
		} catch (IOException e2) {

			e2.printStackTrace();
		}
		// System.out.println("File path : " + absolutePath);

		String folderName = absolutePath.substring(0, absolutePath.lastIndexOf(File.separator));
		String fileName = absolutePath.substring(absolutePath.lastIndexOf(File.separator) + 1,
				absolutePath.length() - 4);
		// System.out.println("File path : " + folderName);

		BufferedReader breader = null;
		CostMatrix costMatrix = null;
		try {
			costMatrix = new CostMatrix(new BufferedReader(new FileReader(folderName + "/costMatrix.txt")));
		} catch (FileNotFoundException e1) {

			e1.printStackTrace();
		} catch (Exception e1) {

			e1.printStackTrace();
		}
		try {
			String trainingDataset = null;
			if (kfold.compareToIgnoreCase("kfold") == 0) {
				trainingDataset = folderName + "/" + fileName+regressionAlgoName + "_CompleteEventFeatures_" + thresholdString + ".arff";
			} else {
				trainingDataset = folderName + "/" + fileName+regressionAlgoName + "_Features_" + thresholdString + "_training.arff";
			}

			breader = new BufferedReader(new FileReader(trainingDataset));
			trainingInstances = new Instances(breader);
			trainingInstances.setClassIndex(trainingInstances.numAttributes() - 1);
			breader.close();

			int[] stats = trainingInstances.attributeStats(trainingInstances.classIndex()).nominalCounts;

			// System.out.println(stats[0]); for print stats

			// System.out.println(train.numInstances() + " " +
			// testing.numInstances());

			currentModel.buildClassifier(trainingInstances);

			Evaluation eval = new Evaluation(trainingInstances, costMatrix);

			Random rand = new Random(3); // using seed = 1
			int folds = 10;
			eval.crossValidateModel(currentModel, trainingInstances, folds, rand);

			classifierEvaluationMap.put(new Classifier[] { currentModel }, new Evaluation[] { eval });

		} catch (FileNotFoundException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		} catch (Exception e) {

			e.printStackTrace();
		}
		;

	}

	public static BufferedReader readDataFile(String filename) {
		BufferedReader inputReader = null;

		try {
			inputReader = new BufferedReader(new FileReader(filename));
		} catch (FileNotFoundException ex) {
			System.err.println("File not found: " + filename);
		}

		return inputReader;
	}

	public void deleteTemporaryFiles(String filename, String thresholdString2) {
		File f = new File(filename);

		String absolutePath = f.getAbsolutePath();
		// System.out.println("File path : " + absolutePath);

		String folderName = absolutePath.substring(0, absolutePath.lastIndexOf(File.separator));
		String fileName = absolutePath.substring(absolutePath.lastIndexOf(File.separator) + 1,
				absolutePath.length() - 4);

		// System.out.println("File path : " + folderName);

		File file = new File(folderName + "/" + fileName+regressionAlgoName + "_CompleteEventFeatures_" + thresholdString + ".arff");

		if (file.delete()) {
			;// System.out.println("File deleted successfully");
		} else {
			System.out.println("Failed to delete the file :" + file.getAbsolutePath());
		}

		File file2 = new File(folderName + "/" + fileName+regressionAlgoName + "_Features_" + thresholdString + "_training.arff");

		if (file2.delete()) {
			;// System.out.println("File2 deleted successfully");
		} else {
			System.out.println("Failed to delete the file: " + file2.getAbsolutePath());
		}

		File file3 = new File(folderName + "/" + fileName+regressionAlgoName + "_Features_" + thresholdString + "_test.arff");

		if (file3.delete()) {
			;// System.out.println("File3 deleted successfully");
		} else {
			System.out.println("Failed to delete the file3: " + file3.getAbsolutePath());
		}
	}

	String getWekaString(int k, Event event) {
		String features;

		if (event.sigma.compareToIgnoreCase("NaN") == 0) {
			// System.out.println(events[k].sigma);
			event.sigma = "-1";
		}

		if (event.sigma.compareToIgnoreCase("Infinity") == 0) {
			// System.out.println(events[k].sigma);
			event.sigma = "-1";
		}

		if (k == 0) {
			event.PreviousDCPrice = "0.0";
			event.previousDChadOvershoot = "no";
			// event._2previousDChadOvershoot = "no";
		}

		if (event.start == event.end)
			event.isFlashEvent = "yes";
		
		if (event.hasOverShoot.isEmpty())
			return "";
		if (k == 0) {
			features = event.percentageDeltaPrice + "," + event.percentageDeltaDuration
					+ /* "," + events[k].type +"," + 0 +"," + 0+ */"," + event.sigma + "," + event.PreviousDCPrice + ","
					+ event.previousDChadOvershoot + ","
					+ /* event._2previousDChadOvershoot+ ","+ */event.isFlashEvent + "," + event.hasOverShoot;
		} else {
			features = event.percentageDeltaPrice + "," + event.percentageDeltaDuration
					+ /*
						 * "," + events[k].type +"," + events[k-1].osv_ext +","+
						 * this.events[k-1].tmv+
						 */ "," + event.sigma + "," + event.PreviousDCPrice + "," + event.previousDChadOvershoot + ","
					+ /* ((k<=1)? "no":event._2previousDChadOvershoot)+ ","+ */ event.isFlashEvent + ","
					+ event.hasOverShoot;
		}

		return features;
	}

	public void printTrainingFeatures(String filename, String thresholdString, Event[] events) {

		File f = new File(filename);

		String absolutePath = "";
		try {
			absolutePath = f.getCanonicalPath();
		} catch (IOException e1) {

			e1.printStackTrace();
		}
		// System.out.println("File path : " + absolutePath);

		String folderName = absolutePath.substring(0, absolutePath.lastIndexOf(File.separator));
		String fileName = absolutePath.substring(absolutePath.lastIndexOf(File.separator) + 1,
				absolutePath.length() - 4);
		// System.out.println("File path : " + folderName);

		Writer writer2 = null;

		try {
			writer2 = new FileWriter(
					folderName + "/" + fileName+regressionAlgoName + "_CompleteEventFeatures_" + thresholdString + ".arff", false);
			writer2.write("@relation overshootTraining");
			// System.out.println("@relation overshootTraining") ;
			// System.out.println("Training File path : " + folderName + "/"+
			// fileName + "_CompleteEventFeatures_" + thresholdString +
			// ".arff");
			writer2.write("\n");
			writer2.write("\n");
			writer2.write("@attribute PriceDifference numeric");
			writer2.write("\n");
			writer2.write("@attribute TimeDifference numeric");
			writer2.write("\n");
			// writer2.write("@attribute eventType {Upturn, Downturn}");
			// writer2.write("\n");
			// writer2.write("@attribute osv_ext numeric");
			// writer2.write("\n");
			// writer2.write("@attribute tmv numeric");
			// writer2.write("\n");
			writer2.write("@attribute sigma numeric");
			writer2.write("\n");
			// writer2.write("@attribute percentageDCEventChunck numeric");
			// writer2.write("\n");
			writer2.write("@attribute previousDCPrice numeric");
			writer2.write("\n");
			writer2.write("@attribute previousHadOvershot{yes,no}");
			writer2.write("\n");
			// writer2.write("@attribute _2previousHadOvershot{yes,no}");
			// writer2.write("\n");
			writer2.write("@attribute isFlashEvent{yes,no}");
			writer2.write("\n");
			writer2.write("@attribute hasOvershoot{yes,no}");
			writer2.write("\n");
			writer2.write("\n");
			writer2.write("@data");
			writer2.write("\n");

			for (int k = 0; k < events.length; k++) {
				// String feaures = events[k].percentageDeltaPrice +"," +
				// events[k].percentageDeltaDuration + "," + events[k].type
				// +","+ events[k].hasOverShoot ;
				// System.out.println(feaures) ;
				if (k == events.length - 1)
					writer2.write(getWekaString(k, events[k]));
				else
					writer2.write(getWekaString(k, events[k]) + "\n");
			}
			writer2.close();
		} catch (IOException e) {

			e.printStackTrace();
		}

		try {
			writer2 = new FileWriter(folderName + "/" + fileName+regressionAlgoName + "_Features_" + thresholdString + "_training.arff",
					false);
			writer2.write("@relation overshootTraining");
			// System.out.println("@relation overshootTraining") ;
			writer2.write("\n");
			writer2.write("\n");
			writer2.write("@attribute PriceDifference numeric");
			writer2.write("\n");
			writer2.write("@attribute TimeDifference numeric");
			writer2.write("\n");
			// writer2.write("@attribute eventType {Upturn, Downturn}");
			// writer2.write("\n");
			// writer2.write("@attribute osv_ext numeric");
			// writer2.write("\n");
			// writer2.write("@attribute tmv numeric");
			// writer2.write("\n");
			writer2.write("@attribute sigma numeric");
			writer2.write("\n");
			// writer2.write("@attribute percentageOvershootInChunck numeric");
			// writer2.write("\n");
			// writer2.write("@attribute percentageDCEventChunck numeric");
			// writer2.write("\n");
			writer2.write("@attribute previousDCPrice numeric");
			writer2.write("\n");
			writer2.write("@attribute previousHadOvershot{yes,no}");
			writer2.write("\n");
			// writer2.write("@attribute _2previousHadOvershot{yes,no}");
			// writer2.write("\n");
			writer2.write("@attribute isFlashEvent{yes,no}");
			writer2.write("\n");
			writer2.write("@attribute hasOvershoot{yes,no}");
			writer2.write("\n");
			writer2.write("\n");
			writer2.write("@data");
			writer2.write("\n");

			// for (int k = 0; k < trainingCount; k++) {
			for (int k = 0; k < events.length; k++) {

				if (k == events.length - 1)
					writer2.write(getWekaString(k, events[k]));
				else
					writer2.write(getWekaString(k, events[k]) + "\n");
			}
			writer2.close();
			// System.out.println("Number of training set:" + trainNum);
		} catch (IOException e) {

			e.printStackTrace();
		}
	}

	public void extractTrainingDCfeatures(Event[] events) {

		for (int k = 0; k < events.length; k++) {
			int start = events[k].start;
			int end = events[k].end;
			// FReader freader= new FReader();
			// FReader.FileMember2 fileMember2= freader.new FileMember2();

			events[k].startDate = FReader.dataRecordInFileArray.get(start).Day;
			events[k].endDate = FReader.dataRecordInFileArray.get(end).Day;
			events[k].startPrice = FReader.dataRecordInFileArray.get(start).price;
			events[k].endPrice = FReader.dataRecordInFileArray.get(end).price;
			events[k].startTime = FReader.dataRecordInFileArray.get(start).time;
			events[k].endTime = FReader.dataRecordInFileArray.get(end).time;

			if (k > 0) {
				if (events[k - 1].overshoot != null && events[k - 1].overshoot.length() > 0)
					events[k].previousDChadOvershoot = "yes";

				events[k].PreviousDCPrice = events[k - 1].startPrice;
			} else {
				events[k].previousDChadOvershoot = "no";
				events[k].PreviousDCPrice = "0.0";
			}

			/*
			 * if (k>1) { if (events[k-2].overshoot != null &&
			 * events[k-2].overshoot.length()>0)
			 * events[k]._2previousDChadOvershoot= "yes"; } else {
			 * events[k]._2previousDChadOvershoot ="no"; }
			 */

			if (events[k].start == events[k].end)
				events[k].isFlashEvent = "yes";

			double endPrice = Double.parseDouble(FReader.dataRecordInFileArray.get(end).price);
			double startPrice = Double.parseDouble(FReader.dataRecordInFileArray.get(start).price);

			double price = Math.abs(((endPrice - startPrice) / startPrice) / thresholdDbl);
			double percentagePriceDisplacement = price; // (price / endPrice)
														// *100.0;
			DecimalFormat df = new DecimalFormat("#.#################");
			String value = Double.toString(Double.parseDouble(df.format(percentagePriceDisplacement)));

			double osPrice = 0.0;
			double osEndPriceDbl = 0.0;
			String osEndPrice = "";
			double currentTmv = 0.0;

			String string[] = events[k].endDate.split("\\.");
			String endYear = string[2].replaceFirst("^0+(?!$)", "");
			String endMonth = string[1].replaceFirst("^0+(?!$)", "");
			String endDay = string[0].replaceFirst("^0+(?!$)", "");
			Arrays.fill(string, null);
			string = events[k].startDate.split("\\.");

			String startYear = string[2].replaceFirst("^0+(?!$)", "");
			String startMonth = string[1].replaceFirst("^0+(?!$)", "");
			String startDay = string[0].replaceFirst("^0+(?!$)", "");

			Arrays.fill(string, null);
			string = events[k].startTime.split(":");

			String startHour = string[2].replaceFirst("^0+(?!$)", "");
			String startMinute = string[1].replaceFirst("^0+(?!$)", "");
			String startSeconds = string[0].replaceFirst("^0+(?!$)", "");

			Arrays.fill(string, null);
			string = events[k].endTime.split(":");

			String endHour = string[2].replaceFirst("^0+(?!$)", "");
			String endMinute = string[1].replaceFirst("^0+(?!$)", "");
			String endSeconds = string[0].replaceFirst("^0+(?!$)", "");

			Calendar endCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			endCalendar.clear();
			endCalendar.set(Integer.parseInt(endYear), Integer.parseInt(endMonth) - 1, Integer.parseInt(endDay),
					Integer.parseInt(endHour), Integer.parseInt(endMinute), Integer.parseInt(endSeconds));
			long endSecondsSinceEpoch = endCalendar.getTimeInMillis() / 1000L;

			Calendar startCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			startCalendar.clear();
			startCalendar.set(Integer.parseInt(startYear), Integer.parseInt(startMonth) - 1, Integer.parseInt(startDay),
					Integer.parseInt(startHour), Integer.parseInt(startMinute), Integer.parseInt(startSeconds));
			long startSecondsSinceEpoch = startCalendar.getTimeInMillis() / 1000L;

			double timeDiff = Math.abs(endSecondsSinceEpoch - startSecondsSinceEpoch);
			// double percentageTimeDisplacement = (
			// timeDiff/endSecondsSinceEpoch) * 100;
			double _10MinsTimeDifference = Math.round(timeDiff / 60.0);

			double _TT10MinsTimeDifference = 0.0;
			if (events[k].overshoot != null && events[k].overshoot.length() > 0) {
				int osStart = events[k].overshoot.start;
				String osStartPrice = FReader.dataRecordInFileArray.get(osStart).price;

				int osEnd = events[k].overshoot.end;
				osEndPrice = FReader.dataRecordInFileArray.get(osEnd).price;
				osEndPriceDbl = Double.parseDouble(osEndPrice);
				double osStartPriceDbl = Double.parseDouble(osStartPrice);

				osPrice = Math.abs(((osEndPriceDbl - osStartPriceDbl) / osStartPriceDbl) / thresholdDbl);
				currentTmv = Math.abs(((osEndPriceDbl - startPrice) / startPrice) / thresholdDbl);

				int overshootEnd = events[k].overshoot.end;
				events[k].overshoot.endDate = FReader.dataRecordInFileArray.get(overshootEnd).Day;

				events[k].overshoot.endPrice = FReader.dataRecordInFileArray.get(overshootEnd).price;

				events[k].overshoot.endTime = FReader.dataRecordInFileArray.get(overshootEnd).time;
				Arrays.fill(string, null);
				string = events[k].overshoot.endDate.split("\\.");

				String overshootEndYear = string[2].replaceFirst("^0+(?!$)", "");
				String overshootEndMonth = string[1].replaceFirst("^0+(?!$)", "");
				String overshootEndDay = string[0].replaceFirst("^0+(?!$)", "");

				Arrays.fill(string, null);
				string = events[k].overshoot.endTime.split(":");

				String overshootEndHour = string[2].replaceFirst("^0+(?!$)", "");
				String overshootEndMinute = string[1].replaceFirst("^0+(?!$)", "");
				String overshootEndSeconds = string[0].replaceFirst("^0+(?!$)", "");

				Calendar overshootEndCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
				overshootEndCalendar.clear();
				overshootEndCalendar.set(Integer.parseInt(overshootEndYear), Integer.parseInt(overshootEndMonth) - 1,
						Integer.parseInt(overshootEndDay), Integer.parseInt(overshootEndHour),
						Integer.parseInt(overshootEndMinute), Integer.parseInt(overshootEndSeconds));

			} else {
				currentTmv = Math.abs(((endPrice - startPrice) / startPrice) / thresholdDbl);

			}
			_TT10MinsTimeDifference = _10MinsTimeDifference;
			events[k].osv_ext = Double.toString(Double.parseDouble(df.format(osPrice)));
			events[k].percentageDeltaPrice = value;

			events[k].tmv = Double.toString(Double.parseDouble(df.format(currentTmv)));
			events[k].sigma = Double.toString((currentTmv * thresholdDbl) / _TT10MinsTimeDifference);

			if (events[k].sigma.compareToIgnoreCase("NaN") == 0) {
				// System.out.println(this.events[k].sigma);
				events[k].sigma = "-1";
			}

			if (events[k].overshoot != null)
				events[k].hasOverShoot = "yes";

			if (events[k].overshoot != null && value == "0.0")
				System.out.println("X");

			events[k].datapoints = new ArrayList<FReader.FileMember2>(
					FReader.dataRecordInFileArray.subList(events[k].start, events[k].end + 1));

			String timeValue = Double.toString(Double.parseDouble(df.format(_10MinsTimeDifference / 10)));

			events[k].percentageDeltaDuration = timeValue;

			// System.out.println((endSecondsSinceEpoch -
			// startSecondsSinceEpoch) + " " + timeValue + " priceValue " +
			// value );
			// System.out.println("StartTime " + " " + events[k].startTime+ "
			// endTime " + events[k].endTime);
			// System.out.println(events[k].startDate + " " +events[k].endDate +
			// " " + events[k].startPrice + "" + events[k].endPrice + " " +
			// events[k].DCValue );
		}

	}

	public void printTestFeatures(String filename, String thresholdString, Event[] testEvents) {

		File f = new File(filename);

		String absolutePath = "";
		try {
			absolutePath = f.getCanonicalPath();
		} catch (IOException e1) {

			e1.printStackTrace();
		}
		// System.out.println("File path : " + absolutePath);

		String folderName = absolutePath.substring(0, absolutePath.lastIndexOf(File.separator));
		String fileName = absolutePath.substring(absolutePath.lastIndexOf(File.separator) + 1,
				absolutePath.length() - 4);
		// System.out.println("File path : " + folderName);

		Writer writer2 = null;
		int count = 0;

		try {
			writer2 = new FileWriter(folderName + "/" + fileName+regressionAlgoName + "_Features_" + thresholdString + "_test.arff",
					false);
			writer2.write("@relation overshootTraining");
			// System.out.println("@relation overshootTraining") ;
			writer2.write("\n");
			writer2.write("\n");
			writer2.write("@attribute PriceDifference numeric");
			writer2.write("\n");
			writer2.write("@attribute TimeDifference numeric");
			writer2.write("\n");
			// writer2.write("@attribute eventType {Upturn, Downturn}");
			// writer2.write("\n");
			// writer2.write("@attribute osv_ext numeric");
			// writer2.write("\n");
			// writer2.write("@attribute tmv numeric");
			// writer2.write("\n");
			writer2.write("@attribute sigma numeric");
			writer2.write("\n");
			// writer2.write("@attribute percentageOvershootInChunck numeric");
			// writer2.write("\n");
			// writer2.write("@attribute percentageDCEventChunck numeric");
			// writer2.write("\n");
			writer2.write("@attribute previousDCPrice numeric");
			writer2.write("\n");
			writer2.write("@attribute previousHadOvershot{yes,no}");
			writer2.write("\n");
			// writer2.write("@attribute _2previousHadOvershot{yes,no}");
			// writer2.write("\n");
			writer2.write("@attribute isFlashEvent{yes,no}");
			writer2.write("\n");
			writer2.write("@attribute hasOvershoot{yes,no}");
			writer2.write("\n");
			writer2.write("\n");
			writer2.write("@data");
			writer2.write("\n");
			
			// for (int k = 0; k < trainingCount; k++) {
			for (int k = 0; k < testEvents.length; k++) {
				count = count + 1;
				String feature;
				if (k == testEvents.length-1)
					feature = getWekaString(k, testEvents[k]);
				else
					feature = getWekaString(k, testEvents[k]) + "\n";
				if (k== testEvents.length){
					System.out.print(" ");	
				}
				if (feature.isEmpty())
					continue;
				
				boolean isFound = feature.indexOf(",,,,") !=-1? true: false;
				if (isFound)
				{
					String replaceString=feature.replace(",,,,","0,0,0,0,");//replaces all 
					writer2.write(replaceString);
				}
				else
				{
					if (!feature.isEmpty())
						writer2.write(feature);
				}
				
				
			}
			writer2.close();
			System.out.println("Wrote test set for threshold:" + thresholdDbl);
		} catch (IOException e) {

			e.printStackTrace();
		}
		// System.out.println(count + " Rows in file");
		
		
	}

	public void selectBestClassifier() {
		double fmeasure = 0.0;
		Iterator<Entry<Classifier[], Evaluation[]>> it = classifierEvaluationMap.entrySet().iterator();
		while (it.hasNext()) {

			Map.Entry<Classifier[], Evaluation[]> pair = (Map.Entry<Classifier[], Evaluation[]>) it.next();
			// System.out.println(pair.getKey() + " = " + pair.getValue());
			Evaluation eval = (Evaluation) pair.getValue()[0];
			Classifier classifier = (Classifier) pair.getKey()[0];
			// (Person)entry.getValue()[i]
			// Evaluation[] evalArray
			// it.remove(); // avoids a ConcurrentModificationException
			double pricisionDouble = 0.0;

			if (eval.numTruePositives(0) != 0.0)
				pricisionDouble = ((eval.numTruePositives(0)
						/ (eval.numTruePositives(0) + eval.numFalsePositives(0))) == Double.NaN) ? 0.0
								: (eval.numTruePositives(0) / (eval.numTruePositives(0) + eval.numFalsePositives(0)));
			else
				pricisionDouble = 0.0;
			if (Double.isNaN(pricisionDouble))
				System.out.println("XXX" + " " + eval.numTruePositives(0));

			// System.out.println(eval.confusionMatrix()[0][0]); // TP
			// System.out.println(eval.confusionMatrix()[0][1]); // FN
			// System.out.println(eval.confusionMatrix()[1][0]); // FP
			// System.out.println(eval.confusionMatrix()[1][1]); // TN

			double accuracy = ((eval.confusionMatrix()[0][0] + eval.confusionMatrix()[1][1])
					/ (eval.confusionMatrix()[0][1] + eval.confusionMatrix()[1][0] + eval.confusionMatrix()[0][0]
							+ eval.confusionMatrix()[1][1]));
			if (Double.isNaN(accuracy))
				accuracy = 0.0;

			double precision = (eval.confusionMatrix()[0][0])
					/ (eval.confusionMatrix()[0][0] + eval.confusionMatrix()[1][0]);
			if (Double.isNaN(precision))
				precision = 0.0;

			double recall = (eval.confusionMatrix()[0][0])
					/ (eval.confusionMatrix()[0][0] + eval.confusionMatrix()[0][1]);

			if (Double.isNaN(recall))
				recall = 0.0;
			// System.out.println(eval.toSummaryString());

			/*
			 * try { System.out.println(eval.toMatrixString()); } catch
			 * (Exception e) {
			 * 
			 * e.printStackTrace(); }
			 */

			double fmeasureCopy = 5.0 * (((precision) * (recall)) / ((4 * precision) + (recall)));// ((2*((pricisionDouble*
																									// recall)/(pricisionDouble+
																									// recall))
																									// *0.7)
																									// +
																									// (0.3*
																									// filterAccuracyDbl));
			if (fmeasureCopy > fmeasure) {
				fmeasure = fmeasureCopy;
				DCCurveClassifier = new Classifier[] { (Classifier) pair.getKey()[0] };
				DCCurveEvaluation = new Evaluation[] { (Evaluation) pair.getValue()[0] };
			}
		}

		// String modelName = DCCurveClassifier[0].getClass().getName();
		// System.out.println("Best model for "+ filename+ " threshold " +
		// thresholdString + " is:" + modelName);

	}

	public void classifyTestData() {
		for (int eventCount = 0; eventCount < testInstances.numInstances(); eventCount++) {

			Double clsLabel = 0.0;
			try {
				if (autoWEKAClassifier == null)
					clsLabel =0.0;
				else
					clsLabel = autoWEKAClassifier.classifyInstance(testInstances.instance(eventCount));
				// clsLabel =
				// DCCurveClassifier[0].classifyInstance(testInstances.instance(eventCount));
			} catch (Exception e) {

				e.printStackTrace();
			}
			System.out.println(clsLabel.toString());
			testInstances.instance(eventCount).setClassValue(clsLabel);
			testInstances.instance(eventCount).stringValue(testInstances.attribute(testInstances.numAttributes() - 1));
			// System.out.println(test.attribute(test.numAttributes() - 3) + " "
			// + test.attribute(test.numAttributes() - 2) + " " +"prediction
			// "+test.instance(eventCount).stringValue(test.attribute(test.numAttributes()
			// - 1)));
		}
	}

	public String printPreprocessClassificationTraining(Event[] trendEvent) {

		if (autoWEKAClassifier == null)
		{
			System.out.print("classifier not generated");
			return "";
		}
		int totalMissedOvershoot = 0;
		double possibleovershootLength = 0.0;
		double overshootLength = 0.0;
		int totalAssumedOvershoot = 0;
		int totalFoundOvershoot = 0;
		double foundOvershootLength = 0.0;
		double testFalsePositive = 0.0;
		double testFalseNegative = 0.0;
		double testTruePositive = 0.0;
		double testTrueNegative = 0.0;
		// String classificationStr =
		// preprocess.test.instance(outputIndex).stringValue(preprocess.test.attribute(preprocess.test.numAttributes()
		// - 1));

		File f = new File(filename);
		Instances trainingInstances1 = null;
		String absolutePath = "";
		try {
			absolutePath = f.getCanonicalPath();
		} catch (IOException e3) {

			e3.printStackTrace();
		}
		// System.out.println("File path : " + absolutePath);

		String folderName = absolutePath.substring(0, absolutePath.lastIndexOf(File.separator));
		String fileName = absolutePath.substring(absolutePath.lastIndexOf(File.separator) + 1,
				absolutePath.length() - 4);

		BufferedReader breader = null;
		try {
			breader = new BufferedReader(
					new FileReader(folderName + "/" + fileName+regressionAlgoName + "_Features_" + thresholdString + "_training.arff"));
		} catch (FileNotFoundException e2) {
			e2.printStackTrace();
		}

		try {
			trainingInstances1 = new Instances(breader);
			breader.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		trainingInstances1.setClassIndex(trainingInstances1.numAttributes() - 1);

		for (int eventCount = 0; eventCount < trainingInstances1.numInstances(); eventCount++) {
			int os = 0;

			double clsLabel = 0.0;
			try {
				clsLabel = autoWEKAClassifier.classifyInstance(trainingInstances1.instance(eventCount));
				// clsLabel =
				// DCCurveClassifier[0].classifyInstance(testInstances.instance(eventCount));
			} catch (Exception e) {
				e.printStackTrace();
			}
			String beforePredict = trainingInstances1.instance(eventCount)
					.stringValue(trainingInstances1.attribute(trainingInstances1.numAttributes() - 1));
			trainingInstances1.instance(eventCount).setClassValue(clsLabel);
			trainingInstances1.instance(eventCount).stringValue(trainingInstances1.attribute(trainingInstances1.numAttributes() - 1));
			// System.out.println("Before prediction," + beforePredict + ","+
			// "After
			// prediction,"+testInstances.instance(eventCount).stringValue(testInstances.attribute(testInstances.numAttributes()
			// - 1))
			// + ",fresh from DB," +
			// testInstances1.instance(eventCount).stringValue(testInstances.attribute(testInstances.numAttributes()
			// - 1)) );
		}

		for (int eventCount = 1; eventCount < trainingInstances1.numInstances(); eventCount++) {
			int os = 0;

			/*
			 * if (trendEvent.length-1 != test.numInstances()) {
			 * System.out.println("Event and prediction not equal");
			 * System.exit(0); }
			 */
			if (trendEvent[eventCount].overshoot != null)
				os = trendEvent[eventCount].overshoot.length();

			String classificationStringValue = trainingInstances1.instance(eventCount)
					.stringValue(trainingInstances1.attribute(trainingInstances1.numAttributes() - 1));

			if (classificationStringValue.compareToIgnoreCase("yes") == 0
					&& (trendEvent[eventCount].overshoot != null && trendEvent[eventCount].overshoot.length() > 0)) {
				testTruePositive = testTruePositive + 1;
				possibleovershootLength = possibleovershootLength + 1;

				totalFoundOvershoot = totalFoundOvershoot + 1;
				foundOvershootLength = foundOvershootLength + trendEvent[eventCount].overshoot.length();
			}

			if (classificationStringValue.compareToIgnoreCase("no") == 0
					&& (trendEvent[eventCount].overshoot == null || trendEvent[eventCount].overshoot.length() == 0))
				testTrueNegative = testTrueNegative + 1;

			if (classificationStringValue.compareToIgnoreCase("no") == 0
					&& (trendEvent[eventCount].overshoot != null && trendEvent[eventCount].overshoot.length() > 0)) {
				testFalseNegative = testFalseNegative + 1;
				totalMissedOvershoot = totalMissedOvershoot + 1;
				overshootLength = overshootLength + trendEvent[eventCount].overshoot.length();
				possibleovershootLength = possibleovershootLength + 1;
			}

			if (classificationStringValue.compareToIgnoreCase("yes") == 0
					&& (trendEvent[eventCount].overshoot == null || trendEvent[eventCount].overshoot.length() == 0)) {
				testFalsePositive = testFalsePositive + 1;
				totalAssumedOvershoot = totalAssumedOvershoot + 1;
			}

		}

				String classificationresult =  filename + "\t" +  thresholdString  + "\t" + totalMissedOvershoot + "\t" + overshootLength + "\t" + totalAssumedOvershoot
				+ "\t" + possibleovershootLength + "\t" + foundOvershootLength + "\t" + totalFoundOvershoot + "\t"
				+ trainingInstances1.numInstances() + "\t" + bestAccuracy  + "\t" + bestRecall + "\t" +  bestPrecision  + "\t" +  bestFMeasure ;

			
		return classificationresult;

	}
	
	
	public String printPreprocessClassification(Event[] trendEvent) {

		int totalMissedOvershoot = 0;
		double possibleovershootLength = 0.0;
		double overshootLength = 0.0;
		int totalAssumedOvershoot = 0;
		int totalFoundOvershoot = 0;
		double foundOvershootLength = 0.0;
		double testFalsePositive = 0.0;
		double testFalseNegative = 0.0;
		double testTruePositive = 0.0;
		double testTrueNegative = 0.0;
		// String classificationStr =
		// preprocess.test.instance(outputIndex).stringValue(preprocess.test.attribute(preprocess.test.numAttributes()
		// - 1));

		File f = new File(filename);
		Instances testInstances1 = null;
		String absolutePath = "";
		try {
			absolutePath = f.getCanonicalPath();
		} catch (IOException e3) {

			e3.printStackTrace();
		}
		// System.out.println("File path : " + absolutePath);

		String folderName = absolutePath.substring(0, absolutePath.lastIndexOf(File.separator));
		String fileName = absolutePath.substring(absolutePath.lastIndexOf(File.separator) + 1,
				absolutePath.length() - 4);

		BufferedReader breader = null;
		try {
			breader = new BufferedReader(
					new FileReader(folderName + "/" + fileName+regressionAlgoName + "_Features_" + thresholdString + "_test.arff"));
		} catch (FileNotFoundException e2) {
			e2.printStackTrace();
		}

		try {
			testInstances1 = new Instances(breader);
			breader.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		testInstances1.setClassIndex(testInstances1.numAttributes() - 1);

		for (int eventCount = 0; eventCount < testInstances.numInstances(); eventCount++) {
			int os = 0;

			double clsLabel = 0.0;
			try {
				clsLabel = autoWEKAClassifier.classifyInstance(testInstances.instance(eventCount));
				// clsLabel =
				// DCCurveClassifier[0].classifyInstance(testInstances.instance(eventCount));
			} catch (Exception e) {
				e.printStackTrace();
			}
			String beforePredict = testInstances.instance(eventCount)
					.stringValue(testInstances.attribute(testInstances.numAttributes() - 1));
			testInstances.instance(eventCount).setClassValue(clsLabel);
			testInstances.instance(eventCount).stringValue(testInstances.attribute(testInstances.numAttributes() - 1));
			// System.out.println("Before prediction," + beforePredict + ","+
			// "After
			// prediction,"+testInstances.instance(eventCount).stringValue(testInstances.attribute(testInstances.numAttributes()
			// - 1))
			// + ",fresh from DB," +
			// testInstances1.instance(eventCount).stringValue(testInstances.attribute(testInstances.numAttributes()
			// - 1)) );
		}
		if (trendEvent.length != testInstances.size()){
			System.out.println("Unable to print classification result. Event and test instance does not match");
			System.exit(-1);
		}
			
		for (int eventCount = 1; eventCount < testInstances.numInstances(); eventCount++) {
			int os = 0;

			/*
			 * if (trendEvent.length-1 != test.numInstances()) {
			 * System.out.println("Event and prediction not equal");
			 * System.exit(0); }
			 */
			if (trendEvent[eventCount].overshoot != null)
				os = trendEvent[eventCount].overshoot.length();

			String classificationStringValue = testInstances.instance(eventCount)
					.stringValue(testInstances.attribute(testInstances.numAttributes() - 1));

			if (classificationStringValue.compareToIgnoreCase("yes") == 0
					&& (trendEvent[eventCount].overshoot != null && trendEvent[eventCount].overshoot.length() > 0)) {
				testTruePositive = testTruePositive + 1;
				possibleovershootLength = possibleovershootLength + 1;

				totalFoundOvershoot = totalFoundOvershoot + 1;
				foundOvershootLength = foundOvershootLength + trendEvent[eventCount].overshoot.length();
			}

			if (classificationStringValue.compareToIgnoreCase("no") == 0
					&& (trendEvent[eventCount].overshoot == null || trendEvent[eventCount].overshoot.length() == 0))
				testTrueNegative = testTrueNegative + 1;

			if (classificationStringValue.compareToIgnoreCase("no") == 0
					&& (trendEvent[eventCount].overshoot != null && trendEvent[eventCount].overshoot.length() > 0)) {
				testFalseNegative = testFalseNegative + 1;
				totalMissedOvershoot = totalMissedOvershoot + 1;
				overshootLength = overshootLength + trendEvent[eventCount].overshoot.length();
				possibleovershootLength = possibleovershootLength + 1;
			}

			if (classificationStringValue.compareToIgnoreCase("yes") == 0
					&& (trendEvent[eventCount].overshoot == null || trendEvent[eventCount].overshoot.length() == 0)) {
				testFalsePositive = testFalsePositive + 1;
				totalAssumedOvershoot = totalAssumedOvershoot + 1;
			}

		}

		Evaluation eval = null;
		String evalString = "";
		
		try {
			eval = new Evaluation(testInstances1);
			eval.evaluateModel(autoWEKAClassifier, testInstances1);
			evalString = eval.toClassDetailsString();
			
	
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		String lines[] = evalString.split("\\r?\\n");
		String precision = "";
		String recall = "";
		String  fmeasure = "";
		for (int detailClassResultCount = 0 ; detailClassResultCount< lines.length; detailClassResultCount++){
			if (detailClassResultCount == 0 || detailClassResultCount ==1 
					|| detailClassResultCount==2 || detailClassResultCount==4 ||
					detailClassResultCount==5)
				continue;
			
			if (detailClassResultCount==3){
				String after = lines[detailClassResultCount].trim().replaceAll(" +", ",");
				String DCWithOSClassification[] = after.split(",");
				precision = DCWithOSClassification[2];
				recall = DCWithOSClassification[3];
				fmeasure = DCWithOSClassification[4] ; //2*((Double.parseDouble(precision) * Double.parseDouble(recall)) / (Double.parseDouble(precision) + Double.parseDouble(recall) ));
			}
		}
			
		double filterTestPrecisionDbl = (testTruePositive / (testTruePositive + testFalsePositive));

		if (Double.isNaN(filterTestPrecisionDbl))
			filterTestPrecisionDbl = 0.0;

		double filterTestAccuracyDbl = (testTruePositive + testTrueNegative)
				/ (testTruePositive + testTrueNegative + testFalsePositive + testFalseNegative);

		if (Double.isNaN(filterTestAccuracyDbl))
			filterTestAccuracyDbl = 0.0;

		double filterTestRecallDbl = (testTruePositive / (testTruePositive + testFalseNegative));
		if (Double.isNaN(filterTestRecallDbl))
			filterTestRecallDbl = 0.0;


		String classificationresult = filename + "\t" +  thresholdString + "\t" +  totalMissedOvershoot + "\t" + overshootLength + "\t" + totalAssumedOvershoot
				+ "\t" + possibleovershootLength + "\t" + foundOvershootLength + "\t" + totalFoundOvershoot + "\t"
				+ testInstances.numInstances() + "\t" + eval.pctCorrect() + "\t" + precision + "\t"
				+ recall  + "\t" + fmeasure;

		return classificationresult;

	}

	
	public String classifyTestInstance(int instanceCount) {

		Double clsLabel = 0.0;
		try {

			if (useAuto) {
				Instances cpy = testInstances;

				//System.out.println("instance num " + testInstances.numInstances() + " counter" + instanceCount);
				clsLabel = autoWEKAClassifier.classifyInstance(testInstances.instance(instanceCount));
			} else
				clsLabel = DCCurveClassifier[0].classifyInstance(testInstances.instance(instanceCount));

			clsLabel.toString();

			// System.out.println(
			// DCCurveClassifier[0].classifyInstance(testInstances.instance(instanceCount))
			// +
			// " " +testInstances.instance(instanceCount));
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (clsLabel.toString().compareToIgnoreCase("0.0") == 0) {
			return "yes";
		} else
			return "no";

	}

	public String classifyTrainingInstance(int instanceCount) {

		Double clsLabel = 0.0;
		try {
			if (useAuto)
				clsLabel = autoWEKAClassifier.classifyInstance(trainingInstancesAuto.instance(instanceCount));
			else
				clsLabel = DCCurveClassifier[0].classifyInstance(trainingInstancesManual.instance(instanceCount));
			clsLabel.toString();
		} catch (Exception e) {

			e.printStackTrace();
		}

		// System.out.println("Message classified as : " +clsLabel);
		if (clsLabel.toString().compareToIgnoreCase("0.0") == 0) {
			return "yes";
		} else
			return "no";

	}

	public Instances getTestInstance() {

		return testInstances;

	}

	public Instances getTrainingInstance() {

		return trainingInstancesManual;

	}

	public Instances getTrainingInstanceAuto() {

		return trainingInstancesAuto;

	}

	public void evaluateModel() {
		try {
			DCCurveEvaluation[0].evaluateModel(DCCurveClassifier[0], testInstances);

			System.out.println("correct rate " + DCCurveEvaluation[0].correct() + " false negatives - no"
					+ DCCurveEvaluation[0].numFalseNegatives(testInstances.numAttributes() - 1) + " true negative - no"
					+ DCCurveEvaluation[0].numTrueNegatives(1) + "  true positive - yes"
					+ DCCurveEvaluation[0].numTruePositives(1) + "  false positive - yes"
					+ DCCurveEvaluation[0].numFalsePositives(1));
		} catch (Exception e) {

			e.printStackTrace();
		}
	}

	public void runAutoWeka() {
		autoWEKAClassifierList = new ArrayList<myAutoWeka>(3);

		File f = new File(filename);
		Instances trainingInstancesCompare = null;
		String absolutePath = "";
		try {
			absolutePath = f.getCanonicalPath();
		} catch (IOException e3) {

			e3.printStackTrace();
		}
		// System.out.println("File path : " + absolutePath);

		String folderName = absolutePath.substring(0, absolutePath.lastIndexOf(File.separator));
		String fileName = absolutePath.substring(absolutePath.lastIndexOf(File.separator) + 1,
				absolutePath.length() - 4);

		int autoWekaCount = 0;
		while (autoWekaCount < 3) {
			SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
			Date date = new Date();
			System.out.println("starting run " + autoWekaCount + " autoweka for thresholdString" + thresholdString
					+ "at:" + formatter.format(date));
			
		
			autoClassifyTraining(autoWekaCount);
			
				
				
			if (autoWekaCount > 3) {
				getBestAutoClassifier();
				deleteAllTempFilesExceptBestAutoClassifier(fileName);
			}

			autoWekaCount++;
		}

		getBestAutoClassifier();
		getAutoWekaClassifierDetailResult();
		System.out.println("Number of auto weka models" + autoWEKAClassifierList.size());

		BufferedReader breader = null;
		try {
			breader = new BufferedReader(
					new FileReader(folderName + "/" + fileName+regressionAlgoName + "_Features_" + thresholdString + "_training.arff"));
		} catch (FileNotFoundException e2) {
			e2.printStackTrace();
		}

		try {
			trainingInstancesCompare = new Instances(breader);
			breader.close();
		} catch (IOException e1) {

			e1.printStackTrace();
		}

		trainingInstancesCompare.setClassIndex(trainingInstancesCompare.numAttributes() - 1);

		Instances trainingInstancesAuto = new Instances(trainingInstancesCompare, 0, trainingInstancesCompare.size());
		trainingInstancesAuto.setClassIndex(trainingInstancesAuto.numAttributes() - 1);
		
		String tempFolderName = tempFilePath.get(0).substring(0, tempFilePath.get(0).lastIndexOf(File.separator));
		System.out.println("delete file path is: " + fileName);

		File dir = new File(tempFolderName);
		if (!dir.isDirectory())
			return;

		File[] tempFile = dir.getParentFile().listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith("autoweka" + fileName);
			}
		});

		for (int tempFileCount = 0; tempFileCount < tempFile.length; tempFileCount++) {
			try {
				deleteDirectoryRecursionJava6(tempFile[tempFileCount]);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("Unable to delete one of the directory");
			}
		}
	}

	public String printAutoWekaVsManualClassifierComparison() {
		return comparisonResult;
	}

	public String getAutoWEKAClassifierListEvalString() {
		return autoWEKAClassifierListEvalString;
	}

	public void clearAutoWEKAClassifierListEvalString() {
		autoWEKAClassifierListEvalString = "";
	}

	public void cleanupAutoWekaTemp(String tempPath, int position) {

		String folderName = tempPath.substring(0, tempPath.lastIndexOf(File.separator));
		System.out.println(folderName);

		tempPath = tempPath.substring(0, tempPath.length() - 1);
		System.out.println("Deleting Weka tmp files at" + tempPath);
		System.out.println(" 1 Number of auto weka models" + autoWEKAClassifierList.size());
		Process p1 = null;
		Path directoryPath = null;
		if (System.getProperty("os.name").contains("Windows")) {
			// Runtime.getRuntime().exec("cmd.exe /c for /d %i in
			// (c:\\Users\\atna3\\AppData\\Local\\Temp\\autoweka*) do echo %i
			// ");
			try {
				String path = tempPath.replaceAll("\\\\", "\\\\\\\\"); // need
																		// to
																		// double
																		// up to
																		// work
				directoryPath = Paths.get(path);
				// if (Files.exists(directoryPath,new
				// LinkOption[]{LinkOption.NOFOLLOW_LINKS}))
				// p1 = Runtime.getRuntime().exec("cmd.exe /c for /d %x in
				// ("+path+"*) do rd /s /q \"%x\" ");
				p1 = Runtime.getRuntime().exec("cmd.exe /c rd /s /q " + path);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			String[] cmd = { "/bin/sh", "-c", "rm -rf " + tempPath };
			try {
				p1 = Runtime.getRuntime().exec(cmd);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		try {
			p1.waitFor();
			// IF successfully deleted remove element
			// if (Files.notExists(directoryPath,new
			// LinkOption[]{LinkOption.NOFOLLOW_LINKS}))
			// tempFilePath.remove(position);
		} catch (InterruptedException e) {
			System.out.println("tmp file has been deleted");
		} catch (NullPointerException e) {
			System.out.println("tmp file has been deleted");
		}

	}

	/*
	 * public void updatetempFilePathArrayList(String tempPath, int position){
	 * tempPath = tempPath.substring(0,tempPath.length()-1); System.out.println(
	 * "Removing element:" + position + " from tempFilePath ArrayList" +
	 * " current tempFilePathArray size is :" + tempFilePath.get(position));
	 * Path directoryPath = null; if
	 * (System.getProperty("os.name").contains("Windows")){
	 * //Runtime.getRuntime().exec(
	 * "cmd.exe /c for /d %i in (c:\\Users\\atna3\\AppData\\Local\\Temp\\autoweka*) do echo %i "
	 * );
	 * 
	 * String path = tempPath.replaceAll("\\\\", "\\\\\\\\"); // need to double
	 * up to work directoryPath = Paths.get(path); if
	 * (Files.notExists(directoryPath,new
	 * LinkOption[]{LinkOption.NOFOLLOW_LINKS})) tempFilePath.remove(position);
	 * 
	 * } else { directoryPath = Paths.get(tempPath); if
	 * (Files.notExists(directoryPath,new
	 * LinkOption[]{LinkOption.NOFOLLOW_LINKS})) tempFilePath.remove(position);
	 * }
	 * 
	 * System.out.println("TempFilePathArray size now is :" + tempFilePath); }
	 */
	public void removeTempFiles() {
		// Clear up any residue;
		int tmpFolderCount = tempFilePath.size() - 1;
		for (tmpFolderCount = tempFilePath.size() - 1; tmpFolderCount >= 0; tmpFolderCount--) {

			cleanupAutoWekaTemp(tempFilePath.get(tmpFolderCount), tmpFolderCount);
		}
		/*
		 * for (tmpFolderCount= tempFilePath.size()-1; tmpFolderCount>=0;
		 * tmpFolderCount--){
		 * updatetempFilePathArrayList(tempFilePath.get(tmpFolderCount),
		 * tmpFolderCount); }
		 */

	}

	public void deleteDirectoryRecursionJava6(File file) throws IOException {
		if (file.isDirectory()) {
			File[] entries = file.listFiles();
			if (entries != null) {
				for (File entry : entries) {
					// System.out.println(" Deleting "+ file.getAbsolutePath());
					deleteDirectoryRecursionJava6(entry);
				}
			}
		}
		if (!file.delete()) {
			throw new IOException("Failed to delete " + file);
		}
	}

	public Instances getCopyOfTestInstances() {
		return new Instances(testInstances);
	}
	
	public void setCopyOfTestInstances(Instances instances) {
		testInstances = new Instances(instances);
	}
	
	public Instances getCopyOfTrainingInstancesAuto() {
		return new Instances(trainingInstancesAuto);
	}
	
	public void setCopyOfTrainingInstancesAuto(Instances instances) {
		trainingInstancesAuto = new Instances(instances);
	}

	String getBestAutoWekaTempFolderPrefix() {
		String prefix = "";
		bestAutoWekaExperimentPaths = Arrays.copyOf(autoWEKAClassifier.getmsExperimentPathsArray(),
				autoWEKAClassifier.getmsExperimentPathsArray().length);
		File file = new File(bestAutoWekaExperimentPaths[0]);
		String simpleFileName = file.getName();

		String fileNameStringprefix[] = simpleFileName.split("_");
		for (int i = 0; i <= 5; ++i) {
			prefix = prefix + fileNameStringprefix[i] + "_";
		}

		return prefix;
	}

	void deleteAllTempFilesExceptBestAutoClassifier(String fileName) {

		String tempFolderName = tempFilePath.get(0).substring(0, tempFilePath.get(0).lastIndexOf(File.separator));
		String bestClassiferTempFilePrefix = getBestAutoWekaTempFolderPrefix();

		System.out.println("delete file path is: " + fileName);

		File dir = new File(tempFolderName);
		if (!dir.isDirectory())
			return;

		File[] tempFile = dir.getParentFile().listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return (name.startsWith("autoweka" + fileName) && !name.startsWith(bestClassiferTempFilePrefix));
			}
		});

		for (int tempFileCount = 0; tempFileCount < tempFile.length; tempFileCount++) {
			try {
				deleteDirectoryRecursionJava6(tempFile[tempFileCount]);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("Unable to delete one of the directory");
			}
		}

	}
	
	
}
