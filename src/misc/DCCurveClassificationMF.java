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

public class DCCurveClassificationMF extends DCCurveRegression {

	public DCCurveClassificationMF() {
		super();
		meanRatio = new double[2];
		meanRatio[0] = 0.0;
		meanRatio[1] = 0.0;
		
	}

	
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
		trainingOutputEvents =  Arrays.copyOf(trainingOutput, trainingOutput.length);

		if (meanRatio[0] > 0.0 && meanRatio[1] > 0.0)
			return;
		
		
		medianRatio[0] = 0.0;

		double meanDownturn = 0.0;
		double meanDownwardOvershoot = 0.0;

		int downturn = 0;
		// upward overshoots
		
		medianRatio[1] = 0.0;

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

					if (preprocess != null)
						classificationStr = preprocess.classifyTrainingInstance(i);
					else {
						System.out.println("DCCurveClassificationMF preprocessor is null");
						System.exit(-1);
					}

					// System.out.println("prediction
					// "+training.instance(eventCount).stringValue(training.attribute(training.numAttributes()
					// - 1)));
					// System.out.println("Classification : " +
					// classificationStr);

					// Use classification to select DC trend to train GP
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


					// Use classification to select DC trend to trade MF
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
			if (preprocess != null)
				classificationStr = preprocess.classifyTestInstance(outputIndex);

			if ((classificationStr.compareToIgnoreCase("no") == 0)) {
				;// System.out.println("no");
			} else {
				if (testEvents[outputIndex].type == Type.Upturn) {
					eval = testEvents[outputIndex].length() * meanRatio[1] ;
				} else
					eval = testEvents[outputIndex].length() *meanRatio[0];
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

	
		@Override
	public String getDCCurveName() {

		return "DCCurveClassificationMF";
	}

	public void estimateTrainingUsingOutputData(PreProcess preprocess) {
		trainingUsingOutputData = new double[trainingOutputEvents.length];
		
		
		for (int outputIndex = 0; outputIndex < trainingOutputEvents.length; outputIndex++) {
			
			double eval=0.0;
			String classificationStr = "no";

			if (preprocess != null)
				classificationStr = preprocess.classifyTrainingInstance(outputIndex);

			if ((classificationStr.compareToIgnoreCase("no") == 0)) {
				;// System.out.println("no");
			 
			} else {
				
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
		meanRatio[1] =  ((Double)  inputArray[1]).doubleValue();
		
	}
}
