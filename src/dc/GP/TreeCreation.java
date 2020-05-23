package dc.GP;

import java.util.Arrays;
import java.util.Random;
import java.util.Vector;

public class TreeCreation {

	public TreeCreation(int count) // if count is even grow, if odd number full
	{
		// AbstractNode rootNode = createRandomRegressionNode(true);

		// createRandomTreeBranches(retFtn,0);
		tree = createRandomTree(0, count); // 0 = rootNode; if count is even
											// grow, if odd number full
		// retFtn2 = createRandomTree(0); //rootNode;
		// TreeHelperClass.mutateTree(retFtn);
		// TreeHelperClass.crossOverTree(retFtn, retFtn2);

	}

	/*
	 * After performing mutation or crossover use this constructor to construct
	 * the new tree
	 */
	public TreeCreation(String treeAsString) {
		// AbstractNode rootNode = createRandomRegressionNode(true);

	}

	public AbstractNode tree = null;
	// static AbstractNode retFtn2 = null;

	AbstractNode createRandomTree(int depth, int count) {//
		AbstractNode retFtn = TreeHelperClass.createRandomRegressionNode(true); // Ensure
																				// that
																				// root
																				// is
																				// function

		if (retFtn.getNumChildren() <= 0)
			return retFtn;
		else {
			if (count % 2 == 0)
				TreeHelperClass.createRandomFullTreeBranches(retFtn, depth + 1);
			else
				TreeHelperClass.createRandomTreeBranches(retFtn, depth + 1);
		}
		// System.out.println(TreeHelperClass.printTreeToString(retFtn, 0));
		return retFtn;
	}

