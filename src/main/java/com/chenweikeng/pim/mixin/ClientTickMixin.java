package com.chenweikeng.pim.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.chenweikeng.pim.screen.PinBookHandler;
import com.chenweikeng.pim.screen.PinDetailHandler;
import com.chenweikeng.pim.screen.PinRarityHandler;
import com.chenweikeng.pim.tracker.BossBarTracker;

import net.minecraft.client.Minecraft;

@Mixin(Minecraft.class)
public class ClientTickMixin {
    @Inject(method = "tick", at = @At("RETURN"))
    private void tick(CallbackInfo ci) {
        BossBarTracker.getInstance().update();
        PinRarityHandler.getInstance().tick();
        PinBookHandler.getInstance().tick();
        PinDetailHandler.getInstance().tick();
    }
}
