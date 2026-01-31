package com.chenweikeng.pim.command;

import com.chenweikeng.pim.pin.MagicString;
import com.chenweikeng.pim.pin.MagicStringUtils;
import com.chenweikeng.pim.pin.PinCalculationUtils;
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
      MagicStringUtils.MagicStringParseResult result =
          MagicStringUtils.parseMagicString(magicString);

      if (!result.isValid()) {
        source.sendFeedback(Component.literal("§6✨ §e[Pim] §c" + result.error));
        if (result.error.contains("Checksum mismatch")) {
          source.sendFeedback(
              Component.literal(
                  "§6✨ §e[Pim] §cThe magic string was generated with different pin series metadata!"));
        }
        return;
      }

      Map<String, List<String>> theirPinsBySeries =
          MagicStringUtils.organizePinsBySeries(result.mintPins);

      if (theirPinsBySeries.isEmpty()) {
        source.sendFeedback(Component.literal("§6✨ §e[Pim] §cNo pins found in the magic string!"));
        return;
      }

      // Get player's current pins from PinDetail data (more reliable than inventory)
      Set<String> playerMintPins = MagicString.getPlayerDetailMintPins();
      Map<String, Set<String>> playerPinsBySeries =
          MagicStringUtils.organizePinsBySeriesAsSet(playerMintPins);

      // Find pins that the other user has but you don't have (one-directional match)
      Map<String, List<String>> missingPinsBySeries = new TreeMap<>();
      int totalMissing = 0;

      // First pass: identify missing pins without suggested values
      for (Map.Entry<String, List<String>> seriesEntry : theirPinsBySeries.entrySet()) {
        String seriesName = seriesEntry.getKey();
        List<String> theirPinsInSeries = seriesEntry.getValue();
        Set<String> playerPinsInSeries =
            playerPinsBySeries.getOrDefault(seriesName, new HashSet<>());

        List<String> missingInSeries = new ArrayList<>();

        for (String pinName : theirPinsInSeries) {
          if (!playerPinsInSeries.contains(pinName)) {
            // Check if player has this pin but not in mint condition
            PinDetailHandler.PinDetailEntry playerPinEntry =
                PinDetailHandler.getInstance().findDetailEntry(seriesName, pinName);

            if (playerPinEntry == null
                || playerPinEntry.condition != PinDetailHandler.PinCondition.MINT) {
              missingInSeries.add(pinName);
            }
          }
        }

        if (!missingInSeries.isEmpty()) {
          missingPinsBySeries.put(seriesName, missingInSeries);
          totalMissing += missingInSeries.size();
        }
      }

      // Display results immediately (without suggested values for now)
      if (missingPinsBySeries.isEmpty()) {
        source.sendFeedback(
            Component.literal("§6✨ §e[Pim] §aYou have all the pins that the other user has!"));
      } else {
        // Check if we have cached suggested values for any series
        Map<String, Map<String, Double>> suggestedPricesBySeries = new HashMap<>();

        for (String seriesName : missingPinsBySeries.keySet()) {
          Map<Rarity, Double> raritySuggestedPrices =
              PinCalculationUtils.calculateSuggestedPricesForSeries(seriesName);
          if (!raritySuggestedPrices.isEmpty()) {
            // Map suggested values to individual pins by rarity
            Map<String, Double> pinSuggestedPrices = new HashMap<>();
            for (String pinName : missingPinsBySeries.get(seriesName)) {
              PinDetailHandler.PinDetailEntry pinEntry =
                  PinDetailHandler.getInstance().findDetailEntry(seriesName, pinName);
              if (pinEntry != null && pinEntry.rarity != null) {
                Double suggestedPrice = raritySuggestedPrices.get(pinEntry.rarity);
                if (suggestedPrice != null) {
                  pinSuggestedPrices.put(pinName, suggestedPrice);
                }
              }
            }
            if (!pinSuggestedPrices.isEmpty()) {
              suggestedPricesBySeries.put(seriesName, pinSuggestedPrices);
            }
          }
        }

        // Display results with suggested prices if available
        source.sendFeedback(
            MagicStringUtils.twoLevelTree(
                "§6✨ §e[Pim] §6Missing pins", missingPinsBySeries, suggestedPricesBySeries));

        // Show total at the end
        source.sendFeedback(
            Component.literal(
                "§6✨ §e[Pim] §6Total: "
                    + totalMissing
                    + " pins the other player has that you don't"));
      }

    } catch (Exception e) {
      source.sendFeedback(
          Component.literal("§6✨ §e[Pim] §cError matching magic string: " + e.getMessage()));
    }
  }
}
