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
import java.util.List;

public class Spearman {
	private static String pathToPageRanks;
	private static String pathToNumViews;
	static String pathToOutput="/Users/Honey/data/index";
	
	static void parseCommandLine(String[] args) throws IOException, NumberFormatException {
		System.out.println("here"+args[0]);
		pathToPageRanks=args[1];
		pathToNumViews=args[0];
		compute(pathToNumViews,pathToPageRanks);
	}
	
	private static void compute(String pathToNumViews, String pathToPageRanks) throws IOException {
	
		String line;
		double x=0,y=0;
		int docId;
		double difference, square=0,summand=0,numerator=0,coeff=0;
		BufferedReader numViewsReader = new BufferedReader
				(new FileReader(pathToNumViews));
		int noOfDocs = 0;
	
		//for each document entry in num views compute the difference	
		while ((line=numViewsReader.readLine()) != null) {
			noOfDocs++;
			String[] eachLine= line.split(" ");
			docId=Integer.parseInt(eachLine[2]); //doc id
			y=Integer.parseInt(eachLine[1]); //numviews
			
			List<String> commands = new ArrayList<String>();
			commands.add("/bin/bash");
			commands.add("-c");
			//find same doc id in pageRank
			commands.add("grep '"+docId+"' '"+pathToPageRanks+"'");
			System.out.println("docId"+docId);
			ProcessBuilder pb = new ProcessBuilder(commands);
			Process p = pb.start();
			BufferedReader pageRankReader = new BufferedReader
					(new InputStreamReader(p.getInputStream()));
			String pageRankLine = pageRankReader.readLine();
			if ( pageRankLine!= null) {
				String[] pageRank = pageRankLine.split(" ");
				x=Integer.parseInt(eachLine[2]);
			}
			difference=x-y;
			square=Math.pow(difference, 2);
			summand=summand+square;		
		}
		numViewsReader.close();
		numerator=6*summand;
		System.out.println("no of Docs"+noOfDocs);
		double denominator=noOfDocs*(Math.pow(noOfDocs,2) -1);
		System.out.println("denominator"+denominator);
		coeff=1-(numerator/denominator);	
		System.out.println("coeff is"+coeff);
		writeToFile(coeff,pathToOutput);
	}
	
	private static void writeToFile(double coeff,String pathToOutput2) {
		try {
			StringBuilder file = new StringBuilder(pathToOutput2)
			.append("/").append("readme.txt");
			BufferedWriter bWriter = new BufferedWriter(new FileWriter(
					file.toString(), true));
			
					bWriter.write("coeff = "+coeff);
							
					   
			bWriter.close();			
		} catch (IOException e) {	  
			System.out.println("You have an exception");
			e.printStackTrace();
		}

	}
	public static void main(String[] args) {
		
		
		try {
			System.out.println("in this main");
			parseCommandLine(args);

		}
		catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}
}
