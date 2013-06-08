package invert;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.TreeMap;

public class UserInterface {
	InvertedFile invObj;
	
	public UserInterface(String docDirectory, String stopListPath, String outputPath) {
		invObj = new InvertedFile(docDirectory, stopListPath, outputPath);
	}
	
	/**
	 * Invokes functions to prepare word list, postings file and write them to disk
	 * @throws IOException
	 */
	public void prepareData() throws IOException {
		try {
			// Create word list
			invObj.createStopList();
		} catch (IOException e) {
			System.out.println("Unable to create stoplist");
		}
		
		invObj.iterateOverDirectory();
		// Write word list and postings file to disk
		invObj.createWordListOnDisk();
		invObj.createPostingsFileOnDisk();
	}

	/**
	 * Handle queries which the user enters
	 * @param userQuery
	 * @throws IOException
	 */
	public void handleUserQuery(String userQuery) throws IOException {
		String terms[] = userQuery.split(InvertedFile.whiteSpacePattern);
		
		// Check if its a single term or multiple term query
		if (terms.length == 1) {
			handleSingleTerm(terms[0]);
		} else {
			handleMultipleTerms(terms);
		}
	}
	
	/**
	 * Handle single term queries
	 * @param term
	 * @throws IOException
	 */
	private void handleSingleTerm(String term) throws IOException {
		TreeMap<String, Double> termFreq = invObj.getTF(term);
		double idf;
		
		if (termFreq != null) {
			idf = invObj.getIDF(term);
			System.out.print("\nIDF = " + idf);
			
			for (String document : termFreq.keySet()) {
				if (termFreq.get(document) == 0) 
					continue;
				
				ArrayList<Integer> termLocInDoc = invObj.getTermLocationInDocument(term, document);
				System.out.println("\n\n" + document + "[" + termLocInDoc.size() + "]: " + termLocInDoc);
				
				System.out.println("TF = " + termFreq.get(document));
				System.out.println("TF.IDF = " + (termFreq.get(document) * idf));
				System.out.println("Context is as follows: ");
				invObj.printTermInDocument(term, document);
			}
		} else {
			if (invObj.isStopListWord(term)) {
				System.out.println("The term you entered is a stop list word and we are not tracking it.\n");
			} else {
				System.out.println("Term " + term.toUpperCase() + " does not exist in our word list.\n");
			}
		}
	}
	
	/**
	 * Handle multiple term queries
	 * @param terms
	 * @throws IOException
	 */
	private void handleMultipleTerms(String terms[]) throws IOException {
		ArrayList<String> queryTerms = new ArrayList<String>();
		TreeMap<String, Double> documentTFIDFs = new TreeMap<String, Double>();
		int i, j, k, counter = 0, displayFlag = 1;
		
		for (i = 0; i < terms.length; i++) {
			if (!invObj.isStopListWord(terms[i])) {
				queryTerms.add(terms[i]);
			}
		}
						
		if (queryTerms.size() == 0) {
			System.out.println("All terms are stop list words. Unable to generate results.");
		} else if (queryTerms.size() == 1) {
			handleSingleTerm(queryTerms.get(0));
		} else {
			for (i = 0; i < queryTerms.size(); i++) {
				if (invObj.isTermOnWordList(queryTerms.get(i)))
					counter++;
			}
			
			if (counter == queryTerms.size()) {
				ArrayList<String> documents = invObj.getDocumentList();
				
				for (i = 0; i < documents.size(); i++) {
					if (invObj.isTermInDocument(queryTerms.get(0), documents.get(i))) {
						for (j = 1; j < queryTerms.size(); j++) {
							if (!invObj.isTermInDocument(queryTerms.get(j), documents.get(i))) 
								break;
						}
						
						if (j == queryTerms.size()) {
							double documentTFIDFsValue = 0;
							
							for (k = 0; k < queryTerms.size(); k++) 
								documentTFIDFsValue+=(invObj.getIDF(queryTerms.get(k)) * invObj.getTFInDocument(queryTerms.get(k), documents.get(i))); 
							
							documentTFIDFs.put(documents.get(i), documentTFIDFsValue);
							displayFlag = 0;
						}
					} 
				}
				
				if (documentTFIDFs.isEmpty() && displayFlag == 1) {
					System.out.println("All query terms do not appear together in any documents.");
				} else {
					displayDocumentsInOrder(documentTFIDFs);
				}
				
			} else if (counter > 0) {
				System.out.println("Some terms are not on word list and so query is unable to generate results.");
			} else {
				System.out.println("None of the terms appear in our word list. Try with a better query.");
			}
		}
	}
	
	/**
	 * Displays documents in descending order of TF.IDF scores
	 * @param documentTFIDFs
	 */
	private void displayDocumentsInOrder(TreeMap<String, Double> documentTFIDFs) {
		String maxTFIDFDocument;
		int count = documentTFIDFs.size();
		while (count != 0) {
			maxTFIDFDocument = getMaxValue(documentTFIDFs);
			System.out.println(maxTFIDFDocument + " " + documentTFIDFs.get(maxTFIDFDocument));
			documentTFIDFs.remove(maxTFIDFDocument);
			count--;
		}
	}
	
	/**
	 * 
	 * @param documentTFIDFs
	 * @return Document name having maximum TF.IDF score
	 */
	private String getMaxValue(TreeMap<String, Double> documentTFIDFs) {
		double maxVal = 0;
		String maxTFIDFDocument = null;
		for (String document : documentTFIDFs.keySet()) {
			if (documentTFIDFs.get(document) > maxVal) {
				maxVal = documentTFIDFs.get(document);
				maxTFIDFDocument = document;
			}
		}
		
		return maxTFIDFDocument;
	}
	
	/**
	 * 
	 * @param args			0 - directory path for documents
	 * 						1 - file path for stop list
	 * 						2 - directory path for storing word list, postings file
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		String userQuery = null;
			
		if (args.length != 3) {
			System.out.println("Arguments missing. Please enter the required parameters i.e. directory path to documents, path to stop list, path to export word list and postings file.\nEXITING");
			System.exit(1);
		}
		
		UserInterface ui = new UserInterface(args[0], args[1], args[2]);
		
		ui.prepareData();
		while (true) {
			System.out.print("\n\nEnter your query: ");
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			userQuery = br.readLine();
			if (userQuery.equals("ZZZ"))
				break;
			if (userQuery.equals("")) {
				System.out.println("No query entered. Enter some query.");
				continue;
			}
			ui.handleUserQuery(userQuery);
		} 
		
		System.out.println("\nThank you for trying out the system");
	}
}
