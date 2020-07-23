package misc;

import static dc.ga.DCCurve.Type.Downturn;
import static dc.ga.DCCurve.Type.DownwardOvershoot;
import static dc.ga.DCCurve.Type.Upturn;
import static dc.ga.DCCurve.Type.UpwardOvershoot;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import dc.GP.AbstractNode;
import dc.ga.PreProcess;
import dc.ga.DCCurve;
import dc.ga.DCCurve.Event;
import dc.ga.DCCurve.Type;
import dc.io.FReader;
import dc.io.Logger;

public class OverShootPredictionComparator {
	Event[] events;
	int numberOfThresholds;
	protected static double[] THRESHOLDS;
	protected static AbstractNode[] GP_TREES;
	protected static String[] GP_TREES_STRING;
	public static int NUM_OF_PROCESSORS = 5;
	// public static boolean LINEAR_FUNCTIONALITY_ONLY = false;
	// public static int NEGATIVE_EXPRESSION_REPLACEMENT = 5;
	public static String FUNCTION_NODE_DEFINITION = null;
	double thresholdIncrement;

	int TrainingDay = -1;

	int currentGeneration;

	Double[] training;
	Double[] test;
	DCCurveRegression[] curves;

	static double[][] pop;
	double[][] newPop;

	static int nRuns;

	protected static Random random;
	static Map<Double, String> thresholdGPStringMap = new HashMap<Double, String>();
	public static Map<Double, String> thresholdGPStringUpwardMap = new HashMap<Double, String>();
	public static Map<Double, String> thresholdGPStringDownwardMap = new HashMap<Double, String>();

	protected static Logger log;
	public static boolean splitDatasetByTrendType = false; // Reads input
	public static boolean REUSE_EXISTING_TREE = true;
	// parameter
	// to decide whether to split dataset to upward and downward trend datasets

