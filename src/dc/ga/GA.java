
package dc.ga;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import dc.EventWriter;
import dc.GP.AbstractNode;
import dc.GP.Const;
import dc.GP.TreeHelperClass;
import dc.GP.TreeOperation;
import dc.GP.Const.treeStructurePostcreate;
import dc.ga.DCCurve.Event;
import dc.ga.DCCurve.Type;
import dc.io.FReader;
import dc.io.Logger;
import files.FWriter;
import misc.SymbolicRegression;

import javax.script.ScriptEngineManager;
import javax.script.ScriptEngine;


public class GA {
	int numberOfThresholds;
	protected static double[] THRESHOLDS;
	protected static AbstractNode[] GP_TREES;
	protected static String[] GP_TREES_STRING;
	public static int NUM_OF_PROCESSORS=5;
	public static boolean LINEAR_FUNCTIONALITY_ONLY=false;
	public static int NEGATIVE_EXPRESSION_REPLACEMENT=2;
	public static boolean hasPrint = true;
	double thresholdIncrement;
	
	ConcurrentHashMap<Integer,String> gpDaysMap = null;
	int TrainingDay = -1;
	
	
	

	int currentGeneration;

	Double[] training;
	Double[] test;
	DCCurve[] curves;

	static double[][] pop;
	double[][] newPop;

	int POPSIZE;
	int tournamentSize;
	int MAX_GENERATIONS;

	double CROSSOVER_PROB;
	double MUTATION_PROB;

	static int nRuns;

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
	double previousFitness = -10.0;
	double bestTrainingFitnessSoFar = -10.0;

	protected static Random random;
	static Map<Double, String> thresholdGPStringMap = new HashMap<Double, String>();
	public static  Map<Double, String> thresholdGPStringUpwardMap  = new HashMap<Double, String>();
	public static  Map<Double, String> thresholdGPStringDownwardMap  = new HashMap<Double, String>();

	protected static Logger log;
	public static boolean HAS_OPEN_CLOSE = false;
	
	String filenameGLobal = "";
	
	private Fitness bestTestFitness = new Fitness();// to keep track of the best
													// test fitness, regardless
													// of which GA run we
													// currently are.

	final protected double transactionCost;
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

	public GA(String filename, int trainingIndexStart, int trainingIndexEnd, int testIndexStart, int testIndexEnd,
			int POPSIZE, int MAX_GENERATIONS, int tournamentSize, double CROSSOVER_PROB, double MUTATION_PROB,
			double thresholdIncrement, int numberOfThresholds, int MAX_QUANTITY, int budget,
			double shortSellingAllowance, double mddWeight, int xoverOperatorIndex, int mutOperatorIndex,
			double initialThreshold) throws IOException, ParseException {

		filenameGLobal = filename;
		double initial = initialThreshold;// 0.01
		TrainingDay = trainingIndexStart;
		  THRESHOLDS = new double[numberOfThresholds];
		  
		  gpDaysMap = FReader.loadDataMap(filename);
		  
			 Const.POP_SIZE = POPSIZE;
			Const.MAX_GENERATIONS = MAX_GENERATIONS;
		  Const.TOURNAMENT_SIZE = tournamentSize;
			// Const.CROSSOVER_PROB = 0.78;
			 Const.MAX_DEPTH = 5;

			
		 
		  //GP_TREES	= new AbstractNode[5];
		  //GP_TREES_STRING = new String[5];
		  for (int i = 0; i < THRESHOLDS.length; i++) { 
			 //  THRESHOLDS[i] = (initial * (i + 1)) / 100.0;
			  THRESHOLDS[i] = (initial + 	(thresholdIncrement * i)) / 100.0; 
			  String thresholdStr = String.format ("%.5f",THRESHOLDS[i]);
				System.out.println(thresholdStr);
				//String gpFileNamePrefix = gpDaysMap.get(trainingIndexStart);
				//System.out.println(gpFileNamePrefix);
			//	addGPTreeToArray(gpFileNamePrefix+"_"+thresholdStr+".txt",i);
		} 
		 
		
		
		this.POPSIZE = POPSIZE;
		this.MAX_GENERATIONS = MAX_GENERATIONS;
		this.tournamentSize = tournamentSize;
		this.CROSSOVER_PROB = CROSSOVER_PROB;
		this.MUTATION_PROB = MUTATION_PROB;
		this.thresholdIncrement = thresholdIncrement;
		this.numberOfThresholds = numberOfThresholds;
		this.MAX_QUANTITY = MAX_QUANTITY;
		GA.budget = budget;
		this.shortSellingAllowance = shortSellingAllowance;
		this.mddWeight = mddWeight;
		this.xoverOperatorIndex = xoverOperatorIndex;
		this.mutOperatorIndex = mutOperatorIndex;

		pop = new double[POPSIZE][THRESHOLDS.length + 5];// +5 because index 0
															// will be the
															// quantity, index 1
															// will be beta, and
															// index 2 will be
															// beta2, and index
															// 3 will be
															// shortSellingQuantity,
															// beta3
		newPop = new double[POPSIZE][THRESHOLDS.length + 5];

		nRuns = 50;

		transactionCost = 0.0;//0.001 / 100;// 0.001/100; the 0.025 is the proper
										// cost.
		slippageAllowance = 0 / 100 ;// 0.01/100 the 0.01 is the proper cost

		bestTestFitness.value = Double.NEGATIVE_INFINITY;

		MAX_SHORT_SELLING_QUANTITY = 1;// set to 1 to disable short-selling,
										// because the only result when I do
										// random.nextInt(1) in the
										// generateQuantity() method is 0.

		System.out.println("Loading directional changes data...");

		// loads the data
		ArrayList<Double[]> days = FReader.loadData(filename,HAS_OPEN_CLOSE);

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
		// budget = 100000;

		curves = new DCCurve[THRESHOLDS.length];

		System.out.println("DC curves:");
		log.save("GPExpressionDistribution.txt",
				"\tThreshold\tUpwardTrendCount\tUpwardTrendNegativeExpressionCount\tDownwardTrendCount\tDownwardTrendNegativeExpressionCount");

		
		for (int i = 0; i < curves.length; i++) {
			curves[i] = new DCCurve();
			
			
			String thresholdStr = String.format ("%.5f",THRESHOLDS[i]);
			//System.out.println(thresholdStr);
			String gpFileNamePrefix = gpDaysMap.get(trainingIndexStart);
			//System.out.println("GA:" +gpFileNamePrefix);
			String gpFileName = gpFileNamePrefix+"_"+thresholdStr+".txt";
			
			curves[i].filename = filename;
			curves[i].build(training, THRESHOLDS[i],gpFileName,true);
			
			//TODO for each curve (i.e. curves[i]) get best GP
			String expressionDistribution = i + "\t" + curves[i].numberOfUpwardEvent + "\t" + curves[i].numberOfNegativeUpwardEventGP + "\t" + curves[i].numberOfDownwardEvent + "\t" + curves[i].numberOfNegativeDownwardEventGP;
			log.save("GPExpressionDistribution.txt",expressionDistribution);
			System.out.println(String.format(
					"%1.3f%%: {length = %5d, meanRatio[up] = %02.2f, meanRatio[down] = %02.2f}", THRESHOLDS[i] * 100,
					curves[i].events.length, curves[i].meanRatio[1], curves[i].meanRatio[0]));
			log.save("Curves.txt",
					String.format("%1.3f%%: {length = %5d, meanRatio[up] = %02.2f, meanRatio[down] = %02.2f}",
							THRESHOLDS[i] * 100, curves[i].events.length, curves[i].meanRatio[1],
							curves[i].meanRatio[0]));
		}
		hasPrint = false;

		//System.out.println();
	}

