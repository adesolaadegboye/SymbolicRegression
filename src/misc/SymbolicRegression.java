package misc;

import java.io.File;

import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import dc.GP.AbstractNode;
import dc.GP.Add;
import dc.GP.Const;

import dc.GP.TreeHelperClass;
import dc.GP.TreeOperation;
import dc.GP.Const.treeStructurePostcreate;

import dc.ga.GA;
import dc.ga.PreProcess;
import dc.ga.PreProcessManual;

import static dc.ga.DCCurve.Type.Downturn;
import static dc.ga.DCCurve.Type.DownwardOvershoot;
import static dc.ga.DCCurve.Type.Upturn;
import static dc.ga.DCCurve.Type.UpwardOvershoot;

import dc.ga.DCCurve.Event;
import dc.ga.DCCurve.Type;
import dc.io.FReader;
import dc.io.Logger;
import eu.verdelhan.ta4j.BaseStrategy;
import eu.verdelhan.ta4j.BaseTick;
import eu.verdelhan.ta4j.BaseTimeSeries;
import eu.verdelhan.ta4j.Order;
import eu.verdelhan.ta4j.Strategy;
import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.TimeSeriesManager;
import eu.verdelhan.ta4j.TradingRecord;
import eu.verdelhan.ta4j.indicators.EMAIndicator;
import eu.verdelhan.ta4j.indicators.SMAIndicator;
import eu.verdelhan.ta4j.indicators.helpers.ClosePriceIndicator;
import eu.verdelhan.ta4j.indicators.statistics.StandardDeviationIndicator;
import eu.verdelhan.ta4j.trading.rules.OverIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.UnderIndicatorRule;
import files.FWriter;
import misc.technicalAnalysis.AroonIndicator;
import misc.technicalAnalysis.BolingerBAndS;
import misc.technicalAnalysis.CommodityChannelIndexIndicator;
import misc.technicalAnalysis.ExponentialMovingAverageFX;
import misc.technicalAnalysis.MovingMomentum;
import misc.technicalAnalysis.ParabolicSar;
import misc.technicalAnalysis.PriceRateOfChangeIndicator;
import misc.technicalAnalysis.RelativeStrengthIndex;
import misc.technicalAnalysis.SimpleMovingAverage;
import misc.technicalAnalysis.StochOscillatorKIndicator;
import misc.technicalAnalysis.TechnicalAnaysisBaseTrading;
import misc.technicalAnalysis.WilliamR;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;

public class SymbolicRegression {

	int numberOfThresholds;
	protected double[] THRESHOLDS;
	protected double[] SELECTED_THRESHOLDS;
	protected double[] SELECTED_THRESHOLDS_MF;
	protected double[] SELECTED_THRESHOLDS_OLSEN;
	

	protected double[] SELECTED_THRESHOLDS_CLASSIFICATION_ONLY;

	
	
	double thresholdIncrement;
	String autowekaRunsList = "";
	ConcurrentHashMap<Integer, String> gpDaysMap = null;
	int TrainingDay = -1;
	
	PreProcess[] preprocess = null;
	PreProcess[] preprocessMF = null;
	PreProcess[] preprocessOlsen = null;
		
	PreProcessManual[] preprocessManualJ48 = null;
	PreProcessManual[] preprocessManualMFJ48 = null;
	PreProcessManual[] preprocessManualOlsenJ48 = null;
	
	
	
	
	PreProcessManual[] preprocessManualMultilayerPerceptron = null;
	PreProcessManual[] preprocessManualMFMultilayerPerceptron = null;
	PreProcessManual[] preprocessManualOlsenMultilayerPerceptron = null;
	
	PreProcessManual[] preprocessManualNaiveBayes = null;
	PreProcessManual[] preprocessManualMFNaiveBayes= null;
	PreProcessManual[] preprocessManualOlsenNaiveBayes = null;
	
	PreProcessManual[] preprocessManualSMO = null;
	PreProcessManual[] preprocessManualMFSMO= null;
	PreProcessManual[] preprocessManualOlsenSMO = null;
	
	PreProcessManual[] preprocessManualPART = null;
	PreProcessManual[] preprocessManualMFPART= null;
	PreProcessManual[] preprocessManualOlsenPART = null;
	
	PreProcessManual[] preprocessManualRandomForest = null;
	PreProcessManual[] preprocessManualMFRandomForest= null;
	PreProcessManual[] preprocessManualOlsenRandomForest = null;
	
	
	Map<String, Event[]> testEventsArray = new LinkedHashMap<String, Event[]>();
	Map<String, Event[]> testEventsArrayMF = new LinkedHashMap<String, Event[]>();
	Map<String, Event[]> testEventsArrayOlsen = new LinkedHashMap<String, Event[]>();
	

	Map<String, Event[]> trainingEventsArray = new LinkedHashMap<String, Event[]>();
	Map<String, Event[]> trainingEventsArrayMF = new LinkedHashMap<String, Event[]>();
	Map<String, Event[]> trainingEventsArrayOlsen = new LinkedHashMap<String, Event[]>();
	
	static int currentProcessorCounter;
	static Instances testInstance; // This is needed because PreProcess is
									// static

	PreProcess preprocessDummy = null;
	int currentGeneration;

	Double[] training;
	Double[] test;
	DCCurveRegression[] curveCifre;
	DCCurveRegression[] curveMF;
	DCCurveRegression[] curveOlsen;
	
	

	DCCurveRegression[] curveClassifcationMF;
	DCCurveRegression[] curveClassifcation;
	DCCurveRegression[] curveClassificationOlsen;
	
	DCCurveRegression[] curveCifreJ48;
	DCCurveRegression[] curveMFJ48;
	DCCurveRegression[] curveOlsenJ48;
	
	DCCurveRegression[] curveCifreMultilayerPerceptron;
	DCCurveRegression[] curveMFMultilayerPerceptron;
	DCCurveRegression[] curveOlsenMultilayerPerceptron;
	
	//weka.classifiers.bayes.NaiveBayes
		//weka.classifiers.functions.SMO;
		//weka.classifiers.rules.PART;
		//weka.classifiers.trees.RandomForest
	
	DCCurveRegression[] curveCifreNaiveBayes;
	DCCurveRegression[] curveMFNaiveBayes;
	DCCurveRegression[] curveOlsenNaiveBayes;
	
	DCCurveRegression[] curveCifreSMO;
	DCCurveRegression[] curveMFSMO;
	DCCurveRegression[] curveOlsenSMO;
	
	DCCurveRegression[] curveCifrePART;
	DCCurveRegression[] curveMFPART;
	DCCurveRegression[] curveOlsenPART;
	
	DCCurveRegression[] curveCifreRandomForest;
	DCCurveRegression[] curveMFRandomForest;
	DCCurveRegression[] curveOlsenRandomForest;
	
	DCCurveRegression[] curvePerfectForesight;
	DCCurveRegression[] curvePerfectForesightMF;
	DCCurveRegression[] curvePerfectForesightOlsen;
	

	DCCurveRegression[] curveProbabilityGP;
	DCCurveRegression[] curveProbabilityMF;
	DCCurveRegression[] curveProbabilityOlsen;
	
	DCCurveRegression[] curveRandomGP;
	DCCurveRegression[] curveRandomMF;
	DCCurveRegression[] curveRandomOlsen;
	
	DCCurveRegression[] curveNoClassificationNoRegressionGP;
	DCCurveRegression[] curveNoClassificationNoRegressionMF;
	DCCurveRegression[] curveNoClassificationNoRegressionOlsen;
	

	
	//static Map<Double, String> thresholdGPStringMap = new HashMap<Double, String>();
	Map<Double, Double> perfectForecastReturnMap = new HashMap<Double, Double>();
	Map<Double, Double> perfectForecastMFReturnMap = new HashMap<Double, Double>();
	Map<Double, Double> perfectForecastOlsenReturnMap = new HashMap<Double, Double>();
	
	

	// Map<String, Double> CGPReturnMap = new HashMap<String, Double>();
	// to decide whether to split dataset to upward and downward trend datasets
	
	
	
