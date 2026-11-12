package org.spectator164.core;

import java.util.Map;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.MCVersion("1.6.4")
@IFMLLoadingPlugin.SortingIndex(1001)
@IFMLLoadingPlugin.TransformerExclusions({"org.spectator164.core"})
public final class SpectatorCore implements IFMLLoadingPlugin {
    public String[] getASMTransformerClass() { return new String[] { "org.spectator164.core.SpectatorTransformer" }; }
    public String getModContainerClass() { return null; }
    public String getSetupClass() { return null; }
    public void injectData(Map<String, Object> data) { }
}

