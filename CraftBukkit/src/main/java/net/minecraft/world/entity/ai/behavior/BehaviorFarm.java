package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsItem;
import net.minecraft.world.InventorySubcontainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.MemoryTarget;
import net.minecraft.world.entity.npc.EntityVillager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemBlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockCrops;
import net.minecraft.world.level.block.BlockSoil;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.gameevent.GameEvent;

// CraftBukkit start
import net.minecraft.world.level.block.Blocks;
import org.bukkit.craftbukkit.event.CraftEventFactory;
// CraftBukkit end

public class BehaviorFarm extends Behavior<EntityVillager> {

    private static final int HARVEST_DURATION = 200;
    public static final float SPEED_MODIFIER = 0.5F;
    @Nullable
    private BlockPosition aboveFarmlandPos;
    private long nextOkStartTime;
    private int timeWorkedSoFar;
    private final List<BlockPosition> validFarmlandAroundVillager = Lists.newArrayList();

    public BehaviorFarm() {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.SECONDARY_JOB_SITE, MemoryStatus.VALUE_PRESENT));
    }

    protected boolean checkExtraStartConditions(WorldServer worldserver, EntityVillager entityvillager) {
        if (!worldserver.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return false;
        } else if (!entityvillager.getVillagerData().profession().is(VillagerProfession.FARMER)) {
            return false;
        } else {
            BlockPosition.MutableBlockPosition blockposition_mutableblockposition = entityvillager.blockPosition().mutable();

            this.validFarmlandAroundVillager.clear();

            for (int i = -1; i <= 1; ++i) {
                for (int j = -1; j <= 1; ++j) {
                    for (int k = -1; k <= 1; ++k) {
                        blockposition_mutableblockposition.set(entityvillager.getX() + (double) i, entityvillager.getY() + (double) j, entityvillager.getZ() + (double) k);
                        if (this.validPos(blockposition_mutableblockposition, worldserver)) {
                            this.validFarmlandAroundVillager.add(new BlockPosition(blockposition_mutableblockposition));
                        }
                    }
                }
            }

            this.aboveFarmlandPos = this.getValidFarmland(worldserver);
            return this.aboveFarmlandPos != null;
        }
    }

    @Nullable
    private BlockPosition getValidFarmland(WorldServer worldserver) {
        return this.validFarmlandAroundVillager.isEmpty() ? null : (BlockPosition) this.validFarmlandAroundVillager.get(worldserver.getRandom().nextInt(this.validFarmlandAroundVillager.size()));
    }

    private boolean validPos(BlockPosition blockposition, WorldServer worldserver) {
        IBlockData iblockdata = worldserver.getBlockState(blockposition);
        Block block = iblockdata.getBlock();
        Block block1 = worldserver.getBlockState(blockposition.below()).getBlock();

        return block instanceof BlockCrops && ((BlockCrops) block).isMaxAge(iblockdata) || iblockdata.isAir() && block1 instanceof BlockSoil;
    }

    protected void start(WorldServer worldserver, EntityVillager entityvillager, long i) {
        if (i > this.nextOkStartTime && this.aboveFarmlandPos != null) {
            entityvillager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BehaviorTarget(this.aboveFarmlandPos));
            entityvillager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new MemoryTarget(new BehaviorTarget(this.aboveFarmlandPos), 0.5F, 1));
        }

    }

    protected void stop(WorldServer worldserver, EntityVillager entityvillager, long i) {
        entityvillager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        entityvillager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        this.timeWorkedSoFar = 0;
        this.nextOkStartTime = i + 40L;
    }

    protected void tick(WorldServer worldserver, EntityVillager entityvillager, long i) {
        if (this.aboveFarmlandPos == null || this.aboveFarmlandPos.closerToCenterThan(entityvillager.position(), 1.0D)) {
            if (this.aboveFarmlandPos != null && i > this.nextOkStartTime) {
                IBlockData iblockdata = worldserver.getBlockState(this.aboveFarmlandPos);
                Block block = iblockdata.getBlock();
                Block block1 = worldserver.getBlockState(this.aboveFarmlandPos.below()).getBlock();

                if (block instanceof BlockCrops && ((BlockCrops) block).isMaxAge(iblockdata)) {
                    if (CraftEventFactory.callEntityChangeBlockEvent(entityvillager, this.aboveFarmlandPos, Blocks.AIR.defaultBlockState())) { // CraftBukkit
                    worldserver.destroyBlock(this.aboveFarmlandPos, true, entityvillager);
                    } // CraftBukkit
                }

                if (iblockdata.isAir() && block1 instanceof BlockSoil && entityvillager.hasFarmSeeds()) {
                    InventorySubcontainer inventorysubcontainer = entityvillager.getInventory();

                    for (int j = 0; j < inventorysubcontainer.getContainerSize(); ++j) {
                        ItemStack itemstack = inventorysubcontainer.getItem(j);
                        boolean flag = false;

                        if (!itemstack.isEmpty() && itemstack.is(TagsItem.VILLAGER_PLANTABLE_SEEDS)) {
                            Item item = itemstack.getItem();

                            if (item instanceof ItemBlock) {
                                ItemBlock itemblock = (ItemBlock) item;
                                IBlockData iblockdata1 = itemblock.getBlock().defaultBlockState();

                                if (CraftEventFactory.callEntityChangeBlockEvent(entityvillager, this.aboveFarmlandPos, iblockdata1)) { // CraftBukkit
                                worldserver.setBlockAndUpdate(this.aboveFarmlandPos, iblockdata1);
                                worldserver.gameEvent(GameEvent.BLOCK_PLACE, this.aboveFarmlandPos, GameEvent.a.of(entityvillager, iblockdata1));
                                flag = true;
                                } // CraftBukkit
                            }
                        }

                        if (flag) {
                            worldserver.playSound((Entity) null, (double) this.aboveFarmlandPos.getX(), (double) this.aboveFarmlandPos.getY(), (double) this.aboveFarmlandPos.getZ(), SoundEffects.CROP_PLANTED, SoundCategory.BLOCKS, 1.0F, 1.0F);
                            itemstack.shrink(1);
                            if (itemstack.isEmpty()) {
                                inventorysubcontainer.setItem(j, ItemStack.EMPTY);
                            }
                            break;
                        }
                    }
                }

                if (block instanceof BlockCrops && !((BlockCrops) block).isMaxAge(iblockdata)) {
                    this.validFarmlandAroundVillager.remove(this.aboveFarmlandPos);
                    this.aboveFarmlandPos = this.getValidFarmland(worldserver);
                    if (this.aboveFarmlandPos != null) {
                        this.nextOkStartTime = i + 20L;
                        entityvillager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new MemoryTarget(new BehaviorTarget(this.aboveFarmlandPos), 0.5F, 1));
                        entityvillager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BehaviorTarget(this.aboveFarmlandPos));
                    }
                }
            }

            ++this.timeWorkedSoFar;
        }
    }

    protected boolean canStillUse(WorldServer worldserver, EntityVillager entityvillager, long i) {
        return this.timeWorkedSoFar < 200;
    }
}
