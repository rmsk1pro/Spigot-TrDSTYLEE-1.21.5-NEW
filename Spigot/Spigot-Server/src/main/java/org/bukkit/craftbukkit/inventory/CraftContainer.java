package org.bukkit.craftbukkit.inventory;

import net.minecraft.world.IInventory;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.ContainerAnvil;
import net.minecraft.world.inventory.ContainerBeacon;
import net.minecraft.world.inventory.ContainerBlastFurnace;
import net.minecraft.world.inventory.ContainerBrewingStand;
import net.minecraft.world.inventory.ContainerCartography;
import net.minecraft.world.inventory.ContainerChest;
import net.minecraft.world.inventory.ContainerDispenser;
import net.minecraft.world.inventory.ContainerEnchantTable;
import net.minecraft.world.inventory.ContainerFurnaceFurnace;
import net.minecraft.world.inventory.ContainerGrindstone;
import net.minecraft.world.inventory.ContainerHopper;
import net.minecraft.world.inventory.ContainerLectern;
import net.minecraft.world.inventory.ContainerLoom;
import net.minecraft.world.inventory.ContainerMerchant;
import net.minecraft.world.inventory.ContainerProperties;
import net.minecraft.world.inventory.ContainerShulkerBox;
import net.minecraft.world.inventory.ContainerSmoker;
import net.minecraft.world.inventory.ContainerWorkbench;
import net.minecraft.world.inventory.Containers;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.MenuType;

public class CraftContainer extends Container {

    private final InventoryView view;
    private InventoryType cachedType;
    private Container delegate;

    public CraftContainer(InventoryView view, EntityHuman player, int id) {
        super(getNotchInventoryType(view.getTopInventory()), id);
        this.view = view;
        // TODO: Do we need to check that it really is a CraftInventory?
        IInventory top = ((CraftInventory) view.getTopInventory()).getInventory();
        PlayerInventory bottom = (PlayerInventory) ((CraftInventory) view.getBottomInventory()).getInventory();
        cachedType = view.getType();
        setupSlots(top, bottom, player);
    }

    public CraftContainer(final Inventory inventory, final EntityHuman player, int id) {
        this(new CraftAbstractInventoryView() {

            private final String originalTitle = (inventory instanceof CraftInventoryCustom) ? ((CraftInventoryCustom.MinecraftInventory) ((CraftInventory) inventory).getInventory()).getTitle() : inventory.getType().getDefaultTitle();
            private String title = originalTitle;

            @Override
            public Inventory getTopInventory() {
                return inventory;
            }

            @Override
            public Inventory getBottomInventory() {
                return getPlayer().getInventory();
            }

            @Override
            public HumanEntity getPlayer() {
                return player.getBukkitEntity();
            }

            @Override
            public InventoryType getType() {
                return inventory.getType();
            }

            @Override
            public String getTitle() {
                return title;
            }

            @Override
            public String getOriginalTitle() {
                return originalTitle;
            }

            @Override
            public void setTitle(String title) {
                CraftInventoryView.sendInventoryTitleChange(this, title);
                this.title = title;
            }

        }, player, id);
    }

    @Override
    public InventoryView getBukkitView() {
        return view;
    }

    public static Containers getNotchInventoryType(Inventory inventory) {
        final InventoryType type = inventory.getType();
        switch (type) {
            case PLAYER:
            case CHEST:
            case ENDER_CHEST:
            case BARREL:
                switch (inventory.getSize()) {
                    case 9:
                        return Containers.GENERIC_9x1;
                    case 18:
                        return Containers.GENERIC_9x2;
                    case 27:
                        return Containers.GENERIC_9x3;
                    case 36:
                    case 41: // PLAYER
                        return Containers.GENERIC_9x4;
                    case 45:
                        return Containers.GENERIC_9x5;
                    case 54:
                        return Containers.GENERIC_9x6;
                    default:
                        throw new IllegalArgumentException("Unsupported custom inventory size " + inventory.getSize());
                }
            default:
                final MenuType menu = type.getMenuType();
                if (menu == null) {
                    return Containers.GENERIC_9x3;
                } else {
                    return ((CraftMenuType<?, ?>) menu).getHandle();
                }
        }
    }

