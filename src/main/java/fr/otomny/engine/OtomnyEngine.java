package fr.otomny.engine;

import org.bukkit.plugin.java.JavaPlugin;

public class OtomnyEngine extends JavaPlugin {

  public static OtomnyEngine instance;

  public static OtomnyEngine get(){
    return instance;
  }

  @Override
  public void onEnable() {
    instance = this;
  }

  @Override
  public void onDisable() {

  }
}
