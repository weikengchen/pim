package com.chenweikeng.pim;

import com.chenweikeng.pim.command.PimCommand;
import com.chenweikeng.pim.tracker.BossBarTracker;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PimClient implements ClientModInitializer {

  public static final String MOD_ID = "pim";
  public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

  @Override
  public void onInitializeClient() {
    LOGGER.info("Pim would like to welcome you.");
    PimCommand.register();
    BossBarTracker.getInstance();
  }
}
