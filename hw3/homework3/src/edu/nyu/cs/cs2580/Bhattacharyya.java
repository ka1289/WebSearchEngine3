package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import com.sun.xml.internal.xsom.impl.scd.Iterators.Map;

public class Bhattacharyya {
	private static String pathToPRF;
	private static String pathToOutput;
	private static HashMap<String, HashMap<String, Double>> queryPairCoeff=new HashMap<String,HashMap<String,Double>>();
	private static HashMap<String, Double> queryCoeff=new HashMap<String,Double>();

	private static void parseCommandLine(String[] args) throws IOException, NumberFormatException {
		pathToPRF=args[1];
		pathToOutput=args[2];

	}

	private static void compute(String pathToPRF) 
			throws NumberFormatException, IOException {
		File queryDir = new File(pathToPRF);
		File[] listOfFiles = queryDir.listFiles();
		double summand=0.0,probability=0.0,product=0.0,
				probabilityQuery1=0.0,probabilityQuery2=0.0;
		double computedValue=0.0;
		String word;
		String[] query1, query2;

		for (File eachFile : listOfFiles){
			BufferedReader query1File = new BufferedReader
					(new FileReader(eachFile.getAbsoluteFile()));
			String wordQuery1, wordQuery2;
			String query1FileName=eachFile.getName();
			query1 = query1FileName.split(".");
			//find the word in the other queries
			for (File otherFile : listOfFiles){
				String query2FileName=otherFile.getName();
				query2 = query2FileName.split(".");
				//get the word from query1
				while (((wordQuery1 = query1File.readLine()) != null)) {
					String[] eachLine = wordQuery1.split("\t");
					word = eachLine[0];
					probabilityQuery1=Integer.parseInt(eachLine[1]);

					//not the same as query1
					if(!otherFile.equals(eachFile)){
			
						BufferedReader query2File = new BufferedReader
								(new FileReader(otherFile.getAbsoluteFile()));				
						List<String> commands = new ArrayList<String>();
						commands.add("/bin/bash");
						commands.add("-c");
						//check with hiral is query.txt will have one line with key and value
						commands.add("grep '"+word+"' '"+otherFile+"'");
						ProcessBuilder pb = new ProcessBuilder(commands);
						Process p = pb.start();
						BufferedReader sameWordQuery2 = new BufferedReader
								(new InputStreamReader(p.getInputStream()));
						String sameWord = sameWordQuery2.readLine();
						if ( sameWord!= null) {
							String[] line = sameWord.split("\t");
							probabilityQuery2 = Integer.parseInt(eachLine[1]);
							probability=probabilityQuery1*probabilityQuery2;
							summand=summand+probability;
						}
					}

				}
				queryCoeff.put(query2[0],summand);
			}
			queryPairCoeff.put(query1[0], queryCoeff);
			queryCoeff.clear();
			summand=0.0;		
		}	
		writeToFile(pathToOutput);
	}

	private static void writeToFile(String pathToOutput2) {
		try {
			StringBuilder file = new StringBuilder(pathToOutput2)
			.append("/").append("BhattacharyyasCoeff.csv");
			BufferedWriter bWriter = new BufferedWriter(new FileWriter(
					file.toString(), true));
			for (String query1 : queryPairCoeff.keySet()) {
				queryCoeff = queryPairCoeff.get(query1);
				for (String query2 : queryCoeff.keySet()) {
					bWriter.write("<"+query1 + "><"+query2+"><"
							+queryCoeff.get(query2)+">");
				}				
			}			   
			bWriter.close();			
		} catch (IOException e) {	  
			System.out.println("You have an exception");
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
		try {
			Bhattacharyya.parseCommandLine(args);
			compute(pathToPRF);

		}
		catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}
}
