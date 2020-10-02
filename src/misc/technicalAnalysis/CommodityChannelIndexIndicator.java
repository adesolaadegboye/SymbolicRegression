package misc.technicalAnalysis;

import java.util.ArrayList;
import java.util.List;

import eu.verdelhan.ta4j.BaseStrategy;
import eu.verdelhan.ta4j.BaseTimeSeries;
import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Order;
import eu.verdelhan.ta4j.Order.OrderType;
import eu.verdelhan.ta4j.Rule;
import eu.verdelhan.ta4j.Strategy;
import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.TimeSeriesManager;
import eu.verdelhan.ta4j.TradingRecord;
import eu.verdelhan.ta4j.indicators.CCIIndicator;
import eu.verdelhan.ta4j.indicators.EMAIndicator;
import eu.verdelhan.ta4j.indicators.helpers.ClosePriceIndicator;
import eu.verdelhan.ta4j.trading.rules.AndRule;
import eu.verdelhan.ta4j.trading.rules.CrossedDownIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.CrossedUpIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.OverIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.UnderIndicatorRule;

public class CommodityChannelIndexIndicator  extends TechnicalAnaysisBaseTrading{

	public CommodityChannelIndexIndicator(List<Tick> bidTicks, List<Tick> askTicks, String name, double openPosition){
		
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
	        
	        
	        
	        CCIIndicator bidLongCci = new CCIIndicator(bidSeries, 200);
	        CCIIndicator bidShortCci = new CCIIndicator(bidSeries, 5);
	        
	        CCIIndicator askLongCci = new CCIIndicator(askSeries, 200);
	        CCIIndicator askShortCci = new CCIIndicator(askSeries, 5);
	        
	        
	        Decimal plus100 = Decimal.HUNDRED;
	        Decimal minus100 = Decimal.valueOf(-100);
	        
	        Rule entryRule = new OverIndicatorRule(askLongCci, plus100) // Bull trend
	                .and(new UnderIndicatorRule(askShortCci, minus100)); // Signal
	        
	        Rule exitRule = new UnderIndicatorRule(bidLongCci, minus100) // Bear trend
	                .and(new OverIndicatorRule(bidShortCci, plus100)); // Signal
	        
	        Strategy strategy = new BaseStrategy(entryRule, exitRule);
	        strategy.setUnstablePeriod(5);
	        
	        
	        

	      	       
	        return strategy;
	    }


}
