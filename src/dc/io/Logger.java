package dc.io;

import java.io.File;
import java.util.ArrayList;

import files.FWriter;

/** Logger Class **/
public class Logger {
	
	FWriter writer;
	String folder;
	static public String publicFolder;
	
	/** Constructor. Creating files inside a folder related to the training and testing index of the data **/
	public Logger(String fileName, String trainingIndex, String testingIndex){
	
		folder = fileName + "/i" + trainingIndex + "_" + testingIndex + "/";
		File f = new File(folder);
		f.mkdirs();
		publicFolder = folder;
		writer = new FWriter(folder + "Curves.txt");
		writer = new FWriter(folder + "Results.txt");
		writer = new FWriter(folder + "Logger.txt");
		writer = new FWriter(folder + "Solutions.txt");
		writer = new FWriter(folder + "EquityCurve.txt");
		writer = new FWriter(folder + "Fitness.txt");
		writer = new FWriter(folder + "GPExpressionDistribution.txt");
	}
	
	/** Empty constructor */
	public Logger(){		
		writer = new FWriter(folder + "Curves.txt");
		writer = new FWriter(folder + "Results.txt");
		writer = new FWriter(folder + "Logger.txt");
		writer = new FWriter(folder + "Solutions.txt");
		writer = new FWriter(folder + "EquityCurve.txt");
		writer = new FWriter(folder + "Fitness.txt");
		writer = new FWriter(folder + "GPExpressionDistribution.txt");
	}
	
	/** Saving a string element */
	public void save(String fileName, String toSave){
		writer.openToAppend(new File(folder + fileName));
		writer.write(toSave);
		writer.closeFile();
	}
	
	public void delete(String fileName){
		File file = new File(folder + fileName);
		if(file.delete())
		{
			System.out.println("File deleted successfully");
		}
		else
		{
			System.out.println("Failed to delete the file");
		}
	}
	/** Saving ArrayList elements */
	public void save(String fileName, ArrayList<String> toSave){
		writer.openToAppend(new File(folder + fileName));
		for (String s:toSave)
			writer.write(s);
		writer.closeFile();
	}
}