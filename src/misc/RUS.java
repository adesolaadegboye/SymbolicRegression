package misc;

/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * RUS.java
 * 
 * Copyright (C) 2014 Sergio Garcia, Daniel Rodriguez
 */



import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.filters.Filter;
import weka.filters.SupervisedFilter;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

/**
 * <!-- globalinfo-start --> A filter that eliminates some random majority
 * instances until the total amount of majority instances reaches the percentage
 * given.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -P &lt;Percentage&gt;
 *  Specifies the proportion of final majority class respecting the minority class. (default 75).
 *  If the percentage specified is higher than the current majority percentage, the filter does nothing,
 *  else, the filter eliminates majority classes randomly until the majority proportion reaches the specified percentage.
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author Sergio Garcia Charameli
 * @author Daniel Rodriguez
 * @version $Revision: 1 $
 */

public class RUS extends Filter implements SupervisedFilter, OptionHandler,
		TechnicalInformationHandler {

	/** for serialization. */
	private static final long serialVersionUID = -1440151492573627546L;

	/** the percentage of majority class. */
	protected double m_Percentage = 75.0;

	/**
	 * Returns a string describing this filter.
	 * 
	 * @return a description of the filter suitable for displaying in the
	 *         explorer/experimenter gui
	 */
	public String globalInfo() {
		return "A filter that eliminates random majority instances until the total amount of majority instances reaches a speficied percentage."
				+ "For more information, see\n\n"
				+ getTechnicalInformation().toString();
	}

	/**
	 * Returns an instance of a TechnicalInformation object, containing detailed
	 * information about the technical background of this class, e.g., paper
	 * reference or book this class is based on.
	 * 
	 * @return the technical information about this class
	 */
	public TechnicalInformation getTechnicalInformation() {
		TechnicalInformation result = new TechnicalInformation(Type.ARTICLE);

		result.setValue(Field.AUTHOR,
				"G.E.A.P.A. Batista, R.C. Prati, M.C. Monard");
		result.setValue(
				Field.TITLE,
				"A study of the behavior of several methods for balancing machine learning training data");
		result.setValue(Field.JOURNAL, "SIGKDD Explorations");
		result.setValue(Field.YEAR, "2004");
		result.setValue(Field.VOLUME, "6");
		result.setValue(Field.PAGES, "Issue 1, 20-29");

		return result;
	}

	/**
	 * Returns the revision string.
	 * 
	 * @return the revision
	 */
	public String getRevision() {
		return RevisionUtils.extract("$Revision: 1 $");
	}

	/**
	 * Returns the Capabilities of this filter.
	 * 
	 * @return the capabilities of this object
	 * @see Capabilities
	 */
	@Override
	public Capabilities getCapabilities() {
		Capabilities result = super.getCapabilities();

		result.disableAll();

		// attributes
		result.enableAllAttributes();

		// class
		result.enable(Capability.BINARY_CLASS);

		return result;
	}

	/**
	 * Returns an enumeration describing the available options.
	 * 
	 * @return an enumeration of all the available options.
	 */
	public Enumeration listOptions() {
		Vector newVector = new Vector();

		newVector.addElement(new Option(
				"\tSpecifies percentage of the proportion of majority class.\n"
						+ "\t(default 75.0)\n", "P", 1, "-P <percentage>"));

		return newVector.elements();
	}

	/**
	 * Parses a given list of options.
	 * 
	 * <!-- options-start --> Valid options are:
	 * <p/>
	 * 
	 * <pre>
	 * -P &lt;Percentage&gt;
	 *  Specifies the proportion of final majority class respecting the minority class. (default 75).
	 *  If the percentage specified is higher than the current majority percentage, the filter does nothing,
	 *  else, the filter eliminates majority classes randomly until the majority proportion reaches the specified percentage.
	 * </pre>
	 * 
	 * <!-- options-end -->
	 * 
	 * @param options
	 *            the list of options as an array of strings
	 * @throws Exception
	 *             if an option is not supported
	 */
	public void setOptions(String[] options) throws Exception {

		String percentageStr = Utils.getOption('P', options);
		if (percentageStr.length() != 0) {
			setPercentage(new Double(percentageStr).doubleValue());
		} else {
			setPercentage(75.0);
		}

	}

	/**
	 * Gets the current settings of the filter.
	 * 
	 * @return an array of strings suitable for passing to setOptions
	 */
	public String[] getOptions() {
		Vector<String> result;

		result = new Vector<String>();

		result.add("-P");
		result.add("" + getPercentage());

		return result.toArray(new String[result.size()]);
	}

	/**
	 * Returns the tip text for this property.
	 * 
	 * @return tip text for this property suitable for displaying in the
	 *         explorer/experimenter gui
	 */
	public String percentageTipText() {
		return "Specifies the proportion of majority class desired.";
	}

	/**
	 * Sets the percentage of the proportion of majority class.
	 * 
	 * @param value
	 *            the percentage to use
	 */
	public void setPercentage(double value) {
		if (value < 1 || value > 99) {
			throw new IllegalArgumentException(
					"Percentage must be a value between 1 and 99.");
		}
		this.m_Percentage = value;
	}

	/**
	 * Gets the percentage of the proportion of majority class.
	 * 
	 * @return the percentage of the proportion of majority class.
	 */
	public double getPercentage() {
		return m_Percentage;
	}

	/**
	 * Sets the format of the input instances.
	 * 
	 * @param instanceInfo
	 *            an Instances object containing the input instance structure
	 *            (any instances contained in the object are ignored - only the
	 *            structure is required).
	 * @return true if the outputFormat may be collected immediately
	 * @throws Exception
	 *             if the input format can't be set successfully
	 */
	public boolean setInputFormat(Instances instanceInfo) throws Exception {
		super.setInputFormat(instanceInfo);
		super.setOutputFormat(instanceInfo);
		return true;
	}

	/**
	 * Input an instance for filtering. Filter requires all training instances
	 * be read before producing output.
	 * 
	 * @param instance
	 *            the input instance
	 * @return true if the filtered instance may now be collected with output().
	 * @throws IllegalStateException
	 *             if no input structure has been defined
	 */
	public boolean input(Instance instance) {
		if (getInputFormat() == null) {
			throw new IllegalStateException("No input instance format defined");
		}
		if (m_NewBatch) {
			resetQueue();
			m_NewBatch = false;
		}
		if (m_FirstBatchDone) {
			push(instance);
			return true;
		} else {
			bufferInput(instance);
			return false;
		}
	}

	/**
	 * Signify that this batch of input to the filter is finished. If the filter
	 * requires all instances prior to filtering, output() may now be called to
	 * retrieve the filtered instances.
	 * 
	 * @return true if there are instances pending output
	 * @throws IllegalStateException
	 *             if no input structure has been defined
	 * @throws Exception
	 *             if provided options cannot be executed on input instances
	 */
	public boolean batchFinished() throws Exception {
		if (getInputFormat() == null) {
			throw new IllegalStateException("No input instance format defined");
		}

		if (!m_FirstBatchDone) {
			// Do RUS, and clear the input instances.
			doRUS();
		}
		flushInput();

		m_NewBatch = true;
		m_FirstBatchDone = true;
		return (numPendingOutput() != 0);
	}

	/**
	 * The procedure implementing the RUS algorithm. The output instances are
	 * pushed onto the output queue for collection.
	 * 
	 * @throws Exception
	 *             if provided options cannot be executed on input instances
	 */
	protected void doRUS() throws Exception {

		Instances instances = new Instances(getInputFormat(), 0,
				getInputFormat().size());

		int minIndex = 0;
		int maxIndex = 0;
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;

		// Find minority and majority class
		int[] classCounts = getInputFormat().attributeStats(
				getInputFormat().classIndex()).nominalCounts;
		for (int i = 0; i < classCounts.length; i++) {
			if (classCounts[i] != 0 && classCounts[i] < min) {
				min = classCounts[i];
				minIndex = i;
			}
			if (classCounts[i] != 0 && classCounts[i] > max) {
				max = classCounts[i];
				maxIndex = i;
			}
		}

		// Create a temporary majority list
		ArrayList<Integer> majority = new ArrayList<Integer>();

		// Add majority instantes to the majority list
		for (int i = 0; i < instances.size(); i++) {
			if ((int) instances.get(i).classValue() == maxIndex) {
				majority.add(i);
			}
		}

		// Calculate the amount of majority instances to delete
		int finalMajorityInstances = (int) (m_Percentage
				* classCounts[minIndex] / (100 - m_Percentage));
		int majorityInstancesToDelete = classCounts[maxIndex]
				- finalMajorityInstances;

		// Delete majority instances randomly
		Random r = new Random();
		for (int i = 0; i < majorityInstancesToDelete; i++) {
			majority.remove(r.nextInt(majority.size()));
		}

		// All initial instances are pushed only if they are contained in the
		// majority temp list or if they are minority instances
		for (int i = 0; i < instances.size(); i++) {
			if (((int) instances.get(i).classValue() == minIndex)
					|| ((int) instances.get(i).classValue() == maxIndex && majority
							.contains(i))) {
				push((Instance) instances.get(i).copy());
			}
		}
	}

	/**
	 * Main method for running this filter.
	 * 
	 * @param args
	 *            should contain arguments to the filter: use -h for help
	 */
	public static void main(String[] args) {
		runFilter(new RUS(), args);
	}
}