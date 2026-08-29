package net.litetex.capes.gui;

import net.litetex.capes.CapeProviderX;
import net.litetex.capes.config.CapeConfig;
import net.litetex.capes.provider.CapeProviderManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.client.entity.AbstractClientPlayer;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.List;

public class GuiCapeSettings extends GuiScreen {

    private static final int BUTTON_TAB_PROVIDERS = 100;
    private static final int BUTTON_TAB_PREVIEW   = 101;
    private static final int BUTTON_TAB_OTHER     = 102;

    private static final int BUTTON_DONE   = 200;
    private static final int BUTTON_RELOAD = 201;
    private static final int BUTTON_RESET  = 202;

    private static final int BUTTON_TOGGLE_PLAYER = 204;

    private static final int BUTTON_ONLY_OWN_CAPE   = 300;
    private static final int BUTTON_ANIMATED        = 301;
    private static final int BUTTON_EXT_PROVIDERS   = 303;
    private static final int BUTTON_MOD_PROVIDERS   = 304;
    private static final int BUTTON_LOCAL_PROVIDERS = 305;

    private static final int BUTTON_EDIT_BASE = 400;
    private static final int BUTTON_UP_BASE   = 500;
    private static final int BUTTON_DOWN_BASE = 600;

    private static final int LINK_CONFIRM_ID = 700;

    private final GuiScreen parentScreen;
    private Tab             tab;
    private CapeConfig      config;
    private List<CapeConfig.ProviderEntry> providers;
    private boolean         showPlayer = true;
    private String          pendingLink;

    private float   previewYaw = 180.0F;
    private boolean dragging   = false;
    private int     lastDragX;

    public GuiCapeSettings(GuiScreen parentScreen) {
        this(parentScreen, Tab.PREVIEW);
    }

    public GuiCapeSettings(GuiScreen parentScreen, Tab tab) {
        this.parentScreen = parentScreen;
        this.tab          = tab;
    }
    
    private static class WideButton extends GuiButton {
        WideButton(int id, int x, int y, int w, int h, String text) {
            super(id, x, y, w, h, text);
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            if (!this.visible) return;

            FontRenderer fontrenderer = mc.fontRendererObj;
            mc.getTextureManager().bindTexture(buttonTextures);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition
                && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
            int state = this.getHoverState(this.hovered);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(770, 771);

            int cap  = 4;
            int texY = 46 + state * 20;

            if (this.width <= cap * 2) {
                this.drawTexturedModalRect(xPosition, yPosition, 0, texY, width / 2, height);
                this.drawTexturedModalRect(xPosition + width / 2, yPosition, 200 - width / 2, texY, width / 2, height);
            } else {
                this.drawTexturedModalRect(xPosition, yPosition, 0, texY, cap, height);
                this.drawTexturedModalRect(xPosition + width - cap, yPosition, 200 - cap, texY, cap, height);
                this.drawScaledCustomSizeModalRect(
                    xPosition + cap, yPosition,
                    cap, texY,
                    200 - cap * 2, height,
                    width - cap * 2, height,
                    256, 256);
            }

            this.mouseDragged(mc, mouseX, mouseY);

            int color = 14737632;
            if (!this.enabled) color = 10526880;
            else if (this.hovered) color = 16777120;

            this.drawCenteredString(fontrenderer, this.displayString,
                xPosition + width / 2, yPosition + (height - 8) / 2, color);
        }
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.config    = CapeConfig.load(CapeProviderX.INSTANCE.getConfigDir());
        this.providers = config.getProviders();

        CapeProviderManager mgr = CapeProviderManager.getInstance();
        if (mgr != null && this.mc.thePlayer != null) {
            mgr.refreshPlayer(this.mc.thePlayer);
        }

        addTabs();

        switch (tab) {
            case PROVIDERS: addProviderButtons(); break;
            case PREVIEW:   addPreviewButtons();  break;
            case OTHER:     addOtherButtons();    break;
        }

        this.buttonList.add(new WideButton(BUTTON_DONE,
            this.width / 2 - 200, this.height - 30, 400, 20, "Done"));
    }

