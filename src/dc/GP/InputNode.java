package dc.GP;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class InputNode extends AbstractNode implements Cloneable {

	public String label = "";
	public int numChildren = 0;
	public String type = null;
	public int nodeIndex = -1;
	int inputIndex;
	public ArrayList<AbstractNode> children = new  ArrayList<AbstractNode> ();
	public AbstractNode parent = null;
	InputNode(int numPossibleInputs) {
		
		numChildren = 0;
		if (numPossibleInputs == 1)
			inputIndex = 0;
		else
		{
			Random generator = new Random(); 
			inputIndex = ThreadLocalRandom.current().nextInt(0, numPossibleInputs);
			if (inputIndex ==  1)
				inputIndex = 1;
		}
		setValues(inputIndex);
	}
	
	
	void setValues(int inIndex){
		String lbl = "InputVal:X"+inIndex;
		label = lbl;
	}

	@Override
	public Object deepCopy(Object oldObj) throws Exception {
		ObjectOutputStream oos = null;
	      ObjectInputStream ois = null;
	      try
	      {
	         ByteArrayOutputStream bos = 
	               new ByteArrayOutputStream(); // A
	         oos = new ObjectOutputStream(bos); // B
	         // serialize and pass the object
	         oos.writeObject(oldObj);   // C
	         oos.flush();               // D
	         ByteArrayInputStream bin = 
	               new ByteArrayInputStream(bos.toByteArray()); // E
	         ois = new ObjectInputStream(bin);                  // F
	         // return the new object
	         return (InputNode) ois.readObject(); // G
	      }
	      catch(Exception e)
	      {
	         System.out.println("Exception in ObjectCloner = " + e);
	         throw(e);
	      }
	      finally
	      {
	         oos.close();
	         ois.close();
	      }

	}

	@Override
	public double eval(double inVal) {
		return inVal;
	}

	@Override
	public String getLabel() {
		// TODO Auto-generated method stub
		return label;
	}
	
	@Override
	public InputNode clone() {
		AbstractNode inputNode = new InputNode(1);
		
		inputNode.perfScore =  this.perfScore;
		((InputNode)inputNode).label = this.label;
		((InputNode)inputNode).numChildren = this.numChildren;
		((InputNode)inputNode).parent =  this.parent;
		((InputNode)inputNode).setValues(this.inputIndex);
		((InputNode)inputNode).setNodeIndex(this.nodeIndex);
		
	        for (int i=0; i<this.numChildren; i++) {
	        	AbstractNode childAdd = this.children.get(i).clone();
	        	childAdd.setNodeIndex(i);
	        	childAdd.setParent(inputNode);
	        	((InputNode)inputNode).children.add(childAdd);
	    	}
	       // this.children.clear();
	        return ((InputNode)inputNode);
	}
	
	@Override
	public int getNumChildren() {
		
		return numChildren;
	}

	@Override
	public ArrayList<AbstractNode> getChildren() {
		// TODO Auto-generated method stub
		return children;
	}

	@Override
	public double getPerfScore() {
		return perfScore;
	}

	@Override
	public void addChild(AbstractNode child) {
		throw new IllegalArgumentException(); 
		//children.add(child);
		
	}


	@Override
	public String printAsInFixFunction() {
		String stringValue =  this.label;
		String[] parts = stringValue.split(":");
		
		return "("+parts[1]+")";
	}
	
	@Override
	public void setParent(AbstractNode parent) {
		this.parent = parent;
	}

	@Override
	public AbstractNode getParent() {
		// TODO Auto-generated method stub
		return parent;
	}


	@Override
	public InputNode cloneAndReplaceLeafNode() {
		AbstractNode inputNode = new InputNode(1);
		
		inputNode.perfScore =  this.perfScore;
		((InputNode)inputNode).label = this.label;
		((InputNode)inputNode).numChildren = this.numChildren;
		((InputNode)inputNode).parent =  this.parent;
		((InputNode)inputNode).setValues(this.inputIndex);
	        for (int i=0; i<this.numChildren; i++) {
	        	AbstractNode childAdd = this.children.get(i).cloneAndReplaceLeafNode();
	        	childAdd.setNodeIndex(i);
	        	childAdd.setParent(inputNode);
	        	((InputNode)inputNode).children.add(childAdd);
	    	}
	       // this.children.clear();
	        return ((InputNode)inputNode);
	}


	@Override
	public String printAsInFixFunctionSimplify() {
		String stringValue =  this.label;
		String[] parts = stringValue.split(":");
		
		return "("+parts[1]+")";
	}
	
	@Override
	public String getType() {
		return Const.LEAF_NODE_TYPE;
	}
	
	@Override
	public void setNodeIndex(int index) {
		this.nodeIndex = index;
		
	}
	
	@Override
	public int getNodeIndex() {
		return this.nodeIndex;
	}


	@Override
	public AbstractNode pruneNode() {
		// TODO Auto-generated method stub
		return this;
	}


	@Override
	public double eval(double inVal, double magnitude) {
		if (label.equalsIgnoreCase("InputVal:X0"))
			return inVal;
		else if (label.equalsIgnoreCase("InputVal:X1")){
			int retval = Double.compare(magnitude, 0.0);
			if (retval == 0)
			{
				if (parent != null && (parent.getLabel() == "Power" ||
						parent.getLabel() == "Multiply" ||
						parent.getLabel() == "Divide" ||
						parent.getLabel() == "SquareRoot" ||
						parent.getLabel() == "Sine" ||
						parent.getLabel() ==  "Cosine" || 
						parent.getLabel() ==  "Square"
						))
					return 1;
			}
			else
				return magnitude;
		}
			
		else
			System.exit(-1);
		
		return 0.0; // should not get here
	}

}