	AbstractNode pruneTree(AbstractNode inTree, int depth, AbstractNode resultTree) {

		if (resultTree != null) {
			AbstractNode parentNode = inTree.getParent();
			int nodeIndex = inTree.getNodeIndex();
			if (parentNode == null) // This is the root
				return resultTree;
			else {
				parentNode.getChildren().set(nodeIndex, resultTree);
				// pruneTree(parentNode, depth, null);
				pruneTree(resultTree, depth + 1, null);
				// We have changed the tree structure. We start again from the
				// root.
				boolean isRoot = false;
				AbstractNode rootNode = null;
				AbstractNode InnerNode = parentNode.clone();
				/*
				 * while (isRoot==false) { try { rootNode =
				 * InnerNode.getParent(); } catch(NullPointerException e) {
				 * isRoot = true; }
				 * 
				 * 
				 * if (rootNode == null){ rootNode = InnerNode.clone();
				 * pruneTree(rootNode,0,null); isRoot = true; } else { InnerNode
				 * = rootNode.clone(); }
				 * 
				 * }
				 */
			}

			/*
			 * for (int i=0; i<parentNode.getNumChildren(); i++) {
			 * pruneTree(parentNode.getChildren().get(i), depth+1, null ) ; }
			 */
		} else {

			if (inTree.getNumChildren() == 2) {
				AbstractNode node1 = inTree.getChildren().get(0);
				AbstractNode node2 = inTree.getChildren().get(1);

				String nodeType1 = node1.getType();
				String nodeType2 = node2.getType();

				if (nodeType1 == Const.INNER_NODE_TYPE && nodeType2 == Const.INNER_NODE_TYPE) {
					for (int i = 0; i < inTree.getNumChildren(); i++) {
						pruneTree(inTree.getChildren().get(i), depth + 1, null);
					}

				} else {

					if (isValueNumeric(node1.getLabel()) == true && isValueNumeric(node2.getLabel()) == true) {
						double nodeValue1 = valueToNumeric(node1.getLabel());
						double nodeValue2 = valueToNumeric(node2.getLabel());

						AbstractNode newNode = null;

						if (inTree.getLabel().equals("Multiply")) {
							double total = -1;
							total = nodeValue1 * nodeValue2;

							newNode = new ConstNode(total);

						} else if (inTree.getLabel().equals("Add")) {
							double total = -1;
							total = nodeValue1 + nodeValue2;

							newNode = new ConstNode(total);

						} else if (inTree.getLabel().equals("Divide")) {
							double total = -1;
							total = nodeValue1 / nodeValue2;

							if (total == Double.MAX_VALUE || total == Double.NEGATIVE_INFINITY
									|| total == Double.POSITIVE_INFINITY || total == Double.NaN) {
								total = 1000000000.0;
							}
							newNode = new ConstNode(total);

						} else if (inTree.getLabel().equals("Power")) {
							double total = -1;
							total = Math.pow(nodeValue1, nodeValue2);
							if (total == Double.MAX_VALUE || total == Double.NEGATIVE_INFINITY
									|| total == Double.POSITIVE_INFINITY || total == Double.NaN) {
								total = 1000000000.0;
							}

							newNode = new ConstNode(total);

						} else if (inTree.getLabel().equals("Subtract")) {
							double total = -1;
							total = nodeValue1 - nodeValue2;

							newNode = new ConstNode(total);

						} else {
							System.out.println("label is undefined");
							return inTree;
						}
						pruneTree(inTree, depth - 1, newNode);
					} else {
						// One of the leaf is not numeric
						for (int i = 0; i < inTree.getNumChildren(); i++) {
							pruneTree(inTree.getChildren().get(i), depth + 1, null);
						}
					}

				}
			} else if (inTree.getNumChildren() == 1) {
				AbstractNode node1 = inTree.getChildren().get(0);
				String nodeType1 = node1.getType();

				if (nodeType1 == Const.INNER_NODE_TYPE) {
					for (int i = 0; i < inTree.getNumChildren(); i++) {
						pruneTree(inTree.getChildren().get(i), depth + 1, null);
					}

				} else {
					if (isValueNumeric(node1.getLabel()) == true) {
						double nodeValue1 = valueToNumeric(node1.getLabel());
						AbstractNode newNode = null;

						if (inTree.getLabel().equals("Cosine")) {

							double total = Math.cos(nodeValue1);
							// System.out.println( evalValue );
							if (total == Double.MAX_VALUE || total == Double.NEGATIVE_INFINITY
									|| total == Double.POSITIVE_INFINITY || total == Double.NaN) {
								total = 1000000000.0;
							}
							newNode = new ConstNode(total);

						} else if (inTree.getLabel().equals("Sine")) {
							double total = Math.sin(nodeValue1);
							// System.out.println( evalValue );
							if (total == Double.MAX_VALUE || total == Double.NEGATIVE_INFINITY
									|| total == Double.POSITIVE_INFINITY || total == Double.NaN) {
								total = 1000000000.0;
							}
							newNode = new ConstNode(total);

						} else if (inTree.getLabel().equals("Exponential")) {
							double total = Math.exp(nodeValue1);
							// System.out.println( evalValue );
							if (total == Double.MAX_VALUE || total == Double.NEGATIVE_INFINITY
									|| total == Double.POSITIVE_INFINITY || total == Double.NaN) {
								total = 1000000000.0;
							}
							newNode = new ConstNode(total);

						} else if (inTree.getLabel().equals("Log")) {
							double total = Math.log(nodeValue1);
							// System.out.println( evalValue );
							if (total == Double.MAX_VALUE || total == Double.NEGATIVE_INFINITY
									|| total == Double.POSITIVE_INFINITY || total == Double.NaN) {
								total = 1000000000.0;
							}
							newNode = new ConstNode(total);

						} else if (inTree.getLabel().equals("SquareRoot")) {
							double total = Math.sqrt(nodeValue1);
							// System.out.println( evalValue );
							if (total == Double.MAX_VALUE || total == Double.NEGATIVE_INFINITY
									|| total == Double.POSITIVE_INFINITY || total == Double.NaN) {
								total = 1000000000.0;
							}
							newNode = new ConstNode(total);
						}
						else if (inTree.getLabel().equals("Square")) {
							double total = nodeValue1 * nodeValue1 ;
							// System.out.println( evalValue );
							if (total == Double.MAX_VALUE || total == Double.NEGATIVE_INFINITY
									|| total == Double.POSITIVE_INFINITY || total == Double.NaN) {
								total = 1000000000.0;
							}
							newNode = new ConstNode(total);
						}
						else {
							System.out.println("label is undefined");
							return inTree;
						}
						pruneTree(inTree, depth + 1, newNode);
					} else {
						// leaf node is none- numeric
						for (int i = 0; i < inTree.getNumChildren(); i++) {
							pruneTree(inTree.getChildren().get(i), depth + 1, null);
						}
					}

				}

			} else if (inTree.numChildren == 0) {
				return pruneTree(inTree, depth + 1, null);
			}

		}
		return inTree.clone();
	}

	boolean isValueNumeric(String stringValue) {
		String items[] = stringValue.split(":");
		// System.out.println(stringValue);
		// System.out.println(" \n");
		String constValueStr;

		if (items.length >= 1 && items[0].contains("Const")) {
			// System.out.println(items[0]);
			// System.out.println(stringValue);
			constValueStr = items[1].trim();
		} else {
			return false;
		}
		try {
			double d = Double.valueOf(constValueStr);
			/*
			 * if (d==(int)d){ System.out.println("integer"+(int)d); }else{
			 * System.out.println("double"+d); }
			 */
			return true;
		} catch (Exception e) {
			System.out.println("not number");
			return false;
		}
	}

	double valueToNumeric(String stringValue) {
		String items[] = stringValue.split(":");
		double d = -1.0;
		String constValueStr;
		if (items.length >= 1) {
			// System.out.println(stringValue);
			constValueStr = items[1].trim();
		} else {
			return -1.0;
		}
		try {
			d = Double.valueOf(constValueStr);
			/*
			 * if (d==(int)d){ System.out.println("integer"+(int)d); }else{
			 * System.out.println("double"+d); }
			 */

		} catch (Exception e) {
			System.out.println("not number");
			return d;
		}
		return d;
	}

}
