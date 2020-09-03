/**
 * The purpose of this class is to report the fitness performance on the testing set of each individual fixed threshold.
 * I'm extending the GA class, with the difference that there's no evolution really happening, as I only allow 1 run, 1 individual, 1 generation, quantities of 1. The point here is not
 * to evolve new individuals, but simply use the fitness calculation capabilities of the GA class and evaluate the performance of each fixed threshold. Results are saved in the 
 * Results.txt file. 
 */
package dc.ga;

import java.io.IOException;
import java.text.ParseException;

import dc.EventWriter;
import dc.ga.DCCurve.Type;
import dc.io.Logger;


/**
 * @author Adesola Adegboye
 *
 */
public class DCCurvePerformance extends GA{

	
	public DCCurvePerformance(String filename, int trainingIndexStart, int trainingIndexEnd, int testIndexStart, int testIndexEnd, int POPSIZE,
			int MAX_GENERATIONS, int tournamentSize, double CROSSOVER_PROB, double MUTATION_PROB, double thresholdIncrement,
			int numberOfThresholds, int MAX_QUANTITY, int budget, double shortSellingAllowance, double mddWeight, int xoverOperatorIndex,
			int mutOperatorIndex, double initialThreshold)
					throws IOException, ParseException {

		super(filename, trainingIndexStart, trainingIndexEnd, testIndexStart, testIndexEnd, POPSIZE,
				MAX_GENERATIONS, tournamentSize, CROSSOVER_PROB, MUTATION_PROB, thresholdIncrement,
				numberOfThresholds, MAX_QUANTITY, budget, shortSellingAllowance, mddWeight, xoverOperatorIndex, mutOperatorIndex, initialThreshold);

		nRuns = 50;//1 run for SDC, 50 runs for SDCevo
	}

	/**
	 * Initialises the GA population. Overriding the GA method, so that we set all weights (with the exception of the first one) to 0.
	 * 
	 */
	protected void initialisePop() {
		for (int i = 0; i < POPSIZE; i++)
		{
			pop[i][0] = generateQuantity(false);//first index we save the quantity
//			pop[i][0] = 1;//fixed for DC-Fixed w/o Evolution (SDC)
			pop[i][1] = random.nextDouble();
//			pop[i][1] = 0;//fixed for DC-Fixed w/o Evolution
			double beta2 = generateBeta2(pop[i][1]);
			pop[i][2] = beta2;
//			pop[i][2] = 1;//fixed for DC-Fixed w/o Evolution
			pop[i][3] = generateQuantity(true);//short-selling quantity
//			pop[i][3] = 1;//fixed for DC-Fixed w/o Evolution
			pop[i][4] = random.nextDouble();//beta3
//			pop[i][4] = 1;//fixed for DC-Fixed w/o Evolution

			pop[i][5] = random.nextDouble();//don't care what weight it is, since it's going to be the only threshold/curve with a weight
			for (int j = 6; j < pop[0].length; j++)//all columns of pop have the same length, i.e. THRESHOLD.length+5; so it doesn't matter if I say pop[0], or pop[50] etc.
			{
				pop[i][j] = 0;
			}
		}
	}

	/**
	 * Mutation
	 * @param individual The individual to be mutated
	 */
	//Overriding GA's method. Only difference is that we always have weights set equal to 0, with the exception of the first weight, as we are evaluating one curve at a time
	protected void mutation(int individual) {
		if (random.nextDouble() < MUTATION_PROB)
		{
			for (int j = 0; j < pop[0].length; j++)//all columns of pop have the same length, i.e. THRESHOLD.length+5; so it doesn't matter if I say pop[0], or pop[50] etc.
			{
				if (random.nextDouble() > 0.5)
				{
					if (j == 0 || j == 3)//normal quantity (0) and short-selling quantity (3)
						newPop[individual][j] = (j == 0) ? generateQuantity(false) : generateQuantity(true);//if j==0, then it's the normal quantity, otherwise it's quantity for short-selling
						else if (j == 2)
							newPop[individual][j] = generateBeta2(newPop[individual][1]);//generate a beta2 which is greater than beta (newPop[individual][1])
						else if (j == 1 || j == 5)//for j=1 for beta, or j=5 for the weight of the first curve
							newPop[individual][j] = random.nextDouble();
						else
							newPop[individual][j] = 0;//everything else remain 0.
				}
			}
		}
	}

