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
import dc.ga.PreProcess;
import dc.ga.DCCurve.Event;
import dc.ga.DCCurve.Type;
import dc.ga.HelperClass;
import dc.io.FReader;
import dc.io.FReader.FileMember2;
import files.FWriter;

public class DCCurvePerfectForesight extends DCCurveRegression {

	public Orders[] orderArray;
	
	public DCCurvePerfectForesight() {
		
		super();
		Const.FUNCTION_NODE_DEFINITION = "NON_LINEAR";
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
	
	public void build(Double[] values, double delta, String GPTreeFileName, Event[] trainEvents,
			 Event[] trainingOutput,PreProcess preprocess) {
		String thresholdStr = String.format("%.8f", delta);
		thresholdString = thresholdStr;
		thresholdValue = delta;
		
		if (trainEvents == null || trainEvents.length < 1)
			return;

		this.trainingEvents = Arrays.copyOf(trainEvents, trainEvents.length);
		this.trainingOutputEvents =  Arrays.copyOf(trainingOutput, trainingOutput.length);
		
		if (bestDownWardEventTree != null && bestUpWardEventTree != null)
		{
			return;	
		
		}

		TreeHelperClass treeHelperClass = new TreeHelperClass();

		if (Const.splitDatasetByTrendType) {

			// get upward dc first

			String gpTreeName = Const.UPWARD_EVENT_STRING + thresholdStr + Const.FUNCTION_NODE_DEFINITION
					+ "_perfectForesight.txt";
			String thisLine = null;

			Vector<Event> trendOfChoiceVec = new Vector<Event>();

			for (int i = 0; i < trainEvents.length; i++) {
				if (trainEvents[i].type == Type.Upturn) {

					if (trainEvents[i].overshoot == null
							|| trainEvents[i].overshoot.end == trainEvents[i].overshoot.start)
						continue;

					trendOfChoiceVec.add(trainEvents[i]);

				}
			}

		//	System.out.println("upward event for classifier " + trendOfChoiceVec.size());
			Event[] uptrendEvent = trendOfChoiceVec.toArray(new Event[trendOfChoiceVec.size()]);
			if (Const.REUSE_EXISTING_TREE) {

				try {
					// open input stream test.txt for reading purpose.
					BufferedReader br = new BufferedReader(
							new FileReader(Const.log.publicFolder + gpTreeName));
					while ((thisLine = br.readLine()) != null) {
						Const.thresholdGPStringUpwardMap.put(delta, thisLine);
						// System.out.println(thisLine);
					}
				} catch (FileNotFoundException fileNotFound) {
					;
				} catch (IOException io) {
					;
				} catch (Exception e) {
					;
				}
			} else {
				FWriter writer = new FWriter(Const.log.publicFolder + gpTreeName);
			}

			if (Const.thresholdGPStringUpwardMap.containsKey(delta)) {
				gpTreeInFixNotation = Const.thresholdGPStringUpwardMap.get(delta);
				upwardTrendTreeString = gpTreeInFixNotation;

			} else {
				if (treeHelperClass.bestTreesInRuns != null)
					treeHelperClass.bestTreesInRuns.clear();
				
				treeHelperClass.getBestTreesForThreshold(uptrendEvent, Const.POP_SIZE, 1, Const.MAX_GENERATIONS,
						thresholdStr);

				if (treeHelperClass.bestTreesInRuns.isEmpty() || treeHelperClass.bestTreesInRuns.size() < 1) {
					System.out.println("treeHelperClass.bestTreesInRuns.isEmpty()");
					System.exit(-1);
				}
				// get best tree
				Comparator<AbstractNode> comparator = Collections.reverseOrder();
				Collections.sort(treeHelperClass.bestTreesInRuns, comparator);
				AbstractNode tree = treeHelperClass.bestTreesInRuns.get(treeHelperClass.bestTreesInRuns.size() - 1);
				String treeAsInfixNotationString = tree.printAsInFixFunction();

				bestUpWardEventTree = treeHelperClass.bestTreesInRuns.get(treeHelperClass.bestTreesInRuns.size() - 1);
				upwarddistPerf = bestUpWardEventTree.perfScore;
				upwardTrendTreeString = bestUpWardEventTree.printAsInFixFunction();
			//	System.out.println("Best tree up:" + SymbolicRegression.file_Name + "->" + tree.getPerfScore());
			//	System.out.println("Best tree structure" + treeAsInfixNotationString);

				curve_bestTreesInRunsUpward.setSize(treeHelperClass.bestTreesInRuns.size());
				Collections.copy(curve_bestTreesInRunsUpward, treeHelperClass.bestTreesInRuns);
				// SymbolicRegression.log.save(gpTreeName,
				// treeAsInfixNotationString);
				
				Const.VARIABLE_EVALUATED = 0;
				treeHelperClass.bestTreesInRuns.clear();
			}

			// Downward trend GP here
			thresholdStr = String.format("%.8f", delta);
			gpTreeName = Const.DOWNWARD_EVENT_STRING + thresholdStr + Const.FUNCTION_NODE_DEFINITION
					+ "_perfectForesight.txt";
			thisLine = null;
			trendOfChoiceVec.clear();

			for (int i = 0; i < trainEvents.length; i++) {
				if (trainEvents[i].type == Type.Downturn) {

					if (trainEvents[i].overshoot == null
							|| trainEvents[i].overshoot.end == trainEvents[i].overshoot.start)
						continue;

					trendOfChoiceVec.add(trainEvents[i]);
				}
			}

			Event[] downtrendEvent = trendOfChoiceVec.toArray(new Event[trendOfChoiceVec.size()]);

			if (Const.REUSE_EXISTING_TREE) {

				try {
					// open input stream test.txt for reading purpose.
					BufferedReader br = new BufferedReader(
							new FileReader(Const.log.publicFolder + gpTreeName));
					while ((thisLine = br.readLine()) != null) {
						Const.thresholdGPStringDownwardMap.put(delta, thisLine);
						// System.out.println(thisLine);
					}
				} catch (FileNotFoundException fileNotFound) {
					System.out.println(
							Const.log.publicFolder + gpTreeName + " not found. Will rebuild GP tree.");
					if (treeHelperClass.bestTreesInRuns != null)
						treeHelperClass.bestTreesInRuns.clear();

					// fileNotFound.printStackTrace();
				} catch (IOException io) {
					System.out.println("IO excption occured. Will loading" + Const.log.publicFolder
							+ gpTreeName + ". Will rebuild GP tree.");
					// io.printStackTrace();
				} catch (Exception e) {
					System.out.println("Unknown error occured. Will loading" + Const.log.publicFolder
							+ gpTreeName + ". Will rebuild GP tree.");
					// e.printStackTrace();
				}
			} else {
				FWriter writer = new FWriter(Const.log.publicFolder + gpTreeName);

			}

			if (Const.thresholdGPStringDownwardMap.containsKey(delta)) {
				gpTreeInFixNotation = Const.thresholdGPStringDownwardMap.get(delta);
				downwardTrendTreeString = gpTreeInFixNotation;
			} else {
				if (treeHelperClass.bestTreesInRuns != null)
					treeHelperClass.bestTreesInRuns.clear();
				
				
				treeHelperClass.getBestTreesForThreshold(downtrendEvent, Const.POP_SIZE, 1, Const.MAX_GENERATIONS,
						thresholdStr);

				if (treeHelperClass.bestTreesInRuns.isEmpty() || treeHelperClass.bestTreesInRuns.size() < 1) {
					System.out.println("treeHelperClass.bestTreesInRuns.isEmpty()");
					System.exit(-1);
				}

				// get best tree
				Comparator<AbstractNode> comparator = Collections.reverseOrder();
				Collections.sort(treeHelperClass.bestTreesInRuns, comparator);
				AbstractNode tree = treeHelperClass.bestTreesInRuns.get(treeHelperClass.bestTreesInRuns.size() - 1);
			
				bestDownWardEventTree = treeHelperClass.bestTreesInRuns.get(treeHelperClass.bestTreesInRuns.size() - 1);
				downarddistPerf = bestDownWardEventTree.perfScore;
				downwardTrendTreeString = bestDownWardEventTree.printAsInFixFunction();
		
				curve_bestTreesInRunsDownward.setSize(treeHelperClass.bestTreesInRuns.size());
				Collections.copy(curve_bestTreesInRunsDownward, treeHelperClass.bestTreesInRuns);
				
				treeHelperClass.bestTreesInRuns.clear();
				
				Const.VARIABLE_EVALUATED = 0;
					
			}

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
		String thresholdStr = String.format("%.8f", delta);

		gpprediction = new double[testEvents.length];
		for (int outputIndex = 1; outputIndex < testEvents.length; outputIndex++) {
			String foo = "";
			if (Const.splitDatasetByTrendType) {
				if (testEvents[outputIndex].type == Type.Upturn) {
					foo = upwardTrendTreeString;
					numberOfUpwardEvent++;
					isUpwardEvent = true;
				} else if (testEvents[outputIndex].type == Type.Downturn) {
					foo = downwardTrendTreeString;
					numberOfDownwardEvent++;
					isUpwardEvent = false;
				} else {
					System.out.println("DCCurvePErfectForesight - DCCurvePErfectForesight - Invalid event");
					continue;
				}
			} else {
				foo = trendTreeString;
			}

			foo = foo.replace("X0", Integer.toString(testEvents[outputIndex].length()));
			foo = foo.replace("X1", Double.toString(testEvents[outputIndex].high - testEvents[outputIndex].low) );
			double eval = 0.0;
			Double javascriptValue = Double.MAX_VALUE;
			if (testEvents[outputIndex].overshoot == null
					|| testEvents[outputIndex].overshoot.end == testEvents[outputIndex].overshoot.start) {
				; // eval = Double.valueOf(testEvents[outputIndex].length());
			} else {
				
				if (testEvents[outputIndex].type == Type.Upturn) {
					eval = testEvents[outputIndex].length() * bestUpWardEventTree.eval(testEvents[outputIndex].length());
				} else if (testEvents[outputIndex].type == Type.Downturn) {
					eval = testEvents[outputIndex].length() * bestDownWardEventTree.eval(testEvents[outputIndex].length());
				}
			}
			
			gpprediction[outputIndex] = eval;
			predictionWithClassifier[outputIndex] = eval;
		}
		System.out.println("Test");
	}

	//Done for GA_new
	public void estimateTrainingUnderPerfectforesight() {
		

		if (trainingOutputEvents == null || trainingOutputEvents.length < 1){
			System.out.println("trainingOutput is empty... existing....." );
			System.exit(-1);
		}
		
		

		gpprediction = new double[trainingOutputEvents.length];
		for (int outputIndex = 1; outputIndex < trainingOutputEvents.length; outputIndex++) {
			String foo = "";
			if (Const.splitDatasetByTrendType) {
				if (trainingOutputEvents[outputIndex].type == Type.Upturn) {
					foo = upwardTrendTreeString;
					numberOfUpwardEvent++;
					isUpwardEvent = true;
				} else if (trainingOutputEvents[outputIndex].type == Type.Downturn) {
					foo = downwardTrendTreeString;
					numberOfDownwardEvent++;
					isUpwardEvent = false;
				} else {
					System.out.println("DCCurvePErfectForesight - DCCurvePErfectForesight - Invalid event");
					continue;
				}
			} else {
				foo = trendTreeString;
			}

			foo = foo.replace("X0", Integer.toString(trainingOutputEvents[outputIndex].length()));
			foo = foo.replace("X1", Double.toString(trainingOutputEvents[outputIndex].high - trainingOutputEvents[outputIndex].low) );
			double eval = 0.0;
			Double javascriptValue = Double.MAX_VALUE;
			if (trainingOutputEvents[outputIndex].overshoot == null
					|| trainingOutputEvents[outputIndex].overshoot.end == trainingOutputEvents[outputIndex].overshoot.start) {
				; // eval = Double.valueOf(testEvents[outputIndex].length());
			} else {
				
				if (trainingOutputEvents[outputIndex].type == Type.Upturn) {
					eval = trainingOutputEvents[outputIndex].length() *  bestUpWardEventTree.eval(trainingOutputEvents[outputIndex].length());
				} else if (trainingOutputEvents[outputIndex].type == Type.Downturn) {
					eval = trainingOutputEvents[outputIndex].length() * bestDownWardEventTree.eval(trainingOutputEvents[outputIndex].length());
				}
			}
			
			gpprediction[outputIndex] = eval;

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
			if (trendEvent[eventCount].overshoot != null && trendEvent[eventCount].overshoot.length() > 0) {
				os = trendEvent[eventCount].overshoot.length();
				// System.out.println("DC:" +
				// trendEvent[eventCount].overshoot.length() + " OS:" + os + "
				// prediction:" + runPrediction[eventCount]);
			}
			// else
			// System.out.println("DC: 0 OS:" + os + " prediction:" +
			// runPrediction[eventCount]);

			double prediction = runPrediction[eventCount];

			// wrapper is 0

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

		return "DCCurvePerfetForesight";
	}


	@Override
	public void estimateTrainingUsingOutputData(PreProcess preprocess) {
		trainingUsingOutputData = new double[trainingOutputEvents.length];
		if (preprocess != null){
			preprocess.loadTrainingData(null);
		}
		
		orderArray = new Orders[trainingOutputEvents.length];
		for (int outputIndex = 0; outputIndex < trainingOutputEvents.length; outputIndex++) {
			Orders gpOrder = new Orders() ;
		/*	trainingGpPredictionUsingOutputData[outputIndex] = HelperClass.estimateOSlength(outputIndex, trainingOutputEvents,
					 bestUpWardEventTree,  bestDownWardEventTree );*/
			
			double eval=0.0;
			
			
			if (preprocess == null){
				if (trainingOutputEvents[outputIndex].overshoot == null
						|| trainingOutputEvents[outputIndex].overshoot.end == trainingOutputEvents[outputIndex].overshoot.start) {
					
					gpOrder.isOSevent= false;
					if (trainingOutputEvents[outputIndex].type == Type.Upturn) {
						gpOrder.eventType =  Type.Upturn;
						gpOrder.isSell = true;
						gpOrder.isOSevent= false;
						gpOrder.eventConfirmationPoint = trainingOutputEvents[outputIndex].end;
					}
					else{
						gpOrder.eventType =  Type.Downturn;
						gpOrder.isSell = false;
						gpOrder.isOSevent= false;
						gpOrder.eventConfirmationPoint = trainingOutputEvents[outputIndex].end;
					}
					
				} else {
					if (trainingOutputEvents[outputIndex].type == Type.Upturn) {
						
						eval = bestUpWardEventTree.eval(trainingOutputEvents[outputIndex].length());
						gpOrder.isSell = true;
						gpOrder.isOSevent= true;
						gpOrder.eventType =  Type.Upturn;
						gpOrder.eventConfirmationPoint = trainingOutputEvents[outputIndex].end;
						gpOrder.prediction =  eval;
					} else if (trainingOutputEvents[outputIndex].type == Type.Downturn) {
						eval = bestDownWardEventTree.eval(trainingOutputEvents[outputIndex].length());
						gpOrder.isSell = false;
						gpOrder.isOSevent= true;
						gpOrder.eventType =  Type.Downturn;
						gpOrder.eventConfirmationPoint = trainingOutputEvents[outputIndex].end;
						gpOrder.prediction =  eval;
					}
					
				}
			}
			else
			{
				String classificationStr = "no";

				if (preprocess != null)
					classificationStr = preprocess.classifyTrainingInstance(outputIndex);

				if ((classificationStr.compareToIgnoreCase("no") == 0)) {
					
					gpOrder.isOSevent= false;
					if (trainingOutputEvents[outputIndex].type == Type.Upturn) {
						gpOrder.eventType =  Type.Upturn;
						gpOrder.isSell = true;
						gpOrder.isOSevent= false;
						gpOrder.eventConfirmationPoint = trainingOutputEvents[outputIndex].end;
					}
					else{
						gpOrder.eventType =  Type.Downturn;
						gpOrder.isSell = false;
						gpOrder.isOSevent= false;
						gpOrder.eventConfirmationPoint = trainingOutputEvents[outputIndex].end;
					}
					
				} else {
					if (trainingOutputEvents[outputIndex].type == Type.Upturn) {
						
						eval = bestUpWardEventTree.eval(trainingOutputEvents[outputIndex].length());
						gpOrder.isSell = true;
						gpOrder.isOSevent= true;
						gpOrder.eventType =  Type.Upturn;
						gpOrder.eventConfirmationPoint = trainingOutputEvents[outputIndex].end;
						gpOrder.prediction =  eval;
					} else if (trainingOutputEvents[outputIndex].type == Type.Downturn) {
						eval = bestDownWardEventTree.eval(trainingOutputEvents[outputIndex].length());
						gpOrder.isSell = false;
						gpOrder.isOSevent= true;
						gpOrder.eventType =  Type.Downturn;
						gpOrder.eventConfirmationPoint = trainingOutputEvents[outputIndex].end;
						gpOrder.prediction =  eval;
					}
					
				}
			}

			
			gpOrder.threshold = thresholdValue;
			gpOrder.dcEventStart =  trainingOutputEvents[outputIndex].start;
			gpOrder.dcEventEnd =  trainingOutputEvents[outputIndex].end;
			if (trainingOutputEvents[outputIndex].overshoot != null){
				if (trainingOutputEvents[outputIndex].overshoot.start == -1 ||trainingOutputEvents[outputIndex].overshoot.end == -1 )
				{	System.out.println("I am here");
				trainingOutputEvents[outputIndex].overshoot.start=0;
				trainingOutputEvents[outputIndex].overshoot.end =0;
				}
				gpOrder.dcOsEventStart =  trainingOutputEvents[outputIndex].overshoot.start;
				gpOrder.dcOsEventEnd =  trainingOutputEvents[outputIndex].overshoot.end;
			}
			
			orderArray[outputIndex] =  gpOrder;
			trainingUsingOutputData[outputIndex] = eval;
		}

	}

	@Override
	public void estimateTraining(PreProcess preprocess) {
		trainingGpPrediction = new double[trainingEvents.length];
		

		for (int outputIndex = 0; outputIndex < trainingEvents.length - 2; outputIndex++) {
			
			trainingGpPrediction[outputIndex] = HelperClass.estimateOSlength(outputIndex, trainingEvents,
					 bestUpWardEventTree,  bestDownWardEventTree );
			
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
		throw new IllegalArgumentException("Perfect foresight regression model cannot be set");
	}
	
	
	public double getCombinedRegressionError(){
		double rtnCode = Double.MAX_VALUE ;
		rtnCode = (Math.sqrt((downarddistPerf *downarddistPerf) + (upwarddistPerf*upwarddistPerf) +
				(downwardMagnitudePerf * downwardMagnitudePerf) + 
				(upwardMagnitudePerf * upwardMagnitudePerf)))/4.0;
		
		
		return rtnCode;
	}

}
