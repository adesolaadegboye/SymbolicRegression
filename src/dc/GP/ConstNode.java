package dc.GP;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Random;

public class ConstNode extends AbstractNode implements Cloneable{

	public String label = "";
	public int numChildren =0; 
	public String type = null;
	public int nodeIndex = -1;
	
	double constVal = -1000000000.0;
	public ArrayList<AbstractNode> children = new  ArrayList<AbstractNode> ();
	public AbstractNode parent = null;
	@SuppressWarnings("unused")
	private ConstNode(){
		
	}
	
	public ConstNode(double preSetVal){
		numChildren = 0;
		if (Double.isNaN(preSetVal))
		{
			int threshold = new Random().nextInt((Const.doubles.length - 1) - 0 + 1) + 0;
			preSetVal = Const.doubles[threshold];
		}
		constVal = preSetVal;
		String valeStr = "Const:" + constVal;
		//System.out.println(valeStr);
		label = valeStr;
	}
	
	public void setNewLabel(double preSetVal){
		
		constVal = preSetVal;
		String valeStr = "Const:" + constVal;
		//System.out.println(valeStr);
		label = valeStr;
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
	         return (ConstNode) ois.readObject(); // G
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
		
		return constVal;
	}

	@Override
	public String getLabel() {
		
		return label;
	}
	
	@Override
	public ConstNode clone() {
		
		AbstractNode constNode = new ConstNode();
		
		constNode.perfScore =  this.perfScore;
		((ConstNode)constNode).label = this.label;
		((ConstNode)constNode).numChildren = this.numChildren;
		((ConstNode)constNode).constVal = this.constVal;
		((ConstNode)constNode).parent =  this.parent;
		((ConstNode)constNode).setNodeIndex(this.nodeIndex);
		// this.children.clear();
		return ((ConstNode)constNode);
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
	public ConstNode cloneAndReplaceLeafNode() {
		AbstractNode constNode = new ConstNode();
		
		Random random = new Random();
		int threshold = random.nextInt((Const.doubles.length - 1) - 0 + 1) + 0;
		double newValue  =Const.doubles[threshold];
		String valeStr = "Const:" + newValue;
		
		
		constNode.perfScore =  this.perfScore;
		((ConstNode)constNode).label = valeStr;
		((ConstNode)constNode).numChildren = this.numChildren;
		((ConstNode)constNode).constVal = newValue;
		((ConstNode)constNode).parent =  this.parent;
		((ConstNode)constNode).setNodeIndex(this.getNodeIndex());
		// this.children.clear();
		return ((ConstNode)constNode);
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
		return constVal;
	}
}
