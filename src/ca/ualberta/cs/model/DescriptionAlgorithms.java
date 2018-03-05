package ca.ualberta.cs.model;

public class DescriptionAlgorithms 
{
	public String  rmgtMKNN			   		   = "RMGT";
	public String  harMKNN					   = "GFHF";
	public String  lapSVM					   = "LapSVM";
	public String  hisscluClassBased   		   = "HISSCLU";
	
	public String  hdbscanCd	    = "HDBSCAN*(cd,-)";
	public String  hdbscanAp		= "HDBSCAN*(ap,-)";
	public String  hdbscanCdWMST    = "HDBSCAN*(cd,wMST)";
	public String  hdbscanApWMST    = "HDBSCAN*(ap,wMST)";
	public String  hdbscanCdWPWD    = "HDBSCAN*(cd,wPWD)"; // New definition for HISSCLU
	public String  hdbscanApWPWD    = "HDBSCAN*(ap,wPWD)";

	// Validation index
	public String  FMClassification  = "F-Measure";

	// Time registers
	public String  wholeProcess 	 = "wholeProcess"; // Take the whole time to run the algorithm.
	public String  graphConstruction = "graphConstruction"; // Take into account the time to construct the graphs
	public String  timeToPropagate   = "timeToPropagate"; // Take into account just the time to propagate the labels
	public String  weightFunction    = "weightFunction"; // Take into account the time to apply the weighting funciton
}