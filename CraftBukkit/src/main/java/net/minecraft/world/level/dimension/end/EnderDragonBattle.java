package net.minecraft.world.level.dimension.end;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.EndFeatures;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.level.BossBattleServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossBattle;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.IEntitySelector;
import net.minecraft.world.entity.boss.enderdragon.EntityEnderCrystal;
import net.minecraft.world.entity.boss.enderdragon.EntityEnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonControllerPhase;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityEnderPortal;
import net.minecraft.world.level.block.state.pattern.ShapeDetector;
import net.minecraft.world.level.block.state.pattern.ShapeDetectorBlock;
import net.minecraft.world.level.block.state.pattern.ShapeDetectorBuilder;
import net.minecraft.world.level.block.state.predicate.BlockPredicate;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.feature.WorldGenEndTrophy;
import net.minecraft.world.level.levelgen.feature.WorldGenEnder;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureConfigured;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureConfiguration;
import net.minecraft.world.phys.AxisAlignedBB;
import org.slf4j.Logger;

public class EnderDragonBattle {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_TICKS_BEFORE_DRAGON_RESPAWN = 1200;
    private static final int TIME_BETWEEN_CRYSTAL_SCANS = 100;
    public static final int TIME_BETWEEN_PLAYER_SCANS = 20;
    private static final int ARENA_SIZE_CHUNKS = 8;
    public static final int ARENA_TICKET_LEVEL = 9;
    private static final int GATEWAY_COUNT = 20;
    private static final int GATEWAY_DISTANCE = 96;
    public static final int DRAGON_SPAWN_Y = 128;
    private final Predicate<Entity> validPlayer;
    public final BossBattleServer dragonEvent;
    public final WorldServer level;
    private final BlockPosition origin;
    private final ObjectArrayList<Integer> gateways;
    private final ShapeDetector exitPortalPattern;
    private int ticksSinceDragonSeen;
    private int crystalsAlive;
    private int ticksSinceCrystalsScanned;
    private int ticksSinceLastPlayerScan;
    private boolean dragonKilled;
    public boolean previouslyKilled;
    private boolean skipArenaLoadedCheck;
    @Nullable
    public UUID dragonUUID;
    private boolean needsStateScanning;
    @Nullable
    public BlockPosition portalLocation;
    @Nullable
    public EnumDragonRespawn respawnStage;
    private int respawnTime;
    @Nullable
    private List<EntityEnderCrystal> respawnCrystals;

    public EnderDragonBattle(WorldServer worldserver, long i, EnderDragonBattle.a enderdragonbattle_a) {
        this(worldserver, i, enderdragonbattle_a, BlockPosition.ZERO);
    }

    public EnderDragonBattle(WorldServer worldserver, long i, EnderDragonBattle.a enderdragonbattle_a, BlockPosition blockposition) {
        this.dragonEvent = (BossBattleServer) (new BossBattleServer(IChatBaseComponent.translatable("entity.minecraft.ender_dragon"), BossBattle.BarColor.PINK, BossBattle.BarStyle.PROGRESS)).setPlayBossMusic(true).setCreateWorldFog(true);
        this.gateways = new ObjectArrayList();
        this.ticksSinceLastPlayerScan = 21;
        this.skipArenaLoadedCheck = false;
        this.needsStateScanning = true;
        this.level = worldserver;
        this.origin = blockposition;
        this.validPlayer = IEntitySelector.ENTITY_STILL_ALIVE.and(IEntitySelector.withinDistance((double) blockposition.getX(), (double) (128 + blockposition.getY()), (double) blockposition.getZ(), 192.0D));
        this.needsStateScanning = enderdragonbattle_a.needsStateScanning;
        this.dragonUUID = (UUID) enderdragonbattle_a.dragonUUID.orElse(null); // CraftBukkit - decompile error
        this.dragonKilled = enderdragonbattle_a.dragonKilled;
        this.previouslyKilled = enderdragonbattle_a.previouslyKilled;
        if (enderdragonbattle_a.isRespawning) {
            this.respawnStage = EnumDragonRespawn.START;
        }

        this.portalLocation = (BlockPosition) enderdragonbattle_a.exitPortalLocation.orElse(null); // CraftBukkit - decompile error
        this.gateways.addAll((Collection) enderdragonbattle_a.gateways.orElseGet(() -> {
            ObjectArrayList<Integer> objectarraylist = new ObjectArrayList(ContiguousSet.create(Range.closedOpen(0, 20), DiscreteDomain.integers()));

            SystemUtils.shuffle(objectarraylist, RandomSource.create(i));
            return objectarraylist;
        }));
        this.exitPortalPattern = ShapeDetectorBuilder.start().aisle("       ", "       ", "       ", "   #   ", "       ", "       ", "       ").aisle("       ", "       ", "       ", "   #   ", "       ", "       ", "       ").aisle("       ", "       ", "       ", "   #   ", "       ", "       ", "       ").aisle("  ###  ", " #   # ", "#     #", "#  #  #", "#     #", " #   # ", "  ###  ").aisle("       ", "  ###  ", " ##### ", " ##### ", " ##### ", "  ###  ", "       ").where('#', ShapeDetectorBlock.hasState(BlockPredicate.forBlock(Blocks.BEDROCK))).build();
    }

