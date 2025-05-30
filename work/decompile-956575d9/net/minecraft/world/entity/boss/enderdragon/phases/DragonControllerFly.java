package net.minecraft.world.entity.boss.enderdragon.phases;

import javax.annotation.Nullable;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.boss.enderdragon.EntityEnderDragon;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.feature.WorldGenEndTrophy;
import net.minecraft.world.level.pathfinder.PathEntity;
import net.minecraft.world.level.pathfinder.PathPoint;
import net.minecraft.world.phys.Vec3D;

public class DragonControllerFly extends AbstractDragonController {

    private boolean firstTick;
    @Nullable
    private PathEntity currentPath;
    @Nullable
    private Vec3D targetLocation;

    public DragonControllerFly(EntityEnderDragon entityenderdragon) {
        super(entityenderdragon);
    }

    @Override
    public void doServerTick(WorldServer worldserver) {
        if (!this.firstTick && this.currentPath != null) {
            BlockPosition blockposition = worldserver.getHeightmapPos(HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, WorldGenEndTrophy.getLocation(this.dragon.getFightOrigin()));

            if (!blockposition.closerToCenterThan(this.dragon.position(), 10.0D)) {
                this.dragon.getPhaseManager().setPhase(DragonControllerPhase.HOLDING_PATTERN);
            }
        } else {
            this.firstTick = false;
            this.findNewTarget();
        }

    }

    @Override
    public void begin() {
        this.firstTick = true;
        this.currentPath = null;
        this.targetLocation = null;
    }

    private void findNewTarget() {
        int i = this.dragon.findClosestNode();
        Vec3D vec3d = this.dragon.getHeadLookVector(1.0F);
        int j = this.dragon.findClosestNode(-vec3d.x * 40.0D, 105.0D, -vec3d.z * 40.0D);

        if (this.dragon.getDragonFight() != null && this.dragon.getDragonFight().getCrystalsAlive() > 0) {
            j %= 12;
            if (j < 0) {
                j += 12;
            }
        } else {
            j -= 12;
            j &= 7;
            j += 12;
        }

        this.currentPath = this.dragon.findPath(i, j, (PathPoint) null);
        this.navigateToNextPathNode();
    }

    private void navigateToNextPathNode() {
        if (this.currentPath != null) {
            this.currentPath.advance();
            if (!this.currentPath.isDone()) {
                BaseBlockPosition baseblockposition = this.currentPath.getNextNodePos();

                this.currentPath.advance();

                double d0;

                do {
                    d0 = (double) ((float) ((BaseBlockPosition) baseblockposition).getY() + this.dragon.getRandom().nextFloat() * 20.0F);
                } while (d0 < (double) ((BaseBlockPosition) baseblockposition).getY());

                this.targetLocation = new Vec3D((double) baseblockposition.getX(), d0, (double) baseblockposition.getZ());
            }
        }

    }

    @Nullable
    @Override
    public Vec3D getFlyTargetLocation() {
        return this.targetLocation;
    }

    @Override
    public DragonControllerPhase<DragonControllerFly> getPhase() {
        return DragonControllerPhase.TAKEOFF;
    }
}
