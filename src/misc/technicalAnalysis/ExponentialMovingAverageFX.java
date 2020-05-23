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
import eu.verdelhan.ta4j.indicators.EMAIndicator;
import eu.verdelhan.ta4j.indicators.helpers.ClosePriceIndicator;
import eu.verdelhan.ta4j.trading.rules.AndRule;
import eu.verdelhan.ta4j.trading.rules.CrossedDownIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.CrossedUpIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.OverIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.UnderIndicatorRule;

public class ExponentialMovingAverageFX  extends TechnicalAnaysisBaseTrading{

	public ExponentialMovingAverageFX(List<Tick> bidTicks, List<Tick> askTicks, String name, double openPosition){
		
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
	        EMAIndicator emaBid5 = new EMAIndicator(bidPrice, 5);
	        EMAIndicator emaBid20 = new EMAIndicator(bidPrice, 20);
	        EMAIndicator emaBid50 = new EMAIndicator(bidPrice, 50);

	        
	        EMAIndicator emaAsk5 = new EMAIndicator(askPrice, 5);
	        EMAIndicator emaAsk20 = new EMAIndicator(askPrice, 20);
	        EMAIndicator emaAsk50 = new EMAIndicator(askPrice, 50);
	        
	       // Buy when the five-period EMA crosses from below 
	       // to above the 20-period EMA, and the price, five, and 
	       // 20-period EMAs are above the 50 EMA.
	        
	        Rule entryRule2 = new OverIndicatorRule(emaBid5, emaBid50) // Bull trend
	                .and(new OverIndicatorRule(emaBid20, emaBid50))
	                		.and(new OverIndicatorRule(bidPrice, emaBid50)); // Signal
	            
	       Rule entryRule1 = new CrossedUpIndicatorRule(emaBid5, emaBid20); // Trend
            
	       Rule bidRule  = new AndRule(entryRule1, entryRule2); 
	       
	       
	      // For a sell trade, sell when the five-period EMA crosses 
	      //from above to below the 20-period EMA, and both EMAs and the 
	       //price are below the 50-period EMA.
	       
	       Rule exitRule1 = new CrossedDownIndicatorRule(emaAsk5, emaAsk20); // Trend
	       Rule exitRule2 = new UnderIndicatorRule(emaAsk5, emaBid50) // Bull trend
	                .and(new UnderIndicatorRule(emaAsk20, emaAsk50))
	                		.and(new UnderIndicatorRule(askPrice, emaAsk50)); // Signal
	       
	       
	       Rule askRule  = new AndRule(exitRule1, exitRule2); 
	       Strategy fxBuySellSignals = new BaseStrategy(
	    		   entryRule1,
	    		   exitRule1
	        );
	        
	       
	        return fxBuySellSignals;
	    }


}
