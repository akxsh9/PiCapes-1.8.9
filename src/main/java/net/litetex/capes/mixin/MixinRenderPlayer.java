package net.litetex.capes.mixin;

import net.litetex.capes.gui.PreviewState;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderPlayer.class)
public class MixinRenderPlayer {

    @Inject(method = "setModelVisibilities", at = @At("TAIL"))
    private void cpx$afterSetModelVisibilities(AbstractClientPlayer player, CallbackInfo ci) {
        if (!PreviewState.hideBodyForPreview) return;

        ModelPlayer model = (ModelPlayer) ((RenderPlayer)(Object) this).getMainModel();
        model.bipedHead.showModel         = false;
        model.bipedHeadwear.showModel     = false;
        model.bipedBody.showModel         = false;
        model.bipedRightArm.showModel     = false;
        model.bipedLeftArm.showModel      = false;
        model.bipedRightLeg.showModel     = false;
        model.bipedLeftLeg.showModel      = false;
        model.bipedRightArmwear.showModel = false;
        model.bipedLeftArmwear.showModel  = false;
        model.bipedRightLegwear.showModel = false;
        model.bipedLeftLegwear.showModel  = false;
        model.bipedBodyWear.showModel     = false;
    }
}