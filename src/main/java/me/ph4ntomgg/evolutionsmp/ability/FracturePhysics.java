package me.ph4ntomgg.evolutionsmp.ability;

import org.bukkit.Location;
import org.bukkit.util.Vector;

/**
 * Shared, deterministic Fracture hit-test and launch calculations.
 * Kept separate from Bukkit scheduling so the physics can be regression-tested.
 */
final class FracturePhysics {
    static final double HORIZONTAL_LAUNCH_SPEED = 0.32;
    static final double VERTICAL_LAUNCH_SPEED = 1.15;

    private FracturePhysics() { }

    static boolean withinRadius(Location center, Location target, double radius) {
        if (center == null || target == null || center.getWorld() == null
                || center.getWorld() != target.getWorld()
                || !Double.isFinite(radius) || radius < 0.0) {
            return false;
        }
        return center.distanceSquared(target) <= radius * radius;
    }

    static Vector launchVelocity(Location origin, Location target, Vector fallbackDirection) {
        Vector horizontal = target.toVector().subtract(origin.toVector()).setY(0.0);
        if (horizontal.lengthSquared() < 1.0E-6 && fallbackDirection != null) {
            horizontal = fallbackDirection.clone().setY(0.0);
        }
        if (horizontal.lengthSquared() < 1.0E-6) {
            horizontal = new Vector(1.0, 0.0, 0.0);
        }
        return horizontal.normalize().multiply(HORIZONTAL_LAUNCH_SPEED)
                .setY(VERTICAL_LAUNCH_SPEED);
    }
}
