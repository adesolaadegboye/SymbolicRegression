package dc.ga;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import dc.GP.AbstractNode;
import dc.GP.Const;
import dc.ga.DCCurve.Event;
import dc.ga.DCCurve.Type;
import dc.io.FReader;
import misc.DCEventGenerator;

public class HelperClass {

	public static boolean  updateClassifierWithTestData(double threshold, Double[] testData, DCCurve dcurve){
		
		
		DCEventGenerator dCEventGenerator = new DCEventGenerator();
		dCEventGenerator.generateEvents(testData, threshold);

		Event[] copiedTestArray = Arrays.copyOf(dCEventGenerator.getEvents(),
				dCEventGenerator.getEvents().length);

		

		dcurve.preprocess.lastTestingEvent = copiedTestArray[copiedTestArray.length - 1];

		dcurve.preprocess.buildTest(copiedTestArray);
		System.out.println("About to print test data for threshold " + threshold);
		if(false == dcurve.preprocess.processTestData(copiedTestArray))
			return false;

		dcurve.preprocess.loadTestData(copiedTestArray);

		dcurve.preprocess.classifyTestData();
		
		return true;
	}

	public static void  cleanUpClassificationTempFile(PreProcess preprocess, String filename){

		// cleanup
		String tempFolderName = preprocess.tempFilePath.get(0).substring(0,
				preprocess.tempFilePath.get(0).lastIndexOf(File.separator));

		File dir = new File(tempFolderName);
		if (!dir.isDirectory())
			return;

		String fileNames = filename.substring(filename.lastIndexOf(File.separator) + 1, filename.length() - 4);

		File[] tempFile = dir.getParentFile().listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith("autoweka" + fileNames);
			}
		});

		for (int tempFileCount = 0; tempFileCount < tempFile.length; tempFileCount++) {
			try {
				preprocess.deleteDirectoryRecursionJava6(tempFile[tempFileCount]);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("Unable to delete one of the directory");
			}
		}

		
	}
	
	
	public static PreProcess  getClassifier(double threshold, String filename, Double [] training, Const.function_code regressionAlgo){
		
		
			String thresholdStr = String.format("%.8f", threshold);
			
			DCEventGenerator dCEventGenerator = new DCEventGenerator();
			dCEventGenerator.generateEvents(training, threshold);
			Event[] copiedArray;
			//Here We are using event to generate the model but while testing we use output
			copiedArray = Arrays.copyOf(dCEventGenerator.getEvents(), dCEventGenerator.getEvents().length);
			Map<String, Event[]> trainingEventsArray = new HashMap<String, Event[]>();
			
			trainingEventsArray.put(thresholdStr, copiedArray);
			
			
			
			PreProcess preprocess = new PreProcess(threshold, filename, Const.hashFunctionTypeToString(regressionAlgo));

			preprocess.buildTraining(copiedArray);
			preprocess.selectBestClassifier(); //This is manual selection

			// loadTrainingData load classification data "order is
			// important"
			preprocess.loadTrainingData(copiedArray);
			
			preprocess.lastTrainingEvent = copiedArray[copiedArray.length - 1];

			if (!preprocess.runAutoWeka())
				return null;
			
			

		
		return preprocess;
	}


	
	/* dead code was used for GA_newest which has now been deleted
	public static Map<Double, DCCurve> getBestThresholdsByRMSE( String filename,int arraySize, Double[] dataset, int numberOfCandidateThreshold, double startingThreshold,
			double interval){
		double [] bestThresholdArray = new double[arraySize];
		double [] THRESHOLDS = new double[numberOfCandidateThreshold];
		
		Map<Double, DCCurve> thresholdMap = new HashMap<Double, DCCurve>();
	
		DCCurve[] perfectForesightDCCurve;
		perfectForesightDCCurve = new PerfectForecastDCCurve[THRESHOLDS.length];
		Map<Double, Double> perfectForecastReturnMap = new HashMap<Double, Double>();
		ConcurrentHashMap<Integer, String> gpDaysMap = null;
		
		try {
			gpDaysMap = FReader.loadDataMap(filename);
		} catch (FileNotFoundException e) {
			System.out.println("File not found exception, HelperClass:getBestThresholds");
			System.exit(-1);
		} catch (IOException e) {
			System.out.println("IO exception, HelperClass:getBestThresholds");
			System.exit(-1);
		}
		for (int i = 0; i < THRESHOLDS.length; i++) {
			THRESHOLDS[i] = (startingThreshold + (interval * i)) / 100.0;
		
		}
		
		for (int i = 0; i < THRESHOLDS.length; i++) {
			DCEventGenerator dCEventGenerator = new DCEventGenerator();
			perfectForesightDCCurve[i] = new PerfectForecastDCCurve();
			ArrayList<String> arrayList = new ArrayList<String>();
			
			THRESHOLDS[i] = (startingThreshold + (interval * i)) / 100.0;
			String thresholdStr = String.format("%.8f", THRESHOLDS[i]);
			String gpFileNamePrefix = gpDaysMap.get(0); //Just get first element in collection
			String gpFileName = gpFileNamePrefix + "_" + thresholdStr + ".txt";

			dCEventGenerator.generateEvents(dataset, THRESHOLDS[i]);
			perfectForesightDCCurve[i].trainingEvents = Arrays.copyOf(dCEventGenerator.getEvents(), dCEventGenerator.getEvents().length);
				
			perfectForesightDCCurve[i].filename = filename;
			perfectForesightDCCurve[i].build(dataset, THRESHOLDS[i], gpFileName, perfectForesightDCCurve[i].trainingEvents , null);
			
			
				perfectForesightDCCurve[i].estimateRMSEs(perfectForesightDCCurve[i].trainingEvents );  // get upward ratio only
				perfectForecastReturnMap.put(THRESHOLDS[i], perfectForesightDCCurve[i].upwardRMSE);
				arrayList.add(String.valueOf(perfectForesightDCCurve[i].upwardRMSE));
			
			
			thresholdMap.put(THRESHOLDS[i], perfectForesightDCCurve[i]);
			
		}
		
		
		List<Entry<Double, Double>> least = findLeast(perfectForecastReturnMap, Const.NUMBER_OF_SELECTED_THRESHOLDS); // best
		// 5
		// thresholds
		
		int tradingThresholdCount =0;
		for (Entry<Double, Double> entry : least) {
			// System.out.println(entry);
			bestThresholdArray[tradingThresholdCount] = entry.getKey();
			
			tradingThresholdCount++;
		}
		Map<Double, DCCurve> selectedThresholdGPMap = new HashMap<Double, DCCurve>();
		for (int k =0; k< bestThresholdArray.length; k++){
			
			selectedThresholdGPMap.put(bestThresholdArray[k], thresholdMap.get(bestThresholdArray[k]));
		}
		
		
		return selectedThresholdGPMap;
	}*/
	public static <K, V extends Comparable<? super V>> List<Entry<K, V>> findGreatest(Map<K, V> map, int n) {
		Comparator<? super Entry<K, V>> comparator = new Comparator<Entry<K, V>>() {
			@Override
			public int compare(Entry<K, V> e0, Entry<K, V> e1) {
				V v0 = e0.getValue();
				V v1 = e1.getValue();
				return v0.compareTo(v1);
			}
		};
		PriorityQueue<Entry<K, V>> highest = new PriorityQueue<Entry<K, V>>(n, comparator);
		for (Entry<K, V> entry : map.entrySet()) {
			highest.offer(entry);
			while (highest.size() > n) {
				highest.poll();
			}
		}

		List<Entry<K, V>> result = new ArrayList<Map.Entry<K, V>>();
		while (highest.size() > 0) {
			result.add(highest.poll());
		}
		return result;
	}

	// Generic function to find the index of an element in an object array in
	// Java
	public static <T> int find(T[] a, T target) {
		return IntStream.range(0, a.length).filter(i -> target.equals(a[i])).findFirst().orElse(-1); // return
																										// -1
																										// if
																										// target
																										// is
																										// not
																										// found
	}

	// Function to find the index of an element in a primitive array in Java
	public static int find(double[] a, double target) {
		return IntStream.range(0, a.length).filter(i -> target == a[i]).findFirst().orElse(-1); // return
																								// -1
																								// if
																								// target
																								// is
																								// not
																								// found
	}
	
	public static <K, V extends Comparable<? super V>> List<Entry<K, V>> findLeast(Map<K, V> map, int n) {
		Comparator<? super Entry<K, V>> comparator = new Comparator<Entry<K, V>>() {
			@Override
			public int compare(Entry<K, V> e0, Entry<K, V> e1) {
				V v0 = e0.getValue();
				V v1 = e1.getValue();
				return (v0.compareTo(v1) * -1);
			}
		};
		PriorityQueue<Entry<K, V>> least = new PriorityQueue<Entry<K, V>>(n, comparator);
		for (Entry<K, V> entry : map.entrySet()) {
			least.offer(entry);
			while (least.size() > n) {
				least.poll();
			}
		}

		List<Entry<K, V>> result = new ArrayList<Map.Entry<K, V>>();
		while (least.size() > 0) {
			result.add(least.poll());
		}
		return result;
	}
	public  static boolean isPreviousDirectionalChangeEventSameAsCurrentDCEvent(Event[] output, int pointInTime){
	
		Event previousEvent = null;
		Event currentEvent =  null;
		try{
			
			currentEvent = output[pointInTime];
			previousEvent = output[pointInTime-1];
		}
		catch(ArrayIndexOutOfBoundsException e){
			return false;
		}	
		
		if (previousEvent.type !=currentEvent.type )
			return false;
		else 
			return true;
		
	}
	public  static boolean isNextDirectionalChangeEventSameDirectionButDifferentLength(Event[] output, int pointInTime){
		Event nextEvent = null;
		Event currentEvent =  null;
		try{
			
			currentEvent = output[pointInTime];
		}
		catch(ArrayIndexOutOfBoundsException e){
			return false;
		}	
		
		try{
			for (int i = pointInTime+1; i < output.length; i++ ){
				nextEvent = output[i] ;
				if (nextEvent.type !=currentEvent.type )
					return false;	
				
				if (nextEvent.type ==currentEvent.type && !nextEvent.equals(currentEvent)   )
						return true;
	
			}
		}
		catch(NullPointerException e){
			System.err.println(e);
			return false;
		}
		
		return false;
	}
	
	public  static int getNextDirectionaChangeEndPoint(Event[] output, int pointInTime){
		
		Event nextEvent = null;
		Event currentEvent =  null;
		try{
			
			currentEvent = output[pointInTime];
		}
		catch(ArrayIndexOutOfBoundsException e){
			return Integer.MAX_VALUE;
		}	
		
		try{
			for (int i = pointInTime+1; i < output.length; i++ ){
				nextEvent = output[i] ;
				if (nextEvent.type !=currentEvent.type )
					break;	
			}
		}
		catch(NullPointerException e){
			return -1;
		}
		
		if (nextEvent ==  null || nextEvent.type ==currentEvent.type  )
			return -1;
		
		return nextEvent.end;
	}

	public static double estimateOSlength(int outputIndex, Event[] evaluatedArray, 
			AbstractNode bestUpWardEventTree, AbstractNode bestDownWardEventTree  ){
		double eval=0.0;
				
		if (evaluatedArray[outputIndex].overshoot == null
				|| evaluatedArray[outputIndex].overshoot.end == evaluatedArray[outputIndex].overshoot.start) {
			;
		} else {
			if (evaluatedArray[outputIndex].type == Type.Upturn) {
				eval = bestUpWardEventTree.eval(evaluatedArray[outputIndex].length());
			} else if (evaluatedArray[outputIndex].type == Type.Downturn) {
				eval = bestDownWardEventTree.eval(evaluatedArray[outputIndex].length());
			}
			
		}

		return eval;
		
	}
	
	public static double getRandomDoubleBetweenRange(double min, double max) {
		if (min >= max) {
			throw new IllegalArgumentException("getRandomDoubleBetweenRange: max must be greater than min");
		}
		double x = min + (max - min) * new Random().nextDouble();

		// double x = (Math.random(). *((max-min)+1))+min;
		return x;
	}
	
	// To use: ObjectTypeInMapValue  OTIMV = mostFrequentElement(map.values());
	public static <E> E mostFrequentElement(Iterable<E> iterable) {
	    Map<E, Integer> freqMap = new HashMap<>();
	    E mostFreq = null;
	    int mostFreqCount = -1;
	    for (E e : iterable) {
	        Integer count = freqMap.get(e);
	        freqMap.put(e, count = (count == null ? 1 : count+1));
	        // maintain the most frequent in a single pass.
	        if (count > mostFreqCount) {
	            mostFreq = e;
	            mostFreqCount = count;
	        }
	    }
	    return mostFreq;
	}

}
