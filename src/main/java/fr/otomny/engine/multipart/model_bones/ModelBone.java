package fr.otomny.engine.multipart.model_bones;

import fr.otomny.engine.multipart.Quaternion;
import fr.otomny.engine.multipart.animations.ModelAnimation;
import java.util.concurrent.CompletableFuture;
import org.apache.logging.log4j.CloseableThreadContext.Instance;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface ModelBone {
  CompletableFuture<Void> spawn(Instance instance, Vector position);
  Vector applyTransform(Vector p);
  void draw();
  void destroy();

  Vector simulateTransform(Vector p, String animation, int time);
  Vector simulateRotation(String animation, int time);

  void setState(String state);
  void setParent(ModelBone parent);

  String getName();
  Entity getEntity();
  Vector getOffset();
  ModelBone getParent();
  Vector getPropogatedRotation();

  Vector calculateRotation(Vector p, Vector rotation, Vector pivot);
  Quaternion calculateFinalAngle(Quaternion q);
  Location calculatePosition();
  Vector calculateRotation();

  void addChild(ModelBone child);
  void addAnimation(ModelAnimation animation);
}