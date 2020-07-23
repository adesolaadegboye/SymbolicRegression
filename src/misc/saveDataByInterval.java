package misc;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import dc.GP.Const;
import dc.io.FReader;
import dc.io.Logger;
import weka.classifiers.Classifier;
import weka.classifiers.CostMatrix;
import weka.classifiers.bayes.net.EditableBayesNet;
import weka.classifiers.functions.Logistic;
import weka.classifiers.meta.CostSensitiveClassifier;
import weka.classifiers.rules.DecisionTable;
import weka.classifiers.rules.JRip;
import weka.classifiers.rules.PART;
import weka.classifiers.trees.DecisionStump;
import weka.classifiers.trees.J48;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.net.BayesNetGenerator;




public class saveDataByInterval {

	
		


	

	public static void main(String[] args) throws Exception {
		
		FReader.saveDataByInterval("fx-spot_GBP_JPY_hqtick_201308.txt", 10);
		//TreeHelperClass.readFitnessFile(2);
	}
}

