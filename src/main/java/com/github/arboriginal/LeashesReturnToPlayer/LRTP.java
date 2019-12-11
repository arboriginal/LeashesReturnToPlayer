package com.github.arboriginal.LeashesReturnToPlayer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityUnleashEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerUnleashEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class LRTP extends JavaPlugin implements Listener {
    private final String             dropK = "VoluntaryDropped";
    private final FixedMetadataValue DropV = new FixedMetadataValue(this, true);

    private HashMap<String, HashSet<UUID>> leashes = new HashMap<String, HashSet<UUID>>();

    // -- JavaPlugin methods -------------------------------------------------------------------------------------------

    @Override
    public void onEnable() {
        super.onEnable();
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    // -- Listener methods ---------------------------------------------------------------------------------------------

    @EventHandler
    private void onEntityUnleash(EntityUnleashEvent e) {
        Entity s = e.getEntity();
        if (!(s instanceof LivingEntity)) return;
        Entity h = ((LivingEntity) s).getLeashHolder();
        if (h != null && h instanceof Player) set((Player) h, s.getLocation());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    private void onItemSpawn(ItemSpawnEvent e) {
        Item i = e.getEntity();
        if (i.hasMetadata(dropK)) return;

        ItemStack s = i.getItemStack();
        if (s.getType() != Material.LEAD) return;

        String        k = locK(e.getLocation());
        HashSet<UUID> h = leashes.get(k);
        if (h == null) return;

        for (UUID u : h) {
            del(k, u);
            Player p = Bukkit.getPlayer(u);
            if (p != null && (p.getInventory().addItem(s).isEmpty())) break;
        }

        i.remove();
        e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    private void onPlayerDropItem(PlayerDropItemEvent e) {
        Item i = e.getItemDrop();
        if (i.getItemStack().getType() == Material.LEAD) i.setMetadata(dropK, DropV);
    }

    @EventHandler(ignoreCancelled = true)
    private void onPlayerUnleashEntity(PlayerUnleashEntityEvent e) {
        set((Player) e.getPlayer(), e.getEntity().getLocation());
    }

    // -- Private methods ----------------------------------------------------------------------------------------------

    private void del(String k, UUID u) {
        HashSet<UUID> h = leashes.get(k);
        if (h == null) return;
        if (h.size() == 1) leashes.remove(k);
        else {
            h.remove(u);
            leashes.put(k, h);
        }
    }

    private void set(Player p, Location l) {
        String        k = locK(l);
        HashSet<UUID> h = leashes.get(k);
        UUID          u = p.getUniqueId();

        if (h == null) h = new HashSet<UUID>();

        h.add(u);
        leashes.put(k, h);

        new BukkitRunnable() {
            @Override
            public void run() {
                del(k, u);
            }
        }.runTask(this);
    }

    private String locK(Location l) {
        return l.getWorld().getName() + "@" + l.getBlockX() + "/" + l.getBlockY() + "/" + l.getBlockZ();
    }
}
