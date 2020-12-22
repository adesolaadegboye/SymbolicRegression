package dc.ga;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import dc.EventWriter;
import dc.ga.DCCurve.Type;
import dc.ga.GA.Fitness;
import dc.io.FReader;
import dc.io.Logger;

/** 
 *  DEPRECATED DEPRECATED DEPRECATED DEPRECATED DEPRECATED DEPRECATED DEPRECATED DEPRECATED 
 *  DEPRECATED DEPRECATED DEPRECATED DEPRECATED DEPRECATED DEPRECATED DEPRECATED DEPRECATED 
 *  DEPRECATED DEPRECATED DEPRECATED DEPRECATED DEPRECATED DEPRECATED DEPRECATED DEPRECATED 
 * The purpose of this class is to report the fitness performance on the testing set of each individual fixed threshold. This is effectively the same class as the GA class. 
 * I've kept pretty much all GA code, with the difference that there's no evolution really happening, as I only allow 1 run, 1 individual, 1 generation, quantities of 1. The point here is not
 * to evolve new individuals, but simply use the fitness calculation capabilities of the GA class and evaluate the performance of each fixed threshold. Results are saved in the 
 * Results.txt file. 
 *  DEPRECATED DEPRECATED DEPRECATED DEPRECATED DEPRECATED DEPRECATED DEPRECATED DEPRECATED 
 *  DEPRECATED DEPRECATED DEPRECATED DEPRECATED DEPRECATED DEPRECATED DEPRECATED DEPRECATED 
 *  DEPRECATED DEPRECATED DEPRECATED DEPRECATED DEPRECATED DEPRECATED DEPRECATED DEPRECATED 
 * **/

public class DCCurvePerformanceOld{

	private static final double[] THRESHOLDS = new double[1];//[10]
	//private static final double[] THRESHOLDS = { 0.01, 0.02, 0.03, 0.04, 0.05, 0.06, 0.07, 0.08, 0.09, 0.1 };
	//private static final double[] THRESHOLDS = { 0.01, 0.02, 0.03, 0.04, 0.05 };

	static
	{
		double initial = 0.01;//0.005//0.01

		for (int i = 0; i < THRESHOLDS.length; i++)
		{
			THRESHOLDS[i] = (initial * (i + 1)) / 100.0;
		}
	}

	Double[] training;
	Double[] test;
	DCCurve[] curves;

	double[][] pop;
	double[][] newPop;

	int POPSIZE;
	int tournamentSize;
	int MAX_GENERATIONS;

	double CROSSOVER_PROB;
	double MUTATION_PROB;

	static int nRuns;

	double bestFitness;
	int argBestFitness;
	static double budget;

	final int MAX_QUANTITY;//maximum quantity we can possibly buy/sell. Random quantities are generated in the range [1, MAX_QUANTITY).
	final int MAX_SHORT_SELLING_QUANTITY;//maximum quantity we can possibly short-sell. Random quantities are generated in the range [1, MAX_SHORT_SELLING_QUANTITY).
	
	private static Random random;

	private static Logger log;

	public DCCurvePerformanceOld(String filename, int trainingIndex, int testIndex)
			throws IOException {
		POPSIZE = 1;
		pop = new double[POPSIZE][THRESHOLDS.length+4];//+4 because index 0 will be the quantity, index 1 will be beta, and index 2 will be beta2, and index 3 will be shortSellingQuantity
		newPop = new double[POPSIZE][THRESHOLDS.length+4];

		tournamentSize = 2;
		MAX_GENERATIONS = 1;
		CROSSOVER_PROB = 0.90;
		MUTATION_PROB = 0.10;
		nRuns = 1;

		MAX_QUANTITY = 2;
		MAX_SHORT_SELLING_QUANTITY = 1;//set to 1 to disable short-selling, because the only result when I do random.nextInt(1) in the generateQuantity() method is 0.
		
		System.out.println("Loading directional changes data...");

		// loads the data
		ArrayList<Double[]> days = FReader.loadData(filename);

		training = days.get(trainingIndex);
		test = days.get(testIndex);
		budget = 10000;//		budget = training[0] * 1000;

		curves = new DCCurve[THRESHOLDS.length];

		System.out.println("DC curves:");

		for (int i = 0; i < curves.length; i++)
		{
			curves[i] = new DCCurve();
			curves[i].build(training, THRESHOLDS[i]);

			System.out.println(String.format("%1.3f%%: {length = %5d, meanRatio[up] = %02.2f, meanRatio[down] = %02.2f}",
					THRESHOLDS[i] * 100, curves[i].events.length, curves[i].meanRatio[1], curves[i].meanRatio[0]));
			log.save("Curves.txt", String.format("%1.3f%%: {length = %5d, meanRatio[up] = %02.2f, meanRatio[down] = %02.2f}",
					THRESHOLDS[i] * 100, curves[i].events.length, curves[i].meanRatio[1], curves[i].meanRatio[0]));
		}

		System.out.println();
			}


