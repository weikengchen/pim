package com.chenweikeng.pim.command;

import com.chenweikeng.pim.pin.MagicString;
import com.chenweikeng.pim.screen.PinDetailHandler;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class PimExportCommand {

  public static void register(
      com.mojang.brigadier.CommandDispatcher<FabricClientCommandSource> dispatcher) {
    dispatcher.register(
        ClientCommandManager.literal("pim:export")
            .executes(
                context -> {
                  exportInventoryPins(context.getSource());
                  return 1;
                }));
  }

  private static void exportInventoryPins(FabricClientCommandSource source) {
    Minecraft.getInstance()
        .execute(
            () -> {
              try {
                String magicString = MagicString.generate();
                Set<String> playerMintPins = MagicString.getPlayerInventoryMintPins();
                TreeMap<String, Integer> seriesCounts = new TreeMap<>();
                for (String pinKey : playerMintPins) {
                  String[] parts = pinKey.split(":");
                  if (parts.length == 2) {
                    String seriesName = parts[0];
                    seriesCounts.put(seriesName, seriesCounts.getOrDefault(seriesName, 0) + 1);
                  }
                }
                source.sendFeedback(Component.literal("§6✨ §e[Pim] §fExporting inventory pins..."));
                if (seriesCounts.isEmpty()) {
                  source.sendFeedback(
                      Component.literal("§6✨ §e[Pim] §cNo mint pins found in inventory!"));
                  return;
                }
                source.sendFeedback(Component.literal("§6✨ §e[Pim] §6Pin series summary:"));
                for (Map.Entry<String, Integer> entry : seriesCounts.entrySet()) {
                  String seriesName = entry.getKey();
                  int count = entry.getValue();
                  Map<String, PinDetailHandler.PinDetailEntry> detailMap =
                      PinDetailHandler.getInstance().getSeriesDetails(seriesName);
                  int totalInSeries = detailMap != null ? detailMap.size() : 0;
                  source.sendFeedback(
                      Component.literal(
                          "§6✨ §e[Pim] §a"
                              + seriesName
                              + ": §f"
                              + count
                              + "/"
                              + totalInSeries
                              + " pins"));
                }
                source.sendFeedback(Component.literal("§6✨ §e[Pim] §6Magic string:"));
                source.sendFeedback(Component.literal("§6✨ §e[Pim] §7" + magicString));
                try {
                  Minecraft mc = Minecraft.getInstance();
                  mc.keyboardHandler.setClipboard(magicString);
                  source.sendFeedback(Component.literal("§6✨ §e[Pim] §a✓ Copied to clipboard!"));
                } catch (Exception e) {
                  source.sendFeedback(
                      Component.literal("§6✨ §e[Pim] §c⚠ Please copy the magic string manually"));
                }
              } catch (Exception e) {
                source.sendFeedback(
                    Component.literal("§6✨ §e[Pim] §cError exporting pins: " + e.getMessage()));
              }
            });
  }
}
