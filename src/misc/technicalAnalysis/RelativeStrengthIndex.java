package misc.technicalAnalysis;

import java.util.ArrayList;
import java.util.List;

import eu.verdelhan.ta4j.BaseStrategy;
import eu.verdelhan.ta4j.BaseTimeSeries;
import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Order;
import eu.verdelhan.ta4j.Rule;
import eu.verdelhan.ta4j.Strategy;
import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.TimeSeriesManager;
import eu.verdelhan.ta4j.TradingRecord;
import eu.verdelhan.ta4j.Order.OrderType;
import eu.verdelhan.ta4j.indicators.RSIIndicator;
import eu.verdelhan.ta4j.indicators.SMAIndicator;
import eu.verdelhan.ta4j.indicators.helpers.ClosePriceIndicator;
import eu.verdelhan.ta4j.trading.rules.CrossedDownIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.CrossedUpIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.OverIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.UnderIndicatorRule;

public class RelativeStrengthIndex extends TechnicalAnaysisBaseTrading{

	public RelativeStrengthIndex(List<Tick> bidTicks, List<Tick> askTicks, String name, double openPosition){
		
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
        
        
    
        SMAIndicator bidShortSma = new SMAIndicator(bidPrice, 5);
        SMAIndicator bidLongSma = new SMAIndicator(bidPrice, 200);
        SMAIndicator askShortSma = new SMAIndicator(askPrice, 5);
        SMAIndicator askLongSma = new SMAIndicator(askPrice, 200);

        // We use a 2-period RSI indicator to identify buying
        // or selling opportunities within the bigger trend.
        RSIIndicator bidRsi = new RSIIndicator(bidPrice, 2);
        RSIIndicator askRsi = new RSIIndicator(askPrice, 2);
        
        // Entry rule
        // The long-term trend is up when a security is above its 200-period SMA.
        Rule entryRule = new OverIndicatorRule(bidShortSma, bidLongSma) // Trend
                .and(new CrossedDownIndicatorRule(bidRsi, Decimal.valueOf(5))) // Signal 1
                .and(new OverIndicatorRule(bidShortSma, bidPrice)); // Signal 2
        
        // Exit rule
        // The long-term trend is down when a security is below its 200-period SMA.
        Rule exitRule = new UnderIndicatorRule(askShortSma, askLongSma) // Trend
                .and(new CrossedUpIndicatorRule(askRsi, Decimal.valueOf(95))) // Signal 1
                .and(new UnderIndicatorRule(askShortSma, askPrice)); // Signal 2
        
        
        
        
       
       Strategy fxBuySellSignals = new BaseStrategy(
    		   entryRule,
    		   exitRule
        );
        
       
        return fxBuySellSignals;
    }

	
}
