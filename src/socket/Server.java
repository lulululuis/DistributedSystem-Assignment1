package socket;

import java.io.*;
import java.net.*;
import java.util.*;
import org.json.*;

// Luis, Mauboy, 1684115

public class Server {
	private static Map<String, Set<String>> dictionary = Collections.synchronizedMap(new HashMap<>());
	private static File dictionaryFile;
	private static volatile boolean running = true;

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
		
			while (running) {
				try {
					Socket clientSocket = serverSocket.accept();
					System.out.println("New client connected: " + clientSocket);
			    	new ClientHandler(clientSocket).start();
			    } catch (SocketException ex) {
			    	break;
			    }
			}
			System.out.println("Server shutdown.");
		} catch (IOException ex) {
			 ex.printStackTrace();
		}
	}
	
	
	private static void loadDictionary() {
		if(!dictionaryFile.exists()) {
			System.out.println("Dictionary file not found.");
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
			System.out.println("SUCCESS: Dictionary loaded.");
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
					JSONObject jsonRequest = new JSONObject(request);
					String response = handleRequest(jsonRequest);
					writer.println(response);
					
					if ("EXIT".equalsIgnoreCase(jsonRequest.optString("command"))) {
						break;
					}
				}
			} catch (IOException ex){
			System.out.println("Client disconnected: " + socket);
	    	}
	    }
	    
		private String handleRequest(JSONObject jsonRequest) {
			String command = jsonRequest.optString("command", "").toUpperCase();
			
			switch(command) {
				case "SEARCH":
					return searchWord(jsonRequest);
				case "ADD":
					return addWord(jsonRequest);
				case "REMOVE":
					return removeWord(jsonRequest);
				case "APPEND":
					return addMeaning(jsonRequest);
				case "UPDATE":
					return updateMeaning(jsonRequest);
				case "EXIT":
					running = false;
					try {
						new Socket("localhost", Integer.parseInt(socket.getLocalPort() + "")).close();
					} catch (IOException ex) {
						
					}
					return ("Server is shutting down.");
				default:
					return "ERROR: Unknown Command.";
			}
		}
		
		private String searchWord(JSONObject json) {
			String word = json.optString("word", "").trim().toLowerCase();
			if(word.isEmpty()) return "ERROR: Word required.";
			Set<String> meanings = dictionary.get(word);
			if(meanings == null) return "ERROR: Word not found.";
			return "Meanings: " + String.join(";", meanings);
		}
		
		private String addWord(JSONObject json) {
			String word = json.optString("word", "").trim().toLowerCase();
			String meaning = json.optString("meaning", "").trim();
			if(word.isEmpty()) return "ERROR: Word required.";
			if(meaning.isEmpty()) return "ERROR: Meaning required.";
			if(dictionary.containsKey(word)) return "ERROR: Word already exists.";
			Set<String> meanings = new HashSet<>(Arrays.asList(meaning.split(";")));			
			dictionary.put(word, meanings);
			saveDictionary();
			return "SUCCESS: Word added.";
		}
		
		private String removeWord(JSONObject json) {
			String word = json.optString("word", "").trim().toLowerCase();
			if(word.isEmpty()) return "ERROR: Word required.";
			if(dictionary.remove(word) != null) {
				saveDictionary();
				return "SUCCESS: Word removed.";
			}
			return "ERROR: Word not found.";
		}
		
		private String addMeaning(JSONObject json) {
			String word = json.optString("word", "").trim().toLowerCase();
			String newMeaning = json.optString("meaning", "").trim();
			if(word.isEmpty()) return "ERROR: Word required.";
			if(newMeaning.isEmpty()) return "ERROR: Meaning required.";
			Set<String> meanings = dictionary.get(word);
			if(meanings == null) return "ERROR: Word not found.";
			if(meanings.contains(newMeaning)) return "ERROR: Meaning already exists.";
			meanings.add(newMeaning);
			saveDictionary();
			return "SUCCESS: New meaning added.";
		}
		
		private String updateMeaning(JSONObject json) {
			String word = json.optString("word", "").trim().toLowerCase();
			String exMeaning = json.optString("exMeaning", "").trim();
			String newMeaning = json.optString("newMeaning", "").trim();
			if(word.isEmpty()) return "Word required.";
			if(exMeaning.isEmpty()) return "Existing meaning required.";
			if(newMeaning.isEmpty()) return "New meaning required.";
			Set<String> meanings = dictionary.get(word);
			if(meanings == null) return "ERROR: Word not found.";
			if(!meanings.contains(exMeaning)) return "ERROR: Existing meaning not found.";
			meanings.remove(exMeaning);
			meanings.add(newMeaning);
			saveDictionary();
			return "SUCCESS: Meaning updated.";
		}
	}
}