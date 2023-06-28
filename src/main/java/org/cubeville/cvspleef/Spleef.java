
package org.cubeville.cvspleef;

import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.World;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

import org.cubeville.cvgames.models.Game;
import org.cubeville.cvgames.models.GameRegion;
import org.cubeville.cvgames.utils.GameUtils;
import org.cubeville.cvgames.vartypes.GameVariableFlag;
import org.cubeville.cvgames.vartypes.GameVariableList;
import org.cubeville.cvgames.vartypes.GameVariableLocation;
import org.cubeville.cvgames.vartypes.GameVariableString;
import org.cubeville.cvgames.vartypes.GameVariableRegion;

public class Spleef extends Game implements Listener {
    
    private int task;
    private boolean gameStarted;
    
    private int xmin, xmax, zmin, zmax;
    private int yl;
    private World world;

    private boolean sandSpleef = false;
    private boolean pizzaSpleef = false;

    private Random rnd = new Random();
    
    public Spleef(String id, String arenaName) {
        super(id, arenaName);
        
        addGameVariable("message-portal", new GameVariableString());
        addGameVariable("spleef-layer", new GameVariableRegion());
        addGameVariable("spawn", new GameVariableList<>(GameVariableLocation.class));
        addGameVariable("spleeftype-sand", new GameVariableFlag());
        addGameVariable("spleeftype-pizza", new GameVariableFlag());
    }

    // "state" is a variable that exists in every game that allows the games plugin to track which players are playing the game
    // All this method does is map the state you have to your custom defined state.
    protected SpleefState getState(Player p) {
        if (state.get(p) == null || !(state.get(p) instanceof SpleefState)) return null;
        return (SpleefState) state.get(p);
    }

