package dc.GP;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;


import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import dc.GP.Const.treeStructurePostcreate;
import dc.ga.DCCurve.Event;
import dc.ga.DCCurve.Type;
import dc.ga.GA;
import dc.io.FReader;
import dc.io.Logger;
import dc.io.FReader.FileMember2;
import files.FWriter;


public class TreeHelperClass {

	static Random rd = new Random();

	private static ExecutorService threadPool;
	private static int Number_Of_Trees = 0;
	private static Event[] Directional_changes_length;
	//private static boolean SPLIT_DATASET;
	private static int number_of_generations = 0;
	

	//private static trend_type_code datasetTrend = trend_type_code.enumUnknown;

	public  Vector<AbstractNode> bestTreesInRuns = new Vector<AbstractNode>();

	static Map<String, List<FitnessClass>> fitnessList = new HashMap<String, List<FitnessClass>>();
	public static treeStructurePostcreate treeStructurePostcreateObj = treeStructurePostcreate.ePrune;
	 ScriptEngineManager mgr = new ScriptEngineManager();
	ScriptEngine engine = mgr.getEngineByName("JavaScript");
	public enum trend_type_code {
		enumUnknown, enumUptrend, enumDowntrend,
	}

	static trend_type_code hashFunction(String inString) {
		if (inString.contains("Uptrend"))
			return trend_type_code.enumUptrend;
		if (inString.contains("DownTrend"))
			return trend_type_code.enumDowntrend;

		throw new IllegalArgumentException();
		// return string_code.eNone;
	}
	

	public static int getDepth(AbstractNode node, int depth) {
		// String str = node.getClass()..getCanonicalName();
		// int numOfChildren = node.getNumChildren();
		if (node == null)
			return (0);
		if (node.getNumChildren() == 0) {
			return (depth);
		}
		if (node.getNumChildren() == 1) {
			return (getDepth(node.getChildren().get(0), depth + 1));
		}
		if (node.getNumChildren() == 2) {
			return (java.lang.Math.max(getDepth(node.getChildren().get(0), depth + 1),
					getDepth(node.getChildren().get(1), depth + 1)));
		}

		return depth;
	}

	public static int mutateDepth(AbstractNode node, int depth, int excludeNode) {
		// String str = node.getClass()..getCanonicalName();
		// int numOfChildren = node.getNumChildren();
		if (node == null)
			return (0);
		if (node.getNumChildren() == 0) {
			return (depth);
		}
		if (node.getNumChildren() == 1) {
			return (getDepth(node.getChildren().get(0), depth + 1));
		}
		if (node.getNumChildren() == 2) {
			return (java.lang.Math.max(getDepth(node.getChildren().get(0), depth + 1),
					getDepth(node.getChildren().get(1), depth + 1)));
		}

		return depth;
	}

	static AbstractNode createRandomSubTree(int depth, String nodeName) {
		AbstractNode retFtn = null;
		if (depth > Const.MAX_DEPTH)
			retFtn = createAlternateRandomRegressionLeafNode(nodeName);
		else
			retFtn = createRandomRegressionNode(false);

		if (retFtn.getNumChildren() <= 0)
			return retFtn;
		else {
			createRandomTreeBranches(retFtn, depth);
		}
		return retFtn;
	}

	public static String mutateTree(AbstractNode node) {

		String treeToStr = printTreeToString(node, 0);
		// System.out.println(getDepth(node, 0));
		String lines[] = treeToStr.split("\\r?\\n");
		// System.out.println(Arrays.toString(lines));
		// System.out.println(treeToStr);
		int idx = new Random().nextInt(lines.length - 1 - 1 + 1) + 1;
		String random = (lines[idx]);
		long count = random.codePoints().filter(ch -> ch == '*').count();
		// recreate node from String
		String nodeToExclude = lines[idx] + "\n";
		Vector<String> vector = new Vector<String>(Arrays.asList(lines));
		// vector.removeElementAt(idx);
		int endOfExcludeNode = idx;
		for (int i = idx + 1; i < lines.length; i++) {

			if (lines[i].codePoints().filter(ch -> ch == '*').count() <= count)
				break;
			else {
				nodeToExclude = nodeToExclude + lines[i] + "\n";
				endOfExcludeNode = endOfExcludeNode + 1;

			}
		}
		if (idx == endOfExcludeNode)
			vector.removeElementAt(idx);
		else
			vector.subList(idx, endOfExcludeNode + 1).clear();

		AbstractNode retFtn1 = createRandomSubTree((int) (count / 2), random);
		String treeToStr2 = printTreeToString(retFtn1, (int) (count / 2));
		// System.out.println(treeToStr2);
		String lines2[] = treeToStr2.split("\\r?\\n");

		int positionToStartAdding = idx;
		for (int i = 0; i < lines2.length; i++) {
			vector.add(positionToStartAdding, lines2[i]);
			positionToStartAdding = positionToStartAdding + 1;
		}
		// System.out.println(vector.toString());
		for (int vec = 0; vec < vector.size(); vec++) {
			String currentStr = vector.get(vec);

			currentStr = currentStr + "\n";
			vector.set(vec, currentStr);
		}
		// System.out.println("After " + vector.toString());
		// System.out.println(vector.toString() + "\n" +
		// Arrays.toString(lines)); print mutated tree
		return vector.toString();
	}

	static AbstractNode createAlternateRandomRegressionLeafNode(String nodeName) {

		AbstractNode retFtn = null;
		Random rn = new Random();
		if (Const.NUMBER_OF_INPUTS == 0)
			return null;
		else {

		}

		// rn.nextInt(max - min + 1) + min

		if (nodeName.contains("Const")) {
			if (Const.NUMBER_OF_INPUTS == 1)
				retFtn = new InputNode(Const.NUMBER_OF_INPUTS);
			else {
				retFtn = new InputNode(Const.NUMBER_OF_INPUTS);
				// Type cast to DerivedNode to access bar
				//((InputNode) retFtn).setValues(rn.nextInt(Const.NUMBER_OF_INPUTS - 1 + 1) + 1);
			}

		} else {
			int threshold = rn.nextInt((Const.doubles.length - 1) - 0 + 1) + 0;
			retFtn = new ConstNode(Const.doubles[threshold]);
		}
		return retFtn;
	}

