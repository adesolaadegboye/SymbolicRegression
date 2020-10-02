package misc;

import static dc.ga.DCCurve.Type.Downturn;
import static dc.ga.DCCurve.Type.DownwardOvershoot;
import static dc.ga.DCCurve.Type.Upturn;
import static dc.ga.DCCurve.Type.UpwardOvershoot;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import dc.GP.AbstractNode;
import dc.GP.Const;
import dc.ga.HelperClass;
import dc.ga.PreProcess;
import dc.ga.DCCurve.Event;
import dc.ga.DCCurve.Type;
import dc.io.FReader;
import dc.io.FReader.FileMember2;

public abstract class DCCurveRegression {
	
	public Event[] testingEvents;
	public Event[] trainingEvents;
	public Event[] trainingOutputEvents;
	

	List<FReader.FileMember2> testDataList = new ArrayList<FReader.FileMember2>();
	List<FReader.FileMember2> trainingDataList = new ArrayList<FReader.FileMember2>();
	
	protected double[] gpprediction;
	protected double[] predictionWithClassifier;
	
	protected double[] trainingGpPrediction;
	protected double[] trainingGpPredictionUsingOutputData;
	
	double predictionRmse;
	protected double OpeningPosition = 500000.00;
	
	double OpeningPositionHist = 0.0;
	double trainingOpeningPositionHist = 0.0;
	
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

	public AbstractNode gptree = null;
	public AbstractNode bestUpWardEventTree = null;
	public AbstractNode bestUpWardEventMagnitudeTree = null;
	public AbstractNode bestDownWardEventTree = null;
	public AbstractNode bestDownWardEventMagnitudeTree = null;
	
	public AbstractNode bestclassifierBasedUpWardEventTree = null;
	public AbstractNode bestclassifierBasedDownWardEventTree = null;
	
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
	
	public double   thresholdValue = -0.1;
	protected boolean  isSelectedThresholdFromCandidateList = false;
	protected double   associatedWeight = 0.0;
	protected double   varianceValue = 0.0;
	protected double   tradingReturnValaue = 0.0;
	
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

	abstract public void build(Double[] values, double delta, String GPTreeFileName, Event[] events, Event[] output, PreProcess preprocess);
	
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
	
	public double getSharpRatio(){
		return simpleSharpeRatio.calulateSharpeRatio();
	}
	
	public String getUpwardTrendTreeString(){
		return upwardTrendTreeString;
	}
	
