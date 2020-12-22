/**
* This file is part of the directional changes DC+GA project.
*
* DC+GA is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* DC+GA is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with DC+GA.  If not, see <http://www.gnu.org/licenses/>.
*/

package dc.ga;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import java.util.concurrent.ConcurrentHashMap;

import java.util.stream.Collectors;



import java.util.Random;
import java.util.Vector;

import dc.EventWriter;
import dc.GP.AbstractNode;
import dc.GP.Const;
import dc.ga.DCCurve.Event;
import dc.ga.DCCurve.Type;

import dc.io.FReader;
import dc.io.Logger;


import misc.DCCurveClassification;

import misc.DCCurvePerfectForesight;

import misc.DCEventGenerator;
import misc.Orders;


import dc.ga.PreProcess;

public class GA_new {

	Orders[] orderArrayTraining;
	Orders[] orderArrayTest;
	int numberOfThresholds;

	protected static double[] CANDIDATE_THRESHOLDS;
	// Map<Double, Double [] > trainiingOrders = new LinkedHashMap<Double,
	// Double []>();
	// Map<Double, Double [] > testOrders = new LinkedHashMap<Double, Double
	// []>();

	double thresholdIncrement;
	String filename = "";
	int currentGeneration;
	String singlethresholdTrainingReturnsStr;
	String singlethresholdTestReturnsStr;
	String singlethresholdTrainingSharpeRatioStr;
	String singlethresholdTestSharpeRatioStr;
	String singlethresholdTrainingMDDStr;
	String singlethresholdTestMDDStr;
	Double[] training;
	Double[] test;

	double[] bestIndividual;

	DCCurvePerfectForesight[] curvePerfectForesight_Selected;
	DCCurveClassification[] curveClassifcation;
	DCCurveClassification curveClassifcationForGA;
	DCCurvePerfectForesight curvePerfectForesight_SelectedForGA;
	
	ConcurrentHashMap<Integer, String> gpDaysMap = null;
	Map<String, Event[]> trainingEventsArray = new LinkedHashMap<String, Event[]>();
	Map<String, Event[]> trainingOutputArray = new LinkedHashMap<String, Event[]>();
	Map<Double, Double> perfectForecastReturnMap = new HashMap<Double, Double>();
	Map<Integer, Double> perfectForecastRatioMap = new HashMap<Integer, Double>();
	Map<Integer, Double> perfectForecastRatioMap80 = new HashMap<Integer, Double>();
	Map<Integer, Double> perfectForecastRatioMap70 = new HashMap<Integer, Double>();
	Map<Integer, Double> perfectForecastRatioMap60 = new HashMap<Integer, Double>();
	Map<Integer, Double> perfectForecastRatioMap50 = new HashMap<Integer, Double>();
	Map<String, Integer> ThresholdChromosomMap = new HashMap<String, Integer>();
	Map<Double, Double> traningSingleThresholdSharpRatio = new HashMap<Double, Double>();
	int numberOfEvenThresholds = 0;
	int totalEvents = 0;

	// Map<Integer, Double> Threshold2EventLengthPredictionUp = new
	// HashMap<Integer, Double>();
	// Map<Integer, Double> Threshold3EventLengthPredictionUp = new
	// HashMap<Integer, Double>();
	// Map<Integer, Double> Threshold4EventLengthPredictionUp = new
	// HashMap<Integer, Double>();
	// Map<Integer, Double> Threshold5EventLengthPredictionUp = new
	// HashMap<Integer, Double>();

	// Map<Integer, Double> Threshold2EventLengthPredictionDown = new
	// HashMap<Integer, Double>();
	// Map<Integer, Double> Threshold3EventLengthPredictionDown = new
	// HashMap<Integer, Double>();
	// Map<Integer, Double> Threshold4EventLengthPredictionDown = new
	// HashMap<Integer, Double>();
	// Map<Integer, Double> Threshold5EventLengthPredictionDown = new
	// HashMap<Integer, Double>();

	Map<Double, Map<Integer, Double>> upGpcalc = new HashMap<Double, Map<Integer, Double>>();
	Map<Double, Map<Integer, Double>> downGpcalc = new HashMap<Double, Map<Integer, Double>>();

	double[] SELECTED_THRESHOLDS;
	PreProcess[] preprocess = null;
	int trainingDay = -1;
	Map<String, Event[]> testEventsArray = new LinkedHashMap<String, Event[]>();
	List<Entry<Double, Double>> greatest = new ArrayList<Entry<Double, Double>>();
	int trainingDataPtCount = 0;
	int testDataPtCount = 0;

	final double transactionCost = 0.025;
	Double[][] trainingMeanRatio;

	AbstractNode[][] trainingGPTrees;

	// FReader st = new FReader();
	// FReader.FileMember2 TestDataObject = st.new FileMember2();

	List<FReader.FileMember2> trainingDataList = new ArrayList<FReader.FileMember2>();
	List<FReader.FileMember2> testDataList = new ArrayList<FReader.FileMember2>();
	static double[][] pop;
	double[][] newPop;
	boolean isTrainingDataPrinted = false;
	boolean isTestDataPrinted = false;

	int POPSIZE;
	int tournamentSize;
	int MAX_GENERATIONS;

	double CROSSOVER_PROB;
	double MUTATION_PROB;

	static int nRuns;
	public static int NEGATIVE_EXPRESSION_REPLACEMENT = 5;
	double bestFitness;
	static int argBestFitness;
	static double budget;
	double shortSellingAllowance;
	double mddWeight;
	int xoverOperatorIndex;
	int mutOperatorIndex;

	int MAX_QUANTITY;// maximum quantity we can possibly buy/sell. Random
						// quantities are generated in the range [1,
						// MAX_QUANTITY).

	int MAX_SHORT_SELLING_QUANTITY;// maximum quantity we can possibly
									// short-sell. Random quantities are
									// generated in the range [1,
									// MAX_SHORT_SELLING_QUANTITY).

	protected static Random random;

	// Map<Double, Double> bestfitnessReturnMap = new HashMap<Double, Double>();
	List<Entry<Double, Double>> bestFitnessList = new ArrayList<Entry<Double, Double>>();;
	protected static Logger log;

	private Fitness bestTestFitness = new Fitness();// to keep track of the best
													// test fitness, regardless
													// of which GA run we
													// currently are.

	final protected double slippageAllowance;// SLIPPAGE is the difference
												// between the price of a trade
												// that is expected and the
												// executed price.
	// Slippage may occur more during high-volatility periods when a Forex
	// trader executes a market order. Slippage can also occur when a
	// currency pair is traded in a large lot with low volume where there may be
	// less interest in the underlying asset being traded.
	// Slippage can take place in Forex markets, and other markets such as
	// equities or bonds, when the market orders are placed. Slippage
	// can also take place when limit orders are used during high periods of
	// volatility based on news or other events. When this occurs,
	// FXDD will by default try to execute the trade at the next best price
	// available.
	// Source: [1]
	// http://www.fxdd.com/us/en/forex-resources/faq-glossary/faq/how-does-fxdd-handle-slippage/
	// Source: [2] Brabazon, O'Neill: "Evolving technical trading rules for spot
	// foreign-exchange markets using grammatical evolution"
	// Source: [3] Levich R., Thomas, L.: "The significance of technical
	// trading-rule profits in the foreign exchange market: a bootstrap
	// approach". Journal of International Money and Finance 12 (5): 451-474
	// (1993) (THIS SOURCE GIVES INFO ON HOW TO PRICE TRADING COSTS AND
	// SLIPPAGE)

