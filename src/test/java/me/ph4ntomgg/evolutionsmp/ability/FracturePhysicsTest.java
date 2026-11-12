package me.ph4ntomgg.evolutionsmp.ability;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class FracturePhysicsTest {
    private final World world = mock(World.class);

    @Test
    void radiusUsesThreeDimensionalSphereRatherThanAxisAlignedBox() {
        Location center = new Location(world, 0, 64, 0);
        assertTrue(FracturePhysics.withinRadius(center, new Location(world, 3, 68, 0), 6));
        assertFalse(FracturePhysics.withinRadius(center, new Location(world, 5, 68, 0), 6));
    }

    @Test
    void radiusRejectsDifferentWorldsAndInvalidRadii() {
        Location center = new Location(world, 0, 64, 0);
        assertFalse(FracturePhysics.withinRadius(center,
                new Location(mock(World.class), 0, 64, 0), 6));
        assertFalse(FracturePhysics.withinRadius(center, center, -1));
    }

    @Test
    void launchHasConfiguredHorizontalAndVerticalSpeed() {
        Vector velocity = FracturePhysics.launchVelocity(
                new Location(world, 0, 64, 0),
                new Location(world, 3, 64, 4),
                new Vector(0, 0, 1));
        assertEquals(FracturePhysics.HORIZONTAL_LAUNCH_SPEED,
                Math.hypot(velocity.getX(), velocity.getZ()), 1.0E-9);
        assertEquals(FracturePhysics.VERTICAL_LAUNCH_SPEED, velocity.getY(), 1.0E-9);
    }

    @Test
    void launchUsesFacingWhenTargetIsExactlyAtImpactCenter() {
        Vector velocity = FracturePhysics.launchVelocity(
                new Location(world, 0, 64, 0),
                new Location(world, 0, 64, 0),
                new Vector(0, 0, -1));
        assertEquals(0.0, velocity.getX(), 1.0E-9);
        assertEquals(-FracturePhysics.HORIZONTAL_LAUNCH_SPEED, velocity.getZ(), 1.0E-9);
        assertEquals(FracturePhysics.VERTICAL_LAUNCH_SPEED, velocity.getY(), 1.0E-9);
    }

    @Test
    void launchHasDeterministicFallbackWhenNoDirectionExists() {
        Vector velocity = FracturePhysics.launchVelocity(
                new Location(world, 0, 64, 0),
                new Location(world, 0, 64, 0),
                new Vector(0, 0, 0));
        assertEquals(FracturePhysics.HORIZONTAL_LAUNCH_SPEED, velocity.getX(), 1.0E-9);
        assertEquals(0.0, velocity.getZ(), 1.0E-9);
    }
}
