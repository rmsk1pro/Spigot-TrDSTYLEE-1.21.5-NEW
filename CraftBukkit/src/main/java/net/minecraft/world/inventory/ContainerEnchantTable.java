package net.minecraft.world.inventory;

import java.util.List;
import java.util.Optional;
import net.minecraft.SystemUtils;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.IInventory;
import net.minecraft.world.InventorySubcontainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.item.enchantment.WeightedRandomEnchant;
import net.minecraft.world.level.block.BlockEnchantmentTable;
import net.minecraft.world.level.block.Blocks;

// CraftBukkit start
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.craftbukkit.enchantments.CraftEnchantment;
import org.bukkit.craftbukkit.inventory.CraftInventoryEnchanting;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.inventory.view.CraftEnchantmentView;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.entity.Player;
// CraftBukkit end

public class ContainerEnchantTable extends Container {

    static final MinecraftKey EMPTY_SLOT_LAPIS_LAZULI = MinecraftKey.withDefaultNamespace("container/slot/lapis_lazuli");
    private final IInventory enchantSlots;
    private final ContainerAccess access;
    private final RandomSource random;
    private final ContainerProperty enchantmentSeed;
    public final int[] costs;
    public final int[] enchantClue;
    public final int[] levelClue;
    // CraftBukkit start
    private CraftEnchantmentView bukkitEntity = null;
    private Player player;
    // CraftBukkit end

    public ContainerEnchantTable(int i, PlayerInventory playerinventory) {
        this(i, playerinventory, ContainerAccess.NULL);
    }

    public ContainerEnchantTable(int i, PlayerInventory playerinventory, ContainerAccess containeraccess) {
        super(Containers.ENCHANTMENT, i);
        this.enchantSlots = new InventorySubcontainer(2) {
            @Override
            public void setChanged() {
                super.setChanged();
                ContainerEnchantTable.this.slotsChanged(this);
            }

            // CraftBukkit start
            @Override
            public Location getLocation() {
                return containeraccess.getLocation();
            }
            // CraftBukkit end
        };
        this.random = RandomSource.create();
        this.enchantmentSeed = ContainerProperty.standalone();
        this.costs = new int[3];
        this.enchantClue = new int[]{-1, -1, -1};
        this.levelClue = new int[]{-1, -1, -1};
        this.access = containeraccess;
        this.addSlot(new Slot(this.enchantSlots, 0, 15, 47) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        this.addSlot(new Slot(this.enchantSlots, 1, 35, 47) {
            @Override
            public boolean mayPlace(ItemStack itemstack) {
                return itemstack.is(Items.LAPIS_LAZULI);
            }

            @Override
            public MinecraftKey getNoItemIcon() {
                return ContainerEnchantTable.EMPTY_SLOT_LAPIS_LAZULI;
            }
        });
        this.addStandardInventorySlots(playerinventory, 8, 84);
        this.addDataSlot(ContainerProperty.shared(this.costs, 0));
        this.addDataSlot(ContainerProperty.shared(this.costs, 1));
        this.addDataSlot(ContainerProperty.shared(this.costs, 2));
        this.addDataSlot(this.enchantmentSeed).set(playerinventory.player.getEnchantmentSeed());
        this.addDataSlot(ContainerProperty.shared(this.enchantClue, 0));
        this.addDataSlot(ContainerProperty.shared(this.enchantClue, 1));
        this.addDataSlot(ContainerProperty.shared(this.enchantClue, 2));
        this.addDataSlot(ContainerProperty.shared(this.levelClue, 0));
        this.addDataSlot(ContainerProperty.shared(this.levelClue, 1));
        this.addDataSlot(ContainerProperty.shared(this.levelClue, 2));
        // CraftBukkit start
        player = (Player) playerinventory.player.getBukkitEntity();
        // CraftBukkit end
    }

    @Override
    public void slotsChanged(IInventory iinventory) {
        if (iinventory == this.enchantSlots) {
            ItemStack itemstack = iinventory.getItem(0);

            if (!itemstack.isEmpty()) { // CraftBukkit - relax condition
                this.access.execute((world, blockposition) -> {
                    Registry<Holder<Enchantment>> registry = world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).asHolderIdMap();
                    int i = 0;

                    for (BlockPosition blockposition1 : BlockEnchantmentTable.BOOKSHELF_OFFSETS) {
                        if (BlockEnchantmentTable.isValidBookShelf(world, blockposition, blockposition1)) {
                            ++i;
                        }
                    }

                    this.random.setSeed((long) this.enchantmentSeed.get());

                    for (int j = 0; j < 3; ++j) {
                        this.costs[j] = EnchantmentManager.getEnchantmentCost(this.random, j, i, itemstack);
                        this.enchantClue[j] = -1;
                        this.levelClue[j] = -1;
                        if (this.costs[j] < j + 1) {
                            this.costs[j] = 0;
                        }
                    }

                    for (int k = 0; k < 3; ++k) {
                        if (this.costs[k] > 0) {
                            List<WeightedRandomEnchant> list = this.getEnchantmentList(world.registryAccess(), itemstack, k, this.costs[k]);

                            if (list != null && !list.isEmpty()) {
                                WeightedRandomEnchant weightedrandomenchant = (WeightedRandomEnchant) list.get(this.random.nextInt(list.size()));

                                this.enchantClue[k] = registry.getId(weightedrandomenchant.enchantment());
                                this.levelClue[k] = weightedrandomenchant.level();
                            }
                        }
                    }

                    // CraftBukkit start
                    CraftItemStack item = CraftItemStack.asCraftMirror(itemstack);
                    org.bukkit.enchantments.EnchantmentOffer[] offers = new EnchantmentOffer[3];
                    for (int j = 0; j < 3; ++j) {
                        org.bukkit.enchantments.Enchantment enchantment = (this.enchantClue[j] >= 0) ? CraftEnchantment.minecraftHolderToBukkit(registry.byId(this.enchantClue[j])) : null;
                        offers[j] = (enchantment != null) ? new EnchantmentOffer(enchantment, this.levelClue[j], this.costs[j]) : null;
                    }

                    PrepareItemEnchantEvent event = new PrepareItemEnchantEvent(player, this.getBukkitView(), access.getLocation().getBlock(), item, offers, i);
                    event.setCancelled(!itemstack.isEnchantable());
                    world.getCraftServer().getPluginManager().callEvent(event);

                    if (event.isCancelled()) {
                        for (int j = 0; j < 3; ++j) {
                            this.costs[j] = 0;
                            this.enchantClue[j] = -1;
                            this.levelClue[j] = -1;
                        }
                        return;
                    }

                    for (int j = 0; j < 3; j++) {
                        EnchantmentOffer offer = event.getOffers()[j];
                        if (offer != null) {
                            this.costs[j] = offer.getCost();
                            this.enchantClue[j] = registry.getId(CraftEnchantment.bukkitToMinecraftHolder(offer.getEnchantment()));
                            this.levelClue[j] = offer.getEnchantmentLevel();
                        } else {
                            this.costs[j] = 0;
                            this.enchantClue[j] = -1;
                            this.levelClue[j] = -1;
                        }
                    }
                    // CraftBukkit end

                    this.broadcastChanges();
                });
            } else {
                for (int i = 0; i < 3; ++i) {
                    this.costs[i] = 0;
                    this.enchantClue[i] = -1;
                    this.levelClue[i] = -1;
                }
            }
        }

    }