	public GA_new(String filename, int trainingIndexStart, int trainingIndexEnd, int testIndexStart, int testIndexEnd,
			int POPSIZE, int MAX_GENERATIONS, int tournamentSize, double CROSSOVER_PROB, double MUTATION_PROB,
			double thresholdIncrement, int numberOfThresholds, int MAX_QUANTITY, int budget,
			double shortSellingAllowance, double mddWeight, int xoverOperatorIndex, int mutOperatorIndex,
			double initialThreshold) throws IOException, ParseException {

		//double initial = initialThreshold;// 0.01

		CANDIDATE_THRESHOLDS = new double[Const.NUMBER_OF_THRESHOLDS];
		trainingMeanRatio = new Double[Const.NUMBER_OF_SELECTED_THRESHOLDS][2];// There's
																				// only
																				// 2
		// ratios, one
		trainingDay = trainingIndexStart; // for upward
		// and one for
		// downward OS
		// events.
		SELECTED_THRESHOLDS = new double[Const.NUMBER_OF_SELECTED_THRESHOLDS];

		trainingGPTrees = new AbstractNode[Const.NUMBER_OF_SELECTED_THRESHOLDS][2];// There's
		// only
		// 2
		this.filename = filename;
		// ratios, one
		// for upward
		// and one for
		// downward OS
		// events.

		this.POPSIZE = POPSIZE;
		this.MAX_GENERATIONS = MAX_GENERATIONS;
		this.tournamentSize = tournamentSize;
		this.CROSSOVER_PROB = CROSSOVER_PROB;
		this.MUTATION_PROB = MUTATION_PROB;
		this.thresholdIncrement = thresholdIncrement;
		this.numberOfThresholds = numberOfThresholds;
		this.MAX_QUANTITY = MAX_QUANTITY;
		GA_new.budget = budget;
		this.shortSellingAllowance = shortSellingAllowance;
		this.mddWeight = mddWeight;
		this.xoverOperatorIndex = xoverOperatorIndex;
		this.mutOperatorIndex = mutOperatorIndex;
		
		pop = new double[POPSIZE][Const.NUMBER_OF_SELECTED_THRESHOLDS + 4];
		// pop = new
		// double[POPSIZE][Const.NUMBER_OF_SELECTED_THRESHOLDS+numberOfExtraChromosom+1];//
		// +5
		// because
		// index
		// 0
		// will be the
		// quantity, index 1
		// will be beta, and
		// index 2 will be
		// beta2, and index
		// 3 will be
		// shortSellingQuantity,
		// beta3
		// maximum number of
		// positions
		// newPop = new
		// double[POPSIZE][Const.NUMBER_OF_SELECTED_THRESHOLDS+numberOfExtraChromosom+1];
		newPop = new double[POPSIZE][Const.NUMBER_OF_SELECTED_THRESHOLDS + 4];

		nRuns = 50;

		slippageAllowance = 0.01 / 100; // 0 / 100;// 0.01/100 the 0.01 is the
										// proper cost

		System.out.println("Slippage allowance: " + slippageAllowance);

		bestTestFitness.value = Double.NEGATIVE_INFINITY;

		MAX_SHORT_SELLING_QUANTITY = 1;// set to 1 to disable short-selling,
										// because the only result when I do
										// random.nextInt(1) in the
										// generateQuantity() method is 0.

		System.out.println("Loading directional changes data...");
		bestIndividual = new double[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		// loads the data
		ArrayList<Double[]> days = FReader.loadData(filename, false);

		FReader.dataRecordInFileArray.get(1);
		// allow the creation of training & testing data sets that are longer
		// than 1 day
		ArrayList<Double[]> ar = new ArrayList<Double[]>();

		for (int i = trainingIndexStart; i <= trainingIndexEnd; i++)
			ar.add(days.get(i));
		int size = 0;
		for (Double[] d : ar)
			size += d.length;
		training = new Double[size];
		int counter = 0;
		for (Double[] d : ar) {
			for (double n : d) {
				training[counter] = n;
				counter++;
			}
		}

		for (int i = 0; i < counter; i++) {
			trainingDataList.add(FReader.dataRecordInFileArray.get(i));
		}
		trainingDataPtCount = counter;
		ar = new ArrayList<Double[]>();
		for (int i = testIndexStart; i <= testIndexEnd; i++)
			ar.add(days.get(i));
		size = 0;
		for (Double[] d : ar)
			size += d.length;
		test = new Double[size];
		counter = 0;
		for (Double[] d : ar) {
			for (double n : d) {
				test[counter] = n;
				counter++;
			}
		}
		testDataPtCount = counter;
		int trainingDataSize = trainingDataList.size();
		for (int i = trainingDataSize; i < (counter + trainingDataSize); i++) {
			testDataList.add(FReader.dataRecordInFileArray.get(i));
		}
		// budget = 100000;

		DCCurvePerfectForesight[] curvePerfectForesight;
		curvePerfectForesight = new DCCurvePerfectForesight[Const.NUMBER_OF_THRESHOLDS];
		curveClassifcation = new DCCurveClassification[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		curvePerfectForesight_Selected = new DCCurvePerfectForesight[Const.NUMBER_OF_SELECTED_THRESHOLDS];

		orderArrayTraining = new Orders[training.length];
		orderArrayTest = new Orders[test.length];
		gpDaysMap = FReader.loadDataMap(filename);

		if (Const.NUMBER_OF_SELECTED_THRESHOLDS >= Const.NUMBER_OF_THRESHOLDS) {
			System.out.println("Number of thresholds to select cannot be greater of equal to candidate thresholds");
			System.exit(-1);
		}
		/*
		 * Candidate threshold generation
		 */
		for (int i = 0; i < Const.NUMBER_OF_THRESHOLDS; i++) {
			CANDIDATE_THRESHOLDS[i] = (0.005 + (0.0025 * i)) / 100.0;
			// String thresholdStr = String.format("%.8f",
			// CANDIDATE_THRESHOLDS[i]);
			// System.out.println(thresholdStr);
		}
		Event[] copiedArray;
		Event[] copiedOutputArray;

		/*
		 * Regression starts
		 */
		for (int i = 0; i < CANDIDATE_THRESHOLDS.length; i++) {

			Const.splitDatasetByTrendType = true;
			curvePerfectForesight[i] = new DCCurvePerfectForesight();

			String thresholdStr = String.format("%.8f", CANDIDATE_THRESHOLDS[i]);
			String gpFileNamePrefix = gpDaysMap.get(trainingIndexStart);
			String gpFileName = gpFileNamePrefix + "_" + thresholdStr + ".txt";

			DCEventGenerator dCEventGenerator = new DCEventGenerator();
			dCEventGenerator.generateEvents(training, CANDIDATE_THRESHOLDS[i]);
			copiedArray = Arrays.copyOf(dCEventGenerator.getEvents(), dCEventGenerator.getEvents().length);
			trainingEventsArray.put(thresholdStr, copiedArray);

			copiedOutputArray = Arrays.copyOf(dCEventGenerator.getOutput(), dCEventGenerator.getOutput().length);
			trainingOutputArray.put(thresholdStr, copiedOutputArray);

			if (copiedArray.length < 10)
				continue;

			curvePerfectForesight[i].filename = filename;

			curvePerfectForesight[i].build(training, CANDIDATE_THRESHOLDS[i], gpFileName, copiedArray,
					copiedOutputArray, null);
			curvePerfectForesight[i].estimateTrainingUsingOutputData(null); // null
																			// because
			// not doing
			// classification

			// curvePerfectForesight[i].trainingOutputEvents
			curvePerfectForesight[i].setMarketdataListTraining(trainingDataPtCount);
			curvePerfectForesight[i].setMarketdataListTest(testDataPtCount);

			curvePerfectForesight[i].estimateTrainingUnderPerfectforesight();

			double perfectForcastTrainingReturn = curvePerfectForesight[i].trainingTrading(null);
			double sharpeRatioTraining = curvePerfectForesight[i].getSharpRatio();
			perfectForecastReturnMap.put(CANDIDATE_THRESHOLDS[i], perfectForcastTrainingReturn);
			traningSingleThresholdSharpRatio.put(CANDIDATE_THRESHOLDS[i], curvePerfectForesight[i].getSharpRatio());

			double upwardPerf = curvePerfectForesight[i].bestUpWardEventTree.perfScore;
			double downwardPerf = curvePerfectForesight[i].bestDownWardEventTree.perfScore;
			double ratio = 0.0;
			if (upwardPerf < downwardPerf)
				ratio = upwardPerf / downwardPerf;
			else
				ratio = downwardPerf / upwardPerf;

			if (Double.compare(upwardPerf, Double.MAX_VALUE) >= 0)
				continue;

			if (Double.compare(upwardPerf, -Double.MAX_VALUE) <= 0)
				continue;

			if (Double.compare(downwardPerf, -Double.MAX_VALUE) <= 0)
				continue;

			if (Double.compare(downwardPerf, Double.MAX_VALUE) >= 0)
				continue;

			if (Double.compare(ratio, 0.9) >= 0)
				perfectForecastRatioMap.put(i, ratio);

			if (Double.compare(ratio, 0.9) < 0 && Double.compare(ratio, 0.8) >= 0)
				perfectForecastRatioMap80.put(i, ratio);

			if (Double.compare(ratio, 0.8) < 0 && Double.compare(ratio, 0.7) >= 0)
				perfectForecastRatioMap70.put(i, ratio);

			if (Double.compare(ratio, 0.7) < 0 && Double.compare(ratio, 0.6) >= 0)
				perfectForecastRatioMap60.put(i, ratio);

			if (Double.compare(ratio, 0.6) < 0 && Double.compare(ratio, 0.5) >= 0)
				perfectForecastRatioMap50.put(i, ratio);

			System.out.println("Performance upward " + curvePerfectForesight[i].bestUpWardEventTree.perfScore
					+ " performance downward" + curvePerfectForesight[i].bestDownWardEventTree.perfScore + " Ratio "
					+ ratio);

		}

		// * To choose best ratio starts
		/*
		 * for (int i = 0; i < CANDIDATE_THRESHOLDS.length; i++) {
		 * perfectForecastReturnMap.put(CANDIDATE_THRESHOLDS[i], 0.0); }
		 * 
		 * if (perfectForecastRatioMap.size() >
		 * Const.NUMBER_OF_SELECTED_THRESHOLDS) { for (Map.Entry<Integer,
		 * Double> entry : perfectForecastRatioMap.entrySet()) { int
		 * dcCurvePositionInCurvePerfectForesightArray = entry.getKey();
		 * 
		 * double perfectForcastTrainingReturn =
		 * curvePerfectForesight[dcCurvePositionInCurvePerfectForesightArray]
		 * .trainingTrading(null); perfectForecastReturnMap.put(
		 * curvePerfectForesight[dcCurvePositionInCurvePerfectForesightArray].
		 * thresholdValue, perfectForcastTrainingReturn); } } else if
		 * ((perfectForecastRatioMap.size() + perfectForecastRatioMap80.size())
		 * > Const.NUMBER_OF_SELECTED_THRESHOLDS) { for (Map.Entry<Integer,
		 * Double> entry : perfectForecastRatioMap.entrySet()) { int
		 * dcCurvePositionInCurvePerfectForesightArray = entry.getKey();
		 * 
		 * double perfectForcastTrainingReturn =
		 * curvePerfectForesight[dcCurvePositionInCurvePerfectForesightArray]
		 * .trainingTrading(null); perfectForecastReturnMap.put(
		 * curvePerfectForesight[dcCurvePositionInCurvePerfectForesightArray].
		 * thresholdValue, perfectForcastTrainingReturn); }
		 * 
		 * for (Map.Entry<Integer, Double> entry :
		 * perfectForecastRatioMap80.entrySet()) { int
		 * dcCurvePositionInCurvePerfectForesightArray = entry.getKey();
		 * 
		 * double perfectForcastTrainingReturn =
		 * curvePerfectForesight[dcCurvePositionInCurvePerfectForesightArray]
		 * .trainingTrading(null); perfectForecastReturnMap.put(
		 * curvePerfectForesight[dcCurvePositionInCurvePerfectForesightArray].
		 * thresholdValue, perfectForcastTrainingReturn); }
		 * 
		 * } else if ((perfectForecastRatioMap.size() +
		 * perfectForecastRatioMap80.size() + perfectForecastRatioMap70.size())
		 * > Const.NUMBER_OF_SELECTED_THRESHOLDS) { for (Map.Entry<Integer,
		 * Double> entry : perfectForecastRatioMap.entrySet()) { int
		 * dcCurvePositionInCurvePerfectForesightArray = entry.getKey();
		 * 
		 * double perfectForcastTrainingReturn =
		 * curvePerfectForesight[dcCurvePositionInCurvePerfectForesightArray]
		 * .trainingTrading(null); perfectForecastReturnMap.put(
		 * curvePerfectForesight[dcCurvePositionInCurvePerfectForesightArray].
		 * thresholdValue, perfectForcastTrainingReturn); }
		 * 
		 * for (Map.Entry<Integer, Double> entry :
		 * perfectForecastRatioMap80.entrySet()) { int
		 * dcCurvePositionInCurvePerfectForesightArray = entry.getKey();
		 * 
		 * double perfectForcastTrainingReturn =
		 * curvePerfectForesight[dcCurvePositionInCurvePerfectForesightArray]
		 * .trainingTrading(null); perfectForecastReturnMap.put(
		 * curvePerfectForesight[dcCurvePositionInCurvePerfectForesightArray].
		 * thresholdValue, perfectForcastTrainingReturn); } for
		 * (Map.Entry<Integer, Double> entry :
		 * perfectForecastRatioMap70.entrySet()) { int
		 * dcCurvePositionInCurvePerfectForesightArray = entry.getKey();
		 * 
		 * double perfectForcastTrainingReturn =
		 * curvePerfectForesight[dcCurvePositionInCurvePerfectForesightArray]
		 * .trainingTrading(null); perfectForecastReturnMap.put(
		 * curvePerfectForesight[dcCurvePositionInCurvePerfectForesightArray].
		 * thresholdValue, perfectForcastTrainingReturn); } } else if
		 * ((perfectForecastRatioMap.size() + perfectForecastRatioMap80.size() +
		 * perfectForecastRatioMap70.size() + perfectForecastRatioMap60.size())
		 * > Const.NUMBER_OF_SELECTED_THRESHOLDS) { for (Map.Entry<Integer,
		 * Double> entry : perfectForecastRatioMap.entrySet()) { int
		 * dcCurvePositionInCurvePerfectForesightArray = entry.getKey();
		 * 
		 * double perfectForcastTrainingReturn =
		 * curvePerfectForesight[dcCurvePositionInCurvePerfectForesightArray]
		 * .trainingTrading(null); perfectForecastReturnMap.put(
		 * curvePerfectForesight[dcCurvePositionInCurvePerfectForesightArray].
		 * thresholdValue, perfectForcastTrainingReturn); }
		 * 
		 * for (Map.Entry<Integer, Double> entry :
		 * perfectForecastRatioMap80.entrySet()) { int
		 * dcCurvePositionInCurvePerfectForesightArray = entry.getKey();
		 * 
		 * double perfectForcastTrainingReturn =
		 * curvePerfectForesight[dcCurvePositionInCurvePerfectForesightArray]
		 * .trainingTrading(null); perfectForecastReturnMap.put(
		 * curvePerfectForesight[dcCurvePositionInCurvePerfectForesightArray].
		 * thresholdValue, perfectForcastTrainingReturn); } for
		 * (Map.Entry<Integer, Double> entry :
		 * perfectForecastRatioMap70.entrySet()) { int
		 * dcCurvePositionInCurvePerfectForesightArray = entry.getKey();
		 * 
		 * double perfectForcastTrainingReturn =
		 * curvePerfectForesight[dcCurvePositionInCurvePerfectForesightArray]
		 * .trainingTrading(null); perfectForecastReturnMap.put(
		 * curvePerfectForesight[dcCurvePositionInCurvePerfectForesightArray].
		 * thresholdValue, perfectForcastTrainingReturn); } for
		 * (Map.Entry<Integer, Double> entry :
		 * perfectForecastRatioMap60.entrySet()) { int
		 * dcCurvePositionInCurvePerfectForesightArray = entry.getKey();
		 * 
		 * double perfectForcastTrainingReturn =
		 * curvePerfectForesight[dcCurvePositionInCurvePerfectForesightArray]
		 * .trainingTrading(null); perfectForecastReturnMap.put(
		 * curvePerfectForesight[dcCurvePositionInCurvePerfectForesightArray].
		 * thresholdValue, perfectForcastTrainingReturn); } } else if
		 * ((perfectForecastRatioMap.size() + perfectForecastRatioMap80.size() +
		 * perfectForecastRatioMap70.size() + perfectForecastRatioMap60.size() +
		 * perfectForecastRatioMap50.size()) >
		 * Const.NUMBER_OF_SELECTED_THRESHOLDS) { for (Map.Entry<Integer,
		 * Double> entry : perfectForecastRatioMap.entrySet()) { int
		 * dcCurvePositionInCurvePerfectForesightArray = entry.getKey();
		 * 
		 * double perfectForcastTrainingReturn =
		 * curvePerfectForesight[dcCurvePositionInCurvePerfectForesightArray]
		 * .trainingTrading(null); perfectForecastReturnMap.put(
		 * curvePerfectForesight[dcCurvePositionInCurvePerfectForesightArray].
		 * thresholdValue, perfectForcastTrainingReturn); }
		 * 
		 * for (Map.Entry<Integer, Double> entry :
		 * perfectForecastRatioMap80.entrySet()) { int
		 * dcCurvePositionInCurvePerfectForesightArray = entry.getKey();
		 * 
		 * double perfectForcastTrainingReturn =
		 * curvePerfectForesight[dcCurvePositionInCurvePerfectForesightArray]
		 * .trainingTrading(null); perfectForecastReturnMap.put(
		 * curvePerfectForesight[dcCurvePositionInCurvePerfectForesightArray].
		 * thresholdValue, perfectForcastTrainingReturn); } for
		 * (Map.Entry<Integer, Double> entry :
		 * perfectForecastRatioMap70.entrySet()) { int
		 * dcCurvePositionInCurvePerfectForesightArray = entry.getKey();
		 * 
		 * double perfectForcastTrainingReturn =
		 * curvePerfectForesight[dcCurvePositionInCurvePerfectForesightArray]
		 * .trainingTrading(null); perfectForecastReturnMap.put(
		 * curvePerfectForesight[dcCurvePositionInCurvePerfectForesightArray].
		 * thresholdValue, perfectForcastTrainingReturn); } for
		 * (Map.Entry<Integer, Double> entry :
		 * perfectForecastRatioMap60.entrySet()) { int
		 * dcCurvePositionInCurvePerfectForesightArray = entry.getKey();
		 * 
		 * double perfectForcastTrainingReturn =
		 * curvePerfectForesight[dcCurvePositionInCurvePerfectForesightArray]
		 * .trainingTrading(null); perfectForecastReturnMap.put(
		 * curvePerfectForesight[dcCurvePositionInCurvePerfectForesightArray].
		 * thresholdValue, perfectForcastTrainingReturn); } for
		 * (Map.Entry<Integer, Double> entry :
		 * perfectForecastRatioMap50.entrySet()) { int
		 * dcCurvePositionInCurvePerfectForesightArray = entry.getKey();
		 * 
		 * double perfectForcastTrainingReturn =
		 * curvePerfectForesight[dcCurvePositionInCurvePerfectForesightArray]
		 * .trainingTrading(null); perfectForecastReturnMap.put(
		 * curvePerfectForesight[dcCurvePositionInCurvePerfectForesightArray].
		 * thresholdValue, perfectForcastTrainingReturn); } } else {
		 * System.out.println(
		 * "Program cannot proceed. Sufficcient thresholds not identified. Exiting"
		 * ); System.exit(0); }
		 */
		// * To choose best ratio end
		greatest = HelperClass.findGreatest(perfectForecastReturnMap, Const.NUMBER_OF_SELECTED_THRESHOLDS); // best

		preprocess = new PreProcess[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		/*
		 * Select best thresholds
		 */
		int tradingThresholdCount = 0;
		System.out.println("Selecting GP threshold");
		int chromosomPositionCount = SELECTED_THRESHOLDS.length;
		for (Entry<Double, Double> entry : greatest) {
			// System.out.println(entry);
			SELECTED_THRESHOLDS[tradingThresholdCount] = entry.getKey();
			System.out.println(SELECTED_THRESHOLDS[tradingThresholdCount]);

			chromosomPositionCount++;
			tradingThresholdCount++;
		}

		/*
		 * update selected DCCurvePerfectForesight
		 */

		for (int i = 0; i < SELECTED_THRESHOLDS.length; i++) {

			for (int candidateThresholdCount = 0; candidateThresholdCount < curvePerfectForesight.length; candidateThresholdCount++) {
				if (Double.compare(curvePerfectForesight[candidateThresholdCount].getThresholdValue(),
						SELECTED_THRESHOLDS[i]) == 0) {
					curvePerfectForesight[candidateThresholdCount].setIsSelectedThresholdFromCandidateList(true);
				}
			}

		}

		// Extend chromosomes
		int extraChromosomCounter = Const.NUMBER_OF_SELECTED_THRESHOLDS;
		for (int thresholdChromosomeCount = 0; thresholdChromosomeCount < SELECTED_THRESHOLDS.length; thresholdChromosomeCount++) {

			for (int thresholdChromosomeCount1 = thresholdChromosomeCount
					+ 1; thresholdChromosomeCount1 < SELECTED_THRESHOLDS.length; thresholdChromosomeCount1++) {
				ThresholdChromosomMap.put(String.valueOf(
						SELECTED_THRESHOLDS[thresholdChromosomeCount] + SELECTED_THRESHOLDS[thresholdChromosomeCount1]),
						extraChromosomCounter);
				extraChromosomCounter++;
			}

		}

		/*
		 * Classification starts
		 */

		for (int i = 0; i < SELECTED_THRESHOLDS.length; i++) {
			String thresholdStr = String.format("%.8f", SELECTED_THRESHOLDS[i]);

			int arrayLength = trainingEventsArray.get(thresholdStr).length;
			Arrays.copyOf(trainingEventsArray.get(thresholdStr), arrayLength);
			copiedArray = Arrays.copyOf(trainingEventsArray.get(thresholdStr), arrayLength);
			// trainingEventsArray.put(thresholdStr, copiedArray);
			preprocess[i] = null;
			preprocess[i] = new PreProcess(SELECTED_THRESHOLDS[i], filename, "GP");

			preprocess[i].buildTraining(copiedArray);
			preprocess[i].selectBestClassifier();

			// loadTrainingData load classification data "order is
			// important"
			preprocess[i].loadTrainingData(copiedArray);

			preprocess[i].lastTrainingEvent = copiedArray[copiedArray.length - 1];

			preprocess[i].runAutoWeka();

		}

		// For debuging
		log.save("Training_Returns_SingleThreshold_" + Const.hashFunctionTypeToString(Const.OsFunctionEnum) + ".txt",
				"Filename\tThreshold1\tThreshold2\tThreshold3\tThreshold4\tThreshold5");

		log.save(
				"Training_SharpeRatio_SingleThreshold_" + Const.hashFunctionTypeToString(Const.OsFunctionEnum) + ".txt",
				"Filename\tThreshold1\tThreshold2\tThreshold3\tThreshold4\tThreshold5");

		log.save("Training_MDD_SingleThreshold_" + Const.hashFunctionTypeToString(Const.OsFunctionEnum) + ".txt",
				"Filename\tThreshold1\tThreshold2\tThreshold3\tThreshold4\tThreshold5");
		
		log.save("Training_dataset.txt","DCstart, DCend, DCtype, DCValue, OSstart ,OSEnd, OSType");
		log.save("Test_dataset.txt","DCstart, DCend, DCtype, DCValue, OSstart ,OSEnd, OSType");

		
		double[] singlethresholdReturns = new double[SELECTED_THRESHOLDS.length];
		double[] singlethresholdSharpeRatio = new double[SELECTED_THRESHOLDS.length];
		double[] singlethresholdMddRatio = new double[SELECTED_THRESHOLDS.length];

		for (int i = 0; i < SELECTED_THRESHOLDS.length; i++) { // The arrays
																// all have
																// same size

			curveClassifcation[i] = new DCCurveClassification();
			curvePerfectForesight_Selected[i] = new DCCurvePerfectForesight();

			String thresholdStr = String.format("%.8f", SELECTED_THRESHOLDS[i]);
			String gpFileNamePrefix = gpDaysMap.get(trainingIndexStart);
			String gpFileName = gpFileNamePrefix + "_" + thresholdStr + ".txt";

			// System.out.println(thresholdStr);

			// System.out.println("GA:" + gpFileNamePrefix);

			curveClassifcation[i].filename = filename;
			curvePerfectForesight_Selected[i].filename = filename;
			// Assign perfect foresight regression Model here

			for (int thresholdCounter = 0; thresholdCounter < curvePerfectForesight.length; thresholdCounter++) {
				String thisThresholdStr = String.format("%.8f", SELECTED_THRESHOLDS[i]);
				if (thisThresholdStr.equalsIgnoreCase(curvePerfectForesight[thresholdCounter].getThresholdString())) {

					curveClassifcation[i]
							.setThresholdValue(curvePerfectForesight[thresholdCounter].getThresholdValue());
					curveClassifcation[i].bestDownWardEventTree = curvePerfectForesight[thresholdCounter].bestDownWardEventTree
							.clone();
					curveClassifcation[i].bestUpWardEventTree = curvePerfectForesight[thresholdCounter].bestUpWardEventTree
							.clone();

					trainingGPTrees[i][0] = curvePerfectForesight[i].bestDownWardEventTree.clone(); // downward
					trainingGPTrees[i][1] = curvePerfectForesight[i].bestUpWardEventTree.clone(); // upward

					curveClassifcation[i].build(training, SELECTED_THRESHOLDS[i], gpFileName,
							trainingEventsArray.get(thresholdStr), trainingOutputArray.get(thresholdStr),
							preprocess[i]);
					curveClassifcation[i].setMarketdataListTraining(trainingDataPtCount);
					curveClassifcation[i].setMarketdataListTest(testDataPtCount);

					curvePerfectForesight_Selected[i]
							.setThresholdValue(curvePerfectForesight[thresholdCounter].getThresholdValue());
					curvePerfectForesight_Selected[i].bestDownWardEventTree = curvePerfectForesight[thresholdCounter].bestDownWardEventTree
							.clone();
					curvePerfectForesight_Selected[i].bestUpWardEventTree = curvePerfectForesight[thresholdCounter].bestUpWardEventTree
							.clone();

					curvePerfectForesight_Selected[i].build(training, SELECTED_THRESHOLDS[i], gpFileName,
							trainingEventsArray.get(thresholdStr), trainingOutputArray.get(thresholdStr), null);
					curvePerfectForesight_Selected[i].setMarketdataListTraining(trainingDataPtCount);
					curvePerfectForesight_Selected[i].setMarketdataListTest(testDataPtCount);
					curvePerfectForesight_Selected[i].setIsSelectedThresholdFromCandidateList(true);
					curvePerfectForesight_Selected[i].gpprediction = Arrays.copyOf(
							curvePerfectForesight[thresholdCounter].gpprediction,
							curvePerfectForesight[thresholdCounter].gpprediction.length);
					curvePerfectForesight_Selected[i].orderArray = curvePerfectForesight[thresholdCounter].orderArray;
					curvePerfectForesight_Selected[i].estimateTrainingUsingOutputData(null);
					curvePerfectForesight_Selected[i]
							.setMySharpeRatio(traningSingleThresholdSharpRatio.get(SELECTED_THRESHOLDS[i]));

					double trainingReturn = curvePerfectForesight_Selected[i].trainingTrading(null);
					singlethresholdReturns[i] = ((trainingReturn - this.budget) / this.budget) * 100;
					singlethresholdSharpeRatio[i] = curvePerfectForesight_Selected[i].getSharpRatio();
					singlethresholdMddRatio[i] = curvePerfectForesight_Selected[i].getMaxMddBase();

					break;
				}
			}

			// curveClassifcation[i].estimateTraining(preprocess[i]);

		}
		singlethresholdTrainingReturnsStr = filename + "\t";
		singlethresholdTrainingSharpeRatioStr = filename + "\t";
		singlethresholdTrainingMDDStr = filename + "\t";
		for (int i = 0; i < SELECTED_THRESHOLDS.length; i++) {
			singlethresholdTrainingReturnsStr = singlethresholdTrainingReturnsStr + singlethresholdReturns[i] + "\t";
			/** For debuging **/

			singlethresholdTrainingSharpeRatioStr = singlethresholdTrainingSharpeRatioStr
					+ +singlethresholdSharpeRatio[i] + "\t";

			singlethresholdTrainingMDDStr = singlethresholdTrainingMDDStr + singlethresholdMddRatio[i] + "\t";

		}

		/*
		 * Prepare test set
		 */
		/**
		 * For debuging // log.save("Test_Returns_SingleThreshold_" +
		 * Const.hashFunctionTypeToString(Const.OsFunctionEnum) + ".txt", //
		 * "Filename\tThreshold1\tThreshold2\tThreshold3\tThreshold4\tThreshold5"
		 * );
		 */
		singlethresholdReturns = new double[SELECTED_THRESHOLDS.length];
		for (int testBuildCount = 0; testBuildCount < SELECTED_THRESHOLDS.length; testBuildCount++) {
			String thresholdStr = String.format("%.8f", SELECTED_THRESHOLDS[testBuildCount]);

			DCEventGenerator dCEventGenerator = new DCEventGenerator();
			dCEventGenerator.generateEvents(this.test, SELECTED_THRESHOLDS[testBuildCount]);

			Event[] copiedTestArray = Arrays.copyOf(dCEventGenerator.getOutput(), dCEventGenerator.getOutput().length);

			testEventsArray.put(thresholdStr, copiedTestArray);

			preprocess[testBuildCount].lastTestingEvent = copiedTestArray[copiedTestArray.length - 1];

			preprocess[testBuildCount].buildTest(copiedTestArray);
			System.out.println("About to print test data for threshold " + SELECTED_THRESHOLDS[testBuildCount]);
			preprocess[testBuildCount].processTestData(copiedTestArray);

			preprocess[testBuildCount].loadTestData(copiedTestArray);

			preprocess[testBuildCount].classifyTestData();

			System.out.println("About to print test data for threshold " + SELECTED_THRESHOLDS[testBuildCount]);

			// Adesola note: i don't need this here because i won't use the
			// object for testing
			// curvePerfectForesight[testBuildCount].testbuild(training.length,
			// this.test,
			// SELECTED_THRESHOLDS[testBuildCount], copiedTestArray, null);

			curveClassifcation[testBuildCount].testbuild(training.length, this.test,
					SELECTED_THRESHOLDS[testBuildCount], copiedTestArray, preprocess[testBuildCount]);

			curveClassifcation[testBuildCount]
					.setMySharpeRatio(traningSingleThresholdSharpRatio.get(SELECTED_THRESHOLDS[testBuildCount]));
			double returns = curveClassifcation[testBuildCount].trade(preprocess[testBuildCount]);
			singlethresholdReturns[testBuildCount] = ((returns - this.budget) / this.budget) * 100;
			singlethresholdSharpeRatio[testBuildCount] = curveClassifcation[testBuildCount].getSharpRatio();
			singlethresholdMddRatio[testBuildCount] = curveClassifcation[testBuildCount].getMaxMddBase();

		} // testing GP

		singlethresholdTestReturnsStr = filename + "\t";
		singlethresholdTestSharpeRatioStr = filename + "\t";
		singlethresholdTestMDDStr = filename + "\t";
		for (int i = 0; i < SELECTED_THRESHOLDS.length; i++) {
			singlethresholdTestReturnsStr = singlethresholdTestReturnsStr + singlethresholdReturns[i] + "\t";

			singlethresholdTestSharpeRatioStr = singlethresholdTestSharpeRatioStr + +singlethresholdSharpeRatio[i]
					+ "\t";

			singlethresholdTestMDDStr = singlethresholdTestMDDStr + singlethresholdMddRatio[i] + "\t";

		}

		/**
		 * For debuging log.save("Test_Returns_SingleThreshold_" +
		 * Const.hashFunctionTypeToString(Const.OsFunctionEnum) + ".txt",
		 * filename+"\t"+singlethresholdReturns[0]+"\t"+singlethresholdReturns[1
		 * ]+"\t"+singlethresholdReturns[2]
		 * +"\t"+singlethresholdReturns[3]+"\t"+singlethresholdReturns[4]);
		 **/

		System.gc();

		for (int i = 0; i < SELECTED_THRESHOLDS.length; i++) {
			Map<Integer, Double> Threshold1EventLengthPredictionUp = new HashMap<Integer, Double>();
			Map<Integer, Double> Threshold1EventLengthPredictionDown = new HashMap<Integer, Double>();
			for (int j = 0; j < 100; j++) {
				Threshold1EventLengthPredictionUp.put(j, curvePerfectForesight_Selected[i].bestUpWardEventTree.eval(j));
				Threshold1EventLengthPredictionDown.put(j,
						curvePerfectForesight_Selected[i].bestDownWardEventTree.eval(j));
			}
			upGpcalc.put(SELECTED_THRESHOLDS[i], Threshold1EventLengthPredictionUp);
			downGpcalc.put(SELECTED_THRESHOLDS[i], Threshold1EventLengthPredictionDown);
		}

	}

	public void reBuild() {
		/*
		 * For rebuild GP model for (int thresholdCounter = 0; thresholdCounter
		 * < curvePerfectForesight_Selected.length; thresholdCounter++) { //
		 * reset trading info double threshold =
		 * curvePerfectForesight_Selected[thresholdCounter].thresholdValue;
		 * String filename =
		 * curvePerfectForesight_Selected[thresholdCounter].filename; Event[]
		 * copiedArray =
		 * Arrays.copyOf(curvePerfectForesight_Selected[thresholdCounter].
		 * trainingEvents,
		 * curvePerfectForesight_Selected[thresholdCounter].trainingEvents.
		 * length - 1); Event[] copiedOutputArray = Arrays.copyOf(
		 * curvePerfectForesight_Selected[thresholdCounter].
		 * trainingOutputEvents,
		 * curvePerfectForesight_Selected[thresholdCounter].trainingOutputEvents
		 * .length - 1);
		 * 
		 * curvePerfectForesight_Selected[thresholdCounter] = new
		 * DCCurvePerfectForesight();
		 * 
		 * String thresholdStr = String.format("%.8f", threshold);
		 * 
		 * DCEventGenerator dCEventGenerator = new DCEventGenerator();
		 * dCEventGenerator.generateEvents(training, threshold); copiedArray =
		 * Arrays.copyOf(dCEventGenerator.getEvents(),
		 * dCEventGenerator.getEvents().length);
		 * trainingEventsArray.put(thresholdStr, copiedArray);
		 * 
		 * copiedOutputArray = Arrays.copyOf(dCEventGenerator.getOutput(),
		 * dCEventGenerator.getOutput().length);
		 * trainingOutputArray.put(thresholdStr, copiedOutputArray);
		 * curvePerfectForesight_Selected[thresholdCounter].filename = filename;
		 * 
		 * curvePerfectForesight_Selected[thresholdCounter].build(training,
		 * threshold, "", copiedArray, copiedOutputArray, null);
		 * curvePerfectForesight_Selected[thresholdCounter].
		 * estimateTrainingUsingOutputData(null); // null // because // not
		 * doing // classification
		 * 
		 * // curvePerfectForesight[i].trainingOutputEvents
		 * curvePerfectForesight_Selected[thresholdCounter].
		 * setMarketdataListTraining(trainingDataPtCount);
		 * curvePerfectForesight_Selected[thresholdCounter].
		 * setMarketdataListTest(testDataPtCount);
		 * 
		 * curvePerfectForesight_Selected[thresholdCounter].
		 * estimateTrainingUnderPerfectforesight();
		 * 
		 * curvePerfectForesight_Selected[thresholdCounter].trainingTrading(null
		 * );
		 * 
		 * curvePerfectForesight_Selected[thresholdCounter].setIsPositionOpen(
		 * false);
		 * curvePerfectForesight_Selected[thresholdCounter].setAssociatedWeight(
		 * 0.0);
		 * curvePerfectForesight_Selected[thresholdCounter].setOpeningPosition(0
		 * .0); curvePerfectForesight_Selected[thresholdCounter].
		 * setMarketdataListTraining(trainingDataPtCount);
		 * 
		 * curvePerfectForesight_Selected[thresholdCounter].
		 * setIsSelectedThresholdFromCandidateList(true);
		 * curvePerfectForesight_Selected[thresholdCounter].
		 * estimateTrainingUsingOutputData(null); Map<Integer, Double>
		 * Threshold1EventLengthPredictionUp = new HashMap<Integer, Double>();
		 * Map<Integer, Double> Threshold1EventLengthPredictionDown = new
		 * HashMap<Integer, Double>(); for (int j = 0; j < 100; j++) {
		 * Threshold1EventLengthPredictionUp.put(j,
		 * curvePerfectForesight_Selected[thresholdCounter].bestUpWardEventTree.
		 * eval(j)); Threshold1EventLengthPredictionDown.put(j,
		 * curvePerfectForesight_Selected[thresholdCounter].
		 * bestDownWardEventTree.eval(j)); }
		 * upGpcalc.put(SELECTED_THRESHOLDS[thresholdCounter],
		 * Threshold1EventLengthPredictionUp);
		 * downGpcalc.put(SELECTED_THRESHOLDS[thresholdCounter],
		 * Threshold1EventLengthPredictionDown);
		 * 
		 * curveClassifcation[thresholdCounter] = new DCCurveClassification();
		 * curveClassifcation[thresholdCounter].setIsPositionOpen(false);
		 * curveClassifcation[thresholdCounter].setAssociatedWeight(0.0);
		 * curveClassifcation[thresholdCounter].setOpeningPosition(0.0);
		 * 
		 * curveClassifcation[thresholdCounter].setThresholdValue(threshold);
		 * curveClassifcation[thresholdCounter].bestDownWardEventTree =
		 * curvePerfectForesight_Selected[thresholdCounter].
		 * bestDownWardEventTree .clone();
		 * curveClassifcation[thresholdCounter].bestUpWardEventTree =
		 * curvePerfectForesight_Selected[thresholdCounter].bestUpWardEventTree
		 * .clone();
		 * 
		 * curveClassifcation[thresholdCounter].build(training, threshold, "",
		 * trainingEventsArray.get(thresholdStr),
		 * trainingOutputArray.get(thresholdStr), preprocess[thresholdCounter]);
		 * curveClassifcation[thresholdCounter].setMarketdataListTraining(
		 * trainingDataPtCount);
		 * curveClassifcation[thresholdCounter].setMarketdataListTest(
		 * testDataPtCount);
		 * 
		 * dCEventGenerator = new DCEventGenerator();
		 * dCEventGenerator.generateEvents(this.test,
		 * SELECTED_THRESHOLDS[thresholdCounter]);
		 * 
		 * Event[] copiedTestArray = Arrays.copyOf(dCEventGenerator.getOutput(),
		 * dCEventGenerator.getOutput().length);
		 * 
		 * curveClassifcation[thresholdCounter].testbuild(training.length,
		 * this.test, SELECTED_THRESHOLDS[thresholdCounter], copiedTestArray,
		 * preprocess[thresholdCounter]);
		 * 
		 * 
		 * } System.out.println("Done");
		 */
	}

	public Fitness run(long seed, int currentRun) {
		// if (seed == 0) {
		// seed = System.currentTimeMillis();
		// }

		// random = new Random(seed);
		random = new Random();
		System.out.println("Starting GA_new...");
		System.out.println(String.format("Random seed: %d", seed));
		System.out.println("Training budget: " + budget);
		System.out.println("Test budget: " + budget);
		System.out.println();

		/**
		 * initialise population, i.e. pass random weights to each individual
		 **/
		initialisePop();

		System.out.println("Generation\tBest\tWorst\tAverage");
		log.save("Logger.txt", "Generation\tBest\tWorst\tAverage");
		// MAX_GENERATIONS
		for (int t = 0; t < MAX_GENERATIONS; t++) {
			currentGeneration = t;

			/** fitness evaluation **/
			Fitness[] fitness = popFitnessEvaluation();

			/** elitism **/
			for (int j = 0; j < pop[0].length; j++)// all columns of pop have
													// the same length, i.e.
													// THRESHOLD.length+5; so it
													// doesn't matter if I say
													// pop[0], or pop[50] etc.
			{

				newPop[0][j] = pop[argBestFitness][j];
			}

			if (Double.compare(fitness[argBestFitness].value, fitness[0].value) > 0)
				System.out.println("Previous best fitnes " + fitness[0].value + " Currentbest fitness "
						+ fitness[argBestFitness].value);

			// int elitismCounter = 0;
			/*
			 * for (Entry<Double, Double> entry : bestFitnessList) {
			 * 
			 * for (int j = 0; j < pop[elitismCounter].length; j++) {
			 * newPop[elitismCounter][j] = pop[entry.getKey().intValue()][j]; }
			 * elitismCounter++; }
			 */

			report(t, fitness);

			/** tournament selection and crossover **/
			for (int p = 1; p < POPSIZE; p++)// 1 because of
												// elitism
			{
				// select first
				int first = tournament(fitness);

				// select second
				int second = tournament(fitness);

				if (p == 0)
					System.out.println("Individual 0 will be updated");
				switch (xoverOperatorIndex) {
				case 0:
					newPop[p] = crossover(first, second);
					break;// uniform crossover
				case 1:
					newPop[p] = crossoverOnePoint(first, second);
					break;
				case 2:
					newPop[p] = crossoverArithmetical(first, second);
					break;
				case 3:
					newPop[p] = crossoverDiscrete(first, second);
					break;
				}

			} // end of going through population

			/** point mutation **/
			for (int p = 1; p < POPSIZE; p++)// 1 because of elitism
			{
				if (p == 0)
					System.out.println("Individual 0 will be updated");
				switch (mutOperatorIndex) {

				case 0:
					mutation(p);
					break;
				case 1:
					mutationNonUniform(p);
					break;
				}

			}

			/** copy new pop into old pop **/
			copyPopulation();

		} // end of generation loop

		double trainFitness = bestFitness;
		// testOutputMap.clear();
		/** fitness evaluation in the test set, of the best individual **/

		Fitness f1 = fitnesser(pop[argBestFitness], false); // For reporting

		log.save("Training_Returns_SingleThreshold_" + Const.hashFunctionTypeToString(Const.OsFunctionEnum) + ".txt",
				singlethresholdTrainingReturnsStr + "\t" + f1.Return + "\t" + numberOfEvenThresholds + "\t"
						+ totalEvents);

		log.save(
				"Training_SharpeRatio_SingleThreshold_" + Const.hashFunctionTypeToString(Const.OsFunctionEnum) + ".txt",
				singlethresholdTrainingSharpeRatioStr + "\t" + f1.sharpRatio);


		
		if (!isTrainingDataPrinted){
		for (Event e : curvePerfectForesight_SelectedForGA.trainingOutputEvents) {

			log.save("Training_dataset.txt", e.toString());
		}
		
		
		log.save("Training_TradePointGA.txt", curvePerfectForesight_SelectedForGA.DataSetInfoString);
	
		int thresholdInt = 0;
		for (int numberOfthresholds = 0 ;  numberOfthresholds< Const.NUMBER_OF_SELECTED_THRESHOLDS; numberOfthresholds++){
			thresholdInt++;
			log.save("Training_TradePointThreshold"+thresholdInt+".txt", curvePerfectForesight_Selected[numberOfthresholds].DataSetInfoString);
			
			
			for (Event e: curvePerfectForesight_Selected[numberOfthresholds].trainingOutputEvents) {
				
				log.save("Training_datasetThreshold"+thresholdInt+".txt", e.toString());
			}
		}
		
		
		isTrainingDataPrinted =  true;
		}
		
		log.save("Training_MDD_SingleThreshold_" + Const.hashFunctionTypeToString(Const.OsFunctionEnum) + ".txt",
				singlethresholdTrainingMDDStr + "\t" + f1.MDD);

		// fitness on training dc curves. //ADE replace fitness here
		Fitness f = fitnesser(pop[argBestFitness], true);

		log.save("SingleThresholdVsGAReturnComparisonTest.txt",
				singlethresholdTestReturnsStr + "\t" + f.Return + "\t" + numberOfEvenThresholds + "\t" + totalEvents);

		log.save("SingleThresholdVsGASharpeRatioComparisonTest.txt",
				singlethresholdTestSharpeRatioStr + "\t" + f.sharpRatio);

		log.save("SingleThresholdVsGAMDDComparisonTest.txt", singlethresholdTestMDDStr + "\t" + f.MDD);
		
		
		if (!isTestDataPrinted){
			for (Event e :  curveClassifcationForGA.testingEvents) {
	
				log.save("Test_dataset.txt",e.toString());
			}
			
			
			log.save("Test_TradePointGA.txt", curveClassifcationForGA.DataSetInfoString);
			
			int thresholdInt = 0;
			for (int numberOfthresholds = 0 ;  numberOfthresholds< Const.NUMBER_OF_SELECTED_THRESHOLDS; numberOfthresholds++){
				thresholdInt++;
				log.save("Test_TradePointThreshold"+thresholdInt+".txt", curveClassifcation[numberOfthresholds].DataSetInfoString);
				
				
				for (Event e: curveClassifcation[numberOfthresholds].testingEvents) {
					
					log.save("Test_datasetThreshold"+thresholdInt+".txt", e.toString());
				}
			}
			isTestDataPrinted= true;
		}

		double testFitness = f.value;// original fitness

		log.save("Fitness_" + Const.hashFunctionTypeToString(Const.OsFunctionEnum) + ".txt",
				trainFitness + "\t" + testFitness);

		return f;
	}

	public static double getRandomDoubleBetweenRange(double min, double max) {
		if (min >= max) {
			throw new IllegalArgumentException("getRandomDoubleBetweenRange: max must be greater than min");
		}
		double x = min + (max - min) * new Random().nextDouble();

		// double x = (Math.random(). *((max-min)+1))+min;
		return x;
	}

	/**
	 * Initialises the GA population.
	 * 
	 */
	protected void initialisePop() {

		for (int i = 0; i < Const.NUMBER_OF_SELECTED_THRESHOLDS; i++) {
			for (int j = 0; j < pop[0].length; j++){
				pop[i][j] = 0.0;
			}

		}
		for (int i = 0; i < Const.NUMBER_OF_SELECTED_THRESHOLDS; i++) {
			for (int j = 0; j < pop[0].length; j++) {
				if (i == j) {
					pop[i][j] = 0.1;
					break;
				}
				if (j ==  pop[0].length-4)
					pop[i][j]  = random.nextDouble();
			}

		}

		for (int i = Const.NUMBER_OF_SELECTED_THRESHOLDS; i < POPSIZE; i++) {

			for (int j = 0; j < pop[0].length; j++)// all columns of pop have
													// the same length, i.e.
													// THRESHOLD.length+5; so it
													// doesn't matter if I say
													// pop[0], or pop[50] etc.
			{

				pop[i][j] = random.nextDouble();
				//if (j == pop[0].length - 2){
				pop[i][j] =  getRandomDoubleBetweenRange(0.5, 1.0);
				//}

			}

		}
	}

	/** Ensure we always generate a positive quantity **/
	protected int generateQuantity(boolean shortSelling) {
		int quantity = -1;
		if (shortSelling == false) {

			while (quantity <= 0) {
				int max = 11;
				int min = 1;
				Random rand = new Random();
				quantity = rand.nextInt((max - min) + 1) + min;
				// quantity = 1 + (10 - 1) * new Random().nextInt();
				// //random.nextInt(MAX_QUANTITY);
			}
		} else {
			while (quantity < 0)// we don't set equal to 0, as we might want to
								// allow 0 quantity in order to disable
								// short-selling
				quantity = random.nextInt(MAX_SHORT_SELLING_QUANTITY);
		}
		return quantity;
	}

	/** Ensure we always generate a beta2 > beta **/
	protected double generateBeta2(double beta) {
		double beta2 = random.nextDouble();

		while (beta2 <= beta) {
			beta2 = random.nextDouble();
		}

		return beta2;
	}

	/**
	 * Calculates the fitness of all individuals in the population
	 * 
	 * @return Fitness The array of fitness for the population
	 */

	/**
	 * Calculates the fitness of all individuals in the population
	 * 
	 * @return Fitness The array of fitness for the population
	 */
	protected Fitness[] popFitnessEvaluation() {
		Fitness[] fitness = new Fitness[POPSIZE];
		bestFitness = Double.NEGATIVE_INFINITY;
		argBestFitness = Integer.MIN_VALUE;

		for (int p = 0; p < POPSIZE; p++) {
			fitness[p] = fitnesser(pop[p], false);
			while (true) {
				if (Double.compare(fitness[p].value, 0.0) < 0) {
					for (int q = 0; q < Const.NUMBER_OF_SELECTED_THRESHOLDS; q++) {
						pop[p][q] = random.nextDouble();
					}
					fitness[p] = fitnesser(pop[p], false);
				} else {
					break;
				}

			}

			if (Double.compare(fitness[p].value, bestFitness) > 0) {
				bestFitness = fitness[p].value;
				argBestFitness = p;
			}
		}
		/*
		 * if (Double.compare(fitness[0].value,fitness[argBestFitness].value )
		 * >0) { bestFitness = fitness[0].value; argBestFitness = 0; }
		 */

		if (argBestFitness > 4) {
			Fitness fitness1 = fitnesser(pop[0], false);
			Fitness fitness2 = fitnesser(pop[1], false);
			Fitness fitness3 = fitnesser(pop[2], false);
			Fitness fitness4 = fitnesser(pop[3], false);
			Fitness fitness5 = fitnesser(pop[4], false);
			System.out.println(fitness[0].value + " new best is " + fitness[argBestFitness].value
					+ " individual 1 fitness is " + fitness1.value + " individual 2 fitness is " + fitness2.value
					+ " individual 3 fitness is " + fitness3.value + " individual 4 fitness is " + fitness4.value
					+ " individual 5 fitness is " + fitness5.value);
		}

		//if (argBestFitness != 0) {
		//	System.out.println("popFitnessEvaluation: After. Previous  fitness is " + fitness[0].value + " new best is "
		//			+ fitness[argBestFitness].value);
		//}
		return fitness;
	}

	/**
	 * Copies the intermediate population (newPop) to the original population
	 * (pop)
	 */
	protected void copyPopulation() {
		for (int i = 0; i < POPSIZE; i++) {
			for (int j = 0; j < pop[0].length; j++)// all columns of pop have
													// the same length, i.e.
													// THRESHOLD.length+5; so it
													// doesn't matter if I say
													// pop[0], or pop[50] etc.
			{
				pop[i][j] = newPop[i][j];
			}
		}
	}

	/**
	 * Uniform Mutation
	 * 
	 * @param individual
	 *            The individual to be mutated
	 */
	protected void mutation(int individual) {
		if (random.nextDouble() < MUTATION_PROB) {
			for (int j = 0; j < pop[0].length; j++)// all columns of pop have
													// the same length, i.e.
													// THRESHOLD.length+5; so it
													// doesn't matter if I say
													// pop[0], or pop[50] etc.
			{
				if (random.nextDouble() > 0.5) {

					newPop[individual][j] = random.nextDouble();
				}
			}
		}
	}

	/**
	 * Non-uniform Mutation
	 * 
	 * @param individual
	 *            The individual to be mutated
	 */
	protected void mutationNonUniform(int individual) {
		if (random.nextDouble() < MUTATION_PROB) {

			for (int j = 0; j < pop[0].length; j++)// all columns of pop have
													// the same length, i.e.
													// THRESHOLD.length+5; so it
													// doesn't matter if I say
													// pop[0], or pop[50] etc.
			{
				if (random.nextDouble() > 0.5) {

					// all other cases go here, even the j=1 for beta.
					double a = 0;
					double b = 1;
					double r = getRandomNumber();
					double tau = random.nextDouble() > 0.5 ? 1 : 0;

					newPop[individual][j] = (tau == 1)
							? pop[individual][j] + (b - pop[individual][j])
									* (1 - Math.pow(r, 1 - (double) currentGeneration / MAX_GENERATIONS))
							: pop[individual][j] - (pop[individual][j] - a)
									* (1 - Math.pow(r, 1 - (double) currentGeneration / MAX_GENERATIONS));

					DecimalFormat df = new DecimalFormat("#.########");
					df.setRoundingMode(RoundingMode.CEILING);
					double d = Double.parseDouble(df.format(newPop[individual][j]));
					newPop[individual][j] = d;
				}
			}
		}
	}

	/**
	 * Tournament selection
	 * 
	 * 
	 * @param fitness
	 *            The fitness array of the population
	 * @return argSmallest The position/index of the individual winning the
	 *         tournament
	 */
	protected int tournament(Fitness[] fitness) {
		double smallest = Double.NEGATIVE_INFINITY;
		int argSmallest = Integer.MIN_VALUE;

		for (int i = 0; i < tournamentSize; i++) {
			int choice = 0;
			do {
				choice = (int) Math.floor(random.nextDouble() * (double) POPSIZE);
			} while (choice == 0);
			if (choice == 0)
				System.out.println(" best selected");

			double fit = fitness[choice].value;// original approach

			if (fit > smallest) {
				argSmallest = choice;
				smallest = fit;
			}
		}

		return argSmallest;

	}

	/**
	 * Uniform Crossover
	 * 
	 * @param first
	 *            The index of the first parent
	 * @param second
	 *            The index of the second parent
	 * 
	 * @return offspring The array of weights of the offspring
	 */
	protected double[] crossover(int first, int second) {

		double[] offspring = new double[pop[0].length];// all columns of pop
														// have the same length,
														// i.e.
														// THRESHOLD.length+5;
														// so it doesn't matter
														// if I say pop[0], or
														// pop[50] etc.

		if (random.nextDouble() < CROSSOVER_PROB) {
			for (int j = 0; j < offspring.length; j++) {
				offspring[j] = random.nextDouble() > 0.5 ? pop[first][j] : pop[second][j];

			}
		} else {
			for (int j = 0; j < offspring.length; j++) {
				offspring[j] = pop[first][j];
			}
		}

		return offspring;
	}

	/**
	 * One-point Crossover
	 * 
	 * @param first
	 *            The index of the first parent
	 * @param second
	 *            The index of the second parent
	 * 
	 * @return offspring The array of weights of the offspring
	 */
	protected double[] crossoverOnePoint(int first, int second) {

		double[] offspring = new double[pop[0].length];// all columns of pop
														// have the same length,
														// i.e.
														// THRESHOLD.length+5;
														// so it doesn't matter
														// if I say pop[0], or
														// pop[50] etc.
		int xoverPoint = random.nextInt(offspring.length);

		if (random.nextDouble() < CROSSOVER_PROB) {
			for (int j = 0; j < xoverPoint; j++) {
				offspring[j] = pop[first][j];

			}

			for (int j = xoverPoint; j < offspring.length; j++) {
				offspring[j] = pop[second][j];

			}

			if (offspring[2] <= offspring[1]) {// ensuring that offspring[2]
												// (beta2) is greater than beta
				offspring[2] = pop[first][2] > offspring[2] ? pop[first][2] : pop[second][2];

			}
		} else {
			for (int j = 0; j < offspring.length; j++) {
				offspring[j] = pop[first][j];
			}
		}

		return offspring;
	}

	/**
	 * Arithmetical Crossover
	 * 
	 * @param first
	 *            The index of the first parent
	 * @param second
	 *            The index of the second parent
	 * 
	 * @return offspring The array of weights of the offspring
	 */
	protected double[] crossoverArithmetical(int first, int second) {

		double[] offspring = new double[pop[0].length];// all columns of pop
														// have the same length,
														// i.e.
														// THRESHOLD.length+5;
														// so it doesn't matter
														// if I say pop[0], or
														// pop[50] etc.

		if (random.nextDouble() < CROSSOVER_PROB) {
			for (int j = 0; j < offspring.length; j++) {

				offspring[j] = 0.5 * pop[first][j] + 0.5 * pop[second][j];// obtaining
																			// the
																			// arithmetic
																			// mean
																			// of
																			// the
																			// two
																			// parents

			}
		} else {
			for (int j = 0; j < offspring.length; j++) {
				offspring[j] = pop[first][j];
			}
		}

		return offspring;
	}

	/**
	 * Discrete Crossover
	 * 
	 * @param first
	 *            The index of the first parent
	 * @param second
	 *            The index of the second parent
	 * 
	 * @return offspring The array of weights of the offspring
	 */
	protected double[] crossoverDiscrete(int first, int second) {

		double[] offspring = new double[pop[0].length];// all columns of pop
														// have the same length,
														// i.e.
														// THRESHOLD.length+5;
														// so it doesn't matter
														// if I say pop[0], or
														// pop[50] etc.

		if (random.nextDouble() < CROSSOVER_PROB) {

			double randCrossOver = random.nextDouble();
			if (Double.compare(randCrossOver, 0.8) < 0) {
				for (int j = 0; j < offspring.length; j++) {
					double cmin = pop[first][j] < pop[second][j] ? pop[first][j] : pop[second][j];
					double cmax = pop[first][j] > pop[second][j] ? pop[first][j] : pop[second][j];

					offspring[j] = random.nextDouble() * (cmax - cmin) + cmin;// rnd
																				// number
																				// in
																				// the
																				// range
																				// [cmin,
																				// cmax]

				}
			} else {
				if (Double.compare(randCrossOver, 0.9) < 0) {
					for (int j = 0; j < offspring.length; j++) {
						offspring[j] = 0.5 * pop[first][j] + 0.5 * pop[second][j];
					}
				} else
					return crossover(first, second);
			}

		} else {
			for (int j = 0; j < offspring.length; j++) {
				offspring[j] = pop[first][j];
			}
		}

		return offspring;
	}

	protected void report(int generation, Fitness[] fitness) {
		double best = Double.NEGATIVE_INFINITY;
		double worst = Double.MAX_VALUE;
		double average = 0.0;
		int bestIndividualIndex = 0;

		for (int i = 0; i < POPSIZE; i++) {
			// ORIGINAL FITNESS
			if (fitness[i].value > best) {
				best = fitness[i].value;
				bestIndividualIndex = i;
			}

			if (fitness[i].value < worst) {
				worst = fitness[i].value;
			}

			average += fitness[i].value;

		}

		average = average / fitness.length;

		System.out.println(String.format("%d\t%12.6f\t%12.6f\t%12.6f", generation, best, worst, average));
		log.save("Logger.txt", String.format("%d\t%12.6f\t%12.6f\t%12.6f", generation, best, worst, average));
		if (generation == MAX_GENERATIONS - 1)
			System.out.println("Number of transactions of best individual in training: "
					+ (fitness[bestIndividualIndex].noOfTransactions
							+ fitness[bestIndividualIndex].noOfShortSellingTransactions));
	}

	public void saveResults(Fitness f, int i) {
		// Saving to Results.txt file
		log.save("Results_" + Const.hashFunctionTypeToString(Const.OsFunctionEnum) + ".txt",
				String.format("Run " + i + "\t%10.6f\t%10.6f\t%10.6f\t%10.6f\t%10.6f\t%10.6f\t%d\t%d", f.wealth,
						f.Return, f.value, f.realisedProfit, Math.abs(f.MDD), f.sharpRatio, f.noOfTransactions,
						f.noOfShortSellingTransactions));// saving
		// and
		// reporting
		// for (int transactionCount = 0; transactionCount <
		// f.tradingClass.boughtTradeList.size(); transactionCount++) // the
		// log.save("IndicatorTransactions_" +
		// Const.hashFunctionTypeToString(Const.OsFunctionEnum) + ".txt",
		// String.format("Run " + i + "\t%10.6f",
		// f.tradingClass.boughtTradeList.get(transactionCount))); // fitness,

		System.out.println();
		System.out.println(String.format("Fitness on test set: %10.6f", f.value));
		System.out.println(String.format("Realised profit on test set: %10.6f", f.realisedProfit));
		System.out.println(String.format("MDD test set: %10.6f", f.MDD));
		System.out.println(String.format("Sharpe Ratio test set: %10.6f", f.sharpRatio));
		System.out.println(String.format("Wealth on test set: %10.6f", f.wealth));
		System.out.println(String.format("Return on test set: %10.6f", f.Return));
		System.out.println(String.format("Unsuccessful buys: %d", f.uBuy));
		System.out.println(String.format("Unsuccessful sells: %d", f.uSell));
		System.out.println(String.format("No-ops: %d", f.noop));
		System.out.println(String.format("No of transactions: %d", f.noOfTransactions));
		System.out.println(String.format("No of short-selling transactions: %d", f.noOfShortSellingTransactions));
		System.out.println();
		System.out.println(">>>>> Solution:\n");

		System.out.println("Threshold weights: ");

		for (int m = 0; m < SELECTED_THRESHOLDS.length; m++) {
			System.out.println(String.format("%1.3f%%: %7.6f", SELECTED_THRESHOLDS[m] * 100, pop[argBestFitness][m]));// +5,
			// coz
			// the
			// first
			// 5
			// are
			// quantities
			// and
			// the
			// betas
			log.save("Solutions_" + Const.hashFunctionTypeToString(Const.OsFunctionEnum) + ".txt",
					String.format("%1.3f%%: %7.6f", SELECTED_THRESHOLDS[m] * 100, pop[argBestFitness][m]));
		}
	}

	public void cleanUpTempFiles() {

		for (int reportCount = 0; reportCount < SELECTED_THRESHOLDS.length; reportCount++) {
			if (preprocess[reportCount] != null)
				preprocess[reportCount].removeTempFiles();
			// curveClassifcation
			String tempFolderName = preprocess[reportCount].tempFilePath.get(0).substring(0,
					preprocess[reportCount].tempFilePath.get(0).lastIndexOf(File.separator));

			File dir = new File(tempFolderName);
			if (!dir.isDirectory())
				continue;

			File[] tempFile = dir.getParentFile().listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.startsWith("autoweka" + filename);
				}
			});

			for (int tempFileCount = 0; tempFileCount < tempFile.length; tempFileCount++) {
				try {
					preprocess[reportCount].deleteDirectoryRecursionJava6(tempFile[tempFileCount]);
				} catch (IOException e) {

					System.out.println("Unable to delete one of the directory");
				}
			}
		}

	}

