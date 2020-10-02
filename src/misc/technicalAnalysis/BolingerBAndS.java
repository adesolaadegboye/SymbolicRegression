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
import eu.verdelhan.ta4j.indicators.EMAIndicator;
import eu.verdelhan.ta4j.indicators.SMAIndicator;
import eu.verdelhan.ta4j.indicators.bollinger.BollingerBandsMiddleIndicator;
import eu.verdelhan.ta4j.indicators.bollinger.BollingerBandsUpperIndicator;
import eu.verdelhan.ta4j.indicators.helpers.ClosePriceIndicator;
import eu.verdelhan.ta4j.indicators.statistics.StandardDeviationIndicator;
import eu.verdelhan.ta4j.trading.rules.AndRule;
import eu.verdelhan.ta4j.trading.rules.CrossedDownIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.CrossedUpIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.OverIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.StopLossRule;
import eu.verdelhan.ta4j.trading.rules.UnderIndicatorRule;

public class BolingerBAndS  extends TechnicalAnaysisBaseTrading{

	public BolingerBAndS(List<Tick> bidTicks, List<Tick> askTicks, String name, double openPosition){
		
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

	        final int period = 8;
	        final ClosePriceIndicator bidClosePrice = new ClosePriceIndicator(bidSeries);
	        final ClosePriceIndicator askClosePrice = new ClosePriceIndicator(askSeries);
	        final SMAIndicator bidSma = new SMAIndicator(bidClosePrice, period);
	        final SMAIndicator askSma = new SMAIndicator(bidClosePrice, period);
	        
	        final BollingerBandsMiddleIndicator bidBBmiddle = new BollingerBandsMiddleIndicator(bidSma);
	        final BollingerBandsMiddleIndicator askBBmiddle = new BollingerBandsMiddleIndicator(askSma);
	        
	        
	        final StandardDeviationIndicator bidSd = new StandardDeviationIndicator(bidClosePrice, period);
	        final StandardDeviationIndicator askSd = new StandardDeviationIndicator(askClosePrice, period);
	         
	        final BollingerBandsUpperIndicator bbdown = new BollingerBandsUpperIndicator(bidBBmiddle, bidSd);
	        final BollingerBandsUpperIndicator bbup  = new BollingerBandsUpperIndicator(askBBmiddle, askSd);

	        final Rule askingRule = new UnderIndicatorRule(askClosePrice, bbup);
	        
	        final Rule biddingRule = new OverIndicatorRule(bidClosePrice, bbdown);

	        final BaseStrategy strategy = new BaseStrategy(askingRule, biddingRule);
	        
	        return strategy;
	        
	        
	      
	    }


}
