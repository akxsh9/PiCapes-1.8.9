package net.litetex.capes.mixin;

import net.litetex.capes.provider.CapeProviderManager;
import net.litetex.capes.gui.PreviewState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerCape;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LayerCape.class)
public class MixinLayerCape {

    @Shadow
    private RenderPlayer playerRenderer;

    private static Boolean cpx$waveyCapesLoaded = null;

    private static boolean waveyCapesLoaded() {
        if (cpx$waveyCapesLoaded == null) {
            cpx$waveyCapesLoaded = Loader.isModLoaded("waveycapes");
        }
        return cpx$waveyCapesLoaded;
    }

    @Inject(
        method      = "doRenderLayer",
        at          = @At("HEAD"),
        cancellable = true
    )
    public void cpx$injectDoRenderLayer(
            AbstractClientPlayer player,
            float limbSwing, float limbSwingAmount, float partialTicks,
            float ageInTicks, float netHeadYaw, float headPitch, float scale,
            CallbackInfo ci) {

        CapeProviderManager mgr = CapeProviderManager.getInstance();
        if (mgr == null) return;

        mgr.requestCape(player);

        if (player.hasPlayerInfo() && waveyCapesLoaded() && !PreviewState.forceOwnCapeRender) return;

        ResourceLocation ourCape = mgr.getCapeTexture(player);
        if (ourCape == null) return;

        if (!player.isInvisible() && player.isWearing(EnumPlayerModelParts.CAPE)) {
            renderCape(player, partialTicks, ourCape);
        }
        ci.cancel();
    }

    private void renderCape(AbstractClientPlayer player,
                             float partialTicks,
                             ResourceLocation texture) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0F, 0.0F, 0.125F);

        double dx = player.prevChasingPosX
            + (player.chasingPosX - player.prevChasingPosX) * (double) partialTicks
            - (player.prevPosX + (player.posX - player.prevPosX) * (double) partialTicks);
        double dy = player.prevChasingPosY
            + (player.chasingPosY - player.prevChasingPosY) * (double) partialTicks
            - (player.prevPosY + (player.posY - player.prevPosY) * (double) partialTicks);
        double dz = player.prevChasingPosZ
            + (player.chasingPosZ - player.prevChasingPosZ) * (double) partialTicks
            - (player.prevPosZ + (player.posZ - player.prevPosZ) * (double) partialTicks);

        float bodyYaw = player.prevRenderYawOffset
            + (player.renderYawOffset - player.prevRenderYawOffset) * partialTicks;
        double sinYaw = (double) MathHelper.sin(bodyYaw * (float) Math.PI / 180.0F);
        double cosYaw = (double) (-MathHelper.cos(bodyYaw * (float) Math.PI / 180.0F));

        float f1 = (float) dy * 10.0F;
        f1 = MathHelper.clamp_float(f1, -6.0F, 32.0F);

        float f2 = (float) (dx * sinYaw + dz * cosYaw) * 100.0F;
        float f3 = (float) (dx * cosYaw - dz * sinYaw) * 100.0F;
        if (f2 < 0.0F) f2 = 0.0F;

        float cameraYaw = player.prevCameraYaw
            + (player.cameraYaw - player.prevCameraYaw) * partialTicks;
        f1 += MathHelper.sin(
            (player.prevDistanceWalkedModified
                + (player.distanceWalkedModified - player.prevDistanceWalkedModified)
                * partialTicks) * 6.0F
        ) * 32.0F * cameraYaw;

        if (player.isSneaking()) f1 += 15.0F;

        GlStateManager.rotate(6.0F + f2 / 2.0F + f1, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(f3 / 2.0F,  0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(-f3 / 2.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(180.0F,     0.0F, 1.0F, 0.0F);

        this.playerRenderer.getMainModel().renderCape(0.0625F);
        GlStateManager.popMatrix();
    }
}