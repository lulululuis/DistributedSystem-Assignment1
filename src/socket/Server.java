package socket;

import java.io.*;
import java.net.*;
import java.util.*;

// Luis, Mauboy, 1684115

public class Server {
	private static Map<String, Set<String>> dictionary = Collections.synchronizedMap(new HashMap<>());
	private static File dictionaryFile;

	public static void main(String[] args) {
	 	if(args.length != 2) {
	 		System.out.println("Usage: java -jar Server.jar <port> <dictionary-file>");
	 		return;
	 	}
		
		
	    int port = Integer.parseInt(args[0]);
	    dictionaryFile = new File(args[1]);
	    
	    loadDictionary();
	    
		try (ServerSocket serverSocket = new ServerSocket(port)) {
		    System.out.println("Server is running on port " + port);
		
			while (true) {
				Socket socket = serverSocket.accept();
				System.out.println("New client connected: " + socket);
			    	new ClientHandler(socket).start();
			    }		   
			} catch (IOException ex) {
			    System.out.println("Server exception: " + ex.getMessage());
			        ex.printStackTrace();
			    }
		}
	
	
	private static void loadDictionary() {
		if(!dictionaryFile.exists()) {
			System.out.println("File not found.");
			return;
		}
		
		try(BufferedReader reader = new BufferedReader(new FileReader(dictionaryFile))) {
			String line;
			while((line = reader.readLine()) != null) {
				String[] parts = line.split(":");
				if(parts.length == 2) {
					String word = parts[0].trim().toLowerCase();
					Set<String> meaning = new HashSet<>(Arrays.asList(parts[1].split(";")));
					dictionary.put(word, meaning);
				}
			}
			System.out.println("Loaded successfully.");
		} catch(IOException ex) {
			System.out.println("Error loading dictionary: " + ex.getMessage());
		}
	}
	
	private static synchronized void saveDictionary() {
		try(PrintWriter writer = new PrintWriter(new FileWriter(dictionaryFile))){
			for(Map.Entry<String, Set<String>> entry : dictionary.entrySet()) {
				writer.println(entry.getKey() + ":" + String.join(";", entry.getValue()));
			}
		} catch(IOException ex){
			System.out.println("Error saving dictionary" + ex.getMessage());
		}
	}
	
	private static class ClientHandler extends Thread{
		private Socket socket;
		
		public ClientHandler(Socket socket) {
			this.socket = socket;
		}
		
		public void run() {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)
							){
				String request;
				while ((request = reader.readLine()) != null){
					String response = handleRequest(request.trim());
					writer.println(response);
				}
			} catch (IOException ex){
			System.out.println("Client disconnected: " + socket);
	    	}
	    }
	    
		private String handleRequest(String request) {
			String[] parts = request.split("\\|", -1);
			String command = parts[0].toUpperCase();
			
			switch(command) {
				case "SEARCH":
					return searchWord(parts);
				case "ADD":
					return addWord(parts);
				case "REMOVE":
					return removeWord(parts);
				case "APPEND":
					return addMeaning(parts);
				case "UPDATE":
					return updateMeaning(parts);
				default:
					return "Unknown Command.";
			}
		}
		
		private String searchWord(String[] parts) {
			if(parts.length < 2 || parts[1].trim().isEmpty()) return "Word required.";
			String word = parts[1].toLowerCase();
			Set<String> meaning = dictionary.get(word);
			if(meaning == null) return "No word.";
			return "Meaning: " + String.join(";", meaning);
		}
		
		private String addWord(String[] parts) {
			if(parts.length < 3 || parts[1].trim().isEmpty()) return "Word required.";
			if(parts.length < 3 || parts[2].trim().isEmpty()) return "Meaning required.";
			String word = parts[1].toLowerCase();
			if(dictionary.containsKey(word)) return "Duplicate: Word already exists.";
			Set<String> meaning = new HashSet<>(Arrays.asList(parts[2].split(";")));
			dictionary.put(word, meaning);
			saveDictionary();
			return "SUCCESS: Word added.";
		}
		
		private String removeWord(String[] parts) {
			if(parts.length < 2 || parts[1].trim().isEmpty()) return "Word required.";
			String word = parts[1].toLowerCase();
			if(dictionary.remove(word) != null) {
				saveDictionary();
				return "Word removed.";
			}
			return "Word not found.";
		}
		
		private String addMeaning(String[] parts) {
			if(parts.length < 3 || parts[1].trim().isEmpty()) return "Word required.";
			if(parts.length < 3 || parts[2].trim().isEmpty()) return "Meaning required.";
			String word = parts[1].toLowerCase();
			String newMeaning = parts[2];
			Set<String> meaning = dictionary.get(word);
			if(meaning == null) return "Word not found.";
			if(meaning.contains(newMeaning)) return "Meaning already exists.";
			meaning.add(newMeaning);
			saveDictionary();
			return "SUCCESS: New meaning added.";
		}
		
		private String updateMeaning(String[] parts) {
			if(parts.length < 4 || parts[1].trim().isEmpty()) return "Word required.";
			if(parts.length < 4 || parts[2].trim().isEmpty()) return "Existing meaning required.";
			if(parts.length < 4 || parts[3].trim().isEmpty()) return "New meaning required.";
			String word = parts[1].toLowerCase();
			String exMeaning = parts[2];
			String newMeaning = parts[3];
			Set<String> meaning = dictionary.get(word);
			if(meaning == null) return "Word not found.";
			if(!meaning.contains(exMeaning)) return "Existing meaning not found.";
			meaning.remove(exMeaning);
			meaning.add(newMeaning);
			saveDictionary();
			return "SUCCESS: Meaning updated.";
		}
	}
}