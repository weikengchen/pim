package com.chenweikeng.pim.command;

import com.chenweikeng.pim.pin.MagicString;
import com.chenweikeng.pim.pin.Rarity;
import com.chenweikeng.pim.screen.PinDetailHandler;
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
import net.minecraft.network.chat.Component;

public class PimMatchCommand {

  /**
   * Calculates fair prices for each rarity type in a series using delta values from the algorithm.
   * Uses the shared PinCalculationUtils for better code reuse and caching. Returns empty map if
   * delta values are not available in cache.
   */
  private static Map<Rarity, Double> calculateFairPricesForSeries(String seriesName) {
    return PinCalculationUtils.calculateFairPricesForSeries(seriesName);
  }

  /**
   * Creates a two-level tree display with fair prices shown inline when available. This is a
   * simpler version that shows prices directly in the text.
   */
  private static Component twoLevelTreeWithHover(
      String title, Map<String, List<String>> data, Map<String, Map<String, Double>> fairPrices) {
    // Just use the existing twoLevelTree method - it already handles fair prices inline
    return twoLevelTree(title, data, fairPrices);
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
    // Create a mutable component to build the result
    net.minecraft.network.chat.MutableComponent tree = Component.literal(title + ":\n");

    List<Map.Entry<String, List<String>>> entries = new ArrayList<>(data.entrySet());
    int totalGroups = entries.size();

    if (totalGroups == 0) {
      return tree.append(Component.literal("(none)\n"));
    }

    for (int i = 0; i < totalGroups; i++) {
      Map.Entry<String, List<String>> entry = entries.get(i);
      boolean lastGroup = (i == totalGroups - 1);
      String groupPrefix = lastGroup ? "└─ " : "├─ ";

      // Add series name
      tree.append(Component.literal(groupPrefix + entry.getKey() + "\n"));

      List<String> children = entry.getValue();
      Map<String, Double> seriesFairPrices = fairPrices.get(entry.getKey());

      for (int j = 0; j < children.size(); j++) {
        boolean lastChild = (j == children.size() - 1);
        String childPrefix = (lastGroup ? "   " : "│  ") + (lastChild ? "└─ " : "├─ ");

        String pinName = children.get(j);

        // Create pin name component with hover text for fair price
        net.minecraft.network.chat.MutableComponent pinComponent;
        if (seriesFairPrices != null && seriesFairPrices.containsKey(pinName)) {
          double price = seriesFairPrices.get(pinName);
          String priceText = "Suggested price: §6" + formatPrice(price);

          // Create component with hover text - pin name without inline price
          pinComponent = Component.literal(childPrefix + pinName);

          // Add hover event using HoverEvent.ShowText as requested
          pinComponent.withStyle(
              style ->
                  style.withHoverEvent(
                      new net.minecraft.network.chat.HoverEvent.ShowText(
                          Component.literal(priceText))));
        } else {
          // No fair price available, just show pin name
          pinComponent = Component.literal(childPrefix + pinName);
        }

        tree.append(pinComponent).append(Component.literal("\n"));
      }
    }

    return tree;
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
    try {
      // Validate the magic string format
      if (!magicString.startsWith("Pim!")) {
        source.sendFeedback(
            Component.literal("§6✨ §e[Pim] §cInvalid magic string format! Must start with 'Pim!'"));
        return;
      }

      // Extract checksum and bitmap
      if (magicString.length() < 12) { // "Pim!" + 8 chars checksum + at least 1 char bitmap
        source.sendFeedback(Component.literal("§6✨ §e[Pim] §cInvalid magic string length!"));
        return;
      }

      String expectedChecksum = MagicString.generateChecksum();
      String providedChecksum = magicString.substring(4, 12); // Extract 8-char checksum

      // Verify checksum
      if (!expectedChecksum.equals(providedChecksum)) {
        source.sendFeedback(Component.literal("§6✨ §e[Pim] §cChecksum mismatch!"));
        source.sendFeedback(
            Component.literal(
                "§6✨ §e[Pim] §7Expected: " + expectedChecksum + ", Got: " + providedChecksum));
        source.sendFeedback(
            Component.literal(
                "§6✨ §e[Pim] §cThe magic string was generated with different pin series metadata!"));
        return;
      }

      // Extract and decode bitmap
      String base58Bitmap = magicString.substring(12);
      Set<String> theirMintPins;
      try {
        theirMintPins = MagicString.decodeBitmap(base58Bitmap);
      } catch (Exception e) {
        source.sendFeedback(
            Component.literal("§6✨ §e[Pim] §cFailed to decode bitmap: " + e.getMessage()));
        return;
      }

      // Convert to HashMap structure: HashMap<SeriesName, HashSet<PinName>>
      Map<String, Set<String>> theirPinsBySeries = new TreeMap<>();
      for (String pinKey : theirMintPins) {
        String[] parts = pinKey.split(":");
        if (parts.length == 2) {
          String seriesName = parts[0];
          String pinName = parts[1];
          theirPinsBySeries.computeIfAbsent(seriesName, k -> new HashSet<>()).add(pinName);
        }
      }

      if (theirPinsBySeries.isEmpty()) {
        source.sendFeedback(Component.literal("§6✨ §e[Pim] §cNo pins found in the magic string!"));
        return;
      }

      // Get player's current pins from PinDetail data (more reliable than inventory)
      Set<String> playerMintPins = MagicString.getPlayerDetailMintPins();
      Map<String, Set<String>> playerPinsBySeries = new TreeMap<>();
      for (String pinKey : playerMintPins) {
        String[] parts = pinKey.split(":");
        if (parts.length == 2) {
          String seriesName = parts[0];
          String pinName = parts[1];
          playerPinsBySeries.computeIfAbsent(seriesName, k -> new HashSet<>()).add(pinName);
        }
      }

      // Find pins that the other user has but you don't have (one-directional match)
      Map<String, List<String>> missingPinsBySeries = new TreeMap<>();
      int totalMissing = 0;

      // First pass: identify missing pins without fair prices
      for (Map.Entry<String, Set<String>> seriesEntry : theirPinsBySeries.entrySet()) {
        String seriesName = seriesEntry.getKey();
        Set<String> theirPinsInSeries = seriesEntry.getValue();
        Set<String> playerPinsInSeries =
            playerPinsBySeries.getOrDefault(seriesName, new HashSet<>());

        List<String> missingInSeries = new ArrayList<>();

        for (String pinName : theirPinsInSeries) {
          if (!playerPinsInSeries.contains(pinName)) {
            // Check if player has this pin but not in mint condition
            PinDetailHandler.PinDetailEntry playerPinEntry =
                PinDetailHandler.getInstance().findDetailEntry(seriesName, pinName);

            if (playerPinEntry == null
                || (playerPinEntry.condition != PinDetailHandler.PinCondition.MINT
                    && playerPinEntry.condition != PinDetailHandler.PinCondition.LOCKED)) {
              missingInSeries.add(pinName);
            }
          }
        }

        if (!missingInSeries.isEmpty()) {
          missingPinsBySeries.put(seriesName, missingInSeries);
          totalMissing += missingInSeries.size();
        }
      }

      // Display results immediately (without fair prices for now)
      if (missingPinsBySeries.isEmpty()) {
        source.sendFeedback(
            Component.literal("§6✨ §e[Pim] §aYou have all the pins that the other user has!"));
      } else {
        // Check if we have cached fair prices for any series
        boolean hasAnyCachedPrices = false;
        Map<String, Map<String, Double>> fairPricesBySeries = new HashMap<>();

        for (String seriesName : missingPinsBySeries.keySet()) {
          Map<Rarity, Double> rarityFairPrices = calculateFairPricesForSeries(seriesName);
          if (!rarityFairPrices.isEmpty()) {
            hasAnyCachedPrices = true;
            // Map fair prices to individual pins by rarity
            Map<String, Double> pinFairPrices = new HashMap<>();
            for (String pinName : missingPinsBySeries.get(seriesName)) {
              PinDetailHandler.PinDetailEntry pinEntry =
                  PinDetailHandler.getInstance().findDetailEntry(seriesName, pinName);
              if (pinEntry != null && pinEntry.rarity != null) {
                Double fairPrice = rarityFairPrices.get(pinEntry.rarity);
                if (fairPrice != null) {
                  pinFairPrices.put(pinName, fairPrice);
                }
              }
            }
            if (!pinFairPrices.isEmpty()) {
              fairPricesBySeries.put(seriesName, pinFairPrices);
            }
          }
        }

        // Display results with fair prices as hover text if available
        source.sendFeedback(
            twoLevelTreeWithHover(
                "§6✨ §e[Pim] §6Missing pins", missingPinsBySeries, fairPricesBySeries));

        // Show total at the end
        source.sendFeedback(
            Component.literal(
                "§6✨ §e[Pim] §6Total: " + totalMissing + " pins they have that you don't"));

        // Show guidance if no cached prices available
        if (!hasAnyCachedPrices && !missingPinsBySeries.isEmpty()) {
          source.sendFeedback(
              Component.literal("§6✨ §e[Pim] §7Run §6/pim:compute §7to see suggested prices"));
        }
      }

    } catch (Exception e) {
      source.sendFeedback(
          Component.literal("§6✨ §e[Pim] §cError matching magic string: " + e.getMessage()));
    }
  }
}