    /** @deprecated */
    @Deprecated
    @VisibleForTesting
    public void skipArenaLoadedCheck() {
        this.skipArenaLoadedCheck = true;
    }

    public EnderDragonBattle.a saveData() {
        return new EnderDragonBattle.a(this.needsStateScanning, this.dragonKilled, this.previouslyKilled, false, Optional.ofNullable(this.dragonUUID), Optional.ofNullable(this.portalLocation), Optional.of(this.gateways));
    }

    public void tick() {
        this.dragonEvent.setVisible(!this.dragonKilled);
        if (++this.ticksSinceLastPlayerScan >= 20) {
            this.updatePlayers();
            this.ticksSinceLastPlayerScan = 0;
        }

        if (!this.dragonEvent.getPlayers().isEmpty()) {
            this.level.getChunkSource().addTicketWithRadius(TicketType.DRAGON, new ChunkCoordIntPair(0, 0), 9);
            boolean flag = this.isArenaLoaded();

            if (this.needsStateScanning && flag) {
                this.scanState();
                this.needsStateScanning = false;
            }

            if (this.respawnStage != null) {
                if (this.respawnCrystals == null && flag) {
                    this.respawnStage = null;
                    this.tryRespawn();
                }

                this.respawnStage.tick(this.level, this, this.respawnCrystals, this.respawnTime++, this.portalLocation);
            }

            if (!this.dragonKilled) {
                if ((this.dragonUUID == null || ++this.ticksSinceDragonSeen >= 1200) && flag) {
                    this.findOrCreateDragon();
                    this.ticksSinceDragonSeen = 0;
                }

                if (++this.ticksSinceCrystalsScanned >= 100 && flag) {
                    this.updateCrystalCount();
                    this.ticksSinceCrystalsScanned = 0;
                }
            }
        } else {
            this.level.getChunkSource().removeTicketWithRadius(TicketType.DRAGON, new ChunkCoordIntPair(0, 0), 9);
        }

    }

    private void scanState() {
        EnderDragonBattle.LOGGER.info("Scanning for legacy world dragon fight...");
        boolean flag = this.hasActiveExitPortal();

        if (flag) {
            EnderDragonBattle.LOGGER.info("Found that the dragon has been killed in this world already.");
            this.previouslyKilled = true;
        } else {
            EnderDragonBattle.LOGGER.info("Found that the dragon has not yet been killed in this world.");
            this.previouslyKilled = false;
            if (this.findExitPortal() == null) {
                this.spawnExitPortal(false);
            }
        }

        List<? extends EntityEnderDragon> list = this.level.getDragons();

        if (list.isEmpty()) {
            this.dragonKilled = true;
        } else {
            EntityEnderDragon entityenderdragon = (EntityEnderDragon) list.get(0);

            this.dragonUUID = entityenderdragon.getUUID();
            EnderDragonBattle.LOGGER.info("Found that there's a dragon still alive ({})", entityenderdragon);
            this.dragonKilled = false;
            if (!flag) {
                EnderDragonBattle.LOGGER.info("But we didn't have a portal, let's remove it.");
                entityenderdragon.discard(null); // CraftBukkit - add Bukkit remove cause
                this.dragonUUID = null;
            }
        }

        if (!this.previouslyKilled && this.dragonKilled) {
            this.dragonKilled = false;
        }

    }

