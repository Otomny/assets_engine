package fr.otomny.engine.multipart;

import fr.otomny.engine.multipart.animations.AnimationHandlerImpl;
import fr.otomny.engine.multipart.model_bones.ModelBone;
import java.util.List;
import java.util.Set;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.ApiStatus;

public interface GenericModel {
  /**
   * Get the ID of the model
   * @return the model ID
   */
  String getId();

  /**
   * Get the pivot point of the model. Used for global rotation
   * @return the global rotation pivot point
   */
  Vector getPivot();

  /**
   * Get the rotation of the model on the Y axis
   * @return the global rotation
   */
  double getGlobalRotation();

  /**
   * Set the rotation of the model on the Y axis
   * @param rotation new global rotation
   */
  void setGlobalRotation(double rotation);

  /**
   * Get the postion offset for drawing the model
   * @return the position
   */
  Vector getGlobalOffset();

  /**
   * Get the position the model is being drawn at
   * @return the model position
   */
  Vector getPosition();

  /**
   * Set the position of the model
   * @param pos new model position
   */
  void setPosition(Vector pos);

  /**
   * Set the state of the model. By default, `normal` and `hit` are supported
   * @param state the new state
   */
  void setState(String state);

  /**
   * Destroy the model
   */
  void destroy();

  /**
   * Remove all hitboxes from the model
   */
  void removeHitboxes();

  void mountEntity(Entity entity);
  void dismountEntity(Entity entity);
  Set<Entity> getPassengers();

  /**
   * Get a VFX bone location
   * @param name the name of the bone
   * @return the bone location
   */
  Vector getVFX(String name);

  @ApiStatus.Internal ModelBone getPart(String boneName);

  @ApiStatus.Internal void draw();

  ModelConfig config();

  /**
   * Set the model's head rotation
   * @param rotation rotation of head
   */
  void setHeadRotation(double rotation);

  List<Entity> getParts();

  ModelBone getSeat();

  /**
   * Check where a bone will be at a specified time during a specified animation
   * @param animation animation
   * @param bone bone name
   * @param time time in ticks
   * @return position of bone
   */
  Vector getBoneAtTime(String animation, String bone, int time);

  /**
   * Set the entity used for the nametag
   * Takes over control of entity movement.
   * @param entity the entity
   */
  void setNametagEntity(LivingEntity entity);
  LivingEntity getNametagEntity();

  World getInstance();

  Vector getOffset(String bone);
  Vector getDiff(String bone);

  void triggerAnimationEnd(String animation,
                           AnimationHandlerImpl.AnimationDirection direction);
}
