package misc;

import static dc.ga.DCCurve.Type.Downturn;
import static dc.ga.DCCurve.Type.DownwardOvershoot;
import static dc.ga.DCCurve.Type.Upturn;
import static dc.ga.DCCurve.Type.UpwardOvershoot;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

import dc.GP.AbstractNode;
import dc.GP.Const;



import dc.ga.PreProcess;
import dc.ga.DCCurve.Event;
import dc.ga.DCCurve.Type;
import dc.io.FReader.FileMember2;

public abstract class DCCurveRegression {
	
	Event[] testingEvents;
	Event[] trainingEvents;
	

	protected double[] gpprediction;
	protected double[] predictionWithClassifier;
	protected double[] predictionMagnitudeWithClassifier;
	
	protected double[] trainingGpPrediction;
	protected double[] trainingGpMagnitudePrediction;
	
	
	double predictionRmse;
	protected double OpeningPosition = 500000.00;
	
	protected double StartSellQuantity = -1.0;
	protected double StartBuyQuantity = -1.0;
	
	protected double standardLot = 10000.00;
	protected double trainingOpeningPosition = 500000.00;
	protected double peakMDD = Double.NEGATIVE_INFINITY;
	protected double peakMDDQuote = Double.NEGATIVE_INFINITY;
	protected  ArrayList<FileMember2> dataRecordInFileArrayTest = new ArrayList<FileMember2>();
	protected int lastTrainingPrice = 0;
	protected double profitLoss = 0.0;
	protected ArrayList<LinkedHashMap<Integer,Integer>> anticipatedTrend = new ArrayList<LinkedHashMap<Integer,Integer>>();
	protected ArrayList<Double> tradedPrice = new ArrayList<Double>();
	protected SimpleDrawDown simpleDrawDown = new SimpleDrawDown() ;
	protected SimpleDrawDown simpleDrawDownQuote = new SimpleDrawDown() ;
	protected SimpleSharpeRatio simpleSharpeRatio = new SimpleSharpeRatio();
	protected ArrayList<Double> positionArray  = new ArrayList<Double>();
	protected ArrayList<Double> positionArrayBase  = new ArrayList<Double>();
	protected ArrayList<Double> positionArrayQuote  = new ArrayList<Double>();
	protected ArrayList<LinkedHashMap<Integer,Integer>> actualTrend = new ArrayList<LinkedHashMap<Integer,Integer>>();
	protected abstract int getMaxTransactionSize();
	protected abstract double getTransanction(int i);
	
	protected String actualTrendString = "";
	protected String predictedTrendString = "";

	protected  double lastSellPrice = 0.0;
	protected  double lastBuyPrice = 0.0; 
	int numberOfUpwardEvent;
	int numberOfDownwardEvent;
	int numberOfNegativeUpwardEventGP;
	int numberOfNegativeDownwardEventGP;

	AbstractNode gptree = null;
	AbstractNode bestUpWardEventTree = null;
	AbstractNode bestUpWardEventMagnitudeTree = null;
	AbstractNode bestDownWardEventTree = null;
	AbstractNode bestDownWardEventMagnitudeTree = null;
	
	AbstractNode bestclassifierBasedUpWardEventTree = null;
	AbstractNode bestclassifierBasedDownWardEventTree = null;
	
	protected String upwardTrendTreeString = null;
	protected String upwardTrendMagnitudeTreeString = null;
	protected String downwardTrendTreeString = null;
	protected String downwardTrendMagnitudeTreeString = null;
	protected double[] meanRatio = null;
	protected double[] meanMagnitudeRatio = null;
	String trendTreeString = null;
	

	String gpTreeInFixNotation = null;

	
	public String filename = "";
	protected String thresholdString= "";
	
	protected String testMarketData;
	protected String dcOsPricePoint;
	protected String transactionpoint;
	protected double []transactionProfitLoss;
	
	
	public double zeroPercentageTraining = -1.0;
	public double zeroPercentageTest = -1.0;
	public double dC_OS_Length_RatioTraining = -1.0;
	public double dC_OS_Length_RatioTest = -1.0;
	
	protected double downarddistPerf;
	protected double upwarddistPerf;
	protected double downwardMagnitudePerf;
	protected double upwardMagnitudePerf;
	