	public void reBuild(){
		for (int i = 0; i < curves.length; i++)
		{
			curves[i] = new DCCurve();
			curves[i].build(training, THRESHOLDS[i]);
		}
	}

	public void run(long seed, int currentRun)
	{
		if (seed == 0)
		{
			seed = System.currentTimeMillis();
		}

		random = new Random(seed);
		System.out.println("Starting GA...");
		System.out.println(String.format("Random seed: %d", seed));
		System.out.println("Training budget: " + budget);
		System.out.println("Test budget: " + budget);
		System.out.println();

		/** initialise population, i.e. pass random weights to each individual **/
		initialisePop();

		System.out.println("Generation\tBest\tWorst\tAverage");
		log.save("Logger.txt", "Generation\tBest\tWorst\tAverage");
		for (int t = 0; t < MAX_GENERATIONS; t++)
		{
			/** fitness evaluation **/
			Fitness[] fitness = popFitnessEvaluation();

			/** elitism **/
			for (int j = 0; j < THRESHOLDS.length; j++)
			{
				newPop[0][j] = pop[argBestFitness][j];
			}

			report(t, fitness);

			/** tournament selection and crossover **/
			for (int p = 1; p < POPSIZE; p++)//1 because of elitism
			{
				//select first
				int first = tournament(fitness);

				//select second
				int second = tournament(fitness);

				// uniform crossover
				newPop[p] = crossover(first, second);

			}//end of going through population

			/** point mutation **/
			for (int p = 1; p < POPSIZE; p++)//1 because of elitism
			{
				mutation(p);
			}

			/** copy new pop into old pop **/
			copyPopulation();

		}//end of generation loop

		/** fitness evaluation **/
		popFitnessEvaluation();

		/** fitness evaluation in the test set, of the best individual**/
		for (int i = 0 ; i < THRESHOLDS.length; i++)
			curves[i].build(this.test, THRESHOLDS[i]);//Re-building the curves when we are dealing with the (unseen) testing data. Otherwise we evaluate testing
		//fitness on training dc curves. CHECK WITH FERNANDO.
		Fitness f = fitness(pop[argBestFitness], true);

		System.out.println();
		System.out.println(String.format("Fitness on test set: %10.6f", f.value));
		System.out.println(String.format("Unsuccessful buys: %d", f.uBuy));
		System.out.println(String.format("Unsuccessful sells: %d", f.uSell));
		System.out.println(String.format("No-ops: %d", f.noop));
		System.out.println();
		System.out.println(">>>>> Solution:\n");

		for (int i = 0; i < THRESHOLDS.length; i++)
		{
			System.out.println(String.format("%1.3f%%: %7.6f", THRESHOLDS[i] * 100, pop[argBestFitness][i]));
			log.save("Solutions.txt", String.format("%1.3f%%: %7.6f", THRESHOLDS[i] * 100, pop[argBestFitness][i]));
		}

		log.save("Results.txt", String.format(THRESHOLDS[0] * 100 + "\t%10.6f", f.value));
	}

	/**
	 * Initialises the GA population.
	 * 
	 */
	private void initialisePop() {
		for (int i = 0; i < POPSIZE; i++)
		{
			for (int j = 0; j < THRESHOLDS.length; j++)
			{
				pop[i][j] = random.nextDouble();
			}
		}
	}

	/**
	 * Calculates the fitness of all individuals in the population
	 * 
	 * @return Fitness The array of fitness for the population
	 */
	private Fitness[] popFitnessEvaluation() {
		Fitness[] fitness = new Fitness[POPSIZE];
		bestFitness = -1;
		argBestFitness = -999;

		for (int p = 0; p < POPSIZE; p++)
		{
			fitness[p] = fitness(pop[p], false);

			if (fitness[p].value > bestFitness)
			{
				bestFitness = fitness[p].value;
				argBestFitness = p;
			}
		}
		return fitness;
	}

