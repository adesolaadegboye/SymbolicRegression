package misc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Random;

import dc.ga.HelperClass;
import dc.ga.PreProcess;
import dc.ga.DCCurve.Event;
import dc.ga.DCCurve.Type;
import dc.io.FReader;
import dc.io.FReader.FileMember2;

public class DCCurveRandomMF extends DCCurveRegression {

	public DCCurveRandomMF() {
		super();
		meanRatio = new double[2];
		meanRatio[0] = 0.0;
		meanRatio[1] = 0.0;
	}

	/**
	 * 0 = downward overshoot 1 = upward overshoot
	 */
	double[] meanRatio = new double[2];

	/**
	 * 0 = downward overshoot 1 = upward overshoot
	 */
	double[] medianRatio = new double[2];

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

		if (meanRatio[0] > 0.0 && meanRatio[1] > 0.0)
			return;
		
		double meanDownturn = 0.0;
		double meanDownwardOvershoot = 0.0;

		int downturn = 0;
		
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

					String classificationStr = "no";

					Random randomno = new Random();
					double decisionNum =  randomno.nextDouble();
					if (decisionNum >= 0.5)
						classificationStr = "yes";
					
					

					// Use decision from Random()
					if ((classificationStr.compareToIgnoreCase("no") == 0)) {
						continue;
					}

					upturn++;
					meanRatio[1] += ratio;
					upwardRatio.add(ratio);

					meanUpturn += events[i].length();
					meanUpwardOvershoot += (events[i].overshoot != null) ? events[i].overshoot.length() : 0;
				} else if (events[i].type == Type.Downturn) {

					String classificationStr = "no";

					if (preprocess != null)
						classificationStr = preprocess.classifyTrainingInstance(i);

					// System.out.println("prediction
					// "+training.instance(eventCount).stringValue(training.attribute(training.numAttributes()
					// - 1)));
					// System.out.println("Classification : " +
					// classificationStr);

					// Use classification to select DC trend to train GP
					if ((classificationStr.compareToIgnoreCase("no") == 0)) {
						continue;
					}

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

			String classificationStr = "no";
			Random randomno = new Random();
			double decisionNum =  randomno.nextDouble();
			if (decisionNum >= 0.5)
				classificationStr = "yes";
			
			

			// Use decision from Random()
			if ((classificationStr.compareToIgnoreCase("no") == 0)) {
				;// System.out.println("no");
			} else {
				if (testEvents[outputIndex].type == Type.Upturn) {
					eval =  testEvents[outputIndex].length() * meanRatio[1];
				} else
					eval =  testEvents[outputIndex].length() *meanRatio[0];
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
				predictionRmse = 10.00;
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

	public String reportTestMF(Double[] values, double delta, String GPTreeFileName) {
		return calculateRMSE_MF(testingEvents, delta, predictionWithClassifier);

	}

	@Override
	String report(Double[] values, double delta, String GPTreeFileName) {
		return calculateRMSE_MF(testingEvents, delta, predictionWithClassifier);
	}
/*
	@Override
	public double trade(PreProcess preprocess) {
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
				String classificationStr = "no";
				Random randomno = new Random();
				double decisionNum =  randomno.nextDouble();
				if (decisionNum >= 0.5)
					classificationStr = "yes";
				
				// Use decision from Random()
				if ((classificationStr.compareToIgnoreCase("no") == 0)) {
					tradePoint = (int) (testingEvents[i].end);
				} else {

					tradePoint = (int) (testingEvents[i].end + (testingEvents[i].length() * meanRatio[1]));
				}
			} else if (testingEvents[i].type == Type.Downturn) {

				String classificationStr = "no";
				Random randomno = new Random();
				double decisionNum =  randomno.nextDouble();
				if (decisionNum >= 0.5)
					classificationStr = "yes";
				
				

				// Use decision from Random()
				if ((classificationStr.compareToIgnoreCase("no") == 0)) {
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
			// fileMember2.Day = GPtestEvents[i].endDate;
			// fileMember2.time = GPtestEvents[i].endTime;
			// fileMember2.price = GPtestEvents[i].endPrice;

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
						// transactionCost = trainingOpeningPosition *
						// (0.025/100);
						// trainingOpeningPosition = (trainingOpeningPosition
						// -transactionCost) *myPrice;

						if (transactionCostPrice < (zeroTransactionCostAskQuantity - askQuantity)){
//						
							
							lastSellPrice = myPrice;
							OpeningPosition = askQuantity;
							isPositionOpen = true;
							positionArrayQuote.add(new Double(OpeningPosition));

							tradedPrice.add(new Double(myPrice));
							anticipatedTrendMap.put(testingEvents[i].start, tradePoint);
							anticipatedTrend.add(anticipatedTrendMap);
							lastUpDCCend = Double.parseDouble(FReader.dataRecordInFileArray.get((lastTrainingPrice - 1) + testingEvents[i].end).bidPrice);
							
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
					System.out.println(" DCCurveRandomMF: Search for element "
							+ ((lastTrainingPrice - 1) + tradePoint) + " is beyond the size of price array  "
							+ FReader.dataRecordInFileArray.size() + " . Trading ended");
					break;

				}
				catch (IndexOutOfBoundsException exception ){
					System.out.println(" DCCurveRandomMF: Search for element " + ((lastTrainingPrice - 1) + tradePoint)
							+ " is beyond the size of price array  " + 
							FReader.dataRecordInFileArray.size() + " . Trading ended") ;
					break;
				}
				catch (Exception exception ){
					System.out.println(" DCCurveRandomMF: Search for element " + ((lastTrainingPrice - 1) + tradePoint)
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
*/
	
	@Override
	public String getDCCurveName() {

		return "DCCurveRandomMF";
	}

	public void estimateTrainingUsingOutputData(PreProcess preprocess) {
		trainingUsingOutputData = new double[trainingOutputEvents.length];
		
		
		for (int outputIndex = 0; outputIndex < trainingOutputEvents.length; outputIndex++) {
			
			double eval=0.0;
			String classificationStr = "no";
			Random randomno = new Random();
			double decisionNum =  randomno.nextDouble();
			if (decisionNum >= 0.5)
				classificationStr = "yes";


			if ((classificationStr.compareToIgnoreCase("no") == 0)) {
				;// System.out.println("no");
			 
			}else {
				
				if (trainingOutputEvents[outputIndex].type == Type.Upturn) {
					eval = trainingOutputEvents[outputIndex].length() * meanRatio[1];
				} else
					eval = trainingOutputEvents[outputIndex].length() * meanRatio[0];
			}

			trainingUsingOutputData[outputIndex] = eval;
		}

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
		meanRatio[0] =  ((Double) inputArray[0]).doubleValue();
		meanRatio[1] =  ((Double) inputArray[1]).doubleValue();
		
	}
}

