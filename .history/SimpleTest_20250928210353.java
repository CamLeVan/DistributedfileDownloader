import java.io.*;
import java.net.*;

public class SimpleTest {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 12345);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            // Test GET command step by step
            System.out.println("Sending: GET test.txt 0 264 2");
            out.println("GET test.txt 0 264 2");
            
            // Read first line
            String firstLine = in.readLine();
            System.out.println("First response: " + firstLine);
            
            if (firstLine != null && firstLine.startsWith("ERROR")) {
                System.out.println("Server returned error, stopping test");
                return;
            }
            
            // Try to read more
            String line;
            int count = 0;
            while ((line = in.readLine()) != null && count < 3) {
                System.out.println("Response " + count + ": " + line);
                count++;
            }
            
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
