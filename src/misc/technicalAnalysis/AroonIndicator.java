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
import eu.verdelhan.ta4j.indicators.AroonDownIndicator;
import eu.verdelhan.ta4j.indicators.AroonUpIndicator;
import eu.verdelhan.ta4j.indicators.RSIIndicator;
import eu.verdelhan.ta4j.indicators.helpers.ClosePriceIndicator;
import eu.verdelhan.ta4j.trading.rules.AndRule;
import eu.verdelhan.ta4j.trading.rules.CrossedDownIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.CrossedUpIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.OverIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.StopLossRule;
import eu.verdelhan.ta4j.trading.rules.UnderIndicatorRule;

public class AroonIndicator  extends TechnicalAnaysisBaseTrading{

	public AroonIndicator(List<Tick> bidTicks, List<Tick> askTicks, String name, double openPosition){
		
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
	        

	        AroonUpIndicator bidaroonUp = new AroonUpIndicator(bidSeries,25);
	        AroonUpIndicator askaroonUp = new AroonUpIndicator(askSeries,25);
	        
	        
	        AroonDownIndicator  askaroonDown = new AroonDownIndicator(askSeries,25);
	        AroonDownIndicator  bidaroonDown = new AroonDownIndicator(bidSeries,25);
	        
	        
	        RSIIndicator bidRsi = new RSIIndicator(bidPrice, 14);
	        RSIIndicator askRsi = new RSIIndicator(askPrice, 14);
	        
	        
	        Rule entryRule = new OverIndicatorRule(askaroonUp,
	        		askaroonDown).and(new CrossedUpIndicatorRule(askRsi,
	                Decimal.valueOf(30)));

	        
	        
	        Rule stopLoss = new StopLossRule(bidPrice, Decimal.valueOf(15));

	        Rule exitRule = new AndRule(new OverIndicatorRule(bidaroonDown,
	        		bidaroonUp), new CrossedDownIndicatorRule(bidRsi, Decimal
	                .valueOf(70))).or(stopLoss);
	        
	   
	       Strategy fxBuySellSignals = new BaseStrategy(
	    		   entryRule,
	    		   exitRule
	        );
	        
	       
	        return fxBuySellSignals;
	    }


}
