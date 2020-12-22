package misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

import dc.ga.HelperClass;
import dc.ga.PreProcess;
import dc.ga.DCCurve.Event;
import dc.ga.DCCurve.Type;
import dc.io.FReader;
import dc.io.FReader.FileMember2;

public class DCCurveOlsen extends DCCurveRegression {

	public DCCurveOlsen() {
		super();
		meanRatio = new double[1];
		meanRatio[0] = 2.0;

	}

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
		if (testEvents == null || testEvents.length < 1)
			return;

		testingEvents = Arrays.copyOf(testEvents, testEvents.length);
		predictionWithClassifier = new double[testEvents.length];
		for (int outputIndex = 0; outputIndex < testEvents.length - 1; outputIndex++) {

			double eval = 0.0;
			eval = testEvents[outputIndex].length() * 2;
			predictionWithClassifier[outputIndex] = eval;
		}


	}

	private String calculateRMSE_Olsen(Event[] trendEvent, double delta) {

		double rmse = 0.0;
		for (int eventCount = 0; eventCount < trendEvent.length; eventCount++) {
			int os = 0;

			if (trendEvent[eventCount].overshoot != null
					&& trendEvent[eventCount].overshoot.end != trendEvent[eventCount].overshoot.start)
				os = trendEvent[eventCount].overshoot.length();

			rmse = rmse + ((os - (trendEvent[eventCount].length() * 2)) * (os - (trendEvent[eventCount].length() * 2)));

		}

		predictionRmse = Math.sqrt(rmse / (trendEvent.length - 1));

		// System.out.println(predictionRmseOlsen);
		return Double.toString(predictionRmse);
	}

	public String reportTestOlsen(Double[] values, double delta, String GPTreeFileName) {
		return calculateRMSE_Olsen(testingEvents, delta);

	}

	@Override
	String report(Double[] values, double delta, String GPTreeFileName) {
		return calculateRMSE_Olsen(testingEvents, delta);
	}


	@Override
	public String getDCCurveName() {

		return "DCCurveOlsen";
	}

	public void estimateTrainingUsingOutputData(PreProcess preprocess) {
		trainingUsingOutputData = new double[trainingOutputEvents.length];
		
		
		for (int outputIndex = 0; outputIndex < trainingOutputEvents.length; outputIndex++) {
			trainingUsingOutputData[outputIndex] = trainingOutputEvents[outputIndex].length() * 2;;
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
		// TODO Auto-generated method stub
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
		meanRatio[0] =  ((Double) inputArray[0]).doubleValue();
	}
}
