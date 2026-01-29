package com.chenweikeng.pim.pin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AlgorithmDebugLogger {
  private static final String LOG_DIR = "pim_debug_logs";
  private static final SimpleDateFormat DATE_FORMAT =
      new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

  private PrintWriter writer;
  private String currentSeries;
  private boolean isEnabled;

  public AlgorithmDebugLogger() {
    this.isEnabled = false;
  }

  public void enable(String seriesName) {
    this.currentSeries = seriesName;
    this.isEnabled = true;
    createLogFile();
  }

  public void disable() {
    this.isEnabled = false;
    if (writer != null) {
      writer.close();
      writer = null;
    }
  }

  private String sanitizeFilename(String seriesName) {
    if (seriesName == null || seriesName.isEmpty()) {
      return "unknown_series";
    }

    // Replace invalid filename characters with underscores
    String sanitized = seriesName.replaceAll("[^a-zA-Z0-9\\s-_]", "_");

    // Remove leading/trailing spaces and underscores
    sanitized = sanitized.trim().replaceAll("^[\\s_]+", "").replaceAll("[\\s_]+$", "");

    // Replace multiple spaces/underscores with single underscore
    sanitized = sanitized.replaceAll("[\\s_]+", "_");

    // Ensure it's not empty and has reasonable length
    if (sanitized.isEmpty()) {
      return "series_" + System.currentTimeMillis();
    }

    // Limit length to avoid filesystem issues
    if (sanitized.length() > 50) {
      sanitized = sanitized.substring(0, 50);
    }

    return sanitized + ".log";
  }

  private void createLogFile() {
    try {
      // Create logs directory in Minecraft directory
      File minecraftDir = new File("."); // Current working directory (Minecraft directory)
      File logDir = new File(minecraftDir, LOG_DIR);
      if (!logDir.exists()) {
        logDir.mkdirs();
      }

      // Create sanitized filename for this series
      String sanitizedFilename = sanitizeFilename(currentSeries);
      File logFile = new File(logDir, sanitizedFilename);

      // Create or overwrite log file for this series
      if (logFile.exists()) {
        logFile.delete();
      }

      writer = new PrintWriter(new FileWriter(logFile));
      writeHeader();

    } catch (IOException e) {
      System.err.println("[Pim] Failed to create debug log file: " + e.getMessage());
      isEnabled = false;
    }
  }

  private void writeHeader() {
    if (writer == null) return;

    writer.println("========================================");
    writer.println("PIM ALGORITHM DEBUG LOG");
    writer.println("Series: " + currentSeries);
    writer.println("Timestamp: " + DATE_FORMAT.format(new Date()));
    writer.println("========================================");
    writer.println();
    writer.flush();
  }

  public void logComputationStep(
      String fromState,
      String toState,
      double probability,
      double contribution,
      double resultValue) {
    if (!isEnabled || writer == null) return;

    writer.println(String.format("[%s] COMPUTATION STEP", DATE_FORMAT.format(new Date())));
    writer.println("  From State: " + fromState);
    writer.println("  To State:   " + toState);
    writer.println("  Probability: " + String.format("%.6f", probability));
    writer.println("  Contribution: " + String.format("%.6f", contribution));
    writer.println("  Result Value: " + String.format("%.6f", resultValue));
    writer.println();
    writer.flush();
  }

  public void logStateTransition(
      String operation, Algorithm.DPState fromState, Algorithm.DPState toState, double value) {
    if (!isEnabled || writer == null) return;

    writer.println(String.format("[%s] STATE TRANSITION", DATE_FORMAT.format(new Date())));
    writer.println("  Operation: " + operation);
    writer.println("  From: " + formatState(fromState));
    writer.println("  To:   " + formatState(toState));
    writer.println("  Value: " + String.format("%.6f", value));
    writer.println();
    writer.flush();
  }

  public void logLayerCompletion(int sumLayer, int statesInLayer) {
    if (!isEnabled || writer == null) return;

    writer.println(String.format("[%s] LAYER COMPLETED", DATE_FORMAT.format(new Date())));
    writer.println("  Sum Layer: " + sumLayer);
    writer.println("  States in Layer: " + statesInLayer);
    writer.println("  ---");
    writer.println();
    writer.flush();
  }

  public void logFinalResult(Algorithm.DPState finalState, double finalValue) {
    if (!isEnabled || writer == null) return;

    writer.println("========================================");
    writer.println("FINAL RESULT");
    writer.println("Final State: " + formatState(finalState));
    writer.println("Final Value: " + String.format("%.6f", finalValue));
    writer.println("========================================");
    writer.println();
    writer.flush();
  }

  public void logError(String errorMessage) {
    if (!isEnabled || writer == null) return;

    writer.println(String.format("[%s] ERROR", DATE_FORMAT.format(new Date())));
    writer.println("  Message: " + errorMessage);
    writer.println();
    writer.flush();
  }

  public void logInfo(String message) {
    if (!isEnabled || writer == null) return;

    writer.println(String.format("[%s] INFO", DATE_FORMAT.format(new Date())));
    writer.println("  " + message);
    writer.println();
    writer.flush();
  }

  private String formatState(Algorithm.DPState state) {
    if (state == null) {
      return "(null)";
    }
    return String.format(
        "(s=%d, d=%d, r=%d, u=%d, c=%d)",
        state.signature, state.deluxe, state.rare, state.uncommon, state.common);
  }

  public boolean isEnabled() {
    return isEnabled;
  }
}
