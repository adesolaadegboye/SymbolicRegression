
package dc.ga;

import static dc.ga.DCCurve.Type.Downturn;
import static dc.ga.DCCurve.Type.DownwardOvershoot;
import static dc.ga.DCCurve.Type.Upturn;
import static dc.ga.DCCurve.Type.UpwardOvershoot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.stream.IntStream;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import dc.MyException;
import dc.GP.AbstractNode;
import dc.GP.Const;
import dc.GP.TreeHelperClass;
import dc.ga.DCCurve.Event;
import dc.ga.DCCurve.Type;
import dc.io.FReader;



import dc.io.FReader.FileMember2;
import misc.DCEventGenerator;
import misc.SimpleDrawDown;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;




/**
 * DC (Directional Change) event detector.
 * 
 * <p>
 * <b>Notes:</b>
 * </p>
 * <ul>
 * <li>Overshoot events are only detected when a Upturn/Downturn event is
 * detected;</li>
 * </ul>
 * 
 * @author Fernando Esteban Barril Otero
 */
public class DCCurve {
	Event[] events;

	protected Event[] output;
	protected double []GpUpwardoutput;
	protected double[] GpDownwardoutput;

	protected double[] gpprediction;
	double[] predictionUpward;
	double[] predictionDownward;
	
	int numberOfUpwardEvent;
	int numberOfDownwardEvent;
	int numberOfNegativeUpwardEventGP;
	int numberOfNegativeDownwardEventGP;
	
	double rmseResult = Double.MAX_VALUE;
	
	
	
	Event[] trainingEvents;
	String upwardTrendTreeString = null;
	String downwardTrendTreeString = null;
	String trendTreeString = null;
	
	
	AbstractNode bestUpWardEventTree = null;
	AbstractNode bestDownWardEventTree = null;
	
	AbstractNode bestclassifierBasedUpWardEventTree = null;
	AbstractNode bestclassifierBasedDownWardEventTree = null;
	
	protected double trainingOpeningPosition = 500000.00;
	protected double[] trainingPrediction;

	String upwardClassifierTrendTreeString = null;
	String downwardClassifierTrendTreeString = null;
	AbstractNode gptree = null;
	
	public Vector<AbstractNode> curve_bestTreesInRunsUpward = new Vector<AbstractNode>();
	public Vector<AbstractNode> curve_bestTreesInRuns = new Vector<AbstractNode>();
	public Vector<AbstractNode> curve_bestTreesInRunsDownward = new Vector<AbstractNode>();
	public String runsFitnessStrings = "";
	public double threshold = -0.1;
	Map<Integer, double[]> runsPrediction = new HashMap<Integer, double[]>();
	SimpleDrawDown simpleDrawDown = new SimpleDrawDown();
	boolean isUpwardEvent = true;
	Instances testInstance;
	Instances trainingInstance;
	Instances outputTestInstance;
	
	public String filename = "";
	public double initialbudget = 0.0;
	public double tradingBudget = 0.0;
	public boolean isPositionOpen = false;
	public double lastClosedPosition = 0.0;
	public double lastOpenPrice = 0.0;
	public int noOfTransactions = 0;
	
	public double tradingBudgetQuoteCCy = 0.0;
	public int numberOfOpenPositions = 0;
	public double lastUpDCEndTraded = 0.0;
	public double percentageOfBudgetTraded = 0.0;
	public double maxNumberOfOpenPositions = 0;

	String gpTreeInFixNotation = null;

	/**
	 * 0 = downward overshoot 1 = upward overshoot
	 */
	double[] meanRatio = new double[2];

	/**
	 * 0 = downward overshoot 1 = upward overshoot
	 */
	double[] medianRatio = new double[2];
	String[] gpRatio = new String[2];
	double[] meanOvershoot = new double[1];
	double[] splitmeanOvershoot = new double[2];
	
	/**
	 * 0 = downward event length 1 = upward event length
	 */
	double[] AverageDCEventLength = new  double[2];
	
	PreProcess preprocess = null;
	ScriptEngineManager mgr = new ScriptEngineManager();
	ScriptEngine engine = mgr.getEngineByName("JavaScript");
	
	double maximumQuatityPerTransaction = 0.0;
	double maximumOpenedPosition = 0;
	

	public static void main(String[] args) {
		System.out.println("Loading directional changes data...");

		// loads the data

		int noOfThresholds = 5;
		double[] THRESHOLDS = new double[noOfThresholds];
		// fill in threshold values

		int trainingDataSize = 10000;
		Double[] training = new Double[trainingDataSize];
		// fill in training data (tick data prices)

		int testDataSize = 3000;
		Double[] test = new Double[testDataSize];
		// fill in test data (tick data prices)

		DCCurve[] curves = new DCCurve[noOfThresholds];//

		//System.out.println("DC curves:");

		for (int i = 0; i < curves.length; i++) {
			curves[i] = new DCCurve();
			curves[i].build(training, THRESHOLDS[i]);// here we are generating
														// the events for the
														// training set, you
														// have to make a
														// separate call for the
														// test set.

			System.out.println(String.format(
					"%1.3f%%: {length = %5d, meanRatio[up] = %02.2f, meanRatio[down] = %02.2f}", THRESHOLDS[i] * 100,
					curves[i].events.length, curves[i].meanRatio[1], curves[i].meanRatio[0]));

		}
	}

