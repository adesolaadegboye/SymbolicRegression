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

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import dc.GP.AbstractNode;
import dc.GP.Const;
import dc.GP.TreeHelperClass;
import dc.ga.GA;
import dc.ga.PreProcess;
import dc.io.FReader;
import dc.io.FReader.FileMember2;
import dc.ga.DCCurve.Event;
import dc.ga.DCCurve.Type;
import files.FWriter;

public class DCCurveLengthMagnitudeTOSDDCUDC extends DCCurveRegression {

	public DCCurveLengthMagnitudeTOSDDCUDC() {
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
	public void build(Double[] values, double delta, String GPTreeFileName, Event[] trainingEvents,
			PreProcess preprocess) {

		String thresholdStr = String.format("%.8f", delta);
		thresholdString = thresholdStr;
		
		if (trainingEvents == null || trainingEvents.length < 1)
			return;

		this.trainingEvents = Arrays.copyOf(trainingEvents, trainingEvents.length);
		
		
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
		lastTrainingPrice = lastTrainingPricePosition;
		if (testEvents == null || testEvents.length < 1)
			return;

		testingEvents = Arrays.copyOf(testEvents, testEvents.length);
		String thresholdStr = String.format("%.8f", delta);

		ScriptEngineManager mgr = new ScriptEngineManager();
		ScriptEngine engine = mgr.getEngineByName("JavaScript");

		predictionWithClassifier = new double[testEvents.length];
		predictionMagnitudeWithClassifier = new double[testEvents.length];

		for (int outputIndex = 0; outputIndex < testEvents.length - 1; outputIndex++) {
			String foo = "";
			String foo2 = "";
			if (Const.splitDatasetByTrendType) {
				if (testEvents[outputIndex].type == Type.Upturn) {
					foo = upwardTrendTreeString;
					foo2 = upwardTrendMagnitudeTreeString;
					foo2 = foo2.replace("X0", FReader.dataRecordInFileArray.get(testEvents[outputIndex].end).askPrice);

				} else if (testEvents[outputIndex].type == Type.Downturn) {
					foo = downwardTrendTreeString;
					foo2 = downwardTrendMagnitudeTreeString;
					foo2 = foo2.replace("X0", FReader.dataRecordInFileArray.get(testEvents[outputIndex].end).bidPrice);

				} else {
					System.out.println("Invalid event");
					System.exit(0);
				}
			} else {
				foo = trendTreeString;
			}

			foo = foo.replace("X0", Integer.toString(testEvents[outputIndex].length()));
			double eval = 0.0;
			double eval2 = 0.0;
			

			//if ((classificationStr.compareToIgnoreCase("no") == 0)) {
				//;// System.out.println("no");
			//} else {
			
				Double javascriptValue = Double.MAX_VALUE;
				try {
					javascriptValue = (Double) engine.eval(foo);
					eval = javascriptValue.doubleValue();
				} catch (ScriptException e) {
					e.printStackTrace();
				} catch (ClassCastException e) {
					e.printStackTrace();
				}
				javascriptValue = Double.MAX_VALUE;
				
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
				bd = new BigDecimal(eval);
				bd2 = new BigDecimal(Double.toString(eval));
			} catch (NumberFormatException e) {
				Integer integerObject = new Integer(testEvents[outputIndex].length());
				eval = integerObject.doubleValue() * (double) GA.NEGATIVE_EXPRESSION_REPLACEMENT;
			}
			
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
		//	}

			predictionWithClassifier[outputIndex] = eval;
			predictionMagnitudeWithClassifier[outputIndex] = eval2;
		}
	}

	private String calculateRMSEClassifier(Event[] trendEvent, double delta, double[] runPrediction) {
		
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

	public String reportTestClassifier(Double[] values, double delta, String GPTreeFileName) {
		return calculateRMSEClassifier(testingEvents, delta, predictionWithClassifier);

	}

	@Override
	String report(Double[] values, double delta, String GPTreeFileName) {
		return calculateRMSEClassifier(testingEvents, delta, predictionWithClassifier);

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

		return "DCCurveClassification";
	}

	@Override
	double trainingTrading(PreProcess preprocess) {
		

		return trainingOpeningPosition;

	}

	@Override
	void estimateTraining(PreProcess preprocess) {
		trainingGpPrediction = new double[trainingEvents.length];
		trainingGpMagnitudePrediction = new double[trainingEvents.length];
		ScriptEngineManager mgr = new ScriptEngineManager();
		ScriptEngine engine = mgr.getEngineByName("JavaScript");

		for (int outputIndex = 0; outputIndex < trainingEvents.length; outputIndex++) {
			String foo = "";
			String foo2 =  "";
			if (Const.splitDatasetByTrendType) {
				if (trainingEvents[outputIndex].type == Type.Upturn) {
					foo = upwardTrendTreeString;

				} else if (trainingEvents[outputIndex].type == Type.Downturn) {
					foo = downwardTrendTreeString;

				} else {
					System.out.println("Invalid event");
					System.exit(0);
				}
			} else {
				foo = trendTreeString;
			}

			foo = foo.replace("X0", Integer.toString(trainingEvents[outputIndex].length()));
			foo2 = foo.replace("X0", Double.toString(Math.abs(trainingEvents[outputIndex].high - trainingEvents[outputIndex].low) ));
			double eval = 0.0;
			double eval2 = 0.0;
			
			
			
				Double javascriptValue = Double.MAX_VALUE;
				try {
					javascriptValue = (double) engine.eval(foo);
					eval = javascriptValue.doubleValue();
				} catch (ScriptException e) {
					e.printStackTrace();
				} catch (ClassCastException e) {
					e.printStackTrace();
				}
				
				Double javascriptValue2 = Double.MAX_VALUE;
				try {
					javascriptValue2= (double) engine.eval(foo2);
					eval2 = javascriptValue2.doubleValue();
				} catch (ScriptException e) {
					e.printStackTrace();
				} catch (ClassCastException e) {
					e.printStackTrace();
				}
			

			BigDecimal bd = null;
			BigDecimal bd2 = null;
			try {
				bd = new BigDecimal(eval);
				bd2 = new BigDecimal(Double.toString(eval));
			} catch (NumberFormatException e) {
				Integer integerObject = new Integer(trainingEvents[outputIndex].length());
				eval = integerObject.doubleValue() * (double) GA.NEGATIVE_EXPRESSION_REPLACEMENT;
			}
			
			try {
				bd = new BigDecimal(eval);
				bd2 = new BigDecimal(Double.toString(eval2));
			} catch (NumberFormatException e) {
				Double doubleObject = new Double(Math.abs(trainingEvents[outputIndex].high - trainingEvents[outputIndex].low));
				eval2 = doubleObject.doubleValue();
			}

			trainingGpPrediction[outputIndex] = eval;
			trainingGpMagnitudePrediction[outputIndex] = eval2;
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
	

	public <E> void assignPerfectForesightMagnitudeRegressionModel(E[] inputArray) {
		downwardTrendMagnitudeTreeString = (String) inputArray[0];
		upwardTrendMagnitudeTreeString = (String) inputArray[1];
		
	}
}

