package misc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

import dc.ga.HelperClass;
import dc.ga.PreProcess;
import dc.ga.DCCurve.Event;
import dc.ga.DCCurve.Type;
import dc.io.FReader;
import dc.io.FReader.FileMember2;

public class DCCurvePerfectForesightMF extends DCCurveRegression {

	public DCCurvePerfectForesightMF() {
		super();
		meanRatio = new double[2];
		meanRatio[0] = 0.0;
		meanRatio[1] = 0.0;
		

		meanMagnitudeRatio =  new double[2];
		meanMagnitudeRatio[0] = 0.0;
		meanMagnitudeRatio[1] = 0.0;
		
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
	public void build(Double[] values, double delta, String GPTreeFileName, Event[] events,
			Event[] trainingOutput,PreProcess preprocess) {
		String thresholdStr = String.format("%.8f", delta);
		thresholdString = thresholdStr;
		
		if (events == null || events.length < 1)
			return;

		trainingEvents = Arrays.copyOf(events, events.length);
		this.trainingOutputEvents =  Arrays.copyOf(trainingOutput, trainingOutput.length);

		meanRatio[0] = 0.0;
		medianRatio[0] = 0.0;
		meanMagnitudeRatio[0] =0.0;
		double meanDownturn = 0.0;
		double meanDownwardOvershoot = 0.0;
		
		
		int downturn = 0;
		// upward overshoots
		meanRatio[1] = 0.0;
		medianRatio[1] = 0.0;
		meanMagnitudeRatio[1]= 0.0;
		double meanUpturn = 0.0;
		double meanUpwardOvershoot = 0.0;

		int upturn = 0;

		ArrayList<Double> downwardRatio = new ArrayList<Double>();
		ArrayList<Double> upwardRatio = new ArrayList<Double>();

		for (int i = 0; i < events.length - 1; i++) {
			// we ignore the first (artificial) event
			if (events[i].start > 0) {
				double ratio = 0.0;

				if (events[i].overshoot != null) {
					ratio = (events[i].overshoot.length() / (double) events[i].length());
				}

				if (events[i].type == Type.Upturn) {
					if (events[i].overshoot == null
							|| events[i].overshoot.end == events[i].overshoot.start)
						continue;
					
					upturn++;
					meanRatio[1] += ratio;
					upwardRatio.add(ratio);

					meanUpturn += events[i].length();
					meanUpwardOvershoot += (events[i].overshoot != null) ? events[i].overshoot.length() : 0;
				} else if (events[i].type == Type.Downturn) {
					if (events[i].overshoot == null
							|| events[i].overshoot.end == events[i].overshoot.start)
						continue;

					downturn++;
					meanRatio[0] += ratio;
					downwardRatio.add(ratio);

					meanDownturn += events[i].length();
					meanDownwardOvershoot += (events[i].overshoot != null) ? events[i].overshoot.length() : 0;

				}
			}
		}

		meanRatio[0] = (meanDownwardOvershoot / downturn) / (meanDownturn / downturn);
		meanRatio[1] = (meanUpwardOvershoot / upturn) / (meanUpturn / upturn);
		
		meanDownwardOvershoot= 0.0;
		downturn = 0;
		upturn = 0;
		meanUpwardOvershoot = 0.0;
		meanUpturn = 0.0;
		meanDownturn = 0.0;
		//os = Double.parseDouble(FReader.dataRecordInFileArray.get(.overshoot.end).askPrice);
		for (int i = 0; i < events.length - 1; i++) {
			// we ignore the first (artificial) event
			double osMagnitude = 0.0;
			double dcMagnitude = 0.0;
			
			if (events[i].type == Type.Upturn){
				if (events[i].overshoot != null && events[i].overshoot.end != events[i].overshoot.start) {
				osMagnitude =  Double.parseDouble(FReader.dataRecordInFileArray.get(events[i].overshoot.end).askPrice) - 
						Double.parseDouble(FReader.dataRecordInFileArray.get(events[i].overshoot.start).askPrice);
			
				}
				dcMagnitude  =  Double.parseDouble(FReader.dataRecordInFileArray.get(events[i].end).askPrice) - 
						Double.parseDouble(FReader.dataRecordInFileArray.get(events[i].start).askPrice);
				
			}
			else
			{
				if (events[i].overshoot != null && events[i].overshoot.end != events[i].overshoot.start) {
					osMagnitude =  Double.parseDouble(FReader.dataRecordInFileArray.get(events[i].overshoot.end).bidPrice) - 
							Double.parseDouble(FReader.dataRecordInFileArray.get(events[i].overshoot.start).bidPrice);
				
					}
					dcMagnitude  =  Double.parseDouble(FReader.dataRecordInFileArray.get(events[i].end).bidPrice) - 
							Double.parseDouble(FReader.dataRecordInFileArray.get(events[i].start).bidPrice);
					
			}
			
			if (events[i].start > 0) {
				double ratio = 0.0;

				if (events[i].overshoot != null) {
					ratio = (osMagnitude / (double) dcMagnitude);
				}

				if (events[i].type == Type.Upturn) {
					if (events[i].overshoot == null
							|| events[i].overshoot.end == events[i].overshoot.start)
						continue;
					
					upturn++;
					meanMagnitudeRatio[1] += ratio;
					upwardRatio.add(ratio);

					meanUpturn += dcMagnitude;
					meanUpwardOvershoot += (events[i].overshoot != null) ? osMagnitude : 0;
				} else if (events[i].type == Type.Downturn) {
					if (events[i].overshoot == null
							|| events[i].overshoot.end == events[i].overshoot.start)
						continue;

					downturn++;
					meanMagnitudeRatio[0] += ratio;
					downwardRatio.add(ratio);

					meanDownturn += dcMagnitude;
					meanDownwardOvershoot += (events[i].overshoot != null) ?osMagnitude : 0;

				}
			}
		}


		meanMagnitudeRatio[0] = (meanDownwardOvershoot / downturn) / (meanDownturn / downturn);
		meanMagnitudeRatio[1] = (meanUpwardOvershoot / upturn) / (meanUpturn / upturn);
		

	}

	public void testbuild(int lastTrainingPricePosition, Double[] values, double delta, Event[] testEvents,
			PreProcess preprocess) {
		lastTrainingPrice = lastTrainingPricePosition;
		if (testEvents == null || testEvents.length < 1)
			return;

		testingEvents = Arrays.copyOf(testEvents, testEvents.length);

		predictionWithClassifier = new double[testEvents.length];
		for (int outputIndex = 0; outputIndex < testEvents.length - 1; outputIndex++) {

			double eval = 0.0;

			

			if (testEvents[outputIndex].overshoot == null
					|| testEvents[outputIndex].overshoot.end == testEvents[outputIndex].overshoot.start){
				;// System.out.println("no");
			} else {
				if (testEvents[outputIndex].type == Type.Upturn) {
					eval = meanRatio[1];
				} else
					eval = meanRatio[0];
			}
			predictionWithClassifier[outputIndex] = eval;
		}

	}

	private String calculateRMSE_MF(Event[] trendEvent, double delta, double[] runPrediction) {

		double rmse = 0.0;
		for (int eventCount = 0; eventCount < trendEvent.length; eventCount++) {
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
			System.out.println("Invalid predictionRmsePerfectForsightMF: " + predictionRmse + " discarding ");
			predictionRmse = 10.0;
		}
		// System.out.println(predictionRmse);
		return Double.toString(predictionRmse);

	}

	////

	public String reportTestMF(Double[] values, double delta, String GPTreeFileName) {
		return calculateRMSE_MF(testingEvents, delta, predictionWithClassifier);

	}

	@Override
	String report(Double[] values, double delta, String GPTreeFileName) {
		return calculateRMSE_MF(testingEvents, delta, predictionWithClassifier);
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
		for (int i = 1; i < testingEvents.length - 1; i++) {
			int tradePoint = 0;
			if (testingEvents[i].type == Type.Upturn) {
				
				if (testingEvents[i].overshoot == null
						|| testingEvents[i].overshoot.end == testingEvents[i].overshoot.start){
					tradePoint = (int) (testingEvents[i].end);
				} else {

					tradePoint = (int) (testingEvents[i].end + (testingEvents[i].length() * meanRatio[1]));
				}
			} else if (testingEvents[i].type == Type.Downturn) {

				if (testingEvents[i].overshoot == null
						|| testingEvents[i].overshoot.end == testingEvents[i].overshoot.start){
					tradePoint = (int) (testingEvents[i].end);
				} else {
					tradePoint = (int) (testingEvents[i].end + (testingEvents[i].length() * meanRatio[0]));
				}

			}
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

			if (tradePoint > FReader.dataRecordInFileArray.size()
					|| (lastTrainingPrice - 1) + tradePoint > FReader.dataRecordInFileArray.size()) {
				System.out.println(" DCCurveClassificationMF: predicted datapoint "
						+ ((lastTrainingPrice - 1) + tradePoint) + " is beyond the size of price array  "
						+ FReader.dataRecordInFileArray.size() + " . Trading ended");
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
					
						if (transactionCostPrice < (zeroTransactionCostAskQuantity - askQuantity)){

							lastSellPrice = myPrice;
							OpeningPosition = askQuantity;
							isPositionOpen = true;
							positionArrayQuote.add(new Double(OpeningPosition));
							lastUpDCCend = Double.parseDouble(FReader.dataRecordInFileArray.get((lastTrainingPrice - 1) + testingEvents[i].end).bidPrice);
							
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
								&&  myPrice < lastUpDCCend){
	
									
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
					System.out.println(" DCCurvePerfectForesightMF: Search for element "
							+ ((lastTrainingPrice - 1) + tradePoint) + " is beyond the size of price array  "
							+ FReader.dataRecordInFileArray.size() + " . Trading ended");
					break;

				}
				catch (IndexOutOfBoundsException exception ){
					System.out.println(" DCCurvePerfectForesightMF: Search for element " + ((lastTrainingPrice - 1) + tradePoint)
							+ " is beyond the size of price array  " + 
							FReader.dataRecordInFileArray.size() + " . Trading ended") ;
					break;
				}
				catch (Exception exception ){
					System.out.println(" DCCurvePerfectForesightMF: Search for element " + ((lastTrainingPrice - 1) + tradePoint)
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

		return "DCCurvePerfectForesightMF";
	}

	@Override
	double trainingTrading(PreProcess preprocess) {
		boolean isPositionOpen = false;
		double myPrice = 0.0;
		double DD = 0;// DrawDown
		double lastClosedPosition = 0.0;
		double transactionCost = 0.025 / 100;
		lastSellPrice = 0.0;
		lastBuyPrice = 0.0;
		
		double lastUpDCCend = 0.0;
		for (int i = 1; i < trainingEvents.length; i++) {

			int tradePoint = 0;
			
			if (trainingEvents[i].overshoot == null
					|| trainingEvents[i].overshoot.end == trainingEvents[i].overshoot.start) {
				tradePoint = (int) (trainingEvents[i].end );// System.out.println("no");
			} else {
				if (trainingEvents[i].type == Type.Upturn)
					tradePoint = (int) (trainingEvents[i].end + (trainingEvents[i].length() * meanRatio[1]));
				else if (trainingEvents[i].type == Type.Downturn)
					tradePoint = (int) (trainingEvents[i].end + (trainingEvents[i].length() * meanRatio[0]));
			}
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

			FReader freader = new FReader();
			FileMember2 fileMember2 = freader.new FileMember2();

			if (tradePoint >= FReader.dataRecordInFileArray.size()) {
				continue;
			}

			// I am opening my position in base currency
			try {
			fileMember2 = FReader.dataRecordInFileArray.get(tradePoint);
			}
			catch (ArrayIndexOutOfBoundsException e){
				System.out.println(e.getMessage());
				continue;
			}
			

			if (trainingEvents[i].type == Type.Upturn && !isPositionOpen) {
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
			
				if (transactionCostPrice < (zeroTransactionCostAskQuantity - askQuantity)){
	
					
					lastSellPrice = myPrice;
					trainingOpeningPosition = askQuantity;
					isPositionOpen = true;
					lastUpDCCend = Double.parseDouble(FReader.dataRecordInFileArray.get(trainingEvents[i].end).bidPrice);
					
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
						&& myPrice < lastUpDCCend){

					lastBuyPrice = myPrice;
					trainingOpeningPosition =  (trainingOpeningPosition -transactionCost) /myPrice;
					lastClosedPosition = trainingOpeningPosition;
					isPositionOpen = false;
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
		throw new IllegalArgumentException("Perfect foresigt regression model cannot be set");
	}
}