	static AbstractNode createRandomRegressionInnerNode(boolean isRoot) {
		AbstractNode retFtn = null;
		Random rn = new Random();

		int randomInt = -1;

		if (Const.LINEAR_FUNCTIONALITY_ONLY)
			randomInt = rn.nextInt(5 - 2 + 1) + 2;
		else
			randomInt = rn.nextInt(10 - 2 + 1) + 2;

		switch (randomInt) {
		case 2: {
			retFtn = new Add();
			break;
		}
		case 3: {
			retFtn = new Subtract();
			break;
		}
		case 4: {
			retFtn = new Multiply();
			break;
		}
		case 5: {
			retFtn = new Divide();
			break;
		}
		case 6: {
			retFtn = new Exponential();
			break;
		}
		case 7: {
			retFtn = new Log();
			break;
		}
		case 8: {
			retFtn = new Sine();
			break;
		}
		case 9: {
			retFtn = new Cosine();
			break;
		}
		case 10: {
			retFtn = new Power();
			break;
		}
		case 11: {
			retFtn = new SquareRoot();
			break;
		}
		case 12: {
			retFtn = new Square();
			break;
		}
		}
		return retFtn;

	}

	static AbstractNode createRandomRegressionNode(boolean isRoot) {

		AbstractNode retFtn = null;
		Random rn = new Random();
		if (Const.NUMBER_OF_INPUTS == 0)
			return null;
		else {

		}

		// rn.nextInt(max - min + 1) + min
		int randomInt = -1;
		if (isRoot) {
			if (Const.LINEAR_FUNCTIONALITY_ONLY)
				randomInt = rn.nextInt(5 - 2 + 1) + 2;
			else
				randomInt = rn.nextInt(10 - 2 + 1) + 2;
		} else {
			if (Const.LINEAR_FUNCTIONALITY_ONLY)
				randomInt = rn.nextInt(5 - 0 + 1) + 0;
			else
				randomInt = rn.nextInt(10 - 0 + 1) + 0;
		}
		switch (randomInt) {
		case 0: {

			int threshold = rn.nextInt((Const.doubles.length - 1) - 0 + 1) + 0;
			retFtn = new ConstNode(Const.doubles[threshold]);
			break;
		}
		case 1: {
			if (Const.NUMBER_OF_INPUTS == 1)
				retFtn = new InputNode(Const.NUMBER_OF_INPUTS);
			else {
				retFtn = new InputNode(Const.NUMBER_OF_INPUTS);
				// Type cast to DerivedNode to access bar
				//((InputNode) retFtn).setValues(rn.nextInt(Const.NUMBER_OF_INPUTS - 1 + 1) + 1);
			}

			break;
		}
		case 2: {
			retFtn = new Add();
			break;
		}
		case 3: {
			retFtn = new Subtract();
			break;
		}
		case 4: {
			retFtn = new Multiply();
			break;
		}
		case 5: {
			retFtn = new Divide();
			break;
		}
		case 6: {
			retFtn = new Exponential();
			break;
		}
		case 7: {
			retFtn = new Log();
			break;
		}
		case 8: {
			retFtn = new Sine();
			break;
		}
		case 9: {
			retFtn = new Cosine();
			break;
		}
		case 10: {
			retFtn = new Power();
			break;
		}
		case 11: {
			retFtn = new SquareRoot();
			break;
		}
		case 12: {
			retFtn = new Square();
			break;
		}
		}
		return retFtn;
	}

	static AbstractNode createRandomRegressionLeafNode(AbstractNode rootNode) {

		AbstractNode retFtn = null;
		Random rn = new Random();
		if (Const.NUMBER_OF_INPUTS == 0)
			return null;
		else {

		}
		
		
	
		
		if ( (treeStructurePostcreateObj == treeStructurePostcreate.eEqualERCAndExternalInputLeaf || treeStructurePostcreateObj == treeStructurePostcreate.ePruneAndEqualERCAndExternalInputLeaf )&& 
				rootNode.getNumChildren() == 2 &&  rootNode.getChildren().size() == 1 
				&& rootNode.getChildren().get(0).getType() == Const.LEAF_NODE_TYPE)
		{
			if (rootNode.getChildren().get(0).getLabel().contains("InputVal:")){
				retFtn = new ConstNode(Const.doubles[rd.nextInt((Const.doubles.length - 1) - 0 + 1) + 0]);
			}
			else if (rootNode.getChildren().get(0).getLabel().contains("Const:")){
			  
				if (Const.NUMBER_OF_INPUTS == 1)
					retFtn = new InputNode(Const.NUMBER_OF_INPUTS);
				else {
					retFtn = new InputNode(Const.NUMBER_OF_INPUTS);
					// Type cast to DerivedNode to access bar
					//((InputNode) retFtn).setValues(rd.nextInt(Const.NUMBER_OF_INPUTS - 1 + 1) + 1);
				}
			}
			else
			{
				System.out.println("Should not get here because sibling is a leaf and this logic is for only binary node");
			}
			
		}
		else
		{

			// rn.nextInt(max - min + 1) + min
			int randomInt = rn.nextInt(1 - 0 + 1) + 0;
	
			switch (randomInt) {
				case 0: {
		
					int threshold = rn.nextInt((Const.doubles.length - 1) - 0 + 1) + 0;
					retFtn = new ConstNode(Const.doubles[threshold]);
					break;
				}
				case 1: {
					if (Const.NUMBER_OF_INPUTS == 1)
						retFtn = new InputNode(Const.NUMBER_OF_INPUTS);
					else {
						retFtn = new InputNode(Const.NUMBER_OF_INPUTS);
						// Type cast to DerivedNode to access bar
						//((InputNode) retFtn).setValues(rn.nextInt(Const.NUMBER_OF_INPUTS - 1 + 1) + 1);
					}
		
					break;
				}
				default: {
					System.out.println("Unknown leafNode");
					break;
				}
			}
		}
		return retFtn;
	}

	static AbstractNode createRandomFullTreeBranches(AbstractNode rootNode, int depth) {

		if (depth >= Const.MAX_DEPTH) {
			if (rootNode.getNumChildren() == 0)
				;
			else {
				for (int i = 0; i < rootNode.getNumChildren(); i++) {
					AbstractNode leafNode = createRandomRegressionLeafNode(rootNode);
					leafNode.setParent(rootNode);
					leafNode.setNodeIndex(i);
					rootNode.addChild(createRandomRegressionLeafNode(rootNode));
				}
			}
			return rootNode;
		}

		AbstractNode child = null;

		for (int i = 0; i < rootNode.getNumChildren(); i++) {
			child = TreeHelperClass.createRandomRegressionInnerNode(false);
			
			if ( (treeStructurePostcreateObj == treeStructurePostcreate.eEqualERCAndExternalInputLeaf  ||  treeStructurePostcreateObj == treeStructurePostcreate.ePruneAndEqualERCAndExternalInputLeaf )&& 
					child.getType() == Const.LEAF_NODE_TYPE &&  rootNode.getNumChildren() == 2 &&  rootNode.getChildren().size() == 1 
					&& rootNode.getChildren().get(0).getType() == Const.LEAF_NODE_TYPE)
			{
				if (rootNode.getChildren().get(0).getLabel().contains("InputVal:")){
					child = new ConstNode(Const.doubles[rd.nextInt((Const.doubles.length - 1) - 0 + 1) + 0]);
				}
				else if (rootNode.getChildren().get(0).getLabel().contains("Const:")){
				  
					if (Const.NUMBER_OF_INPUTS == 1)
						child = new InputNode(Const.NUMBER_OF_INPUTS);
					else {
						child = new InputNode(Const.NUMBER_OF_INPUTS);
						// Type cast to DerivedNode to access bar
						//((InputNode) child).setValues(rd.nextInt(Const.NUMBER_OF_INPUTS - 1 + 1) + 1);
					}
				}
				else
				{
					System.out.println("Should not get here because sibling is a leaf and this logic is for only binary node");
				}
				
			}
			
			
			if (child != null) {
				if (child.getNumChildren() > 0) {
					createRandomFullTreeBranches(child, depth + 1);

				}
				child.setNodeIndex(i);
				child.setParent(rootNode);
				rootNode.addChild(child);
			} else {
				System.out.println("null");
			}
		}
		return rootNode;
	}

