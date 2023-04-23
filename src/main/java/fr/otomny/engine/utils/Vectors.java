package fr.otomny.engine.utils;

import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public final class Vectors {

  private Vectors() {}

  public static final Vector ZERO = new Vector(0, 0, 0);

  public static Vector lerp(@NotNull Vector from, @NotNull Vector to,
                            double alpha) {
    return new Vector(from.getX() + (alpha * (to.getX() - from.getX())),
                      from.getY() + (alpha * (to.getY() - from.getY())),
                      from.getZ() + (alpha * (to.getZ() - from.getZ())));
  }

  public static Vector interpolate(@NotNull Vector from, @NotNull Vector to,
                                   double alpha,
                                   @NotNull Interpolation interpolation) {
    return lerp(from, to, interpolation.apply(alpha));
  }

  public static Vector divide(Vector vector, double scalar) {
    Vector newVec = vector.clone();
    newVec.setX(vector.getX() / scalar);
    newVec.setY(vector.getY() / scalar);
    newVec.setZ(vector.getZ() / scalar);
    return newVec;
  }

  @FunctionalInterface
  public interface Interpolation {
    Interpolation LINEAR = a -> a;
    Interpolation SMOOTH = a -> a * a * (3 - 2 * a);

    double apply(double a);
  }
}