	/*
	 * 
	 * TODO Store perfect foresight values here
	 * add setters and getters
	 */

	/**
	 * 0 = downward overshoot 1 = upward overshoot
	 */
	double[] medianRatio = new double[2];
	public Vector<AbstractNode> curve_bestTreesInRunsUpward = new Vector<AbstractNode>();
	public Vector<AbstractNode> curve_bestTreesInRunsUpwardMagnitude = new Vector<AbstractNode>();
	public Vector<AbstractNode> curve_bestTreesInRunsDownwardMagnitude = new Vector<AbstractNode>();
	public Vector<AbstractNode> curve_bestTreesInRuns = new Vector<AbstractNode>();
	public Vector<AbstractNode> curve_bestTreesInRunsDownward = new Vector<AbstractNode>();
	public String runsFitnessStrings = "";
	Map<Integer, double[]> runsPrediction = new HashMap<Integer, double[]>();
	boolean isUpwardEvent = true;


	protected int numberOfTestOvershoot = 0;
	protected int numberOfTestDC=0;
	protected int numberOfTrainingOvershoot = 0;
	protected int numberOfTrainingDC=0;
	protected int totalOvershootEventLengthTraining=0;
	protected int totalDCEventLengthTraining=0;
	protected int totalOvershootEventLengthTest=0;
	protected int totalDCEventLengthTest=0;
	
	private String DCCurveName = null;
	
	
	
	protected abstract double calculateSD();
	//protected abstract double getMax();
	//protected abstract double getMin();
	
	public enum DCCurveType {
		eDCCurveMF,
		eDCCurveOlsen,
		eDCCurveClassification,
		eDCCurveCifre,
		eDCCurvePerfectForesight,
		eDCCurveClassificationMF,
	};
	
	static DCCurveType hashit(String inString) {
		if (inString.contains("DCCurveMF"))   return DCCurveType.eDCCurveMF;
		if (inString.contains("DCCurveOlsen") ) return DCCurveType.eDCCurveOlsen;
		if (inString.contains("DCCurveClassification") ) return DCCurveType.eDCCurveClassification;
		if (inString.contains("DCCurveCifre") ) return DCCurveType.eDCCurveCifre;
		if (inString.contains("DCCurvePerfectForesight") ) return DCCurveType.eDCCurvePerfectForesight;
		if (inString.contains("DCCurveClassificationMF") ) return DCCurveType.eDCCurveClassificationMF;
		
		throw new IllegalArgumentException(); 
		//return string_code.eNone;
	}
	
	
	public DCCurveRegression(){
		positionArrayQuote.add(new Double(0.0));
		positionArrayBase.add(new Double(OpeningPosition));
	}
	
	abstract public String getDCCurveName();

	abstract public void build(Double[] values, double delta, String GPTreeFileName, Event[] events, PreProcess preprocess);
	
	/**
	 * 
	 * @param lastTrainingPricePosition TODO
	 * @param values
	 *            The array with prices/tick data
	 * @param delta
	 *            The threshold value
	 * @param GPTreeFileName
	 *            the name of the file where GP tree is stored
	 * @param
	 */
	
	abstract public void testbuild(int lastTrainingPricePosition, Double[] values, double delta,Event[] testEvents, PreProcess preprocess) ;

	abstract String report(Double[] values, double delta, String GPTreeFileName);
	/*
	public Integer[] testDCEvent() {

		Vector<Integer> dcOsDuration = new Vector<Integer>();

		for (int i = 0; i < this.testEvents.length; i++) {
			// dcOsDuration.add(testOutput[i].length());
			if (this.testEvents[i].overshoot == null)
				dcOsDuration.add(0);
			else
				dcOsDuration.add(this.testEvents[i].overshoot.length());
		}

		Integer[] Directional_changes_length = new Integer[dcOsDuration.size()];

		dcOsDuration.toArray(Directional_changes_length);
		// Directional_changes_length = new double[testOutput.length];
		// System.arraycopy(testOutput, 0, Directional_changes_length, 0,
		// testOutput.length);
		return Directional_changes_length;
	}
	
	public Integer[] trainingDCEvent() {
		Vector<Integer> dcOsDuration = new Vector<Integer>();
		System.out.println("length is" + this.events.length);
		for (int i = 0; i < this.events.length; i++) { //N:B: this.events is the training events
			// dcOsDuration.add(testOutput[i].length());
			if (this.events[i].overshoot == null)
				dcOsDuration.add(0);
			else
				dcOsDuration.add(this.events[i].overshoot.length());
		}
		
		Integer[] Directional_changes_length = new Integer[dcOsDuration.size()];
		
		dcOsDuration.toArray(Directional_changes_length);
		System.out.println("length is" + Directional_changes_length.length);
		
		return Directional_changes_length;
		

	}
	*/

