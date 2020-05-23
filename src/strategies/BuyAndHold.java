/** This is a simple Buy and Hold strategy. We buy at the beginning of the specified period, and sell at the very end **/
package strategies;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import dc.EventWriter;
import dc.ga.GA.Fitness;
import dc.io.FReader;
import files.FWriter;

public class BuyAndHold {

	Double[] start;
	Double[] end;
	double budget = 500000;
	double tradingCost = 0.001/100.0;//0.025/100
	double slippageAllowance = 0/100.0;//0.01/100
	
	double quantity = 1;
	double shortSellingQuantity = 0;
	
	/** Constructor when the buy and sell indices fall within the same month/filename **/
	public BuyAndHold(String filename, int buyIndex, int sellIndex) throws FileNotFoundException, IOException{
		
		System.out.println("Loading FX data...");

		// loads the data
		ArrayList<Double[]> days = FReader.loadData(filename,false);

		start = days.get(buyIndex);
		
		//if user enters the same buyIndex and sellIndex, this means that we will buy at the beginning of the day, and sell at the end of the very same day. That's why
		//the else-statement sets the 'end' data equal to the data derived from the *buy* (i.e., start) index. If buyIndex and sellIndex are different values, then
		//we go to the if-part of the statement, which sets the 'end' data equal to the data derived from the *sell* (i.e., end) index.
		if (buyIndex != sellIndex)
			end = days.get(sellIndex);
		else
			end = days.get(buyIndex);
	}
	
	/** Constructor when the buy and sell indices do not fall within the same month/filename **/
	public BuyAndHold(String filename, String filename2, int buyIndex, int sellIndex) throws FileNotFoundException, IOException{
		
		System.out.println("Loading FX data...");

		// loads the data
		ArrayList<Double[]> days2 = FReader.loadData(filename2,false);
		ArrayList<Double[]> days = FReader.loadData(filename,false);
		

		start = days.get(buyIndex);//we will buy on the first day of the first period (filename)
		end = days2.get(sellIndex);//we will sell on the last day of the second period (filename)
	}
	
	/** Fitness function: (Realised profit - Maximum DrawDown) **/
	Fitness fitness()
	{

		double cash = budget;//set my initial cash to always be equal to a fixed amount, for both training and testing sets.
		double stock = 0;
		double shortSellingStock = 0;//when I go short, I borrow stocks from a broker, which I immediately sell. So my shortSellingInventory goes negative, because I'm now 'short' of
		//these stocks, which I eventually need to re-buy, so that I can return them to the broker.

		Double[] dataStart = this.start;
		Double[] dataEnd = this.end;

		//BUY (I also allow short-selling as a form of investment) and HOLD until the end of my data period. Then I sell.
		if (cash >= quantity*dataStart[0])
		{
			cash -= quantity*dataStart[0];
			cash -= (cash*tradingCost + cash*slippageAllowance);//adjusting, after allowing trading cost and slippage
			stock += quantity;
			
			//short-selling, which can happen regardless of my normal stock inventory
			shortSellingStock -= shortSellingQuantity;
			cash += shortSellingQuantity*dataStart[0];
			cash -= (cash*tradingCost + cash*slippageAllowance);//adjusting, after allowing trading cost and slippage
		}
		
		//At the end of the period, I sell.
		cash += quantity*dataEnd[dataEnd.length - 1];
		cash -= (cash*tradingCost + cash*slippageAllowance);//adjusting, after allowing trading cost and slippage
		stock -= quantity;
		
		//I need to re-buy the stocks that I was short.
		shortSellingStock += shortSellingQuantity;
		cash -= shortSellingQuantity*dataEnd[dataEnd.length - 1];
		cash -= (cash*tradingCost + cash*slippageAllowance);//adjusting, after allowing trading cost and slippage
		
		Fitness fitness = new Fitness();
		//My fitness is equal to my wealth=cash + value of stocks I owe either due to buying or short-selling activity. If, however, I end up with a negative shortSellingStock number; 
		//so in this occasion the value of my short selling stocks is subtracted from my total fitness, as I need to re-buy the stocks to return them to my broker.
		double realisedProfit = (cash + (stock * dataEnd[dataEnd.length - 1]) + (shortSellingStock * dataEnd[dataEnd.length - 1])) - budget;
		fitness.value = realisedProfit;
		fitness.realisedProfit = realisedProfit;
		fitness.wealth = realisedProfit + budget;//my wealth, at the end of the transaction period
		fitness.Return = 100.0 * (fitness.wealth - budget) / budget;

		return fitness;
	}
	
	public static void main(String[] args) throws NumberFormatException, FileNotFoundException, IOException {
		
		if (args.length < 1)
		{
			System.out.println("usage: " + EventWriter.class.getName()
					+ " <file path>:<file name>:<start index>:<end index>[<file path for another month>]");
			System.exit(1);
		}
		
		String s[] = args[0].split(":");
		
		
		BuyAndHold bh = null;
		
		if (s.length == 4)
			bh = new BuyAndHold(s[0], Integer.parseInt(s[2]), Integer.parseInt(s[3]));//buy and sell index fall within the same month (filename)
		else if (s.length == 5)
			bh = new BuyAndHold(s[0], s[4], Integer.parseInt(s[2]), Integer.parseInt(s[3]));//buy and sell index fall within different months (filenames)
		
		Fitness f = bh.fitness();
		
		System.out.println("Return: " + f.Return + " Wealth: " + f.wealth + " Realised profit: " + f.realisedProfit);
		
		//Save result
		String folder = s[1] + "/i" + Integer.parseInt(s[2]) + "_" + Integer.parseInt(s[3]) + "/";
		File file = new File(folder);
		file.mkdirs();
		FWriter fw = new FWriter(folder + "BandHResults.txt");
		fw.write("Return\tWealth\tRealisedProfit");
		fw.write(f.Return + "\t" + f.wealth + "\t" + f.realisedProfit);
		fw.closeFile();
	}

}
