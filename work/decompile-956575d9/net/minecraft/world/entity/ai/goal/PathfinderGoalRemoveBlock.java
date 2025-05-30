package net.minecraft.world.entity.ai.goal;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.core.particles.ParticleParamItem;
import net.minecraft.core.particles.Particles;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3D;

public class PathfinderGoalRemoveBlock extends PathfinderGoalGotoTarget {

    private final Block blockToRemove;
    private final EntityInsentient removerMob;
    private int ticksSinceReachedGoal;
    private static final int WAIT_AFTER_BLOCK_FOUND = 20;

    public PathfinderGoalRemoveBlock(Block block, EntityCreature entitycreature, double d0, int i) {
        super(entitycreature, d0, 24, i);
        this.blockToRemove = block;
        this.removerMob = entitycreature;
    }

    @Override
    public boolean canUse() {
        if (!getServerLevel((Entity) this.removerMob).getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return false;
        } else if (this.nextStartTick > 0) {
            --this.nextStartTick;
            return false;
        } else if (this.findNearestBlock()) {
            this.nextStartTick = reducedTickDelay(20);
            return true;
        } else {
            this.nextStartTick = this.nextStartTick(this.mob);
            return false;
        }
    }

    @Override
    public void stop() {
        super.stop();
        this.removerMob.fallDistance = 1.0D;
    }

    @Override
    public void start() {
        super.start();
        this.ticksSinceReachedGoal = 0;
    }

    public void playDestroyProgressSound(GeneratorAccess generatoraccess, BlockPosition blockposition) {}

    public void playBreakSound(World world, BlockPosition blockposition) {}

    @Override
    public void tick() {
        super.tick();
        World world = this.removerMob.level();
        BlockPosition blockposition = this.removerMob.blockPosition();
        BlockPosition blockposition1 = this.getPosWithBlock(blockposition, world);
        RandomSource randomsource = this.removerMob.getRandom();

        if (this.isReachedTarget() && blockposition1 != null) {
            if (this.ticksSinceReachedGoal > 0) {
                Vec3D vec3d = this.removerMob.getDeltaMovement();

                this.removerMob.setDeltaMovement(vec3d.x, 0.3D, vec3d.z);
                if (!world.isClientSide) {
                    double d0 = 0.08D;

                    ((WorldServer) world).sendParticles(new ParticleParamItem(Particles.ITEM, new ItemStack(Items.EGG)), (double) blockposition1.getX() + 0.5D, (double) blockposition1.getY() + 0.7D, (double) blockposition1.getZ() + 0.5D, 3, ((double) randomsource.nextFloat() - 0.5D) * 0.08D, ((double) randomsource.nextFloat() - 0.5D) * 0.08D, ((double) randomsource.nextFloat() - 0.5D) * 0.08D, (double) 0.15F);
                }
            }

            if (this.ticksSinceReachedGoal % 2 == 0) {
                Vec3D vec3d1 = this.removerMob.getDeltaMovement();

                this.removerMob.setDeltaMovement(vec3d1.x, -0.3D, vec3d1.z);
                if (this.ticksSinceReachedGoal % 6 == 0) {
                    this.playDestroyProgressSound(world, this.blockPos);
                }
            }

            if (this.ticksSinceReachedGoal > 60) {
                world.removeBlock(blockposition1, false);
                if (!world.isClientSide) {
                    for (int i = 0; i < 20; ++i) {
                        double d1 = randomsource.nextGaussian() * 0.02D;
                        double d2 = randomsource.nextGaussian() * 0.02D;
                        double d3 = randomsource.nextGaussian() * 0.02D;

                        ((WorldServer) world).sendParticles(Particles.POOF, (double) blockposition1.getX() + 0.5D, (double) blockposition1.getY(), (double) blockposition1.getZ() + 0.5D, 1, d1, d2, d3, (double) 0.15F);
                    }

                    this.playBreakSound(world, blockposition1);
                }
            }

            ++this.ticksSinceReachedGoal;
        }

    }

    @Nullable
    private BlockPosition getPosWithBlock(BlockPosition blockposition, IBlockAccess iblockaccess) {
        if (iblockaccess.getBlockState(blockposition).is(this.blockToRemove)) {
            return blockposition;
        } else {
            BlockPosition[] ablockposition = new BlockPosition[]{blockposition.below(), blockposition.west(), blockposition.east(), blockposition.north(), blockposition.south(), blockposition.below().below()};

            for (BlockPosition blockposition1 : ablockposition) {
                if (iblockaccess.getBlockState(blockposition1).is(this.blockToRemove)) {
                    return blockposition1;
                }
            }

            return null;
        }
    }

    @Override
    protected boolean isValidTarget(IWorldReader iworldreader, BlockPosition blockposition) {
        IChunkAccess ichunkaccess = iworldreader.getChunk(SectionPosition.blockToSectionCoord(blockposition.getX()), SectionPosition.blockToSectionCoord(blockposition.getZ()), ChunkStatus.FULL, false);

        return ichunkaccess == null ? false : ichunkaccess.getBlockState(blockposition).is(this.blockToRemove) && ichunkaccess.getBlockState(blockposition.above()).isAir() && ichunkaccess.getBlockState(blockposition.above(2)).isAir();
    }
}