	void generatePredictionCurve(String threshold){
		
	//	String rmseString=thresholdStr+"_GPOnly.txt";
	//	FWriter writer = new FWriter(SymbolicRegression.log.publicFolder + threshold);
	}
	
	abstract double trade(PreProcess preprocess);
	
	abstract double getMddPeak();
	
	abstract double getMddTrough();

	abstract double getMddPeakQuote();
	
	abstract double getMddTroughQuote();
	
	abstract double getMaxMddBase();
	abstract double getMaxMddQuote();
	
	abstract int getNumberOfQuoteCcyTransactions();
	
	abstract int getNumberOfBaseCcyTransactions();
	abstract double getBaseCCyProfit();
	abstract double getQuoteCCyProfit();
	public abstract String getActualTrend();
	public abstract String getPredictedTrend();
	
	public abstract <E> void assignPerfectForesightRegressionModel(E[] inputArray);
	
	
	
	void TestDCEventOSEventDistribution(){
		for (int eventCount = 1; eventCount < testingEvents.length; eventCount++) {
			

			
			if (testingEvents[eventCount].overshoot != null ){
				totalOvershootEventLengthTest  =  totalOvershootEventLengthTest + testingEvents[eventCount].overshoot.length();
				numberOfTestOvershoot = numberOfTestOvershoot + 1;
			}
			totalDCEventLengthTest = totalDCEventLengthTest + testingEvents[eventCount].length();
			
		}
		
		
		
		

		numberOfTestDC = testingEvents.length-1;
		int numberOfZerosTestDC = numberOfTestDC
				- numberOfTestOvershoot;
		zeroPercentageTest = ((float) numberOfZerosTestDC / numberOfTestDC)* 100;
		dC_OS_Length_RatioTest =  ((float) (totalOvershootEventLengthTest /totalDCEventLengthTest) *100);

	}
	
	abstract double trainingTrading(PreProcess preprocess);
	
	abstract void estimateTraining( PreProcess preprocess);
	
	void TrainingDCEventOSEventDistribution(){
		for (int eventCount = 1; eventCount < trainingEvents.length; eventCount++) {
			

			
			if (trainingEvents[eventCount].overshoot != null ){
				totalOvershootEventLengthTraining  = totalOvershootEventLengthTraining +  trainingEvents[eventCount].overshoot.length();
				numberOfTrainingOvershoot = numberOfTrainingOvershoot + 1;
			}
			totalDCEventLengthTraining = totalDCEventLengthTraining +  trainingEvents[eventCount].length();
		}
		numberOfTrainingDC = trainingEvents.length-1;
		int numberOfZerosTraining = numberOfTrainingDC
				- numberOfTrainingOvershoot;
		
		zeroPercentageTraining = ((float) numberOfZerosTraining
				/ numberOfTrainingDC) * 100;
		
		dC_OS_Length_RatioTraining = (totalOvershootEventLengthTraining /totalDCEventLengthTraining) *100;

	}
	
	
	
	String printTestAskMarketData(ArrayList<FileMember2> dataRecordInFileArray )
	{
		String rtStringl ="";
		
		for (FileMember2 mData : dataRecordInFileArray){
			rtStringl = rtStringl + "," + mData.askPrice;
		}
		return rtStringl;
	}
	
	String printTestBidMarketData(ArrayList<FileMember2> dataRecordInFileArray )
	{
		String rtStringl ="";
		
		for (FileMember2 mData : dataRecordInFileArray){
			rtStringl = rtStringl + "," + mData.askPrice;
		}
		return rtStringl;
	}
	
