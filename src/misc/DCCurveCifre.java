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

import dc.ga.GA;
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
			PreProcess preprocess) {

		String thresholdStr = String.format("%.8f", delta);
		thresholdString = thresholdStr;
		if (trainingEevents == null || trainingEevents.length < 1)
			return;

		trainingEvents = Arrays.copyOf(trainingEevents, trainingEevents.length);
		
		if (upwardTrendTreeString != null && !upwardTrendTreeString.isEmpty() && 
				downwardTrendTreeString != null && !downwardTrendTreeString.isEmpty()){
			return;
			
		}

		if (Const.splitDatasetByTrendType) {

			// get upward dc first

			String gpTreeName = Const.UPWARD_EVENT_STRING + thresholdStr + Const.FUNCTION_NODE_DEFINITION
					+ ".txt";
			String thisLine = null;

			Vector<Event> trendOfChoiceVec = new Vector<Event>();

			for (int i = 0; i < trainingEevents.length; i++) {
				if (trainingEevents[i].type == Type.Upturn) {
					trendOfChoiceVec.add(trainingEevents[i]);
				}
			}

			System.out.println("upward event for classifier " + trendOfChoiceVec.size());
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

			TreeHelperClass treeHelperClass = new TreeHelperClass();
			if (Const.thresholdGPStringUpwardMap.containsKey(delta)) {
				gpTreeInFixNotation = Const.thresholdGPStringUpwardMap.get(delta);
				upwardTrendTreeString = gpTreeInFixNotation;

			} else {
				if (treeHelperClass.bestTreesInRuns != null)
					treeHelperClass.bestTreesInRuns.clear();
				// SymbolicRegression.log.save("Testing.txt", "test");
				treeHelperClass.getBestTreesForThreshold(uptrendEvent, Const.POP_SIZE, 1, Const.MAX_GENERATIONS,
						thresholdStr);

				if (treeHelperClass.bestTreesInRuns.isEmpty() || treeHelperClass.bestTreesInRuns.size() < 1) {
					System.out.println("TreeHelperClass.bestTreesInRuns.isEmpty()");
					System.exit(-1);
				}
				// get best tree
				Comparator<AbstractNode> comparator = Collections.reverseOrder();
				Collections.sort(treeHelperClass.bestTreesInRuns, comparator);
				AbstractNode tree = treeHelperClass.bestTreesInRuns.get(treeHelperClass.bestTreesInRuns.size() - 1);
				String treeAsInfixNotationString = tree.printAsInFixFunction();

				bestUpWardEventTree = treeHelperClass.bestTreesInRuns.get(treeHelperClass.bestTreesInRuns.size() - 1);
				upwardTrendTreeString = bestUpWardEventTree.printAsInFixFunction();
				//System.out.println("Best tree up:" + SymbolicRegression.file_Name + "->" + tree.getPerfScore());
				//System.out.println("Best tree structure" + treeAsInfixNotationString);

				curve_bestTreesInRunsUpward.setSize(treeHelperClass.bestTreesInRuns.size());
				Collections.copy(curve_bestTreesInRunsUpward, treeHelperClass.bestTreesInRuns);
				// SymbolicRegression.log.save(gpTreeName,
				// treeAsInfixNotationString);

			}

			// Downward trend GP here
			thresholdStr = String.format("%.8f", delta);
			gpTreeName = Const.DOWNWARD_EVENT_STRING + thresholdStr + Const.FUNCTION_NODE_DEFINITION
					+ "_prefectForesight.txt";
			thisLine = null;
			trendOfChoiceVec.clear();

			for (int i = 0; i < trainingEevents.length; i++) {
				if (trainingEevents[i].type == Type.Downturn) {
					trendOfChoiceVec.add(trainingEevents[i]);
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
				// SymbolicRegression.log.save("DownLoadTesting.txt",
				// "test");
				treeHelperClass.getBestTreesForThreshold(downtrendEvent, Const.POP_SIZE, 1, Const.MAX_GENERATIONS,
						thresholdStr);

				if (treeHelperClass.bestTreesInRuns.isEmpty() || treeHelperClass.bestTreesInRuns.size() < 1) {
					System.out.println("TreeHelperClass.bestTreesInRuns.isEmpty()");
					System.exit(-1);
				}

				// get best tree
				Comparator<AbstractNode> comparator = Collections.reverseOrder();
				Collections.sort(treeHelperClass.bestTreesInRuns, comparator);
				AbstractNode tree = treeHelperClass.bestTreesInRuns.get(treeHelperClass.bestTreesInRuns.size() - 1);
				String treeAsInfixNotationString = tree.printAsInFixFunction();

				bestDownWardEventTree = treeHelperClass.bestTreesInRuns.get(treeHelperClass.bestTreesInRuns.size() - 1);
				// System.out.println("Best tree" + tree.getPerfScore() + "
				// worst tree"+
				// TreeHelperClass.bestTreesInRuns.get(0).getPerfScore()) ;
				downwardTrendTreeString = bestDownWardEventTree.printAsInFixFunction();
			//	System.out.println("Best tree down:" + SymbolicRegression.file_Name + "->" + tree.getPerfScore());
			//	System.out.println("Best tree structure" + treeAsInfixNotationString);

				curve_bestTreesInRunsDownward.setSize(treeHelperClass.bestTreesInRuns.size());
				Collections.copy(curve_bestTreesInRunsDownward, treeHelperClass.bestTreesInRuns);
				// SymbolicRegression.log.save(gpTreeName,
				// treeAsInfixNotationString);
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

		if (testEvents == null || testEvents.length < 1)
			return;

		testingEvents = Arrays.copyOf(testEvents, testEvents.length);
		String thresholdStr = String.format("%.8f", delta);

		javax.script.ScriptEngineManager mgr = new javax.script.ScriptEngineManager();
		javax.script.ScriptEngine engine = mgr.getEngineByName("JavaScript");

		

		gpprediction = new double[testingEvents.length];

		for (int outputIndex = 0; outputIndex < testingEvents.length; outputIndex++) {
			String foo = "";
			if (Const.splitDatasetByTrendType) {
				if (testingEvents[outputIndex].type == Type.Upturn) {
					foo = upwardTrendTreeString;
					numberOfUpwardEvent++;
					isUpwardEvent = true;
				} else if (testEvents[outputIndex].type == Type.Downturn) {
					foo = downwardTrendTreeString;
					numberOfDownwardEvent++;
					isUpwardEvent = false;
				} else {
					System.out.println("Invalid event");
					System.exit(0);
				}
			} else {
				foo = trendTreeString;
			}

			foo = foo.replace("X0", Integer.toString(testingEvents[outputIndex].length()));
			foo = foo.replace("X1", Double.toString(testingEvents[outputIndex].high - testingEvents[outputIndex].low) );
			double eval = 0.0;
			Double javascriptValue = Double.MAX_VALUE;
			if ((testingEvents[outputIndex].overshoot == null || testEvents[outputIndex].overshoot.length() == 0)) {
				// Event eventOverShootCopy = new
				// Event(this.GPtestEvents[outputIndex].start,
				// this.GPtestEvents[outputIndex].end,
				// this.GPtestEvents[outputIndex].type);
				// this.GPtestEvents[outputIndex].overshoot= eventOverShootCopy;
				// if (isUpwardEvent)
				// numberOfNegativeUpwardEventGP++;
				// else
				// numberOfNegativeDownwardEventGP++;
			}

			try {
				javascriptValue = (Double) engine.eval(foo);
				eval = javascriptValue.doubleValue();
			} catch (javax.script.ScriptException e) {
				e.printStackTrace();

			} catch (ClassCastException e) {

				e.printStackTrace();
			}
			if (eval == Double.MAX_VALUE || eval == Double.NEGATIVE_INFINITY || Double.toString(eval) == "Infinity"
					|| eval == Double.NaN || Double.isNaN(eval) || Double.isInfinite(eval)
					|| eval == Double.POSITIVE_INFINITY || eval < 0) {

				eval = testEvents[outputIndex].length() * (double) GA.NEGATIVE_EXPRESSION_REPLACEMENT;

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
	double trade(PreProcess preprocess) {
		boolean isPositionOpen = false;
		double myPrice = 0.0;
		double transactionCost = 0.025 / 100;
		simpleDrawDown.Calculate(OpeningPosition);
		simpleSharpeRatio.addReturn(0);

		lastSellPrice = 0.0;
		lastBuyPrice = 0.0;
		StartSellQuantity = -1.0;
		StartBuyQuantity = -1.0;
		double lastUpDCCend = 0.0;
		for (int i = 1; i < testingEvents.length; i++) {

			int tradePoint = testingEvents[i].end + (int) gpprediction[i];

			if (testingEvents[i] == null)
				continue;

			if (i + 1 > testingEvents.length - 1)
				continue;

			if (testingEvents[i + 1] == null)
				continue;

			if (tradePoint > testingEvents[i + 1].start) // If a new DC is
															// encountered
															// before the
															// estimation point
															// skip trading
				continue;

			FReader freader = new FReader();
			FileMember2 fileMember2 = freader.new FileMember2();
			// fileMember2.Day = GPtestEvents[i].endDate;
			// fileMember2.time = GPtestEvents[i].endTime;
			// fileMember2.price = GPtestEvents[i].endPrice;

			if (tradePoint > FReader.dataRecordInFileArray.size()
					|| (lastTrainingPrice - 1) + tradePoint >= FReader.dataRecordInFileArray.size() - 1) {
				System.out.println(" DCCurveCifre: predicted datapoint " + ((lastTrainingPrice - 1) + tradePoint)
						+ " is beyond the size of price array  " + FReader.dataRecordInFileArray.size()
						+ " . Trading ended");
				break;
			} else {
				// I am opening my position in base currency
				try {
					// I am opening my position in base currency
					fileMember2 = FReader.dataRecordInFileArray.get((lastTrainingPrice - 1) + tradePoint);

					LinkedHashMap<Integer, Integer> anticipatedTrendMap = new LinkedHashMap<Integer, Integer>();
					LinkedHashMap<Integer, Integer> actualTrendMap = new LinkedHashMap<Integer, Integer>();

					if (testingEvents[i].type == Type.Upturn && !isPositionOpen) {
						// Now position is in quote currency
						// I sell base currency in bid price
						double askQuantity = OpeningPosition;
						double zeroTransactionCostAskQuantity = OpeningPosition;
						double transactionCostPrice = 0.0;
						myPrice = Double.parseDouble(fileMember2.askPrice);

						transactionCost = askQuantity * (0.025 / 100);
						transactionCostPrice = transactionCost * myPrice;
						askQuantity = (askQuantity - transactionCost) * myPrice;
						zeroTransactionCostAskQuantity = zeroTransactionCostAskQuantity * myPrice;
						// transactionCost = trainingOpeningPosition *
						// (0.025/100);
						// trainingOpeningPosition = (trainingOpeningPosition
						// -transactionCost) *myPrice;

						if (transactionCostPrice < (zeroTransactionCostAskQuantity - askQuantity) ){
					//	if (transactionCostPrice < (zeroTransactionCostAskQuantity - askQuantity) 
					//			&& (((lastSellPrice > 0.0) ? ((myPrice >= lastSellPrice) ? true : false): true ) ||
					//					(StartSellQuantity > -1.0  ? ((StartSellQuantity <= askQuantity) ? true : false) : true  ))) {
							
							if (StartSellQuantity <= -1.0)
								StartSellQuantity = OpeningPosition;
							
							OpeningPosition = askQuantity;
							
							lastUpDCCend = Double.parseDouble(FReader.dataRecordInFileArray.get((lastTrainingPrice - 1) + testingEvents[i].end).bidPrice);
							
							isPositionOpen = true;
							positionArrayQuote.add(new Double(OpeningPosition));
							lastSellPrice = myPrice;
							tradedPrice.add(new Double(myPrice));
							anticipatedTrendMap.put(testingEvents[i].start, tradePoint);
							anticipatedTrend.add(anticipatedTrendMap);
							

							if (testingEvents[i].overshoot == null || testingEvents[i].overshoot.length() < 1)
								actualTrendMap.put(testingEvents[i].start, testingEvents[i].end);
							else
								actualTrendMap.put(testingEvents[i].start, testingEvents[i].overshoot.end);

							actualTrend.add(actualTrendMap);
						}
					} else if (testingEvents[i].type == Type.Downturn && isPositionOpen) {
						// Now position is in base currency
						// I buy base currency
						double bidQuantity = OpeningPosition;
						double zeroTransactionCostBidQuantity = OpeningPosition;
						double transactionCostPrice = 0.0;
						myPrice = Double.parseDouble(fileMember2.bidPrice);

						transactionCost = bidQuantity * (0.025 / 100);
						transactionCostPrice = transactionCost * myPrice;
						bidQuantity = (bidQuantity - transactionCost) * myPrice;
						zeroTransactionCostBidQuantity = zeroTransactionCostBidQuantity * myPrice;
						// transactionCost = trainingOpeningPosition *
						// (0.025/100);
						// trainingOpeningPosition = (trainingOpeningPosition
						// -transactionCost) /myPrice;

						if (transactionCostPrice < (zeroTransactionCostBidQuantity - bidQuantity)
								&& myPrice < lastUpDCCend){
				//		if (transactionCostPrice < (zeroTransactionCostBidQuantity - bidQuantity)
				//				&& (( lastBuyPrice > 0.0 ? ((myPrice <= lastBuyPrice ) ? true :false ): true )||
				//				(StartBuyQuantity > -1.0  ? ((StartBuyQuantity > bidQuantity) ? true: false) : true  ))) {
									
									if (StartBuyQuantity <= -1.0)
										StartBuyQuantity = OpeningPosition;
					
							lastBuyPrice = myPrice;
							OpeningPosition = (OpeningPosition - transactionCost) / myPrice;
							
							
							isPositionOpen = false;
							positionArrayBase.add(new Double(OpeningPosition));
							
							tradedPrice.add(new Double(myPrice));
							anticipatedTrendMap.put(testingEvents[i].start, tradePoint);
							anticipatedTrend.add(anticipatedTrendMap);

							if (testingEvents[i].overshoot == null || testingEvents[i].overshoot.length() < 1)
								actualTrendMap.put(testingEvents[i].start, testingEvents[i].end);
							else
								actualTrendMap.put(testingEvents[i].start, testingEvents[i].overshoot.end);

							actualTrend.add(actualTrendMap);
						
						}
					}
				} catch (ArrayIndexOutOfBoundsException exception) {
					System.out.println(" DCCurveCifre: Search for element " + ((lastTrainingPrice - 1) + tradePoint)
							+ " is beyond the size of price array  " + FReader.dataRecordInFileArray.size()
							+ " . Trading ended");
					break;

				}

			}

		}

		if (isPositionOpen) {
			tradedPrice.remove(tradedPrice.size() - 1);
			anticipatedTrend.remove(anticipatedTrend.size() - 1);
			actualTrend.remove(actualTrend.size() - 1);
			OpeningPosition = positionArrayBase.get(positionArrayBase.size() - 1);
			positionArrayQuote.remove(positionArrayQuote.size() - 1);
			isPositionOpen = false;
		}
		
		otherTradeCalculations();

		
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
		return "DCCurveCifre";
	}

	@Override
	double trainingTrading(PreProcess preprocess) {
		boolean isPositionOpen = false;
		double myPrice = 0.0;
		double lastClosedPosition = 0.0;
		double transactionCost = 0.025 / 100;
		double DD = 0;// DrawDown
		
		lastSellPrice = 0.0;
		lastBuyPrice = 0.0;
		StartSellQuantity = -1.0;
		StartBuyQuantity = -1.0;
		for (int i = 1; i < trainingEvents.length; i++) {

			// if ( gpprediction[i] == 0) // don't Skip DC classified GP build
			// using all DCs
			// continue;

			int tradePoint = trainingEvents[i].end + (int) trainingGpPrediction[i];

			if (trainingEvents[i] == null)
				continue;

			if (i + 1 > trainingEvents.length - 1)
				continue;

			if (trainingEvents[i + 1] == null)
				continue;

			if (tradePoint > trainingEvents[i + 1].start) // If a new DC is
															// encountered
															// before the
															// estimation point
															// skip trading
				continue;

			FReader freader = new FReader();
			FileMember2 fileMember2 = freader.new FileMember2();

			if (tradePoint >= FReader.dataRecordInFileArray.size()) {
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
			if (trainingEvents[i].type == Type.Upturn && !isPositionOpen) {
				// Now position is in quote currency
				// I sell base currency in bid price
				double askQuantity = trainingOpeningPosition;
				double zeroTransactionCostAskQuantity = trainingOpeningPosition;
				double transactionCostPrice = 0.0;
				myPrice = Double.parseDouble(fileMember2.askPrice);

				transactionCost = askQuantity * (0.025 / 100);
				transactionCostPrice = transactionCost * myPrice;
				askQuantity = (askQuantity - transactionCost) * myPrice;
				zeroTransactionCostAskQuantity = zeroTransactionCostAskQuantity * myPrice;
				
				if (transactionCostPrice < (zeroTransactionCostAskQuantity - askQuantity)){
			//	if (transactionCostPrice < (zeroTransactionCostAskQuantity - askQuantity)
			//			&& (((lastSellPrice > 0.0) ? ((myPrice >= lastSellPrice) ? true : false): true ) ||
			//					(StartSellQuantity > -1.0  ? ((StartSellQuantity <= askQuantity) ? true : false) : true  ))) {
					
					if (StartSellQuantity <= -1.0)
						StartSellQuantity = OpeningPosition;
					lastSellPrice = myPrice;
					trainingOpeningPosition = askQuantity;
					isPositionOpen = true;
				}
			} else if (trainingEvents[i].type == Type.Downturn && isPositionOpen) {
				// Now position is in base currency
				// I buy base currency
				double bidQuantity = trainingOpeningPosition;
				double zeroTransactionCostBidQuantity = trainingOpeningPosition;
				double transactionCostPrice = 0.0;
				myPrice = Double.parseDouble(fileMember2.bidPrice);

				transactionCost = bidQuantity * (0.025 / 100);
				transactionCostPrice = transactionCost * myPrice;
				bidQuantity = (bidQuantity - transactionCost) * myPrice;
				zeroTransactionCostBidQuantity = zeroTransactionCostBidQuantity * myPrice;
				// transactionCost = trainingOpeningPosition * (0.025/100);
				// trainingOpeningPosition = (trainingOpeningPosition
				// -transactionCost) /myPrice;

				if (transactionCostPrice < (zeroTransactionCostBidQuantity - bidQuantity)){
		//		if (transactionCostPrice < (zeroTransactionCostBidQuantity - bidQuantity)
		//				&& (( lastBuyPrice > 0.0 ? ((myPrice <= lastBuyPrice ) ? true :false ): true )||
		//						(StartBuyQuantity > -1.0  ? ((StartBuyQuantity > bidQuantity) ? true: false) : true  ))) {
									
							if (StartBuyQuantity <= -1.0)
								StartBuyQuantity = OpeningPosition;
									
					lastBuyPrice = myPrice;
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
	void estimateTraining(PreProcess preprocess) {
		trainingGpPrediction = new double[trainingEvents.length];
		javax.script.ScriptEngineManager mgr = new javax.script.ScriptEngineManager();
		javax.script.ScriptEngine engine = mgr.getEngineByName("JavaScript");

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
					System.out.println("Invalid event");
					System.exit(0);
				}
			} else {
				foo = trendTreeString;
			}

			foo = foo.replace("X0", Integer.toString(trainingEvents[outputIndex].length()));
			foo = foo.replace("X1", Double.toString(trainingEvents[outputIndex].high - trainingEvents[outputIndex].low) );
			double eval = 0.0;
			Double javascriptValue = Double.MAX_VALUE;

			try {
				javascriptValue = (Double) engine.eval(foo);
				eval = javascriptValue.doubleValue();
			} catch (javax.script.ScriptException e) {
				e.printStackTrace();

			} catch (ClassCastException e) {

				e.printStackTrace();
			}
			if (eval == Double.MAX_VALUE || eval == Double.NEGATIVE_INFINITY || Double.toString(eval) == "Infinity"
					|| eval == Double.NaN || Double.isNaN(eval) || Double.isInfinite(eval)
					|| eval == Double.POSITIVE_INFINITY || eval < 0) {

				eval = trainingEvents[outputIndex].length() * (double) GA.NEGATIVE_EXPRESSION_REPLACEMENT;

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
