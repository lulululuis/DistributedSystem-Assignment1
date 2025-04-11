package socket;

import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Client extends JFrame{
	private Socket socket;
	private PrintWriter writer;
	private BufferedReader reader;
	private JTextField wordField;
	private JTextArea responseArea;
	
	
	public Client(String serverAddress, int serverPort) {
		setTitle("Dictionary");
		setSize(600, 500);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLayout(new BorderLayout());
		
		JPanel inputPanel = new JPanel(new GridLayout(4, 2, 5, 5));
		inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		inputPanel.add(new JLabel("Word: "));
		wordField = new JTextField();
		inputPanel.add(wordField);
		
		add(inputPanel, BorderLayout.NORTH);
		
		responseArea = new JTextArea();
		responseArea.setEditable(false);
		add(new JScrollPane(responseArea), BorderLayout.CENTER);
		
		JPanel buttonPanel = new JPanel(new GridLayout(1, 5, 5, 5));
		
		buttonPanel.add(createButton("Search", e -> sendRequest("SEARCH|" + wordField.getText())));
		
		add(buttonPanel, BorderLayout.SOUTH);
		
		try {
			socket = new Socket(serverAddress, serverPort);
			writer = new PrintWriter(socket.getOutputStream(), true);
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			appendResponse("Connected to server" + serverAddress + ":" + serverPort);
		} catch (IOException ex) {
			showError("Unable to connect: " + ex.getMessage());
		}
		
	}
	
	private JButton createButton(String text, ActionListener listener) {
		JButton button = new JButton(text);
		button.addActionListener(listener);
		return button;
	}
	
	private void sendRequest(String request) {
		if(socket == null || socket.isClosed()) {
			showError("Connection lost.");
			return;
		}
		
		try {
			writer.println(request);
			String response = reader.readLine();
			appendResponse("Server: " + response);
		} catch (IOException ex) {
			showError("Error communicating with server: " + ex.getMessage());
		}
	}
	
	private void appendResponse(String message) {
		responseArea.append(message + "\n");
	}
	
	private void showError(String message) {
		JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
		appendResponse("Error: " + message);
	}
	
	
    public static void main(String[] args) {
    	if(args.length != 2) {
    		JOptionPane.showMessageDialog(null, "Usage: java -jar Client.jar <server-address> <server-port>", "Usage", JOptionPane.WARNING_MESSAGE);
    		return;
    	}
    	
        String serverAddress = args[0];
        int serverPort = Integer.parseInt(args[1]);
        
        SwingUtilities.invokeLater(() -> {
        	Client client = new Client(serverAddress, serverPort);
        	client.setVisible(true);
        });
       
    }
}

