package dc.GP;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;


public class test {

	public static void main(String[] args) throws FileNotFoundException {
		// TODO Auto-generated method stub
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();
		map.put(1, 16);
		map.put(99, 340 );
		map.put(130, 653 );
		
		map.put(7 ,0); 
		map.put(7, 0); 
		map.put(2, 0); 
		map.put(8, 0); 
		map.put(46, 8);
		map.put(122, 23); 
		map.put(72, 92); 
		map.put(45, 583);
		map.put(45, 53);
		map.put(124, 20);
		map.put(67, 164);
		map.put(96, 416);
		map.put(24, 217);
		map.put(74, 30);
		map.put(10, 0);
		map.put(13, 20);
		map.put(31, 162);
		map.put(56, 220);
		map.put(51, 94);
		map.put(77, 18);
		map.put(48, 161);
		map.put(33, 56);
		map.put(42, 119);
		map.put(78, 364);
		map.put(93, 242);
		map.put(3, 0);
		map.put(18, 8);
		map.put(104, 640); 
		map.put(35, 400);
		map.put(4, 0);
		map.put(8, 429); 
		map.put(45, 725);
		
		
		
		
		/* this is a test if I can read GP from tree
			TreeOperation opr = new TreeOperation("test", 0.0);
			AbstractNode  mytree =  opr.getTree();
			String  testStr = TreeHelperClass.printTreeToString(mytree,0);
			System.out.println(testStr);
		*/
	
		 Vector<AbstractNode> GpTreeVec = new Vector<AbstractNode>(); 
      /*  for (int treeCount =0; treeCount < 30; treeCount++ )
        {
        	TreeCreation treeCreation = new TreeCreation(treeCount);
      	  
      	  	AbstractNode randomTree = treeCreation.tree;
      	  	double score = TreeHelperClass.evaluateRegressionTree2(randomTree,map);
      	  	randomTree.perfScore = score;
      	  System.out.println("regression score is : " + randomTree.perfScore);
      	  	GpTreeVec.add(randomTree);
      	  String  testStr = TreeHelperClass.printTreeToString(randomTree,0);
			System.out.println(testStr);
        }
        Comparator comparator = Collections.reverseOrder();
        Collections.sort(GpTreeVec,comparator);        
        for (int treeCount =0; treeCount < 30; treeCount++ ){
        	 System.out.println("Tree + " +(treeCount+1) + " " +  GpTreeVec.get(treeCount).perfScore);
       	// System.out.println(TreeHelperClass.printTreeToString(GpTreeVec.get(treeCount),0));
        }*/
        /*
         * Test clone of trees
        Vector<AbstractNode> GpTreeVecCopy = new Vector<AbstractNode>(); 
        for (int treeCount =0; treeCount < 30; treeCount++ ){
        	
        	System.out.println("Original " +TreeHelperClass.printTreeToString(GpTreeVec.get(treeCount),0) + " performance" + GpTreeVec.get(treeCount).perfScore);
        	AbstractNode copy = GpTreeVec.get(treeCount).clone();
        	
        	System.out.println("");
        	
        	System.out.println("Copy" +TreeHelperClass.printTreeToString(copy,0) + " performance" + copy.perfScore);
        	GpTreeVecCopy.add(copy);
        	
        }*/
        /*Delete original and check if copy is still ok
       // GpTreeVec.clear();
        for (int treeCount =0; treeCount < 30; treeCount++ ){
        	
        	System.out.println("Copy test \n " +TreeHelperClass.printTreeToString(GpTreeVecCopy.get(treeCount),0) + " performance" + GpTreeVecCopy.get(treeCount).perfScore);
        		
        }*/
       // AbstractNode selectedTree = GpTreeVec.get(GpTreeVec.size()-1).clone(); //replace with this after-> TreeHelperClass.tournament(GpTreeVec, 4,0);
        
      //  AbstractNode selectedTree2 = GpTreeVec.get(GpTreeVec.size()-2).clone();  //replace with this after->  TreeHelperClass.tournament(GpTreeVec, 4,0);
       	
       // System.out.println("Copy test \n" +TreeHelperClass.printTreeToString(selectedTree,0) + " performance" + selectedTree.perfScore);
       // System.out.println("Copy test \n" +TreeHelperClass.printTreeToString(selectedTree2,0) + " performance" + selectedTree2.perfScore);
		
      /*  String crossOverTreeString = TreeHelperClass.crossOverTree(selectedTree, selectedTree2);
        crossOverTreeString = crossOverTreeString.replace(",", "");
       // System.out.println("Crossover tree" + crossOverTreeString);
        String crossOverTreeStringArray[] = crossOverTreeString.split("\\r?\\n");
        Vector<String> crossOverTreeStringVector = new Vector<String>(Arrays.asList(crossOverTreeStringArray));
        TreeOperation treeOperation = new TreeOperation(crossOverTreeStringVector);
        AbstractNode newTree = treeOperation.getTree();
        double score = TreeHelperClass.evaluateRegressionTree2(newTree,map);
        newTree.perfScore = score;
        System.out.println("Crossover \n" +TreeHelperClass.printTreeToString(newTree,0) + " performance" + newTree.perfScore);
	
        String mutateTreeString  = TreeHelperClass.mutateTree(newTree);
        mutateTreeString = mutateTreeString.replace(",", "");
        //System.out.println("mutated tree" + mutateTreeString);
        String mutateTreeStringArray[] = mutateTreeString.split("\\r?\\n");
        Vector<String> mutateTreeStringVector = new Vector<String>(Arrays.asList(mutateTreeStringArray));
        TreeOperation treeOperation2 = new TreeOperation(mutateTreeStringVector);
        AbstractNode newTree2 = treeOperation2.getTree();
        double score2 = TreeHelperClass.evaluateRegressionTree2(newTree2,map);
        newTree2.perfScore = score2;
        System.out.println("Mutation: " +TreeHelperClass.printTreeToString(newTree2,0) + " performance" + newTree2.perfScore);
	*/
        TreeOperation opr = new TreeOperation("test", 0.0);
		AbstractNode  mytree =  opr.getTree();
		String  testStr;
		//System.out.println(testStr);
        
        for (int i =0; i<5 ; i++ )
		{		
			System.out.println("Tree");
			TreeCreation treeCreation=  new TreeCreation(i);
			AbstractNode randomTree = treeCreation.tree;
			int depth = TreeHelperClass.getDepth(randomTree, 0);
			
			//System.out.println("Depth is " + depth);
			AbstractNode randomTree1 = null; 
			if (depth >1 )
			{
				for (int j = 1; j < depth; j++){
					
					 System.out.println("A " +TreeHelperClass.printTreeToString(randomTree, 0));
					//randomTree1 = TreeHelperClass.pruneTree(randomTree, 0, null);
					
					 //System.out.println("B " + TreeHelperClass.printTreeToString(randomTree1, 0));
					int depth1 = TreeHelperClass.getDepth(randomTree, 0);
					if (depth1 <=1 )
						break;
					
				}
			}
			else
			{
				randomTree1 = randomTree.clone();
			}
			//testStr = TreeHelperClass.printTreeToString(randomTree,0);
			System.out.println("Prune");
			testStr = TreeHelperClass.printTreeToString(randomTree1,0);
			
		}
	
	}

}