    private void addTabs() {
        int tabW  = Math.min(120, Math.max(88, (this.width - 48) / 3));
        int gap   = 10;
        int startX = this.width / 2 - (tabW * 3 + gap * 2) / 2;
        int y     = 36;

        GuiButton prov    = new GuiButton(BUTTON_TAB_PROVIDERS, startX,               y, tabW, 20, "Manage Providers");
        GuiButton preview = new GuiButton(BUTTON_TAB_PREVIEW,   startX + tabW + gap,  y, tabW, 20, "Preview");
        GuiButton other   = new GuiButton(BUTTON_TAB_OTHER,     startX + (tabW+gap)*2,y, tabW, 20, "Other");

        prov.enabled    = tab != Tab.PROVIDERS;
        preview.enabled = tab != Tab.PREVIEW;
        other.enabled   = tab != Tab.OTHER;

        this.buttonList.add(prov);
        this.buttonList.add(preview);
        this.buttonList.add(other);
    }

    private void addProviderButtons() {
        int lx = listX(), lw = listWidth();
        int y  = providerStartY();

        for (int i = 0; i < providers.size(); i++) {
            CapeConfig.ProviderEntry e = providers.get(i);
            int ry  = y + i * rowHeight();
            String url = editUrl(e.getId());

            if (url != null) {
                this.buttonList.add(new GuiButton(
                    BUTTON_EDIT_BASE + i, lx + lw - 190, ry + 2, 100, 20, "Edit cape"));
            }
            if (i > 0) {
                this.buttonList.add(new GuiButton(
                    BUTTON_UP_BASE + i, lx + lw - 54, ry + 1, 24, 10, "^"));
            }
            if (i < providers.size() - 1) {
                this.buttonList.add(new GuiButton(
                    BUTTON_DOWN_BASE + i, lx + lw - 54, ry + 12, 24, 10, "v"));
            }
        }
    }

    private void addPreviewButtons() {
        int lx = Math.max(24, this.width / 4 - 70);
        int by = this.height / 2;
        this.buttonList.add(new GuiButton(BUTTON_TOGGLE_PLAYER, lx, by, 140, 20, "Toggle Player"));
    }

    private void addOtherButtons() {
        int lx = this.width / 2 - 200;
        int rx = this.width / 2 + 10;
        int y  = 70;

        this.buttonList.add(new GuiButton(BUTTON_ONLY_OWN_CAPE,  lx, y,      190, 20, tog("Only load your cape",        config.isOnlyOwnCape())));
        this.buttonList.add(new GuiButton(BUTTON_ANIMATED,       rx, y,      190, 20, tog("Animated textures",          config.isAnimatedTextures())));
        this.buttonList.add(new GuiButton(BUTTON_MOD_PROVIDERS,  lx, y + 26, 190, 20, tog("Load Mod providers",         config.isModProviders())));
        this.buttonList.add(new GuiButton(BUTTON_LOCAL_PROVIDERS,rx, y + 26, 190, 20, tog("Load local providers",       config.isLocalProviders())));
        this.buttonList.add(new GuiButton(BUTTON_EXT_PROVIDERS,  lx, y + 52, 190, 20, tog("Activate external providers",config.isExternalProviders())));
        this.buttonList.add(new GuiButton(BUTTON_RESET,          rx, y + 52, 190, 20, "Reset"));
    }

