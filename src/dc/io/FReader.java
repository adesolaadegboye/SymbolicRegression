package dc.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.joda.time.*;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import files.FWriter;


public class FReader {
	
	public static String globalfileName = null;

	public FReader(){

	}
	
	
	public class FileMember
	{
	    public String Day; 
	    public String time;  
	    public String    bid;
	    public String    ask;
	    public String    closeBid;
	    public String    closeAsk;
	 };
	 
	 public class FileMember2
	{
	    public String Day; 
	    public String time;  
	    public String price;
	    public String bidPrice;
	    public String askPrice;
	    
	    @Override
	    public boolean equals(Object other) {
	        if (!(other instanceof FileMember2)) {
	            return false;
	        }
	        
	        if (!FileMember2.class.isAssignableFrom(other.getClass())) {
	            return false;
	        }


	        final FileMember2 that = (FileMember2) other;

	        // Custom equality check here.
	      String thisDay =  this.Day.trim();
	      String thistime =  this.time.trim();
	      String thisprice =  this.price.trim();
	      String thisBidPrice =  this.bidPrice.trim(); 
	      String thisAskPrice =  this.askPrice.trim(); 
	      
	       
	       String thatDay =  that.Day.trim();
	       String thattime =  that.time.trim();
	       String thatprice =  that.price.trim();
	       String thatBidPrice =  that.bidPrice.trim(); 
	       String thatAskPrice =  that.askPrice.trim(); 
	       
	       String thistickString = thisDay.trim()+","+thistime.trim()+","+thisprice.trim()+","+thisAskPrice+","+thisBidPrice;
	       String thattickString = thatDay.trim()+","+thattime.trim()+","+thatprice.trim()+","+thatAskPrice+","+thatBidPrice;
	       
	       if(thistickString.compareToIgnoreCase(thattickString) == 0){
               return true;
            }
	       else
	    	   return false;
	    /*    if (this.Day.compareToIgnoreCase(that.Day) == 0 && 
	        		this.Day.compareToIgnoreCase(that.time) == 0 && 
	        		this.price.compareToIgnoreCase(that.price) == 0)
	        	return true;
	        else
	        {
	        	return false;
	        }	*/	
	         
	    }
	    
	    @Override
	    public int hashCode() {
	        int hashCode = 1;

	        hashCode = hashCode * 37 + this.Day.hashCode();
	        hashCode = hashCode * 37 + this.time.hashCode();
	        hashCode = hashCode * 37 + this.price.hashCode();
	        hashCode = hashCode * 37 + this.askPrice.hashCode();
	        hashCode = hashCode * 37 + this.bidPrice.hashCode();

	        return hashCode;
	    }
	 };
	 
	 
	 public class FileMemberCompare implements Comparator<FileMember2>{
		 
		    @Override
		    public int compare(FileMember2 e1, FileMember2 e2) {
		    	Double dObj1 = new Double(e1.price);
		    	Double dObj2 = new Double(e2.price);
		    	
		    	return  dObj1.compareTo(dObj2);
		    }
	};
		
	static private	 FReader freader = new FReader();
	static public ArrayList<FileMember2> dataRecordInFileArray = new ArrayList<FileMember2>();
	static public ArrayList<FileMember2> tickDataArray = new ArrayList<FileMember2>();
	static public ArrayList<String> tickDataArrayString = new ArrayList<String>(1000000);