    private void findOrCreateDragon() {
        List<? extends EntityEnderDragon> list = this.level.getDragons();

        if (list.isEmpty()) {
            EnderDragonBattle.LOGGER.debug("Haven't seen the dragon, respawning it");
            this.createNewDragon();
        } else {
            EnderDragonBattle.LOGGER.debug("Haven't seen our dragon, but found another one to use.");
            this.dragonUUID = ((EntityEnderDragon) list.get(0)).getUUID();
        }

    }

    public void setRespawnStage(EnumDragonRespawn enumdragonrespawn) {
        if (this.respawnStage == null) {
            throw new IllegalStateException("Dragon respawn isn't in progress, can't skip ahead in the animation.");
        } else {
            this.respawnTime = 0;
            if (enumdragonrespawn == EnumDragonRespawn.END) {
                this.respawnStage = null;
                this.dragonKilled = false;
                EntityEnderDragon entityenderdragon = this.createNewDragon();

                if (entityenderdragon != null) {
                    for (EntityPlayer entityplayer : this.dragonEvent.getPlayers()) {
                        CriterionTriggers.SUMMONED_ENTITY.trigger(entityplayer, entityenderdragon);
                    }
                }
            } else {
                this.respawnStage = enumdragonrespawn;
            }

        }
    }