	static AbstractNode createRandomTreeBranches(AbstractNode rootNode, int depth) {

		if (depth >= Const.MAX_DEPTH) {
			if (rootNode.getNumChildren() == 0)
				;
			else {
				for (int i = 0; i < rootNode.getNumChildren(); i++) {
					
					AbstractNode leafNode = createRandomRegressionLeafNode(rootNode);
					leafNode.setParent(rootNode);
					leafNode.setNodeIndex(i);
					rootNode.addChild(leafNode);
				}
			}
			return rootNode;
		}

		AbstractNode child = null;

		for (int i = 0; i < rootNode.getNumChildren(); i++) {
			child = TreeHelperClass.createRandomRegressionNode(false);
			
			if ((treeStructurePostcreateObj == treeStructurePostcreate.eEqualERCAndExternalInputLeaf || treeStructurePostcreateObj == treeStructurePostcreate.ePruneAndEqualERCAndExternalInputLeaf )&& 
					child.getType() == Const.LEAF_NODE_TYPE &&  rootNode.getNumChildren() == 2 &&  rootNode.getChildren().size() == 1 
					&& rootNode.getChildren().get(0).getType() == Const.LEAF_NODE_TYPE)
			{
				if (rootNode.getChildren().get(0).getLabel().contains("InputVal:")){
					child = new ConstNode(Const.doubles[rd.nextInt((Const.doubles.length - 1) - 0 + 1) + 0]);
				}
				else if (rootNode.getChildren().get(0).getLabel().contains("Const:")){
				  
					if (Const.NUMBER_OF_INPUTS == 1)
						child = new InputNode(Const.NUMBER_OF_INPUTS);
					else {
						child = new InputNode(Const.NUMBER_OF_INPUTS);
						// Type cast to DerivedNode to access bar
						//((InputNode) child).setValues(rd.nextInt(Const.NUMBER_OF_INPUTS - 1 + 1) + 1);
					}
				}
				else
				{
					System.out.println("Should not get here because sibling is a leaf and this logic is for only binary node");
				}
				
			}
			
			
			
			if (child != null) {
				if (child.getNumChildren() > 0) {
					createRandomTreeBranches(child, depth + 1);

				}
				child.setParent(rootNode);
				child.setNodeIndex(i);
				rootNode.addChild(child);
			} else {
				System.out.println("null");
			}
		}
		return rootNode;
	}

	public static String crossOverTree(AbstractNode treeToAlter, AbstractNode donatingTree) {
		// look at c++ code

		String offSpringFromCrossOver = null;

		String treeToAlterStr = printTreeToString(treeToAlter, 0);
		String donatingTreeStr = printTreeToString(donatingTree, 0);

		if ((treeToAlter.getNumChildren() > 0) && (donatingTree.getNumChildren() > 0)) {

			// randomly select side of treeToAlter and donatingTree
			String treeToAlterLines[] = treeToAlterStr.split("\\r?\\n");
			// System.out.println(Arrays.toString(treeToAlterLines));
			int treeToAlterIdx = new Random().nextInt(treeToAlterLines.length - 1 - 1 + 1) + 1;
			String treeToAlterRandom = (treeToAlterLines[treeToAlterIdx]);

			long treeToAlterCount = treeToAlterRandom.codePoints().filter(ch -> ch == '*').count();
			// recreate node from String
			String nodeToExclude = treeToAlterLines[treeToAlterIdx] + "\n";
			Vector<String> vector = new Vector<String>(Arrays.asList(treeToAlterLines));
			// vector.removeElementAt(idx);
			int endOfExcludeNode = treeToAlterIdx;
			for (int i = treeToAlterIdx + 1; i < treeToAlterLines.length; i++) {

				if (treeToAlterLines[i].codePoints().filter(ch -> ch == '*').count() <= treeToAlterCount)
					break;
				else {
					nodeToExclude = nodeToExclude + treeToAlterLines[i] + "\n";
					endOfExcludeNode = endOfExcludeNode + 1;

				}
			}
			if (treeToAlterIdx == endOfExcludeNode)
				vector.removeElementAt(treeToAlterIdx);
			else
				vector.subList(treeToAlterIdx, endOfExcludeNode + 1).clear();

			// System.out.println("After removing subtree" +
			// vector.toString());
			for (int vec = 0; vec < vector.size(); vec++) {
				String currentStr = vector.get(vec);

				currentStr = currentStr + "\n";
				vector.set(vec, currentStr);
			}
			//////// donating part

			String donatingTreeLines[] = donatingTreeStr.split("\\r?\\n");
			// System.out.println(Arrays.toString(donatingTreeLines));
			int donatingTreeIdx = new Random().nextInt(donatingTreeLines.length - 1 - 1 + 1) + 1;
			String donatingTreeRandom = (donatingTreeLines[donatingTreeIdx]);

			long donatingTreeCount = donatingTreeRandom.codePoints().filter(ch -> ch == '*').count();
			// recreate node from String
			String nodeToInclude = donatingTreeLines[donatingTreeIdx] + "\n";
			Vector<String> donatingTreeSubTreeVector = new Vector<String>();
			// vector.removeElementAt(idx);
			donatingTreeSubTreeVector.add(nodeToInclude);
			for (int i = donatingTreeIdx + 1; i < donatingTreeLines.length; i++) {

				if (donatingTreeLines[i].codePoints().filter(ch -> ch == '*').count() <= donatingTreeCount)
					break;
				else {
					donatingTreeSubTreeVector.add(donatingTreeLines[i] + "\n");
				}
			}

			if (treeToAlterCount == donatingTreeCount) { // This indicates
															// the
															// depth of the
															// tree
				vector.addAll(treeToAlterIdx, donatingTreeSubTreeVector);
			} else {
				int insertIndex = treeToAlterIdx; // This is the position in
													// vector collection

				int AlteredNodeDepth = Math.toIntExact(treeToAlterCount);// donatingTreeSubTreeVector
				int rootDonatingNodeDepth = Math.toIntExact(donatingTreeCount);
				for (int vec = 0; vec < donatingTreeSubTreeVector.size(); vec++) {
					String currentStr = donatingTreeSubTreeVector.get(vec);
					int currentSonatingNodeDepth = Math
							.toIntExact(currentStr.codePoints().filter(ch -> ch == '*').count());
					int starToAdd = AlteredNodeDepth + currentSonatingNodeDepth - rootDonatingNodeDepth;
					char c = '*';
					char[] repeat = new char[starToAdd];
					Arrays.fill(repeat, c);

					currentStr = currentStr.replace("*", "");
					currentStr = String.valueOf(repeat) + currentStr;
					donatingTreeSubTreeVector.set(vec, currentStr);
				}
				vector.addAll(treeToAlterIdx, donatingTreeSubTreeVector);
			}
			offSpringFromCrossOver = vector.toString();

		}

		return offSpringFromCrossOver;
	}

