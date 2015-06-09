package hw5;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

public class Cluster {

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
	
	public static double jaccardSimilarity(Map<String, Integer> doc1, Map<String, Integer> doc2){
		int union = 0;
		int intersection = 0;
		for(String key : doc1.keySet())
			if(doc2.containsKey(key)){
				union++;
				intersection++;
			}
			else
				intersection++;
		for(String key : doc2.keySet())
			if(!doc1.containsKey(key))
				intersection++;
		return 1.0*union/intersection;
	}
	
	public static Map<String,Integer> centroid(Map<String, Integer> doc1, Map<String, Integer> doc2){
		Map<String,Integer> centroid = new LinkedHashMap<String, Integer>();
		for(String key : allTokens.keySet()){
			if(doc1.containsKey(key))
				centroid.put(key, 1);
			else if(doc2.containsKey(key))
				centroid.put(key, 1);
		}
		return centroid;
	}

	public static void main(String[] args) {
		//Get input/output directories as arguments
		if(args.length != 1){
			System.out.println("Incorrect number of arguments. Usage: Cluster inputdir");
			System.exit(-1);
		}
		String inputDir = args[0];

		long startTime = System.currentTimeMillis();
		//Get all the files in the input directory
		String[] files = new File(inputDir).list();
		ArrayList<Map<String, Integer>> tokens = new ArrayList<Map<String, Integer>>();
		//For each file in the input directory, tokenize it
		for(int i = 0; i < files.length; i++){
			//Tokenize the file
			tokens.add(readAndTokenizeFile("" + inputDir + "\\" + files[i]));
		}
		
		//Remove all words that occur only once in corpus
		for(Entry<String, Integer> entry: allTokens.entrySet())
			if(entry.getValue()<=1)
				allTokens.put(entry.getKey(), 0);
		
		//After this point, frequencies are written to the corresponding documents' hashmap.

		boolean done = false;
		
		//Add all initial clusters
		ArrayList<ArrayList<Integer>> clusters = new ArrayList<ArrayList<Integer>>();
		for(int i = 0; i < tokens.size(); i++){
			clusters.add(new ArrayList<Integer>());
			clusters.get(i).add(new Integer(i+1));
		}

/* To print the document most similar to the corpus centroid
		double max = -1;
		int minDoc = -1;
		for(int i = 0; i < tokens.size(); i++)
			if(max < jaccardSimilarity(allTokens,tokens.get(i))){
				max = jaccardSimilarity(allTokens,tokens.get(i));
				minDoc = i+1;
			}
		System.out.println(minDoc);*/
		
/* To print the least similar documents
		double min = 1;
		int min1 = -1; int min2 = -1;
		for(int i = 0; i < tokens.size(); i++)
			for(int j = i; j < tokens.size(); j++)
				if(jaccardSimilarity(tokens.get(i),tokens.get(j)) < min){
					min = jaccardSimilarity(tokens.get(i),tokens.get(j));
					min1 = i+1;
					min2 = j+1;
				}
		System.out.println("" + min1 + " " + min2 + " " + min);*/

		while(!done){
			//Calculate the similarities for all clusters
			double[][] similarities = new double[tokens.size()][tokens.size()];
			for(int i = 0; i < tokens.size(); i++)
				for(int j = i; j < tokens.size(); j++)
					similarities[i][j] = jaccardSimilarity(tokens.get(i),tokens.get(j));
			
			//find the maximum similarity
			double max = -1;
			int maxI = 0;
			int maxJ = 0;
			for(int i = 0; i < similarities.length; i++)
				for(int j = i; j < similarities[i].length; j++)
					if(similarities[i][j] > max && i != j){
						max = similarities[i][j];
						maxI = i;
						maxJ = j;
					}
			
			if(max < 0.4)
				done = true;
			else{
				//Merge the clusters maxI and maxJ
				System.out.println("Merging cluster " + (maxI+1) + " and " + (maxJ+1));
				tokens.add(centroid(tokens.get(maxI),tokens.get(maxJ)));
				tokens.remove(maxI);
				tokens.remove(maxJ);
				ArrayList<Integer> toAdd = new ArrayList<Integer>();
				for(int i = 0; i < clusters.get(maxI).size(); i++){
					toAdd.add(clusters.get(maxI).get(i));
					clusters.get(maxI).remove(i);
				}
				for(int i = 0; i < clusters.get(maxJ).size(); i++){
					toAdd.add(clusters.get(maxJ).get(i));
					clusters.get(maxJ).remove(i);
				}
				clusters.add(toAdd);
			}
		}
		for(int i = 0; i < clusters.size(); i++){
			System.out.print("Cluster " + i + ": {");
			for(int j = 0; j < clusters.get(i).size(); j++){
				System.out.print(clusters.get(i).get(j) + ".html, ");
			}
			System.out.println("}");
		}
		
		System.out.println("Total time: " + (System.currentTimeMillis() - startTime) + "ms.");
	}
}
