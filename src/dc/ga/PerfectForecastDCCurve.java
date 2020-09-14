
package dc.ga;

import java.io.BufferedReader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;


import dc.MyException;
import dc.GP.AbstractNode;
import dc.GP.Const;
import dc.GP.TreeHelperClass;
import dc.ga.DCCurve.Event;
import dc.ga.DCCurve.Type;
import dc.io.FReader;
import dc.io.FReader.FileMember2;
import files.FWriter;
import weka.core.Instance;

/**
 * DC (Directional Change) event detector.
 * 
 * <p>
 * <b>Notes:</b>
 * </p>
 * <ul>
 * <li>Overshoot events are only detected when a Upturn/Downturn event is
 * detected;</li>
 * </ul>
 * 
 * @author Fernando Esteban Barril Otero
 */
public class PerfectForecastDCCurve extends DCCurve {	
	
	AbstractNode gptree = null;
	
	
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
			Event[] trainOutput, PreProcess preprocess) {

		if (trainEvents == null || trainEvents.length < 1)
			return;

		trainingEvents = Arrays.copyOf(trainEvents, trainEvents.length);
		trainingOutputEvents = Arrays.copyOf(trainOutput, trainOutput.length);
		//TreeHelperClass.treeStructurePostcreateObj = treeStructurePostcreate.ePrune;
		
		if (Const.OsFunctionEnum == Const.function_code.eGP)
			buildGPRatio( values, delta,  GPTreeFileName, trainEvents, preprocess);
		else if (Const.OsFunctionEnum == Const.function_code.eMichaelFernando)
			build( values, delta  );
		else if (Const.OsFunctionEnum == Const.function_code.eOlsen){
			build( values, delta  );
			meanRatio[0]=2.0;
			meanRatio[1]=2.0;
			
		}
			
	}

	
	void buildGPRatio(Double[] values, double delta, String GPTreeFileName, Event[] trainEvents,
			PreProcess preprocess){

		super.build(values, delta );
		String thresholdStr = String.format("%.8f", delta);
		TreeHelperClass treeHelperClass = new TreeHelperClass();

		// get upward dc first

		String gpTreeName = Const.UPWARD_EVENT_STRING + thresholdStr + "NON_LINEAR"
				+ "_perfectForesight.txt";
		String thisLine = null;

		Vector<Event> trendOfChoiceVec = new Vector<Event>();

		for (int i = 0; i < trainEvents.length; i++) {
			if (trainEvents[i].type == Type.Upturn) {

				if (trainEvents[i].overshoot == null || 
						trainEvents[i].overshoot.end == trainEvents[i].overshoot.start)
					continue;

				trendOfChoiceVec.add(trainEvents[i]);

			}
		}

		//System.out.println("upward event for classifier " + trendOfChoiceVec.size());
		Event[] uptrendEvent = trendOfChoiceVec.toArray(new Event[trendOfChoiceVec.size()]);
		if (Const.REUSE_EXISTING_TREE) {

			try {
				// open input stream test.txt for reading purpose.
				BufferedReader br = new BufferedReader(
						new FileReader(GA_new.log.publicFolder + gpTreeName));
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
			FWriter writer = new FWriter(GA_new.log.publicFolder + gpTreeName);
		}

		if (Const.thresholdGPStringUpwardMap.containsKey(delta)) {
			gpTreeInFixNotation = Const.thresholdGPStringUpwardMap.get(delta);
			upwardTrendTreeString = gpTreeInFixNotation;

		} else {
			if (treeHelperClass.bestTreesInRuns != null)
				treeHelperClass.bestTreesInRuns.clear();
			
			
			//TODO Adesola make this configurable 
			System.out.println("Evolving upward event GP for Threshold" + thresholdStr);
			treeHelperClass.getBestTreesForThreshold(uptrendEvent, Const.POP_SIZE, 1, Const.MAX_GP_GENERATIONS,
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
			upwardTrendTreeString = bestUpWardEventTree.printAsInFixFunction();
			//System.out.println("Best tree up: ->" + tree.getPerfScore());
			//System.out.println("Best tree structure" + treeAsInfixNotationString);

			curve_bestTreesInRunsUpward.setSize(treeHelperClass.bestTreesInRuns.size());
			Collections.copy(curve_bestTreesInRunsUpward, treeHelperClass.bestTreesInRuns);
		}

		// Downward trend GP here
		thresholdStr = String.format("%.8f", delta);
		gpTreeName = Const.DOWNWARD_EVENT_STRING + thresholdStr +  "NON_LINEAR"
				+ ".txt";
		thisLine = null;
		trendOfChoiceVec.clear();

		for (int i = 0; i < trainEvents.length; i++) {
			if (trainEvents[i].type == Type.Downturn) {

				if (trainEvents[i].overshoot == null || 
						trainEvents[i].overshoot.end == trainEvents[i].overshoot.start)
					continue;

				trendOfChoiceVec.add(trainEvents[i]);
			}
		}

		Event[] downtrendEvent = trendOfChoiceVec.toArray(new Event[trendOfChoiceVec.size()]);

		if (Const.REUSE_EXISTING_TREE) {

			try {
				// open input stream test.txt for reading purpose.
				BufferedReader br = new BufferedReader(
						new FileReader(GA_new.log.publicFolder + gpTreeName));
				while ((thisLine = br.readLine()) != null) {
					GA_new.thresholdGPStringDownwardMap.put(delta, thisLine);
					// System.out.println(thisLine);
				}
			} catch (FileNotFoundException fileNotFound) {
				System.out.println(
						GA_new.log.publicFolder + gpTreeName + " not found. Will rebuild GP tree.");
				if (treeHelperClass.bestTreesInRuns != null)
					treeHelperClass.bestTreesInRuns.clear();

				// fileNotFound.printStackTrace();
			} catch (IOException io) {
				System.out.println("IO excption occured. Will loading" + GA_new.log.publicFolder
						+ gpTreeName + ". Will rebuild GP tree.");
				// io.printStackTrace();
			} catch (Exception e) {
				System.out.println("Unknown error occured. Will loading" + GA_new.log.publicFolder
						+ gpTreeName + ". Will rebuild GP tree.");
				// e.printStackTrace();
			}
		} else {
			FWriter writer = new FWriter(GA_new.log.publicFolder + gpTreeName);

		}

		if (GA_new.thresholdGPStringDownwardMap.containsKey(delta)) {
			gpTreeInFixNotation = Const.thresholdGPStringDownwardMap.get(delta);
			downwardTrendTreeString = gpTreeInFixNotation;
		} else {
			if (treeHelperClass.bestTreesInRuns != null)
				treeHelperClass.bestTreesInRuns.clear();
			// SymbolicRegression.log.save("DownLoadTesting.txt",
			// "test");
			//TODO Adesola make this configurable
			//treeHelperClass.getBestTreesForThreshold(downtrendEvent, Const.POP_SIZE, 1, Const.MAX_GENERATIONS,thresholdStr);
			System.out.println("Evolving downward event GP for Threshold" + thresholdStr);
			treeHelperClass.getBestTreesForThreshold(downtrendEvent, Const.POP_SIZE, 1, Const.MAX_GP_GENERATIONS,
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
			// System.out.println("Best tree" + tree.getPerfScore() + "
			// worst tree"+
			// treeHelperClass.bestTreesInRuns.get(0).getPerfScore()) ;
			downwardTrendTreeString = bestDownWardEventTree.printAsInFixFunction();
			//System.out.println("Best tree down:" + SymbolicRegression.file_Name + "->" + tree.getPerfScore());
			//System.out.println("Best tree structure" + treeAsInfixNotationString);

			curve_bestTreesInRunsDownward.setSize(treeHelperClass.bestTreesInRuns.size());
			Collections.copy(curve_bestTreesInRunsDownward, treeHelperClass.bestTreesInRuns);
			// SymbolicRegression.log.save(gpTreeName,
			// treeAsInfixNotationString);
		}

	}
	@Override
	double trainingTrading(PreProcess preprocess) {
		boolean isPositionOpen = false;
		double myPrice = 0.0;
		double lastClosedPosition = trainingOpeningPosition;
		double transactionCost = 0.025 / 100;
		double DD = 0;// DrawDown
		
		double lastPurchasebid = 0.0;

		for (int i = 1; i < trainingOutputEvents.length; i++) {

			int tradePoint = 0;
			double eval = 0.0;

			if (trainingOutputEvents[i].overshoot == null
					|| trainingOutputEvents[i].overshoot.end == trainingOutputEvents[i].overshoot.start) {
				;
			} else {
				
				if (trainingOutputEvents[i].type == Type.Upturn) {
					
					eval = bestUpWardEventTree.eval(trainingOutputEvents[i].length());
				} else if (trainingOutputEvents[i].type == Type.Downturn) {
					eval = bestDownWardEventTree.eval(trainingOutputEvents[i].length());

				} else {
					System.out.println("Invalid event");
					System.exit(0);
				}
				
			}

			Double dcPt = new Double(eval);
			Double zeroOs = new Double(0.0);

			if (trainingOutputEvents[i] == null)
				continue;

			if (dcPt.equals(zeroOs)) 
				tradePoint = trainingOutputEvents[i].end;
			else
				tradePoint = trainingOutputEvents[i].end + (int) Math.ceil(eval);

			if (i + 1 > trainingOutputEvents.length - 1)
				continue;

			if (trainingOutputEvents[i + 1] == null)
				continue;

			Event ev = getNextDirectionaChangeEndPoint(trainingOutputEvents,i);
			if (ev == null)
				continue ;
			
			if (tradePoint > ev.end) // If a new DC is
															// encountered
															// before the
															// estimation point
															// skip trading
				continue;

			FReader freader = new FReader();
			FileMember2 fileMember2 = freader.new FileMember2();

			if (tradePoint > FReader.dataRecordInFileArray.size()) {
				continue;
			}

			// I am opening my position in base currency
			try {
			fileMember2 = FReader.dataRecordInFileArray.get(tradePoint);
			}
			catch (ArrayIndexOutOfBoundsException e){
				System.out.println(e.getMessage());
				continue;
			}
			if (trainingOutputEvents[i].type == Type.Upturn && !isPositionOpen) {
				// Now position is in quote currency
				// I sell base currency in bid price
				double askQuantity = trainingOpeningPosition;
				double zeroTransactionCostAskQuantity = trainingOpeningPosition;
				double transactionCostPrice = 0.0;
				myPrice = Double.parseDouble(fileMember2.askPrice);
				
				
				transactionCost = askQuantity * (0.025/100);
				transactionCostPrice = transactionCost * myPrice;
				askQuantity =  (askQuantity -transactionCost) *myPrice;
				zeroTransactionCostAskQuantity = zeroTransactionCostAskQuantity *myPrice;
				//transactionCost = trainingOpeningPosition * (0.025/100);
				//trainingOpeningPosition =  (trainingOpeningPosition -transactionCost) *myPrice;
				
				if (transactionCostPrice <= (zeroTransactionCostAskQuantity - askQuantity) ){
					trainingOpeningPosition = askQuantity;

					lastPurchasebid = Double.parseDouble(FReader.dataRecordInFileArray.get( trainingOutputEvents[i].end).bidPrice);
					
					 
					isPositionOpen = true;
				}
			} else if (trainingOutputEvents[i].type == Type.Downturn && isPositionOpen ) {
				// Now position is in base currency
				// I buy base currency
				double bidQuantity = trainingOpeningPosition;
				double zeroTransactionCostBidQuantity = trainingOpeningPosition;
				double transactionCostPrice = 0.0;
				myPrice = Double.parseDouble(fileMember2.bidPrice);
				
				
				transactionCost = bidQuantity * (0.025/100);
				transactionCostPrice = transactionCost * myPrice;
				bidQuantity =  (bidQuantity -transactionCost) *myPrice;
				zeroTransactionCostBidQuantity = zeroTransactionCostBidQuantity *myPrice;
				//transactionCost = trainingOpeningPosition * (0.025/100);
				//trainingOpeningPosition =  (trainingOpeningPosition -transactionCost) /myPrice;
				
				
				if (transactionCostPrice < (zeroTransactionCostBidQuantity - bidQuantity)
						&& myPrice < lastPurchasebid){
					trainingOpeningPosition =  (trainingOpeningPosition -transactionCost) /myPrice;
					lastClosedPosition = trainingOpeningPosition;
					isPositionOpen = false;
				}
			}

		}

		if (isPositionOpen) {
			trainingOpeningPosition = lastClosedPosition;
		}

		return trainingOpeningPosition;

	}

	@Override
	public
	void estimateTraining(PreProcess preprocess) {
		trainingPrediction = new double[trainingEvents.length];
		
		for (int outputIndex = 0; outputIndex < trainingEvents.length - 2; outputIndex++) {
			String foo = "";
			if (Const.OsFunctionEnum == Const.function_code.eGP){
				if (trainingEvents[outputIndex].type == Type.Upturn) {
					foo = upwardTrendTreeString;
					numberOfUpwardEvent++;
					isUpwardEvent = true;
				} else if (trainingEvents[outputIndex].type == Type.Downturn) {
					foo = downwardTrendTreeString;
					numberOfDownwardEvent++;
					isUpwardEvent = false;
				} else {
					System.out.println("Invalid event");
					System.exit(0);
				}


				foo = foo.replace("X0", Integer.toString(trainingEvents[outputIndex].length()));
				double eval = 0.0;
	
				if (trainingEvents[outputIndex].overshoot == null
						|| trainingEvents[outputIndex].overshoot.end == trainingEvents[outputIndex].overshoot.start) {
					;
				} else {
					
					if (trainingEvents[outputIndex].type == Type.Upturn) {
						
						eval = bestUpWardEventTree.eval(trainingEvents[outputIndex].length());
					} else if (trainingEvents[outputIndex].type == Type.Downturn) {
						eval = bestDownWardEventTree.eval(trainingEvents[outputIndex].length());

					} else {
						System.out.println("Invalid event");
						System.exit(0);
					}
					
				}
	

				trainingPrediction[outputIndex] = eval;
			}
			else if (Const.OsFunctionEnum == Const.function_code.eMichaelFernando){
				if (trainingEvents[outputIndex].type == Type.Upturn) {
					trainingPrediction[outputIndex] =  meanRatio[1];
					isUpwardEvent = true;
				}
				else if (trainingEvents[outputIndex].type == Type.Downturn) {
					trainingPrediction[outputIndex] =  meanRatio[0];
					isUpwardEvent = false;
				} else {
					System.out.println("Invalid event");
					System.exit(0);
				}
			}
			else if (Const.OsFunctionEnum == Const.function_code.eOlsen ){
				if (trainingEvents[outputIndex].type == Type.Upturn) {
					trainingPrediction[outputIndex] = meanRatio[1];
					isUpwardEvent = true;
				}
				else if (trainingEvents[outputIndex].type == Type.Downturn) {
					trainingPrediction[outputIndex] =   meanRatio[0];
					isUpwardEvent = false;
				} else {
					System.out.println("Invalid event");
					System.exit(0);
				}
			}
			else{
				System.out.println("Invalid Regression algorithmType");
				System.exit(0);
			}
		}
		
		

	}

	public AbstractNode getUpwardDCTree(){
		
		return bestUpWardEventTree;
	}
	
	public AbstractNode getDownwardDCTree(){
		
		return bestDownWardEventTree;
	}
	
	
	public String getUpwardDCTreeString(){
		
		return upwardTrendTreeString;
	}
	
	public String getDownwardDCTreeString(){
		
		return downwardTrendTreeString;
	}

	// Run only against test data
	@Override
	protected void estimateTestRMSE() {
		
		double rmse = 0.0;
		 Vector<Double> predictedLengthVector = new Vector<Double>(); 
		 Vector<Double> actualOSLengthVector = new Vector<Double>();
		 
		 /**
		  * Using events instead of output here because the predictions is per event */
		for (int outputIndex = 0; outputIndex < events.length; outputIndex++) {
			String foo = "";
			double  prediction= 0.0;
			Double clsLabel = 0.0;
			String classificationStr  = "yes";
		
				
				if (Const.OsFunctionEnum == Const.function_code.eGP){

					 if (preprocess != null && events.length != preprocess.testInstances.size()){
							System.out.println("Unable to print RMSE result. Event and test instance does not match");
						}
					 
					
					
					Double eval = Double.MAX_VALUE;
					
					if ( events[outputIndex].type == Type.Upturn){
						eval = bestUpWardEventTree.eval(events[outputIndex].length());
					}
					else
						eval = bestDownWardEventTree.eval(events[outputIndex].length());
					
					if (preprocess != null && getTestInstance().get(outputIndex) != null) {

						try {
							clsLabel = preprocess.autoWEKAClassifier
									.classifyInstance(getTestInstance().get(outputIndex));
						} catch (Exception e) {
							
							e.printStackTrace();
						}
						classificationStr = (clsLabel.toString().compareToIgnoreCase("0.0") == 0) ? "yes" : "no";
					}

						
					if ((classificationStr.compareToIgnoreCase("no") == 0)){
						predictedLengthVector.add(prediction);
					} else{
						
								
							if  (  eval == Double.MAX_VALUE || eval == Double.NEGATIVE_INFINITY ||
									eval == Double.POSITIVE_INFINITY || eval ==  Double.NaN ||
									Double.compare(eval, 0.0)  < 0  || Double.isInfinite(eval)  || Double.isNaN(eval)){
								eval = ((Integer ) events[outputIndex].length()).doubleValue();
							}
							
							predictedLengthVector.add(eval);
					} 
					if (events[outputIndex].overshoot == null ){
						actualOSLengthVector.add(0.0);
					}
					else
						actualOSLengthVector.add((double) events[outputIndex].overshoot.length());

						
				}
				else
				{
					if (events[outputIndex].overshoot == null ||
							events[outputIndex].overshoot.start == events[outputIndex].overshoot.end ) {
						predictedLengthVector.add(0.0);
						actualOSLengthVector.add(0.0);
					} else{
						if ( events[outputIndex].type == Type.Upturn){
						predictedLengthVector.add(new Double(meanRatio[1]));
						actualOSLengthVector.add(new Double(events[outputIndex].overshoot.length()));		
						}
						else{
							predictedLengthVector.add(new Double(meanRatio[0]));
							actualOSLengthVector.add(new Double(events[outputIndex].overshoot.length()));
						}
					}	
				}
				
			
		}
		if (predictedLengthVector.size() != actualOSLengthVector.size()){
			System.out.println(" Regresson cannot be calculated -  exiting");
			System.exit(-1);
		}
		for (int index = 0; index < predictedLengthVector.size(); index++) {
			double prediction = predictedLengthVector.get(index);
			double os = actualOSLengthVector.get(index);

			if (Double.isNaN(prediction))
				System.out.println("Is Nan");
			
			double computed = (os - prediction);
				if (Double.isInfinite(computed))
					System.out.println("Is infinity");
				
				if (Double.isNaN(computed))
					System.out.println("Is Nan");
			
			rmse = rmse + ( computed* computed);
			
			//if (Double.isInfinite(rmse))
			//	System.out.println(rmse);
		}
		rmseResult = Math.sqrt(rmse / (predictedLengthVector.size()));
		predictedLengthVector.clear();
		actualOSLengthVector.clear();

			//System.out.println(downwardRMSE);
		
	}
	
public Event getNextDirectionaChangeEndPoint(Event[] output, int pointInTime){
		
		Event nextEvent = null;
		Event currentEvent =  output[pointInTime];
		
		for (int i = pointInTime+1; i < output.length; i++ ){
			nextEvent = output[i] ;
			if (nextEvent.type !=currentEvent.type )
				break;	
		}
		
		if (nextEvent ==  null || nextEvent.type ==currentEvent.type  )
			return null;
		
		return nextEvent;
	}
	
}