	public String getDownwardTrendTreeString(){
		return downwardTrendTreeString;
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
				simpleSharpeRatio.addReturn(positionArrayBase.get(srCount) - positionArrayBase.get(srCount-1)) ;
				simpleDrawDown.Calculate(positionArrayBase.get(srCount-1));
			}
			simpleDrawDown.Calculate(positionArrayBase.get(positionArrayBase.size() -1));
		}
		
		
		
		
	//	else if (positionArrayBase.size() == 1)
	//		simpleSharpeRatio.addReturn(positionArrayBase.get(0) - OpeningPosition);
		// closingPosition = currentPosition-initialPosition;
	/*
	 * not in use at the moment
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
*/
		

		/*
		 * 
		 * 
		 not in use at the moment
		for (int profitLossCount = 0; profitLossCount < positionArrayQuote.size(); profitLossCount++) {
			simpleDrawDownQuote.Calculate(positionArrayQuote.get(profitLossCount));
		}
*/
		peakMDD = simpleDrawDown.getMaxDrawDown();
	//	peakMDDQuote = simpleDrawDownQuote.getMaxDrawDown();


	}
	
	public void clearSharpRatio(){
		simpleSharpeRatio.rmoveAllReturns();
	}
	
	public double getVariance(){
		return simpleSharpeRatio.calulateVariance();
	}
	
	public void refreshMDD(){
		simpleDrawDown.clearMDD();
		simpleDrawDownQuote.clearMDD();
		
	}
	
	public void setMarketdataListTraining(int counter){
		
		for (int i = 0; i < counter; i++) {
			trainingDataList.add(FReader.dataRecordInFileArray.get(i));
		}
	}
	
	public List<FReader.FileMember2>  getMarketDataTraining(){
		List<FReader.FileMember2> bidAskprice;
		bidAskprice =  new ArrayList<FReader.FileMember2>(trainingDataList);
		return bidAskprice;
	}
	
	public List<FReader.FileMember2>  getMarketDataTest(){
		List<FReader.FileMember2> bidAskprice;
		bidAskprice =  new ArrayList<FReader.FileMember2>(testDataList);
		return bidAskprice;
	}
	
	public void setMarketdataListTest(int counter){
		if (trainingDataList.isEmpty()){
			
			System.out.println("trainingDataList.isEmpty() existing");
			System.exit(-1);
		}
		
		int trainingDataSize = trainingDataList.size();
		for (int i = trainingDataSize; i < (counter + trainingDataSize); i++) {
			testDataList.add(FReader.dataRecordInFileArray.get(i));
		}
		
	}
	
	public void  setOpeningPosition (double position)
	{
		OpeningPosition = position;
	}
	
	
	public double  getOpeningPosition ()
	{
		return OpeningPosition;
	}
	
	public void  setOpeningPositionHist (double position)
	{
		OpeningPositionHist = position;
	}
	
	
	public double  getOpeningPositionHist ()
	{
		return OpeningPositionHist;
	}  
	
	public void  setTrainingOpeningPositionHist (double position)
	{
		trainingOpeningPositionHist = position;
	}
	
	
	public double  getTrainingOpeningPositionHist ()
	{
		return trainingOpeningPositionHist;
	} 
	
	public void  setTrainingOpeningPosition (double position)
	{
		trainingOpeningPosition = position;
	}
	
	
	public double  getTrainingOpeningPosition ()
	{
		return trainingOpeningPosition;
	} 
	
	double getMddPeak() {
		return simpleDrawDown.getPeak();
	}

	double getMddTrough() {
		return simpleDrawDown.getTrough();
	}

	public double getMaxMddBase() {
		return simpleDrawDown.getMaxDrawDown();
	}

	double getMddPeakQuote() {
		return simpleDrawDownQuote.getPeak();
	}

	double getMddTroughQuote() {
		return simpleDrawDownQuote.getTrough();
	}

	double getMaxMddQuote() {
		return simpleDrawDownQuote.getMaxDrawDown();
	}

	int getNumberOfQuoteCcyTransactions() {

		return positionArrayQuote.size() - 1;
	}
	
	public void resetNumberOfQuoteCcyTransaction(){
		positionArrayQuote.clear();
	}
	
	int getNumberOfBaseCcyTransactions() {

		return positionArrayBase.size() - 1;
	}
	
	public void resetNumberOfBaseCcyTransaction(){
		positionArrayBase.clear();
	}

	double getBaseCCyProfit() {
		double profit = 0.00;
		ArrayList<Double> profitList = new ArrayList<Double>();
		if (positionArrayBase.size() == 1)
			return 0.00;
		for (int profitLossCount = 1; profitLossCount < positionArrayBase.size(); profitLossCount++) {
			double profitCalculation = positionArrayBase.get(profitLossCount)
					- positionArrayBase.get(profitLossCount - 1) / positionArrayBase.get(profitLossCount - 1);
			profitList.add(profitCalculation);
		}
		profit = profitList.stream().mapToDouble(i -> i.doubleValue()).sum();
		return profit;
	}

	double getQuoteCCyProfit() {
		double profit = 0.00;
		ArrayList<Double> profitList = new ArrayList<Double>();
		if (positionArrayQuote.size() == 1)
			return 0.00;
		// Start from 3rd element because first element is zero
		for (int profitLossCount = 1; profitLossCount < positionArrayQuote.size(); profitLossCount++) {
			double profitCalculation = positionArrayQuote.get(profitLossCount)
					- positionArrayQuote.get(profitLossCount - 1) / positionArrayQuote.get(profitLossCount - 1);
			profitList.add(profitCalculation);
		}
		profit = profitList.stream().mapToDouble(i -> i.doubleValue()).sum();
		return profit;
	}
	
	public void setAssociatedWeight(double weight){
		associatedWeight = weight;
	}
	
	public double getAssociatedWeight(){
		return associatedWeight ;
	}
	
	
	public void setThresholdValue(double value){
		thresholdValue = value;
	}
	public double  getThresholdValue(){
		return thresholdValue;
	}
	
	
	public void setIsSelectedThresholdFromCandidateList(boolean value){
		isSelectedThresholdFromCandidateList = value;
	}
	public boolean  getIsSelectedThresholdFromCandidateList(){
		return isSelectedThresholdFromCandidateList;
	}
	
	
	public void setVarianceValue(double value){
		varianceValue = value;
	}
	public double  getVarianceValue(){
		return simpleSharpeRatio.calulateVariance();
	}
	
	public void setTradingReturnValaue(double value){
		tradingReturnValaue = value;
	}
	public double  getTradingReturnValue(){
		return tradingReturnValaue;
	}
	
	public int getNumberOfTransactions(){
		return positionArrayBase.size();
	}
	
	public void clearPositionArrayBase(){
		positionArrayBase.clear();
	}
	
	public void clearPositionArrayQuote(){
		positionArrayQuote.clear();
	}
	
	public void clearActualTrendString(){
		actualTrendString =  "";
	}
	
	
	
	public void clearPredictedTrendString(){
		predictedTrendString =  "";
	}
	
	public void estimateTrainingUsingOutputData(PreProcess preprocess) {
		trainingGpPredictionUsingOutputData = new double[trainingOutputEvents.length];
		

		for (int outputIndex = 0; outputIndex < trainingOutputEvents.length - 2; outputIndex++) {
			
			trainingGpPredictionUsingOutputData[outputIndex] = HelperClass.estimateOSlength(outputIndex, trainingOutputEvents,
					 bestUpWardEventTree,  bestDownWardEventTree );
			
		}

	}

}
