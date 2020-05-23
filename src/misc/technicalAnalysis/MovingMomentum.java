package misc.technicalAnalysis;

import java.util.ArrayList;
import java.util.List;


import eu.verdelhan.ta4j.BaseStrategy;
import eu.verdelhan.ta4j.BaseTimeSeries;
import eu.verdelhan.ta4j.Order;
import eu.verdelhan.ta4j.Rule;
import eu.verdelhan.ta4j.Strategy;
import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.TimeSeriesManager;
import eu.verdelhan.ta4j.TradingRecord;
import eu.verdelhan.ta4j.Order.OrderType;
import eu.verdelhan.ta4j.indicators.EMAIndicator;
import eu.verdelhan.ta4j.indicators.MACDIndicator;
import eu.verdelhan.ta4j.indicators.helpers.ClosePriceIndicator;
import eu.verdelhan.ta4j.trading.rules.CrossedDownIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.CrossedUpIndicatorRule;



public class MovingMomentum  extends TechnicalAnaysisBaseTrading{
	
	
	

	public MovingMomentum(List<Tick> bidTicks, List<Tick> askTicks, String name, double openPosition){
		
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
        
        MACDIndicator bidMacd= new MACDIndicator(bidPrice,12,26);
        EMAIndicator bidEma= new EMAIndicator(bidMacd,9);
        
        
        MACDIndicator askMacd= new MACDIndicator(askPrice,12,26);
        EMAIndicator askEma= new EMAIndicator(askMacd,9);

        Rule entryRule1 = new CrossedUpIndicatorRule(bidMacd,bidEma);                
        Rule exitRule1 = new CrossedDownIndicatorRule(askMacd, askEma);
        
               
        //Strategy strategy1 = new Strategy(entryRule1, exitRule1);

       
                
       
       Strategy fxBuySellSignals = new BaseStrategy(
    		   entryRule1,
    		   exitRule1
        );
        
       
        return fxBuySellSignals;
    }
}