	/**
	 * Reads, filters (into a specified interval) and loads the data from the text file
	 * @param filename The name of the file to be read
	 * 
	 * @return days The data in an ArrayList<Double[]> format
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ParseException 
	 */
	public static String saveDataByInterval(String filename, int interval)
			throws FileNotFoundException, IOException, ParseException {


		if(!filename.toLowerCase().contains("hqtick".toLowerCase()))
		{
			
			System.out.println("Missing substring");
			return null;
		}
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		String newFileName = filename;
		String line = null;

		newFileName = newFileName.replaceAll("hqtick", "10min");
		ArrayList<Double[]> days = new ArrayList<Double[]>();
		ArrayList<Double> values = new ArrayList<Double>();
		ArrayList<String> dataAtInterval = new ArrayList<String>();
		
		FWriter writer = new FWriter(newFileName);
		writer.openToAppend(new File(newFileName));
		
		String timestamp = null;
		SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS");
		
		String[] token = reader.readLine().split("\\s");
		Date d1 = format.parse(token[0].trim() + " " + token[1].trim());
		Date d2 = null;
		
		reader.close();
		reader = new BufferedReader(new FileReader(filename));

		while ((line = reader.readLine()) != null)
		{
			String[] tokens = line.split("\\s");

			if (timestamp == null)
			{
				timestamp = tokens[0].trim();
				
				writer.write(tokens[0].trim()+","+tokens[1].trim()+","+tokens[2].trim());
			}
		/*	else if (!tokens[0].trim().equals(timestamp))
			{
				days.add(values.toArray(new Double[0]));
				values.clear();

				timestamp = tokens[0].trim();
				writer.closeFile();
				break;
			}
			*/
			d2 = format.parse(tokens[0].trim() + " " + tokens[1].trim());
			long diff = d2.getTime() - d1.getTime();
			long diffMinutes = diff / (60 * 1000);
			
			System.out.println(diffMinutes);
			
			if (diffMinutes >= interval){
				values.add(Double.parseDouble(tokens[2].trim()));
				dataAtInterval.add(tokens[0].trim()+","+tokens[1].trim()+","+tokens[2].trim());
				writer.write(tokens[0].trim()+","+tokens[1].trim()+","+tokens[2].trim());
				//writer.write("\n");
				d1 = format.parse(tokens[0].trim() + " " + tokens[1].trim());
			}

		}

		/*if (!values.isEmpty())
		{
			days.add(values.toArray(new Double[0]));
		}*/

		reader.close();
		writer.closeFile();

		return newFileName;
	}

	
	
	/**
	 * Reads, filters (into a specified interval) and loads the data from the text file
	 * @param filename The name of the file to be read
	 * 
	 * @return days The data in an ArrayList<Double[]> format
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ParseException 
	 */
	public static ArrayList<Double[]> loadData(String filename, int interval)
			throws FileNotFoundException, IOException, ParseException {

		BufferedReader reader = new BufferedReader(new FileReader(filename));

		String line = null;

		ArrayList<Double[]> days = new ArrayList<Double[]>();
		ArrayList<Double> values = new ArrayList<Double>();
		String timestamp = null;
		SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS");
		
		String[] token = reader.readLine().split("\\s");
		Date d1 = format.parse(token[0].trim() + " " + token[1].trim());
		Date d2 = null;

		while ((line = reader.readLine()) != null)
		{
			String[] tokens = line.split("\\s");

			if (timestamp == null)
			{
				timestamp = tokens[0].trim();
			}
			else if (!tokens[0].trim().equals(timestamp))
			{
				days.add(values.toArray(new Double[0]));
				values.clear();

				timestamp = tokens[0].trim();
			}
			
			d2 = format.parse(tokens[0].trim() + " " + tokens[1].trim());
			long diff = d2.getTime() - d1.getTime();
			long diffMinutes = diff / (60 * 1000);
			
			if (diffMinutes >= interval){
				values.add(Double.parseDouble(tokens[2].trim()));
				d1 = format.parse(tokens[0].trim() + " " + tokens[1].trim());
			}

		}

		if (!values.isEmpty())
		{
			days.add(values.toArray(new Double[0]));
		}

		reader.close();

		return days;
	}

