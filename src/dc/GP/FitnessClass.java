package dc.GP;

public class FitnessClass {

	
	private double trainingFitness;
	private double testFitness;
	
	public FitnessClass(double training, double test) {
		trainingFitness = training;
		testFitness = test;
	}
	
	public double getTrainingFitness() {
		return trainingFitness;
	}
	
	public void setTrainingFitness(double trainingFitness) {
		this.trainingFitness = trainingFitness;
	}
	
	public double getTestFitness() {
		return testFitness;
	}
	
	public void setTestFitness(double testFitness) {
		this.testFitness = testFitness;
	}

}