    private static String tog(String label, boolean value) {
        return label + ": " + (value ? "\u00a7aON" : "\u00a7cOFF");
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        int id = button.id;

        if (id == BUTTON_TAB_PROVIDERS) { tab = Tab.PROVIDERS; initGui(); return; }
        if (id == BUTTON_TAB_PREVIEW)   { tab = Tab.PREVIEW;   initGui(); return; }
        if (id == BUTTON_TAB_OTHER)     { tab = Tab.OTHER;      initGui(); return; }

        if (id == BUTTON_DONE) {
            config.save(CapeProviderX.INSTANCE.getConfigDir());
            Minecraft.getMinecraft().displayGuiScreen(parentScreen);
            return;
        }

        if (id == BUTTON_TOGGLE_PLAYER) { showPlayer = !showPlayer; return; }

        if (id == BUTTON_ONLY_OWN_CAPE) {
            config.setOnlyOwnCape(!config.isOnlyOwnCape());
            button.displayString = tog("Only load your cape", config.isOnlyOwnCape());
            saveAndRefresh(); return;
        }
        if (id == BUTTON_ANIMATED) {
            config.setAnimatedTextures(!config.isAnimatedTextures());
            button.displayString = tog("Animated textures", config.isAnimatedTextures());
            saveAndRefresh(); return;
        }
        if (id == BUTTON_EXT_PROVIDERS) {
            config.setExternalProviders(!config.isExternalProviders());
            button.displayString = tog("Activate external providers", config.isExternalProviders());
            saveAndRefresh(); return;
        }
        if (id == BUTTON_MOD_PROVIDERS) {
            config.setModProviders(!config.isModProviders());
            button.displayString = tog("Load Mod providers", config.isModProviders());
            saveAndRefresh(); return;
        }
        if (id == BUTTON_LOCAL_PROVIDERS) {
            config.setLocalProviders(!config.isLocalProviders());
            button.displayString = tog("Load local providers", config.isLocalProviders());
            saveAndRefresh(); return;
        }
        if (id == BUTTON_RESET) {
            config = new CapeConfig();
            saveAndRefresh();
            initGui();
            return;
        }
        if (id == BUTTON_RELOAD) {
            CapeProviderManager mgr = CapeProviderManager.getInstance();
            if (mgr != null) mgr.clearCache();
            button.displayString = "Cache Cleared!";
            return;
        }

        if (id >= BUTTON_EDIT_BASE && id < BUTTON_EDIT_BASE + 100) {
            int idx = id - BUTTON_EDIT_BASE;
            if (idx >= 0 && idx < providers.size()) openLink(editUrl(providers.get(idx).getId()));
            return;
        }
        if (id >= BUTTON_UP_BASE && id < BUTTON_UP_BASE + 100) {
            moveProvider(id - BUTTON_UP_BASE, -1); return;
        }
        if (id >= BUTTON_DOWN_BASE && id < BUTTON_DOWN_BASE + 100) {
            moveProvider(id - BUTTON_DOWN_BASE, 1);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0 && tab == Tab.PROVIDERS) {
            int clicked = providerIndexAt(mouseX, mouseY);
            if (clicked >= 0) {
                int boxX = listX() + 12;
                int boxY = providerStartY() + clicked * rowHeight() + 5;
                if (mouseX >= boxX && mouseX <= boxX + 14 && mouseY >= boxY && mouseY <= boxY + 14) {
                    toggleProvider(clicked); return;
                }
                int nameX = boxX + 24;
                String name = friendlyName(providers.get(clicked).getId());
                if (mouseX >= nameX && mouseX <= nameX + this.fontRendererObj.getStringWidth(name)) {
                    String url = homepageUrl(providers.get(clicked).getId());
                    if (url != null) { openLink(url); return; }
                }
            }
        }
        if (mouseButton == 0 && tab == Tab.PREVIEW && this.mc.thePlayer != null) {
            dragging  = true;
            lastDragX = mouseX;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (dragging && clickedMouseButton == 0) {
            int delta = mouseX - lastDragX;
            previewYaw -= delta * 1.0F;
            lastDragX = mouseX;
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        dragging = false;
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(fontRendererObj, "Cape Options", width / 2, 14, 0xFFFFFF);
        drawHLine(0, width, 32, 0x66FFFFFF);
        drawHLine(0, width, height - 44, 0x66FFFFFF);

        switch (tab) {
            case PROVIDERS: drawProviderList(mouseX, mouseY);  break;
            case PREVIEW:   drawPreview();                     break;
            case OTHER:     		                           break;
        }

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (tab == Tab.PROVIDERS) drawProviderTooltips(mouseX, mouseY);
    }

    private void drawProviderList(int mouseX, int mouseY) {
        for (int i = 0; i < providers.size(); i++) {
            drawProviderRow(providers.get(i), providerStartY() + i * rowHeight(), i, mouseX, mouseY);
        }
        drawDefaultMCRow(providerStartY() + providers.size() * rowHeight());
    }

    private void drawProviderRow(CapeConfig.ProviderEntry e, int y, int index, int mx, int my) {
        int lx = listX(), boxX = lx + 12;
        boolean hovered = providerIndexAt(mx, my) == index;
        if (hovered) drawRect(lx + 4, y, lx + listWidth() - 4, y + rowHeight() - 2, 0x33000000);
        drawCheckbox(boxX, y + 5, e.isEnabled(), true);
        int col = e.isEnabled() ? (homepageUrl(e.getId()) != null ? 0x6666FF : 0xFFFFFF) : 0x777777;
        this.drawString(fontRendererObj, friendlyName(e.getId()), boxX + 24, y + 7, col);
        if (hasWarning(e.getId())) {
            int wx = lx + listWidth() - 222;
            drawRect(wx, y + 3, wx + 20, y + 21, 0xAA8B1A1A);
            drawCenteredString(fontRendererObj, "!", wx + 10, y + 8, 0xFFFF6666);
        }
    }

    private void drawDefaultMCRow(int y) {
        int boxX = listX() + 12;
        drawCheckbox(boxX, y + 5, true, false);
        drawString(fontRendererObj, "Default / Minecraft", boxX + 24, y + 7, 0xFFFFFF);
    }

    private void drawCheckbox(int x, int y, boolean checked, boolean active) {
        drawRect(x, y, x + 14, y + 14, active ? 0xFFFFFFFF : 0xFFAAAAAA);
        drawRect(x + 1, y + 1, x + 13, y + 13, 0xFF111111);
        if (checked) drawRect(x + 3, y + 3, x + 11, y + 11, active ? 0xFFFFFFFF : 0xFF888888);
    }

    private void drawProviderTooltips(int mx, int my) {
        int clicked = providerIndexAt(mx, my);
        if (clicked < 0) return;
        CapeConfig.ProviderEntry e = providers.get(clicked);
        if (!hasWarning(e.getId())) return;
        int wx = listX() + listWidth() - 222;
        int y  = providerStartY() + clicked * rowHeight() + 3;
        if (mx >= wx && mx <= wx + 20 && my >= y && my <= y + 18) {
            drawHoveringText(warningText(e.getId()), mx, my);
        }
    }

    private void drawPreview() {
        if (this.mc.thePlayer == null) {
            drawCenteredString(fontRendererObj, "Join a world to preview your cape",
                width / 2, height / 2, 0xAAAAAA);
            return;
        }

        int renderX = width / 2;
        int renderY = Math.min(height - 76, height / 2 + 78);
        int scale   = Math.max(36, Math.min(62, height / 5));

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        PreviewState.hideBodyForPreview = !showPlayer;
        PreviewState.forceOwnCapeRender = true;
        PoseSnapshot snap = freezeToIdlePose(this.mc.thePlayer);

        drawEntityFromBack(renderX, renderY, scale, previewYaw, this.mc.thePlayer);

        restorePose(this.mc.thePlayer, snap);
        PreviewState.forceOwnCapeRender = false;
        PreviewState.hideBodyForPreview = false;
    }

    private static class PoseSnapshot {
        float limbSwing, limbSwingAmount, prevLimbSwingAmount;
        float swingProgress, prevSwingProgress;
        boolean isSwingInProgress;

        double prevPosX, prevPosY, prevPosZ;
        double chasingPosX, chasingPosY, chasingPosZ;
        double prevChasingPosX, prevChasingPosY, prevChasingPosZ;
        float cameraYaw, prevCameraYaw;
        float distanceWalkedModified, prevDistanceWalkedModified;
    }

    private static PoseSnapshot freezeToIdlePose(AbstractClientPlayer ent) {
        PoseSnapshot snap = new PoseSnapshot();
        snap.limbSwing              = ent.limbSwing;
        snap.limbSwingAmount        = ent.limbSwingAmount;
        snap.prevLimbSwingAmount    = ent.prevLimbSwingAmount;
        snap.swingProgress          = ent.swingProgress;
        snap.prevSwingProgress      = ent.prevSwingProgress;
        snap.isSwingInProgress      = ent.isSwingInProgress;

        snap.prevPosX = ent.prevPosX; snap.prevPosY = ent.prevPosY; snap.prevPosZ = ent.prevPosZ;
        snap.chasingPosX = ent.chasingPosX; snap.chasingPosY = ent.chasingPosY; snap.chasingPosZ = ent.chasingPosZ;
        snap.prevChasingPosX = ent.prevChasingPosX; snap.prevChasingPosY = ent.prevChasingPosY; snap.prevChasingPosZ = ent.prevChasingPosZ;
        snap.cameraYaw = ent.cameraYaw; snap.prevCameraYaw = ent.prevCameraYaw;
        snap.distanceWalkedModified = ent.distanceWalkedModified;
        snap.prevDistanceWalkedModified = ent.prevDistanceWalkedModified;

        ent.limbSwing           = 0.0F;
        ent.limbSwingAmount     = 0.0F;
        ent.prevLimbSwingAmount = 0.0F;
        ent.swingProgress       = 0.0F;
        ent.prevSwingProgress   = 0.0F;
        ent.isSwingInProgress   = false;
        ent.prevPosX = ent.posX; ent.prevPosY = ent.posY; ent.prevPosZ = ent.posZ;
        ent.chasingPosX = ent.posX; ent.chasingPosY = ent.posY; ent.chasingPosZ = ent.posZ;
        ent.prevChasingPosX = ent.posX; ent.prevChasingPosY = ent.posY; ent.prevChasingPosZ = ent.posZ;
        ent.cameraYaw = 0.0F; ent.prevCameraYaw = 0.0F;
        ent.distanceWalkedModified = 0.0F; ent.prevDistanceWalkedModified = 0.0F;

        return snap;
    }

    private static void restorePose(AbstractClientPlayer ent, PoseSnapshot snap) {
        ent.limbSwing           = snap.limbSwing;
        ent.limbSwingAmount     = snap.limbSwingAmount;
        ent.prevLimbSwingAmount = snap.prevLimbSwingAmount;
        ent.swingProgress       = snap.swingProgress;
        ent.prevSwingProgress   = snap.prevSwingProgress;
        ent.isSwingInProgress   = snap.isSwingInProgress;

        ent.prevPosX = snap.prevPosX; ent.prevPosY = snap.prevPosY; ent.prevPosZ = snap.prevPosZ;
        ent.chasingPosX = snap.chasingPosX; ent.chasingPosY = snap.chasingPosY; ent.chasingPosZ = snap.chasingPosZ;
        ent.prevChasingPosX = snap.prevChasingPosX; ent.prevChasingPosY = snap.prevChasingPosY; ent.prevChasingPosZ = snap.prevChasingPosZ;
        ent.cameraYaw = snap.cameraYaw; ent.prevCameraYaw = snap.prevCameraYaw;
        ent.distanceWalkedModified = snap.distanceWalkedModified;
        ent.prevDistanceWalkedModified = snap.prevDistanceWalkedModified;
    }

    private static void drawEntityFromBack(int posX, int posY, int scale,
                                            float yaw, EntityLivingBase ent) {
        GlStateManager.enableColorMaterial();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) posX, (float) posY, 50.0F);
        GlStateManager.scale((float) -scale, (float) scale, (float) scale);
        GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);