	/**
	 * Copies the intermediate population (newPop) to the original population (pop)
	 */
	private void copyPopulation() {
		for (int i = 0; i < POPSIZE; i++)
		{
			for (int j = 0; j < THRESHOLDS.length; j++)
			{
				pop[i][j] = newPop[i][j];
			}
		}
	}

	/**
	 * Mutation
	 * @param individual The individual to be mutated
	 */
	private void mutation(int individual) {
		if (random.nextDouble() < MUTATION_PROB)
		{
			for (int j = 0; j < THRESHOLDS.length; j++)
			{
				if (random.nextDouble() > 0.5)
				{
					newPop[individual][j] = random.nextDouble();
				}
			}
		}
	}

	/**
	 * Tournament selection
	 * @param fitness The fitness array of the population
	 * @return argSmallest The position/index of the individual winning the tournament
	 */
	private int tournament(Fitness[] fitness){
		double smallest = -1;
		int argSmallest = -999;

		for (int i = 0; i < tournamentSize; i++)
		{
			int choice =
					(int) Math.floor(random.nextDouble() * (double) POPSIZE);
			double fit = fitness[choice].value;

			if (fit > smallest)
			{
				argSmallest = choice;
				smallest = fit;
			}
		}

		return argSmallest;

	}

	/**
	 * Crossover
	 * 
	 * @param first The index of the first parent
	 * @param second The index of the second parent
	 * 
	 * @return offspring The array of weights of the offspring
	 */
	private double[] crossover(int first, int second){

		double[] offspring = new double[THRESHOLDS.length];

		if (random.nextDouble() < CROSSOVER_PROB)
		{
			for (int j = 0; j < THRESHOLDS.length; j++)
			{
				offspring[j] =
						random.nextDouble() > 0.5 ? pop[first][j]
								: pop[second][j];
			}
		}
		else
		{
			for (int j = 0; j < THRESHOLDS.length; j++)
			{
				offspring[j] = pop[first][j];
			}
		}

		return offspring;
	}

