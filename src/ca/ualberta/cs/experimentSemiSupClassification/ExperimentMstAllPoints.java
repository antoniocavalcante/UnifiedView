package ca.ualberta.cs.experimentSemiSupClassification;

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
import java.util.TreeMap;

import javafx.util.Pair;
import ssExtraction.UnifiedView;
import ssExtraction.SemiWeight;
import ca.ualberta.cs.distance.DistanceCalculator;
import ca.ualberta.cs.hdbscanApts.HDBSCANApts;
import ca.ualberta.cs.hdbscanstar.HDBSCANStar;
import ca.ualberta.cs.hdbscanstar.UndirectedGraph;
import ca.ualberta.cs.model.Dataset;
import ca.ualberta.cs.model.DescriptionAlgorithms;
import ca.ualberta.cs.model.Instance;
import ca.ualberta.cs.model.Util;
import ca.ualberta.cs.validity.FMeasure;

public class ExperimentMstAllPoints
{
	private static final DescriptionAlgorithms parameters = new DescriptionAlgorithms();


	public static void performIt(String fileName, String fileFolder, String outputFolder,
			DistanceCalculator distance,
			double rho, double expo,
			String delimiter) throws Exception
	{

		FMeasure fMeasureStatistic = new FMeasure();

		// Validation Method (String) -> Algorithm (String) -> Number of missed classes (Integer) -> % labeled (Integer) -> mPts (Integer) -> Result (ArrayList<Double>)
		Map<String, Map<String, Map<Integer, Map<Integer, Map<Integer, ArrayList<Double>>>>>> report = new HashMap<String, Map<String, Map<Integer, Map<Integer, Map<Integer, ArrayList<Double>>>>>>();
		Map<Integer, UndirectedGraph> mstAp= null; // MST files


		// Reading data set file
		int nmc=0;
		Dataset datasetExperiments = new Dataset();
		datasetExperiments  = Util.readInDataSet(fileFolder+fileName+".data", delimiter);
		int numPoints       = datasetExperiments.getObjects().length;
		double[][] database = new double[numPoints][];

		for(Instance inst: datasetExperiments.getObjects())
			database[inst.getIndex()] = inst.getCoordinates();


		// Read file with MSTS
		try(FileInputStream fileIn = new FileInputStream(outputFolder + fileName + ".mstApts");
				ObjectInputStream in   = new ObjectInputStream(fileIn);)
		{
			mstAp= (Map<Integer,UndirectedGraph>) in.readObject();
		}
		catch(ClassNotFoundException | IOException ec)
		{
			System.err.println("[MST-AP]Error while reading MST for dataset " + fileName);
			System.exit(-1);
		}

		//Defining the array of minPoints
		ArrayList<Integer> arrayMinPts = new ArrayList<Integer>(mstAp.keySet());


		String[] algorithmsDesc = 
			{
					// New configurations for the label propagation step

					parameters.hdbscanAp,
					parameters.hdbscanApWPWD,
					parameters.hdbscanApWMST,
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
			TreeMap<Integer, Pair<Integer, double[]>> classifiedHissclu = new TreeMap<Integer, Pair<Integer,double[]>>();


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
					classifiedHissclu.put(idLabeled, new Pair<Integer, double[]>(datasetExperiments.getObjects()[idLabeled].getTrueLabel(), datasetExperiments.getObjects()[idLabeled].getCoordinates()));
				}
				catch (NumberFormatException nfe) 
				{
					System.err.println("[MST-AP]Illegal value on line " + lineIndex + ":" + stringLabeled[i] + " Dataset: " + fileName);
				}
			}

			/**
			 * Run the algorithms
			 */
			Map<Integer, Integer> result = null;
			SemiWeight semi   	 = new SemiWeight(classifiedHissclu, rho, expo, distance);
			semi.performQuery(database); // Perform the query define the optimal labeled objects
			double fm;

			for(Integer minPts: arrayMinPts)
			{
				// Apply: HDBSCAN*(ap,-)
				Util.resetDataset(datasetExperiments, false);
				result=null;
				result = UnifiedView.expandLabels(new UndirectedGraph(mstAp.get(minPts)), preLabeledDenBased, false);

				for(Map.Entry<Integer, Integer> entries: result.entrySet())
					datasetExperiments.getObjects()[entries.getKey()].setLabel(entries.getValue());

				fm    = fMeasureStatistic.getIndex(datasetExperiments.getObjects());
				report.get(parameters.FMClassification).get(parameters.hdbscanAp).get(nmc).get(plab).get(minPts).add(fm);


				// Apply: HDBSCAN*(ap,wMST)
				Util.resetDataset(datasetExperiments, false);
				result=null;
				result = UnifiedView.expandWeighted(database, new UndirectedGraph(mstAp.get(minPts)), preLabeledDenBased, semi, distance, false);

				for(Map.Entry<Integer, Integer> entries: result.entrySet())
					datasetExperiments.getObjects()[entries.getKey()].setLabel(entries.getValue());

				fm    = fMeasureStatistic.getIndex(datasetExperiments.getObjects());
				report.get(parameters.FMClassification).get(parameters.hdbscanApWMST).get(nmc).get(plab).get(minPts).add(fm);


				// Apply: HDBSCAN*(ap,wPWD)
				Util.resetDataset(datasetExperiments, false);
				result=null;
				double[] cd = HDBSCANApts.calculateWeightedCoreDistances(database, distance, semi);
				UndirectedGraph mstWdist = HDBSCANStar.constructWeightedMST(database, cd, true, distance, semi);
				result = UnifiedView.expandLabels(mstWdist, preLabeledDenBased, false);

				for(Map.Entry<Integer, Integer> entries: result.entrySet())
					datasetExperiments.getObjects()[entries.getKey()].setLabel(entries.getValue());

				fm    = fMeasureStatistic.getIndex(datasetExperiments.getObjects());
				report.get(parameters.FMClassification).get(parameters.hdbscanApWPWD).get(nmc).get(plab).get(minPts).add(fm);
			}

			// Read the next trial of the algorithm
			line = reader.readLine();

			//-------------------------------- Save report of the data set ------------------------------------------------------
			String stringFolder 		    = outputFolder + fileName + ".reportAlgorithmsAp";

			try (FileOutputStream outFile   = new FileOutputStream(stringFolder);
					ObjectOutputStream out = new ObjectOutputStream(outFile))
			{
				out.writeObject(report);
			}
			catch(IOException e)
			{
				System.out.println("[MST-AP]An error ocurred while writing the report for data set " + fileName);
				e.printStackTrace();
				System.exit(-1);
			}

			System.out.println("[MST-AP]Dataset:  " + fileName + ". plab: " + plab + ". Trial: " + trial + ". time(min): " + (((System.currentTimeMillis() - timeTrial)/1000)/60));

		}// End for while

		reader.close();

		System.out.println("[MST-AP]Dataset:  " + fileName + ". Overall time(min): " + (((System.currentTimeMillis() - overallStartTime)/1000)/60));

	}
}