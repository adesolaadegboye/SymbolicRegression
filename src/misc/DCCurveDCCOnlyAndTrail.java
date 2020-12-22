package misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

import dc.ga.PreProcess;
import dc.ga.DCCurve.Event;
import dc.ga.DCCurve.Type;
import dc.io.FReader;
import dc.io.FReader.FileMember2;

public class DCCurveDCCOnlyAndTrail extends DCCurveRegression {

	public DCCurveDCCOnlyAndTrail() {
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
	public void build(Double[] values, double delta, String GPTreeFileName, Event[] events, 
			Event[] trainingOutput,PreProcess preprocess) {
		String thresholdStr = String.format("%.8f", delta);
		thresholdString = thresholdStr;

		if (events == null || events.length < 1)
			return;

		trainingEvents = Arrays.copyOf(events, events.length);
		this.trainingOutputEvents =  Arrays.copyOf(trainingOutput, trainingOutput.length);

	}

	public void testbuild(int lastTrainingPricePosition, Double[] values, double delta, Event[] testEvents,
			PreProcess preprocess) {
		lastTrainingPrice = lastTrainingPricePosition;
		if (testEvents == null || testEvents.length < 1)
			return;

		testingEvents = Arrays.copyOf(testEvents, testEvents.length);

		predictionWithClassifier = new double[testEvents.length];
		for (int outputIndex = 0; outputIndex < testEvents.length - 1; outputIndex++) {

			predictionWithClassifier[outputIndex] = 0.0;
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
	public double trade(PreProcess preprocess) {
		boolean isPositionOpen = false;
		double myPrice = 0.0;
		double transactionCost = 0.025 / 100;
		simpleDrawDown.Calculate(OpeningPosition);
		simpleSharpeRatio.addReturn(0);
		lastSellPrice = 0.0;
		lastBuyPrice = 0.0;
		double lastUpDCCend = 0.0;
		
		for (int i = 1; i < trainingOutputEvents.length - 1; i++) {
			int tradePoint = trainingOutputEvents[i].end;

			if (trainingOutputEvents[i] == null)
				continue;

			if (i + 1 > trainingOutputEvents.length - 1)
				continue;

			if (trainingOutputEvents[i + 1] == null)
				continue;

			if (tradePoint > trainingOutputEvents[i + 1].end) // If a new DC is
															// encountered
															// before the
															// estimation point
															// skip trading
				continue;

			FReader freader = new FReader();
			FileMember2 fileMember2 = freader.new FileMember2();

			boolean isActionTaken = false;

			for (int tradePointCointer = tradePoint; tradePointCointer < trainingOutputEvents[i
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
					//	LinkedHashMap<Integer, Integer> anticipatedTrendMap = new LinkedHashMap<Integer, Integer>();
					//	LinkedHashMap<Integer, Integer> actualTrendMap = new LinkedHashMap<Integer, Integer>();

						if (trainingOutputEvents[i].type == Type.Upturn && !isPositionOpen) {
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
							//	positionArrayQuote.add(new Double(OpeningPosition));
								lastUpDCCend = Double.parseDouble(FReader.dataRecordInFileArray.get((lastTrainingPrice - 1) + testingEvents[i].end).bidPrice);
								
								tradedPrice.add(new Double(myPrice));
							//	anticipatedTrendMap.put(trainingOutputEvents[i].start, tradePointCointer);
							//	anticipatedTrend.add(anticipatedTrendMap);

							//	if (trainingOutputEvents[i].overshoot == null || trainingOutputEvents[i].overshoot.length() < 1)
							//		actualTrendMap.put(trainingOutputEvents[i].start, trainingOutputEvents[i].end);
							//	else
							//		actualTrendMap.put(trainingOutputEvents[i].start, trainingOutputEvents[i].overshoot.end);

							//	actualTrend.add(actualTrendMap);
								isActionTaken = true;
							}

						} else if (trainingOutputEvents[i].type == Type.Downturn && isPositionOpen) {
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
							//	anticipatedTrendMap.put(trainingOutputEvents[i].start, tradePointCointer);
							//	anticipatedTrend.add(anticipatedTrendMap);

							//	if (trainingOutputEvents[i].overshoot == null || trainingOutputEvents[i].overshoot.length() < 1)
							//		actualTrendMap.put(trainingOutputEvents[i].start, trainingOutputEvents[i].end);
							//	else
							//		actualTrendMap.put(trainingOutputEvents[i].start, trainingOutputEvents[i].overshoot.end);

							//	actualTrend.add(actualTrendMap);
								isActionTaken = true;
							}
						}
					} catch (ArrayIndexOutOfBoundsException exception) {
						System.out.println(" DCCurveDCCOnlyAndTrail: Search for element "
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
			//anticipatedTrend.remove(anticipatedTrend.size() - 1);
			//actualTrend.remove(actualTrend.size() - 1);
			OpeningPosition = positionArrayBase.get(positionArrayBase.size() - 1);
			//positionArrayQuote.remove(positionArrayQuote.size() - 1);
			isPositionOpen = false;
		}

		otherTradeCalculations();
		return OpeningPosition;
	}


	@Override
	public String getDCCurveName() {

		return "DCCurveClassificationOnlyAndTrail";
	}

	@Override
	public double trainingTrading(PreProcess preprocess) {
		

		return -Double.MAX_VALUE;

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
}
