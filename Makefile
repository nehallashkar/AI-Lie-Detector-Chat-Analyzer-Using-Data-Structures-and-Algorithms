# AI Lie Detector Chat Analyzer — Makefile
# Usage: make run | make build | make clean | make jar

SRC_DIR  = src
OUT_DIR  = out
SOURCES  = $(SRC_DIR)/Message.java $(SRC_DIR)/ChatAnalyzer.java $(SRC_DIR)/Server.java $(SRC_DIR)/ClientHandler.java $(SRC_DIR)/ClientConnection.java $(SRC_DIR)/MainFrame.java $(SRC_DIR)/Main.java
MAIN     = src.Main

build:
	@echo "[BUILD] Compiling sources..."
	@mkdir -p $(OUT_DIR)
	@javac -d $(OUT_DIR) $(SOURCES)
	@echo "[BUILD] Done."

run: build
	@echo "[RUN] Launching AI Lie Detector..."
	@java -cp $(OUT_DIR) $(MAIN)

jar: build
	@echo "[JAR] Packaging fat jar..."
	@echo "Main-Class: $(MAIN)" > manifest.txt
	@jar cfm LieDetector.jar manifest.txt -C $(OUT_DIR) .
	@rm manifest.txt
	@echo "[JAR] LieDetector.jar created."

clean:
	@echo "[CLEAN] Removing build artifacts..."
	@rm -rf $(OUT_DIR) LieDetector.jar
	@echo "[CLEAN] Done."