	/**
	 * Reads and loads the data from the text file
	 * @param filename The name of the file to be read
	 * 
	 * @return days The data in an ArrayList<Double[]> format
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static ArrayList<Double[]> loadDataHasOnePrice(String filename)
			throws FileNotFoundException, IOException {

		BufferedReader reader = new BufferedReader(new FileReader(filename));

		String line = null;

		ArrayList<Double[]> days = new ArrayList<Double[]>();
		ArrayList<Double> values = new ArrayList<Double>();
		String timestamp = null;

		int counter = 0;
		while ((line = reader.readLine()) != null)
		{
			//System.out.println("Line" + line);
			String[] tokens = line.split(",");
			if (tokens.length != 3)
			{
				System.out.println("Invalid price file. Must have date time and price only. Length is" + tokens.length);
				reader.close();
				return null;
			}
			String value = "";

			if (timestamp == null)
			{
				timestamp = tokens[0].trim();
			}
			else if (!tokens[0].trim().equals(timestamp))
			{
				
				days.add(values.toArray(new Double[0]));
			//	System.out.println("Counter" + counter);
				counter++;
				values.clear();

				timestamp = tokens[0].trim();
			}

			//if it does not contain the 10min string, then the file is tick data; if it contains the 10min string, then we go to the else-statement, as the file is 10-min data
			if (!filename.contains("10min")){
				value = tokens[2].trim();
				values.add(Double.parseDouble(tokens[2].trim()));
			}
			else{
				
					double price = Double.parseDouble(tokens[2].trim());//unused
					
					DecimalFormat df = new DecimalFormat("#.########");
					value = Double.toString(Double.parseDouble(df.format(price)));
					values.add(Double.parseDouble(df.format(price)));
			}
			
			FileMember2 dataRecordInFile = freader.new FileMember2();
			dataRecordInFile.Day = tokens[0].trim();
			dataRecordInFile.time = tokens[1].trim();
			dataRecordInFile.price= value;
			dataRecordInFileArray.add(dataRecordInFile);
			
		}

		if (!values.isEmpty())
		{
			days.add(values.toArray(new Double[0]));
		}

		/*
        data = new double[values.size()];

        for (int i = 0; i < data.length; i++)
        {
            data[i] = values.get(i);
        }
		 */

		reader.close();

