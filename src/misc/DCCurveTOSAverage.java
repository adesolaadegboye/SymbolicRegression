package misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

import dc.ga.PreProcess;
import dc.ga.DCCurve.Event;
import dc.ga.DCCurve.Type;
import dc.io.FReader;
import dc.io.FReader.FileMember2;

public class DCCurveTOSAverage  extends DCCurveRegression{

	public DCCurveTOSAverage(){
		super();
	
		 meanRatio = new double[1];
		 meanRatio[0] = 0.0;
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
	public void build(Double[] values, double delta, String GPTreeFileName, Event[] events, PreProcess preprocess) {
		String thresholdStr = String.format("%.8f", delta);
		thresholdString = thresholdStr;
		
		if ( events == null || events.length <1)
			return;
		
	
		trainingEvents =  Arrays.copyOf(events, events.length) ;
		

		if (meanRatio[0] > 0.0 )
			return;
	
		
		double meanDownwardOvershoot = 0.0;
		double meanUpwardOvershoot = 0.0;
		double eventsLength  =0.0;

		for (Event e : events) {
			// we ignore the first (artificial) event
			if (e.start > 0) {
				eventsLength += e.length();
				if (e.type == Type.Upturn) {
					meanUpwardOvershoot += (e.overshoot != null) ? e.overshoot.length() : 0;
				} else if (e.type == Type.Downturn) {

					meanDownwardOvershoot += (e.overshoot != null) ? e.overshoot.length() : 0;

				}
			}
		}

		meanRatio[0] = (meanDownwardOvershoot + meanUpwardOvershoot) /eventsLength ;
		
		
	}

	public void testbuild(int lastTrainingPricePosition, Double[] values, double delta , Event[]  testEvents, PreProcess preprocess) {
		lastTrainingPrice = lastTrainingPricePosition;
		if (testEvents == null || testEvents.length <1)
			return;
		
		testingEvents =  Arrays.copyOf(testEvents, testEvents.length) ;
	}
	
	private String calculateRMSE_MF(Event[] trendEvent,double delta) {

		double rmse = 0.0;
		for (int eventCount = 0; eventCount < trendEvent.length; eventCount++) {
			int os = 0;

			// meanRatio[0] = (meanDownwardOvershoot / downturn) / (meanDownturn
			// / downturn);
			// meanRatio[1] = (meanUpwardOvershoot / upturn) / (meanUpturn /
			// upturn);

			if (trendEvent[eventCount].overshoot != null && trendEvent[eventCount].overshoot.end != trendEvent[eventCount].overshoot.start)
				os = trendEvent[eventCount].overshoot.length();

			if (trendEvent[eventCount].type == Type.Upturn) {
				rmse = rmse + ((os - (trendEvent[eventCount].length() * meanRatio[0]))
						* (os - (trendEvent[eventCount].length() * meanRatio[0])));
			} else if (trendEvent[eventCount].type == Type.Downturn) {
				rmse = rmse + ((os - (trendEvent[eventCount].length() * meanRatio[0]))
						* (os - (trendEvent[eventCount].length() * meanRatio[0])));
			} else {
				System.out.println("Unknown event type. exiting");
				System.exit(0);
			}
		}

		predictionRmse = Math.sqrt(rmse / (trendEvent.length - 1));
		
		// System.out.println(predictionRmse);
		return Double.toString(predictionRmse);
	}

////

	public String reportTestMF(Double[] values, double delta, String GPTreeFileName) {
		return calculateRMSE_MF(testingEvents, delta);

	}
	
	@Override
	String report(Double[] values, double delta, String GPTreeFileName) {
		return calculateRMSE_MF(testingEvents, delta);
	}

	@Override
	double trade( PreProcess preprocess) {
		boolean isPositionOpen =  false;
		double myPrice = 0.0;
		double transactionCost = 0.025/100;
		simpleDrawDown.Calculate(OpeningPosition);
		simpleSharpeRatio.addReturn(0);
		lastSellPrice = 0.0;
		lastBuyPrice = 0.0;
		double lastUpDCCend = 0.0;
		for (int i =1 ; i < testingEvents.length; i++){	
			int tradePoint =  0;
			
			tradePoint =  (int) (testingEvents[i].end + (testingEvents[i].length() * meanRatio[0]));
			
			
			if (testingEvents[i] ==  null)
				continue;
			
			if (i+1 >   testingEvents.length-1)
					continue;
				
				if (testingEvents[i+1] == null)
					continue;
			
			if (tradePoint >   testingEvents[i+1].end)  // If a new DC is encountered before the estimation point skip trading
				continue;
			
			FReader freader = new FReader();
			FileMember2 fileMember2 =  freader.new FileMember2();
			
			if (  tradePoint > FReader.dataRecordInFileArray.size() ||  (lastTrainingPrice-1)+tradePoint > FReader.dataRecordInFileArray.size() ){
				System.out.println(" DCCurveMF: predicted datapoint "
						+ ((lastTrainingPrice - 1) + tradePoint) + " is beyond the size of price array  "
						+ FReader.dataRecordInFileArray.size() + " . Trading ended");
				break;
			}
			else {
				
				try {
			
			//I am opening my position in base currency
			fileMember2 = FReader.dataRecordInFileArray.get((lastTrainingPrice-1)+tradePoint);
			LinkedHashMap<Integer,Integer> anticipatedTrendMap=new LinkedHashMap<Integer,Integer>();
			LinkedHashMap<Integer,Integer> actualTrendMap=new LinkedHashMap<Integer,Integer>();
		
			if (testingEvents[i].type == Type.Upturn && !isPositionOpen){
				//Now position is in quote currency
				// I sell base currency in bid price
				double askQuantity = OpeningPosition;
				double zeroTransactionCostAskQuantity = OpeningPosition;
				double transactionCostPrice = 0.0;
				myPrice = Double.parseDouble(fileMember2.askPrice);
				
				
				transactionCost = askQuantity * (0.025/100);
				transactionCostPrice = transactionCost * myPrice;
				askQuantity =  (askQuantity -transactionCost) *myPrice;
				zeroTransactionCostAskQuantity = zeroTransactionCostAskQuantity *myPrice;
				
				
				if (transactionCostPrice < (zeroTransactionCostAskQuantity - askQuantity)){
//				if (transactionCostPrice < (zeroTransactionCostAskQuantity - askQuantity) 
//						&& (((lastSellPrice > 0.0) ? ((myPrice >= lastSellPrice) ? true : false): true ) ||
//								(StartSellQuantity > -1.0  ? ((StartSellQuantity <= askQuantity) ? true : false) : true  ))) {
							
							if (StartSellQuantity <= -1.0)
								StartSellQuantity = OpeningPosition;
					
					lastSellPrice = myPrice;
					OpeningPosition = askQuantity;
					isPositionOpen = true;
					positionArrayQuote.add(new Double(OpeningPosition));
					
					tradedPrice.add(new Double(myPrice));
					anticipatedTrendMap.put(testingEvents[i].start, tradePoint);
					anticipatedTrend.add(anticipatedTrendMap);
					lastUpDCCend = Double.parseDouble(FReader.dataRecordInFileArray.get((lastTrainingPrice - 1) + testingEvents[i].end).bidPrice);
					
					
					if (testingEvents[i].overshoot == null || testingEvents[i].overshoot.length() < 1)
						actualTrendMap.put(testingEvents[i].start, testingEvents[i].end );
					else
						actualTrendMap.put(testingEvents[i].start, testingEvents[i].overshoot.end );
					
					actualTrend.add(actualTrendMap);
				}
				
			}
			else if (testingEvents[i].type == Type.Downturn &&  isPositionOpen){
				//Now position is in base currency
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
//				if (transactionCostPrice < (zeroTransactionCostBidQuantity - bidQuantity)
//						&& (( lastBuyPrice > 0.0 ? ((myPrice <= lastBuyPrice ) ? true :false ): true )||
//								(StartBuyQuantity > -1.0  ? ((StartBuyQuantity > bidQuantity) ? true: false) : true  ))) {
									
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
					System.out.println(" DCCurveTOSAverage: Search for element "
							+ ((lastTrainingPrice - 1) + tradePoint) + " is beyond the size of price array  "
							+ FReader.dataRecordInFileArray.size() + " . Trading ended");
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
	
	
	
	double getMddPeak(){
		return simpleDrawDown.getPeak();
	}
	
	double getMddTrough(){
		return simpleDrawDown.getTrough();
	}
	
	double getMaxMddBase(){
		return simpleDrawDown.getMaxDrawDown();
	}
	
	double getMddPeakQuote(){
		return simpleDrawDownQuote.getPeak();
	}
	
	double getMddTroughQuote(){
		return simpleDrawDownQuote.getTrough();
	}
	
	double getMaxMddQuote(){
		return simpleDrawDownQuote.getMaxDrawDown();
	}
	
	int getNumberOfQuoteCcyTransactions(){
		
		return positionArrayQuote.size() - 1 ;
	}
	
	int getNumberOfBaseCcyTransactions(){
		
		return positionArrayBase.size() - 1 ;
	}

	double getBaseCCyProfit(){
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

	double getQuoteCCyProfit(){
		double profit = 0.00;
		ArrayList<Double> profitList = new ArrayList<Double>();
		if (positionArrayQuote.size() == 1)
			return 0.00;
		//Start from 3rd element because first element is zero 
		for (int profitLossCount=1; profitLossCount < positionArrayQuote.size() ; profitLossCount++  ){
			double profitCalculation =  positionArrayQuote.get(profitLossCount) - positionArrayQuote.get(profitLossCount-1)/ positionArrayQuote.get(profitLossCount-1);
			profitList.add(profitCalculation);
		}
		profit = profitList.stream().mapToDouble(i -> i.doubleValue()).sum();
		return profit;
	}
	
	@Override
	public String getDCCurveName() {
		
		return "DCCurveMF";
	}

	@Override
	double trainingTrading(PreProcess preprocess) {
		boolean isPositionOpen =  false;
		double myPrice = 0.0;
		double DD = 0;// DrawDown
		double lastClosedPosition = 0.0;
		double transactionCost = 0.025/100;
		lastSellPrice = 0.0;
		lastBuyPrice = 0.0;
		for (int i =1 ; i < trainingEvents.length; i++){
			
			
			int tradePoint =  0;
			tradePoint =  (int) (trainingEvents[i].end + (trainingEvents[i].length() * meanRatio[0]));
			
			if (trainingEvents[i] ==  null)
				continue;
			
			if (i+1 >  trainingEvents.length-1)
					continue;
				
				if (trainingEvents[i+1] == null)
					continue;
			
			if (tradePoint >   trainingEvents[i+1].start)  // If a new DC is encountered before the estimation point skip trading
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
			

			if (trainingEvents[i].type == Type.Upturn && !isPositionOpen){
				// I sell base currency in bid price
				double askQuantity = trainingOpeningPosition;
				double zeroTransactionCostAskQuantity = trainingOpeningPosition;
				double transactionCostPrice = 0.0;
				myPrice = Double.parseDouble(fileMember2.askPrice);
				
				
				transactionCost = askQuantity * (0.025/100);
				transactionCostPrice = transactionCost * myPrice;
				askQuantity =  (askQuantity -transactionCost) *myPrice;
				zeroTransactionCostAskQuantity = zeroTransactionCostAskQuantity *myPrice;
			
				if (transactionCostPrice < (zeroTransactionCostAskQuantity - askQuantity)){
//				if (transactionCostPrice < (zeroTransactionCostAskQuantity - askQuantity) 
//						&& (((lastSellPrice > 0.0) ? ((myPrice >= lastSellPrice) ? true : false): true ) ||
//								(StartSellQuantity > -1.0  ? ((StartSellQuantity <= askQuantity) ? true : false) : true  ))) {
							
							if (StartSellQuantity <= -1.0)
								StartSellQuantity = OpeningPosition;
					
					lastSellPrice = myPrice;
					trainingOpeningPosition = askQuantity;
					isPositionOpen = true;
				}
			}
			else if (trainingEvents[i].type == Type.Downturn &&  isPositionOpen){
				//Now position is in base currency
				// I buy base currency
				double bidQuantity = trainingOpeningPosition;
				double zeroTransactionCostBidQuantity = trainingOpeningPosition;
				double transactionCostPrice = 0.0;
				myPrice = Double.parseDouble(fileMember2.bidPrice);
				
				
				transactionCost = bidQuantity * (0.025/100);
				transactionCostPrice = transactionCost * myPrice;
				bidQuantity =  (bidQuantity -transactionCost) *myPrice;
				zeroTransactionCostBidQuantity = zeroTransactionCostBidQuantity *myPrice;
	
				
				if (transactionCostPrice < (zeroTransactionCostBidQuantity - bidQuantity)){
//				if (transactionCostPrice < (zeroTransactionCostBidQuantity - bidQuantity) 
//						&& (( lastBuyPrice > 0.0 ? ((myPrice <= lastBuyPrice ) ? true :false ): true )||
//								(StartBuyQuantity > -1.0  ? ((StartBuyQuantity > bidQuantity) ? true: false) : true  ))) {
									
							if (StartBuyQuantity <= -1.0)
								StartBuyQuantity = OpeningPosition;
							
					lastBuyPrice = myPrice;
					trainingOpeningPosition =  (trainingOpeningPosition -transactionCost) /myPrice;
					lastClosedPosition = trainingOpeningPosition;
					isPositionOpen = false;
				}
			}
			
			
			
		}
		
		
		if (isPositionOpen ){
			trainingOpeningPosition = lastClosedPosition;
		}
		
		
		return trainingOpeningPosition;

	}
	
	@Override
	void estimateTraining( PreProcess preprocess){
		;
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
		meanRatio[0] =   ((Double) inputArray[0]).doubleValue();
	}

}
