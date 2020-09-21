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

import org.apache.commons.math3.util.FastMath;

import java.util.Random;
import java.util.Vector;

import dc.EventWriter;
import dc.GP.AbstractNode;
import dc.GP.Const;
import dc.ga.DCCurve.Event;
import dc.ga.DCCurve.Type;
import dc.io.FReader;
import dc.io.Logger;
import dc.io.FReader.FileMember2;
import misc.DCCurveCifre;
import misc.DCCurveClassification;
import misc.DCCurveClassificationMF;
import misc.DCCurveClassificationOlsen;
import misc.DCCurveDCCOnlyAndTrail;
import misc.DCCurveMF;
import misc.DCCurveNoClassificationNoRegression;
import misc.DCCurveOlsen;
import misc.DCCurvePerfectForesight;
import misc.DCCurveRandomGP;
import misc.DCCurveRandomMF;
import misc.DCCurveRandomOlsen;
import misc.DCEventGenerator;
import misc.SimpleDrawDown;
import misc.SimpleSharpeRatio;
import weka.core.Instances;
import weka.core.matrix.Maths;
import dc.ga.PreProcess;

public class GA_new {
	int numberOfThresholds;
	protected static double[] THRESHOLDS;
	protected static double[] CANDIDATE_THRESHOLDS;
	double thresholdIncrement;
	String filename = "";
	int currentGeneration;

	Double[] training;
	Double[] test;
	
	
	DCCurvePerfectForesight[] curvePerfectForesight_Selected;
	DCCurveClassification[] curveClassifcation ;
	ConcurrentHashMap<Integer, String> gpDaysMap = null;
	Map<String, Event[]> trainingEventsArray = new LinkedHashMap<String, Event[]>();
	Map<String, Event[]> trainingOutputArray = new LinkedHashMap<String, Event[]>();
	Map<Double, Double> perfectForecastReturnMap = new HashMap<Double, Double>();
	double[] SELECTED_THRESHOLDS;
	PreProcess[] preprocess = null;
	int trainingDay = -1;
	Map<String, Event[]> testEventsArray = new LinkedHashMap<String, Event[]>();
	List<Entry<Double, Double>> greatest  =  new  ArrayList<Entry<Double, Double>> ();
	int trainingDataPtCount = 0;
	int testDataPtCount = 0;
	
	Double[][] trainingMeanRatio;
	
	AbstractNode[][] trainingGPTrees;

	// FReader st = new FReader();
	// FReader.FileMember2 TestDataObject = st.new FileMember2();

	List<FReader.FileMember2> trainingDataList = new ArrayList<FReader.FileMember2>();
	List<FReader.FileMember2> testDataList = new ArrayList<FReader.FileMember2>();
	static double[][] pop;
	double[][] newPop;

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

	
	Map<Double, Double> bestfitnessReturnMap = new HashMap<Double, Double>();
	List<Entry<Double, Double>> bestFitnessList  =  new ArrayList<Entry<Double, Double>>();;
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

		double initial = initialThreshold;// 0.01
		THRESHOLDS = new double[numberOfThresholds];
		CANDIDATE_THRESHOLDS  = new double[Const.NUMBER_OF_THRESHOLDS];
		trainingMeanRatio = new Double[THRESHOLDS.length][2];// There's only 2
																// ratios, one
		trainingDay = trainingIndexStart;									// for upward
																// and one for
																// downward OS
																// events.
		SELECTED_THRESHOLDS = new double[Const.NUMBER_OF_SELECTED_THRESHOLDS];

		trainingGPTrees = new AbstractNode[Const.NUMBER_OF_SELECTED_THRESHOLDS][2];// There's only 2
		this.filename = filename;
		// ratios, one
		// for upward
		// and one for
		// downward OS
		// events.
		for (int i = 0; i < THRESHOLDS.length; i++) {
			// THRESHOLDS[i] = (initial * (i + 1)) / 100.0;
			THRESHOLDS[i] = (initial + (thresholdIncrement * i)) / 100.0;
		}

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

		pop = new double[POPSIZE][THRESHOLDS.length + 2];// +5 because index 0
															// will be the
															// quantity, index 1
															// will be beta, and
															// index 2 will be
															// beta2, and index
															// 3 will be
															// shortSellingQuantity,
															// beta3
															// maximum number of positions
		newPop = new double[POPSIZE][THRESHOLDS.length + 2];

		nRuns = 50;

		slippageAllowance = 0.01/100 ; //0 / 100;// 0.01/100 the 0.01 is the proper cost

		System.out.println("Slippage allowance: " + slippageAllowance);

		bestTestFitness.value = Double.NEGATIVE_INFINITY;

		MAX_SHORT_SELLING_QUANTITY = 1;// set to 1 to disable short-selling,
										// because the only result when I do
										// random.nextInt(1) in the
										// generateQuantity() method is 0.

		System.out.println("Loading directional changes data...");

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
		trainingDataPtCount= counter;
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
		testDataPtCount= counter;
		int trainingDataSize = trainingDataList.size();
		for (int i = trainingDataSize; i < (counter + trainingDataSize); i++) {
			testDataList.add(FReader.dataRecordInFileArray.get(i));
		}
		// budget = 100000;