		return days;
	}
	

	/**
	 * Reads and loads the data from the text file
	 * @param filename The name of the file to be read
	 * 
	 * @return days The data in an ArrayList<Double[]> format
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void loadDataTickData(String filename)
			throws FileNotFoundException, IOException {
		
		globalfileName = filename;
		if (!tickDataArray.isEmpty())
		{
			tickDataArray.clear();
			tickDataArray = new ArrayList<FileMember2>();
		}

		BufferedReader reader = new BufferedReader(new FileReader(filename));

		String line = null;

		while ((line = reader.readLine()) != null)
		{
			//System.out.println("Line" + line);
			String[] tokens = line.split("\t");
			if (tokens.length != 3)
			{
				System.out.println("Invalid price file. Must have date time and price only. Length is" + tokens.length);
				reader.close();
				return ;
			}

			FileMember2 dataRecordInFile = freader.new FileMember2();
			dataRecordInFile.Day = tokens[0].trim();
			dataRecordInFile.time = tokens[1].trim();
			dataRecordInFile.price= String.valueOf(Double.parseDouble(tokens[2].trim()));
			tickDataArray.add(dataRecordInFile);
			//tickDataArrayString.add(dataRecordInFile.Day+","+dataRecordInFile.time+","+dataRecordInFile.price);
			
		}
		reader.close();
		//loadTickdataAsString();
	}

	/*public static void loadTickdataAsString()
	{
		for (FileMember2 dataRecordInFile : FReader.tickDataArray) {
			
			
			tickDataArrayString.add(dataRecordInFile.Day+","+dataRecordInFile.time+","+dataRecordInFile.price);
		}
	}*/
	
	
	/**
	 * Reads and loads the data from the text file
	 * @param filename The name of the file to be read
	 * 
	 * @return days The data in an ArrayList<Double[]> format
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static ArrayList<Double[]> loadData(String filename, boolean isOpenClose)
			throws FileNotFoundException, IOException {

		BufferedReader reader = new BufferedReader(new FileReader(filename));

		String line = null;

		ArrayList<Double[]> days = new ArrayList<Double[]>();
		ArrayList<Double> values = new ArrayList<Double>();
		String timestamp = null;

		int counter = 0;
		while ((line = reader.readLine()) != null)
		{
			//System.out.println("Line" + line);
			FileMember2 dataRecordInFile = freader.new FileMember2();
			String[] tokens = line.split("\\s");
			String value = "";

			if (timestamp == null)
			{
				timestamp = tokens[0].trim();
			}
			else if (!tokens[0].trim().equals(timestamp))
			{
				
				days.add(values.toArray(new Double[0]));
			//	System.out.println("Counter" + counter);
				counter++;
				values.clear();

				timestamp = tokens[0].trim();
			}

			//if it does not contain the 10min string, then the file is tick data; if it contains the 10min string, then we go to the else-statement, as the file is 10-min data
			if (!filename.contains("10min") && !filename.contains("1min")){
				value = tokens[2].trim();
				values.add(Double.parseDouble(tokens[2].trim()));
			}
			else{
				if (!isOpenClose)
				{
					double bid = Double.parseDouble(tokens[2].trim());//unused
					double ask = Double.parseDouble(tokens[3].trim());//unused
					double midOpen = (bid + ask) / 2;//unused
					dataRecordInFile.bidPrice =  tokens[2].trim();
					dataRecordInFile.askPrice =  tokens[3].trim();  
					DecimalFormat df = new DecimalFormat("#.########");
					value = Double.toString(Double.parseDouble(df.format(midOpen)));
					values.add(Double.parseDouble(df.format(midOpen)));
				}
				else
				{
					double openBid = Double.parseDouble(tokens[2].trim());//unused
					double openAsk = Double.parseDouble(tokens[3].trim());//unused
					double closeBid = Double.parseDouble(tokens[4].trim());
					double closeAsk = Double.parseDouble(tokens[5].trim());
					double midOpen = (openBid + openAsk) / 2;//unused
					double midClose = (closeBid + closeAsk) / 2;
					DecimalFormat df = new DecimalFormat("#.########");
					value = Double.toString(Double.parseDouble(df.format(midClose)));
					dataRecordInFile.bidPrice =  tokens[4].trim();
					dataRecordInFile.askPrice =  tokens[5].trim();
					//we are trading using the closing midPrice at the end of the 10-min interval.
					values.add(Double.parseDouble(df.format(midClose)));
				}
			}
			
			
			dataRecordInFile.Day = tokens[0].trim();
			dataRecordInFile.time = tokens[1].trim();
			dataRecordInFile.price= value;
			dataRecordInFileArray.add(dataRecordInFile);
			
		}

		if (!values.isEmpty())
		{
			days.add(values.toArray(new Double[0]));
		}

		/*
        data = new double[values.size()];

        for (int i = 0; i < data.length; i++)
        {
            data[i] = values.get(i);
        }
		 */

		reader.close();

		return days;
	}
	
	
	/**
	 * Adesola Version
	 * 
	 * 
	 * 
	 * ***/
	
	public static ArrayList<Double[]> loadData(String filename)
			throws FileNotFoundException, IOException {

		BufferedReader reader = new BufferedReader(new FileReader(filename));

		String line = null;

		ArrayList<Double[]> days = new ArrayList<Double[]>();
		ArrayList<Double> values = new ArrayList<Double>();
		String timestamp = null;

		while ((line = reader.readLine()) != null)
		{
			String[] tokens = line.split("\\s");

			if (timestamp == null)
			{
				timestamp = tokens[0].trim();
			}
			else if (!tokens[0].trim().equals(timestamp))
			{
				days.add(values.toArray(new Double[0]));
				values.clear();

				timestamp = tokens[0].trim();
			}

			//if it does not contain the 10min string, then the file is tick data; if it contains the 10min string, then we go to the else-statement, as the file is 10-min data
			if (!filename.contains("10min"))
				values.add(Double.parseDouble(tokens[2].trim()));
			else{
				double openBid = Double.parseDouble(tokens[2].trim());//unused
				double openAsk = Double.parseDouble(tokens[3].trim());//unused
				double closeBid = Double.parseDouble(tokens[4].trim());
				double closeAsk = Double.parseDouble(tokens[5].trim());
				double midOpen = (openBid + openAsk) / 2;//unused
				double midClose = (closeBid + closeAsk) / 2;
				DecimalFormat df = new DecimalFormat("#.########");

				//we are trading using the closing midPrice at the end of the 10-min interval.
				values.add(Double.parseDouble(df.format(midClose)));
			}
		}

		if (!values.isEmpty())
		{
			days.add(values.toArray(new Double[0]));
		}

		/*
        data = new double[values.size()];

        for (int i = 0; i < data.length; i++)
        {
            data[i] = values.get(i);
        }
		 */

		reader.close();

		return days;
	}


	public static ConcurrentHashMap<Integer,String>  loadDataMap(String filename)
			throws FileNotFoundException, IOException {

		BufferedReader reader = new BufferedReader(new FileReader(filename));

		String line = null;

		ConcurrentHashMap<Integer,String>  daysMap =  new ConcurrentHashMap<Integer,String>();
		int count = 0;
		String timestamp = null;

		while ((line = reader.readLine()) != null)
		{
			String[] tokens = line.split("\\s");

			if (timestamp == null)
			{
				timestamp = tokens[0].trim();
				daysMap.put(count, tokens[0]);
				count = count + 1;
			}
			else if (!tokens[0].trim().equals(timestamp))
			{
				timestamp = tokens[0].trim();
				daysMap.put(count, tokens[0]);
				count = count + 1;	
			}
		}

		reader.close();

		return daysMap;
	}

	/**
	 * Reads and loads the data from the text file
	 * @param filename The name of the file to be read
	 * 
	 * @return days The data in an ArrayList<Double[]> format
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static HashMap<String, ArrayList<FileMember>> loadDataObjectByDay(String filename, boolean hasOpenClose)
			throws FileNotFoundException, IOException {

		BufferedReader reader = new BufferedReader(new FileReader(filename));
		BufferedReader reader2 = new BufferedReader(new FileReader(filename));
		HashMap<String, ArrayList<FileMember>> fileRecords = new HashMap<String, ArrayList<FileMember>>();

		String line = null;

		ArrayList<Double[]> days = new ArrayList<Double[]>();
		ArrayList<Double> values = new ArrayList<Double>();
		ArrayList<FileMember> rowInFile =  new ArrayList<FileMember>();
		String timestamp = null;
		FReader  fReader = new FReader();
		DateTime dateTime1 = new  DateTime(); 
		DateTime dateTime2 =  new DateTime();
		DateTimeFormatter formatter = DateTimeFormat.forPattern("dd.MM.yyyy");
		
		int lines =0;
		while ((line = reader2.readLine()) != null)lines++;
		reader2.close();
		
		int counter = 0;
		while ((line = reader.readLine()) != null)
		{
			counter++;
			//String[] tokens = line.split("[,\\s\\-:\\?]");
			String[] tokens = line.split("[,]");
			FReader.FileMember fileMember =  fReader.new FileMember();
			
			fileMember.Day = tokens[0];
			fileMember.time = tokens[1];
			fileMember.bid = tokens[2];
			fileMember.ask = tokens[3];
			if (hasOpenClose)
			{
				fileMember.closeBid = tokens[4];
				fileMember.closeAsk = tokens[5];
			}	
			if (timestamp == null)
			{	
				rowInFile.add(fileMember);
				timestamp = tokens[0].trim();
				dateTime1 = formatter.parseDateTime(tokens[0]);
				dateTime2 = formatter.parseDateTime(tokens[0]);
			}
			else  //if (!tokens[0].trim().equals(timestamp))
			{
				dateTime2 = formatter.parseDateTime(tokens[0]);
															 
				if (dateTime1.getMonthOfYear() == dateTime2.getMonthOfYear() && dateTime1.getYear() == dateTime2.getYear())
				{
					rowInFile.add(fileMember);
				}
				else
				{	
					ArrayList<FileMember> rowInFile2  = (ArrayList<FReader.FileMember>)rowInFile.clone();
					if (counter == lines)
					{
						rowInFile2.add(fileMember);
					}
					fileRecords.put(dateTime1.toString("dd.MM.yyyy"), rowInFile2);
					
	
					rowInFile.clear();
					rowInFile.add(fileMember);
					dateTime1 = formatter.parseDateTime(tokens[0]);
				}
				timestamp = tokens[0].trim();
			}	
		}
		reader.close();

		return fileRecords;
	}

}

