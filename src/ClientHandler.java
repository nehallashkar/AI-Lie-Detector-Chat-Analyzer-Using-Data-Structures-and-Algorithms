package src;

import java.io.*;
import java.net.*;
import java.util.List;

/**
 * ClientHandler.java — Per-client server-side thread.
 *
 * Responsibilities:
 *   - Reads incoming messages from a single connected client
 *   - Validates message format ( username|message|timestamp )
 *   - Broadcasts valid messages to all other clients via Server.broadcast()
 *   - Cleans up and notifies server on disconnect
 *
 * Each instance runs on its own thread (started by Server).
 * Thread model: thread-per-client (simple, sufficient for class-scale deployment).
 */
public class ClientHandler implements Runnable {

    private final Socket         socket;
    private       PrintWriter    out;
    private       BufferedReader in;
    private       String         clientAddress;

    public ClientHandler(Socket socket, List<ClientHandler> allClients) {
        this.socket     = socket;
        this.clientAddress = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
    }

    @Override
    public void run() {
        try {
            // Set up streams
            out = new PrintWriter(new BufferedWriter(
                      new OutputStreamWriter(socket.getOutputStream(), "UTF-8")), true);
            in  = new BufferedReader(
                      new InputStreamReader(socket.getInputStream(), "UTF-8"));

            String rawMessage;

            // ── Read loop: O(1) per message, runs until client disconnects ──
            while ((rawMessage = in.readLine()) != null) {
                rawMessage = rawMessage.trim();
                if (rawMessage.isEmpty()) continue;

                // Validate format: must contain at least two '|' separators
                if (!isValidFormat(rawMessage)) {
                    Server.log("[WARN] Invalid message format from " + clientAddress
                        + ": " + rawMessage);
                    continue;
                }

                // Log on server console
                String[] parts = rawMessage.split("\\|", 3);
                Server.log("[MSG] " + parts[0] + ": " + parts[1]);

                // Broadcast to everyone (including sender for echo)
                Server.broadcast(rawMessage, this);
            }

        } catch (IOException e) {
            // Expected on client disconnect — not an error
            Server.log("[INFO] Client " + clientAddress + " disconnected: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    /**
     * Send a raw message string to this client's socket output stream.
     * Called by Server.broadcast() — must be thread-safe.
     * PrintWriter.println() is not inherently synchronized, but since each
     * ClientHandler owns its own PrintWriter and calls here come from a
     * CopyOnWriteArrayList iteration, concurrent writes are safe.
     */
    public synchronized void sendMessage(String rawMessage) {
        if (out != null && !socket.isClosed()) {
            out.println(rawMessage);
        }
    }

    /** Validate that a raw message follows the username|message|timestamp format. */
    private boolean isValidFormat(String raw) {
        String[] parts = raw.split("\\|", -1);
        // Needs exactly 3 parts: username, message, timestamp
        if (parts.length < 3) return false;
        // Username and message must not be blank
        if (parts[0].trim().isEmpty() || parts[1].trim().isEmpty()) return false;
        return true;
    }

    /** Gracefully close streams and socket, remove self from server's client list. */
    private void cleanup() {
        try {
            if (in  != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
        Server.removeClient(this);
    }
}
