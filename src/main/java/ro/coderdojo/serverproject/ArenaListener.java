package ro.coderdojo.serverproject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftCreature;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class ArenaListener implements Listener {

    public World arena;

    ArrayList<Location> beaconLocations = new ArrayList();

    public static HashMap<String, Location> lastLocation = new HashMap<>();

    public ArenaListener(World arena) {
        this.arena = arena;
        locateBeacons();
//            placePowerBlock();
        blocksTimer();

    }

    @EventHandler
    public void onJoin(PlayerChangedWorldEvent event) {

        Player player = event.getPlayer();
        
        if(player.getWorld() != arena) return;

        player.getActivePotionEffects().forEach((effect) -> {
            player.removePotionEffect(effect.getType());
        });

        player.setExhaustion(0);
        player.setFoodLevel(20);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION,3600,1));
        player.getInventory().clear();
        player.setExhaustion​(0);
        player.setAllowFlight(false);
        player.setCanPickupItems(false);
    }
    
    @EventHandler
    public void OnMove(PlayerMoveEvent event) {
    
           Player player = event.getPlayer();
            boolean isFirstTimeOnBlock = true;
        if (lastLocation.get(player.getName()) != null) {
            Location lastTeleportBlock = lastLocation.get(player.getName()).getBlock().getLocation();
            Location playerPositionBlock = player.getLocation().getBlock().getLocation();

            int lastTeleportBlockX = lastTeleportBlock.getBlockX();
            int lastTeleportBlockZ = lastTeleportBlock.getBlockZ();

            int playerPositionBlockX = playerPositionBlock.getBlockX();
            int playerPositionBlockZ = playerPositionBlock.getBlockZ();

            if (lastTeleportBlockX == playerPositionBlockX && lastTeleportBlockZ == playerPositionBlockZ) {
                isFirstTimeOnBlock = false;   
                return;
            } else {
                lastLocation.remove(player.getName());
                isFirstTimeOnBlock = true;
            }
        }
        onTeleportMove(event);
        getGodKit(event);
        petSpawn(event);
        petOnMove(event);
        getWitchKit(event);
        getHealing(event);
        
    }
    
    //------------------------------------------------------------------------------------------
    
    public void onTeleportMove(PlayerMoveEvent event) {
        if (event.getPlayer().getWorld() != arena) {
            return;
        }

        Player player = event.getPlayer();

        

        //if the block at the player location minus 1 on y is dirt
        if (event.getPlayer().getLocation().subtract(0, 1, 0).getBlock().getType() == Material.BEACON) {
            Location chance = beaconLocations.get(new Random().nextInt(beaconLocations.size()));
            event.getPlayer().teleport(chance);
//            player.sendMessage("You have been teleported to " + chance);
            lastLocation.put(player.getName(), chance);
        }
    }

    public void locateBeacons() {
        int amount = 0;

        for (Chunk chunk : arena.getLoadedChunks()) {
            int cx = chunk.getX() << 4;
            int cz = chunk.getZ() << 4;
            for (int x = cx; x < cx + 16; x++) {
                for (int z = cz; z < cz + 16; z++) {
                    for (int y = 0; y < 128; y++) {
                        Block block = arena.getBlockAt(x, y + 1, z);
                        if (block.getType() == Material.BEACON) {
//                            System.out.println("Found beacon: " + block.getLocation());
                            beaconLocations.add(arena.getHighestBlockAt(block.getLocation()).getLocation().subtract(0, -1, 0));
                            amount++;
                        }
                    }
                }
            }
        }
        System.out.println("Total beacons found: " + amount);
    }

    //**************************** GOD KIT *********************************
    ArrayList<Location> blockLocations = new ArrayList<>();

    
    public void getGodKit(PlayerMoveEvent event) {

        Player player = event.getPlayer();

        if (event.getPlayer().getWorld() != arena) {
            return;
        }
        
        Location kitLocation = event.getPlayer().getLocation().subtract(0, 1, 0);
        
        if (kitLocation.getBlock().getType() == Material.DIAMOND_BLOCK) {
            
            player.getInventory().clear();

            ItemStack godSword = new ItemStack(Material.DIAMOND_SWORD, 1);
            godSword.addEnchantment(Enchantment.KNOCKBACK, 2);

            player.getInventory().addItem(godSword);
            player.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET, 1));
            player.getInventory().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE, 1));
            player.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS, 1));
            player.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS, 1));
            
            lastLocation.put(player.getName(), kitLocation);
            
            kitLocation.getBlock().setType(Material.AIR);
            kitLocation.getBlock().setType(Material.SAND);
          
            
             powerTimer(player);
        }