	public static int getTreeSize(AbstractNode inTree) {

		if (inTree == null)
			return (0);
		if (inTree.getNumChildren() == 0) {
			return (1);
		} else if (inTree.getNumChildren() == 1) {
			return (getTreeSize(inTree.getChildren().get(0)) + 1);
		} else if (inTree.getNumChildren() == 2) {
			return (getTreeSize(inTree.getChildren().get(0)) + 1 + getTreeSize(inTree.getChildren().get(1)));
		} else {
			System.out.println("Invalid  number of children");
			System.exit(0);
		}

		return Integer.MAX_VALUE;
	}

	public static String printTreeToString(AbstractNode inTree, int depth) {
		String treeString = "";
		for (int j = 0; j < depth; j++) {
			// printf("..");
			treeString = treeString + "**";
		}
		// printf("%s\n", inTree->label.c_str());
		// cout << inTree->label.c_str() <<endl;
		treeString = treeString + inTree.getLabel() + "\n";
		// System.out.print(treeString);
		for (int i = 0; i < inTree.getNumChildren(); i++) {
			treeString = treeString + printTreeToString(inTree.getChildren().get(i), depth + 1);
		}

		return treeString;
	}

	public  double evaluateRegressionTree(AbstractNode tree, Event[] directionChangesLength) {
		double treeEvaluation = 0.0;
		double err = 0.0;
		double sumSquaredErr = 0.0;
		
		double variableSize = 0.0;
		
		double os  = 0.0;

		String treeAsString = printTreeToString(tree, 0);
		boolean hasVariable0 = treeAsString.indexOf(Const.VARIABLE_1) >= 0; // true

		boolean hasConstant = treeAsString.indexOf("Const") >= 0; // must be true
		
		if (!hasVariable0)
			return Double.MAX_VALUE;
		
		
		
		if (!hasConstant)
			return Double.MAX_VALUE;
		
		if(!treeAsString.contains(Const.VARIABLE_1))
			return Double.MAX_VALUE;
		
		String treeToString = tree.printAsInFixFunction();
		
		
		
		treeToString = treeToString.replace("X0", Integer.toString(2));
		treeToString = treeToString.replace("X1", Integer.toString(2));
		Double javascriptValue = Double.MAX_VALUE;
		double eval = 0.0;
	/*	try {
			javascriptValue = (Double) engine.eval(treeToString);
			
			if  ( javascriptValue == Double.MAX_VALUE || javascriptValue == Double.NEGATIVE_INFINITY ||
					javascriptValue == Double.POSITIVE_INFINITY || javascriptValue ==  Double.NaN ||
					Double.compare(javascriptValue, 0.0)  < 0  || Double.isInfinite(javascriptValue)  || Double.isNaN(javascriptValue)){
				return Double.MAX_VALUE;
			}
			else{
				eval = javascriptValue.doubleValue();
			}
			
		} catch (ScriptException e) {
			return Double.MAX_VALUE;
		} catch (ClassCastException e) {
			return Double.MAX_VALUE;
		}
		*/
	
		
		javascriptValue = new Double(tree.eval(1, 0.005));
		if  ( javascriptValue == Double.MAX_VALUE || javascriptValue == Double.NEGATIVE_INFINITY ||
				javascriptValue == Double.POSITIVE_INFINITY || javascriptValue ==  Double.NaN ||
				Double.compare(javascriptValue, 0.0)  < 0  || Double.isInfinite(javascriptValue)  || Double.isNaN(javascriptValue)){
			return Double.MAX_VALUE;
		}
		
		
		javascriptValue = new Double(tree.eval(30,0.067));
		if  ( javascriptValue == Double.MAX_VALUE || javascriptValue == Double.NEGATIVE_INFINITY ||
				javascriptValue == Double.POSITIVE_INFINITY || javascriptValue ==  Double.NaN ||
				Double.compare(javascriptValue, 0.0)  < 0  || Double.isInfinite(javascriptValue)  || Double.isNaN(javascriptValue)){
			return Double.MAX_VALUE;
		}
		
		
		
		for (Event e : directionChangesLength) {
		//	if (Const.VARIABLE_EVALUATED == 0)
				variableSize = e.length();
		/*	else
			{
				if (e.type == Type.Upturn)
				{
					variableSize = Double.parseDouble(FReader.dataRecordInFileArray.get(e.end).askPrice);
				}
				else
					variableSize = Double.parseDouble(FReader.dataRecordInFileArray.get(e.end).bidPrice);
			}*/
				 
			os = 0.0; 
			if (e.overshoot != null)
			{
				//if (Const.VARIABLE_EVALUATED == 0)
					os = e.overshoot.length();
			/*	else
				{
					if (e.type == Type.Upturn)
					{
						os = Double.parseDouble(FReader.dataRecordInFileArray.get(e.overshoot.end).askPrice);
					}
					else
						os = Double.parseDouble(FReader.dataRecordInFileArray.get(e.overshoot.end).bidPrice);
				}	*/
			}
			//Double doubleObject = new Double(tree.eval(dc,magnitude)); // in order t
			Double doubleObject = new Double(tree.eval(variableSize,(Math.abs(e.startPriceDbl - e.endPriceDbl)+0.0000000001))); // in order to cast
																// to int
			treeEvaluation = doubleObject.doubleValue();

			err = os - treeEvaluation;
			if (Double.isInfinite(treeEvaluation)){
			//	System.out.println(TreeHelperClass.printTreeToString(tree, 0));
			//	System.out.println(tree.printAsInFixFunction());
			//	System.out.println(tree.toString());
			//	System.out.println("In Nan");
			
			}
			if (Double.isNaN(treeEvaluation)){
			//	System.out.println(TreeHelperClass.printTreeToString(tree, 0));
			//	System.out.println(tree.toString());
			//	System.out.println("Is infinity");
			}

			double squareErr = err * err;
			sumSquaredErr += squareErr;
			// sumAbsErr += Math.abs(err);
		}
		// Double osSum = new Double(sumOfOvershoot(directionChangesLength));
		double fitness = Math.sqrt(sumSquaredErr / directionChangesLength.length);
		Double evalTotal = new Double(fitness);
		if (fitness == Double.MAX_VALUE || fitness == Double.NEGATIVE_INFINITY || fitness == Double.POSITIVE_INFINITY
				|| fitness == Double.NaN || Double.isNaN(fitness) || Double.isInfinite(fitness)
				|| Double.compare(evalTotal, Double.valueOf(0.0)) < 0) {
			return Double.MAX_VALUE;

			// err = e.length() * GA.NEGATIVE_EXPRESSION_REPLACEMENT;

		}
		if ( evalTotal != Double.NaN &&  !Double.isNaN(evalTotal) && !Double.isInfinite(evalTotal) && Double.compare(evalTotal, Double.valueOf(0.0)) > 0) {
			return fitness;
		} else {
			return Double.MAX_VALUE;
		}

	}

