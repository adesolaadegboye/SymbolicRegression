package dc.GP;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class SquareRoot extends AbstractNode implements Cloneable {

	public String label = "SquareRoot";
	public int numChildren =1; 
	public String type = null;
	public int nodeIndex = -1;
	public ArrayList<AbstractNode> children = new  ArrayList<AbstractNode> (1);
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
	         return (SquareRoot) ois.readObject(); // G
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
	public SquareRoot clone() {
AbstractNode squareRoot = new SquareRoot();
		
squareRoot.perfScore =  this.perfScore;
		((SquareRoot)squareRoot).label = this.label;
		((SquareRoot)squareRoot).numChildren = this.numChildren;
		((SquareRoot)squareRoot).parent =  this.parent;
		((SquareRoot)squareRoot).setNodeIndex(this.nodeIndex);
	        for (int i=0; i<this.numChildren; i++) {
	        	AbstractNode childAdd = this.children.get(i).clone();
	        	childAdd.setNodeIndex(i);
	        	childAdd.setParent(squareRoot);
	        	((SquareRoot)squareRoot).children.add(childAdd);
	    	}
	       // this.children.clear();
	        return ((SquareRoot)squareRoot);
	}

	@Override
	public double eval(double inVal) {
		if (this.children.get(0) != null ){
			double childValue = (this.children.get(0)).eval(inVal);
			
			
			if  ( childValue == Double.MAX_VALUE || childValue == Double.NEGATIVE_INFINITY ||
				 childValue == Double.POSITIVE_INFINITY || childValue ==  Double.NaN ||
				 Double.isInfinite(childValue) || Double.isNaN(childValue)||
				 inVal == Double.MAX_VALUE || inVal == Double.NEGATIVE_INFINITY ||
			     inVal == Double.POSITIVE_INFINITY || inVal ==  Double.NaN ||
			    Double.isInfinite(inVal) || Double.isNaN(inVal))
				return Double.MAX_VALUE;
			
			
			double evalValue = Math.sqrt((this.children.get(0)).eval(inVal));
		//	System.out.println( evalValue );
			if  ( evalValue == Double.MAX_VALUE || evalValue == Double.NEGATIVE_INFINITY ||
				  evalValue == Double.POSITIVE_INFINITY || evalValue ==  Double.NaN ||
				 Double.isInfinite(evalValue) || Double.isNaN(evalValue))
				return Double.MAX_VALUE;
			else
				return evalValue;
		}
		else {
			System.out.println( "left not defined in square root");
			return Double.MAX_VALUE;
		}
	}

	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public int getNumChildren() {
		return numChildren;
	}

	@Override
	public ArrayList<AbstractNode> getChildren() {
		return children;
	}

	@Override
	public void setParent(AbstractNode parent) {
		this.parent = parent;
	}

	@Override
	public AbstractNode getParent() {
		return parent;
	}

	@Override
	public double getPerfScore() {
		return perfScore;
	}

	@Override
	public void addChild(AbstractNode child) {
		child.setNodeIndex(0);
		child.setParent(this);
		children.add(child);
	}

	@Override
	public String printAsInFixFunction() {
		if (this.children.get(0) != null ){
			
			String evalValue = "(Math.sqrt(" + (this.children.get(0)).printAsInFixFunction()+"))";
			return evalValue;
		}
		else {
			System.out.println( "left not defined in Square root");
			return "";
		}
	}

	@Override
	public String printAsInFixFunctionSimplify() {
		if (this.children.get(0) != null ){
			String evalValue = "";
			String value = (this.children.get(0)).printAsInFixFunctionSimplify();
			String value_ = value.substring(1, value.length()-1);
			if(isDouble(value_))
				evalValue = "("+String.valueOf(Math.sqrt(Double.parseDouble(value_)))+")";
			else
				evalValue = "(Math.sqrt(" + (this.children.get(0)).printAsInFixFunctionSimplify()+"))";
			
			return evalValue;
		}
		else {
			System.out.println( "left not defined in Square root");
			return "";
		}
	}

	@Override
	public AbstractNode pruneNode() {
		AbstractNode node1 =  this.getChildren().get(0);
		
		String nodeType1 = node1.getType();
					
		if (nodeType1 == Const.INNER_NODE_TYPE ){
			AbstractNode child1  = node1.pruneNode();
			
			if ( child1!= null)
			{
				child1.setNodeIndex(0);
				child1.setParent(this);
				this.getChildren().set(0, child1);
			}
			if (isValueNumeric(child1.getLabel()) == true  ){
				double evalValue = Math.sqrt(valueToNumeric(child1.getLabel()));
				if  ( evalValue == Double.MAX_VALUE || evalValue == Double.NEGATIVE_INFINITY ||
						evalValue == Double.POSITIVE_INFINITY || evalValue ==  Double.NaN)
				{
					return this;
				}
				
				String nanValue = Double.toString(evalValue);
				if (nanValue ==  "NaN"){
					//System.out.println("SquareRoot return " + evalValue );
					return this;
				}
				AbstractNode iterateUpNode = new ConstNode(evalValue);
				iterateUpNode.setNodeIndex(this.getNodeIndex());
				iterateUpNode.setParent(this.getParent());
				
				return iterateUpNode;
				
			}
			else
				return this;
			
		}
		else{

			if  (isValueNumeric(node1.getLabel()) == true )
			{
				double nodeValue1 = valueToNumeric(node1.getLabel());
				
				
				AbstractNode parentNode =this.getParent();
				//System.out.println("squareRoot9 return " + node1.getLabel() +  " this label" + this.getLabel() + " " + node1.getParent() + " " + this.getParent() ) ;
				
				
				if (parentNode == null)
					return this;
				
									
				double evalValue = Math.sin(nodeValue1);
				//System.out.println( evalValue );
				if  ( evalValue == Double.MAX_VALUE || evalValue == Double.NEGATIVE_INFINITY ||
						evalValue == Double.POSITIVE_INFINITY || evalValue ==  Double.NaN)
				{
					return this;
				}
				else
				{
					AbstractNode newNode = new ConstNode(evalValue);
					//int index = this.getNodeIndex();
					newNode.setNodeIndex(this.getNodeIndex());
					newNode.setParent(this.getParent());
					//parentNode.getChildren().set(index, newNode); ADesola removed this for the time being
					//System.out.println("squareRoot1 return " + evalValue );
					return newNode;
				}
									
			}
			else
			{
				AbstractNode child1  = node1.pruneNode();
				if ( child1!= null)
				{
					child1.setNodeIndex(0);
					child1.setParent(this);
					this.getChildren().set(0, child1);
				}
				if (isValueNumeric(child1.getLabel()) == true  ){
					double value = valueToNumeric(child1.getLabel()) ;
					
					double evalValue = Math.sin(value);
					if  ( evalValue == Double.MAX_VALUE || evalValue == Double.NEGATIVE_INFINITY ||
							evalValue == Double.POSITIVE_INFINITY || evalValue ==  Double.NaN ||
							Double.toString(evalValue)== "Infinity" ||
							Double.isNaN(evalValue) || 
							Double.isInfinite(evalValue) )
					{
						return this;
					}
					AbstractNode iterateUpNode = new ConstNode(evalValue);
					iterateUpNode.setNodeIndex(this.getNodeIndex());
					iterateUpNode.setParent(this.getParent());
					return iterateUpNode;
					
				}
				return this;
			}	
		}

	}

	@Override
	public SquareRoot cloneAndReplaceLeafNode() {
		AbstractNode squareRoot = new SquareRoot();
		
		squareRoot.perfScore =  this.perfScore;
		((SquareRoot)squareRoot).label = this.label;
		((SquareRoot)squareRoot).numChildren = this.numChildren;
		((SquareRoot)squareRoot).parent =  this.parent;
	        for (int i=0; i<this.numChildren; i++) {
	        	AbstractNode childAdd = this.children.get(i).cloneAndReplaceLeafNode();
	        	childAdd.setNodeIndex(i);
	        	childAdd.setParent(squareRoot);
	        	((SquareRoot)squareRoot).children.add(childAdd);
	    	}
	       // this.children.clear();
	        return ((SquareRoot)squareRoot);

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
	public String getType() {
				return "innerNode";
	}

	@Override
	public double eval(double inVal, double magnitude) {
		if (this.children.get(0) != null ){
			double childValue = (this.children.get(0)).eval(inVal,magnitude);
			
			
			if  ( childValue == Double.MAX_VALUE || childValue == Double.NEGATIVE_INFINITY ||
				 childValue == Double.POSITIVE_INFINITY || childValue ==  Double.NaN ||
				 Double.isInfinite(childValue) || Double.isNaN(childValue)||
				 inVal == Double.MAX_VALUE || inVal == Double.NEGATIVE_INFINITY ||
			     inVal == Double.POSITIVE_INFINITY || inVal ==  Double.NaN ||
			    Double.isInfinite(inVal) || Double.isNaN(inVal))
				return Double.MAX_VALUE;
			
			
			double evalValue = Math.sqrt((this.children.get(0)).eval(inVal));
		//	System.out.println( evalValue );
			if  ( evalValue == Double.MAX_VALUE || evalValue == Double.NEGATIVE_INFINITY ||
				  evalValue == Double.POSITIVE_INFINITY || evalValue ==  Double.NaN ||
				 Double.isInfinite(evalValue) || Double.isNaN(evalValue))
				return Double.MAX_VALUE;
			else
				return evalValue;
		}
		else {
			System.out.println( "left not defined in square root");
			return Double.MAX_VALUE;
		}
	}

}
