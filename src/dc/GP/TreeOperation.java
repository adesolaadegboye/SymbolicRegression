package dc.GP;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;
import java.util.Vector;

public class TreeOperation {

	static int  NUM_OF_INPUTS = 1;
	File input;
	double dcLength =0.0;
	 Vector<String> v = new Vector<String>();
	 int vectoriteration =0;
	 AbstractNode retFtn = null;
	
	public enum string_code {
		eConstNode,
		eInputNode,
		eAdd,
		eSubtract,
		eMultiply,
		eDivide,
		eExponential,
		eLog,
		eSine,
		eCosine,
		ePower,
		eSquareRoot,
		eSquare,
	};
	
	static string_code hashit(String inString) {
		if (inString.contains("Const"))   return string_code.eConstNode;
		if (inString.contains("InputVal") ) return string_code.eInputNode;
		if (inString.contains("Add") ) return string_code.eAdd;
		if (inString.contains("Subtract") ) return string_code.eSubtract;
		if (inString.contains("Multiply") ) return string_code.eMultiply;
		if (inString.contains("Divide") ) return string_code.eDivide;
		if (inString.contains("Exponential") ) return string_code.eExponential;
		if (inString.contains("Log") ) return string_code.eLog;
		if (inString.contains("Sine") ) return string_code.eSine;
		if (inString.contains("Cosine") ) return string_code.eCosine;
		if (inString.contains("Power") ) return string_code.ePower;
		if (inString.contains("SquareRoot") ) return string_code.eSquareRoot;
		if (inString.contains("Square") ) return string_code.eSquare;
		System.out.println("Unable to find node");
		throw new IllegalArgumentException(); 
		//return string_code.eNone;
	}

	
	@SuppressWarnings("unused")
	private TreeOperation(){}
	
	public TreeOperation(Vector<String> treeAsVector){
		v.addAll(treeAsVector);
	}
	
	public TreeOperation(String inputFileName, double currentDCLength ) throws FileNotFoundException{
		dcLength = currentDCLength; // passed from DC to replace X
		input = new File(inputFileName);
		// Get input data 
 		if (!input.exists()) {
 			System.out.println("Invalid input file: " + input.toString());
			throw new java.io.FileNotFoundException("ahhg");
 		}
 		
 		v.clear();
 		/*
 		v.add("Add");
 		v.add("**Power");
 		v.add("****Divide");
 		v.add("******InputVal:X0");
 		v.add("******Const: 0.911802");
 		v.add("****Multiply");
 		v.add("******Exponential");
 		v.add("********Const: 0.694571");
 		v.add("******Sine");
 		v.add("********Const: 0.236641");
 		v.add("**Divide");
 		v.add("****Power");
 		v.add("******InputVal:X0");
 		v.add("******Const: 0.911802");
 		v.add("****Sine");
 		v.add("******Cosine");
 		v.add("********Const: 0.422742");
 */
 	    
	}
	
	
	 