	public void reBuild() {
		for (int i = 0; i < curves.length; i++) {
			String thresholdStr = String.format ("%.5f",THRESHOLDS[i]);
			String gpFileNamePrefix = gpDaysMap.get(TrainingDay);
			String gpFileName = gpFileNamePrefix+"_"+thresholdStr+".txt";
			System.out.println("rebuild::" + gpFileName );
			curves[i] = new DCCurve();
			curves[i].filename = filenameGLobal;
			curves[i].build(training, THRESHOLDS[i],gpFileName, true);
		}
	}
	
	public Fitness run(long seed, int currentRun) {
		if (seed == 0) {
			seed = System.currentTimeMillis();
		}

		random = new Random(seed);
		System.out.println("Starting GA...");
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

			report(t, fitness);

			/** tournament selection and crossover **/
			for (int p = 1; p < POPSIZE; p++)// 1 because of elitism
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
		
		/* Adesola 
		if (bestFitness < previousFitness){
			Const.REUSE_EXISTING_TREE = true;
			previousFitness = bestFitness;
			
		}
		else{
			if (bestTrainingFitnessSoFar > previousFitness)
				Const.REUSE_EXISTING_TREE = false;
			else{
				Const.REUSE_EXISTING_TREE = false;
			}
		}
		
		if (bestFitness < bestTrainingFitnessSoFar)
			bestTrainingFitnessSoFar = bestFitness;
*/

		
	
		/** fitness evaluation in the test set, of the best individual **/
		for (int i = 0; i < THRESHOLDS.length; i++)
		{
			String thresholdStr = String.format ("%.5f",THRESHOLDS[i]);
			
			String gpFileNamePrefix = gpDaysMap.get(TrainingDay);
			System.out.println("RUN:: "+gpFileNamePrefix);
			String gpFileName = gpFileNamePrefix+"_"+thresholdStr+".txt";
			boolean currentReuseFlagValue = Const.REUSE_EXISTING_TREE;
			Const.REUSE_EXISTING_TREE = true;
			curves[i].build(this.test, THRESHOLDS[i],gpFileName,false);// Re-building the curves
			Const.REUSE_EXISTING_TREE = currentReuseFlagValue;
														// when we are dealing
														// with the (unseen)
														// testing data.
														// Otherwise we evaluate
		}												// testing
		// fitness on training dc curves.
		Fitness f = fitness(pop[argBestFitness], true);
		
		
		Const.REUSE_EXISTING_TREE = false;
		

		double testFitness = f.value;// original fitness

		log.save("Fitness.txt", trainFitness + "\t" + testFitness);

		return f;
	}

