package hw4;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Scanner;

public class RetrievalEngineWt {

	
	public static ArrayList<DictionaryInstance> loadDictionary(){
		ArrayList<DictionaryInstance> dictionary = new ArrayList<DictionaryInstance>();
		Scanner s = null;
		try {
			s = new Scanner(new FileInputStream("dictionary.txt"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//For each item read by the scanner...
		while(s.hasNextLine()){
			String token = s.nextLine();
			int numInstances = s.nextInt();
			s.nextLine();
			int location = s.nextInt();
			s.nextLine();
			dictionary.add(new DictionaryInstance(token, numInstances, location));
		}
		return dictionary;
	}
	
	public static ArrayList<PostingsInstance> loadPostings(int start, int numInstances){
		ArrayList<PostingsInstance> postings = new ArrayList<PostingsInstance>();
		Scanner s = null;
		try {
			s = new Scanner(new FileInputStream("postings.txt"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//Skip to line in postings
		for(int i = 1; i < start; i++)
			s.nextLine();
		
		//Read in lines corresponding to terms
		for(int i = 0; i < numInstances; i++){
			String line = s.nextLine();
			postings.add(new PostingsInstance(new Integer(line.split(",")[0]), new Double(line.split(",")[1])));
		}
		return postings;
	}
	
	//TODO: Add weights as third argument here
	public static double dotProduct(ArrayList<ArrayList<PostingsInstance>> postings, int docNum, double[] weights){
		double total = 0;
		//for(ArrayList<PostingsInstance> list : postings){
		for(int i = 0; i < postings.size(); i++){
			for(PostingsInstance p : postings.get(i)){
				if(p.docNum==docNum)
					total += weights[i]*p.termWeight;
			}
		}
		return total;
	}
	
	public static void main(String[] args) {
		//Get search terms as arguments
		if(args.length == 0){
			System.out.println("Incorrect number of arguments. Usage: retrieve search_term1 search_weight1 ...");
			System.exit(-1);
		}
		String [] searchterms = new String[args.length/2];
		double[] searchweights = new double[args.length/2];
		for(int i = 0; i < args.length/2; i++){
			searchterms[i] = args[2*i];
			searchweights[i] = new Double(args[2*i+1]);
		}
		
		//Load dictionary into memory to locate items in postings file
		ArrayList<DictionaryInstance> dictionary = loadDictionary();
		ArrayList<ArrayList<PostingsInstance>> postings = new ArrayList<ArrayList<PostingsInstance>>();
		//For each searchterm, find its corresponding vector of {DocumentID, termWeight}
		for(int i = 0; i < searchterms.length; i++){
			boolean found = false;
			//find the searchterm in the dictionary, add it to the postings list
			for(int j = 0; j < dictionary.size() && !found; j++)
				if(searchterms[i].toLowerCase().equals(dictionary.get(j).token)){
					postings.add(loadPostings(dictionary.get(j).location,dictionary.get(j).numInstances));
					found=true;
				}
		}
		//So now we have a list of postings instances of size <= searchterms.length (list of lists of tuples of docs and term weights)
		//Now, we just dotproduct the vectors for each document and return the 10 highest.
		ArrayList<PostingsInstance> dotProds = new ArrayList<PostingsInstance>();
		for(int i = 1; i < 504; i++){
			double dotProd = dotProduct(postings, i, searchweights);
			if (dotProd > 0){

				dotProds.add(new PostingsInstance(i,dotProd));
				Collections.sort(dotProds, new Comparator<PostingsInstance>(){
					public int compare(PostingsInstance p1, PostingsInstance p2) {
						return (int)Math.floor(1000 * p2.termWeight - 1000 * p1.termWeight);
					}
				});
				if (dotProds.size() > 10){
					dotProds.remove(10);
				}
			}
		}
		
		if(dotProds.size()==0)
			System.out.println("There were no relevant documents corresponding to your query.");
		else
			for(PostingsInstance p : dotProds)
				System.out.println(String.format("%03d", p.docNum) + ".html " + (p.termWeight));
	}
}
