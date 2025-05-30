package net.minecraft.world.level;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleParam;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.IChunkProvider;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.redstone.NeighborUpdater;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.ticks.NextTickListEntry;
import net.minecraft.world.ticks.TickListPriority;

public interface GeneratorAccess extends ICombinedAccess, IWorldTime, ScheduledTickAccess {

    @Override
    default long dayTime() {
        return this.getLevelData().getDayTime();
    }

    long nextSubTickCount();

    @Override
    default <T> NextTickListEntry<T> createTick(BlockPosition blockposition, T t0, int i, TickListPriority ticklistpriority) {
        return new NextTickListEntry<T>(t0, blockposition, this.getLevelData().getGameTime() + (long) i, ticklistpriority, this.nextSubTickCount());
    }

    @Override
    default <T> NextTickListEntry<T> createTick(BlockPosition blockposition, T t0, int i) {
        return new NextTickListEntry<T>(t0, blockposition, this.getLevelData().getGameTime() + (long) i, this.nextSubTickCount());
    }

    WorldData getLevelData();

    DifficultyDamageScaler getCurrentDifficultyAt(BlockPosition blockposition);

    @Nullable
    MinecraftServer getServer();

    default EnumDifficulty getDifficulty() {
        return this.getLevelData().getDifficulty();
    }

    IChunkProvider getChunkSource();

    @Override
    default boolean hasChunk(int i, int j) {
        return this.getChunkSource().hasChunk(i, j);
    }

    RandomSource getRandom();

    default void updateNeighborsAt(BlockPosition blockposition, Block block) {}

    default void neighborShapeChanged(EnumDirection enumdirection, BlockPosition blockposition, BlockPosition blockposition1, IBlockData iblockdata, int i, int j) {
        NeighborUpdater.executeShapeUpdate(this, enumdirection, blockposition, blockposition1, iblockdata, i, j - 1);
    }

    default void playSound(@Nullable Entity entity, BlockPosition blockposition, SoundEffect soundeffect, SoundCategory soundcategory) {
        this.playSound(entity, blockposition, soundeffect, soundcategory, 1.0F, 1.0F);
    }

    void playSound(@Nullable Entity entity, BlockPosition blockposition, SoundEffect soundeffect, SoundCategory soundcategory, float f, float f1);

    void addParticle(ParticleParam particleparam, double d0, double d1, double d2, double d3, double d4, double d5);

    void levelEvent(@Nullable Entity entity, int i, BlockPosition blockposition, int j);

    default void levelEvent(int i, BlockPosition blockposition, int j) {
        this.levelEvent((Entity) null, i, blockposition, j);
    }

    void gameEvent(Holder<GameEvent> holder, Vec3D vec3d, GameEvent.a gameevent_a);

    default void gameEvent(@Nullable Entity entity, Holder<GameEvent> holder, Vec3D vec3d) {
        this.gameEvent(holder, vec3d, new GameEvent.a(entity, (IBlockData) null));
    }

    default void gameEvent(@Nullable Entity entity, Holder<GameEvent> holder, BlockPosition blockposition) {
        this.gameEvent(holder, blockposition, new GameEvent.a(entity, (IBlockData) null));
    }

    default void gameEvent(Holder<GameEvent> holder, BlockPosition blockposition, GameEvent.a gameevent_a) {
        this.gameEvent(holder, Vec3D.atCenterOf(blockposition), gameevent_a);
    }

    default void gameEvent(ResourceKey<GameEvent> resourcekey, BlockPosition blockposition, GameEvent.a gameevent_a) {
        this.gameEvent(this.registryAccess().lookupOrThrow(Registries.GAME_EVENT).getOrThrow(resourcekey), blockposition, gameevent_a);
    }
}
