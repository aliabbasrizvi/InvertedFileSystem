package invert;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.TreeMap;

public class InvertedFile {

	String dirPath, stopListPath, outputDirPath;
	ArrayList<String> stopWordList = new ArrayList<String>();
	TreeMap<String, Integer> wordList = new TreeMap<String, Integer>();
	TreeMap<String, TreeMap<String, ArrayList<Integer>>> wordPosition = new TreeMap<String, TreeMap<String, ArrayList<Integer>>>();
	TreeMap<String, Integer> documentLength = new TreeMap<String, Integer>();
	int contextWords = 10;
	int totalDocuments = 0;
	public static final String whiteSpacePattern = "\\s";

	public InvertedFile(String directory, String stoplist, String outputDirectory) {
		dirPath = directory;
		stopListPath = stoplist;
		outputDirPath = outputDirectory;
	}

	/**
	 * 
	 * @throws IOException
	 * 
	 * Create a stop list from the stop-list file provided	
	 */
	public void createStopList() throws IOException {
		FileInputStream fstream = null;
		String line;
		try {
			fstream = new FileInputStream(stopListPath);
		} catch (FileNotFoundException e) {
			System.out.println("File does not exist. Set correct path");
		}

		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));

		while((line = br.readLine()) != null) {
			stopWordList.add(line.trim().toLowerCase());
		}
	}

	/**
	 * 
	 * @param word
	 * @return Whether the term is a stop list word or not
	 */
	public boolean isStopListWord(String word) {
		if (stopWordList.contains(word.toLowerCase()))
			return true;

		return false;
	}
	
	/**
	 * Display the word list generated
	 */
	public void displayWordList() {
		for (String word : wordList.keySet())
			System.out.println(word + " " + wordList.get(word));
	}

	/**
	 * 
	 * @param term
	 * @return Whether the term appears in the word list or not
	 */
	public boolean isTermOnWordList(String term) {
		if (wordList.containsKey(term.toLowerCase()))
			return true;

		return false;
	}

	/**
	 * 
	 * @param term
	 * @param document
	 * @return Whether term appears in a particular document or not
	 */
	public boolean isTermInDocument(String term, String document) {
		TreeMap<String, ArrayList<Integer>> documentList = wordPosition.get(term.toLowerCase());

		if (documentList.containsKey(document))
			return true;

		return false;
	}
	
	/**
	 * Writes word list generated on disk at <outputDirPath>/wordlist.txt
	 * @throws IOException 
	 */
	public void createWordListOnDisk() throws IOException {
		FileWriter fstream = new FileWriter(outputDirPath + "/wordlist.txt");
		
		BufferedWriter out = new BufferedWriter(fstream);
		
		for (String word : wordList.keySet()) 
			out.write(word + " " + wordList.get(word) + "\n");
		
		out.close();
	}
	
	/**
	 * Writes postings file onto the disk at <outputDirPath>/postings.txt
	 * @throws IOException
	 */
	public void createPostingsFileOnDisk() throws IOException {
		FileWriter fstream = new FileWriter(outputDirPath + "/postings.txt");
		
		BufferedWriter out = new BufferedWriter(fstream);
		
		for (String word : wordPosition.keySet()) {
			out.write(word + "\n");
			for (String fileName : wordPosition.get(word).keySet()) {
				out.write(fileName + " [" + wordPosition.get(word).get(fileName).size() + "]: " + wordPosition.get(word).get(fileName) + "\n");
			}
			
			out.write("\n");
		}
		
		out.close();
	}

	/**
	 * 
	 * @throws IOException
	 * 
	 * Iterate over all files in a directory which form the document corpus
	 */
	public void iterateOverDirectory() throws IOException {
		File dir = new File(dirPath);
		for (File file : dir.listFiles()) {
			if (file.getName().equals(".") ||
					file.getName().equals("..") || 
					file.isHidden() || 
					file.isDirectory())
				continue;

			parseFile(file.getAbsolutePath(), file.getName());
			totalDocuments++;
		}
	}

	/**
	 * 
	 * @param filePath
	 * @param fileName
	 * @throws IOException
	 * 
	 * Parse a single file having path filePath and name fileName and populate word list and posting data
	 */
	private void parseFile(String filePath, String fileName) throws IOException {
		FileInputStream fstream = null;
		String line, word;
		ArrayList<Integer> positionList = null;
		TreeMap<String, ArrayList<Integer>> filePositionMap = null;
		int wordCounter = 0, i;

		try {
			fstream = new FileInputStream(filePath);
		} catch (FileNotFoundException e) {
			System.out.println("File does not exist. Set correct path");
		}

		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));

		while((line = br.readLine()) != null) {
			String wordsInLine[] = line.split(whiteSpacePattern);
			for (i = 0; i < wordsInLine.length; i++) {
				word = wordsInLine[i].trim().toLowerCase();
				if (isStopListWord(word)) {
					wordCounter+=1;
					continue;
				}
				if (wordList.containsKey(word)) {
					filePositionMap = wordPosition.get(word);
					if (filePositionMap.containsKey(fileName)) {
						filePositionMap.get(fileName).add(wordCounter);
					} else {
						positionList = new ArrayList<Integer>();
						positionList.add(wordCounter);
						filePositionMap.put(fileName, positionList);
						wordList.put(word, wordList.get(word) + 1);
					}
				} else {
					wordList.put(word, 1);
					positionList = new ArrayList<Integer>();
					positionList.add(wordCounter);
					filePositionMap = new TreeMap<String, ArrayList<Integer>>();
					filePositionMap.put(fileName, positionList);

					wordPosition.put(word, filePositionMap);
				}
				wordCounter+=1;
			}
		}

		in.close();
		documentLength.put(fileName, wordCounter);
	}

	/**
	 * 
	 * @param term
	 * @param document
	 * @return List of location(s) where term appears in a document
	 */
	public ArrayList<Integer> getTermLocationInDocument(String term, String document) {
		return wordPosition.get(term.toLowerCase()).get(document);
	}

	/**
	 * 
	 * @param term
	 * @return Number of documents a particular term appears in
	 */
	private int getDocumentFrequency(String term) {
		if (wordList.get(term.toLowerCase()) != null)
			return wordList.get(term.toLowerCase());

		return 0;
	}

	/**
	 * 
	 * @param term
	 * @return Inverse document frequency for the term
	 */
	public double getIDF(String term) {
		int termFreq = getDocumentFrequency(term.toLowerCase());
		if (termFreq != 0)
			return (double) (1 + Math.log10(totalDocuments) - Math.log10(termFreq));

		return (Double) null;
	}

	/**
	 * 
	 * @param term
	 * @param document
	 * @return Term frequency for term in document
	 */
	public double getTFInDocument(String term, String document) {
		if (isTermInDocument(term.toLowerCase(), document)) {
			int termAppearance = wordPosition.get(term.toLowerCase()).get(document).size();
			return (1 + Math.log10(termAppearance));
		}

		return 0;
	}

	/**
	 * 
	 * @return List of document names which are there in the corpus
	 */
	public ArrayList<String> getDocumentList() {
		ArrayList<String> documentList = new ArrayList<String>();

		for (String document : documentLength.keySet())
			documentList.add(document);

		return documentList;
	}

	/**
	 * 
	 * @param term
	 * @return Term frequency map which maps term frequency for a term to a particular document
	 */
	public TreeMap<String, Double> getTF(String term) {
		if (!wordList.containsKey(term.toLowerCase()))
			return null;
		ArrayList<String> documentList = getDocumentList();

		TreeMap<String, Double> termFreq = new TreeMap<String, Double>();
		for (int i = 0; i < documentList.size(); i++) {
			termFreq.put(documentList.get(i), getTFInDocument(term.toLowerCase(), documentList.get(i)));
		}

		return termFreq;
	}

	/**
	 * 
	 * @param term
	 * @param document
	 * @throws IOException
	 * 
	 * Print the term as it appears in the document 
	 */
	public void printTermInDocument(String term, String document) throws IOException {
		int location = wordPosition.get(term.toLowerCase()).get(document).get(0);
		int wordCounter = 0, i, j;
		FileInputStream fstream = null;
		ArrayList<String> wordList = new ArrayList<String>();
		String line, words[];

		try {
			fstream = new FileInputStream(dirPath + "/" + document);
		} catch (FileNotFoundException e) {
			System.out.println("File does not exist. Set correct path");
		}
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		
		// Checking if word appears in the very beginning of the document.
		// It then checks if the word is appearing around the end of the document
		// The attempt here is to print certain words appearing before the term
		// and certain words appearing after the term.
		if (location < contextWords/2) {
			while((line = br.readLine()) != null && wordCounter < contextWords) {
				words = line.split(whiteSpacePattern);
				if (wordCounter + words.length > contextWords) {
					for (i = 0; wordCounter < contextWords; i++, wordCounter++) 
						wordList.add(words[i]);
				} else {
					for (i = 0; i < words.length; i++)
						wordList.add(words[i]);
					wordCounter+=words.length;
				}
			}
		} else if (location > documentLength.get(document) - contextWords/2) {
			while((line = br.readLine()) != null) {
				words = line.split(whiteSpacePattern);
				if (wordCounter + words.length <= documentLength.get(document) - contextWords)
					wordCounter+=words.length;
				else
					break;
			}
			
			words = line.split(whiteSpacePattern);
			for (i = 0; wordCounter < documentLength.get(document) - contextWords; i++, wordCounter++);
			for (j = i; j < words.length; j++)
				wordList.add(words[j]);
			
			while((line = br.readLine()) != null) {
				words = line.split(whiteSpacePattern);
				for (i = 0; i < words.length; i++)
					wordList.add(words[i]);
			}
		} else {
			while((line = br.readLine()) != null) {
				words = line.split(whiteSpacePattern);
				if (wordCounter + words.length <= location - contextWords/2)
					wordCounter+=words.length;
				else
					break;
			}
			
			words = line.split(whiteSpacePattern);
			for (i = 0; wordCounter < location - contextWords/2; i++, wordCounter++);
			for (j = i; j < words.length && wordCounter < location + contextWords/2; j++, wordCounter++)
				wordList.add(words[j]);
			
			if (wordCounter < location + contextWords/2) {
				while((line = br.readLine()) != null && wordCounter < location + contextWords/2) {
					words = line.split(whiteSpacePattern);
					for (i = 0; wordCounter < location + contextWords/2; i++, wordCounter++)
						wordList.add(words[i]);
				}
			}
		}
		
		for (i = 0; i < wordList.size(); i++)
			System.out.print(wordList.get(i) + " ");
	}
}
