# 🔍 AI Lie Detector Chat Analyzer — Network Edition

> **A premium Java Swing desktop application for real-time deception detection in chat conversations — powered by advanced Data Structures & Algorithms and TCP Socket Networking.**

---

## 🎯 Overview

**AI Lie Detector Chat Analyzer** is a multi-client, network-enabled Java Swing desktop application that analyzes chat conversations to detect potential deception, contradictions, and suspicious language patterns in real-time. The application uses multiple DSA structures to produce a detailed **Suspicion Score (0–100%)** for each user seamlessly operating over a central socket server.

---

## 🖥️ Screenshots

The application features a **professional 3-panel dark UI**:
- **Connection Prompt**: Requests User Name, Host, and Port before entering chat.
- **Left**: Message input area with "Send Message" functionality to the TCP server.
- **Center**: WhatsApp-style chat bubble view reflecting live messages sent by anyone in the connected network.
- **Right**: Live analysis report with color-coded scores and suspicious message highlights.

---

## ✨ Features

### 🌐 Real-Time Networking
| Feature | Description |
|---|---|
| **Multi-Threaded Server** | Accepts unlimited TCP clients; handles each via `ClientHandler` thread |
| **Broadcasting Mechanism** | Pushes all messages natively using `CopyOnWriteArrayList` |
| **Cloud-Ready** | Headless server runs easily on Render, AWS, or Railway using the `$PORT` environment variable |

### 🧠 AI Detection Capabilities
| Feature | Description |
|---|---|
| **Keyword Analysis** | Detects high-risk phrases ("trust me", "I swear") and uncertain language |
| **Location/Time Contradiction** | Flags users asserting conflicting whereabouts |
| **Dynamic Execution** | Message analysis triggers automatically when messages are received over the network |

### 💻 UI Highlights
- **Online/Offline Tracking** — Live `●` green indicator reflects connection.
- **Custom Graphics** — WhatsApp-style chat bubbles, smooth progression bars.
- **Smooth Animation** — Dynamic UI updates safely managed via `SwingUtilities.invokeLater()`.

---

## 🧩 Project Structure

```
AI Lie Detector Chat Analyzer/
├── src/
│   ├── Server.java            ← Headless TCP chat server
│   ├── ClientHandler.java     ← Server connection logic
│   ├── ClientConnection.java  ← Client daemon TCP networking 
│   ├── ChatAnalyzer.java      ← DSA engine
│   ├── MainFrame.java         ← Swing UI
│   ├── Message.java           ← Data model
│   └── Main.java              ← Entry point for GUI
├── LieDetector.jar            ← Runnable fat JAR containing GUI & Server
├── run.bat                    ← Windows batch runner script
├── Makefile                   ← Linux/macOS build script
└── README.md                  ← This file
```

---

## 🏗️ DSA Implementation

| Structure | Location | Purpose |
|---|---|---|
| `HashMap<String, List<Message>>` | `ChatAnalyzer` | Groups messages by username |
| `Queue<Message>` | `ChatAnalyzer` | Implements ordered FIFO message global history |
| `Graph / HashSet` | `ChatAnalyzer` | Models interaction connections |
| `CopyOnWriteArrayList` | `Server` | Thread-safe O(1) broadcast iteration array |

---

## 🚀 How to Run

### Step 1: Start the Server (Terminal 1)
```bash
# Option A: Compile and run easily via CL 
javac -d out src/*.java
java -cp out src.Server

# Option B: Run the Server from the JAR
java -cp LieDetector.jar src.Server
```
_The server will run headlessly on port `5000`._

### Step 2: Start the UI Clients (Terminal 2 & 3)
```bash
# Option A: Double-click LieDetector.jar
# Option B (Windows): Run the script
run.bat

# Option C: Use Java JAR runner
java -jar LieDetector.jar
```
_A prompt will appear. Enter a Username, set host to `localhost` and port `5000`._

---

## 🌐 Deploying Server to the Cloud 
The implementation of `System.getenv("PORT")` allows deploying `Server.java` entirely headlessly onto platforms like Heroku/Render natively.

---

> Built as an advanced DSA + Networking project.