    @Override
    public boolean clickMenuButton(EntityHuman entityhuman, int i) {
        if (i >= 0 && i < this.costs.length) {
            ItemStack itemstack = this.enchantSlots.getItem(0);
            ItemStack itemstack1 = this.enchantSlots.getItem(1);
            int j = i + 1;

            if ((itemstack1.isEmpty() || itemstack1.getCount() < j) && !entityhuman.hasInfiniteMaterials()) {
                return false;
            } else if (this.costs[i] <= 0 || itemstack.isEmpty() || (entityhuman.experienceLevel < j || entityhuman.experienceLevel < this.costs[i]) && !entityhuman.hasInfiniteMaterials()) {
                return false;
            } else {
                this.access.execute((world, blockposition) -> {
                    ItemStack itemstack2 = itemstack;
                    List<WeightedRandomEnchant> list = this.getEnchantmentList(world.registryAccess(), itemstack, i, this.costs[i]);

                    // CraftBukkit start
                    Registry<Holder<Enchantment>> registry = world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).asHolderIdMap();
                    if (true || !list.isEmpty()) {
                        // entityhuman.onEnchantmentPerformed(itemstack, j); // Moved down
                        Map<org.bukkit.enchantments.Enchantment, Integer> enchants = new java.util.HashMap<org.bukkit.enchantments.Enchantment, Integer>();
                        for (WeightedRandomEnchant instance : list) {
                            enchants.put(CraftEnchantment.minecraftHolderToBukkit(instance.enchantment()), instance.level());
                        }
                        CraftItemStack item = CraftItemStack.asCraftMirror(itemstack2);

                        org.bukkit.enchantments.Enchantment hintedEnchantment = CraftEnchantment.minecraftHolderToBukkit(registry.byId(enchantClue[i]));
                        int hintedEnchantmentLevel = levelClue[i];
                        EnchantItemEvent event = new EnchantItemEvent((Player) entityhuman.getBukkitEntity(), this.getBukkitView(), access.getLocation().getBlock(), item, this.costs[i], enchants, hintedEnchantment, hintedEnchantmentLevel, i);
                        world.getCraftServer().getPluginManager().callEvent(event);

                        int level = event.getExpLevelCost();
                        if (event.isCancelled() || (level > entityhuman.experienceLevel && !entityhuman.getAbilities().instabuild) || event.getEnchantsToAdd().isEmpty()) {
                            return;
                        }
                        // CraftBukkit end
                        if (itemstack.is(Items.BOOK)) {
                            itemstack2 = itemstack.transmuteCopy(Items.ENCHANTED_BOOK);
                            this.enchantSlots.setItem(0, itemstack2);
                        }

                        // CraftBukkit start
                        for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : event.getEnchantsToAdd().entrySet()) {
                            Holder<Enchantment> nms = CraftEnchantment.bukkitToMinecraftHolder(entry.getKey());
                            if (nms == null) {
                                continue;
                            }

                            WeightedRandomEnchant weightedrandomenchant = new WeightedRandomEnchant(nms, entry.getValue());
                            itemstack2.enchant(weightedrandomenchant.enchantment(), weightedrandomenchant.level());
                        }

                        entityhuman.onEnchantmentPerformed(itemstack, j);
                        // CraftBukkit end

                        // CraftBukkit - TODO: let plugins change this
                        itemstack1.consume(j, entityhuman);
                        if (itemstack1.isEmpty()) {
                            this.enchantSlots.setItem(1, ItemStack.EMPTY);
                        }

                        entityhuman.awardStat(StatisticList.ENCHANT_ITEM);
                        if (entityhuman instanceof EntityPlayer) {
                            CriterionTriggers.ENCHANTED_ITEM.trigger((EntityPlayer) entityhuman, itemstack2, j);
                        }

                        this.enchantSlots.setChanged();
                        this.enchantmentSeed.set(entityhuman.getEnchantmentSeed());
                        this.slotsChanged(this.enchantSlots);
                        world.playSound((Entity) null, blockposition, SoundEffects.ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 1.0F, world.random.nextFloat() * 0.1F + 0.9F);
                    }

                });
                return true;
            }
        } else {
            String s = String.valueOf(entityhuman.getName());

            SystemUtils.logAndPauseIfInIde(s + " pressed invalid button id: " + i);
            return false;
        }
    }

    private List<WeightedRandomEnchant> getEnchantmentList(IRegistryCustom iregistrycustom, ItemStack itemstack, int i, int j) {
        this.random.setSeed((long) (this.enchantmentSeed.get() + i));
        Optional<HolderSet.Named<Enchantment>> optional = iregistrycustom.lookupOrThrow(Registries.ENCHANTMENT).get(EnchantmentTags.IN_ENCHANTING_TABLE);

        if (optional.isEmpty()) {
            return List.of();
        } else {
            List<WeightedRandomEnchant> list = EnchantmentManager.selectEnchantment(this.random, itemstack, j, ((HolderSet.Named) optional.get()).stream());

            if (itemstack.is(Items.BOOK) && list.size() > 1) {
                list.remove(this.random.nextInt(list.size()));
            }

            return list;
        }
    }

    public int getGoldCount() {
        ItemStack itemstack = this.enchantSlots.getItem(1);

        return itemstack.isEmpty() ? 0 : itemstack.getCount();
    }

    public int getEnchantmentSeed() {
        return this.enchantmentSeed.get();
    }

    @Override
    public void removed(EntityHuman entityhuman) {
        super.removed(entityhuman);
        this.access.execute((world, blockposition) -> {
            this.clearContainer(entityhuman, this.enchantSlots);
        });
    }

    @Override
    public boolean stillValid(EntityHuman entityhuman) {
        if (!this.checkReachable) return true; // CraftBukkit
        return stillValid(this.access, entityhuman, Blocks.ENCHANTING_TABLE);
    }

    @Override
    public ItemStack quickMoveStack(EntityHuman entityhuman, int i) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(i);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();

            itemstack = itemstack1.copy();
            if (i == 0) {
                if (!this.moveItemStackTo(itemstack1, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (i == 1) {
                if (!this.moveItemStackTo(itemstack1, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (itemstack1.is(Items.LAPIS_LAZULI)) {
                if (!this.moveItemStackTo(itemstack1, 1, 2, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (((Slot) this.slots.get(0)).hasItem() || !((Slot) this.slots.get(0)).mayPlace(itemstack1)) {
                    return ItemStack.EMPTY;
                }

                ItemStack itemstack2 = itemstack1.copyWithCount(1);

                itemstack1.shrink(1);
                ((Slot) this.slots.get(0)).setByPlayer(itemstack2);
            }

            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(entityhuman, itemstack1);
        }

        return itemstack;
    }

    // CraftBukkit start
    @Override
    public CraftEnchantmentView getBukkitView() {
        if (bukkitEntity != null) {
            return bukkitEntity;
        }

        CraftInventoryEnchanting inventory = new CraftInventoryEnchanting(this.enchantSlots);
        bukkitEntity = new CraftEnchantmentView(this.player, inventory, this);
        return bukkitEntity;
    }
    // CraftBukkit end
}
