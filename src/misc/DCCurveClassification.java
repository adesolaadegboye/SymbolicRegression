package misc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import dc.GP.Const;
import dc.ga.DCCurve.Event;
import dc.ga.DCCurve.Type;
import dc.ga.HelperClass;
import dc.ga.PreProcess;
import dc.io.FReader;




public class DCCurveClassification extends DCCurveRegression {
	
	public Orders[] orderArray;
	public DCCurveClassification() {
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
			Event[] trainingOutput,PreProcess preprocess) {

		String thresholdStr = String.format("%.8f", delta);
		thresholdString = thresholdStr;
		thresholdValue = delta;
		if (trainingEvents == null || trainingEvents.length < 1)
			return;

		this.trainingEvents = Arrays.copyOf(trainingEvents, trainingEvents.length);
		this.trainingOutputEvents =  Arrays.copyOf(trainingOutput, trainingOutput.length);
		
		

		if (bestDownWardEventTree != null && bestUpWardEventTree != null ){
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
		lastTrainingPrice = lastTrainingPricePosition;
		if (testEvents == null || testEvents.length < 1)
			return;

		testingEvents = Arrays.copyOf(testEvents, testEvents.length);
		predictionWithClassifier = new double[testEvents.length];
		orderArray =  new Orders[testEvents.length];
		

		
		for (int outputIndex = 0; outputIndex < testEvents.length; outputIndex++) {
			String foo = "";
			double eval = 0.0;
			
			Orders gpOrder = new Orders() ;
			String classificationStr = "no";
			if (preprocess != null)
				classificationStr = preprocess.classifyTestInstance(outputIndex);

			gpOrder.eventConfirmationPoint = testingEvents[outputIndex].end;
			if ((classificationStr.compareToIgnoreCase("no") == 0)) {
				;// System.out.println("no");
				gpOrder.isOSevent= false;
				if (testingEvents[outputIndex].type == Type.Upturn) {
					gpOrder.eventType =  Type.Upturn;
					gpOrder.isSell = true;
				}
				else{
					gpOrder.eventType =  Type.Downturn;
					gpOrder.isSell = false;
				}
			} else {
				if (testEvents[outputIndex].type == Type.Upturn) {
					
					eval =  bestUpWardEventTree.eval(testEvents[outputIndex].length());
					if (Double.compare(eval, 0.0) < 0)
						eval = 0.0;
						
					gpOrder.isSell = true;
					gpOrder.isOSevent= true;
					gpOrder.prediction =  eval;
					gpOrder.eventType =  Type.Upturn;
				} else  {
					eval =  bestDownWardEventTree.eval(testEvents[outputIndex].length());
					if (Double.compare(eval, 0.0) < 0)
						eval = 0.0;
						
						
					gpOrder.isSell = false;
					gpOrder.isOSevent= true;
					gpOrder.prediction =  eval;
					gpOrder.eventType =  Type.Downturn;
				
				}
			}
			gpOrder.dcEventStart =  testingEvents[outputIndex].start;
			gpOrder.dcEventEnd =  testingEvents[outputIndex].end;
			gpOrder.threshold = thresholdValue;
			if (testingEvents[outputIndex].overshoot != null){
				if (testingEvents[outputIndex].overshoot.start == -1 ||testingEvents[outputIndex].overshoot.end == -1 )
				{	System.out.println("I am here");
					testingEvents[outputIndex].overshoot.start=0;
					testingEvents[outputIndex].overshoot.end =0;
				}
				gpOrder.dcOsEventStart =  testingEvents[outputIndex].overshoot.start;
				gpOrder.dcOsEventEnd =  testingEvents[outputIndex].overshoot.end;
			}
			orderArray[outputIndex] = gpOrder;
			
			predictionWithClassifier[outputIndex] = eval;
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
	public String getDCCurveName() {

		return "DCCurveClassification";
	}

	@Override
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
						eval = bestUpWardEventTree.eval(trainingOutputEvents[outputIndex].length());
					} else if (trainingEvents[outputIndex].type == Type.Downturn) {
						eval = bestDownWardEventTree.eval(trainingOutputEvents[outputIndex].length());
					}
			}

			trainingUsingOutputData[outputIndex] = eval;
		}

	}
	
	@Override
	public
	void estimateTraining(PreProcess preprocess) {
		trainingGpPrediction = new double[trainingEvents.length];
		

		for (int outputIndex = 0; outputIndex < trainingEvents.length; outputIndex++) {
			String foo = "";
			
			double eval = 0.0;

			String classificationStr = "no";

			if (preprocess != null)
				classificationStr = preprocess.classifyTrainingInstance(outputIndex);

			if ((classificationStr.compareToIgnoreCase("no") == 0)) {
				;// System.out.println("no");
			} else {
				if (trainingEvents[outputIndex].type == Type.Upturn) {
					eval = bestUpWardEventTree.eval(trainingEvents[outputIndex].length());
				} else if (trainingEvents[outputIndex].type == Type.Downturn) {
					eval = bestDownWardEventTree.eval(trainingEvents[outputIndex].length());
				}
			}

			BigDecimal bd = null;
			BigDecimal bd2 = null;
			try {
				bd = new BigDecimal(eval);
				bd2 = new BigDecimal(Double.toString(eval));
			} catch (NumberFormatException e) {
				Integer integerObject = new Integer(trainingEvents[outputIndex].length());
				eval = integerObject.doubleValue() * (double) Const.NEGATIVE_EXPRESSION_REPLACEMENT;
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

