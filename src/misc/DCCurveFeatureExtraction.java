package misc;

import static dc.ga.DCCurve.Type.Downturn;
import static dc.ga.DCCurve.Type.DownwardOvershoot;
import static dc.ga.DCCurve.Type.Upturn;
import static dc.ga.DCCurve.Type.UpwardOvershoot;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.Vector;

import javax.swing.JFrame;

import dc.GP.AbstractNode;
import dc.ga.DCCurve.Event;
import dc.ga.DCCurve.Type;
import dc.io.FReader;
import weka.classifiers.Classifier;
import weka.classifiers.CostMatrix;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.evaluation.NominalPrediction;
import weka.classifiers.functions.Logistic;
import weka.classifiers.functions.SMO;
import weka.classifiers.lazy.KStar;
import weka.classifiers.meta.CostSensitiveClassifier;
import weka.classifiers.rules.DecisionTable;
import weka.classifiers.rules.JRip;
import weka.classifiers.rules.PART;
import weka.classifiers.trees.DecisionStump;
import weka.classifiers.trees.J48;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.supervised.instance.Resample;
import weka.classifiers.evaluation.*;
import weka.core.AttributeStats;
import weka.filters.supervised.instance.SMOTE;
import weka.gui.graphvisualizer.GraphVisualizer;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.NaiveBayes;

public class DCCurveFeatureExtraction {
	public Event[] events;
	public Event[] testEvents;
	Event[] GPtestEvents;

	Event[] output;
	Event[] testOutput;

	double[] gpprediction;
	double predictionRmse;
	double predictionRmseMF;
	double predictionRmseOlsen;

	int numberOfUpwardEvent;
	int numberOfDownwardEvent;
	int numberOfNegativeUpwardEventGP;
	int numberOfNegativeDownwardEventGP;

	AbstractNode gptree = null;
	AbstractNode bestUpWardEventTree = null;
	AbstractNode bestDownWardEventTree = null;

	String upwardTrendTreeString = null;
	String downwardTrendTreeString = null;
	String trendTreeString = null;
	String thresholdString = "";
	double threshold = 0.0;
	String gpTreeInFixNotation = null;
	/**
	 * 0 = downward overshoot 1 = upward overshoot
	 */
	double[] meanRatio = new double[2];

	/**
	 * 0 = downward overshoot 1 = upward overshoot
	 */
	double[] medianRatio = new double[2];
	public Vector<AbstractNode> curve_bestTreesInRunsUpward = new Vector<AbstractNode>();
	public Vector<AbstractNode> curve_bestTreesInRuns = new Vector<AbstractNode>();
	public Vector<AbstractNode> curve_bestTreesInRunsDownward = new Vector<AbstractNode>();
	public String runsFitnessStrings = "";
	Map<Integer, double[]> runsPrediction = new HashMap<Integer, double[]>();
	boolean isUpwardEvent = true;

	SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
	static public ArrayList<String[]> eventFeatures = new ArrayList<String[]>();

	Instances train = null;
	Instances test = null;
	Instances data = null;
	int trainingCount = -1;
	Classifier currentModel = null;
	Classifier currentModelNoFilter = null;
	
	public DCCurveFeatureExtraction(Classifier classifier, Classifier classifierNoFilter) {
		// TODO Auto-generated constructor stub
		currentModel = classifier;
		currentModelNoFilter = classifierNoFilter;
	}

	public DCCurveFeatureExtraction(Classifier classifier) {
		// TODO Auto-generated constructor stub
		currentModel = classifier;
		currentModelNoFilter = null;
	}

	public DCCurveFeatureExtraction() {
		// TODO Auto-generated constructor stub
		currentModel = null;
		currentModelNoFilter = null;
	}
	/**
	 * 
	 * @param values
	 *            The array with prices/tick data
	 * @param delta
	 *            The threshold value
	 * @param GPTreeFileName
	 *            the name of the file where GP tree is stored
	 */
	public void buildtest(Double[] values, double delta, String GPTreeFileName) {
		ArrayList<Event> events = new ArrayList<Event>();
		Type event = Type.Upturn;

		Event last = new Event(0, 0, Type.Upturn);
		events.add(last);

		String thresholdStr = String.format("%.8f", delta);
		thresholdString = thresholdStr;
		threshold = delta;
		testOutput = new Event[values.length];

		double pHigh = 0;
		double pLow = 0;

		int[] indexDC = new int[2]; // DC event indexes
		int[] indexOS = new int[2]; // OS event indexes
		int index = 1;

		for (double value : values) {
			if (index == 1) {
				// it is the first line

				pHigh = value;
				pLow = value;

				Arrays.fill(indexDC, index);
				Arrays.fill(indexOS, index);
			} else if (event == Type.Upturn) {
				if (value <= (pHigh * (1 - delta))) {
					last.overshoot = detect(UpwardOvershoot, indexDC, indexOS);

					adjust(last.overshoot == null ? last : last.overshoot, indexDC, indexOS);

					event = Downturn;
					double percentageDisplacement = Math.abs(pHigh - pLow) / 100.0;
					pLow = value;

					indexDC[1] = index;
					indexOS[0] = index + 1;

					last = new Event(indexDC[0], indexDC[1], Downturn, percentageDisplacement);
					events.add(last);
				} else if (pHigh < value) {
					pHigh = value;

					indexDC[0] = index;
					indexOS[1] = index - 1;
				}
			} else {
				if (value >= (pLow * (1 + delta))) {
					last.overshoot = detect(DownwardOvershoot, indexDC, indexOS);

					adjust(last.overshoot == null ? last : last.overshoot, indexDC, indexOS);
					double percentageDisplacement = Math.abs(pHigh - pLow) / 100.0;
					event = Upturn;
					pHigh = value;

					indexDC[1] = index;
					indexOS[0] = index + 1;

					last = new Event(indexDC[0], indexDC[1], Upturn, percentageDisplacement);
					events.add(last);
				} else if (pLow > value) {
					pLow = value;

					indexDC[0] = index;
					indexOS[1] = index - 1;
				}
			}

			testOutput[index - 1] = last;

			index++;
		}

		this.testEvents = events.toArray(new Event[events.size()]);
	}

