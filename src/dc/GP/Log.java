package dc.GP;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class Log  extends AbstractNode implements Cloneable{

	public String label = "Log";
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
	         return (Log) ois.readObject(); // G
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
		double childValue = 0.0;
		if (this.children.get(0) != null ){
			childValue = this.children.get(0).eval(inVal);
			if  ( inVal == Double.MAX_VALUE || inVal == Double.NEGATIVE_INFINITY ||
				     inVal == Double.POSITIVE_INFINITY || inVal ==  Double.NaN ||
				    Double.compare(inVal, 0.0)  < 0  || Double.isInfinite(inVal) || Double.isNaN(inVal) ||
					 childValue == Double.MAX_VALUE || childValue == Double.NEGATIVE_INFINITY ||
					 childValue == Double.POSITIVE_INFINITY || childValue ==  Double.NaN ||
					Double.compare(childValue, 0.0)  < 0   || Double.isInfinite(childValue) || Double.isNaN(childValue))
				return  Double.MAX_VALUE;
			
			// Removed this conversion as NaN will return MAX_VALUE below
			//double var1 = Math.abs(childValue);  // Handle Nan
			//if (var1 == 0)
			//	var1 = 1.0;
			
			
			double evalValue = Math.log(childValue);
		//	System.out.println( evalValue );
			if  ( evalValue == Double.MAX_VALUE || evalValue == Double.NEGATIVE_INFINITY ||
					evalValue == Double.POSITIVE_INFINITY || evalValue ==  Double.NaN ||
					 Double.isInfinite(evalValue) || Double.isNaN(evalValue))
				return  Double.MAX_VALUE;
			else
				return evalValue;
		}
		else {
			System.out.println( "left not defined in Log");
			return Double.MAX_VALUE;
		}
	}

	@Override
	public String getLabel() {
		return label;
	}
	
	@Override
	public Log clone() {
		AbstractNode   log = new Log();
		
		log.perfScore =  this.perfScore;
		((Log)log).label = this.label;
		((Log)log).numChildren = this.numChildren;
		((Log)log).parent =  this.parent;
		((Log)log).setNodeIndex(this.nodeIndex);
	        for (int i=0; i<this.numChildren; i++) {
	        	AbstractNode childAdd = this.children.get(i).clone();
	        	childAdd.setNodeIndex(i);
	        	childAdd.setParent(log);
	        	((Log)log).children.add(childAdd);
	    	}
	      //  this.children.clear();
	        return ((Log)log);
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
		child.setNodeIndex(0);
		child.setParent(this);
		children.add(child);
		
	}

	@Override
	public String printAsInFixFunction() {
		if (this.children.get(0) != null ){
					
			String evalValue = "(Math.log(" + (this.children.get(0)).printAsInFixFunction()+"))";
			return evalValue;
		}
		else {
			System.out.println( "left not defined in Log");
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
	public Log cloneAndReplaceLeafNode() {
AbstractNode   log = new Log();
		
		log.perfScore =  this.perfScore;
		((Log)log).label = this.label;
		((Log)log).numChildren = this.numChildren;
		((Log)log).parent =  this.parent;
	        for (int i=0; i<this.numChildren; i++) {
	        	AbstractNode childAdd = this.children.get(i).cloneAndReplaceLeafNode();
	        	childAdd.setNodeIndex(i);
	        	childAdd.setParent(log);
	        	((Log)log).children.add(childAdd);
	    	}
	        //this.children.clear();
	        return ((Log)log);
	}

	@Override
	public String printAsInFixFunctionSimplify() {
		if (this.children.get(0) != null ){
			
			String evalValue = "";
			String value = (this.children.get(0)).printAsInFixFunctionSimplify();
			String value_ = value.substring(1, value.length()-1);
			if(isDouble(value_))
				evalValue = "("+String.valueOf(Math.log(Double.parseDouble(value_)))+")";
			else
				evalValue = "(Math.log(" + (this.children.get(0)).printAsInFixFunctionSimplify()+"))";
			
			return evalValue;
		}
		else {
			System.out.println( "left not defined in Log");
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
					
					double evalValue = Math.log(valueToNumeric(child1.getLabel()));
					//System.out.println("Log return " + evalValue );
					if  ( evalValue == Double.MAX_VALUE || evalValue == Double.NEGATIVE_INFINITY ||
							evalValue == Double.POSITIVE_INFINITY || evalValue ==  Double.NaN ||
							Double.toString(evalValue)== "Infinity" ||
							Double.isNaN(evalValue) || 
							Double.isInfinite(evalValue) )
					{
						return this;
					}
					//System.out.println("Log " + evalValue);
					AbstractNode iterateUpNode = new ConstNode(evalValue);
				//	System.out.println("Log2 return " + evalValue );
					iterateUpNode.setNodeIndex(this.getNodeIndex());
					iterateUpNode.setParent(this.getParent());
					return iterateUpNode;
					
				}
				else
				{
					return this;
				}
			}
			else{

				if  (isValueNumeric(node1.getLabel()) == true )
				{
					double nodeValue1 = valueToNumeric(node1.getLabel());
					
					
					AbstractNode parentNode =this.getParent();
					//System.out.println("Log9 return " + node1.getLabel() +  " this label" + this.getLabel() + " " + node1.getParent() + " " + this.getParent() ) ;
					
					if (parentNode == null)
						return this;
										
					double evalValue = Math.log(nodeValue1);
					//System.out.println( evalValue );
					if  ( evalValue == Double.MAX_VALUE || evalValue == Double.NEGATIVE_INFINITY ||
							evalValue == Double.POSITIVE_INFINITY || evalValue ==  Double.NaN)
					{
						;
					}
					else
					{
						AbstractNode newNode = new ConstNode(evalValue);
						int index = this.getNodeIndex();
						newNode.setNodeIndex(this.getNodeIndex());
						newNode.setParent(this.getParent());
					//	System.out.println("Log 1 return " + evalValue );
						return newNode;
					}
					
					//pruneTree(parentNode, depth, null);
					return  this;
										
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
						
						double evalValue = Math.log(value);
						if  ( evalValue == Double.MAX_VALUE || evalValue == Double.NEGATIVE_INFINITY ||
								evalValue == Double.POSITIVE_INFINITY || evalValue ==  Double.NaN)
						{
							return this;
						}
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
		double childValue = 0.0;
		if (this.children.get(0) != null ){
			childValue = this.children.get(0).eval(inVal,magnitude);
			if  ( inVal == Double.MAX_VALUE || inVal == Double.NEGATIVE_INFINITY ||
				     inVal == Double.POSITIVE_INFINITY || inVal ==  Double.NaN ||
				    Double.compare(inVal, 0.0)  < 0  || Double.isInfinite(inVal) || Double.isNaN(inVal) ||
					 childValue == Double.MAX_VALUE || childValue == Double.NEGATIVE_INFINITY ||
					 childValue == Double.POSITIVE_INFINITY || childValue ==  Double.NaN ||
					Double.compare(childValue, 0.0)  < 0   || Double.isInfinite(childValue) || Double.isNaN(childValue))
				return  Double.MAX_VALUE;
			
			
			double evalValue = Math.log(childValue);
		//	System.out.println( evalValue );
			if  ( evalValue == Double.MAX_VALUE || evalValue == Double.NEGATIVE_INFINITY ||
					evalValue == Double.POSITIVE_INFINITY || evalValue ==  Double.NaN ||
					 Double.isInfinite(evalValue) || Double.isNaN(evalValue))
				return  Double.MAX_VALUE;
			else
				return evalValue;
		}
		else {
			System.out.println( "left not defined in Log");
			return Double.MAX_VALUE;
		}
	}
}
