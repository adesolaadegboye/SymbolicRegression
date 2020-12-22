package dc.GP;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class Subtract extends AbstractNode{

	public String label = "Subtract";
	public int numChildren =2; 
	public String type = null;
	public int nodeIndex = -1;
	public ArrayList<AbstractNode> children = new  ArrayList<AbstractNode> (2);
	public AbstractNode parent = null;
	
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
	         return (Subtract) ois.readObject(); // G
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
		if (this.children.get(0) != null && this.children.get(1) != null){
			double value1 = (this.children.get(0)).eval(inVal);
			double value2 = (this.children.get(1)).eval(inVal);
			if  ( value2 == Double.MAX_VALUE || value2 == Double.NEGATIVE_INFINITY ||
					value2 == Double.POSITIVE_INFINITY || value2 ==  Double.NaN ||
					 Double.isInfinite(value2) || Double.isNaN(value2) ||
					value1 == Double.MAX_VALUE || value1 == Double.NEGATIVE_INFINITY ||
					value1 == Double.POSITIVE_INFINITY || value1 ==  Double.NaN ||
							 Double.isInfinite(value1) || Double.isNaN(value1)	
										)
					return Double.MAX_VALUE;
			
			double value3 = value1 - value2;
			if  ( value3 == Double.MAX_VALUE || value3 == Double.NEGATIVE_INFINITY ||
					value3 == Double.POSITIVE_INFINITY || value3 ==  Double.NaN ||
					 Double.isInfinite(value3) || Double.isNaN(value3))
					return Double.MAX_VALUE;
				else
					return value3;
		}
		else {
			System.out.println( "left and right not defined in subtract");
			return Double.MAX_VALUE;
		}
	}

	@Override
	public String getLabel() {
		
		return label;
	}

	@Override
	public Subtract clone() {
		AbstractNode subtract = new Subtract();
		 
		 subtract.perfScore =  this.perfScore;
		 ((Subtract)subtract).label = this.label;
		 ((Subtract)subtract).parent = this.parent;
		 ((Subtract)subtract).numChildren = this.numChildren;
		 ((Subtract)subtract).parent =  this.parent;
		 ((Subtract)subtract).setNodeIndex(this.nodeIndex);
	        for (int i=0; i<this.numChildren; i++) {
	        	AbstractNode childAdd = this.children.get(i).clone();
	        	childAdd.setNodeIndex(i);
	        	childAdd.setParent(subtract);
	        	
	        	((Subtract)subtract).children.add(childAdd);
	    	}
	      //  this.children.clear();
	        return ((Subtract)subtract);
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
		if (this.children.isEmpty())
			child.setNodeIndex(0);
		else
			child.setNodeIndex(1);
		children.add(child);
		
	}
	
	@Override
	public String printAsInFixFunction() {
		if (this.children.get(0) != null && this.children.get(1) != null){
			String  value1 = (this.children.get(0)).printAsInFixFunction();
			String value2 = (this.children.get(1)).printAsInFixFunction();
			String value3 = "("+value1+ "-" +value2 +")";
		//	System.out.println( value1+ " " +value2  + " "+value3);
			return value3;
		}
		else {
			System.out.println( "left and right not defined in Subtract");
			return "";
		}
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
	public Subtract cloneAndReplaceLeafNode() {
		AbstractNode subtract = new Subtract();
		 
		 subtract.perfScore =  this.perfScore;
		 ((Subtract)subtract).label = this.label;
		 ((Subtract)subtract).parent = this.parent;
		 ((Subtract)subtract).numChildren = this.numChildren;
		 ((Subtract)subtract).parent =  this.parent;
	        for (int i=0; i<this.numChildren; i++) {
	        	 AbstractNode childAdd = this.children.get(i).cloneAndReplaceLeafNode();
		        	childAdd.setNodeIndex(i);
		        	childAdd.setParent(subtract);
	        	((Subtract)subtract).children.add(childAdd);
	    	}
	     //   this.children.clear();
	        return ((Subtract)subtract);
	}

	@Override
	public String printAsInFixFunctionSimplify() {
		if (this.children.get(0) != null && this.children.get(1) != null){
			String  value1 = (this.children.get(0)).printAsInFixFunctionSimplify();
			String value1_ = value1.substring(1, value1.length()-1);
			String value2 = (this.children.get(1)).printAsInFixFunctionSimplify();
			String value2_ = value2.substring(1, value2.length()-1);
			
			String value3 = "";
			if (isDouble(value1_) && isDouble(value2_))
				value3 = "("+String.valueOf(Double.parseDouble(value1_) - Double.parseDouble(value2_))+")";
			else
				value3 = "("+value1+ "-" +value2 +")";
		//	System.out.println( value1+ " " +value2  + " "+value3);
			return value3;
		}
		else {
			System.out.println( "left and right not defined in Subtract");
			return "";
		}
	}
	
	@Override
	public String getType() {
		// TODO Auto-generated method stub
		return "innerNode";
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
		
			AbstractNode node1 =  this.getChildren().get(0);
			AbstractNode node2 =  this.getChildren().get(1);
			
			
			
			String nodeType1 = node1.getType();
			String nodeType2 = node2.getType();
			
			if (nodeType1 == Const.INNER_NODE_TYPE && nodeType2 == Const.INNER_NODE_TYPE){
				AbstractNode child1  = node1.pruneNode();
				AbstractNode child2  = node2.pruneNode();
				if ( child1!= null)
				{
					child1.setNodeIndex(0);
					child1.setParent(this);
					this.getChildren().set(0, child1);
				}
				if( child2!= null){
					child2.setNodeIndex(1);
					child2.setParent(this);
					this.getChildren().set(1, child2);
				}
				
				if (isValueNumeric(child1.getLabel()) == true && isValueNumeric(child2.getLabel()) == true ){
					double value = valueToNumeric(child1.getLabel()) - valueToNumeric(child2.getLabel());
					AbstractNode iterateUpNode = new ConstNode(value);
					iterateUpNode.setNodeIndex(this.getNodeIndex());
					iterateUpNode.setParent(this.getParent());
					return iterateUpNode;
					
				}
				return this;
				
			}
			else{

				if  (isValueNumeric(node1.getLabel()) == true  &&  isValueNumeric(node2.getLabel()) == true)
				{
					double nodeValue1 = valueToNumeric(node1.getLabel());
					double nodeValue2 = valueToNumeric(node2.getLabel());
					
					AbstractNode parentNode =this.getParent();
					if (parentNode == null)
						return this;
					
					double total = -1;
					total = nodeValue1 - nodeValue2;
					
					AbstractNode newNode = new ConstNode(total);
					int index = this.getNodeIndex();
					newNode.setNodeIndex(this.getNodeIndex());
					newNode.setParent(this.getParent());
					return newNode;
										
				}
				else
				{
					AbstractNode child1  = node1.pruneNode();
					AbstractNode child2  = node2.pruneNode();
					if ( child1!= null)
					{
						child1.setNodeIndex(0);
						child1.setParent(this);
						this.getChildren().set(0, child1);
					}
					if( child2!= null){
						child2.setNodeIndex(1);
						child2.setParent(this);
						this.getChildren().set(1, child2);
					}
					
					if (isValueNumeric(child1.getLabel()) == true && isValueNumeric(child2.getLabel()) ==true ){
						double value = valueToNumeric(child1.getLabel()) - valueToNumeric(child2.getLabel());
						AbstractNode iterateUpNode = new ConstNode(value);
						iterateUpNode.setNodeIndex(this.getNodeIndex());
						iterateUpNode.setParent(this.getParent());
						return iterateUpNode;
						
					}
					return this;
				}	
			}
		
	}

	@Override
	public double eval(double inVal, double magnitude) {
		if (this.children.get(0) != null && this.children.get(1) != null){
			double value1 = (this.children.get(0)).eval(inVal,magnitude);
			double value2 = (this.children.get(1)).eval(inVal,magnitude);
			if  ( value2 == Double.MAX_VALUE || value2 == Double.NEGATIVE_INFINITY ||
					value2 == Double.POSITIVE_INFINITY || value2 ==  Double.NaN ||
					 Double.isInfinite(value2) || Double.isNaN(value2) ||
					value1 == Double.MAX_VALUE || value1 == Double.NEGATIVE_INFINITY ||
					value1 == Double.POSITIVE_INFINITY || value1 ==  Double.NaN ||
							 Double.isInfinite(value1) || Double.isNaN(value1)	
										)
					return Double.MAX_VALUE;
			
			double value3 = value1 - value2;
			if  ( value3 == Double.MAX_VALUE || value3 == Double.NEGATIVE_INFINITY ||
					value3 == Double.POSITIVE_INFINITY || value3 ==  Double.NaN ||
					 Double.isInfinite(value3) || Double.isNaN(value3))
					return Double.MAX_VALUE;
				else
					return value3;
		}
		else {
			System.out.println( "left and right not defined in subtract");
			return Double.MAX_VALUE;
		}
	}

	
}