	public void build(Double[] values, double delta, String GPTreeFileName) {
		ArrayList<Event> events = new ArrayList<Event>();
		Type event = Type.Upturn;

		Event last = new Event(0, 0, Type.Upturn);
		events.add(last);

		String thresholdStr = String.format("%.8f", delta);
		thresholdString = thresholdStr;
		threshold = delta;
		output = new Event[values.length];

		double pHigh = 0;
		double pLow = 0;

		int[] indexDC = new int[2]; // DC event indexes
		int[] indexOS = new int[2]; // OS event indexes
		int index = 1;

		for (double value : values) {
			if (index == 1) {
				// it is the first line

				pHigh = value;
				pLow = value;

				Arrays.fill(indexDC, index);
				Arrays.fill(indexOS, index);
			} else if (event == Type.Upturn) {
				if (value <= (pHigh * (1 - delta))) {
					last.overshoot = detect(UpwardOvershoot, indexDC, indexOS);

					adjust(last.overshoot == null ? last : last.overshoot, indexDC, indexOS);

					event = Downturn;
					double percentageDisplacement = Math.abs(pHigh - pLow) / 100.0;
					pLow = value;

					indexDC[1] = index;
					indexOS[0] = index + 1;

					last = new Event(indexDC[0], indexDC[1], Downturn, percentageDisplacement);
					events.add(last);
				} else if (pHigh < value) {
					pHigh = value;

					indexDC[0] = index;
					indexOS[1] = index - 1;
				}
			} else {
				if (value >= (pLow * (1 + delta))) {
					last.overshoot = detect(DownwardOvershoot, indexDC, indexOS);

					adjust(last.overshoot == null ? last : last.overshoot, indexDC, indexOS);
					double percentageDisplacement = Math.abs(pHigh - pLow) / 100.0;
					event = Upturn;
					pHigh = value;

					indexDC[1] = index;
					indexOS[0] = index + 1;

					last = new Event(indexDC[0], indexDC[1], Upturn, percentageDisplacement);
					events.add(last);
				} else if (pLow > value) {
					pLow = value;

					indexDC[0] = index;
					indexOS[1] = index - 1;
				}
			}

			output[index - 1] = last;

			index++;
		}

		this.events = events.toArray(new Event[events.size()]);
	}
	public void extractDCDatasetTemporalFeacture(String filename, String thresholdString, int numberOfPartitions) {

		int x = numberOfPartitions; // chunk size
		int len = this.events.length;

		int i = 0;
		for (i = 0; i < len; i++) {
			if (this.events[i].endDate.equalsIgnoreCase(" ") || this.events[i].endDate.isEmpty())
				continue;

		}
		System.out.println("I am done");

	}