	String printDCAndOSPricePoints(ArrayList<String> DCtype, ArrayList<Double> priceAtDC, ArrayList<Double> priceAtOS)
	{
			String rtStringl ="";
			
			if (DCtype.size() !=  priceAtDC.size() || priceAtDC.size() !=DCtype.size())
				return rtStringl;
			
			 for(int i = 0; i < DCtype.size(); i++) {
				 rtStringl = rtStringl + "," + DCtype.get(i) + priceAtDC.get(i) + "," + priceAtOS.get(i) + "\n" ;
		     }
		
		
		return rtStringl;
	}
	
	String printDCHasOvershoot(ArrayList<String> DCtype, ArrayList<Boolean> hasOvershoot){
		String rtStringl ="";
		return rtStringl;
	}
	
	protected static  double calculateBaseSD(ArrayList<Double>  numArray)
    {
        double sum = 0.0, standardDeviation = 0.0;
        int length = numArray.size();
       
        for(double num : numArray) {
            sum += num;
        }
        double mean = sum/length;
        for(double num: numArray) {
            standardDeviation += Math.pow(num - mean, 2);
        }
        return Math.sqrt(standardDeviation/length-1);
    }
	
	protected static double getMaxValue(ArrayList<Double> numbers){
		  double maxValue = numbers.get(0);
		  for(double num :   numbers){
		    if(num > maxValue){
			  maxValue = num;
			}
		  }
		  return maxValue;
		}
	protected  static double getMinValue(ArrayList<Double> numbers){
	  double minValue = numbers.get(0);
	  for(double num: numbers){
	    if(num < minValue){
		  minValue = num;
		}
	  }
	  return minValue;
	}
	
	protected double getSharpRatio(){
		return simpleSharpeRatio.calulateSharpeRatio();
	}
	
	public String getUpwardTrendTreeString(){
		return upwardTrendTreeString;
	}
	
	public String getDownwardTrendTreeString(){
		return downwardTrendTreeString;
	}
	
	public String getUpwardTrendMagnitudeTreeString(){
		return upwardTrendMagnitudeTreeString;
	}
	
	public String getDownwardTrendMagnitudeTreeString(){
		return downwardTrendMagnitudeTreeString;
	}
	
	public String getThresholdString (){
		return thresholdString;
	}
	
	public double getDownwardTrendRatio(){
		return  meanRatio[0];
	}
	
	public double getSingleRatio(){
		return  meanRatio[0];
	}
	
	public double getUpwardTrendRatio(){
		return  meanRatio[1];
	}
	
	public double getDownwardMagnitudeRatio(){
		return  meanMagnitudeRatio[0];
	}
	
	public double getSingleMagnitudeRatio(){
		return  meanMagnitudeRatio[0];
	}
	
	public double getUpwardMagnitudeRatio(){
		return  meanMagnitudeRatio[1];
	}
	
	
	protected void otherTradeCalculations(){
		
		if (positionArrayBase.size() > 1){
			//Calculate sharp ratio start from 1 as it is a moving window
			for(int srCount = 1 ; srCount < positionArrayBase.size(); srCount++ ){
				simpleSharpeRatio.addReturn(positionArrayBase.get(srCount) - positionArrayBase.get(srCount-1) );
			}
		}
	//	else if (positionArrayBase.size() == 1)
	//		simpleSharpeRatio.addReturn(positionArrayBase.get(0) - OpeningPosition);
		// closingPosition = currentPosition-initialPosition;
		for (LinkedHashMap<Integer, Integer> map : actualTrend) {
			int key = map.keySet().iterator().next();
			int value = map.get(key);

			actualTrendString = actualTrendString + Double.toString(key) + " ," + Double.toString(value) + "\n";

		}

		for (LinkedHashMap<Integer, Integer> map : anticipatedTrend) {
			int key = map.keySet().iterator().next();
			int value = map.get(key);

			predictedTrendString = predictedTrendString + Double.toString(key) + " ," + Double.toString(value) + "\n";

		}

		for (int profitLossCount = 0; profitLossCount < positionArrayBase.size(); profitLossCount++) {
			simpleDrawDown.Calculate(positionArrayBase.get(profitLossCount));
		}

		for (int profitLossCount = 0; profitLossCount < positionArrayQuote.size(); profitLossCount++) {
			simpleDrawDownQuote.Calculate(positionArrayQuote.get(profitLossCount));
		}

		peakMDD = simpleDrawDown.getMaxDrawDown();
		peakMDDQuote = simpleDrawDownQuote.getMaxDrawDown();


	}
}
