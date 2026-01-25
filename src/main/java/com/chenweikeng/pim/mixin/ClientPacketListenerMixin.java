package com.chenweikeng.pim.mixin;

import com.chenweikeng.pim.screen.PinBookHandler;
import com.chenweikeng.pim.screen.PinDetailHandler;
import com.chenweikeng.pim.screen.PinRarityHandler;
import com.chenweikeng.pim.screen.ScreenManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

  @Inject(at = @At("HEAD"), method = "handleContainerContent")
  public void onInventory(
      ClientboundContainerSetContentPacket inventoryS2CPacket, CallbackInfo ci) {
    Screen screen = Minecraft.getInstance().screen;
    if (screen != null) {
      ScreenManager.ScreenType screenType = ScreenManager.detectScreenType(screen);

      if (screenType == ScreenManager.ScreenType.PIN_RARITY_WINDOW) {
        PinRarityHandler.getInstance().handleContainerData(inventoryS2CPacket);
      } else if (screenType == ScreenManager.ScreenType.PIN_PINBOOK_WINDOW) {
        PinBookHandler.getInstance().handleContainerData(inventoryS2CPacket);
        PinDetailHandler.getInstance().handleContainerData(inventoryS2CPacket);
      }
    }
  }

  @Inject(at = @At("HEAD"), method = "handleContainerSetSlot")
  public void onScreenHandlerSlotUpdate(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
    Screen screen = Minecraft.getInstance().screen;
    if (screen != null) {
      ScreenManager.ScreenType screenType = ScreenManager.detectScreenType(screen);

      if (screenType == ScreenManager.ScreenType.PIN_RARITY_WINDOW) {
        PinRarityHandler.getInstance().handleContainerSetSlotData(packet);
      } else if (screenType == ScreenManager.ScreenType.PIN_PINBOOK_WINDOW) {
        PinBookHandler.getInstance().handleContainerSetSlotData(packet);
        PinDetailHandler.getInstance().handleContainerSetSlotData(packet);
      }
    }
  }
}
