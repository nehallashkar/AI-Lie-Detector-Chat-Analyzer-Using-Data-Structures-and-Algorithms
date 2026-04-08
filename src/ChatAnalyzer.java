package src;

import java.time.LocalDateTime;
import java.util.*;

/**
 * ChatAnalyzer class - Core DSA + AI lie detection engine.
 *
 * DSA Used:
 *   - HashMap<String, List<Message>> : stores messages per user     (O(1) avg lookup)
 *   - Queue (LinkedList)             : maintains insertion order     (FIFO)
 *   - Graph (Adjacency List)         : user interaction graph       (O(V+E) traversal)
 *
 * Algorithms:
 *   - Keyword analysis               : O(n * k) where k = keyword count ≈ O(n)
 *   - Contradiction detection        : O(n) optimized (brute-force O(n²) noted in comments)
 *   - Suspicion score computation    : O(n)
 */
public class ChatAnalyzer {

    // ─── DSA STRUCTURES ────────────────────────────────────────────────
    /** HashMap: username → list of Message objects (O(1) average access) */
    private final HashMap<String, List<Message>> userMessages;

    /** Queue: preserves insertion order of ALL messages across users */
    private final Queue<Message> messageQueue;

    /** Graph: adjacency list — who talked after whom (user interaction) */
    private final HashMap<String, Set<String>> interactionGraph;

    /** Quick look-up: username → suspicion score */
    private final HashMap<String, Integer> suspicionScores;

    // ─── SUSPICIOUS KEYWORDS ───────────────────────────────────────────
    private static final List<String> HIGH_RISK_KEYWORDS = Arrays.asList(
        "i swear", "trust me", "honestly", "i promise", "believe me",
        "i would never", "i have nothing to hide", "to be honest",
        "not lying", "i'm serious", "i didn't do it", "i wasn't there",
        "you have to believe", "i guarantee", "on my life", "i'm telling the truth"
    );

    private static final List<String> MEDIUM_RISK_KEYWORDS = Arrays.asList(
        "maybe", "perhaps", "i think", "i guess", "sort of", "kind of",
        "i don't remember", "i forgot", "not sure", "can't recall",
        "i might have", "possibly", "probably"
    );

    private static final List<String> LOCATION_KEYWORDS = Arrays.asList(
        "home", "office", "work", "gym", "park", "mall", "school",
        "restaurant", "hospital", "abroad", "library", "friend's place",
        "here", "there", "nowhere", "somewhere"
    );

    private static final List<String> TIME_KEYWORDS = Arrays.asList(
        "morning", "afternoon", "evening", "night", "midnight",
        "yesterday", "today", "last night", "early", "late",
        "few hours ago", "just now", "earlier", "at noon", "at dawn"
    );

    // ─── CONSTRUCTOR ───────────────────────────────────────────────────
    public ChatAnalyzer() {
        userMessages      = new HashMap<>();
        messageQueue      = new LinkedList<>();
        interactionGraph  = new HashMap<>();
        suspicionScores   = new HashMap<>();
    }

    // ─── ADD MESSAGE ───────────────────────────────────────────────────
    /**
     * Add a message to the DSA structures.
     * Time Complexity: O(1) for HashMap + Queue operations.
     */
    public void addMessage(String username, String text) {
        Message msg = new Message(username, text, LocalDateTime.now());

        // HashMap: group by user — O(1)
        userMessages.computeIfAbsent(username, k -> new ArrayList<>()).add(msg);

        // Queue: global ordered list — O(1)
        messageQueue.offer(msg);

        // Graph: link sender → previous sender — O(1)
        updateGraph(username);
    }

    /**
     * Builds/updates the adjacency list interaction graph.
     * Adds edge from previous speaker → current speaker.
     */
    private void updateGraph(String currentUser) {
        interactionGraph.computeIfAbsent(currentUser, k -> new HashSet<>());

        // Get last user from queue (previous message sender)
        List<Message> allMessages = new ArrayList<>(messageQueue);
        if (allMessages.size() >= 2) {
            String prevUser = allMessages.get(allMessages.size() - 2).getUsername();
            if (!prevUser.equals(currentUser)) {
                interactionGraph.get(prevUser).add(currentUser);
            }
        }
    }

    // ─── MAIN ANALYSIS ENGINE ──────────────────────────────────────────
    /**
     * Analyze ALL messages and compute suspicion scores.
     *
     * OPTIMIZED — O(n): single pass over message queue + O(m) keyword checks.
     * BRUTE-FORCE NOTE: A naive contradiction check would compare every pair
     * of messages → O(n²). This optimized approach uses HashMap lookups → O(n).
     *
     * @return Map of username → suspicion score (0–100)
     */
    public Map<String, Integer> analyze() {
        suspicionScores.clear();

        // Reset all suspicion flags
        for (Message msg : messageQueue) {
            msg.setSuspicious(false);
            msg.setSuspicionReason("");
        }

        // ── STEP 1: Per-user analysis — O(n) total across all users ───
        for (Map.Entry<String, List<Message>> entry : userMessages.entrySet()) {
            String user   = entry.getKey();
            List<Message> msgs = entry.getValue();

            int score = 0;
            score += analyzeKeywords(msgs);       // keyword scoring
            score += detectContradictions(msgs);  // contradiction detection
            score += analyzeFrequency(msgs);      // rapid-fire messaging penalty

            // Clamp score to [0, 100]
            score = Math.min(100, Math.max(0, score));
            suspicionScores.put(user, score);
        }

        return Collections.unmodifiableMap(suspicionScores);
    }

