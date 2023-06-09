package fr.otomny.engine.multipart;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.util.Vector;

public class ModelLoader {
  protected static final Gson GSON =
      new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
  private static final Map<String, JsonObject> loadedAnimations =
      new HashMap<>();
  private static final Map<String, JsonObject> loadedModels = new HashMap<>();

  // <Model> -> <Bone/Animation>
  private static final Map<String, Map<String, Map<Short, Vector>>>
      interpolationTranslateCache = new HashMap<>();
  private static final Map<String, Map<String, Map<Short, Vector>>>
      interpolationRotateCache = new HashMap<>();

  public static void clearCache() {
    interpolationTranslateCache.clear();
    interpolationRotateCache.clear();
    loadedAnimations.clear();
    loadedModels.clear();
  }

  public enum AnimationType { ROTATION, TRANSLATION }

  public static JsonObject loadAnimations(String toLoad) {
    if (loadedAnimations.containsKey(toLoad))
      return loadedAnimations.get(toLoad);

    JsonObject loadedAnimations1;

    try {
      loadedAnimations1 =
          GSON.fromJson(new InputStreamReader(new FileInputStream(
                            ModelEngine.getAnimationPath(toLoad))),
                        JsonObject.class);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      loadedAnimations1 = null;
    }

    loadedAnimations.put(toLoad, loadedAnimations1);
    return loadedAnimations1;
  }

  public static JsonObject loadModel(String id) {
    if (loadedModels.containsKey(id))
      return loadedModels.get(id);

    JsonObject loadedModel1;
    try {
      loadedModel1 = GSON.fromJson(new InputStreamReader(new FileInputStream(
                                       ModelEngine.getGeoPath(id))),
                                   JsonObject.class);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      loadedModel1 = null;
    }

    loadedModels.put(id, loadedModel1);
    return loadedModel1;
  }

  public static void addToTranslationCache(String key, String model,
                                           Map<Short, Vector> val) {
    if (!interpolationTranslateCache.containsKey(model))
      interpolationTranslateCache.put(model, new HashMap<>());

    interpolationTranslateCache.get(model).put(key, val);
  }

  public static void addToRotationCache(String key, String model,
                                        Map<Short, Vector> val) {
    if (!interpolationRotateCache.containsKey(model))
      interpolationRotateCache.put(model, new HashMap<>());

    interpolationRotateCache.get(model).put(key, val);
  }

  public static Map<Short, Vector> getCacheRotation(String key, String model) {
    Map<String, Map<Short, Vector>> m = interpolationRotateCache.get(model);
    if (m == null)
      return null;
    return m.get(key);
  }

  public static Map<Short, Vector> getCacheTranslation(String key,
                                                       String model) {
    Map<String, Map<Short, Vector>> m = interpolationTranslateCache.get(model);
    if (m == null)
      return null;
    return m.get(key);
  }

  public static Map<String, JsonObject>
  parseAnimations(String animationString) {
    Map<String, JsonObject> res = new LinkedHashMap<>();

    JsonObject animations =
        GSON.fromJson(new StringReader(animationString), JsonObject.class);
    for (Map.Entry<String, JsonElement> animation :
         animations.get("animations").getAsJsonObject().entrySet()) {
      res.put(animation.getKey(), animation.getValue().getAsJsonObject());
    }

    return res;
  }
}
