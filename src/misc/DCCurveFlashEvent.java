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

public class DCCurveFlashEvent {
	Event[] events;
	
	Event[] output;
	


	String thresholdString = "";
	
	SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
	static public ArrayList<String []> eventFeatures = new ArrayList<String []>();
	
	
	int trainingCount =-1;
	
	int flashEventCount =0;
	int flashEventWithOvershoot =0;
	int flashEventWithoutOverShoot=0;
	int numberOfOverShoot=0;
	static String flashEventString = "";
	
	public DCCurveFlashEvent() {
		
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

	public void findFlashEvent() {
		
		flashEventString = flashEventString+thresholdString + "," ;
		
		for (int k=0; k<events.length-1; k++){
			int start = events[k].start;
			int end	= events[k].end;
			
			this.events[k].startDate =  FReader.dataRecordInFileArray.get(start).Day;
			this.events[k].endDate = FReader.dataRecordInFileArray.get(end).Day;
			this.events[k].startPrice = FReader.dataRecordInFileArray.get(start).price;
			this.events[k].endPrice = FReader.dataRecordInFileArray.get(end).price;
			this.events[k].startTime = FReader.dataRecordInFileArray.get(start).time;
			this.events[k].endTime = FReader.dataRecordInFileArray.get(end).time;
			
			double endPrice =Double.parseDouble(FReader.dataRecordInFileArray.get(end).price);
			double startPrice = Double.parseDouble(FReader.dataRecordInFileArray.get(start).price);
			
			
			if (start == end){
				
				flashEventCount = flashEventCount +1;
				if (this.events[k].overshoot != null && this.events[k].overshoot.length() > 0){
					flashEventWithOvershoot =  flashEventWithOvershoot + 1;
					System.out.println(this.events[k].overshoot.start + ","  + this.events[k].overshoot.end + " " + " Start datetime: "+this.events[k].startDate+"-"+this.events[k].startTime   + " End datetime: " + this.events[k].endDate+"-"+this.events[k].endTime );
				}
				else
					flashEventWithoutOverShoot = flashEventWithoutOverShoot + 1;
					
				//System.out.println("Start datetime: "+this.events[k].startDate+"-"+this.events[k].startTime   + " End datetime: " + this.events[k].endDate+"-"+this.events[k].endTime );
			}
			if (this.events[k].overshoot != null && this.events[k].overshoot.length() > 0){
				numberOfOverShoot =  numberOfOverShoot + 1;
			}
		}
		
		System.out.println("number of flash "+ flashEventCount);
		flashEventString =  flashEventString +flashEventCount + "," +flashEventWithOvershoot + ","+ flashEventWithoutOverShoot+ ",";
		try {
			double percentageOfflashEventCount = ((double)flashEventCount)/((double)this.events.length) * 100.0;
			double percentaFlashEventWithOvershoot = ((double)flashEventWithOvershoot)/((double)flashEventCount) * 100.0;
			double percentaFlashEventWithoutOvershoot = ((double)flashEventWithoutOverShoot)/((double)flashEventCount) * 100.0;
			double percentageNumberOfOverShoot =  ((double)numberOfOverShoot)/((double)this.events.length) * 100.0;
			double percentageFlashEventWithOvershootInAllEvent = ((double)flashEventWithOvershoot)/((double)this.events.length) * 100.0;
			FlashEvent.fileWriter.write(thresholdString + ","+percentageOfflashEventCount + "," +percentaFlashEventWithOvershoot + ","+ percentaFlashEventWithoutOvershoot+ "," + percentageFlashEventWithOvershootInAllEvent+ ","+percentageNumberOfOverShoot+",");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Data "+ flashEventString);
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
}