    private void setupSlots(IInventory top, PlayerInventory bottom, EntityHuman entityhuman) {
        int windowId = -1;
        switch (cachedType) {
            case CREATIVE:
                break; // TODO: This should be an error?
            case PLAYER:
            case CHEST:
            case ENDER_CHEST:
            case BARREL:
                delegate = new ContainerChest(Containers.GENERIC_9x3, windowId, bottom, top, top.getContainerSize() / 9);
                break;
            case DISPENSER:
            case DROPPER:
                delegate = new ContainerDispenser(windowId, bottom, top);
                break;
            case FURNACE:
                delegate = new ContainerFurnaceFurnace(windowId, bottom, top, new ContainerProperties(4));
                break;
            case CRAFTING: // TODO: This should be an error?
            case WORKBENCH:
                setupWorkbench(top, bottom); // SPIGOT-3812 - manually set up slots so we can use the delegated inventory and not the automatically created one
                break;
            case ENCHANTING:
                delegate = new ContainerEnchantTable(windowId, bottom);
                break;
            case BREWING:
                delegate = new ContainerBrewingStand(windowId, bottom, top, new ContainerProperties(2));
                break;
            case HOPPER:
                delegate = new ContainerHopper(windowId, bottom, top);
                break;
            case ANVIL:
                setupAnvil(top, bottom); // SPIGOT-6783 - manually set up slots so we can use the delegated inventory and not the automatically created one
                break;
            case BEACON:
                delegate = new ContainerBeacon(windowId, bottom);
                break;
            case SHULKER_BOX:
                delegate = new ContainerShulkerBox(windowId, bottom, top);
                break;
            case BLAST_FURNACE:
                delegate = new ContainerBlastFurnace(windowId, bottom, top, new ContainerProperties(4));
                break;
            case LECTERN:
                delegate = new ContainerLectern(windowId, top, new ContainerProperties(1), bottom);
                break;
            case SMOKER:
                delegate = new ContainerSmoker(windowId, bottom, top, new ContainerProperties(4));
                break;
            case LOOM:
                delegate = new ContainerLoom(windowId, bottom);
                break;
            case CARTOGRAPHY:
                delegate = new ContainerCartography(windowId, bottom);
                break;
            case GRINDSTONE:
                delegate = new ContainerGrindstone(windowId, bottom);
                break;
            case STONECUTTER:
                setupStoneCutter(top, bottom); // SPIGOT-7757 - manual setup required for individual slots
                break;
            case MERCHANT:
                delegate = new ContainerMerchant(windowId, bottom);
                break;
            case SMITHING:
            case SMITHING_NEW:
                setupSmithing(top, bottom); // SPIGOT-6783 - manually set up slots so we can use the delegated inventory and not the automatically created one
                break;
            case CRAFTER:
                delegate = new CrafterMenu(windowId, bottom);
                break;
        }

        if (delegate != null) {
            this.lastSlots = delegate.lastSlots;
            this.slots = delegate.slots;
            this.remoteSlots = delegate.remoteSlots;
        }

        // SPIGOT-4598 - we should still delegate the shift click handler
        switch (cachedType) {
            case WORKBENCH:
                delegate = new ContainerWorkbench(windowId, bottom);
                break;
            case ANVIL:
                delegate = new ContainerAnvil(windowId, bottom);
                break;
        }
    }

    private void setupWorkbench(IInventory top, IInventory bottom) {
        // This code copied from ContainerWorkbench
        this.addSlot(new Slot(top, 0, 124, 35));

        int row;
        int col;

        for (row = 0; row < 3; ++row) {
            for (col = 0; col < 3; ++col) {
                this.addSlot(new Slot(top, 1 + col + row * 3, 30 + col * 18, 17 + row * 18));
            }
        }

        for (row = 0; row < 3; ++row) {
            for (col = 0; col < 9; ++col) {
                this.addSlot(new Slot(bottom, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        for (col = 0; col < 9; ++col) {
            this.addSlot(new Slot(bottom, col, 8 + col * 18, 142));
        }
        // End copy from ContainerWorkbench
    }

    private void setupAnvil(IInventory top, IInventory bottom) {
        // This code copied from ContainerAnvilAbstract
        this.addSlot(new Slot(top, 0, 27, 47));
        this.addSlot(new Slot(top, 1, 76, 47));
        this.addSlot(new Slot(top, 2, 134, 47));

        int row;
        int col;

        for (row = 0; row < 3; ++row) {
            for (col = 0; col < 9; ++col) {
                this.addSlot(new Slot(bottom, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        for (row = 0; row < 9; ++row) {
            this.addSlot(new Slot(bottom, row, 8 + row * 18, 142));
        }
        // End copy from ContainerAnvilAbstract
    }

    private void setupSmithing(IInventory top, IInventory bottom) {
        // This code copied from ContainerSmithing
        this.addSlot(new Slot(top, 0, 8, 48));
        this.addSlot(new Slot(top, 1, 26, 48));
        this.addSlot(new Slot(top, 2, 44, 48));
        this.addSlot(new Slot(top, 3, 98, 48));

        int row;
        int col;

        for (row = 0; row < 3; ++row) {
            for (col = 0; col < 9; ++col) {
                this.addSlot(new Slot(bottom, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        for (row = 0; row < 9; ++row) {
            this.addSlot(new Slot(bottom, row, 8 + row * 18, 142));
        }
        // End copy from ContainerSmithing
    }

    private void setupStoneCutter(IInventory top, IInventory bottom) {
        // This code copied from ContainerStonecutter
        this.addSlot(new Slot(top, 0, 20, 33));
        this.addSlot(new Slot(top, 1, 143, 33));

        int row;
        int col;

        for (row = 0; row < 3; ++row) {
            for (col = 0; col < 9; ++col) {
                this.addSlot(new Slot(bottom, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        for (row = 0; row < 9; ++row) {
            this.addSlot(new Slot(bottom, row, 8 + row * 18, 142));
        }
        // End copy from ContainerSmithing
    }

    @Override
    public ItemStack quickMoveStack(EntityHuman entityhuman, int i) {
        return (delegate != null) ? delegate.quickMoveStack(entityhuman, i) : ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(EntityHuman entity) {
        return true;
    }

    @Override
    public Containers<?> getType() {
        return getNotchInventoryType(view.getTopInventory());
    }
}
