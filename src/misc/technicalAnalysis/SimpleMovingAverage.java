package misc.technicalAnalysis;

import java.util.ArrayList;
import java.util.List;

import eu.verdelhan.ta4j.BaseStrategy;
import eu.verdelhan.ta4j.BaseTimeSeries;
import eu.verdelhan.ta4j.Order;
import eu.verdelhan.ta4j.Order.OrderType;
import eu.verdelhan.ta4j.Rule;
import eu.verdelhan.ta4j.Strategy;
import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.TimeSeriesManager;
import eu.verdelhan.ta4j.TradingRecord;
import eu.verdelhan.ta4j.indicators.SMAIndicator;
import eu.verdelhan.ta4j.indicators.helpers.ClosePriceIndicator;
import eu.verdelhan.ta4j.trading.rules.AndRule;
import eu.verdelhan.ta4j.trading.rules.CrossedDownIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.CrossedUpIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.OverIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.UnderIndicatorRule;

public class SimpleMovingAverage  extends TechnicalAnaysisBaseTrading{

	public SimpleMovingAverage(List<Tick> bidTicks, List<Tick> askTicks, String name, double openPosition){
		
		if (bidTicks != null)
			this.bidTicks =  new ArrayList<>(bidTicks);
		
		if (askTicks != null)
			this.askTicks =  new ArrayList<>(askTicks);
		
		this.productName = name;
		
		this.startPosition = openPosition;
		
	}

	@Override
	public List<Order> getOrders(){
		
		TimeSeries bidSeries =  new BaseTimeSeries(productName,bidTicks);
		TimeSeries askSeries =  new BaseTimeSeries(productName,askTicks);
		
		
		TimeSeriesManager seriesManager = new TimeSeriesManager(bidSeries,askSeries);
		Strategy myStrategy =buildStrategy(bidSeries,askSeries);

		TradingRecord tradingRecord = seriesManager.run(myStrategy,OrderType.SELL);
		fullOrder = new ArrayList<Order>(tradingRecord.getOrders());
		return tradingRecord.getOrders();
		
	}
	
	
	
	 private static Strategy buildStrategy(TimeSeries bidSeries , TimeSeries askSeries) {
	        if (bidSeries == null ) {
	            throw new IllegalArgumentException("Series cannot be null");
	        }

	        ClosePriceIndicator bidPrice = new ClosePriceIndicator(bidSeries);
	        ClosePriceIndicator askPrice = new ClosePriceIndicator(askSeries);
	        SMAIndicator smaBid8 = new SMAIndicator(bidPrice, 5);
	        SMAIndicator smaBid20 = new SMAIndicator(bidPrice, 20);
	        SMAIndicator smaBid50 = new SMAIndicator(bidPrice, 50);

	        
	        SMAIndicator smaAsk8 = new SMAIndicator(askPrice, 5);
	        SMAIndicator smaAsk20 = new SMAIndicator(askPrice, 20);
	        SMAIndicator smaAsk50 = new SMAIndicator(askPrice, 50);
	        
	       // Buy when the five-period EMA crosses from below 
	       // to above the 20-period EMA, and the price, five, and 
	       // 20-period EMAs are above the 50 EMA.
	        
	       
	            
	       Rule entryRule1 = new CrossedUpIndicatorRule(smaBid8, smaBid20); // Trend
            
	    
	       
	      // For a sell trade, sell when the five-period EMA crosses 
	      //from above to below the 20-period EMA, and both EMAs and the 
	       //price are below the 50-period EMA.
	       
	       Rule exitRule1 = new CrossedDownIndicatorRule(smaAsk8, smaAsk50); // Trend
	      
	    
	      
	       Strategy fxBuySellSignals = new BaseStrategy(
	    		   entryRule1,
	    		   exitRule1
	        );
	        
	       
	        return fxBuySellSignals;
	    }


}
