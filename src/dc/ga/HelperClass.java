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

import dc.GP.Const;
import dc.ga.DCCurve.Event;
import dc.io.FReader;
import misc.DCEventGenerator;

public class HelperClass {

	public static void  updateClassifierWithTestData(double threshold, Double[] testData, DCCurve dcurve){
		
		
		DCEventGenerator dCEventGenerator = new DCEventGenerator();
		dCEventGenerator.generateEvents(testData, threshold);

		Event[] copiedTestArray = Arrays.copyOf(dCEventGenerator.getEvents(),
				dCEventGenerator.getEvents().length);

		

		dcurve.preprocess.lastTestingEvent = copiedTestArray[copiedTestArray.length - 1];

		dcurve.preprocess.buildTest(copiedTestArray);
		System.out.println("About to print test data for threshold " + threshold);
		dcurve.preprocess.processTestData(copiedTestArray);

		dcurve.preprocess.loadTestData(copiedTestArray);

		dcurve.preprocess.classifyTestData();
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
	
	
	public static PreProcess  getClassifier(double threshold, String filename, Double [] training, String regAlgo){
		
		
			String thresholdStr = String.format("%.8f", threshold);
			
			DCEventGenerator dCEventGenerator = new DCEventGenerator();
			dCEventGenerator.generateEvents(training, threshold);
			Event[] copiedArray;
			copiedArray = Arrays.copyOf(dCEventGenerator.getEvents(), dCEventGenerator.getEvents().length);
			Map<String, Event[]> trainingEventsArray = new HashMap<String, Event[]>();
			
			trainingEventsArray.put(thresholdStr, copiedArray);
			
			PreProcess preprocess = new PreProcess(threshold, filename, regAlgo);

			preprocess.buildTraining(copiedArray);
			preprocess.selectBestClassifier();

			// loadTrainingData load classification data "order is
			// important"
			preprocess.loadTrainingData(copiedArray);

			preprocess.lastTrainingEvent = copiedArray[copiedArray.length - 1];

			preprocess.runAutoWeka();
			
			

		
		return preprocess;
	}

	public static Map<Double, Map<String, Double>> getBestThresholdsMF( String filename,int arraySize, Double[] dataset, int numberOfCandidateThreshold, double startingThreshold,
			double interval){
		double [] bestThresholdArray = new double[arraySize];
		double [] THRESHOLDS = new double[numberOfCandidateThreshold];
		Map<String, Event[]> trainingEventsMap = new HashMap<String, Event[]>();
		Map<Double, Map<String, Double>> thresholdGPMap = new HashMap<Double, Map<String, Double>>();
		
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
			Map<String, Double> gpMap = new HashMap<String, Double>();
			Event[] copiedArray;
			THRESHOLDS[i] = (startingThreshold + (interval * i)) / 100.0;
			String thresholdStr = String.format("%.8f", THRESHOLDS[i]);
			String gpFileNamePrefix = gpDaysMap.get(0); //Just get first element in collection
			String gpFileName = gpFileNamePrefix + "_" + thresholdStr + ".txt";

			dCEventGenerator.generateEvents(dataset, THRESHOLDS[i]);
			copiedArray = Arrays.copyOf(dCEventGenerator.getEvents(), dCEventGenerator.getEvents().length);
			trainingEventsMap.put(thresholdStr, copiedArray);
			
			perfectForesightDCCurve[i].filename = filename;
			perfectForesightDCCurve[i].build(dataset, THRESHOLDS[i], gpFileName, copiedArray, null);
			perfectForesightDCCurve[i].estimateTraining(null); 
			double perfectForcastTrainingReturn = perfectForesightDCCurve[i].trainingTrading(null);
			perfectForecastReturnMap.put(THRESHOLDS[i], perfectForcastTrainingReturn);
			gpMap.put("DownwardGP", ((PerfectForecastDCCurve)perfectForesightDCCurve[i]).medianRatio[0]);
			gpMap.put("UpwardGP", ((PerfectForecastDCCurve)perfectForesightDCCurve[i]).medianRatio[1]);
			
			thresholdGPMap.put(THRESHOLDS[i], gpMap);
		}
		List<Entry<Double, Double>> greatest = findGreatest(perfectForecastReturnMap, Const.NUMBER_OF_SELECTED_THRESHOLDS); // best
		// 5
		// thresholds
		
		int tradingThresholdCount =0;
		for (Entry<Double, Double> entry : greatest) {
			// System.out.println(entry);
			bestThresholdArray[tradingThresholdCount] = entry.getKey();
			
			tradingThresholdCount++;
		}
		Map<Double, Map<String, Double>> selectedThresholdGPMap = new HashMap<Double, Map<String, Double>>();
		for (int k =0; k< bestThresholdArray.length; k++){
			
			selectedThresholdGPMap.put(bestThresholdArray[k], thresholdGPMap.get(bestThresholdArray[k]));
		}
		
		for (Entry<Double, Map<String, Double>> entry : selectedThresholdGPMap.entrySet()) {
			
			// System.out.println(entry);
	
			System.out.println("entries are "+ entry.getKey() + ": " +entry.getValue().get("UpwardGP"));
		}
		
		return selectedThresholdGPMap;
	}
	
