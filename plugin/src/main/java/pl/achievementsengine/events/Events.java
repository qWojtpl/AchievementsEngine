package pl.achievementsengine.events;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;
import pl.achievementsengine.AchievementsEngine;
import pl.achievementsengine.achievements.Achievement;
import pl.achievementsengine.achievements.PlayerAchievementState;
import pl.achievementsengine.gui.GUIHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Events implements Listener {

    /*
    Why "priority = EventPriority.HIGHEST"?
    by Spigot documentation:
    "Event call is critical and must have the final say in what happens to the event"
    Source: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/event/EventPriority.html
     */

    private final HashMap<String, List<Achievement>> registeredEvents = new HashMap<>();

    public List<Achievement> getEventAchievements(String event) {
        event = event.toLowerCase();
        if(!registeredEvents.containsKey(event)) {
            registeredEvents.put(event, new ArrayList<>());
        }
        return registeredEvents.get(event);
    }

    public HashMap<String, List<Achievement>> getRegisteredEvents() {
        return this.registeredEvents;
    }

    public void registerEvent(String event, Achievement achievement) {
        event = event.toLowerCase();
        List<Achievement> list = getEventAchievements(event);
        list.add(achievement);
    }

    public void checkForAchievementEvents(Player player, String checkable) {
        String[] ev = checkable.split(" ");
        AchievementsEngine.getInstance().getLogger().info("Checking event: " + ev[0] + " " + ev[1]);
        for(Achievement a : getEventAchievements(ev[0] + " " + ev[1])) {
            AchievementsEngine.getInstance().getAchievementManager().Check(player, checkable, a);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PlayerAchievementState.Create(event.getPlayer());
        checkForAchievementEvents(event.getPlayer(), "join server");
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if(event.getEntity().getKiller() instanceof Player) {
            checkForAchievementEvents(event.getEntity().getKiller(), "kill " + event.getEntity().getType().name()
                    + " named " + event.getEntity().getCustomName());
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if(event.getClickedBlock() != null) {
            checkForAchievementEvents(event.getPlayer(), "interact " + event.getClickedBlock().getType().name());
            checkForAchievementEvents(event.getPlayer(), "click " + event.getClickedBlock().getType().name());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBreak(BlockBreakEvent event) {
        if(event.isCancelled()) return;
        checkForAchievementEvents(event.getPlayer(), "break " + event.getBlock().getType().name());
        checkForAchievementEvents(event.getPlayer(), "destroy " + event.getBlock().getType().name());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlace(BlockPlaceEvent event) {
        if(event.isCancelled()) return;
        checkForAchievementEvents(event.getPlayer(), "place " + event.getBlock().getType().name());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPickup(EntityPickupItemEvent event) {
        if(event.isCancelled()) return;
        if(event.getEntity() instanceof Player) {
            for(int i = 0; i < event.getItem().getItemStack().getAmount(); i++) {
                checkForAchievementEvents((Player) event.getEntity(), "pickup " + event.getItem().getItemStack().getType()
                        + " named " + event.getItem().getItemStack().getItemMeta().getDisplayName());
            }
            checkForAchievementEvents((Player) event.getEntity(), "T_pickup " + event.getItem().getItemStack().getType()
                    + " named " + event.getItem().getItemStack().getItemMeta().getDisplayName());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent event) {
        if(event.isCancelled()) return;
        for(int i = 0; i < event.getItemDrop().getItemStack().getAmount(); i++) {
            checkForAchievementEvents(event.getPlayer(), "drop " + event.getItemDrop().getItemStack().getType()
                    + " named " + event.getItemDrop().getItemStack().getItemMeta().getDisplayName());
        }
        checkForAchievementEvents(event.getPlayer(), "T_drop " + event.getItemDrop().getItemStack().getType()
                + " named " + event.getItemDrop().getItemStack().getItemMeta().getDisplayName());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCraft(CraftItemEvent event) {
        if(event.isCancelled()) return;
        for(int i = 0; i < event.getCurrentItem().getAmount(); i++) {
            checkForAchievementEvents((Player) event.getWhoClicked(), "craft " + event.getCurrentItem().getType());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEnchant(EnchantItemEvent event) {
        if(event.isCancelled()) return;
        checkForAchievementEvents(event.getEnchanter(), "enchant " + event.getItem().getType()
                + " named " + event.getItem().getItemMeta().getDisplayName());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFish(PlayerFishEvent event) {
        if(event.isCancelled()) return;
        if(event.getCaught() != null && event.getCaught() instanceof Item && event.getState().toString().equals("CAUGHT_FISH")) {
            checkForAchievementEvents(event.getPlayer(), "fish " + ((Item) event.getCaught()).getItemStack().getType());
        }
        if(event.getCaught() != null && !event.getState().toString().equals("CAUGHT_FISH")) {
            checkForAchievementEvents(event.getPlayer(), "catch " + event.getCaught().getType());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onShootBow(EntityShootBowEvent event) {
        if(event.isCancelled()) return;
        if(event.getEntity() instanceof Player) {
            checkForAchievementEvents((Player) event.getEntity(), "shoot bow named " + event.getBow().getItemMeta().getDisplayName());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if(event.isCancelled()) return;
        checkForAchievementEvents(event.getPlayer(), "command " + event.getMessage());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if(event.isCancelled()) return;
        checkForAchievementEvents(event.getPlayer(), "chat " + event.getMessage());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        for(GUIHandler gui : GUIHandler.registeredInventories) { // Loop through registered inventories
            if (event.getInventory().equals(gui.getInventory())) { // If player's inventory is in registered inventories
                event.setCancelled(true); // Cancel drag event
                break; // Exit loop
            }
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        for(GUIHandler gui : GUIHandler.registeredInventories) { // Loop through registered inventories
            if (event.getInventory().equals(gui.getInventory())) { // If player's inventory is in registered inventories
                int currentStart = gui.getCurrentStart();
                switch(event.getSlot()) {
                    case 53: // Next page button
                        if(AchievementsEngine.achievements.size() > currentStart + 28) { // Check if there's second page
                            GUIHandler.New(((Player) event.getWhoClicked()).getPlayer(), gui.setCurrentStart(currentStart + 28)); // Create new GUI (next 28 achievements)
                        }
                        break;
                    case 45: // Previous page button
                        if(gui.getCurrentStart() >= 28) { // Check if there's previous page
                            GUIHandler.New(((Player) event.getWhoClicked()).getPlayer(), gui.setCurrentStart(currentStart - 28)); // Create new GUI (previous 28 achievements)
                        }
                        break;
                }
                event.setCancelled(true); // Cancel click event
                break; // Exit loop
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) { // Remove inventory from registered inventories on close
        for(GUIHandler gui : GUIHandler.registeredInventories) { // Loop through registered inventories
            if (event.getInventory().equals(gui.getInventory())) { // Check if looped inventory equals closed inventory
                GUIHandler.registeredInventories.remove(gui); // Remove inventory from registered inventories
                break; // Exit loop
            }
        }
    }



}
