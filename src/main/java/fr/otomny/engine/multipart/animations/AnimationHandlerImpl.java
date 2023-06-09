package fr.otomny.engine.multipart.animations;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.otomny.engine.OtomnyEngine;
import fr.otomny.engine.multipart.GenericModel;
import fr.otomny.engine.multipart.ModelLoader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public class AnimationHandlerImpl implements AnimationHandler {
  private final Map<String, Integer> animationTimes = new HashMap<>();
  private final Map<String, Integer> animationPriorities = new HashMap<>();
  private final Map<String, AnimationDirection> direction = new HashMap<>();

  private final LinkedHashMap<String, Set<ModelAnimation>> animations =
      new LinkedHashMap<>();
  private final GenericModel model;
  private final BukkitTask task;

  TreeMap<Integer, String> repeating = new TreeMap<>();
  String playingOnce = null;

  Map<String, Consumer<Void>> callbacks = new ConcurrentHashMap<>();
  Map<String, Integer> callbackTimers = new ConcurrentHashMap<>();

  public AnimationHandlerImpl(GenericModel model) {
    this.model = model;
    loadDefaultAnimations();

    this.task = Bukkit.getScheduler().runTaskTimerAsynchronously(
        OtomnyEngine.get(), this::tick, 0, 1);
  }

  protected void loadDefaultAnimations() {
    JsonObject loadedAnimations = ModelLoader.loadAnimations(model.getId());
    // Init animation
    int i = 0;
    for (Map.Entry<String, JsonElement> animation :
         loadedAnimations.get("animations").getAsJsonObject().entrySet()) {
      registerAnimation(animation.getKey(), animation.getValue(), i);
      i--;
    }
  }

  @Override
  public void registerAnimation(String name, JsonElement animation,
                                int priority) {
    final JsonElement animationLength =
        animation.getAsJsonObject().get("animation_length");
    final double length =
        animationLength == null ? 0 : animationLength.getAsDouble();

    HashSet<ModelAnimation> animationSet = new HashSet<>();
    for (Map.Entry<String, JsonElement> boneEntry : animation.getAsJsonObject()
                                                        .get("bones")
                                                        .getAsJsonObject()
                                                        .entrySet()) {
      String boneName = boneEntry.getKey();
      var bone = model.getPart(boneName);
      if (bone == null)
        continue;

      JsonElement animationRotation =
          boneEntry.getValue().getAsJsonObject().get("rotation");
      JsonElement animationPosition =
          boneEntry.getValue().getAsJsonObject().get("position");

      if (animationRotation != null) {
        ModelAnimation boneAnimation = new ModelAnimation(
            model.getId(), name, boneName, bone, animationRotation,
            ModelLoader.AnimationType.ROTATION, length);
        animationSet.add(boneAnimation);
      }
      if (animationPosition != null) {
        ModelAnimation boneAnimation = new ModelAnimation(
            model.getId(), name, boneName, bone, animationPosition,
            ModelLoader.AnimationType.TRANSLATION, length);
        animationSet.add(boneAnimation);
      }
    }

    animationTimes.put(name, (int)(length * 20));
    animationPriorities.put(name, priority);
    animations.put(name, animationSet);
  }

  public void playRepeat(String animation) {
    playRepeat(animation, AnimationDirection.FORWARD);
  }

  @Override
  public void playRepeat(String animation, AnimationDirection direction) {
    if (this.repeating.containsKey(this.animationPriorities().get(animation)) &&
        this.direction.get(animation) == direction)
      return;

    this.direction.put(animation, direction);

    this.repeating.put(this.animationPriorities().get(animation), animation);
    var top = this.repeating.firstEntry();

    if (playingOnce != null) {
      if (this.callbacks.get(playingOnce) != null) {
        this.callbacks.get(playingOnce).accept(null);
        this.callbacks.remove(playingOnce);
        this.callbackTimers.remove(playingOnce);
      }

      this.animations.get(playingOnce).forEach(ModelAnimation::stop);
    }

    if (top != null && animation.equals(top.getValue())) {
      this.repeating.values().forEach(v -> {
        if (!v.equals(animation)) {
          this.animations.get(v).forEach(ModelAnimation::stop);
        }
      });
      this.animations.get(animation).forEach(a -> a.setDirection(direction));
      this.animations.get(animation).forEach(ModelAnimation::play);
    }
  }

  public void stopRepeat(String animation) {
    this.animations.get(animation).forEach(ModelAnimation::stop);
    int priority = this.animationPriorities().get(animation);

    Map.Entry<Integer, String> currentTop = this.repeating.firstEntry();

    this.repeating.remove(priority);

    Map.Entry<Integer, String> firstEntry = this.repeating.firstEntry();

    if (firstEntry != null && currentTop != null &&
        !firstEntry.getKey().equals(currentTop.getKey())) {
      this.animations.get(firstEntry.getValue()).forEach(ModelAnimation::play);
    }
  }

  public void playOnce(String animation, Consumer<Void> cb) {
    this.playOnce(animation, AnimationDirection.FORWARD, cb);
  }

  @Override
  public void playOnce(String animation, AnimationDirection direction,
                       Consumer<Void> cb) {
    AnimationDirection currentDirection = this.direction.get(animation);
    this.direction.put(animation, direction);

    if (this.callbacks.containsKey(animation)) {
      this.callbacks.get(animation).accept(null);
    }

    int callbackTimer = this.callbackTimers.getOrDefault(animation, 0);

    if (animation.equals(this.playingOnce) &&
        direction == AnimationDirection.PAUSE && callbackTimer > 0) {
      // Pause. Only call if we're not stopped
      playingOnce = animation;
      this.animations.get(animation).forEach(a -> a.setDirection(direction));
      this.callbacks.put(animation, cb);
    } else if (animation.equals(this.playingOnce) &&
               currentDirection != direction) {
      playingOnce = animation;
      this.animations.get(animation).forEach(a -> a.setDirection(direction));
      this.callbacks.put(animation, cb);
      if (currentDirection != AnimationDirection.PAUSE)
        this.callbackTimers.put(animation, animationTimes.get(animation) -
                                               callbackTimer + 1);
    } else if (direction != AnimationDirection.PAUSE) {
      if (playingOnce != null)
        this.animations.get(playingOnce).forEach(ModelAnimation::stop);
      playingOnce = animation;

      this.callbacks.put(animation, cb);
      this.callbackTimers.put(animation, animationTimes.get(animation));
      this.animations.get(animation).forEach(a -> a.setDirection(direction));
      this.animations.get(animation).forEach(ModelAnimation::play);

      this.repeating.values().forEach(v -> {
        if (!v.equals(animation)) {
          this.animations.get(v).forEach(ModelAnimation::stop);
        }
      });
    }
  }

  private void tick() {
    try {
      for (Map.Entry<String, Integer> entry : callbackTimers.entrySet()) {
        if (entry.getValue() <= 0) {
          if (this.playingOnce != null &&
              this.playingOnce.equals(entry.getKey())) {
            Map.Entry<Integer, String> firstEntry = this.repeating.firstEntry();
            if (firstEntry != null) {
              this.animations.get(firstEntry.getValue())
                  .forEach(ModelAnimation::play);
            }

            this.playingOnce = null;
          }

          this.model.triggerAnimationEnd(entry.getKey(),
                                         this.direction.get(entry.getKey()));

          animations.get(entry.getKey()).forEach(ModelAnimation::stop);
          callbackTimers.remove(entry.getKey());

          var cb = callbacks.remove(entry.getKey());
          if (cb != null)
            cb.accept(null);
        } else {
          if (this.direction.get(entry.getKey()) != AnimationDirection.PAUSE) {
            callbackTimers.put(entry.getKey(), entry.getValue() - 1);
          }
        }
      }

      if (callbacks.size() + repeating.size() == 0)
        return;
      this.model.draw();

      this.animations.forEach((animation, animations) -> {
        animations.forEach(ModelAnimation::tick);
      });
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void destroy() { this.task.cancel(); }

  public String getPlaying() {
    if (this.playingOnce != null)
      return this.playingOnce;
    var playing = this.repeating.firstEntry();
    return playing != null ? playing.getValue() : null;
  }

  @Override
  public Map<String, Integer> animationPriorities() {
    return animationPriorities;
  }
}