	private static double sumOfOvershoot(Event[] directionChangesLength) {
		double overshootLengthSum = 0.0;
		for (Event e : directionChangesLength) {
			double os = 0.0;
			if (e.overshoot != null)
				os = e.overshoot.length();
			overshootLengthSum = overshootLengthSum + os;
		}
		return overshootLengthSum;

	}

	
	public static AbstractNode tournament(Vector<AbstractNode> treeCollection, int numberOfRandomTrees,
			int fromLookupPosition) {

		// System.out.println("vector size" + treeCollection.size() );

		// Randomly select tree location from 0 to vector size
		if (fromLookupPosition < 0 || fromLookupPosition == Integer.MAX_VALUE) {
			fromLookupPosition = 0;
		}
		int[] treeLocation = ThreadLocalRandom.current().ints(fromLookupPosition, treeCollection.size() - 1).distinct()
				.limit(numberOfRandomTrees).toArray();

		Vector<AbstractNode> tournamentCollection = new Vector<AbstractNode>(treeLocation.length);
		for (int i = 0; i < treeLocation.length; i++) {
			tournamentCollection.add(treeCollection.get(treeLocation[i]));
		}

		Comparator<AbstractNode> comparator = Collections.reverseOrder();
		Collections.sort(tournamentCollection, comparator);

		/*
		 * for (int i =0; i< tournamentCollection.size(); i++){
		 * System.out.println(tournamentCollection.get(i).perfScore); }
		 */

		AbstractNode bestTree = tournamentCollection.get(tournamentCollection.size() - 1).clone();

		return bestTree;
	}

	public  AbstractNode getbestTreeForThreshold(Event[] directionChangesLength, int NumberOfTrees, int run,
			String thresholdStr) {

		// GenerateInitialTrees
		Vector<AbstractNode> vector = new Vector<AbstractNode>(NumberOfTrees);
		// System.out.println("Capacity is" + vector.capacity());
		
		for (int treeCount = 0; treeCount < NumberOfTrees; treeCount++) {

			TreeCreation treeCreation = new TreeCreation(treeCount);

			//AbstractNode randomTree = treeCreation.tree;
			AbstractNode randomTree = treeCreation.tree;
			
			double score = evaluateRegressionTree(randomTree, directionChangesLength);
			randomTree.perfScore = score;
			
			vector.add(randomTree);
		}

		Comparator comparator = Collections.reverseOrder();
		Collections.sort(vector, comparator);

		/*
		 * for (int treeCount = 0; treeCount < NumberOfTrees; treeCount++) {
		 * System.out.println(vector.get(treeCount) + " Score:" +
		 * vector.get(treeCount).perfScore);
		 * 
		 * }
		 */

		double percentageToEvolved = Const.EVOLUTION_RATIO;
		double crossoverRatio = Const.CROSSOVER_PROB;
		int numberOfGenerations = Const.MAX_GENERATIONS;
		AbstractNode bestTree = evolve(directionChangesLength, vector, percentageToEvolved, crossoverRatio,
				numberOfGenerations, run, thresholdStr); // Already cloned in
															// evolve method
		return bestTree;
	}

