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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;



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
import misc.SimpleDrawDown;
import misc.SimpleSharpeRatio;
import weka.core.Instances;
import dc.ga.PreProcess;

public class GA_new {
	int numberOfThresholds;
	protected static double[] THRESHOLDS;
	double thresholdIncrement;
	String filename = "";
	int currentGeneration;

	Double[] training;
	Double[] test;
	DCCurve[] curves;
	Double[][] trainingMeanRatio;
	double[][][] gpEstimates = new double[5][2][]; //This is done to improve performance and not to have to
							// evaluate every data point
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

	static Map<Double, String> thresholdGPStringMap = new HashMap<Double, String>();
	public static Map<Double, String> thresholdGPStringUpwardMap = new HashMap<Double, String>();
	public static Map<Double, String> thresholdGPStringDownwardMap = new HashMap<Double, String>();

	public static Map<Double, String> thresholdClassifcationBasedGPStringUpwardMap = new HashMap<Double, String>();
	public static Map<Double, String> thresholdClassifcationBasedGPStringDownwardMap = new HashMap<Double, String>();
	public static Map<Double, PreProcess> thresholdClassifcationModelMap = new HashMap<Double, PreProcess>();
	public static Map<Double, Instances> thresholdTrainingInstanceMap = new HashMap<Double, Instances>();
	public static Map<Double, Instances> thresholdTestInstanceMap = new HashMap<Double, Instances>();
	Map<Double, Double> bestfitnessReturnMap = new HashMap<Double, Double>();
	List<Entry<Double, Double>> bestFitnessList  =  new ArrayList<Entry<Double, Double>>();;
	protected static Logger log;

	private Fitness bestTestFitness = new Fitness();// to keep track of the best
													// test fitness, regardless
													// of which GA run we
													// currently are.
	public SimpleDrawDown simpleDrawDown = new SimpleDrawDown();
	public SimpleSharpeRatio sharpeRatio = new SimpleSharpeRatio();

	Map<Double, Map<String, String>> bestThresholdArray;
	Map<Double, Map<String, AbstractNode>> bestGPThresholdArray;
	Map<Double, PreProcess> processMap;
	double[][] multiArrClassification = new double[5][];
	double[][] transactionsPerThreshold = new double[5][];
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
		trainingMeanRatio = new Double[THRESHOLDS.length][2];// There's only 2
																// ratios, one
																// for upward
																// and one for
																// downward OS
																// events.

		trainingGPTrees = new AbstractNode[THRESHOLDS.length][2];// There's only 2
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
		int trainingDataSize = trainingDataList.size();
		for (int i = trainingDataSize; i < (counter + trainingDataSize); i++) {
			testDataList.add(FReader.dataRecordInFileArray.get(i));
		}
		// budget = 100000;

		curves = new PerfectForecastDCCurve[THRESHOLDS.length];
		
		System.out.println("DC curves:");
		// simpleDrawDown.Calculate(budget); 
		if (Const.OsFunctionEnum == Const.function_code.eGP){
			bestGPThresholdArray = HelperClass.getBestGPThresholds(filename, THRESHOLDS.length, training, Const.NUMBER_OF_THRESHOLDS, 0.005, 0.0025);
			
		}
		else{
			
			bestThresholdArray = HelperClass.getBestThresholds(filename, THRESHOLDS.length, training, Const.NUMBER_OF_THRESHOLDS, 0.005, 0.0025);
			System.out.println("Best thresholds are " + bestThresholdArray.toString());

		}
		// processMap = HelperClass.getClassifiers(THRESHOLDS, filename,
		// training);

		
		// TODO : check if GP ratio. If yes update thresolds
		int thresholdCount = 0;
		