        float f2 = ent.renderYawOffset;
        float f3 = ent.rotationYaw;
        float f4 = ent.rotationPitch;
        float f5 = ent.prevRotationYawHead;
        float f6 = ent.rotationYawHead;

        GlStateManager.rotate(135.0F, 0.0F, 1.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.rotate(-135.0F, 0.0F, 1.0F, 0.0F);

        ent.renderYawOffset     = yaw;
        ent.rotationYaw         = yaw;
        ent.rotationPitch       = 0.0F;
        ent.rotationYawHead     = yaw;
        ent.prevRotationYawHead = yaw;

        RenderManager rm = Minecraft.getMinecraft().getRenderManager();
        rm.setPlayerViewY(180.0F);
        rm.setRenderShadow(false);
        rm.renderEntityWithPosYaw(ent, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F);
        rm.setRenderShadow(true);

        ent.renderYawOffset     = f2;
        ent.rotationYaw         = f3;
        ent.rotationPitch       = f4;
        ent.prevRotationYawHead = f5;
        ent.rotationYawHead     = f6;

        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }


    private void drawHLine(int x1, int x2, int y, int color) {
        drawRect(x1, y, x2, y + 1, color);
        drawRect(x1, y + 1, x2, y + 2, 0x66000000);
    }


    private void toggleProvider(int index) {
        providers.get(index).setEnabled(!providers.get(index).isEnabled());
        saveAndRefresh();
        initGui();
    }