	public OverShootPredictionComparator(String filename, String prefix, boolean isOpenClosePrice,
			String storedDirectory, boolean estimateFabricatedData) throws IOException, ParseException {
		ScriptEngineManager mgr = new ScriptEngineManager();
		ScriptEngine engine = mgr.getEngineByName("JavaScript");
		File dir = new File(storedDirectory);
		File[] downwardDCfoundFiles = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith("DOWNWARD_DC_");
			}
		});

		File[] upwardDCfoundFiles = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith("UPWARD_DC_");
			}
		});

		if (downwardDCfoundFiles.length != upwardDCfoundFiles.length) {
			System.out.println("The number of Upwardrun GP and downward run GP must be the same ");
			System.exit(-1);
		}
		String actualVsEstimate = "";
		String deltaString = "";

		if (false == estimateFabricatedData) {
			PreProcess preprocess = null;
			// loads the data
			ArrayList<Double[]> days = FReader.loadData(filename, isOpenClosePrice);


			// allow the creation of training & testing data sets that are
			// longer
			// than 1 day
			ArrayList<Double[]> ar = new ArrayList<Double[]>();
			for (int i = 0; i <= 20; i++)
				ar.add(days.get(i));
			int size = 0;
			for (Double[] d : ar)
				size += d.length;
			training = new Double[size];
			int counter = 0;
			for (Double[] d : ar) {
				for (double n : d) {
					training[counter] = n;
					counter++;
				}
			}
			ar = new ArrayList<Double[]>();
			for (int i = 21; i <= 27; i++)
				ar.add(days.get(i));
			size = 0;
			for (Double[] d : ar)
				size += d.length;
			test = new Double[size];
			counter = 0;
			for (Double[] d : ar) {
				for (double n : d) {
					test[counter] = n;
					counter++;
				}
			}
			// budget = 100000;

			for (int i = 0; i < downwardDCfoundFiles.length; i++) {
				actualVsEstimate = "DC Event length, Olsen prediction overshoot, Actual Overshoot, GP predicted overshoot";
				deltaString = "";

				File file = downwardDCfoundFiles[i];
				File upwardRunCorrespondingfile = upwardDCfoundFiles[i];

				String upwardRunfilepath = upwardRunCorrespondingfile.getPath();
				// String upwardRunfileName =
				// upwardRunCorrespondingfile.getName();

				// getGPTreeForCurve(GPTreeFileName); // Adesola Get the best GP
				// for
				// this curve while reading GP from harddrive
				String filepath = file.getPath();
				String fileName = file.getName();
				deltaString = fileName.substring(fileName.lastIndexOf("DOWNWARD_DC_") + 12, fileName.lastIndexOf("."));

				File deleteFile = new File(storedDirectory + "/" + deltaString + "_PredictionMap.csv");
				if (deleteFile.delete()) {
					System.out.println("File deleted successfully");
				} else {
					System.out.println("Failed to delete the file");
				}

				double delta = Double.parseDouble(deltaString);

				getDCevents(delta, training);
				
				preprocess =  new PreProcess( delta, filename, "GP");
				
				
				preprocess.buildTraining(this.events);
				preprocess.selectBestClassifier();
				preprocess.lastTrainingEvent = this.events[this.events.length - 1];
				preprocess.loadTrainingData(this.events);
				getDCevents(delta, test);
				preprocess.lastTestingEvent = this.events[this.events.length - 1];
				preprocess.buildTest(this.events);
				preprocess.loadTestData(this.events);

					
				
				
				preprocess.classifyTestData();

				BufferedReader br = new BufferedReader(new FileReader(filepath));
				String GPToString;
				GPToString = br.readLine();
				br.close();

				br = new BufferedReader(new FileReader(upwardRunfilepath));
				String GPToStringUpwardRun;
				GPToStringUpwardRun = br.readLine();
				br.close();

				for (int outputIndex = 0; outputIndex < this.events.length; outputIndex++) {

					String foo = "";

					if (this.events[outputIndex].type == Type.Upturn)
						foo = GPToStringUpwardRun;
					else
						foo = GPToString;

					String classificationStr = "no";
					
					//TODO 28/09/2018 needs fixxing test is no longer genn
					if (preprocess.getTestInstance().numInstances() > outputIndex)
						classificationStr = preprocess.classifyTestInstance(outputIndex);
						
					foo = foo.replace("X0", Integer.toString(this.events[outputIndex].length()));
					double eval = 0.0;
					Double javascriptValue = Double.MAX_VALUE;

					try {
						if (classificationStr.equals("yes")) {
							javascriptValue = (Double) engine.eval(foo);
							eval = javascriptValue.doubleValue();
						}
					} catch (ScriptException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ClassCastException e) {
						e.printStackTrace();
					}
					if (this.events[outputIndex].overshoot == null)
						actualVsEstimate = actualVsEstimate + this.events[outputIndex].length() + ","
								+ (this.events[outputIndex].length() * 2) + ",0.0," + eval + "\n";
					else
						actualVsEstimate = actualVsEstimate + this.events[outputIndex].length() + ","
								+ (this.events[outputIndex].length() * 2) + ","
								+ this.events[outputIndex].overshoot.length() + "," + eval + "\n";
				}
				// filepath

				String mappingFile = storedDirectory + "/" + deltaString + "_PredictionMap.csv";
				BufferedWriter writer = new BufferedWriter(new FileWriter(mappingFile, true));
				writer.append(actualVsEstimate);

				writer.close();
				// FWriter writer = null;
				// writer.openToAppend(new File(mappingFile));
				// writer.write(actualVsEstimate);
				// writer.closeFile();

				// save here
			}

		}
		else
		{
			ArrayList<Integer> eventsInt = (ArrayList<Integer>) Arrays.asList(1,2,3,4,5,6,7,8,9,10
					,11,12,13,14,15,16,17,18,19,20,
					21,22,23,24,25,26,27,28,29,30,
					31,32,33,34,35,36,37,38,39,40,
					41,42,43,44,45,46,47,48,49,50,
					51,52,53,54,55,56,57,58,59,60,
					61,62,63,64,65,66,67,68,69,70,
					71,72,73,74,75,76,77,78,79,80,
					81,82,83,84,85,86,87,88,89,90,
					91,92,93,94,95,96,97,98,99,100);



			for (int i=0; i < 5 ; i++){

				File file = downwardDCfoundFiles[i];
				File upwardRunCorrespondingfile = upwardDCfoundFiles[i];

				String upwardRunfilepath = upwardRunCorrespondingfile.getPath();


				String filepath = file.getPath();
				String fileName = file.getName();
				deltaString = fileName.substring(fileName.lastIndexOf("DOWNWARD_DC_") + 12, fileName.lastIndexOf("."));


				BufferedReader br = new BufferedReader(new FileReader(filepath));
				String GPToString;
				GPToString = br.readLine();
				br.close();

				br = new BufferedReader(new FileReader(upwardRunfilepath));
				String GPToStringUpwardRun;
				GPToStringUpwardRun = br.readLine();
				br.close();


				if (downwardDCfoundFiles.length != upwardDCfoundFiles.length) {
					System.out.println("The number of Upwardrun GP and downward run GP must be the same ");
					System.exit(-1);
				}


				actualVsEstimate = "DC Event length, Olsen prediction overshoot, Actual Overshoot, GP predicted overshoot";
				deltaString = "";


				deltaString = fileName.substring(fileName.lastIndexOf("DOWNWARD_DC_") + 12, fileName.lastIndexOf("."));

				File deleteFile = new File(storedDirectory + "/" + deltaString + "_PredictionMap.csv");
				if (deleteFile.delete()) {
					System.out.println("File deleted successfully");
				} else {
					System.out.println("Failed to delete the file");
				}

				double delta = Double.parseDouble(deltaString);

				//upturn
				for (int eventsIntIndex = 0; eventsIntIndex < eventsInt.size(); eventsIntIndex++) {

					String foo = GPToStringUpwardRun;


					foo = foo.replace("X0", Integer.toString(eventsInt.get(eventsIntIndex)));
					double eval = 0.0;
					Double javascriptValue = Double.MAX_VALUE;

					try {

						javascriptValue = (Double) engine.eval(foo);
						eval = javascriptValue.doubleValue();
					}
					catch (ScriptException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ClassCastException e) {
						e.printStackTrace();
					}

					actualVsEstimate = actualVsEstimate + eventsInt.get(eventsIntIndex) + ","+ eval + "\n";

					// filepath


				}

				String mappingFile = storedDirectory + "/" + deltaString + "_EstimatedUpwardOvershootEquaton.csv";
				BufferedWriter writer = new BufferedWriter(new FileWriter(mappingFile, true));
				writer.append(actualVsEstimate);

				writer.close();
				
				//downturn
				for (int eventsIntIndex = 0; eventsIntIndex < eventsInt.size(); eventsIntIndex++) {

					String foo = GPToString;


					foo = foo.replace("X0", Integer.toString(eventsInt.get(eventsIntIndex)));
					
					double eval = 0.0;
					Double javascriptValue = Double.MAX_VALUE;

					try {

						javascriptValue = (Double) engine.eval(foo);
						eval = javascriptValue.doubleValue();
					}
					catch (ScriptException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ClassCastException e) {
						e.printStackTrace();
					}

					actualVsEstimate = actualVsEstimate + eventsInt.get(eventsIntIndex) + ","+ eval + "\n";
				}
				// filepath
				mappingFile = storedDirectory + "/" + deltaString + "_EstimatedDownwardOvershootEquaton.csv";
				writer = new BufferedWriter(new FileWriter(mappingFile, true));
				writer.append(actualVsEstimate);

				writer.close();
				
			}
		}

		System.out.println("XXXXXX");

	}

	private void getDCevents(double delta, Double[] rawData) {
		double pHigh = 0;
		double pLow = 0;

		int[] indexDC = new int[2]; // DC event indexes
		int[] indexOS = new int[2]; // OS event indexes
		int index = 1;

		ArrayList<Event> events = new ArrayList<Event>();
		Type event = Type.Upturn;

		DCCurve.Event last = new DCCurve.Event(0, 0, Type.Upturn);
		events.add(last);

		Event[] output = new Event[rawData.length];

		for (double value : rawData) {
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
					pLow = value;

					indexDC[1] = index;
					indexOS[0] = index + 1;

					last = new Event(indexDC[0], indexDC[1], Downturn);
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

					event = Upturn;
					pHigh = value;

					indexDC[1] = index;
					indexOS[0] = index + 1;

					last = new Event(indexDC[0], indexDC[1], Upturn);
					events.add(last);
				} else if (pLow > value) {
					pLow = value;

					indexDC[0] = index;
					indexOS[1] = index - 1;
				}
			}

			// output[index - 1] = (byte) (last.type == Type.Upturn ? 1 : 0);

			/*
			 * If tree is read from file use this. if this fails remove and
			 * rollback to previous
			 */
			output[index - 1] = last;

			index++;
		}
		this.events = null;
		this.events = events.toArray(new Event[events.size()]);

	}

	public static void main(String[] args) throws Exception {

		// Split the long parameter file , according to the delimiter
		String s[] = args[0].split(":");
		boolean isOpenClose = false;
		if (s[2].equalsIgnoreCase("true"))
			isOpenClose = true;

		File dir = new File(s[3]);

		boolean successful = dir.mkdir();

		String storedDirectory = null;

		if (!dir.exists()) {
			System.out.println("Direcrtory not set");
			System.exit(0);
		} else {
			storedDirectory = dir.getAbsolutePath();
			System.setProperty("user.dir", storedDirectory);
			System.out.println("Files stored in :" + storedDirectory);
		}

		OverShootPredictionComparator ga = new OverShootPredictionComparator(s[0], s[1], isOpenClose, storedDirectory,
				false);
		System.out.println("XXXXXX");

		// TreeHelperClass.readFitnessFile(2);
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

	private Event detect(Type type, int[] indexDC, int[] indexOS) {
		// overshoot event must have a start index lower that
		// the DC event start index
		if (indexOS[0] < indexOS[1] && indexOS[0] < indexDC[0]) {
			return new Event(indexOS[0], indexOS[1], type);
		}

		return null;
	}

}