	/**
	 * Initialises the GA population.
	 * 
	 */
	protected void initialisePop() {
		for (int i = 0; i < POPSIZE; i++) {
			pop[i][0] = generateQuantity(false);// first index we save the
												// quantity
			pop[i][1] = random.nextDouble();
			double beta2 = generateBeta2(pop[i][1]);
			pop[i][2] = beta2;
			pop[i][3] = generateQuantity(true); 
			pop[i][4] = random.nextDouble();// beta3

			for (int j = 5; j < pop[0].length; j++)// all columns of pop have
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

			while (quantity <= 0)
				quantity = random.nextInt(MAX_QUANTITY);
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

		for (int p = 0; p < POPSIZE; p++) {
			fitness[p] = fitness(pop[p], false);

			if (fitness[p].value > bestFitness) {
				bestFitness = fitness[p].value;
				argBestFitness = p;
			}
		}
		
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
					if (j == 0 || j == 3)// normal quantity (0) and
											// short-selling quantity (3)
						newPop[individual][j] = (j == 0) ? generateQuantity(false) : generateQuantity(true);// if
																											// j==0,
																											// then
																											// it's
																											// the
																											// normal
																											// quantity,
																											// otherwise
																											// it's
																											// quantity
																											// for
																											// short-selling
					else if (j == 2)
						newPop[individual][j] = generateBeta2(newPop[individual][1]);// generate
																						// a
																						// beta2
																						// which
																						// is
																						// greater
																						// than
																						// beta
																						// (newPop[individual][1])
					else// all other cases go here, even the j=1 for beta.
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
					if (j == 0 || j == 3)// normal quantity (0) and
											// short-selling quantity (3)
						newPop[individual][j] = (j == 0) ? generateQuantity(false) : generateQuantity(true);// if
																											// j==0,
																											// then
																											// it's
																											// the
																											// normal
																											// quantity,
																											// otherwise
																											// it's
																											// quantity
																											// for
																											// short-selling
					else if (j == 2)
						newPop[individual][j] = generateBeta2(newPop[individual][1]);// generate
																						// a
																						// beta2
																						// which
																						// is
																						// greater
																						// than
																						// beta
																						// (newPop[individual][1])
					else {// all other cases go here, even the j=1 for beta.
						double a = 0;
						double b = 1;
						double r = random.nextDouble();
						double tau = random.nextDouble() > 0.5 ? 1 : 0;

						newPop[individual][j] = (tau == 1)
								? pop[individual][j] + (b - pop[individual][j])
										* (1 - Math.pow(r, 1 - (double) currentGeneration / MAX_GENERATIONS))
								: pop[individual][j] - (pop[individual][j] - a)
										* (1 - Math.pow(r, 1 - (double) currentGeneration / MAX_GENERATIONS));
					}
				}
			}
		}
	}

	/**
	 * Tournament selection
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
			int choice = (int) Math.floor(random.nextDouble() * (double) POPSIZE);

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

				if (j == 2 && offspring[2] <= offspring[1]) {// ensuring that
																// offspring[2]
																// (beta2) is
																// greater than
																// beta
					offspring[2] = pop[first][j] > offspring[2] ? pop[first][j] : pop[second][j];

					// forced mutation in case beta2 is still less than beta.
					while (offspring[2] <= offspring[1])
						offspring[2] = random.nextDouble();
				}
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

				// forced mutation in case beta2 is still less than beta.
				while (offspring[2] <= offspring[1])
					offspring[2] = random.nextDouble();
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
				if (j == 0)// we need to generate an integer for the quantity
					offspring[j] = (int) (0.5 * pop[first][j] + 0.5 * pop[second][j]);// obtaining
																						// the
																						// arithmetic
																						// mean
																						// of
																						// the
																						// two
																						// parents
				else
					offspring[j] = 0.5 * pop[first][j] + 0.5 * pop[second][j];// obtaining
																				// the
																				// arithmetic
																				// mean
																				// of
																				// the
																				// two
																				// parents

				if (j == 2 && offspring[2] <= offspring[1]) {// ensuring that
																// offspring[2]
																// (beta2) is
																// greater than
																// beta
					offspring[2] = pop[first][j] > offspring[2] ? pop[first][j] : pop[second][j];

					// forced mutation in case beta2 is still less than beta.
					while (offspring[2] <= offspring[1])
						offspring[2] = random.nextDouble();
				}
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
			for (int j = 0; j < offspring.length; j++) {
				double cmin = pop[first][j] < pop[second][j] ? pop[first][j] : pop[second][j];
				double cmax = pop[first][j] > pop[second][j] ? pop[first][j] : pop[second][j];

				if (j == 0)// we need to generate an integer for the quantity
					offspring[j] = (int) (random.nextDouble() * (cmax - cmin) + cmin);// rnd
																						// number
																						// in
																						// the
																						// range
																						// [cmin,
																						// cmax]
				else
					offspring[j] = random.nextDouble() * (cmax - cmin) + cmin;// rnd
																				// number
																				// in
																				// the
																				// range
																				// [cmin,
																				// cmax]

				if (j == 2 && offspring[2] <= offspring[1]) {// ensuring that
																// offspring[2]
																// (beta2) is
																// greater than
																// beta
					offspring[2] = pop[first][j] > offspring[2] ? pop[first][j] : pop[second][j];

					// forced mutation in case beta2 is still less than beta.
					while (offspring[2] <= offspring[1])
						offspring[2] = random.nextDouble();
				}
			}
		} else {
			for (int j = 0; j < offspring.length; j++) {
				offspring[j] = pop[first][j];
			}
		}

		return offspring;
	}

	/** Fitness function: (Return - Maximum DrawDown) **/
	Fitness fitness(double[] individual, boolean test) {
		final double quantity = individual[0];
		final double beta = individual[1];
		final double beta2 = individual[2];
		// final double shortSellingQuantity = individual[3];//how many stocks
		// to short-sell
		final double beta3 = individual[4];

		double cash = budget;// set my initial cash to always be equal to a
								// fixed amount, for both training and testing
								// sets.
		double stock = 0;

		// number of operations not successful
		int uSell = 0;
		int uBuy = 0;
		int noop = 0;

		Type last = null;
		Double[] data = (test ? this.test : this.training);
		final int start = 0;
		final int length = data.length;

		// the length of the current overshoot
		int current = 0;

		// Maximum DrawDown variables
		double peak = Double.NEGATIVE_INFINITY;
		double trough = Double.POSITIVE_INFINITY;
		double peakMDD = Double.NEGATIVE_INFINITY;

		double DD = 0;// DrawDown
		double MDD = 0;// Maximum DrawDown

		int noOfTransactions = 0;
		int noOfShortSellingTransactions = 0;
		double shortSellingPrice = 0;
		
		ScriptEngineManager mgr = new ScriptEngineManager();
	    ScriptEngine engine = mgr.getEngineByName("JavaScript");

		// perform actions at every iteration, as long as certain conditions
		// hold
		// (e.g. enough stock, money, support for the upturn/downturn overshoot)
		for (int i = start; i < length; i++) {
			// sum of the weights
			double sell = 0.0;
			double buy = 0.0;

			// number of events over the threshold (beta)
			int upturnCount = 0;
			int downturnCount = 0;

			// increment the length of the current overshoot
			current++;

			// Peak and trough calculation
			if (data[i] > peak)
				peak = data[i];
			else if (data[i] < trough)
				trough = data[i];
			for (int j = 0; j < curves.length; j++) {
				// Using both a starting and an ending point for the threshold.
				// Actions have to take place within these two thresholds.
				
				int thresholdUpStart = -1;
				int thresholdUpEnd	 = -1;
				int thresholdDownStart = -1;
				int thresholdDownEnd = -1;
				if (Const.OsFunctionEnum == Const.function_code.eGP)
				{
					/* fallack to this if javascript fails
					String thresholdStr = String.format ("%.5f",THRESHOLDS[j]);
					double eval = GP_TREES[j].eval(curves[j].output[i].length());
					String evalStr = String.format ("%.10f",eval);
					int oslength = (curves[j].output[i].overshoot == null ) ? 0: curves[j].output[i].overshoot.length();
					if  ( eval == Double.MAX_VALUE || eval == Double.NEGATIVE_INFINITY ||
						eval == Double.POSITIVE_INFINITY || eval ==  Double.NaN ){
						System.out.println("Infinity Threshold:" + thresholdStr+" DC: " + curves[j].output[i].length()+  ". Observed OS:" + oslength + ". GP OS:" +eval + ". Michael OS:"+ curves[j].output[i].length() * curves[j].meanRatio[1]);
					}
					
					Double double1 = new Double(10000.000001);
					Double double2 = new Double(eval);
					double dbl = 10000.000001;
					if (eval > dbl)
					{
						System.out.println(double1.compareTo(double2) + "Too Big Threshold:" + thresholdStr+" DC: " + curves[j].output[i].length()+  ". Observed OS:" + oslength + ". GP OS:" +evalStr + ". Michael OS:"+ curves[j].output[i].length() * curves[j].meanRatio[1]);
					}
					*/
					
					/*
				    String foo = GP_TREES_STRING[j];//"(Math.pow(40+(2),(5)))";
				    foo = foo.replace("X0", Integer.toString(curves[j].output[i].length()));
				    double eval = 0.0;
				    try {
				    	Double javascriptValue = (Double)engine.eval(foo);
				    	eval = javascriptValue.doubleValue();
						System.out.println(eval);
					} catch (ScriptException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					*/
					if (Const.splitDatasetByTrendType){
						thresholdUpStart = (int)(curves[j].GpUpwardoutput[i] * beta);
						thresholdUpEnd = (int) (curves[j].GpUpwardoutput[i] * beta2);
						
						
						if ((thresholdUpStart !=0 &&   thresholdUpEnd !=0 ) && (thresholdUpStart ==  thresholdUpEnd))
							thresholdUpEnd = thresholdUpEnd + 1;
						
						thresholdDownStart = (int) (curves[j].GpDownwardoutput[i] * beta);
						thresholdDownEnd = (int) (curves[j].GpDownwardoutput[i] * beta2);
						
						if ((thresholdDownStart !=0 &&   thresholdDownEnd !=0 ) && (thresholdDownStart ==  thresholdDownEnd))
							thresholdDownEnd = thresholdDownEnd + 1;
						
					}
					else
					{
					thresholdUpStart = (int)(curves[j].gpprediction[i] * beta);
					thresholdUpEnd = (int) (curves[j].gpprediction[i] * beta2);
					
					if ((thresholdUpStart !=0 &&   thresholdUpEnd !=0 ) && (thresholdUpStart ==  thresholdUpEnd))
						thresholdUpEnd = thresholdUpEnd + 1;
					
					thresholdDownStart = (int) (curves[j].gpprediction[i] * beta);
					thresholdDownEnd = (int) (curves[j].gpprediction[i] * beta2);
					
					if ((thresholdDownStart !=0 &&   thresholdDownEnd !=0 ) && (thresholdDownStart ==  thresholdDownEnd))
						thresholdDownEnd = thresholdDownEnd + 1;
					}
				}
				else if (Const.OsFunctionEnum == Const.function_code.eMichaelFernando)
				{
					//System.out.println(double1.compareTo(double2) + "Too Big Threshold:" + thresholdStr+" DC: " + curves[j].output[i].length()+  ". Observed OS:" + oslength + ". GP OS:" +evalStr + ". Michael OS:"+ curves[j].output[i].length() * curves[j].meanRatio[1]);
					thresholdUpStart = (int) (curves[j].output[i].length() + (curves[j].output[i].length() * curves[j].meanRatio[1] * beta));
					thresholdUpEnd = (int) (curves[j].output[i].length() +(curves[j].output[i].length() * curves[j].meanRatio[1] * beta2));
					if (thresholdUpStart ==  thresholdUpEnd)
						thresholdUpEnd = thresholdUpEnd + 1;

					// We use a different meanRatio here (different array index), as
					// now we deal with downturn events.
					thresholdDownStart = (int) (curves[j].output[i].length() + (curves[j].output[i].length() * curves[j].meanRatio[0] * beta));
					thresholdDownEnd = (int) (curves[j].output[i].length() +(curves[j].output[i].length() * curves[j].meanRatio[0] * beta2));
					
					if (thresholdDownStart ==  thresholdDownEnd)
						thresholdDownEnd = thresholdDownEnd + 1;
				}
				else if (Const.OsFunctionEnum == Const.function_code.eOlsen)
				{
					//System.out.println(double1.compareTo(double2) + "Too Big Threshold:" + thresholdStr+" DC: " + curves[j].output[i].length()+  ". Observed OS:" + oslength + ". GP OS:" +evalStr + ". Michael OS:"+ curves[j].output[i].length() * curves[j].meanRatio[1]);
					
					thresholdUpStart = (int) ((curves[j].output[i].length() + (curves[j].output[i].length() * 2)) * beta);
					thresholdUpEnd = (int) ((curves[j].output[i].length() + (curves[j].output[i].length() * 2)) * beta2);
					
					if (thresholdUpStart ==  thresholdUpEnd)
						thresholdUpEnd = thresholdUpEnd + 1;

					// We use a different meanRatio here (different array index), as
					// now we deal with downturn events.
					thresholdDownStart = (int) ((curves[j].output[i].length() + (curves[j].output[i].length() * 2))* beta);
					thresholdDownEnd = (int) ((curves[j].output[i].length() + (curves[j].output[i].length() * 2)) * beta2);
					
					if (thresholdDownStart ==  thresholdDownEnd)
						thresholdDownEnd = thresholdDownEnd + 1;

					
				}
				else{
					System.out.println("Unknown OsFunctionEnum");
					System.exit(0);
				}
				
				if (Const.OsFunctionEnum == Const.function_code.eGP ){
					if (curves[j].output[i].type == Type.Downturn && (thresholdDownStart == 0  && thresholdDownEnd == 0)){
					
						thresholdUpStart = (int) ((curves[j].output[i].length() +curves[j].AverageDCEventLength[1])  *beta);
						thresholdUpEnd = (int) ((curves[j].output[i].length() + curves[j].AverageDCEventLength[1] ) *beta2);
						if (thresholdUpStart ==  thresholdUpEnd)
							thresholdUpEnd = thresholdUpEnd + 1;
						// upturn there is no overshoot
						if (thresholdUpStart == 0  && thresholdUpEnd == 0)
							continue;
						sell += individual[j + 5];// +5, because we are not taking
													// into account the first 5
													// indices, which represent the
													// quantity, beta and beta2, and
													// shortSellingQuantity, beta3

						if (current >= thresholdUpStart && current < thresholdUpEnd)//
						{
							upturnCount++;
						} else {
							upturnCount--;
						}
					} 
					else if (curves[j].output[i].type == Type.Upturn && (thresholdUpStart == 0  && thresholdUpEnd == 0)){
						
						thresholdDownStart = (int) ((curves[j].output[i].length() + curves[j].AverageDCEventLength[0]) *beta);
						thresholdDownEnd = (int) ((curves[j].output[i].length() + curves[j].AverageDCEventLength[0]) *beta2);
						
						if (thresholdDownStart ==  thresholdDownEnd)
							thresholdDownEnd = thresholdDownEnd + 1;

						// do downturn because there is no overshoot
						buy += individual[j + 5];// +5, because we are not taking
													// into account the first 5
													// indices, which represent the
													// quantity, beta and beta2, and
													// shortSellingQuantity, beta3
						if (current >= thresholdDownStart && current < thresholdDownEnd)//
						{
							downturnCount++;
						} else {
							downturnCount--;
						}
					}
					else{
						if (curves[j].output[i].type == Type.Downturn) {
							
							if (thresholdDownStart == 0  && thresholdDownEnd == 0)
								continue;
							// downturn
							buy += individual[j + 5];// +5, because we are not taking
														// into account the first 5
														// indices, which represent the
														// quantity, beta and beta2, and
														// shortSellingQuantity, beta3
							if (current >= thresholdDownStart && current < thresholdDownEnd)//
							{
								downturnCount++;
							} else {
								downturnCount--;
							}
						} else {
							// upturn
							if (thresholdUpStart == 0  && thresholdUpEnd == 0)
								continue;
							sell += individual[j + 5];// +5, because we are not taking
														// into account the first 5
														// indices, which represent the
														// quantity, beta and beta2, and
														// shortSellingQuantity, beta3

							if (current >= thresholdUpStart && current < thresholdUpEnd)//
							{
								upturnCount++;
							} else {
								upturnCount--;
							}
						}
					}
				}
				
				else{ // Original starts here
				
				if (curves[j].output[i].type == Type.Downturn) {
					
					if (thresholdDownStart == 0  && thresholdDownEnd == 0)
						continue;
					// downturn
					buy += individual[j + 5];// +5, because we are not taking
												// into account the first 5
												// indices, which represent the
												// quantity, beta and beta2, and
												// shortSellingQuantity, beta3
					if (current >= thresholdDownStart && current < thresholdDownEnd)//
					{
						downturnCount++;
					} else {
						downturnCount--;
					}
				} else {
					// upturn
					if (thresholdUpStart == 0  && thresholdUpEnd == 0)
						continue;
					sell += individual[j + 5];// +5, because we are not taking
												// into account the first 5
												// indices, which represent the
												// quantity, beta and beta2, and
												// shortSellingQuantity, beta3

					if (current >= thresholdUpStart && current < thresholdUpEnd)//
					{
						upturnCount++;
					} else {
						upturnCount--;
					}
				}
				} //Original ends here
			}

			if (sell > buy) {
				// we only perform the sell if we have enough
				// support for the downturn overshoot
				if (upturnCount > 0 && data[i] >= peak * beta3) {
					// the quantity to trade depends on the chromosome and how
					// many curves are advising to sell (upturnCount)
					// So by dividing this count with the number of thresholds,
					// I get a percentage, which I then multiply
					// with the quantity given by the chromosome. In this way, I
					// sell even more when more curves advise me
					// to do so.
					int quantityToTrade = (int) Math
							.ceil(quantity + ((double) upturnCount / numberOfThresholds) * quantity);
					double deductions = quantityToTrade * data[i] * (transactionCost + slippageAllowance);

					// Don't want to overdo it with short-selling, as a training
					// data set might have a very long downward period,
					// which will result in excessive short-selling; however, if
					// the test data set does not have such long
					// downward periods, the seemingly successful trading
					// strategy in training, performs EXTREMELY bad in test,
					// leading to significant losses. To deal with this, we have
					// introduced a new condition, where the short-selling
					// takes place as long as the existing short-sold stock does
					// not exist our initial budget * Parameter. This
					// is to ensure that we don't do too much short-selling,
					// only do that much that will cost at most your
					// initial budget * Parameter. Parameter can be anything
					// above 0. Obviously, with a very low Parameter, we are
					// very restrictive towards short-selling, whereas with high
					// values we are generally loose towards short-selling.
					// Of course, I allow ordinary selling when stock > 0.
					if (stock > 0 || (stock <= 0 && Math.abs(stock) * data[i]
							+ Math.abs(stock) * data[i] * (transactionCost + slippageAllowance) <= shortSellingAllowance
									* budget)) {
						if (quantityToTrade > stock && stock > 0)
							quantityToTrade = (int) stock; //Math.abs((int)(stock - quantityToTrade));
						
						stock -= quantityToTrade;

						cash += quantityToTrade * data[i];
						deductions = quantityToTrade * data[i] * (transactionCost + slippageAllowance);
						cash -= deductions;// adjusting, after allowing trading cost and slippage	
						

						if (stock < 0) {
							noOfShortSellingTransactions++;
							shortSellingPrice = data[i];
						} else
							noOfTransactions++;
					}
				} else {
					noop++;
				}

				if (last == null) {
					last = Type.Downturn;
				} else if (last != Type.Downturn) {
					current = 0;
					last = Type.Downturn;
				}
			} else if (sell < buy) {
				if (downturnCount > 0 && data[i] <= (trough + trough * (1 - beta3))) {
					// the quantity to trade depends on the chromosome and how
					// many curves are advising to buy (downturnCount)
					// So by dividing this count with the number of thresholds,
					// I get a percentage, which I then multiply
					// with the quantity given by the chromosome. In this way, I
					// buy even more when more curves advise me
					// to do so.
					int quantityToTrade = (int) Math
							.ceil(quantity + ((double) downturnCount / numberOfThresholds) * quantity);

					// closing the short-selling position, as long as the
					// current price is less than the one when I short-sold
					// OR
					// simple BUY, as long as we have a positive stock; this
					// means we are not trying to close a short-selling
					// position. Checks
					// if we have enough cash to go forward with the buy, follow
					// below in a few lines.

					double deductions = quantityToTrade * data[i] * (transactionCost + slippageAllowance);

					// Need to have enough cash to cover the stock purchase and
					// the costs
					// Sign for deductions is (+), added to the stock money
					// we'll be spending, and all of this together is subtracted
					// from our cash.
					if (cash > (quantityToTrade * data[i] + deductions)) {
						if (stock < 0)
							noOfShortSellingTransactions++;
						else
							noOfTransactions++;

						cash -= (quantityToTrade * data[i] + deductions);
						stock += quantityToTrade;
					} else {// Don't have enough cash to cover all of the
							// quantity, but at least close short-selling
							// position for some (or at least simple-buy).
							// Calculate deductions for a single quantity
							// (quantity=1)
						double singleQuantityDeductions = data[i] * (transactionCost + slippageAllowance);
						// Expenses for a single quantity are the cost of the
						// current price (data[i]), plus the deductions.
						double expenses = data[i] + singleQuantityDeductions;
						// Find out how much I can afford with my current cash
						quantityToTrade = (int) (cash / expenses);

						if (quantityToTrade > 0) {
							if (stock < 0)
								noOfShortSellingTransactions++; 
							else
								noOfTransactions++;

							cash -= quantityToTrade * data[i];// Close some of
																// the positions
							deductions = quantityToTrade * data[i] * (transactionCost + slippageAllowance);
							cash -= deductions;// Deductions/costs
							stock += quantityToTrade;// Update the quantity of
														// the closed positions
						} else
							uBuy++;
					}
				} else {
					noop++;
				}

				if (last == null) {
					last = Type.Upturn;
				} else if (last != Type.Upturn) {
					current = 0;
					last = Type.Upturn;
				}
			}

			double wealth = cash + stock * data[i] - Math.abs(stock) * data[i] * (transactionCost + slippageAllowance);// wealth
																														// for
																														// MDD
																														// purposes

			if (wealth > peakMDD)
				peakMDD = wealth;

			DD = 100.0 * (peakMDD - wealth) / peakMDD;

			if (DD > MDD)
				MDD = DD;
			// End of MDD calculation
		}
		Fitness fitness = new Fitness();

		// My realisedProfit is equal to my wealth=cash + value of stocks I owe
		// either due to buying or short-selling activity. If, however, I end up
		// with a negative shortSellingStock number;
		// so in this occasion the value of my short selling stocks is
		// subtracted from my total fitness, as I need to re-buy the stocks to
		// return them to my broker. I also subtract the
		// transaction cost and slippage. Note that even though I have a plus
		// (+) sign in the equation, in fact there is a subtraction, as the
		// shortSellingStock has a negative value
		// in the statement below. In addition, I need to subtract the
		// transaction costs.
		double realisedProfit = cash + stock * data[length - 1]
				- (Math.abs(stock)) * data[length - 1] * (transactionCost + slippageAllowance) - budget;

		fitness.uSell = uSell;
		fitness.uBuy = uBuy;
		fitness.noop = noop;
		fitness.realisedProfit = realisedProfit;
		fitness.MDD = MDD;
		fitness.wealth = realisedProfit + budget;// my wealth, at the end of the
													// transaction period
		fitness.Return = 100.0 * (fitness.wealth - budget) / budget;
		fitness.value = fitness.Return - mddWeight * fitness.MDD;
		fitness.noOfTransactions = noOfTransactions;
		fitness.noOfShortSellingTransactions = noOfShortSellingTransactions; 
		
		
			

		// A single transaction is dangerous, because it's based on pure luck,
		// whether the last day's data is preferable, and can lead to a positive
		// position. So better to avoid this,
		// and require to have more than 1 transaction. We of course only do
		// this for the training set (test == false); with test data, we want to
		// have the real/true fitness, so we
		// don't want to mess with this number - not doing search any more, no
		// reason for penalising.
		if (fitness.noOfTransactions + fitness.noOfShortSellingTransactions == 1 && test == false) {
			fitness.value = -9999;// Heavily penalise individuals with a single
									// transaction
		}

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
		log.save("Logger.txt", String.format("%d\t%12.6f\t%12.6f\t%12.6f", generation, best, worst, average));
		if (generation == MAX_GENERATIONS - 1)
			System.out.println("Number of transactions of best individual in training: "
					+ (fitness[bestIndividualIndex].noOfTransactions
							+ fitness[bestIndividualIndex].noOfShortSellingTransactions));
	}

	public void saveResults(Fitness f, int i) {
		// Saving to Results.txt file
		log.save("Results.txt", String.format("Run " + i + "\t%10.6f\t%10.6f\t%10.6f\t%10.6f\t%10.6f\t%d\t%d", f.wealth,
				f.Return, f.value, f.realisedProfit, f.MDD, f.noOfTransactions, f.noOfShortSellingTransactions));// saving
																													// and
																													// reporting
																													// the
																													// fitness,
																													// realised
																													// profit,
																													// MDD,
																													// and
																													// wealth

		System.out.println();
		System.out.println(String.format("Fitness on test set: %10.6f", f.value));
		System.out.println(String.format("Realised profit on test set: %10.6f", f.realisedProfit));
		System.out.println(String.format("MDD test set: %10.6f", f.MDD));
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
		log.save("Solutions.txt", "Quantity: " + pop[argBestFitness][0]);
		log.save("Solutions.txt", "Beta: " + pop[argBestFitness][1]);
		log.save("Solutions.txt", "Beta2: " + pop[argBestFitness][2]);
		log.save("Solutions.txt", "Short-selling quantity: " + pop[argBestFitness][3]);
		log.save("Solutions.txt", "Beta3: " + pop[argBestFitness][4]);
		log.save("Solutions.txt", "Threshold weights: ");

		for (int m = 0; m < THRESHOLDS.length; m++) {
			System.out.println(String.format("%1.3f%%: %7.6f", THRESHOLDS[m] * 100, pop[argBestFitness][m + 5]));// +5,
																													// coz
																													// the
																													// first
																													// 5
																													// are
																													// quantities
																													// and
																													// the
																													// betas
			log.save("Solutions.txt", String.format("%1.3f%%: %7.6f", THRESHOLDS[m] * 100, pop[argBestFitness][m + 5]));
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
		
		if (s[6] != null){
			switch (Const.hashFunctionType(s[6])) {
				case eMichaelFernando:
					Const.OsFunctionEnum = Const.function_code.eMichaelFernando; break;
				case eGP: 
					Const.OsFunctionEnum = Const.function_code.eGP; break;
				case eOlsen:
					Const.OsFunctionEnum = Const.function_code.eOlsen; break;
				default:{
					System.out.println(	"Wrong OS estimator defined ");
					System.exit(1);
				}
			}
		}
		else
		{
			System.out.println(	"Wrong OS estimator defined 2 ");
			System.exit(1);
		}
		
		try{
			if(s[7] != null){
				try{
					if (s[7].compareToIgnoreCase("TRUE")==0)
						Const.splitDatasetByTrendType = true;
				}
				catch(NumberFormatException nfe)  
				{  
					Const.splitDatasetByTrendType = false;  
				} 	
			}
		}
		catch(ArrayIndexOutOfBoundsException aie){
			Const.splitDatasetByTrendType = false;  
		}
		
		if (s[8].compareToIgnoreCase("TRUE")==0){
			LINEAR_FUNCTIONALITY_ONLY=true;
			System.out.println("LINEAR_FUNCTIONALITY_ONLY is TRUE");
		}
		else{
			System.out.println("LINEAR_FUNCTIONALITY_ONLY is FALSE");
			LINEAR_FUNCTIONALITY_ONLY = false;
		}
		NEGATIVE_EXPRESSION_REPLACEMENT=Integer.parseInt(s[9]);
		
		try{
			if(s[8] != null){
				try{
				NUM_OF_PROCESSORS = Integer.parseInt(s[10]);
				}
				catch(NumberFormatException nfe)  
				{  
					NUM_OF_PROCESSORS = 5;  
				} 	
			}
		}
		catch(ArrayIndexOutOfBoundsException aie){
			NUM_OF_PROCESSORS = 5;  
		}
		
		try{
			
		
			if(s[11] != null){
				
					
					if (s[11].compareToIgnoreCase("TRUE")==0){
						HAS_OPEN_CLOSE=true;
						System.out.println("HAS_OPEN_CLOSE is TRUE");
					}
					else{
						System.out.println("HAS_OPEN_CLOSE is FALSE");
						HAS_OPEN_CLOSE = false;
					}
				
			}
			
			if (s.length >= 12) 
			{
				switch (Const.hashtreeStructurePostcreateType(s[12])) {
				case ePruneAndEqualERCAndExternalInputLeaf:
					TreeHelperClass.treeStructurePostcreateObj = treeStructurePostcreate.ePruneAndEqualERCAndExternalInputLeaf;
					break;
				case ePrune:
					TreeHelperClass.treeStructurePostcreateObj = treeStructurePostcreate.ePrune;
					break;
				case eEqualERCAndExternalInputLeaf:
					TreeHelperClass.treeStructurePostcreateObj = treeStructurePostcreate.eEqualERCAndExternalInputLeaf;
					break;
				case eRandom:
					TreeHelperClass.treeStructurePostcreateObj = treeStructurePostcreate.eRandom;
					break;
				default:
					TreeHelperClass.treeStructurePostcreateObj = treeStructurePostcreate.eRandom;
					break;
				}
			}
		
			
			if(s[13] != null){
				
				
				if (s[13].compareToIgnoreCase("TRUE")==0){
					Const.INCLUDE_ZERO_OS_ITEMS=true;
					System.out.println("INCLUDE_ZERO_OS_ITEMS is TRUE");
				}
				else{
					System.out.println("INCLUDE_ZERO_OS_ITEMS is FALSE");
					Const.INCLUDE_ZERO_OS_ITEMS = false;
				}
			}
			
			if(s[14] != null){
				
				
				if (s[14].compareToIgnoreCase("TRUE")==0){
					Const.REUSE_EXISTING_TREE=true;
					System.out.println("REUSE_EXISTING_TREE is TRUE");
				}
				else{
					System.out.println("REUSE_EXISTING_TREE is FALSE");
					Const.REUSE_EXISTING_TREE = false;
				}
			}
			
			
		}
		catch(ArrayIndexOutOfBoundsException aie){
		}
		log = new Logger(s[1], s[3], s[4]);
		
		GA ga = new GA(s[0], Integer.parseInt(s[2]), Integer.parseInt(s[3]), Integer.parseInt(s[4]),
				Integer.parseInt(s[5]), Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]),
				Double.parseDouble(args[4]), Double.parseDouble(args[5]), Double.parseDouble(args[6]),
				Integer.parseInt(args[7]), Integer.parseInt(args[8]), Integer.parseInt(args[9]),
				Double.parseDouble(args[10]), Double.parseDouble(args[11]), Integer.parseInt(args[12]),
				Integer.parseInt(args[13]), Double.parseDouble(args[14]));

		log.save("Results.txt",
				"\tWealth\tReturn\tFitness\tRealised Profit\tMDD\tNoOfTransactions\tNoOfShortSellingTransactions");
		log.save("Fitness.txt", "Train fitness\tTest fitness");
		
		//String classificationresult = "Dataset \t Threshold \t TotalMissedOvershoot \t TotalMissedOvershootLength \t TotalAssumedOvershoot \t PossibleOvershoot \t FoundOvershootLength \t TotalFoundOvershoot  \t totalDcEvent \t testAccuracy \t testPrecision \t testRecall" ;
		//System.out.println( classificationresult );
		//FWriter Classicationwriter = new FWriter(log.publicFolder +"ClassificationAnalysis.txt");
		//log.save("ClassificationAnalysis.txt",classificationresult);
		
		
		for (int i = 0; i < nRuns; i++) {
			System.out.println("=========================== Run " + i + "==========================");
			log.save("Logger.txt", "\n=========================== Run " + i + "==========================");

			Fitness f = ga.run(seed, i);
			
			ga.saveResults(f, i);

			ga.reBuild();
		}
		log.save("Results.txt", "\n\nTesting Budget\t" + budget);
		//TreeHelperClass.readFitnessFile(2);
	}

	public static class Fitness {
		public double value;

		public int uSell;

		public int uBuy;

		public int noop;

		public double realisedProfit;

		public double MDD;

		public double Return;

		public double wealth;

		public int noOfTransactions;

		public int noOfShortSellingTransactions;

	}
	
	void addGPTreeToArray(String gpFileName, int arrayPosition)
	{
		TreeOperation opr = null;
		try {
			opr = new TreeOperation(gpFileName, 0.0);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(0);
		}
		if (opr != null)
		{
			opr.populateGpTreeVector();
		
			AbstractNode  mytree =  opr.getTree();
			String  testStr = TreeHelperClass.printTreeToString(mytree,0);
			
		
			//System.out.println(testStr);
			GP_TREES[arrayPosition] = mytree;
			GP_TREES_STRING[arrayPosition] = mytree.printAsInFixFunction();
		}
	}
}