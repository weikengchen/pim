package com.chenweikeng.pim.command;

import com.chenweikeng.pim.pin.PinCalculationUtils;
import com.chenweikeng.pim.pin.Rarity;
import com.chenweikeng.pim.screen.PinRarityHandler;
import java.util.Map;
import java.util.TreeMap;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class PimPriceCommand {

  public static void register(
      com.mojang.brigadier.CommandDispatcher<FabricClientCommandSource> dispatcher) {
    dispatcher.register(
        ClientCommandManager.literal("pim:price")
            .executes(
                context -> {
                  displayPrices(context.getSource());
                  return 1;
                }));
  }

  private static void displayPrices(FabricClientCommandSource source) {
    source.sendFeedback(
        Component.literal("§6✨ §e[Pim] §6Suggested Values for REQUIRED Pin Series"));

    Map<String, Map<Rarity, Double>> allPrices = new TreeMap<>();

    for (String seriesName : PinRarityHandler.getInstance().getAllSeriesNames()) {
      PinRarityHandler.PinSeriesEntry seriesEntry =
          PinRarityHandler.getInstance().getSeriesEntry(seriesName);

      if (seriesEntry != null
          && seriesEntry.availability == PinRarityHandler.Availability.REQUIRED) {
        Map<Rarity, Double> prices =
            PinCalculationUtils.calculateSuggestedPricesForSeries(seriesName);
        if (!prices.isEmpty()) {
          allPrices.put(seriesName, prices);
        }
      }
    }

    if (allPrices.isEmpty()) {
      source.sendFeedback(
          Component.literal(
              "§6✨ §e[Pim] §cNo suggested prices available. Please ensure you have pin series data."));
      return;
    }

    for (Map.Entry<String, Map<Rarity, Double>> seriesEntry : allPrices.entrySet()) {
      String seriesName = seriesEntry.getKey();
      Map<Rarity, Double> prices = seriesEntry.getValue();

      MutableComponent seriesLine =
          Component.literal("§e").append(Component.literal(seriesName + ":"));

      boolean hasAnyPrice = false;
      for (Rarity rarity : Rarity.values()) {
        Double price = prices.get(rarity);
        if (price != null) {
          seriesLine
              .append(
                  Component.literal(
                      " §f" + rarity.name().toLowerCase() + "§7=§a" + formatPrice(price)))
              .append(Component.literal("§7,"));
          hasAnyPrice = true;
        }
      }

      if (hasAnyPrice) {
        String seriesText = seriesLine.getString();
        if (seriesText.endsWith(",")) {
          seriesLine =
              Component.literal(
                  seriesText.substring(0, seriesText.length() - 1)); // Remove trailing comma
        }
        source.sendFeedback(seriesLine);
      }
    }

    source.sendFeedback(
        Component.literal(
            "§6✨ §e[Pim] §6Showing " + allPrices.size() + " REQUIRED pin series with values"));
  }

  private static String formatPrice(double value) {
    if (value == Math.floor(value)) {
      return String.format("%.0f", value);
    }
    return String.format("%.2f", value);
  }
}