		if (Const.OsFunctionEnum == Const.function_code.eGP){
			for (Entry<Double, Map<String, AbstractNode>> entry : bestGPThresholdArray.entrySet()) {
				// THRESHOLDS[i] = (initial * (i + 1)) / 100.0;
				THRESHOLDS[thresholdCount] = entry.getKey();
				thresholdCount++;
			}
			
		}
		else{
			for (Entry<Double, Map<String, String>> entry : bestThresholdArray.entrySet()) {
				// THRESHOLDS[i] = (initial * (i + 1)) / 100.0;
				THRESHOLDS[thresholdCount] = entry.getKey();
				thresholdCount++;
			}
		}
			for (int i = 0; i < curves.length; i++) {
				curves[i] = new PerfectForecastDCCurve();
				curves[i].build(training, THRESHOLDS[i]);
				if (Const.OsFunctionEnum == Const.function_code.eGP){
					curves[i].bestDownWardEventTree = bestGPThresholdArray.get(THRESHOLDS[i]).get("DownwardEvent");
					curves[i].bestUpWardEventTree = bestGPThresholdArray.get(THRESHOLDS[i]).get("UpwardEvent");
					
					//processMap.put(THRESHOLDS[i],);
					curves[i].preprocess =  HelperClass.getClassifier(THRESHOLDS[i], filename, training, Const.OsFunctionEnum);
				
					if (curves[i].preprocess == null)
						curves[i].preprocess = null;
					
					System.out.println("classification for " + THRESHOLDS[i] + " completed");
					if (curves[i].preprocess != null)
						HelperClass.cleanUpClassificationTempFile(curves[i].preprocess, filename);
					
					System.out.println("Clean you temp classification file for " + THRESHOLDS[i] + " completed");
					
					if (curves[i].preprocess != null)
						if (false == HelperClass.updateClassifierWithTestData(THRESHOLDS[i], test, curves[i]))
							curves[i].preprocess = null;
					try {
						thresholdClassifcationModelMap.put(THRESHOLDS[i], curves[i].preprocess);
					}
					catch(Exception e){
						thresholdClassifcationModelMap.put(THRESHOLDS[i], null);
					}
					if (curves[i].bestUpWardEventTree == null || curves[i].bestDownWardEventTree == null){
						System.out.println("Exiting");
						System.exit(-1);
					}
					curves[i].predictionOnly();  // GP estimate of OS event length
					
					gpEstimates[i][1] =  new double[curves[i].predictionUpward.length];
					System.arraycopy(curves[i].predictionUpward, 0, gpEstimates[i][1] , 0, curves[i].predictionUpward.length);
					gpEstimates[i][0] =  new double[curves[i].predictionDownward.length];
					System.arraycopy(curves[i].predictionDownward, 0, gpEstimates[i][0] , 0, curves[i].predictionDownward.length);
					
					
					System.out.println("Classifier  updated for " + THRESHOLDS[i] + " completed");
					
					if (curves[i].preprocess != null){
						curves[i].setTrainingInstance(curves[i].output);
					// curves[i].setPreprocessTestInstance();
					
						curves[i].setTestInstance(curves[i].preprocess.testInstances);
					
						thresholdTrainingInstanceMap.put(THRESHOLDS[i], new Instances(curves[i].getTrainingInstance()));
						thresholdTestInstanceMap.put(THRESHOLDS[i], new Instances(curves[i].getTestInstance()));
					}
					else
					{
						thresholdTrainingInstanceMap.put(THRESHOLDS[i], null);
						thresholdTestInstanceMap.put(THRESHOLDS[i],null);
					}
					curves[i].threshold = THRESHOLDS[i];
					if (curves[i].preprocess != null) {
					Instances trainingInstance = curves[i].getTrainingInstance();
					int trainingInstanceCount = trainingInstance.size();
		
					double[] trainingClassificationArray = new double[trainingInstanceCount];
					for (int tic = 0; tic < trainingInstance.size(); tic++) {
						Double clsLabel = 0.0;
						try {
							if (curves[i].preprocess != null)
								clsLabel = curves[i].preprocess.autoWEKAClassifier.classifyInstance(trainingInstance.get(tic));
						} catch (Exception e) {
							// TODO Auto-generated catch block
							System.out.print("classifyTraining: invalid label");
							// String classificationStr =
							// (clsLabel.toString().compareToIgnoreCase("0.0") == 0) ?
							// "yes" : "no";
							// trainingClassificationArray[i]= classificationStr;
						}
						trainingClassificationArray[tic] = clsLabel;
					}
					multiArrClassification[i] = trainingClassificationArray;
					}
				}
				else
					multiArrClassification[i] = null;
	
						System.out.println(String.format(
					"%1.3f%%: {length = %5d, meanRatio[up] = %02.2f, meanRatio[down] = %02.2f}", THRESHOLDS[i] * 100,
					curves[i].events.length, curves[i].meanRatio[1], curves[i].meanRatio[0]));
			log.save("Curves_"+Const.hashFunctionTypeToString(Const.OsFunctionEnum)+".txt",
					String.format("%1.3f%%: {length = %5d, meanRatio[up] = %02.2f, meanRatio[down] = %02.2f}",
							THRESHOLDS[i] * 100, curves[i].events.length, curves[i].meanRatio[1],
							curves[i].meanRatio[0]));
			}  //End Classification
		
