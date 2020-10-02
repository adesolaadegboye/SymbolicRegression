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
import eu.verdelhan.ta4j.indicators.ROCIndicator;
import eu.verdelhan.ta4j.indicators.RSIIndicator;
import eu.verdelhan.ta4j.indicators.helpers.ClosePriceIndicator;
import eu.verdelhan.ta4j.trading.rules.AndRule;
import eu.verdelhan.ta4j.trading.rules.CrossedDownIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.CrossedUpIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.OverIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.StopLossRule;
import eu.verdelhan.ta4j.trading.rules.UnderIndicatorRule;

public class PriceRateOfChangeIndicator  extends TechnicalAnaysisBaseTrading{

	public PriceRateOfChangeIndicator(List<Tick> bidTicks, List<Tick> askTicks, String name, double openPosition){
		
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
	       
	        ClosePriceIndicator askClosePrice = new ClosePriceIndicator(askSeries);
	        ClosePriceIndicator bidClosePrice = new ClosePriceIndicator(bidSeries);

	        ROCIndicator bidRocIndicator = new ROCIndicator(bidClosePrice, 12);
	        ROCIndicator askRocIndicator = new ROCIndicator(askClosePrice, 12);
	        
	        // Entry rule
	        Rule entryRule = new CrossedUpIndicatorRule(askRocIndicator, Decimal.valueOf(0));

	        // Exit rule
	        Rule exitRule = new CrossedDownIndicatorRule(bidRocIndicator, Decimal.valueOf(0));
	        
	   
	       Strategy fxBuySellSignals = new BaseStrategy(
	    		   entryRule,
	    		   exitRule
	        );
	        
	       
	        return fxBuySellSignals;
	    }


}