    private void moveProvider(int index, int dir) {
        int target = index + dir;
        if (index < 0 || index >= providers.size() || target < 0 || target >= providers.size()) return;
        providers.add(target, providers.remove(index));
        saveAndRefresh();
        initGui();
    }

    private void saveAndRefresh() {
        config.save(CapeProviderX.INSTANCE.getConfigDir());
        CapeProviderManager.init(CapeProviderX.INSTANCE.getConfigDir(), config);
    }

    private int providerIndexAt(int mx, int my) {
        int y = providerStartY(), x = listX(), w = listWidth();
        if (mx < x || mx > x + w || my < y) return -1;
        int idx = (my - y) / rowHeight();
        return (idx >= 0 && idx < providers.size()) ? idx : -1;
    }

    private void openLink(String url) {
        if (url == null || url.isEmpty()) return;
        pendingLink = url;
        GuiConfirmOpenLink confirm = new GuiConfirmOpenLink(this, url, LINK_CONFIRM_ID, true);
        confirm.disableSecurityWarning();
        mc.displayGuiScreen(confirm);
    }

    @Override
    public void confirmClicked(boolean result, int id) {
        if (id == LINK_CONFIRM_ID) {
            if (result && pendingLink != null) {
                try { Desktop.getDesktop().browse(new URI(pendingLink)); }
                catch (Exception e) {
                    CapeProviderX.LOGGER.warn("[CapeProviderX] Link open failed: {}", e.getMessage());
                }
            }
            mc.displayGuiScreen(this);
            pendingLink = null;
        }
    }