	public void populateGpTreeVector()
	{
		BufferedReader reader;
 		
 		try {
			reader = new BufferedReader(new FileReader(input));
			String line;
			
			while ((line = reader.readLine()) != null) {
				v.add(line);			
			}

			reader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (IOException  io) {
			io.printStackTrace();
		}

	}
	
	public AbstractNode getTree(){
		
		if (v.size() == 0)
		{
			System.out.println("Unable to load GP from file " + input.toString());
			
			return null;
		}
		stringToGP( 0);
		return retFtn;
	}
	
	void stringToGP( int depth) {
		//retFtn = createNode(v.get(0));
		AbstractNode newChild = createNode(v.get(0));
		//treeVector.erase(treeVector.begin());
		 vectoriteration = 1;
		// String name = retFtn.getLabel();
		// int numchild = //retFtn.getNumChildren();
		/* for (int i = 0; i < retFtn.getNumChildren(); i++) {
			 retFtn.getChildren().add(getGPTreeChildren(1));
		 }*/
		 getGPTreeChildren(newChild, 1);
		 
		 retFtn = newChild;
		 //System.out.println("");

	}
	 
	AbstractNode getGPTreeChildren(AbstractNode retFtn1, int depth)
	{
		AbstractNode child = null;
		int childDepth = depth * 2;
		for (int i = 0; i < retFtn1.getNumChildren(); i++) {
			child = findChild(childDepth);
			if (child != null)
			{
				if (retFtn1.getNumChildren() > 0)
					getGPTreeChildren(child, depth + 1);
				retFtn1.addChild(child);
			}
		}
		return retFtn1;
	}

	/*static void getGPTreeChildren(AbstractNode retFtn1, int depth)
	{
		
		int childDepth = depth * 2;
		for (int i = 0; i < retFtn1.getNumChildren(); i++) {
			AbstractNode child = findChild(childDepth);
			if (child != null)
			{
				if (retFtn1.getNumChildren() > 0)
					getGPTreeChildren(child, depth + 1);
				retFtn1.addChild(child);
			}
		}
	
	}*/

	
	AbstractNode getGPTreeChildren(int depth)
	{
		//System.out.println(depth);
		AbstractNode child = null;
		int childDepth = depth * 2;
		child = findChild(childDepth);
		
		if (child != null)
		{
			//System.out.println(child.getClass().toString());
			for (int i = 0; i < child.getNumChildren(); i++) {
				
				AbstractNode grandChild  = getGPTreeChildren(depth + 1);
				grandChild.setNodeIndex(i);
				grandChild.setParent(child);
				child.getChildren().add(grandChild);
			}
		}
		return child;
	}
	
	AbstractNode findChild( int childDepth)
	{
		AbstractNode rtnFtnChild = null;
		int vCount = vectoriteration;
		for (int i = vCount; i < v.size(); i++)
		{
			long count = v.get(i).codePoints().filter(ch -> ch =='*').count();
			int toInt = java.lang.Math.toIntExact(count);
			if (childDepth == toInt)
			{
				vectoriteration = vectoriteration + 1;
				rtnFtnChild = createNode(v.get(i));
				break;
			}
		}
		return rtnFtnChild;
	}
	
	
	static AbstractNode createNode(String nodeName) {
		AbstractNode retFtn = null;
		switch (hashit(nodeName)) {
			case eConstNode:
			{
				String items[] = nodeName.split(":"); 
				String constValueStr = items[1].trim();
				
				double constValueDbl = Double.parseDouble(constValueStr);
				retFtn = new ConstNode(constValueDbl);
				break;
			}
			case eInputNode:
			{
				String item = nodeName;
				String inputValue = item.substring(item.length() - 1);
				
				retFtn = new InputNode(NUM_OF_INPUTS);
				 if( retFtn instanceof InputNode ){
			            // Type cast to DerivedNode to access bar
			            ((InputNode)retFtn).setValues(Integer.parseInt(inputValue));
			        }
			        else {
			            // Throw exception or what ever
			            throw new RuntimeException("Invalid Object Type");
			        }

				
				break;
			}
			case eAdd:
			{
				retFtn = new Add();
				break;
			}
			case eSubtract:
			{
				retFtn = new Subtract();
				break;
			}
			case eMultiply:
			{
				retFtn = new Multiply();
				break;
			}
			case eDivide:
			{
				retFtn = new Divide();
				break;
			}
			case eExponential:
			{
				retFtn = new Exponential();
				break;
			}
			case eLog:
			{
				retFtn = new Log();
				break;
			}
			case eSine:
			{
				retFtn = new Sine();
				break;
			}
			case eCosine:
			{
				retFtn = new Cosine();
				break;
			}
			case ePower:
			{
				retFtn = new Power();
				break;
			}
			case eSquareRoot:
			{
				retFtn = new SquareRoot();
				break;
			}
			case eSquare:
			{
				retFtn = new Square();
				break;
			}
		}
		return retFtn;
	}
	
	static AbstractNode createNodeReplacingConsts(String nodeName) {
		AbstractNode retFtn = null;
		switch (hashit(nodeName)) {
			case eConstNode:
			{
				Random random = null;
				int threshold = random.nextInt((Const.doubles.length - 1) - 0 + 1) + 0;
				retFtn = new ConstNode(Const.doubles[threshold]);
				
				break;
			}
			case eInputNode:
			{
				String item = nodeName;
				String inputValue = item.substring(item.length() - 1);
				
				retFtn = new InputNode(NUM_OF_INPUTS);
				 if( retFtn instanceof InputNode ){
			            // Type cast to DerivedNode to access bar
			            ((InputNode)retFtn).setValues(Integer.parseInt(inputValue));
			        }
			        else {
			            // Throw exception or what ever
			            throw new RuntimeException("Invalid Object Type");
			        }

				
				break;
			}
			case eAdd:
			{
				retFtn = new Add();
				break;
			}
			case eSubtract:
			{
				retFtn = new Subtract();
				break;
			}
			case eMultiply:
			{
				retFtn = new Multiply();
				break;
			}
			case eDivide:
			{
				retFtn = new Divide();
				break;
			}
			case eExponential:
			{
				retFtn = new Exponential();
				break;
			}
			case eLog:
			{
				retFtn = new Log();
				break;
			}
			case eSine:
			{
				retFtn = new Sine();
				break;
			}
			case eCosine:
			{
				retFtn = new Cosine();
				break;
			}
			case ePower:
			{
				retFtn = new Power();
				break;
			}
			case eSquareRoot:
			{
				retFtn = new SquareRoot();
				break;
			}
			case eSquare:
			{
				retFtn = new Square();
				break;
			}
		}
		return retFtn;
	}
	

}
