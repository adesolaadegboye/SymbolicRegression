package dc.GP;

import java.util.ArrayList;
import java.util.Map.Entry;

public abstract class AbstractNode implements Cloneable,Comparable<AbstractNode>{

	public String label = null;
	public int numChildren = -1;
	public double perfScore = -1;
	//public ArrayList<AbstractNode> children;
	public int nodeIndex = -1;
	
	public abstract Object deepCopy(Object oldObj) throws Exception;
	
	public  abstract AbstractNode clone();
	
	
	public abstract double eval(double inVal);
	
	public abstract double eval(double inVal, double magnitude);
	//GOftn clone() = 0; //make a deep copy of the current tree
	public  abstract  String getLabel(); 
	
	public abstract int getNumChildren();
	
	public abstract ArrayList<AbstractNode> getChildren();
	
	public abstract void setParent(AbstractNode parent);
	
	public abstract AbstractNode getParent();
	
	public abstract  double getPerfScore();
	
	public abstract void addChild(AbstractNode child);
	
	public abstract String printAsInFixFunction();
	
	public abstract String printAsInFixFunctionSimplify();
	
	public abstract AbstractNode pruneNode();
	
	public abstract AbstractNode cloneAndReplaceLeafNode();
	
	
	
	@Override
	public boolean equals(Object o)
	 {
		 // self check
	    if (this == o)
	        return true;
	    // null check
	    if (o == null)
	        return false;
	    // type check and cast
	    if (getClass() != o.getClass())
	        return false;
	    
	    // field comparison
	    String oString = (String) ((AbstractNode)o).printAsInFixFunction();
	    String thatString = this.printAsInFixFunction();
	    return ((AbstractNode)o).perfScore == this.perfScore
	            && oString.equals(thatString);
	}
	
	

	public int compareTo(AbstractNode that) {
		final int BEFORE = -1;
	    final int EQUAL = 0;
	    final int AFTER = 1;
	    int compareValue = Double.compare(this.perfScore, that.perfScore);
	    if (compareValue ==   AFTER) return AFTER;
	    else if (compareValue == BEFORE) return BEFORE;
	    else if ( compareValue == EQUAL){
	    	int thisGp = TreeHelperClass.getDepth(this, 0);
	    	int thatGp = TreeHelperClass.getDepth(that, 0);
	    	if (thisGp > thatGp)
	    		return AFTER;
	    	if (thisGp < thatGp)
	    		return BEFORE;
	    	return EQUAL;
	    }
	    else return EQUAL;
		
	}
	
	
	 public boolean isDouble(String str) {
	        try {
	            Double.parseDouble(str);
	            return true;
	        } catch (NumberFormatException e) {
	            return false;
	        }
	    }
	 public abstract void setNodeIndex(int index);
	
	 public abstract int getNodeIndex();
	 
	 public  abstract  String getType(); 
	 
	 boolean isValueNumeric(String stringValue){
			String items[] = stringValue.split(":"); 
			//System.out.println(stringValue);
			//System.out.println(" \n");
			String constValueStr;
			
			if (items.length >= 1 && items[0].contains("Const"))
			{
				//System.out.println(items[0]);
				//System.out.println(stringValue);
				constValueStr = items[1].trim();
			}
			else
			{
				return false;
			}
			try{
			    double d= Double.valueOf(constValueStr);
			   /* if (d==(int)d){
			        System.out.println("integer"+(int)d);
			    }else{
			        System.out.println("double"+d);
			    }*/
				return true;
			}catch(Exception e){
			    System.out.println("not number");
				return false;
			}
		}
		
		double valueToNumeric(String stringValue){
			String items[] = stringValue.split(":"); 		
			double d = -1.0;
			String constValueStr;
			if (items.length >= 1 )
			{
				//System.out.println(stringValue);
				constValueStr = items[1].trim();
			}
			else
			{
				return -1.0;
			}
			try{
			    d= Double.valueOf(constValueStr);
			   /* if (d==(int)d){
			        System.out.println("integer"+(int)d);
			    }else{
			        System.out.println("double"+d);
			    }*/
				
			}catch(Exception e){
			    System.out.println("not number");
				return d;
			}
			return d;
		}

}
