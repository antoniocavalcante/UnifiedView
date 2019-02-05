package ca.ualberta.cs.experimentSemiSupClassification;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import ca.ualberta.cs.distance.CosineSimilarity;
import ca.ualberta.cs.distance.DistanceCalculator;
import ca.ualberta.cs.distance.EuclideanDistance;
import ca.ualberta.cs.distance.ManhattanDistance;
import ca.ualberta.cs.distance.PearsonCorrelation;
import ca.ualberta.cs.distance.TanimotoSimilarity;
import ca.ualberta.cs.ssClassification.ExpRMGT;

public class RuntimeMainHDBSCAN implements Serializable
{

	private static final long serialVersionUID = 1L;

	//	private static final String MESSAGE_ERROR= "How to call the experiment code: \n Java -jar Experiments.jar [file] [folder with data] [output folder] [distance function] nmc1 nmc2 nmcN (number of missed classes)";
	private static final String MESSAGE_ERROR= "How to call the experiment code: \n Java -jar [FileExperiments].jar [folder with data] [output folder] listOfFilesAndDistances";

	public static void main(String [] args) throws Exception
	{

		if(args.length < 3)
		{
			System.err.println("Error in the input parameters.");
			System.err.println(MESSAGE_ERROR);
			System.exit(-1);
		}

		String dataFolder   = args[0];
		String outputFolder = args[1];

		int numberTrials	= 20;
		String delimiter	= ",";

		//Parameters for the classification approach
		double rho			= 50.0;
		double expoent		= 5.0;

		LabelSelection methodLabelSelection = new SelectRandomObjectsGivenPercentage();
		String listOfFiles = args[2];
	
		ArrayList<Integer> arrayMinPts   = new ArrayList<Integer>();
		arrayMinPts.add(15);

		BufferedReader reader = new BufferedReader(new FileReader(listOfFiles));
		String line = reader.readLine();

		while (line != null) 
		{
			line.replaceAll("\\s+",""); // Remove spaces that might exist in the line
			String[] lineContents = line.split(delimiter);

			String dataFile      = lineContents[0];
			String inputName     = dataFile.substring(0, dataFile.lastIndexOf("."));
			String distanceParam = lineContents[1];

			DistanceCalculator distance = null;

			if(distanceParam.equals("euclidean"))
				distance	= new EuclideanDistance();

			else if (distanceParam.equals("cosine"))
				distance	= new CosineSimilarity();

			else if (distanceParam.equals("tanimoto"))
				distance	= new TanimotoSimilarity();

			else if (distanceParam.equals("pearson"))
				distance	= new PearsonCorrelation();

			else if (distanceParam.equals("manhattan"))
				distance	= new ManhattanDistance();
			else
			{
				System.err.println(MESSAGE_ERROR);
				System.exit(-1);
			}
			
			System.out.println("Dataset: " + inputName);
			RuntimeCreateScenarios.generateInformation(inputName, dataFolder, outputFolder, methodLabelSelection, distance, numberTrials, arrayMinPts, delimiter);

//			RuntimeMstAllPoints.performIt(inputName, dataFolder, outputFolder, distance, rho, expoent, delimiter);
//
//			RuntimeMstCoreDistance.performIt(inputName, dataFolder, outputFolder, distance, rho, expoent, delimiter);
//			
//			RuntimeHISSCLU.performIt(inputName, dataFolder, outputFolder, distance, rho, expoent, delimiter);
			
			
			
			// Cubic algorithms
			RuntimeGFHF.performIt(inputName, dataFolder, outputFolder, distance, delimiter);
			RuntimeRMGT.performIt(inputName, dataFolder, outputFolder, distance, delimiter);

			// Read the next trial of the algorithm
			line = reader.readLine();
		}// End for while
		
		reader.close();
	}
}