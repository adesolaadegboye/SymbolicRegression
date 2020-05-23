/** This is a simple Buy and Hold strategy. We buy at the beginning of the specified period, and sell at the very end **/
package strategies;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import dc.EventWriter;
import dc.ga.GA.Fitness;
import dc.io.FReader;
import dc.io.FReader.FileMember2;
import files.FWriter;

public class BuyAndHoldForFX {

	Double[] start;
	Double[] end;
	double budget = 500000.00;
	double tradingCost = 0.001/100.0;//0.025/100
	double slippageAllowance = 0/100.0;//0.01/100
	ArrayList<FileMember2> sellBaseCcyRecordInFileArray;
	ArrayList<FileMember2> buyBaseCcyRecordInFileArray ;
	double OpeningPosition = 500000;
	double closePosition = 500000;
	double quantity = 1;
	double shortSellingQuantity = 0;
	
	
	/** Constructor when the buy and sell indices do not fall within the same month/filename **/
	public BuyAndHoldForFX(String filename, String filename2, int buyIndex, int sellIndex) throws FileNotFoundException, IOException{
		
		System.out.println("Loading FX data...");

		// loads the data
		ArrayList<Double[]> days2 = FReader.loadData(filename,false);
		sellBaseCcyRecordInFileArray = new ArrayList<FileMember2>(FReader.dataRecordInFileArray);
		FReader.dataRecordInFileArray.clear();
		ArrayList<Double[]> days = FReader.loadData(filename2,false);
		buyBaseCcyRecordInFileArray = new ArrayList<FileMember2>(FReader.dataRecordInFileArray);
		

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

		
		double myPrice = Double.parseDouble(sellBaseCcyRecordInFileArray.get(0).askPrice);
		double transactionCost = OpeningPosition * (0.025 / 100);
		OpeningPosition = (OpeningPosition - transactionCost) * myPrice;
		double mybidPrice=  Double.parseDouble(buyBaseCcyRecordInFileArray.get(0).bidPrice);
		closePosition =  ((OpeningPosition - transactionCost) * myPrice)/mybidPrice;
		transactionCost = 0.0;
		myPrice = Double.parseDouble(buyBaseCcyRecordInFileArray.get(buyBaseCcyRecordInFileArray.size()-1).bidPrice);
		transactionCost = OpeningPosition * (0.025 / 100);
		OpeningPosition = (OpeningPosition - transactionCost) / myPrice;
		
		
		Fitness fitness = new Fitness();
		//My fitness is equal to my wealth=cash + value of stocks I owe either due to buying or short-selling activity. If, however, I end up with a negative shortSellingStock number; 
		//so in this occasion the value of my short selling stocks is subtracted from my total fitness, as I need to re-buy the stocks to return them to my broker.
		fitness.value = OpeningPosition - 500000.0 ; // realisedProfit;
		fitness.realisedProfit = OpeningPosition - 500000.0 ; //realisedProfit;
		fitness.wealth = OpeningPosition; //realisedProfit + budget;//my wealth, at the end of the transaction period
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
		
		
		BuyAndHoldForFX bh = null;
		
		 if (s.length == 5)
			bh = new BuyAndHoldForFX(s[0], s[4], Integer.parseInt(s[2]), Integer.parseInt(s[3]));//buy and sell index fall within different months (filenames)
		 else
			 System.exit(-1);
		 
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
