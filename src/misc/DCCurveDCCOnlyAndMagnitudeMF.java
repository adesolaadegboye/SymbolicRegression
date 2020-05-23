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

public class DCCurveDCCOnlyAndMagnitudeMF extends DCCurveRegression {

	public DCCurveDCCOnlyAndMagnitudeMF() {
		super();
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
		if (testEvents == null || testEvents.length <1)
			return;
		
		testingEvents =  Arrays.copyOf(testEvents, testEvents.length) ;

	}

	private String calculateRMSE_DCCOnlyAndMagnitude(Event[] trendEvent, double delta, double[] runPrediction) {
		// System.out.println(predictionRmse);
		return Double.toString(Double.MAX_VALUE);

	}

	////

	public String reportTestMF(Double[] values, double delta, String GPTreeFileName) {
		return calculateRMSE_DCCOnlyAndMagnitude(testingEvents, delta, predictionWithClassifier);

	}

	@Override
	String report(Double[] values, double delta, String GPTreeFileName) {
		return calculateRMSE_DCCOnlyAndMagnitude(testingEvents, delta, predictionWithClassifier);
	}

	@Override
	double trade(PreProcess preprocess) {
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
		
		return profit;
	}

	double getQuoteCCyProfit() {
		double profit = 0.00;
		
		return profit;
	}

	@Override
	public String getDCCurveName() {

		return "DCCurveOnlyAndMagnitudeMF";
	}

	@Override
	double trainingTrading(PreProcess preprocess) {
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
		meanMagnitudeRatio[0] =  ((Double) inputArray[0]).doubleValue();
		meanMagnitudeRatio[1] =  ((Double)  inputArray[1]).doubleValue();
		
		
	}
}
