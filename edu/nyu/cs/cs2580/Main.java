package edu.nyu.cs.cs2580;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.Scanner;


public class Main {

	public static void main(String[] args) {
		
		String a = "doc_map_3.csv";
		System.out.println(a.matches("doc_map_[0-9][0-9]?.csv"));
		
		
		float value = (10228 / 20);
		int fileNum = (int) (1 / value);
		System.out.println(fileNum);
		
//		try {
//			File corpusDir = new File("/Users/hiral/Documents/workspace/G05/hw3/g05/data/wiki/");
//			File[] listOfFiles = corpusDir.listFiles();
//			int noOfFiles = listOfFiles.length;
//			
//			HashSet<String> set = new HashSet<String>(noOfFiles);
//			for (File eachFile : listOfFiles) {
//				System.out.println(eachFile.getName());
//				set.add(eachFile.getName());
//			}
//			
//			Scanner sc = new Scanner(new FileReader("/Users/hiral/Documents/workspace/G05/hw3/g05/data/log/20130301-160000.log"));
//			String line = null;
//			int count = 0;
//			while(sc.hasNextLine()) {
//				line = sc.nextLine();
//				String[] split = line.split(" ");
//				try {
//					String tmp = URLDecoder.decode(split[1].trim(), "UTF-8");
//					if(set.contains(tmp)) {
//						count++;
//						System.out.println(tmp);
//					}
//					
//				}catch(IllegalArgumentException e) {
//					
//					System.out.println(e.getMessage());
//				}
//			}
//			System.out.println("*****************");
//			System.out.println(count);
//			sc.close();
//		} catch (FileNotFoundException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}catch (UnsupportedEncodingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}
}
