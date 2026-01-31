package com.chenweikeng.pim.command;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

public class PimCommand {

  public static void registerCommands() {
    ClientCommandRegistrationCallback.EVENT.register(
        (dispatcher, registryAccess) -> {
          // Register all sub-commands
          PimTradeCommand.register(dispatcher);
          PimResetCommand.register(dispatcher);
          PimComputeCommand.register(dispatcher);
          PimExportCommand.register(dispatcher);
          PimMatchCommand.register(dispatcher);
          PimViewCommand.register(dispatcher);
          PimPriceCommand.register(dispatcher);
        });
  }
}
