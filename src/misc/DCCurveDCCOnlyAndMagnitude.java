package misc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import dc.GP.Const;
import dc.ga.GA;
import dc.ga.PreProcess;
import dc.ga.DCCurve.Event;
import dc.ga.DCCurve.Type;
import dc.io.FReader;
import dc.io.FReader.FileMember2;

public class DCCurveDCCOnlyAndMagnitude extends DCCurveRegression {

	public DCCurveDCCOnlyAndMagnitude() {
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
	public void build(Double[] values, double delta, String GPTreeFileName, Event[] events, PreProcess preprocess) {
		String thresholdStr = String.format("%.8f", delta);
		thresholdString = thresholdStr;

		if (events == null || events.length < 1)
			return;

		trainingEvents = Arrays.copyOf(events, events.length);

	}

	public void testbuild(int lastTrainingPricePosition, Double[] values, double delta, Event[] testEvents,
			PreProcess preprocess) {
		lastTrainingPrice = lastTrainingPricePosition;
		if (testEvents == null || testEvents.length < 1)
			return;

		testingEvents = Arrays.copyOf(testEvents, testEvents.length);
		String thresholdStr = String.format("%.8f", delta);

		ScriptEngineManager mgr = new ScriptEngineManager();
		ScriptEngine engine = mgr.getEngineByName("JavaScript");

		predictionMagnitudeWithClassifier = new double[testEvents.length];

		for (int outputIndex = 0; outputIndex < testEvents.length - 1; outputIndex++) {
			String foo2 = "";
			if (testEvents[outputIndex].type == Type.Upturn) {
				foo2 = upwardTrendMagnitudeTreeString;
				foo2 = foo2.replace("X0", FReader.dataRecordInFileArray.get(testEvents[outputIndex].end).askPrice);

			} else if (testEvents[outputIndex].type == Type.Downturn) {
				foo2 = downwardTrendMagnitudeTreeString;
				foo2 = foo2.replace("X0", FReader.dataRecordInFileArray.get(testEvents[outputIndex].end).bidPrice);

			} else {
				System.out.println("Invalid event");
				System.exit(0);
			}
			
			double eval2 = 0.0;
			Double javascriptValue = Double.MAX_VALUE;
			
			
			try {
				javascriptValue = (Double) engine.eval(foo2);
				eval2 = javascriptValue.doubleValue();
			} catch (ScriptException e) {
				e.printStackTrace();
			} catch (ClassCastException e) {
				e.printStackTrace();
			}
			

			BigDecimal bd = null;
			BigDecimal bd2 = null;
			
			
			try {
				bd = new BigDecimal(eval2);
				bd2 = new BigDecimal(Double.toString(eval2));
			} catch (NumberFormatException e) {
				Double doubleObject = null;
				if (testEvents[outputIndex].type == Type.Upturn) {
					doubleObject = Double.parseDouble(FReader.dataRecordInFileArray.get(testEvents[outputIndex].end).askPrice);
				}
				else{
					doubleObject = Double.parseDouble(FReader.dataRecordInFileArray.get(testEvents[outputIndex].end).bidPrice);
				}
					eval2 = doubleObject.doubleValue();
			}

			
			predictionMagnitudeWithClassifier[outputIndex] = eval2;
		}

	}

	private String calculateRMSE_DCCOnlyAndTrail(Event[] trendEvent, double delta, double[] runPrediction) {
		// System.out.println(predictionRmse);
		return Double.toString(Double.MAX_VALUE);

	}

	////

	public String reportTestMF(Double[] values, double delta, String GPTreeFileName) {
		return calculateRMSE_DCCOnlyAndTrail(testingEvents, delta, predictionWithClassifier);

	}

	@Override
	String report(Double[] values, double delta, String GPTreeFileName) {
		return calculateRMSE_DCCOnlyAndTrail(testingEvents, delta, predictionWithClassifier);
	}

	@Override
	double trade(PreProcess preprocess) {
		boolean isPositionOpen = false;
		double myPrice = 0.0;
		double previousDirectionalChangeEndPrice = 0.0;
		double transactionCost = 0.025 / 100;
		simpleDrawDown.Calculate(OpeningPosition);
		simpleSharpeRatio.addReturn(0);
		lastSellPrice = 0.0;
		lastBuyPrice = 0.0;
		StartSellQuantity = -1.0;
		StartBuyQuantity = -1.0;
		for (int i = 1; i < testingEvents.length - 1; i++) {
			int tradePoint = testingEvents[i].end;

			if (testingEvents[i] == null)
				continue;

			if (i + 1 > testingEvents.length - 1)
				continue;

			if (testingEvents[i + 1] == null)
				continue;

			if (tradePoint > testingEvents[i + 1].end) // If a new DC is
															// encountered
															// before the
															// estimation point
															// skip trading
				continue;

			FReader freader = new FReader();
			FileMember2 fileMember2 = freader.new FileMember2();
			
			boolean isActionTaken = false;
			
			Double magnitude = new Double(predictionMagnitudeWithClassifier[i]);
			Double zeroOs = new Double(0.0);
/*
			if (magnitude.equals(zeroOs)){
				if (testingEvents[i].type == Type.Upturn)
					magnitude = Math.abs(Math.max(testingEvents[i].high, testingEvents[i].low));
				else
					magnitude = Math.abs(Math.min(testingEvents[i].high, testingEvents[i].low));
			}
			else {
				if (testingEvents[i].type == Type.Upturn)
					magnitude = magnitude + Math.max(testingEvents[i].high, testingEvents[i].low);
				else
					magnitude =   Math.min(testingEvents[i].high, testingEvents[i].low) - magnitude;
			}
	*/
			
			for (int tradePointCointer = tradePoint; tradePointCointer < testingEvents[i
					+ 1].end; tradePointCointer++) {
				if (tradePointCointer > FReader.dataRecordInFileArray.size()
						|| ((lastTrainingPrice - 1) + tradePointCointer) > FReader.dataRecordInFileArray.size()) {
					System.out.println(" DCCurveDCCOnlyAndTrail: predicted datapoint "
							+ ((lastTrainingPrice - 1) + tradePointCointer) + " is beyond the size of price array  "
							+ FReader.dataRecordInFileArray.size() + " . Trading ended");
					break;
				} else {
					if (isActionTaken) {
						break;
					}
					// I am opening my position in base currency
					try {
						// I am opening my position in base currency
						fileMember2 = FReader.dataRecordInFileArray.get((lastTrainingPrice - 1) + tradePointCointer);
							
						LinkedHashMap<Integer, Integer> anticipatedTrendMap = new LinkedHashMap<Integer, Integer>();
						LinkedHashMap<Integer, Integer> actualTrendMap = new LinkedHashMap<Integer, Integer>();
						/*	We only use magnitude used for opening a positio
							If we start with a sell will use only upturn magnitude
							If we start witha buy we use on downturn magnitude
						*/
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

							if (transactionCostPrice < (zeroTransactionCostAskQuantity - askQuantity)
									&& myPrice >= magnitude){
		//					if (transactionCostPrice < (zeroTransactionCostAskQuantity - askQuantity)
		//							&& (((lastSellPrice > 0.0) ? ((myPrice >= lastSellPrice) ? true : false): true ) ||
		//									(StartSellQuantity > -1.0  ? ((StartSellQuantity <= askQuantity) ? true : false) : true  ))) {
								
								if (StartSellQuantity <= -1.0)
									StartSellQuantity = OpeningPosition;
								
								previousDirectionalChangeEndPrice = Double.parseDouble(FReader.dataRecordInFileArray.get((lastTrainingPrice - 1) + testingEvents[i].end).bidPrice);

								
								
								lastSellPrice = myPrice;
								OpeningPosition = askQuantity;
								isPositionOpen = true;
								positionArrayQuote.add(new Double(OpeningPosition));

								tradedPrice.add(new Double(myPrice));
								anticipatedTrendMap.put(testingEvents[i].start, tradePointCointer);
								anticipatedTrend.add(anticipatedTrendMap);

								if (testingEvents[i].overshoot == null || testingEvents[i].overshoot.length() < 1)
									actualTrendMap.put(testingEvents[i].start, testingEvents[i].end);
								else
									actualTrendMap.put(testingEvents[i].start, testingEvents[i].overshoot.end);

								actualTrend.add(actualTrendMap);
								isActionTaken = true;
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
									&&  myPrice < previousDirectionalChangeEndPrice){
			//				if (transactionCostPrice < (zeroTransactionCostBidQuantity - bidQuantity)
			//						&& (( lastBuyPrice > 0.0 ? ((myPrice <= lastBuyPrice ) ? true :false ): true )||
			//								(StartBuyQuantity > -1.0  ? ((StartBuyQuantity > bidQuantity) ? true: false) : true  ))) {
												
										if (StartBuyQuantity <= -1.0)
											StartBuyQuantity = OpeningPosition;
										
								lastBuyPrice = myPrice;
								OpeningPosition = (OpeningPosition - transactionCost) / myPrice;

								isPositionOpen = false;
								positionArrayBase.add(new Double(OpeningPosition));

								tradedPrice.add(new Double(myPrice));
								anticipatedTrendMap.put(testingEvents[i].start, tradePointCointer);
								anticipatedTrend.add(anticipatedTrendMap);

								if (testingEvents[i].overshoot == null || testingEvents[i].overshoot.length() < 1)
									actualTrendMap.put(testingEvents[i].start, testingEvents[i].end);
								else
									actualTrendMap.put(testingEvents[i].start, testingEvents[i].overshoot.end);

								actualTrend.add(actualTrendMap);
								isActionTaken = true;
							}
						}
					} catch (ArrayIndexOutOfBoundsException exception) {
						System.out.println(" DCCurveClassificationMF: Search for element "
								+ ((lastTrainingPrice - 1) + tradePointCointer) + " is beyond the size of price array  "
								+ FReader.dataRecordInFileArray.size() + " . Trading ended");
						break;

					}

				}
			} // for (int tradePointCointer = tradePoint; tradePointCointer <
				// testingEvents.length; tradePointCointer++)

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

		return "DCCurveClassificationOnlyAndTrail";
	}

	@Override
	double trainingTrading(PreProcess preprocess) {
		boolean isPositionOpen = false;
		double myPrice = 0.0;
		double lastClosedPosition = 0.0;
		double transactionCost = 0.025 / 100;
		lastSellPrice = 0.0;
		lastBuyPrice = 0.0;
		StartSellQuantity = -1.0;
		StartBuyQuantity = -1.0;
		for (int i = 1; i < trainingEvents.length; i++) {

			int tradePoint = 0;
			//Double magnitude = new Double(predictionMagnitudeWithClassifier[i]);
			Double zeroOs = new Double(0.0);
			
			if (trainingEvents[i] == null)
				continue;

			if (i + 1 > trainingEvents.length - 1)
				continue;

			if (trainingEvents[i + 1] == null)
				continue;

			if (tradePoint > trainingEvents[i + 1].start) // If a new DC is
															// encountered
															// before the
															// estimation point
															// skip trading
				continue;

		/*	if (magnitude.equals(zeroOs)){
				if (trainingEvents[i].type == Type.Upturn)
					magnitude = Math.abs(Math.max(trainingEvents[i].high, trainingEvents[i].low));
				else
					magnitude = Math.abs(Math.min(testingEvents[i].high, trainingEvents[i].low));
			}
			else {
				if (trainingEvents[i].type == Type.Upturn)
					magnitude = magnitude + Math.max(trainingEvents[i].high, trainingEvents[i].low);
				else
					magnitude =   Math.min(trainingEvents[i].high, trainingEvents[i].low) - magnitude;
			}
			*/
			
			FReader freader = new FReader();
			FileMember2 fileMember2 = freader.new FileMember2();
			// fileMember2.Day = GPtestEvents[i].endDate;
			// fileMember2.time = GPtestEvents[i].endTime;
			// fileMember2.price = GPtestEvents[i].endPrice;

			if (tradePoint > FReader.dataRecordInFileArray.size() - 1) {
				continue;
			}

			boolean isActionTaken = false;

			for (int tradePointCointer = tradePoint; tradePointCointer < trainingEvents[i
					+ 1].start; tradePointCointer++) {
				if (isActionTaken) {
					break;
				}
				
				// I am opening my position in base currency
				try {
				fileMember2 = FReader.dataRecordInFileArray.get(tradePointCointer);
				}
				catch (ArrayIndexOutOfBoundsException e){
					System.out.println(e.getMessage());
					continue;
				}
				
				
				if (trainingEvents[i].type == Type.Upturn && !isPositionOpen) {
					// Now position is in quote currency
					// I sell base currency in bid price

					// I sell base currency in bid price
					double askQuantity = trainingOpeningPosition;
					double zeroTransactionCostAskQuantity = trainingOpeningPosition;
					double transactionCostPrice = 0.0;
					myPrice = Double.parseDouble(fileMember2.askPrice);

					transactionCost = askQuantity * (0.025 / 100);
					transactionCostPrice = transactionCost * myPrice;
					askQuantity = (askQuantity - transactionCost) * myPrice;
					zeroTransactionCostAskQuantity = zeroTransactionCostAskQuantity * myPrice;
					// transactionCost = trainingOpeningPosition * (0.025/100);
					// trainingOpeningPosition = (trainingOpeningPosition
					// -transactionCost) *myPrice;

					if (transactionCostPrice < (zeroTransactionCostAskQuantity - askQuantity)
							 /*&& myPrice >= magnitude*/){
//					if (transactionCostPrice < (zeroTransactionCostAskQuantity - askQuantity)
//							&& (((lastSellPrice > 0.0) ? ((myPrice >= lastSellPrice) ? true : false): true ) ||
//									(StartSellQuantity > -1.0  ? ((StartSellQuantity <= askQuantity) ? true : false) : true  ))) {
						
						if (StartSellQuantity <= -1.0)
							StartSellQuantity = OpeningPosition;
						
						lastSellPrice = myPrice;
						trainingOpeningPosition = askQuantity;
						isPositionOpen = true;
						isActionTaken = true;
					}
				} else if (trainingEvents[i].type == Type.Downturn && isPositionOpen) {
					
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

					if (transactionCostPrice < (zeroTransactionCostBidQuantity - bidQuantity)
							/*&& myPrice <= magnitude*/){
	//				if (transactionCostPrice < (zeroTransactionCostBidQuantity - bidQuantity)
	//						&& (( lastBuyPrice > 0.0 ? ((myPrice <= lastBuyPrice ) ? true :false ): true )||
	//								(StartBuyQuantity > -1.0  ? ((StartBuyQuantity > bidQuantity) ? true: false) : true  ))) {
										
								if (StartBuyQuantity <= -1.0)
									StartBuyQuantity = OpeningPosition;
								
						lastBuyPrice = myPrice;
						trainingOpeningPosition = (trainingOpeningPosition - transactionCost) / myPrice;
						lastClosedPosition = trainingOpeningPosition;
						isPositionOpen = false;
						isActionTaken = true;
					}
				}

			}

		}

		if (isPositionOpen) {
			trainingOpeningPosition = lastClosedPosition;
		}

		return trainingOpeningPosition;

	}

	@Override
	void estimateTraining(PreProcess preprocess) {
		;
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

		return positionArrayBase.size();
	}

	@Override
	protected double getTransanction(int i) {
		if (i >= positionArrayBase.size())
			return 0.0;

		return positionArrayBase.get(i);
	}

	public double calculateSD() {
		return calculateBaseSD(positionArrayBase);
	}

	public double getMaxValue() {

		return getMaxValue(positionArrayBase);
	}

	public double getMinValue() {

		return getMinValue(positionArrayBase);
	}

	@Override
	public <E> void assignPerfectForesightRegressionModel(E[] inputArray) {
		// Not using regression model so ignore

	}
	
	public <E> void assignPerfectForesightMagnitudeRegressionModel(E[] inputArray) {
		downwardTrendMagnitudeTreeString = (String) inputArray[0];
		upwardTrendMagnitudeTreeString = (String) inputArray[1];
		
	}
}
