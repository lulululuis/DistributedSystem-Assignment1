package socket;

import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import org.json.JSONObject;

// Luis Mauboy, 1684115

public class DictionaryClient extends JFrame{
	private Socket socket;
	private PrintWriter writer;
	private BufferedReader reader;
	private JTextField wordField, meaningField, exMeaningField, newMeaningField;
	private JTextArea responseArea;
	
	
	public DictionaryClient(String serverAddress, int serverPort) {
		setTitle("Dictionary");
		setSize(800, 600);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLayout(new BorderLayout());
		
		JPanel inputPanel = new JPanel(new GridLayout(4, 2, 5, 5));
		inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		inputPanel.add(new JLabel("Word "));
		wordField = new JTextField();
		inputPanel.add(wordField);
		
		inputPanel.add(new JLabel("Meaning (For Adding)"));
		meaningField = new JTextField();
		inputPanel.add(meaningField);
		
		inputPanel.add(new JLabel("Existing Meaning (For Updating)"));
		exMeaningField = new JTextField();
		inputPanel.add(exMeaningField);
		
		inputPanel.add(new JLabel("New meaning (For Updating)"));
		newMeaningField = new JTextField();
		inputPanel.add(newMeaningField);

		add(inputPanel, BorderLayout.NORTH);
		
		responseArea = new JTextArea();
		responseArea.setEditable(false);
		add(new JScrollPane(responseArea), BorderLayout.CENTER);
		
		JPanel buttonPanel = new JPanel(new GridLayout(1, 6, 5, 5));
		
		buttonPanel.add(createButton("Search Word", e -> sendRequest("SEARCH")));
		buttonPanel.add(createButton("Add Word", e -> sendRequest("ADD")));
		buttonPanel.add(createButton("Remove Word", e -> sendRequest("REMOVE")));
		buttonPanel.add(createButton("Add Meaning", e -> sendRequest("APPEND")));
		buttonPanel.add(createButton("Update Meaning", e -> sendRequest("UPDATE")));
		buttonPanel.add(createButton("Shutdown", e -> sendRequest("EXIT")));
		
		add(buttonPanel, BorderLayout.SOUTH);
		
		connectRetryServer(serverAddress, serverPort);
	}
		
	private JButton createButton(String text, ActionListener listener) {
		JButton button = new JButton(text);
		button.addActionListener(listener);
		return button;
	}
	
	private void connectRetryServer(String serverAddress, int serverPort) {
		int attempts = 0;
		while (attempts < 3) {
			try {
				socket = new Socket(serverAddress, serverPort);
				writer = new PrintWriter(socket.getOutputStream(), true);
				reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				appendResponse("Connected to server: " + serverAddress + ":" + serverPort);
				return;
			} catch (IOException ex) {
				attempts++;
				appendResponse(attempts + " failed connection attempt(s): " + ex.getMessage());
				try {
					Thread.sleep(3000);
				} catch (InterruptedException ignored) {
					
				}
			}
		}
		
		showError("Unable to connect to server after 3 attempts.");
		System.exit(1);
	}
	
	private void sendRequest(String command) {
		if(socket == null || socket.isClosed()) {
			showError("Connection lost.");
			return;
		}
		
		try {
			JSONObject request = new JSONObject();
			request.put("command", command);
			
			String word = wordField.getText().trim();
			String meaning = meaningField.getText().trim();
			String exMeaning = exMeaningField.getText().trim();
			String newMeaning = newMeaningField.getText().trim();
			
			if(!word.isEmpty()) request.put("word", word);
			if(!meaning.isEmpty()) request.put("meaning", meaning);
			if(!exMeaning.isEmpty()) request.put("exMeaning", exMeaning);
			if(!newMeaning.isEmpty()) request.put("newMeaning", newMeaning);
			
			writer.println(request.toString());
			String response = reader.readLine();
			appendResponse(response);
			
			if("EXIT".equalsIgnoreCase(command)) {
				appendResponse("Client shutting down after exit command.");
				System.exit(0);
			}
		} catch (IOException ex) {
			showError("Error communicating with server: " + ex.getMessage());
		}
	}
	
	private void appendResponse(String message) {
		responseArea.append(message + "\n");
	}
	
	private void showError(String message) {
		JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
		appendResponse("ERROR: " + message);
	}
	
	
    public static void main(String[] args) {
    	if(args.length != 2) {
    		JOptionPane.showMessageDialog(null, "Usage: java -jar DictionaryClient.jar <server-address> <server-port>", "Usage", JOptionPane.WARNING_MESSAGE);
    		System.exit(1);
    		return;
    	}
    	
        String serverAddress = args[0];
        int serverPort = Integer.parseInt(args[1]);
        
        SwingUtilities.invokeLater(() -> {
        	DictionaryClient client = new DictionaryClient(serverAddress, serverPort);
        	client.setVisible(true);
        });
    }
}

