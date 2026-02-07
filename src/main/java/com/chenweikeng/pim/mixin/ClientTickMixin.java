package com.chenweikeng.pim.mixin;

import com.chenweikeng.pim.PimClient;
import com.chenweikeng.pim.screen.PinBookHandler;
import com.chenweikeng.pim.screen.PinDetailHandler;
import com.chenweikeng.pim.screen.PinRarityHandler;
import com.chenweikeng.pim.tracker.BossBarTracker;
import com.chenweikeng.pim.tracker.ClipboardParser;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class ClientTickMixin {
  @Inject(method = "tick", at = @At("RETURN"))
  private void tick(CallbackInfo ci) {
    if (!PimClient.isImagineFunServer()) {
      return;
    }

    BossBarTracker.getInstance().update();
    PinRarityHandler.getInstance().tick();
    PinBookHandler.getInstance().tick();
    PinDetailHandler.getInstance().tick();
    ClipboardParser.getInstance().tick();
  }
}