    private boolean hasActiveExitPortal() {
        for (int i = -8; i <= 8; ++i) {
            for (int j = -8; j <= 8; ++j) {
                Chunk chunk = this.level.getChunk(i, j);

                for (TileEntity tileentity : chunk.getBlockEntities().values()) {
                    if (tileentity instanceof TileEntityEnderPortal) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Nullable
    public ShapeDetector.ShapeDetectorCollection findExitPortal() {
        ChunkCoordIntPair chunkcoordintpair = new ChunkCoordIntPair(this.origin);

        for (int i = -8 + chunkcoordintpair.x; i <= 8 + chunkcoordintpair.x; ++i) {
            for (int j = -8 + chunkcoordintpair.z; j <= 8 + chunkcoordintpair.z; ++j) {
                Chunk chunk = this.level.getChunk(i, j);

                for (TileEntity tileentity : chunk.getBlockEntities().values()) {
                    if (tileentity instanceof TileEntityEnderPortal) {
                        ShapeDetector.ShapeDetectorCollection shapedetector_shapedetectorcollection = this.exitPortalPattern.find(this.level, tileentity.getBlockPos());

                        if (shapedetector_shapedetectorcollection != null) {
                            BlockPosition blockposition = shapedetector_shapedetectorcollection.getBlock(3, 3, 3).getPos();

                            if (this.portalLocation == null) {
                                this.portalLocation = blockposition;
                            }

                            return shapedetector_shapedetectorcollection;
                        }
                    }
                }
            }
        }

        BlockPosition blockposition1 = WorldGenEndTrophy.getLocation(this.origin);
        int k = this.level.getHeightmapPos(HeightMap.Type.MOTION_BLOCKING, blockposition1).getY();

        for (int l = k; l >= this.level.getMinY(); --l) {
            ShapeDetector.ShapeDetectorCollection shapedetector_shapedetectorcollection1 = this.exitPortalPattern.find(this.level, new BlockPosition(blockposition1.getX(), l, blockposition1.getZ()));

            if (shapedetector_shapedetectorcollection1 != null) {
                if (this.portalLocation == null) {
                    this.portalLocation = shapedetector_shapedetectorcollection1.getBlock(3, 3, 3).getPos();
                }

                return shapedetector_shapedetectorcollection1;
            }
        }

        return null;
    }

    private boolean isArenaLoaded() {
        if (this.skipArenaLoadedCheck) {
            return true;
        } else {
            ChunkCoordIntPair chunkcoordintpair = new ChunkCoordIntPair(this.origin);

            for (int i = -8 + chunkcoordintpair.x; i <= 8 + chunkcoordintpair.x; ++i) {
                for (int j = 8 + chunkcoordintpair.z; j <= 8 + chunkcoordintpair.z; ++j) {
                    IChunkAccess ichunkaccess = this.level.getChunk(i, j, ChunkStatus.FULL, false);

                    if (!(ichunkaccess instanceof Chunk)) {
                        return false;
                    }

                    FullChunkStatus fullchunkstatus = ((Chunk) ichunkaccess).getFullStatus();

                    if (!fullchunkstatus.isOrAfter(FullChunkStatus.BLOCK_TICKING)) {
                        return false;
                    }
                }
            }

            return true;
        }
    }

    private void updatePlayers() {
        Set<EntityPlayer> set = Sets.newHashSet();

        for (EntityPlayer entityplayer : this.level.getPlayers(this.validPlayer)) {
            this.dragonEvent.addPlayer(entityplayer);
            set.add(entityplayer);
        }

        Set<EntityPlayer> set1 = Sets.newHashSet(this.dragonEvent.getPlayers());

        set1.removeAll(set);

        for (EntityPlayer entityplayer1 : set1) {
            this.dragonEvent.removePlayer(entityplayer1);
        }

    }

    private void updateCrystalCount() {
        this.ticksSinceCrystalsScanned = 0;
        this.crystalsAlive = 0;

        for (WorldGenEnder.Spike worldgenender_spike : WorldGenEnder.getSpikesForLevel(this.level)) {
            this.crystalsAlive += this.level.getEntitiesOfClass(EntityEnderCrystal.class, worldgenender_spike.getTopBoundingBox()).size();
        }

        EnderDragonBattle.LOGGER.debug("Found {} end crystals still alive", this.crystalsAlive);
    }

    public void setDragonKilled(EntityEnderDragon entityenderdragon) {
        if (entityenderdragon.getUUID().equals(this.dragonUUID)) {
            this.dragonEvent.setProgress(0.0F);
            this.dragonEvent.setVisible(false);
            this.spawnExitPortal(true);
            this.spawnNewGateway();
            if (!this.previouslyKilled) {
                this.level.setBlockAndUpdate(this.level.getHeightmapPos(HeightMap.Type.MOTION_BLOCKING, WorldGenEndTrophy.getLocation(this.origin)), Blocks.DRAGON_EGG.defaultBlockState());
            }

            this.previouslyKilled = true;
            this.dragonKilled = true;
        }

    }

    /** @deprecated */
    @Deprecated
    @VisibleForTesting
    public void removeAllGateways() {
        this.gateways.clear();
    }

    private void spawnNewGateway() {
        if (!this.gateways.isEmpty()) {
            int i = (Integer) this.gateways.remove(this.gateways.size() - 1);
            int j = MathHelper.floor(96.0D * Math.cos(2.0D * (-Math.PI + 0.15707963267948966D * (double) i)));
            int k = MathHelper.floor(96.0D * Math.sin(2.0D * (-Math.PI + 0.15707963267948966D * (double) i)));

            this.spawnNewGateway(new BlockPosition(j, 75, k));
        }
    }

    private void spawnNewGateway(BlockPosition blockposition) {
        this.level.levelEvent(3000, blockposition, 0);
        this.level.registryAccess().lookup(Registries.CONFIGURED_FEATURE).flatMap((iregistry) -> {
            return iregistry.get(EndFeatures.END_GATEWAY_DELAYED);
        }).ifPresent((holder_c) -> {
            ((WorldGenFeatureConfigured) holder_c.value()).place(this.level, this.level.getChunkSource().getGenerator(), RandomSource.create(), blockposition);
        });
    }

    public void spawnExitPortal(boolean flag) {
        WorldGenEndTrophy worldgenendtrophy = new WorldGenEndTrophy(flag);

        if (this.portalLocation == null) {
            for (this.portalLocation = this.level.getHeightmapPos(HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, WorldGenEndTrophy.getLocation(this.origin)).below(); this.level.getBlockState(this.portalLocation).is(Blocks.BEDROCK) && this.portalLocation.getY() > 63; this.portalLocation = this.portalLocation.below()) {
                ;
            }

            this.portalLocation = this.portalLocation.atY(Math.max(this.level.getMinY() + 1, this.portalLocation.getY()));
        }

        if (worldgenendtrophy.place(WorldGenFeatureConfiguration.NONE, this.level, this.level.getChunkSource().getGenerator(), RandomSource.create(), this.portalLocation)) {
            int i = MathHelper.positiveCeilDiv(4, 16);

            this.level.getChunkSource().chunkMap.waitForLightBeforeSending(new ChunkCoordIntPair(this.portalLocation), i);
        }

    }

    @Nullable
    private EntityEnderDragon createNewDragon() {
        this.level.getChunkAt(new BlockPosition(this.origin.getX(), 128 + this.origin.getY(), this.origin.getZ()));
        EntityEnderDragon entityenderdragon = EntityTypes.ENDER_DRAGON.create(this.level, EntitySpawnReason.EVENT);

        if (entityenderdragon != null) {
            entityenderdragon.setDragonFight(this);
            entityenderdragon.setFightOrigin(this.origin);
            entityenderdragon.getPhaseManager().setPhase(DragonControllerPhase.HOLDING_PATTERN);
            entityenderdragon.snapTo((double) this.origin.getX(), (double) (128 + this.origin.getY()), (double) this.origin.getZ(), this.level.random.nextFloat() * 360.0F, 0.0F);
            this.level.addFreshEntity(entityenderdragon);
            this.dragonUUID = entityenderdragon.getUUID();
        }

        return entityenderdragon;
    }

    public void updateDragon(EntityEnderDragon entityenderdragon) {
        if (entityenderdragon.getUUID().equals(this.dragonUUID)) {
            this.dragonEvent.setProgress(entityenderdragon.getHealth() / entityenderdragon.getMaxHealth());
            this.ticksSinceDragonSeen = 0;
            if (entityenderdragon.hasCustomName()) {
                this.dragonEvent.setName(entityenderdragon.getDisplayName());
            }
        }

    }

    public int getCrystalsAlive() {
        return this.crystalsAlive;
    }

    public void onCrystalDestroyed(EntityEnderCrystal entityendercrystal, DamageSource damagesource) {
        if (this.respawnStage != null && this.respawnCrystals.contains(entityendercrystal)) {
            EnderDragonBattle.LOGGER.debug("Aborting respawn sequence");
            this.respawnStage = null;
            this.respawnTime = 0;
            this.resetSpikeCrystals();
            this.spawnExitPortal(true);
        } else {
            this.updateCrystalCount();
            Entity entity = this.level.getEntity(this.dragonUUID);

            if (entity instanceof EntityEnderDragon) {
                EntityEnderDragon entityenderdragon = (EntityEnderDragon) entity;

                entityenderdragon.onCrystalDestroyed(this.level, entityendercrystal, entityendercrystal.blockPosition(), damagesource);
            }
        }

    }

    public boolean hasPreviouslyKilledDragon() {
        return this.previouslyKilled;
    }

    public boolean tryRespawn() { // CraftBukkit - return boolean
        if (this.dragonKilled && this.respawnStage == null) {
            BlockPosition blockposition = this.portalLocation;

            if (blockposition == null) {
                EnderDragonBattle.LOGGER.debug("Tried to respawn, but need to find the portal first.");
                ShapeDetector.ShapeDetectorCollection shapedetector_shapedetectorcollection = this.findExitPortal();

                if (shapedetector_shapedetectorcollection == null) {
                    EnderDragonBattle.LOGGER.debug("Couldn't find a portal, so we made one.");
                    this.spawnExitPortal(true);
                } else {
                    EnderDragonBattle.LOGGER.debug("Found the exit portal & saved its location for next time.");
                }

                blockposition = this.portalLocation;
            }

            List<EntityEnderCrystal> list = Lists.newArrayList();
            BlockPosition blockposition1 = blockposition.above(1);

            for (EnumDirection enumdirection : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
                List<EntityEnderCrystal> list1 = this.level.<EntityEnderCrystal>getEntitiesOfClass(EntityEnderCrystal.class, new AxisAlignedBB(blockposition1.relative(enumdirection, 2)));

                if (list1.isEmpty()) {
                    return false; // CraftBukkit - return value
                }

                list.addAll(list1);
            }

            EnderDragonBattle.LOGGER.debug("Found all crystals, respawning dragon.");
            return this.respawnDragon(list); // CraftBukkit - return value
        }
        return false; // CraftBukkit - return value
    }

    public boolean respawnDragon(List<EntityEnderCrystal> list) { // CraftBukkit - return boolean
        if (this.dragonKilled && this.respawnStage == null) {
            for (ShapeDetector.ShapeDetectorCollection shapedetector_shapedetectorcollection = this.findExitPortal(); shapedetector_shapedetectorcollection != null; shapedetector_shapedetectorcollection = this.findExitPortal()) {
                for (int i = 0; i < this.exitPortalPattern.getWidth(); ++i) {
                    for (int j = 0; j < this.exitPortalPattern.getHeight(); ++j) {
                        for (int k = 0; k < this.exitPortalPattern.getDepth(); ++k) {
                            ShapeDetectorBlock shapedetectorblock = shapedetector_shapedetectorcollection.getBlock(i, j, k);

                            if (shapedetectorblock.getState().is(Blocks.BEDROCK) || shapedetectorblock.getState().is(Blocks.END_PORTAL)) {
                                this.level.setBlockAndUpdate(shapedetectorblock.getPos(), Blocks.END_STONE.defaultBlockState());
                            }
                        }
                    }
                }
            }

            this.respawnStage = EnumDragonRespawn.START;
            this.respawnTime = 0;
            this.spawnExitPortal(false);
            this.respawnCrystals = list;
            return true; // CraftBukkit - return value
        }
        return false; // CraftBukkit - return value
    }

    public void resetSpikeCrystals() {
        for (WorldGenEnder.Spike worldgenender_spike : WorldGenEnder.getSpikesForLevel(this.level)) {
            for (EntityEnderCrystal entityendercrystal : this.level.getEntitiesOfClass(EntityEnderCrystal.class, worldgenender_spike.getTopBoundingBox())) {
                entityendercrystal.setInvulnerable(false);
                entityendercrystal.setBeamTarget((BlockPosition) null);
            }
        }

    }

    @Nullable
    public UUID getDragonUUID() {
        return this.dragonUUID;
    }

    public static record a(boolean needsStateScanning, boolean dragonKilled, boolean previouslyKilled, boolean isRespawning, Optional<UUID> dragonUUID, Optional<BlockPosition> exitPortalLocation, Optional<List<Integer>> gateways) {

        public static final Codec<EnderDragonBattle.a> CODEC = RecordCodecBuilder.create((instance) -> {
            return instance.group(Codec.BOOL.fieldOf("NeedsStateScanning").orElse(true).forGetter(EnderDragonBattle.a::needsStateScanning), Codec.BOOL.fieldOf("DragonKilled").orElse(false).forGetter(EnderDragonBattle.a::dragonKilled), Codec.BOOL.fieldOf("PreviouslyKilled").orElse(false).forGetter(EnderDragonBattle.a::previouslyKilled), Codec.BOOL.lenientOptionalFieldOf("IsRespawning", false).forGetter(EnderDragonBattle.a::isRespawning), UUIDUtil.CODEC.lenientOptionalFieldOf("Dragon").forGetter(EnderDragonBattle.a::dragonUUID), BlockPosition.CODEC.lenientOptionalFieldOf("ExitPortalLocation").forGetter(EnderDragonBattle.a::exitPortalLocation), Codec.list(Codec.INT).lenientOptionalFieldOf("Gateways").forGetter(EnderDragonBattle.a::gateways)).apply(instance, EnderDragonBattle.a::new);
        });
        public static final EnderDragonBattle.a DEFAULT = new EnderDragonBattle.a(true, false, false, false, Optional.empty(), Optional.empty(), Optional.empty());
    }
}
