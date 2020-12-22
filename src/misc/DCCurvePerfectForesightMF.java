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

public class DCCurvePerfectForesightMF extends DCCurveRegression {

	public DCCurvePerfectForesightMF() {
		super();
		meanRatio = new double[2];
		meanRatio[0] = 0.0;
		meanRatio[1] = 0.0;
		

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
	public void build(Double[] values, double delta, String GPTreeFileName, Event[] events,
			Event[] trainingOutput,PreProcess preprocess) {
		String thresholdStr = String.format("%.8f", delta);
		thresholdString = thresholdStr;
		
		if (events == null || events.length < 1)
			return;

		trainingEvents = Arrays.copyOf(events, events.length);
		this.trainingOutputEvents =  Arrays.copyOf(trainingOutput, trainingOutput.length);

		meanRatio[0] = 0.0;
		medianRatio[0] = 0.0;
		meanMagnitudeRatio[0] =0.0;
		double meanDownturn = 0.0;
		double meanDownwardOvershoot = 0.0;
		
		
		int downturn = 0;
		// upward overshoots
		meanRatio[1] = 0.0;
		medianRatio[1] = 0.0;
		meanMagnitudeRatio[1]= 0.0;
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
					if (events[i].overshoot == null
							|| events[i].overshoot.end == events[i].overshoot.start)
						continue;
					
					upturn++;
					meanRatio[1] += ratio;
					upwardRatio.add(ratio);

					meanUpturn += events[i].length();
					meanUpwardOvershoot += (events[i].overshoot != null) ? events[i].overshoot.length() : 0;
				} else if (events[i].type == Type.Downturn) {
					if (events[i].overshoot == null
							|| events[i].overshoot.end == events[i].overshoot.start)
						continue;

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
		
		meanDownwardOvershoot= 0.0;
		downturn = 0;
		upturn = 0;
		meanUpwardOvershoot = 0.0;
		meanUpturn = 0.0;
		meanDownturn = 0.0;
		//os = Double.parseDouble(FReader.dataRecordInFileArray.get(.overshoot.end).askPrice);
		for (int i = 0; i < events.length - 1; i++) {
			// we ignore the first (artificial) event
			double osMagnitude = 0.0;
			double dcMagnitude = 0.0;
			
			if (events[i].type == Type.Upturn){
				if (events[i].overshoot != null && events[i].overshoot.end != events[i].overshoot.start) {
				osMagnitude =  Double.parseDouble(FReader.dataRecordInFileArray.get(events[i].overshoot.end).askPrice) - 
						Double.parseDouble(FReader.dataRecordInFileArray.get(events[i].overshoot.start).askPrice);
			
				}
				dcMagnitude  =  Double.parseDouble(FReader.dataRecordInFileArray.get(events[i].end).askPrice) - 
						Double.parseDouble(FReader.dataRecordInFileArray.get(events[i].start).askPrice);
				
			}
			else
			{
				if (events[i].overshoot != null && events[i].overshoot.end != events[i].overshoot.start) {
					osMagnitude =  Double.parseDouble(FReader.dataRecordInFileArray.get(events[i].overshoot.end).bidPrice) - 
							Double.parseDouble(FReader.dataRecordInFileArray.get(events[i].overshoot.start).bidPrice);
				
					}
					dcMagnitude  =  Double.parseDouble(FReader.dataRecordInFileArray.get(events[i].end).bidPrice) - 
							Double.parseDouble(FReader.dataRecordInFileArray.get(events[i].start).bidPrice);
					
			}
			
			if (events[i].start > 0) {
				double ratio = 0.0;

				if (events[i].overshoot != null) {
					ratio = (osMagnitude / (double) dcMagnitude);
				}

				if (events[i].type == Type.Upturn) {
					if (events[i].overshoot == null
							|| events[i].overshoot.end == events[i].overshoot.start)
						continue;
					
					upturn++;
					meanMagnitudeRatio[1] += ratio;
					upwardRatio.add(ratio);

					meanUpturn += dcMagnitude;
					meanUpwardOvershoot += (events[i].overshoot != null) ? osMagnitude : 0;
				} else if (events[i].type == Type.Downturn) {
					if (events[i].overshoot == null
							|| events[i].overshoot.end == events[i].overshoot.start)
						continue;

					downturn++;
					meanMagnitudeRatio[0] += ratio;
					downwardRatio.add(ratio);

					meanDownturn += dcMagnitude;
					meanDownwardOvershoot += (events[i].overshoot != null) ?osMagnitude : 0;

				}
			}
		}


		meanMagnitudeRatio[0] = (meanDownwardOvershoot / downturn) / (meanDownturn / downturn);
		meanMagnitudeRatio[1] = (meanUpwardOvershoot / upturn) / (meanUpturn / upturn);
		

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

			

			if (testEvents[outputIndex].overshoot == null
					|| testEvents[outputIndex].overshoot.end == testEvents[outputIndex].overshoot.start){
				;// System.out.println("no");
			} else {
				if (testEvents[outputIndex].type == Type.Upturn) {
					eval = testEvents[outputIndex].length() * meanRatio[1];
				} else
					eval = testEvents[outputIndex].length() * meanRatio[0];
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
			System.out.println("Invalid predictionRmsePerfectForsightMF: " + predictionRmse + " discarding ");
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

		return "DCCurvePerfectForesightMF";
	}

	

	public void estimateTrainingUsingOutputData(PreProcess preprocess) {
		trainingUsingOutputData = new double[trainingOutputEvents.length];
		
		
		for (int outputIndex = 0; outputIndex < trainingOutputEvents.length; outputIndex++) {
			
		/*	trainingGpPredictionUsingOutputData[outputIndex] = HelperClass.estimateOSlength(outputIndex, trainingOutputEvents,
					 bestUpWardEventTree,  bestDownWardEventTree );*/
			
			double eval=0.0;
			
			if (trainingOutputEvents[outputIndex].overshoot == null
					|| trainingOutputEvents[outputIndex].overshoot.end == trainingOutputEvents[outputIndex].overshoot.start) {
				
				;	
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
		throw new IllegalArgumentException("Perfect foresigt regression model cannot be set");
	}
}
