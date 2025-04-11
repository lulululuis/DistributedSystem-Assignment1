package socket;

import java.io.*;
import java.net.*;
import java.util.*;

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
					Set<String> meanings = new HashSet<>(Arrays.asList(parts[1].split(";")));
					dictionary.put(word, meanings);
				}
			}
			System.out.println("Loaded successfully.");
		} catch(IOException ex) {
			System.out.println("Error loading dictionary: " + ex.getMessage());
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
				default:
					return "Unknown Command.";
			}
		}
		
		private String searchWord(String[] parts) {
			if(parts.length < 2) return "Missing word.";
			String word = parts[1].toLowerCase();
			Set<String> meanings = dictionary.get(word);
			if(meanings == null) return "No word.";
			return "Meaning: " + String.join(";", meanings);
		}
	}
}