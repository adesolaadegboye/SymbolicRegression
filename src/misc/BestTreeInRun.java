package misc;

import dc.EventWriter;
import dc.GP.TreeHelperClass;
import dc.io.Logger;

public class BestTreeInRun {

	protected static Logger log;
	public BestTreeInRun() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) {
		if (args.length < 14) {
		System.out.println("usage: " + EventWriter.class.getName()
				+ " <file path:file name:training index start:training index end:test index start:test index end> + "
				+ "<popSize> <maxGens> <tournamentSize> <xoverProb> <mutProb> <thresholdIncrement> <noOfThresholds> <maxQuantity> + "
				+ "<budget> <shortSellingAllowance> <mddWeight> <xoverOperatorIndex> <mutOperatorIndex> <initialThreshold> [seed]");
		System.exit(1);
	}

	// Split the long parameter file , according to the delimiter
	String s[] = args[0].split(":");
	if (s.length < 6) {
		System.out.println(
				"Expect 6 parameters: <file path:file name:training index start:training index end:test index start:test index end>");
		System.exit(1);
	}
	
			log = new Logger(s[1], s[3], s[4]);
			 TreeHelperClass treeHelperClass = new TreeHelperClass();
			 treeHelperClass.readFitnessFile(2);

	}

}
