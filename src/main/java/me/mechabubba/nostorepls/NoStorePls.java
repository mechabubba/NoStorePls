package me.mechabubba.nostorepls;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class NoStorePls extends JavaPlugin implements Listener {
    public FileConfiguration config;
    public Logger log;
    public List<?> items;
    public List<?> containers;

    private final InventoryAction[] transfers = { // Actions in which an item has moved.
            InventoryAction.HOTBAR_MOVE_AND_READD,
            InventoryAction.MOVE_TO_OTHER_INVENTORY,
            InventoryAction.PLACE_ALL,
            InventoryAction.PLACE_ONE,
            InventoryAction.PLACE_SOME,
            InventoryAction.SWAP_WITH_CURSOR
    };

    // Helper function that determines which actions result in a transfer of items.
    private boolean itemsTransferred(InventoryAction action) {
        for(InventoryAction a : transfers) {
            if(action == a) return true;
        }
        return false;
    }

    // Tests if item is a container that we would need to search through; basically just shulkers and bundles.
    // todo: there is **absolutely** a better way of doing this. im only doing this to solve an immediate problem involving bundles in echests on my server.
    private ItemStack[] isContainer(ItemStack item) {
        Material type = item.getType();
        switch(type) {
            case SHULKER_BOX:
            case BLACK_SHULKER_BOX:
            case BLUE_SHULKER_BOX:
            case BROWN_SHULKER_BOX:
            case CYAN_SHULKER_BOX:
            case GRAY_SHULKER_BOX:
            case GREEN_SHULKER_BOX:
            case LIGHT_BLUE_SHULKER_BOX:
            case LIGHT_GRAY_SHULKER_BOX:
            case LIME_SHULKER_BOX:
            case MAGENTA_SHULKER_BOX:
            case ORANGE_SHULKER_BOX:
            case PINK_SHULKER_BOX:
            case PURPLE_SHULKER_BOX:
            case RED_SHULKER_BOX:
            case WHITE_SHULKER_BOX:
            case YELLOW_SHULKER_BOX: {
                BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
                if (meta != null) {
                    ShulkerBox box = (ShulkerBox) meta.getBlockState();
                    return box.getInventory().getContents();
                }
                return null;
            }
            case BUNDLE: {
                BundleMeta meta = (BundleMeta) item.getItemMeta();
                return meta.getItems().toArray(new ItemStack[]{});
            }
            default:
                return null;
        }
    }

    private boolean testContainer(ItemStack[] itemStacks) {
        for(ItemStack item : itemStacks) {
            if(item != null && items.contains(item.getType().toString())) return true;
        }
        return false;
    }

    // Helper logging function.
    private void logStorageAttempt(ItemStack item, Inventory inv, HumanEntity ply, String container) {
        if(!config.getBoolean("logInvalidAttempts")) return;
        Location coords = inv.getLocation();
        String loc = "X: " + coords.getBlockX() + ", Z: " + coords.getBlockZ();
        log.info("# Invalid item moved into incompatible container!");
        log.info("Item:      " + item.getType().toString());
        log.info("Container: " + inv.getType().toString() + " (" + (container.isEmpty() ? "" : "item is inside " + container + ", ") + loc + ")");
        log.info("Player:    " + ply.getName());
    }
    private void logStorageAttempt(ItemStack item, Inventory inv, HumanEntity ply) { logStorageAttempt(item, inv, ply, ""); }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();

        getServer().getPluginManager().registerEvents(this, this);
        getCommand("nsp").setExecutor(new NSPCommand(this));

        loadBlocklists();

        log = getLogger();
        log.info("=== NoStorePls is enabled! ===");
    }

    public void loadBlocklists() {
        items = config.getList("items");
        containers = config.getList("containers");
    }

    @Override
    public void onDisable() {
        // ¯\_(ツ)_/¯
    }

    @EventHandler
    public void onItemMove(InventoryClickEvent event) {
        InventoryAction action = event.getAction();
        if(!itemsTransferred(action)) return;

        Inventory inv;
        if(action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            inv = event.getInventory();
        } else {
            inv = event.getClickedInventory();
        }

        if(inv != null && containers.contains(inv.getType().toString())) {
            // First we get the item that's actually transferred.
            // With different actions, the ItemStack that we want to track is referenced in different places; this gets that under control.
            ItemStack item;
            if(action == InventoryAction.HOTBAR_SWAP || action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                item = event.getCurrentItem();
            } else if(action == InventoryAction.HOTBAR_MOVE_AND_READD) {
                // We need to check item at a players hotbar key.
                int hb = event.getHotbarButton();
                item = event.getWhoClicked().getInventory().getItem(hb);
            } else {
                item = event.getCursor();
            }

            // Test if its a container, such as a shulker or a bundle. if so, checks its inventory for illicit contents.
            ItemStack[] container = isContainer(item);
            if(container != null) {
                if(testContainer(container)) {
                    event.setCancelled(true);
                    logStorageAttempt(item, inv, event.getWhoClicked(), item.getType().toString());
                }
            } else if(items.contains(item.getType().toString())) {
                event.setCancelled(true);
                logStorageAttempt(item, inv, event.getWhoClicked());
            }
        }
    }

    @EventHandler
    public void onItemDrag(InventoryDragEvent event) {
        Inventory inv = event.getInventory();

        if(containers.contains(inv.getType().toString())) {
            Iterator it = event.getNewItems().entrySet().iterator();

            while(it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                ItemStack item = (ItemStack) pair.getValue();

                ItemStack[] container = isContainer(item);
                if(container != null) {
                    if(testContainer(container)) {
                        event.setCancelled(true);
                        logStorageAttempt(item, inv, event.getWhoClicked(), item.getType().toString());
                        break;
                    }
                } else if(items.contains(item.getType().toString())) {
                    event.setCancelled(true);
                    logStorageAttempt(item, inv, event.getWhoClicked());
                    break;
                }
            }
        }
    }
}
