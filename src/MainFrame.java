package src;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * MainFrame — Premium dark-themed Java Swing UI for AI Lie Detector Chat Analyzer.
 * Network-Enabled Edition: Uses ClientConnection to talk to a Server.
 * Layout: Left panel (input) | Center panel (chat display) | Right panel (analysis output)
 */
public class MainFrame extends JFrame {

    // ─── COLOR PALETTE ────────────────────────────────────────────────
    private static final Color BG_DEEP      = new Color(10, 12, 20);
    private static final Color BG_PANEL     = new Color(18, 22, 35);
    private static final Color BG_CARD      = new Color(24, 30, 48);
    private static final Color BG_INPUT     = new Color(30, 36, 58);
    private static final Color ACCENT_BLUE  = new Color(64, 156, 255);
    private static final Color ACCENT_CYAN  = new Color(0, 210, 230);
    private static final Color ACCENT_PURP  = new Color(140, 80, 255);
    private static final Color COLOR_RED    = new Color(255, 75, 75);
    private static final Color COLOR_WARN   = new Color(255, 196, 0);
    private static final Color COLOR_GREEN  = new Color(50, 220, 130);
    private static final Color TEXT_PRIMARY = new Color(230, 235, 255);
    private static final Color TEXT_MUTED   = new Color(120, 135, 175);
    private static final Color BORDER_COLOR = new Color(40, 50, 80);
    private static final Color BUBBLE_LEFT  = new Color(28, 36, 65);
    private static final Color BUBBLE_RIGHT = new Color(40, 70, 130);

    // ─── FONTS ────────────────────────────────────────────────────────
    private static final Font FONT_TITLE    = new Font("Segoe UI", Font.BOLD, 20);
    private static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font FONT_LABEL    = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_BODY     = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_SMALL    = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FONT_SCORE    = new Font("Segoe UI", Font.BOLD, 42);
    private static final Font FONT_BTN      = new Font("Segoe UI", Font.BOLD, 13);

    // ─── CORE COMPONENTS ─────────────────────────────────────────────
    private final ChatAnalyzer analyzer = new ChatAnalyzer();
    private ClientConnection clientConnection;
    private String myUsername;

    // Left Panel
    private JTextArea  messageInputArea;
    private JComboBox<String> userCombo;

    // Center Panel
    private JPanel chatPanel;
    private JScrollPane chatScroll;

    // Right Panel
    private JLabel       scoreLabel;
    private JLabel       scoreDesc;
    private JLabel       selectedUserLabel;
    private JProgressBar suspicionBar;
    private JTextPane    analysisPane;
    private JLabel       verdictLabel;
    
    // Status Bar / Top Bar
    private JLabel       liveDot;
    private JLabel       connectionStatusLabel;