	AbstractNode evolve(Event[] directionChangesLength, Vector<AbstractNode> gpTrees, double evolutionRate,
			double crossoverRatio, int numberOfGenerations, int run, String thresholdStr) {

		Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd  HH:mm:ss");
        //System.out.println( "Evolution of GP Started at :" +sdf.format(cal.getTime()) );
		//System.out.println("Evolution of GA Startedcapacity of random tree is" +
		// gpTrees.capacity());
		int size = (gpTrees.size());
		int evolvedElementsSize = (int) (size * evolutionRate);
		int preservedElementsSize = size - evolvedElementsSize;
		if (preservedElementsSize < 1)
			preservedElementsSize = 1;
		Vector<AbstractNode> evolvedGpTrees = new Vector<AbstractNode>(size);
		AbstractNode bestTree = null;
		for (int genNum = 0; genNum < numberOfGenerations; genNum++) {
		//	System.out.println("Generation:" + gen );
			if (gpTrees.get(gpTrees.size() - 1).perfScore == 0) {
				System.out.println("Perfect match found.");
				break;
			}
			
			// Adesola remove comment when not
			// tunning//System.out.println("Threshold: " + thresholdStr + " Run:
			// " + run + " Generation: " + generationCount);
			//System.out.println("The Best performing trees' scores generation "+  gen + " are: ");
			for (int counter = size - 1; counter >= evolvedElementsSize; counter--) {
				AbstractNode copy = gpTrees.get(counter).clone();
				copy.perfScore = gpTrees.get(counter).perfScore;
			//	System.out.println(printTreeToString(copy, 0));
			//	System.out.println(copy.perfScore + " depth " + getDepth(copy, 1) + " size " + getTreeSize(copy));
				evolvedGpTrees.add(gpTrees.get(counter));
				//System.out.println(printTreeToString(gpTrees.get(counter),0));
			}

			cal = Calendar.getInstance();
	    //    System.out.println( "Evolving population started at :" +sdf.format(cal.getTime())  );
			AbstractNode selectedTree = tournament(gpTrees, Const.TOURNAMENT_SIZE, 0); // Already
			for (int i = 0; i < evolvedElementsSize; i++) {
				
				
		      //  System.out.println( "Evolving population: " +i  );
				// cloned
				// in
				// tournament
				double randomValue = rd.nextDouble();
				if (randomValue < crossoverRatio) {
					
					//cal = Calendar.getInstance();
			        //System.out.println( "Crossover  started at :" +sdf.format(cal.getTime())  );
					
					//double d = (double) Const.MAX_DEPTH; //
					//int max_node_count = ((int) Math.pow(2.0, d) - 1);
					
					AbstractNode newTree = null;
					
					// crossover
					AbstractNode selectedTree2 = tournament(gpTrees, Const.TOURNAMENT_SIZE, preservedElementsSize); // Already
					// cloned
					// in
					// tournament
					String crossOverTreeString = TreeHelperClass.crossOverTree(selectedTree, selectedTree2);
					crossOverTreeString = crossOverTreeString.replace(",", "");
					// System.out.println("Crossover tree" +
					// crossOverTreeString);
					String crossOverTreeStringArray[] = crossOverTreeString.split("\\r?\\n");
					Vector<String> crossOverTreeStringVector = new Vector<String>(
							Arrays.asList(crossOverTreeStringArray));
					TreeOperation treeOperation = new TreeOperation(crossOverTreeStringVector);
					newTree = treeOperation.getTree();
					//int treeSize =  getTreeSize(newTree);
					
					/*if ( treeSize <= max_node_count) {
						double score = evaluateRegressionTree(newTree, directionChangesLength);
						newTree.perfScore = score;
					} else {
						//System.out.println("Penalizing tree because size is " + treeSize + " while max tree size allowed is " + max_node_count);
						newTree.perfScore = Double.MAX_VALUE;
					}*/
					
					
					/*if ( randomValue < 0.03) { // Crossover and mutate a few
						String mutateTreeString = TreeHelperClass.mutateTree(newTree);
						mutateTreeString = mutateTreeString.replace(",", "");
						// System.out.println("mutated tree" +
						// mutateTreeString);
						String mutateTreeStringArray[] = mutateTreeString.split("\\r?\\n");
						Vector<String> mutateTreeStringVector = new Vector<String>(
								Arrays.asList(mutateTreeStringArray));
						TreeOperation treeOperation2 = new TreeOperation(mutateTreeStringVector);
						AbstractNode newTree2 = treeOperation2.getTree();
						
						
						evolvedGpTrees.add(newTree2);

					} else {*/
						
						evolvedGpTrees.add(newTree);
					//}
						
				} else {
					String mutateTreeString = TreeHelperClass.mutateTree(selectedTree);
					mutateTreeString = mutateTreeString.replace(",", "");
					// System.out.println("mutated tree" + mutateTreeString);
					String mutateTreeStringArray[] = mutateTreeString.split("\\r?\\n");
					Vector<String> mutateTreeStringVector = new Vector<String>(Arrays.asList(mutateTreeStringArray));
					TreeOperation treeOperation2 = new TreeOperation(mutateTreeStringVector);
					AbstractNode newTree2 = treeOperation2.getTree();
									
					evolvedGpTrees.add(newTree2);
					
				}
			} // end of a generation
			cal = Calendar.getInstance();
	       // System.out.println( "Crossover population ended at :" +sdf.format(cal.getTime())  );
			
			gpTrees.clear();
			// System.out.println("After clearing random tree capacity is" +
			// gpTrees.capacity());

			
			//removeDuplicates(evolvedGpTrees);
			
		/*	int numberOftries = 0;
			while ( evolvedGpTrees.size() <= size ){
				TreeCreation treeCreation = new TreeCreation(evolvedGpTrees.size());

				//AbstractNode randomTree = treeCreation.tree;
				AbstractNode randomTree = treeCreation.tree;
				
				double score = evaluateRegressionTree(randomTree, directionChangesLength);
				randomTree.perfScore = score;
				
				if (randomTree.perfScore <0 )
				{
					randomTree.perfScore = Double.MAX_VALUE;
					
				}
				
				if(randomTree.perfScore == Double.MAX_VALUE) {
					
					String mutateTreeString = TreeHelperClass.mutateTree(randomTree);
					mutateTreeString = mutateTreeString.replace(",", "");
					// System.out.println("mutated tree" +
					// mutateTreeString);
					String mutateTreeStringArray[] = mutateTreeString.split("\\r?\\n");
					Vector<String> mutateTreeStringVector = new Vector<String>(
							Arrays.asList(mutateTreeStringArray));
					TreeOperation treeOperation2 = new TreeOperation(mutateTreeStringVector);
					AbstractNode newTree2 = treeOperation2.getTree();
					double score2 = evaluateRegressionTree(newTree2, directionChangesLength);
					newTree2.perfScore = score2;
					
					if (newTree2.perfScore <0 )
					{
						newTree2.perfScore = Double.MAX_VALUE;
						
					}
					
					evolvedGpTrees.add(newTree2);
					numberOftries=0;
				}
				else
				{
					evolvedGpTrees.add(randomTree);
				}
				
				
			}*/
			cal = Calendar.getInstance();
	       // System.out.println( "Prunning population started at :" +sdf.format(cal.getTime())  );

			for (int k=0 ; k< evolvedGpTrees.size() ; k++ )
			{
				//System.out.println("A " + TreeHelperClass.printTreeToString(evolvedGpTrees.get(k),0));
				
				if (treeStructurePostcreateObj == treeStructurePostcreate.ePruneAndEqualERCAndExternalInputLeaf || treeStructurePostcreateObj == treeStructurePostcreate.ePrune)
				{	
					AbstractNode treeCopy =  evolvedGpTrees.get(k).clone();	
					 evolvedGpTrees.get(k).pruneNode();
					 if (evolvedGpTrees.get(k).getNumChildren() < 1)
					 evolvedGpTrees.set(k, treeCopy); // Don't prune if it leads to only on Node. Give it  chance to evolve
					
				}
				//to print the evolved tree
				//System.out.println("C " + TreeHelperClass.printTreeToString(evolvedGpTrees.get(k),0));

				gpTrees.add(evolvedGpTrees.get(k));
			}
			cal = Calendar.getInstance();
	       // System.out.println( "Prunning population ended at :" +sdf.format(cal.getTime())  );

	        cal = Calendar.getInstance();
	      //  System.out.println( "Evaluating population started at :" +sdf.format(cal.getTime())  );

			
			for (int k=0 ; k< gpTrees.size() ; k++ )
			{
					double score2 = evaluateRegressionTree(gpTrees.get(k), directionChangesLength);
					gpTrees.get(k).perfScore = score2;	
			}
			
			cal = Calendar.getInstance();
	     //   System.out.println( "Evaluating population ended at :" +sdf.format(cal.getTime())  );

			
			
			System.gc();
			Comparator<AbstractNode> comparator = Collections.reverseOrder();
			//Collections.sort(evolvedGpTrees, comparator);
			evolvedGpTrees.clear();
		
			
			
		
			cal = Calendar.getInstance();
	       // System.out.println( "sorting population started at :" +sdf.format(cal.getTime())  );

			
			//Comparator<AbstractNode> comparator = Collections.reverseOrder();
			Collections.sort(gpTrees, comparator);
			cal = Calendar.getInstance();
	      //  System.out.println( "sorting population ended at :" +sdf.format(cal.getTime())  );

			
			//Collections.sort(gpTrees2, comparator);
			
			TreeHelperClass.printTreeToString(gpTrees.get(0),0);
			
			evolvedGpTrees.clear();
			
		}
		/*for (int k =  0 ; k< gpTrees.size(); k++){
			String treeToString = gpTrees.get(k).printAsInFixFunction();
	
			treeToString = treeToString.replace("X0", Integer.toString(1));
			Double javascriptValue = Double.MAX_VALUE;
			double eval = 0.0;
			try {
				javascriptValue = (Double) engine.eval(treeToString);
				eval = javascriptValue.doubleValue();
				if (Double.compare(eval, 0) < 0)
					gpTrees.get(k).perfScore = Double.MAX_VALUE;
			} catch (ScriptException e) {
				gpTrees.get(k).perfScore = Double.MAX_VALUE;
			} catch (ClassCastException e) {
				gpTrees.get(k).perfScore = Double.MAX_VALUE;
			}
		}*/
		
		Comparator<AbstractNode> comparator = Collections.reverseOrder();
		Collections.sort(gpTrees, comparator);
		
		//Collections.sort(gpTrees2, comparator);

		bestTree = gpTrees.get(gpTrees.size() - 1).clone();
		
		cal = Calendar.getInstance();
       // System.out.println( "GP evolution ended at: "+sdf.format(cal.getTime()) + " completed " );
        
        
		


		

		return bestTree;
	}
	
