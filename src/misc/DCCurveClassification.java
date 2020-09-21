package misc;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Vector;
import dc.GP.AbstractNode;
import dc.GP.Const;
import dc.GP.TreeHelperClass;
import dc.ga.HelperClass;
import dc.ga.PreProcess;
import dc.io.FReader;
import dc.io.FReader.FileMember2;
import dc.ga.DCCurve.Event;
import dc.ga.DCCurve.Type;
import files.FWriter;

public class DCCurveClassification extends DCCurveRegression {
	public DCCurveClassification() {
		super();
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
	public void build(Double[] values, double delta, String GPTreeFileName, Event[] trainingEvents,
			Event[] trainingOutput,PreProcess preprocess) {

		String thresholdStr = String.format("%.8f", delta);
		thresholdString = thresholdStr;
		thresholdValue = delta;
		if (trainingEvents == null || trainingEvents.length < 1)
			return;

		this.trainingEvents = Arrays.copyOf(trainingEvents, trainingEvents.length);
		this.trainingOutputEvents =  Arrays.copyOf(trainingOutput, trainingOutput.length);
		
		

		if (bestDownWardEventTree != null && bestUpWardEventTree != null ){
			return;
			
		}
		else
		{
			System.out.println("Please evolve GP Trees under perfect foresight... exiting");
			System.exit(-1);
		}
			
	}

	/**
	 * 
	 * @param values
	 *            The array with prices/tick data
	 * @param delta
	 *            The threshold value
	 * @param GPTreeFileName
	 *            the name of the file where GP tree is stored
	 * @param
	 */
	public void testbuild(int lastTrainingPricePosition, Double[] values, double delta, Event[] testEvents,
			PreProcess preprocess) {
		lastTrainingPrice = lastTrainingPricePosition;
		if (testEvents == null || testEvents.length < 1)
			return;

		testingEvents = Arrays.copyOf(testEvents, testEvents.length);
		predictionWithClassifier = new double[testEvents.length];

		for (int outputIndex = 0; outputIndex < testEvents.length - 1; outputIndex++) {
			String foo = "";
			double eval = 0.0;
				
			String classificationStr = "no";
			if (preprocess != null)
				classificationStr = preprocess.classifyTestInstance(outputIndex);

			if ((classificationStr.compareToIgnoreCase("no") == 0)) {
				;// System.out.println("no");
			} else {
				if (testEvents[outputIndex].type == Type.Upturn) {
					
					eval = bestUpWardEventTree.eval(testEvents[outputIndex].length());
				} else if (testEvents[outputIndex].type == Type.Downturn) {
					eval = bestDownWardEventTree.eval(testEvents[outputIndex].length());

				} else {
					System.out.println("DCCurveClassification - DCCurveClassification - Invalid event");
					continue;
				}
			}

			

			predictionWithClassifier[outputIndex] = eval;
		}
	}

	private String calculateRMSEClassifier(Event[] trendEvent, double delta, double[] runPrediction) {
		
		double rmse = 0.0;
		for (int eventCount = 1; eventCount < trendEvent.length; eventCount++) {
			int os = 0;

			if (trendEvent.length != runPrediction.length) {
				System.out.println("Event and prediction not equal");
				System.exit(0);
			}

			if (trendEvent[eventCount].overshoot != null) {
				os = trendEvent[eventCount].overshoot.length();
				// numberOfTestOvershoot = numberOfTestOvershoot + 1;
			}

			// numberOfTestDC = trendEvent.length;

			double prediction = runPrediction[eventCount];

			// System.out.println("DC:" + trendEvent[eventCount].length() + "
			// OS:" + os + " prediction:" + prediction);
			rmse = rmse + ((os - prediction) * (os - prediction));

			if (rmse == Double.MAX_VALUE || rmse == Double.NEGATIVE_INFINITY || rmse == Double.NEGATIVE_INFINITY
					|| rmse == Double.NaN || Double.isNaN(rmse) || Double.isInfinite(rmse)
					|| rmse == Double.POSITIVE_INFINITY) {
				System.out.println("Invalid RMSE: " + rmse + ". discarding ");
				// predictionRmseClassifier= 10.0;
				predictionRmse = 10.0;
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
	////

	public String reportTestClassifier(Double[] values, double delta, String GPTreeFileName) {
		return calculateRMSEClassifier(testingEvents, delta, predictionWithClassifier);

	}

	@Override
	String report(Double[] values, double delta, String GPTreeFileName) {
		return calculateRMSEClassifier(testingEvents, delta, predictionWithClassifier);

	}

	@Override
	public
	double trade(PreProcess preprocess) {
		boolean isPositionOpen = false;
		double myPrice = 0.0;
		double transactionCost = 0.025 / 100;
		simpleDrawDown.Calculate(OpeningPosition);
		simpleSharpeRatio.addReturn(0);
		//System.out.println("classification: current processor count " + SymbolicRegression.currentProcessorCounter);
		
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
			
			int nextEventEndPOint = HelperClass.getNextDirectionaChangeEndPoint(testingEvents,  tradePoint);

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
					

					LinkedHashMap<Integer, Integer> anticipatedTrendMap = new LinkedHashMap<Integer, Integer>();
					LinkedHashMap<Integer, Integer> actualTrendMap = new LinkedHashMap<Integer, Integer>();

					if (testingEvents[i].type == Type.Upturn && !isPositionOpen) {
						// Now position is in quote currency
						// I sell base currency in bid price
						double askQuantity = OpeningPosition;
						double zeroTransactionCostAskQuantity = OpeningPosition;
						double transactionCostPrice = 0.0;
						myPrice = Double.parseDouble(bidAskprice.get(tradePoint).askPrice);
						
						
						transactionCost = askQuantity * (0.025/100);
						transactionCostPrice = transactionCost * myPrice;
						askQuantity =  (askQuantity -transactionCost) *myPrice;
						zeroTransactionCostAskQuantity = zeroTransactionCostAskQuantity *myPrice;
						//transactionCost = trainingOpeningPosition * (0.025/100);
						//trainingOpeningPosition =  (trainingOpeningPosition -transactionCost) *myPrice;
						
						
						if (transactionCostPrice < (zeroTransactionCostAskQuantity - askQuantity)){
						
							
							lastUpDCCend = Double.parseDouble(FReader.dataRecordInFileArray.get((lastTrainingPrice - 1) + testingEvents[i].end).bidPrice);
							
							lastSellPrice = myPrice;
							OpeningPosition = askQuantity;
							isPositionOpen = true;
							positionArrayQuote.add(new Double(OpeningPosition));
							tradedPrice.add(new Double(myPrice));
							anticipatedTrendMap.put(testingEvents[i].start, tradePoint);
							anticipatedTrend.add(anticipatedTrendMap);
							
							if (testingEvents[i].overshoot == null || testingEvents[i].overshoot.length() < 1)
								actualTrendMap.put(testingEvents[i].start, testingEvents[i].end );
							else
								actualTrendMap.put(testingEvents[i].start, testingEvents[i].overshoot.end );
							
							actualTrend.add(actualTrendMap);
						}

					} else if (testingEvents[i].type == Type.Downturn && isPositionOpen) {
						// Now position is in base currency
						// I buy base currency
						double bidQuantity = OpeningPosition;
						double zeroTransactionCostBidQuantity = OpeningPosition;
						double transactionCostPrice = 0.0;
						myPrice = Double.parseDouble(bidAskprice.get(tradePoint).bidPrice);

						transactionCost = bidQuantity * (0.025 / 100);
						transactionCostPrice = transactionCost * myPrice;
						bidQuantity = (bidQuantity - transactionCost) * myPrice;
						zeroTransactionCostBidQuantity = zeroTransactionCostBidQuantity * myPrice;
					
						if (transactionCostPrice < (zeroTransactionCostBidQuantity - bidQuantity)
								&& myPrice < lastUpDCCend){
										
						
							lastBuyPrice = myPrice;
							OpeningPosition = (OpeningPosition - transactionCost) / myPrice;
							
							isPositionOpen = false;
							positionArrayBase.add(new Double(OpeningPosition));
							tradedPrice.add(new Double(myPrice));
							anticipatedTrendMap.put(testingEvents[i].start, tradePoint);
							anticipatedTrend.add(anticipatedTrendMap);

							if (testingEvents[i].overshoot == null || testingEvents[i].overshoot.length() < 1)
								actualTrendMap.put(testingEvents[i].start, testingEvents[i].end);
							else
								actualTrendMap.put(testingEvents[i].start, testingEvents[i].overshoot.end);

							actualTrend.add(actualTrendMap);
						}
					}
				} catch (ArrayIndexOutOfBoundsException exception) {
					System.out.println(" DCCurveClassiifcation: Search for element " + ((lastTrainingPrice - 1) + tradePoint)
							+ " is beyond the size of price array  " + 
							FReader.dataRecordInFileArray.size() + " . Trading ended") ;
					break;
					
				}
				catch (IndexOutOfBoundsException exception ){
					System.out.println(" DCCurveClassiifcation: Search for element " + ((lastTrainingPrice - 1) + tradePoint)
							+ " is beyond the size of price array  " + 
							FReader.dataRecordInFileArray.size() + " . Trading ended") ;
					break;
				}
				catch (Exception exception ){
					System.out.println(" DCCurveClassiifcation: Search for element " + ((lastTrainingPrice - 1) + tradePoint)
							+ " is beyond the size of price array  " + 
							FReader.dataRecordInFileArray.size() + " . Trading ended") ;
					break;
				}

			}

		}

		if (isPositionOpen) {
			tradedPrice.remove(tradedPrice.size() - 1);
			anticipatedTrend.remove(anticipatedTrend.size() - 1);
			actualTrend.remove(actualTrend.size() - 1);
			if (!positionArrayBase.isEmpty())
				OpeningPosition = positionArrayBase.get(positionArrayBase.size() - 1);
			else
				OpeningPosition = OpeningPositionHist;
			
			if (!positionArrayQuote.isEmpty())
				positionArrayQuote.remove(positionArrayQuote.size() - 1);
			
			isPositionOpen = false;
		}
		
		otherTradeCalculations();

		return OpeningPosition;
	}

	
	@Override
	public String getDCCurveName() {

		return "DCCurveClassification";
	}

	@Override
	double trainingTrading(PreProcess preprocess) {
		
		return Double.MIN_VALUE;

	}

	@Override
	public
	void estimateTraining(PreProcess preprocess) {
		trainingGpPrediction = new double[trainingEvents.length];
		

		for (int outputIndex = 0; outputIndex < trainingEvents.length; outputIndex++) {
			String foo = "";
			
			double eval = 0.0;

			String classificationStr = "no";

			if (preprocess != null)
				classificationStr = preprocess.classifyTrainingInstance(outputIndex);

			if ((classificationStr.compareToIgnoreCase("no") == 0)) {
				;// System.out.println("no");
			} else {
				if (trainingEvents[outputIndex].type == Type.Upturn) {
					eval = bestUpWardEventTree.eval(trainingEvents[outputIndex].length());
				} else if (trainingEvents[outputIndex].type == Type.Downturn) {
					eval = bestDownWardEventTree.eval(trainingEvents[outputIndex].length());
				}
			}

			BigDecimal bd = null;
			BigDecimal bd2 = null;
			try {
				bd = new BigDecimal(eval);
				bd2 = new BigDecimal(Double.toString(eval));
			} catch (NumberFormatException e) {
				Integer integerObject = new Integer(trainingEvents[outputIndex].length());
				eval = integerObject.doubleValue() * (double) Const.NEGATIVE_EXPRESSION_REPLACEMENT;
			}

			trainingGpPrediction[outputIndex] = eval;
		}

	}

	@Override
	public String getActualTrend() {
		return actualTrendString;
	}

	@Override
	public String getPredictedTrend() {
		return predictedTrendString;
	}
	
	@Override
	protected int getMaxTransactionSize() {
		// TODO Auto-generated method stub
		return positionArrayBase.size();
	}

	
	@Override
	protected double getTransanction(int i) {
		if ( i >= positionArrayBase.size())
			return 0.0;
		
		return positionArrayBase.get(i);
	}
	
	public  double calculateSD(){
		return calculateBaseSD(positionArrayBase);
	}
	
	public  double getMaxValue(){
		  
		  return getMaxValue(positionArrayBase);
		}
	public   double getMinValue(){
	 
	  return   getMinValue(positionArrayBase);
	}
	
	@Override
	public <E> void assignPerfectForesightRegressionModel(E[] inputArray) {
		downwardTrendTreeString = (String) inputArray[0];
		upwardTrendTreeString = (String) inputArray[1];
		
	}
}