		DCCurvePerfectForesight[] curvePerfectForesight;
		curvePerfectForesight = new DCCurvePerfectForesight[Const.NUMBER_OF_THRESHOLDS];
		curveClassifcation = new DCCurveClassification[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		curvePerfectForesight_Selected = new DCCurvePerfectForesight[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		
		
		gpDaysMap = FReader.loadDataMap(filename);

		/*
		 * Candidate threshold generation*/
		for (int i = 0; i < Const.NUMBER_OF_THRESHOLDS; i++) {
			CANDIDATE_THRESHOLDS[i] = (0.005 + (0.0025 * i)) / 100.0;
			//String thresholdStr = String.format("%.8f", CANDIDATE_THRESHOLDS[i]);
			// System.out.println(thresholdStr);
		}
		Event[] copiedArray;
		Event[] copiedOutputArray;
		
		/*
		 * Regression starts
		 * */
		for (int i = 0; i <  Const.NUMBER_OF_THRESHOLDS; i++) { 

				Const.splitDatasetByTrendType = true;
				curvePerfectForesight[i] = new DCCurvePerfectForesight();
				
				
				String thresholdStr = String.format("%.8f", CANDIDATE_THRESHOLDS[i]);
				String gpFileNamePrefix = gpDaysMap.get(trainingIndexStart);
				String gpFileName = gpFileNamePrefix + "_" + thresholdStr + ".txt";
				
				DCEventGenerator dCEventGenerator = new DCEventGenerator();
				dCEventGenerator.generateEvents(training, CANDIDATE_THRESHOLDS[i]);
				copiedArray = Arrays.copyOf(dCEventGenerator.getEvents(), dCEventGenerator.getEvents().length);
				trainingEventsArray.put(thresholdStr, copiedArray);
				
				copiedOutputArray = Arrays.copyOf(dCEventGenerator.getOutput(), dCEventGenerator.getEvents().length);
				trainingOutputArray.put(thresholdStr, copiedOutputArray);
				
				if (copiedArray.length < 10)
				continue;
				
				curvePerfectForesight[i].filename = filename;
				curvePerfectForesight[i].build(training, CANDIDATE_THRESHOLDS[i], gpFileName, copiedArray, copiedOutputArray, null);
				curvePerfectForesight[i].estimateTrainingUsingOutputData(null); // null because
									// not doing
									// classification
				
				//curvePerfectForesight[i].trainingOutputEvents
				curvePerfectForesight[i].setMarketdataListTraining(trainingDataPtCount);
				curvePerfectForesight[i].setMarketdataListTest(testDataPtCount);
				double perfectForcastTrainingReturn = curvePerfectForesight[i].trainingTrading(null);
				perfectForecastReturnMap.put(CANDIDATE_THRESHOLDS[i], perfectForcastTrainingReturn);
				
		}

		
		
		greatest = HelperClass.findGreatest(perfectForecastReturnMap,
				Const.NUMBER_OF_SELECTED_THRESHOLDS); // best
		
		preprocess = new PreProcess[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		/*
		 * Select best thresholds
		 * */
		int tradingThresholdCount = 0;
		System.out.println("Selecting GP threshold");
		for (Entry<Double, Double> entry : greatest) {
			// System.out.println(entry);
			SELECTED_THRESHOLDS[tradingThresholdCount] = entry.getKey();
			System.out.println(SELECTED_THRESHOLDS[tradingThresholdCount]);
			
			tradingThresholdCount++;
		}
		
		/*
		 * update selected DCCurvePerfectForesight 
		 *  */

		for (int i = 0; i < SELECTED_THRESHOLDS.length; i++) {
			
			for (int candidateThresholdCount = 0 ;
					candidateThresholdCount < curvePerfectForesight.length ; 
					candidateThresholdCount++)
			{
				if (curvePerfectForesight[candidateThresholdCount].getThresholdValue() == SELECTED_THRESHOLDS[i] )
				{
					curvePerfectForesight[candidateThresholdCount].setIsSelectedThresholdFromCandidateList( true);
				}
			}
				
				
			
		}
		
		
		/*
		 * Classification starts
		 * */
		
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
			curvePerfectForesight_Selected[i].filename =  filename;
			// Assign perfect foresight regression Model here
			

			for (int thresholdCounter = 0; thresholdCounter < curvePerfectForesight.length; thresholdCounter++) {
				String thisThresholdStr = String.format("%.8f", SELECTED_THRESHOLDS[i]);
				if (thisThresholdStr
						.equalsIgnoreCase(curvePerfectForesight[thresholdCounter].getThresholdString())) {
					
					curveClassifcation[i].setThresholdValue( curvePerfectForesight[thresholdCounter].getThresholdValue());
					curveClassifcation[i].bestDownWardEventTree = curvePerfectForesight[thresholdCounter].bestDownWardEventTree.clone();
					curveClassifcation[i].bestUpWardEventTree = curvePerfectForesight[thresholdCounter].bestUpWardEventTree.clone();
					
					trainingGPTrees[i][0] = curvePerfectForesight[i].bestDownWardEventTree.clone(); // downward
					trainingGPTrees[i][1] = curvePerfectForesight[i].bestUpWardEventTree.clone(); // upward
			
					curveClassifcation[i].build(training, SELECTED_THRESHOLDS[i], gpFileName, trainingEventsArray.get(thresholdStr),
							trainingOutputArray.get(thresholdStr),
							preprocess[i]);
					curveClassifcation[i].setMarketdataListTraining(trainingDataPtCount);
					curveClassifcation[i].setMarketdataListTest(testDataPtCount);
					
					
					
					curvePerfectForesight_Selected[i].setThresholdValue( curvePerfectForesight[thresholdCounter].getThresholdValue());
					curvePerfectForesight_Selected[i].bestDownWardEventTree = curvePerfectForesight[thresholdCounter].bestDownWardEventTree.clone();
					curvePerfectForesight_Selected[i].bestUpWardEventTree = curvePerfectForesight[thresholdCounter].bestUpWardEventTree.clone();
					
					
					curvePerfectForesight_Selected[i].build(training, SELECTED_THRESHOLDS[i], gpFileName, trainingEventsArray.get(thresholdStr),
									trainingOutputArray.get(thresholdStr),null);
					curvePerfectForesight_Selected[i].setMarketdataListTraining(trainingDataPtCount);
					curvePerfectForesight_Selected[i].setMarketdataListTest(testDataPtCount);
					curvePerfectForesight_Selected[i].setIsSelectedThresholdFromCandidateList(true);
					
					break;
				}
			}
			
			
			
			
			//curveClassifcation[i].estimateTraining(preprocess[i]);
		} // for (int i = 0; i < SELECTED_THRESHOLDS.length; i++) {

		
		/*
		 * Prepare test set
		 * */
		
		for (int testBuildCount = 0; testBuildCount < SELECTED_THRESHOLDS.length; testBuildCount++) {
			String thresholdStr = String.format("%.8f", SELECTED_THRESHOLDS[testBuildCount]);

			DCEventGenerator dCEventGenerator = new DCEventGenerator();
			dCEventGenerator.generateEvents(this.test, SELECTED_THRESHOLDS[testBuildCount]);

			Event[] copiedTestArray = Arrays.copyOf(dCEventGenerator.getOutput(),
					dCEventGenerator.getOutput().length);

			testEventsArray.put(thresholdStr, copiedTestArray);

			preprocess[testBuildCount].lastTestingEvent = copiedTestArray[copiedTestArray.length - 1];

			preprocess[testBuildCount].buildTest(copiedTestArray);
			System.out.println("About to print test data for threshold " + SELECTED_THRESHOLDS[testBuildCount]);
			preprocess[testBuildCount].processTestData(copiedTestArray);

			preprocess[testBuildCount].loadTestData(copiedTestArray);

			preprocess[testBuildCount].classifyTestData();

			System.out.println("About to print test data for threshold " + SELECTED_THRESHOLDS[testBuildCount]);


			//Adesola note: i don't need this here because i won't use the object for testing
			//curvePerfectForesight[testBuildCount].testbuild(training.length, this.test,
			//		SELECTED_THRESHOLDS[testBuildCount], copiedTestArray, null);

			

			curveClassifcation[testBuildCount].testbuild(training.length, this.test,
					SELECTED_THRESHOLDS[testBuildCount], copiedTestArray, preprocess[testBuildCount]);
			
		} // testing GP
		
		
		/// ******/////
		
		

		System.gc();
	}

	public void reBuild() {
		
		

		for (int thresholdCounter = 0; thresholdCounter < curvePerfectForesight_Selected.length; thresholdCounter++)
		{
			String thisThresholdStr = String.format("%.8f", SELECTED_THRESHOLDS[thresholdCounter]);
			curvePerfectForesight_Selected[thresholdCounter] = new DCCurvePerfectForesight();
			curvePerfectForesight_Selected[thresholdCounter].filename =  filename;
			// Assign perfect foresight regression Model here
				curvePerfectForesight_Selected[thresholdCounter].setThresholdValue( SELECTED_THRESHOLDS[thresholdCounter]);
				curvePerfectForesight_Selected[thresholdCounter].bestDownWardEventTree = trainingGPTrees[thresholdCounter][0];
				curvePerfectForesight_Selected[thresholdCounter].bestUpWardEventTree = trainingGPTrees[thresholdCounter][1];
				

				curvePerfectForesight_Selected[thresholdCounter].build(training, SELECTED_THRESHOLDS[thresholdCounter], "", trainingEventsArray.get(thisThresholdStr),
								trainingOutputArray.get(thisThresholdStr),null);
				curvePerfectForesight_Selected[thresholdCounter].setMarketdataListTraining(trainingDataPtCount);
				curvePerfectForesight_Selected[thresholdCounter].setMarketdataListTest(testDataPtCount);
				curvePerfectForesight_Selected[thresholdCounter].setIsSelectedThresholdFromCandidateList(true);
			}
		}
	

	public Fitness run(long seed, int currentRun) {
		//if (seed == 0) {
	//		seed = System.currentTimeMillis();
	//	}

		//random = new Random(seed);
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
		log.save("Logger_"+Const.hashFunctionTypeToString(Const.OsFunctionEnum)+".txt", "Generation\tBest\tWorst\tAverage");
		//MAX_GENERATIONS
		for (int t = 0; t <MAX_GENERATIONS ; t++) {
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
			
			
			
			int elitismCounter = 0;
			for (Entry<Double, Double> entry : bestFitnessList) {
			
				for (int j = 0; j < pop[elitismCounter].length; j++){
					newPop[elitismCounter][j] = pop[entry.getKey().intValue()][j];
				}
				elitismCounter++;
			}


			report(t, fitness);
			
			/** tournament selection and crossover **/
			for (int p = Const.ELISTISM_COUNT; p < POPSIZE; p++)// 1 because of elitism
			{
				// select first
				int first = tournament(fitness);

				// select second
				int second = tournament(fitness);

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

		/** fitness evaluation **/
		popFitnessEvaluation();

		double trainFitness = bestFitness;
		// testOutputMap.clear();
		/** fitness evaluation in the test set, of the best individual **/


		// fitness on training dc curves.
		Fitness f = fitness(pop[argBestFitness], true);

		double testFitness = f.value;// original fitness

		log.save("Fitness_"+Const.hashFunctionTypeToString(Const.OsFunctionEnum)+".txt", trainFitness + "\t" + testFitness);

		return f;
	}
	
	

	public static double getRandomDoubleBetweenRange(double min, double max){
		if (min >= max) {
			throw new IllegalArgumentException("getRandomDoubleBetweenRange: max must be greater than min");
		}
		double x  = min + (max - min) * new Random().nextDouble();
	
    	//double x = (Math.random(). *((max-min)+1))+min;
    	return x;
	}

	/**
	 * Initialises the GA population.
	 * 
	 */
	protected void initialisePop() {
		for (int i = 0; i < POPSIZE; i++) {
			pop[i][0] = generateQuantity(false);//first index we save the quantity		
			pop[i][1] = random.nextDouble();//beta3
			for (int j = 2; j < pop[0].length; j++)// all columns of pop have
													// the same length, i.e.
													// THRESHOLD.length+5; so it
													// doesn't matter if I say
													// pop[0], or pop[50] etc.
			{
				
				pop[i][j] = random.nextDouble();
			}
		}
	}

	/** Ensure we always generate a positive quantity **/
	protected int generateQuantity(boolean shortSelling) {
		int quantity = -1;
		if (shortSelling == false) {

			while (quantity <= 0){
				int max = 11;
				int min = 1;
				Random rand = new Random();
				quantity = rand.nextInt((max - min) + 1) + min;
				//quantity =  1 + (10 - 1) * new Random().nextInt(); //random.nextInt(MAX_QUANTITY);
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
	protected Fitness[] popFitnessEvaluation() {
		Fitness[] fitness = new Fitness[POPSIZE];
		bestFitness = Double.NEGATIVE_INFINITY;
		argBestFitness = Integer.MIN_VALUE;
		bestfitnessReturnMap.clear();
		bestFitnessList.clear();
		for (int p = 0; p < POPSIZE; p++) {
			fitness[p] = fitness(pop[p], false);
			bestfitnessReturnMap.put(Double.valueOf(p),fitness[p].value );
			if (fitness[p].value > bestFitness) {
				bestFitness = fitness[p].value;
				argBestFitness = p;
			}
		}
		bestFitnessList = HelperClass.findGreatest(bestfitnessReturnMap,
				Const.ELISTISM_COUNT); 
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
	 * @param individual The individual to be mutated
	 */
	protected void mutation(int individual) {
		if (random.nextDouble() < MUTATION_PROB)
		{
			for (int j = 0; j < pop[0].length; j++)//all columns of pop have the same length, i.e. THRESHOLD.length+5; so it doesn't matter if I say pop[0], or pop[50] etc.
			{
				if (random.nextDouble() > 0.5)
				{
					if (j == 0 )//normal quantity (0) and short-selling quantity (3)
						newPop[individual][j] =  generateQuantity(false);//if j==0, then it's the normal quantity, otherwise it's quantity for short-selling	
					else//all other cases go here, even the j=1 for beta.
							newPop[individual][j] = random.nextDouble();
				}
			}
		}
	}
	
	/**
	 * Non-uniform Mutation
	 * @param individual The individual to be mutated
	 */
	protected void mutationNonUniform(int individual) {
		if (random.nextDouble() < MUTATION_PROB)
		{
			for (int j = 0; j < pop[0].length; j++)//all columns of pop have the same length, i.e. THRESHOLD.length+5; so it doesn't matter if I say pop[0], or pop[50] etc.
			{
				if (random.nextDouble() > 0.5)
				{
					if (j == 0)//we need to generate an integer for the quantity
						newPop[individual][j] = (int) generateQuantity(false);
					else{
						//all other cases go here, even the j=1 for beta.
							double a = 0;
							double b = 1;
							double r = random.nextDouble();
							double tau = random.nextDouble() > 0.5 ? 1 : 0;
							
							newPop[individual][j] = (tau == 1) ? 
									pop[individual][j] + (b - pop[individual][j]) * (1 - Math.pow(r, 1 - (double)currentGeneration/MAX_GENERATIONS)) : 
									pop[individual][j] - (pop[individual][j] - a) * (1 - Math.pow(r, 1 - (double)currentGeneration/MAX_GENERATIONS)); 			
					}
				}
			}
		}
	}
	

	/**
	 * Tournament selection
	 * @param fitness The fitness array of the population
	 * @return argSmallest The position/index of the individual winning the tournament
	 */
	protected int tournament(Fitness[] fitness){
		double smallest = Double.NEGATIVE_INFINITY;
		int argSmallest = Integer.MIN_VALUE;

		for (int i = 0; i < tournamentSize; i++)
		{
			int choice =
					(int) Math.floor(random.nextDouble() * (double) POPSIZE);

			double fit = fitness[choice].value;//original approach

			if (fit > smallest)
			{
				argSmallest = choice;
				smallest = fit;
			}
		}

		return argSmallest;

	}

	/**
	 * Uniform Crossover
	 * 
	 * @param first The index of the first parent
	 * @param second The index of the second parent
	 * 
	 * @return offspring The array of weights of the offspring
	 */
	protected double[] crossover(int first, int second){

		double[] offspring = new double[pop[0].length];//all columns of pop have the same length, i.e. THRESHOLD.length+5; so it doesn't matter if I say pop[0], or pop[50] etc.

		if (random.nextDouble() < CROSSOVER_PROB)
		{
			for (int j = 0; j < offspring.length; j++)
			{
				offspring[j] =
						random.nextDouble() > 0.5 ? pop[first][j]
								: pop[second][j];
			}
		}
		else
		{
			for (int j = 0; j < offspring.length; j++)
			{
				offspring[j] = pop[first][j];
			}
		}

		return offspring;
	}

	/**
	 * One-point Crossover
	 * 
	 * @param first The index of the first parent
	 * @param second The index of the second parent
	 * 
	 * @return offspring The array of weights of the offspring
	 */
	protected double[] crossoverOnePoint(int first, int second){

		double[] offspring = new double[pop[0].length];//all columns of pop have the same length, i.e. THRESHOLD.length+5; so it doesn't matter if I say pop[0], or pop[50] etc.
		int xoverPoint = random.nextInt(offspring.length); 

		if (random.nextDouble() < CROSSOVER_PROB)
		{
			for (int j = 0; j < xoverPoint; j++)
			{
				offspring[j] =pop[first][j];

			}

			for (int j = xoverPoint; j < offspring.length; j++)
			{
				offspring[j] =pop[second][j];

			}
		}
		else
		{
			for (int j = 0; j < offspring.length; j++)
			{
				offspring[j] = pop[first][j];
			}
		}

		return offspring;
	}

	/**
	 * Arithmetical Crossover
	 * 
	 * @param first The index of the first parent
	 * @param second The index of the second parent
	 * 
	 * @return offspring The array of weights of the offspring
	 */
	protected double[] crossoverArithmetical(int first, int second){

		double[] offspring = new double[pop[0].length];//all columns of pop have the same length, i.e. THRESHOLD.length+5; so it doesn't matter if I say pop[0], or pop[50] etc.

		if (random.nextDouble() < CROSSOVER_PROB)
		{
			for (int j = 0; j < offspring.length; j++)
			{
				if (j == 0)//we need to generate an integer for the quantity
					offspring[j] = (int) ( 0.5 * pop[first][j] + 0.5 * pop[second][j] );//obtaining the arithmetic mean of the two parents
				else
					offspring[j] = 0.5 * pop[first][j] + 0.5 * pop[second][j];//obtaining the arithmetic mean of the two parents
			}
		}
		else
		{
			for (int j = 0; j < offspring.length; j++)
			{
				offspring[j] = pop[first][j];
			}
		}

		return offspring;
	}
	

	
	/**
	 * Discrete Crossover
	 * 
	 * @param first The index of the first parent
	 * @param second The index of the second parent
	 * 
	 * @return offspring The array of weights of the offspring
	 */
	protected double[] crossoverDiscrete(int first, int second){

		double[] offspring = new double[pop[0].length];//all columns of pop have the same length, i.e. THRESHOLD.length+5; so it doesn't matter if I say pop[0], or pop[50] etc.
		
		if (random.nextDouble() < CROSSOVER_PROB)
		{
			for (int j = 0; j < offspring.length; j++)
			{
				double cmin = pop[first][j] < pop[second][j] ? pop[first][j] : pop[second][j];
				double cmax = pop[first][j] > pop[second][j] ? pop[first][j] : pop[second][j];

				if (j == 0)//we need to generate an integer for the quantity
					offspring[j] = (int) generateQuantity(false); //( random.nextDouble() * (cmax - cmin) + cmin );//rnd number in the range [cmin, cmax]
				else
					offspring[j] = random.nextDouble() * (cmax - cmin) + cmin;//rnd number in the range [cmin, cmax]

				
			}
		}
		else
		{
			for (int j = 0; j < offspring.length; j++)
			{
				offspring[j] = pop[first][j];
			}
		}

		return offspring;
	}
	

	/** Fitness function: (Return - Maximum DrawDown) **/
	
	Fitness fitness(double[] individual, boolean test) {
	
		clearTradingDetails();
		// number of operations not successful
				int uSell = 0;
				int uBuy = 0;
				int noop = 0;
				
			

				Fitness fitness = new Fitness();
				TradingClass  tradeClass =  new TradingClass();
				tradeClass.baseCurrencyAmount = budget;
				tradeClass.boughtTradeList.add(budget);
				double totalWeight = 0.0;
				
				//calculate total weight
				for (int k = 0; k < SELECTED_THRESHOLDS.length; k++ ){
					totalWeight = totalWeight + individual[k + 2];
				}
				
				/**
				 * defensive coding. validate that the number of selected DCCurvePerfectforesight 
				 * element match  SELECTED_THRESHOLDS
				 * 
				 * */ 
				
				
				
				//assign budget
				if (!test){
					double allocatedBudget = 0.0;
					int budgetAllocatorCounter =0;
					for (int k = 0; k < curvePerfectForesight_Selected.length; k++ ){
						if (curvePerfectForesight_Selected[k].getIsSelectedThresholdFromCandidateList()) {
							
							double assignedbudget = (individual[budgetAllocatorCounter + 2]/totalWeight) * budget;
							allocatedBudget = allocatedBudget + assignedbudget;
							curvePerfectForesight_Selected[k].setTrainingOpeningPosition(assignedbudget);
							curvePerfectForesight_Selected[k].setTrainingOpeningPositionHist(assignedbudget);
							curvePerfectForesight_Selected[k].setAssociatedWeight((individual[budgetAllocatorCounter + 2]/totalWeight));
							budgetAllocatorCounter++;
						}
					}
							
					int chosenThreshold = (int) ((Math.random() * (4 - 0)) + 0);
					
							
					curvePerfectForesight_Selected[chosenThreshold].setTrainingOpeningPosition(curvePerfectForesight_Selected[chosenThreshold].getTrainingOpeningPosition() + (budget - allocatedBudget)) ;
					curvePerfectForesight_Selected[chosenThreshold].setTrainingOpeningPositionHist(curvePerfectForesight_Selected[chosenThreshold].getTrainingOpeningPosition() + (budget - allocatedBudget)) ;
					
				
				}
				
				double perfectForcastTrainingReturn = 0.0;
				
				//set budget for testing data
				if (test){
					for (int j = 0; j < curvePerfectForesight_Selected.length; j++ ){
						
						for (int budgetAssigner = 0; budgetAssigner < curvePerfectForesight_Selected.length; budgetAssigner++ ){
							if (curvePerfectForesight_Selected[budgetAssigner].getThresholdValue() ==
									curvePerfectForesight_Selected[j].getThresholdValue())
							{
								curveClassifcation[budgetAssigner].setOpeningPosition(curvePerfectForesight_Selected[j].getTrainingOpeningPositionHist());	
								curveClassifcation[budgetAssigner].setOpeningPositionHist(curvePerfectForesight_Selected[j].getTrainingOpeningPositionHist());	
								perfectForcastTrainingReturn = curveClassifcation[budgetAssigner].trade( preprocess[budgetAssigner]);
								curveClassifcation[budgetAssigner].setTradingReturnValaue(perfectForcastTrainingReturn);
								curveClassifcation[budgetAssigner].setAssociatedWeight(curvePerfectForesight_Selected[j].getAssociatedWeight());
							}
						}
						
					}
				}
				else{
					for (int j = 0; j < curvePerfectForesight_Selected.length; j++ ){
						curvePerfectForesight_Selected[j].estimateTrainingUsingOutputData(null);
						perfectForcastTrainingReturn = curvePerfectForesight_Selected[j].trainingTrading(null);
						curvePerfectForesight_Selected[j].setTradingReturnValaue(perfectForcastTrainingReturn);
					}
				}
				
				//Calculate sharp ratio
				double weightedReturn = 0.0;
				double weightedVariance = 0.0;
				double weigthedSharpRatio = 0.0;
				double totalWealth = 0.0;
				double totalWeightedMdd =0.0;
				int numOfTransactions = 0;
				int numberOfTradingThresholds = 0;
				
				if (test){
					System.out.println("In testing phase. exiting");
					for (int j = 0; j < curveClassifcation.length; j++ ){
						totalWealth = totalWealth + curveClassifcation[j].getTradingReturnValue();
						weightedReturn = weightedReturn + 
								((((curveClassifcation[j].getOpeningPositionHist() - curveClassifcation[j].getTradingReturnValue() )
										/ curveClassifcation[j].getOpeningPositionHist() ) 
										* 100)
										* curveClassifcation[j].getAssociatedWeight()); 
						weightedVariance = weightedVariance +	(curveClassifcation[j].getSharpRatio() * curveClassifcation[j].getAssociatedWeight()); 
						totalWeightedMdd = totalWeightedMdd + (curveClassifcation[j].getMaxMddBase() * curveClassifcation[j].getAssociatedWeight());
						numOfTransactions =  numOfTransactions + curveClassifcation[j].getNumberOfTransactions();
						numberOfTradingThresholds++;
					}
				}
				else{
				
					for (int j = 0; j < curvePerfectForesight_Selected.length; j++ ){
				
						totalWealth = totalWealth + curvePerfectForesight_Selected[j].getTradingReturnValue();
						weightedReturn = weightedReturn + 
								((((curvePerfectForesight_Selected[j].getTradingReturnValue()  -curvePerfectForesight_Selected[j].getTrainingOpeningPositionHist())
										/ curvePerfectForesight_Selected[j].getTrainingOpeningPositionHist()) 
										* 100)
										* curvePerfectForesight_Selected[j].getAssociatedWeight()); 
						weightedVariance = weightedVariance +	(curvePerfectForesight_Selected[j].getSharpRatio() * curvePerfectForesight_Selected[j].getAssociatedWeight()); 
						totalWeightedMdd = totalWeightedMdd + (curvePerfectForesight_Selected[j].getMaxMddBase() * curvePerfectForesight_Selected[j].getAssociatedWeight());
						numOfTransactions =  numOfTransactions + curvePerfectForesight_Selected[j].getNumberOfTransactions();
						numberOfTradingThresholds++;
					}		
				}
				
				weigthedSharpRatio = weightedVariance;//  (weightedReturn/numberOfTradingThresholds)/FastMath.sqrt(weightedVariance);
			//	System.out.println(weigthedSharpRatio);
				
				
				
				fitness.uSell = uSell;
				fitness.uBuy = uBuy;
				fitness.noop = noop;
				fitness.realisedProfit = totalWealth - budget;
				
				fitness.MDD = totalWeightedMdd;// /numberOfMdds;
				fitness.wealth = totalWealth;// my wealth, at the end of the
										// transaction period
				fitness.sharpRatio = weigthedSharpRatio;// /numberOffSharpRatio;
				fitness.Return = 100.0 * (totalWealth - budget) / budget;
				fitness.value = weigthedSharpRatio; //fitness.Return - (mddWeight * Math.abs(fitness.MDD)); // + fitness.sharpRatio;
				fitness.noOfTransactions = numOfTransactions;
				fitness.noOfShortSellingTransactions = 0;
				
				fitness.tradingClass = tradeClass;

				return fitness;
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
		log.save("Logger_"+Const.hashFunctionTypeToString(Const.OsFunctionEnum)+".txt", String.format("%d\t%12.6f\t%12.6f\t%12.6f", generation, best, worst, average));
		if (generation == MAX_GENERATIONS - 1)
			System.out.println("Number of transactions of best individual in training: "
					+ (fitness[bestIndividualIndex].noOfTransactions
							+ fitness[bestIndividualIndex].noOfShortSellingTransactions));
	}

	public void saveResults(Fitness f, int i) {
		// Saving to Results.txt file
		log.save("Results_"+Const.hashFunctionTypeToString(Const.OsFunctionEnum)+".txt", String.format("Run " + i + "\t%10.6f\t%10.6f\t%10.6f\t%10.6f\t%10.6f\t%10.6f\t%d\t%d", f.wealth,
				f.Return, f.value, f.realisedProfit, Math.abs( f.MDD), f.sharpRatio, f.noOfTransactions, f.noOfShortSellingTransactions));// saving
																													// and
																													// reporting
		for (int transactionCount = 0; transactionCount< f.tradingClass.boughtTradeList.size(); transactionCount++ )																											// the
		log.save("IndicatorTransactions_"+Const.hashFunctionTypeToString(Const.OsFunctionEnum)+".txt",String.format("Run " + i + "\t%10.6f",f.tradingClass.boughtTradeList.get(transactionCount) ));																								// fitness,
																													

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

		System.out.println("Quantity: " + pop[argBestFitness][0]);
		System.out.println("Beta: " + pop[argBestFitness][1]);
		System.out.println("Beta2: " + pop[argBestFitness][2]);
		System.out.println("Short-selling quantity: " + pop[argBestFitness][3]);
		System.out.println("Beta3: " + pop[argBestFitness][4]);
		System.out.println("Threshold weights: ");

		// Saving to Solutions.txt file
		log.save("Solutions_"+Const.hashFunctionTypeToString(Const.OsFunctionEnum)+".txt", "Quantity: " + pop[argBestFitness][0]);
		log.save("Solutions_"+Const.hashFunctionTypeToString(Const.OsFunctionEnum)+".txt", "Beta: " + pop[argBestFitness][1]);
		log.save("Solutions_"+Const.hashFunctionTypeToString(Const.OsFunctionEnum)+".txt", "Beta2: " + pop[argBestFitness][2]);
		log.save("Solutions_"+Const.hashFunctionTypeToString(Const.OsFunctionEnum)+".txt", "Short-selling quantity: " + pop[argBestFitness][3]);
		log.save("Solutions_"+Const.hashFunctionTypeToString(Const.OsFunctionEnum)+".txt", "Beta3: " + pop[argBestFitness][4]);
		log.save("Solutions_"+Const.hashFunctionTypeToString(Const.OsFunctionEnum)+".txt", "Threshold weights: ");

		for (int m = 0; m < THRESHOLDS.length; m++) {
			System.out.println(String.format("%1.3f%%: %7.6f", THRESHOLDS[m] * 100, pop[argBestFitness][m + 2]));// +5,
																													// coz
																													// the
																													// first
																													// 5
																													// are
																													// quantities
																													// and
																													// the
																	// betas
			log.save("Solutions_"+Const.hashFunctionTypeToString(Const.OsFunctionEnum)+".txt", String.format("%1.3f%%: %7.6f", THRESHOLDS[m] * 100, pop[argBestFitness][m + 2]));
		}
	}
	
	public void cleanUpTempFiles(){
		
		for (int reportCount = 0; reportCount < SELECTED_THRESHOLDS.length; reportCount++) {
			if (preprocess[reportCount] != null)
				preprocess[reportCount].removeTempFiles();
			//curveClassifcation
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
	
	public void reportRMSE(){
		log.delete("RegressionAnalysis_"+Const.hashFunctionTypeToString(Const.OsFunctionEnum)+".txt");
		log.save("RegressionAnalysis_"+Const.hashFunctionTypeToString(Const.OsFunctionEnum)+".txt", " Filename \t threshold \t RMSE");
		
		for (int j = 0; j < SELECTED_THRESHOLDS.length; j++) {
		
			//TODO how best to combine multiple RMSEs 
			
		
		}
	}
	
	public void reportClassification(){
		
		//TODO decide how best to combine classification results
		
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
		
		Const.OsFunctionEnum =  Const.hashFunctionType(s[6]);
		Const.optimisationSelectedThreshold = Const.optimisation_selected_threshold.eMedian;
		
		log = new Logger(s[1], s[3], s[4]);
		log.delete("Solutions_"+Const.hashFunctionTypeToString(Const.OsFunctionEnum)+".txt");												
		log.delete("Results_"+Const.hashFunctionTypeToString(Const.OsFunctionEnum)+".txt");
		log.delete("Fitness_"+Const.hashFunctionTypeToString(Const.OsFunctionEnum)+".txt");
		log.delete("Logger_"+Const.hashFunctionTypeToString(Const.OsFunctionEnum)+".txt");
		log.delete("IndicatorTransactions_"+Const.hashFunctionTypeToString(Const.OsFunctionEnum)+".txt");

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

		log.save("Results_"+Const.hashFunctionTypeToString(Const.OsFunctionEnum)+".txt",
				"\tWealth\tReturn\tFitness\tRealised Profit\tMDD\tSharpRatio\tNoOfTransactions\tNoOfShortSellingTransactions");
		log.save("Fitness_"+Const.hashFunctionTypeToString(Const.OsFunctionEnum)+".txt", "Train fitness\tTest fitness");

		log.save("IndicatorTransactions_"+Const.hashFunctionTypeToString(Const.OsFunctionEnum)+".txt","\tTransaction");
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
		LocalDateTime now = LocalDateTime.now();
		System.out.println(dtf.format(now) + " Starting run");
		//nRuns
		for (int i = 0; i < nRuns; i++) {
			System.out.println("=========================== Run " + i + "==========================");
			log.save("Logger_"+Const.hashFunctionTypeToString(Const.OsFunctionEnum)+".txt", "\n=========================== Run " + i + "==========================");

			Fitness f = ga.run(seed, i);

			ga.saveResults(f, i);

			ga.reBuild();
			now = LocalDateTime.now();
			System.out.println(dtf.format(now) + " Completed run " + i);
		}
		
		ga.reportRMSE();
		
		if (Const.OsFunctionEnum == Const.function_code.eGP ){
			//ga.reportClassification();
			ga.cleanUpTempFiles();
		}
		
		
		log.save("Results_"+Const.hashFunctionTypeToString(Const.OsFunctionEnum)+".txt", "\n\nTesting Budget\t" + budget);
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
		
		public TradingClass tradingClass = new TradingClass();
		

	}
	
	public static class TradingClass {
		
		double baseCurrencyAmount;
		double quoteCurrencyAmount;
		List <Double> soldTradeList = new ArrayList<Double>();
		List <Double> boughtTradeList = new ArrayList<Double>();
		boolean isOpenPosition = false;
		int baseCurrencyTransaction  =0;
		int quoteCurrencyTransaction  =0;
		double lastBidPrice = 0.0;
		int openPositionCount = 0;
	}
	
	void clearTradingDetails(){
	
		for (int i = 0; i < curveClassifcation.length; i++) { // The arrays
			curveClassifcation[i].clearSharpRatio();
			curveClassifcation[i].refreshMDD();
			curveClassifcation[i].resetNumberOfQuoteCcyTransaction();
			curveClassifcation[i].resetNumberOfBaseCcyTransaction();
			curveClassifcation[i].clearPositionArrayBase();
			curveClassifcation[i].clearPositionArrayQuote();
			curveClassifcation[i].clearPredictedTrendString();
			curveClassifcation[i].clearActualTrendString();
			
			
		}
		
		for (int i = 0; i < curvePerfectForesight_Selected.length; i++) { // The arrays
			
			if (curvePerfectForesight_Selected[i].getIsSelectedThresholdFromCandidateList())
			{
				curvePerfectForesight_Selected[i].clearSharpRatio();
				curvePerfectForesight_Selected[i].refreshMDD();
				curvePerfectForesight_Selected[i].resetNumberOfQuoteCcyTransaction();
				curvePerfectForesight_Selected[i].resetNumberOfBaseCcyTransaction();
				curvePerfectForesight_Selected[i].clearPositionArrayBase();
				curvePerfectForesight_Selected[i].clearPositionArrayQuote();
				curvePerfectForesight_Selected[i].clearPredictedTrendString();
				curvePerfectForesight_Selected[i].clearActualTrendString();
			}
		}
	}
}
