package com.chenweikeng.pim.command;

import com.chenweikeng.pim.PimState;
import com.chenweikeng.pim.pin.Algorithm;
import com.chenweikeng.pim.pin.Algorithm.DPResult;
import com.chenweikeng.pim.pin.Algorithm.PinSeriesCounts;
import com.chenweikeng.pim.screen.PinBookHandler;
import com.chenweikeng.pim.screen.PinDetailHandler;
import com.chenweikeng.pim.screen.PinRarityHandler;
import com.chenweikeng.pim.tracker.BossBarTracker;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class PimCommand {

  public static void register() {
    ClientCommandRegistrationCallback.EVENT.register(
        (dispatcher, registryAccess) -> {
          dispatcher.register(
              ClientCommandManager.literal("pim:trade")
                  .executes(
                      context -> {
                        boolean newState = !PimState.isEnabled();
                        PimState.setEnabled(newState);

                        if (newState) {
                          PimState.resetWarpPoint();
                          context
                              .getSource()
                              .sendFeedback(
                                  Component.literal(
                                      "§6✨ §e[Pim] §fUse the IFone to warp to the first pin trader."));
                        } else {
                          BossBarTracker.getInstance().disable();
                          context
                              .getSource()
                              .sendFeedback(
                                  Component.literal("§6✨ §e[Pim] §fPin trading has been stopped."));
                        }
                        return 1;
                      }));

          dispatcher.register(
              ClientCommandManager.literal("pim:reset")
                  .executes(
                      context -> {
                        PinRarityHandler.getInstance().reset();
                        PinBookHandler.getInstance().reset();
                        PinDetailHandler.getInstance().reset();
                        context
                            .getSource()
                            .sendFeedback(
                                Component.literal(
                                    "§6✨ §e[Pim] §fAll pin data has been reset successfully."));
                        return 1;
                      }));

          dispatcher.register(
              ClientCommandManager.literal("pim:compute")
                  .executes(
                      context -> {
                        context
                            .getSource()
                            .sendFeedback(
                                Component.literal(
                                    "§6✨ §e[Pim] §fStarting pin series calculations..."));
                        context
                            .getSource()
                            .sendFeedback(
                                Component.literal(
                                    "§6✨ §e[Pim] §7Debug logs will be saved to: ./pim_debug_logs/"));

                        // Start calculation on separate thread
                        startCalculationThread(context.getSource());
                        return 1;
                      }));
        });
  }

  private static final ExecutorService calculationExecutor =
      Executors.newSingleThreadExecutor(
          new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
              Thread thread = new Thread(r, "Pim-Calculation-" + counter.incrementAndGet());
              thread.setDaemon(true);
              thread.setPriority(Thread.MIN_PRIORITY);
              return thread;
            }
          });

  private static final ConcurrentHashMap<String, Algorithm.DPStartPoint> cachedStartPoints =
      new ConcurrentHashMap<>();

  private static final ConcurrentHashMap<String, DPResult> cachedResults = new ConcurrentHashMap<>();

  private static void startCalculationThread(FabricClientCommandSource source) {
    calculationExecutor.submit(
        () -> {
          try {
            processPinSeriesCalculations(source);
          } catch (Exception e) {
            Minecraft.getInstance()
                .execute(
                    () -> {
                      source.sendFeedback(
                          Component.literal(
                              "§6✨ §e[Pim] §cError during calculation: " + e.getMessage()));
                    });
          }
        });
  }

  private static void processPinSeriesCalculations(FabricClientCommandSource source) {
    // Get all series names from PinRarityHandler
    java.util.Set<String> allSeriesNames = PinRarityHandler.getInstance().getAllSeriesNames();

    if (allSeriesNames.isEmpty()) {
      Minecraft.getInstance()
          .execute(
              () -> {
                source.sendFeedback(
                    Component.literal("§6✨ §e[Pim] §cNo pin series data available."));
              });
      return;
    }

    int totalSeries = 0;

    // Count total valid series first
    for (String seriesName : allSeriesNames) {
      if (isSeriesValidForCalculation(seriesName)) {
        totalSeries++;
      }
    }

    if (totalSeries == 0) {
      Minecraft.getInstance()
          .execute(
              () -> {
                source.sendFeedback(
                    Component.literal(
                        "§6✨ §e[Pim] §cNo complete pin series found for calculation."));
              });
      return;
    }

    final int finalTotalSeries = totalSeries;
    Minecraft.getInstance()
        .execute(
            () -> {
              source.sendFeedback(
                  Component.literal(
                      "§6✨ §e[Pim] §fFound "
                          + finalTotalSeries
                          + " complete series to calculate."));
            });

    // Process each series sequentially
    int processedSeries = 0;
    for (String seriesName : allSeriesNames) {
      final String finalSeriesName = seriesName;

      if (!isSeriesValidForCalculation(finalSeriesName)) {
        continue;
      }

      processedSeries++;

      try {
        // Get pin counts for this series
        PinSeriesCounts counts = getPinSeriesCounts(finalSeriesName);
        if (counts == null) {
          Minecraft.getInstance()
              .execute(
                  () -> {
                    source.sendFeedback(
                        Component.literal(
                            "§6✨ §e[Pim] §cCould not get pin counts for: " + finalSeriesName));
                  });
          continue;
        }

        DPResult result;

        Algorithm.DPStartPoint cachedStartPoint = cachedStartPoints.get(finalSeriesName);

        if (cachedStartPoint != null && cachedStartPoint.equals(counts.startPoint)) {
          result = cachedResults.get(finalSeriesName);
        } else {
          result = Algorithm.runDynamicProgramming(finalSeriesName, counts);
          cachedStartPoints.put(finalSeriesName, counts.startPoint);
          cachedResults.put(finalSeriesName, result);
        }

        final DPResult finalResult = result;

        Minecraft.getInstance()
            .execute(
                () -> {
                  if (finalResult.isSuccess()) {
                    double value = finalResult.value.get();
                    source.sendFeedback(
                        Component.literal(
                            "§6✨ §e[Pim] §a"
                                + finalSeriesName
                                + ": §f"
                                + String.format("%.2f", value)));
                  } else if (finalResult.isError()) {
                    source.sendFeedback(
                        Component.literal(
                            "§6✨ §e[Pim] §cError calculating "
                                + finalSeriesName
                                + ": "
                                + finalResult.error.get()));
                  }
                });

        // Small delay between series to prevent overwhelming the system
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }

      } catch (Exception e) {
        Minecraft.getInstance()
            .execute(
                () -> {
                  source.sendFeedback(
                      Component.literal(
                          "§6✨ §e[Pim] §cError processing "
                              + finalSeriesName
                              + ": "
                              + e.getMessage()));
                });
      }
    }

    final int finalProcessedSeries = processedSeries;
    Minecraft.getInstance()
        .execute(
            () -> {
              source.sendFeedback(
                  Component.literal(
                      "§6✨ §e[Pim] §aCalculation complete! Processed "
                          + finalProcessedSeries
                          + " series."));
            });
  }

  private static boolean isSeriesValidForCalculation(String seriesName) {
    // Check if series has complete information (no blinking condition)
    Map<String, PinDetailHandler.PinDetailEntry> detailMap =
        PinDetailHandler.getInstance().getSeriesDetails(seriesName);

    if (detailMap == null || detailMap.isEmpty()) {
      return false;
    }

    // Get PinBook entry to check totalMints
    PinBookHandler.PinBookEntry bookEntry = PinBookHandler.getInstance().getBookEntry(seriesName);
    if (bookEntry == null) {
      return false;
    }

    // Check if detailMap size matches totalMints (no blinking condition from line 92-94)
    if (detailMap.size() != bookEntry.totalMints) {
      return false;
    }

    // Check if all entries have rarity not null
    for (PinDetailHandler.PinDetailEntry entry : detailMap.values()) {
      if (entry.rarity == null) {
        return false;
      }
    }

    // Check if series is REQUIRED (ignore OPTIONAL series)
    PinRarityHandler.PinSeriesEntry seriesEntry =
        PinRarityHandler.getInstance().getSeriesEntry(seriesName);
    if (seriesEntry == null || seriesEntry.availability != PinRarityHandler.Availability.REQUIRED) {
      return false;
    }

    return true;
  }

  private static PinSeriesCounts getPinSeriesCounts(String seriesName) {
    try {
      // Get series details
      Map<String, PinDetailHandler.PinDetailEntry> detailMap =
          PinDetailHandler.getInstance().getSeriesDetails(seriesName);
      if (detailMap == null || detailMap.isEmpty()) {
        return null;
      }

      // Get book entry for total counts
      PinBookHandler.PinBookEntry bookEntry = PinBookHandler.getInstance().getBookEntry(seriesName);
      if (bookEntry == null) {
        return null;
      }

      // Count pins by rarity
      int signature = 0;
      int deluxe = 0;
      int rare = 0;
      int uncommon = 0;
      int common = 0;

      int mintSignature = 0;
      int mintDeluxe = 0;
      int mintRare = 0;
      int mintUncommon = 0;
      int mintCommon = 0;

      for (Map.Entry<String, PinDetailHandler.PinDetailEntry> entry : detailMap.entrySet()) {
        PinDetailHandler.PinDetailEntry detailEntry = entry.getValue();
        if (detailEntry.rarity == null) {
          continue; // Skip entries without rarity
        }

        switch (detailEntry.rarity) {
          case SIGNATURE:
            signature++;
            if (detailEntry.condition == PinDetailHandler.PinCondition.MINT) {
              mintSignature++;
            }
            break;
          case DELUXE:
            deluxe++;
            if (detailEntry.condition == PinDetailHandler.PinCondition.MINT) {
              mintDeluxe++;
            }
            break;
          case RARE:
            rare++;
            if (detailEntry.condition == PinDetailHandler.PinCondition.MINT) {
              mintRare++;
            }
            break;
          case UNCOMMON:
            uncommon++;
            if (detailEntry.condition == PinDetailHandler.PinCondition.MINT) {
              mintUncommon++;
            }
            break;
          case COMMON:
            common++;
            if (detailEntry.condition == PinDetailHandler.PinCondition.MINT) {
              mintCommon++;
            }
            break;
        }
      }

      // Create goal and start point
      Algorithm.DPGoal goal = new Algorithm.DPGoal(signature, deluxe, rare, uncommon, common);
      Algorithm.DPStartPoint startPoint =
          new Algorithm.DPStartPoint(mintSignature, mintDeluxe, mintRare, mintUncommon, mintCommon);

      return new PinSeriesCounts(goal, startPoint);

    } catch (Exception e) {
      return null;
    }
  }
}
