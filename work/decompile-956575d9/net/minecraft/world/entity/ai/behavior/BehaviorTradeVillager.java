package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.InventorySubcontainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.EntityVillager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class BehaviorTradeVillager extends Behavior<EntityVillager> {

    private Set<Item> trades = ImmutableSet.of();

    public BehaviorTradeVillager() {
        super(ImmutableMap.of(MemoryModuleType.INTERACTION_TARGET, MemoryStatus.VALUE_PRESENT, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT));
    }

    protected boolean checkExtraStartConditions(WorldServer worldserver, EntityVillager entityvillager) {
        return BehaviorUtil.targetIsValid(entityvillager.getBrain(), MemoryModuleType.INTERACTION_TARGET, EntityTypes.VILLAGER);
    }

    protected boolean canStillUse(WorldServer worldserver, EntityVillager entityvillager, long i) {
        return this.checkExtraStartConditions(worldserver, entityvillager);
    }

    protected void start(WorldServer worldserver, EntityVillager entityvillager, long i) {
        EntityVillager entityvillager1 = (EntityVillager) entityvillager.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET).get();

        BehaviorUtil.lockGazeAndWalkToEachOther(entityvillager, entityvillager1, 0.5F, 2);
        this.trades = figureOutWhatIAmWillingToTrade(entityvillager, entityvillager1);
    }

    protected void tick(WorldServer worldserver, EntityVillager entityvillager, long i) {
        EntityVillager entityvillager1 = (EntityVillager) entityvillager.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET).get();

        if (entityvillager.distanceToSqr((Entity) entityvillager1) <= 5.0D) {
            BehaviorUtil.lockGazeAndWalkToEachOther(entityvillager, entityvillager1, 0.5F, 2);
            entityvillager.gossip(worldserver, entityvillager1, i);
            boolean flag = entityvillager.getVillagerData().profession().is(VillagerProfession.FARMER);

            if (entityvillager.hasExcessFood() && (flag || entityvillager1.wantsMoreFood())) {
                throwHalfStack(entityvillager, EntityVillager.FOOD_POINTS.keySet(), entityvillager1);
            }

            if (flag && entityvillager.getInventory().countItem(Items.WHEAT) > Items.WHEAT.getDefaultMaxStackSize() / 2) {
                throwHalfStack(entityvillager, ImmutableSet.of(Items.WHEAT), entityvillager1);
            }

            if (!this.trades.isEmpty() && entityvillager.getInventory().hasAnyOf(this.trades)) {
                throwHalfStack(entityvillager, this.trades, entityvillager1);
            }

        }
    }

    protected void stop(WorldServer worldserver, EntityVillager entityvillager, long i) {
        entityvillager.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);
    }

    private static Set<Item> figureOutWhatIAmWillingToTrade(EntityVillager entityvillager, EntityVillager entityvillager1) {
        ImmutableSet<Item> immutableset = ((VillagerProfession) entityvillager1.getVillagerData().profession().value()).requestedItems();
        ImmutableSet<Item> immutableset1 = ((VillagerProfession) entityvillager.getVillagerData().profession().value()).requestedItems();

        return (Set) immutableset.stream().filter((item) -> {
            return !immutableset1.contains(item);
        }).collect(Collectors.toSet());
    }

    private static void throwHalfStack(EntityVillager entityvillager, Set<Item> set, EntityLiving entityliving) {
        InventorySubcontainer inventorysubcontainer = entityvillager.getInventory();
        ItemStack itemstack = ItemStack.EMPTY;
        int i = 0;

        while (i < inventorysubcontainer.getContainerSize()) {
            ItemStack itemstack1;
            Item item;
            int j;
            label28:
            {
                itemstack1 = inventorysubcontainer.getItem(i);
                if (!itemstack1.isEmpty()) {
                    item = itemstack1.getItem();
                    if (set.contains(item)) {
                        if (itemstack1.getCount() > itemstack1.getMaxStackSize() / 2) {
                            j = itemstack1.getCount() / 2;
                            break label28;
                        }

                        if (itemstack1.getCount() > 24) {
                            j = itemstack1.getCount() - 24;
                            break label28;
                        }
                    }
                }

                ++i;
                continue;
            }

            itemstack1.shrink(j);
            itemstack = new ItemStack(item, j);
            break;
        }

        if (!itemstack.isEmpty()) {
            BehaviorUtil.throwItem(entityvillager, itemstack, entityliving.position());
        }

    }
}
