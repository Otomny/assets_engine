package fr.otomny.engine.multipart.model_bones;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.CloseableThreadContext.Instance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import fr.otomny.engine.multipart.GenericModel;
import fr.otomny.engine.multipart.ModelEngine;
import fr.otomny.engine.multipart.ModelLoader.AnimationType;
import fr.otomny.engine.multipart.ModelMath;
import fr.otomny.engine.multipart.animations.ModelAnimation;
import fr.otomny.engine.multipart.events.EntityControlEvent;
import fr.otomny.engine.multipart.mount.MobRidable;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.entity.damage.EntityDamage;
import net.minestom.server.event.EventDispatcher;

public abstract class ModelBoneImpl implements ModelBone {
    protected final HashMap<String, ItemStack> items;
    final Point diff;
    final Point pivot;
    private final String name;

    protected final List<ModelAnimation> allAnimations = new ArrayList<>();

    protected Point offset;
    protected Point rotation;
    ModelBone parent;
    protected LivingEntity stand;

    protected final ArrayList<ModelBone> children = new ArrayList<>();
    protected final GenericModel model;

    @Override
    public Entity getEntity() {
        return stand;
    }

    @Override
    public ModelBone getParent() {
        return parent;
    }

    @Override
    public void setParent(ModelBone parent) {
        this.parent = parent;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public ModelBoneImpl(Point pivot, String name, Point rotation, GenericModel model) {
        this.name = name;
        this.rotation = rotation;
        this.model = model;

        this.diff = model.getDiff(name);
        this.offset = model.getOffset(name);

        if (this.diff != null)
            this.pivot = pivot.add(this.diff);
        else
            this.pivot = pivot;

        this.items = ModelEngine.getItems(model.getId(), name);
    }

    public Point calculateGlobalRotation(Point endPos) {
        return calculateRotation(endPos, new Vec(0, model.getGlobalRotation(), 0), this.model.getPivot());
    }

    public Point calculateRotation(Point p, Point rotation, Point pivot) {
        Point position = p.sub(pivot);
        return ModelMath.rotate(position, rotation).add(pivot);
    }

    @Override
    public Point simulateTransform(Point p, String animation, int time) {
        Point endPos = p;

        if (this.diff != null)
            endPos = calculateRotation(endPos, this.simulateRotation(animation, time), this.pivot.sub(this.diff));
        else
            endPos = calculateRotation(endPos, this.simulateRotation(animation, time), this.pivot);

        for (ModelAnimation currentAnimation : this.allAnimations) {
            if (currentAnimation == null || !currentAnimation.name().equals(animation)) continue;

            if (currentAnimation.getType() == AnimationType.TRANSLATION) {
                var calculatedTransform = currentAnimation.getTransformAtTime(time);
                endPos = endPos.add(calculatedTransform);
            }
        }

        if (this.parent != null) {
            endPos = parent.simulateTransform(endPos, animation, time);
        }

        return endPos;
    }

    public Point applyTransform(Point p) {
        Point endPos = p;

        if (this.diff != null)
            endPos = calculateRotation(endPos, this.getPropogatedRotation(), this.pivot.sub(this.diff));
        else
            endPos = calculateRotation(endPos, this.getPropogatedRotation(), this.pivot);

        for (ModelAnimation currentAnimation : this.allAnimations) {
            if (currentAnimation != null && currentAnimation.isPlaying()) {
                if (currentAnimation.getType() == AnimationType.TRANSLATION) {
                    var calculatedTransform = currentAnimation.getTransform();
                    endPos = endPos.add(calculatedTransform);
                }
            }
        }

        if (this.parent != null) {
            endPos = parent.applyTransform(endPos);
        }

        return endPos;
    }

    public Point getPropogatedRotation() {
        Point netTransform = Vec.ZERO;

        for (ModelAnimation currentAnimation : this.allAnimations) {
            if (currentAnimation != null && currentAnimation.isPlaying()) {
                if (currentAnimation.getType() == AnimationType.ROTATION) {
                    Point calculatedTransform = currentAnimation.getTransform();
                    netTransform = netTransform.add(calculatedTransform);
                }
            }
        }

        return this.rotation.add(netTransform);
    }

    public Point simulateRotation(String animation, int time) {
        Point netTransform = Vec.ZERO;

        for (ModelAnimation currentAnimation : this.allAnimations) {
            if (currentAnimation == null || !currentAnimation.name().equals(animation)) continue;

            if (currentAnimation.getType() == AnimationType.ROTATION) {
                Point calculatedTransform = currentAnimation.getTransformAtTime(time);
                netTransform = netTransform.add(calculatedTransform);
            }
        }

        return this.rotation.add(netTransform);
    }

    public Quaternion calculateFinalAngle(Quaternion q) {
        if (this.parent != null) {
            Quaternion pq = parent.calculateFinalAngle(new Quaternion(parent.getPropogatedRotation()));
            q = pq.multiply(q);
        }

        return q;
    }

    public void addAnimation(ModelAnimation animation) {
        this.allAnimations.add(animation);
    }
    public void addChild(ModelBone child) {
        this.children.add(child);
    }

    @Override
    public void destroy() {
        if (this.stand != null) {
            this.stand.setInvisible(true);
            this.stand.remove();
        }

        this.children.clear();
    }

    public CompletableFuture<Void> spawn(Instance instance, Point position) {
        if (this.offset != null && this.stand != null) {
            this.stand.setNoGravity(true);
            this.stand.setSilent(true);
            MinecraftServer.getSchedulerManager().scheduleNextTick(() -> this.stand.setInvisible(true));

            return this.stand.setInstance(instance, position);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Point getOffset() {
        return this.offset;
    }

    public static void hookPart(ModelBoneImpl part, LivingEntity forwardTo) {
        if (part.stand == null || part.model == null) return;

        part.stand.setTag(Tag.String("WSEE"), "part");
        part.stand.eventNode().addListener(EntityDamageEvent.class, (event -> {
            event.setCancelled(true);

            if (forwardTo != null) {
                if (event.getDamageType() instanceof EntityDamage source) {
                    forwardTo.damage(DamageType.fromEntity(source.getSource()), event.getDamage());
                }
            }
        }));

        part.stand.eventNode().addListener(EntityDismountEvent.class, (event -> {
            part.model.dismountEntity(event.getRider());
        }));

        part.stand.eventNode().addListener(EntityControlEvent.class, (event -> {
            if (forwardTo instanceof MobRidable rideable) {
                rideable.getControlGoal().setForward(event.getForward());
                rideable.getControlGoal().setSideways(event.getSideways());
                rideable.getControlGoal().setJump(event.getJump());
            }
        }));

        part.stand.eventNode().addListener(EntityInteractEvent.class, (event -> {
            if (forwardTo != null) {
                EntityInteractEvent entityInteractEvent = new EntityInteractEvent(forwardTo, event.getInteracted());
                EventDispatcher.call(entityInteractEvent);
            }
        }));
    }

    public abstract Pos calculatePosition();
    public abstract Point calculateRotation();
}