//        powerTimer();
    }

        //*******************  animal_pet with an invisible golem in it  **************************
    Map<String, List<CraftCreature>> pets = new HashMap<>();

    
    public void petSpawn(PlayerMoveEvent event) {

        Player player = event.getPlayer();

        if (event.getPlayer().getWorld() != arena) {
            return;
        }

        Location spawnLocation = event.getPlayer().getLocation().subtract(0, 1, 0);

        if (spawnLocation.getBlock().getType() == Material.EMERALD_BLOCK) {

            pets.put(player.getName(), new ArrayList<>());
            CraftCreature cow = (CraftCreature) arena.spawnEntity(spawnLocation.add(0, 1, 0), EntityType.COW);
            CraftCreature golem = (CraftCreature) arena.spawnEntity(spawnLocation.add(0, 1, 0), EntityType.IRON_GOLEM);
            pets.get(player.getName()).add(golem);
            pets.get(player.getName()).add(cow);

            cow.setPassenger(golem);
            golem.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 3600, 2));

            spawnLocation.subtract(0, 1, 0).getBlock().setType(Material.AIR);
            spawnLocation.subtract(0, 1, 0).getBlock().setType(Material.SAND);
            
            powerTimer(player);
        }

    }

    
    public void petOnMove(PlayerMoveEvent event) {

        if (event.getPlayer().getWorld() != arena) {
            return;
        }

        Player player = event.getPlayer();

//                double speed = player.getWalkSpeed();
        Location loc = player.getLocation().add(1, 0, 1);

        List<CraftCreature> plP = pets.get(player.getName());
        if (plP != null) {
            for (CraftCreature creature : plP) {
                creature.getHandle().getNavigation().a(loc.getX()+3, loc.getY(), loc.getZ()+3 , 1.6);
                creature.setMaxHealth(100);
                creature.setHealth(100);
                if (loc.distanceSquared(creature.getLocation()) > 100 && player.isOnGround() ) {
                    creature.teleport(loc);
                }
            }
        }
    }
    
    @EventHandler
    public void onEntityHit(EntityDamageByEntityEvent event) {
        
        if (event.getEntity().getWorld() != arena) {
            return;
        }
        
        if (event.getEntity() instanceof Player) { // cu Monster merge - NU uita ca e si la money schimbat
            if (event.getDamager() instanceof Player) {
                Player p = (Player) event.getDamager();
                List<CraftCreature> plP = pets.get(p.getName());
                if (plP != null) {
                    for (CraftCreature creature : plP) {
                        creature.setTarget((LivingEntity) event.getEntity());
                    }
                }
            }
        }
    }

    //*********************** WITCH KIT **********************************
    
    public void getWitchKit(PlayerMoveEvent event) {

        Player player = event.getPlayer();

        if (event.getPlayer().getWorld() != arena) {
            return;
        }

        Location kitLocation = event.getPlayer().getLocation().subtract(0, 1, 0);
        
        if (kitLocation.getBlock().getType() == Material.GOLD_BLOCK) {
            
            player.getInventory().clear();

            ItemStack witchSword = new ItemStack(Material.GOLD_SWORD, 1);
            witchSword.addEnchantment(Enchantment.KNOCKBACK, 2);

            player.getInventory().addItem(witchSword);
            player.getInventory().setHelmet(new ItemStack(Material.GOLD_HELMET, 1));
            player.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE, 1));
            player.getInventory().setLeggings(new ItemStack(Material.GOLD_LEGGINGS, 1));
            player.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS, 1));
            
            lastLocation.put(player.getName(), kitLocation);
            
            kitLocation.getBlock().setType(Material.AIR);
            kitLocation.getBlock().setType(Material.SAND);

            
             powerTimer(player);
        }
    }
    
    //------------------Healing Block----------------------------
    
    public void getHealing(PlayerMoveEvent event){
        Player player = event.getPlayer();

        if (event.getPlayer().getWorld() != arena) {
            return;
        }

        Location healLocation = event.getPlayer().getLocation().subtract(0, 1, 0);
        
        if (healLocation.getBlock().getType() == Material.REDSTONE_BLOCK) {
            
            player.addPotionEffect(new PotionEffect (PotionEffectType.HEAL, 50, 2));
            
            lastLocation.put(player.getName(), healLocation);
            
            healLocation.getBlock().setType(Material.AIR);
            healLocation.getBlock().setType(Material.SAND);
        }
    }
    
    //*********************************************************************
    
    private void blocksTimer() {
        RepeatTimer timer = new RepeatTimer();
        timer.runTaskTimer(MainPlugin.plugin, 0L, 20*20L);
    }

//        public void placePowerBlock(){
//            PowerBlocks powerBlock = new PowerBlocks();
//            powerBlock.placeBlock(arena);
//        }
    
    private void powerTimer(Player player){
        
        CountDownTimer timer = new CountDownTimer(pets.get(player.getName()),player);
        timer.runTaskTimer(MainPlugin.plugin, 1,1);
    }
}
