package misc;

import static dc.ga.DCCurve.Type.Downturn;
import static dc.ga.DCCurve.Type.DownwardOvershoot;
import static dc.ga.DCCurve.Type.Upturn;
import static dc.ga.DCCurve.Type.UpwardOvershoot;

import java.util.ArrayList;
import java.util.Arrays;

import dc.ga.DCCurve.Event;
import dc.ga.DCCurve.Type;

public class DCEventGenerator {
	 String thresholdString = "";
	 Event[] output;
	 Event[] generatedEvents;
	 int numberOfDCEvent =0;
	 int numberOfOSEvent= 0;
	 double osToDcEventRatio = 0.0;
	 double averageDCRunLength = 0.0;
	 double averageDownwardDCRunLength = 0.0;
	 double averageUpwardDCRunLength = 0.0;
	 
	 
	/**
	 * 
	 * @param values
	 *            The array with prices/tick data
	 * @param delta
	 *            The threshold value
	 * @param GPTreeFileName
	 *            the name of the file where GP tree is stored
	 */
	 public void generateEvents(Double[] values, double delta) {
		ArrayList<Event> events = new ArrayList<Event>();
		Type event = Type.Upturn;

		Event last = new Event(0, 0, Type.Upturn);
		events.add(last);

		
		output = new Event[values.length];
		
		double pHigh = 0;
		double pLow = 0;

		int[] indexDC = new int[2]; // DC event indexes
		int[] indexOS = new int[2]; // OS event indexes
		int index = 1;

		for (double value : values) {
			if (index == 1) {
				// it is the first line

				pHigh = value;
				pLow = value;

				Arrays.fill(indexDC, index);
				Arrays.fill(indexOS, index);
			} else if (event == Type.Upturn) {
				if (value <= (pHigh * (1 - delta))) {
					last.overshoot = detect2(UpwardOvershoot, indexDC, indexOS,value);

					adjust(last.overshoot == null ? last : last.overshoot, indexOS, indexOS);

					event = Downturn;
					double percentageDisplacement = Math.abs(pHigh-pLow) /100.0;
					pLow = value;

					
						indexDC[1] = index;
						indexOS[0] = index + 1;
					
					last = new Event(indexDC[0], indexDC[1], Downturn,percentageDisplacement);
					events.add(last);
				} else if (pHigh < value) {
					pHigh = value;
					
					
						indexDC[0] = index;
						indexOS[1] = index - 1;
					
				}
			} else {
				if (value >= (pLow * (1 + delta))) {
					last.overshoot = detect2(DownwardOvershoot, indexDC, indexOS,value);

					adjust(last.overshoot == null ? last : last.overshoot, indexDC, indexOS);
					double percentageDisplacement = Math.abs(pHigh-pLow) /100.0;
					event = Upturn;
					pHigh = value;

					
						indexDC[1] = index;
						indexOS[0] = index + 1;
					
					//[27.02545, 27.02545, 27.02545, 27.02545, 27.02545, 27.02545, 27.0265, 27.02495, 27.0265, 27.0255, 27.0265, 27.025,
				//	27.02495 = low, 27.0265  , low index 9

					last = new Event(indexDC[0], indexDC[1], Upturn,percentageDisplacement);
					events.add(last);
				} else if (pLow > value) {
					pLow = value;
					
					
						indexDC[0] = index;
						indexOS[1] = index - 1;
					
				}
			}

			output[index - 1] = last;

			index++;
		}
	System.out.println(" generateEvents completed for " + delta);
		this.generatedEvents = events.toArray(new Event[events.size()]); 
		
		//this.eventsClassifier = events.toArray(new Event[events.size()]);
		numberOfDCEvent = generatedEvents.length;
		int downwardEventCount = 0;
		int upwardEventCount = 0;
		for (int i = 0; i< generatedEvents.length ; i++){
			averageDCRunLength =  averageDCRunLength +  Double.valueOf(generatedEvents[i].length());
			if (generatedEvents[i].type  == Type.Downturn){
				averageDownwardDCRunLength = averageDownwardDCRunLength +  Double.valueOf(generatedEvents[i].length());
				downwardEventCount++;
			}
			else
			{
				averageUpwardDCRunLength = averageUpwardDCRunLength +  Double.valueOf(generatedEvents[i].length());
				upwardEventCount++;
			}
			if (generatedEvents[i].overshoot == null
					|| generatedEvents[i].overshoot.end == generatedEvents[i].overshoot.start){
				numberOfOSEvent++;
			}
		}
		
		averageDCRunLength = averageDCRunLength/numberOfDCEvent;
		osToDcEventRatio = Double.valueOf(numberOfOSEvent)/Double.valueOf(numberOfDCEvent);
		
		averageDownwardDCRunLength = averageDownwardDCRunLength/downwardEventCount;
		averageUpwardDCRunLength = averageUpwardDCRunLength/upwardEventCount;
		
		
	}

	 public Event[] getEvents(){
		return generatedEvents;
	}
	 
	
	

	protected  Event detect2(Type type, int[] indexDC, int[] indexOS, double value) {
		// overshoot event must have a start index lower that
		// the DC event start index
		if (indexOS[0] < indexOS[1] && indexOS[0] < indexDC[0]) {
			return new Event(indexOS[0], indexOS[1], type, value);
		}

		return null;
	}

	protected  void adjust(Event last, int[] indexDC, int[] indexOS) {
		// we might miss the start of an event
		if (indexDC[0] == last.start) {
			indexDC[0] = last.end + 1;
		}
		// we might skip the start of an event when there
		// are repeated values or large increases during an
		// upturn overshoot followed by a downturn event and
		// vice-versa (the overshoot will be invalid since
		// the end index will be smaller than the start index)
		else if (indexDC[0] > (last.end + 1)) {
			indexDC[0] = (last.end + 1);
		}
	}

	public Event findEvent(int index) {
		Event last = null;

		for (Event e : generatedEvents) {
			if (index < e.end) {
				break;
			}

			last = e;
		}

		return last;
	}
	
	public int getNumberOfDCEvent(){
		return numberOfDCEvent;
	 }
	 
	 
	 public int getNumberOfOSEvent(){
		 
			return numberOfOSEvent;
	 }
	
	 public double getOsToDcEventRatio(){
			return osToDcEventRatio;
	 }
	 
	 public double getAverageDCRunLength(){
			return averageDCRunLength;
	 }
	 
	 
	 public double getAverageDownwardDCRunLength(){
			return averageDownwardDCRunLength;
	 }
	 
	 public double getAverageUpwardDCRunLength(){
			return averageUpwardDCRunLength;
		 }
		 


}
