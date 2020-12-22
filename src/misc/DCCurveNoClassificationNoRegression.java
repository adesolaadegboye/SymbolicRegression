package misc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

import dc.ga.PreProcess;
import dc.ga.DCCurve.Event;
import dc.ga.DCCurve.Type;
import dc.io.FReader;
import dc.io.FReader.FileMember2;

public class DCCurveNoClassificationNoRegression extends DCCurveRegression {

	public DCCurveNoClassificationNoRegression() {
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
		
		if ( events == null || events.length <1)
			return;
		
	
		trainingEvents =  Arrays.copyOf(events, events.length) ;
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

	private String calculateRMSE_NoClassficationNoRegression(Event[] trendEvent, double delta, double[] runPrediction) {

		return Double.toString(Double.MAX_VALUE);

	}

	////

	public String reportTest_NoClassficationNoRegression(Double[] values, double delta, String GPTreeFileName) {
		return calculateRMSE_NoClassficationNoRegression(testingEvents, delta, predictionWithClassifier);

	}

	@Override
	String report(Double[] values, double delta, String GPTreeFileName) {
		return calculateRMSE_NoClassficationNoRegression(testingEvents, delta, predictionWithClassifier);
	}


	double getMddPeak() {
		return simpleDrawDown.getPeak();
	}

	double getMddTrough() {
		return simpleDrawDown.getTrough();
	}

	
	@Override
	public String getDCCurveName() {

		return "DCCurveMF";
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
	
	
	
	
	public  double getMaxValue(){
		  
		  return getMaxValue(positionArrayBase);
		}
	public   double getMinValue(){
	 
	  return   getMinValue(positionArrayBase);
	}

	@Override
	public <E> void assignPerfectForesightRegressionModel(E[] inputArray) {
		// Can ignore because not using regression model here
		
	}

}
