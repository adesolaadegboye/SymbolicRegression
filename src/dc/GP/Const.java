package dc.GP;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import dc.GP.Const.function_code;
import dc.io.Logger;

public final class Const {

	 public static  int NUMBER_OF_INPUTS = 1;
	// public static  int MAX_TREE_DEPTH = 2;
	 public static int VARIABLE_EVALUATED= 0;
	 public static final String MAGNITUDE_NODE_LABEL = "InputVal:X";
	 public static final String DISTANCE_NODE_LABEL = "InputVal:Y";
	 public static final double MUTATION_THRESH = 0.1;
	 public static final  int NUMBER_OF_SELECTED_THRESHOLDS = 5;
	 public static int POP_SIZE = -1;
	 public static int NUMBER_OF_THRESHOLDS = 10; //100;
	 public static int ELISTISM_COUNT = 5;
	 public static int MAX_GENERATIONS = 37;
	 public static int TOURNAMENT_SIZE = 7;
	 public static int NUM_OF_PROCESSORS = 5;
	 public static double CROSSOVER_PROB = 0.9;
	 public static int MAX_DEPTH = 5;
	 public static final double EVOLUTION_RATIO = 0.8; 
	 public static ArrayList<Double> threshold = new ArrayList<Double>();
	 public static final double[] doubles = 
			 new Random().doubles(0.5, 7.5).distinct().limit(15000).toArray(); // generate 300 distinct random doubles
	 public static final String VARIABLE_1 = "X0";
	 public static final String VARIABLE_2 = "X1";
	 public static final String UPWARD_EVENT_STRING = "UPWARD_DC_";
	 public static final String DOWNWARD_EVENT_STRING = "DOWNWARD_DC_";
	 public static final String INNER_NODE_TYPE = "innerNode";
	 public static final String LEAF_NODE_TYPE = "leafNode";
	 public static boolean REUSE_EXISTING_TREE = false;
	 public static boolean splitDatasetByTrendType = false; // Reads input
		// parameter
	 public static boolean INCLUDE_ZERO_OS_ITEMS= true;
	 
	 public enum treeStructurePostcreate {
		 	eNone,
			eRandom, // this is default we have at the moment
			ePrune,
			eEqualERCAndExternalInputLeaf,
			ePruneAndEqualERCAndExternalInputLeaf,
		}
		
	public enum function_code {
		eGP, eMichaelFernando,eOlsen,
	}

	public static function_code hashFunctionType(String inString) {
		if (inString.contains("GP"))   return function_code.eGP;
		if (inString.contains("MichaelFernando") ) return function_code.eMichaelFernando;
		if (inString.contains("Olsen") ) return function_code.eOlsen;
				
		throw new IllegalArgumentException(); 
		//return string_code.eNone;
	}

	public static String hashFunctionTypeToString(function_code enumCode) {
		if (Const.OsFunctionEnum == Const.function_code.eGP)  return "GP";
		if (Const.OsFunctionEnum == Const.function_code.eMichaelFernando)  return "MichaelFernando";
		if (Const.OsFunctionEnum == Const.function_code.eOlsen)  return "Olsen";
		
				
		throw new IllegalArgumentException(); 
		//return string_code.eNone;
	}
	public	static Const.treeStructurePostcreate hashtreeStructurePostcreateType(String inString) {
			if (inString.contains("PruneAndEqualERCAndExternalInputLeaf")) return Const.treeStructurePostcreate.ePruneAndEqualERCAndExternalInputLeaf;
			if (inString.contains("Random"))   return Const.treeStructurePostcreate.eRandom;
			if (inString.contains("Prune") ) return Const.treeStructurePostcreate.ePrune;
			if (inString.contains("EqualERCAndExternalInputLeaf") ) return Const.treeStructurePostcreate.eEqualERCAndExternalInputLeaf;
			if (inString.contains("None") ) return Const.treeStructurePostcreate.eRandom;
					
			throw new IllegalArgumentException(); 
			//return string_code.eNone;
		}

	// public static boolean LINEAR_FUNCTIONALITY_ONLY = false;
	// public static int NEGATIVE_EXPRESSION_REPLACEMENT = 5;
	public static String FUNCTION_NODE_DEFINITION = null;
	public static Logger log;
	public static Map<Double, String> thresholdClassifcationBasedGPStringDownwardMap = new HashMap<Double, String>();
	public static Map<Double, String> thresholdClassifcationBasedGPStringUpwardMap = new HashMap<Double, String>();
	public static Map<Double, String> thresholdGPStringDownwardMagnitudeMap = new HashMap<Double, String>();
	public static Map<Double, String> thresholdGPStringUpwardMagnitudeMap = new HashMap<Double, String>();
	public static Map<Double, String> thresholdGPStringDownwardMap = new HashMap<Double, String>();
	public static Map<Double, String> thresholdGPStringUpwardMap = new HashMap<Double, String>();
	public static String file_Name;
	public static Const.function_code OsFunctionEnum;
	public static AbstractNode[] GP_TREES;
	public static String[] GP_TREES_STRING;
	 
}