	/** Deprecated **/
	Fitness fitness(double[] weights, boolean test)
	{
		final double beta = 0.95;
		final double beta2 = 1.0;

		// downturn: sell (avoid losing money on the overshoot)
		// upturn: buy (gain money as a result of the overshoot)

		//		double cash = (test ? 1000 * training[0] : 1000 * training[0]);//set my initial cash to always be equal to a fixed amount, for both training and testing sets.
		double cash = budget;//set my initial cash to always be equal to a fixed amount, for both training and testing sets.
		double stock = 0;

		// number of operations not successful
		int uSell = 0;
		int uBuy = 0;
		int noop = 0;

		Type last = null;
		Double[] data = (test ? this.test : this.training);
		//final int start = (test ? 1000 : 0);
		//final int length = start + (test ? 500 : 1000);
		final int start = 0;
		final int length = data.length;

		// the length of the current overshoot
		int current = 0;

		// perform actions at every iteration, as long as certain conditions hold 
		// (e.g. enough stock, money, support for the upturn/downturn overshoot)
		for (int i = start; i < length; i++)
		{
			// sum of the weights
			double sell = 0.0;
			double buy = 0.0;

			// number of events over the threshold (beta)
			int upturnCount = 0;
			int downturnCount = 0;

			// increment the length of the current overshoot
			current++;

			for (int j = 0; j < curves.length; j++)
			{
				// events always start at index 1
				/*
                Event event = curves[j].findEvent(i + 1);

                switch (event.type)
                {
                    case Downturn:
                    case DownwardOvershoot:
                        sell += weights[j];
                        break;

                    case Upturn:
                    case UpwardOvershoot:
                        buy += weights[j];
                        break;

                    //default:
                        // do nothing (ignore overshoot events - for now)
                        //break;
                }
				 */

				int thresholdUpStart = (int) (curves[j].output[i].length() * curves[j].meanRatio[1] * beta);
				//				int threshold = (int) (curves[j].output[0].length() * curves[j].meanRatio[1] * beta);//CHECK WITH FERNANDO. Used to be output[i], but this
				//crashes when the testing data length is greater than training. So index i returns an array index out of bounds exception. Since we are
				//only interested in the length of the curve, we don't need output[i], even output[0] should be enough.

				int thresholdUpEnd = (int) (curves[j].output[i].length() * curves[j].meanRatio[1] * beta2);
				
				int thresholdDownStart = (int) (curves[j].output[i].length() * curves[j].meanRatio[0] * beta);
				int thresholdDownEnd = (int) (curves[j].output[i].length() * curves[j].meanRatio[0] * beta2);
				
				if (curves[j].output[i].type == Type.Downturn)
				{
					// downturn
					buy += weights[j];

					if (current >= thresholdDownStart && current < thresholdDownEnd)
					{
						downturnCount--;
					}
					else
					{
						downturnCount++;
					}
				}
				else
				{
					// upturn
					sell += weights[j];

					if (current >= thresholdUpStart && current < thresholdUpEnd)
					{
						upturnCount--;
					}
					else
					{
						upturnCount++;
					}
				}
			}

			if (sell > buy)
			{
				// we only perform the sell if we have enough
				// support for the downturn overshoot

				if (upturnCount > 0)
				{
					if (stock > 0)
					{
						cash += data[i];
						stock--;
					}
					else
					{
						// not enough stocks
						uSell++;
					}
				}
				else
				{
					noop++;
				}

				if (last == null)
				{
					last = Type.Downturn;
				}
				else if (last != Type.Downturn)
				{
					current = 0;
					last = Type.Downturn;
				}
			}
			else if (sell < buy)
			{
				if (downturnCount > 0)
				{
					if (cash >= data[i])
					{
						cash -= data[i];
						stock++;
					}
					else
					{
						// not enough money
						uBuy++;
					}
				}
				else
				{
					noop++;
				}

				if (last == null)
				{
					last = Type.Upturn;
				}
				else if (last != Type.Upturn)
				{
					current = 0;
					last = Type.Upturn;
				}
			}
		}

		Fitness fitness = new Fitness();
		fitness.value = cash + (stock * data[length - 1]);
		System.out.println(cash + " " + stock);
		fitness.uSell = uSell;
		fitness.uBuy = uBuy;
		fitness.noop = noop;

		return fitness;
	}

	private void report(int generation, Fitness[] fitness)
	{
		double best = -Double.MAX_VALUE;
		double worst = Double.MAX_VALUE;
		double average = 0.0;

		for (int i = 0; i < POPSIZE; i++)
		{
			if (fitness[i].value > best)
			{
				best = fitness[i].value;
			}

			if (fitness[i].value < worst)
			{
				worst = fitness[i].value;
			}

			average += fitness[i].value;
		}

		average = average / fitness.length;

		System.out.println(String.format("%d\t%12.6f\t%12.6f\t%12.6f", generation, best, worst, average));
		log.save("Logger.txt", String.format("%d\t%12.6f\t%12.6f\t%12.6f", generation, best, worst, average));

	}

	
	public static void main(String[] args) throws Exception{

		long seed = 0;

		if (args.length < 4)
		{
			System.out.println("usage: " + EventWriter.class.getName()
					+ " <file path> <file name> <training index> <test index> [seed]");
			System.exit(1);
		}
		else if (args.length == 5)
		{
			seed = Long.parseLong(args[4]);
		}

		log = new Logger(args[1], args[2], args[3]);

		double[] fixedThresholds = new double[10];
		double initial = 0.01;//0.005//0.01

		for (int i = 0; i < fixedThresholds.length; i++)
		{
			fixedThresholds[i] = (initial * (i + 1)) / 100.0;
		}
		
		
		log.save("Results.txt", "Threshold\tTesting Budget");
		
		for (int j = 0; j < fixedThresholds.length; j++)
		{
			THRESHOLDS[0] = fixedThresholds[j];



			DCCurvePerformanceOld dc = new DCCurvePerformanceOld(args[0], Integer.parseInt(args[2]), Integer.parseInt(args[3]));
			for (int i = 0; i < nRuns; i++){
				System.out.println("=========================== Run " + i + "==========================");
				log.save("Logger.txt", "\n=========================== Run " + i + "==========================");
				log.save("Solutions.txt", "\n=========================== Run " + i + "==========================\nCurve\tWeight");

				dc.run(seed, i);
				dc.reBuild();
			}
		


		}
	}

}
