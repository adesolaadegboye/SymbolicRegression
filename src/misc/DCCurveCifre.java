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
import java.util.Vector;

import dc.GP.AbstractNode;
import dc.GP.Const;
import dc.GP.TreeHelperClass;
import dc.ga.HelperClass;
import dc.ga.PreProcess;
import dc.ga.DCCurve.Event;
import dc.ga.DCCurve.Type;
import dc.io.FReader;
import dc.io.FReader.FileMember2;
import files.FWriter;

public class DCCurveCifre extends DCCurveRegression {

	public DCCurveCifre() {
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
	public void build(Double[] values, double delta, String GPTreeFileName, Event[] trainingEevents,
			 Event[] trainingOutput,
			PreProcess preprocess) {

		String thresholdStr = String.format("%.8f", delta);
		thresholdString = thresholdStr;
		if (trainingEevents == null || trainingEevents.length < 1)
			return;

		trainingEvents = Arrays.copyOf(trainingEevents, trainingEevents.length);
		trainingOutputEvents =  Arrays.copyOf(trainingOutput, trainingOutput.length);
		
		if (upwardTrendTreeString != null && !upwardTrendTreeString.isEmpty() && 
				downwardTrendTreeString != null && !downwardTrendTreeString.isEmpty()){
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

		if (testEvents == null || testEvents.length < 1)
			return;

		testingEvents = Arrays.copyOf(testEvents, testEvents.length);
		String thresholdStr = String.format("%.8f", delta);

		

		gpprediction = new double[testingEvents.length];

		for (int outputIndex = 0; outputIndex < testingEvents.length; outputIndex++) {
			String foo = "";
		
				if (testingEvents[outputIndex].type == Type.Upturn) {
					foo = upwardTrendTreeString;
					numberOfUpwardEvent++;
					isUpwardEvent = true;
				} else if (testEvents[outputIndex].type == Type.Downturn) {
					foo = downwardTrendTreeString;
					numberOfDownwardEvent++;
					isUpwardEvent = false;
				} else {
					System.out.println("DCCurveCifre - DCCurveCifre - Invalid event");
					continue;
				}
			

			foo = foo.replace("X0", Integer.toString(testingEvents[outputIndex].length()));
			foo = foo.replace("X1", Double.toString(testingEvents[outputIndex].high - testingEvents[outputIndex].low) );
			double eval = 0.0;
			
			if (testingEvents[outputIndex].type == Type.Upturn) {
				
				eval = bestUpWardEventTree.eval(testingEvents[outputIndex].length());
			} else if (testingEvents[outputIndex].type == Type.Downturn) {
				eval = bestDownWardEventTree.eval(testingEvents[outputIndex].length());

			} else {
				System.out.println("DCCurveCifre - DCCurveCifre - Invalid event");
				continue;
			}
			gpprediction[outputIndex] = eval;

		}

	}

	private String calculateRMSE(Event[] trendEvent, double delta, double[] runPrediction) {

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

			double prediction = runPrediction[eventCount];

			// wrapper is 0

			if (prediction > 500 * trendEvent[eventCount].length()) {
				System.out.println("Unable to predict trend. GP predicted" + prediction + " setting to 0");
				prediction = 0;
			}

			// System.out.println("DC:" + trendEvent[eventCount].length() + "
			// OS:" + os + " prediction:" + prediction);
			rmse = rmse + ((os - prediction) * (os - prediction));

			// System.out.println("DC:" + trendEvent[eventCount].length() + "
			// OS:" + os + " prediction:" + prediction);

			if (rmse == Double.MAX_VALUE || rmse == Double.NEGATIVE_INFINITY || rmse == Double.NEGATIVE_INFINITY
					|| rmse == Double.NaN || Double.isNaN(rmse) || Double.isInfinite(rmse)
					|| rmse == Double.POSITIVE_INFINITY) {
				System.out.println("Invalid RMSE: " + rmse + ". discarding ");
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
			System.out.println("Invalid predictionRmse: " + predictionRmse + " discarding ");
			predictionRmse = 10.0;
		}
		// System.out.println(predictionRmse);
		return Double.toString(predictionRmse);
	}

	public String reportTest(Double[] values, double delta, String GPTreeFileName) {
		return calculateRMSE(testingEvents, delta, gpprediction);

	}

	@Override
	String report(Double[] values, double delta, String GPTreeFileName) {
		return calculateRMSE(testingEvents, delta, gpprediction);
	}

	@Override
	double trade(PreProcess preprocess) {
		boolean isPositionOpen = false;
		double myPrice = 0.0;
		double transactionCost = 0.025 / 100;
		simpleDrawDown.Calculate(OpeningPosition);
		simpleSharpeRatio.addReturn(0);

		lastSellPrice = 0.0;
		lastBuyPrice = 0.0;
		
		double lastUpDCCend = 0.0;
		for (int i = 1; i < testingEvents.length; i++) {

			int tradePoint = testingEvents[i].end + (int) gpprediction[i];

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

			FReader freader = new FReader();
			FileMember2 fileMember2 = freader.new FileMember2();
			// fileMember2.Day = GPtestEvents[i].endDate;
			// fileMember2.time = GPtestEvents[i].endTime;
			// fileMember2.price = GPtestEvents[i].endPrice;

			if (tradePoint > FReader.dataRecordInFileArray.size()
					|| (lastTrainingPrice - 1) + tradePoint >= FReader.dataRecordInFileArray.size() - 1) {
				System.out.println(" DCCurveCifre: predicted datapoint " + ((lastTrainingPrice - 1) + tradePoint)
						+ " is beyond the size of price array  " + FReader.dataRecordInFileArray.size()
						+ " . Trading ended");
				break;
			} else {
				// I am opening my position in base currency
				try {
					// I am opening my position in base currency
					fileMember2 = FReader.dataRecordInFileArray.get((lastTrainingPrice - 1) + tradePoint);

					LinkedHashMap<Integer, Integer> anticipatedTrendMap = new LinkedHashMap<Integer, Integer>();
					LinkedHashMap<Integer, Integer> actualTrendMap = new LinkedHashMap<Integer, Integer>();

					if (testingEvents[i].type == Type.Upturn && !isPositionOpen) {
						// Now position is in quote currency
						// I sell base currency in bid price
						double askQuantity = OpeningPosition;
						double zeroTransactionCostAskQuantity = OpeningPosition;
						double transactionCostPrice = 0.0;
						myPrice = Double.parseDouble(fileMember2.askPrice);

						transactionCost = askQuantity * (0.025 / 100);
						transactionCostPrice = transactionCost * myPrice;
						askQuantity = (askQuantity - transactionCost) * myPrice;
						zeroTransactionCostAskQuantity = zeroTransactionCostAskQuantity * myPrice;
						// transactionCost = trainingOpeningPosition *
						// (0.025/100);
						// trainingOpeningPosition = (trainingOpeningPosition
						// -transactionCost) *myPrice;

						if (transactionCostPrice < (zeroTransactionCostAskQuantity - askQuantity) ){
						
							OpeningPosition = askQuantity;
							
							lastUpDCCend = Double.parseDouble(FReader.dataRecordInFileArray.get((lastTrainingPrice - 1) + testingEvents[i].end).bidPrice);
							
							isPositionOpen = true;
							positionArrayQuote.add(new Double(OpeningPosition));
							lastSellPrice = myPrice;
							tradedPrice.add(new Double(myPrice));
							anticipatedTrendMap.put(testingEvents[i].start, tradePoint);
							anticipatedTrend.add(anticipatedTrendMap);
							

							if (testingEvents[i].overshoot == null || testingEvents[i].overshoot.length() < 1)
								actualTrendMap.put(testingEvents[i].start, testingEvents[i].end);
							else
								actualTrendMap.put(testingEvents[i].start, testingEvents[i].overshoot.end);

							actualTrend.add(actualTrendMap);
						}
					} else if (testingEvents[i].type == Type.Downturn && isPositionOpen) {
						// Now position is in base currency
						// I buy base currency
						double bidQuantity = OpeningPosition;
						double zeroTransactionCostBidQuantity = OpeningPosition;
						double transactionCostPrice = 0.0;
						myPrice = Double.parseDouble(fileMember2.bidPrice);

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
					System.out.println(" DCCurveCifre: Search for element " + ((lastTrainingPrice - 1) + tradePoint)
							+ " is beyond the size of price array  " + FReader.dataRecordInFileArray.size()
							+ " . Trading ended");
					break;

				}
				catch (IndexOutOfBoundsException exception ){
					System.out.println(" DCCurveCifre: Search for element " + ((lastTrainingPrice - 1) + tradePoint)
							+ " is beyond the size of price array  " + 
							FReader.dataRecordInFileArray.size() + " . Trading ended") ;
					break;
				}
				catch (Exception exception ){
					System.out.println(" DCCurveCifre: Search for element " + ((lastTrainingPrice - 1) + tradePoint)
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
			OpeningPosition = positionArrayBase.get(positionArrayBase.size() - 1);
			positionArrayQuote.remove(positionArrayQuote.size() - 1);
			isPositionOpen = false;
		}
		
		otherTradeCalculations();

		
		return OpeningPosition;
	}

	double getMddPeak() {
		return simpleDrawDown.getPeak();
	}

	double getMddTrough() {
		return simpleDrawDown.getTrough();
	}

	double getMaxMddBase() {
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

	int getNumberOfBaseCcyTransactions() {

		return positionArrayBase.size() - 1;
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

	@Override
	public String getDCCurveName() {
		return "DCCurveCifre";
	}

	@Override
	double trainingTrading(PreProcess preprocess) {
		
		return Double.MIN_VALUE;

	}

	@Override
	void estimateTraining(PreProcess preprocess) {
		trainingGpPrediction = new double[trainingEvents.length];
		

		for (int outputIndex = 0; outputIndex < trainingEvents.length; outputIndex++) {
			String foo = "";
			if (Const.splitDatasetByTrendType) {
				if (trainingEvents[outputIndex].type == Type.Upturn) {
					foo = upwardTrendTreeString;
					numberOfUpwardEvent++;
					isUpwardEvent = true;
				} else if (trainingEvents[outputIndex].type == Type.Downturn) {
					foo = downwardTrendTreeString;
					numberOfDownwardEvent++;
					isUpwardEvent = false;
				} else {
					System.out.println("DCCurveCifre - DCCurveCifre - Invalid event");
					continue;
				}
			} else {
				foo = trendTreeString;
			}

			foo = foo.replace("X0", Integer.toString(trainingEvents[outputIndex].length()));
			foo = foo.replace("X1", Double.toString(trainingEvents[outputIndex].high - trainingEvents[outputIndex].low) );
			double eval = 0.0;
			
			if (trainingEvents[outputIndex].type == Type.Upturn) {
				
				eval = bestUpWardEventTree.eval(trainingEvents[outputIndex].length());
			} else if (trainingEvents[outputIndex].type == Type.Downturn) {
				eval = bestDownWardEventTree.eval(trainingEvents[outputIndex].length());

			} else {
				System.out.println("DCCurveCifre - DCCurveCifre - Invalid event");
				continue;
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
