package com.chenweikeng.pim.command;

import com.chenweikeng.pim.screen.PinBookHandler;
import com.chenweikeng.pim.screen.PinDetailHandler;
import com.chenweikeng.pim.screen.PinRarityHandler;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

public class PimResetCommand {

  public static void register(
      com.mojang.brigadier.CommandDispatcher<FabricClientCommandSource> dispatcher) {
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
  }
}