	 public static void removeDuplicates(Vector<AbstractNode>  v)
	 {

	        for(int i=0;i<v.size();i++)

	        {

	            for(int j=0;j<v.size();j++)

	            {

	                    if(i!=j)

	                    {

                        	try 
	                        {
		                        if(v.elementAt(i).equals(v.elementAt(j)))
		                        {
		                        	//   System.out.println(v.elementAt(i).printAsInFixFunction());
		                        	//  System.out.println(v.elementAt(j).printAsInFixFunction());
		                        	
			                        	v.removeElementAt(j);
			                       
	
		                        }
	                        }
	                        catch (ArrayIndexOutOfBoundsException  e) {
	                        	;//System.out.println("ArrayIndexOutOfBoundsException handled" );
	                        }

	                    }

	            }

	        }

	    }

	 

	public   void getBestTreesForThreshold(Event[] directionChangesLength, int NumberOfTrees, int numberOfRuns,
			int numberOfGenerations, String thresholdStr) {
		Directional_changes_length = new Event[directionChangesLength.length];
		System.arraycopy(directionChangesLength, 0, Directional_changes_length, 0, directionChangesLength.length);
		Number_Of_Trees = NumberOfTrees;
		// threadPool= Executors.newCachedThreadPool();
		int processors = 10;
		processors = Runtime.getRuntime().availableProcessors();
		threadPool = Executors.newFixedThreadPool(1);
		bestTreesInRuns = new Vector<AbstractNode>(numberOfRuns);
		number_of_generations = numberOfGenerations;
		for (int i = 0; i < numberOfRuns; i++) {
			runSomeTaskInThreadPool(i + 1, thresholdStr);
		}
		// the shutdown method causes the executor to:
		// 1. stop accepting new tasks, and
		// 2. allow previously queued jobs to complete, and
		// 3. shut down all pooled threads once all jobs are complete
		threadPool.shutdown();
		// block until the threadPool has finished shutting down,
		// which indicates that all tasks have finished executing
		try {
			threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			threadPool.shutdown();
			e.printStackTrace();
		}
		threadPool.shutdown();
		if (bestTreesInRuns.size() == 0) {
			System.out.println("Vector is empty1");
		} else {
			/*
			 * for (int gptreeCount = 0; gptreeCount< numberOfRuns ;
			 * gptreeCount++) {
			 * System.out.println(printTreeToString(bestTreesInRuns.get(
			 * gptreeCount),0)); }
			 */
		}

		//System.out.println("getBestTreesForThreshold- completed");
	}

	private  void runSomeTaskInThreadPool(int run, String thresholdStr) {

			bestTreesInRuns.add(getbestTreeForThresholdConcurrent(run, thresholdStr));
	}

	//Starts here
	public  AbstractNode getbestTreeForThresholdConcurrent(int run, String thresholdStr) {

		// GenerateInitialTrees
		Vector<AbstractNode> vector = new Vector<AbstractNode>(Number_Of_Trees);
		// System.out.println("Capacity is" + vector.capacity());
		for (int treeCount = 0; treeCount < Number_Of_Trees; treeCount++) {

			TreeCreation treeCreation = new TreeCreation(treeCount);

			AbstractNode randomTree = treeCreation.tree;
			double score = evaluateRegressionTree(randomTree, Directional_changes_length);
			randomTree.perfScore = score;
			vector.add(randomTree);
		}

		Comparator comparator = Collections.reverseOrder();
		Collections.sort(vector, comparator);

		/*
		 * for (int treeCount = 0; treeCount < NumberOfTrees; treeCount++) {
		 * System.out.println(vector.get(treeCount) + " Score:" +
		 * vector.get(treeCount).perfScore);
		 * 
		 * }
		 */
		double percentageToEvolved = Const.EVOLUTION_RATIO;
		double crossoverRatio = Const.CROSSOVER_PROB;

		AbstractNode bestTree = evolve(Directional_changes_length, vector, percentageToEvolved, crossoverRatio,
				number_of_generations, run, thresholdStr); // Already cloned in
															// evolve method
		return bestTree;
	}

	 AbstractNode getGPTreeForCurve(String gpFileName) {
		TreeOperation opr = null;
		AbstractNode mytree = null;
		try {
			opr = new TreeOperation(gpFileName, 0.0);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(0);
		}
		if (opr != null) {
			opr.populateGpTreeVector();

			mytree = opr.getTree();
			String testStr = TreeHelperClass.printTreeToString(mytree, 0);

			//System.out.println(testStr);

		}
		return mytree;
	}