    // ─── CONSTRUCTOR ─────────────────────────────────────────────────
    public MainFrame() {
        super("AI Lie Detector Chat Analyzer");
        initializeLookAndFeel();
        buildUI();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 800);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);
        
        // Show connect dialog before revealing main frame
        promptForConnection();
    }

    // ─── NETWORKING INITIALIZATION ────────────────────────────────────
    private void promptForConnection() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.add(new JLabel("Username:"));
        JTextField nameField = new JTextField(System.getProperty("user.name"));
        panel.add(nameField);
        
        panel.add(new JLabel("Server Host:"));
        JTextField hostField = new JTextField("localhost");
        panel.add(hostField);
        
        panel.add(new JLabel("Server Port:"));
        JTextField portField = new JTextField("5000");
        panel.add(portField);

        int result = JOptionPane.showConfirmDialog(null, panel, 
                "Connect to Chat Server", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            myUsername = nameField.getText().trim();
            String host = hostField.getText().trim();
            int port = 5000;
            try {
                port = Integer.parseInt(portField.getText().trim());
            } catch (NumberFormatException ignored) {}

            if (myUsername.isEmpty()) myUsername = "Anonymous";

            clientConnection = new ClientConnection(this::handleIncomingMessage, this::handleDisconnection);
            try {
                clientConnection.connect(host, port);
                updateConnectionStatus(true);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Could not connect to server: " + e.getMessage() + "\nStarting in offline mode.", "Connection Error", JOptionPane.ERROR_MESSAGE);
                updateConnectionStatus(false);
            }
        } else {
            System.exit(0);
        }
        
        setVisible(true);
    }

    private void handleIncomingMessage(String rawMsg) {
        SwingUtilities.invokeLater(() -> {
            String[] parts = ClientConnection.parseMessage(rawMsg);
            if (parts != null) {
                String username = parts[0];
                String text = parts[1];
                // timestamp is parts[2], but analyzer creates its own internal timestamp on add
                
                analyzer.addMessage(username, text);
                
                // Add chat bubble
                addChatBubble(username, text, false); // not yet analyzed
                
                // Auto-analyze network messages
                runAnalysis();
                scrollChatToBottom();
            }
        });
    }

    private void handleDisconnection() {
        SwingUtilities.invokeLater(() -> {
            updateConnectionStatus(false);
            showError("Connection to server lost.");
        });
    }
    
    private void updateConnectionStatus(boolean connected) {
        if (connected) {
            liveDot.setText("● ONLINE");
            liveDot.setForeground(COLOR_GREEN);
            connectionStatusLabel.setText("Connected to Server");
            connectionStatusLabel.setForeground(COLOR_GREEN);
        } else {
            liveDot.setText("○ OFFLINE");
            liveDot.setForeground(COLOR_RED);
            connectionStatusLabel.setText("Disconnected");
            connectionStatusLabel.setForeground(COLOR_RED);
        }
    }

    // ─── LOOK AND FEEL ────────────────────────────────────────────────
    private void initializeLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}
        getContentPane().setBackground(BG_DEEP);
    }

    // ─── BUILD UI ─────────────────────────────────────────────────────
    private void buildUI() {
        setLayout(new BorderLayout(0, 0));
        add(buildTopBar(), BorderLayout.NORTH);

        JPanel mainContent = new JPanel(new GridBagLayout());
        mainContent.setBackground(BG_DEEP);
        mainContent.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill    = GridBagConstraints.BOTH;
        gbc.insets  = new Insets(0, 6, 0, 6);
        gbc.weighty = 1.0;

        // Left Panel (Input) — 25%
        gbc.gridx  = 0;
        gbc.weightx = 0.25;
        mainContent.add(buildLeftPanel(), gbc);

        // Center Panel (Chat) — 45%
        gbc.gridx  = 1;
        gbc.weightx = 0.45;
        mainContent.add(buildCenterPanel(), gbc);

        // Right Panel (Analysis) — 30%
        gbc.gridx  = 2;
        gbc.weightx = 0.30;
        mainContent.add(buildRightPanel(), gbc);

        add(mainContent, BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    // ─── TOP BAR ──────────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(
                    0, 0, new Color(15, 20, 40),
                    getWidth(), 0, new Color(25, 30, 60)
                );
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Bottom accent line
                g2.setColor(ACCENT_BLUE);
                g2.setStroke(new BasicStroke(2f));
                g2.drawLine(0, getHeight()-1, getWidth(), getHeight()-1);
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(0, 64));
        bar.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 24));

        // Icon + Title group
        JPanel titleGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titleGroup.setOpaque(false);

        // Animated logo icon panel
        JPanel logoPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(64, 156, 255, 60));
                g2.fillOval(2, 2, 40, 40);
                GradientPaint gp = new GradientPaint(4, 4, ACCENT_CYAN, 40, 40, ACCENT_PURP);
                g2.setPaint(gp);
                g2.fillOval(4, 4, 36, 36);
                g2.setColor(BG_DEEP);
                g2.fillOval(12, 14, 20, 16);
                g2.setColor(ACCENT_CYAN);
                g2.fillOval(16, 17, 12, 10);
                g2.setColor(BG_DEEP);
                g2.fillOval(19, 19, 6, 6);
            }
        };
        logoPanel.setPreferredSize(new Dimension(44, 44));
        logoPanel.setOpaque(false);

        JPanel textStack = new JPanel();
        textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));
        textStack.setOpaque(false);

        JLabel titleLabel = new JLabel("AI Lie Detector Chat Analyzer");
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(TEXT_PRIMARY);

        JLabel subtitleLabel = new JLabel("Advanced Deception Detection • DSA + Networking Edition");
        subtitleLabel.setFont(FONT_SMALL);
        subtitleLabel.setForeground(TEXT_MUTED);

        textStack.add(titleLabel);
        textStack.add(subtitleLabel);

        titleGroup.add(logoPanel);
        titleGroup.add(textStack);

        // Right side — live indicator
        JPanel indicatorPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        indicatorPanel.setOpaque(false);

        liveDot = new JLabel("○ CONNECTING...");
        liveDot.setFont(new Font("Segoe UI", Font.BOLD, 12));
        liveDot.setForeground(COLOR_WARN);

        JLabel versionLabel = new JLabel("v3.0 Networked");
        versionLabel.setFont(FONT_SMALL);
        versionLabel.setForeground(TEXT_MUTED);

        indicatorPanel.add(versionLabel);
        indicatorPanel.add(liveDot);

        bar.add(titleGroup, BorderLayout.WEST);
        bar.add(indicatorPanel, BorderLayout.EAST);
        return bar;
    }

    // ─── LEFT PANEL (INPUT) ──────────────────────────────────────────
    private JPanel buildLeftPanel() {
        JPanel panel = createCard("💬 Message Input");

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));

        // Message input
        content.add(createFieldLabel("✏️ Message Text"));
        content.add(Box.createVerticalStrut(6));
        messageInputArea = new JTextArea(7, 20);
        messageInputArea.setFont(FONT_BODY);
        messageInputArea.setBackground(BG_INPUT);
        messageInputArea.setForeground(TEXT_PRIMARY);
        messageInputArea.setCaretColor(ACCENT_CYAN);
        messageInputArea.setLineWrap(true);
        messageInputArea.setWrapStyleWord(true);
        messageInputArea.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(BORDER_COLOR, 10, 1),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        messageInputArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        JScrollPane msgScroll = new JScrollPane(messageInputArea);
        msgScroll.setBorder(BorderFactory.createEmptyBorder());
        msgScroll.setBackground(BG_INPUT);
        msgScroll.getViewport().setBackground(BG_INPUT);
        msgScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));
        content.add(msgScroll);
        content.add(Box.createVerticalStrut(16));

        // Add Message Button
        JButton addBtn = createPrimaryButton("📤  Send Message", ACCENT_BLUE);
        addBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        addBtn.addActionListener(e -> onAddMessage());
        content.add(addBtn);
        content.add(Box.createVerticalStrut(12));

        // Analyze Button
        JButton analyzeBtn = createPrimaryButton("🔍  Refresh Analysis", ACCENT_PURP);
        analyzeBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        analyzeBtn.addActionListener(e -> runAnalysis());
        content.add(analyzeBtn);
        content.add(Box.createVerticalStrut(12));

        // Clear Button (Local only)
        JButton clearBtn = createOutlineButton("🗑  Clear Local Chat");
        clearBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        clearBtn.addActionListener(e -> onClear());
        content.add(clearBtn);

        // Quick load presets
        content.add(Box.createVerticalStrut(16));
        content.add(createFieldLabel("⚡ Quick Demo"));
        content.add(Box.createVerticalStrut(6));
        JButton demoBtn = createOutlineButton("Load Demo Conversation");
        demoBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        demoBtn.addActionListener(e -> loadDemoData());
        content.add(demoBtn);

        content.add(Box.createVerticalGlue());

        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    // ─── CENTER PANEL (CHAT DISPLAY) ─────────────────────────────────
    private JPanel buildCenterPanel() {
        JPanel panel = createCard("💬 Conversation View");

        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setBackground(BG_PANEL);
        chatPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Empty state hint
        JLabel emptyHint = new JLabel("<html><center><span style='color:#7888AF;font-size:14px;'>No messages yet.<br>Send messages from the left panel.</span></center></html>");
        emptyHint.setAlignmentX(Component.CENTER_ALIGNMENT);
        emptyHint.setName("emptyHint");
        chatPanel.add(Box.createVerticalGlue());
        chatPanel.add(emptyHint);
        chatPanel.add(Box.createVerticalGlue());

        chatScroll = new JScrollPane(chatPanel);
        chatScroll.setBorder(BorderFactory.createEmptyBorder());
        chatScroll.setBackground(BG_PANEL);
        chatScroll.getViewport().setBackground(BG_PANEL);
        chatScroll.getVerticalScrollBar().setUnitIncrement(16);
        styleScrollBar(chatScroll.getVerticalScrollBar());

        panel.add(chatScroll, BorderLayout.CENTER);
        return panel;
    }

    // ─── RIGHT PANEL (ANALYSIS OUTPUT) ────────────────────────────────
    private JPanel buildRightPanel() {
        JPanel panel = createCard("📊 Analysis Report");

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        // User selector
        content.add(createFieldLabel("🎯 Analyze User"));
        content.add(Box.createVerticalStrut(6));
        userCombo = new JComboBox<>(new String[]{"— run analysis first —"});
        styleComboBox(userCombo);
        userCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        userCombo.addActionListener(e -> updateUserAnalysisView());
        content.add(userCombo);
        content.add(Box.createVerticalStrut(20));

        // Score card
        JPanel scoreCard = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(30, 15, 60), getWidth(), getHeight(), new Color(20, 40, 80));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(new Color(140, 80, 255, 60));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 16, 16);
            }
        };
        scoreCard.setOpaque(false);
        scoreCard.setLayout(new BoxLayout(scoreCard, BoxLayout.Y_AXIS));
        scoreCard.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        scoreCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        selectedUserLabel = new JLabel("No user selected");
        selectedUserLabel.setFont(FONT_LABEL);
        selectedUserLabel.setForeground(TEXT_MUTED);
        selectedUserLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        scoreLabel = new JLabel("--");
        scoreLabel.setFont(FONT_SCORE);
        scoreLabel.setForeground(TEXT_MUTED);
        scoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        scoreDesc = new JLabel("Suspicion Score");
        scoreDesc.setFont(FONT_SMALL);
        scoreDesc.setForeground(TEXT_MUTED);
        scoreDesc.setAlignmentX(Component.CENTER_ALIGNMENT);

        verdictLabel = new JLabel("Awaiting Analysis");
        verdictLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        verdictLabel.setForeground(TEXT_MUTED);
        verdictLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        scoreCard.add(selectedUserLabel);
        scoreCard.add(Box.createVerticalStrut(8));
        scoreCard.add(scoreLabel);
        scoreCard.add(Box.createVerticalStrut(2));
        scoreCard.add(scoreDesc);
        scoreCard.add(Box.createVerticalStrut(8));
        scoreCard.add(verdictLabel);

        content.add(scoreCard);
        content.add(Box.createVerticalStrut(14));

        // Progress bar
        content.add(createFieldLabel("📈 Suspicion Level"));
        content.add(Box.createVerticalStrut(6));
        suspicionBar = new JProgressBar(0, 100);
        suspicionBar.setStringPainted(false);
        suspicionBar.setBackground(BG_INPUT);
        suspicionBar.setForeground(COLOR_GREEN);
        suspicionBar.setBorder(new RoundedBorder(BORDER_COLOR, 8, 1));
        suspicionBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 14));
        suspicionBar.setPreferredSize(new Dimension(0, 14));
        content.add(suspicionBar);
        content.add(Box.createVerticalStrut(20));

        // Analysis detail pane
        content.add(createFieldLabel("🚨 Suspicious Messages"));
        content.add(Box.createVerticalStrut(6));

        analysisPane = new JTextPane();
        analysisPane.setEditable(false);
        analysisPane.setBackground(BG_INPUT);
        analysisPane.setForeground(TEXT_PRIMARY);
        analysisPane.setFont(FONT_SMALL);
        analysisPane.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JScrollPane analysisScroll = new JScrollPane(analysisPane);
        analysisScroll.setBorder(new RoundedBorder(BORDER_COLOR, 10, 1));
        analysisScroll.setBackground(BG_INPUT);
        analysisScroll.getViewport().setBackground(BG_INPUT);
        styleScrollBar(analysisScroll.getVerticalScrollBar());

        content.add(analysisScroll);

        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    // ─── STATUS BAR ───────────────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(12, 16, 30));
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR),
            BorderFactory.createEmptyBorder(6, 20, 6, 20)
        ));
        bar.setPreferredSize(new Dimension(0, 32));

        connectionStatusLabel = new JLabel("Initializing...");
        connectionStatusLabel.setFont(FONT_SMALL);
        connectionStatusLabel.setForeground(TEXT_MUTED);

        JLabel copyright = new JLabel("AI Lie Detector  •  Real-Time DSA Network Edition");
        copyright.setFont(FONT_SMALL);
        copyright.setForeground(TEXT_MUTED);

        bar.add(connectionStatusLabel, BorderLayout.WEST);
        bar.add(copyright, BorderLayout.EAST);
        return bar;
    }

    // ─── ACTION HANDLERS ─────────────────────────────────────────────
    private void onAddMessage() {
        String text = messageInputArea.getText().trim();

        if (text.isEmpty()) {
            showError("Please enter a message!");
            messageInputArea.requestFocus();
            return;
        }
        
        if (clientConnection != null && clientConnection.isConnected()) {
            clientConnection.sendMessage(myUsername, text);
            messageInputArea.setText("");
            messageInputArea.requestFocus();
        } else {
            showError("Not connected to server! Cannot send message.");
        }
    }

    private void runAnalysis() {
        if (analyzer.getUsers().isEmpty()) return;

        // Run analysis
        Map<String, Integer> scores = analyzer.analyze();

        // Refresh chat display with suspicion highlights
        rebuildChatDisplay();

        // Populate user combo by remembering selection
        String previousSelection = (String) userCombo.getSelectedItem();
        boolean previousExists = false;
        
        userCombo.removeAllItems();
        for (String user : scores.keySet()) {
            int sc = scores.get(user);
            String item = user + "  [" + sc + "%]";
            userCombo.addItem(item);
            if (previousSelection != null && previousSelection.startsWith(user + " ")) {
                userCombo.setSelectedItem(item);
                previousExists = true;
            }
        }

        // Select first user if old selection is gone
        if (!previousExists && userCombo.getItemCount() > 0) {
            userCombo.setSelectedIndex(0);
        }
        
        updateUserAnalysisView();
    }

    private void onClear() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Clear all LOCAL messages and reset analysis?",
            "Confirm Clear", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            analyzer.reset();
            rebuildChatDisplay();
            userCombo.removeAllItems();
            userCombo.addItem("— run analysis first —");
            scoreLabel.setText("--");
            scoreLabel.setForeground(TEXT_MUTED);
            scoreDesc.setText("Suspicion Score");
            selectedUserLabel.setText("No user selected");
            verdictLabel.setText("Awaiting Analysis");
            verdictLabel.setForeground(TEXT_MUTED);
            suspicionBar.setValue(0);
            suspicionBar.setForeground(COLOR_GREEN);
            clearAnalysisPane();
        }
    }

    private void updateUserAnalysisView() {
        String selected = (String) userCombo.getSelectedItem();
        if (selected == null || selected.startsWith("—")) return;

        // Extract username (remove score part)
        String user = selected.contains("  [") ? selected.substring(0, selected.indexOf("  [")) : selected;

        Map<String, Integer> scores = analyzer.analyze();
        Integer score = scores.get(user);
        if (score == null) return;

        selectedUserLabel.setText("User: " + user);
        scoreLabel.setText(score + "%");

        // Color and verdict
        Color scoreColor;
        String verdict;
        if (score >= 60) {
            scoreColor = COLOR_RED;
            verdict    = "⚠️  HIGH SUSPICION — Likely Deceptive";
            suspicionBar.setForeground(COLOR_RED);
        } else if (score >= 30) {
            scoreColor = COLOR_WARN;
            verdict    = "🟡  MODERATE — Some Inconsistencies";
            suspicionBar.setForeground(COLOR_WARN);
        } else {
            scoreColor = COLOR_GREEN;
            verdict    = "✅  LOW RISK — Appears Truthful";
            suspicionBar.setForeground(COLOR_GREEN);
        }

        scoreLabel.setForeground(scoreColor);
        verdictLabel.setText(verdict);
        verdictLabel.setForeground(scoreColor);

        // Animate progress bar
        animateProgressBar(score);

        // Populate analysis pane
        populateAnalysisPane(user);
    }

    private void animateProgressBar(int targetValue) {
        javax.swing.Timer timer = new javax.swing.Timer(12, null);
        int[] current = {suspicionBar.getValue()};
        int step = targetValue > current[0] ? 1 : -1;
        timer.addActionListener(e -> {
            current[0] += step;
            suspicionBar.setValue(current[0]);
            if (current[0] == targetValue) ((javax.swing.Timer) e.getSource()).stop();
        });
        timer.start();
    }

    private void populateAnalysisPane(String user) {
        List<Message> msgs = analyzer.getMessagesForUser(user);
        StyledDocument doc = analysisPane.getStyledDocument();

        // Clear
        try { doc.remove(0, doc.getLength()); } catch (BadLocationException ignored) {}

        Style styleRed    = analysisPane.addStyle("red",    null);
        Style styleYellow = analysisPane.addStyle("yellow", null);
        Style styleGray   = analysisPane.addStyle("gray",   null);
        Style styleBold   = analysisPane.addStyle("bold",   null);

        StyleConstants.setForeground(styleRed,    COLOR_RED);
        StyleConstants.setForeground(styleYellow, COLOR_WARN);
        StyleConstants.setForeground(styleGray,   TEXT_MUTED);
        StyleConstants.setForeground(styleBold,   TEXT_PRIMARY);
        StyleConstants.setBold(styleBold, true);

        int suspicious = 0;
        for (Message msg : msgs) {
            if (msg.isSuspicious()) {
                suspicious++;
                try {
                    doc.insertString(doc.getLength(), "🔴 SUSPICIOUS\n", analysisPane.getStyle("red"));
                    doc.insertString(doc.getLength(), "\"" + msg.getText() + "\"\n", analysisPane.getStyle("bold"));
                    doc.insertString(doc.getLength(), "  Reason: " + msg.getSuspicionReason().replace(" | ", "\n  → ") + "\n\n", analysisPane.getStyle("yellow"));
                } catch (BadLocationException ignored) {}
            }
        }

        if (suspicious == 0) {
            try {
                doc.insertString(doc.getLength(), "✅ No suspicious messages detected.\n\nUser appears consistent and truthful based on language analysis.", analysisPane.getStyle("gray"));
            } catch (BadLocationException ignored) {}
        }
    }

    private void clearAnalysisPane() {
        StyledDocument doc = analysisPane.getStyledDocument();
        try { doc.remove(0, doc.getLength()); } catch (BadLocationException ignored) {}
    }

    // ─── CHAT BUBBLE BUILDER ─────────────────────────────────────────
    private final Map<String, Color> userColorMap = new HashMap<>();
    private final Color[] USER_COLORS = {ACCENT_BLUE, ACCENT_CYAN, ACCENT_PURP,
        new Color(255, 120, 80), new Color(80, 220, 180)};
    private int colorIndex = 0;

    private Color getUserColor(String username) {
        return userColorMap.computeIfAbsent(username, k -> USER_COLORS[colorIndex++ % USER_COLORS.length]);
    }

    private void addChatBubble(String username, String text, boolean suspicious) {
        // Remove empty hint on first message
        for (Component c : chatPanel.getComponents()) {
            if (c instanceof JLabel && "emptyHint".equals(c.getName())) {
                chatPanel.remove(c);
                break;
            }
        }
        // For live add (pre-analysis), just append
        appendBubble(username, text, suspicious, null);
    }

    private void appendBubble(String username, String text, boolean suspicious, String reason) {
        Color userColor = getUserColor(username);
        // My messages on the right, others on the left
        boolean isRight = username.equals(myUsername);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        // Bubble panel
        JPanel bubble = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = suspicious ? new Color(80, 15, 15) : (isRight ? BUBBLE_RIGHT : BUBBLE_LEFT);
                g2.setColor(base);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                // Suspicious border glow
                if (suspicious) {
                    g2.setColor(new Color(255, 60, 60, 120));
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 18, 18);
                }
            }
        };
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setOpaque(false);
        bubble.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        // Username label
        JLabel nameLabel = new JLabel(username);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        nameLabel.setForeground(userColor);

        // Message text
        JTextArea msgText = new JTextArea(text);
        msgText.setFont(FONT_BODY);
        msgText.setForeground(TEXT_PRIMARY);
        msgText.setBackground(new Color(0,0,0,0));
        msgText.setOpaque(false);
        msgText.setEditable(false);
        msgText.setLineWrap(true);
        msgText.setWrapStyleWord(true);
        msgText.setFocusable(false);
        msgText.setMaximumSize(new Dimension(380, Integer.MAX_VALUE));

        bubble.add(nameLabel);
        bubble.add(Box.createVerticalStrut(4));
        bubble.add(msgText);

        // Suspicion tag
        if (suspicious && reason != null && !reason.isEmpty()) {
            JLabel tag = new JLabel("⚠ " + truncate(reason, 60));
            tag.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            tag.setForeground(COLOR_WARN);
            bubble.add(Box.createVerticalStrut(4));
            bubble.add(tag);
        }

        // Max width via panel constraint
        JPanel constrainedBubble = new JPanel(new FlowLayout(isRight ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));
        constrainedBubble.setOpaque(false);
        constrainedBubble.add(bubble);

        wrapper.add(constrainedBubble, isRight ? BorderLayout.EAST : BorderLayout.WEST);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, wrapper.getPreferredSize().height + 16));

        chatPanel.add(wrapper);
        chatPanel.revalidate();
        chatPanel.repaint();
    }

    private void rebuildChatDisplay() {
        chatPanel.removeAll();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));

        Queue<Message> queue = analyzer.getMessageQueue();
        if (queue.isEmpty()) {
            JLabel emptyHint = new JLabel("<html><center><span style='color:#7888AF;font-size:14px;'>No messages yet.</span></center></html>");
            emptyHint.setAlignmentX(Component.CENTER_ALIGNMENT);
            emptyHint.setName("emptyHint");
            chatPanel.add(Box.createVerticalGlue());
            chatPanel.add(emptyHint);
            chatPanel.add(Box.createVerticalGlue());
        } else {
            chatPanel.add(Box.createVerticalStrut(8));
            for (Message msg : queue) {
                appendBubble(msg.getUsername(), msg.getText(), msg.isSuspicious(),
                    msg.isSuspicious() ? msg.getSuspicionReason() : null);
            }
            chatPanel.add(Box.createVerticalStrut(8));
        }

        chatPanel.revalidate();
        chatPanel.repaint();
        scrollChatToBottom();
    }

    // ─── DEMO DATA ────────────────────────────────────────────────────
    private void loadDemoData() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Load a demo conversation? This will clear current local messages.",
            "Load Demo", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        analyzer.reset();
        rebuildChatDisplay();
        userColorMap.clear();
        colorIndex = 0;

        String[][] demo = {
            {"Alice", "Hi, I was home all evening, I didn't go anywhere."},
            {"Bob",   "I saw your car at the mall around 6pm though."},
            {"Alice", "Trust me, I was at home. I have nothing to hide."},
            {"Alice", "Well, I mean maybe I went out for a bit, I guess I forgot."},
            {"Bob",   "You said you were home all evening just now!"},
            {"Alice", "I'm serious, believe me. I was at home the whole night."},
            {"Bob",   "I took a photo of your car at the restaurant at midnight."},
            {"Alice", "Okay fine, I might have gone out. I'm not sure anymore."},
            {"Bob",   "This is a contradiction. First home, then mall, now restaurant?"},
            {"Alice", "I swear on my life there's a good explanation."},
        };

        for (String[] entry : demo) {
            analyzer.addMessage(entry[0], entry[1]);
        }

        rebuildChatDisplay();
        runAnalysis();
        JOptionPane.showMessageDialog(this,
            "Demo conversation loaded locally!\nResults are in the right panel.",
            "Demo Loaded", JOptionPane.INFORMATION_MESSAGE);
    }

    // ─── SCROLL HELPERS ───────────────────────────────────────────────
    private void scrollChatToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = chatScroll.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    // ─── UI FACTORY HELPERS ───────────────────────────────────────────
    private JPanel createCard(String title) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(BORDER_COLOR);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 16, 16);
            }
        };
        card.setOpaque(false);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FONT_LABEL);
        titleLabel.setForeground(TEXT_PRIMARY);
        header.add(titleLabel);

        card.add(header, BorderLayout.NORTH);
        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        card.add(body, BorderLayout.CENTER);

        // Return the body panel with the card as parent — actually return card
        // but store body reference for adding content
        card.putClientProperty("bodyPanel", body);

        // Replace CENTER with body
        card.remove(body);
        card.add(body, BorderLayout.CENTER);

        return wrapCard(card, header, body);
    }

    private JPanel wrapCard(JPanel card, JPanel header, JPanel body) {
        // Override add to add to body
        JPanel outer = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(BORDER_COLOR);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 16, 16);
            }
        };
        outer.setOpaque(false);
        outer.add(header, BorderLayout.NORTH);
        outer.add(body, BorderLayout.CENTER);
        return outer;
    }

    private JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_LABEL);
        label.setForeground(TEXT_MUTED);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }



    private JButton createPrimaryButton(String text, Color accent) {
        JButton btn = new JButton(text) {
            private boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c1 = hovered ? accent.brighter() : accent;
                Color c2 = hovered ? accent : accent.darker();
                GradientPaint gp = new GradientPaint(0, 0, c1, 0, getHeight(), c2);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                // Text
                g2.setFont(getFont());
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);
            }
            @Override public void paintBorder(Graphics g) {}
            @Override public boolean isOpaque() { return false; }
        };
        btn.setFont(FONT_BTN);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setPreferredSize(new Dimension(0, 44));
        return btn;
    }

    private JButton createOutlineButton(String text) {
        JButton btn = new JButton(text) {
            private boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hovered ? new Color(40, 50, 80) : BG_INPUT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(hovered ? ACCENT_BLUE : BORDER_COLOR);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 10, 10);
                g2.setFont(getFont());
                g2.setColor(hovered ? ACCENT_BLUE : TEXT_MUTED);
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);
            }
            @Override public void paintBorder(Graphics g) {}
            @Override public boolean isOpaque() { return false; }
        };
        btn.setFont(FONT_BTN);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setPreferredSize(new Dimension(0, 40));
        return btn;
    }

    private void styleComboBox(JComboBox<String> combo) {
        combo.setBackground(BG_INPUT);
        combo.setForeground(TEXT_PRIMARY);
        combo.setFont(FONT_BODY);
        combo.setBorder(new RoundedBorder(BORDER_COLOR, 10, 1));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean selected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, selected, cellHasFocus);
                setBackground(selected ? new Color(40, 55, 90) : BG_INPUT);
                setForeground(TEXT_PRIMARY);
                setFont(FONT_BODY);
                setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
                return this;
            }
        });
    }

    private void styleScrollBar(JScrollBar bar) {
        bar.setBackground(BG_PANEL);
        bar.setOpaque(true);
        bar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor         = new Color(50, 65, 100);
                trackColor         = BG_PANEL;
                thumbHighlightColor= new Color(70, 90, 140);
            }
            @Override protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
            @Override protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
            private JButton createZeroButton() {
                JButton btn = new JButton();
                btn.setPreferredSize(new Dimension(0, 0));
                return btn;
            }
            @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isDragging ? new Color(80, 110, 180) : thumbColor);
                g2.fillRoundRect(r.x+2, r.y+2, r.width-4, r.height-4, 8, 8);
            }
        });
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Input Error", JOptionPane.WARNING_MESSAGE);
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    // ─── CUSTOM BORDER ────────────────────────────────────────────────
    static class RoundedBorder extends AbstractBorder {
        private final Color color;
        private final int radius;
        private final int thickness;

        RoundedBorder(Color color, int radius, int thickness) {
            this.color = color; this.radius = radius; this.thickness = thickness;
        }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            g2.drawRoundRect(x, y, w-1, h-1, radius, radius);
        }
        @Override public Insets getBorderInsets(Component c) { return new Insets(radius/2, radius/2, radius/2, radius/2); }
    }
}
