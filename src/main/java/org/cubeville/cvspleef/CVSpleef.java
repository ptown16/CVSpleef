package org.cubeville.cvspleef;

import org.bukkit.plugin.java.JavaPlugin;
import org.cubeville.cvgames.CVGames;

public final class CVSpleef extends JavaPlugin {

    static private CVSpleef instance;

    static CVSpleef getInstance() { return instance; }
    
    @Override
    public void onEnable() {
        instance = this;
        CVGames.gameManager().registerGame("spleef", Spleef::new);
    }

}
