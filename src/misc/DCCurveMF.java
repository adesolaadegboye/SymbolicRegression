package misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

import dc.ga.HelperClass;
import dc.ga.PreProcess;
import dc.ga.DCCurve.Event;
import dc.ga.DCCurve.Type;
import dc.io.FReader;
import dc.io.FReader.FileMember2;

public class DCCurveMF  extends DCCurveRegression{

	public DCCurveMF(){
		super();
		meanRatio = new double[2];
		meanRatio[0] = 0.0;
		meanRatio[1] = 0.0;
		
	}

	
	/**
	 * 0 = downward overshoot 1 = upward overshoot
	 */
	double[] medianRatio = new double[2];

	/**
	 * 
	 * @param values
	 *            The array with prices/tick data
	 * @param delta
	 *            The threshold value
	 * @param GPTreeFileName
	 *            the name of the file where GP tree is stored
	 */
	public void build(Double[] values, double delta, String GPTreeFileName, Event[] events, 
			Event[] trainingOutput,PreProcess preprocess) {

		String thresholdStr = String.format("%.8f", delta);
		thresholdString = thresholdStr;
		
		if ( events == null || events.length <1)
			return;
		
	
		trainingEvents =  Arrays.copyOf(events, events.length) ;
		this.trainingOutputEvents =  Arrays.copyOf(trainingOutput, trainingOutput.length);
		
		if (meanRatio[0] > 0.0 && meanRatio[1] > 0.0)
			return;
	
		double meanDownturn = 0.0;
		double meanDownwardOvershoot = 0.0;

		int downturn = 0;
		// upward overshoots
		medianRatio[1] = 0.0;
		
		//downward overshoots
		medianRatio[0] = 0.0;

		double meanUpturn = 0.0;
		double meanUpwardOvershoot = 0.0;

		int upturn = 0;

		ArrayList<Double> downwardRatio = new ArrayList<Double>();
		ArrayList<Double> upwardRatio = new ArrayList<Double>();

		for (Event e : events) {
			// we ignore the first (artificial) event
			if (e.start > 0) {
				double ratio = 0.0;

				if (e.overshoot != null) {
					ratio = (e.overshoot.length() / (double) e.length());
				}

				if (e.type == Type.Upturn) {
					upturn++;
					meanRatio[1] += ratio;
					upwardRatio.add(ratio);

					meanUpturn += e.length();
					meanUpwardOvershoot += (e.overshoot != null) ? e.overshoot.length() : 0;
				} else if (e.type == Type.Downturn) {
					downturn++;
					meanRatio[0] += ratio;
					downwardRatio.add(ratio);

					meanDownturn += e.length();
					meanDownwardOvershoot += (e.overshoot != null) ? e.overshoot.length() : 0;

				}
			}
		}

		meanRatio[0] = (meanDownwardOvershoot / downturn) / (meanDownturn / downturn);
		meanRatio[1] = (meanUpwardOvershoot / upturn) / (meanUpturn / upturn);
		
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
				rmse = rmse + ((os - (trendEvent[eventCount].length() * meanRatio[1]))
						* (os - (trendEvent[eventCount].length() * meanRatio[1])));
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
	double trade(PreProcess preprocess) {
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
			if (testingEvents[i].type == Type.Upturn)
				tradePoint =  (int) (testingEvents[i].end + (testingEvents[i].length() * meanRatio[1]));
			else if (testingEvents[i].type == Type.Downturn)
				tradePoint =  (int) (testingEvents[i].end + (testingEvents[i].length() * meanRatio[0]));
			
			
			if (testingEvents[i] ==  null)
				continue;
			
			if (i+1 >   testingEvents.length-1)
					continue;
				
			if (testingEvents[i+1] == null)
					continue;
			
			int nextEventEndPOint = HelperClass.getNextDirectionaChangeEndPoint(testingEvents,  tradePoint);

			if (tradePoint >  nextEventEndPOint)  // If a new DC is encountered before the estimation point skip trading
				continue;
			
			FReader freader = new FReader();
			FileMember2 fileMember2 =  freader.new FileMember2();
			//fileMember2.Day = GPtestEvents[i].endDate;
			//fileMember2.time = GPtestEvents[i].endTime;
			//fileMember2.price = GPtestEvents[i].endPrice;
			
			
			if (  tradePoint > FReader.dataRecordInFileArray.size() ||  (lastTrainingPrice-1)+tradePoint > FReader.dataRecordInFileArray.size() ){
				System.out.println(" DCCurveMF: predicted datapoint "
						+ ((lastTrainingPrice - 1) + tradePoint) + " is beyond the size of price array  "
						+ FReader.dataRecordInFileArray.size() + " . Trading ended");
				break;
			}
			else {
				// I am opening my position in base currency
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
			
					lastSellPrice = myPrice;
					OpeningPosition = askQuantity;
					isPositionOpen = true;
					positionArrayQuote.add(new Double(OpeningPosition));
					
					tradedPrice.add(new Double(myPrice));
					anticipatedTrendMap.put(testingEvents[i].start, tradePoint);
					anticipatedTrend.add(anticipatedTrendMap);
					
					if (testingEvents[i].overshoot == null || testingEvents[i].overshoot.length() < 1)
						actualTrendMap.put(testingEvents[i].start, testingEvents[i].end );
					else
						actualTrendMap.put(testingEvents[i].start, testingEvents[i].overshoot.end );
					
					lastUpDCCend = Double.parseDouble(FReader.dataRecordInFileArray.get((lastTrainingPrice - 1) + testingEvents[i].end).bidPrice);
					
					
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
			

				if (transactionCostPrice < (zeroTransactionCostBidQuantity - bidQuantity)
						&& myPrice < lastUpDCCend){
					
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
					System.out.println(" DCCurveMF: Search for element "
							+ ((lastTrainingPrice - 1) + tradePoint) + " is beyond the size of price array  "
							+ FReader.dataRecordInFileArray.size() + " . Trading ended");
					break;

				}
				catch (IndexOutOfBoundsException exception ){
					System.out.println(" DCCurveMF: Search for element " + ((lastTrainingPrice - 1) + tradePoint)
							+ " is beyond the size of price array  " + 
							FReader.dataRecordInFileArray.size() + " . Trading ended") ;
					break;
				}
				catch (Exception exception ){
					System.out.println(" DCCurveMF: Search for element " + ((lastTrainingPrice - 1) + tradePoint)
							+ " is beyond the size of price array  " + 
							FReader.dataRecordInFileArray.size() + " . Trading ended") ;
					break;
				}

			}

		}

		
		if (isPositionOpen )
		{
			positionArrayQuote.remove(positionArrayQuote.size()-1);
			tradedPrice.remove(tradedPrice.size()-1);
			anticipatedTrend.remove(anticipatedTrend.size()-1);
			actualTrend.remove(actualTrend.size() - 1 );
			OpeningPosition = positionArrayBase.get(positionArrayBase.size() - 1);
		
		}
		
		if (positionArrayBase.size() > 1){
			//Calculate sharp ratio start from 1 as it is a moving window
			for(int srCount = 1 ; srCount < positionArrayBase.size(); srCount++ ){
				simpleSharpeRatio.addReturn(positionArrayBase.get(srCount) - positionArrayBase.get(srCount-1) );
			}
		}
		else if (positionArrayBase.size() == 1)
			simpleSharpeRatio.addReturn(positionArrayBase.get(0) - OpeningPosition);
		
		//closingPosition = currentPosition-initialPosition;
		for(LinkedHashMap<Integer, Integer> map : actualTrend ){
			int  key = map.keySet().iterator().next();
			int value = map.get(key);
			
			actualTrendString =  actualTrendString + Double.toString(key) + " ," + Double.toString(value) + "\n";
			
		}
		
		for(LinkedHashMap<Integer, Integer> map : anticipatedTrend ){
			int  key = map.keySet().iterator().next();
			int value = map.get(key);
			
			predictedTrendString =  predictedTrendString + Double.toString(key) + " ," + Double.toString(value)  + "\n";
			
		}

		for (int profitLossCount=0; profitLossCount < positionArrayBase.size() ; profitLossCount++  ){
			simpleDrawDown.Calculate(positionArrayBase.get(profitLossCount));
		}
		
		for (int profitLossCount=0; profitLossCount < positionArrayQuote.size() ; profitLossCount++  ){
			simpleDrawDownQuote.Calculate(positionArrayQuote.get(profitLossCount));
		}
		
		peakMDD = simpleDrawDown.getMaxDrawDown();
		peakMDDQuote = simpleDrawDownQuote.getMaxDrawDown();
		
		return OpeningPosition;
	}
	
	
	
	double getMddPeak(){
		return simpleDrawDown.getPeak();
	}
	
	double getMddTrough(){
		return simpleDrawDown.getTrough();
	}
	
	@Override
	public 
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
			double profitCalculation =  positionArrayQuote.get(profitLossCount) - positionArrayQuote.get(profitLossCount-1)/ positionArrayBase.get(profitLossCount-1);
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
		return Double.MIN_VALUE;

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
		// TODO Auto-generated method stub
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
		meanRatio[0] =  ((Double) inputArray[0]).doubleValue();
		meanRatio[1] =  ((Double)  inputArray[1]).doubleValue();
		
	}

}