	/** Fitness function: (Return - Maximum DrawDown) **/
	//Only difference from GA's fitness is that here the quantityToTrade is fixed and equal to quantity=individual[0]
	Fitness fitness(double[] individual, boolean test)
	{
		final double quantity = individual[0];
		final double beta = individual[1];
		final double beta2 = individual[2];
		//		final double shortSellingQuantity = individual[3];//how many stocks to short-sell
		final double beta3 = individual[4];

		double cash = budget;//set my initial cash to always be equal to a fixed amount, for both training and testing sets.
		double stock = 0;
		//		double shortSellingStock = 0;//when I go short, I borrow stocks from a broker, which I immediately sell. So my shortSellingInventory goes negative, because I'm now 'short' of
		//these stocks, which I eventually need to re-buy, so that I can return them to the broker.

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

		//Maximum DrawDown variables
		double peak = Double.NEGATIVE_INFINITY;
		double trough = Double.POSITIVE_INFINITY;
		double peakMDD = Double.NEGATIVE_INFINITY;

		double DD = 0;//DrawDown
		double MDD = 0;//Maximum DrawDown

		int noOfTransactions = 0;
		int noOfShortSellingTransactions = 0;
		double shortSellingPrice = 0;

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

			//Peak and trough calculation
			if (data[i] > peak)
				peak = data[i];
			else if (data[i] < trough)
				trough = data[i];
			for (int j = 0; j < curves.length; j++)
			{
				//Using both a starting and an ending point for the threshold. Actions have to take place within these two thresholds.
				int thresholdUpStart = (int) (curves[j].output[i].length() * curves[j].meanRatio[1] * beta);
				int thresholdUpEnd = (int) (curves[j].output[i].length() * curves[j].meanRatio[1] * beta2);

				//We use a different meanRatio here (different array index), as now we deal with downturn events.
				int thresholdDownStart = (int) (curves[j].output[i].length() * curves[j].meanRatio[0] * beta);
				int thresholdDownEnd = (int) (curves[j].output[i].length() * curves[j].meanRatio[0] * beta2);

				if (curves[j].output[i].type == Type.Downturn)
				{
					// downturn
					buy += individual[j+5];//+5, because we are not taking into account the first 5 indices, which represent the quantity, beta and beta2, and shortSellingQuantity, beta3
					if (current >= thresholdDownStart && current < thresholdDownEnd)//
					{
						downturnCount++;
					}
					else
					{
						downturnCount--;
					}
				}
				else
				{
					// upturn
					sell += individual[j+5];//+5, because we are not taking into account the first 5 indices, which represent the quantity, beta and beta2, and shortSellingQuantity, beta3

					if (current >= thresholdUpStart && current < thresholdUpEnd)//
					{
						upturnCount++;
					}
					else
					{
						upturnCount--;
					}
				}
			}

			if (sell > buy)
			{
				// we only perform the sell if we have enough
				// support for the downturn overshoot
				if (upturnCount > 0 && data[i] >= peak*beta3)
				{
					//the quantity to trade depends on the chromosome and how many curves are advising to sell (upturnCount)
					//So by dividing this count with the number of thresholds, I get a percentage, which I then multiply
					//with the quantity given by the chromosome. In this way, I sell even more when more curves advise me
					//to do so.
					int quantityToTrade = (int) (quantity);
					double deductions = quantityToTrade * data[i] * (transactionCost + slippageAllowance);


					//Don't want to overdo it with short-selling, as a training data set might have a very long downward period, 
					//which will result in excessive short-selling; however, if the test data set does not have such long
					//downward periods, the seemingly successful trading strategy in training, performs EXTREMELY bad in test,
					//leading to significant losses. To deal with this, we have introduced a new condition, where the short-selling
					//takes place as long as the existing short-sold stock does not exist our initial budget * Parameter. This
					//is to ensure that we don't do too much short-selling, only do that much that will cost at most your
					//initial budget * Parameter. Parameter can be anything above 0. Obviously, with a very low Parameter, we are
					//very restrictive towards short-selling, whereas with high values we are generally loose towards short-selling.
					//Of course, I allow ordinary selling when stock > 0.
					if (stock > 0 || 
							(stock <= 0 && Math.abs(stock) * data[i] + Math.abs(stock) * data[i] * (transactionCost + slippageAllowance) <= shortSellingAllowance * budget) ){

						cash += quantityToTrade * data[i];
						cash -= deductions;//adjusting, after allowing trading cost and slippage
						stock -= quantityToTrade;

					}

					if (stock < 0){
						noOfShortSellingTransactions++;
						shortSellingPrice = data[i];
					}
					else
						noOfTransactions++;

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
				if (downturnCount > 0 && data[i] <= (trough + trough * (1 - beta3) ) )
				{
					//the quantity to trade depends on the chromosome and how many curves are advising to buy (downturnCount)
					//So by dividing this count with the number of thresholds, I get a percentage, which I then multiply
					//with the quantity given by the chromosome. In this way, I buy even more when more curves advise me
					//to do so.
					int quantityToTrade = (int) (quantity);

					//closing the short-selling position, as long as the current price is less than the one when I short-sold
					//OR
					//simple BUY, as long as we have a positive stock; this means we are not trying to close a short-selling position. Checks
					//if we have enough cash to go forward with the buy, follow below in a few lines.
					double deductions = quantityToTrade * data[i] * (transactionCost + slippageAllowance);

					//Need to have enough cash to cover the stock purchase and the costs
					//Sign for deductions is (+), added to the stock money we'll be spending, and all of this together is subtracted
					//from our cash.
					if (cash > (quantityToTrade * data[i] + deductions) ){
						if (stock < 0) noOfShortSellingTransactions++; 
						else noOfTransactions++;

						cash -= (quantityToTrade * data[i] + deductions);
						stock += quantityToTrade;
					}
					else{//Don't have enough cash to cover all of the quantity, but at least close short-selling position for some (or at least simple-buy).
						//Calculate deductions for a single quantity (quantity=1)
						double singleQuantityDeductions = data[i] * (transactionCost + slippageAllowance);
						//Expenses for a single quantity are the cost of the current price (data[i]), plus the deductions.
						double expenses = data[i] + singleQuantityDeductions;
						//Find out how much I can afford with my current cash
						quantityToTrade = (int) (cash / expenses);

						if (quantityToTrade > 0){
							if (stock < 0) noOfShortSellingTransactions++; 
							else noOfTransactions++;

							cash -= quantityToTrade * data[i];//Close some of the positions
							deductions = quantityToTrade * data[i] * (transactionCost + slippageAllowance);
							cash -= deductions;//Deductions/costs
							stock += quantityToTrade;//Update the quantity of the closed positions
						}
						else
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

			double wealth = 
					cash + stock * data[i] - Math.abs(stock) * data[i] * (transactionCost + slippageAllowance);//wealth for MDD purposes

			if (wealth > peakMDD)
				peakMDD = wealth;

			DD = 100.0 * (peakMDD - wealth)/peakMDD;

			if (DD > MDD)
				MDD = DD;

			//End of MDD calculation
		}
		Fitness fitness = new Fitness();

		//My realisedProfit is equal to my wealth=cash + value of stocks I owe either due to buying or short-selling activity. If, however, I end up with a negative shortSellingStock number; 
		//so in this occasion the value of my short selling stocks is subtracted from my total fitness, as I need to re-buy the stocks to return them to my broker. I also subtract the 
		//transaction cost and slippage. Note that even though I have a plus (+) sign in the equation, in fact there is a subtraction, as the shortSellingStock has a negative value 
		//in the statement below. In addition, I need to subtract the transaction costs. 
		double realisedProfit = 
				cash + stock * data[length - 1] - 
				(Math.abs(stock)) * data[length - 1] * (transactionCost + slippageAllowance) - budget;

		fitness.uSell = uSell;
		fitness.uBuy = uBuy;
		fitness.noop = noop;
		fitness.realisedProfit = realisedProfit;
		fitness.MDD = MDD;
		fitness.wealth = realisedProfit + budget;//my wealth, at the end of the transaction period
		fitness.Return = 100.0 * (fitness.wealth - budget) / budget;
		fitness.value = fitness.Return - mddWeight * fitness.MDD;
		fitness.noOfTransactions = noOfTransactions;
		fitness.noOfShortSellingTransactions = noOfShortSellingTransactions;

		//A single transaction is dangerous, because it's based on pure luck, whether the last day's data is preferable, and can lead to a positive position. So better to avoid this,
		// and require to have more than 1 transaction. We of course only do this for the training set (test == false); with test data, we want to have the real/true fitness, so we
		//don't want to mess with this number - not doing search any more, no reason for penalising.
		if (fitness.noOfTransactions + fitness.noOfShortSellingTransactions == 1 && test == false){
			fitness.value = -9999;//Heavily penalise individuals with a single transaction
			//			fitness.Return = -9999;//Lexicographic
		}

		return fitness;
	}

	/**
	 * @param args
	 * @throws IOException 
	 * @throws NumberFormatException 
	 */
	public static void main(String[] args) throws Exception {
		long seed = 0;

		if (args.length < 14)
		{
			System.out.println("usage: " + EventWriter.class.getName()
					+ " <file path:file name:training index start:training index end:test index start:test index end> + "
					+ "<popSize> <maxGens> <tournamentSize> <xoverProb> <mutProb> <thresholdIncrement> <noOfThresholds> <maxQuantity> + "
					+ "<budget> <shortSellingAllowance> <mddWeight> <xoverOperatorIndex> <mutOperatorIndex> <initialThreshold> [seed]");
			System.exit(1);
		}
		else if (args.length == 16)
		{
			seed = Long.parseLong(args[15]);
		}

		//Split the long parameter file , according to the delimiter
		String s[] = args[0].split(":");
		if (s.length < 6){
			System.out.println("Expect 6 parameters: <file path:file name:training index start:training index end:test index start:test index end>");
			System.exit(1);
		}

		log = new Logger(s[1], s[3], s[4]);

		double[] fixedThresholds = new double[Integer.parseInt(args[7])];
		double initial = Double.parseDouble(args[14]);//Starts from 0.01
		double thresholdIncrement = Double.parseDouble(args[6]);
		
		for (int i = 0; i < fixedThresholds.length; i++)
		{
			fixedThresholds[i] = (initial * (i + 1)) / 100.0;//This is for SDCevo
//			fixedThresholds[i] = (initial + (thresholdIncrement * i)) / 100.0;//This is for SDC
		}


		log.save("Results.txt", "\tWealth\tReturn\tFitness\tRealised Profit\tMDD\tNoOfTransactions\tNoOfShortSellingTransactions");
		log.save("Fitness.txt", "Train fitness\tTest fitness");

		for (int j = 0; j < fixedThresholds.length; j++)
		{

			DCCurvePerformance dc = new DCCurvePerformance(s[0], Integer.parseInt(s[2]), Integer.parseInt(s[3]), Integer.parseInt(s[4]), Integer.parseInt(s[5]),
					Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]), Double.parseDouble(args[4]),
					Double.parseDouble(args[5]), Double.parseDouble(args[6]), Integer.parseInt(args[7]), Integer.parseInt(args[8]), 
					Integer.parseInt(args[9]), Double.parseDouble(args[10]), Double.parseDouble(args[11]), Integer.parseInt(args[12]),
					Integer.parseInt(args[13]), initial);

			THRESHOLDS[0] = fixedThresholds[j];

			for (int i = 0; i < nRuns; i++){
				System.out.println("=========================== Run " + i + "==========================");
				log.save("Logger.txt", "\n=========================== Run " + i + "==========================");
				log.save("Solutions.txt", "\n=========================== Run " + i + "==========================\nCurve\tWeight");

				Fitness f = dc.run(seed, i);
				dc.saveResults(f, i);
				dc.reBuild();
			}
		}
	}
}


