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

	public static Map<Double, Map<String, AbstractNode>> getBestGPThresholds( String filename,int arraySize, Double[] dataset, int numberOfCandidateThreshold, double startingThreshold,
			double interval){
		double [] bestThresholdArray = new double[arraySize];
		double [] THRESHOLDS = new double[numberOfCandidateThreshold];
		Map<String, Event[]> trainingEventsMap = new HashMap<String, Event[]>();
		Map<String, Event[]> traininOutputMap = new HashMap<String, Event[]>();
		Map<Double, Map<String, AbstractNode>> thresholdMap = new HashMap<Double, Map<String, AbstractNode>>();
		
		PreProcess[] preprocess = new PreProcess[THRESHOLDS.length];
		DCCurve[] perfectForesightDCCurve;
		perfectForesightDCCurve = new PerfectForecastDCCurve[THRESHOLDS.length];
		Map<Double, Double> perfectForecastReturnMap = new HashMap<Double, Double>();
		Map<Double, Double> perfectForecastThresholdStatMap = new HashMap<Double, Double>();
		ConcurrentHashMap<Integer, String> gpDaysMap = null;
		DCCurve dc = new DCCurve();
		DCCurve.ThresholdStats thresholdStats = dc.new ThresholdStats();
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
			Map<String, AbstractNode> gpMap = new HashMap<String, AbstractNode>();
			Event[] copiedArray;
			Event[] copiedOutputArray;
			THRESHOLDS[i] = (startingThreshold + (interval * i)) / 100.0;
			String thresholdStr = String.format("%.8f", THRESHOLDS[i]);
			System.out.println("getting best GP for threshold" + thresholdStr);
			String gpFileNamePrefix = gpDaysMap.get(0); //Just get first element in collection
			String gpFileName = gpFileNamePrefix + "_" + thresholdStr + ".txt";

			dCEventGenerator.generateEvents(dataset, THRESHOLDS[i]);
			copiedArray = Arrays.copyOf(dCEventGenerator.getEvents(), dCEventGenerator.getEvents().length);
			trainingEventsMap.put(thresholdStr, copiedArray);
			
			copiedOutputArray =  Arrays.copyOf(dCEventGenerator.getOutput(), dCEventGenerator.getOutput().length);
			traininOutputMap.put(thresholdStr, copiedOutputArray);
			
			perfectForesightDCCurve[i].filename = filename;
			perfectForesightDCCurve[i].build(dataset, THRESHOLDS[i], gpFileName, copiedArray,copiedOutputArray,  null);
			perfectForesightDCCurve[i].estimateTraining(null); 
			double perfectForcastTrainingReturn = perfectForesightDCCurve[i].trainingTrading(preprocess[i]);
			perfectForecastReturnMap.put(THRESHOLDS[i], perfectForcastTrainingReturn);
			
			
			//System.out.print(perfectForcastTrainingReturn);
			//System.out.print(((PerfectForecastDCCurve)curvePerfectForesight[i]).getUpwardDCTree());
			if (Const.OsFunctionEnum == Const.function_code.eGP){
				gpMap.put("UpwardEvent", ((PerfectForecastDCCurve)perfectForesightDCCurve[i]).getUpwardDCTree());
				gpMap.put("DownwardEvent", ((PerfectForecastDCCurve)perfectForesightDCCurve[i]).getDownwardDCTree());
				thresholdMap.put(THRESHOLDS[i], gpMap);
				thresholdStats.upwardFitness= ((PerfectForecastDCCurve)perfectForesightDCCurve[i]).bestUpWardEventTree.perfScore;
				thresholdStats.downwardFitness= ((PerfectForecastDCCurve)perfectForesightDCCurve[i]).bestDownWardEventTree.perfScore;
				thresholdStats.numberOfEvents = copiedArray.length;
				thresholdStats.budget = 500000.0;
				thresholdStats.totalReturn =  perfectForcastTrainingReturn;
				int OScount =  0;
				for (Event event : copiedArray){
					if (event.overshoot !=null)
						OScount++;
				}
				thresholdStats.numberOfOSEvents = OScount;
				thresholdStats.numberOfDataPts = dataset.length;
				double score = thresholdStats.getScore();
				perfectForecastThresholdStatMap.put(THRESHOLDS[i],score);
				System.out.println("score is" + score);
			}
		}
		
		//List<Entry<Double, Double>> greatest = findGreatest( perfectForecastThresholdStatMap, Const.NUMBER_OF_SELECTED_THRESHOLDS);
		List<Entry<Double, Double>> greatest = findGreatest( perfectForecastReturnMap, Const.NUMBER_OF_SELECTED_THRESHOLDS);
		
		int tradingThresholdCount =0;
		for (Entry<Double, Double> entry : greatest) {
			System.out.println("Selected Thresholds are :" + String.format("%.8f", entry.getKey()));
			bestThresholdArray[tradingThresholdCount] = entry.getKey();
			
			tradingThresholdCount++;
		}
		Map<Double, Map<String, AbstractNode>> selectedThresholdGPMap = new HashMap<Double, Map<String, AbstractNode>>();
		for (int k =0; k< bestThresholdArray.length; k++){
			
			selectedThresholdGPMap.put(bestThresholdArray[k], thresholdMap.get(bestThresholdArray[k]));
		}
		if (Const.OsFunctionEnum == Const.function_code.eGP){
		
			for (Entry<Double, Map<String, AbstractNode>> entry : selectedThresholdGPMap.entrySet()) {
				// System.out.println(entry);
				System.out.println("entries are "+ entry.getKey() + ": \n upward tree"  +entry.getValue().get("UpwardEvent").printAsInFixFunction()
						+ " upward score is"  + entry.getValue().get("UpwardEvent").perfScore + "  \n downward Tree" + entry.getValue().get("DownwardEvent").printAsInFixFunction() 
						+ " downward score is" +  entry.getValue().get("DownwardEvent").perfScore );
			}
		}
		
		return selectedThresholdGPMap;
	}
	
	
	public static Map<Double, Map<String, String>> getBestThresholds( String filename,int arraySize, Double[] dataset, int numberOfCandidateThreshold, double startingThreshold,
			double interval){
		double [] bestThresholdArray = new double[arraySize];
		double [] THRESHOLDS = new double[numberOfCandidateThreshold];
		Map<String, Event[]> trainingEventsMap = new HashMap<String, Event[]>();
		Map<String, Event[]> traininOutputMap = new HashMap<String, Event[]>();
		Map<Double, Map<String, String>> thresholdMap = new HashMap<Double, Map<String, String>>();
		PreProcess[] preprocess = new PreProcess[THRESHOLDS.length];
		DCCurve[] perfectForesightDCCurve;
		perfectForesightDCCurve = new PerfectForecastDCCurve[THRESHOLDS.length];
		Map<Double, Double> perfectForecastReturnMap = new HashMap<Double, Double>();
		Map<Double, Double> perfectForecastNormalisedReturnMap = new HashMap<Double, Double>();
		ConcurrentHashMap<Integer, String> gpDaysMap = null;
		DCCurve dc = new DCCurve();
		DCCurve.ThresholdStats thresholdStats = dc.new ThresholdStats();
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
			Map<String, String> gpMap = new HashMap<String, String>();
			Event[] copiedArray;
			Event[] copiedOutputArray;
			THRESHOLDS[i] = (startingThreshold + (interval * i)) / 100.0;
			String thresholdStr = String.format("%.8f", THRESHOLDS[i]);
			System.out.println("getting best GP for threshold" + thresholdStr);
			String gpFileNamePrefix = gpDaysMap.get(0); //Just get first element in collection
			String gpFileName = gpFileNamePrefix + "_" + thresholdStr + ".txt";

			dCEventGenerator.generateEvents(dataset, THRESHOLDS[i]);
			copiedArray = Arrays.copyOf(dCEventGenerator.getEvents(), dCEventGenerator.getEvents().length);
			trainingEventsMap.put(thresholdStr, copiedArray);
			
			copiedOutputArray =  Arrays.copyOf(dCEventGenerator.getOutput(), dCEventGenerator.getOutput().length);
			traininOutputMap.put(thresholdStr, copiedOutputArray);
			
			
			perfectForesightDCCurve[i].filename = filename;
			perfectForesightDCCurve[i].build(dataset, THRESHOLDS[i], gpFileName, copiedArray,copiedOutputArray, null);
			perfectForesightDCCurve[i].estimateTraining(null); 
			double perfectForcastTrainingReturn = perfectForesightDCCurve[i].trainingTrading(preprocess[i]);
			perfectForecastReturnMap.put(THRESHOLDS[i], perfectForcastTrainingReturn);
			
			
			//System.out.print(perfectForcastTrainingReturn);
			//System.out.print(((PerfectForecastDCCurve)curvePerfectForesight[i]).getUpwardDCTree());
			if (Const.OsFunctionEnum == Const.function_code.eGP){
				gpMap.put("UpwardEvent", ((PerfectForecastDCCurve)perfectForesightDCCurve[i]).getUpwardDCTreeString());
				gpMap.put("DownwardEvent", ((PerfectForecastDCCurve)perfectForesightDCCurve[i]).getDownwardDCTreeString());
				thresholdMap.put(THRESHOLDS[i], gpMap);
				thresholdStats.upwardFitness= ((PerfectForecastDCCurve)perfectForesightDCCurve[i]).bestUpWardEventTree.perfScore;
				thresholdStats.downwardFitness= ((PerfectForecastDCCurve)perfectForesightDCCurve[i]).bestDownWardEventTree.perfScore;
				thresholdStats.numberOfEvents = copiedArray.length;
				thresholdStats.numberOfDataPts = dataset.length;
				double score = thresholdStats.getScore();
				perfectForecastNormalisedReturnMap.put(THRESHOLDS[i],score);
				System.out.println("score is" + score);
			}
			else if (Const.OsFunctionEnum == Const.function_code.eMichaelFernando){
				gpMap.put("UpwardEvent", String.valueOf(((PerfectForecastDCCurve)perfectForesightDCCurve[i]).meanRatio[1]));
				gpMap.put("DownwardEvent", String.valueOf(((PerfectForecastDCCurve)perfectForesightDCCurve[i]).meanRatio[0]));
				thresholdMap.put(THRESHOLDS[i], gpMap);
			}
			else if (Const.OsFunctionEnum == Const.function_code.eOlsen){
				gpMap.put("UpwardEvent", "2.0");
				gpMap.put("DownwardEvent", "2.0");
				thresholdMap.put(THRESHOLDS[i], gpMap);
				
			}
		}
		
		//List<Entry<Double, Double>> least = findLeast( perfectForecastNormalisedReturnMap, Const.NUMBER_OF_SELECTED_THRESHOLDS);
		List<Entry<Double, Double>> greatest = findGreatest(perfectForecastReturnMap, Const.NUMBER_OF_SELECTED_THRESHOLDS); // best
		// 5
		// thresholds
		
		int tradingThresholdCount =0;
		for (Entry<Double, Double> entry : greatest) {
			// System.out.println(entry);
			bestThresholdArray[tradingThresholdCount] = entry.getKey();
			
			tradingThresholdCount++;
		}
		Map<Double, Map<String, String>> selectedThresholdGPMap = new HashMap<Double, Map<String, String>>();
		for (int k =0; k< bestThresholdArray.length; k++){
			
			selectedThresholdGPMap.put(bestThresholdArray[k], thresholdMap.get(bestThresholdArray[k]));
		}
		if (Const.OsFunctionEnum == Const.function_code.eGP){
		
			for (Entry<Double, Map<String, String>> entry : selectedThresholdGPMap.entrySet()) {
				// System.out.println(entry);
				System.out.println("entries are "+ entry.getKey() + ": \n"  +entry.getValue().get("UpwardEvent")
						+ "\n" + entry.getValue().get("DownwardEvent") );
			}
		}
		
		return selectedThresholdGPMap;
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
	
	public  static int getNextDirectionaChangeEndPoint(Event[] output, int pointInTime){
		
		Event nextEvent = null;
		Event currentEvent =  null;
		try{
			
			currentEvent = output[pointInTime];
		}
		catch(ArrayIndexOutOfBoundsException e){
			return Integer.MAX_VALUE;
		}	
		for (int i = pointInTime+1; i < output.length; i++ ){
			nextEvent = output[i] ;
			if (nextEvent.type !=currentEvent.type )
				break;	
		}
		
		if (nextEvent ==  null || nextEvent.type ==currentEvent.type  )
			return -1;
		
		return nextEvent.end;
	}

	public static double estimateOSlength(int outputIndex, Event[] evaluatedArray, 
			String upwardTrendTreeString, String downwardTrendTreeString,
			AbstractNode bestUpWardEventTree, AbstractNode bestDownWardEventTree  ){
		double eval=0.0;
		String foo = "";
		
			if (evaluatedArray[outputIndex].type == Type.Upturn) {
				foo = upwardTrendTreeString;		
				
				
				
			} else if (evaluatedArray[outputIndex].type == Type.Downturn) {
				foo = downwardTrendTreeString;
				
			} else {
				 throw new IllegalArgumentException("Invalid Event type!");
			}

	

		foo = foo.replace("X0", Integer.toString(evaluatedArray[outputIndex].length()));
		
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

}
