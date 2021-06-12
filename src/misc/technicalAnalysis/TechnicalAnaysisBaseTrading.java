package misc.technicalAnalysis;

import java.util.ArrayList;
import java.util.List;

import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Order;
import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.Trade;
import misc.SimpleDrawDown;
import misc.SimpleSharpeRatio;

public class TechnicalAnaysisBaseTrading {
	
	protected List<Tick> bidTicks = null;
	protected List<Tick> askTicks = null;
	
	protected String productName="";  //e.g. FX_USD_GBP
	protected double startPosition ;
	protected List<Order> fullOrder = null;
	protected ArrayList<Double> positionArrayQuote  = new ArrayList<Double>();
	protected ArrayList<Double> tradedPrice = new ArrayList<Double>();
	protected SimpleDrawDown simpleDrawDown = new SimpleDrawDown() ;
	protected SimpleDrawDown simpleDrawDownQuote = new SimpleDrawDown() ;
	protected ArrayList<Double> positionArray  = new ArrayList<Double>();
	protected ArrayList<Double> positionArrayBase  = new ArrayList<Double>();
	protected double peakMDD = Double.NEGATIVE_INFINITY;
	protected double peakMDDQuote = Double.NEGATIVE_INFINITY;
	protected SimpleSharpeRatio simpleSharpeRatio = new SimpleSharpeRatio();
	protected double StartSellQuantity = -1.0;
	protected double StartBuyQuantity = -1.0;
	
	protected  double lastSellPrice = 0.0;
	protected  double lastBuyPrice = 0.0; 
	
	public List<Order> getOrders(){
		return null;
	};
	
 	public double trade(){
 		boolean isPositionOpen =  false;
		double myPrice = 0.0;
		double transactionCost = 0.025/100;
		double startPositionCopy  =  startPosition;
		simpleDrawDown.Calculate(startPosition);
		simpleSharpeRatio.addReturn(0);
		lastSellPrice = 0.0;
		lastBuyPrice = 0.0;
		StartSellQuantity = -1.0;
		StartBuyQuantity = -1.0;
		for (int i =1 ; i < fullOrder.size(); i++){
			
			
			myPrice = fullOrder.get(i).getPrice().toDouble();
			if (fullOrder.get(i).isSell() && !isPositionOpen){
				//Now position is in quote currency
				// I sell base currency in bid price
				double askQuantity = startPosition;
				double zeroTransactionCostAskQuantity = startPosition;
				double transactionCostPrice = 0.0;
				
				//transactionCost = startPosition * (0.025/100);
				//startPosition =  (startPosition -transactionCost) *myPrice;
				
				transactionCost = askQuantity * (0.025/100);
				transactionCostPrice = transactionCost * myPrice;
				askQuantity =  (askQuantity -transactionCost) *myPrice;
				zeroTransactionCostAskQuantity  = zeroTransactionCostAskQuantity * myPrice;
				
				
				if (Double.compare(transactionCostPrice, (zeroTransactionCostAskQuantity - askQuantity)) < 0 &&
						(((lastSellPrice > 0.0) ? ((myPrice >= lastSellPrice) ? true : false): true ) ||
						(StartSellQuantity > -1.0  ? ((StartSellQuantity <= askQuantity) ? true : false) : true  ))) {
					
					
					

					if (StartSellQuantity <= -1.0)
						StartSellQuantity = startPosition;
					
					lastSellPrice = myPrice;
					
					startPosition = askQuantity;
					Order myOrder = Order.buyAt(fullOrder.get(i).getIndex(), fullOrder.get(i).getPrice(),Decimal.valueOf(startPosition) );
					fullOrder.set(i, myOrder);
					isPositionOpen = true;
				
					positionArrayQuote.add(new Double(startPosition));
					tradedPrice.add(new Double(myPrice));
				}
			}
			else if (fullOrder.get(i).isBuy() &&  isPositionOpen){
				//Now position is in base currency
				// I buy base currency
				double bidQuantity = startPosition;
				double zeroTransactionCostBidQuantity = startPosition;
				double transactionCostPrice = 0.0;
				
				
				transactionCost = bidQuantity * (0.025 / 100);
				transactionCostPrice = transactionCost * myPrice;
				bidQuantity = (bidQuantity - transactionCost) * myPrice;
				zeroTransactionCostBidQuantity = zeroTransactionCostBidQuantity * myPrice;
				//transactionCost = startPosition * (0.025/100);
				//startPosition =  (startPosition -  transactionCost)/ myPrice;
				
				if (Double.compare(transactionCostPrice, (zeroTransactionCostBidQuantity - bidQuantity)) <  0
					&& (( lastBuyPrice > 0.0 ? ((myPrice <= lastBuyPrice ) ? true :false ): true )||
							(StartBuyQuantity > -1.0  ? ((StartBuyQuantity > bidQuantity) ? true: false) : true  ))) {
								
						if (StartBuyQuantity <= -1.0)
							StartBuyQuantity = startPosition;
								
				lastBuyPrice = myPrice;
				
				
					startPosition =  (startPosition -  transactionCost)/ myPrice;
					Order myOrder = Order.buyAt(fullOrder.get(i).getIndex(), fullOrder.get(i).getPrice(),Decimal.valueOf(startPosition) );
					fullOrder.set(i, myOrder);
					isPositionOpen = false;
					positionArrayBase.add(new Double(startPosition));
					
					tradedPrice.add(new Double(myPrice));
				}
				
			}
			else
			{
				Order myOrder = Order.buyAt(fullOrder.get(i).getIndex(), Decimal.valueOf(0.0),Decimal.valueOf(0.0));
				fullOrder.set(i, myOrder);
			}
		}
		
		if (isPositionOpen )
		{
			if (positionArrayBase.size()> 1){
				
				System.out.println("Reverting last Trade. Position is " +  positionArrayBase.get(positionArrayBase.size() - 1)+ "Class name is:" + this.getClass().getName());
				
				startPosition = positionArrayBase.get(positionArrayBase.size() - 1);
			}
			else
			{
				System.out.println("Only one trade was made. Reverting Trade. Position is " +  startPositionCopy+ "Class name is:" + this.getClass().getName());
			
				startPosition = startPositionCopy;
			}
			tradedPrice.remove(tradedPrice.size()-1);
			positionArrayQuote.remove(positionArrayQuote.size()-1);
			
		}
		
		
		for (int profitLossCount=0; profitLossCount < positionArrayBase.size() ; profitLossCount++  ){
			simpleDrawDown.Calculate(positionArrayBase.get(profitLossCount));
		}
		
		for (int profitLossCount=0; profitLossCount < positionArrayQuote.size() ; profitLossCount++  ){
			simpleDrawDownQuote.Calculate(positionArrayQuote.get(profitLossCount));
		}
		
		if (positionArrayBase.size() > 1){
			//Calculate sharp ratio start from 1 as it is a moving window
			for(int srCount = 1 ; srCount < positionArrayBase.size(); srCount++ ){
				simpleSharpeRatio.addReturn(positionArrayBase.get(srCount) - positionArrayBase.get(srCount-1) );
			}
		}
		
		peakMDD = simpleDrawDown.getMaxDrawDown();
		peakMDDQuote = simpleDrawDownQuote.getMaxDrawDown();
		
		return startPosition;
	}
	
	
	
