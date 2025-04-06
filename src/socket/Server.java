package socket;

import java.io.*;
import java.net.*;

public class Server {
    public static void main(String[] args) {
        int port = 6198;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port " + port);

            Socket socket = serverSocket.accept();
            System.out.println("New client connected");

            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);

            String text;

            while ((text = reader.readLine()) != null) {
                System.out.println("Received: " + text);

                writer.println("Server: " + text);

                if (text.equalsIgnoreCase("end")) {
                    System.out.println("Client disconnected.");
                    break;
                }
            }

            socket.close();

        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}