		public static Map<Double, Map<String, Double>> getBestThresholdsOlsen( String filename,int arraySize, Double[] dataset, int numberOfCandidateThreshold, double startingThreshold,
				double interval){
			double [] bestThresholdArray = new double[arraySize];
			double [] THRESHOLDS = new double[numberOfCandidateThreshold];
			Map<String, Event[]> trainingEventsMap = new HashMap<String, Event[]>();
			Map<Double, Map<String, Double>> thresholdGPMap = new HashMap<Double, Map<String, Double>>();
			
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
				Map<String, Double> gpMap = new HashMap<String, Double>();
				Event[] copiedArray;
				THRESHOLDS[i] = (startingThreshold + (interval * i)) / 100.0;
				String thresholdStr = String.format("%.8f", THRESHOLDS[i]);
				String gpFileNamePrefix = gpDaysMap.get(0); //Just get first element in collection
				String gpFileName = gpFileNamePrefix + "_" + thresholdStr + ".txt";

				dCEventGenerator.generateEvents(dataset, THRESHOLDS[i]);
				copiedArray = Arrays.copyOf(dCEventGenerator.getEvents(), dCEventGenerator.getEvents().length);
				trainingEventsMap.put(thresholdStr, copiedArray);
				
				perfectForesightDCCurve[i].filename = filename;
				perfectForesightDCCurve[i].build(dataset, THRESHOLDS[i], gpFileName, copiedArray, null);
				perfectForesightDCCurve[i].estimateTraining(null); 
				double perfectForcastTrainingReturn = perfectForesightDCCurve[i].trainingTrading(null);
				perfectForecastReturnMap.put(THRESHOLDS[i], perfectForcastTrainingReturn);
				gpMap.put("DownwardGP", 2.0);
				gpMap.put("UpwardGP", 2.0);
				
				thresholdGPMap.put(THRESHOLDS[i], gpMap);
			}
			List<Entry<Double, Double>> greatest = findGreatest(perfectForecastReturnMap, Const.NUMBER_OF_SELECTED_THRESHOLDS); // best
			// 5
			// thresholds
			
			int tradingThresholdCount =0;
			for (Entry<Double, Double> entry : greatest) {
				// System.out.println(entry);
				bestThresholdArray[tradingThresholdCount] = entry.getKey();
				
				tradingThresholdCount++;
			}
			Map<Double, Map<String, Double>> selectedThresholdGPMap = new HashMap<Double, Map<String, Double>>();
			for (int k =0; k< bestThresholdArray.length; k++){
				
				selectedThresholdGPMap.put(bestThresholdArray[k], thresholdGPMap.get(bestThresholdArray[k]));
			}
			
			for (Entry<Double, Map<String, Double>> entry : selectedThresholdGPMap.entrySet()) {
				
				// System.out.println(entry);
		
				System.out.println("entries are "+ entry.getKey() + ": " +entry.getValue().get("UpwardGP"));
			}
			
