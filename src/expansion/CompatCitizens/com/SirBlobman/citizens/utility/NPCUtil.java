package com.SirBlobman.citizens.utility;

import com.SirBlobman.citizens.config.ConfigCitizens;
import com.SirBlobman.citizens.config.ConfigData;
import com.SirBlobman.combatlogx.utility.Util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;

import java.util.Map;
import java.util.UUID;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.trait.Inventory;
import net.citizensnpcs.trait.LookClose;

public class NPCUtil extends Util {
    public static Map<UUID, NPC> NPC_REGISTRY = newMap();
    
    public static NPC createNPC(Player p, Location lastLocation) {
        NPCRegistry reg = CitizensAPI.getNPCRegistry();
        UUID uuid = p.getUniqueId();
        String name = p.getName();
        double health = p.getHealth();
        PlayerInventory pi = p.getInventory();
        EntityType type = EntityType.valueOf(ConfigCitizens.OPTION_NPC_ENTITY_TYPE);
        if(type == null || !type.isAlive()) {
            Util.print("Invalid NPC type '" + ConfigCitizens.OPTION_NPC_ENTITY_TYPE + "'. Defaulting to Player");
            type = EntityType.PLAYER;
        }
        
        Inventory traitInventory = new Inventory();
        traitInventory.setContents(pi.getContents());
        pi.clear();
        
        NPC npc = reg.createNPC(type, name);
        npc.setProtected(false);
        npc.addTrait(LookClose.class);
        npc.addTrait(CombatNPC.class);
        npc.addTrait(traitInventory);
        npc.spawn(lastLocation);
        
        LivingEntity le = (LivingEntity) npc.getEntity();
        le.setHealth(health);
        
        NPC_REGISTRY.put(uuid, npc);
        runLater(new Runnable() {
            @Override
            public void run() {
                removeNPC(uuid);
            }
        }, ConfigCitizens.OPTION_NPC_SURVIVAL_TIME * 20L);
        
        return npc;
    }
    
    public static double getHealth(NPC npc) {
        if(npc == null) return 0.0D;
        if(npc.isSpawned()) {
            Entity e = npc.getEntity();
            if(e instanceof LivingEntity) {
                LivingEntity le = (LivingEntity) e;
                double health = le.getHealth();
                return health;
            } else return 0.0D;
        } else return 0.0D;
    }
    
    public static void removeNPC(UUID uuid) {
        NPC npc = NPC_REGISTRY.get(uuid);
        if(npc != null) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            Location l = npc.getStoredLocation();
            double health = getHealth(npc);
            ConfigData.force(op, "health", health);
            ConfigData.force(op, "last location", l);
            npc.despawn();
            npc.destroy();
            NPC_REGISTRY.remove(uuid);
        } else return;
    }
    
    @SuppressWarnings("deprecation")
    public static void removeNPC(NPC npc) {
        if(npc != null) {
            String name = npc.getName();
            OfflinePlayer op = Bukkit.getOfflinePlayer(name);
            Location l = npc.getStoredLocation();
            double health = getHealth(npc);
            ConfigData.force(op, "health", health);
            ConfigData.force(op, "last location", l);
            npc.despawn();
            npc.destroy();
            NPC_REGISTRY.remove(op.getUniqueId());
        } else return;
    }
    
    public static void removeAllNPCs() {
        for(UUID uuid : newList(NPC_REGISTRY.keySet())) {
            removeNPC(uuid);
        }
    }
    
    public static class CombatNPC extends Trait {
        public CombatNPC() {super("combatlogx_npc");}
    }
}