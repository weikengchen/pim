package com.chenweikeng.pim.command;

import com.chenweikeng.pim.pin.Algorithm;
import com.chenweikeng.pim.pin.MagicString;
import com.chenweikeng.pim.pin.Rarity;
import com.chenweikeng.pim.screen.PinDetailHandler;
import com.chenweikeng.pim.screen.PinRarityHandler;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class PimMatchCommand {

  /**
   * Calculates fair prices for each rarity type in a series using delta values from the algorithm.
   * Fair price = (delta * pinbox_price) / 2 Returns empty map if delta values are not available in
   * cache.
   */
  private static Map<Rarity, Double> calculateFairPricesForSeries(String seriesName) {
    Map<Rarity, Double> fairPrices = new HashMap<>();

    // Get series entry to find pinbox price
    PinRarityHandler.PinSeriesEntry seriesEntry =
        PinRarityHandler.getInstance().getSeriesEntry(seriesName);
    if (seriesEntry == null || seriesEntry.color == null) {
      return fairPrices; // Empty map if no price data
    }

    double pinboxPrice = seriesEntry.color.price;

    // Try to get cached algorithm result with delta values
    // For now, we'll need to call the algorithm to get the delta values
    // In the future, these could be cached separately
    try {
      // Get pin counts for this series (similar to PimComputeCommand)
      // We need to simulate the player's current state vs goal state
      Map<String, PinDetailHandler.PinDetailEntry> seriesDetails =
          PinDetailHandler.getInstance().getSeriesDetails(seriesName);
      if (seriesDetails == null || seriesDetails.isEmpty()) {
        return fairPrices; // Empty map if no series details
      }

      // Count pins by rarity in the series to establish goal
      int goalSignature = 0, goalDeluxe = 0, goalRare = 0, goalUncommon = 0, goalCommon = 0;
      for (PinDetailHandler.PinDetailEntry entry : seriesDetails.values()) {
        if (entry.rarity != null) {
          switch (entry.rarity) {
            case SIGNATURE:
              goalSignature++;
              break;
            case DELUXE:
              goalDeluxe++;
              break;
            case RARE:
              goalRare++;
              break;
            case UNCOMMON:
              goalUncommon++;
              break;
            case COMMON:
              goalCommon++;
              break;
          }
        }
      }

      // Get player's current pin counts for this series
      Set<String> playerMintPins = MagicString.getPlayerMintPins();
      int playerSignature = 0,
          playerDeluxe = 0,
          playerRare = 0,
          playerUncommon = 0,
          playerCommon = 0;

      for (String pinKey : playerMintPins) {
        String[] parts = pinKey.split(":");
        if (parts.length == 2 && parts[0].equals(seriesName)) {
          String pinName = parts[1];
          PinDetailHandler.PinDetailEntry pinEntry = seriesDetails.get(pinName);
          if (pinEntry != null && pinEntry.rarity != null) {
            switch (pinEntry.rarity) {
              case SIGNATURE:
                playerSignature++;
                break;
              case DELUXE:
                playerDeluxe++;
                break;
              case RARE:
                playerRare++;
                break;
              case UNCOMMON:
                playerUncommon++;
                break;
              case COMMON:
                playerCommon++;
                break;
            }
          }
        }
      }

      // Create PinSeriesCounts for the algorithm
      Algorithm.DPStartPoint startPoint =
          new Algorithm.DPStartPoint(
              playerSignature, playerDeluxe, playerRare, playerUncommon, playerCommon);
      Algorithm.DPGoal goal =
          new Algorithm.DPGoal(goalSignature, goalDeluxe, goalRare, goalUncommon, goalCommon);
      Algorithm.PinSeriesCounts counts = new Algorithm.PinSeriesCounts(goal, startPoint);

      // Run algorithm to get delta values
      Algorithm.DPResult result = Algorithm.runDynamicProgramming(seriesName, counts);

      if (result.isSuccess()) {
        // Calculate fair prices for each rarity type where delta is available
        if (result.whatIfOneMoreSignature.isPresent()) {
          fairPrices.put(
              Rarity.SIGNATURE, (result.whatIfOneMoreSignature.get() * pinboxPrice) / 2.0);
        }
        if (result.whatIfOneMoreDeluxe.isPresent()) {
          fairPrices.put(Rarity.DELUXE, (result.whatIfOneMoreDeluxe.get() * pinboxPrice) / 2.0);
        }
        if (result.whatIfOneMoreRare.isPresent()) {
          fairPrices.put(Rarity.RARE, (result.whatIfOneMoreRare.get() * pinboxPrice) / 2.0);
        }
        if (result.whatIfOneMoreUncommon.isPresent()) {
          fairPrices.put(Rarity.UNCOMMON, (result.whatIfOneMoreUncommon.get() * pinboxPrice) / 2.0);
        }
        if (result.whatIfOneMoreCommon.isPresent()) {
          fairPrices.put(Rarity.COMMON, (result.whatIfOneMoreCommon.get() * pinboxPrice) / 2.0);
        }
      }
    } catch (Exception e) {
      // If algorithm fails or cache is not available, return empty map
      // This follows the requirement: "if the cache is not available, then do not calculate the pin
      // price for this series"
    }

    return fairPrices;
  }

  /** Formats a price value into a human-readable string (e.g., "1.2K", "500"). */
  private static String formatPrice(double price) {
    if (price >= 1000) {
      return String.format("%.1fK", price / 1000.0);
    } else {
      return String.format("%.0f", price);
    }
  }

  private static Component twoLevelTree(
      String title, Map<String, List<String>> data, Map<String, Map<String, Double>> fairPrices) {
    StringBuilder tree = new StringBuilder(title + ":\n");

    List<Map.Entry<String, List<String>>> entries = new ArrayList<>(data.entrySet());
    int totalGroups = entries.size();

    if (totalGroups == 0) {
      tree.append("(none)\n");
      return Component.literal(tree.toString());
    }

    for (int i = 0; i < totalGroups; i++) {
      Map.Entry<String, List<String>> entry = entries.get(i);
      boolean lastGroup = (i == totalGroups - 1);
      String groupPrefix = lastGroup ? "└─ " : "├─ ";

      tree.append(groupPrefix).append(entry.getKey()).append("\n");

      List<String> children = entry.getValue();
      Map<String, Double> seriesFairPrices = fairPrices.get(entry.getKey());

      for (int j = 0; j < children.size(); j++) {
        boolean lastChild = (j == children.size() - 1);
        String childPrefix = (lastGroup ? "   " : "│  ") + (lastChild ? "└─ " : "├─ ");

        String pinName = children.get(j);
        tree.append(childPrefix).append(pinName);

        // Add fair price if available
        if (seriesFairPrices != null && seriesFairPrices.containsKey(pinName)) {
          double price = seriesFairPrices.get(pinName);
          tree.append(" §7(≈").append(formatPrice(price)).append(")");
        }

        tree.append("\n");
      }
    }

    return Component.literal(tree.toString());
  }

  public static void register(
      com.mojang.brigadier.CommandDispatcher<FabricClientCommandSource> dispatcher) {
    dispatcher.register(
        ClientCommandManager.literal("pim:match")
            .then(
                ClientCommandManager.argument("magicString", StringArgumentType.greedyString())
                    .executes(
                        context -> {
                          String magicString = StringArgumentType.getString(context, "magicString");
                          matchMagicString(context.getSource(), magicString);
                          return 1;
                        }))
            .executes(
                context -> {
                  context
                      .getSource()
                      .sendFeedback(
                          Component.literal("§6✨ §e[Pim] §cUsage: /pim:match <magic_string>"));
                  return 1;
                }));
  }

  private static void matchMagicString(FabricClientCommandSource source, String magicString) {
    Minecraft.getInstance()
        .execute(
            () -> {
              try {
                // Validate the magic string format
                if (!magicString.startsWith("Pim!")) {
                  source.sendFeedback(
                      Component.literal(
                          "§6✨ §e[Pim] §cInvalid magic string format! Must start with 'Pim!'"));
                  return;
                }

                // Extract checksum and bitmap
                if (magicString.length()
                    < 12) { // "Pim!" + 8 chars checksum + at least 1 char bitmap
                  source.sendFeedback(
                      Component.literal("§6✨ §e[Pim] §cInvalid magic string length!"));
                  return;
                }

                String expectedChecksum = MagicString.generateChecksum();
                String providedChecksum = magicString.substring(4, 12); // Extract 8-char checksum

                // Verify checksum
                if (!expectedChecksum.equals(providedChecksum)) {
                  source.sendFeedback(Component.literal("§6✨ §e[Pim] §cChecksum mismatch!"));
                  source.sendFeedback(
                      Component.literal(
                          "§6✨ §e[Pim] §7Expected: "
                              + expectedChecksum
                              + ", Got: "
                              + providedChecksum));
                  source.sendFeedback(
                      Component.literal(
                          "§6✨ §e[Pim] §cThe magic string was generated with different pin series metadata!"));
                  return;
                }

                source.sendFeedback(
                    Component.literal("§6✨ §e[Pim] §aChecksum matches! Analyzing pins..."));

                // Extract and decode bitmap
                String base58Bitmap = magicString.substring(12);
                Set<String> theirMintPins;
                try {
                  theirMintPins = MagicString.decodeBitmap(base58Bitmap);
                } catch (Exception e) {
                  source.sendFeedback(
                      Component.literal(
                          "§6✨ §e[Pim] §cFailed to decode bitmap: " + e.getMessage()));
                  return;
                }

                // Convert to HashMap structure: HashMap<SeriesName, HashSet<PinName>>
                Map<String, Set<String>> theirPinsBySeries = new TreeMap<>();
                for (String pinKey : theirMintPins) {
                  String[] parts = pinKey.split(":");
                  if (parts.length == 2) {
                    String seriesName = parts[0];
                    String pinName = parts[1];
                    theirPinsBySeries
                        .computeIfAbsent(seriesName, k -> new HashSet<>())
                        .add(pinName);
                  }
                }

                if (theirPinsBySeries.isEmpty()) {
                  source.sendFeedback(
                      Component.literal("§6✨ §e[Pim] §cNo pins found in the magic string!"));
                  return;
                }

                // Get player's current pins
                Set<String> playerMintPins = MagicString.getPlayerMintPins();
                Map<String, Set<String>> playerPinsBySeries = new TreeMap<>();
                for (String pinKey : playerMintPins) {
                  String[] parts = pinKey.split(":");
                  if (parts.length == 2) {
                    String seriesName = parts[0];
                    String pinName = parts[1];
                    playerPinsBySeries
                        .computeIfAbsent(seriesName, k -> new HashSet<>())
                        .add(pinName);
                  }
                }

                // Find pins that the other user has but you don't have (one-directional match)
                // and calculate fair prices for each missing pin
                Map<String, List<String>> missingPinsBySeries = new TreeMap<>();
                Map<String, Map<String, Double>> fairPricesBySeries =
                    new TreeMap<>(); // series -> pin -> fair price
                int totalMissing = 0;

                for (Map.Entry<String, Set<String>> seriesEntry : theirPinsBySeries.entrySet()) {
                  String seriesName = seriesEntry.getKey();
                  Set<String> theirPinsInSeries = seriesEntry.getValue();
                  Set<String> playerPinsInSeries =
                      playerPinsBySeries.getOrDefault(seriesName, new HashSet<>());

                  List<String> missingInSeries = new ArrayList<>();
                  Map<String, Double> fairPricesInSeries = new HashMap<>();

                  // Calculate fair prices for this series if possible
                  Map<Rarity, Double> rarityFairPrices = calculateFairPricesForSeries(seriesName);

                  for (String pinName : theirPinsInSeries) {
                    if (!playerPinsInSeries.contains(pinName)) {
                      // Check if player has this pin but not in mint condition
                      PinDetailHandler.PinDetailEntry playerPinEntry =
                          PinDetailHandler.getInstance().findDetailEntry(seriesName, pinName);

                      if (playerPinEntry == null
                          || (playerPinEntry.condition != PinDetailHandler.PinCondition.MINT
                              && playerPinEntry.condition
                                  != PinDetailHandler.PinCondition.LOCKED)) {
                        missingInSeries.add(pinName);

                        // Calculate fair price for this pin if possible
                        if (!rarityFairPrices.isEmpty()) {
                          // Get pin rarity from pin details
                          PinDetailHandler.PinDetailEntry pinEntry =
                              PinDetailHandler.getInstance().findDetailEntry(seriesName, pinName);
                          if (pinEntry != null && pinEntry.rarity != null) {
                            Double fairPrice = rarityFairPrices.get(pinEntry.rarity);
                            if (fairPrice != null) {
                              fairPricesInSeries.put(pinName, fairPrice);
                            }
                          }
                        }
                      }
                    }
                  }

                  if (!missingInSeries.isEmpty()) {
                    missingPinsBySeries.put(seriesName, missingInSeries);
                    if (!fairPricesInSeries.isEmpty()) {
                      fairPricesBySeries.put(seriesName, fairPricesInSeries);
                    }
                    totalMissing += missingInSeries.size();
                  }
                }

                // Display results
                if (missingPinsBySeries.isEmpty()) {
                  source.sendFeedback(
                      Component.literal(
                          "§6✨ §e[Pim] §aYou have all the pins that the other user has!"));
                } else {
                  // Display results in tree format with fair prices
                  source.sendFeedback(
                      twoLevelTree(
                          "§6✨ §e[Pim] §6Missing pins", missingPinsBySeries, fairPricesBySeries));

                  // Show total at the end
                  source.sendFeedback(
                      Component.literal(
                          "§6✨ §e[Pim] §6Total: "
                              + totalMissing
                              + " pins they have that you don't"));
                }

              } catch (Exception e) {
                source.sendFeedback(
                    Component.literal(
                        "§6✨ §e[Pim] §cError matching magic string: " + e.getMessage()));
              }
            });
  }
}
