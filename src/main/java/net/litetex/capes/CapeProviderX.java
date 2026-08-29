package net.litetex.capes;

import net.litetex.capes.config.CapeConfig;
import net.litetex.capes.provider.CapeProviderManager;
import net.litetex.capes.init.CapeAutoRefreshHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

@Mod(
    modid      = CapeProviderX.MODID,
    name       = CapeProviderX.NAME,
    version    = CapeProviderX.VERSION,
    clientSideOnly = true
)
public class CapeProviderX {

    public static final String MODID   = "capeproviderx";
    public static final String NAME    = "Cape Provider X";
    public static final String VERSION = "1.8";

    public static final Logger LOGGER = LogManager.getLogger(MODID);

    @Mod.Instance(MODID)
    public static CapeProviderX INSTANCE;

    private File configDir;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        configDir = new File(event.getModConfigurationDirectory(), "cape-provider-x");
        if (!configDir.exists()) configDir.mkdirs();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        CapeConfig config = CapeConfig.load(configDir);
        CapeProviderManager.init(configDir, config);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new CapeAutoRefreshHandler());
        LOGGER.info("[CapeProviderX] Initialized with {} provider(s)",
            CapeProviderManager.getInstance().getProviderCount());
    }

    public File getConfigDir() { return configDir; }
}