	public void extractDCfeatures() {

		for (int k = 0; k < events.length - 1; k++) {
			int start = events[k].start;
			int end = events[k].end;
			this.events[k].startDate = FReader.dataRecordInFileArray.get(start).Day;
			this.events[k].endDate = FReader.dataRecordInFileArray.get(end).Day;
			this.events[k].startPrice = FReader.dataRecordInFileArray.get(start).price;
			this.events[k].endPrice = FReader.dataRecordInFileArray.get(end).price;
			this.events[k].startTime = FReader.dataRecordInFileArray.get(start).time;
			this.events[k].endTime = FReader.dataRecordInFileArray.get(end).time;

			double endPrice = Double.parseDouble(FReader.dataRecordInFileArray.get(end).price);
			double startPrice = Double.parseDouble(FReader.dataRecordInFileArray.get(start).price);

			double price = Math.abs(((endPrice - startPrice) / startPrice) / threshold);
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
			if (this.events[k].overshoot != null && this.events[k].overshoot.length() > 0) {
				int osStart = events[k].overshoot.start;
				String osStartPrice = FReader.dataRecordInFileArray.get(osStart).price;

				int osEnd = events[k].overshoot.end;
				osEndPrice = FReader.dataRecordInFileArray.get(osEnd).price;
				osEndPriceDbl = Double.parseDouble(osEndPrice);
				double osStartPriceDbl = Double.parseDouble(osStartPrice);

				osPrice = Math.abs(((osEndPriceDbl - osStartPriceDbl) / osStartPriceDbl) / threshold);
				currentTmv = Math.abs(((osEndPriceDbl - startPrice) / startPrice) / threshold);

				int overshootEnd = events[k].overshoot.end;
				this.events[k].overshoot.endDate = FReader.dataRecordInFileArray.get(overshootEnd).Day;

				this.events[k].overshoot.endPrice = FReader.dataRecordInFileArray.get(overshootEnd).price;

				this.events[k].overshoot.endTime = FReader.dataRecordInFileArray.get(overshootEnd).time;
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
				long overshootEndSecondsSinceEpoch = overshootEndCalendar.getTimeInMillis() / 1000L;

				double TTtimeDiff = Math.abs(overshootEndSecondsSinceEpoch - startSecondsSinceEpoch);
				// double percentageTimeDisplacement = (
				// timeDiff/endSecondsSinceEpoch) * 100;
				// _TT10MinsTimeDifference = Math.round(TTtimeDiff/60.0);

			} else {
				currentTmv = Math.abs(((endPrice - startPrice) / startPrice) / threshold);

			}
			_TT10MinsTimeDifference = _10MinsTimeDifference;
			this.events[k].osv_ext = Double.toString(Double.parseDouble(df.format(osPrice)));
			this.events[k].percentageDeltaPrice = value;

			this.events[k].tmv = Double.toString(Double.parseDouble(df.format(currentTmv)));
			this.events[k].sigma = Double.toString((currentTmv * threshold) / _TT10MinsTimeDifference);

			if (this.events[k].sigma.compareToIgnoreCase("NaN") == 0) {
				// System.out.println(this.events[k].sigma);
				this.events[k].sigma = "-1";
			}

			if (events[k].overshoot != null)
				this.events[k].hasOverShoot = "yes";

			if (events[k].overshoot != null && value == "0.0")
				System.out.println("X");

			this.events[k].datapoints = new ArrayList<FReader.FileMember2>(
					FReader.dataRecordInFileArray.subList(events[k].start, events[k].end + 1));

			String timeValue = Double.toString(Double.parseDouble(df.format(_10MinsTimeDifference / 10)));

			this.events[k].percentageDeltaDuration = timeValue;

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

	private Event detect(Type type, int[] indexDC, int[] indexOS) {
		// overshoot event must have a start index lower that
		// the DC event start index
		if (indexOS[0] < indexOS[1] && indexOS[0] < indexDC[0]) {
			return new Event(indexOS[0], indexOS[1], type);
		}

		return null;
	}

	private void adjust(Event last, int[] indexDC, int[] indexOS) {
		// we might miss the start of an event
		if (indexDC[0] == last.start) {
			indexDC[0] = last.end + 1;
		}
		// we might skip the start of an event when there
		// are repeated values or large increases during an
		// upturn overshoot followed by a downturn event and
		// vice-versa (the overshoot will be invalid since
		// the end index will be smaller than the start index)
		else if (indexDC[0] > (last.end + 1)) {
			indexDC[0] = (last.end + 1);
		}
	}

	public Event findEvent(int index) {
		Event last = null;

		for (Event e : events) {
			if (index < e.end) {
				break;
			}

			last = e;
		}

		return last;
	}

	/**
	 * Filters an entire set of instances through a filter and returns the new
	 * set.
	 *
	 * @param data
	 *            the data to be filtered
	 * @param filter
	 *            the filter to be used
	 * @return the filtered set of data
	 * @throws Exception
	 *             if the filter can't be used successfully
	 */
	public static Instances useFilter(Instances data, Filter filter) throws Exception {
		/*
		 * System.err.println(filter.getClass().getName() + " in:" +
		 * data.numInstances());
		 */
		for (int i = 0; i < data.numInstances(); i++) {
			filter.input(data.instance(i));
		}
		filter.batchFinished();
		Instances newData = filter.getOutputFormat();
		Instance processed;
		while ((processed = filter.output()) != null) {
			newData.add(processed);
		}

		/*
		 * System.err.println(filter.getClass().getName() + " out:" +
		 * newData.numInstances());
		 */
		return newData;
	}

	public void classifyTraining(String filename, String thresholdString, String kfold, String classifierName, Map<Classifier[], Evaluation[]> classifierEvalMap) {

		File f = new File(filename);
		
		String modelName = currentModel.getClass().getName();
		String modelNameArray[] = modelName.split("\\.");
		String cname = modelNameArray[modelNameArray.length-1]; 
		System.out.println(cname);

		String absolutePath="";
		try {
			absolutePath = f.getCanonicalPath();
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		System.out.println("File path : " + absolutePath);

		String folderName = absolutePath.substring(0, absolutePath.lastIndexOf(File.separator));
		String fileName		= absolutePath.substring(absolutePath.lastIndexOf(File.separator)+1, absolutePath.length()-4);
		System.out.println("File path : " + folderName);

		BufferedReader breader = null;
		CostMatrix costMatrix = null;
		try {
			costMatrix = new CostMatrix(new BufferedReader(new FileReader(folderName + "/costMatrix.txt")));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			String trainingDataset = null;
			if (kfold.compareToIgnoreCase("kfold") == 0) {
				trainingDataset = folderName + "/" +fileName+ "_CompleteEventFeatures_" + thresholdString + ".arff";
			} else {
				trainingDataset = folderName + "/" +fileName+  "_Features_" + thresholdString + "_training.arff";
			}

			breader = new BufferedReader(new FileReader(trainingDataset));
			train = new Instances(breader);
			train.setClassIndex(train.numAttributes() - 1);

			

			int[] stats = train.attributeStats(train.classIndex()).nominalCounts;

			System.out.println(stats[0]);

			int totalBeforeSmote = 0;

			double minorityClassBeforeSmote = 0.0;
			double majorityClassBeforeSmote = 0.0;

			for (int i = 0; i < train.numInstances(); i++) {
				totalBeforeSmote++;

				String instanceClass = train.instance(i).stringValue(train.attribute(train.numAttributes() - 1));
				if (instanceClass.equalsIgnoreCase("yes"))
					minorityClassBeforeSmote = minorityClassBeforeSmote + 1;
				if (instanceClass.equalsIgnoreCase("no"))
					majorityClassBeforeSmote = majorityClassBeforeSmote + 1;
			}

			double PercentageMinorityClassBeforeSmote = ((minorityClassBeforeSmote / totalBeforeSmote) * 100.0);
			
			Resample filter = new Resample();
			SMOTE smoteFilter = new SMOTE();

			smoteFilter.setNearestNeighbors(5);

			if (PercentageMinorityClassBeforeSmote < 20.0)
				smoteFilter.setPercentage(320.0);
			else
				smoteFilter.setPercentage(150.0);
			smoteFilter.setInputFormat(train);

			filter.setBiasToUniformClass(3.0);
			filter.setInputFormat(train);
			filter.setRandomSeed(123);
			filter.setSampleSizePercent(100);

			Instances testing = Filter.useFilter(train, smoteFilter);
			System.out.println(train.numInstances() + " " + testing.numInstances());

			BufferedReader datafile = readDataFile(folderName + "/" +fileName+  "_CompleteEventFeatures_" + thresholdString + ".arff");
			int lines = 0;
			while (datafile.readLine() != null)
				lines++;
			System.out.println(lines);
			BufferedReader datafile2 = readDataFile(folderName + "/" +fileName+ "_CompleteEventFeatures_" + thresholdString + ".arff");
			data = new Instances(datafile2);

			data.setClassIndex(data.numAttributes() - 1);

			breader.close();
			String[] options = new String[1];
			options[0] = "-U";
			currentModel.buildClassifier(testing);
		//	currentModelNoFilter.buildClassifier(train);
			Evaluation eval = new Evaluation(testing, costMatrix);
			//Evaluation evalNoFilter = new Evaluation(train, costMatrix);

				Random rand = new Random(3); // using seed = 1
				int folds = 10;
				eval.crossValidateModel(currentModel, testing, folds, rand);
			//	evalNoFilter.crossValidateModel(currentModelNoFilter, train, folds, rand);
			

			//

			System.out.println("Rules " + currentModel.toString());
			System.out.println(eval.toClassDetailsString());
			// System.out.println(eval.toMatrixString());
			System.out.println("Correct to Incorrect" + eval.correct() + "/" + eval.incorrect());
		//	System.out.println("Correct to Incorrect" + evalNoFilter.correct() + "/" + evalNoFilter.incorrect());
			
			double minorityClass = 0.0;
			double majorityClass = 0.0;
			int total = 0;
			for (int i = 0; i < testing.numInstances(); i++) {
				total++;
				String instanceClass = testing.instance(i).stringValue(testing.attribute(testing.numAttributes() - 1));
				if (instanceClass.equalsIgnoreCase("yes"))
					minorityClass = minorityClass + 1;
				if (instanceClass.equalsIgnoreCase("no"))
					majorityClass = majorityClass + 1;
			}

			double pricisionDouble = 0.0;

			if (eval.numTruePositives(0) != 0.0)
				pricisionDouble = ((eval.numTruePositives(0)
						/ (eval.numTruePositives(0) + eval.numFalsePositives(0))) == Double.NaN) ? 0.0
								: (eval.numTruePositives(0)
										/ (eval.numTruePositives(0) + eval.numFalsePositives(0)));
			else
				pricisionDouble = 0.0;
			if (Double.isNaN(pricisionDouble))
				System.out.println("XXX" + " " + eval.numTruePositives(0));

			System.out.println(eval.confusionMatrix()[0][0]); // TP
			System.out.println(eval.confusionMatrix()[0][1]); // FN
			System.out.println(eval.confusionMatrix()[1][0]); // FP
			System.out.println(eval.confusionMatrix()[1][1]); // TN
			

			double filterAccuracyDbl = ((eval.confusionMatrix()[0][0] + eval.confusionMatrix()[1][1])
					/ (eval.confusionMatrix()[0][1] + eval.confusionMatrix()[1][0] + eval.confusionMatrix()[0][0]
							+ eval.confusionMatrix()[1][1]));
			if (Double.isNaN(filterAccuracyDbl))
				filterAccuracyDbl = 0.0;

			double filterPrecisionDbl = (eval.confusionMatrix()[0][0])
					/ (eval.confusionMatrix()[0][0] + eval.confusionMatrix()[1][0]);
			if (Double.isNaN(filterPrecisionDbl))
				filterPrecisionDbl = 0.0;

		//	SymbolicRegressionWithClassification.fileWriter
		//			.write(filename + " ," + thresholdString + "," + PercentageMinorityClassBeforeSmote + ", "
		//					+ PercentageMinorityClass + ", " + PercentageMajorityClassBeforeSmote + ","
		//					+ PercentageMajorityClass + 
		///					","
		//					+ (eval.confusionMatrix()[0][0])
		//							/ (eval.confusionMatrix()[0][0] + eval.confusionMatrix()[0][1]) +								// +
		//					"," + filterPrecisionDbl + // eval.weightedPrecision()
														// +
		//					 "," + eval.weightedFMeasure() +
		//					 "," + filterAccuracyDbl +

		//					" , " + eval.rootMeanSquaredError());

			
			
			System.out.println(eval.toSummaryString());

			System.out.println(eval.toMatrixString());

			classifierEvalMap.put(new Classifier[] { currentModel }, new Evaluation[] {eval});
			datafile2.close();
			datafile.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		;

	}

	public static Evaluation classify(Classifier model, Instances trainingSet, Instances testingSet) throws Exception {
		Evaluation evaluation = new Evaluation(trainingSet);

		model.buildClassifier(trainingSet);
		evaluation.evaluateModel(model, testingSet);

		return evaluation;
	}

	public static double calculateAccuracy(ArrayList<Prediction> predictions) {
		double correct = 0;

		for (int i = 0; i < predictions.size(); i++) {
			NominalPrediction np = (NominalPrediction) predictions.get(i);
			if (np.predicted() == np.actual()) {
				correct++;
			}
		}

		return 100 * correct / predictions.size();
	}

	public static Instances[][] crossValidationSplit(Instances data, int numberOfFolds) {
		Instances[][] split = new Instances[2][numberOfFolds];

		for (int i = 0; i < numberOfFolds; i++) {
			split[0][i] = data.trainCV(numberOfFolds, i);
			split[1][i] = data.testCV(numberOfFolds, i);
		}

		return split;
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
		System.out.println("File path : " + absolutePath);

		String folderName = absolutePath.substring(0, absolutePath.lastIndexOf(File.separator));
		System.out.println("File path : " + folderName);

		File file = new File(folderName + "/CompleteEventFeatures_" + thresholdString + ".arff");

		if (file.delete()) {
			System.out.println("File deleted successfully");
		} else {
			System.out.println("Failed to delete the file");
		}

		File file2 = new File(folderName + "/Features_" + thresholdString + "_training.arff");

		if (file2.delete()) {
			System.out.println("File2 deleted successfully");
		} else {
			System.out.println("Failed to delete the file2");
		}

		File file3 = new File(folderName + "/Features_" + thresholdString + "_test.arff");

		if (file3.delete()) {
			System.out.println("File3 deleted successfully");
		} else {
			System.out.println("Failed to delete the file3");
		}
	}

	String getWekaString(int k) {
		String features;

		if (events[k].sigma.compareToIgnoreCase("NaN") == 0) {
			// System.out.println(events[k].sigma);
			events[k].sigma = "-1";
		}

		if (events[k].sigma.compareToIgnoreCase("Infinity") == 0) {
			// System.out.println(events[k].sigma);
			events[k].sigma = "-1";
		}

		if (k == 0) {
			features = events[k].percentageDeltaPrice + "," + events[k].percentageDeltaDuration
					+ /* "," + events[k].type +"," + 0 +"," + 0+ */"," + events[k].sigma + "," + events[k].hasOverShoot;
		} else {
			features = events[k].percentageDeltaPrice + "," + events[k].percentageDeltaDuration
					+ /*
						 * "," + events[k].type +"," + events[k-1].osv_ext +","+
						 * this.events[k-1].tmv+
						 */ "," + events[k].sigma + "," + events[k].hasOverShoot;
		}

		return features;
	}

	public void assignTrainingEvent(ArrayList<Event> eventsArrayList) {

		this.events = eventsArrayList.toArray(new Event[eventsArrayList.size()]);

	}

	public void assignTestEvent(ArrayList<Event> testingEventsArrayList) {
		this.testEvents = testingEventsArrayList.toArray(new Event[testingEventsArrayList.size()]);
	}

	public void assignThreshold(double delta) {
		threshold = delta;
	}

	public void printTrainingFeatures(String filename, String thresholdString) {

		File f = new File(filename);

		String absolutePath ="";
		try {
			absolutePath = f.getCanonicalPath();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		System.out.println("File path : " + absolutePath);

		String folderName	= absolutePath.substring(0, absolutePath.lastIndexOf(File.separator));
		String fileName		= absolutePath.substring(absolutePath.lastIndexOf(File.separator)+1, absolutePath.length()-4);
		System.out.println("File path : " + folderName);

		Writer writer2 = null;

		try {
			writer2 = new FileWriter(folderName + "/"+ fileName + "_CompleteEventFeatures_" + thresholdString + ".arff", false);
			writer2.write("@relation overshootTraining");
			// System.out.println("@relation overshootTraining") ;
			System.out.println("Training File path : " + folderName + "/"+ fileName + "_CompleteEventFeatures_" + thresholdString + ".arff");
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
			writer2.write("@attribute hasOvershoot{yes,no}");
			writer2.write("\n");
			writer2.write("\n");
			writer2.write("@data");
			writer2.write("\n");

			for (int k = 0; k < this.events.length - 1; k++) {
				// String feaures = events[k].percentageDeltaPrice +"," +
				// events[k].percentageDeltaDuration + "," + events[k].type
				// +","+ events[k].hasOverShoot ;
				// System.out.println(feaures) ;
				writer2.write(getWekaString(k) + "\n");
			}
			writer2.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			writer2 = new FileWriter(folderName +"/"+ fileName + "_Features_" + thresholdString + "_training.arff", false);
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
			writer2.write("@attribute hasOvershoot{yes,no}");
			writer2.write("\n");
			writer2.write("\n");
			writer2.write("@data");
			writer2.write("\n");
			
		
			//for (int k = 0; k < trainingCount; k++) {
			for (int k = 0; k < this.events.length-1; k++) {
				
				writer2.write(getWekaString(k) + "\n");
			}
			writer2.close();
			// System.out.println("Number of training set:" + trainNum);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void printTestFeatures(String filename, String thresholdString) {

		File f = new File(filename);

		String absolutePath="";
		try {
			absolutePath = f.getCanonicalPath();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		System.out.println("File path : " + absolutePath);

		String folderName = absolutePath.substring(0, absolutePath.lastIndexOf(File.separator));
		String fileName = absolutePath.substring(absolutePath.lastIndexOf(File.separator)+1, absolutePath.length()-5);
		System.out.println("File path : " + folderName);

		Writer writer2 = null;

		try {
			writer2 = new FileWriter(folderName +"/"+ fileName + ".arff", false);
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
			writer2.write("@attribute hasOvershoot{yes,no}");
			writer2.write("\n");
			writer2.write("\n");
			writer2.write("@data");
			writer2.write("\n");

			for (int k = 0; k < this.testEvents.length-1; k++) {

				// String feaures = events[k].percentageDeltaPrice +"," +
				// events[k].percentageDeltaDuration + "," + events[k].type
				// +","+ events[k].hasOverShoot ;
				// System.out.println(feaures) ;
				writer2.write(getWekaString(k) + "\n");
			}
			writer2.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			writer2 = new FileWriter(folderName +"/"+ fileName + "_predict.arff", false);
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
			writer2.write("@attribute hasOvershoot{yes,no}");
			writer2.write("\n");
			writer2.write("\n");
			writer2.write("@data");
			writer2.write("\n");

			for (int k = 0; k < this.testEvents.length; k++) {
				// String feaures = events[k].percentageDeltaPrice +"," +
				// events[k].percentageDeltaDuration + "," + events[k].type
				// +",?" ;
				// System.out.println(feaures) ;
				writer2.write(getWekaString(k) + "\n");
			}
			writer2.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void printFeatures(String filename, String thresholdString, double split) {

		File f = new File(filename);

		String absolutePath = f.getAbsolutePath();
		System.out.println("File path : " + absolutePath);

		String folderName = absolutePath.substring(0, absolutePath.lastIndexOf(File.separator));
		System.out.println("File path : " + folderName);
		trainingCount = (int) (split * this.events.length);

		Writer writer2 = null;

		try {
			writer2 = new FileWriter(folderName + "/CompleteEventFeatures_" + thresholdString + ".arff", false);
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
			// writer2.write("@attribute percentageDCEventChunck numeric");
			// writer2.write("\n");
			writer2.write("@attribute hasOvershoot{yes,no}");
			writer2.write("\n");
			writer2.write("\n");
			writer2.write("@data");
			writer2.write("\n");

			for (int k = 0; k < this.events.length - 1; k++) {
				// String feaures = events[k].percentageDeltaPrice +"," +
				// events[k].percentageDeltaDuration + "," + events[k].type
				// +","+ events[k].hasOverShoot ;
				// System.out.println(feaures) ;
				writer2.write(getWekaString(k) + "\n");
			}
			writer2.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			writer2 = new FileWriter(folderName + "\\Features_" + thresholdString + "_training.arff", false);
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
			writer2.write("@attribute hasOvershoot{yes,no}");
			writer2.write("\n");
			writer2.write("\n");
			writer2.write("@data");
			writer2.write("\n");
			int trainNum = 0;
			for (int k = 0; k < trainingCount; k++) {

				// feaures = events[k].percentageDeltaPrice +"," +
				// events[k].percentageDeltaDuration + "," + events[k].type
				// +","+events[k].hasOverShoot ;
				// System.out.println(feaures) ;
				writer2.write(getWekaString(k) + "\n");
				trainNum = k;
			}
			writer2.close();
			// System.out.println("Number of training set:" + trainNum);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			writer2 = new FileWriter(folderName + "\\Features_" + thresholdString + "_test.arff", false);
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
			writer2.write("@attribute hasOvershoot{yes,no}");
			writer2.write("\n");
			writer2.write("\n");
			writer2.write("@data");
			writer2.write("\n");

			for (int k = trainingCount; k < this.events.length - 1; k++) {

				// String feaures = events[k].percentageDeltaPrice +"," +
				// events[k].percentageDeltaDuration + "," + events[k].type
				// +","+ events[k].hasOverShoot ;
				// System.out.println(feaures) ;
				writer2.write(getWekaString(k) + "\n");
			}
			writer2.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			writer2 = new FileWriter(folderName + "\\Features_" + thresholdString + "_test_predict.arff", false);
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
			writer2.write("@attribute hasOvershoot{yes,no}");
			writer2.write("\n");
			writer2.write("\n");
			writer2.write("@data");
			writer2.write("\n");

			for (int k = trainingCount; k < this.events.length - 1; k++) {
				// String feaures = events[k].percentageDeltaPrice +"," +
				// events[k].percentageDeltaDuration + "," + events[k].type
				// +",?" ;
				// System.out.println(feaures) ;
				writer2.write(getWekaString(k) + "\n");
			}
			writer2.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public void classify(String filename, String thresholdString, String kfold, String classifierName) {

		File f = new File(filename);

		String absolutePath = f.getAbsolutePath();
		System.out.println("File path : " + absolutePath);

		String folderName = absolutePath.substring(0, absolutePath.lastIndexOf(File.separator));
		System.out.println("File path : " + folderName);

		BufferedReader breader = null;
		CostMatrix costMatrix = null;
		try {
			costMatrix = new CostMatrix(new BufferedReader(new FileReader(folderName + "/costMatrix.txt")));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			String trainingDataset = null;
			if (kfold.compareToIgnoreCase("kfold") == 0) {
				trainingDataset = folderName + "/CompleteEventFeatures_" + thresholdString + ".arff";
			} else {
				trainingDataset = folderName + "/Features_" + thresholdString + "_training.arff";
			}

			breader = new BufferedReader(new FileReader(trainingDataset));
			train = new Instances(breader);
			train.setClassIndex(train.numAttributes() - 1);

			breader = new BufferedReader(new FileReader(folderName + "\\Features_" + thresholdString + "_test.arff"));
			test = new Instances(breader);
			test.setClassIndex(test.numAttributes() - 1);

			int[] stats = train.attributeStats(train.classIndex()).nominalCounts;

			System.out.println(stats[0]);

			int totalBeforeSmote = 0;

			double minorityClassBeforeSmote = 0.0;
			double majorityClassBeforeSmote = 0.0;

			for (int i = 0; i < train.numInstances(); i++) {
				totalBeforeSmote++;

				String instanceClass = train.instance(i).stringValue(train.attribute(train.numAttributes() - 1));
				if (instanceClass.equalsIgnoreCase("yes"))
					minorityClassBeforeSmote = minorityClassBeforeSmote + 1;
				if (instanceClass.equalsIgnoreCase("no"))
					majorityClassBeforeSmote = majorityClassBeforeSmote + 1;
			}

			double PercentageMinorityClassBeforeSmote = ((minorityClassBeforeSmote / totalBeforeSmote) * 100.0);
			double PercentageMajorityClassBeforeSmote = ((majorityClassBeforeSmote / totalBeforeSmote) * 100.0);

			Resample filter = new Resample();
			SMOTE smoteFilter = new SMOTE();

			smoteFilter.setNearestNeighbors(5);

			if (PercentageMinorityClassBeforeSmote < 20.0)
				smoteFilter.setPercentage(320.0);
			else
				smoteFilter.setPercentage(150.0);
			smoteFilter.setInputFormat(train);

			filter.setBiasToUniformClass(3.0);
			filter.setInputFormat(train);
			filter.setRandomSeed(123);
			filter.setSampleSizePercent(100);

			Instances testing = Filter.useFilter(train, smoteFilter);
			System.out.println(train.numInstances() + " " + testing.numInstances());

			BufferedReader datafile = readDataFile(folderName + "/CompleteEventFeatures_" + thresholdString + ".arff");
			int lines = 0;
			while (datafile.readLine() != null)
				lines++;
			System.out.println(lines);
			BufferedReader datafile2 = readDataFile(
					folderName + "/CompleteEventFeatures_" + thresholdString + ".arff");
			data = new Instances(datafile2);

			data.setClassIndex(data.numAttributes() - 1);

			breader.close();
			String[] options = new String[1];
			options[0] = "-U";
			currentModel.buildClassifier(testing);
			currentModelNoFilter.buildClassifier(train);
			Evaluation eval = new Evaluation(testing, costMatrix);
			Evaluation evalNoFilter = new Evaluation(train, costMatrix);

			if (kfold.compareToIgnoreCase("kfold") == 0) {
				Random rand = new Random(3); // using seed = 1
				int folds = 10;
				eval.crossValidateModel(currentModel, testing, folds, rand);
				evalNoFilter.crossValidateModel(currentModelNoFilter, train, folds, rand);
			} else {
				eval.evaluateModel(currentModel, test);
				evalNoFilter.evaluateModel(currentModelNoFilter, test);
			}

			//

			System.out.println("Rules " + currentModel.toString());
			System.out.println(eval.toClassDetailsString());
			// System.out.println(eval.toMatrixString());
			System.out.println("Correct to Incorrect" + eval.correct() + "/" + eval.incorrect());
			System.out.println("Correct to Incorrect" + evalNoFilter.correct() + "/" + evalNoFilter.incorrect());
			Instances labeled = new Instances(test);

			double minorityClass = 0.0;
			double majorityClass = 0.0;
			int total = 0;
			for (int i = 0; i < testing.numInstances(); i++) {
				total++;
				String instanceClass = testing.instance(i).stringValue(testing.attribute(testing.numAttributes() - 1));
				if (instanceClass.equalsIgnoreCase("yes"))
					minorityClass = minorityClass + 1;
				if (instanceClass.equalsIgnoreCase("no"))
					majorityClass = majorityClass + 1;
			}

			// fileWriter.write(", accuracy_Yes, precision_Yes,
			// sensitivity_Yes,specificity_Yes, recall_Yes, f-measure_Yes,
			// FPR_Yes, TPR_Yes, accuracy_No, precision_No,
			// sensitivity_No,specificity_No, recall_No, f-measure_No,FPR_No,
			// TPR_No");

			double PercentageMinorityClass = ((minorityClass / total) * 100.0);
			double PercentageMajorityClass = ((majorityClass / total) * 100.0);

			System.out.println(evalNoFilter.pctIncorrect());
			System.out.println(evalNoFilter.pctCorrect());

			double pricisionDouble = 0.0;

			if (evalNoFilter.numTruePositives(0) != 0.0)
				pricisionDouble = ((evalNoFilter.numTruePositives(0)
						/ (evalNoFilter.numTruePositives(0) + evalNoFilter.numFalsePositives(0))) == Double.NaN) ? 0.0
								: (evalNoFilter.numTruePositives(0)
										/ (evalNoFilter.numTruePositives(0) + evalNoFilter.numFalsePositives(0)));
			else
				pricisionDouble = 0.0;
			if (Double.isNaN(pricisionDouble))
				System.out.println("XXX" + " " + evalNoFilter.numTruePositives(0));

			System.out.println(evalNoFilter.confusionMatrix()[0][0]); // TP
			System.out.println(evalNoFilter.confusionMatrix()[0][1]); // FN
			System.out.println(evalNoFilter.confusionMatrix()[1][0]); // FP
			System.out.println(evalNoFilter.confusionMatrix()[1][1]); // TN
			System.out.println(evalNoFilter.toMatrixString("=== Confusion matrix for fold "));
			double noFilterAccuracyDbl = ((evalNoFilter.confusionMatrix()[0][0] + evalNoFilter.confusionMatrix()[1][1])
					/ (evalNoFilter.confusionMatrix()[0][1] + evalNoFilter.confusionMatrix()[1][0]
							+ evalNoFilter.confusionMatrix()[0][0] + evalNoFilter.confusionMatrix()[1][1]));
			if (Double.isNaN(noFilterAccuracyDbl))
				noFilterAccuracyDbl = 0.0;

			double filterAccuracyDbl = ((eval.confusionMatrix()[0][0] + eval.confusionMatrix()[1][1])
					/ (eval.confusionMatrix()[0][1] + eval.confusionMatrix()[1][0] + eval.confusionMatrix()[0][0]
							+ eval.confusionMatrix()[1][1]));
			if (Double.isNaN(filterAccuracyDbl))
				filterAccuracyDbl = 0.0;

			double noFilterPrecisionDbl = (evalNoFilter.confusionMatrix()[0][0])
					/ (evalNoFilter.confusionMatrix()[0][0] + evalNoFilter.confusionMatrix()[1][0]);
			if (Double.isNaN(noFilterPrecisionDbl))
				noFilterPrecisionDbl = 0.0;

			double filterPrecisionDbl = (eval.confusionMatrix()[0][0])
					/ (eval.confusionMatrix()[0][0] + eval.confusionMatrix()[1][0]);
			if (Double.isNaN(filterPrecisionDbl))
				filterPrecisionDbl = 0.0;

			ExtractIndicators.fileWriter
					.write(filename + " ," + thresholdString + "," + PercentageMinorityClassBeforeSmote + ", "
							+ PercentageMinorityClass + ", " + PercentageMajorityClassBeforeSmote + ","
							+ PercentageMajorityClass + ","
							+ (evalNoFilter.confusionMatrix()[0][0])
									/ (evalNoFilter.confusionMatrix()[0][0] + evalNoFilter.confusionMatrix()[0][1])
							+ // evalNoFilter.weightedRecall() +
							","
							+ (eval.confusionMatrix()[0][0])
									/ (eval.confusionMatrix()[0][0] + eval.confusionMatrix()[0][1])
							+ // eval.weightedRecall() +
							"," + noFilterPrecisionDbl + // evalNoFilter.weightedPrecision()
															// +
							"," + filterPrecisionDbl + // eval.weightedPrecision()
														// +
							"," + evalNoFilter.weightedFMeasure() + "," + eval.weightedFMeasure() + ","
							+ noFilterAccuracyDbl + "," + filterAccuracyDbl +

							// "," + ((evalNoFilter.numTruePositives(1)+
							// evalNoFilter.numTrueNegatives(1))/(evalNoFilter.numFalseNegatives(1)+
							// evalNoFilter.numFalsePositives(1)+
							// evalNoFilter.numTruePositives(1)+
							// evalNoFilter.numTrueNegatives(1))) +
							// "," + ((eval.numTruePositives(1)+
							// eval.numTrueNegatives(1))/(eval.numFalseNegatives(1)+
							// eval.numFalsePositives(1)+
							// eval.numTruePositives(1)+
							// eval.numTrueNegatives(1))) +

							// "," + ((evalNoFilter.weightedTruePositiveRate()+
							// evalNoFilter.weightedTrueNegativeRate())/(evalNoFilter.weightedFalseNegativeRate()
							// + evalNoFilter.weightedFalsePositiveRate()+
							// evalNoFilter.numTruePositives(1)+
							// evalNoFilter.weightedTrueNegativeRate())) +
							// "," + ((eval.weightedTruePositiveRate()+
							// eval.weightedTrueNegativeRate())/(eval.weightedFalseNegativeRate()+
							// eval.weightedFalsePositiveRate()+
							// eval.weightedTruePositiveRate()+
							// eval.weightedTrueNegativeRate())) +

							/*
							 * " , " + evalNoFilter.recall(0)+ " , " +
							 * eval.recall(0)+
							 * 
							 * " , " + evalNoFilter.fMeasure(0) + " , " +
							 * eval.fMeasure(0) +
							 * 
							 * " , " + ((evalNoFilter.numTruePositives(0)+
							 * evalNoFilter.numTrueNegatives(0))/(evalNoFilter.
							 * numFalseNegatives(0)+
							 * evalNoFilter.numFalsePositives(0)+
							 * evalNoFilter.numTruePositives(0)+
							 * evalNoFilter.numTrueNegatives(0))) + " , " +
							 * ((eval.numTruePositives(0)+
							 * eval.numTrueNegatives(0))/(eval.numFalseNegatives
							 * (0)+ eval.numFalsePositives(0)+
							 * eval.numTruePositives(0)+
							 * eval.numTrueNegatives(0))) +
							 * 
							 * " , " + pricisionDouble + " , " +
							 * ((eval.numTruePositives(0) != 0.0)
							 * ?(eval.numTruePositives(0)/
							 * (eval.numTruePositives(0) +
							 * eval.numFalsePositives(0))) : 0.0)+ " , " +
							 * evalNoFilter.recall(1)+ " , " + eval.recall(1)+
							 * 
							 * " , " + evalNoFilter.fMeasure(1) + " , " +
							 * eval.fMeasure(1) + " , " +
							 * ((evalNoFilter.numTruePositives(1)+
							 * evalNoFilter.numTrueNegatives(1))/(evalNoFilter.
							 * numFalseNegatives(1)+
							 * evalNoFilter.numFalsePositives(1)+
							 * evalNoFilter.numTruePositives(1)+
							 * evalNoFilter.numTrueNegatives(1))) + " , " +
							 * ((eval.numTruePositives(1)+
							 * eval.numTrueNegatives(1))/(eval.numFalseNegatives
							 * (1)+ eval.numFalsePositives(1)+
							 * eval.numTruePositives(1)+
							 * eval.numTrueNegatives(1))) +
							 * 
							 * 
							 * " , " + ((evalNoFilter.numTruePositives(1) !=
							 * 0.0)? (evalNoFilter.numTruePositives(1)/
							 * (evalNoFilter.numTruePositives(1) +
							 * evalNoFilter.numFalsePositives(1))) : 0.0 )+
							 * " , " + ((eval.numTruePositives(1) !=
							 * 0.0)?(eval.numTruePositives(1)/
							 * (eval.numTruePositives(1) +
							 * eval.numFalsePositives(1))): 0.0)+
							 */
							" , " + evalNoFilter.rootMeanSquaredError() + " , " + eval.rootMeanSquaredError());

			ExtractIndicators.fileWriter.write("\n");
			// eval.evaluateModel(tree, test);
			System.out.println(eval.toSummaryString());

			ExtractIndicators.fileWriterModelRules
					.write(filename + "," + thresholdString + "," + currentModel.toString());
			ExtractIndicators.fileWriterModelRules.write("**********************************");
			ExtractIndicators.fileWriterModelRules.write("\n");
			ExtractIndicators.fileWriterModelRules.write("\n");
			ExtractIndicators.fileWriterModelRules.write("\n");
			ExtractIndicators.fileWriterModelRules.write("\n");
			System.out.println("Rule " + currentModel.toString());
			//////
			// System.out.println("Rule " +
			////// ((NaiveBayes)currentModel).getDisplayModelInOldFormat());

			if (classifierName.compareToIgnoreCase("JRIP") == 0) {

			}
			if (classifierName.compareToIgnoreCase("PART") == 0) {

			}

			if (classifierName.compareToIgnoreCase("DecisionTable") == 0) {

			}
			if (classifierName.compareToIgnoreCase("KStar") == 0) {
				System.out.println(((KStar) currentModel).toString());
			}

			if (classifierName.compareToIgnoreCase("J48") == 0) {

			}

			if (classifierName.compareToIgnoreCase("Logistic") == 0) {

			}

			if (classifierName.compareToIgnoreCase("EditableBayesNet") == 0) {

			}

			if (classifierName.compareToIgnoreCase("BayesNetGenerator") == 0) {

			}

			if (classifierName.compareToIgnoreCase("NaiveBayes") == 0) {

			}

			if (classifierName.compareToIgnoreCase("BayesNet") == 0) {
				/*
				 * GraphVisualizer graphVisualizer = new GraphVisualizer();
				 * graphVisualizer.readBIF(((BayesNet)currentModel).graph());
				 * 
				 * final JFrame jFrame = new JFrame(
				 * "Weka Classifier Graph Visualizer: Bayes net");
				 * jFrame.setSize(500, 400);
				 * jFrame.getContentPane().setLayout(new BorderLayout());
				 * jFrame.getContentPane().add(graphVisualizer,
				 * BorderLayout.CENTER); jFrame.addWindowFocusListener(new
				 * WindowAdapter() { public void windowClosing(WindowEvent e) {
				 * jFrame.dispose(); } }); jFrame.setVisible(true);
				 * graphVisualizer.layoutGraph();
				 */
			}

			if (classifierName.compareToIgnoreCase("SMO") == 0) {

			}

			//////

			System.out.println(eval.toMatrixString());

			ExtractIndicators.evaluationsMap.put(filename + "+" + thresholdString + "+" + classifierName,
					new Evaluation[] { eval, evalNoFilter });

			ExtractIndicators.modelsNoFilterMap.put(filename + "+" + thresholdString + "+" + classifierName,
					new Classifier[] { currentModel, currentModelNoFilter });
			for (int i = 0; i < test.numInstances(); i++) {
				double clsLabel = currentModel.classifyInstance(test.instance(i));
				labeled.instance(i).setClassValue(clsLabel);
				// System.out.println("prediction
				// "+labeled.instance(i).stringValue(labeled.attribute(labeled.numAttributes()
				// - 1)));
			}

			// System.out.println("FPR:"+eval.falsePositiveRate(0) + " FPR:" +
			// eval.falsePositiveRate(1) +
			// " TPR:"+ eval.truePositiveRate(0) + " TPR:" +
			// eval.truePositiveRate(1) +
			// " " + eval.fMeasure(0) + " F-score:" + eval.fMeasure(1));

			BufferedWriter outcomeWriter = new BufferedWriter(
					new FileWriter(folderName + "\\Features_" + thresholdString + "_TestAfterLabelling.arff"));
			outcomeWriter.write(labeled.toString());
			outcomeWriter.close();
			datafile2.close();
			datafile.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		;

	}


}