	/**
	 * 
	 * @param values
	 *            The array with prices/tick data
	 * @param delta
	 *            The threshold value
	 */
	public void build(Double[] values, double delta) {
		ArrayList<Event> events = new ArrayList<Event>();
		Type event = Type.Upturn;

		Event last = new Event(0, 0, Type.Upturn);
		events.add(last);

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
					pLow = value;

					indexDC[1] = index;
					indexOS[0] = index + 1;

					last = new Event(indexDC[0], indexDC[1], Downturn);
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

					event = Upturn;
					pHigh = value;

					indexDC[1] = index;
					indexOS[0] = index + 1;

					last = new Event(indexDC[0], indexDC[1], Upturn);
					events.add(last);
				} else if (pLow > value) {
					pLow = value;

					indexDC[0] = index;
					indexOS[1] = index - 1;
				}
			}

			// output[index - 1] = (byte) (last.type == Type.Upturn ? 1 : 0);
			output[index - 1] = last;

			index++;
		}

		// fix start index of events
		/*
		 * ArrayList<Event> reverse = new ArrayList<Event>(events);
		 * Collections.reverse(reverse);
		 * 
		 * Event last = null;
		 * 
		 * for (Event e : reverse) { if (last != null && e.start == last.start)
		 * { last.start = e.end + 1; }
		 * 
		 * last = e; }
		 */

		this.events = events.toArray(new Event[events.size()]);
		// downward overshoots
		meanRatio[0] = 0.0;
		medianRatio[0] = 0.0;
		AverageDCEventLength[0] = 0.0;

		double meanDownturn = 0.0;
		double meanDownwardOvershoot = 0.0;

		int downturn = 0;
		// upward overshoots
		meanRatio[1] = 0.0;
		medianRatio[1] = 0.0;
		AverageDCEventLength[1] =0.0;

		double meanUpturn = 0.0;
		double meanUpwardOvershoot = 0.0;

		int upturn = 0;

		ArrayList<Double> downwardRatio = new ArrayList<Double>();
		ArrayList<Double> upwardRatio = new ArrayList<Double>();

		for (Event e : this.events) {
			// we ignore the first (artificial) event
			if (e.start > 0) {
				double ratio = 0.0;

				if (e.overshoot != null) {
					ratio = (e.overshoot.length() / (double) e.length());
				}

				if (e.type == Type.Upturn) {
					upturn++;
					meanRatio[1] += ratio;
					upwardRatio.add(ratio);

					meanUpturn += e.length();
					meanUpwardOvershoot += (e.overshoot != null) ? e.overshoot.length() : 0;
				} else if (e.type == Type.Downturn) {
					downturn++;
					meanRatio[0] += ratio;
					downwardRatio.add(ratio);

					meanDownturn += e.length();
					meanDownwardOvershoot += (e.overshoot != null) ? e.overshoot.length() : 0;

				}
			}
		}

		// meanRatio[0] /= downturn;
		// meanRatio[1] /= upturn;

		if (Const.OsFunctionEnum == Const.function_code.eMichaelFernando){
			meanRatio[0] = (meanDownwardOvershoot / downturn) / (meanDownturn / downturn);
			meanRatio[1] = (meanUpwardOvershoot / upturn) / (meanUpturn / upturn);
		}
		
		if (Const.OsFunctionEnum == Const.function_code.eOlsen){
			meanRatio[0] = 2.0;
			meanRatio[1] = 2.0;
		}
		
		AverageDCEventLength[0] = meanDownturn/downturn;
		AverageDCEventLength[1] = meanUpturn/upturn;
		meanOvershoot[0] = (meanDownwardOvershoot + meanUpwardOvershoot) / meanUpturn + meanDownturn ;
		splitmeanOvershoot[0] = meanDownwardOvershoot/meanDownturn;
		splitmeanOvershoot[1] = meanUpwardOvershoot/meanUpturn;
		// Collections.sort(downwardRatio);
		// Collections.sort(upwardRatio);

		// medianRatio[0] = downwardRatio.get(downwardRatio.size() / 2);
		// medianRatio[1] = upwardRatio.get(upwardRatio.size() / 2);
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
	public void build(Double[] values, double delta, String GPTreeFileName, boolean isTraining) {
		ArrayList<Event> events = new ArrayList<Event>();
		Type event = Type.Upturn;

		Event last = new Event(0, 0, Type.Upturn);
		events.add(last);

		output = new Event[values.length];
		gpprediction = new double[values.length];

		double pHigh = 0;
		double pLow = 0;

		int[] indexDC = new int[2]; // DC event indexes
		int[] indexOS = new int[2]; // OS event indexes
		int index = 1;

		// getGPTreeForCurve(GPTreeFileName); // Adesola Get the best GP for
		// this curve while reading GP from harddrive
		ScriptEngineManager mgr = new ScriptEngineManager();
		ScriptEngine engine = mgr.getEngineByName("JavaScript");

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
					pLow = value;

					indexDC[1] = index;
					indexOS[0] = index + 1;

					last = new Event(indexDC[0], indexDC[1], Downturn);
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

					event = Upturn;
					pHigh = value;

					indexDC[1] = index;
					indexOS[0] = index + 1;

					last = new Event(indexDC[0], indexDC[1], Upturn);
					events.add(last);
				} else if (pLow > value) {
					pLow = value;

					indexDC[0] = index;
					indexOS[1] = index - 1;
				}
			}

			// output[index - 1] = (byte) (last.type == Type.Upturn ? 1 : 0);

			/*
			 * If tree is read from file use this. if this fails remove and
			 * rollback to previous
			 */
			output[index - 1] = last;

			index++;
		}

		
		
		//output = new Event[values.length];
		this.events = events.toArray(new Event[events.size()]);
		
		

		///Continue 
		if (Const.OsFunctionEnum == Const.function_code.eGP) {
			if (preprocess == null)
				preprocess =  new PreProcess( delta, filename, "GP");
			if (isTraining ){	
				preprocess.buildTraining(this.events);
				preprocess.selectBestClassifier();
				preprocess.lastTrainingEvent =  this.events[this.events.length-1];
				// loadTrainingData load classification data
				preprocess.loadTrainingData(this.events);
			
				preprocess.runAutoWeka();
				
				
			}
			else
			{
				preprocess.lastTestingEvent=  this.events[this.events.length-1];
				
				
				preprocess.buildTest(this.events);
				
				try
				{
					while( preprocess.lastTestingEvent.start >= preprocess.dataRecordInFileArrayTest.size()  || preprocess.lastTestingEvent.end >= preprocess.dataRecordInFileArrayTest.size() )
					{
						this.events = null;
						//Returns a view of the portion of this list between the specified fromIndex, inclusive, and toIndex, exclusive. 
						//System.out.println( "elements size is " + events.size() + " datarecord size" + preprocess.dataRecordInFileArrayTest.size());
						events.remove(events.size()-1);
						//System.out.println( "elements size is now " + events.size() );
						//this.events = events.subList(0, events.size()-1).toArray(new Event[events.size()-1]);
						this.events = events.toArray(new Event[events.size()]);
						preprocess.lastTestingEvent=  this.events[this.events.length-1];
						
					}
				}
				catch (IndexOutOfBoundsException e){
					this.events = null;
					//Returns a view of the portion of this list between the specified fromIndex, inclusive, and toIndex, exclusive. 
					//System.out.println( " 1 elements size is " + events.size() );
					events.remove(events.size()-1);
					//System.out.println( "1 elements size is now " + events.size() );
					//this.events = events.subList(0, events.size()-1).toArray(new Event[events.size()-1]);
					this.events = events.toArray(new Event[events.size()]);
					preprocess.lastTestingEvent=  this.events[this.events.length-1];
				}
				preprocess.processTestData(this.events);
				
				preprocess.loadTestData(this.events);
				
				preprocess.classifyTestData();
			//	if(GA.hasPrint){ TODO seperate upward classifier from down classifiers
			//	String classificationResult = GPTreeFileName + "\t" + String.format("%.5f", delta) + "\t" + preprocess.printPreprocessClassification(this.events);
			//	GA.log.save("ClassificationAnalysis.txt",classificationResult);
			//	}
			}
			
		}
		
		
	
		if (Const.OsFunctionEnum == Const.function_code.eGP) {
			if (Const.splitDatasetByTrendType) {
				
				//get upward dc first
				String thresholdStr = String.format("%.5f", delta);
				String gpTreeName = Const.UPWARD_EVENT_STRING+thresholdStr + ".txt";
				String thisLine = null;
				
				Vector<Event> trendOfChoiceVec = new Vector<Event>();
				GpUpwardoutput= new double[values.length];
				GpDownwardoutput = new double[values.length];
				for (int i =0; i< this.events.length; ++i)
				{
					if (this.events[i].type ==  Type.Upturn){
					/*	if ((this.events[i].overshoot == null  || this.events[i].overshoot.length() == 0 )
								&& Const.INCLUDE_ZERO_OS_ITEMS == false ){
							Event _os = new Event(this.events[i].end, this.events[i].end,this.events[i].type);
							this.events[i].overshoot = _os;
							//continue;
						}*/
						
						
						String classificationStr = "";
						if (isTraining){
							classificationStr  = preprocess.classifyTrainingInstance(i);
						}
						else
						{
							if (preprocess.testInstances.numInstances() >= i)
								classificationStr = preprocess.classifyTestInstance(i);
							else
								classificationStr = "no";
								}
							//System.out.println("prediction "+training.instance(eventCount).stringValue(training.attribute(training.numAttributes() - 1)));
						//System.out.println("Classification : " + classificationStr);
					//	if (isTraining){
							if ((classificationStr.compareToIgnoreCase("no") == 0))
							{
								;
							}
							else
								trendOfChoiceVec.add(this.events[i]);
						
					}
				}
				
				//TODO only load events with overshoot
				
								
				 Event[] uptrendEvent = trendOfChoiceVec.toArray(new Event[trendOfChoiceVec.size()]);
				 TreeHelperClass treeHelperClass = new TreeHelperClass();
				 if (treeHelperClass.bestTreesInRuns != null)
						treeHelperClass.bestTreesInRuns.clear();
				try {
					if (Const.REUSE_EXISTING_TREE == false && isTraining){
						GA.thresholdGPStringUpwardMap.clear();
						throw new MyException("Run Configured not to reuse old GP trees. Will rebuild");
					}
						// open input stream test.txt for reading purpose.
					BufferedReader br = new BufferedReader(new FileReader(GA.log.publicFolder + gpTreeName));
					while ((thisLine = br.readLine()) != null) {
						if (GA.thresholdGPStringUpwardMap.containsKey(delta)) 
							break;
						else
							GA.thresholdGPStringUpwardMap.put(delta, thisLine);
						
					}
				}catch (MyException myException) {
					System.out.println(GA.log.publicFolder + gpTreeName + " " + myException);
				}
				catch (FileNotFoundException fileNotFound) {
					System.out.println(GA.log.publicFolder + gpTreeName + " not found. Will rebuild GP tree.");
					
				} catch (IOException io) {
					System.out.println("IO exception occured. Will loading" + GA.log.publicFolder + gpTreeName
							+ ". Will rebuild GP tree.");
					// io.printStackTrace();
				} catch (Exception e) {
					System.out.println("Unknown error occured. Will loading" + GA.log.publicFolder + gpTreeName
							+ ". Will rebuild GP tree.");
					
					// e.printStackTrace();
				}

				if (GA.thresholdGPStringUpwardMap.containsKey(delta)) {
					gpTreeInFixNotation = GA.thresholdGPStringUpwardMap.get(delta);
				} else {
					if (treeHelperClass.bestTreesInRuns != null)
						treeHelperClass.bestTreesInRuns.clear();
					//GA.log.save("Testing.txt", "test");
					treeHelperClass.getBestTreesForThreshold(uptrendEvent, Const.POP_SIZE, 1, Const.MAX_GENERATIONS, thresholdStr);
					Comparator<AbstractNode> comparator = Collections.reverseOrder();
					Collections.sort(treeHelperClass.bestTreesInRuns, comparator);

					if (treeHelperClass.bestTreesInRuns.isEmpty() || treeHelperClass.bestTreesInRuns.size() < 1) {
					//	System.out.println("treeHelperClass.bestTreesInRuns.isEmpty()");
						System.exit(-1);
					}
					// get best tree
					AbstractNode tree = treeHelperClass.bestTreesInRuns.get(treeHelperClass.bestTreesInRuns.size() - 1);
					String treeAsInfixNotationString = tree.printAsInFixFunction();
					treeAsInfixNotationString = treeAsInfixNotationString.replaceAll("\n", "").replaceAll("\r", "");
					treeAsInfixNotationString = treeAsInfixNotationString.replaceAll("\\r|\\n", "");
					// FWriter writer = new FWriter(GA.log.publicFolder +
					// gpTreeName); used this if fails
					GA.log.delete(gpTreeName);
					GA.log.save(gpTreeName, treeAsInfixNotationString);
					// writer.openToAppend(new File(GA.log.publicFolder +
					// gpTreeName));
					// writer.write(treeAsInfixNotationString);
					// writer.closeFile();
					GA.thresholdGPStringUpwardMap.put(delta, treeAsInfixNotationString);
					gpTreeInFixNotation = treeAsInfixNotationString;
				}

				for (int outputIndex = 0; outputIndex < output.length; ++outputIndex) {
					String foo = gpTreeInFixNotation;
					foo = foo.replace("X0", Integer.toString(output[outputIndex].length()));
					double eval = 0.0;
					Double javascriptValue = Double.MAX_VALUE ;
					if ((this.output[outputIndex].overshoot == null  || this.output[outputIndex].overshoot.length() == 0 )
							&& Const.INCLUDE_ZERO_OS_ITEMS == false )
					{
						;
					}
					else
					{
						String classificationStr = "no";
						int indexOf = events.indexOf(output[outputIndex]);
						//System.out.println("index is" + indexOf);
						if (indexOf > 0){
						Event eventInEventCollection = events.get(indexOf);
						
							if(isTraining)
								classificationStr  = preprocess.classifyTrainingInstance(indexOf);
							else
								classificationStr  = preprocess.classifyTestInstance(indexOf);
						}
						if ((classificationStr.compareToIgnoreCase("no") == 0))
						{
							;
						}
						else
						{
							try {
								javascriptValue = (Double) engine.eval(foo);
								eval = javascriptValue.doubleValue();
							} catch (ScriptException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							catch (ClassCastException e){
								 Integer integerObject = new Integer(output[outputIndex].length());
									eval = integerObject.doubleValue();
							}
						}
					}
					if (output[outputIndex].type ==  Type.Upturn){
						 Integer integerObject = new Integer(output[outputIndex].length());
						int retval = Double.compare(0.0, integerObject.doubleValue());
						if (retval > 0)
							numberOfNegativeUpwardEventGP = numberOfNegativeUpwardEventGP + 1;
							
					}
					if  ( eval == Double.MAX_VALUE || eval == Double.NEGATIVE_INFINITY ||
							Double.toString(eval)== "Infinity" ||  eval ==  Double.NaN ||  
							
							Double.isNaN(eval) || 
							Double.isInfinite(eval) || eval == Double.POSITIVE_INFINITY)
					{
						 Integer integerObject = new Integer(output[outputIndex].length());
						 eval = integerObject.doubleValue() * (double)GA.NEGATIVE_EXPRESSION_REPLACEMENT;
					}
					
					if (eval < 0 ){
						 Integer integerObject = new Integer(output[outputIndex].length());
						 eval = integerObject.doubleValue() * (double)GA.NEGATIVE_EXPRESSION_REPLACEMENT;
					}
					
					BigDecimal bd = null;
					BigDecimal bd2 = null;
					try{
						bd = new BigDecimal(eval);
						bd2 = new BigDecimal(Double.toString(eval));
					}
					catch(NumberFormatException e){
						Integer integerObject = new Integer(output[outputIndex].length());
						 eval = integerObject.doubleValue() * (double)GA.NEGATIVE_EXPRESSION_REPLACEMENT;
					}
					
					
					if (output[outputIndex].type ==  Type.Upturn){
						numberOfUpwardEvent = numberOfUpwardEvent +1;
						GpUpwardoutput[outputIndex] = output[outputIndex].length()+  eval;
					}
						
				}
				
							
				
				//Downward trend GP here	
				thresholdStr = String.format("%.5f", delta);
				gpTreeName = Const.DOWNWARD_EVENT_STRING+thresholdStr + ".txt";
				thisLine = null;
				trendOfChoiceVec.clear();
				
				for (int i =0; i< this.events.length; ++i)
				{
					if (this.events[i].type ==  Type.Downturn){
					/*	if ((this.events[i].overshoot == null  || this.events[i].overshoot.length() == 0 )
								&& Const.INCLUDE_ZERO_OS_ITEMS == false ){
							Event _os = new Event(this.events[i].end, this.events[i].end,this.events[i].type);
							this.events[i].overshoot = _os;
							//continue;
						}*/
					
							String classificationStr =  "no";
							if (isTraining){
								classificationStr  = preprocess.classifyTrainingInstance(i);
							}
							else
							{
								//System.out.println(preprocess.test.numInstances());
								if (preprocess.testInstances.numInstances() >=  i)
									classificationStr = preprocess.classifyTestInstance(i);
								else
									classificationStr = "no";
							}
							
							//System.out.println("prediction "+training.instance(eventCount).stringValue(training.attribute(training.numAttributes() - 1)));
							//System.out.println("Classification : " + classificationStr);
							if ((classificationStr.compareToIgnoreCase("no") == 0))
							{
								;
							}
							else
								trendOfChoiceVec.add(this.events[i]);
						
					}
				}
				//Repopulate output[]
				//TODO only include event classified as having OS
				 Event[] downtrendEvent = trendOfChoiceVec.toArray(new Event[trendOfChoiceVec.size()]);
				 
				try {
					if (Const.REUSE_EXISTING_TREE == false && isTraining){
						GA.thresholdGPStringDownwardMap.clear();
						throw new MyException("Run Configured not to reuse old GP trees. Will rebuild");
					}
					// open input stream test.txt for reading purpose.
					BufferedReader br = new BufferedReader(new FileReader(GA.log.publicFolder + gpTreeName));
					while ((thisLine = br.readLine()) != null) {
						if (GA.thresholdGPStringDownwardMap.containsKey(delta))
							break;
						else
						GA.thresholdGPStringDownwardMap.put(delta, thisLine);
						//System.out.println(thisLine);
					}
				}catch (MyException myException) {
					System.out.println(GA.log.publicFolder + gpTreeName + " " + myException);
					if (treeHelperClass.bestTreesInRuns != null)
						treeHelperClass.bestTreesInRuns.clear();
					
						
					// fileNotFound.printStackTrace();
				} catch (FileNotFoundException fileNotFound) {
					System.out.println(GA.log.publicFolder + gpTreeName + " not found. Will rebuild GP tree.");
					if (treeHelperClass.bestTreesInRuns != null)
						treeHelperClass.bestTreesInRuns.clear();
				} catch (IOException io) {
					System.out.println("IO excption occured. Will loading" + GA.log.publicFolder + gpTreeName
							+ ". Will rebuild GP tree.");
					
				} catch (Exception e) {
					System.out.println("Unknown error occured. Will loading" + GA.log.publicFolder + gpTreeName
							+ ". Will rebuild GP tree.");
					
				}

				if (GA.thresholdGPStringDownwardMap.containsKey(delta)) {
					gpTreeInFixNotation = GA.thresholdGPStringDownwardMap.get(delta);
				} else {
					if (treeHelperClass.bestTreesInRuns != null)
						treeHelperClass.bestTreesInRuns.clear();
					treeHelperClass.getBestTreesForThreshold(downtrendEvent,  Const.POP_SIZE, 1, Const.MAX_GENERATIONS, thresholdStr);
					Comparator<AbstractNode> comparator = Collections.reverseOrder();
					Collections.sort(treeHelperClass.bestTreesInRuns, comparator);

					if (treeHelperClass.bestTreesInRuns.isEmpty() || treeHelperClass.bestTreesInRuns.size() < 1) {
						System.out.println("treeHelperClass.bestTreesInRuns.isEmpty()");
						System.exit(-1);
					}
					// get best tree
					AbstractNode tree = treeHelperClass.bestTreesInRuns.get(treeHelperClass.bestTreesInRuns.size() - 1);
					String treeAsInfixNotationString = tree.printAsInFixFunction();
					treeAsInfixNotationString = treeAsInfixNotationString.replaceAll("\n", "").replaceAll("\r", "");
					treeAsInfixNotationString = treeAsInfixNotationString.replaceAll("\\r|\\n", "");
					treeAsInfixNotationString = treeAsInfixNotationString.replaceAll("\\n", "");
					// FWriter writer = new FWriter(GA.log.publicFolder +
					// gpTreeName); used this if fails
					GA.log.delete(gpTreeName);
					GA.log.save(gpTreeName, treeAsInfixNotationString);
					// writer.openToAppend(new File(GA.log.publicFolder +
					// gpTreeName));
					// writer.write(treeAsInfixNotationString);
					// writer.closeFile();
					GA.thresholdGPStringDownwardMap.put(delta, treeAsInfixNotationString);
					gpTreeInFixNotation = treeAsInfixNotationString;
				}

				for (int outputIndex = 0; outputIndex < output.length; outputIndex++) {
					String foo = gpTreeInFixNotation;
					foo = foo.replace("X0", Integer.toString(output[outputIndex].length()));
					double eval = 0.0;
					Double javascriptValue = Double.MAX_VALUE ;
					if ((output[outputIndex].overshoot == null  || output[outputIndex].overshoot.length() == 0 )
							&& Const.INCLUDE_ZERO_OS_ITEMS == false )
					{
						;
					}
					else
					{
						String classificationStr= "no";
						int indexOf = events.indexOf(output[outputIndex]);
						//System.out.println("index is" + indexOf);
						if (indexOf > 0){
						Event eventInEventCollection = events.get(indexOf);
						
						
						if(isTraining)
							classificationStr  = preprocess.classifyTrainingInstance(indexOf);
						else
							classificationStr  = preprocess.classifyTestInstance(indexOf);;
						}
						if ((classificationStr.compareToIgnoreCase("no") == 0))
						{
							;
						}
						else
						{
							try {
								 javascriptValue = (Double) engine.eval(foo);
								eval = javascriptValue.doubleValue();
							} catch (ScriptException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							catch (ClassCastException e){
								 Integer integerObject = new Integer(output[outputIndex].length());
									eval = integerObject.doubleValue();
							}
						}
					}
					if (output[outputIndex].type ==  Type.Downturn){
						 Integer integerObject = new Integer(output[outputIndex].length());
						int retval = Double.compare(0.0, integerObject.doubleValue());
						if (retval > 0)
							numberOfNegativeDownwardEventGP = numberOfNegativeDownwardEventGP + 1;
							
					}
					if  ( eval == Double.MAX_VALUE || eval == Double.NEGATIVE_INFINITY ||
							Double.toString(eval)== "Infinity" ||  eval ==  Double.NaN ||  
							Double.isNaN(eval) || 
							Double.isInfinite(eval) || eval == Double.POSITIVE_INFINITY)
					{
						 Integer integerObject = new Integer(output[outputIndex].length());
						 eval = integerObject.doubleValue() * (double)GA.NEGATIVE_EXPRESSION_REPLACEMENT;
					}
					
					if (eval < 0 ){
						 Integer integerObject = new Integer(output[outputIndex].length());
						 eval = integerObject.doubleValue() * (double)GA.NEGATIVE_EXPRESSION_REPLACEMENT;
					}
					
					BigDecimal bd = null;
					BigDecimal bd2 = null;
					try{
						bd = new BigDecimal(eval);
						bd2 = new BigDecimal(Double.toString(eval));
					}
					catch(NumberFormatException e){
						Integer integerObject = new Integer(output[outputIndex].length());
						 eval = integerObject.doubleValue() * (double)GA.NEGATIVE_EXPRESSION_REPLACEMENT;
					}
					
					
					//
					
					
					 if (output[outputIndex].type ==  Type.Downturn){
							numberOfDownwardEvent = numberOfDownwardEvent +1;
							GpDownwardoutput[outputIndex] = output[outputIndex].length() + eval;
						}
					
				}


			} 
		}
		
		if (isTraining) {
			// downward overshoots
			meanRatio[0] = 0.0;
			medianRatio[0] = 0.0;
			AverageDCEventLength[0] = 0.0;

			double meanDownturn = 0.0;
			double meanDownwardOvershoot = 0.0;

			int downturn = 0;
			// upward overshoots
			meanRatio[1] = 0.0;
			medianRatio[1] = 0.0;
			AverageDCEventLength[1] = 0.0;

			double meanUpturn = 0.0;
			double meanUpwardOvershoot = 0.0;

			int upturn = 0;

			ArrayList<Double> downwardRatio = new ArrayList<Double>();
			ArrayList<Double> upwardRatio = new ArrayList<Double>();

			for (Event e : this.events) {
				// we ignore the first (artificial) event
				if (e.start > 0) {
					double ratio = 0.0;

					if (e.overshoot != null) {
						ratio = (e.overshoot.length() / (double) e.length());
					}

					if (e.type == Type.Upturn) {
						upturn++;
						meanRatio[1] += ratio;
						upwardRatio.add(ratio);

						meanUpturn += e.length();
						meanUpwardOvershoot += (e.overshoot != null) ? e.overshoot.length() : 0;
					} else if (e.type == Type.Downturn) {
						downturn++;
						meanRatio[0] += ratio;
						downwardRatio.add(ratio);

						meanDownturn += e.length();
						meanDownwardOvershoot += (e.overshoot != null) ? e.overshoot.length() : 0;

					}
				}
			}

			// meanRatio[0] /= downturn;
			// meanRatio[1] /= upturn;

			meanRatio[0] = (meanDownwardOvershoot / downturn) / (meanDownturn / downturn);
			meanRatio[1] = (meanUpwardOvershoot / upturn) / (meanUpturn / upturn);
			AverageDCEventLength[0] = meanDownturn / downturn;
			AverageDCEventLength[1] = meanUpturn / upturn;

			// Collections.sort(downwardRatio);
			// Collections.sort(upwardRatio);

			// medianRatio[0] = downwardRatio.get(downwardRatio.size() / 2);
			// medianRatio[1] = upwardRatio.get(upwardRatio.size() / 2);
		}
	}

	protected Event detect(Type type, int[] indexDC, int[] indexOS) {
		// overshoot event must have a start index lower that
		// the DC event start index
		if (indexOS[0] < indexOS[1] && indexOS[0] < indexDC[0]) {
			return new Event(indexOS[0], indexOS[1], type);
		}

		return null;
	}

	protected void adjust(Event last, int[] indexDC, int[] indexOS) {
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

	public enum Type {
		Upturn, Downturn, UpwardOvershoot, DownwardOvershoot;
	}

	public static class Event {
		public int start = 0;
		public int end = 0;
		public double low = 0.0;
		public double high = 0.0;
		public Type type;
		public Event overshoot;
		public double DCValue = 0.0;
		public String startPrice = "";
		public String endPrice = "";
		public String startDate = "";
		public String endDate  = "";
		public String startTime = "";
		public String endTime = "";
		public String percentageDeltaDuration = "";
		public String percentageDeltaPrice = "";
		public String sigma = "";
		public String osv_ext = "";
		public String tmv = "";
		public String hasOverShoot="no";
		public String previousDChadOvershoot="no";
		//public String _2previousDChadOvershoot="no";
		public String isFlashEvent="no";
		public String PreviousDCPrice ="";
		public ArrayList<FReader.FileMember2> datapoints = null;
		public ArrayList<FReader.FileMember2> tickDatapoints = null;
		public double averageNoOfTicksInDC;
		public double averageNoOfTicksInOS;
		public double highestTick;
		public double lowestTick;
		public String startAskPrice =  "";
		public String startBidPrice =  "";
		public String endAskPrice =  "";
		public String endBidPrice =  "";
		public int positionInPriceCurve=0;
		
		
		
		//public String 
		
		public Event(Event e){
			this.start = e.start;
			this.end = e.end;
			this.low = e.low;
			this.high = e.high;
			this.type = e.type;
			this.DCValue = e.DCValue;
			this.overshoot = e.overshoot;
			this.hasOverShoot = e.hasOverShoot;
			this.datapoints = new ArrayList<FReader.FileMember2>(e.datapoints);
			this.endPrice =  e.endPrice;
			this.startDate = e.startDate;
			this.endDate = e.endDate;
			this.startTime = e.startTime;
			this.endTime = e.endTime;
			this.startPrice = e.startPrice;
			this.percentageDeltaDuration = e.percentageDeltaDuration;
			this.percentageDeltaPrice = e.percentageDeltaPrice;
			
		}

		public Event(int start, int end, Type type) {
			this.start = start;
			this.end = end;
			this.type = type;
			this.low = low;
			this.high = high;
		}
		
		public Event(int start, int end, Type type, double value) {
			this.start = start;
			this.end = end;
			this.type = type;
			this.DCValue = value;
		}

		@Override
		public String toString() {
			return String.format("%4d %4d   %s   %4f", start, end, type, DCValue);
		}

		public final int length() {
			return (end - start) + 1;
		}
		
		 @Override
		    public boolean equals(Object o) {
		 
		        // If the object is compared with itself then return true  
		        if (o == this) {
		            return true;
		        }
		 
		        /* Check if o is an instance of Complex or not
		          "null instanceof [type]" also returns false */
		        if (!(o instanceof Event)) {
		            return false;
		        }
		         
		        // typecast o to Complex so that we can compare data members 
		        Event c = (Event) o;
		         
		        // Compare the data members and return accordingly
		        if (c.start == this.start && c.end == this.end && c.type == this.type)
		        	return true;
		        else
		        	return false;
		    }
	}
	
	double trainingTrading(PreProcess preprocess){
		try
        { 
            throw new NullPointerException("Invalid Call"); 
        } 
        catch(NullPointerException e) 
        { 
            System.out.println("Invalid call DCCurve:trainingTrading."); 
            System.exit(-1);  
        }
		return Double.MIN_VALUE; 
	}

	 public void build(Double[] values, double delta, String GPTreeFileName, Event[] events, PreProcess preprocess){
		 try
	        { 
	            throw new NullPointerException("Invalid Call"); 
	        } 
	        catch(NullPointerException e) 
	        { 
	            System.out.println("Invalid call DCCurve:build(Double[] values, double delta, String GPTreeFileName, Event[] events, PreProcess preprocess)"
	            		+ "."); 
	            System.exit(-1);  
	        }
		
	}
	 
	public void estimateTraining(PreProcess preprocess) {
		try
        { 
            throw new NullPointerException("Invalid Call"); 
        } 
        catch(NullPointerException e) 
        { 
            System.out.println("Invalid call DCCurve:estimateTraining(PreProcess preprocess)"
            		+ "."); 
            System.exit(-1);  
        }
	
	}
	public Instances getTestInstance(){
		return testInstance;
	}
	
	public void setTestInstance(Instances instances){
		 testInstance  = new Instances(instances);;
	}
	
	public void setTestInstance(Event[] output, Instances tInstance){
		testInstance = new Instances(tInstance,output.length);
		preprocess.loadTestData();
		for (int e =0 ; e<output.length; e++){
			int eventIndex = -1;
			Event targetEvent = output[e];
			try {
				eventIndex = IntStream.range(0, events.length)
						.filter(index -> targetEvent.equals(events[index])).findFirst().orElse(-1);

			} catch (NullPointerException ne) {

				System.out.println(ne.toString());
			}
			
			if (eventIndex > -1){
				tInstance.get(eventIndex);
				testInstance.add(e, tInstance.get(eventIndex));
			}
			else
			{
				System.out.print("An error occured while copying instance");
				System.exit(0);
			}

		}
		
	}

	public Instances getOutputTestInstance(){
			return outputTestInstance;
	}

	public void setOutputTestInstance(Event[] output){
		outputTestInstance = new Instances(testInstance,output.length);
		
		for (int e =0 ; e<output.length; ++e){
			int eventIndex = -1;
			Event targetEvent = output[e];
			try {
				eventIndex = IntStream.range(0, events.length)
						.filter(index -> targetEvent.equals(events[index])).findFirst().orElse(-1);

			} catch (NullPointerException ne) {

				System.out.println(ne.toString());
			}
			
			if (eventIndex > -1){
				
				if (eventIndex == events.length)
					outputTestInstance.add(e, testInstance.get(eventIndex-1));
				else
				{
					try{
						outputTestInstance.add(e, testInstance.get(eventIndex));
					}catch(IndexOutOfBoundsException inErr){
						System.out.println("An error occured" + e );
					}
					
				}
			}
			else
			{
				System.out.print("An error occured while copying instance");
				System.exit(0);
			}

		}
		
	}
	
	public Instances getTrainingInstance(){
		return new Instances(trainingInstance);
	}
	
	
	
	public void setTrainingInstance(Instances instances){
		trainingInstance = new Instances(instances);
		
	}
	
	
	

	public void setTrainingInstance(Event[] output){
		
		trainingInstance = new Instances(preprocess.getTrainingInstanceAuto(),output.length);
	
		for (int e =0 ; e<output.length; e++){
			int eventIndex = -1;
			Event targetEvent = output[e];
			try {
				eventIndex = IntStream.range(0, events.length)
						.filter(index -> targetEvent.equals(events[index])).findFirst().orElse(-1);

			} catch (NullPointerException ne) {

				System.out.println(ne.toString());
			}
			
			if (eventIndex > -1){
				//Instance inst = new DenseInstance(7);
				//inst.copy((preprocess.getTrainingInstanceAuto().get(eventIndex)).get);
				
				trainingInstance.add(e, preprocess.getTrainingInstanceAuto().get(eventIndex));
			}
			else
			{
				System.out.print("An error occured while copying instance");
				System.exit(0);
			}

		}
		trainingInstance.setClassIndex(trainingInstance.numAttributes() - 1);
	}
	
	void predictionOnly(){
		predictionUpward =  new double[output.length];
		predictionDownward =  new double[output.length];
		for( int i = 0; i < output.length -1; i++){
			
			
			predictionUpward[i] = 0.0;
			predictionDownward[i] = 0.0;
			double eval = 0.0;
			String foo = "";
			Double javascriptValue = Double.MAX_VALUE;
			BigDecimal bd = null;
			BigDecimal bd2 = null;
			if (output[i].type == Type.Upturn){
				if (Const.OsFunctionEnum == Const.function_code.eGP ){
					foo = gpRatio[1];
					foo = foo.replace("X0", Integer.toString(output[i].length()));
					
					try {
						javascriptValue = (Double) engine.eval(foo);
						eval = javascriptValue.doubleValue();
						
						if ( Double.isInfinite(eval) || Double.isNaN(eval))
						{
							;//System.out.println("I am here 1");
						}
					} catch (ScriptException e) {
						eval = output[i].length();
						;
					} catch (ClassCastException e) {
						eval = output[i].length();
						;
					}catch (Exception e) {
						System.out.println(e.toString());
					}
					if  ( eval == Double.MAX_VALUE || eval == Double.NEGATIVE_INFINITY ||
							eval == Double.POSITIVE_INFINITY || eval ==  Double.NaN ||
							Double.compare(eval, 0.0)  < 0  || Double.isInfinite(eval)  || Double.isNaN(eval)){
						eval = ((Integer ) output[i].length()).doubleValue();
					}
							
					try {
						bd = new BigDecimal(eval);
						bd2 = new BigDecimal(Double.toString(eval));
					} catch (NumberFormatException e) {
						Integer integerObject = output[i].length();
						eval = integerObject.doubleValue() ;//* (double) GA_new.NEGATIVE_EXPRESSION_REPLACEMENT;
						
					}
				}
				 else if (Const.OsFunctionEnum == Const.function_code.eMichaelFernando ||
						 Const.OsFunctionEnum == Const.function_code.eOlsen ){
					 
					 eval = meanRatio[1];
				 }
				 else{
					 System.out.print("Invalid upward regression type");
					 System.exit(-1);
				 }
				predictionUpward[i] = eval;
			}
			else if (output[i].type == Type.Downturn){
				if (Const.OsFunctionEnum == Const.function_code.eGP ){
					foo = gpRatio[0];
					foo = foo.replace("X0", Integer.toString(output[i].length()));
					eval = 0.0;
					javascriptValue = Double.MAX_VALUE;
					try {
						javascriptValue = (Double) engine.eval(foo);
						eval = javascriptValue.doubleValue();
						if  ( eval == Double.MAX_VALUE || eval == Double.NEGATIVE_INFINITY ||
								eval == Double.POSITIVE_INFINITY || eval ==  Double.NaN ||
								Double.compare(eval, 0.0)  < 0  || Double.isInfinite(eval)  || Double.isNaN(eval)){
							eval = ((Integer ) output[i].length()).doubleValue();
						}
					} catch (ScriptException e) {
						eval = output[i].length();
						if ( Double.isInfinite(eval) || Double.isNaN(eval))
						{
							;//System.out.println("I am here 2");
						}
					} catch (ClassCastException e) {
						eval = output[i].length();
						;
					}
					bd = null;
					bd2 = null;
					try {
						bd = new BigDecimal(eval);
						bd2 = new BigDecimal(Double.toString(eval));
					} catch (NumberFormatException e) {
						Integer integerObject = new Integer(output[i].length());
						eval = integerObject.doubleValue() * (double) GA_new.NEGATIVE_EXPRESSION_REPLACEMENT;
					}
				}
				else if (Const.OsFunctionEnum == Const.function_code.eMichaelFernando ||
						 Const.OsFunctionEnum == Const.function_code.eOlsen ){
					 eval = meanRatio[0];
				 }
				 else{
					 System.out.print("Invalid downward regression type");
					 System.exit(-1);
				 }
			predictionDownward[i] = eval;
			}

		}
	}
	

	//RMSE without classification
	public String calculateRMSE(Event[] trendEvent) {
		
		double rmse = 0.0;
		double predictionRmse = 10.0;;
		for (int eventCount = 1; eventCount < trendEvent.length; eventCount++) {
			int os = 0;

			if (trendEvent.length != predictionUpward.length ||
					trendEvent.length != predictionDownward.length) {
				System.out.println("Event and prediction not equal");
				System.exit(0);
			}

			if (trendEvent[eventCount].overshoot != null) {
				os = trendEvent[eventCount].overshoot.length();
				// numberOfTestOvershoot = numberOfTestOvershoot + 1;
			}
			double prediction = 0.0;

			// numberOfTestDC = trendEvent.length;
			if (trendEvent[eventCount].type == Type.Upturn){
			 prediction = predictionUpward[eventCount];
			}
			else if (trendEvent[eventCount].type == Type.Downturn){
				prediction = predictionDownward[eventCount];
			}

			// System.out.println("DC:" + trendEvent[eventCount].length() + "
			// OS:" + os + " prediction:" + prediction);
			rmse = rmse + ((os - prediction) * (os - prediction));

			if (rmse == Double.MAX_VALUE || rmse == Double.NEGATIVE_INFINITY || rmse == Double.NEGATIVE_INFINITY
					|| rmse == Double.NaN || Double.isNaN(rmse) || Double.isInfinite(rmse)
					|| rmse == Double.POSITIVE_INFINITY) {
				System.out.println("Invalid RMSE: " + rmse + ". discarding ");
				// predictionRmseClassifier= 10.0;
				return Double.toString(predictionRmse);
			}
		}

		predictionRmse = Math.sqrt(rmse / (trendEvent.length - 1));
		BigDecimal bd = null;
		BigDecimal bd2 = null;
		try {
			bd = new BigDecimal(predictionRmse);
			bd2 = new BigDecimal(Double.toString(predictionRmse));
			if (predictionRmse >= Double.MAX_VALUE)
				return Double.toString(10.0);
		} catch (NumberFormatException e) {
			System.out.println("Invalid predictionRmseClassifier: " + predictionRmse + " discarding ");
			predictionRmse = 10.0;
		}
		// System.out.println(predictionRmse);
		return Double.toString(predictionRmse);
	}


	public  String  reportClassificationResult(Event[] trendEvent){
	
		if (preprocess == null)
			return "";
		
		String classificationResult = preprocess  
				.printPreprocessClassification(trendEvent);
		
		
		if (classificationResult == "" || classificationResult == null){
			System.out.println("Classification string is empty");
			return "";
		}
		
		return classificationResult;
	}

	
	protected void estimateTestRMSE() {
		System.out.println("estimateRMSEs : This must never be called");
		System.exit(-1);
	}

}