    private int listX()         { return width / 2 - listWidth() / 2; }
    private int listWidth()     { return Math.min(620, width - 60); }
    private int providerStartY(){ return 78; }
    private int rowHeight()     { return 24; }

    private static String friendlyName(String id) {
        if (id == null) return "";
        switch (id) {
            case "local":          return "Local File";
            case "picapes":        return "PiCapes";
            case "optifine":       return "OptiFine";
            case "minecraftcapes": return "MinecraftCapes";
            case "skinmc":         return "SkinMC";
            case "cosmetica":      return "Cosmetica";
            default: return Character.toUpperCase(id.charAt(0)) + id.substring(1);
        }
    }

    private static String editUrl(String id) {
        if ("picapes".equals(id))        return "https://catalog.picapes.syanic.org/";
        if ("optifine".equals(id))       return "https://optifine.net/capeChange";
        if ("minecraftcapes".equals(id)) return "https://minecraftcapes.net";
        if ("skinmc".equals(id))         return "https://skinmc.net/capes/";
        if ("cosmetica".equals(id))      return "https://login.cosmetica.cc";
        return null;
    }

    private static String homepageUrl(String id) {
        if ("picapes".equals(id))        return "https://picapes.syanic.org/";
        if ("optifine".equals(id))       return "https://optifine.net/home";
        if ("minecraftcapes".equals(id)) return "https://minecraftcapes.net";
        if ("skinmc".equals(id))         return "https://skinmc.net/";
        if ("cosmetica".equals(id))      return "https://cosmetica.cc/";
        return null;
    }

    private static boolean hasWarning(String id) {
        return "optifine".equals(id) || "cosmetica".equals(id);
    }

    private static java.util.List<String> warningText(String id) {
        if ("optifine".equals(id))
            return java.util.Arrays.asList("Payment required to unlock capes", "Connection can be slow");
        if ("cosmetica".equals(id))
            return java.util.Arrays.asList("Provider may respond slowly");
        return java.util.Collections.emptyList();
    }

    public enum Tab { PROVIDERS, PREVIEW, OTHER }
}