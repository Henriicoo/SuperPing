package com.henriquenapimo1.ping;

//import org.bukkit.Bukkit;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
//import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
//import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public final class SuperPing extends JavaPlugin implements Listener {

    private static Team team;
    private final List<Object> pingList = new ArrayList<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this,this);

        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        team = board.registerNewTeam("pingColorBlue");
        team.color(NamedTextColor.AQUA);
    }

    @Override
    public void onDisable() {
        team.unregister();
    }

    private final String pref = "§7[§e§lSuperPing§7]";

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if(event.getAction().isRightClick() && event.getItem() != null && event.getItem().getType() == Material.COMPASS) {
            Object target = getTarget(event.getPlayer(), 50);

            if (target instanceof Entity) {
                handlePing(target, PingType.ENTITY, ((LivingEntity) target).getEyeLocation(),event.getPlayer());
            } else if (target instanceof org.bukkit.block.Block) {
                handlePing(target, PingType.BLOCK, ((Block) target).getLocation(),event.getPlayer());
            } else {
                event.getPlayer().sendMessage(pref+" §cNenhum bloco ou entidade encontrado num raio de 50 blocos.");
                event.getPlayer().playSound(event.getPlayer().getLocation(),Sound.BLOCK_NOTE_BLOCK_SNARE,0.5F,0.5F);
            }
        }
    }

    private enum PingType { ENTITY, BLOCK }

    final Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(85, 255, 255), 1);

    private void handlePing(Object ping, PingType t, Location l, Player p) {
        if(pingList.contains(ping)) {
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_SNARE, 0.5F, 0.5F);
            p.sendMessage(pref + " Já existe um ping neste local!");
            return;
        }

        pingList.add(ping);

        // SOM
        l.getWorld().playSound(l, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 1F, 1F);
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 0.5F, 0.5F);

        // PARTÍCULAS TRAIL
        double distance = p.getEyeLocation().distance(l);

        Vector p1 = p.getEyeLocation().toVector();
        Vector p2 = l.toVector();

        Vector vector = p2.clone().subtract(p1).normalize().multiply(0.2);

        double covered = 0;

        for (; covered < distance; p1.add(vector)) {
            l.getWorld().spawnParticle(Particle.DUST, p1.getX(), p1.getY(), p1.getZ(), 1, dust);
            covered += 0.2;
        }

        // CASOS ESPECÍFICOS
        if(t == PingType.ENTITY) {
            LivingEntity e = (LivingEntity) ping;

            e.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,5*20,1,false,false));
            team.addEntity(e);

            int[] taskId = new int[1];
            taskId[0] = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
                int counter = 0;
                @Override
                public void run() {
                    drawArrow(((LivingEntity) l.getWorld().getEntity(e.getUniqueId())).getEyeLocation()); // TODO: algo pra cancelar caso morra/suma/etc
                    counter += 1;
                    if(counter >= 100) {
                        Bukkit.getScheduler().cancelTask(taskId[0]);
                        e.removePotionEffect(PotionEffectType.GLOWING);
                        team.removeEntity(e);
                        pingList.remove(ping);
                    }
                }
            },0L,1L);
        } else {
            Block b = (Block) ping;
            Shulker e = b.getWorld().spawn(b.getLocation(), Shulker.class);
            e.setGravity(false);
            e.setAI(false);
            e.setInvulnerable(true);
            e.setInvisible(true);

            e.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,5*20,1,false,false));
            team.addEntity(e);

            Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
                e.remove();
                pingList.remove(ping);
            },100L);
        }
    }

    public static Object getTarget(Player player, int range) {
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection();

        for (int i = 0; i < range; i++) {
            Location checkLocation = eyeLocation.clone().add(direction.clone().multiply(i));

            if (checkLocation.getBlock().getType() != Material.AIR) {
                return checkLocation.getBlock();
            }

            List<Entity> nearbyEntities = (List<Entity>) checkLocation.getWorld().getNearbyEntities(checkLocation, 0.5, 0.5, 0.5);
            for (Entity entity : nearbyEntities) {
                if (!team.hasEntity(entity) && entity != player) {
                    return entity;
                }
            }
        }

        return null;
    }

    private void drawArrow(Location location) {
        double space = 0.20;
        double defX = location.getX() - (space * arrow[0].length / 2) + (space/2);
        double x = defX;
        double y = location.clone().getY() + 3.5;
        double fire = Math.toRadians(location.getYaw());

        for (boolean[] booleans : arrow) {
            for (boolean aBoolean : booleans) {
                if (aBoolean) {
                    Location target = location.clone();
                    target.setX(x);
                    target.setY(y);

                    Vector v = target.toVector().subtract(location.toVector());
                    v = rotateAroundAxisY(v, fire);

                    location.add(v);
                    location.getWorld().spawnParticle(Particle.DUST, location.getX(), location.getY(), location.getZ(), 1, dust);
                    location.subtract(v);
                }
                x += space;
            }
            y -= space;
            x = defX;
        }
    }

    public static Vector rotateAroundAxisY(Vector v, double fire) {
        double x, z, cos, sin;
        cos = Math.cos(fire);
        sin = Math.sin(fire);
        x = v.getX() * cos - v.getZ() * sin;
        z = v.getX() * sin + v.getZ() * cos;
        return v.setX(x).setZ(z);
    }

    private final boolean o = false;
    private final boolean x = true;

    private final boolean[][] arrow = {
            { o, o, o, o, o, o, x, x, x, x, o, o, o, o, o, o},
            { o, o, o, o, o, o, x, x, x, x, o, o, o, o, o, o},
            { o, o, o, o, o, o, x, x, x, x, o, o, o, o, o, o},
            { o, o, o, o, o, o, x, x, x, x, o, o, o, o, o, o},
            { o, o, o, o, o, o, x, x, x, x, o, o, o, o, o, o},
            { o, o, o, o, o, o, x, x, x, x, o, o, o, o, o, o},
            { o, o, o, o, o, o, x, x, x, x, o, o, o, o, o, o},
            { o, o, o, o, o, o, x, x, x, x, o, o, o, o, o, o},
            { o, o, o, o, o, o, x, x, x, x, o, o, o, o, o, o},
            { o, o, o, o, o, o, x, x, x, x, o, o, o, o, o, o},
            { o, o, o, x, x, x, x, x, x, x, x, x, x, o, o, o},
            { o, o, o, o, x, x, x, x, x, x, x, x, o, o, o, o},
            { o, o, o, o, o, x, x, x, x, x, x, o, o, o, o, o},
            { o, o, o, o, o, o, x, x, x, x, o, o, o, o, o, o},
            { o, o, o, o, o, o, o, x, x, o, o, o, o, o, o, o}};
}
