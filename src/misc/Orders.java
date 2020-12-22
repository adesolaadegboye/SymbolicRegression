package misc;

public class Orders {
	public double threshold = 0.0;
	public double prediction = 0.0;
	public boolean isOSevent = false;
	public boolean isSell = false;
	public dc.ga.DCCurve.Type eventType =  null;
	public int eventConfirmationPoint = -1;
	public int dcEventStart = 0;
	public int dcEventEnd = 0;
	public int dcOsEventStart = 0;
	public int dcOsEventEnd = 0;
}