package net.litetex.capes.init;

import net.litetex.capes.provider.CapeProviderManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class CapeAutoRefreshHandler {

    // 5 minutes at 20 ticks/second
    private static final int REFRESH_INTERVAL_TICKS = 6000;

    private int tickCounter = 0;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) return;

        tickCounter++;
        if (tickCounter < REFRESH_INTERVAL_TICKS) return;
        tickCounter = 0;

        CapeProviderManager mgr = CapeProviderManager.getInstance();
        if (mgr == null) return;

        for (Object obj : mc.theWorld.playerEntities) {
            if (obj instanceof AbstractClientPlayer) {
                mgr.refreshPlayer((AbstractClientPlayer) obj);
            }
        }
    }
}