			return selectedThresholdGPMap;
		}

			
			public static Map<Double, Map<String, String>> getBestThresholdsH( String filename,int arraySize, Double[] dataset, int numberOfCandidateThreshold, double startingThreshold,
					double interval){
				double [] bestThresholdArray = new double[arraySize];
				double [] THRESHOLDS = new double[numberOfCandidateThreshold];
				Map<String, Event[]> trainingEventsMap = new HashMap<String, Event[]>();
				Map<Double, Map<String, String>> thresholdGPMap = new HashMap<Double, Map<String, String>>();
				Map<Double, Double> thresholdRatioMap = new HashMap<Double, Double>();
				
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
					Map<String, String> gpMap = new HashMap<String, String>();
					Event[] copiedArray;
					THRESHOLDS[i] = (startingThreshold + (interval * i)) / 100.0;
					String thresholdStr = String.format("%.8f", THRESHOLDS[i]);
					String gpFileNamePrefix = gpDaysMap.get(0); //Just get first element in collection
					String gpFileName = gpFileNamePrefix + "_" + thresholdStr + ".txt";

					dCEventGenerator.generateEvents(dataset, THRESHOLDS[i]);
					copiedArray = Arrays.copyOf(dCEventGenerator.getEvents(), dCEventGenerator.getEvents().length);
					trainingEventsMap.put(thresholdStr, copiedArray);
					
					perfectForesightDCCurve[i].filename = filename;
					perfectForesightDCCurve[i].build(dataset, THRESHOLDS[i], gpFileName, copiedArray, null);
					perfectForesightDCCurve[i].estimateTraining(null); 
					double perfectForcastTrainingReturn = perfectForesightDCCurve[i].trainingTrading(null);
					perfectForecastReturnMap.put(THRESHOLDS[i], perfectForcastTrainingReturn);
					//System.out.print(perfectForcastTrainingReturn);
					//System.out.print(((PerfectForecastDCCurve)curvePerfectForesight[i]).getUpwardDCTree());
					gpMap.put("UpwardGP", ((PerfectForecastDCCurve)perfectForesightDCCurve[i]).getUpwardDCTree());
					gpMap.put("DownwardGP", ((PerfectForecastDCCurve)perfectForesightDCCurve[i]).getDownwardDCTree());
					thresholdGPMap.put(THRESHOLDS[i], gpMap);
				}
				List<Entry<Double, Double>> greatest = findGreatest(perfectForecastReturnMap, 5); // best
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
					
					selectedThresholdGPMap.put(bestThresholdArray[k], thresholdGPMap.get(bestThresholdArray[k]));
				}
				
				for (Entry<Double, Map<String, String>> entry : selectedThresholdGPMap.entrySet()) {
					
					// System.out.println(entry);
					
					
					System.out.println("entries are "+ entry.getKey() + ": " +entry.getValue().get("UpwardGP"));
				}
				
				return selectedThresholdGPMap;
			}

		
	public static Map<Double, Map<String, String>> getBestThresholds( String filename,int arraySize, Double[] dataset, int numberOfCandidateThreshold, double startingThreshold,
			double interval){
		double [] bestThresholdArray = new double[arraySize];
		double [] THRESHOLDS = new double[numberOfCandidateThreshold];
		Map<String, Event[]> trainingEventsMap = new HashMap<String, Event[]>();
		Map<Double, Map<String, String>> thresholdGPMap = new HashMap<Double, Map<String, String>>();
		Map<Double, Double> thresholdRatioMap = new HashMap<Double, Double>();
		PreProcess[] preprocess = new PreProcess[THRESHOLDS.length];
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
			Map<String, String> gpMap = new HashMap<String, String>();
			Event[] copiedArray;
			THRESHOLDS[i] = (startingThreshold + (interval * i)) / 100.0;
			String thresholdStr = String.format("%.8f", THRESHOLDS[i]);
			String gpFileNamePrefix = gpDaysMap.get(0); //Just get first element in collection
			String gpFileName = gpFileNamePrefix + "_" + thresholdStr + ".txt";

			dCEventGenerator.generateEvents(dataset, THRESHOLDS[i]);
			copiedArray = Arrays.copyOf(dCEventGenerator.getEvents(), dCEventGenerator.getEvents().length);
			trainingEventsMap.put(thresholdStr, copiedArray);
			
			perfectForesightDCCurve[i].filename = filename;
			perfectForesightDCCurve[i].build(dataset, THRESHOLDS[i], gpFileName, copiedArray, null);
			perfectForesightDCCurve[i].estimateTraining(null); 
			double perfectForcastTrainingReturn = perfectForesightDCCurve[i].trainingTrading(preprocess[i]);
			perfectForecastReturnMap.put(THRESHOLDS[i], perfectForcastTrainingReturn);
			//System.out.print(perfectForcastTrainingReturn);
			//System.out.print(((PerfectForecastDCCurve)curvePerfectForesight[i]).getUpwardDCTree());
			gpMap.put("UpwardGP", ((PerfectForecastDCCurve)perfectForesightDCCurve[i]).getUpwardDCTree());
			gpMap.put("DownwardGP", ((PerfectForecastDCCurve)perfectForesightDCCurve[i]).getDownwardDCTree());
			thresholdGPMap.put(THRESHOLDS[i], gpMap);
		}
		
		
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
			
			selectedThresholdGPMap.put(bestThresholdArray[k], thresholdGPMap.get(bestThresholdArray[k]));
		}
		
		for (Entry<Double, Map<String, String>> entry : selectedThresholdGPMap.entrySet()) {
			
			// System.out.println(entry);
			
			
			System.out.println("entries are "+ entry.getKey() + ": " +entry.getValue().get("UpwardGP"));
		}
		
		return selectedThresholdGPMap;
	}
	
	private static <K, V extends Comparable<? super V>> List<Entry<K, V>> findGreatest(Map<K, V> map, int n) {
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
}
