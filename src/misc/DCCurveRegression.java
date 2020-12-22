package misc;



import java.util.ArrayList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

	double mySharpeRatio;
	public Event[] testingEvents;
	public Event[] trainingEvents;
	public Event[] trainingOutputEvents;
	private boolean isPositionOpen = false;

	List<FReader.FileMember2> testDataList = new ArrayList<FReader.FileMember2>();
	List<FReader.FileMember2> trainingDataList = new ArrayList<FReader.FileMember2>();

	public double[] gpprediction;
	public double[] predictionWithClassifier;

	protected double[] trainingGpPrediction;
	public double[] trainingUsingOutputData;
	
	

	double predictionRmse;
	protected double OpeningPosition = Const.OPEN_POSITION;
	protected double soldPosition = 0.0;
	
	double OpeningPositionHist = 0.0;
	double trainingOpeningPositionHist = 0.0;

	protected double standardLot = 10000.00;
	protected double trainingOpeningPosition = 500000.00;
	protected double peakMDD = Double.NEGATIVE_INFINITY;
	protected double peakMDDQuote = Double.NEGATIVE_INFINITY;
	protected ArrayList<FileMember2> dataRecordInFileArrayTest = new ArrayList<FileMember2>();
	public int lastTrainingPrice = 0;
	protected double profitLoss = 0.0;
	
	protected ArrayList<Double> tradedPrice = new ArrayList<Double>();
	protected SimpleDrawDown simpleDrawDown = new SimpleDrawDown();
	
	protected SimpleSharpeRatio simpleSharpeRatio = new SimpleSharpeRatio();
	protected ArrayList<Double> positionArray = new ArrayList<Double>();
	protected ArrayList<Double> positionArrayBase = new ArrayList<Double>();
	
	//public Map<Integer, Double > trainingTradesAndDatapointTraded ;
	//public Map<Integer, Double > testTradesAndDatapointTraded;
	public Map<Integer, Type > futurePredictionUpturn =  new LinkedHashMap<Integer, Type>();;
	public Map<Integer, Type > futurePredictionDownturn  = new LinkedHashMap<Integer, Type>();;
	
	protected abstract int getMaxTransactionSize();

	

	protected String actualTrendString = "";
	protected String predictedTrendString = "";

	protected double lastSellPrice = 0.0;
	protected double lastBuyPrice = 0.0;
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
	public String DataSetInfoString =  "DCEventStart \t DCEventEnd, DCEventType \t OSEventStart \t OSEventEnd, OSEventType  \t predictEndOfDC \t "
			+ "nextDCCpoint \t NoOfSeqDCEventInSameDirectionFromDiffThreshold "
			+ "\t NoOfDCeventSkippedBecauseNewOppEventEncountered \t "
			+ "portfolioAfterTransaction \t rolledBackOpenPosition \n" ;
	String gpTreeInFixNotation = null;

	public String filename = "";
	protected String thresholdString = "";

	protected String testMarketData;
	protected String dcOsPricePoint;
	protected String transactionpoint;
	protected double[] transactionProfitLoss;

	public double zeroPercentageTraining = -1.0;
	public double zeroPercentageTest = -1.0;
	public double dC_OS_Length_RatioTraining = -1.0;
	public double dC_OS_Length_RatioTest = -1.0;

	protected double downarddistPerf;
	protected double upwarddistPerf;
	protected double downwardMagnitudePerf;
	protected double upwardMagnitudePerf;

	Set<String> datasetInfo= new HashSet<String>();
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
	protected int numberOfTestDC = 0;
	protected int numberOfTrainingOvershoot = 0;
	protected int numberOfTrainingDC = 0;
	protected int totalOvershootEventLengthTraining = 0;
	protected int totalDCEventLengthTraining = 0;
	protected int totalOvershootEventLengthTest = 0;
	protected int totalDCEventLengthTest = 0;

	public double thresholdValue = -0.1;
	protected boolean isSelectedThresholdFromCandidateList = false;
	protected double associatedWeight = 0.0;
	protected double varianceValue = 0.0;
	protected double tradingReturnValaue = 0.0;

	
	// protected abstract double getMax();
	// protected abstract double getMin();
	
	public  double calculateSD(){
		return calculateBaseSD(positionArrayBase);
	}
	

	public enum DCCurveType {
		eDCCurveMF, eDCCurveOlsen, eDCCurveClassification, eDCCurveCifre, eDCCurvePerfectForesight, eDCCurveClassificationMF,
	};

	static DCCurveType hashit(String inString) {
		if (inString.contains("DCCurveMF"))
			return DCCurveType.eDCCurveMF;
		if (inString.contains("DCCurveOlsen"))
			return DCCurveType.eDCCurveOlsen;
		if (inString.contains("DCCurveClassification"))
			return DCCurveType.eDCCurveClassification;
		if (inString.contains("DCCurveCifre"))
			return DCCurveType.eDCCurveCifre;
		if (inString.contains("DCCurvePerfectForesight"))
			return DCCurveType.eDCCurvePerfectForesight;
		if (inString.contains("DCCurveClassificationMF"))
			return DCCurveType.eDCCurveClassificationMF;

		throw new IllegalArgumentException();
		// return string_code.eNone;
	}

	public DCCurveRegression() {
		//positionArrayQuote.add(new Double(0.0));
		positionArrayBase.add(new Double(OpeningPosition));
	}

	abstract public String getDCCurveName();

	abstract public void build(Double[] values, double delta, String GPTreeFileName, Event[] events, Event[] output,
			PreProcess preprocess);

	

	abstract public void testbuild(int lastTrainingPricePosition, Double[] values, double delta, Event[] testEvents,
			PreProcess preprocess);

	abstract String report(Double[] values, double delta, String GPTreeFileName);
	/*
	 * public Integer[] testDCEvent() {
	 * 
	 * Vector<Integer> dcOsDuration = new Vector<Integer>();
	 * 
	 * for (int i = 0; i < this.testEvents.length; i++) { //
	 * dcOsDuration.add(testOutput[i].length()); if
	 * (this.testEvents[i].overshoot == null) dcOsDuration.add(0); else
	 * dcOsDuration.add(this.testEvents[i].overshoot.length()); }
	 * 
	 * Integer[] Directional_changes_length = new Integer[dcOsDuration.size()];
	 * 
	 * dcOsDuration.toArray(Directional_changes_length); //
	 * Directional_changes_length = new double[testOutput.length]; //
	 * System.arraycopy(testOutput, 0, Directional_changes_length, 0, //
	 * testOutput.length); return Directional_changes_length; }
	 * 
	 * public Integer[] trainingDCEvent() { Vector<Integer> dcOsDuration = new
	 * Vector<Integer>(); System.out.println("length is" + this.events.length);
	 * for (int i = 0; i < this.events.length; i++) { //N:B: this.events is the
	 * training events // dcOsDuration.add(testOutput[i].length()); if
	 * (this.events[i].overshoot == null) dcOsDuration.add(0); else
	 * dcOsDuration.add(this.events[i].overshoot.length()); }
	 * 
	 * Integer[] Directional_changes_length = new Integer[dcOsDuration.size()];
	 * 
	 * dcOsDuration.toArray(Directional_changes_length); System.out.println(
	 * "length is" + Directional_changes_length.length);
	 * 
	 * return Directional_changes_length;
	 * 
	 * 
	 * }
	 */

	void generatePredictionCurve(String threshold) {

		// String rmseString=thresholdStr+"_GPOnly.txt";
		// FWriter writer = new FWriter(SymbolicRegression.log.publicFolder +
		// threshold);
	}

	public double trade(PreProcess preprocess) {
		boolean isPositionOpen = false;
		double myPrice = 0.0;
		double transactionCost = 0.025 / 100;
		OpeningPosition = Const.OPEN_POSITION;
		simpleDrawDown.Calculate(OpeningPosition);
		simpleSharpeRatio.addReturn(0);
		//testTradesAndDatapointTraded = new LinkedHashMap<Integer,Double>();
		// System.out.println("classification: current processor count " +
		// SymbolicRegression.currentProcessorCounter);

		lastSellPrice = 0.0;
		lastBuyPrice = 0.0;
		List<FReader.FileMember2> bidAskprice = getMarketDataTest();
		double lastUpDCCend = 0.0;
		
		for (int i = 1; i < testingEvents.length; i++) {
			
			Double dcPt = new Double(predictionWithClassifier[i]);
			Double zeroOs = new Double(0.0);
			int tradePoint = 0;
			DataSetInfoString = DataSetInfoString + testingEvents[i].toString() + "\t";
			if (dcPt.equals(zeroOs)) // Skip DC classified as not having
										// overshoot
				tradePoint = testingEvents[i].end;
			else
				tradePoint = testingEvents[i].end + (int) Math.floor(predictionWithClassifier[i]);

			if (testingEvents[i] == null)
				continue;

			if (i + 1 > testingEvents.length - 1)
				continue;

			if (testingEvents[i + 1] == null)
				continue;
			
			

			int nextEventEndPOint = HelperClass.getNextDirectionaChangeEndPoint(testingEvents, tradePoint);
			DataSetInfoString = DataSetInfoString + "\t"+ tradePoint + "\t" + nextEventEndPOint + "\t";
			if (!HelperClass.isPreviousDirectionalChangeEventSameAsCurrentDCEvent(testingEvents, tradePoint) && 
					HelperClass.isNextDirectionalChangeEventSameDirectionButDifferentLength(testingEvents, tradePoint)){
				DataSetInfoString = DataSetInfoString+"\t" + 1 + "\t";
			}
			else
				DataSetInfoString = DataSetInfoString+"\t" + 0 + "\t";
			
			if (tradePoint > nextEventEndPOint){ // If a new DC is
												// encountered
												// before the
												// estimation point
												// skip trading
				DataSetInfoString = DataSetInfoString+"\t skipped \t";
				continue;
			}

			if (tradePoint > bidAskprice.size()) {
				System.out.println(" DCCurveClassification: predicted datapoint "
						+ ((lastTrainingPrice - 1) + tradePoint) + " is beyond the size of price array  "
						+ FReader.dataRecordInFileArray.size() + " . Trading ended");
				break;
			} else {
				// I am opening my position in base currency
				try {

					// LinkedHashMap<Integer, Integer> anticipatedTrendMap = new
					// LinkedHashMap<Integer, Integer>();
					// LinkedHashMap<Integer, Integer> actualTrendMap = new
					// LinkedHashMap<Integer, Integer>();
					double transactionCostPrice = 0.0;
					if (testingEvents[i].type == Type.Upturn && !isPositionOpen) {
						// Now position is in quote currency
						// I sell base currency in bid price
						double askQuantity = OpeningPosition;
						double zeroTransactionCostAskQuantity = OpeningPosition;

						myPrice = Double.parseDouble(bidAskprice.get(tradePoint).askPrice);

						transactionCost = askQuantity * (0.025 / 100);
						transactionCostPrice = transactionCost * myPrice;
						askQuantity = (askQuantity - transactionCost) * myPrice;
						zeroTransactionCostAskQuantity = zeroTransactionCostAskQuantity * myPrice;
						// transactionCost = trainingOpeningPosition *
						// (0.025/100);
						// trainingOpeningPosition = (trainingOpeningPosition
						// -transactionCost) *myPrice;

						if (transactionCostPrice < (zeroTransactionCostAskQuantity - askQuantity)) {

							lastUpDCCend = Double.parseDouble(FReader.dataRecordInFileArray
									.get((lastTrainingPrice - 1) + testingEvents[i].end).bidPrice);

							lastSellPrice = myPrice;
							OpeningPosition = askQuantity;
							isPositionOpen = true;
							//positionArrayQuote.add(new Double(OpeningPosition));
							tradedPrice.add(new Double(myPrice));
							DataSetInfoString = DataSetInfoString+"\t OpeningPosition \t";
							//testTradesAndDatapointTraded.put(tradePoint,OpeningPosition);
							// anticipatedTrendMap.put(testingEvents[i].start,
							// tradePoint);
							// anticipatedTrend.add(anticipatedTrendMap);

							// if (testingEvents[i].overshoot == null ||
							// testingEvents[i].overshoot.length() < 1)
							// actualTrendMap.put(testingEvents[i].start,
							// testingEvents[i].end );
							// else
							// actualTrendMap.put(testingEvents[i].start,
							// testingEvents[i].overshoot.end );
							//
							// actualTrend.add(actualTrendMap);
						}

					} else if (testingEvents[i].type == Type.Downturn && isPositionOpen) {
						// Now position is in base currency
						// I buy base currency
						double bidQuantity = OpeningPosition;
						double zeroTransactionCostBidQuantity = OpeningPosition;

						myPrice = Double.parseDouble(bidAskprice.get(tradePoint).bidPrice);

						transactionCost = bidQuantity * (0.025 / 100);
						transactionCostPrice = transactionCost * myPrice;
						bidQuantity = (bidQuantity - transactionCost) * myPrice;
						zeroTransactionCostBidQuantity = zeroTransactionCostBidQuantity * myPrice;

						if (transactionCostPrice < (zeroTransactionCostBidQuantity - bidQuantity)
								&& myPrice < lastUpDCCend) {

							lastBuyPrice = myPrice;
							OpeningPosition = (OpeningPosition - transactionCost) / myPrice;

							isPositionOpen = false;
							positionArrayBase.add(new Double(OpeningPosition));
							tradedPrice.add(new Double(myPrice));
							DataSetInfoString = DataSetInfoString+"\t OpeningPosition \t";
							//testTradesAndDatapointTraded.put(tradePoint,OpeningPosition);
							// anticipatedTrendMap.put(testingEvents[i].start,
							// tradePoint);
							// anticipatedTrend.add(anticipatedTrendMap);

							// if (testingEvents[i].overshoot == null ||
							// testingEvents[i].overshoot.length() < 1)
							// actualTrendMap.put(testingEvents[i].start,
							// testingEvents[i].end);
							// else
							// actualTrendMap.put(testingEvents[i].start,
							// testingEvents[i].overshoot.end);

							// actualTrend.add(actualTrendMap);
						}
					}
				} catch (ArrayIndexOutOfBoundsException exception) {
					System.out.println(" DCCurveClassiifcation: Search for element "
							+ ((lastTrainingPrice - 1) + tradePoint) + " is beyond the size of price array  "
							+ FReader.dataRecordInFileArray.size() + " . Trading ended");
					break;

				} catch (IndexOutOfBoundsException exception) {
					System.out.println(" DCCurveClassiifcation: Search for element "
							+ ((lastTrainingPrice - 1) + tradePoint) + " is beyond the size of price array  "
							+ FReader.dataRecordInFileArray.size() + " . Trading ended");
					break;
				} catch (Exception exception) {
					System.out.println(" DCCurveClassiifcation: Search for element "
							+ ((lastTrainingPrice - 1) + tradePoint) + " is beyond the size of price array  "
							+ FReader.dataRecordInFileArray.size() + " . Trading ended");
					break;
				}

			}
			DataSetInfoString = DataSetInfoString  + "\n";

		}

		if (isPositionOpen) {
			tradedPrice.remove(tradedPrice.size() - 1);
			//anticipatedTrend.remove(anticipatedTrend.size() - 1);
			//actualTrend.remove(actualTrend.size() - 1);
			if (!positionArrayBase.isEmpty()){
				OpeningPosition = positionArrayBase.get(positionArrayBase.size() - 1);
				//testTradesAndDatapointTraded.remove(testTradesAndDatapointTraded.size() -1);
			}
			else
				OpeningPosition = OpeningPositionHist;
			
			DataSetInfoString = DataSetInfoString+"\t OpeningPosition \t";
			//if (!positionArrayQuote.isEmpty())
			//	positionArrayQuote.remove(positionArrayQuote.size() - 1);

			isPositionOpen = false;
		}

		otherTradeCalculations();

		return OpeningPosition;

	}
	
	public double tradeGA(PreProcess preprocess, double ratio ) {
		
		double myPrice = 0.0;
		double transactionCost = 0.025 / 100;
		OpeningPosition = Const.OPEN_POSITION;
		
		soldPosition = 0.0;
		simpleDrawDown.Calculate(OpeningPosition);
		simpleSharpeRatio.addReturn(0);
		//testTradesAndDatapointTraded = new LinkedHashMap<Integer,Double>();
		// System.out.println("classification: current processor count " +
		// SymbolicRegression.currentProcessorCounter);

		lastSellPrice = 0.0;
		lastBuyPrice = 0.0;
		List<FReader.FileMember2> bidAskprice = getMarketDataTest();
		double lastUpDCCend = 0.0;
		for (int i = 0; i < testingEvents.length; i++) {

			Double dcPt = new Double(predictionWithClassifier[i]);
			Double zeroOs = new Double(0.0);
			int tradePoint = 0;

			if (dcPt.equals(zeroOs)) // Skip DC classified as not having
										// overshoot
				tradePoint = testingEvents[i].end;
			else
				tradePoint = testingEvents[i].end + (int) Math.floor(predictionWithClassifier[i]);

			if (testingEvents[i] == null)
				continue;

			if (i + 1 > testingEvents.length - 1)
				continue;

			if (testingEvents[i + 1] == null)
				continue;

			//int nextEventEndPOint = HelperClass.getNextDirectionaChangeEndPoint(testingEvents, tradePoint);
			Type actionToTake = null;
			boolean tradingpointFound = false;
			if (tradePoint > i) {
				
				
					if (testingEvents[i].type == Type.Upturn && 
						 (futurePredictionUpturn.get(tradePoint) == null)){
						futurePredictionUpturn.put(tradePoint, Type.Upturn);
					}
					else if (testingEvents[i].type == Type.Downturn && 
							 (futurePredictionDownturn.get(tradePoint) == null)){
						futurePredictionDownturn.put(tradePoint, Type.Downturn);
					}
			}
			
			if (tradePoint == i ){
				tradingpointFound =  true;
				actionToTake =  testingEvents[i].type;
			}
			else{
				for (Map.Entry<Integer, Type> entry : futurePredictionUpturn.entrySet()){
					if (entry.getKey() == i){
						tradePoint =  i;
					    actionToTake =  entry.getValue();
					    tradingpointFound = true;
					}
				}
				if (tradingpointFound == true){
					for (Map.Entry<Integer, Type> entry : futurePredictionDownturn.entrySet()){
						if (entry.getKey() == i){
							tradePoint =  i;
						    actionToTake =  entry.getValue();
						    tradingpointFound = true;
						}
					}
				}
			}
			
			if (tradingpointFound == false)
				continue;

			if (tradePoint > bidAskprice.size()) {
				System.out.println(" DCCurveClassification: predicted datapoint "
						+ ((lastTrainingPrice - 1) + tradePoint) + " is beyond the size of price array  "
						+ FReader.dataRecordInFileArray.size() + " . Trading ended");
				break;
			} 
				// I am opening my position in base currency
				try {

					double transactionCostPrice = 0.0;
					if (actionToTake == Type.Upturn && Double.compare(OpeningPosition, 0.0)> 0 ) {
						// Now position is in quote currency
						// I sell base currency in bid price
						
						
						 transactionCost = 0.025 / 100;
						double askQuantity = ratio *OpeningPosition;
						double zeroTransactionCostAskQuantity = ratio * OpeningPosition;

						myPrice =Double.parseDouble(bidAskprice.get(tradePoint).askPrice);
						transactionCost = askQuantity * (0.025 / 100);
						 transactionCostPrice = transactionCost * myPrice;
						double soldQuantity  = (askQuantity - transactionCost) * myPrice;
						zeroTransactionCostAskQuantity = zeroTransactionCostAskQuantity * myPrice;
						
						
						if ((Double.compare(transactionCostPrice ,(zeroTransactionCostAskQuantity - OpeningPosition)) < 0)  
							
								&& Double.compare(OpeningPosition, 0.0)> 0 ) {

						
							if (Double.compare(lastUpDCCend, 0.0) == 0)
								lastUpDCCend = myPrice;
							else if (Double.compare(lastUpDCCend, 0.0) > 0 && Double.compare(myPrice,lastUpDCCend) < 0 )
								lastUpDCCend = myPrice;
							
							
							lastSellPrice = myPrice;
							OpeningPosition = OpeningPosition - askQuantity;
							soldPosition = soldPosition + soldQuantity;
							isPositionOpen = true;
							//positionArrayQuote.add(new Double(OpeningPosition));
							tradedPrice.add(new Double(myPrice));
							
						}
					} else if (actionToTake == Type.Downturn && Double.compare(soldPosition, 0.0)> 0 ) {
						// Now position is in base currency
						// I buy base currency
						/*double bidQuantity = trainingOpeningPosition;
						double zeroTransactionCostBidQuantity = trainingOpeningPosition;
						double transactionCostPrice = 0.0;
						myPrice = Double.parseDouble(fileMember2.bidPrice);
						transactionCost = bidQuantity * (0.025 / 100);
						transactionCostPrice = transactionCost * myPrice;
						bidQuantity = (bidQuantity - transactionCost) * myPrice;
						zeroTransactionCostBidQuantity = zeroTransactionCostBidQuantity * myPrice;
		*/
						double bidQuantity = soldPosition;
						double zeroTransactionCostBidQuantity =  soldPosition;
						transactionCost = 0.025 / 100;
						myPrice = Double.parseDouble(bidAskprice.get(tradePoint).bidPrice);
						transactionCost = bidQuantity * (0.025 / 100);
						transactionCostPrice = transactionCost * myPrice;
						double boughtQuantity  = (bidQuantity - transactionCost) * myPrice;
						zeroTransactionCostBidQuantity = zeroTransactionCostBidQuantity * myPrice;
						
						
						if ((Double.compare(transactionCostPrice , (zeroTransactionCostBidQuantity - boughtQuantity) )< 0) && Double.compare(myPrice,  lastUpDCCend) <0) {
						
							lastBuyPrice = myPrice;
							soldPosition = 0.0;
							OpeningPosition = OpeningPosition +( (bidQuantity - transactionCost) / myPrice);
							tradedPrice.add(new Double(myPrice));
							positionArrayBase.add(OpeningPosition );
						}
					}
				} catch (ArrayIndexOutOfBoundsException exception) {
					System.out.println(" DCCurveClassiifcation: Search for element "
							+ ((lastTrainingPrice - 1) + tradePoint) + " is beyond the size of price array  "
							+ FReader.dataRecordInFileArray.size() + " . Trading ended");
					break;

				} catch (IndexOutOfBoundsException exception) {
					System.out.println(" DCCurveClassiifcation: Search for element "
							+ ((lastTrainingPrice - 1) + tradePoint) + " is beyond the size of price array  "
							+ FReader.dataRecordInFileArray.size() + " . Trading ended");
					break;
				} catch (Exception exception) {
					System.out.println(" DCCurveClassiifcation: Search for element "
							+ ((lastTrainingPrice - 1) + tradePoint) + " is beyond the size of price array  "
							+ FReader.dataRecordInFileArray.size() + " . Trading ended");
					break;
				}

			

		}

		if (Double.compare(soldPosition, 0.0) > 0) {
			double bidQuantity = soldPosition;
			soldPosition = 0.0;
			 transactionCost =  bidQuantity * 0.025 / 100;
			myPrice = Double.parseDouble(FReader.
					dataRecordInFileArray.get(testingEvents.length-1).askPrice);
			
			double boughtQuantity  = (bidQuantity - transactionCost) / myPrice;
			OpeningPosition = OpeningPosition + boughtQuantity;
			
			positionArrayBase.add(OpeningPosition + (soldPosition/myPrice));
		}

		otherTradeCalculations();

		return OpeningPosition;
		
		

	}

	
	public double tradeGA_SingleQuantityForwardLooking(PreProcess preprocess) {
		boolean isPositionOpen = false;
		double myPrice = 0.0;
		double transactionCost = 0.025 / 100;
		OpeningPosition = Const.OPEN_POSITION;
		simpleDrawDown.Calculate(OpeningPosition);
		simpleSharpeRatio.addReturn(0);
		//testTradesAndDatapointTraded = new LinkedHashMap<Integer,Double>();
		// System.out.println("classification: current processor count " +
		// SymbolicRegression.currentProcessorCounter);

		lastSellPrice = 0.0;
		lastBuyPrice = 0.0;
		List<FReader.FileMember2> bidAskprice = getMarketDataTest();
		double lastUpDCCend = 0.0;
		for (int i = 0; i < testingEvents.length; i++) {

			Double dcPt = new Double(predictionWithClassifier[i]);
			Double zeroOs = new Double(0.0);
			int tradePoint = 0;

			if (dcPt.equals(zeroOs)) // Skip DC classified as not having
										// overshoot
				tradePoint = testingEvents[i].end;
			else
				tradePoint = testingEvents[i].end + (int) Math.floor(predictionWithClassifier[i]);

			if (testingEvents[i] == null)
				continue;

			if (i + 1 > testingEvents.length - 1)
				continue;

			if (testingEvents[i + 1] == null)
				continue;

			//int nextEventEndPOint = HelperClass.getNextDirectionaChangeEndPoint(testingEvents, tradePoint);
			Type actionToTake = null;
			boolean tradingpointFound = false;
			if (tradePoint > i) {
				
				
					if (testingEvents[i].type == Type.Upturn && 
						 (futurePredictionUpturn.get(tradePoint) == null)){
						futurePredictionUpturn.put(tradePoint, Type.Upturn);
					}
					else if (testingEvents[i].type == Type.Downturn && 
							 (futurePredictionDownturn.get(tradePoint) == null)){
						futurePredictionDownturn.put(tradePoint, Type.Downturn);
					}
			}
			
			if (tradePoint == i ){
				tradingpointFound =  true;
				actionToTake =  testingEvents[i].type;
			}
			else{
				for (Map.Entry<Integer, Type> entry : futurePredictionUpturn.entrySet()){
					if (entry.getKey() == i){
						tradePoint =  i;
					    actionToTake =  entry.getValue();
					    tradingpointFound = true;
					}
				}
				if (tradingpointFound == true){
					for (Map.Entry<Integer, Type> entry : futurePredictionDownturn.entrySet()){
						if (entry.getKey() == i){
							tradePoint =  i;
						    actionToTake =  entry.getValue();
						    tradingpointFound = true;
						}
					}
				}
			}
			
			if (tradingpointFound == false)
				continue;

			if (tradePoint > bidAskprice.size()) {
				System.out.println(" DCCurveClassification: predicted datapoint "
						+ ((lastTrainingPrice - 1) + tradePoint) + " is beyond the size of price array  "
						+ FReader.dataRecordInFileArray.size() + " . Trading ended");
				break;
			} else {
				// I am opening my position in base currency
				try {

					double transactionCostPrice = 0.0;
					if (actionToTake == Type.Upturn && !isPositionOpen) {
						// Now position is in quote currency
						// I sell base currency in bid price
						double askQuantity = OpeningPosition;
						double zeroTransactionCostAskQuantity = OpeningPosition;

						myPrice = Double.parseDouble(bidAskprice.get(tradePoint).askPrice);

						transactionCost = askQuantity * (0.025 / 100);
						transactionCostPrice = transactionCost * myPrice;
						askQuantity = (askQuantity - transactionCost) * myPrice;
						zeroTransactionCostAskQuantity = zeroTransactionCostAskQuantity * myPrice;
						
						if (transactionCostPrice < (zeroTransactionCostAskQuantity - askQuantity)) {

							lastUpDCCend = Double.parseDouble(FReader.dataRecordInFileArray
									.get((lastTrainingPrice - 1) + testingEvents[i].end).bidPrice);

							lastSellPrice = myPrice;
							OpeningPosition = askQuantity;
							isPositionOpen = true;
							//positionArrayQuote.add(new Double(OpeningPosition));
							tradedPrice.add(new Double(myPrice));
							//testTradesAndDatapointTraded.put(tradePoint,OpeningPosition);
						
						}

					} else if (actionToTake == Type.Downturn && isPositionOpen) {
						// Now position is in base currency
						// I buy base currency
						double bidQuantity = OpeningPosition;
						double zeroTransactionCostBidQuantity = OpeningPosition;

						myPrice = Double.parseDouble(bidAskprice.get(tradePoint).bidPrice);

						transactionCost = bidQuantity * (0.025 / 100);
						transactionCostPrice = transactionCost * myPrice;
						bidQuantity = (bidQuantity - transactionCost) * myPrice;
						zeroTransactionCostBidQuantity = zeroTransactionCostBidQuantity * myPrice;

						if (transactionCostPrice < (zeroTransactionCostBidQuantity - bidQuantity)
								&& myPrice < lastUpDCCend) {

							lastBuyPrice = myPrice;
							OpeningPosition = (OpeningPosition - transactionCost) / myPrice;

							isPositionOpen = false;
							positionArrayBase.add(new Double(OpeningPosition));
							tradedPrice.add(new Double(myPrice));
							//testTradesAndDatapointTraded.put(tradePoint,OpeningPosition);
							// anticipatedTrendMap.put(testingEvents[i].start,
							// tradePoint);
							// anticipatedTrend.add(anticipatedTrendMap);

							// if (testingEvents[i].overshoot == null ||
							// testingEvents[i].overshoot.length() < 1)
							// actualTrendMap.put(testingEvents[i].start,
							// testingEvents[i].end);
							// else
							// actualTrendMap.put(testingEvents[i].start,
							// testingEvents[i].overshoot.end);

							// actualTrend.add(actualTrendMap);
						}
					}
				} catch (ArrayIndexOutOfBoundsException exception) {
					System.out.println(" DCCurveClassiifcation: Search for element "
							+ ((lastTrainingPrice - 1) + tradePoint) + " is beyond the size of price array  "
							+ FReader.dataRecordInFileArray.size() + " . Trading ended");
					break;

				} catch (IndexOutOfBoundsException exception) {
					System.out.println(" DCCurveClassiifcation: Search for element "
							+ ((lastTrainingPrice - 1) + tradePoint) + " is beyond the size of price array  "
							+ FReader.dataRecordInFileArray.size() + " . Trading ended");
					break;
				} catch (Exception exception) {
					System.out.println(" DCCurveClassiifcation: Search for element "
							+ ((lastTrainingPrice - 1) + tradePoint) + " is beyond the size of price array  "
							+ FReader.dataRecordInFileArray.size() + " . Trading ended");
					break;
				}

			}

		}

		if (isPositionOpen) {
			tradedPrice.remove(tradedPrice.size() - 1);
			//anticipatedTrend.remove(anticipatedTrend.size() - 1);
			//actualTrend.remove(actualTrend.size() - 1);
			if (!positionArrayBase.isEmpty()){
				OpeningPosition = positionArrayBase.get(positionArrayBase.size() - 1);
				//testTradesAndDatapointTraded.remove(testTradesAndDatapointTraded.size() -1);
			}
			else
				OpeningPosition = OpeningPositionHist;
			
			
			//if (!positionArrayQuote.isEmpty())
			//	positionArrayQuote.remove(positionArrayQuote.size() - 1);

			isPositionOpen = false;
		}

		otherTradeCalculations();

		return OpeningPosition;
	}

	
	public double tradeVariableQuantityUsingInitialSingleThresholdTradingidea(PreProcess preprocess, double ratio) {
		boolean isPositionOpen = false;
		double myPrice = 0.0;
		double transactionCost = 0.025 / 100;
		OpeningPosition = Const.OPEN_POSITION;
		simpleDrawDown.Calculate(OpeningPosition);
		simpleSharpeRatio.addReturn(0);
		// System.out.println("classification: current processor count " +
		// SymbolicRegression.currentProcessorCounter);

		lastSellPrice = 0.0;
		lastBuyPrice = 0.0;
		List<FReader.FileMember2> bidAskprice = getMarketDataTest();
		double lastUpDCCend = 0.0;
		for (int i = 1; i < testingEvents.length; i++) {

			Double dcPt = new Double(predictionWithClassifier[i]);
			Double zeroOs = new Double(0.0);
			int tradePoint = 0;

			if (dcPt.equals(zeroOs)) // Skip DC classified as not having
										// overshoot
				tradePoint = testingEvents[i].end;
			else
				tradePoint = testingEvents[i].end + (int) Math.floor(predictionWithClassifier[i]);

			if (testingEvents[i] == null)
				continue;

			if (i + 1 > testingEvents.length - 1)
				continue;

			if (testingEvents[i + 1] == null)
				continue;

			int nextEventEndPOint = HelperClass.getNextDirectionaChangeEndPoint(testingEvents, tradePoint);

			if (tradePoint > nextEventEndPOint) // If a new DC is
												// encountered
												// before the
												// estimation point
												// skip trading
				continue;

			if (tradePoint > bidAskprice.size()) {
				System.out.println(" DCCurveClassification: predicted datapoint "
						+ ((lastTrainingPrice - 1) + tradePoint) + " is beyond the size of price array  "
						+ FReader.dataRecordInFileArray.size() + " . Trading ended");
				break;
			} else {
				// I am opening my position in base currency
				try {

					// LinkedHashMap<Integer, Integer> anticipatedTrendMap = new
					// LinkedHashMap<Integer, Integer>();
					// LinkedHashMap<Integer, Integer> actualTrendMap = new
					// LinkedHashMap<Integer, Integer>();
					double transactionCostPrice = 0.0;
					if (testingEvents[i].type == Type.Upturn && !isPositionOpen) {
						// Now position is in quote currency
						// I sell base currency in bid price
						double askQuantity = ratio *OpeningPosition;
						double zeroTransactionCostAskQuantity = ratio * OpeningPosition;

						myPrice = Double.parseDouble(bidAskprice.get(tradePoint).askPrice);
						double tempOpenPostision =OpeningPosition-askQuantity; // Initial Position
						
						transactionCost = askQuantity * (0.025 / 100);
						transactionCostPrice = transactionCost * myPrice;
						askQuantity = (askQuantity - transactionCost) * myPrice;
						zeroTransactionCostAskQuantity = zeroTransactionCostAskQuantity * myPrice;
						
						if (transactionCostPrice < (zeroTransactionCostAskQuantity - askQuantity)) {

							double currentlastUpDCCend = Double.parseDouble(FReader.dataRecordInFileArray
									.get((lastTrainingPrice - 1) + testingEvents[i].end).bidPrice);

							if (Double.compare(currentlastUpDCCend, lastUpDCCend)<1)
								lastUpDCCend = currentlastUpDCCend;
							
							lastSellPrice = myPrice;
							OpeningPosition = tempOpenPostision;
							soldPosition = soldPosition + askQuantity;
							isPositionOpen = true;
							//positionArrayQuote.add(new Double(OpeningPosition));
							tradedPrice.add(new Double(myPrice));
						}

					} else if (testingEvents[i].type == Type.Downturn && isPositionOpen) {
						// Now position is in base currency
						// I buy base currency
						double bidQuantity = soldPosition;
						double zeroTransactionCostBidQuantity = soldPosition;

						myPrice = Double.parseDouble(bidAskprice.get(tradePoint).bidPrice);

						transactionCost = bidQuantity * (0.025 / 100);
						transactionCostPrice = transactionCost * myPrice;
						bidQuantity = (bidQuantity - transactionCost) * myPrice;
						zeroTransactionCostBidQuantity = zeroTransactionCostBidQuantity * myPrice;

						if (transactionCostPrice < (zeroTransactionCostBidQuantity - bidQuantity)
								&& myPrice < lastUpDCCend) {

							lastBuyPrice = myPrice;
							OpeningPosition = OpeningPosition + ((soldPosition - transactionCost) / myPrice);
							soldPosition = 0.0;
							isPositionOpen = false;
							positionArrayBase.add(new Double(OpeningPosition));
							tradedPrice.add(new Double(myPrice));
													}
					}
				} catch (ArrayIndexOutOfBoundsException exception) {
					System.out.println(" DCCurveClassiifcation: Search for element "
							+ ((lastTrainingPrice - 1) + tradePoint) + " is beyond the size of price array  "
							+ FReader.dataRecordInFileArray.size() + " . Trading ended");
					break;

				} catch (IndexOutOfBoundsException exception) {
					System.out.println(" DCCurveClassiifcation: Search for element "
							+ ((lastTrainingPrice - 1) + tradePoint) + " is beyond the size of price array  "
							+ FReader.dataRecordInFileArray.size() + " . Trading ended");
					break;
				} catch (Exception exception) {
					System.out.println(" DCCurveClassiifcation: Search for element "
							+ ((lastTrainingPrice - 1) + tradePoint) + " is beyond the size of price array  "
							+ FReader.dataRecordInFileArray.size() + " . Trading ended");
					break;
				}

			}

		}

		if (isPositionOpen) {
			tradedPrice.remove(tradedPrice.size() - 1);			
			if (!positionArrayBase.isEmpty())
				OpeningPosition = positionArrayBase.get(positionArrayBase.size() - 1);
			else
				OpeningPosition = OpeningPositionHist;

			//if (!positionArrayQuote.isEmpty())
			//	positionArrayQuote.remove(positionArrayQuote.size() - 1);

			isPositionOpen = false;
		}

		otherTradeCalculations();

		return OpeningPosition;
	}

	public abstract String getActualTrend();

	public abstract String getPredictedTrend();

	public abstract <E> void assignPerfectForesightRegressionModel(E[] inputArray);

	void TestDCEventOSEventDistribution() {
		for (int eventCount = 1; eventCount < testingEvents.length; eventCount++) {

			if (testingEvents[eventCount].overshoot != null) {
				totalOvershootEventLengthTest = totalOvershootEventLengthTest
						+ testingEvents[eventCount].overshoot.length();
				numberOfTestOvershoot = numberOfTestOvershoot + 1;
			}
			totalDCEventLengthTest = totalDCEventLengthTest + testingEvents[eventCount].length();

		}

		numberOfTestDC = testingEvents.length - 1;
		int numberOfZerosTestDC = numberOfTestDC - numberOfTestOvershoot;
		zeroPercentageTest = ((float) numberOfZerosTestDC / numberOfTestDC) * 100;
		dC_OS_Length_RatioTest = ((float) (totalOvershootEventLengthTest / totalDCEventLengthTest) * 100);

	}

	public double trainingTrading(PreProcess preprocess) {
		boolean isPositionOpen = false;
		double myPrice = 0.0;
		double lastClosedPosition = 0.0;
		double transactionCost = 0.025 / 100;

		
		positionArrayBase.clear();
		trainingOpeningPosition = 500000.00;
		double lastUpDCCend = 0.0;
		simpleDrawDown.clearMDD();
		simpleSharpeRatio.rmoveAllReturns();
		simpleDrawDown.Calculate(OpeningPosition);
		simpleSharpeRatio.addReturn(0);
		
		for (int i = 1; i < trainingOutputEvents.length; i++) {

			int tradePoint = 0;

			double dcPt = trainingUsingOutputData[i];
			double zeroOs = 0.0;

			if (trainingOutputEvents[i] == null)
				continue;

			DataSetInfoString = "\n" + DataSetInfoString + trainingOutputEvents[i].toString() + "\t";
			if (Double.compare(dcPt, zeroOs) == 0 || Double.compare(dcPt, zeroOs) < 0) // Skip
																						// DC
																						// classified
																						// as
																						// not
																						// having
			// overshoot
			{
				tradePoint = trainingOutputEvents[i].end;

			} else {
				tradePoint = trainingOutputEvents[i].end + (int) Math.ceil(dcPt);

			}
			if (i + 1 > trainingOutputEvents.length - 1)
				continue;

			if (trainingOutputEvents[i + 1] == null)
				continue;
			

			int nextEventEndPOint = HelperClass.getNextDirectionaChangeEndPoint(trainingOutputEvents, tradePoint);

			DataSetInfoString = DataSetInfoString + "\t"+ tradePoint + "\t" + nextEventEndPOint + "\t";
			
			if (!HelperClass.isPreviousDirectionalChangeEventSameAsCurrentDCEvent(trainingOutputEvents, tradePoint) && 
					HelperClass.isNextDirectionalChangeEventSameDirectionButDifferentLength(trainingOutputEvents, tradePoint)){
				DataSetInfoString = DataSetInfoString+"\t" + 1 + "\t";
			}
			else
				DataSetInfoString = DataSetInfoString+"\t" + 0 + "\t";
			
			
			if (tradePoint > nextEventEndPOint) {// If a new DC is
												// encountered
												// before the
												// estimation point
												// skip trading
				DataSetInfoString = DataSetInfoString+"\t skipped \t";
				continue;
			}

			FReader freader = new FReader();
			FileMember2 fileMember2 = freader.new FileMember2();

			if (tradePoint > FReader.dataRecordInFileArray.size() - 1) {
				continue;
			}

			// I am opening my position in base currency
			try {
				fileMember2 = FReader.dataRecordInFileArray.get(tradePoint);
			} catch (ArrayIndexOutOfBoundsException e) {
				System.out.println(e.getMessage());
				continue;
			}

			
			if (trainingOutputEvents[i].type == Type.Upturn && !isPositionOpen) {
				// Now position is in quote currency
				// I sell base currency in bid price
				double askQuantity = trainingOpeningPosition;
				double zeroTransactionCostAskQuantity = trainingOpeningPosition;
				double transactionCostPrice = 0.0;
				myPrice = Double.parseDouble(fileMember2.askPrice);

				transactionCost = askQuantity * (0.025 / 100);
				transactionCostPrice = transactionCost * myPrice;
				askQuantity = (askQuantity - transactionCost) * myPrice;
				zeroTransactionCostAskQuantity = zeroTransactionCostAskQuantity * myPrice;

				if (transactionCostPrice < (zeroTransactionCostAskQuantity - askQuantity)) {

					lastSellPrice = myPrice;
					trainingOpeningPosition = askQuantity;
					//positionArrayQuote.add(new Double(trainingOpeningPosition));
					tradedPrice.add(new Double(myPrice));
					isPositionOpen = true;
					lastUpDCCend = Double
							.parseDouble(FReader.dataRecordInFileArray.get(trainingOutputEvents[i].end).bidPrice);
					DataSetInfoString = DataSetInfoString+"\t trainingOpeningPosition \t";
					
					// anticipatedTrendMap.put(trainingOutputEvents[i].start,
					// tradePoint);
					// anticipatedTrend.add(anticipatedTrendMap);

					// if (trainingOutputEvents[i].overshoot == null ||
					// trainingOutputEvents[i].overshoot.length() < 1)
					// actualTrendMap.put(trainingOutputEvents[i].start,
					// trainingOutputEvents[i].end );
					// else{
					//
					// actualTrendMap.put(trainingOutputEvents[i].start,
					// trainingOutputEvents[i].overshoot.end );
					// }

					// actualTrend.add(actualTrendMap);
				}
			} else if (trainingOutputEvents[i].type == Type.Downturn && isPositionOpen) {
				// Now position is in base currency
				// I buy base currency
				double bidQuantity = trainingOpeningPosition;
				double zeroTransactionCostBidQuantity = trainingOpeningPosition;
				double transactionCostPrice = 0.0;
				myPrice = Double.parseDouble(fileMember2.bidPrice);
				transactionCost = bidQuantity * (0.025 / 100);
				transactionCostPrice = transactionCost * myPrice;
				bidQuantity = (bidQuantity - transactionCost) * myPrice;
				zeroTransactionCostBidQuantity = zeroTransactionCostBidQuantity * myPrice;

				if (transactionCostPrice < (zeroTransactionCostBidQuantity - bidQuantity) && myPrice < lastUpDCCend) {

					lastBuyPrice = myPrice;
					trainingOpeningPosition = (trainingOpeningPosition - transactionCost) / myPrice;
					positionArrayBase.add(new Double(trainingOpeningPosition));
					tradedPrice.add(new Double(myPrice));
					lastClosedPosition = trainingOpeningPosition;
					isPositionOpen = false;
					DataSetInfoString = DataSetInfoString+"\t trainingOpeningPosition \t";
					// anticipatedTrendMap.put(trainingOutputEvents[i].start,
					// tradePoint);
					// anticipatedTrend.add(anticipatedTrendMap);

					// if (trainingOutputEvents[i].overshoot == null ||
					// trainingOutputEvents[i].overshoot.length() < 1)
					// actualTrendMap.put(trainingOutputEvents[i].start,
					// trainingOutputEvents[i].end);
					// else
					// actualTrendMap.put(trainingOutputEvents[i].start,
					// trainingOutputEvents[i].overshoot.end);

					// actualTrend.add(actualTrendMap);
				}
			}
			DataSetInfoString = DataSetInfoString  + "\n";

		}

		if (isPositionOpen) {
			trainingOpeningPosition = lastClosedPosition;
			tradedPrice.remove(tradedPrice.size() - 1);
		//	anticipatedTrend.remove(anticipatedTrend.size() - 1);
		//	actualTrend.remove(actualTrend.size() - 1);
			if (!positionArrayBase.isEmpty()) {
				positionArrayBase.get(positionArrayBase.size() - 1);
				
			} else
				trainingOpeningPosition = trainingOpeningPositionHist;

			DataSetInfoString = DataSetInfoString+"\t trainingOpeningPosition \t";
			
			//if (!positionArrayQuote.isEmpty())
			//	positionArrayQuote.remove(positionArrayQuote.size() - 1);
		}

		otherTradeCalculations();

		return trainingOpeningPosition;
	}

	
	public double trainingTradingGA(PreProcess preprocess, double ratio) {
		
		double myPrice = 0.0;
	
		
		soldPosition = 0.0;
		positionArrayBase.clear();
		OpeningPosition = 500000.00;
		double lastUpDCCend = 0.0;
		simpleDrawDown.clearMDD();
		simpleSharpeRatio.rmoveAllReturns();
		simpleDrawDown.Calculate(OpeningPosition);
		simpleSharpeRatio.addReturn(0);
		for (int i = 0; i < trainingOutputEvents.length; i++) {

			int tradePoint = 0;

			double dcPt = trainingUsingOutputData[i];
			double zeroOs = 0.0;

			if (trainingOutputEvents[i] == null)
				continue;

			if (Double.compare(dcPt, zeroOs) == 0 || Double.compare(dcPt, zeroOs) < 0) // Skip
																						// DC
																						// classified
																						// as
																						// not
																						// having
			// overshoot
			{
				tradePoint = trainingOutputEvents[i].end;

			} else {
				tradePoint = trainingOutputEvents[i].end + (int) Math.ceil(dcPt);

			}
			if (i + 1 > trainingOutputEvents.length - 1)
				continue;

			if (trainingOutputEvents[i + 1] == null)
				continue;

			Type actionToTake = null;
			boolean tradingpointFound = false;
			if (tradePoint > i) {
				
				
					if (trainingOutputEvents[i].type == Type.Upturn && 
						 (futurePredictionUpturn.get(tradePoint) == null)){
						futurePredictionUpturn.put(tradePoint, Type.Upturn);
					}
					else if (trainingOutputEvents[i].type == Type.Downturn && 
							 (futurePredictionDownturn.get(tradePoint) == null)){
						futurePredictionDownturn.put(tradePoint, Type.Downturn);
					}
			}
			
			if (tradePoint == i ){
				tradingpointFound =  true;
				actionToTake =  trainingOutputEvents[i].type;
			}
			else{
				for (Map.Entry<Integer, Type> entry : futurePredictionUpturn.entrySet()){
					if (entry.getKey() == i){
						tradePoint =  i;
					    actionToTake =  entry.getValue();
					    tradingpointFound = true;
					}
				}
				if (tradingpointFound == true){
					for (Map.Entry<Integer, Type> entry : futurePredictionDownturn.entrySet()){
						if (entry.getKey() == i){
							tradePoint =  i;
						    actionToTake =  entry.getValue();
						    tradingpointFound = true;
						}
					}
				}
			}
			
			if (tradingpointFound == false)
				continue;


			FReader freader = new FReader();
			FileMember2 fileMember2 = freader.new FileMember2();

			if (tradePoint > FReader.dataRecordInFileArray.size() - 1) {
				continue;
			}

			// I am opening my position in base currency
			try {
				fileMember2 = FReader.dataRecordInFileArray.get(tradePoint);
			} catch (ArrayIndexOutOfBoundsException e) {
				System.out.println(e.getMessage());
				continue;
			}

			if (actionToTake == Type.Upturn && Double.compare(OpeningPosition, 0.0)> 0 ) {
				// Now position is in quote currency
				// I sell base currency in bid price
				
				
				double transactionCost = 0.025 / 100;
				double askQuantity = ratio *OpeningPosition;
				double zeroTransactionCostAskQuantity = ratio * OpeningPosition;

				myPrice = Double.parseDouble(fileMember2.askPrice);
				transactionCost = askQuantity * (0.025 / 100);
				double transactionCostPrice = transactionCost * myPrice;
				double soldQuantity  = (askQuantity - transactionCost) * myPrice;
				zeroTransactionCostAskQuantity = zeroTransactionCostAskQuantity * myPrice;
				
				
				if ((Double.compare(transactionCostPrice ,(zeroTransactionCostAskQuantity - OpeningPosition)) < 0)  
					
						&& Double.compare(OpeningPosition, 0.0)> 0 ) {

				
					if (Double.compare(lastUpDCCend, 0.0) == 0)
						lastUpDCCend = myPrice;
					else if (Double.compare(lastUpDCCend, 0.0) > 0 && Double.compare(myPrice,lastUpDCCend) < 0 )
						lastUpDCCend = myPrice;
					
					
					lastSellPrice = myPrice;
					OpeningPosition = OpeningPosition - askQuantity;
					soldPosition = soldPosition + soldQuantity;
					isPositionOpen = true;
					//positionArrayQuote.add(new Double(OpeningPosition));
					tradedPrice.add(new Double(myPrice));
					
				}
			} else if (actionToTake == Type.Downturn && Double.compare(soldPosition, 0.0)> 0 ) {
				// Now position is in base currency
				// I buy base currency
				/*double bidQuantity = trainingOpeningPosition;
				double zeroTransactionCostBidQuantity = trainingOpeningPosition;
				double transactionCostPrice = 0.0;
				myPrice = Double.parseDouble(fileMember2.bidPrice);
				transactionCost = bidQuantity * (0.025 / 100);
				transactionCostPrice = transactionCost * myPrice;
				bidQuantity = (bidQuantity - transactionCost) * myPrice;
				zeroTransactionCostBidQuantity = zeroTransactionCostBidQuantity * myPrice;
*/
				double bidQuantity = soldPosition;
				double zeroTransactionCostBidQuantity =  soldPosition;
				double transactionCost = 0.025 / 100;
				myPrice = Double.parseDouble(fileMember2.bidPrice);
				transactionCost = bidQuantity * (0.025 / 100);
				double transactionCostPrice = transactionCost * myPrice;
				double boughtQuantity  = (bidQuantity - transactionCost) * myPrice;
				zeroTransactionCostBidQuantity = zeroTransactionCostBidQuantity * myPrice;
				
				
				if ((Double.compare(transactionCostPrice , (zeroTransactionCostBidQuantity - boughtQuantity) )< 0) && Double.compare(myPrice,  lastUpDCCend) <0) {
				
					lastBuyPrice = myPrice;
					soldPosition = 0.0;
					OpeningPosition = OpeningPosition +( (bidQuantity - transactionCost) / myPrice);
					tradedPrice.add(new Double(myPrice));
					positionArrayBase.add(OpeningPosition );
				}
			}

		}

		if (Double.compare(soldPosition, 0.0) > 0) {
			double bidQuantity = soldPosition;
			soldPosition = 0.0;
			double transactionCost =  bidQuantity * 0.025 / 100;
			myPrice = Double.parseDouble(FReader.
					dataRecordInFileArray.get(trainingOutputEvents.length-1).askPrice);
			double boughtQuantity  = (bidQuantity - transactionCost) / myPrice;
			OpeningPosition = OpeningPosition + boughtQuantity;
			
			positionArrayBase.add(OpeningPosition + (soldPosition/myPrice));
		}

		otherTradeCalculations();

		return OpeningPosition;
	}
	abstract void estimateTraining(PreProcess preprocess);

	void TrainingDCEventOSEventDistribution() {
		for (int eventCount = 1; eventCount < trainingEvents.length; eventCount++) {

			if (trainingEvents[eventCount].overshoot != null) {
				totalOvershootEventLengthTraining = totalOvershootEventLengthTraining
						+ trainingEvents[eventCount].overshoot.length();
				numberOfTrainingOvershoot = numberOfTrainingOvershoot + 1;
			}
			totalDCEventLengthTraining = totalDCEventLengthTraining + trainingEvents[eventCount].length();
		}
		numberOfTrainingDC = trainingEvents.length - 1;
		int numberOfZerosTraining = numberOfTrainingDC - numberOfTrainingOvershoot;

		zeroPercentageTraining = ((float) numberOfZerosTraining / numberOfTrainingDC) * 100;

		dC_OS_Length_RatioTraining = (totalOvershootEventLengthTraining / totalDCEventLengthTraining) * 100;

	}

	String printTestAskMarketData(ArrayList<FileMember2> dataRecordInFileArray) {
		String rtStringl = "";

		for (FileMember2 mData : dataRecordInFileArray) {
			rtStringl = rtStringl + "," + mData.askPrice;
		}
		return rtStringl;
	}

	String printTestBidMarketData(ArrayList<FileMember2> dataRecordInFileArray) {
		String rtStringl = "";

		for (FileMember2 mData : dataRecordInFileArray) {
			rtStringl = rtStringl + "," + mData.askPrice;
		}
		return rtStringl;
	}

	String printDCAndOSPricePoints(ArrayList<String> DCtype, ArrayList<Double> priceAtDC, ArrayList<Double> priceAtOS) {
		String rtStringl = "";

		if (DCtype.size() != priceAtDC.size() || priceAtDC.size() != DCtype.size())
			return rtStringl;

		for (int i = 0; i < DCtype.size(); i++) {
			rtStringl = rtStringl + "," + DCtype.get(i) + priceAtDC.get(i) + "," + priceAtOS.get(i) + "\n";
		}

		return rtStringl;
	}

	String printDCHasOvershoot(ArrayList<String> DCtype, ArrayList<Boolean> hasOvershoot) {
		String rtStringl = "";
		return rtStringl;
	}

	protected static double calculateBaseSD(ArrayList<Double> numArray) {
		double sum = 0.0, standardDeviation = 0.0;
		int length = numArray.size();

		for (double num : numArray) {
			sum += num;
		}
		double mean = sum / length;
		for (double num : numArray) {
			standardDeviation += Math.pow(num - mean, 2);
		}
		return Math.sqrt(standardDeviation / length - 1);
	}

	protected static double getMaxValue(ArrayList<Double> numbers) {
		double maxValue = numbers.get(0);
		for (double num : numbers) {
			if (num > maxValue) {
				maxValue = num;
			}
		}
		return maxValue;
	}

	protected static double getMinValue(ArrayList<Double> numbers) {
		double minValue = numbers.get(0);
		for (double num : numbers) {
			if (num < minValue) {
				minValue = num;
			}
		}
		return minValue;
	}

	public double getSharpRatio() {
		return simpleSharpeRatio.calulateSharpeRatio();
	}

	public String getUpwardTrendTreeString() {
		return upwardTrendTreeString;
	}

	public String getDownwardTrendTreeString() {
		return downwardTrendTreeString;
	}

	public String getThresholdString() {
		return thresholdString;
	}

	public double getDownwardTrendRatio() {
		return meanRatio[0];
	}

	public double getSingleRatio() {
		return meanRatio[0];
	}

	public double getUpwardTrendRatio() {
		return meanRatio[1];
	}

	public double getDownwardMagnitudeRatio() {
		return meanMagnitudeRatio[0];
	}

	public double getSingleMagnitudeRatio() {
		return meanMagnitudeRatio[0];
	}

	public double getUpwardMagnitudeRatio() {
		return meanMagnitudeRatio[1];
	}

	protected void otherTradeCalculations() {

		if (positionArrayBase.size() > 1) {
			// Calculate sharp ratio start from 1 as it is a moving window
			for (int srCount = 1; srCount < positionArrayBase.size(); srCount++) {
				simpleSharpeRatio.addReturn(positionArrayBase.get(srCount) - positionArrayBase.get(srCount - 1));
				simpleDrawDown.addReturn(positionArrayBase.get(srCount));
			}
			
		}
		
		// else if (positionArrayBase.size() == 1)
		// simpleSharpeRatio.addReturn(positionArrayBase.get(0) -
		// OpeningPosition);
		// closingPosition = currentPosition-initialPosition;
		/*
		 * not in use at the moment for (LinkedHashMap<Integer, Integer> map :
		 * actualTrend) {
		 * 
		 * int key = map.keySet().iterator().next(); int value = map.get(key);
		 * 
		 * actualTrendString = actualTrendString + Double.toString(key) + " ," +
		 * Double.toString(value) + "\n";
		 * 
		 * }
		 * 
		 * for (LinkedHashMap<Integer, Integer> map : anticipatedTrend) { int
		 * key = map.keySet().iterator().next(); int value = map.get(key);
		 * 
		 * predictedTrendString = predictedTrendString + Double.toString(key) +
		 * " ," + Double.toString(value) + "\n";
		 * 
		 * }
		 */

		/*
		 * 
		 * 
		 * not in use at the moment for (int profitLossCount = 0;
		 * profitLossCount < positionArrayQuote.size(); profitLossCount++) {
		 * simpleDrawDownQuote.Calculate(positionArrayQuote.get(profitLossCount)
		 * ); }
		 */
		peakMDD = simpleDrawDown.getMaxDrawDown();
		// peakMDDQuote = simpleDrawDownQuote.getMaxDrawDown();

	}

	public void clearSharpRatio() {
		simpleSharpeRatio.rmoveAllReturns();
	}

	public double getVariance() {
		return simpleSharpeRatio.calulateVariance();
	}

	public void refreshMDD() {
		simpleDrawDown.clearMDD();
		//simpleDrawDownQuote.clearMDD();

	}

	public void setMarketdataListTraining(int counter) {

		for (int i = 0; i < counter; i++) {
			trainingDataList.add(FReader.dataRecordInFileArray.get(i));
		}
	}

	public List<FReader.FileMember2> getMarketDataTraining() {
		List<FReader.FileMember2> bidAskprice;
		bidAskprice = new ArrayList<FReader.FileMember2>(trainingDataList);
		return bidAskprice;
	}

	public List<FReader.FileMember2> getMarketDataTest() {
		List<FReader.FileMember2> bidAskprice;
		bidAskprice = new ArrayList<FReader.FileMember2>(testDataList);
		return bidAskprice;
	}

	public void setMarketdataListTest(int counter) {
		if (trainingDataList.isEmpty()) {

			System.out.println("trainingDataList.isEmpty() existing");
			System.exit(-1);
		}

		int trainingDataSize = trainingDataList.size();
		for (int i = trainingDataSize; i < (counter + trainingDataSize); i++) {
			testDataList.add(FReader.dataRecordInFileArray.get(i));
		}

	}

	public void setOpeningPosition(double position) {
		OpeningPosition = position;
	}

	public double getOpeningPosition() {
		return OpeningPosition;
	}

	public void setOpeningPositionHist(double position) {
		OpeningPositionHist = position;
	}

	public double getOpeningPositionHist() {
		return OpeningPositionHist;
	}

	public void setTrainingOpeningPositionHist(double position) {
		trainingOpeningPositionHist = position;
	}

	public double getTrainingOpeningPositionHist() {
		return trainingOpeningPositionHist;
	}

	public void setTrainingOpeningPosition(double position) {
		trainingOpeningPosition = position;
	}

	public double getTrainingOpeningPosition() {
		return trainingOpeningPosition;
	}

	double getMddPeak() {
		return simpleDrawDown.getPeak();
	}

	double getMddTrough() {
		return simpleDrawDown.getTrough();
	}

	public double getMaxMddBase() {
		simpleDrawDown.recalculateMDD();
		return simpleDrawDown.getMaxDrawDown();
	}

	/*double getMddPeakQuote() {
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

	public void resetNumberOfQuoteCcyTransaction() {
		positionArrayQuote.clear();
	}
*/
	public int getNumberOfBaseCcyTransactions() {

		return positionArrayBase.size() - 1;
	}

	public void resetNumberOfBaseCcyTransaction() {
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
/*
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
*/
	public void setAssociatedWeight(double weight) {
		associatedWeight = weight;
	}

	public double getAssociatedWeight() {
		return associatedWeight;
	}

	public void setThresholdValue(double value) {
		thresholdValue = value;
	}

	public double getThresholdValue() {
		return thresholdValue;
	}

	public void setIsSelectedThresholdFromCandidateList(boolean value) {
		isSelectedThresholdFromCandidateList = value;
	}

	public boolean getIsSelectedThresholdFromCandidateList() {
		return isSelectedThresholdFromCandidateList;
	}

	public void setVarianceValue(double value) {
		varianceValue = value;
	}

	public double getVarianceValue() {
		return simpleSharpeRatio.calulateVariance();
	}

	public void setTradingReturnValaue(double value) {
		tradingReturnValaue = value;
	}

	public double getTradingReturnValue() {
		return tradingReturnValaue;
	}

	public int getNumberOfTransactions() {
		return positionArrayBase.size();
	}

	public void clearPositionArrayBase() {
		positionArrayBase.clear();
	}

/*	public void clearPositionArrayQuote() {
		positionArrayQuote.clear();
	}*/

	public void clearActualTrendString() {
		actualTrendString = "";
	}

	public void clearPredictedTrendString() {
		predictedTrendString = "";
	}

	public void estimateTrainingUsingOutputData(PreProcess preprocess) {
		trainingUsingOutputData = new double[trainingOutputEvents.length];

		for (int outputIndex = 0; outputIndex < trainingOutputEvents.length - 1; outputIndex++) {

			trainingUsingOutputData[outputIndex] = HelperClass.estimateOSlength(outputIndex, trainingOutputEvents,
					bestUpWardEventTree, bestDownWardEventTree);

		}

	}

	public void setIsPositionOpen(boolean value) {
		isPositionOpen = value;
	}

	public boolean getIsPositionOpen() {
		return isPositionOpen;
	}
	
	
	protected double getTransanction(int i) {
		if ( i >= positionArrayBase.size())
			return 0.0;
		
		return positionArrayBase.get(i);
	}
	
	public void setMySharpeRatio(double sr){
		mySharpeRatio =  sr;
	}
	
	public double getMySharpeRatio(){
		return mySharpeRatio;
	}


}
