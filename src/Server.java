package src;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/**
 * Server.java — Multi-threaded TCP chat server for the AI Lie Detector system.
 *
 * Architecture:
 *   - ServerSocket accepts connections on a configurable port (default 5000, or $PORT env var)
 *   - Each client is handed off to a ClientHandler thread (thread-per-client model)
 *   - CopyOnWriteArrayList ensures thread-safe broadcast without explicit synchronization
 *   - Can run headlessly on remote servers (e.g., Render, Railway, any VPS)
 *
 * Message format: username|message|timestamp
 *
 * Usage:
 *   java -cp out src.Server              → listens on port 5000
 *   PORT=8080 java -cp out src.Server    → listens on port 8080 (Render-style)
 */
public class Server {

    // ─── Thread-safe client list (CopyOnWriteArrayList: O(1) read, O(n) write) ──
    private static final List<ClientHandler> connectedClients =
        new CopyOnWriteArrayList<>();

    private static final DateTimeFormatter LOG_FMT =
        DateTimeFormatter.ofPattern("HH:mm:ss");

    public static void main(String[] args) {
        // Render / cloud-friendly: read PORT from environment variable
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "5000"));

        log("AI Lie Detector Chat Server starting...");
        log("Port: " + port);
        log("Press Ctrl+C to stop.\n");

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);
            log("Server listening on port " + port + " ✓");
            log("Waiting for clients...\n");

            // Accept loop — runs forever, each client gets its own thread
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(clientSocket, connectedClients);
                    connectedClients.add(handler);

                    Thread clientThread = new Thread(handler);
                    clientThread.setDaemon(true);    // won't block JVM shutdown
                    clientThread.start();

                    log("[+] New client connected from " + clientSocket.getInetAddress().getHostAddress()
                        + "  (total clients: " + connectedClients.size() + ")");

                } catch (IOException e) {
                    log("[ERROR] Failed to accept client: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            log("[FATAL] Could not bind on port " + port + ": " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Broadcast a raw message string to ALL connected clients.
     * Thread-safe because CopyOnWriteArrayList iteration is snapshot-based.
     * Time complexity: O(n) where n = number of connected clients.
     */
    public static void broadcast(String rawMessage, ClientHandler sender) {
        for (ClientHandler client : connectedClients) {
            // Echo back to sender too so they see their own message in other windows
            client.sendMessage(rawMessage);
        }
    }

    /** Remove a disconnected client from the list. */
    public static void removeClient(ClientHandler client) {
        connectedClients.remove(client);
        log("[-] Client removed  (remaining: " + connectedClients.size() + ")");
    }

    /** Server-side console logger with timestamp. */
    public static void log(String message) {
        String time = LocalDateTime.now().format(LOG_FMT);
        System.out.println("[" + time + "] " + message);
    }
}