 	public double getMddPeak(){
		return simpleDrawDown.getPeak();
	}
	
	public  double getMddTrough(){
		return simpleDrawDown.getTrough();
	}
	
	public double getMaxMddBase(){
		return simpleDrawDown.getMaxDrawDown();
	}
	
	public double getMddPeakQuote(){
		return simpleDrawDownQuote.getPeak();
	}
	
	public double getMddTroughQuote(){
		return simpleDrawDownQuote.getTrough();
	}
	
	public double getMaxMddQuote(){
		return simpleDrawDownQuote.getMaxDrawDown();
	}
	
	public int getNumberOfQuoteCcyTransactions(){
		
		return positionArrayQuote.size() - 1 ;
	}
	
	public int getNumberOfBaseCcyTransactions(){
		
		return positionArrayBase.size() - 1 ;
	}

	public double getBaseCCyProfit(){
		double profit = 0.00;
		ArrayList<Double> profitList = new ArrayList<Double>();
		if (positionArrayBase.size() == 1)
			return 0.00;
		for (int profitLossCount=1; profitLossCount < positionArrayBase.size() ; profitLossCount++  ){
			double profitCalculation =  positionArrayBase.get(profitLossCount) - positionArrayBase.get(profitLossCount-1)/ positionArrayBase.get(profitLossCount-1);
			profitList.add(profitCalculation);
		}
		profit = profitList.stream().mapToDouble(i -> i.doubleValue()).sum();
		return profit;
	}

	public double getQuoteCCyProfit(){
		double profit = 0.00;
		ArrayList<Double> profitList = new ArrayList<Double>();
		if (positionArrayQuote.size() == 1)
			return 0.00;
		//Start from 3rd element because first element is zero 
		for (int profitLossCount=2; profitLossCount < positionArrayQuote.size() ; profitLossCount++  ){
			double profitCalculation =  positionArrayQuote.get(profitLossCount) - positionArrayQuote.get(profitLossCount-1)/ positionArrayBase.get(profitLossCount-1);
			profitList.add(profitCalculation);
		}
		profit = profitList.stream().mapToDouble(i -> i.doubleValue()).sum();
		return profit;
	}
	

	public int getMaxTransactionSize() {
		// TODO Auto-generated method stub
		return positionArrayBase.size();
	}
	
	
	public double getTransanction(int i) {
		if ( i >= positionArrayBase.size())
			return 0.0;
		
		return positionArrayBase.get(i);
	}
	
	public double  calculateSD()
	{
		return calculateBaseSD(positionArrayBase);
	}
	protected static  double calculateBaseSD(ArrayList<Double>  numArray)
    {
        double sum = 0.0, standardDeviation = 0.0;
        int length = numArray.size();
       
        for(double num : numArray) {
            sum += num;
        }
        double mean = sum/length;
        for(double num: numArray) {
            standardDeviation += Math.pow(num - mean, 2);
        }
        return Math.sqrt(standardDeviation/length-1);
    }
	
	protected static double getMaxValue(ArrayList<Double> numbers){
		  double maxValue = numbers.get(0);
		  for(double num :   numbers){
		    if(num > maxValue){
			  maxValue = num;
			}
		  }
		  return maxValue;
		}
	protected  static double getMinValue(ArrayList<Double> numbers){
	  double minValue = numbers.get(0);
	  for(double num: numbers){
	    if(num < minValue){
		  minValue = num;
		}
	  }
	  return minValue;
	}
	
	public double getSharpRatio(){
		return simpleSharpeRatio.calulateSharpeRatio();
	}
	
	

}