		System.out.println();
	}

	public void reBuild() {
		for (int i = 0; i < curves.length; i++) {

			trainingMeanRatio[i][0] = curves[i].meanRatio[0];
			trainingMeanRatio[i][1] = curves[i].meanRatio[1];
			
			//trainingGPTrees[i][0] = curves[i].bestDownWardEventTree; // downward
			//trainingGPTrees[i][1] = curves[i].bestUpWardEventTree; // upward

			// curves[i].setPreprocessTestInstance();
			
			curves[i] = new PerfectForecastDCCurve();
			curves[i].build(training, THRESHOLDS[i]);
			//curves[i].gpRatio[0] = bestThresholdArray.get(THRESHOLDS[i]).get("DownwardEvent");
			//curves[i].gpRatio[1] = bestThresholdArray.get(THRESHOLDS[i]).get("UpwardEvent");
			if (Const.OsFunctionEnum == Const.function_code.eGP ){
				curves[i].bestDownWardEventTree = bestGPThresholdArray.get(THRESHOLDS[i]).get("DownwardEvent");
				curves[i].bestUpWardEventTree = bestGPThresholdArray.get(THRESHOLDS[i]).get("UpwardEvent");
				if (thresholdClassifcationModelMap.get(THRESHOLDS[i]) != null){
					curves[i].preprocess = thresholdClassifcationModelMap.get(THRESHOLDS[i]);
					curves[i].setTrainingInstance(thresholdTrainingInstanceMap.get(THRESHOLDS[i]));
					//curves[i].setPreprocessTestInstance(testInstance);
					curves[i].setTestInstance(thresholdTestInstanceMap.get(THRESHOLDS[i]));
				}
			}
			curves[i].threshold = THRESHOLDS[i];

			curves[i].meanRatio[0] = trainingMeanRatio[i][0];
			curves[i].meanRatio[1] = trainingMeanRatio[i][1];
			curves[i].filename = filename;
			//curves[i].predictionOnly();
			
			if (Const.OsFunctionEnum == Const.function_code.eGP ){
				curves[i].predictionUpward = new double[gpEstimates[i][1].length];
				curves[i].predictionDownward = new  double[gpEstimates[i][0].length];
				
				for (int m = 0 ; m < curves[i].predictionUpward.length; m++ )
					curves[i].predictionUpward[m] = gpEstimates[i][1][m];
				
				for (int m = 0 ; m < curves[i].predictionDownward.length; m++ )
					curves[i].predictionDownward[m] = gpEstimates[i][0][m];
			}
			
		//	System.arraycopy(gpEstimates[i][1], 0,curves[i].predictionUpward , 0, gpEstimates[i].length);
		//	System.arraycopy(gpEstimates[i][0], 0,curves[i].predictionDownward , 0, gpEstimates[i].length);
			
			System.out.println("Old output " + curves[i].output.length);
			// System.out.println("New output " +
			// trainingOutputMap.get(THRESHOLDS[i]).length);

			// System.arraycopy(trainingOutputMap.get(THRESHOLDS[i]), 0,
			// curves[i].output, 0,
			// trainingOutputMap.get(THRESHOLDS[i]).length);

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
		for (int i = 0; i < THRESHOLDS.length; i++) {
			// Because we are in testing, we do not want to use the testing mean
			// ratios, because these should be unknown. Instead, we should use
			// the ratios from training data set. But the build() method
			// re-calculates everything,
			// including the mean ratios. So before this happening, we copy the
			// training ratio upwards and downwards values and save them; then,
			// after the execution of the build() method, we copy back the
			// training ratios.
			trainingMeanRatio[i][0] = curves[i].meanRatio[0];
			trainingMeanRatio[i][1] = curves[i].meanRatio[1];

			//trainingGPMeanRatio[i][0] = curves[i].gpRatio[0]; // downward
			//trainingGPMeanRatio[i][1] = curves[i].gpRatio[1]; // upward

			// curves[i].setPreprocessTestInstance();

			curves[i].build(this.test, THRESHOLDS[i]);// Re-building the curves
														// when we are dealing
														// with the (unseen)
														// testing data.
														// Otherwise we evaluate
														// testing
			if (curves[i].preprocess != null){
				curves[i].setTrainingInstance(thresholdTrainingInstanceMap.get(THRESHOLDS[i]));
				curves[i].setTestInstance(thresholdTestInstanceMap.get(THRESHOLDS[i]));
				curves[i].setOutputTestInstance(curves[i].output);
			}
			curves[i].threshold = THRESHOLDS[i];

			
			curves[i].meanRatio[0] = trainingMeanRatio[i][0];
			curves[i].meanRatio[1] = trainingMeanRatio[i][1];
			
			if (Const.OsFunctionEnum == Const.function_code.eGP ){
				curves[i].bestDownWardEventTree = bestGPThresholdArray.get(THRESHOLDS[i]).get("DownwardEvent").clone();
				curves[i].bestUpWardEventTree = bestGPThresholdArray.get(THRESHOLDS[i]).get("UpwardEvent").clone();
			}

			//curves[i].gpRatio[0] = bestThresholdArray.get(THRESHOLDS[i]).get("DownwardEvent");
			//curves[i].gpRatio[1] = bestThresholdArray.get(THRESHOLDS[i]).get("UpwardEvent");
			curves[i].preprocess = thresholdClassifcationModelMap.get(THRESHOLDS[i]);
			curves[i].predictionOnly();

		}
		// fitness on training dc curves.
		Fitness f = fitness(pop[argBestFitness], true);

		double testFitness = f.value;// original fitness

		log.save("Fitness_"+Const.hashFunctionTypeToString(Const.OsFunctionEnum)+".txt", trainFitness + "\t" + testFitness);

		return f;
	}
	
	private static int getRandomIntegerBetweenRange(int min, int max){
		if (min >= max) {
			throw new IllegalArgumentException("getRandomIntegerBetweenRange: max must be greater than min");
		}
		Random r = new Random();
		return r.nextInt((max - min) + 1) + min;
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
	
		// number of operations not successful
				int uSell = 0;
				int uBuy = 0;
				int noop = 0;
				
				simpleDrawDown = new SimpleDrawDown();
				sharpeRatio = new SimpleSharpeRatio();
				simpleDrawDown.Calculate(GA_new.budget);
				sharpeRatio.addReturn(0.0);
				Double[] data = (test ? this.test : this.training);
				List<FReader.FileMember2> bidAskprice;  

				if (test)
					bidAskprice = new ArrayList<FReader.FileMember2>(testDataList);
				else
					bidAskprice = new ArrayList<FReader.FileMember2>(trainingDataList);
				final int start = 0;
				final int length = data.length;

				// the length of the current overshoot			
				int noOfShortSellingTransactions = 0;
			
				Fitness fitness = new Fitness();
				TradingClass  tradeClass =  new TradingClass();
				tradeClass.baseCurrencyAmount = budget;
				tradeClass.boughtTradeList.add(budget);
				double totalWeight = 0.0;
				for (int j = 0; j < curves.length; j++) {
					totalWeight = totalWeight + individual[j + 2];
				}
				
				//assign budget
				double allocatedBudget = 0.0;
				for (int j = 0; j < curves.length; j++ ){
					curves[j].initialbudget = (individual[j + 2]/totalWeight) * budget;
					curves[j].tradingBudget = curves[j].initialbudget;
					allocatedBudget = curves[j].initialbudget + allocatedBudget;
					curves[j].isPositionOpen =  false;
				}

				curves[0].initialbudget  = curves[0].initialbudget + (budget - allocatedBudget);
				curves[0].tradingBudget  = curves[0].tradingBudget + (budget - allocatedBudget);
				
				
				for (int j = 0; j < curves.length; j++ ){
					curves[j].simpleDrawDown = new SimpleDrawDown();
					curves[j].simpleDrawDown.Calculate(curves[j].initialbudget);
					curves[j].simpleSharpeRatio = new SimpleSharpeRatio();
					curves[j].simpleSharpeRatio.addReturn(0);
					curves[j].noOfTransactions = 0;
				}
				
				
				for (int i = start; i < length; i++) {
					
					
					for (int m = 0; m < curves.length; m++ ){
						Double clsLabel = 1.0;
						String classificationStr = "no";
						double  transactionCost=0.0;
						double eval = 0.0;
						if (test) {
							if ( curves[m].preprocess == null){
								System.out.print("Classificier not found exiting- Test hase cannot continue");
								System.exit(-1);
							}
							if ( curves[m].preprocess != null){
								if (i >= curves[m].getOutputTestInstance().size()) {
									System.out.println("Threshold " + curves[m].threshold + " does not have datapoint " + i);
									continue;
								}
								
								if (curves[m].preprocess != null && curves[m].getOutputTestInstance().get(i) != null) {

									try {
										clsLabel = curves[m].preprocess.autoWEKAClassifier
												.classifyInstance(curves[m].getOutputTestInstance().get(i));
									} catch (Exception e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									classificationStr = (clsLabel.toString().compareToIgnoreCase("0.0") == 0) ? "yes" : "no";
								}
								else {
									System.out.println(" invalid preprocess or testinstance. I is " + i);
									continue;
								} 
							}
							else{
						//		if (Const.OsFunctionEnum == Const.function_code.eGP)
						//			classificationStr = "no";
						///		else
									classificationStr = "yes";
							}
						}
						else{
							Double instanceClass ;
							if (multiArrClassification[m] != null){ // used double array to maintain order and it is faster
							 instanceClass = multiArrClassification[m][i];
							 classificationStr = (instanceClass.toString().compareToIgnoreCase("0.0") == 0) ? "yes" : "no";
							}
							else{
								//if (Const.OsFunctionEnum == Const.function_code.eGP)
								//	classificationStr = "no";
								//else
									classificationStr = "yes";
							}
						}

						
						
						int overshootEstimationPoint =  0; 
						int tradePoint = -1;
						
						if ((classificationStr.compareToIgnoreCase("no") == 0)) {  // This means these is no overshoot
							tradePoint = curves[m].output[i].end;
						}
						else {
							
							
							if (curves[m].output[i].type ==  Type.Upturn){
								if (Const.OsFunctionEnum == Const.function_code.eGP){
									eval = curves[m].bestUpWardEventTree.eval(curves[m].output[i].length()); // curves[curveCounter].predictionUpward[physicalTimeCounter];
								}
								else
									eval = curves[m].meanRatio[1];
							}
							else
								if (Const.OsFunctionEnum == Const.function_code.eGP){
									eval = curves[m].bestDownWardEventTree.eval(curves[m].output[i].length()); // curves[curveCounter].predictionDownward[physicalTimeCounter]; //					
								}
								else
									eval = curves[m].meanRatio[0];
						}
							
						//Round because GP has decimals
						overshootEstimationPoint =  (int) Math.floor(eval);
						tradePoint =  overshootEstimationPoint + curves[m].output[i].end;
				
						if (curves[m].output[i] == null)
							continue;

						if (i + 1 > curves[m].output.length - 1)
							continue;

						if (curves[m].output[i + 1] == null)
							continue;
						int nextEventEndPOint = getNextDirectionaChangeEndPoint(curves[m].output,  tradePoint);
						
						if (tradePoint > nextEventEndPOint ) // If a new DC is
																		// encountered
																		// before the
																		// estimation point
																		// skip trading
							continue;

						
						double myPrice;
						
						if (tradePoint >= bidAskprice.size() ) {
							//System.out.println(" DCCurveClassification: predicted datapoint "
							//		+  tradePoint + " is beyond the size of price array  "
							//		+ bidAskprice.size() + " . Trading ended");
							continue;
						} else {
							// I am opening my position in base currency
							try {
								
								
								if (curves[m].output[i].type == Type.Upturn && !curves[m].isPositionOpen) {
									// Now position is in quote currency
									double askQuantity = curves[m].tradingBudget;
									double zeroTransactionCostAskQuantity = curves[m].tradingBudget;
									double transactionCostPrice = 0.0;
									myPrice = Double.parseDouble(bidAskprice.get(tradePoint).askPrice);
									
									
									transactionCost = askQuantity * (0.025/100);
									transactionCostPrice = transactionCost * myPrice;
									askQuantity =  (askQuantity -transactionCost) *myPrice;
									zeroTransactionCostAskQuantity = zeroTransactionCostAskQuantity *myPrice;
									
									
									if (transactionCostPrice < (zeroTransactionCostAskQuantity - askQuantity)){		
										
									
										curves[m].lastUpDCCend = Double.parseDouble(bidAskprice.get(tradePoint).bidPrice);
										curves[m].tradingBudget = askQuantity;
										curves[m].isPositionOpen = true;
									}

								} else if (curves[m].output[i].type == Type.Downturn && curves[m].isPositionOpen) {
									// Now position is in base currency
									// I buy base currency
									double bidQuantity = curves[m].tradingBudget;
									double zeroTransactionCostBidQuantity = curves[m].tradingBudget;
									double transactionCostPrice = 0.0;
									myPrice = Double.parseDouble(bidAskprice.get(tradePoint).bidPrice);

									transactionCost = bidQuantity * (0.025 / 100);
									transactionCostPrice = transactionCost * myPrice;
									bidQuantity = (bidQuantity - transactionCost) * myPrice;
									zeroTransactionCostBidQuantity = zeroTransactionCostBidQuantity * myPrice;
								
									if (transactionCostPrice < (zeroTransactionCostBidQuantity - bidQuantity)
											&& myPrice < curves[m].lastUpDCCend){
							
										curves[m].previousMdd = curves[m].currentMdd;
										curves[m].tradingBudget = (curves[m].tradingBudget - transactionCost) / myPrice;
										
										curves[m].isPositionOpen = false;
										curves[m].simpleDrawDown.Calculate(curves[m].tradingBudget);
										curves[m].simpleSharpeRatio.addReturn(curves[m].tradingBudget - curves[m].lastClosedPosition);
										curves[m].lastClosedPosition = curves[m].tradingBudget;
										curves[m].currentMdd = curves[m].simpleDrawDown.getMaxDrawDown();
										curves[m].noOfTransactions++;
									}
								}
							} catch (ArrayIndexOutOfBoundsException exception) {
								System.out.println(" DCCurveClassiifcation: Search for element " +  tradePoint
										+ " is beyond the size of price array  " + 
										bidAskprice.size() + " . Trading ended") ;
								break;
								
							}

						}

					}
					

				
				}  //int i = start; i < length; i++

				for (int j = 0; j < curves.length; j++) {
					if (curves[j].isPositionOpen){
						curves[j].tradingBudget = curves[j].lastClosedPosition;
						curves[j].simpleSharpeRatio.removeLastElementFromreturnsList();
						curves[j].currentMdd =  curves[j].previousMdd; 
						curves[j].noOfTransactions--;
					}	
				}
				
				double totalMdd =0.0;
				double totalsharpRatio = 0.0;
				double totalWealth = 0.0;
				
				int numOfTransactions = 0;
				
				// Calculate the sum of each number (return, mdd, sharpRatio) multiplied by its weight
				for (int j = 0; j < curves.length; j++) {
					
						if ( curves[j].noOfTransactions < 2){
						//	System.out.println("Sharp ratio is Nan"); 
							totalWealth = totalWealth + (curves[j].initialbudget) ;
							totalMdd =  totalMdd + ( 0.0  * individual[j + 2]) ;
							totalsharpRatio = totalsharpRatio + ( 0.0  * individual[j + 2]) ;
							  ;
						}
						else{
							totalWealth = totalWealth + (curves[j].tradingBudget) ;
							numOfTransactions = numOfTransactions+ curves[j].noOfTransactions   ;
							totalsharpRatio =  totalsharpRatio +( curves[j].simpleSharpeRatio.calulateSharpeRatio() * individual[j + 2]) ;
							totalMdd = totalMdd + (curves[j].currentMdd * individual[j + 2]) ;
						}	
				}
				
				// Divide totals  by the sum of all weights
				totalsharpRatio		=	totalsharpRatio/totalWeight;
				totalMdd			=	totalMdd/totalWeight;
				
				
				
				fitness.uSell = uSell;
				fitness.uBuy = uBuy;
				fitness.noop = noop;
				fitness.realisedProfit = totalWealth - budget;
				
				fitness.MDD = totalMdd;// /numberOfMdds;
				fitness.wealth = totalWealth;// my wealth, at the end of the
										// transaction period
				fitness.sharpRatio = totalsharpRatio;// /numberOffSharpRatio;
				fitness.Return = 100.0 * (fitness.wealth - budget) / budget;
				fitness.value = fitness.Return - (mddWeight * Math.abs(fitness.MDD)); // + fitness.sharpRatio;
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
		
		for (int reportCount = 0; reportCount < curves.length; reportCount++) {
			if (curves[reportCount].preprocess != null)
				curves[reportCount].preprocess.removeTempFiles();
	
			String tempFolderName = curves[reportCount].preprocess.tempFilePath.get(0).substring(0,
					curves[reportCount].preprocess.tempFilePath.get(0).lastIndexOf(File.separator));
	
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
					curves[reportCount].preprocess.deleteDirectoryRecursionJava6(tempFile[tempFileCount]);
				} catch (IOException e) {
	
					System.out.println("Unable to delete one of the directory");
				}
			}
		}

	}
	
	public void reportRMSE(){
		log.delete("RegressionAnalysis_"+Const.hashFunctionTypeToString(Const.OsFunctionEnum)+".txt");
		log.save("RegressionAnalysis_"+Const.hashFunctionTypeToString(Const.OsFunctionEnum)+".txt", " Filename \t threshold \t RMSE");
		
		for (int j = 0; j < curves.length; j++) {
		
			//need to rebuild with test data
			
			curves[j] = new PerfectForecastDCCurve();
			curves[j].build(test, THRESHOLDS[j]);
			
			if (Const.OsFunctionEnum == Const.function_code.eGP ){
				
				curves[j].bestDownWardEventTree = bestGPThresholdArray.get(THRESHOLDS[j]).get("DownwardEvent");
				curves[j].bestUpWardEventTree = bestGPThresholdArray.get(THRESHOLDS[j]).get("UpwardEvent");
				
				//curves[j].gpRatio[0] = bestThresholdArray.get(THRESHOLDS[j]).get("DownwardEvent");
				//curves[j].gpRatio[1] = bestThresholdArray.get(THRESHOLDS[j]).get("UpwardEvent");
				curves[j].preprocess = thresholdClassifcationModelMap.get(THRESHOLDS[j]);
				if (thresholdClassifcationModelMap.get(THRESHOLDS[j]) != null){
						curves[j].preprocess.filename = filename;
						curves[j].preprocess.loadTestData();
						curves[j].setTrainingInstance(thresholdTrainingInstanceMap.get(THRESHOLDS[j]));
							//curves[i].setPreprocessTestInstance(testInstance);
						curves[j].setTestInstance(thresholdTestInstanceMap.get(THRESHOLDS[j]));
				}
			}
			else
			{	
				curves[j].meanRatio[0] = Double.parseDouble(bestThresholdArray.get(THRESHOLDS[j]).get("DownwardEvent"));
				curves[j].meanRatio[1] = Double.parseDouble(bestThresholdArray.get(THRESHOLDS[j]).get("UpwardEvent"));
			}
			curves[j].estimateTestRMSE();
			double rmse = curves[j].rmseResult;
			log.save("RegressionAnalysis_"+Const.hashFunctionTypeToString(Const.OsFunctionEnum)+".txt", String.format("%s \t %.8f \t %7.6f", filename , THRESHOLDS[j], rmse));
			
			System.out.println("usage: " + rmse);
		}
	}
	
	public void reportClassification(){
		
		log.delete("ClassificationAnalysis_"+Const.hashFunctionTypeToString(Const.OsFunctionEnum)+".txt");
		System.out.println("Classification analysis");
		if (Const.OsFunctionEnum == Const.function_code.eGP ){
			for (int j = 0; j < curves.length; j++) {
				curves[j] = new PerfectForecastDCCurve();
				curves[j].build(test, THRESHOLDS[j]);
				
				if (Const.OsFunctionEnum == Const.function_code.eGP ){
					curves[j].gpRatio[0] = bestThresholdArray.get(THRESHOLDS[j]).get("DownwardEvent");
					curves[j].gpRatio[1] = bestThresholdArray.get(THRESHOLDS[j]).get("UpwardEvent");
					curves[j].preprocess = thresholdClassifcationModelMap.get(THRESHOLDS[j]);
					if (thresholdClassifcationModelMap.get(THRESHOLDS[j]) != null){
						curves[j].preprocess.filename = filename;
						curves[j].preprocess.lastTrainingEvent = thresholdClassifcationModelMap.get(THRESHOLDS[j]).lastTrainingEvent;
						curves[j].preprocess.lastTestingEvent = thresholdClassifcationModelMap.get(THRESHOLDS[j]).lastTestingEvent;
						curves[j].preprocess.loadTestData();
						curves[j].setTrainingInstance(thresholdTrainingInstanceMap.get(THRESHOLDS[j]));
							//curves[i].setPreprocessTestInstance(testInstance);
						curves[j].setTestInstance(thresholdTestInstanceMap.get(THRESHOLDS[j]));
					}
				}
				curves[j].preprocess = thresholdClassifcationModelMap.get(THRESHOLDS[j]);
				if (thresholdClassifcationModelMap.get(THRESHOLDS[j]) != null)
					log.save("ClassificationAnalysis_"+Const.hashFunctionTypeToString(Const.OsFunctionEnum)+".txt", thresholdClassifcationModelMap.get(THRESHOLDS[j]).printPreprocessClassification(thresholdClassifcationModelMap.get(THRESHOLDS[j]).testEventArray));
				else{
					String thresholdStr = String.format("%.8f", THRESHOLDS[j]);
					String reportClassification =  filename + "\t" +  thresholdStr + "\t" +  0.000 + "\t" + 0.000 + "\t" + 0.000
					+ "\t" + 0.000 + "\t" + 0.000 + "\t" + 0.000 + "\t"
					+ 0.000 + "\t" + 0.000 + "\t" + 0.000 + "\t"
					+ 0.000  + "\t" + 0.000;
					log.save("ClassificationAnalysis_"+Const.hashFunctionTypeToString(Const.OsFunctionEnum)+".txt", reportClassification);
				}
			}
		}
		
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
	
	public int getNextDirectionaChangeEndPoint(Event[] output, int pointInTime){
		
		Event nextEvent = null;
		Event currentEvent =  null;
		try{
			
			currentEvent = output[pointInTime];
		}
		catch(ArrayIndexOutOfBoundsException e){
			return Integer.MAX_VALUE;
		}	
		for (int i = pointInTime+1; i < output.length; i++ ){
			nextEvent = output[i] ;
			if (nextEvent.type !=currentEvent.type )
				break;	
		}
		
		if (nextEvent ==  null || nextEvent.type ==currentEvent.type  )
			return -1;
		
		return nextEvent.end;
	}
	
	public int getTradingPoint(int physicalTimeCounter, boolean test, Type eventType,Vector<Double> selectedThresholds){
		
		int tradingPoint = -1;
		int finalTradingPoint = -1;
		double bestPerformanceScore = Double.MAX_VALUE;
		double bestPerformanceScoreTemp = Double.MAX_VALUE;
		
		ArrayList<Integer>  predictionArray =  new ArrayList<Integer>(); 
		for (int curveCounter= 0 ; curveCounter < curves.length ; curveCounter++){
			int index = selectedThresholds.indexOf(curves[curveCounter].threshold); 
				
			if (index == -1)
				continue;
			
			if (curves[curveCounter] == null) {
				System.out.println("curves[" + curveCounter+ "] does not exist. Exiting" );
			}
			
			if (curves[curveCounter].output[physicalTimeCounter] == null) {

				System.out.println("dccurves[" + curveCounter+ "].output[" + "i] does not exist. Exiting");
				System.exit(-1);
			}

			Double clsLabel = 1.0;
			String classificationStr = "no";
			/** Start: Get classification decision for the data-point in the DC trend **/
			if (test) {
				
				if (Const.OsFunctionEnum == Const.function_code.eGP && curves[curveCounter].preprocess != null){
					if (physicalTimeCounter >= curves[curveCounter].getOutputTestInstance().size()) {
						System.out.println("Threshold " + curves[curveCounter].threshold + " does not have datapoint " + physicalTimeCounter);
						continue;
					}
					
					if (curves[curveCounter].preprocess != null && curves[curveCounter].getOutputTestInstance().get(physicalTimeCounter) != null) {

						try {
							clsLabel = curves[curveCounter].preprocess.autoWEKAClassifier
									.classifyInstance(curves[curveCounter].getOutputTestInstance().get(physicalTimeCounter));
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						classificationStr = (clsLabel.toString().compareToIgnoreCase("0.0") == 0) ? "yes" : "no";
					}
					else {
						System.out.println(" invalid preprocess or testinstance. I is " + physicalTimeCounter);
						continue;
					} 
				}
				else{
			//		if (Const.OsFunctionEnum == Const.function_code.eGP)
			//			classificationStr = "no";
			///		else
						classificationStr = "yes";
				}
			}
			else{
				Double instanceClass ;
				if (multiArrClassification[curveCounter] != null){// Wll be null for non-GP
				 instanceClass = multiArrClassification[curveCounter][physicalTimeCounter];
				 classificationStr = (instanceClass.toString().compareToIgnoreCase("0.0") == 0) ? "yes" : "no";
				}
				else{
					//if (Const.OsFunctionEnum == Const.function_code.eGP)
					//	classificationStr = "no";
					//else
						classificationStr = "yes";
				}
			}
			/** End: Get classification decision for the data-point in the DC trend **/
			
			int overshootEstimationPoint =  -1; 
			
			if ((classificationStr.compareToIgnoreCase("no") == 0)) {  // This means these is no overshoot

				overshootEstimationPoint = (curves[curveCounter].output[physicalTimeCounter].length());
				tradingPoint = curves[curveCounter].output[physicalTimeCounter].end;
				
				
			}
			else {
				
				double eval = 0.0;
				if (curves[curveCounter].output[physicalTimeCounter].type ==  Type.Upturn){
					if (Const.OsFunctionEnum == Const.function_code.eGP){
						eval = curves[curveCounter].bestUpWardEventTree.eval(curves[curveCounter].output[physicalTimeCounter].length()); // curves[curveCounter].predictionUpward[physicalTimeCounter];
						bestPerformanceScoreTemp = curves[curveCounter].bestUpWardEventTree.perfScore ;
					}
					else
						eval = curves[curveCounter].meanRatio[1];
				}
				else
					if (Const.OsFunctionEnum == Const.function_code.eGP){
						eval = curves[curveCounter].bestDownWardEventTree.eval(curves[curveCounter].output[physicalTimeCounter].length()); // curves[curveCounter].predictionDownward[physicalTimeCounter]; //					
							bestPerformanceScoreTemp = curves[curveCounter].bestDownWardEventTree.perfScore ;
					}
					else
						eval = curves[curveCounter].meanRatio[0];
				
				//Round because GP has decimals
				overshootEstimationPoint =  (int) Math.round(eval);
				tradingPoint =  overshootEstimationPoint + curves[curveCounter].output[physicalTimeCounter].end;
				
				/*This is where we do prediction. 
				 * Always want to trade after the last known 
				 * Direction change confirmation point.
				*/
				
				if (physicalTimeCounter < curves[curveCounter].output[physicalTimeCounter].end-1)
					continue;
				
				
				/*
				 * If our prediction is beyond next directional change 
				 * confirmation point. We need to discard because we know 
				 * at that point that directions have changed.
				 * */
				try{
					
					if ( tradingPoint >= getNextDirectionaChangeEndPoint(curves[curveCounter].output, physicalTimeCounter)) {//
												//We only know when current DC ends and a new DC trend
												// begins when the next DC is confirmed
						continue;
					}
				}
				catch(ArrayIndexOutOfBoundsException e){
					continue;
				}					
			} 
			
			//This is dangerous add muct never be uncommented because it
			// is counter intuitive
			//if (tradingPoint < curves[curveCounter].output[physicalTimeCounter].end) // We only want to trade in OS region
			//	continue; 
			
			predictionArray.add(tradingPoint);
			if (Const.optimisationSelectedThreshold == Const.optimisation_selected_threshold.eShortest){
				if (tradingPoint >= 0 && tradingPoint <finalTradingPoint)
				finalTradingPoint = tradingPoint;
			}
			else if (Const.optimisationSelectedThreshold == Const.optimisation_selected_threshold.eLongest){
				if (tradingPoint >= 0 && tradingPoint >finalTradingPoint)
				finalTradingPoint = tradingPoint;
			}
			else if (Const.OsFunctionEnum == Const.function_code.eGP &&
					Const.optimisationSelectedThreshold == Const.optimisation_selected_threshold.eperformanceScore){
					if (bestPerformanceScoreTemp < bestPerformanceScore){
						bestPerformanceScore = bestPerformanceScoreTemp;
						finalTradingPoint = tradingPoint;
					}
			}
			else{
					
			}
		} //end for loop
			
		
		if (Const.optimisationSelectedThreshold == Const.optimisation_selected_threshold.eMedian){
			Collections.sort(predictionArray);
			if (predictionArray.size() == 1)
				 finalTradingPoint = predictionArray.get(0);
			else if (predictionArray.size() == 2)
				 finalTradingPoint = (int) (((double)predictionArray.get(0) + (double) predictionArray.get(1))/2);
			else if (predictionArray.size() == 3)
				 finalTradingPoint =  predictionArray.get(1);
			else if (predictionArray.size() == 4)
				 finalTradingPoint = (int) (((double)predictionArray.get(1) + (double) predictionArray.get(2))/2);
			else if (predictionArray.size() == 5)
				 finalTradingPoint =  predictionArray.get(2);
			else
				finalTradingPoint = -1;
		}
		else  if (Const.OsFunctionEnum == Const.function_code.eGP &&
				Const.optimisationSelectedThreshold == Const.optimisation_selected_threshold.eperformanceScore){
			
		}
		
			return finalTradingPoint;
	}
}
