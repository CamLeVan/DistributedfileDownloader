import java.io.*;
import java.net.*;

public class DebugClient2 {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 12345);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            // Test GET_SIZE first
            System.out.println("Testing GET_SIZE...");
            out.println("GET_SIZE test.txt");
            String sizeResponse = in.readLine();
            System.out.println("Size response: " + sizeResponse);
            
            // Test GET command
            System.out.println("\nTesting GET command...");
            out.println("GET test.txt 0 264 2");
            
            // Read response
            String line;
            int count = 0;
            while ((line = in.readLine()) != null && count < 3) {
                System.out.println("Response " + count + ": " + line);
                count++;
            }
            
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
