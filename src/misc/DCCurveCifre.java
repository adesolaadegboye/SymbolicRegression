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

		predictionWithClassifier = new double[testEvents.length];

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
				
				eval = testEvents[outputIndex].length() * bestUpWardEventTree.eval(testingEvents[outputIndex].length());
			} else if (testingEvents[outputIndex].type == Type.Downturn) {
				eval = testEvents[outputIndex].length()* bestDownWardEventTree.eval(testingEvents[outputIndex].length());

			} else {
				System.out.println("DCCurveCifre - DCCurveCifre - Invalid event");
				continue;
			}
			gpprediction[outputIndex] = eval;
			predictionWithClassifier[outputIndex] = eval;

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
	public String getDCCurveName() {
		return "DCCurveCifre";
	}

	@Override
	

	public void estimateTrainingUsingOutputData(PreProcess preprocess) {
		trainingUsingOutputData = new double[trainingOutputEvents.length];
		
		
		for (int outputIndex = 0; outputIndex < trainingOutputEvents.length; outputIndex++) {
			
		
			double eval=0.0;
			if (trainingEvents[outputIndex].type == Type.Upturn) {
				eval = bestUpWardEventTree.eval(trainingEvents[outputIndex].length());
			} else if (trainingEvents[outputIndex].type == Type.Downturn) {
				eval = bestDownWardEventTree.eval(trainingEvents[outputIndex].length());
			}		
			trainingUsingOutputData[outputIndex] = eval;
		}

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
