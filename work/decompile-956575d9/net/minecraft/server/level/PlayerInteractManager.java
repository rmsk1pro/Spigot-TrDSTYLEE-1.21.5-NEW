package net.minecraft.server.level;

import com.mojang.logging.LogUtils;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.PacketPlayInBlockDig;
import net.minecraft.network.protocol.game.PacketPlayOutBlockChange;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.ITileInventory;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.ItemActionContext;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.level.EnumGamemode;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.Vec3D;
import org.slf4j.Logger;

public class PlayerInteractManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    protected WorldServer level;
    protected final EntityPlayer player;
    private EnumGamemode gameModeForPlayer;
    @Nullable
    private EnumGamemode previousGameModeForPlayer;
    private boolean isDestroyingBlock;
    private int destroyProgressStart;
    private BlockPosition destroyPos;
    private int gameTicks;
    private boolean hasDelayedDestroy;
    private BlockPosition delayedDestroyPos;
    private int delayedTickStart;
    private int lastSentState;

    public PlayerInteractManager(EntityPlayer entityplayer) {
        this.gameModeForPlayer = EnumGamemode.DEFAULT_MODE;
        this.destroyPos = BlockPosition.ZERO;
        this.delayedDestroyPos = BlockPosition.ZERO;
        this.lastSentState = -1;
        this.player = entityplayer;
        this.level = entityplayer.serverLevel();
    }

    public boolean changeGameModeForPlayer(EnumGamemode enumgamemode) {
        if (enumgamemode == this.gameModeForPlayer) {
            return false;
        } else {
            this.setGameModeForPlayer(enumgamemode, this.previousGameModeForPlayer);
            this.player.onUpdateAbilities();
            this.player.server.getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.a.UPDATE_GAME_MODE, this.player));
            this.level.updateSleepingPlayerList();
            if (enumgamemode == EnumGamemode.CREATIVE) {
                this.player.resetCurrentImpulseContext();
            }

            return true;
        }
    }

    protected void setGameModeForPlayer(EnumGamemode enumgamemode, @Nullable EnumGamemode enumgamemode1) {
        this.previousGameModeForPlayer = enumgamemode1;
        this.gameModeForPlayer = enumgamemode;
        enumgamemode.updatePlayerAbilities(this.player.getAbilities());
    }

    public EnumGamemode getGameModeForPlayer() {
        return this.gameModeForPlayer;
    }

    @Nullable
    public EnumGamemode getPreviousGameModeForPlayer() {
        return this.previousGameModeForPlayer;
    }

    public boolean isSurvival() {
        return this.gameModeForPlayer.isSurvival();
    }

    public boolean isCreative() {
        return this.gameModeForPlayer.isCreative();
    }

    public void tick() {
        ++this.gameTicks;
        if (this.hasDelayedDestroy) {
            IBlockData iblockdata = this.level.getBlockState(this.delayedDestroyPos);

            if (iblockdata.isAir()) {
                this.hasDelayedDestroy = false;
            } else {
                float f = this.incrementDestroyProgress(iblockdata, this.delayedDestroyPos, this.delayedTickStart);

                if (f >= 1.0F) {
                    this.hasDelayedDestroy = false;
                    this.destroyBlock(this.delayedDestroyPos);
                }
            }
        } else if (this.isDestroyingBlock) {
            IBlockData iblockdata1 = this.level.getBlockState(this.destroyPos);

            if (iblockdata1.isAir()) {
                this.level.destroyBlockProgress(this.player.getId(), this.destroyPos, -1);
                this.lastSentState = -1;
                this.isDestroyingBlock = false;
            } else {
                this.incrementDestroyProgress(iblockdata1, this.destroyPos, this.destroyProgressStart);
            }
        }

    }

    private float incrementDestroyProgress(IBlockData iblockdata, BlockPosition blockposition, int i) {
        int j = this.gameTicks - i;
        float f = iblockdata.getDestroyProgress(this.player, this.player.level(), blockposition) * (float) (j + 1);
        int k = (int) (f * 10.0F);

        if (k != this.lastSentState) {
            this.level.destroyBlockProgress(this.player.getId(), blockposition, k);
            this.lastSentState = k;
        }

        return f;
    }

    private void debugLogging(BlockPosition blockposition, boolean flag, int i, String s) {}

    public void handleBlockBreakAction(BlockPosition blockposition, PacketPlayInBlockDig.EnumPlayerDigType packetplayinblockdig_enumplayerdigtype, EnumDirection enumdirection, int i, int j) {
        if (!this.player.canInteractWithBlock(blockposition, 1.0D)) {
            this.debugLogging(blockposition, false, j, "too far");
        } else if (blockposition.getY() > i) {
            this.player.connection.send(new PacketPlayOutBlockChange(blockposition, this.level.getBlockState(blockposition)));
            this.debugLogging(blockposition, false, j, "too high");
        } else {
            if (packetplayinblockdig_enumplayerdigtype == PacketPlayInBlockDig.EnumPlayerDigType.START_DESTROY_BLOCK) {
                if (!this.level.mayInteract(this.player, blockposition)) {
                    this.player.connection.send(new PacketPlayOutBlockChange(blockposition, this.level.getBlockState(blockposition)));
                    this.debugLogging(blockposition, false, j, "may not interact");
                    return;
                }

                if (this.player.getAbilities().instabuild) {
                    this.destroyAndAck(blockposition, j, "creative destroy");
                    return;
                }

                if (this.player.blockActionRestricted(this.level, blockposition, this.gameModeForPlayer)) {
                    this.player.connection.send(new PacketPlayOutBlockChange(blockposition, this.level.getBlockState(blockposition)));
                    this.debugLogging(blockposition, false, j, "block action restricted");
                    return;
                }

                this.destroyProgressStart = this.gameTicks;
                float f = 1.0F;
                IBlockData iblockdata = this.level.getBlockState(blockposition);

                if (!iblockdata.isAir()) {
                    EnchantmentManager.onHitBlock(this.level, this.player.getMainHandItem(), this.player, this.player, EnumItemSlot.MAINHAND, Vec3D.atCenterOf(blockposition), iblockdata, (item) -> {
                        this.player.onEquippedItemBroken(item, EnumItemSlot.MAINHAND);
                    });
                    iblockdata.attack(this.level, blockposition, this.player);
                    f = iblockdata.getDestroyProgress(this.player, this.player.level(), blockposition);
                }

                if (!iblockdata.isAir() && f >= 1.0F) {
                    this.destroyAndAck(blockposition, j, "insta mine");
                } else {
                    if (this.isDestroyingBlock) {
                        this.player.connection.send(new PacketPlayOutBlockChange(this.destroyPos, this.level.getBlockState(this.destroyPos)));
                        this.debugLogging(blockposition, false, j, "abort destroying since another started (client insta mine, server disagreed)");
                    }

                    this.isDestroyingBlock = true;
                    this.destroyPos = blockposition.immutable();
                    int k = (int) (f * 10.0F);

                    this.level.destroyBlockProgress(this.player.getId(), blockposition, k);
                    this.debugLogging(blockposition, true, j, "actual start of destroying");
                    this.lastSentState = k;
                }
            } else if (packetplayinblockdig_enumplayerdigtype == PacketPlayInBlockDig.EnumPlayerDigType.STOP_DESTROY_BLOCK) {
                if (blockposition.equals(this.destroyPos)) {
                    int l = this.gameTicks - this.destroyProgressStart;
                    IBlockData iblockdata1 = this.level.getBlockState(blockposition);

                    if (!iblockdata1.isAir()) {
                        float f1 = iblockdata1.getDestroyProgress(this.player, this.player.level(), blockposition) * (float) (l + 1);

                        if (f1 >= 0.7F) {
                            this.isDestroyingBlock = false;
                            this.level.destroyBlockProgress(this.player.getId(), blockposition, -1);
                            this.destroyAndAck(blockposition, j, "destroyed");
                            return;
                        }

                        if (!this.hasDelayedDestroy) {
                            this.isDestroyingBlock = false;
                            this.hasDelayedDestroy = true;
                            this.delayedDestroyPos = blockposition;
                            this.delayedTickStart = this.destroyProgressStart;
                        }
                    }
                }

                this.debugLogging(blockposition, true, j, "stopped destroying");
            } else if (packetplayinblockdig_enumplayerdigtype == PacketPlayInBlockDig.EnumPlayerDigType.ABORT_DESTROY_BLOCK) {
                this.isDestroyingBlock = false;
                if (!Objects.equals(this.destroyPos, blockposition)) {
                    PlayerInteractManager.LOGGER.warn("Mismatch in destroy block pos: {} {}", this.destroyPos, blockposition);
                    this.level.destroyBlockProgress(this.player.getId(), this.destroyPos, -1);
                    this.debugLogging(blockposition, true, j, "aborted mismatched destroying");
                }

                this.level.destroyBlockProgress(this.player.getId(), blockposition, -1);
                this.debugLogging(blockposition, true, j, "aborted destroying");
            }

        }
    }

    public void destroyAndAck(BlockPosition blockposition, int i, String s) {
        if (this.destroyBlock(blockposition)) {
            this.debugLogging(blockposition, true, i, s);
        } else {
            this.player.connection.send(new PacketPlayOutBlockChange(blockposition, this.level.getBlockState(blockposition)));
            this.debugLogging(blockposition, false, i, s);
        }

    }

    public boolean destroyBlock(BlockPosition blockposition) {
        IBlockData iblockdata = this.level.getBlockState(blockposition);

        if (!this.player.getMainHandItem().canDestroyBlock(iblockdata, this.level, blockposition, this.player)) {
            return false;
        } else {
            TileEntity tileentity = this.level.getBlockEntity(blockposition);
            Block block = iblockdata.getBlock();

            if (block instanceof GameMasterBlock && !this.player.canUseGameMasterBlocks()) {
                this.level.sendBlockUpdated(blockposition, iblockdata, iblockdata, 3);
                return false;
            } else if (this.player.blockActionRestricted(this.level, blockposition, this.gameModeForPlayer)) {
                return false;
            } else {
                IBlockData iblockdata1 = block.playerWillDestroy(this.level, blockposition, iblockdata, this.player);
                boolean flag = this.level.removeBlock(blockposition, false);

                if (flag) {
                    block.destroy(this.level, blockposition, iblockdata1);
                }

                if (this.player.preventsBlockDrops()) {
                    return true;
                } else {
                    ItemStack itemstack = this.player.getMainHandItem();
                    ItemStack itemstack1 = itemstack.copy();
                    boolean flag1 = this.player.hasCorrectToolForDrops(iblockdata1);

                    itemstack.mineBlock(this.level, iblockdata1, blockposition, this.player);
                    if (flag && flag1) {
                        block.playerDestroy(this.level, this.player, blockposition, iblockdata1, tileentity, itemstack1);
                    }

                    return true;
                }
            }
        }
    }

    public EnumInteractionResult useItem(EntityPlayer entityplayer, World world, ItemStack itemstack, EnumHand enumhand) {
        if (this.gameModeForPlayer == EnumGamemode.SPECTATOR) {
            return EnumInteractionResult.PASS;
        } else if (entityplayer.getCooldowns().isOnCooldown(itemstack)) {
            return EnumInteractionResult.PASS;
        } else {
            int i = itemstack.getCount();
            int j = itemstack.getDamageValue();
            EnumInteractionResult enuminteractionresult = itemstack.use(world, entityplayer, enumhand);
            ItemStack itemstack1;

            if (enuminteractionresult instanceof EnumInteractionResult.d) {
                EnumInteractionResult.d enuminteractionresult_d = (EnumInteractionResult.d) enuminteractionresult;

                itemstack1 = (ItemStack) Objects.requireNonNullElse(enuminteractionresult_d.heldItemTransformedTo(), entityplayer.getItemInHand(enumhand));
            } else {
                itemstack1 = entityplayer.getItemInHand(enumhand);
            }

            if (itemstack1 == itemstack && itemstack1.getCount() == i && itemstack1.getUseDuration(entityplayer) <= 0 && itemstack1.getDamageValue() == j) {
                return enuminteractionresult;
            } else if (enuminteractionresult instanceof EnumInteractionResult.a && itemstack1.getUseDuration(entityplayer) > 0 && !entityplayer.isUsingItem()) {
                return enuminteractionresult;
            } else {
                if (itemstack != itemstack1) {
                    entityplayer.setItemInHand(enumhand, itemstack1);
                }

                if (itemstack1.isEmpty()) {
                    entityplayer.setItemInHand(enumhand, ItemStack.EMPTY);
                }

                if (!entityplayer.isUsingItem()) {
                    entityplayer.inventoryMenu.sendAllDataToRemote();
                }

                return enuminteractionresult;
            }
        }
    }

    public EnumInteractionResult useItemOn(EntityPlayer entityplayer, World world, ItemStack itemstack, EnumHand enumhand, MovingObjectPositionBlock movingobjectpositionblock) {
        BlockPosition blockposition = movingobjectpositionblock.getBlockPos();
        IBlockData iblockdata = world.getBlockState(blockposition);

        if (!iblockdata.getBlock().isEnabled(world.enabledFeatures())) {
            return EnumInteractionResult.FAIL;
        } else if (this.gameModeForPlayer == EnumGamemode.SPECTATOR) {
            ITileInventory itileinventory = iblockdata.getMenuProvider(world, blockposition);

            if (itileinventory != null) {
                entityplayer.openMenu(itileinventory);
                return EnumInteractionResult.CONSUME;
            } else {
                return EnumInteractionResult.PASS;
            }
        } else {
            boolean flag = !entityplayer.getMainHandItem().isEmpty() || !entityplayer.getOffhandItem().isEmpty();
            boolean flag1 = entityplayer.isSecondaryUseActive() && flag;
            ItemStack itemstack1 = itemstack.copy();

            if (!flag1) {
                EnumInteractionResult enuminteractionresult = iblockdata.useItemOn(entityplayer.getItemInHand(enumhand), world, entityplayer, enumhand, movingobjectpositionblock);

                if (enuminteractionresult.consumesAction()) {
                    CriterionTriggers.ITEM_USED_ON_BLOCK.trigger(entityplayer, blockposition, itemstack1);
                    return enuminteractionresult;
                }

                if (enuminteractionresult instanceof EnumInteractionResult.f && enumhand == EnumHand.MAIN_HAND) {
                    EnumInteractionResult enuminteractionresult1 = iblockdata.useWithoutItem(world, entityplayer, movingobjectpositionblock);

                    if (enuminteractionresult1.consumesAction()) {
                        CriterionTriggers.DEFAULT_BLOCK_USE.trigger(entityplayer, blockposition);
                        return enuminteractionresult1;
                    }
                }
            }

            if (!itemstack.isEmpty() && !entityplayer.getCooldowns().isOnCooldown(itemstack)) {
                ItemActionContext itemactioncontext = new ItemActionContext(entityplayer, enumhand, movingobjectpositionblock);
                EnumInteractionResult enuminteractionresult2;

                if (entityplayer.hasInfiniteMaterials()) {
                    int i = itemstack.getCount();

                    enuminteractionresult2 = itemstack.useOn(itemactioncontext);
                    itemstack.setCount(i);
                } else {
                    enuminteractionresult2 = itemstack.useOn(itemactioncontext);
                }

                if (enuminteractionresult2.consumesAction()) {
                    CriterionTriggers.ITEM_USED_ON_BLOCK.trigger(entityplayer, blockposition, itemstack1);
                }

                return enuminteractionresult2;
            } else {
                return EnumInteractionResult.PASS;
            }
        }
    }

    public void setLevel(WorldServer worldserver) {
        this.level = worldserver;
    }
}
