package src;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Message class - Represents a single chat message.
 * Contains username, text content, timestamp, and suspicion flag.
 */
public class Message {
    private String username;
    private String text;
    private LocalDateTime timestamp;
    private boolean suspicious;
    private String suspicionReason;

    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("hh:mm a");

    public Message(String username, String text, LocalDateTime timestamp) {
        this.username = username;
        this.text = text;
        this.timestamp = timestamp;
        this.suspicious = false;
        this.suspicionReason = "";
    }

    // Getters
    public String getUsername() { return username; }
    public String getText() { return text; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public boolean isSuspicious() { return suspicious; }
    public String getSuspicionReason() { return suspicionReason; }
    public String getFormattedTime() { return timestamp.format(FORMATTER); }

    // Setters
    public void setSuspicious(boolean suspicious) { this.suspicious = suspicious; }
    public void setSuspicionReason(String reason) { this.suspicionReason = reason; }

    @Override
    public String toString() {
        return "[" + username + " @ " + getFormattedTime() + "]: " + text;
    }
}
