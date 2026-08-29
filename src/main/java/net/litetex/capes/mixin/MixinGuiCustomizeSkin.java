package net.litetex.capes.mixin;

import net.litetex.capes.gui.GuiCapeSettings;
import net.litetex.capes.gui.GuiCapeOptionsButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiCustomizeSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

@Mixin(GuiCustomizeSkin.class)
public abstract class MixinGuiCustomizeSkin extends GuiScreen {

    private static final int CPX_SETTINGS_BUTTON_ID = 997;

    @Inject(method = "initGui", at = @At("TAIL"))
    private void cpx$addSettingsButton(CallbackInfo ci) {
        GuiButton capeButton = null;
        for (Object obj : this.buttonList) {
            GuiButton btn = (GuiButton) obj;
            if (btn.displayString != null && btn.displayString.contains("Cape")) {
                capeButton = btn;
                break;
            }
        }

        if (capeButton != null) {
            int buttonX = capeButton.xPosition - 25;
            if (buttonX < 4) {
                buttonX = capeButton.xPosition + capeButton.width + 5;
            }

            this.buttonList.add(new GuiCapeOptionsButton(
                CPX_SETTINGS_BUTTON_ID,
                buttonX,
                capeButton.yPosition
            ));
        } else {
            this.buttonList.add(new GuiButton(
                CPX_SETTINGS_BUTTON_ID,
                this.width / 2 - 100,
                this.height - 75,
                200, 20,
                "\u00a7eCape Provider X Settings"
            ));
        }
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"))
    private void cpx$onAction(GuiButton button, CallbackInfo ci) throws IOException {
        if (button.id == CPX_SETTINGS_BUTTON_ID) {
            Minecraft.getMinecraft().displayGuiScreen(
                new GuiCapeSettings((GuiScreen)(Object) this, GuiCapeSettings.Tab.PREVIEW)
            );
        }
    }
}
