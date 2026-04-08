package src;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * ClientConnection.java — Client-side networking layer.
 *
 * Responsibilities:
 *   - Connects to the chat server via TCP socket
 *   - Sends messages in the standard format: username|message|timestamp
 *   - Listens for incoming messages on a background daemon thread
 *   - Delivers received messages to the UI via a callback (Consumer<String>)
 *   - Handles reconnection errors gracefully
 *
 * Thread model:
 *   - sendMessage() is called from the Swing EDT (safe: PrintWriter is synchronized)
 *   - A background daemon thread handles incoming message listening (never blocks UI)
 *
 * Message format: username|message|timestamp
 */
public class ClientConnection {

    private static final DateTimeFormatter TS_FMT =
        DateTimeFormatter.ofPattern("hh:mm a");

    private Socket         socket;
    private PrintWriter    out;
    private BufferedReader in;
    private Thread         listenerThread;

    private boolean connected = false;

    /** Callback invoked on the background thread when a message arrives from server. */
    private final Consumer<String> onMessageReceived;

    /** Callback invoked when the connection is lost unexpectedly. */
    private final Runnable onDisconnected;

    // ─── CONSTRUCTOR ─────────────────────────────────────────────────
    public ClientConnection(Consumer<String> onMessageReceived, Runnable onDisconnected) {
        this.onMessageReceived = onMessageReceived;
        this.onDisconnected    = onDisconnected;
    }

    // ─── CONNECT ─────────────────────────────────────────────────────
    /**
     * Attempt to connect to the server at the given host and port.
     * Throws IOException if the server is unreachable (caller shows UI error).
     *
     * @param host server hostname or IP (e.g. "localhost" or "your-app.onrender.com")
     * @param port server port (e.g. 5000)
     */
    public void connect(String host, int port) throws IOException {
        socket = new Socket();
        // 5-second connection timeout — prevents UI freeze on unreachable server
        socket.connect(new InetSocketAddress(host, port), 5000);
        socket.setSoTimeout(0); // no read timeout once connected

        out = new PrintWriter(new BufferedWriter(
                  new OutputStreamWriter(socket.getOutputStream(), "UTF-8")), true);
        in  = new BufferedReader(
                  new InputStreamReader(socket.getInputStream(), "UTF-8"));

        connected = true;
        startListening();
    }

    // ─── SEND MESSAGE ────────────────────────────────────────────────
    /**
     * Send a chat message to the server.
     * Formats it as: username|messageText|timestamp
     * Time complexity: O(1) — single write to socket output stream.
     *
     * @param username  the sender's display name
     * @param text      the message body
     */
    public void sendMessage(String username, String text) {
        if (!connected || out == null) return;
        String timestamp = LocalDateTime.now().format(TS_FMT);
        // Sanitize: replace any pipe chars in text with a dash to protect format
        String safeName = username.replace("|", "-");
        String safeText = text.replace("|", "-");
        String formatted = safeName + "|" + safeText + "|" + timestamp;
        out.println(formatted);
    }

    // ─── LISTEN FOR INCOMING MESSAGES ────────────────────────────────
    /**
     * Starts a background daemon thread that reads lines from the server socket.
     * Each line is a raw message string (username|text|timestamp).
     * Invokes the onMessageReceived callback for each valid line.
     */
    private void startListening() {
        listenerThread = new Thread(() -> {
            try {
                String rawLine;
                while (connected && (rawLine = in.readLine()) != null) {
                    final String msg = rawLine.trim();
                    if (!msg.isEmpty()) {
                        onMessageReceived.accept(msg);   // hand off to UI
                    }
                }
            } catch (IOException e) {
                if (connected) {
                    // Unexpected disconnection
                    connected = false;
                    onDisconnected.run();
                }
            }
        });
        listenerThread.setDaemon(true);  // won't prevent JVM exit
        listenerThread.setName("ChatListener");
        listenerThread.start();
    }

    // ─── DISCONNECT ──────────────────────────────────────────────────
    /**
     * Gracefully close the connection and stop the listener thread.
     */
    public void disconnect() {
        connected = false;
        try {
            if (listenerThread != null) listenerThread.interrupt();
            if (out    != null) out.close();
            if (in     != null) in.close();
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
    }

    // ─── STATUS ──────────────────────────────────────────────────────
    public boolean isConnected() { return connected; }

    // ─── STATIC PARSE HELPER ─────────────────────────────────────────
    /**
     * Parse a raw message string into its 3 components.
     * Format: username|messageText|timestamp
     * Returns null if the format is invalid.
     * O(1) — split on first 2 delimiters only.
     */
    public static String[] parseMessage(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        String[] parts = raw.split("\\|", 3);
        if (parts.length < 3) return null;
        if (parts[0].trim().isEmpty() || parts[1].trim().isEmpty()) return null;
        return parts;   // [0]=username, [1]=text, [2]=timestamp
    }
}
