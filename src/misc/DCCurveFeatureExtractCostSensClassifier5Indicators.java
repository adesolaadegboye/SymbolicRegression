package misc;

import static dc.ga.DCCurve.Type.Downturn;
import static dc.ga.DCCurve.Type.DownwardOvershoot;
import static dc.ga.DCCurve.Type.Upturn;
import static dc.ga.DCCurve.Type.UpwardOvershoot;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.Vector;

import dc.GP.AbstractNode;
import dc.ga.DCCurve.Event;
import dc.ga.DCCurve.Type;
import dc.io.FReader;
import dc.io.FReader.FileMember2;
import weka.classifiers.Classifier;
import weka.classifiers.CostMatrix;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.evaluation.NominalPrediction;
import weka.classifiers.functions.Logistic;
import weka.classifiers.functions.SMO;
import weka.classifiers.meta.CostSensitiveClassifier;
import weka.classifiers.rules.DecisionTable;
import weka.classifiers.rules.JRip;
import weka.classifiers.rules.PART;
import weka.classifiers.trees.DecisionStump;
import weka.classifiers.trees.J48;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.supervised.instance.Resample;
import weka.classifiers.evaluation.*;
import weka.core.AttributeStats;
import weka.filters.supervised.instance.SMOTE;

public class DCCurveFeatureExtractCostSensClassifier5Indicators {
	Event[] events;
	

	Event[] output;
	Event[] testOutput;

	

	

	
	String upwardTrendTreeString = null;
	String downwardTrendTreeString = null;
	String trendTreeString = null;
	String thresholdString = "";
	
	
	boolean isUpwardEvent = true;
	
	SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:SSS");
	static public ArrayList<String []> eventFeatures = new ArrayList<String []>();
	
	Instances train = null;
	Instances test = null;
	Instances data = null;
	int trainingCount =-1;
	CostSensitiveClassifier currentModel=null;
	CostSensitiveClassifier currentModelNoFilter=null;

	ArrayList<Event> eventChunksByTime1 = new ArrayList<Event>();
	ArrayList<Event> eventChunksByTime2 = new ArrayList<Event>();
	ArrayList<Event> eventChunksByTime3 = new ArrayList<Event>();
	ArrayList<Event> eventChunksByTime4 = new ArrayList<Event>();
	ArrayList<Event> eventChunksByTime5 = new ArrayList<Event>();
	ArrayList<Event> eventChunksByTime6 = new ArrayList<Event>();
	ArrayList<Event> eventChunksByTime7 = new ArrayList<Event>();
	ArrayList<Event> eventChunksByTime8 = new ArrayList<Event>();
	
	double percenatageTemporalByTime1 = 0.0;
	double percenatageTemporalByTime2 = 0.0;
	double percenatageTemporalByTime3 = 0.0;
	double percenatageTemporalByTime4 = 0.0;
	double percenatageTemporalByTime5 = 0.0;
	double percenatageTemporalByTime6 = 0.0;
	double percenatageTemporalByTime7 = 0.0;
	double percenatageTemporalByTime8 = 0.0;
	ArrayList<Event> eventChunksByWeek1 = new ArrayList<Event>();
	ArrayList<Event> eventChunksByWeek2 = new ArrayList<Event>();
	ArrayList<Event> eventChunksByWeek3 = new ArrayList<Event>();
	ArrayList<Event> eventChunksByWeek4 = new ArrayList<Event>();
	ArrayList<Event> eventChunksByWeek5 = new ArrayList<Event>();
	
	
	 private static final String SMO_OPTIONS = "-C 1.0 -L 0.0010 -P 1.0E-12 -N 0 -V -1 -W 1 -K \"weka.classifiers.functions.supportVector.PolyKernel -C 250007 -E 1.0\"";
	    private boolean buildLogisticModel = false;
	    private double falsePositiveCost = 1;
	    private double falseNegativeCost = 1;
	    private boolean useReweighting = false;
	    private boolean useCostTraining = false;
	
	
	public DCCurveFeatureExtractCostSensClassifier5Indicators(Classifier classifier, Classifier classifierNoFilter) {
		// TODO Auto-generated constructor stub
		CostMatrix cm = new CostMatrix(2);
		cm.normalize(); // set diaginals to 0
		// cm.setCell(rowIndex, int columnIndex, java.lang.Object value)
		cm.setCell(0, 1, 10.0);
		cm.setCell(1, 0, 6.0);
		
		currentModel = new CostSensitiveClassifier();
		currentModelNoFilter =  new CostSensitiveClassifier();
		// set up the cost sensitive classifier
		currentModel.setCostMatrix(cm);
		currentModelNoFilter.setCostMatrix(cm);
		
		currentModel.setCostMatrix(cm);
		currentModelNoFilter.setCostMatrix(cm);
		
		currentModel.setClassifier(classifier);
		currentModelNoFilter.setClassifier(classifierNoFilter);
		
		
	}

	/**
	 * 
	 * @param values
	 *            The array with prices/tick data
	 * @param delta
	 *            The threshold value
	 * @param GPTreeFileName
	 *            the name of the file where GP tree is stored
	 */
	public void build(Double[] values, double delta, String GPTreeFileName) {
		ArrayList<Event> events = new ArrayList<Event>();
		Type event = Type.Upturn;

		Event last = new Event(0, 0, Type.Upturn);
		events.add(last);

		String thresholdStr = String.format("%.8f", delta);
		thresholdString = thresholdStr;
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
					last.overshoot = detect(UpwardOvershoot, indexDC, indexOS);

					adjust(last.overshoot == null ? last : last.overshoot, indexDC, indexOS);

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
					last.overshoot = detect(DownwardOvershoot, indexDC, indexOS);

					adjust(last.overshoot == null ? last : last.overshoot, indexDC, indexOS);
					double percentageDisplacement = Math.abs(pHigh-pLow) /100.0;
					event = Upturn;
					pHigh = value;

					indexDC[1] = index;
					indexOS[0] = index + 1;
					
					

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

		this.events = events.toArray(new Event[events.size()]);
	}

