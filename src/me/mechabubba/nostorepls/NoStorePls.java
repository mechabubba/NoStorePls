package me.mechabubba.nostorepls;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

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
import org.bukkit.plugin.java.JavaPlugin;

public class NoStorePls extends JavaPlugin implements Listener {
    public FileConfiguration config;
    public Logger log;
    private final InventoryAction[] transfers = { // Actions in which an item has moved.
            InventoryAction.HOTBAR_MOVE_AND_READD,
            InventoryAction.MOVE_TO_OTHER_INVENTORY,
            InventoryAction.PLACE_ALL,
            InventoryAction.PLACE_ONE,
            InventoryAction.PLACE_SOME,
            InventoryAction.SWAP_WITH_CURSOR
    };

    // Helper function that determines which actions result in a transfer of items.
    public boolean itemsTransferred(InventoryAction action) {
        for(InventoryAction a : transfers) {
            if(action == a) return true;
        }
        return false;
    }

    // Tests if item is a ShulkerBox and returns it as such if so.
    // Courtesy of https://www.spigotmc.org/threads/getting-the-inventory-of-a-shulker-box-itemstack.212369/#post-2191614.
    public ShulkerBox testShulker(ItemStack item) {
        if(item.getItemMeta() instanceof BlockStateMeta) {
            BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
            if (meta.getBlockState() instanceof ShulkerBox) {
                return (ShulkerBox) meta.getBlockState();
            }
        }
        return null;
    }

    // Helper logging function.
    public void logStorageAttempt(ItemStack item, Inventory inv, HumanEntity ply, boolean isShulker) {
        if(!config.getBoolean("logInvalidAttempts")) return;
        log.info("# Invalid item moved into incompatible container!");
        log.info("Item:      " + item.getType().toString());
        log.info("Container: " + inv.getType().toString() + (isShulker ? " (item is inside a shulker box)" : ""));
        log.info("Player:    " + ply.getName());
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();

        getServer().getPluginManager().registerEvents(this, this);
        getCommand("nsp").setExecutor(new NSPCommand(this));

        log = getLogger();
        log.info("NoStorePls is enabled!");
    }

    @Override
    public void onDisable() {
        // ¯\_(ツ)_/¯
    }

    @EventHandler
    public void onItemMove(InventoryClickEvent event) {
        InventoryAction action = event.getAction();
        if(!itemsTransferred(action)) return;

        List<?> containers = config.getList("containers");

        Inventory inv;
        if(action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            inv = event.getInventory();
        } else {
            inv = event.getClickedInventory();
        }

        if(inv != null && containers.contains(inv.getType().toString())) {
            List<?> items = config.getList("items");

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

            // Test if its a shulker box. if so, checks its inventory for illicit contents.
            ShulkerBox box = testShulker(item);
            if(box instanceof ShulkerBox) {
                Inventory boxInv = box.getInventory();
                ItemStack[] itemStacks = boxInv.getContents();
                for(ItemStack _item : itemStacks) {
                    if(_item != null && items.contains(_item.getType().toString())) {
                        logStorageAttempt(_item, inv, event.getWhoClicked(), true);
                        event.setCancelled(true);
                        return;
                    }
                }
            }

            if(items.contains(item.getType().toString())) {
                logStorageAttempt(item, inv, event.getWhoClicked(), false);
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onItemDrag(InventoryDragEvent event) {
        List<?> containers = config.getList("containers");
        Inventory inv = event.getInventory();

        if(containers.contains(inv.getType().toString())) {
            List<?> items = config.getList("items");
            Iterator it = event.getNewItems().entrySet().iterator();

            while(it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                ItemStack item = (ItemStack) pair.getValue();

                if(items.contains(item.getType().toString())) {
                    logStorageAttempt(item, inv, event.getWhoClicked(), false);
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }
}
