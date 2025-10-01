package com.example.distributed;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Comprehensive Java class demonstrating distributed computing concepts
 * This class implements various patterns for distributed systems
 */
public class DistributedComputingExample {
    
    private static final int DEFAULT_PORT = 8080;
    private static final int THREAD_POOL_SIZE = 10;
    private ExecutorService executorService;
    private List<ServerSocket> serverSockets;
    private Map<String, Object> sharedData;
    
    /**
     * Constructor initializes the distributed computing environment
     */
    public DistributedComputingExample() {
        this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        this.serverSockets = new ArrayList<>();
        this.sharedData = new ConcurrentHashMap<>();
    }
    
    /**
     * Starts multiple server instances for distributed processing
     */
    public void startDistributedServers(int numberOfServers) {
        System.out.println("Starting " + numberOfServers + " distributed servers...");
        
        for (int i = 0; i < numberOfServers; i++) {
            final int serverId = i;
            executorService.submit(() -> {
                try {
                    ServerSocket serverSocket = new ServerSocket(DEFAULT_PORT + serverId);
                    serverSockets.add(serverSocket);
                    System.out.println("Server " + serverId + " started on port " + (DEFAULT_PORT + serverId));
                    
                    while (!Thread.currentThread().isInterrupted()) {
                        Socket clientSocket = serverSocket.accept();
                        handleClient(clientSocket, serverId);
                    }
                } catch (IOException e) {
                    System.err.println("Error starting server " + serverId + ": " + e.getMessage());
                }
            });
        }
    }
    
    /**
     * Handles client connections and processes distributed tasks
     */
    private void handleClient(Socket clientSocket, int serverId) {
        executorService.submit(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {
                
                String request = reader.readLine();
                System.out.println("Server " + serverId + " received: " + request);
                
                // Process different types of requests
                String response = processRequest(request, serverId);
                writer.println(response);
                
          System.err.println("Error handling client on server " + serverId + ": " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Error closing client socket: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Processes various types of distributed computing requests
     */
    private String processRequest(String request, int serverId) {
        String[] parts = request.split(" ");
        String command = parts[0];
        
        switch (command) {
            case "COMPUTE":
                return performComputation(parts, serverId);
            case "STORE":
                return storeData(parts, serverId);
            case "RETRIEVE":
                return retrieveData(parts, serverId);
            case "SYNC":
                return synchronizeData(parts, serverId);
            case "HEALTH":
                return "Server " + serverId + " is healthy";
            default:
                return "Unknown command: " + command;
        }
    }
    
    /**
     * Performs computational tasks in a distributed manner
     */
    private String performComputation(String[] parts, int serverId) {
        if (parts.length < 3) {
            return "ERROR: Invalid computation request";
        }
        
        String operation = parts[1];
        String data = parts[2];
        
        try {
            double result = 0;
            switch (operation) {
                case "SUM":
                    result = Arrays.stream(data.split(","))
                            .mapToDouble(Double::parseDouble)
                            .sum();
                    break;
                case "AVERAGE":
                    result = Arrays.stream(data.split(","))
                            .mapToDouble(Double::parseDouble)
                            .average()
                            .orElse(0.0);
                    break;
                case "MAX":
                    result = Arrays.stream(data.split(","))
                            .mapToDouble(Double::parseDouble)
                            .max()
                            .orElse(0.0);
                    break;
                case "MIN":
                    result = Arrays.stream(data.split(","))
                            .mapToDouble(Double::parseDouble)
                            .min()
                            .orElse(0.0);
                    break;
                default:
                    return "ERROR: Unknown operation " + operation;
            }
            
            return "RESULT:" + result + ":Server" + serverId;
            {
            return "ERROR: Invalid numeric data";
        }
    }
    
    /**
     * Stores data in the distributed system
     */
    private String storeData(String[] parts, int serverId) {
        if (parts.length < 3) {
            return "ERROR: Invalid store request";
        }
        
        String key = parts[1];
        String value = parts[2];
        
        sharedData.put(key, value);
        System.out.println("Server " + serverId + " stored: " + key + " = " + value);
        
        return "STORED:" + key + ":Server" + serverId;
    }
    
    /**
     * Retrieves data from the distributed system
     */
    private String retrieveData(String[] parts, int serverId) {
        if (parts.length < 2) {
            return "ERROR: Invalid retrieve request";
        }
        
        String key = parts[1];
        Object value = sharedData.get(key);
        
        if (value != null) {
            return "RETRIEVED:" + key + ":" + value + ":Server" + serverId;
        } else {
            return "NOT_FOUND:" + key + ":Server" + serverId;
        }
    }
    
    /**
     * Synchronizes data across distributed servers
     */
    private String synchronizeData(String[] parts, int serverId) {
        // Implement data synchronization logic here
        return "SYNC_COMPLETE:Server" + serverId;
    }
    
    /**
     * Shuts down the distributed computing system
     */
    public void shutdown() {
        System.out.println("Shutting down distributed computing system...");
        
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        for (ServerSocket socket : serverSockets) {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Error closing server socket: " + e.getMessage());
            }
        }
        
        System.out.println("Distributed computing system shutdown complete");
    }
    
    /**
     * Main method for testing the distributed computing system
     */
    public static void main(String[] args) {
        DistributedComputingExample system = new DistributedComputingExample();
        
        // Start distributed servers
        system.startDistributedServers(3);
        
        // Keep the system running
        try {
            Thread.sleep(60000); // Run for 1 minute
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
     