	public void reportRMSE() {
		log.delete("RegressionAnalysis_" + Const.hashFunctionTypeToString(Const.OsFunctionEnum) + ".txt");
		log.save("RegressionAnalysis_" + Const.hashFunctionTypeToString(Const.OsFunctionEnum) + ".txt",
				" Filename \t threshold \t RMSE");

		for (int j = 0; j < SELECTED_THRESHOLDS.length; j++) {

			// TODO how best to combine multiple RMSEs

		}
	}

	public void reportClassification() {

		// TODO decide how best to combine classification results

	}

	public static void main(String[] args) throws Exception {
		long seed = 0;

		if (args.length < 14) {
			System.out.println("usage: " + EventWriter.class.getName()
					+ " <file path:file name:training index start:training index end:test index start:test index end> + "
					+ "<popSize> <maxGens> <tournamentSize> <xoverProb> <mutProb> <thresholdIncrement> <noOfThresholds> <maxQuantity> + "
					+ "<budget> <shortSellingAllowance> <mddWeight> <xoverOperatorIndex> <mutOperatorIndex> <initialThreshold> [seed]");
			System.exit(1);
		} else if (args.length == 16) {
			seed = Long.parseLong(args[15]);
		}

		// Split the long parameter file , according to the delimiter
		String s[] = args[0].split(":");
		if (s.length < 6) {
			System.out.println(
					"Expect 6 parameters: <file path:file name:training index start:training index end:test index start:test index end>");
			System.exit(1);
		}

		Const.OsFunctionEnum = Const.hashFunctionType(s[6]);
		Const.optimisationSelectedThreshold = Const.optimisation_selected_threshold.eLongest;

		log = new Logger(s[1], s[3], s[4]);
		log.delete("Solutions_" + Const.hashFunctionTypeToString(Const.OsFunctionEnum) + ".txt");
		log.delete("Results_" + Const.hashFunctionTypeToString(Const.OsFunctionEnum) + ".txt");
		log.delete("Fitness_" + Const.hashFunctionTypeToString(Const.OsFunctionEnum) + ".txt");
		log.delete("Logger_" + Const.hashFunctionTypeToString(Const.OsFunctionEnum) + ".txt");
		log.delete("IndicatorTransactions_" + Const.hashFunctionTypeToString(Const.OsFunctionEnum) + ".txt");
		log.delete("Training_Returns_SingleThreshold_" + Const.hashFunctionTypeToString(Const.OsFunctionEnum) + ".txt");
		log.delete("Test_Returns_SingleThreshold_" + Const.hashFunctionTypeToString(Const.OsFunctionEnum) + ".txt");
		log.delete("SingleThresholdVsGAReturnComparisonTraining.txt");
		log.delete("SingleThresholdVsGAReturnComparisonTest.txt");
		log.delete("EventLengthComparioson.txt");
		log.delete("EventObjectComparioson.txt");
		log.delete("SingleThresholdVsGAReturnComparisonTest.txt");
		log.delete("Training_SharpeRatio_SingleThreshold_" + Const.hashFunctionTypeToString(Const.OsFunctionEnum)
				+ ".txt");
		log.delete("Training_MDD_SingleThreshold_" + Const.hashFunctionTypeToString(Const.OsFunctionEnum) + ".txt");
		log.delete("SingleThresholdVsGASharpeRatioComparisonTest.txt");
		log.delete("Test_dataset.txt");
		log.delete("Training_dataset.txt");
		log.delete("SingleThresholdVsGAMDDComparisonTest.txt");
		log.delete("Training_TradePointGA.txt");
		log.delete("Test_TradePointGA.txt");
		
		
		for (int numberOfthresholds = 1 ;  numberOfthresholds< Const.NUMBER_OF_SELECTED_THRESHOLDS; numberOfthresholds++){
			
			log.delete("Training_TradePointThreshold"+numberOfthresholds+".txt");
			log.delete("Test_TradePointThreshold"+numberOfthresholds+".txt");
			log.delete("Training_datasetThreshold"+numberOfthresholds+".txt");
			log.delete("Test_datasetThreshold"+numberOfthresholds+".txt");
			
		}

		System.out.println("Population size: " + args[1] + "\n" + "Generations: " + args[2] + "\n" + "Tournament: "
				+ args[3] + "\n" + "Crossover prob: " + args[4] + "\n" + "Mutation prob: " + args[5] + "\n"
				+ "Threshold increment: " + args[6] + "\n" + "Number of thresholds: " + args[7] + "\n"
				+ "Maximum quantity: " + args[8] + "\n" + "Budget: " + args[9] + "\n" + "Short selling allowance: "
				+ args[10] + "\n" + "MDD weight: " + args[11] + "\n" + "Xover operator index: " + args[12] + "\n"
				+ "Mutation operator index: " + args[13] + "\n" + "Initial threshold: " + args[14] + "\n" + "Seed: "
				+ seed);

		GA_new ga = new GA_new(s[0], Integer.parseInt(s[2]), Integer.parseInt(s[3]), Integer.parseInt(s[4]),
				Integer.parseInt(s[5]), Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]),
				Double.parseDouble(args[4]), Double.parseDouble(args[5]), Double.parseDouble(args[6]),
				Integer.parseInt(args[7]), Integer.parseInt(args[8]), Integer.parseInt(args[9]),
				Double.parseDouble(args[10]), Double.parseDouble(args[11]), Integer.parseInt(args[12]),
				Integer.parseInt(args[13]), Double.parseDouble(args[14]));

		System.out.println("Population size: " + args[1] + "\n" + "Generations: " + args[2] + "\n" + "Tournament: "
				+ args[3] + "\n" + "Crossover prob: " + args[4] + "\n" + "Mutation prob: " + args[5] + "\n"
				+ "Threshold increment: " + args[6] + "\n" + "Number of thresholds: " + args[7] + "\n"
				+ "Maximum quantity: " + args[8] + "\n" + "Budget: " + args[9] + "\n" + "Short selling allowance: "
				+ args[10] + "\n" + "MDD weight: " + args[11] + "\n" + "Xover operator index: " + args[12] + "\n"
				+ "Mutation operator index: " + args[13] + "\n" + "Initial threshold: " + args[14] + "\n" + "Seed: "
				+ seed);

		log.save("Results_" + Const.hashFunctionTypeToString(Const.OsFunctionEnum) + ".txt",
				"\tWealth\tReturn\tFitness\tRealised Profit\tMDD\tSharpRatio\tNoOfTransactions\tNoOfShortSellingTransactions");
		log.save("Fitness_" + Const.hashFunctionTypeToString(Const.OsFunctionEnum) + ".txt",
				"Train fitness\tTest fitness");

		log.save("IndicatorTransactions_" + Const.hashFunctionTypeToString(Const.OsFunctionEnum) + ".txt",
				"\tTransaction");
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
		LocalDateTime now = LocalDateTime.now();
		System.out.println(dtf.format(now) + " Starting run");
		// nRuns
		for (int i = 0; i < nRuns; i++) {
			System.out.println("=========================== Run " + i + "==========================");
			log.save("Logger_" + Const.hashFunctionTypeToString(Const.OsFunctionEnum) + ".txt",
					"\n=========================== Run " + i + "==========================");

			Fitness f = ga.run(seed, i);

			ga.saveResults(f, i);

			ga.reBuild();
			now = LocalDateTime.now();
			System.out.println(dtf.format(now) + " Completed run " + i);
		}

		ga.reportRMSE();

		if (Const.OsFunctionEnum == Const.function_code.eGP) {
			// ga.reportClassification();
			ga.cleanUpTempFiles();
		}

		log.save("Results_" + Const.hashFunctionTypeToString(Const.OsFunctionEnum) + ".txt",
				"\n\nTesting Budget\t" + budget);
	}

	public static class Fitness {
		public double value;

		public int uSell;

		public int uBuy;

		public int noop;

		public double realisedProfit;

		public double MDD;

		public double sharpRatio;

		public double Return;

		public double wealth;

		public int noOfTransactions;

		public int noOfShortSellingTransactions;

	}

	public static class TradingClass {

		double baseCurrencyAmount = 0.0;
		double quoteCurrencyAmount = 0.0;
		double previousBaseCurrencyAmount = 0.0;
		List<Double> soldTradeList = new ArrayList<Double>();
		List<Double> boughtTradeList = new ArrayList<Double>();
		boolean isOpenPosition = false;
		int baseCurrencyTransaction = 0;
		int quoteCurrencyTransaction = 0;
		double lastBidPrice = 0.0;
		int openPositionCount = 0;
	}

	void clearTradingDetails() {

		for (int i = 0; i < curveClassifcation.length; i++) { // The arrays
			curveClassifcation[i].clearSharpRatio();
			curveClassifcation[i].refreshMDD();

			curveClassifcation[i].resetNumberOfBaseCcyTransaction();
			curveClassifcation[i].clearPositionArrayBase();

			curveClassifcation[i].clearPredictedTrendString();
			curveClassifcation[i].clearActualTrendString();

		}

		for (int i = 0; i < curvePerfectForesight_Selected.length; i++) { // The
																			// arrays

			if (curvePerfectForesight_Selected[i].getIsSelectedThresholdFromCandidateList()) {
				curvePerfectForesight_Selected[i].clearSharpRatio();
				curvePerfectForesight_Selected[i].refreshMDD();

				curvePerfectForesight_Selected[i].resetNumberOfBaseCcyTransaction();
				curvePerfectForesight_Selected[i].clearPositionArrayBase();

				curvePerfectForesight_Selected[i].clearPredictedTrendString();
				curvePerfectForesight_Selected[i].clearActualTrendString();
			}
		}

	}

	public int getTradingPoint(int physicalTimeCounter, boolean test, Type eventType, Vector<Double> selectedThresholds,
			int datalength) {

		int tradingPoint = -1;
		int finalTradingPoint = -1;
		double bestPerformanceScore = Double.MAX_VALUE;
		double bestPerformanceScoreTemp = Double.MAX_VALUE;
		ArrayList<Integer> predictionArray = new ArrayList<Integer>();
		// DCCurveClassification[] selectedTestCurve = new
		// DCCurveClassification[selectedThresholds.size()];
		// DCCurvePerfectForesight[] selectedTrainingCurve = new
		// DCCurvePerfectForesight[selectedThresholds.size()];
		// PreProcess[] selectedTestClassifier = new
		// PreProcess[selectedThresholds.size()];
		if (test) {

			for (int thresholdsCount = 0; thresholdsCount < SELECTED_THRESHOLDS.length; thresholdsCount++) {
				for (int curveCounter = 0; curveCounter < selectedThresholds.size(); curveCounter++) {
					if (Double.compare(selectedThresholds.get(curveCounter),
							curveClassifcation[thresholdsCount].thresholdValue) == 0) {
						int dcEnd = curveClassifcation[thresholdsCount].testingEvents[physicalTimeCounter].end;
						int OsLength = (int) Math.ceil(
								curveClassifcation[thresholdsCount].predictionWithClassifier[physicalTimeCounter]);
						tradingPoint = dcEnd + OsLength;
						predictionArray.add(tradingPoint);
					}
				}
			}

			/*
			 * for (int thresholdsCount = 0; thresholdsCount <
			 * SELECTED_THRESHOLDS.length; thresholdsCount++) { for (int
			 * curveCounter = 0; curveCounter < selectedThresholds.size();
			 * curveCounter++) { if
			 * (Double.compare(selectedThresholds.get(curveCounter),
			 * curveClassifcation[thresholdsCount].thresholdValue) == 0) {
			 * selectedTestCurve[curveCounter] =
			 * curveClassifcation[thresholdsCount]; break; } }
			 * 
			 * for (int curveCounterOuter = 0; curveCounterOuter <
			 * SELECTED_THRESHOLDS.length; curveCounterOuter++) { for (int
			 * curveCounter = 0; curveCounter < selectedThresholds.size();
			 * curveCounter++) { if
			 * (Double.compare(selectedThresholds.get(curveCounter),
			 * preprocess[curveCounterOuter].thresholdDbl) == 0) {
			 * selectedTestClassifier[curveCounter] =
			 * preprocess[thresholdsCount]; break; } } } }
			 * 
			 * if (Arrays.asList(selectedTestCurve).subList(0,
			 * selectedTestCurve.length).contains(null) ||
			 * Arrays.asList(selectedTestClassifier).subList(0,
			 * selectedTestCurve.length).contains(null)) { System.out.println(
			 * "Unable to find dccurve or classifier returning"); return -1; }
			 */

		} else {
			for (int thresholdsCount = 0; thresholdsCount < SELECTED_THRESHOLDS.length; thresholdsCount++) {
				for (int curveCounter = 0; curveCounter < selectedThresholds.size(); curveCounter++) {
					if (Double.compare(selectedThresholds.get(curveCounter),
							curvePerfectForesight_Selected[thresholdsCount].thresholdValue) == 0) {
						int dcEnd = curvePerfectForesight_Selected[thresholdsCount].trainingOutputEvents[physicalTimeCounter].end;
						int OsLength = (int) Math.round(
								curvePerfectForesight_Selected[thresholdsCount].gpprediction[physicalTimeCounter]);
						if (OsLength > 0) {
							tradingPoint = dcEnd + OsLength;
							predictionArray.add(tradingPoint);
						}
						break;
					}
				}
			}
		}

		if (predictionArray.size() < 1)
			return 0;

		Collections.sort(predictionArray);
		if (Const.optimisationSelectedThreshold == Const.optimisation_selected_threshold.eMedian) {

			if (predictionArray.size() == 1)
				finalTradingPoint = predictionArray.get(0);
			else if (predictionArray.size() == 2)
				finalTradingPoint = (int) (((double) predictionArray.get(0) + (double) predictionArray.get(1)) / 2);
			else if (predictionArray.size() == 3)
				finalTradingPoint = predictionArray.get(1);
			else if (predictionArray.size() == 4)
				finalTradingPoint = (int) (((double) predictionArray.get(1) + (double) predictionArray.get(2)) / 2);
			else if (predictionArray.size() == 5)
				finalTradingPoint = predictionArray.get(2);
			else
				finalTradingPoint = -1;
		} else if (Const.OsFunctionEnum == Const.function_code.eGP
				&& Const.optimisationSelectedThreshold == Const.optimisation_selected_threshold.eperformanceScore) {

		} else if (Const.optimisationSelectedThreshold == Const.optimisation_selected_threshold.eLongest) {
			finalTradingPoint = predictionArray.get(predictionArray.size() - 1);
		}

		return finalTradingPoint;
	}

	Fitness fitnesser(double[] individual, boolean test) {

		double sellCount = 0.0;
		double buyCount = 0.0;

		// double[] individualTemp = new double[individual.length];

		if (test) {
			numberOfEvenThresholds = 0;
			curveClassifcationForGA = new DCCurveClassification();

			curveClassifcationForGA.setIsPositionOpen(false);
			curveClassifcationForGA.setAssociatedWeight(0.0);
			curveClassifcationForGA.setOpeningPosition(budget);

			curveClassifcationForGA.setThresholdValue(-1);
			curveClassifcationForGA.setMarketdataListTraining(trainingDataPtCount);
			curveClassifcationForGA.setMarketdataListTest(testDataPtCount);
		} else {
			numberOfEvenThresholds = 0;
			curvePerfectForesight_SelectedForGA = new DCCurvePerfectForesight();
			curvePerfectForesight_SelectedForGA.setAssociatedWeight(0.0);
			curvePerfectForesight_SelectedForGA.setOpeningPosition(budget);

			curvePerfectForesight_SelectedForGA.setThresholdValue(-1);
			curvePerfectForesight_SelectedForGA.setMarketdataListTraining(trainingDataPtCount);
			curvePerfectForesight_SelectedForGA.setMarketdataListTest(testDataPtCount);

		}

		Double[] data = (test ? this.test : this.training);
		
		totalEvents = data.length;
		
		if (test) {
			curveClassifcationForGA.predictionWithClassifier = new double[data.length];
			curveClassifcationForGA.testingEvents = new Event[data.length];
			curveClassifcationForGA.lastTrainingPrice = training.length;
		} else {
			curvePerfectForesight_SelectedForGA.trainingUsingOutputData = new double[data.length];
			curvePerfectForesight_SelectedForGA.trainingOutputEvents = new Event[data.length];
		}

		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
		LocalDateTime now = LocalDateTime.now();
		System.out.println("DC event creation Started :" + dtf.format(now));
		for (int m = 0; m < data.length; m++) {

			sellCount = 0.0;
			buyCount = 0.0;

			Map<Double, Event> predictionUpEventMap = new LinkedHashMap<Double, Event>();
			Map<Double, Event> predictionDownEventMap = new LinkedHashMap<Double, Event>();

			Map<Double, Double> predictionUpValueMap = new LinkedHashMap<Double, Double>();
			Map<Double, Double> predictionDownValueMap = new LinkedHashMap<Double, Double>();

			Map<Double, Orders> predictionUpOrderMap = new LinkedHashMap<Double, Orders>();
			Map<Double, Orders> predictionDownOrderMap = new LinkedHashMap<Double, Orders>();

			Map<Double, Double> sharpeRatioXweightUpMap = new LinkedHashMap<Double, Double>();
			Map<Double, Double> sharpeRatioXweightDownMap = new LinkedHashMap<Double, Double>();

			Map<Double, Double> thresholdweightUpMap = new LinkedHashMap<Double, Double>();
			Map<Double, Double> thresholdweightDownMap = new LinkedHashMap<Double, Double>();

			Vector<Double> selectedThresholdPredictionSell = new Vector<Double>();
			Vector<Double> selectedThresholdPredictionBuy = new Vector<Double>();

			Map<Double, Double> thresholdDCRunUpMap = new LinkedHashMap<Double, Double>();
			Map<Double, Double> thresholdDCRunDownMap = new LinkedHashMap<Double, Double>();

			for (int j = 0; j < SELECTED_THRESHOLDS.length; j++) {

				if (test) {

					Orders gpOrder = curveClassifcation[j].orderArray[m];

					Event dcEvent = new Event(gpOrder.dcEventStart, gpOrder.dcEventEnd, gpOrder.eventType);
					if (gpOrder.isOSevent) {
						dcEvent.hasOverShoot = "yes";
						dcEvent.overshoot = new Event(gpOrder.dcOsEventStart, gpOrder.dcOsEventEnd, gpOrder.eventType);
						if (gpOrder.dcOsEventStart == -1)
							System.out.println("I am here");
					}

					if (gpOrder.eventType == Type.Upturn) {
						sellCount = sellCount + individual[j];
							
						if (Double.compare(individual[j],0.0) >0){
							selectedThresholdPredictionSell.add(gpOrder.prediction);
	
							predictionUpEventMap.put(curveClassifcation[j].thresholdValue, dcEvent);
							predictionUpValueMap.put(curveClassifcation[j].thresholdValue, gpOrder.prediction);
							predictionUpOrderMap.put(curveClassifcation[j].thresholdValue, gpOrder);
							sharpeRatioXweightUpMap.put(curveClassifcation[j].thresholdValue,
									individual[j] * (curvePerfectForesight_Selected[j].getMySharpeRatio() / 100));
							thresholdweightUpMap.put(curveClassifcation[j].thresholdValue, individual[j]);
							thresholdDCRunUpMap.put(curveClassifcation[j].thresholdValue,
									dcEvent.length() + gpOrder.prediction);
						}

					} else if (gpOrder.eventType == Type.Downturn) {
						buyCount = buyCount + individual[j];

						if (Double.compare(individual[j],0.0) >0){
						selectedThresholdPredictionBuy.add(gpOrder.prediction);
	
							predictionDownEventMap.put(curveClassifcation[j].thresholdValue, dcEvent);
							predictionDownValueMap.put(curveClassifcation[j].thresholdValue, gpOrder.prediction);
							predictionDownOrderMap.put(curveClassifcation[j].thresholdValue, gpOrder);
							sharpeRatioXweightDownMap.put(curveClassifcation[j].thresholdValue,
									individual[j] * (curvePerfectForesight_Selected[j].getMySharpeRatio() / 100));
							thresholdweightDownMap.put(curveClassifcation[j].thresholdValue, individual[j]);
							thresholdDCRunDownMap.put(curveClassifcation[j].thresholdValue,
									dcEvent.length() + gpOrder.prediction);
						}
					}
				} else {

					Orders gpOrder = curvePerfectForesight_Selected[j].orderArray[m];

					Event dcEvent = new Event(gpOrder.dcEventStart, gpOrder.dcEventEnd, gpOrder.eventType);
					if (gpOrder.isOSevent) {
						dcEvent.hasOverShoot = "yes";
						dcEvent.overshoot = new Event(gpOrder.dcOsEventStart, gpOrder.dcOsEventEnd, gpOrder.eventType);
					}

					if (gpOrder.eventType == Type.Upturn) {
						sellCount = sellCount + individual[j];

						if (Double.compare(individual[j],0.0) >0){
							selectedThresholdPredictionSell.add(gpOrder.prediction);
	
							predictionUpEventMap.put(curvePerfectForesight_Selected[j].thresholdValue, dcEvent);
							predictionUpValueMap.put(curvePerfectForesight_Selected[j].thresholdValue, gpOrder.prediction);
							predictionUpOrderMap.put(curvePerfectForesight_Selected[j].thresholdValue, gpOrder);
							sharpeRatioXweightUpMap.put(curvePerfectForesight_Selected[j].thresholdValue,
									individual[j] * curvePerfectForesight_Selected[j].getMySharpeRatio());
							thresholdweightUpMap.put(curvePerfectForesight_Selected[j].thresholdValue, individual[j]);
							thresholdDCRunUpMap.put(curvePerfectForesight_Selected[j].thresholdValue,
									dcEvent.length() + gpOrder.prediction);
						}
					} else if (gpOrder.eventType == Type.Downturn) {
						buyCount = buyCount + individual[j];

						if (Double.compare(individual[j],0.0) >0){
							selectedThresholdPredictionBuy.add(gpOrder.prediction);
	
							predictionDownEventMap.put(curvePerfectForesight_Selected[j].thresholdValue, dcEvent);
							predictionDownValueMap.put(curvePerfectForesight_Selected[j].thresholdValue,
									gpOrder.prediction);
							predictionDownOrderMap.put(curvePerfectForesight_Selected[j].thresholdValue, gpOrder);
							sharpeRatioXweightDownMap.put(curvePerfectForesight_Selected[j].thresholdValue,
									individual[j] * (curvePerfectForesight_Selected[j].getMySharpeRatio() / 100));
							thresholdweightDownMap.put(curvePerfectForesight_Selected[j].thresholdValue, individual[j]);
							thresholdDCRunDownMap.put(curvePerfectForesight_Selected[j].thresholdValue,
									dcEvent.length() + gpOrder.prediction);
						}
					}
				}
			}
			
		
			
			Event selectedEvent = null;
			double osPredictedLength = 0;
			if (sellCount > buyCount) {
				selectedEvent = null;
				selectedEvent = getPredictatedEvent(thresholdDCRunUpMap, predictionUpValueMap, predictionUpEventMap,
						individual, Type.Upturn);
				if (selectedEvent.overshoot != null)
					osPredictedLength = selectedEvent.overshoot.length();
				else
					osPredictedLength = 0.0;

				if (test) {
					curveClassifcationForGA.predictionWithClassifier[m] = osPredictedLength;
					curveClassifcationForGA.testingEvents[m] = selectedEvent;
				} else {
					curvePerfectForesight_SelectedForGA.trainingUsingOutputData[m] = osPredictedLength;
					curvePerfectForesight_SelectedForGA.trainingOutputEvents[m] = selectedEvent;
				}

			} else {

				selectedEvent = null;
				selectedEvent = getPredictatedEvent(thresholdDCRunDownMap, predictionDownValueMap,
						predictionDownEventMap, individual, Type.Downturn);

				if (selectedEvent.overshoot != null)
					osPredictedLength = selectedEvent.overshoot.length();
				else
					osPredictedLength = 0.0;

				if (test) {

					curveClassifcationForGA.predictionWithClassifier[m] = osPredictedLength;
					curveClassifcationForGA.testingEvents[m] = selectedEvent;

				} else {
					curvePerfectForesight_SelectedForGA.trainingUsingOutputData[m] = osPredictedLength;
					curvePerfectForesight_SelectedForGA.trainingOutputEvents[m] = selectedEvent;
				}

			} // Sell and buy weight is equal hence skip

		} // end of data
		now = LocalDateTime.now();
		System.out.println("DC event creation ended :" + dtf.format(now));

		Fitness fitness = new Fitness();

		double gaSingleReturn = 0.0;
		double sharpeRatioValue = 0.0;
		double mdd = 0.0;
		int numOfTransactions = 0;

		if (test) {

			gaSingleReturn = curveClassifcationForGA.trade(null);
			sharpeRatioValue = curveClassifcationForGA.getSharpRatio();
			numOfTransactions = curveClassifcationForGA.getNumberOfBaseCcyTransactions();
			mdd = curveClassifcationForGA.getMaxMddBase();
			

		} else {
			//double single1 = curvePerfectForesight_Selected[0].trainingTradingGA(null, individual[individual.length-4]);
			//double single1a = curvePerfectForesight_Selected[0].trainingTrading(null);
			//double single2 = curvePerfectForesight_Selected[1].trainingTradingGA(null,  individual[individual.length-4]);
			//double single3 = curvePerfectForesight_Selected[2].trainingTradingGA(null, individual[individual.length-4]);

			//gaSingleReturn = curvePerfectForesight_SelectedForGA.trainingTradingGA(null,1.0);
			gaSingleReturn = curvePerfectForesight_SelectedForGA.trainingTrading(null);
			sharpeRatioValue = curvePerfectForesight_SelectedForGA.getSharpRatio();
			numOfTransactions = curvePerfectForesight_SelectedForGA.getNumberOfBaseCcyTransactions();
			mdd = curvePerfectForesight_SelectedForGA.getMaxMddBase();
		}

		fitness.realisedProfit = gaSingleReturn - budget;

		fitness.MDD = mdd;

		fitness.wealth = gaSingleReturn;// my wealth, at the end
										// of the
		// transaction period
		fitness.sharpRatio = sharpeRatioValue;

		fitness.Return = 100.0 * ((gaSingleReturn - budget) / budget);
		fitness.value = fitness.sharpRatio;

		fitness.noOfTransactions = numOfTransactions;
		fitness.noOfShortSellingTransactions = 0;

		// fitness.tradingClass = tradeClass;

		return fitness;
	}

	double getRandomNumber() {

		double num = random.nextDouble();
		while (num == Double.MAX_VALUE || num == Double.NEGATIVE_INFINITY || num == Double.POSITIVE_INFINITY
				|| num == Double.NaN || Double.compare(num, 0.0) < 0 || Double.isInfinite(num) || Double.isNaN(num)) {
			System.out.println("getRandomNumber : Is nan");
		}

		DecimalFormat df = new DecimalFormat("#.####");
		df.setRoundingMode(RoundingMode.CEILING);
		double d = Double.parseDouble(df.format(num));
		return d;
	}

	Event getPredictatedEvent(Map<Double, Double> predictionDCRunLengthMap, Map<Double, Double> predictionValueMap,
			Map<Double, Event> predictionEventMap, double[] individual, Type eventType) {

		if (predictionDCRunLengthMap.size() != predictionValueMap.size()
				&& predictionValueMap.size() != predictionEventMap.size()) {
			System.out.println("Maps are not equal ");
			System.exit(-1);
		}
		// individual.length - 2 is for selecting based on OS length or DC run
		// length
		// individual.length - 3 is for selecting based on on sorted or Unsorted
		// Sort by value
		Map<Double, Double> evaluatedMap = new LinkedHashMap<Double, Double>();
		;

		if (Double.compare(individual[individual.length - 3], 0.5) >= 0 && Double.compare(individual[individual.length - 2], 0.5) >= 0) {
			evaluatedMap = predictionValueMap.entrySet().stream().sorted(Map.Entry.comparingByValue())
					.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue(), (e1, e2) -> e2, LinkedHashMap::new));
		} else if (Double.compare(individual[individual.length - 3], 0.5) < 0 && Double.compare(individual[individual.length - 2], 0.5) >= 0) {
			evaluatedMap.putAll(predictionValueMap);
		} else if (Double.compare(individual[individual.length - 3], 0.5) >= 0 && Double.compare(individual[individual.length - 2], 0.5) < 0) {
			evaluatedMap = predictionDCRunLengthMap.entrySet().stream().sorted(Map.Entry.comparingByValue())
					.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue(), (e1, e2) -> e2, LinkedHashMap::new));
		} else if (Double.compare(individual[individual.length - 3], 0.5) < 0 && Double.compare(individual[individual.length - 2], 0.5) < 0) {
			evaluatedMap.putAll(predictionDCRunLengthMap);
		} else {
			System.out.print("Should never get here. investigates");
			System.exit(-1);
		}

		// indexesValue is the predicted overshoot length
		List<Double> indexesValue = new ArrayList<Double>(evaluatedMap.values());
		if (indexesValue.isEmpty() || indexesValue.isEmpty() || indexesValue == null ||
				evaluatedMap.size() == 0 || evaluatedMap.isEmpty() ||  evaluatedMap == null){
			System.out.print("Should never get here. investigates");
			System.exit(-1);
		}
		Collections.reverse(indexesValue); // Reorder to DC with OS in front
		double mostFrequent = HelperClass.mostFrequentElement(indexesValue);

		List<Double> indexesKey = new ArrayList<Double>(evaluatedMap.keySet());
		Collections.reverse(indexesKey);
		int eventIndex = -1;

		Event selectedEvent = null;
		if (predictionValueMap.size() == 0)
			System.exit(-1);
		else if (predictionValueMap.size() == 1) {
			eventIndex = 0;

			selectedEvent = predictionEventMap.get(indexesKey.get(eventIndex));

		} else if (predictionValueMap.size() == 2) {

			if (Double.compare(individual[individual.length - 1], 0.1) <= 0) { // min
				eventIndex = 0;
				Event event1 = predictionEventMap.get(indexesKey.get(0));

				selectedEvent = event1;
			} else if (Double.compare(individual[individual.length - 1], 0.2) <= 0) { // max
				Event event1 = predictionEventMap.get(indexesKey.get(1));

				selectedEvent = event1;
			} else if (Double.compare(individual[individual.length - 1], 0.3) <= 0) { // average

				Event event1 = predictionEventMap.get(indexesKey.get(0));
				Event event2 = predictionEventMap.get(indexesKey.get(1));
				Event eventNew = new Event(((event1.start + event2.start) / 2), ((event1.end + event2.end) / 2),
						eventType);

				selectedEvent = eventNew;
			} else if (Double.compare(individual[individual.length - 1], 0.4) <= 0) { // mode

				int start = 0;
				int end = 0;

				int numberOfElement = 0;
				if (Double.compare(individual[individual.length - 2], 0.5) < 0){
					
					for (Map.Entry<Double, Double> pair : predictionDCRunLengthMap.entrySet()) {
						
						if (Double.compare(pair.getValue(), mostFrequent) == 0) {
							Event event1 = predictionEventMap.get(pair.getKey());
							start += event1.start;
							end += event1.end;
	
							numberOfElement++;
						}
					}
				}
				else{
					for (Map.Entry<Double, Double> pair : predictionValueMap.entrySet()) {
	
						if (Double.compare(pair.getValue(), mostFrequent) == 0) {
							Event event1 = predictionEventMap.get(pair.getKey());
							start += event1.start;
							end += event1.end;
	
							numberOfElement++;
						}
					}
				}

				Event eventNew = new Event((start / numberOfElement), (end / numberOfElement), eventType);

				selectedEvent = eventNew;
			} else if (Double.compare(individual[individual.length - 1], 0.5) <= 0) { // median
				Event event1 = predictionEventMap.get(indexesKey.get(0));
				Event event2 = predictionEventMap.get(indexesKey.get(1));

				Event eventNew = new Event(((event1.start + event2.start) / 2), ((event1.end + event2.end) / 2),
						eventType);

				selectedEvent = eventNew;

			} else if (Double.compare(individual[individual.length - 1], 0.6) <= 0) { // first
				Event event1 = predictionEventMap.get(indexesKey.get(0));
				selectedEvent = event1;
			} else if (Double.compare(individual[individual.length - 1], 0.7) <= 0) { // second
				Event event1 = predictionEventMap.get(indexesKey.get(0));
				selectedEvent = event1;
			} else if (Double.compare(individual[individual.length - 1], 0.8) <= 0) { // third
				Event event1 = predictionEventMap.get(indexesKey.get(1));
				selectedEvent = event1;
			} else if (Double.compare(individual[individual.length - 1], 0.9) <= 0) { // fourth
				Event event1 = predictionEventMap.get(indexesKey.get(1));
				selectedEvent = event1;
			} else if (Double.compare(individual[individual.length - 1], 1.0) <= 0) { // fifth
				Event event1 = predictionEventMap.get(indexesKey.get(1));
				selectedEvent = event1;
			}

			numberOfEvenThresholds++;
		} else if (predictionValueMap.size() == 3) {
			if (Double.compare(individual[individual.length - 1], 0.1) <= 0) { // min
				eventIndex = 0;
				Event event1 = predictionEventMap.get(indexesKey.get(0));

				selectedEvent = event1;
			} else if (Double.compare(individual[individual.length - 1], 0.2) <= 0) { // max
				Event event1 = predictionEventMap.get(indexesKey.get(2));

				selectedEvent = event1;
			} else if (Double.compare(individual[individual.length - 1], 0.3) <= 0) { // average

				Event event1 = predictionEventMap.get(indexesKey.get(0));
				Event event2 = predictionEventMap.get(indexesKey.get(1));
				Event event3 = predictionEventMap.get(indexesKey.get(2));
				Event eventNew = new Event(((event1.start + event2.start + event3.start) / 3),
						((event1.end + event2.end + event3.end) / 3), eventType);

				selectedEvent = eventNew;
			} else if (Double.compare(individual[individual.length - 1], 0.4) <= 0) { // mode

				int start = 0;
				int end = 0;

				int numberOfElement = 0;
				if (Double.compare(individual[individual.length - 2], 0.5) < 0){
					
					for (Map.Entry<Double, Double> pair : predictionDCRunLengthMap.entrySet()) {
						
						if (Double.compare(pair.getValue(), mostFrequent) == 0) {
							Event event1 = predictionEventMap.get(pair.getKey());
							start += event1.start;
							end += event1.end;
	
							numberOfElement++;
						}
					}
				}
				else{
					for (Map.Entry<Double, Double> pair : predictionValueMap.entrySet()) {
	
						if (Double.compare(pair.getValue(), mostFrequent) == 0) {
							Event event1 = predictionEventMap.get(pair.getKey());
							start += event1.start;
							end += event1.end;
	
							numberOfElement++;
						}
					}
				}

				Event eventNew = new Event((start / numberOfElement), (end / numberOfElement), eventType);
				selectedEvent = eventNew;
			} else if (Double.compare(individual[individual.length - 1], 0.5) <= 0) { // median
				Event event1 = predictionEventMap.get(indexesKey.get(1));

				selectedEvent = event1;

			} else if (Double.compare(individual[individual.length - 1], 0.6) <= 0) { // first
				Event event1 = predictionEventMap.get(indexesKey.get(0));
				selectedEvent = event1;
			} else if (Double.compare(individual[individual.length - 1], 0.7) <= 0) { // second
				Event event1 = predictionEventMap.get(indexesKey.get(1));
				selectedEvent = event1;
			} else if (Double.compare(individual[individual.length - 1], 0.8) <= 0) { // third
				Event event1 = predictionEventMap.get(indexesKey.get(1));
				selectedEvent = event1;
			} else if (Double.compare(individual[individual.length - 1], 0.9) <= 0) { // fourth
				Event event1 = predictionEventMap.get(indexesKey.get(2));
				selectedEvent = event1;
			} else if (Double.compare(individual[individual.length - 1], 1.0) <= 0) { // fifth
				Event event1 = predictionEventMap.get(indexesKey.get(2));
				selectedEvent = event1;
			}

		} else if (predictionValueMap.size() == 4) {

			if (Double.compare(individual[individual.length - 1], 0.1) <= 0) { // min
				eventIndex = 0;
				Event event1 = predictionEventMap.get(indexesKey.get(0));

				selectedEvent = event1;
			} else if (Double.compare(individual[individual.length - 1], 0.2) <= 0) { // max
				Event event1 = predictionEventMap.get(indexesKey.get(3));

				selectedEvent = event1;
			} else if (Double.compare(individual[individual.length - 1], 0.3) <= 0) { // average

				Event event1 = predictionEventMap.get(indexesKey.get(0));
				Event event2 = predictionEventMap.get(indexesKey.get(1));
				Event event3 = predictionEventMap.get(indexesKey.get(2));
				Event event4 = predictionEventMap.get(indexesKey.get(3));
				Event eventNew = new Event(((event1.start + event2.start + event3.start + event4.start) / 4),
						((event1.end + event2.end + event3.end + event4.end) / 4), eventType);

				selectedEvent = eventNew;
			} else if (Double.compare(individual[individual.length - 1], 0.4) <= 0) { // mode

				int start = 0;
				int end = 0;

				int numberOfElement = 0;
				if (Double.compare(individual[individual.length - 2], 0.5) < 0){
					
					for (Map.Entry<Double, Double> pair : predictionDCRunLengthMap.entrySet()) {
						
						if (Double.compare(pair.getValue(), mostFrequent) == 0) {
							Event event1 = predictionEventMap.get(pair.getKey());
							start += event1.start;
							end += event1.end;
	
							numberOfElement++;
						}
					}
				}
				else{
					for (Map.Entry<Double, Double> pair : predictionValueMap.entrySet()) {
	
						if (Double.compare(pair.getValue(), mostFrequent) == 0) {
							Event event1 = predictionEventMap.get(pair.getKey());
							start += event1.start;
							end += event1.end;
	
							numberOfElement++;
						}
					}
				}

				Event eventNew = new Event((start / numberOfElement), (end / numberOfElement), eventType);
				selectedEvent = eventNew;
			} else if (Double.compare(individual[individual.length - 1], 0.5) <= 0) { // median
				Event event1 = predictionEventMap.get(indexesKey.get(1));
				Event event2 = predictionEventMap.get(indexesKey.get(2));
				Event eventNew = new Event(((event1.start + event2.start) / 2), ((event1.end + event2.end) / 2),
						eventType);

				selectedEvent = eventNew;

			} else if (Double.compare(individual[individual.length - 1], 0.6) <= 0) { // first
				Event event1 = predictionEventMap.get(indexesKey.get(0));
				selectedEvent = event1;
			} else if (Double.compare(individual[individual.length - 1], 0.7) <= 0) { // second
				Event event1 = predictionEventMap.get(indexesKey.get(1));
				selectedEvent = event1;
			} else if (Double.compare(individual[individual.length - 1], 0.8) <= 0) { // third
				Event event1 = predictionEventMap.get(indexesKey.get(2));
				selectedEvent = event1;
			} else if (Double.compare(individual[individual.length - 1], 0.9) <= 0) { // fourth
				Event event1 = predictionEventMap.get(indexesKey.get(2));
				selectedEvent = event1;
			} else if (Double.compare(individual[individual.length - 1], 1.0) <= 0) { // fifth
				Event event1 = predictionEventMap.get(indexesKey.get(3));
				selectedEvent = event1;
			}

			numberOfEvenThresholds++;

		} else if (predictionValueMap.size() == 5) {

			if (Double.compare(individual[individual.length - 1], 0.1) <= 0) { // min
				eventIndex = 0;
				Event event1 = predictionEventMap.get(indexesKey.get(0));

				selectedEvent = event1;
			} else if (Double.compare(individual[individual.length - 1], 0.2) <= 0) { // max
				Event event1 = predictionEventMap.get(indexesKey.get(4));

				selectedEvent = event1;
			} else if (Double.compare(individual[individual.length - 1], 0.3) <= 0) { // average

				Event event1 = predictionEventMap.get(indexesKey.get(0));
				Event event2 = predictionEventMap.get(indexesKey.get(1));
				Event event3 = predictionEventMap.get(indexesKey.get(2));
				Event event4 = predictionEventMap.get(indexesKey.get(3));
				Event event5 = predictionEventMap.get(indexesKey.get(4));
				Event eventNew = new Event(
						((event1.start + event2.start + event3.start + event4.start + event5.start) / 5),
						((event1.end + event2.end + event3.end + event4.end + event5.end) / 5), eventType);

				selectedEvent = eventNew;
			} else if (Double.compare(individual[individual.length - 1], 0.4) <= 0) { // mode

				int start = 0;
				int end = 0;
				int numberOfElement = 0;
				if (Double.compare(individual[individual.length - 2], 0.5) < 0){
					
					for (Map.Entry<Double, Double> pair : predictionDCRunLengthMap.entrySet()) {
						
						if (Double.compare(pair.getValue(), mostFrequent) == 0) {
							Event event1 = predictionEventMap.get(pair.getKey());
							start += event1.start;
							end += event1.end;
	
							numberOfElement++;
						}
					}
				}
				else{
					for (Map.Entry<Double, Double> pair : predictionValueMap.entrySet()) {
	
						if (Double.compare(pair.getValue(), mostFrequent) == 0) {
							Event event1 = predictionEventMap.get(pair.getKey());
							start += event1.start;
							end += event1.end;
	
							numberOfElement++;
						}
					}
				}

				Event eventNew = new Event((start / numberOfElement), (end / numberOfElement), eventType);
				selectedEvent = eventNew;
			} else if (Double.compare(individual[individual.length - 1], 0.5) <= 0) { // median
				Event event1 = predictionEventMap.get(indexesKey.get(2));

				selectedEvent = event1;

			} else if (Double.compare(individual[individual.length - 1], 0.6) <= 0) { // first
				Event event1 = predictionEventMap.get(indexesKey.get(0));
				selectedEvent = event1;
			} else if (Double.compare(individual[individual.length - 1], 0.7) <= 0) { // second
				Event event1 = predictionEventMap.get(indexesKey.get(1));
				selectedEvent = event1;
			} else if (Double.compare(individual[individual.length - 1], 0.8) <= 0) { // third
				Event event1 = predictionEventMap.get(indexesKey.get(2));
				selectedEvent = event1;
			} else if (Double.compare(individual[individual.length - 1], 0.9) <= 0) { // fourth
				Event event1 = predictionEventMap.get(indexesKey.get(3));
				selectedEvent = event1;
			} else if (Double.compare(individual[individual.length - 1], 1.0) <= 0) { // fifth
				Event event1 = predictionEventMap.get(indexesKey.get(4));
				selectedEvent = event1;
			}

		}

		return selectedEvent;
	}

}
