package pl.achievementsengine;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GUIHandler {

    public Inventory inventory; // inventory for object
    public Player player; // inventory owner
    public int currentStart; // start slot (pages based on this variable)
    public static List<GUIHandler> registeredInventories = new ArrayList<>(); // opened inventories

    public GUIHandler(Player p, int start) {
        this.player = p;
        this.currentStart = start;
        this.Create();
    }

    public static void New(Player p, int start) {
        PlayerAchievementState state = PlayerAchievementState.Create(p); // Create or get state
        if(!state.initialized) { // If state is not initialized (data is not downloaded) then mark GUI as "will be opened when data will be downloaded"
            state.openGUI = true;
            return;
        }
        GUIHandler gui = new GUIHandler(p, start); // Create object
        registeredInventories.add(gui); // Add gui to registered inventories
        gui.Open(); // Open GUI to player
        p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0F, 1.0F); // Play sound
    }

    public void Create() {
        int size = 27; // Min size of the window (9 - border, 9 - achievements, 9 - border)
        for(int i = 1; i <= 3; i++) { // In one window you can see max 3 lines of achievements. Mark, how many lines will be generated
            if(AchievementsEngine.achievements.size() > 7 * i) {
                size += 9;
            }
        }
        int current = currentStart/28+1; // Current page
        int max = AchievementsEngine.achievements.size()/28+1; // Max pages
        PlayerAchievementState state = PlayerAchievementState.Create(player); // Get state
        float c = (float) state.completedAchievements.size() / (float) AchievementsEngine.achievements.size() * 100.0F; // Get completed percent
        int completed = Math.round(c); // Round percent to integer
        inventory = Bukkit.createInventory(null, size, MessageFormat.format(AchievementsEngine.ReadLanguage("gui-title") + " " + completed + "%", current, max)); // Create inventory
        for(int i = 0; i < size; i++) { // Create black background
            AddItem(i, Material.BLACK_STAINED_GLASS_PANE, 1, " ", " ", false);
        }
        for(int i = 10; i < size-9;) { // Create gray background
            if(i > 43) break;
            AddItem(i, Material.LIGHT_GRAY_STAINED_GLASS_PANE, 1, " ", " ", false);
            if(i == 16 || i == 25 || i == 34 || i == 43) { // Border
                i += 3;
            } else {
                i++;
            }
        }
        if(AchievementsEngine.achievements.size() > currentStart+28) { // If there's second page, create "Next page" button
            AddItem(53, Material.ARROW, 1, AchievementsEngine.ReadLanguage("gui-next"), "", false);
        }
        if(currentStart >= 28) { // If there's previous page, create "Previous page" button
            AddItem(45, Material.ARROW, 1, AchievementsEngine.ReadLanguage("gui-previous"), "", false);
        }
        int i = 10; // From which slot we'll start generating achievements. Border - 9 slots (0-8), 1 slot of border (9), so we'll start from 10
        int j = 0; // Iterator.
        for(Achievement a : AchievementsEngine.achievements) { // Loop through all achievements
            if(j < currentStart) { // Loop iterator to current start.
                j++;
                continue;
            }
            if(i > 43) break; // If achievement slot is higher than 43, then break loop. (43 is last slot before border.)
            String desc = a.description; // Set item description to achievement description
            boolean glow = false; // Mark if item is glowing
            if(a.showProgress) { // If achievement has turned on showProgress
                desc = desc + "%nl%" + AchievementsEngine.ReadLanguage("progress"); // Add "Progress:" to description
                for (int k = 0; k < a.events.size(); k++) { // Loop through events
                    if(!state.progress.containsKey(a)) { // If state progress don't have key, create empty progress
                        state.UpdateProgress(a, new int[a.events.size()]);
                    }
                    desc = desc + "%nl%" + AchievementsEngine.ReadLanguage("progress-field-prefix") + a.events.get(k) +
                            ": " + state.progress.get(a)[k] + "/" + a.events.get(k).split(" ")[1]; // Create field. (progress)/(max)
                }
            }
            if(!state.completedAchievements.isEmpty()) { // If player's state completed achievements is not empty
                if(state.completedAchievements.contains(a)) { // If player's state completed achievement contains achievement
                    desc = desc + "%nl%" + ChatColor.GREEN + AchievementsEngine.ReadLanguage("completed"); // Add "completed" to description
                    glow = true; // Make item glow
                }
            }
            AddItem(i, a.item, 1, a.name, desc, glow); // Create item
            if(i == 16 || i == 25 || i == 34 || i == 43) { // Border
                i += 3;
            } else {
                i++;
            }
        }
    }

    public void Open() {
        player.openInventory(inventory);
    }

    public void AddItem(int slot, Material material, int amount, String name, String lore, boolean isGlowing) {
        ItemStack item = new ItemStack(material, amount); // Create item
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(name); // Set name
        meta.setLore(Arrays.asList(lore.split("%nl%")));
        if(isGlowing) { // Create glow effect
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        item.setItemMeta(meta);

        inventory.setItem(slot, item); // Add item to GUI
    }

    public static void CloseAllInventories() { // Close all registered inventories
        List<GUIHandler> guis = new ArrayList<>(GUIHandler.registeredInventories);
        for(GUIHandler handler : guis) {
            handler.player.closeInventory();
        }
    }
}
