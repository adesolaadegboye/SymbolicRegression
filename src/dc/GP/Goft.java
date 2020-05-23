package dc.GP;


import java.io.Serializable;
import java.util.ArrayList;


public interface Goft  extends Serializable{

		//String label;
		//int numChildren;
		public ArrayList<Goft> children() ;
		double eval(double inVal);  //setting the 0 makes it a PURE
		Object deepClone(Object object); //make a deep copy of the current tree
		 String getLabel();
		
		
		
}
