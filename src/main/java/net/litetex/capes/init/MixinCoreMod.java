package net.litetex.capes.init;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.8.9")
@IFMLLoadingPlugin.Name("CapeProviderXCore")
@IFMLLoadingPlugin.SortingIndex(1001)
public class MixinCoreMod implements IFMLLoadingPlugin {

    public MixinCoreMod() {
        MixinBootstrap.init();
        Mixins.addConfiguration("mixins.capeproviderx.json");
        MixinEnvironment.getDefaultEnvironment()
            .setObfuscationContext("searge");
    }

    @Override public String[] getASMTransformerClass() { return new String[0]; }
    @Override public String getModContainerClass()    { return null; }
    @Override public String getSetupClass()           { return null; }
    @Override public void injectData(Map<String, Object> data) {}
    @Override public String getAccessTransformerClass() { return null; }
}