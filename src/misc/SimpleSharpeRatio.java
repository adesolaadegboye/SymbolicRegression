
package misc;

import java.util.ListIterator;
import java.util.Vector;

import org.apache.commons.math3.stat.descriptive.*;


public class SimpleSharpeRatio {
	
	 
    private double sharpeRatio; 
    private double movingSharpRatio;
    
    private double returns=0;
    
    public Vector<Double> returnsList = new Vector<Double>();
    
    private Vector<Double> movingSharpRatioList= new Vector<Double>();
  
    private DescriptiveStatistics movingStats = new DescriptiveStatistics();
    
    private DescriptiveStatistics stats = new DescriptiveStatistics();
    
    private org.apache.commons.math3.stat.descriptive.rank.Median median = null;
    
    public double getReturns(){
    	return returns;
    }
    
    
    public double getSharpeRatio(){
    	return sharpeRatio;
    }
    
    public void setSharpeRatio(double s ){
    	sharpeRatio = s;
    }
    
    public double getMovingSharpRatio(){
    	return movingSharpRatio;
    }
    
    
    public void addReturn(double rtn){
    	returnsList.add(rtn);
    	returns =  rtn;
    }
    
   
    
    
    public double getMovingSharpeRatio(){
    	if (returnsList.size() < 5){
    		
    		movingSharpRatioList.add(0.0);
    		return 0.0;
    	}
    	
    	//read in revers order
    	int count = 0;
    	// Generate an iterator. Start just after the last element.
    	ListIterator li = returnsList.listIterator(returnsList.size());
    	movingStats.clear();
    	
    	while(li.hasPrevious()) {
    		if (count > 5)
    			break;
    		
    		movingStats.addValue((double) li.previous());
    		//System.out.println(li.previous());
    		count++;
    	}
    	double riskFreeReturn = 0.0;
    	double mean = movingStats.getMean();

        double std = movingStats.getStandardDeviation();

        double sharpeRatio = (mean - (riskFreeReturn) ) / std;
        movingSharpRatioList.add(sharpeRatio);
        movingSharpRatio = sharpeRatio;

        return sharpeRatio;
    }
    
    public double getSharpeRatio(double rtn){
    	
    	stats.clear();
    	for (int i =0; i < returnsList.size(); i++)
    	{
    		stats.addValue(returnsList.get(i));
    	}
    	
    	
    	double riskFreeReturn = 0.0;
    	double mean = stats.getMean();

        double std = stats.getStandardDeviation();

        double sharpeRatio = (mean - (riskFreeReturn) ) / std;
        

        return sharpeRatio;

    }
    
    public double calulateSharpeRatio(){
    
    	if (returnsList.size() < 2)
    		return 0.0;
    	stats.clear();
    	for (int i =0; i < returnsList.size(); i++)
    	{
    		stats.addValue(returnsList.get(i));
    	}
    	
    	
    	double riskFreeReturn = 0.0;
    	double mean = stats.getMean();

        double std = stats.getStandardDeviation();

        double sharpeRatio = (mean - (riskFreeReturn) ) / std;
        

        return sharpeRatio;

    }
    
    public void rmoveAllReturns(){
    	returnsList.clear();
    	returns = -1;
    	
    }
    
    public void removeLastElementFromreturnsList(){
    	if(!returnsList.isEmpty())
    	    returnsList.remove(returnsList.size()-1) ;
    	
    }
    
    
    public double calulateVariance(){
        
    	if (returnsList.size() < 2)
    		return 0.0;
    	stats.clear();
    	for (int i =0; i < returnsList.size(); i++)
    	{
    		stats.addValue(returnsList.get(i));
    	}
    	
    
        
        return stats.getVariance();

    }
    

}

