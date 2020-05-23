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

public class DCCurveLengthMagnitudeMF  extends DCCurveRegression{

	public DCCurveLengthMagnitudeMF(){
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
	public void build(Double[] values, double delta, String GPTreeFileName, Event[] events, PreProcess preprocess) {

		String thresholdStr = String.format("%.8f", delta);
		thresholdString = thresholdStr;
		
		if ( events == null || events.length <1)
			return;
		
	
		trainingEvents =  Arrays.copyOf(events, events.length) ;
		
		if (meanRatio[0] > 0.0 && meanRatio[1] > 0.0)
			return;
	
		double meanDownturn = 0.0;
		double meanDownwardOvershoot = 0.0;

		int downturn = 0;
		// upward overshoots
		medianRatio[1] = 0.0;
		
		//downward overshoots
		medianRatio[0] = 0.0;

		double meanUpturn = 0.0;
		double meanUpwardOvershoot = 0.0;

		int upturn = 0;

		ArrayList<Double> downwardRatio = new ArrayList<Double>();
		ArrayList<Double> upwardRatio = new ArrayList<Double>();

		for (Event e : events) {
			// we ignore the first (artificial) event
			if (e.start > 0) {
				double ratio = 0.0;

				if (e.overshoot != null) {
					ratio = (e.overshoot.length() / (double) e.length());
				}

				if (e.type == Type.Upturn) {
					upturn++;
					meanRatio[1] += ratio;
					upwardRatio.add(ratio);

					meanUpturn += e.length();
					meanUpwardOvershoot += (e.overshoot != null) ? e.overshoot.length() : 0;
				} else if (e.type == Type.Downturn) {
					downturn++;
					meanRatio[0] += ratio;
					downwardRatio.add(ratio);

					meanDownturn += e.length();
					meanDownwardOvershoot += (e.overshoot != null) ? e.overshoot.length() : 0;

				}
			}
		}

		meanRatio[0] = (meanDownwardOvershoot / downturn) / (meanDownturn / downturn);
		meanRatio[1] = (meanUpwardOvershoot / upturn) / (meanUpturn / upturn);
		
	}

	public void testbuild(int lastTrainingPricePosition, Double[] values, double delta , Event[]  testEvents, PreProcess preprocess) {
		lastTrainingPrice = lastTrainingPricePosition;
		if (testEvents == null || testEvents.length <1)
			return;
		
		testingEvents =  Arrays.copyOf(testEvents, testEvents.length) ;
	}
	
	private String calculateRMSE_MF(Event[] trendEvent,double delta) {

		double rmse = 0.0;
		for (int eventCount = 0; eventCount < trendEvent.length; eventCount++) {
			int os = 0;

			
			if (trendEvent[eventCount].overshoot != null && trendEvent[eventCount].overshoot.end != trendEvent[eventCount].overshoot.start)
				os = trendEvent[eventCount].overshoot.length();

			if (trendEvent[eventCount].type == Type.Upturn) {
				rmse = rmse + ((os - (trendEvent[eventCount].length() * meanRatio[1]))
						* (os - (trendEvent[eventCount].length() * meanRatio[1])));
			} else if (trendEvent[eventCount].type == Type.Downturn) {
				rmse = rmse + ((os - (trendEvent[eventCount].length() * meanRatio[0]))
						* (os - (trendEvent[eventCount].length() * meanRatio[0])));
			} else {
				System.out.println("Unknown event type. exiting");
				System.exit(0);
			}
		}

		predictionRmse = Math.sqrt(rmse / (trendEvent.length - 1));
		
		// System.out.println(predictionRmse);
		return Double.toString(predictionRmse);
	}

////

	public String reportTestMF(Double[] values, double delta, String GPTreeFileName) {
		return calculateRMSE_MF(testingEvents, delta);

	}
	
	@Override
	String report(Double[] values, double delta, String GPTreeFileName) {
		return calculateRMSE_MF(testingEvents, delta);
	}

	@Override
	double trade(PreProcess preprocess) {
				
		return OpeningPosition;
	}
	
	
	
	double getMddPeak(){
		return simpleDrawDown.getPeak();
	}
	
	double getMddTrough(){
		return simpleDrawDown.getTrough();
	}
	
	double getMaxMddBase(){
		return simpleDrawDown.getMaxDrawDown();
	}
	
	double getMddPeakQuote(){
		return simpleDrawDownQuote.getPeak();
	}
	
	double getMddTroughQuote(){
		return simpleDrawDownQuote.getTrough();
	}
	
	double getMaxMddQuote(){
		return simpleDrawDownQuote.getMaxDrawDown();
	}
	
	int getNumberOfQuoteCcyTransactions(){
		
		return positionArrayQuote.size() - 1 ;
	}
	
	int getNumberOfBaseCcyTransactions(){
		
		return positionArrayBase.size() - 1 ;
	}

	double getBaseCCyProfit(){
		double profit = 0.00;
		ArrayList<Double> profitList = new ArrayList<Double>();
		if (positionArrayBase.size() == 1)
			return 0.00;
		for (int profitLossCount=1; profitLossCount < positionArrayBase.size() ; profitLossCount++  ){
			double profitCalculation =  positionArrayBase.get(profitLossCount) - positionArrayBase.get(profitLossCount-1)/ positionArrayBase.get(profitLossCount-1);
			profitList.add(profitCalculation);
		}
		profit = profitList.stream().mapToDouble(i -> i.doubleValue()).sum();
		return profit;
	}

	double getQuoteCCyProfit(){
		double profit = 0.00;
		ArrayList<Double> profitList = new ArrayList<Double>();
		if (positionArrayQuote.size() == 1)
			return 0.00;
		//Start from 3rd element because first element is zero 
		for (int profitLossCount=1; profitLossCount < positionArrayQuote.size() ; profitLossCount++  ){
			double profitCalculation =  positionArrayQuote.get(profitLossCount) - positionArrayQuote.get(profitLossCount-1)/ positionArrayBase.get(profitLossCount-1);
			profitList.add(profitCalculation);
		}
		profit = profitList.stream().mapToDouble(i -> i.doubleValue()).sum();
		return profit;
	}
	
	@Override
	public String getDCCurveName() {
		
		return "DCCurveLengthMagnitudeMF";
	}

	@Override
	double trainingTrading(PreProcess preprocess) {

		return trainingOpeningPosition;

	}
	
	@Override
	void estimateTraining( PreProcess preprocess){
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
		meanRatio[1] =  ((Double)  inputArray[1]).doubleValue();
		
	}
	


	public <E> void assignPerfectForesightMagnitudeRegressionModel(E[] inputArray) {
		downwardTrendMagnitudeTreeString = (String) inputArray[0];
		upwardTrendMagnitudeTreeString = (String) inputArray[1];
		
	}

}

