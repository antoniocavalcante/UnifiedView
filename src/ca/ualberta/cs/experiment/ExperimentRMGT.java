package ca.ualberta.cs.experiment;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import ca.ualberta.cs.distance.DistanceCalculator;
import ca.ualberta.cs.model.Dataset;
import ca.ualberta.cs.model.DescriptionAlgorithms;
import ca.ualberta.cs.model.Instance;
import ca.ualberta.cs.model.Util;
import ca.ualberta.cs.ssClassification.ExpLapSVM;
import ca.ualberta.cs.ssClassification.ExpRMGT;
import ca.ualberta.cs.ssClassification.Graph;
import ca.ualberta.cs.validity.FMeasure;

public class ExperimentRMGT
{
	private static final DescriptionAlgorithms parameters = new DescriptionAlgorithms();


	public static void performIt(String fileName, String fileFolder, String outputFolder,
			DistanceCalculator distance, String delimiter) throws Exception
	{

		FMeasure fMeasureStatistic = new FMeasure();

		// Validation Method (String) -> Algorithm (String) -> Number of missed classes (Integer) -> % labeled (Integer) -> mPts (Integer) -> Result (ArrayList<Double>)
		Map<String, Map<String, Map<Integer, Map<Integer, Map<Integer, ArrayList<Double>>>>>> report = new HashMap<String, Map<String, Map<Integer, Map<Integer, Map<Integer, ArrayList<Double>>>>>>();
		Map<Integer, Graph> mKNN= null; // MST files


		// Reading data set file
		int nmc=0;
		Dataset datasetExperiments = new Dataset();
		datasetExperiments  = Util.readInDataSet(fileFolder+fileName+".data", delimiter);
		int numPoints       = datasetExperiments.getObjects().length;
		double[][] database = new double[numPoints][];

		for(Instance inst: datasetExperiments.getObjects())
			database[inst.getIndex()] = inst.getCoordinates();


		// Read file with MSTS
		try(FileInputStream fileIn = new FileInputStream(outputFolder + fileName + ".mknn");
				ObjectInputStream in   = new ObjectInputStream(fileIn);)
		{
			mKNN= (Map<Integer,Graph>) in.readObject();
		}
		catch(ClassNotFoundException | IOException ec)
		{
			System.err.println("[RMGT]Error while reading Graph for dataset " + fileName);
			System.exit(-1);
		}

		//Defining the array of minPoints
		ArrayList<Integer> arrayMinPts = new ArrayList<Integer>(mKNN.keySet());


		String[] algorithmsDesc = 
			{
					// New configurations for the label propagation step
					parameters.rmgtMKNN,
			};

		// Initialization of the report for the algorithms
		report.put(parameters.FMClassification, new HashMap<String, Map<Integer, Map<Integer, Map<Integer, ArrayList<Double>>>>>());

		for(String alg: algorithmsDesc)
		{
			report.get(parameters.FMClassification).put(alg, new HashMap<Integer, Map<Integer, Map<Integer, ArrayList<Double>>>>());
			report.get(parameters.FMClassification).get(alg).put(nmc, new HashMap<Integer, Map<Integer, ArrayList<Double>>>());
		}

		long overallStartTime = System.currentTimeMillis();

		String fileLabeled = outputFolder+fileName+"-labeled-objects.csv";

		BufferedReader reader = new BufferedReader(new FileReader(fileLabeled));
		String line = reader.readLine();

		int lineIndex = 0;
		while (line != null) 
		{
			long timeTrial = System.currentTimeMillis();
			Util.resetDataset(datasetExperiments, true);

			HashMap<Integer, Integer> preLabeledDenBased = new HashMap<Integer, Integer>();

			lineIndex++;
			line.replaceAll("\\s+",""); // Remove spaces that might exist in the line
			String[] lineContents = line.split(":");

			int plab  = Integer.parseInt(lineContents[0]);
			int trial = Integer.parseInt(lineContents[1]);

			for(String alg: algorithmsDesc)
			{
				if(!report.get(parameters.FMClassification).get(alg).get(nmc).containsKey(plab))
				{
					report.get(parameters.FMClassification).get(alg).get(nmc).put(plab, new HashMap<Integer, ArrayList<Double>>());

					for(Integer mPts: arrayMinPts)
						report.get(parameters.FMClassification).get(alg).get(nmc).get(plab).put(mPts, new ArrayList<Double>());
				}
			}


			lineContents[2] = lineContents[2].replaceAll("\\s+","");
			String[] stringLabeled = lineContents[2].split(delimiter);

			// Store the set of labeled objects
			for (int i = 0; i < stringLabeled.length; i++) 
			{
				try
				{
					int idLabeled = Integer.parseInt(stringLabeled[i]);

					datasetExperiments.getObjects()[idLabeled].setPreLabeled(true);
					datasetExperiments.getObjects()[idLabeled].setLabel(datasetExperiments.getObjects()[idLabeled].getTrueLabel());
					preLabeledDenBased.put(idLabeled, datasetExperiments.getObjects()[idLabeled].getTrueLabel());
				}
				catch (NumberFormatException nfe) 
				{
					System.err.println("[LapSVM]Illegal value on line " + lineIndex + ":" + stringLabeled[i] + " Dataset: " + fileName);
				}
			}

			/**
			 * Run the algorithms
			 */
			double fm;
			for(Integer minPts: arrayMinPts)
			{
				// Run RMGT
				// -------Expand labels with the RMGT function-------
				Util.resetDataset(datasetExperiments, false);
				ExpRMGT.propagateLabels(mKNN.get(minPts), datasetExperiments, preLabeledDenBased);

				fm 		 = fMeasureStatistic.getIndex(datasetExperiments.getObjects());
				report.get(parameters.FMClassification).get(parameters.rmgtMKNN).get(nmc).get(plab).get(minPts).add(fm);
			}

			// Read the next trial of the algorithm
			line = reader.readLine();

			//-------------------------------- Save report of the data set ------------------------------------------------------
			String stringFolder 		    = outputFolder + fileName + ".reportRmgt";

			try (FileOutputStream outFile   = new FileOutputStream(stringFolder);
					ObjectOutputStream out = new ObjectOutputStream(outFile))
			{
				out.writeObject(report);
			}
			catch(IOException e)
			{
				System.out.println("[RMGT]An error ocurred while writing the report for data set " + fileName);
				e.printStackTrace();
				System.exit(-1);
			}

			System.out.println("[RMGT]Dataset:  " + fileName + ". plab: " + plab + ". Trial: " + trial + ". time(min): " + (((System.currentTimeMillis() - timeTrial)/1000)/60));

		}// End for while

		reader.close();

		System.out.println("[RMGT]Dataset:  " + fileName + ". Overall time(min): " + (((System.currentTimeMillis() - overallStartTime)/1000)/60));

	}
}