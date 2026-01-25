package com.chenweikeng.pim.mixin;

import com.chenweikeng.pim.screen.PinDetailHandler;
import com.chenweikeng.pim.screen.PinRarityHandler;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {
  @Shadow private Minecraft minecraft;

  @Inject(at = @At("HEAD"), method = "renderSlot")
  private void onRenderSlot(
      GuiGraphics guiGraphics,
      int i,
      int j,
      DeltaTracker deltaTracker,
      Player player,
      ItemStack itemStack,
      int k,
      CallbackInfo ci) {
    if (itemStack.isEmpty()) {
      return;
    }

    String pinSeries = PinDetailHandler.getInstance().parsePinSeriesFromLore(itemStack);
    if (pinSeries == null) {
      return;
    }

    String itemName = itemStack.getHoverName().getString();

    PinDetailHandler.PinDetailEntry onBoardEntry =
        PinDetailHandler.getInstance().parsePinEntry(itemStack);
    PinRarityHandler.PinSeriesEntry pinSeriesEntry =
        PinRarityHandler.getInstance().getSeriesEntry(pinSeries);
    PinDetailHandler.PinDetailEntry pinDetailEntry =
        PinDetailHandler.getInstance().findDetailEntry(pinSeries, itemName);

    if (onBoardEntry != null
        && onBoardEntry.condition == PinDetailHandler.PinCondition.MINT
        && pinSeriesEntry != null
        && pinSeriesEntry.availability == PinRarityHandler.Availability.REQUIRED) {
      if (pinDetailEntry != null
          && (pinDetailEntry.condition == PinDetailHandler.PinCondition.LOCKED
              || pinDetailEntry.condition == PinDetailHandler.PinCondition.NOTMINT)) {
        guiGraphics.fill(i, j, i + 16, j + 16, 0xFF00FF00);

        float f = itemStack.getPopTime() - deltaTracker.getGameTimeDeltaPartialTick(false);
        if (f > 0.0F) {
          float g = 1.0F + f / 5.0F;
          guiGraphics.pose().pushMatrix();
          guiGraphics.pose().translate(i + 8, j + 12);
          guiGraphics.pose().scale(1.0F / g, (g + 1.0F) / 2.0F);
          guiGraphics.pose().translate(-(i + 8), -(j + 12));
        }

        guiGraphics.renderItem(player, itemStack, i, j, k);
        if (f > 0.0F) {
          guiGraphics.pose().popMatrix();
        }

        guiGraphics.renderItemDecorations(this.minecraft.font, itemStack, i, j);
      } else {
        guiGraphics.fill(i, j, i + 16, j + 16, 0xFFFFE000);

        float f = itemStack.getPopTime() - deltaTracker.getGameTimeDeltaPartialTick(false);
        if (f > 0.0F) {
          float g = 1.0F + f / 5.0F;
          guiGraphics.pose().pushMatrix();
          guiGraphics.pose().translate(i + 8, j + 12);
          guiGraphics.pose().scale(1.0F / g, (g + 1.0F) / 2.0F);
          guiGraphics.pose().translate(-(i + 8), -(j + 12));
        }

        guiGraphics.renderItem(player, itemStack, i, j, k);
        if (f > 0.0F) {
          guiGraphics.pose().popMatrix();
        }
        guiGraphics.fill(i, j, i + 16, j + 16, 0x80FFE000);

        guiGraphics.renderItemDecorations(this.minecraft.font, itemStack, i, j);
      }
    }
  }
}
