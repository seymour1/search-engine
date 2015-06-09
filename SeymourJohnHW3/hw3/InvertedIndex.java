package hw3;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

public class InvertedIndex {
	/**TODO: Add a hashmap for the tokens/tf*idf values and maybe even tokens/numDocs**/

	//HashSet of all tokens in entire corpus
	static Map<String, Integer> allTokens = new LinkedHashMap<String, Integer>();

	public static Map<String, Integer> readAndTokenizeFile(String filename){

		//ArrayList of all tokens in this particular data file
		Map<String, Integer> tokens = new LinkedHashMap<String, Integer>();

		//Open the file for reading
		Scanner s = null;
		try {
			s = new Scanner(new FileInputStream(filename));//.useDelimiter(" |<[^>]*>");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//For each item read by the scanner...
		while(s.hasNext()){

			//Lowercase, remove all html tags, and strip all non alphabetic characters
			String token = s.next().toLowerCase().replaceAll("[^a-z]", "");

			//If we've already seen that token in this file, increment frequency; else add to list
			if(tokens.containsKey(token)){
				tokens.put(token, new Integer(tokens.get(token).intValue()+1));
			}
			else
				tokens.put(token, new Integer(1));

			//If we've already seen that token in this corpus, increment frequency; else add to list
			if(allTokens.containsKey(token)){
				allTokens.put(token, new Integer(allTokens.get(token).intValue()+1));
			}
			else
				allTokens.put(token, new Integer(1));
		}

		//Close the stream
		s.close();

		//Here's where we're going to do our processing.

		//remove stopwords
		//Open the stopwords file for reading
		Scanner s2 = null;
		try {
			s2 = new Scanner(new FileInputStream("stoplist.txt"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//For each item read by the scanner...
		while(s2.hasNext()){

			//Lowercase and strip all non alphabetic characters
			String token = s2.next().toLowerCase().replaceAll("[^a-z]", "");

			//If either hashmap contains word then remove it
			tokens.remove(token);
			allTokens.remove(token);
		}

		//Close the stream
		s2.close();

		//remove words of length <=1
		char singleton = 'a';
		for(int i = 0; i < 26; i++){
			tokens.remove(singleton+i);
			allTokens.remove(singleton+i);
		}

		//We'll handle removing words that occur only once in the entire corpus at term weighting to avoid a second pass.

		//Because we delimit AFTER we read the item, we can end up with null tokens.  Remove them.
		tokens.remove("");
		allTokens.remove("");
		return tokens;
	}

	public static void main(String[] args) {
		//Get input/output directories as arguments
		if(args.length != 2){
			System.out.println("Incorrect number of arguments. Usage: CalcWeights inputdir outputdir");
			System.exit(-1);
		}
		String inputDir = args[0];
		String outputDir = args[1];

		long startTime = System.currentTimeMillis();
		//Get all the files in the input directory
		String[] files = new File(inputDir).list();
		ArrayList<Map<String, Integer>> tokens = new ArrayList<Map<String, Integer>>();
		ArrayList<Map<String, Double>> tfidfs = new ArrayList<Map<String, Double>>();
		//For each file in the input directory, tokenize it
		for(int i = 0; i < files.length; i++){
			//Tokenize the file
			tokens.add(readAndTokenizeFile("" + inputDir + "\\" + files[i]));
			tfidfs.add(new LinkedHashMap<String, Double>());
		}
		PrintWriter dictWriter = null;
		PrintWriter postingsWriter = null;
		try{
			dictWriter = new PrintWriter(outputDir + "\\dictionary.txt", "UTF-8");
			postingsWriter = new PrintWriter(outputDir + "\\postings.txt", "UTF-8");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		//term weight each set of tokens and write to file.
		for(int i = 0; i < tokens.size(); i++){
			for(Entry<String, Integer> entry: tokens.get(i).entrySet())
				//Remove all words that occur only once in corpus
				if(allTokens.get(entry.getKey())<=1)
					tokens.remove(entry.getKey());
				else{
					//Get TF
					int tf = entry.getValue();

					//Get idf
					int n = files.length;
					int docsWithTerm = 0;
					for(int j = 0; j < tokens.size(); j++)
						if(tokens.get(j).containsKey(entry.getKey()))
							docsWithTerm++;
					double idf = Math.log(1.0 * n / docsWithTerm);
					//Get normalization term
					int total = 0;
					for(Entry<String, Integer> entry2: tokens.get(i).entrySet())
						total += entry2.getValue();

					//Add the tfidf to the map
					tfidfs.get(i).put(entry.getKey(), (1.0 * tf * idf / total));
				}

		}
		//After this point, all tf*idf values are written to the corresponding documents' hashmap.
		//To go further, we iterate over the terms:
		//For each term
			//For Each Document
				//print {document num,Term Weight} to postings file
			//Print {term, numdocuments with term, location in postings file} to dict file
			//Update location in postings file
		int locInPostings = 1;
		for(Entry<String, Integer> entry: allTokens.entrySet()){
			int docsWithTerm = 0;
			for(int i = 0; i < files.length; i++){
				if(tokens.get(i).containsKey(entry.getKey()) && (tfidfs.get(i)).containsKey(entry.getKey())){
					docsWithTerm++;
					postingsWriter.println((i+1) + "," + tfidfs.get(i).get(entry.getKey()));
				}
			}
			dictWriter.println(entry.getKey());
			dictWriter.println(docsWithTerm);
			dictWriter.println(locInPostings);
			locInPostings = locInPostings+docsWithTerm;
		}
		dictWriter.close();
		postingsWriter.close();

		System.out.println("Total time: " + (System.currentTimeMillis() - startTime) + "ms.");
	}
}
