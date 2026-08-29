package net.litetex.capes.mixin;

import net.litetex.capes.provider.CapeProviderManager;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class MixinAbstractClientPlayer {

    @Inject(
        method      = "getLocationCape",
        at          = @At("HEAD"),
        cancellable = true
    )
    private void cpx$getLocationCape(CallbackInfoReturnable<ResourceLocation> cir) {
        CapeProviderManager mgr = CapeProviderManager.getInstance();
        if (mgr == null) return;

        AbstractClientPlayer self = (AbstractClientPlayer)(Object) this;

        mgr.requestCape(self);

        ResourceLocation ourCape = mgr.getCapeTexture(self);
        if (ourCape != null) {
            cir.setReturnValue(ourCape);
        }
    }
}