    // This method runs when the game starts, and is used to set up the game (including player states).
    @Override
    public void onGameStart(Set<Player> players) {
        sandSpleef = (Boolean) getVariable("spleeftype-sand");
        pizzaSpleef = (Boolean) getVariable("spleeftype-pizza");
        
        GameRegion spleeflayer = (GameRegion) getVariable("spleef-layer");
        yl = spleeflayer.getMin().getBlockY();

        xmin = spleeflayer.getMin().getBlockX();
        xmax = spleeflayer.getMax().getBlockX();
        zmin = spleeflayer.getMin().getBlockZ();
        zmax = spleeflayer.getMax().getBlockZ();
        world = spleeflayer.getMin().getWorld();

        for(int x = xmin; x <= xmax; x++) {
            for(int z = zmin; z <= zmax; z++) {
                if(sandSpleef) {
                    if(world.getBlockAt(x, yl, z).getType() == Material.AIR) {
                        world.getBlockAt(x, yl, z).setType(Material.SANDSTONE);
                    }
                }
                if(pizzaSpleef) {
                    Block block = world.getBlockAt(x, yl, z);
                    Material m = block.getType();
                    if(m == Material.AIR || m == Material.NETHERRACK || m == Material.OAK_LEAVES || m == Material.SNOW_BLOCK || m == Material.BLACK_WOOL || m == Material.YELLOW_TERRACOTTA) {
                        int r = rnd.nextInt(100);
                        if(r == 0 || r == 1) block.setType(Material.NETHERRACK);
                        else if(r == 2) block.setType(Material.OAK_LEAVES);
                        else if(r == 3) block.setType(Material.SNOW_BLOCK);
                        else if(r == 4) block.setType(Material.BLACK_WOOL);
                        else block.setType(Material.YELLOW_TERRACOTTA);
                    }
                }
            }
        }

        List<Location> spawns = (List<Location>) getVariable("spawn");

        int pcount = 0;
        for(Player player: players) {
            state.put(player, new SpleefState());
            player.teleport(spawns.get(pcount++));
            if(pizzaSpleef) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "loadout apply pizzaspleef player:" + player.getName());
            }
        }

        gameStarted = false;

        if(sandSpleef) {
            Bukkit.getScheduler().runTaskLater(CVSpleef.getInstance(), new Runnable() {
                    @Override
                    public void run() {
                        world.getBlockAt(xmin, yl, zmin).setType(Material.SAND, true);
                        world.getBlockAt(xmax, yl, zmin).setType(Material.SAND, true);
                        world.getBlockAt(xmax, yl, zmax).setType(Material.SAND, true);
                        world.getBlockAt(xmin, yl, zmax).setType(Material.SAND, true);
                        gameStarted = true;
                    }
                }, 100L);
        }
        else {
            gameStarted = true;
        }
    }
    
    private void title(String title, String subtitle, boolean messageCopy) {
        if(getVariable("message-portal") == null) return;
        
        String portalName = (String) getVariable("message-portal");
        
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "cvportal sendtitle " + portalName + " \"" + title + "\" \"" + subtitle + "\" 20 40 20");
        if(messageCopy) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "cvportal sendmessage " + portalName + " \"" + title + " " + subtitle + "\"");
        }
    }
    
    // This method runs when the game finished
    // This happens if finishGame() is called, or if all players leave the game
    @Override
    public void onGameFinish() {
        if(task != -1)
            Bukkit.getScheduler().cancelTask(task);
        task = -1;
        if(state.size() > 0) {
            if(state.size() == 1) {
                String portalName = (String) getVariable("message-portal");     
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "cvportal sendtitle " + portalName + " \"&a" + state.keySet().iterator().next().getName() + "\" \"&ewon, congrats!\" 20 40 20");
            }
            for(Player player: state.keySet()) {
                player.teleport((Location) getVariable("exit"));
                player.getInventory().clear();
            }
            state.clear();
            gameStarted = false;
        }
    }

    @Override
    public void onCountdown(int counter) {
        if(counter % 10 == 0 || counter == 5) {
            String portalName = (String) getVariable("message-portal");     
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "cvportal sendtitle " + portalName + " \"&a" + counter + " seconds\" \"&eNext round starting soon\" 20 40 20");
        }
    }

    // This method runs when a player in the game logs out of the game
    @Override
    public void onPlayerLeave(Player player) {
        state.remove(player);
        player.getInventory().clear();
        if(state.size() < 2)
            finishGame();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        if(state.size() == 0) return;
        if(!gameStarted) return;

        Player player = event.getPlayer();
        if(!player.getWorld().equals(world)) return;
        if(!state.containsKey(player)) return;

        Block block = event.getBlock();
        boolean b = false;
        if(block.getType() == Material.NETHERRACK) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 150, 2));
            b = true;
        }
        else if(block.getType() == Material.OAK_LEAVES) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 150, 2));
            b = true;
        }
        else if(block.getType() == Material.SNOW_BLOCK) {
            for(Player p: state.keySet()) {
                if(p.equals(player)) continue;
                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 150, 1));
            }
            b = true;
        }
        else if(block.getType() == Material.BLACK_WOOL) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 150, 1));
            b = true;
        }
        else if(block.getType() == Material.YELLOW_TERRACOTTA) {
            b = true;
        }
        if(b) {
            event.setCancelled(false);
        }
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if(state.size() == 0) return;
        if(!gameStarted) return;
        
        Player player = event.getPlayer();
        if(!player.getWorld().equals(world)) return;
        if(!state.containsKey(player)) return;

        double y = event.getTo().getY();
        if(y < yl - 3) {
            state.remove(player);
            player.sendMessage("§cBetter luck next time!");
            player.teleport((Location) getVariable("exit"));
            player.getInventory().clear();
            if(state.size() < 2)
                finishGame();
        }

        if(sandSpleef == false) return;
        
        if(y < yl + 0.9 || y > yl + 1.2) return;

        {
            double xbl = event.getTo().getBlockX();
            double zbl = event.getTo().getBlockZ();
            if(xbl < xmin || xbl > xmax || zbl < zmin || zbl > zmax) return;
        }
        
        double x = event.getTo().getX();
        double z = event.getTo().getZ();
        
        int xs, xe;
        double xp = x - Math.floor(x);
        if(xp > .7) {
            xs = ((Double)Math.floor(x)).intValue();
            xe = xs + 1;
        }
        else if(xp < .3) {
            xs = ((Double)Math.floor(x)).intValue() - 1;
            xe = xs + 1;
        }
        else {
            xs = ((Double)Math.floor(x)).intValue();
            xe = xs;
        }

        int zs, ze;
        double zp = z - Math.floor(z);
        if(zp > .7) {
            zs = ((Double)Math.floor(z)).intValue();
            ze = zs + 1;
        }
        else if(zp < .3) {
            zs = ((Double)Math.floor(z)).intValue() - 1;
            ze = zs + 1;
        }
        else {
            zs = ((Double)Math.floor(z)).intValue();
            ze = zs;
        }

        for(int xb = xs; xb <= xe; xb++) {
            for(int zb = zs; zb <= ze; zb++) {
                Block block = event.getTo().getWorld().getBlockAt(xb, yl, zb);
                if(block.getType() == Material.SANDSTONE) {
                    block.setType(Material.SAND, true);
                }
            }
        }
    }

    // This is an example of how to send a custom scoreboard to the arena
    private void displayScoreboard() {
        List<String> scoreboardLines = List.of(
                "Hello world!",
                "§a§lThis is a scoreboard!",
                "§c§oDon't use custom hex colors here plz it will break"
        );
        Scoreboard scoreboard = GameUtils.createScoreboard(arena, "&b&lCool Title", scoreboardLines);
        sendScoreboardToArena(scoreboard);
    }
}
