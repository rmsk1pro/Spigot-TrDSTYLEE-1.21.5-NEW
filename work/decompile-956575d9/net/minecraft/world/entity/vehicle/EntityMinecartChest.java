package net.minecraft.world.entity.vehicle;

import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.piglin.PiglinAI;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.ContainerChest;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockChest;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.gameevent.GameEvent;

public class EntityMinecartChest extends EntityMinecartContainer {

    public EntityMinecartChest(EntityTypes<? extends EntityMinecartChest> entitytypes, World world) {
        super(entitytypes, world);
    }

    @Override
    protected Item getDropItem() {
        return Items.CHEST_MINECART;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(Items.CHEST_MINECART);
    }

    @Override
    public int getContainerSize() {
        return 27;
    }

    @Override
    public IBlockData getDefaultDisplayBlockState() {
        return (IBlockData) Blocks.CHEST.defaultBlockState().setValue(BlockChest.FACING, EnumDirection.NORTH);
    }

    @Override
    public int getDefaultDisplayOffset() {
        return 8;
    }

    @Override
    public Container createMenu(int i, PlayerInventory playerinventory) {
        return ContainerChest.threeRows(i, playerinventory, this);
    }

    @Override
    public void stopOpen(EntityHuman entityhuman) {
        this.level().gameEvent(GameEvent.CONTAINER_CLOSE, this.position(), GameEvent.a.of((Entity) entityhuman));
    }

    @Override
    public EnumInteractionResult interact(EntityHuman entityhuman, EnumHand enumhand) {
        EnumInteractionResult enuminteractionresult = this.interactWithContainerVehicle(entityhuman);

        if (enuminteractionresult.consumesAction()) {
            World world = entityhuman.level();

            if (world instanceof WorldServer) {
                WorldServer worldserver = (WorldServer) world;

                this.gameEvent(GameEvent.CONTAINER_OPEN, entityhuman);
                PiglinAI.angerNearbyPiglins(worldserver, entityhuman, true);
            }
        }

        return enuminteractionresult;
    }
}
