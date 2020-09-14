package misc;

public class SimpleDrawDown {

	private double Peak; 
    private double Trough; 
    private double MaxDrawDown = 0.0; 

    public double getPeak(){
    	return Peak;
    }
    
    public void setPeak(double peak ){
    	Peak = peak;
    }

    public double getTrough(){
    	return Trough;
    }
    
    public void setTrough(double trough ){
    	Trough = trough;
    }
    
    public double getMaxDrawDown(){
    	return MaxDrawDown;
    }
    
    public void setMaxDrawDown(double maxDrawDown ){
    	MaxDrawDown = maxDrawDown;
    }
    
    public SimpleDrawDown()
    {
        Peak = 0.0;
        Trough = 0.0;
        MaxDrawDown = 0.0;
    }

    public void Calculate(double newValue)
    {
        if (newValue > Peak)
        {
            Peak = newValue;
            Trough = Peak;
        }
        else if (newValue < Trough)
        {
            Trough = newValue;
            double tmpDrawDown = (( Trough -  Peak)/Peak) *100;
            
            if (MaxDrawDown ==  Double.NEGATIVE_INFINITY)
            	MaxDrawDown = tmpDrawDown;
            
           // if ( tmpDrawDown > MaxDrawDown)
                MaxDrawDown = tmpDrawDown;
        }
    }
}