	public void extractDCDatasetTemporalFeacture(String filename, String thresholdString,int numberOfPartitions){
	
	 
	 int x = numberOfPartitions;  // chunk size
	 int len = this.events.length;
	 
	 
	
	
	int i = 0;
	for ( i = 0; i < len; i++){
		if( this.events[i].endDate.equalsIgnoreCase(" ") || this.events[i].endDate.isEmpty())
			continue;
		
		try {
			 Date curDate =  sdf.parse(this.events[i].endDate + " "+ this.events[i].endTime);
			 Date date1 =  sdf.parse(this.events[i].endDate + " 03:00:00");
			 Date date2 =  sdf.parse(this.events[i].endDate + " 06:00:00");
			 Date date3 =  sdf.parse(this.events[i].endDate + " 09:00:00");
			 Date date4 =  sdf.parse(this.events[i].endDate + " 12:00:00");
			 Date date5 =  sdf.parse(this.events[i].endDate + " 15:00:00");
			 Date date6 =  sdf.parse(this.events[i].endDate + " 18:00:00");
			 Date date7 =  sdf.parse(this.events[i].endDate + " 21:00:00");
			 Date date8 =  sdf.parse(this.events[i].endDate + " 23:59:59");
			 String DateToStr = sdf.format(curDate);
			     //    System.out.println(DateToStr);
			date1.setSeconds(date1.getSeconds() + 1);
			date2.setSeconds(date2.getSeconds() + 1);
			date3.setSeconds(date3.getSeconds() + 1);
			date4.setSeconds(date4.getSeconds() + 1);
			date5.setSeconds(date5.getSeconds() + 1);
			date6.setSeconds(date6.getSeconds() + 1);
			date7.setSeconds(date7.getSeconds() + 1);
			date8.setSeconds(date8.getSeconds() + 1);
			if (curDate.before(date1))
				eventChunksByTime1.add(new Event(this.events[i]));
			else if (curDate.after(date1) && curDate.before(date2))
				eventChunksByTime2.add(new Event(this.events[i]));
			else if (curDate.after(date2) && curDate.before(date3))
				eventChunksByTime3.add(new Event(this.events[i]));
			else if (curDate.after(date3) && curDate.before(date4))
				eventChunksByTime4.add(new Event(this.events[i]));
			else if (curDate.after(date4) && curDate.before(date5))
				eventChunksByTime5.add(new Event(this.events[i]));
			else if (curDate.after(date5) && curDate.before(date6))
				eventChunksByTime6.add(new Event(this.events[i]));
			else if (curDate.after(date6) && curDate.before(date7))
				eventChunksByTime7.add(new Event(this.events[i]));
			else
				eventChunksByTime8.add(new Event(this.events[i]));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
	}
	System.out.println("I am done");
	
	int numberOfOvershoot =0;
	for( i=0; i< eventChunksByTime1.size(); i++){
		if (eventChunksByTime1.get(i).overshoot != null)
			numberOfOvershoot = numberOfOvershoot +1;
	}
	double value = ((double)numberOfOvershoot/(double)100);
	percenatageTemporalByTime1 =  ((double)numberOfOvershoot/(double)this.eventChunksByTime1.size()) * 100.0;
	numberOfOvershoot = 0;
	for ( i=0; i< eventChunksByTime2.size(); i++){
		if (eventChunksByTime2.get(i).overshoot != null)
			numberOfOvershoot++;
	}
	
	percenatageTemporalByTime2 = ((double)numberOfOvershoot/(double)this.eventChunksByTime2.size()) * 100.0;
	numberOfOvershoot = 0;
	
	for ( i=0; i< eventChunksByTime3.size(); i++){
		if (eventChunksByTime3.get(i).overshoot != null)
			numberOfOvershoot++;
	}
	
	percenatageTemporalByTime3 = ((double)numberOfOvershoot/(double)this.eventChunksByTime3.size()) * 100.0;
	numberOfOvershoot = 0;
	
	for ( i=0; i< eventChunksByTime4.size(); i++){
		if (eventChunksByTime4.get(i).overshoot != null)
			numberOfOvershoot++;
	}
	
	percenatageTemporalByTime4 = ((double)numberOfOvershoot/(double)this.eventChunksByTime4.size()) * 100.0;
	numberOfOvershoot = 0;
	
	
	for ( i=0; i< eventChunksByTime5.size(); i++){
		if (eventChunksByTime5.get(i).overshoot != null)
			numberOfOvershoot++;
	}
	
	percenatageTemporalByTime5 = ((double)numberOfOvershoot/(double)this.eventChunksByTime5.size()) * 100.0;
	numberOfOvershoot = 0;
	
	
	for (i=0; i< eventChunksByTime6.size(); i++){
		if (eventChunksByTime6.get(i).overshoot != null)
			numberOfOvershoot++;
	}
	
	percenatageTemporalByTime6 = ((double)numberOfOvershoot/(double)this.eventChunksByTime6.size()) * 100.0;
	numberOfOvershoot = 0;
	for ( i=0; i< eventChunksByTime7.size(); i++){
		if (eventChunksByTime7.get(i).overshoot != null)
			numberOfOvershoot++;
	}
	
	percenatageTemporalByTime7 = ((double)numberOfOvershoot/(double)this.eventChunksByTime7.size()) * 100.0;
	numberOfOvershoot = 0;
	for ( i=0; i< eventChunksByTime8.size(); i++){
		if (eventChunksByTime8.get(i).overshoot != null)
			numberOfOvershoot++;
	}
	
	percenatageTemporalByTime8 = ((double)numberOfOvershoot/(double)this.eventChunksByTime8.size()) * 100.0;
	numberOfOvershoot = 0;
	
	System.out.println(percenatageTemporalByTime1 + " " + percenatageTemporalByTime2 + " " + percenatageTemporalByTime3 + " " +
	" " +percenatageTemporalByTime4 + " " + percenatageTemporalByTime5 + " " + percenatageTemporalByTime6 +
	" " + percenatageTemporalByTime7 + " " + percenatageTemporalByTime8 );
	double numberOfEventsChunk1 =  ((double)eventChunksByTime1.size()/(double)this.events.length) * 100.0;
	double numberOfEventsChunk2 =  ((double)eventChunksByTime2.size()/(double)this.events.length) * 100.0;
	double numberOfEventsChunk3 =  ((double)eventChunksByTime3.size()/(double)this.events.length) * 100.0;
	double numberOfEventsChunk4 =  ((double)eventChunksByTime4.size()/(double)this.events.length) * 100.0;
	double numberOfEventsChunk5 =  ((double)eventChunksByTime5.size()/(double)this.events.length) * 100.0;
	double numberOfEventsChunk6 =  ((double)eventChunksByTime6.size()/(double)this.events.length) * 100.0;
	double numberOfEventsChunk7 =  ((double)eventChunksByTime7.size()/(double)this.events.length) * 100.0;
	double numberOfEventsChunk8 =  ((double)eventChunksByTime8.size()/(double)this.events.length) * 100.0;
	try {
		ExtractTickIndicators.fileWriterTemporalByTime.write(filename + 
				" ," + thresholdString+
				"," +percenatageTemporalByTime1 +
				"," + numberOfEventsChunk1 +
				"," + percenatageTemporalByTime2 +
				"," + numberOfEventsChunk2+
				"," + percenatageTemporalByTime3 +
				"," + numberOfEventsChunk3 +
				"," + percenatageTemporalByTime4 +
				"," + numberOfEventsChunk4 +
				"," + percenatageTemporalByTime5 +
				"," + numberOfEventsChunk5 +
				"," + percenatageTemporalByTime6 +
				"," + numberOfEventsChunk6 +
				"," + percenatageTemporalByTime7 +
				"," + numberOfEventsChunk7 +
				"," + percenatageTemporalByTime8 +
				"," + numberOfEventsChunk8 );
		ExtractTickIndicators.fileWriterTemporalByTime.write("\n");
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
			
	/*for (int i = 0; i < len - x + 1; i += x){
		int portion = len/x;
		Event newArray[] = new Event[portion];
		System.arraycopy(this.events.length, i, newArray, 0, portion);
		eventChunks.add(newArray);
	}*/
	// if (len % x != 0)
	 //    newArray[counter] = Arrays.copyOfRange(this.events.length, len - len % x, len);
	
	}
	
	public void extractFeacturesFromTickData() {
		ArrayList<Event> eventArrayList = new ArrayList<Event>(Arrays.asList(this.events));
		for (int k=0; k<eventArrayList.size()-1; k++){
			
			if (eventArrayList.get(k) == null)
				continue;
			
			String startDate = eventArrayList.get(k).startDate;
			String endDate = eventArrayList.get(k).endDate;
			String startPrice = eventArrayList.get(k).startPrice;
			String endPrice = eventArrayList.get(k).endPrice;
			String startTime = eventArrayList.get(k).startTime;
			String endTime = eventArrayList.get(k).endTime;
			FReader.FileMember2 dcEventStart = new FReader().new FileMember2();
			dcEventStart.Day = startDate.trim();
			dcEventStart.time = startTime.trim();
			dcEventStart.price= startPrice.trim();
			
			FReader.FileMember2 dcEventEnd = new FReader().new FileMember2();
			dcEventEnd.Day = endDate.trim();
			dcEventEnd.time = endTime.trim();
			dcEventEnd.price= endPrice.trim();
			
			int DCEventStartPosition = FReader.tickDataArray.indexOf(dcEventStart);
			int DCEventEndPosition = FReader.tickDataArray.indexOf(dcEventEnd);
			
			if (DCEventEndPosition == -1)
			{
				System.out.println(" Skipping record. Start data " + eventArrayList.get(k).startDate + " " +
						eventArrayList.get(k).startTime + " " +
						eventArrayList.get(k).startPrice);
				continue;
			}
			/*if (DCEventEndPosition == -1)
			{
				String endDCEventString = endDate.trim()+","+endTime.trim()+","+endPrice.trim();
			
					for (int l = 0; l <  FReader.tickDataArray.size(); l++) {
						if(FReader.tickDataArray.get(l) == null)
							continue;
						
						String myDay = FReader.tickDataArray.get(l).Day.trim();
						String myTime = FReader.tickDataArray.get(l).time.trim();
						String myPrice = FReader.tickDataArray.get(l).price.trim();
						String concat = myDay.trim()+","+myTime.trim()+","+myPrice.trim();
						if (l >= 4658676){
							System.out.println(concat + " Is equal ");
						}
		               
						if(concat.compareToIgnoreCase(endDCEventString) == 0){
		                  System.out.println("Is equal");
		               }
						String str = myDay.trim()+","+myTime.trim();
						if (str.compareTo("15.08.2013,02:06:53.717") == 0){
							System.out.println("Row number is " + l);
							System.exit(0);
						}
			               
					}
					System.out.println("XXXX " + dcEventEnd );
				
				System.out.println("XXXX " + dcEventEnd );
			}*/
			System.out.println("tick Data start data " + FReader.tickDataArray.get(DCEventStartPosition).Day + " " +
					FReader.tickDataArray.get(DCEventStartPosition).time + " " +
					FReader.tickDataArray.get(DCEventStartPosition).price);
			
			System.out.println(" Start data " + eventArrayList.get(k).startDate + " " +
					eventArrayList.get(k).startTime + " " +
					eventArrayList.get(k).startPrice);
			
			
			System.out.println("End data " + eventArrayList.get(k).endDate + " " +
					eventArrayList.get(k).endTime + " " +
					eventArrayList.get(k).endPrice);
			
			
			System.out.println(DCEventStartPosition + "  range end" + DCEventEndPosition + " of " + FReader.tickDataArray.size());
			
			
			System.out.println("tick Data end data " + FReader.tickDataArray.get(DCEventEndPosition).Day + " " +
					FReader.tickDataArray.get(DCEventEndPosition).time + " " +
					FReader.tickDataArray.get(DCEventEndPosition).price);
			
			
			eventArrayList.get(k).tickDatapoints = new ArrayList<FReader.FileMember2>(FReader.tickDataArray.subList(DCEventStartPosition, DCEventEndPosition+1));
			/*if (eventArrayList.get(k).hasOverShoot.equalsIgnoreCase("yes")  && events[k].start != events[k].end){
				Event overshootEvent = eventArrayList.get(k).overshoot;
				
				System.out.println(events[k].start + " " + events[k].end);
				startDate = overshootEvent.startDate;
				endDate = overshootEvent.endDate;
				startPrice = overshootEvent.startPrice;
				endPrice = overshootEvent.endPrice;
				startTime = overshootEvent.startTime;
				endTime = overshootEvent.endTime;
				
				dcEventStart = new FReader().new FileMember2();
				dcEventStart.Day = startDate;
				dcEventStart.time = startTime;
				dcEventStart.price= startPrice;
				
				dcEventEnd = new FReader().new FileMember2();
				dcEventEnd.Day = endDate;
				dcEventEnd.time = endTime;
				dcEventEnd.price= endPrice;
				
				DCEventStartPosition = FReader.tickDataArray.indexOf(dcEventStart);
				DCEventEndPosition = FReader.tickDataArray.indexOf(dcEventEnd);
				System.out.println(DCEventStartPosition);
				System.out.println(DCEventStartPosition + " " + DCEventEndPosition+1);
				
				if (DCEventStartPosition == -1)
					System.out.println(DCEventStartPosition);
				eventArrayList.get(k).overshoot.tickDatapoints = new ArrayList<FReader.FileMember2>(FReader.tickDataArray.subList(DCEventStartPosition, DCEventEndPosition+1));
				
			}*/
		}
		//this.events = new Event[eventArrayList.size()];
		//this.events =  eventArrayList.toArray(new Event[eventArrayList.size()]);
		
		for (int k=0; k<eventArrayList.size()-1; k++){
			
			if (eventArrayList.get(k) == null)
				continue;
			
			if ( eventArrayList.get(k).tickDatapoints == null)
				continue;
			
			
			ArrayList<FReader.FileMember2> eventTickData = new ArrayList<FReader.FileMember2>(eventArrayList.get(k).tickDatapoints);
		
			FReader.FileMember2  maxTickObject= Collections.max(eventTickData,new FReader().new FileMemberCompare());
			System.out.println(maxTickObject.Day +" " + maxTickObject.time + " " + maxTickObject.price);
			FReader.FileMember2  minTickObject= Collections.min(eventTickData,new FReader().new FileMemberCompare());
			System.out.println(minTickObject.Day +" " + minTickObject.time + " " + minTickObject.price);
			eventArrayList.get(k).averageNoOfTicksInDC = eventTickData.size();
				//if (eventArrayList.get(k).type ==  Type.Upturn){
					eventArrayList.get(k).highestTick = Double.parseDouble(maxTickObject.price);
					eventArrayList.get(k).lowestTick = Double.parseDouble(minTickObject.price);
					
				//}
			}
	}
	
	public void extractDCfeatures() {
		
		ArrayList<Event> eventArrayList = new ArrayList<Event>(Arrays.asList(this.events));
		for (int k=0; k<eventArrayList.size()-1; ++k){
			
			if (eventArrayList.get(k) == null)
				continue;
			
			if (k== 1400)
				System.out.println("XXXX");
			int start = eventArrayList.get(k).start;
			int end	= eventArrayList.get(k).end;
			eventArrayList.get(k).startDate =  FReader.dataRecordInFileArray.get(start).Day;
			eventArrayList.get(k).endDate = FReader.dataRecordInFileArray.get(end).Day;
			eventArrayList.get(k).startPrice = FReader.dataRecordInFileArray.get(start).price;
			eventArrayList.get(k).endPrice = FReader.dataRecordInFileArray.get(end).price;
			eventArrayList.get(k).startTime = FReader.dataRecordInFileArray.get(start).time;
			eventArrayList.get(k).endTime = FReader.dataRecordInFileArray.get(end).time;
			
			double endPrice =Double.parseDouble(FReader.dataRecordInFileArray.get(end).price);
			double startPrice = Double.parseDouble(FReader.dataRecordInFileArray.get(start).price);
			
			double price =  Math.abs( endPrice - startPrice);
			double percentagePriceDisplacement = (price / endPrice) *100.0;
			DecimalFormat df = new DecimalFormat("#.#################");
			String value = Double.toString(Double.parseDouble(df.format(percentagePriceDisplacement)));
			
			eventArrayList.get(k).percentageDeltaPrice = value;
			
			
			String string[] = eventArrayList.get(k).endDate.split("\\.");
			String endYear = string[2].replaceFirst("^0+(?!$)", "");
			String endMonth = string[1].replaceFirst("^0+(?!$)", "");
			String endDay = string[0].replaceFirst("^0+(?!$)", "");
			Arrays.fill(string, null) ; 
			string =  eventArrayList.get(k).startDate.split("\\.");
			
			String startYear = string[2].replaceFirst("^0+(?!$)", "");
			String startMonth = string[1].replaceFirst("^0+(?!$)", "");
			String startDay = string[0].replaceFirst("^0+(?!$)", "");
			
			Arrays.fill(string, null) ; 
			string =  eventArrayList.get(k).startTime.split(":");
			
			
			String startHour = string[1].replaceFirst("^0+(?!$)", "");
			String startMinute = string[1].replaceFirst("^0+(?!$)", "");
			String startSeconds = string[2].replaceFirst("^0+(?!$)", "");
			
			String startSecondsAndmilliseconds[] = startSeconds.split("\\.");
			
			Arrays.fill(string, null) ; 
			string =  eventArrayList.get(k).endTime.split(":");
			
			
			if (k == 64)
				System.out.println(k + " " +eventArrayList.get(k).endTime) ;
			
			String endHour = string[0].replaceFirst("^0+(?!$)", "");
			String endMinute = string[1].replaceFirst("^0+(?!$)", "");
			String endSeconds = string[2].replaceFirst("^0+(?!$)", "");
			
			String endSecondsAndmilliseconds[] = endSeconds.split("\\.");
			
			
			Calendar endCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			endCalendar.clear();
			
			if (endSecondsAndmilliseconds[0].isEmpty())
				endSecondsAndmilliseconds[0] = "00";
			endCalendar.set(Integer.parseInt(endYear), Integer.parseInt(endMonth)-1, Integer.parseInt(endDay),Integer.parseInt(endHour), Integer.parseInt(endMinute), Integer.parseInt(endSecondsAndmilliseconds[0]));
			if (endSecondsAndmilliseconds.length > 1)
				endCalendar.add(Calendar.MILLISECOND, Integer.parseInt(endSecondsAndmilliseconds[1]) );
			long endSecondsSinceEpoch = endCalendar.getTimeInMillis() / 1000L;
			
			Calendar startCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			startCalendar.clear();
			
			if (startSecondsAndmilliseconds[0].isEmpty())
				startSecondsAndmilliseconds[0] = "00";
			startCalendar.set(Integer.parseInt(startYear), Integer.parseInt(startMonth)-1, Integer.parseInt(startDay),Integer.parseInt(startHour), Integer.parseInt(startMinute), Integer.parseInt(startSecondsAndmilliseconds[0]));
			if (startSecondsAndmilliseconds.length > 1)
				startCalendar.add(Calendar.MILLISECOND, Integer.parseInt(startSecondsAndmilliseconds[1]) );
			long startSecondsSinceEpoch = startCalendar.getTimeInMillis() / 1000L;
			
			double timeDiff =  Math.abs(endSecondsSinceEpoch - startSecondsSinceEpoch) ;
			//double percentageTimeDisplacement = ( timeDiff/endSecondsSinceEpoch) * 100;
			double _10MinsTimeDifference = Math.round(timeDiff/60.0);
			
			if (events[k].overshoot != null )
				eventArrayList.get(k).hasOverShoot = "yes";
			
			if (eventArrayList.get(k).overshoot != null  &&  value == "0.0")
				System.out.println("X");
			
			eventArrayList.get(k).datapoints = new ArrayList<FReader.FileMember2>(FReader.dataRecordInFileArray.subList(events[k].start, events[k].end+1));
			
			String timeValue = Double.toString(Double.parseDouble(df.format(_10MinsTimeDifference/10)));
			
			eventArrayList.get(k).percentageDeltaDuration  = timeValue;
			
			
			//System.out.println((endSecondsSinceEpoch - startSecondsSinceEpoch) + " " + timeValue + " priceValue " + value  );
		//	System.out.println("StartTime " + " " +  events[k].startTime+ " endTime "  +  events[k].endTime);
			//System.out.println(events[k].startDate + " " +events[k].endDate + " " + events[k].startPrice + "" + events[k].endPrice + " " + events[k].DCValue );
		}
		
		this.events = eventArrayList.toArray(new Event[eventArrayList.size()]);
		
	}
	private Event detect(Type type, int[] indexDC, int[] indexOS) {
		// overshoot event must have a start index lower that
		// the DC event start index
		if (indexOS[0] < indexOS[1] && indexOS[0] < indexDC[0]) {
			return new Event(indexOS[0], indexOS[1], type);
		}

		return null;
	}
	
	

	private void adjust(Event last, int[] indexDC, int[] indexOS) {
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

		for (Event e : events) {
			if (index < e.end) {
				break;
			}

			last = e;
		}

		return last;
	}

	public void printFeatures(String filename, String thresholdString,double split) {	
	
		
		File f = new File(filename);
		
		
		String absolutePath =  f.getAbsolutePath();
		System.out.println("File path : " + absolutePath);

		String folderName = absolutePath.substring(0,absolutePath.lastIndexOf(File.separator));
		System.out.println("File path : " + folderName);
		trainingCount = (int) (split * this.events.length);
	
		Writer writer2 = null;
		
		try {
			writer2 = new FileWriter(folderName + "\\CompleteEventFeatures_"+thresholdString+".arff",false);
			writer2.write("@relation overshootTraining");
			// System.out.println("@relation overshootTraining") ;
			writer2.write("\n");
			writer2.write("\n");
			writer2.write("@attribute PriceDifference numeric");
			writer2.write("\n");
			writer2.write("@attribute TimeDifference numeric");
			writer2.write("\n");
			writer2.write("@attribute eventType {Upturn, Downturn}");
			writer2.write("\n");
			writer2.write("@attribute percentageOvershootInChunck numeric");
			writer2.write("\n");
			writer2.write("@attribute percentageDCEventChunck numeric");
			writer2.write("\n");
			writer2.write("@attribute averageNumberOfTicks numeric");
			writer2.write("\n");
			writer2.write("@attribute highestTick numeric");
			writer2.write("\n");
			writer2.write("@attribute lowestTick numeric");
			writer2.write("\n");
			writer2.write("@attribute hasOvershoot{yes,no}");
			writer2.write("\n");
			writer2.write("\n");
			writer2.write("@data");
			writer2.write("\n");
			
			for (int k=0; k<this.events.length; k++){	
				//String feaures = events[k].percentageDeltaPrice +"," + events[k].percentageDeltaDuration + "," + events[k].type +","+ events[k].hasOverShoot ;
				//System.out.println(feaures) ;
				writer2.write(getWekaString(k)  +"\n");
			}
			writer2.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		try {
			writer2 = new FileWriter(folderName + "\\Features_"+thresholdString+"_training.arff",false);
			writer2.write("@relation overshootTraining");
			// System.out.println("@relation overshootTraining") ;
			writer2.write("\n");
			writer2.write("\n");
			writer2.write("@attribute PriceDifference numeric");
			writer2.write("\n");
			writer2.write("@attribute TimeDifference numeric");
			writer2.write("\n");
			writer2.write("@attribute eventType {Upturn, Downturn}");
			writer2.write("\n");
			writer2.write("@attribute percentageOvershootInChunck numeric");
			writer2.write("\n");
			writer2.write("@attribute percentageDCEventChunck numeric");
			writer2.write("\n");
			writer2.write("@attribute averageNumberOfTicks numeric");
			writer2.write("\n");
			writer2.write("@attribute highestTick numeric");
			writer2.write("\n");
			writer2.write("@attribute lowestTick numeric");
			writer2.write("\n");
			writer2.write("@attribute hasOvershoot{yes,no}");
			writer2.write("\n");
			writer2.write("\n");
			writer2.write("@data");
			writer2.write("\n");
			int trainNum=0;
			for (int k=0; k<trainingCount; k++){	
				
				//feaures = events[k].percentageDeltaPrice +"," + events[k].percentageDeltaDuration + "," + events[k].type +","+events[k].hasOverShoot ;
				//System.out.println(feaures) ;
				writer2.write(getWekaString(k) +"\n");
				trainNum = k;
			}
			writer2.close();
			//System.out.println("Number of training set:" + trainNum);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		try {
			writer2 = new FileWriter(folderName + "\\Features_"+thresholdString+"_test.arff",false);
			writer2.write("@relation overshootTraining");
			// System.out.println("@relation overshootTraining") ;
			writer2.write("\n");
			writer2.write("\n");
			writer2.write("@attribute PriceDifference numeric");
			writer2.write("\n");
			writer2.write("@attribute TimeDifference numeric");
			writer2.write("\n");
			writer2.write("@attribute eventType {Upturn, Downturn}");
			writer2.write("\n");
			writer2.write("@attribute percentageOvershootInChunck numeric");
			writer2.write("\n");
			writer2.write("@attribute percentageDCEventChunck numeric");
			writer2.write("\n");
			writer2.write("@attribute averageNumberOfTicks numeric");
			writer2.write("\n");
			writer2.write("@attribute highestTick numeric");
			writer2.write("\n");
			writer2.write("@attribute lowestTick numeric");
			writer2.write("\n");
			writer2.write("@attribute hasOvershoot{yes,no}");
			writer2.write("\n");
			writer2.write("\n");
			writer2.write("@data");
			writer2.write("\n");
			
			for (int k=trainingCount; k<this.events.length; k++){	
				
				//String feaures = events[k].percentageDeltaPrice +"," + events[k].percentageDeltaDuration + "," + events[k].type +","+ events[k].hasOverShoot ;
				//System.out.println(feaures) ;
				writer2.write(getWekaString(k) +"\n");
			}
			writer2.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		try {
			writer2 = new FileWriter(folderName + "\\Features_"+thresholdString+"_test_predict.arff",false);
			writer2.write("@relation overshootTraining");
			// System.out.println("@relation overshootTraining") ;
			writer2.write("\n");
			writer2.write("\n");
			writer2.write("@attribute PriceDifference numeric");
			writer2.write("\n");
			writer2.write("@attribute TimeDifference numeric");
			writer2.write("\n");
			writer2.write("@attribute eventType {Upturn, Downturn}");
			writer2.write("\n");
			writer2.write("@attribute percentageOvershootInChunck numeric");
			writer2.write("\n");
			writer2.write("@attribute percentageDCEventChunck numeric");
			writer2.write("\n");
			writer2.write("@attribute averageNumberOfTicks numeric");
			writer2.write("\n");
			writer2.write("@attribute highestTick numeric");
			writer2.write("\n");
			writer2.write("@attribute lowestTick numeric");
			writer2.write("\n");
			writer2.write("@attribute hasOvershoot{yes,no}");
			writer2.write("\n");
			writer2.write("\n");
			writer2.write("@data");
			writer2.write("\n");
			
			for (int k=trainingCount; k<this.events.length-1; k++){	
				String feaures = "Printed info is: " + events[k].endDate + "," + events[k].endTime + "," + events[k].percentageDeltaPrice +"," + events[k].percentageDeltaDuration + "," + events[k].type +",?"  ;
				System.out.println(feaures) ;
				writer2.write(getWekaString(k) +"\n");
			}
			writer2.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

	/**
	 * Filters an entire set of instances through a filter and returns
	 * the new set. 
	 *
	 * @param data the data to be filtered
	 * @param filter the filter to be used
	 * @return the filtered set of data
	 * @throws Exception if the filter can't be used successfully
	 */
	public static Instances useFilter(Instances data,    Filter filter) throws Exception {
	  /*
	  System.err.println(filter.getClass().getName() 
	                     + " in:" + data.numInstances());
	  */
	  for (int i = 0; i < data.numInstances(); i++) {
	    filter.input(data.instance(i));
	  }
	  filter.batchFinished();
	  Instances newData = filter.getOutputFormat();
	  Instance processed;
	  while ((processed = filter.output()) != null) {
	    newData.add(processed);
	  }

	  /*
	  System.err.println(filter.getClass().getName() 
	                     + " out:" + newData.numInstances());
	  */
	  return newData;
	}
	
	public void classify(String filename, String thresholdString){
		
		File f = new File(filename);
		
		
		
		String absolutePath =  f.getAbsolutePath();
		System.out.println("File path : " + absolutePath);

		String folderName = absolutePath.substring(0,absolutePath.lastIndexOf(File.separator));
		System.out.println("File path : " + folderName);
	
		BufferedReader breader = null;
		
		
		try {
			String trainingDataset = folderName + "\\Features_"+thresholdString+"_training.arff";
			breader = new BufferedReader(new FileReader(trainingDataset));
			train =new Instances(breader);
			train.setClassIndex(train.numAttributes()-1);
			
			
			breader = new BufferedReader(new FileReader(folderName + "\\Features_"+thresholdString+"_test.arff"));
			test =new Instances(breader);
			test.setClassIndex(test.numAttributes()-1);
			
			
			int[]  stats= train.attributeStats(train.classIndex()).nominalCounts;
					  
			System.out.println(stats[0]);
			
			int totalBeforeSmote =0;
			

			double minorityClassBeforeSmote = 0.0;
			double majorityClassBeforeSmote = 0.0;
			
					
			for (int i=0 ; i< train.numInstances(); i++)
			{
				totalBeforeSmote++;
				
				String  instanceClass = train.instance(i).stringValue(train.attribute(train.numAttributes() - 1));
				if (instanceClass.equalsIgnoreCase("yes" ))
					minorityClassBeforeSmote = minorityClassBeforeSmote + 1;
				if (instanceClass.equalsIgnoreCase("no"))
					majorityClassBeforeSmote = majorityClassBeforeSmote + 1;
			}
			
			double PercentageMinorityClassBeforeSmote  = ((minorityClassBeforeSmote/totalBeforeSmote) * 100.0);
			double PercentageMajorityClassBeforeSmote  = ((majorityClassBeforeSmote/totalBeforeSmote) * 100.0);
			
					
			Resample filter = new Resample();
			SMOTE smoteFilter = new SMOTE();
			
			
			
			smoteFilter.setNearestNeighbors(5);
			
			if (PercentageMinorityClassBeforeSmote < 20.0)
				smoteFilter.setPercentage(180.0);
			else
				smoteFilter.setPercentage(100.0);
			smoteFilter.setInputFormat(train);
			
			
			
			
			filter.setBiasToUniformClass(3.0);
			filter.setInputFormat(train);
			filter.setRandomSeed(123);
			filter.setSampleSizePercent(200);
			
			Instances testing = Filter.useFilter(train,smoteFilter);
			System.out.println(train.numInstances() + " " + testing.numInstances());
			
			
			BufferedReader datafile = readDataFile(folderName + "\\CompleteEventFeatures_"+thresholdString+".arff");
			int lines = 0;
			while (datafile.readLine() != null) lines++;
			System.out.println(lines);
			BufferedReader datafile2 = readDataFile(folderName + "\\CompleteEventFeatures_"+thresholdString+".arff");
			data = new Instances(datafile2);
			
			data.setClassIndex(data.numAttributes() - 1);
			
			breader.close();
			String[] options = new String[1];
			options[0] = "-U";
			//J48 tree = new J48();
			//J48 treeNoFilter = new J48();
			//tree.setOptions(options);
			//tree.setUnpruned(true); 
			//tree.buildClassifier(testing);
			currentModel.buildClassifier(testing);
			currentModelNoFilter.buildClassifier(train);
			Evaluation eval = new Evaluation(testing);
			Evaluation evalNoFilter = new Evaluation(train);
			
			Random rand = new Random(3);  // using seed = 1
			int folds = 10;
			//eval.crossValidateModel(currentModel, testing, folds, rand);
			eval.evaluateModel(currentModel, test);
			//evalNoFilter.crossValidateModel(currentModelNoFilter, train,folds,rand);
			evalNoFilter.evaluateModel(currentModelNoFilter, test);
			System.out.println(eval.toClassDetailsString());
			//System.out.println(eval.toMatrixString());
			 System.out.println("Correct to Incorrect" + eval.correct()+"/"+eval.incorrect());
			Instances labeled = new Instances(test);
			
			//ExtractIndicators.fileWriter.write("Filename, threshold, percentage_minority_class, Percentage_majority class, accuracy_Yes, precision_Yes, sensitivity_Yes,specificity_Yes,  recall_Yes,  f-measure_Yes, FP_Yes, TP_Yes, accuracy_No, precision_No, sensitivity_No,specificity_No,  recall_No,  f-measure_No,FP_No, TP_No");
			//int rate = testing.attributeStats(0).nominalCounts[0] ;
			//System.out.println(testing.numAttributes() + "    " + testing.attributeStats(1).nominalCounts[1]  );
			
			double minorityClass = 0.0;
			double majorityClass = 0.0;
			int total=0;
			for (int i=0 ; i< testing.numInstances(); i++)
			{
				total++;
				String  instanceClass = testing.instance(i).stringValue(testing.attribute(testing.numAttributes() - 1));
				if (instanceClass.equalsIgnoreCase("yes" ))
					minorityClass = minorityClass + 1;
				if (instanceClass.equalsIgnoreCase("no"))
					majorityClass = majorityClass + 1;
			}
			
			
			//fileWriter.write(", accuracy_Yes, precision_Yes, sensitivity_Yes,specificity_Yes,  recall_Yes,  f-measure_Yes, FPR_Yes, TPR_Yes, accuracy_No, precision_No, sensitivity_No,specificity_No,  recall_No,  f-measure_No,FPR_No, TPR_No");
			 
			double PercentageMinorityClass  = ((minorityClass/total) * 100.0);
			double PercentageMajorityClass  = ((majorityClass/total) * 100.0);
			
			
			System.out.println(" recall_yes_beforeFilter: "	 	+ evalNoFilter.getClass().getName() );
 			System.out.println(" FileName: "+ filename + 
					" threshold: " + thresholdString+
					" Percentage minority class before filter: " +PercentageMinorityClassBeforeSmote +
					" Percentage minority class after filter: " +PercentageMinorityClass +
					" Percentage majority class before filter: " +PercentageMajorityClassBeforeSmote +
					" Percentage majority class after filter: " +PercentageMajorityClass +
					
					//" sensitivity_Yes_beforFilter: " + (evalNoFilter.numTruePositives(0)/(evalNoFilter.numTruePositives(0) + evalNoFilter.numFalseNegatives(0))) +
					//" sensitivity_Yes_afterFilter: " + (eval.numTruePositives(0)/(eval.numTruePositives(0) + eval.numFalseNegatives(0))) +
					
					//" specificity_yes_beforeFilter: "	+ (evalNoFilter.numTrueNegatives(0)/(evalNoFilter.numFalsePositives(0) + evalNoFilter.numTrueNegatives(0))) +
					//" specificity_yes_afterFilter: " 	+ (eval.numTrueNegatives(0)/(eval.numFalsePositives(0) + eval.numTrueNegatives(0))) +
					
					" recall_yes_beforeFilter: "	 	+ evalNoFilter.recall(0)+
					" recall_yes_afterFilter: "	 		+ eval.recall(0)+
					
					" F-score_yes_beforeFilter: " 		+ evalNoFilter.fMeasure(0) +
					" F-score_yes_afterFilter: " 		+ eval.fMeasure(0) +
					
					//" FPR_yes_beforeFilter: "			+evalNoFilter.falsePositiveRate(0) + 
				//	" FPR_yes_afterFilter: "			+eval.falsePositiveRate(0) + 
					
				//	" TPR_yes_beforeFilter: "			+ evalNoFilter.truePositiveRate(0) +
				//	" TPR_yes_afterFilter: "			+ eval.truePositiveRate(0) +
					
					" Accuracy_yes_beforeFilter: " 					+ ((evalNoFilter.numTruePositives(0)+ evalNoFilter.numTrueNegatives(0))/(evalNoFilter.numFalseNegatives(0)+ evalNoFilter.numFalsePositives(0)+ evalNoFilter.numTruePositives(0)+ evalNoFilter.numTrueNegatives(0))) +
					" Accuracy_yes_afterFilter: " 					+ ((eval.numTruePositives(0)+ eval.numTrueNegatives(0))/(eval.numFalseNegatives(0)+ eval.numFalsePositives(0)+ eval.numTruePositives(0)+ eval.numTrueNegatives(0))) +
					
					" precision_yes_beforeFilter: " 	+ (evalNoFilter.numTruePositives(0)/ (evalNoFilter.numTruePositives(0) + evalNoFilter.numFalsePositives(0))) +
					" precision_yes_afterFilter: " 					+ (eval.numTruePositives(0)/ (eval.numTruePositives(0) + eval.numFalsePositives(0))) +
					
					
					//" sensitivity_no_beforeFilter: " 	+ (evalNoFilter.numTruePositives(1)/(evalNoFilter.numTruePositives(1) + evalNoFilter.numFalseNegatives(1))) +
					//" sensitivity_no_afterFilter: " 	+ (eval.numTruePositives(1)/(eval.numTruePositives(1) + eval.numFalseNegatives(1))) +
					
					//" specificity_no_beforeFilter: " 	+ (evalNoFilter.numTrueNegatives(1)/(evalNoFilter.numFalsePositives(1) + evalNoFilter.numTrueNegatives(1))) +
					//" specificity_no_afterFilter: " 	+ (eval.numTrueNegatives(1)/(eval.numFalsePositives(1) + eval.numTrueNegatives(1))) +
					
					" recall_no_beforeFilter: "	 		+ evalNoFilter.recall(1)+	 
					" recall_no_afterFilter: "	 		+ eval.recall(1)+	 
					
					" F-score_no_beforeFilter: " 		+ evalNoFilter.fMeasure(1) +
					" F-score_no_afterFilter: "			+ eval.fMeasure(1) +
					
		//			" FPR_no_beforeFilter: "			+ evalNoFilter.falsePositiveRate(1) +
		//			" FPR_no_afterFilter: "				+ eval.falsePositiveRate(1) +
					
		//			" TPR_no_beforeFilter:  "			+ evalNoFilter.truePositiveRate(1) +
		//			" TPR_no_afterFilter:  "			+ eval.truePositiveRate(1) +
					
					" Accuracy_no_beforeFilter: "		+ ((evalNoFilter.numTruePositives(1)+ evalNoFilter.numTrueNegatives(1))/(evalNoFilter.numFalseNegatives(1)+ evalNoFilter.numFalsePositives(1)+ evalNoFilter.numTruePositives(1)+ evalNoFilter.numTrueNegatives(1))) +
					" Accuracy_no_afterFilter: "		+ ((eval.numTruePositives(1)+ eval.numTrueNegatives(1))/(eval.numFalseNegatives(1)+ eval.numFalsePositives(1)+ eval.numTruePositives(1)+ eval.numTrueNegatives(1))) +
					
					
					" precision_no_beforeFilter: "		+ (evalNoFilter.numTruePositives(1)/ (evalNoFilter.numTruePositives(1) + evalNoFilter.numFalsePositives(1)))+
 					" precision_no_afterFilter: "		+ (eval.numTruePositives(1)/ (eval.numTruePositives(1) + eval.numFalsePositives(1))) +
 					" RMSE  beforeFilter:"    +  						evalNoFilter.rootMeanSquaredError()  +
 					" RMSE  afterFilter:"    +  						eval.rootMeanSquaredError());
 			
 			System.out.println(evalNoFilter.pctIncorrect());
 			System.out.println(evalNoFilter.pctCorrect());
 			
 			System.out.println(eval.pctCorrect());
 			double pricisionDouble= 0.0;
 			
 			if (evalNoFilter.numTruePositives(0) != 0.0)
 				pricisionDouble = ((evalNoFilter.numTruePositives(0)/ (evalNoFilter.numTruePositives(0) + evalNoFilter.numFalsePositives(0))) == Double.NaN) ? 0.0 : (evalNoFilter.numTruePositives(0)/ (evalNoFilter.numTruePositives(0) + evalNoFilter.numFalsePositives(0))); 
 			else
 				pricisionDouble = 0.0;
 			if(Double.isNaN(pricisionDouble))
 				System.out.println("XXX" + " " + evalNoFilter.numTruePositives(0));
 			
 			ExtractTickIndicators.fileWriter.write(filename + 
					" ," + thresholdString+
					"," +PercentageMinorityClassBeforeSmote +
					", " +PercentageMinorityClass +
					", " +PercentageMajorityClassBeforeSmote +
					"," +PercentageMajorityClass +
					
					//" sensitivity_Yes_beforFilter: " + (evalNoFilter.numTruePositives(0)/(evalNoFilter.numTruePositives(0) + evalNoFilter.numFalseNegatives(0))) +
					//" sensitivity_Yes_afterFilter: " + (eval.numTruePositives(0)/(eval.numTruePositives(0) + eval.numFalseNegatives(0))) +
					
					//" specificity_yes_beforeFilter: "	+ (evalNoFilter.numTrueNegatives(0)/(evalNoFilter.numFalsePositives(0) + evalNoFilter.numTrueNegatives(0))) +
					//" specificity_yes_afterFilter: " 	+ (eval.numTrueNegatives(0)/(eval.numFalsePositives(0) + eval.numTrueNegatives(0))) +
					
					" , "	 	+ evalNoFilter.recall(0)+
					" , "	 		+ eval.recall(0)+
					
					" , " 		+ evalNoFilter.fMeasure(0) +
					" , " 		+ eval.fMeasure(0) +
					
		//			" , "			+evalNoFilter.falsePositiveRate(0) + 
		//			" , "			+eval.falsePositiveRate(0) + 
					
		//			" , "			+ evalNoFilter.truePositiveRate(0) +
		//			" , "			+ eval.truePositiveRate(0) +
					
					" , " 			+ ((evalNoFilter.numTruePositives(0)+ evalNoFilter.numTrueNegatives(0))/(evalNoFilter.numFalseNegatives(0)+ evalNoFilter.numFalsePositives(0)+ evalNoFilter.numTruePositives(0)+ evalNoFilter.numTrueNegatives(0))) +
					" , " 			+ ((eval.numTruePositives(0)+ eval.numTrueNegatives(0))/(eval.numFalseNegatives(0)+ eval.numFalsePositives(0)+ eval.numTruePositives(0)+ eval.numTrueNegatives(0))) +
					
					" , " 			+ evalNoFilter.precision(0) +//+ pricisionDouble +
					" , " 			+ eval.precision(0) + //		+ ((eval.numTruePositives(0) != 0.0) ?(eval.numTruePositives(0)/ (eval.numTruePositives(0) + eval.numFalsePositives(0))) : 0.0)+
					
					
					//" sensitivity_no_beforeFilter: " 	+ (evalNoFilter.numTruePositives(1)/(evalNoFilter.numTruePositives(1) + evalNoFilter.numFalseNegatives(1))) +
					//" sensitivity_no_afterFilter: " 	+ (eval.numTruePositives(1)/(eval.numTruePositives(1) + eval.numFalseNegatives(1))) +
					
					//" specificity_no_beforeFilter: " 	+ (evalNoFilter.numTrueNegatives(1)/(evalNoFilter.numFalsePositives(1) + evalNoFilter.numTrueNegatives(1))) +
					//" specificity_no_afterFilter: " 	+ (eval.numTrueNegatives(1)/(eval.numFalsePositives(1) + eval.numTrueNegatives(1))) +
					
					" , "	 		+ evalNoFilter.recall(1)+	 
					" , "	 		+ eval.recall(1)+	 
					
					" , " 		+ evalNoFilter.fMeasure(1) +
					" , "			+ eval.fMeasure(1) +
					
//					" , "			+ evalNoFilter.falsePositiveRate(1) +
//					" , "				+ eval.falsePositiveRate(1) +
					
//					" ,  "			+ evalNoFilter.truePositiveRate(1) +
//					" ,  "			+ eval.truePositiveRate(1) +
					
					" , "		+ ((evalNoFilter.numTruePositives(1)+ evalNoFilter.numTrueNegatives(1))/(evalNoFilter.numFalseNegatives(1)+ evalNoFilter.numFalsePositives(0)+ evalNoFilter.numTruePositives(1)+ evalNoFilter.numTrueNegatives(1))) +
					" , "		+ ((eval.numTruePositives(1)+ eval.numTrueNegatives(1))/(eval.numFalseNegatives(1)+ eval.numFalsePositives(1)+ eval.numTruePositives(1)+ eval.numTrueNegatives(1))) +
					
					
					" , "		+ evalNoFilter.precision(1) +// ((evalNoFilter.numTruePositives(1) != 0.0)? (evalNoFilter.numTruePositives(1)/ (evalNoFilter.numTruePositives(1) + evalNoFilter.numFalsePositives(1))) : 0.0 )+
 					" , "		+ eval.precision(1) +// ((eval.numTruePositives(1) != 0.0)?(eval.numTruePositives(1)/ (eval.numTruePositives(1) + eval.numFalsePositives(1))): 0.0)+
 					" , "    +  						evalNoFilter.rootMeanSquaredError()  +
 					" , "    +  						eval.rootMeanSquaredError());
 			
 			ExtractTickIndicators.fileWriter.write("\n");
			// eval.evaluateModel(tree, test);
			 System.out.println(eval.toSummaryString());
			 
			 
			 System.out.println(eval.toMatrixString());
			
			for (int i = 0; i< test.numInstances(); i++){
				double clsLabel = currentModel.classifyInstance(test.instance(i));
				labeled.instance(i).setClassValue(clsLabel);
				//System.out.println("prediction "+labeled.instance(i).stringValue(labeled.attribute(labeled.numAttributes() - 1)));
			}
			
			
			System.out.println("FPR:"+eval.falsePositiveRate(0) + " FPR:" + eval.falsePositiveRate(1) +
					" TPR:"+ eval.truePositiveRate(0) + " TPR:" + eval.truePositiveRate(1) +
					" " + eval.fMeasure(0) + " F-score:" + eval.fMeasure(1));
			
			BufferedWriter outcomeWriter =  new BufferedWriter(
					new FileWriter(folderName + "\\Features_"+thresholdString+"_TestAfterLabelling.arff"));
			outcomeWriter.write(labeled.toString());
			outcomeWriter.close();
			datafile2.close();
			datafile.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		;
		
	/*
 
		// Do 10-split cross validation
		Instances[][] split = crossValidationSplit(data, 10);
 
		// Separate split into training and testing arrays
		Instances[] trainingSplits = split[0];
		Instances[] testingSplits = split[1];
 
		// Use a set of classifiers
		Classifier[] models = { 
				new J48(), // a decision tree
				//new PART(), 
				//new DecisionTable(),//decision table majority classifier
				//new DecisionStump(), //one-level decision tree
				new JRip(),
				new Logistic()
		};
 
		// Run for each model
		for (int j = 0; j < models.length; j++) {
 
			// Collect every group of predictions for current model in a FastVector
			ArrayList<Prediction> predictions = new ArrayList<Prediction>();
 
			// For each training-testing split pair, train and test the classifier
			for (int i = 0; i < trainingSplits.length; i++) {
				Evaluation validation = null;
				try {
					validation = classify(models[j], trainingSplits[i], testingSplits[i]);
					System.out.println(validation.toMatrixString());
					 System.out.println(" error rate "+validation.errorRate());
					 System.out.println(" Fmeasure 1 "+validation.fMeasure(1));
					 System.out.println(" Fmeasure 2 "+validation.fMeasure(0));
					 System.out.println(" Precision 1 "+validation.precision(1));
					 System.out.println(" recall 2 "+validation.recall(1));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				predictions.addAll(validation.predictions());
				//System.out.println(validation.toClassDetailsString());
				// Uncomment to see the summary for each training-testing pair.
				System.out.println(models[j].toString());
			}
			
			try {
				Instances labeled = new Instances(test);
				for (int k = 0; k< test.numInstances(); k++){
					double clsLabel = models[j].classifyInstance(test.instance(k));
					labeled.instance(k).setClassValue(clsLabel);
				}
				
				BufferedWriter outcomeWriter =  new BufferedWriter(
						new FileWriter(folderName + "\\Features_"+thresholdString+"_TestAfterLabelling.arff"));
				outcomeWriter.write(labeled.toString());
				outcomeWriter.close();
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
 
			// Calculate overall accuracy of current classifier on all splits
			double accuracy = calculateAccuracy(predictions);
 
			// Print current classifier's name and accuracy in a complicated,
			// but nice-looking way.
			System.out.println("Accuracy of " + models[j].getClass().getSimpleName() + ": "
					+ String.format("%.2f%%", accuracy)
					+ "\n---------------------------------");
		}
 */
		
	}
	
	
	public static Evaluation classify(Classifier model,
			Instances trainingSet, Instances testingSet) throws Exception {
		Evaluation evaluation = new Evaluation(trainingSet);
 
		model.buildClassifier(trainingSet);
		evaluation.evaluateModel(model, testingSet);
 
		return evaluation;
	}
 
	public static double calculateAccuracy(ArrayList<Prediction> predictions) {
		double correct = 0;
 
		for (int i = 0; i < predictions.size(); i++) {
			NominalPrediction np = (NominalPrediction) predictions.get(i);
			if (np.predicted() == np.actual()) {
				correct++;
			}
		}
 
		return 100 * correct / predictions.size();
	}
 
	public static Instances[][] crossValidationSplit(Instances data, int numberOfFolds) {
		Instances[][] split = new Instances[2][numberOfFolds];
 
		for (int i = 0; i < numberOfFolds; i++) {
			split[0][i] = data.trainCV(numberOfFolds, i);
			split[1][i] = data.testCV(numberOfFolds, i);
		}
 
		return split;
	}
	
	public static BufferedReader readDataFile(String filename) {
		BufferedReader inputReader = null;
 
		try {
			inputReader = new BufferedReader(new FileReader(filename));
		} catch (FileNotFoundException ex) {
			System.err.println("File not found: " + filename);
		}
 
		return inputReader;
	}

	public void deleteTemporaryFiles(String filename, String thresholdString2) {
		File f = new File(filename);
		
		
		String absolutePath =  f.getAbsolutePath();
		System.out.println("File path : " + absolutePath);

		String folderName = absolutePath.substring(0,absolutePath.lastIndexOf(File.separator));
		System.out.println("File path : " + folderName);
		
	
		
		
			
			File file = new File(folderName + "\\CompleteEventFeatures_"+thresholdString+".arff");
	         
	        if(file.delete())
	        {
	            System.out.println("File deleted successfully");
	        }
	        else
	        {
	            System.out.println("Failed to delete the file");
	        }
	        
	        
	        File file2 = new File(folderName + "\\Features_"+thresholdString+"_training.arff");
	         
	        if(file2.delete())
	        {
	            System.out.println("File2 deleted successfully");
	        }
	        else
	        {
	            System.out.println("Failed to delete the file2");
	        }
	        
	        
	        File file3 = new File(folderName + "\\Features_"+thresholdString+"_test.arff");
	         
	        if(file3.delete())
	        {
	            System.out.println("File3 deleted successfully");
	        }
	        else
	        {
	            System.out.println("Failed to delete the file3");
	        }
	}

	String getWekaString(int k){
		String features;

			 Date curDate = null;
			 Date date1 = null;
			 Date date2 = null;
			 Date date3 = null;
			 Date date4 = null;
			 Date date5 = null;
			 Date date6 = null;
			 Date date7 = null;
			 Date date8 = null;
			try {
				curDate = sdf.parse(this.events[k].endDate + " "+ this.events[k].endTime);
				date1 =  sdf.parse(this.events[k].endDate + " 03:00:00");
				 date2 =  sdf.parse(this.events[k].endDate + " 06:00:00");
				 date3 =  sdf.parse(this.events[k].endDate + " 09:00:00");
				 date4 =  sdf.parse(this.events[k].endDate + " 12:00:00");
				 date5 =  sdf.parse(this.events[k].endDate + " 15:00:00");
				 date6 =  sdf.parse(this.events[k].endDate + " 18:00:00");
				 date7 =  sdf.parse(this.events[k].endDate + " 21:00:00");
				 date8 =  sdf.parse(this.events[k].endDate + " 23:59:59");
			} catch (ParseException e) {
				 System.err.println("An exception was thrown. Event record" + k );
				 return "";
				//e.printStackTrace();
			}
			 
			 String DateToStr = sdf.format(curDate);
			         System.out.println(curDate);
			date1.setSeconds(date1.getSeconds() + 1);
			date2.setSeconds(date2.getSeconds() + 1);
			date3.setSeconds(date3.getSeconds() + 1);
			date4.setSeconds(date4.getSeconds() + 1);
			date5.setSeconds(date5.getSeconds() + 1);
			date6.setSeconds(date6.getSeconds() + 1);
			date7.setSeconds(date7.getSeconds() + 1);
			date8.setSeconds(date8.getSeconds() + 1);
			if (curDate.before(date1)){
				features = events[k].percentageDeltaPrice +"," + events[k].percentageDeltaDuration + "," + events[k].type +","  +  percenatageTemporalByTime1+","+ (((double)eventChunksByTime1.size()/(double)this.events.length) * 100.0)+ ","+events[k].averageNoOfTicksInDC +"," +  events[k].highestTick +"," + events[k].lowestTick + ","+events[k].hasOverShoot ;
			}
			else if (curDate.after(date1) && curDate.before(date2))
				features = events[k].percentageDeltaPrice +"," + events[k].percentageDeltaDuration + "," + events[k].type +"," +  percenatageTemporalByTime2+","+ (((double)eventChunksByTime2.size()/(double)this.events.length) * 100.0)+ ","+  events[k].averageNoOfTicksInDC +"," +  events[k].highestTick +"," + events[k].lowestTick + ","+events[k].hasOverShoot ;
			else if (curDate.after(date2) && curDate.before(date3))
				features = events[k].percentageDeltaPrice +"," + events[k].percentageDeltaDuration + "," + events[k].type +"," +  percenatageTemporalByTime3+","+ (((double)eventChunksByTime3.size()/(double)this.events.length) * 100.0)+ ","+ events[k].averageNoOfTicksInDC +"," +  events[k].highestTick +"," + events[k].lowestTick + ","+events[k].hasOverShoot ;
			else if (curDate.after(date3) && curDate.before(date4))
				features = events[k].percentageDeltaPrice +"," + events[k].percentageDeltaDuration + "," + events[k].type +"," + percenatageTemporalByTime4+","+ (((double)eventChunksByTime4.size()/(double)this.events.length) * 100.0)+ ","+  events[k].averageNoOfTicksInDC +"," +  events[k].highestTick +"," + events[k].lowestTick + ","+events[k].hasOverShoot ;
			else if (curDate.after(date4) && curDate.before(date5))
				features = events[k].percentageDeltaPrice +"," + events[k].percentageDeltaDuration + "," + events[k].type +"," + percenatageTemporalByTime5+","+ (((double)eventChunksByTime5.size()/(double)this.events.length) * 100.0)+ ","+ events[k].averageNoOfTicksInDC +"," + events[k].highestTick +"," + events[k].lowestTick + ","+events[k].hasOverShoot ;
			else if (curDate.after(date5) && curDate.before(date6))
				features = events[k].percentageDeltaPrice +"," + events[k].percentageDeltaDuration + "," + events[k].type +"," + percenatageTemporalByTime6+","+ (((double)eventChunksByTime6.size()/(double)this.events.length) * 100.0)+ ","+  events[k].averageNoOfTicksInDC +"," +  events[k].highestTick +"," + events[k].lowestTick + ","+events[k].hasOverShoot ;
			else if (curDate.after(date6) && curDate.before(date7))
				features = events[k].percentageDeltaPrice +"," + events[k].percentageDeltaDuration + "," + events[k].type +"," + percenatageTemporalByTime7+","+ (((double)eventChunksByTime7.size()/(double)this.events.length) * 100.0)+ ","+ events[k].averageNoOfTicksInDC +"," + events[k].highestTick +"," + events[k].lowestTick + ","+events[k].hasOverShoot ;
			else
				features = events[k].percentageDeltaPrice +"," + events[k].percentageDeltaDuration + "," + events[k].type +"," + percenatageTemporalByTime8+","+ (((double)eventChunksByTime7.size()/(double)this.events.length) * 100.0)+ ","+  events[k].averageNoOfTicksInDC +"," +  events[k].highestTick +"," + events[k].lowestTick + ","+events[k].hasOverShoot ;
		
			System.out.print(features);
				return features;
	}
}