	@SuppressWarnings({ "static-access", "unused" })
	public SymbolicRegression(String filename, int trainingIndexStart, int trainingIndexEnd, int testIndexStart,
			int testIndexEnd, int populationSize, int noOfGENERATIONS, int tournamentSize, double crossOverProb,
			int maxDepth, double threshold) throws IOException, ParseException {

		numberOfThresholds = Const.NUMBER_OF_THRESHOLDS;
		Const.POP_SIZE = populationSize;
		Const.MAX_GENERATIONS = noOfGENERATIONS;
		Const.TOURNAMENT_SIZE = tournamentSize;
		Const.CROSSOVER_PROB = crossOverProb;
		Const.MAX_DEPTH = maxDepth;
		// Const.EVOLUTION_RATIO = reproductionRatio;
		Const.file_Name = filename;

		TrainingDay = trainingIndexStart;
		THRESHOLDS = new double[Const.NUMBER_OF_THRESHOLDS];// Adesola change
															// after tunning to

		gpDaysMap = FReader.loadDataMap(filename);

		for (int i = 0; i < THRESHOLDS.length; i++) {
			// THRESHOLDS[i] = (initial * (i + 1)) / 100.0;
			// THRESHOLDS[i] = (initial + (0.0020 * i)) / 100.0; //Correct one
			// /Right one, replace after test
			THRESHOLDS[i] = (0.005 + (0.0025 * i)) / 100.0;
			String thresholdStr = String.format("%.8f", THRESHOLDS[i]);
			// System.out.println(thresholdStr);
		}

		// System.out.println("Loading directional changes data...");

		// loads the data
		ArrayList<Double[]> days = FReader.loadData(filename, false);

		// allow the creation of training & testing data sets that are longer
		// than 1 day
		ArrayList<Double[]> ar = new ArrayList<Double[]>();
		for (int i = trainingIndexStart; i <= trainingIndexEnd; i++)
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
		for (int i = testIndexStart; i <= testIndexEnd; i++) {
			// System.out.println("Print " + i);
			ar.add(days.get(i));
		}
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

		int timeSeriesDataPt = training.length;
		List<Tick> bidTicks = new ArrayList<>();
		List<Tick> askTicks = new ArrayList<>();

		for (int timeSeriesCount = timeSeriesDataPt; timeSeriesCount < FReader.dataRecordInFileArray.size()
				&& bidTicks.size() < test.length; timeSeriesCount++) {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
			LocalDateTime datetime = LocalDateTime.parse(FReader.dataRecordInFileArray.get(timeSeriesCount).Day + " "
					+ FReader.dataRecordInFileArray.get(timeSeriesCount).time, formatter);
			ZonedDateTime klDateTIme = datetime.atZone(ZoneId.systemDefault());
			bidTicks.add(new BaseTick(klDateTIme, 0.0, 0.0, 0.0,
					Double.parseDouble(FReader.dataRecordInFileArray.get(timeSeriesCount).bidPrice), 0.0));
			askTicks.add(new BaseTick(klDateTIme, 0.0, 0.0, 0.0,
					Double.parseDouble(FReader.dataRecordInFileArray.get(timeSeriesCount).askPrice), 0.0));

			// System.out.println(bidTicks.get(0).toString());
		}

		String fileNameWithoutExtension = filename.split("\\.")[0]; // extension
																	// removed
		TechnicalAnaysisBaseTrading emaTrading = new ExponentialMovingAverageFX(bidTicks, askTicks,
				fileNameWithoutExtension, 500000.00);

		TechnicalAnaysisBaseTrading rsiTrading = new RelativeStrengthIndex(bidTicks, askTicks, fileNameWithoutExtension,
				500000.00);
		TechnicalAnaysisBaseTrading mcadTrading = new MovingMomentum(bidTicks, askTicks, fileNameWithoutExtension,
				500000.00);
		
		TechnicalAnaysisBaseTrading bollingerBAndS = new BolingerBAndS(bidTicks, askTicks, fileNameWithoutExtension,
				500000.00);
		
		TechnicalAnaysisBaseTrading commodityChannelIndexIndicator = new CommodityChannelIndexIndicator(bidTicks, askTicks, fileNameWithoutExtension,
				500000.00);
		
		TechnicalAnaysisBaseTrading simpleMovingAverage = new SimpleMovingAverage(bidTicks, askTicks, fileNameWithoutExtension,
				500000.00);
		
		TechnicalAnaysisBaseTrading parabolicSar = new ParabolicSar(bidTicks, askTicks, fileNameWithoutExtension,
				500000.00);
		
		TechnicalAnaysisBaseTrading aroonIndicator = new AroonIndicator(bidTicks, askTicks, fileNameWithoutExtension,
				500000.00);
		
		TechnicalAnaysisBaseTrading stochOscillatorKIndicator = new StochOscillatorKIndicator(bidTicks, askTicks, fileNameWithoutExtension,
				500000.00);
		
		
		
		TechnicalAnaysisBaseTrading williamR = new WilliamR(bidTicks, askTicks, fileNameWithoutExtension,
				500000.00);
		
		
		
		TechnicalAnaysisBaseTrading priceRateOfChangeIndicator = new PriceRateOfChangeIndicator(bidTicks, askTicks, fileNameWithoutExtension,
				500000.00);
		
		rsiTrading.getOrders();
		mcadTrading.getOrders();
		emaTrading.getOrders();
		bollingerBAndS.getOrders();
		commodityChannelIndexIndicator.getOrders();
		simpleMovingAverage.getOrders();
		parabolicSar.getOrders();
		aroonIndicator.getOrders();
		stochOscillatorKIndicator.getOrders();
		williamR.getOrders();
		priceRateOfChangeIndicator.getOrders();
		
		
		
		double rsiTradeResult = rsiTrading.trade();
		double mcadTradeResult = mcadTrading.trade();
		double emaTradeResult = ((ExponentialMovingAverageFX) emaTrading).trade();
		double bollingerBAndSResult =  bollingerBAndS.trade();
		double commodityChannelIndexIndicatorResult = commodityChannelIndexIndicator.trade();
		double simpleMovingAverageResult =  simpleMovingAverage.trade();
		double parabolicSarResult = parabolicSar.trade();
		double aroonIndicatorResult = aroonIndicator.trade();
		double stochOscillatorKIndicatorResult =  stochOscillatorKIndicator.trade();
		double williamRResult = williamR.trade();
		double priceRateOfChangeIndicatorResult = priceRateOfChangeIndicator.trade();
		
		
		String TechnicalIndicatorMDD = "Dataset \t bollinger \t "
				+ "commodityChannelIndexIndicatorResult \t SMA  "
				+ "\t parabolicSarResult \t aroon \t StochastichOscillatorK \t"
				+ " williamR \t ROC "
				+ "\t RSI \t EMA \t MACD  ";
		
		FWriter TechnicalIndicatorMDDWriter = new FWriter(Const.log.publicFolder + "TechnicalIndicatorMDDBaseCcy.txt");
		Const.log.save("TechnicalIndicatorMDDBaseCcy.txt", TechnicalIndicatorMDD);
		
		for (int printCount = 0  ; printCount < 5 ; printCount++ ){
		
		TechnicalIndicatorMDD = filename + " \t" 
				+ bollingerBAndS.getMaxMddBase() + "\t"
				+ commodityChannelIndexIndicator.getMaxMddBase() + "\t"
				+ simpleMovingAverage.getMaxMddBase() + "\t"
				+ parabolicSar.getMaxMddBase() + "\t"
				+ aroonIndicator.getMaxMddBase() + "\t"
				+ stochOscillatorKIndicator.getMaxMddBase() +"\t"
				+ williamR.getMaxMddBase() + "\t"
				+ priceRateOfChangeIndicator.getMaxMddBase() + "\t"
				+ rsiTrading.getMaxMddBase() + "\t" + emaTrading.getMaxMddBase() + "\t" + mcadTrading.getMaxMddBase();
		Const.log.save("TechnicalIndicatorMDDBaseCcy.txt", TechnicalIndicatorMDD);
		}
		
		String TechnicalIndicatorReturn = "Dataset \t bollinger \t "
				+ "commodityChannelIndexIndicatorResult \t SMA  "
				+ "\t parabolicSarResult \t aroon \t StochastichOscillatorK \t"
				+ " williamR \t ROC "
				+ "\t RSI \t EMA \t MACD  ";
		FWriter TechnicalIndicatorReturnWriter = new FWriter(Const.log.publicFolder + "TechnicalIndicatorSimpleTradingResult.txt");
		Const.log.save("TechnicalIndicatorSimpleTradingResult.txt", TechnicalIndicatorReturn);
		

		
		
		
		DecimalFormat df = new DecimalFormat("#.####");
		df.setRoundingMode(RoundingMode.CEILING);
	
		for (int printCount = 0  ; printCount < 5 ; printCount++ ){
			
		
		TechnicalIndicatorReturn = filename + " \t" 
				+ df.format((( bollingerBAndSResult - 500000)/500000) * 100) + "\t"
				+ df.format(((commodityChannelIndexIndicatorResult - 500000)/500000) * 100) + "\t"
				+ df.format(((simpleMovingAverageResult  - 500000)/500000) * 100 )+ "\t"
				+ df.format(((parabolicSarResult - 500000)/ 500000) * 100) + "\t"
				+ df.format(((aroonIndicatorResult - 500000)/ 500000) * 100) + "\t"
				+ df.format(((stochOscillatorKIndicatorResult  - 500000)/500000) * 100) +"\t"
				+ df.format(((williamRResult - 500000)/500000) * 100) + "\t"
				+ df.format(((priceRateOfChangeIndicatorResult - 500000)/500000) * 100) + "\t"
				+ df.format(((rsiTradeResult - 500000)/500000) * 100) + "\t"
				+ df.format(((emaTradeResult - 500000)/500000) * 100) + "\t"
				+ df.format(((mcadTradeResult - 500000)/500000) * 100);
		Const.log.save("TechnicalIndicatorSimpleTradingResult.txt", TechnicalIndicatorReturn);
		}
		
	
		String TechnicalIndicatorSR = "Dataset \t bollinger \t "
				+ "commodityChannelIndexIndicatorResult \t SMA  "
				+ "\t parabolicSarResult \t aroon \t StochastichOscillatorK \t"
				+ " williamR \t ROC "
				+ "\t RSI \t EMA \t MACD  ";
		
		
		FWriter TechnicalIndicatorSharpeRatioWriter = new FWriter(Const.log.publicFolder + "TechnicalIndicatorSharpeRatioBaseCcy.txt");
		Const.log.save("TechnicalIndicatorSharpeRatio.txt", TechnicalIndicatorSR);
		
		
		for (int printCount = 0  ; printCount < 5 ; printCount++ ){
			
			TechnicalIndicatorSR = filename + " \t" 
					+ bollingerBAndS.getSharpRatio() + "\t"
					+ commodityChannelIndexIndicator.getSharpRatio() + "\t"
					+ simpleMovingAverage.getSharpRatio() + "\t"
					+ parabolicSar.getSharpRatio() + "\t"
					+ aroonIndicator.getSharpRatio() + "\t"
					+ stochOscillatorKIndicator.getSharpRatio() +"\t"
					+ williamR.getSharpRatio() + "\t"
					+ priceRateOfChangeIndicator.getSharpRatio() + "\t"
					+ rsiTrading.getSharpRatio() + "\t" + emaTrading.getSharpRatio() + "\t" + mcadTrading.getSharpRatio();
			Const.log.save("TechnicalIndicatorSharpeRatio.txt", TechnicalIndicatorSR);
			}

		
		

		curvePerfectForesight = new DCCurvePerfectForesight[THRESHOLDS.length];
		curvePerfectForesightMF = new DCCurvePerfectForesightMF[THRESHOLDS.length];
		curvePerfectForesightOlsen = new DCCurvePerfectForesightOlsen[THRESHOLDS.length];
		

		curveCifre = new DCCurveCifre[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		curveClassifcation = new DCCurveClassification[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		curveProbabilityGP = new DCCurveProbabilityGP[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		curveRandomGP = new DCCurveRandomGP[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		
		curveCifreJ48= new DCCurveClassificationManual[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		curveCifreMultilayerPerceptron =  new DCCurveClassificationManual[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		curveCifreNaiveBayes =  new DCCurveClassificationManual[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		curveCifreSMO =  new DCCurveClassificationManual[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		curveCifrePART=  new DCCurveClassificationManual[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		curveCifreRandomForest=  new DCCurveClassificationManual[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		
		curveNoClassificationNoRegressionGP = new DCCurveNoClassificationNoRegression[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		
		
		
		
		curveMF = new DCCurveMF[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		curveClassifcationMF = new DCCurveClassificationMF[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		curveProbabilityMF = new DCCurveProbabilityMF[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		curveRandomMF = new DCCurveRandomMF[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		curveNoClassificationNoRegressionMF = new DCCurveNoClassificationNoRegression[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		curveMFJ48 = new DCCurveClassificationManualMF[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		curveMFMultilayerPerceptron =  new DCCurveClassificationManualMF[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		curveMFNaiveBayes =  new DCCurveClassificationManualMF[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		curveMFSMO =  new DCCurveClassificationManualMF[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		curveMFPART=  new DCCurveClassificationManualMF[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		curveMFRandomForest=  new DCCurveClassificationManualMF[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		
		
		
		curveOlsen = new DCCurveOlsen[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		curveClassificationOlsen = new DCCurveClassificationOlsen[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		curveProbabilityOlsen = new DCCurveProbabilityOlsen[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		curveRandomOlsen = new DCCurveRandomOlsen[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		curveNoClassificationNoRegressionOlsen = new DCCurveNoClassificationNoRegression[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		curveOlsenJ48 = new DCCurveClassificationManualOlsen[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		curveOlsenMultilayerPerceptron =  new DCCurveClassificationManualOlsen[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		curveOlsenNaiveBayes =  new DCCurveClassificationManualOlsen[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		curveOlsenSMO =  new DCCurveClassificationManualOlsen[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		curveOlsenPART=  new DCCurveClassificationManualOlsen[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		curveOlsenRandomForest=  new DCCurveClassificationManualOlsen[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		
		
		preprocess = new PreProcess[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		preprocessMF = new PreProcess[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		preprocessOlsen = new PreProcess[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		
		preprocessManualJ48 = new PreProcessManual[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		preprocessManualMFJ48  = new PreProcessManual[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		preprocessManualOlsenJ48  = new PreProcessManual[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		
		preprocessManualMultilayerPerceptron  = new PreProcessManual[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		preprocessManualMFMultilayerPerceptron  = new PreProcessManual[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		preprocessManualOlsenMultilayerPerceptron  = new PreProcessManual[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		
		preprocessManualNaiveBayes  = new PreProcessManual[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		preprocessManualMFNaiveBayes = new PreProcessManual[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		preprocessManualOlsenNaiveBayes = new PreProcessManual[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		
		preprocessManualSMO = new PreProcessManual[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		preprocessManualMFSMO = new PreProcessManual[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		preprocessManualOlsenSMO = new PreProcessManual[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		
		preprocessManualPART = new PreProcessManual[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		preprocessManualMFPART = new PreProcessManual[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		preprocessManualOlsenPART = new PreProcessManual[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		
		preprocessManualRandomForest = new PreProcessManual[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		preprocessManualMFRandomForest = new PreProcessManual[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		preprocessManualOlsenRandomForest = new PreProcessManual[Const.NUMBER_OF_SELECTED_THRESHOLDS];

		String[] testArrayPerfectForesight = new String[Const.NUMBER_OF_THRESHOLDS];
		String[] testArrayCifre = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		String[] testArrayClassification = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		String[] testArrayProbability = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		String[] testArrayRandom = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		String[] testArrayClassificationNoRegressionGP = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];

		
		String[] testArrayPerfectForesightMF = new String[Const.NUMBER_OF_THRESHOLDS];
		String[] testArrayMF = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		String[] testArrayClassificationMF = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		String[] testArrayProbabilityMF = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		String[] testArrayRandomMF = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		String[] testArrayClassificationNoRegressionMF = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];

		String[] testArrayPerfectForesightOlsen = new String[Const.NUMBER_OF_THRESHOLDS];
		String[] testArrayOlsen = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		String[] testArrayClassificationOlsen = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		String[] testArrayProbabilityOlsen = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		String[] testArrayRandomOlsen = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		String[] testArrayClassificationNoRegressionOlsen = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		
		String[] testArrayClassificationJ48 = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		String[] testArrayClassificationMFJ48 = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		String[] testArrayClassificationOlsenJ48 = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		
		String[] testArrayClassificationMultilayerPerceptron = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		String[] testArrayClassificationMFMultilayerPerceptron = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		String[] testArrayClassificationOlsenMultilayerPerceptron = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		
		
		String[] testArrayClassificationNaiveBayes = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		String[] testArrayClassificationMFNaiveBayes = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		String[] testArrayClassificationOlsenNaiveBayes = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		
		String[] testArrayClassificationSMO = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		String[] testArrayClassificationMFSMO = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		String[] testArrayClassificationOlsenSMO = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		
		String[] testArrayClassificationPART = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		String[] testArrayClassificationMFPART = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		String[] testArrayClassificationOlsenPART = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		
		String[] testArrayClassificationRandomForest = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		String[] testArrayClassificationMFRandomForest = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		String[] testArrayClassificationOlsenRandomForest = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		

		String autoWEKAClassifierListEvalString = "Dataset \t Threshold \t classification_type \t Accuracy \t TP_Rate \t FP_Rate \t Precision \t Recall \t F-Measure \t MCC \t ROC_Area \t PRC_Area  \t Class";

		FWriter autoWEKAClassifierListEvaluation = new FWriter(
				Const.log.publicFolder + "autoWEKAClassifierListEvaluation.txt");

		autoWEKAClassifierListEvaluation = new FWriter(
				Const.log.publicFolder + "autoWEKAClassifierListEvaluationMF.txt");

		autoWEKAClassifierListEvaluation = new FWriter(
				Const.log.publicFolder + "autoWEKAClassifierListEvaluationOlsen.txt");

		
		Const.log.save("autoWEKAClassifierListEvaluation.txt", autoWEKAClassifierListEvalString);
		Const.log.save("autoWEKAClassifierListEvaluationMF.txt", autoWEKAClassifierListEvalString);
		Const.log.save("autoWEKAClassifierListEvaluationOlsen.txt", autoWEKAClassifierListEvalString);
		
			String SimpleTradingTraining = "Dataset \t PerfectForesight+GP \t GP \t Classifier+GP"
					+ "  \t Probability+GP \t GP+DCC \t PerfectForesight+MF \t MF  \t Classifier+MF \t "
					+ "Probability+MF \t MF+DCC  \t PerfectForesight+Olsen \t Olsen \t Classifier+Olsen \t "
					+ "Probability+Olsen \t Olsen+DCC  \t GP+J48";

		FWriter SimpleTradingTrainingWriter = new FWriter(
				Const.log.publicFolder + "SimpleTradingTraining.txt");
		Const.log.save("SimpleTradingTraining.txt", SimpleTradingTraining);

		String selectedThresholds = "Dataset \t ThresholdGP \t zeroOvershootGP  \t  OvershootLengthPercentageGP"
				+ "\t ThresholdMF   \t zeroOvershootMF  \t  OvershootLengthPercentagetMF  "
				+ "\t  ThresholdOlsen  \t zeroOvershootOlsen  \t  OvershootLengthPercentagetOlsen "
				+ "\t  HThresholdH  \t zeroOvershootH  \t  OvershootLengthPercentagetH "
				+ "\t  ThresholdHDDCUDC \t zeroOvershootHDCCUDC  \t  OvershootLengthPercentagetHDCCUDC  ";

		SimpleTradingTrainingWriter = new FWriter(Const.log.publicFolder + "selectedThresholdsTest.txt");
		Const.log.save("selectedThresholdsTest.txt", selectedThresholds);

		SimpleTradingTrainingWriter = new FWriter(
				Const.log.publicFolder + "selectedThresholdsTraining.txt");
		Const.log.save("selectedThresholdsTraining.txt", selectedThresholds);

		Event[] copiedArray;
		
		for (int i = 0; i < THRESHOLDS.length; i++) { // The arrays all have
			// same size

			curvePerfectForesightMF[i] = new DCCurvePerfectForesightMF();

			String thresholdStr = String.format("%.8f", THRESHOLDS[i]);
			String gpFileNamePrefix = gpDaysMap.get(trainingIndexStart);
			String gpFileName = gpFileNamePrefix + "_" + thresholdStr + ".txt";


			DCEventGenerator dCEventGenerator = new DCEventGenerator();
			dCEventGenerator.generateEvents(training, THRESHOLDS[i]);
			copiedArray =Arrays.copyOf(dCEventGenerator.getEvents(), dCEventGenerator.getEvents().length);
			if (copiedArray.length < 10)
				continue;

			
			curvePerfectForesightMF[i].filename = filename;
			curvePerfectForesightMF[i].build(training, THRESHOLDS[i], gpFileName, copiedArray, preprocessDummy);
			
			
			curvePerfectForesightMF[i].estimateTraining(preprocessDummy); // null because not doing classification
			
			curvePerfectForesightMF[i].setNumberOfDCEvent(dCEventGenerator.getNumberOfDCEvent());
			curvePerfectForesightMF[i].setNumberOfOSEvent(dCEventGenerator.getNumberOfOSEvent());
			curvePerfectForesightMF[i].setOsToDcEventRatio(dCEventGenerator.getOsToDcEventRatio());
			curvePerfectForesightMF[i].setAverageDCRunLength(dCEventGenerator.getAverageDCRunLength());
			curvePerfectForesightMF[i].setAverageDownwardDCRunLength(dCEventGenerator.getAverageDownwardDCRunLength());
			curvePerfectForesightMF[i].setAverageUpwardDCRunLength(dCEventGenerator.getAverageUpwardDCRunLength());
				
			
			double perfectForcastTrainingReturn = curvePerfectForesightMF[i].trainingTrading(preprocessDummy);
			perfectForecastMFReturnMap.put(THRESHOLDS[i], perfectForcastTrainingReturn);

		}

		
		for (int runCount = 0; runCount < 1; runCount++) {
			// select threshold via perfect foresight
			for (int i = 0; i < THRESHOLDS.length; i++) { // The arrays all have
															// same size

				curvePerfectForesight[i] = new DCCurvePerfectForesight();

				String thresholdStr = String.format("%.8f", THRESHOLDS[i]);
				String gpFileNamePrefix = gpDaysMap.get(trainingIndexStart);
				String gpFileName = gpFileNamePrefix + "_" + thresholdStr + ".txt";

				DCEventGenerator dCEventGenerator = new DCEventGenerator();
				dCEventGenerator.generateEvents(training, THRESHOLDS[i]);
				copiedArray = Arrays.copyOf(dCEventGenerator.getEvents(), dCEventGenerator.getEvents().length);
				trainingEventsArray.put(thresholdStr, copiedArray);

				if (copiedArray.length < 10)
					continue;

				curvePerfectForesight[i].filename = filename;
				PreProcess preprocessDummy = null;
				curvePerfectForesight[i].build(training, THRESHOLDS[i], gpFileName, copiedArray, preprocessDummy);
				
				curvePerfectForesight[i].estimateTraining(preprocessDummy); // null because // not doing // classification
				
				curvePerfectForesight[i].setNumberOfDCEvent(dCEventGenerator.getNumberOfDCEvent());
				curvePerfectForesight[i].setNumberOfOSEvent(dCEventGenerator.getNumberOfOSEvent());
				curvePerfectForesight[i].setOsToDcEventRatio(dCEventGenerator.getOsToDcEventRatio());
				curvePerfectForesight[i].setAverageDCRunLength(dCEventGenerator.getAverageDCRunLength());
				curvePerfectForesight[i].setAverageDownwardDCRunLength(dCEventGenerator.getAverageDownwardDCRunLength());
				curvePerfectForesight[i].setAverageUpwardDCRunLength(dCEventGenerator.getAverageUpwardDCRunLength());
				
				double perfectForcastTrainingReturn = curvePerfectForesight[i].trainingTrading(preprocessDummy);
				perfectForecastReturnMap.put(THRESHOLDS[i], perfectForcastTrainingReturn);
			}

			// populated threshold of all algorithms
			trainingEventsArray.forEach((k, v) -> {
				trainingEventsArrayMF.put(k, Arrays.copyOf(v, v.length));
				trainingEventsArrayOlsen.put(k, Arrays.copyOf(v, v.length));
			});

			

			for (int i = 0; i < THRESHOLDS.length; i++) { // The arrays all have
				// same size
				curvePerfectForesightOlsen[i] = new DCCurvePerfectForesightOlsen();

				String thresholdStr = String.format("%.8f", THRESHOLDS[i]);
				String gpFileNamePrefix = gpDaysMap.get(trainingIndexStart);
				String gpFileName = gpFileNamePrefix + "_" + thresholdStr + ".txt";


				DCEventGenerator dCEventGenerator = new DCEventGenerator();
				dCEventGenerator.generateEvents(training, THRESHOLDS[i]);
				copiedArray =Arrays.copyOf(dCEventGenerator.getEvents(), dCEventGenerator.getEvents().length);
				if (copiedArray.length < 10)
					continue;
				curvePerfectForesightOlsen[i].filename = filename;
				
				curvePerfectForesightOlsen[i].build(training, THRESHOLDS[i], gpFileName, copiedArray, preprocessDummy);
				curvePerfectForesightOlsen[i].estimateTraining(preprocessDummy); // null // because // not doing // classification
				
				curvePerfectForesightOlsen[i].setNumberOfDCEvent(dCEventGenerator.getNumberOfDCEvent());
				curvePerfectForesightOlsen[i].setNumberOfOSEvent(dCEventGenerator.getNumberOfOSEvent());
				curvePerfectForesightOlsen[i].setOsToDcEventRatio(dCEventGenerator.getOsToDcEventRatio());
				curvePerfectForesightOlsen[i].setAverageDCRunLength(dCEventGenerator.getAverageDCRunLength());
				curvePerfectForesightOlsen[i].setAverageDownwardDCRunLength(dCEventGenerator.getAverageDownwardDCRunLength());
				curvePerfectForesightOlsen[i].setAverageUpwardDCRunLength(dCEventGenerator.getAverageUpwardDCRunLength());

				
				double perfectForcastTrainingReturn = curvePerfectForesightOlsen[i].trainingTrading(preprocessDummy);
				perfectForecastOlsenReturnMap.put(THRESHOLDS[i], perfectForcastTrainingReturn);
			}



			// Select best threshold based on perfect forecast result
			SELECTED_THRESHOLDS = new double[Const.NUMBER_OF_SELECTED_THRESHOLDS];
			SELECTED_THRESHOLDS_MF = new double[Const.NUMBER_OF_SELECTED_THRESHOLDS];
			SELECTED_THRESHOLDS_OLSEN = new double[Const.NUMBER_OF_SELECTED_THRESHOLDS];
			
			SELECTED_THRESHOLDS_CLASSIFICATION_ONLY = new double[Const.NUMBER_OF_SELECTED_THRESHOLDS];
			List<Entry<Double, Double>> greatest = findGreatest(perfectForecastReturnMap,
					Const.NUMBER_OF_SELECTED_THRESHOLDS); // best
			
			
			// 5
			// thresholds

			int tradingThresholdCount = 0;
			System.out.println("Selecting GP threshold");
			for (Entry<Double, Double> entry : greatest) {
				// System.out.println(entry);
				SELECTED_THRESHOLDS[tradingThresholdCount] = entry.getKey();
				System.out.println(SELECTED_THRESHOLDS[tradingThresholdCount]);
				tradingThresholdCount++;
			}

			greatest.clear();
			greatest = findGreatest(perfectForecastMFReturnMap, Const.NUMBER_OF_SELECTED_THRESHOLDS); // best
																										// 5
																										// thresholds
			tradingThresholdCount = 0;
			System.out.println("Selecting MF threshold");
			for (Entry<Double, Double> entry : greatest) {
				// System.out.println(entry);
				SELECTED_THRESHOLDS_MF[tradingThresholdCount] = entry.getKey();
				System.out.println(SELECTED_THRESHOLDS_MF[tradingThresholdCount]);
				tradingThresholdCount++;
			}

			greatest.clear();
			greatest = findGreatest(perfectForecastOlsenReturnMap, Const.NUMBER_OF_SELECTED_THRESHOLDS); // best
			// 5
			// thresholds
			tradingThresholdCount = 0;
			System.out.println("Selecting Olsen threshold");
			for (Entry<Double, Double> entry : greatest) {
				// System.out.println(entry);
				SELECTED_THRESHOLDS_OLSEN[tradingThresholdCount] = entry.getKey();
				System.out.println(SELECTED_THRESHOLDS_OLSEN[tradingThresholdCount]);
				tradingThresholdCount++;
			}

			

			
			// DO classification for all thresholds
			for (int i = 0; i < SELECTED_THRESHOLDS.length; i++) {
				String thresholdStr = String.format("%.8f", SELECTED_THRESHOLDS[i]);

				int arrayLength = trainingEventsArray.get(thresholdStr).length;
				Arrays.copyOf(trainingEventsArray.get(thresholdStr), arrayLength);
				copiedArray = Arrays.copyOf(trainingEventsArray.get(thresholdStr), arrayLength);
				// trainingEventsArray.put(thresholdStr, copiedArray);
				preprocess[i] = null;
				preprocess[i] = new PreProcess(SELECTED_THRESHOLDS[i], filename, "GP");
				
				
				preprocess[i].buildTraining(copiedArray);
				preprocess[i].selectBestClassifier();
				
				preprocess[i].loadTrainingData(copiedArray);
				preprocess[i].lastTrainingEvent = copiedArray[copiedArray.length - 1];
				preprocess[i].runAutoWeka();
				try {
					preprocessManualJ48[i] = new PreProcessManual(SELECTED_THRESHOLDS[i], filename, "GP","weka.classifiers.trees.J48");
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				preprocessManualJ48[i].buildTraining(copiedArray);
				preprocessManualJ48[i].selectBestClassifier();
				preprocessManualJ48[i].loadTrainingData(copiedArray);
				preprocessManualJ48[i].lastTrainingEvent = copiedArray[copiedArray.length - 1];

				try {
					preprocessManualMultilayerPerceptron[i] = new PreProcessManual(SELECTED_THRESHOLDS[i], filename, "GP","weka.classifiers.functions.MultilayerPerceptron");
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
				preprocessManualMultilayerPerceptron[i].buildTraining(copiedArray);
				preprocessManualMultilayerPerceptron[i].selectBestClassifier();			
				preprocessManualMultilayerPerceptron[i].loadTrainingData(copiedArray);
				preprocessManualMultilayerPerceptron[i].lastTrainingEvent = copiedArray[copiedArray.length - 1];
				
				
				
				try {
					preprocessManualNaiveBayes[i] = new PreProcessManual(SELECTED_THRESHOLDS[i], filename, "GP","weka.classifiers.bayes.NaiveBayes");
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
				preprocessManualNaiveBayes[i].buildTraining(copiedArray);
				preprocessManualNaiveBayes[i].selectBestClassifier();			
				preprocessManualNaiveBayes[i].loadTrainingData(copiedArray);
				preprocessManualNaiveBayes[i].lastTrainingEvent = copiedArray[copiedArray.length - 1];	
				
			
				try {
					preprocessManualSMO[i] = new PreProcessManual(SELECTED_THRESHOLDS[i], filename, "GP","weka.classifiers.functions.SMO");
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
				preprocessManualSMO[i].buildTraining(copiedArray);
				preprocessManualSMO[i].selectBestClassifier();			
				preprocessManualSMO[i].loadTrainingData(copiedArray);
				preprocessManualSMO[i].lastTrainingEvent = copiedArray[copiedArray.length - 1];	
				
								

				try {
					preprocessManualPART[i] = new PreProcessManual(SELECTED_THRESHOLDS[i], filename, "GP","weka.classifiers.rules.PART");
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
				preprocessManualPART[i].buildTraining(copiedArray);
				preprocessManualPART[i].selectBestClassifier();			
				preprocessManualPART[i].loadTrainingData(copiedArray);
				preprocessManualPART[i].lastTrainingEvent = copiedArray[copiedArray.length - 1];	
				
				
				try {
					preprocessManualRandomForest[i] = new PreProcessManual(SELECTED_THRESHOLDS[i], filename, "GP","weka.classifiers.trees.RandomForest");
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
				preprocessManualRandomForest[i].buildTraining(copiedArray);
				preprocessManualRandomForest[i].selectBestClassifier();			
				preprocessManualRandomForest[i].loadTrainingData(copiedArray);
				preprocessManualRandomForest[i].lastTrainingEvent = copiedArray[copiedArray.length - 1];	
				
			}

			for (int i = 0; i < SELECTED_THRESHOLDS_MF.length; i++) {
				String thresholdStr = String.format("%.8f", SELECTED_THRESHOLDS_MF[i]);

				copiedArray = Arrays.copyOf(trainingEventsArrayMF.get(thresholdStr),
						trainingEventsArrayMF.get(thresholdStr).length);
				// trainingEventsArrayMF.put(thresholdStr, copiedArray);
				preprocessMF[i] = null;
				preprocessMF[i] = new PreProcess(SELECTED_THRESHOLDS_MF[i], filename, "MF");

				preprocessMF[i].buildTraining(copiedArray);
				preprocessMF[i].selectBestClassifier();
				
				try {
					preprocessManualMFJ48[i] = new PreProcessManual(SELECTED_THRESHOLDS_MF[i], filename, "MF","weka.classifiers.trees.J48");
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				
				preprocessManualMFJ48[i].buildTraining(copiedArray);
				preprocessManualMFJ48[i].selectBestClassifier();

				
				// loadTrainingData load classification data "order is
				// important"
				preprocessMF[i].loadTrainingData(copiedArray);
				preprocessManualMFJ48[i].loadTrainingData(copiedArray);

				preprocessMF[i].lastTrainingEvent = copiedArray[copiedArray.length - 1];
				preprocessManualMFJ48[i].lastTrainingEvent = copiedArray[copiedArray.length - 1];

				preprocessMF[i].runAutoWeka();
				
				
				try {
					preprocessManualMFMultilayerPerceptron[i] = new PreProcessManual(SELECTED_THRESHOLDS_MF[i], filename, "MF","weka.classifiers.functions.MultilayerPerceptron");
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
				preprocessManualMFMultilayerPerceptron[i].buildTraining(copiedArray);
				preprocessManualMFMultilayerPerceptron[i].selectBestClassifier();			
				preprocessManualMFMultilayerPerceptron[i].loadTrainingData(copiedArray);
				preprocessManualMFMultilayerPerceptron[i].lastTrainingEvent = copiedArray[copiedArray.length - 1];
				
				
				try {
					preprocessManualMFNaiveBayes[i] = new PreProcessManual(SELECTED_THRESHOLDS_MF[i], filename, "MF","weka.classifiers.bayes.NaiveBayes");
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
				preprocessManualMFNaiveBayes[i].buildTraining(copiedArray);
				preprocessManualMFNaiveBayes[i].selectBestClassifier();			
				preprocessManualMFNaiveBayes[i].loadTrainingData(copiedArray);
				preprocessManualMFNaiveBayes[i].lastTrainingEvent = copiedArray[copiedArray.length - 1];


				try {
					preprocessManualMFSMO[i] = new PreProcessManual(SELECTED_THRESHOLDS_MF[i], filename, "MF","weka.classifiers.functions.SMO");
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
				preprocessManualMFSMO[i].buildTraining(copiedArray);
				preprocessManualMFSMO[i].selectBestClassifier();			
				preprocessManualMFSMO[i].loadTrainingData(copiedArray);
				preprocessManualMFSMO[i].lastTrainingEvent = copiedArray[copiedArray.length - 1];
				

				try {
					preprocessManualMFPART[i] = new PreProcessManual(SELECTED_THRESHOLDS_MF[i], filename, "MF","weka.classifiers.rules.PART");
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
				preprocessManualMFPART[i].buildTraining(copiedArray);
				preprocessManualMFPART[i].selectBestClassifier();			
				preprocessManualMFPART[i].loadTrainingData(copiedArray);
				preprocessManualMFPART[i].lastTrainingEvent = copiedArray[copiedArray.length - 1];
				
								
				try {
					preprocessManualMFRandomForest[i] = new PreProcessManual(SELECTED_THRESHOLDS_MF[i], filename, "MF","weka.classifiers.trees.RandomForest");
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
				preprocessManualMFRandomForest[i].buildTraining(copiedArray);
				preprocessManualMFRandomForest[i].selectBestClassifier();			
				preprocessManualMFRandomForest[i].loadTrainingData(copiedArray);
				preprocessManualMFRandomForest[i].lastTrainingEvent = copiedArray[copiedArray.length - 1];
						
				
			}

			for (int i = 0; i < SELECTED_THRESHOLDS_OLSEN.length; i++) {
				String thresholdStr = String.format("%.8f", SELECTED_THRESHOLDS_OLSEN[i]);
				copiedArray = Arrays.copyOf(trainingEventsArrayOlsen.get(thresholdStr),
						trainingEventsArrayOlsen.get(thresholdStr).length);
				// trainingEventsArrayOlsen.put(thresholdStr, copiedArray);
				preprocessOlsen[i] = null;
				preprocessOlsen[i] = new PreProcess(SELECTED_THRESHOLDS_OLSEN[i], filename, "Olsen");

				
				try {
					preprocessManualOlsenJ48[i] = new PreProcessManual(SELECTED_THRESHOLDS_OLSEN[i], filename, "Olsen","weka.classifiers.trees.J48");
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				preprocessOlsen[i].buildTraining(copiedArray);
				preprocessOlsen[i].selectBestClassifier();
				
				
				preprocessManualOlsenJ48[i].buildTraining(copiedArray);
				preprocessManualOlsenJ48[i].selectBestClassifier();


				// loadTrainingData load classification data "order is
				// important"
				preprocessOlsen[i].loadTrainingData(copiedArray);
				preprocessManualOlsenJ48[i].loadTrainingData(copiedArray);
				
				preprocessOlsen[i].lastTrainingEvent = copiedArray[copiedArray.length - 1];
				preprocessManualOlsenJ48[i].lastTrainingEvent = copiedArray[copiedArray.length - 1];
				
				
				preprocessOlsen[i].runAutoWeka();
				
				
				try {
					preprocessManualOlsenMultilayerPerceptron[i] = new PreProcessManual(SELECTED_THRESHOLDS_OLSEN[i], filename, "Olsen","weka.classifiers.functions.MultilayerPerceptron");
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
				preprocessManualOlsenMultilayerPerceptron[i].buildTraining(copiedArray);
				preprocessManualOlsenMultilayerPerceptron[i].selectBestClassifier();			
				preprocessManualOlsenMultilayerPerceptron[i].loadTrainingData(copiedArray);
				preprocessManualOlsenMultilayerPerceptron[i].lastTrainingEvent = copiedArray[copiedArray.length - 1];


				try {
					preprocessManualOlsenNaiveBayes[i] = new PreProcessManual(SELECTED_THRESHOLDS_OLSEN[i], filename, "Olsen","weka.classifiers.bayes.NaiveBayes");
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
				preprocessManualOlsenNaiveBayes[i].buildTraining(copiedArray);
				preprocessManualOlsenNaiveBayes[i].selectBestClassifier();			
				preprocessManualOlsenNaiveBayes[i].loadTrainingData(copiedArray);
				preprocessManualOlsenNaiveBayes[i].lastTrainingEvent = copiedArray[copiedArray.length - 1];
				
				
				

				try {
					preprocessManualOlsenSMO[i] = new PreProcessManual(SELECTED_THRESHOLDS_OLSEN[i], filename, "Olsen","weka.classifiers.functions.SMO");
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
				preprocessManualOlsenSMO[i].buildTraining(copiedArray);
				preprocessManualOlsenSMO[i].selectBestClassifier();			
				preprocessManualOlsenSMO[i].loadTrainingData(copiedArray);
				preprocessManualOlsenSMO[i].lastTrainingEvent = copiedArray[copiedArray.length - 1];
				
								
				try {
					preprocessManualOlsenPART[i] = new PreProcessManual(SELECTED_THRESHOLDS_OLSEN[i], filename, "Olsen","weka.classifiers.rules.PART");
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
				preprocessManualOlsenPART[i].buildTraining(copiedArray);
				preprocessManualOlsenPART[i].selectBestClassifier();			
				preprocessManualOlsenPART[i].loadTrainingData(copiedArray);
				preprocessManualOlsenPART[i].lastTrainingEvent = copiedArray[copiedArray.length - 1];
				
								
				
				try {
					preprocessManualOlsenRandomForest[i] = new PreProcessManual(SELECTED_THRESHOLDS_OLSEN[i], filename, "Olsen","weka.classifiers.trees.RandomForest");
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
				preprocessManualOlsenRandomForest[i].buildTraining(copiedArray);
				preprocessManualOlsenRandomForest[i].selectBestClassifier();			
				preprocessManualOlsenRandomForest[i].loadTrainingData(copiedArray);
				preprocessManualOlsenRandomForest[i].lastTrainingEvent = copiedArray[copiedArray.length - 1];
			}

			
			

			if ((SELECTED_THRESHOLDS.length != SELECTED_THRESHOLDS_MF.length)
					|| (SELECTED_THRESHOLDS_MF.length != SELECTED_THRESHOLDS_OLSEN.length)) {
				System.out.println(" SELECTED_THRESHOLDS  ARRAYS are not matching");
				System.exit(-1);
			}

			for (int i = 0; i < SELECTED_THRESHOLDS.length; i++) { // The arrays
																	// all have
																	// same size
				currentProcessorCounter = i;

				curveCifre[i] = new DCCurveCifre();
				curveClassifcation[i] = new DCCurveClassification();
				curveProbabilityGP[i] = new DCCurveProbabilityGP();
				curveRandomGP[i] = new DCCurveRandomGP();
				curveNoClassificationNoRegressionGP[i] = new DCCurveNoClassificationNoRegression();
				
				curveCifreJ48[i] = new DCCurveClassificationManual();
				curveCifreMultilayerPerceptron[i] = new DCCurveClassificationManual(); 
				curveCifreNaiveBayes[i] = new DCCurveClassificationManual();
				curveCifreSMO[i] = new DCCurveClassificationManual();
				curveCifrePART[i] = new DCCurveClassificationManual();
				curveCifreRandomForest[i]= new DCCurveClassificationManual();

				
				curveMF[i] = new DCCurveMF();
				curveClassifcationMF[i] = new DCCurveClassificationMF();
				curveProbabilityMF[i] = new DCCurveProbabilityMF();
				curveRandomMF[i] = new DCCurveRandomMF();
				curveNoClassificationNoRegressionMF[i] = new DCCurveNoClassificationNoRegression();
				
				curveMFJ48[i] = new DCCurveClassificationManualMF();
				curveMFMultilayerPerceptron[i] = new DCCurveClassificationManualMF();
				curveMFNaiveBayes[i] = new DCCurveClassificationManualMF();
				curveMFSMO[i] = new DCCurveClassificationManualMF();
				curveMFPART[i] = new DCCurveClassificationManualMF();
				curveMFRandomForest[i]= new DCCurveClassificationManualMF();
				

				
				curveOlsen[i] = new DCCurveOlsen();
				curveClassificationOlsen[i] = new DCCurveClassificationOlsen();
				curveProbabilityOlsen[i] = new DCCurveProbabilityOlsen();
				curveRandomOlsen[i] = new DCCurveRandomOlsen();
				curveNoClassificationNoRegressionOlsen[i] = new DCCurveNoClassificationNoRegression();
				
				curveOlsenJ48[i] = new DCCurveClassificationManualOlsen();
				curveOlsenMultilayerPerceptron[i] = new DCCurveClassificationManualOlsen();
				curveOlsenNaiveBayes[i] = new DCCurveClassificationManualOlsen();
				curveOlsenSMO[i] = new DCCurveClassificationManualOlsen();
				curveOlsenPART[i] = new DCCurveClassificationManualOlsen();
				curveOlsenRandomForest[i]= new DCCurveClassificationManualOlsen();
				
				

				String thresholdStr = String.format("%.8f", SELECTED_THRESHOLDS[i]);
				String gpFileNamePrefix = gpDaysMap.get(trainingIndexStart);
				String gpFileName = gpFileNamePrefix + "_" + thresholdStr + ".txt";

				// System.out.println(thresholdStr);

				// System.out.println("GA:" + gpFileNamePrefix);
				String regressionModelGP[] = new String[2];
				int numOfDCEvent=0;
				int numOfOSEvent=0;
				double osToDcEventRatio=0.0;
				double aveDCRunLength = 0.0;
				double aveDCDownwardLength = 0.0;
				double aveDCUpwardLength = 0.0;
				
				for (int thresholdCounter = 0; thresholdCounter < curvePerfectForesight.length; thresholdCounter++) {
					String thisThresholdStr = String.format("%.8f", SELECTED_THRESHOLDS[i]);
					if (thisThresholdStr
							.equalsIgnoreCase(curvePerfectForesight[thresholdCounter].getThresholdString())) {
						regressionModelGP[0] = curvePerfectForesight[thresholdCounter].getDownwardTrendTreeString();
						regressionModelGP[1] = curvePerfectForesight[thresholdCounter].getUpwardTrendTreeString();
						numOfDCEvent =  curvePerfectForesight[thresholdCounter].getNumberOfDCEvent();
						numOfOSEvent = curvePerfectForesight[thresholdCounter].getNumberOFOSEvent();
						osToDcEventRatio = curvePerfectForesight[thresholdCounter].getOsToDcEventRatio();
						aveDCRunLength = curvePerfectForesight[thresholdCounter].getAverageDCRunLength();
						aveDCDownwardLength = curvePerfectForesight[thresholdCounter].getAverageDownwardDCRunLength();
						aveDCUpwardLength = curvePerfectForesight[thresholdCounter].getAverageUpwardDCRunLength();
						
						break;
					}
				}

				curveCifre[i].filename = filename;
				
				// Assign perfect foresight regression Model here
				if (regressionModelGP[0] != null && !regressionModelGP[0].isEmpty() && regressionModelGP[1] != null
						&& !regressionModelGP[1].isEmpty())
					curveCifre[i].assignPerfectForesightRegressionModel(regressionModelGP);

				curveCifre[i].build(training, SELECTED_THRESHOLDS[i], gpFileName, Arrays.copyOf(
						trainingEventsArray.get(thresholdStr), trainingEventsArray.get(thresholdStr).length), preprocessDummy);
				curveCifre[i].estimateTraining(preprocessDummy);

				curveClassifcation[i].filename = filename;
				// Assign perfect foresight regression Model here
				if (regressionModelGP[0] != null && !regressionModelGP[0].isEmpty() && regressionModelGP[1] != null
						&& !regressionModelGP[1].isEmpty())
					curveClassifcation[i].assignPerfectForesightRegressionModel(regressionModelGP);

				curveClassifcation[i].build(training, SELECTED_THRESHOLDS[i], gpFileName, Arrays
						.copyOf(trainingEventsArray.get(thresholdStr), trainingEventsArray.get(thresholdStr).length),
						preprocess[i]);
				curveClassifcation[i].estimateTraining(preprocess[i]);

				
				curveCifreJ48[i].filename = filename;
				if (regressionModelGP[0] != null && !regressionModelGP[0].isEmpty() && regressionModelGP[1] != null
						&& !regressionModelGP[1].isEmpty())
					curveCifreJ48[i].assignPerfectForesightRegressionModel(regressionModelGP);
				

				curveCifreJ48[i].buildManual(training, SELECTED_THRESHOLDS[i], gpFileName, Arrays
						.copyOf(trainingEventsArray.get(thresholdStr), trainingEventsArray.get(thresholdStr).length),
						preprocessManualJ48[i]);
				curveCifreJ48[i].estimateTraining(preprocessManualJ48[i]);
				
				
				
				
				curveCifreMultilayerPerceptron[i].filename = filename;
				if (regressionModelGP[0] != null && !regressionModelGP[0].isEmpty() && regressionModelGP[1] != null
						&& !regressionModelGP[1].isEmpty())
					curveCifreMultilayerPerceptron[i].assignPerfectForesightRegressionModel(regressionModelGP);
				

				curveCifreMultilayerPerceptron[i].buildManual(training, SELECTED_THRESHOLDS[i], gpFileName, Arrays
						.copyOf(trainingEventsArray.get(thresholdStr), trainingEventsArray.get(thresholdStr).length),
						preprocessManualMultilayerPerceptron[i]);
				curveCifreMultilayerPerceptron[i].estimateTraining(preprocessManualMultilayerPerceptron[i]);
				
				
				curveCifreNaiveBayes[i].filename = filename;
				if (regressionModelGP[0] != null && !regressionModelGP[0].isEmpty() && regressionModelGP[1] != null
						&& !regressionModelGP[1].isEmpty())
					curveCifreNaiveBayes[i].assignPerfectForesightRegressionModel(regressionModelGP);
				

				curveCifreNaiveBayes[i].buildManual(training, SELECTED_THRESHOLDS[i], gpFileName, Arrays
						.copyOf(trainingEventsArray.get(thresholdStr), trainingEventsArray.get(thresholdStr).length),
						preprocessManualNaiveBayes[i]);
				curveCifreNaiveBayes[i].estimateTraining(preprocessManualNaiveBayes[i]);
			
				curveCifreSMO[i].filename = filename;
				if (regressionModelGP[0] != null && !regressionModelGP[0].isEmpty() && regressionModelGP[1] != null
						&& !regressionModelGP[1].isEmpty())
					curveCifreSMO[i].assignPerfectForesightRegressionModel(regressionModelGP);
				

				curveCifreSMO[i].buildManual(training, SELECTED_THRESHOLDS[i], gpFileName, Arrays
						.copyOf(trainingEventsArray.get(thresholdStr), trainingEventsArray.get(thresholdStr).length),
						preprocessManualSMO[i]);
				curveCifreSMO[i].estimateTraining(preprocessManualSMO[i]);
			
				
				curveCifrePART[i].filename = filename;
				if (regressionModelGP[0] != null && !regressionModelGP[0].isEmpty() && regressionModelGP[1] != null
						&& !regressionModelGP[1].isEmpty())
					curveCifrePART[i].assignPerfectForesightRegressionModel(regressionModelGP);
				

				curveCifrePART[i].buildManual(training, SELECTED_THRESHOLDS[i], gpFileName, Arrays
						.copyOf(trainingEventsArray.get(thresholdStr), trainingEventsArray.get(thresholdStr).length),
						preprocessManualPART[i]);
				curveCifrePART[i].estimateTraining(preprocessManualPART[i]);
				
				
				curveCifreRandomForest[i].filename = filename;
				if (regressionModelGP[0] != null && !regressionModelGP[0].isEmpty() && regressionModelGP[1] != null
						&& !regressionModelGP[1].isEmpty())
					curveCifreRandomForest[i].assignPerfectForesightRegressionModel(regressionModelGP);
				

				curveCifreRandomForest[i].buildManual(training, SELECTED_THRESHOLDS[i], gpFileName, Arrays
						.copyOf(trainingEventsArray.get(thresholdStr), trainingEventsArray.get(thresholdStr).length),
						preprocessManualRandomForest[i]);
				curveCifreRandomForest[i].estimateTraining(preprocessManualRandomForest[i]);
				
				
				
				
				curveProbabilityGP[i].filename = filename;
				if (regressionModelGP[0] != null && !regressionModelGP[0].isEmpty() && regressionModelGP[1] != null
						&& !regressionModelGP[1].isEmpty())
					curveProbabilityGP[i].assignPerfectForesightRegressionModel(regressionModelGP);
				curveProbabilityGP[i].build(training, SELECTED_THRESHOLDS[i], gpFileName, Arrays.copyOf(
						trainingEventsArray.get(thresholdStr), trainingEventsArray.get(thresholdStr).length), preprocessDummy);
				curveProbabilityGP[i].estimateTraining(preprocessDummy);
				curveProbabilityGP[i].setNumberOfDCEvent(numOfDCEvent);
				curveProbabilityGP[i].setNumberOfOSEvent(numOfOSEvent);
				curveProbabilityGP[i].setOsToDcEventRatio(osToDcEventRatio);
				curveProbabilityGP[i].setAverageDCRunLength(aveDCRunLength);
				curveProbabilityGP[i].setAverageDownwardDCRunLength(aveDCDownwardLength);
				curveProbabilityGP[i].setAverageUpwardDCRunLength(aveDCUpwardLength);
				
				
				curveRandomGP[i].filename = filename;
				if (regressionModelGP[0] != null && !regressionModelGP[0].isEmpty() && regressionModelGP[1] != null
						&& !regressionModelGP[1].isEmpty())
					curveRandomGP[i].assignPerfectForesightRegressionModel(regressionModelGP);
				curveRandomGP[i].build(training, SELECTED_THRESHOLDS[i], gpFileName, Arrays.copyOf(
						trainingEventsArray.get(thresholdStr), trainingEventsArray.get(thresholdStr).length), preprocessDummy);
				curveRandomGP[i].estimateTraining(preprocessDummy);


				

				curveNoClassificationNoRegressionGP[i].filename = filename;
				
				curveNoClassificationNoRegressionGP[i].build(training, SELECTED_THRESHOLDS[i], gpFileName, Arrays
						.copyOf(trainingEventsArray.get(thresholdStr), trainingEventsArray.get(thresholdStr).length),
						preprocessDummy);
				curveNoClassificationNoRegressionGP[i].estimateTraining(preprocessDummy);

				thresholdStr = String.format("%.8f", SELECTED_THRESHOLDS_MF[i]);
				Double regressionModelMF[] = new Double[2];
				regressionModelMF[0] = new Double(0.0);
				regressionModelMF[1] = new Double(0.0);
				
				int numOfDCEventMF=0;
				int numOfOSEventMF=0;
				double osToDcEventRatioMF=0.0;
				double aveDCRunLengthMF = 0.0;
				double aveDCDownwardLengthMF = 0.0;
				double aveDCUpwardLengthMF = 0.0;

				for (int thresholdCounter = 0; thresholdCounter < curvePerfectForesightMF.length; thresholdCounter++) {
					String thisThresholdStr = String.format("%.8f", SELECTED_THRESHOLDS_MF[i]);
					if (thisThresholdStr
							.equalsIgnoreCase(curvePerfectForesightMF[thresholdCounter].getThresholdString())) {
						regressionModelMF[0] = new Double(
								curvePerfectForesightMF[thresholdCounter].getDownwardTrendRatio());
						regressionModelMF[1] = new Double(
								curvePerfectForesightMF[thresholdCounter].getUpwardTrendRatio());
						numOfDCEventMF =  curvePerfectForesightMF[thresholdCounter].getNumberOfDCEvent();
						numOfOSEventMF = curvePerfectForesightMF[thresholdCounter].getNumberOFOSEvent();
						osToDcEventRatioMF = curvePerfectForesightMF[thresholdCounter].getOsToDcEventRatio();
						aveDCRunLengthMF = curvePerfectForesightMF[thresholdCounter].getAverageDCRunLength();
						aveDCDownwardLengthMF = curvePerfectForesightMF[thresholdCounter].getAverageDownwardDCRunLength();
						aveDCUpwardLengthMF = curvePerfectForesightMF[thresholdCounter].getAverageUpwardDCRunLength();
						break;
					}
				}

				curveMF[i].filename = filename;
				// Assign perfect foresight regression Model here
				if (regressionModelMF[0].doubleValue() > 0.0 && regressionModelMF[1].doubleValue() > 0.0)
					curveMF[i].assignPerfectForesightRegressionModel(regressionModelMF);

				curveMF[i].build(training, SELECTED_THRESHOLDS_MF[i], gpFileName, Arrays.copyOf(
						trainingEventsArrayMF.get(thresholdStr), trainingEventsArrayMF.get(thresholdStr).length), preprocessDummy);
				curveMF[i].estimateTraining(preprocessDummy);

				curveClassifcationMF[i].filename = filename;
				// Assign perfect foresight regression Model here
				if (regressionModelMF[0].doubleValue() > 0.0 && regressionModelMF[1].doubleValue() > 0.0)
					curveClassifcationMF[i].assignPerfectForesightRegressionModel(regressionModelMF);

				curveClassifcationMF[i].build(training, SELECTED_THRESHOLDS_MF[i], gpFileName,
						Arrays.copyOf(trainingEventsArrayMF.get(thresholdStr),
								trainingEventsArrayMF.get(thresholdStr).length),
						preprocessMF[i]);
				curveClassifcationMF[i].estimateTraining(preprocessMF[i]);
				
				curveMFJ48[i].filename = filename;
				if (regressionModelMF[0].doubleValue() > 0.0 && regressionModelMF[1].doubleValue() > 0.0)
					curveMFJ48[i].assignPerfectForesightRegressionModel(regressionModelMF);
				

				curveMFJ48[i].buildManual(training, SELECTED_THRESHOLDS_MF[i], gpFileName, Arrays
						.copyOf(trainingEventsArrayMF.get(thresholdStr), trainingEventsArrayMF.get(thresholdStr).length),
						preprocessManualMFJ48[i]);
				curveMFJ48[i].estimateTraining(preprocessManualMFJ48[i]);
				
				
				
				curveMFMultilayerPerceptron[i].filename = filename;
				if (regressionModelMF[0].doubleValue() > 0.0 && regressionModelMF[1].doubleValue() > 0.0)
					curveMFMultilayerPerceptron[i].assignPerfectForesightRegressionModel(regressionModelMF);
				
				curveMFMultilayerPerceptron[i].buildManual(training, SELECTED_THRESHOLDS_MF[i], gpFileName, Arrays
						.copyOf(trainingEventsArrayMF.get(thresholdStr), trainingEventsArrayMF.get(thresholdStr).length),
						preprocessManualMFMultilayerPerceptron[i]);
				curveMFMultilayerPerceptron[i].estimateTraining(preprocessManualMFMultilayerPerceptron[i]);
			
				

				
				curveMFNaiveBayes[i].filename = filename;
				if (regressionModelMF[0].doubleValue() > 0.0 && regressionModelMF[1].doubleValue() > 0.0)
					curveMFNaiveBayes[i].assignPerfectForesightRegressionModel(regressionModelMF);
				curveMFNaiveBayes[i].buildManual(training, SELECTED_THRESHOLDS_MF[i], gpFileName, Arrays
						.copyOf(trainingEventsArrayMF.get(thresholdStr), trainingEventsArrayMF.get(thresholdStr).length),
						preprocessManualMFNaiveBayes[i]);
				curveMFNaiveBayes[i].estimateTraining(preprocessManualMFNaiveBayes[i]);
				
				
				curveMFSMO[i].filename = filename;
				if (regressionModelMF[0].doubleValue() > 0.0 && regressionModelMF[1].doubleValue() > 0.0)
					curveMFSMO[i].assignPerfectForesightRegressionModel(regressionModelMF);
				curveMFSMO[i].buildManual(training, SELECTED_THRESHOLDS_MF[i], gpFileName, Arrays
						.copyOf(trainingEventsArrayMF.get(thresholdStr), trainingEventsArrayMF.get(thresholdStr).length),
						preprocessManualMFSMO[i]);
				curveMFSMO[i].estimateTraining(preprocessManualMFSMO[i]);
				
				
				curveMFPART[i].filename = filename;
				if (regressionModelMF[0].doubleValue() > 0.0 && regressionModelMF[1].doubleValue() > 0.0)
					curveMFPART[i].assignPerfectForesightRegressionModel(regressionModelMF);
				curveMFPART[i].buildManual(training, SELECTED_THRESHOLDS_MF[i], gpFileName, Arrays
						.copyOf(trainingEventsArrayMF.get(thresholdStr), trainingEventsArrayMF.get(thresholdStr).length),
						preprocessManualMFPART[i]);
				curveMFPART[i].estimateTraining(preprocessManualMFPART[i]);
				
				
				curveMFRandomForest[i].filename = filename;
				if (regressionModelMF[0].doubleValue() > 0.0 && regressionModelMF[1].doubleValue() > 0.0)
					curveMFRandomForest[i].assignPerfectForesightRegressionModel(regressionModelMF);
				curveMFRandomForest[i].buildManual(training, SELECTED_THRESHOLDS_MF[i], gpFileName, Arrays
						.copyOf(trainingEventsArrayMF.get(thresholdStr), trainingEventsArrayMF.get(thresholdStr).length),
						preprocessManualMFRandomForest[i]);
				curveMFRandomForest[i].estimateTraining(preprocessManualMFRandomForest[i]);
				
				
				
				curveProbabilityMF[i].filename = filename;
				// Assign perfect foresight regression Model here
				 if (regressionModelMF[0].doubleValue() > 0.0 && regressionModelMF[1].doubleValue()> 0.0)
					 curveProbabilityMF[i].assignPerfectForesightRegressionModel(regressionModelMF);

				curveProbabilityMF[i].build(training, SELECTED_THRESHOLDS_MF[i], gpFileName, Arrays.copyOf(
						trainingEventsArrayMF.get(thresholdStr), trainingEventsArrayMF.get(thresholdStr).length), preprocessDummy);
				curveProbabilityMF[i].estimateTraining(preprocessDummy);
				curveProbabilityMF[i].setNumberOfDCEvent(numOfDCEventMF);
				curveProbabilityMF[i].setNumberOfOSEvent(numOfOSEventMF);
				curveProbabilityMF[i].setOsToDcEventRatio(osToDcEventRatioMF);
				curveProbabilityMF[i].setAverageDCRunLength(aveDCRunLengthMF);
				curveProbabilityMF[i].setAverageDownwardDCRunLength(aveDCDownwardLengthMF);
				curveProbabilityMF[i].setAverageUpwardDCRunLength(aveDCUpwardLengthMF);

				curveRandomMF[i].filename = filename;
				
				 if (regressionModelMF[0].doubleValue() > 0.0 && regressionModelMF[1].doubleValue()> 0.0)
					 curveRandomMF[i].assignPerfectForesightRegressionModel(regressionModelMF);

				 curveRandomMF[i].build(training, SELECTED_THRESHOLDS_MF[i], gpFileName, Arrays.copyOf(
						trainingEventsArrayMF.get(thresholdStr), trainingEventsArrayMF.get(thresholdStr).length), preprocessDummy);

				
				curveNoClassificationNoRegressionMF[i].filename = filename;
				curveNoClassificationNoRegressionMF[i].build(training, SELECTED_THRESHOLDS_MF[i], gpFileName,
						Arrays.copyOf(trainingEventsArrayMF.get(thresholdStr),
								trainingEventsArrayMF.get(thresholdStr).length),
						preprocessDummy);
				curveNoClassificationNoRegressionMF[i].estimateTraining(preprocessDummy);

				thresholdStr = String.format("%.8f", SELECTED_THRESHOLDS_OLSEN[i]);
				Double regressionModelOlsen[] = new Double[1];
				regressionModelOlsen[0] = new Double(0.0);
				int numOfDCEventOlsen=0;
				int numOfOSEventOlsen=0;
				double osToDcEventRatioOlsen=0.0;
				double aveDCRunLengthOlsen = 0.0;
				double aveDCDownwardLengthOlsen = 0.0;
				double aveDCUpwardLengthOlsen = 0.0;

				for (int thresholdCounter = 0; thresholdCounter < curvePerfectForesightOlsen.length; thresholdCounter++) {
					String thisThresholdStr = String.format("%.8f", SELECTED_THRESHOLDS_OLSEN[i]);
					if (thisThresholdStr
							.equalsIgnoreCase(curvePerfectForesightOlsen[thresholdCounter].getThresholdString())) {
						regressionModelOlsen[0] = new Double(
								curvePerfectForesightOlsen[thresholdCounter].getSingleRatio());
						
						numOfDCEventOlsen =  curvePerfectForesightOlsen[thresholdCounter].getNumberOfDCEvent();
						numOfOSEventOlsen = curvePerfectForesightOlsen[thresholdCounter].getNumberOFOSEvent();
						osToDcEventRatioOlsen = curvePerfectForesightOlsen[thresholdCounter].getOsToDcEventRatio();
						aveDCRunLengthOlsen = curvePerfectForesightOlsen[thresholdCounter].getAverageDCRunLength();
						aveDCDownwardLengthOlsen = curvePerfectForesightOlsen[thresholdCounter].getAverageDownwardDCRunLength();
						aveDCUpwardLengthOlsen = curvePerfectForesightOlsen[thresholdCounter].getAverageUpwardDCRunLength();
						
						break;

					}
				}

				curveOlsen[i].filename = filename;
				// Assign perfect foresight regression Model here
				if (regressionModelOlsen[0].doubleValue() > 0.0)
					curveOlsen[i].assignPerfectForesightRegressionModel(regressionModelOlsen);

				curveOlsen[i].build(training, SELECTED_THRESHOLDS_OLSEN[i], gpFileName,
						Arrays.copyOf(trainingEventsArrayOlsen.get(thresholdStr),
								trainingEventsArrayOlsen.get(thresholdStr).length),
						preprocessDummy);
				curveOlsen[i].estimateTraining(preprocessDummy);

				curveClassificationOlsen[i].filename = filename;
				// Assign perfect foresight regression Model here
				if (regressionModelOlsen[0].doubleValue() > 0.0)
					curveClassificationOlsen[i].assignPerfectForesightRegressionModel(regressionModelOlsen);

				curveClassificationOlsen[i].build(training, SELECTED_THRESHOLDS_OLSEN[i], gpFileName,
						Arrays.copyOf(trainingEventsArrayOlsen.get(thresholdStr),
								trainingEventsArrayOlsen.get(thresholdStr).length),
						preprocessOlsen[i]);
				curveClassificationOlsen[i].estimateTraining(preprocessOlsen[i]);
				
				
				curveOlsenJ48[i].filename = filename;
				if (regressionModelOlsen[0].doubleValue() > 0.0)
					curveOlsenJ48[i].assignPerfectForesightRegressionModel(regressionModelOlsen);

				curveOlsenJ48[i].buildManual(training, SELECTED_THRESHOLDS_OLSEN[i], gpFileName, Arrays
						.copyOf(trainingEventsArrayOlsen.get(thresholdStr), trainingEventsArrayOlsen.get(thresholdStr).length),
						preprocessManualOlsenJ48[i]);
				curveOlsenJ48[i].estimateTraining(preprocessManualOlsenJ48[i]);
				

				curveOlsenMultilayerPerceptron[i].filename = filename;
				if (regressionModelOlsen[0].doubleValue() > 0.0)
					curveOlsenMultilayerPerceptron[i].assignPerfectForesightRegressionModel(regressionModelOlsen);

				curveOlsenMultilayerPerceptron[i].buildManual(training, SELECTED_THRESHOLDS_OLSEN[i], gpFileName, Arrays
						.copyOf(trainingEventsArrayOlsen.get(thresholdStr), trainingEventsArrayOlsen.get(thresholdStr).length),
						preprocessManualOlsenMultilayerPerceptron[i]);
				curveOlsenMultilayerPerceptron[i].estimateTraining(preprocessManualOlsenMultilayerPerceptron[i]);
			
				
				curveOlsenNaiveBayes[i].filename = filename;
				if (regressionModelOlsen[0].doubleValue() > 0.0)
					curveOlsenNaiveBayes[i].assignPerfectForesightRegressionModel(regressionModelOlsen);

				curveOlsenNaiveBayes[i].buildManual(training, SELECTED_THRESHOLDS_OLSEN[i], gpFileName, Arrays
						.copyOf(trainingEventsArrayOlsen.get(thresholdStr), trainingEventsArrayOlsen.get(thresholdStr).length),
						preprocessManualOlsenNaiveBayes[i]);
				curveOlsenNaiveBayes[i].estimateTraining(preprocessManualOlsenNaiveBayes[i]);
				
				curveOlsenSMO[i].filename = filename;
				if (regressionModelOlsen[0].doubleValue() > 0.0)
					curveOlsenSMO[i].assignPerfectForesightRegressionModel(regressionModelOlsen);

				curveOlsenSMO[i].buildManual(training, SELECTED_THRESHOLDS_OLSEN[i], gpFileName, Arrays
						.copyOf(trainingEventsArrayOlsen.get(thresholdStr), trainingEventsArrayOlsen.get(thresholdStr).length),
						preprocessManualOlsenSMO[i]);
				curveOlsenSMO[i].estimateTraining(preprocessManualOlsenSMO[i]);
				
				curveOlsenPART[i].filename = filename;
				if (regressionModelOlsen[0].doubleValue() > 0.0)
					curveOlsenPART[i].assignPerfectForesightRegressionModel(regressionModelOlsen);

				curveOlsenPART[i].buildManual(training, SELECTED_THRESHOLDS_OLSEN[i], gpFileName, Arrays
						.copyOf(trainingEventsArrayOlsen.get(thresholdStr), trainingEventsArrayOlsen.get(thresholdStr).length),
						preprocessManualOlsenPART[i]);
				curveOlsenPART[i].estimateTraining(preprocessManualOlsenPART[i]);
				
				
				curveOlsenRandomForest[i].filename = filename;
				if (regressionModelOlsen[0].doubleValue() > 0.0)
					curveOlsenRandomForest[i].assignPerfectForesightRegressionModel(regressionModelOlsen);

				curveOlsenRandomForest[i].buildManual(training, SELECTED_THRESHOLDS_OLSEN[i], gpFileName, Arrays
						.copyOf(trainingEventsArrayOlsen.get(thresholdStr), trainingEventsArrayOlsen.get(thresholdStr).length),
						preprocessManualOlsenRandomForest[i]);
				curveOlsenRandomForest[i].estimateTraining(preprocessManualOlsenRandomForest[i]);
				
				
				curveProbabilityOlsen[i].filename = filename;
				// Assign perfect foresight regression Model here
				 if (regressionModelOlsen[0].doubleValue() > 0.0)
					 curveProbabilityOlsen[i].assignPerfectForesightRegressionModel(regressionModelOlsen);

				curveProbabilityOlsen[i].build(training, SELECTED_THRESHOLDS_OLSEN[i], gpFileName,
						Arrays.copyOf(trainingEventsArrayOlsen.get(thresholdStr),
								trainingEventsArrayOlsen.get(thresholdStr).length),
						preprocessDummy);
				curveProbabilityOlsen[i].estimateTraining(preprocessDummy);
				curveProbabilityOlsen[i].setNumberOfDCEvent(numOfDCEventOlsen);
				curveProbabilityOlsen[i].setNumberOfOSEvent(numOfOSEventOlsen);
				curveProbabilityOlsen[i].setOsToDcEventRatio(osToDcEventRatioOlsen);
				curveProbabilityOlsen[i].setAverageDCRunLength(aveDCRunLengthOlsen);
				curveProbabilityOlsen[i].setAverageDownwardDCRunLength(aveDCDownwardLengthOlsen);
				curveProbabilityOlsen[i].setAverageUpwardDCRunLength(aveDCUpwardLengthOlsen);

				
				curveRandomOlsen[i].filename = filename;
				// Assign perfect foresight regression Model here
				 if (regressionModelOlsen[0].doubleValue() > 0.0)
					 curveRandomOlsen[i].assignPerfectForesightRegressionModel(regressionModelOlsen);

				 curveRandomOlsen[i].build(training, SELECTED_THRESHOLDS_OLSEN[i], gpFileName,Arrays.copyOf(trainingEventsArrayOlsen.get(thresholdStr),
								trainingEventsArrayOlsen.get(thresholdStr).length),
						preprocessDummy);
				
				
				curveNoClassificationNoRegressionOlsen[i].filename = filename;
				curveNoClassificationNoRegressionOlsen[i].build(training, SELECTED_THRESHOLDS_OLSEN[i], gpFileName,
						Arrays.copyOf(trainingEventsArrayOlsen.get(thresholdStr),
								trainingEventsArrayOlsen.get(thresholdStr).length),
						preprocessDummy);
				curveNoClassificationNoRegressionOlsen[i].estimateTraining(preprocessDummy);

				
				// perfectForecastReturnMap from previous loop
				double perfectForcastTrainingReturn = perfectForecastReturnMap.get(SELECTED_THRESHOLDS[i]);
				double perfectForcastMFTrainingReturn = perfectForecastMFReturnMap.get(SELECTED_THRESHOLDS_MF[i]);
				double perfectForcastOlsenTrainingReturn = perfectForecastOlsenReturnMap
						.get(SELECTED_THRESHOLDS_OLSEN[i]);
				
				double curveCifreTrainingReturn = curveCifre[i].trainingTrading(preprocessDummy);
				double curveMFTrainingReturn = curveMF[i].trainingTrading(preprocessDummy);
				double curveOlsenTrainingReturn = curveOlsen[i].trainingTrading(preprocessDummy);
				
				double curveClassifcationTrainingReturn = curveClassifcation[i].trainingTrading(preprocess[i]);
				double curveClassifcationMFTrainingReturn = curveClassifcationMF[i].trainingTrading(preprocessMF[i]);
				double curveClassificationOlsenTrainingReturn = curveClassificationOlsen[i].trainingTrading(preprocessOlsen[i]);
				
				double curveCifreJ48TrainingReturn = curveCifreJ48[i].trainingTrading(preprocessManualJ48[i]);
				double curveMFJ48TrainingReturn = curveMFJ48[i].trainingTrading(preprocessManualMFJ48[i]);
				double curveOlsenJ48TrainingReturn = curveOlsenJ48[i].trainingTrading(preprocessManualOlsenJ48[i]);
				
				
				double curveCifreNaiveBayesTrainingReturn = curveCifreNaiveBayes[i].trainingTrading(preprocessManualNaiveBayes[i]);
				double curveMFNaiveBayesTrainingReturn = curveMFNaiveBayes[i].trainingTrading(preprocessManualMFNaiveBayes[i]);
				double curveOlsenNaiveBayesTrainingReturn = curveOlsenNaiveBayes[i].trainingTrading(preprocessManualOlsenNaiveBayes[i]);
				
				
				double curveCifreMultilayerPerceptronTrainingReturn = curveCifreMultilayerPerceptron[i].trainingTrading(preprocessManualMultilayerPerceptron[i]);
				double curveMFMultilayerPerceptronTrainingReturn = curveMFMultilayerPerceptron[i].trainingTrading(preprocessManualMFMultilayerPerceptron[i]);
				double curveOlsenMultilayerPerceptronTrainingReturn = curveOlsenMultilayerPerceptron[i].trainingTrading(preprocessManualOlsenMultilayerPerceptron[i]);
				
				double curveCifreSMOTrainingReturn = curveCifreSMO[i].trainingTrading(preprocessManualSMO[i]);
				double curveMFSMOTrainingReturn = curveMFSMO[i].trainingTrading(preprocessManualMFSMO[i]);
				double curveOlsenSMOTrainingReturn = curveOlsenSMO[i].trainingTrading(preprocessManualOlsenSMO[i]);
				
				double curveCifrePARTTrainingReturn = curveCifrePART[i].trainingTrading(preprocessManualPART[i]);
				double curveMFPARTTrainingReturn = curveMFPART[i].trainingTrading(preprocessManualMFPART[i]);
				double curveOlsenPARTTrainingReturn = curveOlsenPART[i].trainingTrading(preprocessManualOlsenPART[i]);
				
				double curveCifreRandomForestTrainingReturn = curveCifreRandomForest[i].trainingTrading(preprocessManualRandomForest[i]);
				double curveMFRandomForestTrainingReturn = curveMFRandomForest[i].trainingTrading(preprocessManualMFRandomForest[i]);
				double curveOlsenRandomForestTrainingReturn = curveOlsenRandomForest[i].trainingTrading(preprocessManualOlsenRandomForest[i]);
				
				double curveProbabilityTrainingReturn = curveProbabilityGP[i].trainingTrading(preprocessDummy);
				double curveProbabilityMFTrainingReturn = curveProbabilityMF[i].trainingTrading(preprocessDummy);
				double curveProbabilityOlsenTrainingReturn = curveProbabilityOlsen[i].trainingTrading(preprocessDummy);
				
				double curveRandomTrainingReturn = curveRandomGP[i].trainingTrading(preprocessDummy);
				double curveRandomMFTrainingReturn = curveRandomMF[i].trainingTrading(preprocessDummy);
				double curveRandomOlsenTrainingReturn = curveRandomOlsen[i].trainingTrading(preprocessDummy);
				
				

				double curveClassificationNoRegressionTrainingReturn = curveNoClassificationNoRegressionGP[i]
						.trainingTrading(preprocessDummy);
				double curveClassificationNoRegressionMFTrainingReturn = curveNoClassificationNoRegressionMF[i]
						.trainingTrading(preprocessDummy);
				
				double curveClassificationNoRegressionOlsenTrainingReturn = curveNoClassificationNoRegressionOlsen[i]
						.trainingTrading(preprocessDummy);
				
				SimpleTradingTraining = filename + " \t" + thresholdStr + " \t " + perfectForcastTrainingReturn + "\t"
						+ curveCifreTrainingReturn + "\t" + curveClassifcationTrainingReturn + "\t"
						+ curveProbabilityTrainingReturn + "\t" + curveClassificationNoRegressionTrainingReturn + "\t" 
						+ perfectForcastMFTrainingReturn + "\t" + curveMFTrainingReturn + "\t"
						+ curveClassifcationMFTrainingReturn + "\t" + curveProbabilityMFTrainingReturn + "\t"
						+ curveClassificationNoRegressionMFTrainingReturn + "\t" + perfectForcastOlsenTrainingReturn
						+ "\t" + curveOlsenTrainingReturn + "\t" + curveClassificationOlsenTrainingReturn + "\t"
						+ curveProbabilityOlsenTrainingReturn + "\t" + curveClassificationNoRegressionOlsenTrainingReturn + "\t" 
						+ curveCifreJ48TrainingReturn + "\t" + curveMFJ48TrainingReturn + "\t" + curveOlsenJ48TrainingReturn + "\t"
						+ curveCifreMultilayerPerceptronTrainingReturn + "\t" + curveMFMultilayerPerceptronTrainingReturn + "\t" + curveOlsenMultilayerPerceptronTrainingReturn  + "\t"
						+ curveCifreNaiveBayesTrainingReturn + "\t" + curveMFNaiveBayesTrainingReturn + "\t" + curveOlsenNaiveBayesTrainingReturn + "\t"
						+ curveCifreSMOTrainingReturn + "\t" + curveMFSMOTrainingReturn + "\t" + curveOlsenSMOTrainingReturn + "\t"
						+ curveCifrePARTTrainingReturn + "\t" + curveMFPARTTrainingReturn + "\t" + curveOlsenPARTTrainingReturn + "\t"
						+ curveCifreRandomForestTrainingReturn + "\t" + curveMFRandomForestTrainingReturn + "\t" + curveOlsenRandomForestTrainingReturn + "\t" 
						+ curveRandomTrainingReturn + "\t" + curveRandomMFTrainingReturn + "\t" + curveRandomOlsenTrainingReturn;
				
				
				
				Const.log.save("SimpleTradingTraining.txt", SimpleTradingTraining);

				Const.log.save("autoWEKAClassifierListEvaluation.txt", preprocess[i].getAutoWEKAClassifierListEvalString());
				Const.log.save("autoWEKAClassifierListEvaluationMF.txt",
						preprocessMF[i].getAutoWEKAClassifierListEvalString());
				Const.log.save("autoWEKAClassifierListEvaluationOlsen.txt",
						preprocessOlsen[i].getAutoWEKAClassifierListEvalString());
				
				// curveClassifcation[i].preprocess.clearAutoWEKAClassifierListEvalString();
				// CGPReturnMap.put(thresholdStr,
				// curveClassifcationTrainingReturn);

				// cleanup
				String tempFolderName = preprocess[i].tempFilePath.get(0).substring(0,
						preprocess[i].tempFilePath.get(0).lastIndexOf(File.separator));

				File dir = new File(tempFolderName);
				if (!dir.isDirectory())
					continue;

				String fileNames = filename.substring(filename.lastIndexOf(File.separator) + 1, filename.length() - 4);

				File[] tempFile = dir.getParentFile().listFiles(new FilenameFilter() {
					public boolean accept(File dir, String name) {
						return name.startsWith("autoweka" + fileNames);
					}
				});

				for (int tempFileCount = 0; tempFileCount < tempFile.length; tempFileCount++) {
					try {
						preprocess[i].deleteDirectoryRecursionJava6(tempFile[tempFileCount]);
					} catch (IOException e) {

						System.out.println("Unable to delete one of the directory");
					}
				}

			} // for (int i = 0; i < SELECTED_THRESHOLDS.length; i++) {

			// TODO Do above loop for each regregression algo

			// reset currentProcessorCounter
			currentProcessorCounter = 0;
			for (int testBuildCount = 0; testBuildCount < SELECTED_THRESHOLDS.length; testBuildCount++) {
				String thresholdStr = String.format("%.8f", SELECTED_THRESHOLDS[testBuildCount]);

				String gpFileNamePrefix = gpDaysMap.get(TrainingDay); // This is
																		// just
																		// to
																		// get
																		// folder
																		// locations
				DCEventGenerator dCEventGenerator = new DCEventGenerator();
				dCEventGenerator.generateEvents(this.test, SELECTED_THRESHOLDS[testBuildCount]);

				Event[] copiedTestArray = Arrays.copyOf(dCEventGenerator.getEvents(),
						dCEventGenerator.getEvents().length);

				testEventsArray.put(thresholdStr, copiedTestArray);

				preprocess[testBuildCount].lastTestingEvent = copiedTestArray[copiedTestArray.length - 1];

				preprocess[testBuildCount].buildTest(copiedTestArray);
				System.out.println("About to print test data for threshold " + SELECTED_THRESHOLDS[testBuildCount]);
				preprocess[testBuildCount].processTestData(copiedTestArray);

				preprocess[testBuildCount].loadTestData(copiedTestArray);

				preprocess[testBuildCount].classifyTestData();
				
				preprocessManualJ48[testBuildCount].lastTestingEvent = copiedTestArray[copiedTestArray.length - 1];
				preprocessManualJ48[testBuildCount].buildTest(copiedTestArray);
				System.out.println("About to print test data for threshold " + SELECTED_THRESHOLDS[testBuildCount]);
				preprocessManualJ48[testBuildCount].processTestData(copiedTestArray);
				preprocessManualJ48[testBuildCount].loadTestData(copiedTestArray);
				preprocessManualJ48[testBuildCount].classifyTestData();
				
				
				
				
				preprocessManualMultilayerPerceptron[testBuildCount].lastTestingEvent = copiedTestArray[copiedTestArray.length - 1];
				preprocessManualMultilayerPerceptron[testBuildCount].buildTest(copiedTestArray);
				System.out.println("About to print test data for threshold " + SELECTED_THRESHOLDS[testBuildCount]);
				preprocessManualMultilayerPerceptron[testBuildCount].processTestData(copiedTestArray);
				preprocessManualMultilayerPerceptron[testBuildCount].loadTestData(copiedTestArray);
				preprocessManualMultilayerPerceptron[testBuildCount].classifyTestData();
				
				

				
				preprocessManualNaiveBayes[testBuildCount].lastTestingEvent = copiedTestArray[copiedTestArray.length - 1];
				preprocessManualNaiveBayes[testBuildCount].buildTest(copiedTestArray);
				System.out.println("About to print test data for threshold " + SELECTED_THRESHOLDS[testBuildCount]);
				preprocessManualNaiveBayes[testBuildCount].processTestData(copiedTestArray);
				preprocessManualNaiveBayes[testBuildCount].loadTestData(copiedTestArray);
				preprocessManualNaiveBayes[testBuildCount].classifyTestData();
				
				preprocessManualSMO[testBuildCount].lastTestingEvent = copiedTestArray[copiedTestArray.length - 1];
				preprocessManualSMO[testBuildCount].buildTest(copiedTestArray);
				preprocessManualSMO[testBuildCount].processTestData(copiedTestArray);
				preprocessManualSMO[testBuildCount].loadTestData(copiedTestArray);
				preprocessManualSMO[testBuildCount].classifyTestData();
				
				
				preprocessManualPART[testBuildCount].lastTestingEvent = copiedTestArray[copiedTestArray.length - 1];
				preprocessManualPART[testBuildCount].buildTest(copiedTestArray);
				preprocessManualPART[testBuildCount].processTestData(copiedTestArray);
				preprocessManualPART[testBuildCount].loadTestData(copiedTestArray);
				preprocessManualPART[testBuildCount].classifyTestData();
				
				preprocessManualRandomForest[testBuildCount].lastTestingEvent = copiedTestArray[copiedTestArray.length - 1];
				preprocessManualRandomForest[testBuildCount].buildTest(copiedTestArray);
				preprocessManualRandomForest[testBuildCount].processTestData(copiedTestArray);
				preprocessManualRandomForest[testBuildCount].loadTestData(copiedTestArray);
				preprocessManualRandomForest[testBuildCount].classifyTestData();
				
				
				
				System.out.println("About to print test data for threshold " + SELECTED_THRESHOLDS[testBuildCount]);

				curvePerfectForesight[testBuildCount].testbuild(training.length, this.test,
						SELECTED_THRESHOLDS[testBuildCount], copiedTestArray, preprocessDummy);

				curveCifre[testBuildCount].testbuild(training.length, this.test, SELECTED_THRESHOLDS[testBuildCount],
						copiedTestArray, preprocessDummy);

				curveClassifcation[testBuildCount].testbuild(training.length, this.test,
						SELECTED_THRESHOLDS[testBuildCount], copiedTestArray, preprocess[testBuildCount]);

				
				curveProbabilityGP[testBuildCount].testbuild(training.length, this.test, SELECTED_THRESHOLDS[testBuildCount],
						copiedTestArray, preprocessDummy);
				
				curveProbabilityGP[testBuildCount].setNumberOfDCEventTest(dCEventGenerator.getNumberOfDCEvent());
				curveProbabilityGP[testBuildCount].setNumberOfOSEventTest(dCEventGenerator.getNumberOfOSEvent());
				curveProbabilityGP[testBuildCount].setOsToDcEventRatioTest(dCEventGenerator.getOsToDcEventRatio());
				curveProbabilityGP[testBuildCount].setAverageDCRunLengthTest(dCEventGenerator.getAverageDCRunLength());
				curveProbabilityGP[testBuildCount].setAverageDownwardDCRunLengthTest(dCEventGenerator.getAverageDownwardDCRunLength());
				curveProbabilityGP[testBuildCount].setAverageUpwardDCRunLengthTest(dCEventGenerator.getAverageUpwardDCRunLength());
				
				

				curveRandomGP[testBuildCount].testbuild(training.length, this.test, SELECTED_THRESHOLDS[testBuildCount],
						copiedTestArray, preprocessDummy);
				
				curveNoClassificationNoRegressionGP[testBuildCount].testbuild(training.length, this.test,
						SELECTED_THRESHOLDS[testBuildCount], copiedTestArray, preprocessDummy);

				curveCifreJ48[testBuildCount].testbuildManual(training.length, this.test,
						SELECTED_THRESHOLDS[testBuildCount], copiedTestArray, preprocessManualJ48[testBuildCount]);
				
				
				curveCifreMultilayerPerceptron[testBuildCount].testbuildManual(training.length, this.test,
						SELECTED_THRESHOLDS[testBuildCount], copiedTestArray, preprocessManualMultilayerPerceptron[testBuildCount]);
				
				
				curveCifreNaiveBayes[testBuildCount].testbuildManual(training.length, this.test,
						SELECTED_THRESHOLDS[testBuildCount], copiedTestArray, preprocessManualNaiveBayes[testBuildCount]);
				
				curveCifreSMO[testBuildCount].testbuildManual(training.length, this.test,
						SELECTED_THRESHOLDS[testBuildCount], copiedTestArray, preprocessManualSMO[testBuildCount]);
				
				curveCifrePART[testBuildCount].testbuildManual(training.length, this.test,
						SELECTED_THRESHOLDS[testBuildCount], copiedTestArray, preprocessManualPART[testBuildCount]);
				
				curveCifreRandomForest[testBuildCount].testbuildManual(training.length, this.test,
						SELECTED_THRESHOLDS[testBuildCount], copiedTestArray, preprocessManualRandomForest[testBuildCount]);
				
				currentProcessorCounter = testBuildCount;
			} // testing GP

			currentProcessorCounter = 0;
			for (int testBuildCount = 0; testBuildCount < SELECTED_THRESHOLDS_MF.length; testBuildCount++) {
				String thresholdStr = String.format("%.8f", SELECTED_THRESHOLDS_MF[testBuildCount]);

				String gpFileNamePrefix = gpDaysMap.get(TrainingDay); // This is
																		// just
																		// to
																		// get
																		// folder
																		// locations
				DCEventGenerator dCEventGenerator = new DCEventGenerator();
				dCEventGenerator.generateEvents(this.test, SELECTED_THRESHOLDS_MF[testBuildCount]);

				Event[] copiedTestArray = Arrays.copyOf(dCEventGenerator.getEvents(),
						dCEventGenerator.getEvents().length);

				testEventsArrayMF.put(thresholdStr, copiedTestArray);

				preprocessMF[testBuildCount].lastTestingEvent = copiedTestArray[copiedTestArray.length - 1];

				preprocessMF[testBuildCount].buildTest(copiedTestArray);
				System.out.println("About to print test data for threshold " + SELECTED_THRESHOLDS_MF[testBuildCount]);
				preprocessMF[testBuildCount].processTestData(copiedTestArray);

				preprocessMF[testBuildCount].loadTestData(copiedTestArray);

				preprocessMF[testBuildCount].classifyTestData();
				
				
				preprocessManualMFJ48[testBuildCount].lastTestingEvent = copiedTestArray[copiedTestArray.length - 1];

				preprocessManualMFJ48[testBuildCount].buildTest(copiedTestArray);
				System.out.println("About to print test data for threshold " + SELECTED_THRESHOLDS_MF[testBuildCount]);
				preprocessManualMFJ48[testBuildCount].processTestData(copiedTestArray);

				preprocessManualMFJ48[testBuildCount].loadTestData(copiedTestArray);

				preprocessManualMFJ48[testBuildCount].classifyTestData();

				
				
				preprocessManualMFMultilayerPerceptron[testBuildCount].lastTestingEvent = copiedTestArray[copiedTestArray.length - 1];

				preprocessManualMFMultilayerPerceptron[testBuildCount].buildTest(copiedTestArray);
				System.out.println("About to print test data for threshold " + SELECTED_THRESHOLDS_MF[testBuildCount]);
				preprocessManualMFMultilayerPerceptron[testBuildCount].processTestData(copiedTestArray);

				preprocessManualMFMultilayerPerceptron[testBuildCount].loadTestData(copiedTestArray);

				preprocessManualMFMultilayerPerceptron[testBuildCount].classifyTestData();
				

				
				preprocessManualMFNaiveBayes[testBuildCount].lastTestingEvent = copiedTestArray[copiedTestArray.length - 1];
				preprocessManualMFNaiveBayes[testBuildCount].buildTest(copiedTestArray);
				preprocessManualMFNaiveBayes[testBuildCount].processTestData(copiedTestArray);
				preprocessManualMFNaiveBayes[testBuildCount].loadTestData(copiedTestArray);
				preprocessManualMFNaiveBayes[testBuildCount].classifyTestData();
				
				preprocessManualMFSMO[testBuildCount].lastTestingEvent = copiedTestArray[copiedTestArray.length - 1];
				preprocessManualMFSMO[testBuildCount].buildTest(copiedTestArray);
				//System.out.println("About to print test data for threshold " + SELECTED_THRESHOLDS_MF[testBuildCount]);
				preprocessManualMFSMO[testBuildCount].processTestData(copiedTestArray);
				preprocessManualMFSMO[testBuildCount].loadTestData(copiedTestArray);
				preprocessManualMFSMO[testBuildCount].classifyTestData();
				
				preprocessManualMFPART[testBuildCount].lastTestingEvent = copiedTestArray[copiedTestArray.length - 1];
				preprocessManualMFPART[testBuildCount].buildTest(copiedTestArray);
				//System.out.println("About to print test data for threshold " + SELECTED_THRESHOLDS_MF[testBuildCount]);
				preprocessManualMFPART[testBuildCount].processTestData(copiedTestArray);
				preprocessManualMFPART[testBuildCount].loadTestData(copiedTestArray);
				preprocessManualMFPART[testBuildCount].classifyTestData();
				
				preprocessManualMFRandomForest[testBuildCount].lastTestingEvent = copiedTestArray[copiedTestArray.length - 1];
				preprocessManualMFRandomForest[testBuildCount].buildTest(copiedTestArray);
				//System.out.println("About to print test data for threshold " + SELECTED_THRESHOLDS_MF[testBuildCount]);
				preprocessManualMFRandomForest[testBuildCount].processTestData(copiedTestArray);
				preprocessManualMFRandomForest[testBuildCount].loadTestData(copiedTestArray);
				preprocessManualMFRandomForest[testBuildCount].classifyTestData();
				
				// testInstance =
				// preprocess[testBuildCount].getCopyOfTestInstances();

				System.out
						.println("About to print test data for MF threshold " + SELECTED_THRESHOLDS_MF[testBuildCount]);

				curveMF[testBuildCount].testbuild(training.length, this.test, SELECTED_THRESHOLDS_MF[testBuildCount],
						copiedTestArray, preprocessDummy);

				curveClassifcationMF[testBuildCount].testbuild(training.length, this.test,
						SELECTED_THRESHOLDS_MF[testBuildCount], copiedTestArray, preprocessMF[testBuildCount]);

				curvePerfectForesightMF[testBuildCount].testbuild(training.length, this.test,
						SELECTED_THRESHOLDS_MF[testBuildCount], copiedTestArray, preprocessDummy);

				curveProbabilityMF[testBuildCount].testbuild(training.length, this.test,
						SELECTED_THRESHOLDS_MF[testBuildCount], copiedTestArray, preprocessDummy);
				
				curveProbabilityMF[testBuildCount].setNumberOfDCEventTest(dCEventGenerator.getNumberOfDCEvent());
				curveProbabilityMF[testBuildCount].setNumberOfOSEventTest(dCEventGenerator.getNumberOfOSEvent());
				curveProbabilityMF[testBuildCount].setOsToDcEventRatioTest(dCEventGenerator.getOsToDcEventRatio());
				curveProbabilityMF[testBuildCount].setAverageDCRunLengthTest(dCEventGenerator.getAverageDCRunLength());
				curveProbabilityMF[testBuildCount].setAverageDownwardDCRunLengthTest(dCEventGenerator.getAverageDownwardDCRunLength());
				curveProbabilityMF[testBuildCount].setAverageUpwardDCRunLengthTest(dCEventGenerator.getAverageUpwardDCRunLength());

				curveRandomMF[testBuildCount].testbuild(training.length, this.test,
						SELECTED_THRESHOLDS_MF[testBuildCount], copiedTestArray, preprocessDummy);
				
				
				curveNoClassificationNoRegressionMF[testBuildCount].testbuild(training.length, this.test,
						SELECTED_THRESHOLDS_MF[testBuildCount], copiedTestArray, preprocessDummy);

				curveMFJ48[testBuildCount].testbuildManual(training.length, this.test,
						SELECTED_THRESHOLDS_MF[testBuildCount], copiedTestArray, preprocessManualMFJ48[testBuildCount]);
				
				curveMFMultilayerPerceptron[testBuildCount].testbuildManual(training.length, this.test,
						SELECTED_THRESHOLDS_MF[testBuildCount], copiedTestArray, preprocessManualMFMultilayerPerceptron[testBuildCount]);
				
				
				curveMFNaiveBayes[testBuildCount].testbuildManual(training.length, this.test,
						SELECTED_THRESHOLDS_MF[testBuildCount], copiedTestArray, preprocessManualMFNaiveBayes[testBuildCount]);
				
				curveMFSMO[testBuildCount].testbuildManual(training.length, this.test,
						SELECTED_THRESHOLDS_MF[testBuildCount], copiedTestArray, preprocessManualMFSMO[testBuildCount]);
				
				
				curveMFPART[testBuildCount].testbuildManual(training.length, this.test,
						SELECTED_THRESHOLDS_MF[testBuildCount], copiedTestArray, preprocessManualMFPART[testBuildCount]);
				
				curveMFRandomForest[testBuildCount].testbuildManual(training.length, this.test,
						SELECTED_THRESHOLDS_MF[testBuildCount], copiedTestArray, preprocessManualMFRandomForest[testBuildCount]);
				
				currentProcessorCounter = testBuildCount;
			} // testing MF

			currentProcessorCounter = 0;
			for (int testBuildCount = 0; testBuildCount < SELECTED_THRESHOLDS_OLSEN.length; testBuildCount++) {
				String thresholdStr = String.format("%.8f", SELECTED_THRESHOLDS_OLSEN[testBuildCount]);

				String gpFileNamePrefix = gpDaysMap.get(TrainingDay);
				DCEventGenerator dCEventGenerator = new DCEventGenerator();
				dCEventGenerator.generateEvents(this.test, SELECTED_THRESHOLDS_OLSEN[testBuildCount]);

				Event[] copiedTestArray = Arrays.copyOf(dCEventGenerator.getEvents(),
						dCEventGenerator.getEvents().length);

				testEventsArrayOlsen.put(thresholdStr, copiedTestArray);

				preprocessOlsen[testBuildCount].lastTestingEvent = copiedTestArray[copiedTestArray.length - 1];
				preprocessOlsen[testBuildCount].buildTest(copiedTestArray);
				System.out.println(
						"About to print test data for Olsen threshold " + SELECTED_THRESHOLDS_OLSEN[testBuildCount]);
				preprocessOlsen[testBuildCount].processTestData(copiedTestArray);
				preprocessOlsen[testBuildCount].loadTestData(copiedTestArray);
				preprocessOlsen[testBuildCount].classifyTestData();

				

				preprocessManualOlsenJ48[testBuildCount].lastTestingEvent = copiedTestArray[copiedTestArray.length - 1];
				preprocessManualOlsenJ48[testBuildCount].buildTest(copiedTestArray);
				System.out.println(
						"About to print test data for Olsen threshold " + SELECTED_THRESHOLDS_OLSEN[testBuildCount]);
				preprocessManualOlsenJ48[testBuildCount].processTestData(copiedTestArray);
				preprocessManualOlsenJ48[testBuildCount].loadTestData(copiedTestArray);
				preprocessManualOlsenJ48[testBuildCount].classifyTestData();

				
	
				preprocessManualOlsenMultilayerPerceptron[testBuildCount].lastTestingEvent = copiedTestArray[copiedTestArray.length - 1];
				preprocessManualOlsenMultilayerPerceptron[testBuildCount].buildTest(copiedTestArray);
				System.out.println(
						"About to print test data for Olsen threshold " + SELECTED_THRESHOLDS_OLSEN[testBuildCount]);
				preprocessManualOlsenMultilayerPerceptron[testBuildCount].processTestData(copiedTestArray);
				preprocessManualOlsenMultilayerPerceptron[testBuildCount].loadTestData(copiedTestArray);
				preprocessManualOlsenMultilayerPerceptron[testBuildCount].classifyTestData();
				
				
				
				preprocessManualOlsenNaiveBayes[testBuildCount].lastTestingEvent = copiedTestArray[copiedTestArray.length - 1];
				preprocessManualOlsenNaiveBayes[testBuildCount].buildTest(copiedTestArray);
				preprocessManualOlsenNaiveBayes[testBuildCount].processTestData(copiedTestArray);
				preprocessManualOlsenNaiveBayes[testBuildCount].loadTestData(copiedTestArray);
				preprocessManualOlsenNaiveBayes[testBuildCount].classifyTestData();
				
				preprocessManualOlsenSMO[testBuildCount].lastTestingEvent = copiedTestArray[copiedTestArray.length - 1];
				preprocessManualOlsenSMO[testBuildCount].buildTest(copiedTestArray);
				preprocessManualOlsenSMO[testBuildCount].processTestData(copiedTestArray);
				preprocessManualOlsenSMO[testBuildCount].loadTestData(copiedTestArray);
				preprocessManualOlsenSMO[testBuildCount].classifyTestData();
				
				
				preprocessManualOlsenPART[testBuildCount].lastTestingEvent = copiedTestArray[copiedTestArray.length - 1];
				preprocessManualOlsenPART[testBuildCount].buildTest(copiedTestArray);
				preprocessManualOlsenPART[testBuildCount].processTestData(copiedTestArray);
				preprocessManualOlsenPART[testBuildCount].loadTestData(copiedTestArray);
				preprocessManualOlsenPART[testBuildCount].classifyTestData();
				
				preprocessManualOlsenRandomForest[testBuildCount].lastTestingEvent = copiedTestArray[copiedTestArray.length - 1];
				preprocessManualOlsenRandomForest[testBuildCount].buildTest(copiedTestArray);
				preprocessManualOlsenRandomForest[testBuildCount].processTestData(copiedTestArray);
				preprocessManualOlsenRandomForest[testBuildCount].loadTestData(copiedTestArray);
				preprocessManualOlsenRandomForest[testBuildCount].classifyTestData();
				
				curveOlsen[testBuildCount].testbuild(training.length, this.test,
						SELECTED_THRESHOLDS_OLSEN[testBuildCount], copiedTestArray, preprocessDummy);

				curveClassificationOlsen[testBuildCount].testbuild(training.length, this.test,
						SELECTED_THRESHOLDS_OLSEN[testBuildCount], copiedTestArray, 
						preprocessOlsen[testBuildCount]);

				curvePerfectForesightOlsen[testBuildCount].testbuild(training.length, this.test,
						SELECTED_THRESHOLDS_OLSEN[testBuildCount], copiedTestArray, preprocessDummy);

				curveProbabilityOlsen[testBuildCount].testbuild(training.length, this.test,
						SELECTED_THRESHOLDS_OLSEN[testBuildCount], copiedTestArray, preprocessDummy);
				
				curveProbabilityOlsen[testBuildCount].setNumberOfDCEventTest(dCEventGenerator.getNumberOfDCEvent());
				curveProbabilityOlsen[testBuildCount].setNumberOfOSEventTest(dCEventGenerator.getNumberOfOSEvent());
				curveProbabilityOlsen[testBuildCount].setOsToDcEventRatioTest(dCEventGenerator.getOsToDcEventRatio());
				curveProbabilityOlsen[testBuildCount].setAverageDCRunLengthTest(dCEventGenerator.getAverageDCRunLength());
				curveProbabilityOlsen[testBuildCount].setAverageDownwardDCRunLengthTest(dCEventGenerator.getAverageDownwardDCRunLength());
				curveProbabilityOlsen[testBuildCount].setAverageUpwardDCRunLengthTest(dCEventGenerator.getAverageUpwardDCRunLength());

				
				curveNoClassificationNoRegressionOlsen[testBuildCount].testbuild(training.length, this.test,
						SELECTED_THRESHOLDS_OLSEN[testBuildCount], copiedTestArray, preprocessDummy);

				curveOlsenJ48[testBuildCount].testbuildManual(training.length, this.test,
						SELECTED_THRESHOLDS_OLSEN[testBuildCount], copiedTestArray, preprocessManualOlsenJ48[testBuildCount]);
				
				
				curveOlsenMultilayerPerceptron[testBuildCount].testbuildManual(training.length, this.test,
						SELECTED_THRESHOLDS_OLSEN[testBuildCount], copiedTestArray, preprocessManualOlsenMultilayerPerceptron[testBuildCount]);
				
				curveRandomOlsen[testBuildCount].testbuild(training.length, this.test,
						SELECTED_THRESHOLDS_OLSEN[testBuildCount], copiedTestArray, preprocessDummy);
				
				curveOlsenNaiveBayes[testBuildCount].testbuildManual(training.length, this.test,
						SELECTED_THRESHOLDS_OLSEN[testBuildCount], copiedTestArray, preprocessManualOlsenNaiveBayes[testBuildCount]);
				
				curveOlsenSMO[testBuildCount].testbuildManual(training.length, this.test,
						SELECTED_THRESHOLDS_OLSEN[testBuildCount], copiedTestArray, preprocessManualOlsenSMO[testBuildCount]);
				
				curveOlsenPART[testBuildCount].testbuildManual(training.length, this.test,
						SELECTED_THRESHOLDS_OLSEN[testBuildCount], copiedTestArray, preprocessManualOlsenPART[testBuildCount]);
				
				curveOlsenRandomForest[testBuildCount].testbuildManual(training.length, this.test,
						SELECTED_THRESHOLDS_OLSEN[testBuildCount], copiedTestArray, preprocessManualOlsenRandomForest[testBuildCount]);
				
				currentProcessorCounter = testBuildCount;
			} // testing Olsen

			currentProcessorCounter = 0;
			
			currentProcessorCounter = 0;
			

			String regressionresult = "Dataset   \t "
					+ "Classifier+GP \t GP \t probability+GP \t J48+GP   \t MultilayerPerceptron+GP \t NaiveBayes+GP \t SMO+GP \t PART+GP \t RandomForest+GP\t Random+GP\t" 
					+ "Classifier+MF \t MF \t probability+MF \t J48+MF \t  MultilayerPerceptron+MF  \t NaiveBayes+MF \t SMO+MF \tPART+MF \t RandomForest+MF \t Random+MF\t"
					+ "Classifier+Olsen \t Olsen  \t probability+Olsen \t J48+Olsen \t MultilayerPerceptron+olsen \t NaiveBayes+Olsen \t SMO+Olsen \t PART+Olsen  \t RandomForest+Olsen \t Random+Olsen";
			System.out.println(regressionresult);
			FWriter writer = new FWriter(Const.log.publicFolder + "RegressionAnalysisCurves.txt");
			Const.log.save("RegressionAnalysisCurves.txt", regressionresult);

			String classificationresult = "Dataset \t Threshold \t TotalMissedOvershoot \t TotalMissedOvershootLength \t TotalAssumedOvershoot \t PossibleOvershoot \t FoundOvershootLength \t TotalFoundOvershoot  \t totalDcEvent \t testAccuracy \t testPrecision \t testRecall";

			FWriter Classicationwriter = new FWriter(
					Const.log.publicFolder + "ClassificationAnalysis.txt");
			Const.log.save("ClassificationAnalysis.txt", classificationresult);

			Classicationwriter = new FWriter(
					Const.log.publicFolder + "ClassificationAnalysisJ48.txt");
			Const.log.save("ClassificationAnalysisJ48.txt", classificationresult);
			
			Classicationwriter = new FWriter(
					Const.log.publicFolder + "ClassificationAnalysisMultilayerPerceptron.txt");
			Const.log.save("ClassificationAnalysisMultilayerPerceptron.txt", classificationresult);
			
			Classicationwriter = new FWriter(
					Const.log.publicFolder + "ClassificationAnalysisNaiveBayes.txt");
			Const.log.save("ClassificationAnalysisNaiveBayes.txt", classificationresult);
			
			Classicationwriter = new FWriter(
					Const.log.publicFolder + "ClassificationAnalysisSMO.txt");
			Const.log.save("ClassificationAnalysisSMO.txt", classificationresult);
			
			Classicationwriter = new FWriter(
					Const.log.publicFolder + "ClassificationAnalysisPART.txt");
			Const.log.save("ClassificationAnalysisPART.txt", classificationresult);
			
			Classicationwriter = new FWriter(
					Const.log.publicFolder + "ClassificationAnalysisRandomForest.txt");
			Const.log.save("ClassificationAnalysisRandomForest.txt", classificationresult);
			
			Classicationwriter = new FWriter(Const.log.publicFolder + "ClassificationAnalysisMF.txt");
			Const.log.save("ClassificationAnalysisMF.txt", classificationresult);
			
			Classicationwriter = new FWriter(
					Const.log.publicFolder + "ClassificationAnalysisMFJ48.txt");
			Const.log.save("ClassificationAnalysisMFJ48.txt", classificationresult);
			
			Classicationwriter = new FWriter(
					Const.log.publicFolder + "ClassificationAnalysisMFMultilayerPerceptron.txt");
			Const.log.save("ClassificationAnalysisMFMultilayerPerceptron.txt", classificationresult);
			
			
			Classicationwriter = new FWriter(
					Const.log.publicFolder + "ClassificationAnalysisMFNaiveBayes.txt");
			Const.log.save("ClassificationAnalysisMFNaiveBayes.txt", classificationresult);
			
			
			Classicationwriter = new FWriter(
					Const.log.publicFolder + "ClassificationAnalysisMFSMO.txt");
			Const.log.save("ClassificationAnalysisMFSMO.txt", classificationresult);
			
			Classicationwriter = new FWriter(
					Const.log.publicFolder + "ClassificationAnalysisMFPART.txt");
			Const.log.save("ClassificationAnalysisMFPART.txt", classificationresult);
			
			Classicationwriter = new FWriter(
					Const.log.publicFolder + "ClassificationAnalysisMFRandomForest.txt");
			Const.log.save("ClassificationAnalysisMFRandomForest.txt", classificationresult);
			
			Classicationwriter = new FWriter(Const.log.publicFolder + "ClassificationAnalysisOlsen.txt");
			Const.log.save("ClassificationAnalysisOlsen.txt", classificationresult);

			Classicationwriter = new FWriter(
					Const.log.publicFolder + "ClassificationAnalysisOlsenJ48.txt");
			Const.log.save("ClassificationAnalysisOlsenJ48.txt", classificationresult);
			
			Classicationwriter = new FWriter(
					Const.log.publicFolder + "ClassificationAnalysisOlsenMultilayerPerceptron.txt");
			Const.log.save("ClassificationAnalysisOlsenMultilayerPerceptron.txt", classificationresult);
			
			Classicationwriter = new FWriter(
					Const.log.publicFolder + "ClassificationAnalysisOlsenNaiveBayes.txt");
			Const.log.save("ClassificationAnalysisOlsenNaiveBayes.txt", classificationresult);
			
			Classicationwriter = new FWriter(
					Const.log.publicFolder + "ClassificationAnalysisOlsenSMO.txt");
			Const.log.save("ClassificationAnalysisOlsenSMO.txt", classificationresult);
			
			Classicationwriter = new FWriter(
					Const.log.publicFolder + "ClassificationAnalysisOlsenPART.txt");
			Const.log.save("ClassificationAnalysisOlsenPART.txt", classificationresult);
			
			Classicationwriter = new FWriter(
					Const.log.publicFolder + "ClassificationAnalysisOlsenRandomForest.txt");
			Const.log.save("ClassificationAnalysisOlsenRandomForest.txt", classificationresult);
			
			
			Classicationwriter = new FWriter(
					Const.log.publicFolder + "ClassificationAnalysisTraining.txt");
			Const.log.save("ClassificationAnalysisTraining.txt", classificationresult);

			Classicationwriter = new FWriter(
					Const.log.publicFolder + "ClassificationAnalysisTrainingMF.txt");
			Const.log.save("ClassificationAnalysisTrainingMF.txt", classificationresult);

			Classicationwriter = new FWriter(
					Const.log.publicFolder + "ClassificationAnalysisTrainingOlsen.txt");
			Const.log.save("ClassificationAnalysisTrainingOlsen.txt", classificationresult);

			
			String SimpleTradingResult = "Dataset \t  GP \t Classifier+GP  \t "
					+ "Probability+GP \t GP+DCC \t GP+J48 \t GP+MultilayerPerceptron \t GP+NaiveBayes \t  GP+SMO \t GP+PART \t GP+RandomForest \t GP+Random \t"
					+ "MF  \t Classifier+MF \t Probability+MF \t MF+DCC  \t MF+J48 \t MF+MultilayerPerceptron \t  MF+NaiveBayes \t  MF+SMO \t MF+PART \t MF+RandomForest \t MF+Random \t"
					+ " Olsen \t Classifier+Olsen \t Probability+Olsen \t Olsen+DCC \t Olsen+J48 \t Olsen+MultilayerPerceptron \t Olsen+NaiveBayes \t  Olsen+SMO  \t  Olsen+PART  \t  Olsen+RandomForest \t  Olsen+Random";
			FWriter SimpleTradingResultWriter = new FWriter(
					Const.log.publicFolder + "SimpleTradingResult.txt");
			Const.log.save("SimpleTradingResult.txt", SimpleTradingResult);

			FWriter sharpRatioWriter = new FWriter(Const.log.publicFolder + "SharpRatio.txt");
			Const.log.save("SharpRatio.txt", SimpleTradingResult);

			String mDD = "Dataset \t PerfectForesight+GP \t GP \t Classifier+GP  \t GP+NaiveBayes \t GP+SMO \t GP+PART \t GP+RandomForest \t GP+Random \t"
					+ "\t Probability+GP \t GP+DCC \t GP+J48 \t GP+MultilayerPerceptron \t"
					+ " PerfectForesight+MF \t MF  \t Classifier+MF \t "
					+ " Probability+MF \t MF+DCC \t MF+J48 \t MF+MultilayerPerceptron \t MF+NaiveBayes \t MF+SMO \t MF+PART \t MF+RandomForest \t MF+Random \t" 
					+ "PerfectForesight+Olsen \t Olsen \t Classifier+Olsen "
					+ "\t Probability+Olsen \t Olsen+DCC \t Olsen+J48 \t Olsen+MultilayerPerceptron \t Olsen+NaiveBayes \t  Olsen+SMO  \t  Olsen+PART \t  Olsen+RandomForest \t  Olsen+Randomm ";
			FWriter MDDWriter = new FWriter(Const.log.publicFolder + "mddBaseCcy.txt");
			Const.log.save("mddBaseCcy.txt", mDD);

			FWriter MDDWriterQuote = new FWriter(Const.log.publicFolder + "mddQuoteCcy.txt");
			Const.log.save("mddQuoteCcy.txt", mDD);

			String profit =  "Dataset \t PerfectForesight+GP \t GP \t Classifier+GP  \t GP+NaiveBayes \t GP+SMO \t GP+PART \t GP+RandomForest \t GP+Random \t"
					+ "\t Probability+GP \t GP+DCC \t GP+J48 \t GP+MultilayerPerceptron \t"
					+ " PerfectForesight+MF \t MF  \t Classifier+MF \t "
					+ " Probability+MF \t MF+DCC \t MF+J48 \t MF+MultilayerPerceptron \t MF+NaiveBayes \t MF+SMO \t MF+PART \t MF+RandomForest \t MF+Random \t" 
					+ "PerfectForesight+Olsen \t Olsen \t Classifier+Olsen "
					+ "\t Probability+Olsen \t Olsen+DCC \t Olsen+J48 \t Olsen+MultilayerPerceptron \t Olsen+NaiveBayes \t  Olsen+SMO  \t  Olsen+PART \t  Olsen+RandomForest \t  Olsen+Randomm ";
			FWriter profitWriter = new FWriter(Const.log.publicFolder + "BaseCCyProfit.txt");
			Const.log.save("BaseCCyProfit.txt", profit);

			FWriter profitWriterQuote = new FWriter(Const.log.publicFolder + "QuoteCCyProfit.txt");
			Const.log.save("QuoteCCyProfit.txt", profit);

			String transactions =  "Dataset \t PerfectForesight+GP \t GP \t Classifier+GP  \t GP+NaiveBayes \t GP+SMO \t GP+PART \t GP+RandomForest \t GP+Random \t"
					+ "\t Probability+GP \t GP+DCC \t GP+J48 \t GP+MultilayerPerceptron \t"
					+ " PerfectForesight+MF \t MF  \t Classifier+MF \t "
					+ " Probability+MF \t MF+DCC \t MF+J48 \t MF+MultilayerPerceptron \t MF+NaiveBayes \t MF+SMO \t MF+PART \t MF+RandomForest \t MF+Random \t" 
					+ "PerfectForesight+Olsen \t Olsen \t Classifier+Olsen "
					+ "\t Probability+Olsen \t Olsen+DCC \t Olsen+J48 \t Olsen+MultilayerPerceptron \t Olsen+NaiveBayes \t  Olsen+SMO  \t  Olsen+PART \t  Olsen+RandomForest \t  Olsen+Randomm ";
			FWriter NumberOfBaseCCyTransactionWriter = new FWriter(
					Const.log.publicFolder + "NumberOfBaseCCyTransaction.txt");
			Const.log.save("NumberOfBaseCCyTransaction.txt", transactions);

			FWriter NumberOfQuoteCCyTransactionWriter = new FWriter(
					Const.log.publicFolder + "NumberOfQuoteCCyTransaction.txt");
			Const.log.save("NumberOfQuoteCCyTransaction.txt", transactions);

			
			String DCAndTaTradingResult =  "Dataset \t PerfectForesight+GP \t GP \t Classifier+GP  \t GP+NaiveBayes \t GP+SMO \t GP+PART \t GP+RandomForest \t GP+Random \t"
					+ "\t Probability+GP \t GP+DCC \t GP+J48 \t GP+MultilayerPerceptron \t"
					+ " PerfectForesight+MF \t MF  \t Classifier+MF \t "
					+ " Probability+MF \t MF+DCC \t MF+J48 \t MF+MultilayerPerceptron \t MF+NaiveBayes \t MF+SMO \t MF+PART \t MF+RandomForest \t MF+Random \t" 
					+ "PerfectForesight+Olsen \t Olsen \t Classifier+Olsen "
					+ "\t Probability+Olsen \t Olsen+DCC \t Olsen+J48 \t Olsen+MultilayerPerceptron \t Olsen+NaiveBayes \t  Olsen+SMO  \t  Olsen+PART \t  Olsen+RandomForest \t  Olsen+Randomm ";
			FWriter DCAndTaTradingResultWriter = new FWriter(
					Const.log.publicFolder + "DCAndTaTradingResult.txt");
			Const.log.save("DCAndTaTradingResult.txt", DCAndTaTradingResult);
			
			String dcstats = "Dataset \t NumberOfDCEventTraining \t NumberOfDCEventTest \t NumberOfOSEventTraining  \t NumberOfOSEventTest "
					+ "\t OsToDcEventRatioTraining  \t OsToDcEventRatioTest \t AverageDCLengthTraining \t AverageDCLengthTest"
					+ " \t AverageDownwardDCLengthTraining  \t AverageDownwardDCLengthTest \t AverageUpwardDCLengthTraining    \t AverageUpwardDCLengthTest";
			FWriter dcstatsWriter = new FWriter(
					Const.log.publicFolder + "DCStats.txt");
			Const.log.save("DCStats.txt", dcstats);

			// reset currentProcessorCounter
			currentProcessorCounter = 0;
			double perfectForesightTradeResult = 0.0;
			double gPTradeResult = 0.0;
			double classificationAndGpTradeResult = 0.0;
			double probabilityAndGpTradeResult = 0.0;
			double randomAndGpTradeResult = 0.0;
			double J48AndGpTradeResult = 0.0;
			double multilayerPerceptronAndGpTradeResult = 0.0;
			double NaiveBayesAndGpTradeResult = 0.0;
			double SMOAndGpTradeResult = 0.0;
			double PARTAndGpTradeResult = 0.0;
			double RandomForestAndGpTradeResult = 0.0;
			
			
			double ddcOnlyGPTradeResult = 0.0;
			
		
			double perfectForesightMFTradeResult = 0.0;
			double mFTradeResult = 0.0;
			double classificationAndMFTradeResult = 0.0;
			double probabilityAndMFTradeResult = 0.0;
			double randomAndMFTradeResult = 0.0;
			double ddcOnlyMFTradeResult = 0.0;
			double J48AndMFTradeResult = 0.0;
			double multilayerPerceptronAndMFTradeResult = 0.0;
			double NaiveBayesAndMFTradeResult = 0.0;
			double SMOAndMFTradeResult = 0.0;
			double PARTAndMFTradeResult = 0.0;
			double RandomForestAndMFTradeResult = 0.0;
			
			double perfectForesightOlsenTradeResult = 0.0;
			double olsenTradeResult = 0.0;
			double classificationAndOlsenTradeResult = 0.0;
			double probabilityAndOlsenTradeResult = 0.0;
			double randomAndOlsenTradeResult = 0.0;
			double ddcOnlyOlsenTradeResult = 0.0;
			double J48AndOlsenTradeResult = 0.0;
			double multilayerPerceptronAndOlsenTradeResult = 0.0;
			double NaiveBayesAndOlsenTradeResult = 0.0;
			double SMOAndOlsenTradeResult = 0.0;
			double PARTAndOlsenTradeResult = 0.0;
			double RandomForestAndOlsenTradeResult = 0.0;
			
			

			double rsiTechnicalAnalysisTradeResult = 0.0;
			double emaTechnicalAnalysisTradeResult = 0.0;
			double macdTechnicalAnalysisTradeResult = 0.0;

			for (int reportCount = 0; reportCount < SELECTED_THRESHOLDS.length; reportCount++) {
				// String thresholdStr = String.format("%.8f",
				// SELECTED_THRESHOLDS[reportCount]);
				String gpFileNamePrefix = gpDaysMap.get(TrainingDay);
				// System.out.println("RUN:: " + gpFileNamePrefix);
				String gpFileName = ""; // no longer relevant

				// Report RMSE
				testArrayPerfectForesight[reportCount] = curvePerfectForesight[reportCount].report(this.test,
						SELECTED_THRESHOLDS[reportCount], gpFileName);
				testArrayCifre[reportCount] = curveCifre[reportCount].report(this.test,
						SELECTED_THRESHOLDS[reportCount], gpFileName);
				testArrayClassification[reportCount] = curveClassifcation[reportCount].report(this.test,
						SELECTED_THRESHOLDS[reportCount], gpFileName);
				testArrayProbability[reportCount] = curveProbabilityGP[reportCount].report(this.test,
						SELECTED_THRESHOLDS[reportCount], gpFileName);
				
				testArrayRandom[reportCount] = curveRandomGP[reportCount].report(this.test,
						SELECTED_THRESHOLDS[reportCount], gpFileName);
				
				testArrayClassificationNoRegressionGP[reportCount] = curveNoClassificationNoRegressionGP[reportCount]
						.report(this.test, SELECTED_THRESHOLDS[reportCount], gpFileName);
				
				testArrayClassificationJ48[reportCount] =curveCifreJ48[reportCount]
						.report(this.test, SELECTED_THRESHOLDS[reportCount], gpFileName);
				
				
				testArrayClassificationMultilayerPerceptron[reportCount] =curveCifreMultilayerPerceptron[reportCount]
						.report(this.test, SELECTED_THRESHOLDS[reportCount], gpFileName);
				
				
				testArrayClassificationNaiveBayes[reportCount] =curveCifreNaiveBayes[reportCount]
						.report(this.test, SELECTED_THRESHOLDS[reportCount], gpFileName);
				
				testArrayClassificationSMO[reportCount] =curveCifreSMO[reportCount]
						.report(this.test, SELECTED_THRESHOLDS[reportCount], gpFileName);
				
				testArrayClassificationPART[reportCount] =curveCifrePART[reportCount]
						.report(this.test, SELECTED_THRESHOLDS[reportCount], gpFileName);
				
				testArrayClassificationRandomForest[reportCount] =curveCifreRandomForest[reportCount]
						.report(this.test, SELECTED_THRESHOLDS[reportCount], gpFileName);

				testArrayPerfectForesightMF[reportCount] = curvePerfectForesightMF[reportCount].report(this.test,
						SELECTED_THRESHOLDS_MF[reportCount], gpFileName);
				testArrayMF[reportCount] = curveMF[reportCount].report(this.test, SELECTED_THRESHOLDS_MF[reportCount],
						gpFileName);
				testArrayClassificationMF[reportCount] = curveClassifcationMF[reportCount].report(this.test,
						SELECTED_THRESHOLDS_MF[reportCount], gpFileName);
				testArrayProbabilityMF[reportCount] = curveProbabilityMF[reportCount].report(this.test,
						SELECTED_THRESHOLDS_MF[reportCount], gpFileName);
				
				testArrayRandomMF[reportCount] = curveRandomMF[reportCount].report(this.test,
						SELECTED_THRESHOLDS_MF[reportCount], gpFileName);
			
				testArrayClassificationNoRegressionMF[reportCount] = curveNoClassificationNoRegressionMF[reportCount]
						.report(this.test, SELECTED_THRESHOLDS_MF[reportCount], gpFileName);

				testArrayClassificationMFJ48[reportCount] =curveMFJ48[reportCount]
						.report(this.test, SELECTED_THRESHOLDS_MF[reportCount], gpFileName);

				
				testArrayClassificationMFMultilayerPerceptron[reportCount] =curveMFMultilayerPerceptron[reportCount]
						.report(this.test, SELECTED_THRESHOLDS_MF[reportCount], gpFileName);
				
				testArrayClassificationMFNaiveBayes[reportCount] =curveMFNaiveBayes[reportCount]
						.report(this.test, SELECTED_THRESHOLDS_MF[reportCount], gpFileName);

				testArrayClassificationMFSMO[reportCount] =curveMFSMO[reportCount]
						.report(this.test, SELECTED_THRESHOLDS_MF[reportCount], gpFileName);
				
				testArrayClassificationMFPART[reportCount] =curveMFPART[reportCount]
						.report(this.test, SELECTED_THRESHOLDS_MF[reportCount], gpFileName);
				
				testArrayClassificationMFRandomForest[reportCount] =curveMFRandomForest[reportCount]
						.report(this.test, SELECTED_THRESHOLDS_MF[reportCount], gpFileName);
				
				testArrayPerfectForesightOlsen[reportCount] = curvePerfectForesightOlsen[reportCount].report(this.test,
						SELECTED_THRESHOLDS_OLSEN[reportCount], gpFileName);
				testArrayOlsen[reportCount] = curveOlsen[reportCount].report(this.test,
						SELECTED_THRESHOLDS_OLSEN[reportCount], gpFileName);
				testArrayClassificationOlsen[reportCount] = curveClassificationOlsen[reportCount].report(this.test,
						SELECTED_THRESHOLDS_OLSEN[reportCount], gpFileName);
				testArrayProbabilityOlsen[reportCount] = curveProbabilityOlsen[reportCount].report(this.test,
						SELECTED_THRESHOLDS_OLSEN[reportCount], gpFileName);
				
				testArrayRandomOlsen[reportCount] = curveRandomOlsen[reportCount].report(this.test,
						SELECTED_THRESHOLDS_OLSEN[reportCount], gpFileName);
				
				testArrayClassificationNoRegressionOlsen[reportCount] = curveNoClassificationNoRegressionOlsen[reportCount]
						.report(this.test, SELECTED_THRESHOLDS_OLSEN[reportCount], gpFileName);
				
				testArrayClassificationOlsenJ48[reportCount] =curveOlsenJ48[reportCount]
						.report(this.test, SELECTED_THRESHOLDS_OLSEN[reportCount], gpFileName);

				testArrayClassificationOlsenMultilayerPerceptron[reportCount] =curveOlsenMultilayerPerceptron[reportCount]
						.report(this.test, SELECTED_THRESHOLDS_OLSEN[reportCount], gpFileName);
				
				
				testArrayClassificationOlsenNaiveBayes[reportCount] =curveOlsenNaiveBayes[reportCount]
						.report(this.test, SELECTED_THRESHOLDS_OLSEN[reportCount], gpFileName);
				
				testArrayClassificationOlsenSMO[reportCount] =curveOlsenSMO[reportCount]
						.report(this.test, SELECTED_THRESHOLDS_OLSEN[reportCount], gpFileName);
				
				testArrayClassificationOlsenPART[reportCount] =curveOlsenPART[reportCount]
						.report(this.test, SELECTED_THRESHOLDS_OLSEN[reportCount], gpFileName);
				
				testArrayClassificationOlsenRandomForest[reportCount] =curveOlsenRandomForest[reportCount]
						.report(this.test, SELECTED_THRESHOLDS_OLSEN[reportCount], gpFileName);
				
				
				/*
				 * Start because we use different threshold for each algo we
				 * need to get seperated distribution for each regression
				 * algorithm compared
				 */
				curveCifre[reportCount].TrainingDCEventOSEventDistribution();
				curveCifre[reportCount].TestDCEventOSEventDistribution();

				curveMF[reportCount].TrainingDCEventOSEventDistribution();
				curveMF[reportCount].TestDCEventOSEventDistribution();

				curveOlsen[reportCount].TrainingDCEventOSEventDistribution();
				curveOlsen[reportCount].TestDCEventOSEventDistribution();

				
				/*
				 * End
				 */

				selectedThresholds = filename + "\t" + String.format("%.8f", SELECTED_THRESHOLDS[reportCount]) + "\t"
						+ String.format("%.8f", curveCifre[reportCount].zeroPercentageTest) + "\t"
						+ String.format("%.8f", curveCifre[reportCount].dC_OS_Length_RatioTest) + "\t"
						+ String.format("%.8f", SELECTED_THRESHOLDS_MF[reportCount]) + "\t"
						+ String.format("%.8f", curveMF[reportCount].zeroPercentageTest) + "\t"
						+ String.format("%.8f", curveMF[reportCount].dC_OS_Length_RatioTest) + "\t"
						+ String.format("%.8f", SELECTED_THRESHOLDS_OLSEN[reportCount]) + "\t"
						+ String.format("%.8f", curveOlsen[reportCount].zeroPercentageTest) + "\t"
						+ String.format("%.8f", curveOlsen[reportCount].dC_OS_Length_RatioTest);

				Const.log.save("selectedThresholdsTest.txt", selectedThresholds);

				selectedThresholds = filename + "\t" + String.format("%.8f", SELECTED_THRESHOLDS[reportCount]) + "\t"
						+ String.format("%.8f", curveCifre[reportCount].zeroPercentageTraining) + "\t"
						+ String.format("%.8f", curveCifre[reportCount].dC_OS_Length_RatioTraining) + "\t"
						+ String.format("%.8f", SELECTED_THRESHOLDS_MF[reportCount]) + "\t"
						+ String.format("%.8f", curveMF[reportCount].zeroPercentageTraining) + "\t"
						+ String.format("%.8f", curveMF[reportCount].dC_OS_Length_RatioTraining) + "\t"
						+ String.format("%.8f", SELECTED_THRESHOLDS_OLSEN[reportCount]) + "\t"
						+ String.format("%.8f", curveOlsen[reportCount].zeroPercentageTraining) + "\t"
						+ String.format("%.8f", curveOlsen[reportCount].dC_OS_Length_RatioTraining);

				Const.log.save("selectedThresholdsTraining.txt", selectedThresholds);

				Locale locale = new Locale("en", "UK");
				String pattern = "##.##########";

				DecimalFormat decimalFormat = (DecimalFormat) NumberFormat.getNumberInstance(locale);
				decimalFormat.applyPattern(pattern);

				String formatPerfectForesight = decimalFormat
						.format(Double.parseDouble(testArrayPerfectForesight[reportCount]));
				String formatCifre = decimalFormat.format(Double.parseDouble(testArrayCifre[reportCount]));
				String formatClassifier = decimalFormat
						.format(Double.parseDouble(testArrayClassification[reportCount]));
				String formatProbability = decimalFormat.format(Double.parseDouble(testArrayProbability[reportCount]));
				String formatRandom = decimalFormat.format(Double.parseDouble(testArrayRandom[reportCount]));
				
				String formatClassifierJ48 = decimalFormat.format(Double.parseDouble(testArrayClassificationJ48[reportCount]));
				String formatClassifierMultilayerPerceptron = decimalFormat.format(Double.parseDouble(testArrayClassificationMultilayerPerceptron[reportCount]));
				String formatClassifierNaiveBayes = decimalFormat.format(Double.parseDouble(testArrayClassificationNaiveBayes[reportCount]));
				String formatClassifierSMO = decimalFormat.format(Double.parseDouble(testArrayClassificationSMO[reportCount]));
				String formatClassifierPART = decimalFormat.format(Double.parseDouble(testArrayClassificationPART[reportCount]));
				String formatClassifierRandomForest = decimalFormat.format(Double.parseDouble(testArrayClassificationRandomForest[reportCount]));                     
				                     
				
				/*
				 * Skipping testArrayDCCOnlyAndTrailGP[reportCount]
				 * testArrayClassificationNoRegressionGP[reportCount];
				 */

				String formatPerfectForesightMF = decimalFormat
						.format(Double.parseDouble(testArrayPerfectForesightMF[reportCount]));
				String formatMF = decimalFormat.format(Double.parseDouble(testArrayMF[reportCount]));
				String formatClassifierMF = decimalFormat
						.format(Double.parseDouble(testArrayClassificationMF[reportCount]));
				String formatProbabilityMF = decimalFormat.format(Double.parseDouble(testArrayProbabilityMF[reportCount]));
				String formatRandomMF = decimalFormat.format(Double.parseDouble(testArrayRandomMF[reportCount]));
				String formatClassifierMFJ48 = decimalFormat.format(Double.parseDouble(testArrayClassificationMFJ48[reportCount]));
				String formatClassifierMFMultilayerPerceptron = decimalFormat.format(Double.parseDouble(testArrayClassificationMFMultilayerPerceptron[reportCount]));
				String formatClassifierMFNaiveBayes = decimalFormat.format(Double.parseDouble(testArrayClassificationMFNaiveBayes[reportCount]));
				String formatClassifierMFSMO = decimalFormat.format(Double.parseDouble(testArrayClassificationMFSMO[reportCount]));
				String formatClassifierMFPART = decimalFormat.format(Double.parseDouble(testArrayClassificationMFPART[reportCount]));
				String formatClassifierMFRandomForest = decimalFormat.format(Double.parseDouble(testArrayClassificationMFRandomForest[reportCount]));
				
				
				
				/*
				 * Skipping testArrayDCCOnlyAndTrailMF[reportCount]
				 * testArrayClassificationNoRegressionMF[reportCount];
				 */

				String formatPerfectForesightOlsen = decimalFormat
						.format(Double.parseDouble(testArrayPerfectForesightOlsen[reportCount]));
				String formatOlsen = decimalFormat.format(Double.parseDouble(testArrayOlsen[reportCount]));
				String formatClassifierOlsen = decimalFormat
						.format(Double.parseDouble(testArrayClassificationOlsen[reportCount]));
				String formatProbabilityOlsen = decimalFormat.format(Double.parseDouble(testArrayProbabilityOlsen[reportCount]));
				String formatRandomOlsen = decimalFormat.format(Double.parseDouble(testArrayRandomOlsen[reportCount]));
				String formatClassifierOlsenJ48 = decimalFormat.format(Double.parseDouble(testArrayClassificationOlsenJ48[reportCount]));
				String formatClassifierOlsenMultilayerPerceptron = decimalFormat.format(Double.parseDouble(testArrayClassificationOlsenMultilayerPerceptron[reportCount]));
				String formatClassifierOlsenNaiveBayes = decimalFormat.format(Double.parseDouble(testArrayClassificationOlsenNaiveBayes[reportCount]));
				String formatClassifierOlsenSMO = decimalFormat.format(Double.parseDouble(testArrayClassificationOlsenSMO[reportCount]));
				String formatClassifierOlsenPART = decimalFormat.format(Double.parseDouble(testArrayClassificationOlsenPART[reportCount]));
				String formatClassifierOlsenRandomForest = decimalFormat.format(Double.parseDouble(testArrayClassificationOlsenRandomForest[reportCount]));

				

				regressionresult = filename + "\t" 
						+  formatClassifier      + "\t"+  formatCifre + "\t"  + formatProbability+ "\t" +formatClassifierJ48 + "\t" + formatClassifierMultilayerPerceptron + "\t"  + formatClassifierNaiveBayes  + "\t" + formatClassifierSMO  + "\t" + formatClassifierPART  + "\t" + formatClassifierRandomForest  + "\t" + formatRandom + "\t" 
						+ formatClassifierMF     + "\t" + formatMF + "\t" + formatProbabilityMF+ "\t" +formatClassifierMFJ48 + "\t" + formatClassifierMFMultilayerPerceptron + "\t" + formatClassifierMFNaiveBayes  + "\t"  + formatClassifierMFSMO  + "\t"  +  formatClassifierMFPART  + "\t" + formatClassifierMFRandomForest  + "\t" + formatRandomMF + "\t"
						+ formatClassifierOlsen  + "\t" +  formatOlsen + "\t" + formatProbabilityOlsen+ "\t" +formatClassifierOlsenJ48 + "\t" + formatClassifierOlsenMultilayerPerceptron + "\t" + formatClassifierOlsenNaiveBayes + "\t" + formatClassifierOlsenSMO + "\t" + formatClassifierOlsenPART + "\t" + formatClassifierOlsenRandomForest  + "\t" + formatRandomOlsen;

				Const.log.save("RegressionAnalysisCurves.txt", regressionresult);

				double pFTrade = curvePerfectForesight[reportCount].trade();
				double gPTrade = curveCifre[reportCount].trade();
				double classificationAndGpTrade = curveClassifcation[reportCount].trade();
				double probabilityTrade = curveProbabilityGP[reportCount].trade();
				double randomTrade = curveRandomGP[reportCount].trade();
				double noClassifierNoRegressionGPTrade = curveNoClassificationNoRegressionGP[reportCount].trade();
				double J48AndGpTrade = curveCifreJ48[reportCount].trade();
				double multilayerPerceptronAndGpTrade = curveCifreMultilayerPerceptron[reportCount].trade();
				double NaiveBayesAndGpTrade = curveCifreNaiveBayes[reportCount].trade();
				double SMOAndGpTrade = curveCifreSMO[reportCount].trade();
				double PARTAndGpTrade = curveCifrePART[reportCount].trade();
				double RandomForestAndGpTrade = curveCifreRandomForest[reportCount].trade();
				
				
				double perfectForesightMFTrade = curvePerfectForesightMF[reportCount].trade();
				double mFTrade = curveMF[reportCount].trade();
				double classificationAndMFTrade = curveClassifcationMF[reportCount].trade();
				double probabilityTradeMF = curveProbabilityMF[reportCount].trade();
				double randomTradeMF = curveRandomMF[reportCount].trade();
				double noClassifierNoRegressionMFTrade = curveNoClassificationNoRegressionMF[reportCount].trade();
				double J48AndMFTrade = curveMFJ48[reportCount].trade();
				double multilayerPerceptronAndMFTrade = curveMFMultilayerPerceptron[reportCount].trade();
				double NaiveBayesAndMFTrade = curveMFNaiveBayes[reportCount].trade();
				double SMOAndMFTrade = curveMFSMO[reportCount].trade();
				double PARTAndMFTrade = curveMFPART[reportCount].trade();
				double RandomForestAndMFTrade = curveMFRandomForest[reportCount].trade();
				
				double perfectForesightOlsenTrade = curvePerfectForesightOlsen[reportCount].trade();
				double olsenTrade = curveOlsen[reportCount].trade();
				double classificationAndOlsenTrade = curveClassificationOlsen[reportCount]
						.trade();
				double probabilityTradeOlsen = curveProbabilityOlsen[reportCount].trade();
				double randomTradeOlsen = curveRandomOlsen[reportCount].trade();
				double noClassifierNoRegressionOlsenTrade = curveNoClassificationNoRegressionOlsen[reportCount]
						.trade();

				double J48AndOlsenTrade = curveOlsenJ48[reportCount].trade();
				double multilayerPerceptronAndOlsenTrade = curveOlsenMultilayerPerceptron[reportCount].trade();
				double NaiveBayesAndOlsenTrade = curveOlsenNaiveBayes[reportCount].trade();
				double SMOAndOlsenTrade = curveOlsenSMO[reportCount].trade();
				double PARTAndOlsenTrade = curveOlsenPART[reportCount].trade();
				double RandomForestAndOlsenTrade = curveOlsenRandomForest[reportCount].trade();

				// let's sum up the provit from selected thresholds
				perfectForesightTradeResult = perfectForesightTradeResult + pFTrade;
				gPTradeResult = gPTradeResult + gPTrade;
				classificationAndGpTradeResult = classificationAndGpTradeResult + classificationAndGpTrade;
				probabilityAndGpTradeResult = probabilityAndGpTradeResult + probabilityTrade;
				randomAndGpTradeResult = randomAndGpTradeResult + randomTrade;
				
				ddcOnlyGPTradeResult = ddcOnlyGPTradeResult + noClassifierNoRegressionGPTrade;
				J48AndGpTradeResult  = J48AndGpTradeResult +J48AndGpTrade ;
				multilayerPerceptronAndGpTradeResult = multilayerPerceptronAndGpTradeResult + multilayerPerceptronAndGpTrade;
				NaiveBayesAndGpTradeResult = NaiveBayesAndGpTradeResult + NaiveBayesAndGpTrade;
				SMOAndGpTradeResult = SMOAndGpTradeResult + SMOAndGpTrade;
				PARTAndGpTradeResult = PARTAndGpTradeResult + PARTAndGpTrade;
				RandomForestAndGpTradeResult = RandomForestAndGpTradeResult + RandomForestAndGpTrade;
				
				perfectForesightMFTradeResult = perfectForesightMFTradeResult + perfectForesightMFTrade;
				mFTradeResult = mFTradeResult + mFTrade;
				classificationAndMFTradeResult = classificationAndMFTradeResult + classificationAndMFTrade;
				probabilityAndMFTradeResult = probabilityAndMFTradeResult + probabilityTradeMF;
				randomAndMFTradeResult = randomAndMFTradeResult + randomTradeMF;
				ddcOnlyMFTradeResult = ddcOnlyMFTradeResult + noClassifierNoRegressionMFTrade;
				J48AndMFTradeResult  = J48AndGpTradeResult +J48AndMFTrade ;
				multilayerPerceptronAndMFTradeResult = multilayerPerceptronAndMFTradeResult + multilayerPerceptronAndMFTrade;
				NaiveBayesAndMFTradeResult = NaiveBayesAndMFTradeResult + NaiveBayesAndMFTrade;
				SMOAndMFTradeResult = SMOAndMFTradeResult + SMOAndMFTrade;
				PARTAndMFTradeResult = PARTAndMFTradeResult + PARTAndMFTrade;
				RandomForestAndMFTradeResult = RandomForestAndMFTradeResult + RandomForestAndMFTrade;
				

				perfectForesightOlsenTradeResult = perfectForesightOlsenTradeResult + perfectForesightOlsenTrade;
				olsenTradeResult = olsenTradeResult + olsenTrade;
				classificationAndOlsenTradeResult = classificationAndOlsenTradeResult + classificationAndOlsenTrade;
				probabilityAndOlsenTradeResult = probabilityAndOlsenTradeResult + probabilityTradeOlsen;
				randomAndOlsenTradeResult = randomAndOlsenTradeResult + randomTradeOlsen;
				ddcOnlyOlsenTradeResult = ddcOnlyOlsenTradeResult + noClassifierNoRegressionOlsenTrade;
				J48AndOlsenTradeResult  = J48AndGpTradeResult +J48AndOlsenTrade ;
				multilayerPerceptronAndOlsenTradeResult = multilayerPerceptronAndOlsenTradeResult + multilayerPerceptronAndOlsenTrade;
				NaiveBayesAndOlsenTradeResult = NaiveBayesAndOlsenTradeResult + NaiveBayesAndOlsenTrade;
				SMOAndOlsenTradeResult = SMOAndOlsenTradeResult + SMOAndOlsenTrade;
				PARTAndOlsenTradeResult = PARTAndOlsenTradeResult + PARTAndOlsenTrade;
				RandomForestAndOlsenTradeResult = RandomForestAndOlsenTradeResult + RandomForestAndOlsenTrade;

				rsiTechnicalAnalysisTradeResult = rsiTradeResult;
				emaTechnicalAnalysisTradeResult = emaTradeResult;
				macdTechnicalAnalysisTradeResult = mcadTradeResult;

				SimpleTradingResult = filename + "\t" + gPTrade + "\t" + classificationAndGpTrade
						+ "\t" + probabilityTrade + "\t" + noClassifierNoRegressionGPTrade + "\t"
						+ J48AndGpTradeResult + "\t" + multilayerPerceptronAndGpTradeResult + "\t" + NaiveBayesAndGpTradeResult + "\t" + SMOAndGpTradeResult + "\t" +  PARTAndGpTradeResult + "\t" + RandomForestAndGpTradeResult + "\t" + randomAndGpTradeResult + "\t"
						+ mFTrade + "\t" + classificationAndMFTrade + "\t"
						+ probabilityTradeMF + "\t" + noClassifierNoRegressionMFTrade + "\t" 
						+ J48AndMFTradeResult + "\t" + multilayerPerceptronAndMFTradeResult + "\t" + NaiveBayesAndMFTradeResult + "\t" +  SMOAndMFTradeResult + "\t" +PARTAndMFTradeResult + "\t" + RandomForestAndMFTradeResult + "\t" + randomAndMFTradeResult + "\t"
						+ olsenTrade + "\t" + classificationAndOlsenTrade + "\t"
						+ probabilityTradeOlsen + "\t" + noClassifierNoRegressionOlsenTrade + "\t"
						+ J48AndOlsenTradeResult + "\t" + multilayerPerceptronAndOlsenTradeResult +  "\t" + NaiveBayesAndOlsenTradeResult + "\t" + SMOAndOlsenTradeResult + "\t" + PARTAndOlsenTradeResult +  "\t" +RandomForestAndOlsenTradeResult +  "\t" +randomAndOlsenTradeResult  ;

				Const.log.save("SimpleTradingResult.txt", SimpleTradingResult);

				// TODO continue here for DCConly and DCCAnd trail

				profit = filename + " \t" + +curvePerfectForesight[reportCount].getBaseCCyProfit() + "\t"
						+ curveCifre[reportCount].getBaseCCyProfit() + "\t"
						+ curveClassifcation[reportCount].getBaseCCyProfit() + "\t"
						+ curveProbabilityGP[reportCount].getBaseCCyProfit() + "\t"
						+ curveNoClassificationNoRegressionGP[reportCount].getBaseCCyProfit() + "\t"
						+ curveCifreJ48[reportCount].getBaseCCyProfit() + "\t"
						+ curveCifreMultilayerPerceptron[reportCount].getBaseCCyProfit() + "\t"
						+ curveCifreNaiveBayes[reportCount].getBaseCCyProfit() + "\t"
						+ curveCifreSMO[reportCount].getBaseCCyProfit() + "\t"
						+ curveCifrePART[reportCount].getBaseCCyProfit() + "\t"
						+ curveCifreRandomForest[reportCount].getBaseCCyProfit() + "\t"
						+ curveRandomGP[reportCount].getBaseCCyProfit() + "\t"
						
						+ curveMF[reportCount].getBaseCCyProfit() + "\t"
						+ curveClassifcationMF[reportCount].getBaseCCyProfit() + "\t"
						+ curveProbabilityMF[reportCount].getBaseCCyProfit() + "\t"
						+ curveNoClassificationNoRegressionMF[reportCount].getBaseCCyProfit() + "\t"
						+ curveMFJ48[reportCount].getBaseCCyProfit() + "\t"
						+ curveMFMultilayerPerceptron[reportCount].getBaseCCyProfit() + "\t"
						+ curveMFNaiveBayes[reportCount].getBaseCCyProfit() + "\t"
						+ curveMFSMO[reportCount].getBaseCCyProfit() + "\t"
						+ curveMFPART[reportCount].getBaseCCyProfit() + "\t"
						+ curveMFRandomForest[reportCount].getBaseCCyProfit() + "\t"
						+ curveRandomMF[reportCount].getBaseCCyProfit() + "\t"
								
						+ curveOlsen[reportCount].getBaseCCyProfit() + "\t"
						+ curveClassificationOlsen[reportCount].getBaseCCyProfit() + "\t"
						+ curveProbabilityOlsen[reportCount].getBaseCCyProfit() + "\t"
						+ curveNoClassificationNoRegressionOlsen[reportCount].getBaseCCyProfit() + "\t"
						+ curveOlsenJ48[reportCount].getBaseCCyProfit() + "\t"
						+ curveOlsenMultilayerPerceptron[reportCount].getBaseCCyProfit() + "\t"
						+ curveOlsenNaiveBayes[reportCount].getBaseCCyProfit() + "\t"
						+ curveOlsenSMO[reportCount].getBaseCCyProfit() + "\t"
						+ curveOlsenPART[reportCount].getBaseCCyProfit() + "\t"
						+ curveOlsenRandomForest[reportCount].getBaseCCyProfit()+ "\t"
						+ curveRandomOlsen[reportCount].getBaseCCyProfit() + "\t";
				
				Const.log.save("BaseCCyProfit.txt", profit);

				profit = filename + " \t" + curvePerfectForesight[reportCount].getQuoteCCyProfit() + "\t"
						+ curveCifre[reportCount].getQuoteCCyProfit() + "\t"
						+ curveClassifcation[reportCount].getQuoteCCyProfit() + "\t"
						+ curveProbabilityGP[reportCount].getQuoteCCyProfit() + "\t"
						+ curveNoClassificationNoRegressionGP[reportCount].getQuoteCCyProfit() + "\t"
						+ curveCifreJ48[reportCount].getQuoteCCyProfit() + "\t"
						+ curveCifreMultilayerPerceptron[reportCount].getQuoteCCyProfit() + "\t"
						+ curveCifreNaiveBayes[reportCount].getQuoteCCyProfit() + "\t"
						+ curveCifreSMO[reportCount].getQuoteCCyProfit() + "\t"
						+ curveCifrePART[reportCount].getQuoteCCyProfit() + "\t"
						+ curveCifreRandomForest[reportCount].getQuoteCCyProfit() + "\t"
					
						+ curveMF[reportCount].getQuoteCCyProfit() + "\t"
						+ curveProbabilityMF[reportCount].getQuoteCCyProfit() + "\t"
						+ curveNoClassificationNoRegressionMF[reportCount].getQuoteCCyProfit() + "\t"
						+ curveClassifcationMF[reportCount].getQuoteCCyProfit() + "\t"
						+ curveMFJ48[reportCount].getQuoteCCyProfit() + "\t"
						+ curveMFMultilayerPerceptron[reportCount].getQuoteCCyProfit() + "\t"
						+ curveMFNaiveBayes[reportCount].getQuoteCCyProfit() + "\t"
						+ curveMFSMO[reportCount].getQuoteCCyProfit() + "\t"
						+ curveMFPART[reportCount].getQuoteCCyProfit() + "\t"
						+ curveMFRandomForest[reportCount].getQuoteCCyProfit() + "\t"
					
						+ curveOlsen[reportCount].getQuoteCCyProfit() + "\t"
						+ curveClassificationOlsen[reportCount].getQuoteCCyProfit() + "\t"
						+ curveProbabilityOlsen[reportCount].getQuoteCCyProfit() + "\t"
						+ curveNoClassificationNoRegressionOlsen[reportCount].getQuoteCCyProfit() 
						+ curveOlsenJ48[reportCount].getQuoteCCyProfit() + "\t"
						+ curveOlsenMultilayerPerceptron[reportCount].getQuoteCCyProfit() + "\t" 
						+ curveOlsenNaiveBayes[reportCount].getQuoteCCyProfit() + "\t" 
						+ curveOlsenSMO[reportCount].getQuoteCCyProfit() + "\t" 
						+ curveOlsenPART[reportCount].getQuoteCCyProfit()+ "\t" 
						+ curveOlsenRandomForest[reportCount].getQuoteCCyProfit();
				
				Const.log.save("QuoteCCyProfit.txt", profit);

				profit = filename + " \t" 
						+ curveCifre[reportCount].getSharpRatio() + "\t"
						+ curveClassifcation[reportCount].getSharpRatio() + "\t"
						+ curveProbabilityGP[reportCount].getSharpRatio() + "\t"
						+ curveNoClassificationNoRegressionGP[reportCount].getSharpRatio() + "\t"
						+ curveCifreJ48[reportCount].getSharpRatio() + "\t"
						+ curveCifreMultilayerPerceptron[reportCount].getSharpRatio() + "\t"
						+ curveCifreNaiveBayes[reportCount].getSharpRatio() + "\t"
						+ curveCifreSMO[reportCount].getSharpRatio() + "\t"
						+ curveCifrePART[reportCount].getSharpRatio() + "\t"
						+ curveCifreRandomForest[reportCount].getSharpRatio() + "\t"
						+ curveRandomGP[reportCount].getSharpRatio() + "\t"
								
						+ curveMF[reportCount].getSharpRatio() + "\t" 
						+ curveClassifcationMF[reportCount].getSharpRatio() + "\t"
						+ curveProbabilityMF[reportCount].getSharpRatio() + "\t"
						+ curveNoClassificationNoRegressionMF[reportCount].getSharpRatio() + "\t"
						+ curveMFJ48[reportCount].getSharpRatio() + "\t"
						+ curveMFMultilayerPerceptron[reportCount].getSharpRatio() + "\t"
						+ curveMFNaiveBayes[reportCount].getSharpRatio() + "\t"
						+ curveMFSMO[reportCount].getSharpRatio() + "\t"
						+ curveMFPART[reportCount].getSharpRatio() + "\t"
						+ curveMFRandomForest[reportCount].getSharpRatio() + "\t"
						+ curveRandomMF[reportCount].getSharpRatio() + "\t"
								
						
						+ curveOlsen[reportCount].getSharpRatio() + "\t"
						+ curveClassificationOlsen[reportCount].getSharpRatio() + "\t"
						+ curveProbabilityOlsen[reportCount].getSharpRatio() + "\t"
						+ curveNoClassificationNoRegressionOlsen[reportCount].getSharpRatio() + "\t"
						+ curveOlsenJ48[reportCount].getSharpRatio() + "\t"
						+ curveOlsenMultilayerPerceptron[reportCount].getSharpRatio() + "\t"
						+ curveOlsenNaiveBayes[reportCount].getSharpRatio() + "\t"
						+ curveOlsenSMO[reportCount].getSharpRatio() + "\t"
						+ curveOlsenPART[reportCount].getSharpRatio()+ "\t"
						+ curveOlsenRandomForest[reportCount].getSharpRatio();
				profit  = profit  + "\t" + curveRandomOlsen[reportCount].getSharpRatio();
				
				Const.log.save("SharpRatio.txt", profit);
				
				
				
				profit = filename + " \t" + curveProbabilityGP[reportCount].getNumberOfDCEvent() + "\t"
						+ curveProbabilityGP[reportCount].getNumberOfDCEventTest() + "\t"
						+ curveProbabilityGP[reportCount].getNumberOFOSEvent()+ "\t"
						+ curveProbabilityGP[reportCount].getNumberOFOSEventTest() + "\t"
						+ curveProbabilityGP[reportCount].getOsToDcEventRatio() + "\t"
						+ curveProbabilityGP[reportCount].getOsToDcEventRatioTest()+ "\t"
						+ curveProbabilityGP[reportCount].getAverageDCRunLength() + "\t"
						+ curveProbabilityGP[reportCount].getAverageDCRunLengthTest() + "\t"
						+ curveProbabilityGP[reportCount].getAverageDownwardDCRunLength() + "\t"
						+ curveProbabilityGP[reportCount].getAverageDownwardDCRunLengthTest() + "\t"
						+ curveProbabilityGP[reportCount].getAverageUpwardDCRunLength() + "\t"
						+ curveProbabilityGP[reportCount].getAverageUpwardDCRunLengthTest() ;
				Const.log.save("DCStats.txt", profit);
				
				

				if (preprocess[reportCount] != null && preprocessMF[reportCount] != null
						&& preprocessOlsen[reportCount] != null ) {
					
					
					

					

					String thresholdStr = String.format("%.8f", SELECTED_THRESHOLDS[reportCount]);
					
					String classificationResult = preprocess[reportCount]
							.printPreprocessClassificationTraining(trainingEventsArray.get(thresholdStr));
					Const.log.save("ClassificationAnalysisTraining.txt", classificationResult);
					
					classificationResult = preprocessManualJ48[reportCount]
							.printPreprocessClassificationTraining(trainingEventsArray.get(thresholdStr));
					Const.log.save("ClassificationAnalysisJ48Training.txt", classificationResult);
					
					classificationResult = preprocessManualMultilayerPerceptron[reportCount]
							.printPreprocessClassificationTraining(trainingEventsArray.get(thresholdStr));
					Const.log.save("ClassificationAnalysisMultilayerPerceptronTraining.txt", classificationResult);
					
					
					classificationResult = preprocessManualNaiveBayes[reportCount]
							.printPreprocessClassificationTraining(trainingEventsArray.get(thresholdStr));
					Const.log.save("ClassificationAnalysisNaiveBayesTraining.txt", classificationResult);
					
					classificationResult = preprocessManualSMO[reportCount]
							.printPreprocessClassificationTraining(trainingEventsArray.get(thresholdStr));
					Const.log.save("ClassificationAnalysisSMOTraining.txt", classificationResult);
					
					classificationResult = preprocessManualPART[reportCount]
							.printPreprocessClassificationTraining(trainingEventsArray.get(thresholdStr));
					Const.log.save("ClassificationAnalysisPARTTraining.txt", classificationResult);
					
					classificationResult = preprocessManualRandomForest[reportCount]
							.printPreprocessClassificationTraining(trainingEventsArray.get(thresholdStr));
					Const.log.save("ClassificationAnalysisRandomForestTraining.txt", classificationResult);
					
					
					
					classificationResult = preprocess[reportCount]
							.printPreprocessClassification(testEventsArray.get(thresholdStr));

					Const.log.save("ClassificationAnalysis.txt", classificationResult);
								
					
					classificationResult = preprocessManualJ48[reportCount]
							.printPreprocessClassification(testEventsArray.get(thresholdStr));
					Const.log.save("ClassificationAnalysisJ48.txt", classificationResult);
					
					classificationResult = preprocessManualMultilayerPerceptron[reportCount]
							.printPreprocessClassification(testEventsArray.get(thresholdStr));
					Const.log.save("ClassificationAnalysisMultilayerPerceptron.txt", classificationResult);
					
										
					classificationResult = preprocessManualNaiveBayes[reportCount]
							.printPreprocessClassification(testEventsArray.get(thresholdStr));
					Const.log.save("ClassificationAnalysisNaiveBayes.txt", classificationResult);
					
					classificationResult = preprocessManualSMO[reportCount]
							.printPreprocessClassification(testEventsArray.get(thresholdStr));
					Const.log.save("ClassificationAnalysisSMO.txt", classificationResult);
					
					classificationResult = preprocessManualPART[reportCount]
							.printPreprocessClassification(testEventsArray.get(thresholdStr));
					Const.log.save("ClassificationAnalysisPART.txt", classificationResult);
					
					classificationResult = preprocessManualRandomForest[reportCount]
							.printPreprocessClassification(testEventsArray.get(thresholdStr));
					Const.log.save("ClassificationAnalysisRandomForest.txt", classificationResult);
					
					
					
					thresholdStr = String.format("%.8f", SELECTED_THRESHOLDS_MF[reportCount]);
					String classificationResultMF = preprocessMF[reportCount]
							.printPreprocessClassification(testEventsArrayMF.get(thresholdStr));
					Const.log.save("ClassificationAnalysisMF.txt", classificationResultMF);
					
					classificationResultMF = preprocessManualMFJ48[reportCount]
							.printPreprocessClassification(testEventsArrayMF.get(thresholdStr));
					Const.log.save("ClassificationAnalysisMFJ48.txt", classificationResultMF);
					
					classificationResultMF = preprocessManualMFMultilayerPerceptron[reportCount]
							.printPreprocessClassification(testEventsArrayMF.get(thresholdStr));
					Const.log.save("ClassificationAnalysisMFMultilayerPerceptron.txt", classificationResultMF);
					
					classificationResultMF = preprocessManualMFNaiveBayes[reportCount]
							.printPreprocessClassification(testEventsArrayMF.get(thresholdStr));
					Const.log.save("ClassificationAnalysisMFNaiveBayes.txt", classificationResultMF);
					
					classificationResultMF = preprocessManualMFSMO[reportCount]
							.printPreprocessClassification(testEventsArrayMF.get(thresholdStr));
					Const.log.save("ClassificationAnalysisMFSMO.txt", classificationResultMF);
					
					classificationResultMF = preprocessManualMFPART[reportCount]
							.printPreprocessClassification(testEventsArrayMF.get(thresholdStr));
					Const.log.save("ClassificationAnalysisMFPART.txt", classificationResultMF);
					
					classificationResultMF = preprocessManualMFRandomForest[reportCount]
							.printPreprocessClassification(testEventsArrayMF.get(thresholdStr));
					Const.log.save("ClassificationAnalysisMFRandomForest.txt", classificationResultMF);

					
					classificationResultMF = preprocessMF[reportCount]
							.printPreprocessClassificationTraining(trainingEventsArray.get(thresholdStr));
					Const.log.save("ClassificationAnalysisTrainingMF.txt", classificationResultMF);
										
					classificationResultMF = preprocessManualMFJ48[reportCount]
							.printPreprocessClassificationTraining(trainingEventsArray.get(thresholdStr));
					Const.log.save("ClassificationAnalysisMFJ48Training.txt", classificationResultMF);
					
					classificationResultMF = preprocessManualMFMultilayerPerceptron[reportCount]
							.printPreprocessClassificationTraining(trainingEventsArray.get(thresholdStr));
					Const.log.save("ClassificationAnalysisMFMultilayerPerceptronTraining.txt", classificationResultMF);
					
					
					classificationResultMF = preprocessManualMFNaiveBayes[reportCount]
							.printPreprocessClassificationTraining(trainingEventsArray.get(thresholdStr));
					Const.log.save("ClassificationAnalysisMFNaiveBayesTraining.txt", classificationResultMF);
					
					classificationResultMF = preprocessManualMFSMO[reportCount]
							.printPreprocessClassificationTraining(trainingEventsArray.get(thresholdStr));
					Const.log.save("ClassificationAnalysisMFSMOTraining.txt", classificationResultMF);
					
					classificationResultMF = preprocessManualMFPART[reportCount]
							.printPreprocessClassificationTraining(trainingEventsArray.get(thresholdStr));
					Const.log.save("ClassificationAnalysisMFPARTTraining.txt", classificationResultMF);
					
					classificationResultMF = preprocessManualMFRandomForest[reportCount]
							.printPreprocessClassificationTraining(trainingEventsArray.get(thresholdStr));
					Const.log.save("ClassificationAnalysisMFRandomForestTraining.txt", classificationResultMF);

					
					
					thresholdStr = String.format("%.8f", SELECTED_THRESHOLDS_OLSEN[reportCount]);
					String classificationResultOlsen = filename + "\t" + preprocessOlsen[reportCount]
							.printPreprocessClassification(testEventsArrayOlsen.get(thresholdStr));
					Const.log.save("ClassificationAnalysisOlsen.txt", classificationResultOlsen);
					
					classificationResultOlsen = preprocessManualOlsenJ48[reportCount]
							.printPreprocessClassification(testEventsArrayOlsen.get(thresholdStr));
					Const.log.save("ClassificationAnalysisOlsenJ48.txt", classificationResultOlsen);
					
					classificationResultOlsen = preprocessManualOlsenMultilayerPerceptron[reportCount]
							.printPreprocessClassification(testEventsArrayOlsen.get(thresholdStr));
					Const.log.save("ClassificationAnalysisOlsenMultilayerPerceptron.txt", classificationResultOlsen);
					
					classificationResultOlsen = preprocessManualOlsenNaiveBayes[reportCount]
							.printPreprocessClassification(testEventsArrayOlsen.get(thresholdStr));
					Const.log.save("ClassificationAnalysisOlsenNaiveBayes.txt", classificationResultOlsen);
					
					classificationResultOlsen = preprocessManualOlsenSMO[reportCount]
							.printPreprocessClassification(testEventsArrayOlsen.get(thresholdStr));
					Const.log.save("ClassificationAnalysisOlsenSMO.txt", classificationResultOlsen);
					
					classificationResultOlsen = preprocessManualOlsenPART[reportCount]
							.printPreprocessClassification(testEventsArrayOlsen.get(thresholdStr));
					Const.log.save("ClassificationAnalysisOlsenPART.txt", classificationResultOlsen);
					
					classificationResultOlsen = preprocessManualOlsenRandomForest[reportCount]
							.printPreprocessClassification(testEventsArrayOlsen.get(thresholdStr));
					Const.log.save("ClassificationAnalysisOlsenRandomForest.txt", classificationResultOlsen);
					
					
					classificationResultOlsen = preprocessOlsen[reportCount]
							.printPreprocessClassificationTraining(trainingEventsArrayOlsen.get(thresholdStr));
					Const.log.save("ClassificationAnalysisTrainingOlsen.txt", classificationResultOlsen);
					
					
					classificationResultOlsen = preprocessManualOlsenJ48[reportCount]
							.printPreprocessClassificationTraining(trainingEventsArray.get(thresholdStr));
					Const.log.save("ClassificationAnalysisOlsenJ48Training.txt", classificationResultOlsen);
					
					classificationResultOlsen = preprocessManualOlsenMultilayerPerceptron[reportCount]
							.printPreprocessClassificationTraining(trainingEventsArray.get(thresholdStr));
					Const.log.save("ClassificationAnalysisOlsenMultilayerPerceptronTraining.txt", classificationResultOlsen);
					
					
					classificationResultOlsen = preprocessManualOlsenNaiveBayes[reportCount]
							.printPreprocessClassificationTraining(trainingEventsArray.get(thresholdStr));
					Const.log.save("ClassificationAnalysisOlsenNaiveBayesTraining.txt", classificationResultOlsen);
					
					classificationResultOlsen = preprocessManualOlsenSMO[reportCount]
							.printPreprocessClassificationTraining(trainingEventsArray.get(thresholdStr));
					Const.log.save("ClassificationAnalysisOlsenSMOTraining.txt", classificationResultOlsen);
					
					classificationResultOlsen = preprocessManualOlsenPART[reportCount]
							.printPreprocessClassificationTraining(trainingEventsArray.get(thresholdStr));
					Const.log.save("ClassificationAnalysisOlsenPARTTraining.txt", classificationResultOlsen);
					
					classificationResultOlsen = preprocessManualOlsenRandomForest[reportCount]
							.printPreprocessClassificationTraining(trainingEventsArray.get(thresholdStr));
					Const.log.save("ClassificationAnalysisOlsenRandomForestTraining.txt", classificationResultOlsen);
					

				}

			
				
				mDD = filename + " \t" + curvePerfectForesight[reportCount].getMaxMddBase() + "\t"
						+ curveCifre[reportCount].getMaxMddBase() + "\t"
						+ curveClassifcation[reportCount].getMaxMddBase() + "\t"
						+ curveProbabilityGP[reportCount].getMaxMddBase() + "\t"
						+ curveNoClassificationNoRegressionGP[reportCount].getMaxMddBase() + "\t"
						+ curveCifreJ48[reportCount].getMaxMddBase() + "\t"
						+ curveCifreMultilayerPerceptron[reportCount].getMaxMddBase() + "\t"
						+ curveCifreNaiveBayes[reportCount].getMaxMddBase() + "\t"
						+ curveCifreSMO[reportCount].getMaxMddBase() + "\t"
						+ curveCifrePART[reportCount].getMaxMddBase() + "\t"
						+ curveCifreRandomForest[reportCount].getMaxMddBase() + "\t"
						+ curveRandomGP[reportCount].getMaxMddBase() + "\t"
						
						+ curvePerfectForesightMF[reportCount].getMaxMddBase() + "\t"
						+ curveMF[reportCount].getMaxMddBase() + "\t"
						+ curveClassifcationMF[reportCount].getMaxMddBase() + "\t"
						+ curveProbabilityMF[reportCount].getMaxMddBase() + "\t"
						+ curveNoClassificationNoRegressionMF[reportCount].getMaxMddBase() + "\t"
						+ curveMFJ48[reportCount].getMaxMddBase() + "\t"
						+ curveMFMultilayerPerceptron[reportCount].getMaxMddBase() + "\t"
						+ curveMFNaiveBayes[reportCount].getMaxMddBase() + "\t"
						+ curveMFSMO[reportCount].getMaxMddBase() + "\t"
						+ curveMFPART[reportCount].getMaxMddBase() + "\t"
						+ curveMFRandomForest[reportCount].getMaxMddBase() + "\t"
						+ curveRandomMF[reportCount].getMaxMddBase() + "\t"
						
						+ curvePerfectForesightOlsen[reportCount].getMaxMddBase() + "\t"
						+ curveOlsen[reportCount].getMaxMddBase() + "\t"
						+ curveClassificationOlsen[reportCount].getMaxMddBase() + "\t"
						+ curveProbabilityOlsen[reportCount].getMaxMddBase() + "\t"
						+ curveNoClassificationNoRegressionOlsen[reportCount].getMaxMddBase() + "\t"
						+ curveOlsenJ48[reportCount].getMaxMddBase() + "\t"
						+ curveOlsenMultilayerPerceptron[reportCount].getMaxMddBase() + "\t"
						+ curveOlsenNaiveBayes[reportCount].getMaxMddBase()  + "\t"
						+ curveOlsenSMO[reportCount].getMaxMddBase()  + "\t"
						+ curveOlsenPART[reportCount].getMaxMddBase()  + "\t"
						+ curveOlsenRandomForest[reportCount].getMaxMddBase()+ "\t"
						+ curveRandomOlsen[reportCount].getMaxMddBase();
						
				Const.log.save("mddBaseCcy.txt", mDD);

				mDD = filename + " \t" + curvePerfectForesight[reportCount].getMaxMddQuote() + "\t"
						+ curveCifre[reportCount].getMaxMddQuote() + "\t"
						+ curveClassifcation[reportCount].getMaxMddQuote() + "\t"
						+ curveProbabilityGP[reportCount].getMaxMddQuote() + "\t"
						+ curveNoClassificationNoRegressionGP[reportCount].getMaxMddQuote() + "\t"
						+ curveCifreJ48[reportCount].getMaxMddQuote() + "\t"
						+ curveCifreMultilayerPerceptron[reportCount].getMaxMddQuote() + "\t"
						+ curveCifreNaiveBayes[reportCount].getMaxMddQuote() + "\t"
						+ curveCifreSMO[reportCount].getMaxMddQuote() + "\t"
						+ curveCifrePART[reportCount].getMaxMddQuote() + "\t"
						+ curveCifreRandomForest[reportCount].getMaxMddQuote() + "\t"
						+ curveRandomGP[reportCount].getMaxMddQuote() + "\t"
														
						+ curvePerfectForesightMF[reportCount].getMaxMddQuote() + "\t"
						+ curveMF[reportCount].getMaxMddQuote() + "\t"
						+ curveClassifcationMF[reportCount].getMaxMddQuote() + "\t"
						+ curveProbabilityMF[reportCount].getMaxMddBase() + "\t"
						+ curveMFJ48[reportCount].getMaxMddQuote() + "\t"
						+ curveMFMultilayerPerceptron[reportCount].getMaxMddQuote() + "\t"
						+ curveMFNaiveBayes[reportCount].getMaxMddQuote() + "\t"
						+ curveMFSMO[reportCount].getMaxMddQuote() + "\t"
						+ curveMFPART[reportCount].getMaxMddQuote() + "\t"
						+ curveMFRandomForest[reportCount].getMaxMddQuote() + "\t"
						+ curveRandomMF[reportCount].getMaxMddBase() + "\t"
						
						+ curvePerfectForesightOlsen[reportCount].getMaxMddQuote() + "\t"
						+ curveOlsen[reportCount].getMaxMddQuote() + "\t"
						+ curveClassificationOlsen[reportCount].getMaxMddQuote() + "\t"
						+ curveProbabilityOlsen[reportCount].getMaxMddBase() + "\t"
						+ curveOlsenJ48[reportCount].getMaxMddQuote() + "\t"
						+ curveOlsenMultilayerPerceptron[reportCount].getMaxMddQuote() + "\t"
						+ curveOlsenNaiveBayes[reportCount].getMaxMddQuote() + "\t"
						+ curveOlsenSMO[reportCount].getMaxMddQuote() + "\t"
						+ curveOlsenPART[reportCount].getMaxMddQuote() + "\t"
						+ curveOlsenRandomForest[reportCount].getMaxMddQuote() + "\t"
						+ curveRandomOlsen[reportCount].getMaxMddBase();
				Const.log.save("mddQuoteCcy.txt", mDD);

				transactions = filename + " \t" + +curvePerfectForesight[reportCount].getNumberOfQuoteCcyTransactions()
						+ "\t" + curveCifre[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveClassifcation[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveProbabilityGP[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveNoClassificationNoRegressionGP[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curvePerfectForesightMF[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveCifreJ48[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveCifreMultilayerPerceptron[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveCifreNaiveBayes[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveCifreSMO[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveCifrePART[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveCifreRandomForest[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveRandomGP[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						
						+ curveMF[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveClassifcationMF[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveProbabilityMF[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveNoClassificationNoRegressionMF[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveMFJ48[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveMFMultilayerPerceptron[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveMFNaiveBayes[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveMFSMO[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveMFPART[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveMFRandomForest[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveRandomMF[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
								
						+ curvePerfectForesightOlsen[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveOlsen[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveClassificationOlsen[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveProbabilityOlsen[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveNoClassificationNoRegressionOlsen[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveOlsenJ48[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveOlsenMultilayerPerceptron[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveOlsenNaiveBayes[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveOlsenSMO[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveOlsenPART[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveOlsenRandomForest[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveRandomOlsen[reportCount].getNumberOfQuoteCcyTransactions() ;
				Const.log.save("NumberOfQuoteCCyTransaction.txt", transactions);

				transactions = filename + " \t" + +curvePerfectForesight[reportCount].getNumberOfBaseCcyTransactions()
						+ "\t" + curveCifre[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveClassifcation[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveProbabilityGP[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveNoClassificationNoRegressionGP[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveCifreJ48[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveCifreMultilayerPerceptron[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveCifreNaiveBayes[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveCifreSMO[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveCifrePART[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveCifreRandomForest[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveRandomGP[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						
						+ curvePerfectForesightMF[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveMF[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveClassifcationMF[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveProbabilityMF[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveNoClassificationNoRegressionMF[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveMFJ48[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveMFMultilayerPerceptron[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveMFNaiveBayes[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveMFSMO[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveMFPART[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveMFRandomForest[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveRandomMF[reportCount].getNumberOfBaseCcyTransactions() + "\t"			
					
						+ curvePerfectForesightOlsen[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveOlsen[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveClassificationOlsen[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveProbabilityOlsen[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveNoClassificationNoRegressionOlsen[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveOlsenJ48[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveOlsenMultilayerPerceptron[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveOlsenNaiveBayes[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveOlsenSMO[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveOlsenPART[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveOlsenRandomForest[reportCount].getNumberOfBaseCcyTransactions()  + "\t"
						+ curveRandomOlsen[reportCount].getNumberOfBaseCcyTransactions();
				Const.log.save("NumberOfBaseCCyTransaction.txt", transactions);

				if (preprocess[reportCount] != null)
					preprocess[reportCount].removeTempFiles();

				String tempFolderName = preprocess[reportCount].tempFilePath.get(0).substring(0,
						preprocess[reportCount].tempFilePath.get(0).lastIndexOf(File.separator));

				File dir = new File(tempFolderName);
				if (!dir.isDirectory())
					continue;

				File[] tempFile = dir.getParentFile().listFiles(new FilenameFilter() {
					public boolean accept(File dir, String name) {
						return name.startsWith("autoweka" + filename);
					}
				});

				for (int tempFileCount = 0; tempFileCount < tempFile.length; tempFileCount++) {
					try {
						preprocess[reportCount].deleteDirectoryRecursionJava6(tempFile[tempFileCount]);
					} catch (IOException e) {

						System.out.println("Unable to delete one of the directory");
					}
				}
				currentProcessorCounter = reportCount;

				int maxTransaction = -1;

				maxTransaction = Math.max(curvePerfectForesight[reportCount].getMaxTransactionSize(),
						curveClassifcation[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, curveCifre[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, curveProbabilityGP[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, curveNoClassificationNoRegressionGP[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, curveCifreJ48[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, curveCifreMultilayerPerceptron[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, curveCifreNaiveBayes[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, curveCifreSMO[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, curveCifrePART[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, curveCifreRandomForest[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, curveRandomGP[reportCount].getMaxTransactionSize());
				
				
				maxTransaction = Math.max(maxTransaction, curvePerfectForesightMF[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, curveMF[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, curveClassifcationMF[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, curveProbabilityMF[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, curveNoClassificationNoRegressionMF[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, curveMFJ48[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, curveMFMultilayerPerceptron[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, curveMFNaiveBayes[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, curveMFSMO[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, curveMFPART[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, curveMFRandomForest[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, curveRandomMF[reportCount].getMaxTransactionSize());
				
				maxTransaction = Math.max(maxTransaction,curvePerfectForesightOlsen[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, curveOlsen[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction,curveClassificationOlsen[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, curveProbabilityOlsen[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, curveNoClassificationNoRegressionOlsen[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, curveOlsenJ48[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, curveOlsenMultilayerPerceptron[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, curveOlsenNaiveBayes[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, curveOlsenSMO[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, curveOlsenPART[reportCount].getMaxTransactionSize());                                                                                                          
				maxTransaction = Math.max(maxTransaction, curveOlsenRandomForest[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, curveRandomOlsen[reportCount].getMaxTransactionSize());

				profit = "Dataset \t PerfectForesight+GP \t GP  \t Classifier+GP \t Random+GP \t PerfectForesight+MF \t MF \t Classifier+MF \t Random+MF \t PerfectForesight+Olsen \t Olsen \t  Classifier+Olsen \t Random+Olsen \t PerfectForesight+H \t H \t Classifier+H \t Random+H \t PerfectForesight+HSplitDCUp_Down \t HSplitDCUp_Down\t Classifier+HSplitDCUp_Down \t Random+HSplitDCUp_Down \t rsi \t ema \t macd  ";
				FWriter TransactionWriter = new FWriter(Const.log.publicFolder + "TransactionCount.txt");
				Const.log.save("TransactionCount.txt", profit);

				for (int k = 0; k < maxTransaction; k++) {

					profit = filename + " \t" + curvePerfectForesight[reportCount].getTransanction(k) + "\t"
							+ curveCifre[reportCount].getTransanction(k) + "\t"
							+ curveClassifcation[reportCount].getTransanction(k) + "\t"
							+ curveProbabilityGP[reportCount].getNumberOfBaseCcyTransactions() + "\t"
							+ curveNoClassificationNoRegressionGP[reportCount].getNumberOfBaseCcyTransactions() + "\t"
							+ curveCifreJ48[reportCount].getNumberOfBaseCcyTransactions() + "\t"
							+ curveCifreMultilayerPerceptron[reportCount].getNumberOfBaseCcyTransactions() + "\t"
							+ curveCifreNaiveBayes[reportCount].getNumberOfBaseCcyTransactions() + "\t"
							+ curveCifreSMO[reportCount].getNumberOfBaseCcyTransactions() + "\t"
							+ curveCifrePART[reportCount].getNumberOfBaseCcyTransactions() + "\t"
							+ curveCifreRandomForest[reportCount].getNumberOfBaseCcyTransactions() + "\t"
							+ curveRandomGP[reportCount].getNumberOfBaseCcyTransactions() + "\t"
							
							+ curvePerfectForesightMF[reportCount].getTransanction(k) + "\t"
							+ curveMF[reportCount].getTransanction(k) + "\t"
							+ curveClassifcationMF[reportCount].getTransanction(k) + "\t"
							+ curveProbabilityMF[reportCount].getNumberOfBaseCcyTransactions() + "\t"
							+ curveNoClassificationNoRegressionMF[reportCount].getNumberOfBaseCcyTransactions() + "\t"
							+ curveMFJ48[reportCount].getNumberOfBaseCcyTransactions() + "\t"
							+ curveMFMultilayerPerceptron[reportCount].getNumberOfBaseCcyTransactions() + "\t"
							+ curveMFNaiveBayes[reportCount].getNumberOfBaseCcyTransactions() + "\t"
							+ curveMFSMO[reportCount].getNumberOfBaseCcyTransactions() + "\t"
							+ curveMFPART[reportCount].getNumberOfBaseCcyTransactions() + "\t"
							+ curveMFRandomForest[reportCount].getNumberOfBaseCcyTransactions() + "\t"
							+ curveRandomMF[reportCount].getNumberOfBaseCcyTransactions() + "\t"
									
									
							+ curvePerfectForesightOlsen[reportCount].getTransanction(k) + "\t"
							+ curveOlsen[reportCount].getTransanction(k) + "\t"
							+ curveClassificationOlsen[reportCount].getTransanction(k) + "\t"
							+ curveProbabilityOlsen[reportCount].getNumberOfBaseCcyTransactions() + "\t"
							+ curveNoClassificationNoRegressionOlsen[reportCount].getNumberOfBaseCcyTransactions() + "\t"
							+ curveOlsenJ48[reportCount].getNumberOfBaseCcyTransactions() + "\t"
							+ curveOlsenMultilayerPerceptron[reportCount].getNumberOfBaseCcyTransactions() + "\t"
							+ curveOlsenNaiveBayes[reportCount].getNumberOfBaseCcyTransactions() + "\t"
							+ curveOlsenSMO[reportCount].getNumberOfBaseCcyTransactions() + "\t"
							+ curveOlsenPART[reportCount].getNumberOfBaseCcyTransactions() + "\t"
							+ curveOlsenRandomForest[reportCount].getNumberOfBaseCcyTransactions() + "\t"
							+ curveRandomOlsen[reportCount].getNumberOfBaseCcyTransactions();
					Const.log.save("TransactionCount.txt", profit);

				}

				profit = "Dataset \t PerfectForesight+GP \t GP  \t Classifier+GP \t Random+GP \t PerfectForesight+MF \t MF \t Classifier+MF \t Random+MF \t PerfectForesight+Olsen \t Olsen \t  Classifier+Olsen \t Random+Olsen \t PerfectForesight+H \t H \t Classifier+H \t Random+H \t PerfectForesight+HSplitDCUp_Down \t HSplitDCUp_Down\t Classifier+HSplitDCUp_Down \t Random+HSplitDCUp_Down \t rsi \t ema \t macd  ";
				FWriter stdDevWriter = new FWriter(Const.log.publicFolder + "stdDev.txt");
				Const.log.save("stdDev.txt", profit);

				profit = filename + " \t" + curvePerfectForesight[reportCount].calculateSD() + "\t"
						+ curveCifre[reportCount].calculateSD() + "\t" 
						+ curveClassifcation[reportCount].calculateSD() + "\t" 
						+ curveProbabilityGP[reportCount].calculateSD() + "\t"
						+ curveNoClassificationNoRegressionGP[reportCount].calculateSD() + "\t"
						+ curveCifreJ48[reportCount].calculateSD() + "\t"
						+ curveCifreMultilayerPerceptron[reportCount].calculateSD() + "\t"
						+ curveCifreNaiveBayes[reportCount].calculateSD() + "\t"
						+ curveCifreSMO[reportCount].calculateSD() + "\t"
						+ curveCifrePART[reportCount].calculateSD() + "\t"
						+ curveCifreRandomForest[reportCount].calculateSD() + "\t"
						+ curveRandomGP[reportCount].calculateSD() + "\t"
						
						+ curvePerfectForesightMF[reportCount].calculateSD() + "\t" 
						+ curveMF[reportCount].calculateSD() + "\t" 
						+ curveClassifcationMF[reportCount].calculateSD() + "\t"
						+ curveProbabilityMF[reportCount].calculateSD() + "\t"
						+ curveNoClassificationNoRegressionMF[reportCount].calculateSD() + "\t"
						+ curveMFJ48[reportCount].calculateSD() + "\t"
						+ curveMFMultilayerPerceptron[reportCount].calculateSD() + "\t"
						+ curveMFNaiveBayes[reportCount].calculateSD() + "\t"
						+ curveMFSMO[reportCount].calculateSD() + "\t"
						+ curveMFPART[reportCount].calculateSD() + "\t"
						+ curveMFRandomForest[reportCount].calculateSD() + "\t"
						+ curveRandomMF[reportCount].calculateSD() + "\t"
										
						+ curvePerfectForesightOlsen[reportCount].calculateSD() + "\t"
						+ curveOlsen[reportCount].calculateSD() + "\t"
						+ curveClassificationOlsen[reportCount].calculateSD() + "\t"
						+ curveProbabilityOlsen[reportCount].calculateSD() + "\t"
						+ curveNoClassificationNoRegressionOlsen[reportCount].calculateSD() + "\t"
						+ curveOlsenJ48[reportCount].calculateSD() + "\t"
						+ curveOlsenMultilayerPerceptron[reportCount].calculateSD()  + "\t" 
						+ curveOlsenNaiveBayes[reportCount].calculateSD()  + "\t"
						+ curveOlsenSMO[reportCount].calculateSD()  + "\t"
						+ curveOlsenPART[reportCount].calculateSD()  + "\t"
						+ curveOlsenRandomForest[reportCount].calculateSD();

				Const.log.save("stdDev.txt", profit);
			}
		}

	}

	public static void main(String[] args) throws Exception {

		/*
		 * File dir = new File("c:\\Users\\atna3\\AppData\\Local\\Temp"); File[]
		 * foundFiles = dir.listFiles(new FilenameFilter() { public boolean
		 * accept(File dir, String name) { //System.out.println(
		 * name.startsWith("autoweka")); return name.startsWith("autoweka"); }
		 * });
		 * 
		 * for (int k =0; k< foundFiles.length; k++){
		 * System.out.println(foundFiles[k].toString());
		 * Runtime.getRuntime().exec(
		 * "cmd.exe /c for /d %x in (c:\\Users\\atna3\\AppData\\Local\\Temp\\autoweka*) do rd /s /q \"%x\" "
		 * ); }
		 */
		// Split the long parameter file , according to the delimiter
		String s[] = args[0].split(":");
		if (s.length < 6) {
			System.out.println(
					"Expect 6 parameters: <file path:file name:training index start:training index end:test index start:test index end>");
			System.exit(1);
		}

		if (s[6] != null) {
			switch (Const.hashFunctionType(s[6])) {
			case eMichaelFernando:
				Const.OsFunctionEnum = Const.function_code.eMichaelFernando;
				break;
			case eGP:
				Const.OsFunctionEnum = Const.function_code.eGP;
				break;
			default:
				Const.OsFunctionEnum = Const.function_code.eMichaelFernando;
				break;
			}
		} else {
			Const.OsFunctionEnum = Const.function_code.eMichaelFernando;
		}

		try {
			if (s[7] != null) {
				try {
					if (s[7].compareToIgnoreCase("TRUE") == 0)
						Const.splitDatasetByTrendType = true;
				} catch (NumberFormatException nfe) {
					Const.splitDatasetByTrendType = false;
				}
			}
		} catch (ArrayIndexOutOfBoundsException aie) {
			Const.splitDatasetByTrendType = false;
		}

		if (s[8].compareToIgnoreCase("TRUE") == 0) {
			Const.LINEAR_FUNCTIONALITY_ONLY = true;
			Const.FUNCTION_NODE_DEFINITION = "LINEAR";
			// System.out.println("LINEAR_FUNCTIONALITY_ONLY is TRUE");
		} else {
			// System.out.println("LINEAR_FUNCTIONALITY_ONLY is FALSE");
			Const.FUNCTION_NODE_DEFINITION = "NON_LINEAR";
			Const.LINEAR_FUNCTIONALITY_ONLY = false;
		}
		Const.NEGATIVE_EXPRESSION_REPLACEMENT = Integer.parseInt(s[9]);

		try {
			if (s[10] != null) {
				try {
					Const.NUM_OF_PROCESSORS = Integer.parseInt(s[10]);
				} catch (NumberFormatException nfe) {
					Const.NUM_OF_PROCESSORS = 5;
				}
			}
		} catch (ArrayIndexOutOfBoundsException aie) {
			Const.NUM_OF_PROCESSORS = 5;
		}

		if (s[11].compareToIgnoreCase("TRUE") == 0) {
			Const.REUSE_EXISTING_TREE = true;
			// System.out.println("RECREATE_TREE will be recreated");
		} else {
			// System.out.println("RECREATE_TREE will not be recreated if
			// found");
			Const.REUSE_EXISTING_TREE = false;
		}

		double test = 0.0;

		if (s.length >= 12) {
			switch (Const.hashtreeStructurePostcreateType(s[12])) {
			case ePruneAndEqualERCAndExternalInputLeaf:
				TreeHelperClass.treeStructurePostcreateObj = treeStructurePostcreate.ePruneAndEqualERCAndExternalInputLeaf;
				break;
			case ePrune:
				TreeHelperClass.treeStructurePostcreateObj = treeStructurePostcreate.ePrune;
				break;
			case eEqualERCAndExternalInputLeaf:
				TreeHelperClass.treeStructurePostcreateObj = treeStructurePostcreate.eEqualERCAndExternalInputLeaf;
				break;
			case eRandom:
				TreeHelperClass.treeStructurePostcreateObj = treeStructurePostcreate.eRandom;
				break;
			default:
				TreeHelperClass.treeStructurePostcreateObj = treeStructurePostcreate.eRandom;
				break;
			}
		}

		Const.log = new Logger(s[1], s[3], s[4]);

		// TreeHelperClass.treeStructurePostcreateObj

	//	s[0], Integer.parseInt(s[2]), Integer.parseInt(s[3]),
	//	Integer.parseInt(s[4]), Integer.parseInt(s[5]), Integer.parseInt(args[1]), Integer.parseInt(args[2]),
	//	Integer.parseInt(args[3]), Double.parseDouble(args[4]), Integer.parseInt(args[5]), test
		
		
		//s[0], 0, 3,
		//4, 5, 700,30,
		//7, 0.7, 5, test
		
		//SymbolicRegression ga = new SymbolicRegression(s[0], 0, 3,4, 5, 700,30,7, 0.7, 5, test);
		SymbolicRegression ga = new SymbolicRegression(s[0], Integer.parseInt(s[2]), Integer.parseInt(s[3]),
				Integer.parseInt(s[4]), Integer.parseInt(s[5]), Integer.parseInt(args[1]), Integer.parseInt(args[2]),
				Integer.parseInt(args[3]), Double.parseDouble(args[4]), Integer.parseInt(args[5]), test);
			/*
		 * 
		 * // Clear any remaining tmp files Process p1 = null; if
		 * (System.getProperty("os.name").contains("Windows")){
		 * //Runtime.getRuntime().exec(
		 * "cmd.exe /c for /d %i in (c:\\Users\\atna3\\AppData\\Local\\Temp\\autoweka*) do echo %i "
		 * ); p1 = Runtime.getRuntime().exec(
		 * "cmd.exe /c for /d %x in (c:\\Users\\atna3\\AppData\\Local\\Temp\\autoweka*) do rd /s /q \"%x\" "
		 * ); } else { String[] cmd = { "/bin/sh", "-c", "rm -rf /tmp/autoweka*"
		 * }; p1 = Runtime.getRuntime().exec(cmd);
		 * 
		 * } try { p1.waitFor(); } catch (InterruptedException e) { // TODO
		 * Auto-generated catch block e.printStackTrace(); }
		 * 
		 */
		// TreeHelperClass.readFitnessFile(2);
	}

	void addGPTreeToArray(String gpFileName, int arrayPosition) {
		TreeOperation opr = null;
		try {
			opr = new TreeOperation(gpFileName, 0.0);
		} catch (FileNotFoundException e) {

			e.printStackTrace();
			System.exit(0);
		}
		if (opr != null) {
			opr.populateGpTreeVector();

			AbstractNode mytree = opr.getTree();
			// String testStr = TreeHelperClass.printTreeToString(mytree, 0);

			// System.out.println(testStr);
			Const.GP_TREES[arrayPosition] = mytree;
			Const.GP_TREES_STRING[arrayPosition] = mytree.printAsInFixFunction();
		}

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
	
	private static <K, V extends Comparable<? super V>> List<Entry<K, V>> findLeast(Map<K, V> map, int n) {
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

/*
 * Fix selectedThresholdsTraining.txt selectedThresholdsTest.txt
 * SimpleTradingTraining.txt not reporting all
 * 
 */
