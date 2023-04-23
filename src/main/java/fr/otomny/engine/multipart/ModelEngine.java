package fr.otomny.engine.multipart;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.otomny.engine.utils.Vectors;
import java.io.Reader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

public class ModelEngine {
  static final Gson GSON =
      new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

  private final static HashMap<String, HashMap<String, ItemStack>>
      blockMappings = new HashMap<>();
  public final static HashMap<String, Vector> offsetMappings = new HashMap<>();
  public final static HashMap<String, Vector> diffMappings = new HashMap<>();
  private static Path modelPath;

  /**
   * Loads the model from the given path
   * @param mappingsData mappings file created by model parser
   * @param modelPath path of the models
   */
  public static void loadMappings(Reader mappingsData, Path modelPath) {
    JsonObject map = GSON.fromJson(mappingsData, JsonObject.class);
    ModelEngine.modelPath = modelPath;

    blockMappings.clear();
    offsetMappings.clear();
    diffMappings.clear();
    ModelLoader.clearCache();

    map.entrySet().forEach(entry -> {
      HashMap<String, ItemStack> keys = new HashMap<>();

      entry.getValue()
          .getAsJsonObject()
          .get("id")
          .getAsJsonObject()
          .entrySet()
          .forEach(id
                   -> keys.put(id.getKey(),
                               generateBoneItem(id.getValue().getAsInt())));

      blockMappings.put(entry.getKey(), keys);
      offsetMappings.put(
          entry.getKey(),
          getPos(
              entry.getValue().getAsJsonObject().get("offset").getAsJsonArray())
              .orElse(Vectors.ZERO));
      diffMappings.put(
          entry.getKey(),
          getPos(
              entry.getValue().getAsJsonObject().get("diff").getAsJsonArray())
              .orElse(Vectors.ZERO));
    });
  }

  private static ItemStack generateBoneItem(int model_id) {
    Material material = Material.LEATHER_HORSE_ARMOR;

    ItemStack itemStack = new ItemStack(material);
    ItemMeta meta = itemStack.getItemMeta();
    meta.displayName(Component.empty());
    meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DESTROYS,
                      ItemFlag.HIDE_DYE, ItemFlag.HIDE_ENCHANTS,
                      ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_POTION_EFFECTS,
                      ItemFlag.HIDE_UNBREAKABLE);
    meta.setCustomModelData(model_id);
    itemStack.setItemMeta(meta);
    return itemStack;
  }

  public static HashMap<String, ItemStack> getItems(String model, String name) {
    return blockMappings.get(model + "/" + name);
  }

  public static String getGeoPath(String id) {
    return modelPath + "/" + id + "/model.geo.json";
  }
  public static String getAnimationPath(String id) {
    return modelPath + "/" + id + "/model.animation.json";
  }

  public static Optional<Vector> getPos(JsonElement pivot) {
    if (pivot == null)
      return Optional.empty();
    else {
      JsonArray arr = pivot.getAsJsonArray();
      return Optional.of(new Vector(arr.get(0).getAsDouble(),
                                    arr.get(1).getAsDouble(),
                                    arr.get(2).getAsDouble()));
    }
  }
}
