package net.litetex.capes.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class GuiCapeOptionsButton extends GuiButton {

    private static final ResourceLocation ICON =
        new ResourceLocation("capeproviderx", "textures/gui/icon/cape_options.png");

    public GuiCapeOptionsButton(int id, int x, int y) {
        super(id, x, y, 20, 20, "");
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible) return;

        mc.getTextureManager().bindTexture(buttonTextures);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition
            && mouseX < this.xPosition + this.width
            && mouseY < this.yPosition + this.height;

        int hoverState = this.getHoverState(this.hovered);
        this.drawTexturedModalRect(this.xPosition, this.yPosition, 0, 46 + hoverState * 20, 10, 20);
        this.drawTexturedModalRect(this.xPosition + 10, this.yPosition, 190, 46 + hoverState * 20, 10, 20);

        mc.getTextureManager().bindTexture(ICON);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        Gui.drawScaledCustomSizeModalRect(
            this.xPosition + 2,
            this.yPosition + 2,
            0.0F,
            0.0F,
            16,
            16,
            16,
            16,
            16.0F,
            16.0F
        );
    }
}