	 String getGPTreeForCurveInfixNotation(String gpFileName) {
		TreeOperation opr = null;
		String gpTreeInFixNotation = null;
		try {
			opr = new TreeOperation(gpFileName, 0.0);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(0);
		}
		if (opr != null) {
			opr.populateGpTreeVector();

			AbstractNode mytree = opr.getTree();
			String testStr = TreeHelperClass.printTreeToString(mytree, 0);

			//System.out.println(testStr);

			gpTreeInFixNotation = mytree.printAsInFixFunction();

		}
		return gpTreeInFixNotation;
	}

	public  AbstractNode cloneAndReplaceInnerNode(AbstractNode mytree) {
		String treeToStr = printTreeToString(mytree, 0);
		String[] twoOperandfunctionTypes;
		twoOperandfunctionTypes = new String[] { "Add", "Divide", "Multiply", "Subtract", "Power" };

		String[] oneOperandfunctionTypes;
		oneOperandfunctionTypes = new String[] { "Cosine", "Exponential", "Log", "Sine", "SquareRoot", "Square" };
		//oneOperandfunctionTypes = new String[] { "Cosine", "Exponential", "Log", "Sine" };
		String lines[] = treeToStr.split("\\r?\\n");

		long count = 0;

		for (int i = 0; i < lines.length; i++) {
			String node = lines[i];
			count = node.codePoints().filter(ch -> ch == '*').count();
			char c = '*';
			int starToAdd = (int) count;
			char[] repeat = new char[starToAdd];
			Arrays.fill(repeat, c);

			node = node.replace("*", "");
			boolean functionFound = false;
			Random rn = new Random();
			functionFound = Arrays.asList(twoOperandfunctionTypes).contains(node);
			if (functionFound == true) {
				int biggerWher = Arrays.asList(twoOperandfunctionTypes).indexOf(node);
				int functionPositionInArray = biggerWher;
				while (functionPositionInArray == biggerWher) {
					functionPositionInArray = new Random().nextInt(twoOperandfunctionTypes.length - 1 - 1 + 1) + 1;
				}
				String currentStr = String.valueOf(repeat) + twoOperandfunctionTypes[functionPositionInArray];
				lines[i] = currentStr;
			} else if ((functionFound = Arrays.asList(oneOperandfunctionTypes).contains(node)) == true) {
				int biggerWher = Arrays.asList(oneOperandfunctionTypes).indexOf(node);
				int functionPositionInArray = biggerWher;
				while (functionPositionInArray == biggerWher) {
					functionPositionInArray = new Random().nextInt(oneOperandfunctionTypes.length - 1 - 1 + 1) + 1;
				}
				String currentStr = String.valueOf(repeat) + oneOperandfunctionTypes[functionPositionInArray];
				lines[i] = currentStr;
			} else {
				continue;
			}
		}

		Vector<String> vector = new Vector<String>(Arrays.asList(lines));

		TreeOperation treeOperation = new TreeOperation(vector);
		AbstractNode newTree = treeOperation.getTree();

		return newTree;
	}

	public  void readFitnessFile(int folderDepth) {
		String folder = Logger.publicFolder;

		for (int i = 0; i < folderDepth; i++) {
			File fitnesFile = new File(folder);
			if (fitnesFile.isDirectory()) {
				folder = fitnesFile.getParentFile().getAbsolutePath();

			}
		}
		File fitnesFile = new File(folder);
		if (fitnesFile.isDirectory()) {
			// fitnesFile.getParent();
			getFitnessInRun(fitnesFile.getAbsolutePath());
			System.out.println("Done");
			// Map<String, List<FitnessClass>> fitnessList
			for (Entry<String, List<FitnessClass>> entry : fitnessList.entrySet()) {
				System.out.println("Key : " + entry.getKey() + " Value : " + entry.getValue());
				List<FitnessClass> bobs = entry.getValue();
				double biggestTraining = 0.0;
				double correspondingTest = 0.0;
				for (int i = 0; i < bobs.size(); i++) {
					FitnessClass bob1 = bobs.get(i);
					int retval = Double.compare(bob1.getTrainingFitness(), biggestTraining);
					if (retval > 0) {
						biggestTraining = bob1.getTrainingFitness();
						correspondingTest = bob1.getTestFitness();

					}

					// System.out.println("Training : " +
					// bob1.getTrainingFitness() );
					// System.out.println("Test : " + bob1.getTestFitness() );
				}
				FWriter writer = new FWriter(Logger.publicFolder + "bestTrainingFitness.txt");
				writer.openToAppend(new File(Logger.publicFolder + "bestTrainingFitness.txt"));
				writer.write(" DatasetName \tbest training Fitness \t training");
				writer.write(biggestTraining + "\t" + "\t" + correspondingTest);
				writer.closeFile();
				System.out.println("best training : " + biggestTraining);
				System.out.println("best Test : " + correspondingTest);
			}
		}
	}

	public  void getFitnessInRun(String sensorName) {
		File input = new File(sensorName);
		// Get input data
		if (input.isDirectory()) {
			System.out.println("input file: " + input.toString());
			String[] names = input.list();

			for (String name : names) {
				if (new File(sensorName + "\\" + name).isDirectory()) {
					getFitnessInRun(sensorName + "\\" + name);
				} else if (new File(sensorName + "\\" + name).isFile()) {
					File fitnesFile = new File(sensorName + "\\" + name);
					// Get input data
					if (!name.equalsIgnoreCase("fitness.txt"))
						continue;
					if (fitnesFile.exists()) {
						System.out.println("Input file: " + fitnesFile.toString());
						BufferedReader reader;

						try {
							reader = new BufferedReader(new FileReader(fitnesFile));
							String line;
							int count = 0;
							List<FitnessClass> fitness = new ArrayList<FitnessClass>();
							while ((line = reader.readLine()) != null) {
								if (count < 0) {
									count = count + 1;
									continue;
								}
								String[] splited = line.split("\\r?\\t");
								double trainingFitness = 0.0;
								double testFitness = 0.0;
								try {
									trainingFitness = Double.parseDouble(splited[0]);
									testFitness = Double.parseDouble(splited[1]);
									System.out.println(trainingFitness + " " + trainingFitness);
									fitness.add(new FitnessClass(trainingFitness, testFitness));
								} catch (NumberFormatException e) {
									System.out.println("invalid double");
									continue;
								}

							}
							if (fitness.size() > 0)
								fitnessList.put(fitnesFile.getAbsolutePath(), fitness);
							reader.close();
						} catch (FileNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException io) {
							io.printStackTrace();
						}
					}
				}

			}

		}
		// return fitnessList;
	}
}
