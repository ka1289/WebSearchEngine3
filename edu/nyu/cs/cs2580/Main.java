package edu.nyu.cs.cs2580;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Scanner;

public class Main {

	public static void main(String[] args) {
		try {
			Scanner sc = new Scanner(new FileReader("/Users/hiral/Documents/workspace/G05/hw3/g05/data/log/20130301-160000.log"));
			String line = null;
			while((line = sc.nextLine()) != null) {
				String[] split = line.split(" ");
				System.out.println(URLDecoder.decode(split[1], "UTF-8"));
			}
			
			System.out.println();
			sc.close();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
