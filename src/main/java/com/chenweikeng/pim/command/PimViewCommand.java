package com.chenweikeng.pim.command;

import com.chenweikeng.pim.pin.MagicStringUtils;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

public class PimViewCommand {

  public static void register(
      com.mojang.brigadier.CommandDispatcher<FabricClientCommandSource> dispatcher) {
    dispatcher.register(
        ClientCommandManager.literal("pim:view")
            .then(
                ClientCommandManager.argument("magicString", StringArgumentType.greedyString())
                    .executes(
                        context -> {
                          String magicString = StringArgumentType.getString(context, "magicString");
                          viewMagicString(context.getSource(), magicString);
                          return 1;
                        }))
            .executes(
                context -> {
                  context
                      .getSource()
                      .sendFeedback(
                          Component.literal("§6✨ §e[Pim] §cUsage: /pim:view <magic_string>"));
                  return 1;
                }));
  }

  private static void viewMagicString(FabricClientCommandSource source, String magicString) {
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

      var pinsBySeries = MagicStringUtils.organizePinsBySeries(result.mintPins);
      int totalPins = result.mintPins.size();

      if (pinsBySeries.isEmpty()) {
        source.sendFeedback(Component.literal("§6✨ §e[Pim] §cNo pins found in the magic string!"));
        return;
      }

      source.sendFeedback(
          MagicStringUtils.twoLevelTree("§6✨ §e[Pim] §6Pins in magic string", pinsBySeries));

      source.sendFeedback(Component.literal("§6✨ §e[Pim] §6Total: " + totalPins + " mint pins"));

    } catch (Exception e) {
      source.sendFeedback(
          Component.literal("§6✨ §e[Pim] §cError viewing magic string: " + e.getMessage()));
    }
  }
}