    // ─── KEYWORD ANALYSIS — O(n * k) ≈ O(n) ──────────────────────────
    /**
     * Score messages based on suspicious keyword presence.
     * k (keyword count) is constant, so overall complexity is O(n).
     */
    private int analyzeKeywords(List<Message> msgs) {
        int score = 0;
        for (Message msg : msgs) {
            String lower = msg.getText().toLowerCase();
            StringBuilder reasons = new StringBuilder();
            int msgScore = 0;

            // High-risk keywords: +15 each
            for (String kw : HIGH_RISK_KEYWORDS) {
                if (lower.contains(kw)) {
                    msgScore += 15;
                    reasons.append("High-risk phrase: \"").append(kw).append("\" | ");
                }
            }

            // Medium-risk: +8 each
            for (String kw : MEDIUM_RISK_KEYWORDS) {
                if (lower.contains(kw)) {
                    msgScore += 8;
                    reasons.append("Uncertain language: \"").append(kw).append("\" | ");
                }
            }

            if (msgScore > 0) {
                msg.setSuspicious(true);
                if (msg.getSuspicionReason().isEmpty()) {
                    msg.setSuspicionReason(reasons.toString().trim());
                } else {
                    msg.setSuspicionReason(msg.getSuspicionReason() + " | " + reasons.toString().trim());
                }
            }
            score += msgScore;
        }
        return score;
    }

    // ─── CONTRADICTION DETECTION — O(n) OPTIMIZED ─────────────────────
    /**
     * Detects location and time contradictions within a user's messages.
     *
     * OPTIMIZED O(n): Uses HashMap to record first-seen location/time per user.
     *   When a conflict is found, mark and continue — single pass.
     *
     * BRUTE-FORCE O(n²) EQUIVALENT (for reference):
     *   for i in range(n):
     *     for j in range(i+1, n):
     *       if conflicts(msgs[i], msgs[j]): flag both;
     */
    private int detectContradictions(List<Message> msgs) {
        int score = 0;

        // Track first-seen location and time per user in this pass — O(1) each
        HashMap<String, String> seenLocations = new HashMap<>();
        HashMap<String, String> seenTimes     = new HashMap<>();

        for (Message msg : msgs) {
            String lower = msg.getText().toLowerCase();

            // ── Location conflict detection ──
            for (String loc : LOCATION_KEYWORDS) {
                if (lower.contains(loc)) {
                    if (seenLocations.containsKey("location")) {
                        String prev = seenLocations.get("location");
                        if (!prev.equals(loc)) {
                            // CONFLICT: said they were in two different places
                            score += 25;
                            msg.setSuspicious(true);
                            msg.setSuspicionReason(msg.getSuspicionReason() +
                                " | Location conflict: said \"" + prev + "\" before, now \"" + loc + "\"");
                        }
                    } else {
                        seenLocations.put("location", loc);
                    }
                    break;
                }
            }

            // ── Time conflict detection ──
            for (String t : TIME_KEYWORDS) {
                if (lower.contains(t)) {
                    if (seenTimes.containsKey("time")) {
                        String prev = seenTimes.get("time");
                        if (!prev.equals(t)) {
                            score += 20;
                            msg.setSuspicious(true);
                            msg.setSuspicionReason(msg.getSuspicionReason() +
                                " | Time conflict: said \"" + prev + "\" before, now \"" + t + "\"");
                        }
                    } else {
                        seenTimes.put("time", t);
                    }
                    break;
                }
            }
        }
        return score;
    }

    // ─── FREQUENCY ANALYSIS — O(n) ────────────────────────────────────
    /**
     * Penalizes users who send many rapid messages (evasive/nervous behavior).
     * Time complexity: O(n) single pass.
     */
    private int analyzeFrequency(List<Message> msgs) {
        if (msgs.size() > 8)  return 10;
        if (msgs.size() > 5)  return 5;
        return 0;
    }

    // ─── PUBLIC GETTERS ───────────────────────────────────────────────
    public Map<String, List<Message>> getUserMessages()  { return userMessages; }
    public Queue<Message> getMessageQueue()              { return messageQueue; }
    public Map<String, Set<String>> getInteractionGraph(){ return interactionGraph; }
    public List<Message> getMessagesForUser(String user) {
        return userMessages.getOrDefault(user, Collections.emptyList());
    }
    public List<String> getUsers() { return new ArrayList<>(userMessages.keySet()); }

    public void reset() {
        userMessages.clear();
        messageQueue.clear();
        interactionGraph.clear();
        suspicionScores.clear();
    }
}
