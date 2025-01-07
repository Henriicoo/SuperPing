package com.henriquenapimo1.ping;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.util.*;

public final class SuperPing extends JavaPlugin implements Listener {

    private static Map<Integer,Team> teams;
    private final List<Object> pingList = new ArrayList<>();
    private final List<UUID> playerPingList = new ArrayList<>();
    private final Map<UUID, Object> pingWaitList = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        teams = new PingMenu().getColorTeams(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    @Override
    public void onDisable() {
        teams.forEach((integer, team) -> {
            if(team != null)
                team.unregister();
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if(event.getPlayer().getInventory().getItemInMainHand().getType() != Material.AIR &&  event.getPlayer().getInventory().getItemInMainHand().getType() == Material.COMPASS) {
            event.setCancelled(true);
            handlePingEvent(event.getPlayer(), PingMenu.PingType.PADRAO);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if(event.getItem() == null || event.getItem().getType() != Material.COMPASS)
            return;

        event.setCancelled(true);

        if(event.getAction().isRightClick())
            handlePingEvent(event.getPlayer(), PingMenu.PingType.PADRAO);

        if(event.getAction().isLeftClick()) {
            if(hasCooldown(event.getPlayer()))
                return;

            pingWaitList.put(event.getPlayer().getUniqueId(),getTarget(event.getPlayer(),150));
            event.getPlayer().openInventory(PingMenu.getPingMenu());
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onMenuInteract(InventoryClickEvent event) {
        if(!event.getView().title().equals(Component.text("Selecione o tipo de ping")) || !(event.getWhoClicked() instanceof Player))
            return;

        event.setCancelled(true);

        if(Arrays.asList(0,1,7,0).contains(event.getRawSlot()))
            return;

        handlePingEvent((Player) event.getWhoClicked(), PingMenu.PingType.getByValue(event.getRawSlot()-2));
        event.getWhoClicked().closeInventory();
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onCloseInv(InventoryCloseEvent event) {
        if(!event.getView().title().equals(Component.text("Selecione o tipo de ping")) || !(event.getPlayer() instanceof Player))
            return;

        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> pingWaitList.remove(event.getPlayer().getUniqueId()),20L);
    }

    private boolean hasCooldown(Player p) {
        if(playerPingList.contains(p.getUniqueId())) {
            p.sendActionBar(Component.text("§cEspere alguns segundos antes de pingar novamente."));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_SNARE, 0.5F, 0.5F);
            return true;
        }
        return false;
    }

    private void handlePingEvent(Player p, PingMenu.PingType type) {
        if(hasCooldown(p))
            return;

        Object target;

        if(pingWaitList.containsKey(p.getUniqueId())) {
            target = pingWaitList.get(p.getUniqueId());
            pingWaitList.remove(p.getUniqueId());
        } else
            target = getTarget(p, 150);

        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> playerPingList.remove(p.getUniqueId()),20L);

        if (target instanceof Entity) {
            handlePing(target, PingTarget.ENTITY, ((LivingEntity) target).getEyeLocation(),p, type);
        } else if (target instanceof Block) {
            handlePing(target, PingTarget.BLOCK, ((Block) target).getLocation(),p, type);
        } else {
            p.sendActionBar(Component.text("§cNenhum bloco ou entidade encontrado num raio de 150 blocos."));
            p.playSound(p.getLocation(),Sound.BLOCK_NOTE_BLOCK_SNARE,0.5F,0.5F);
        }
    }

    private enum PingTarget { ENTITY, BLOCK }

    private void handlePing(Object ping, PingTarget t, Location l, Player p, PingMenu.PingType type) {
        if(pingList.contains(ping)) {
            p.sendActionBar(Component.text("§cEsta localização já foi pingada recentemente."));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_SNARE, 0.5F, 0.5F);
            return;
        }

        playerPingList.add(p.getUniqueId());
        pingList.add(ping);

        Location oldCompass = p.getCompassTarget();
        p.setCompassTarget(l);

        // SOM
        l.getWorld().playSound(l, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 1.5F, 1.5F);
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 1F, 1F);

        // PARTÍCULAS TRAIL
        double distance = p.getLocation().distance(l);

        Vector p1 = p.getLocation().add(0,1,0).toVector();
        Vector p2 = l.toVector();

        if(t== PingTarget.BLOCK)
            p2 = l.clone().add(0.5, 0.5, 0.5).toVector();

        Vector vector = p2.clone().subtract(p1).normalize().multiply(0.2);

        double covered = 0;

        for (; covered < distance; p1.add(vector)) {
            l.getWorld().spawnParticle(Particle.DUST, p1.getX(), p1.getY(), p1.getZ(), 1, PingMenu.getParticleColor(type.getValue()));
            covered += 0.2;
        }

        TextDisplay target = l.getWorld().spawn(l, TextDisplay.class);
        target.setBillboard(Display.Billboard.CENTER);
        target.setBackgroundColor(Color.fromRGB(PingMenu.getChatColor(type.getValue()).value()));
        target.setBrightness(new Display.Brightness(8,15));
        target.setSeeThrough(true);

        TextDisplay player = l.getWorld().spawn(l, TextDisplay.class);
        player.setBillboard(Display.Billboard.CENTER);
        player.setBrightness(new Display.Brightness(5,8));
        player.text(Component.empty().append(Component.text("§o⚑ "+p.getName()).color(TextColor.color(224,224,224))));
        player.setSeeThrough(true);

        TextDisplay dist = l.getWorld().spawn(l,TextDisplay.class);
        dist.setBillboard(Display.Billboard.CENTER);
        dist.setBrightness(new Display.Brightness(8,15));
        dist.setSeeThrough(true);

        List<Boolean> isEmpty = new ArrayList<>();

        // CASOS ESPECÍFICOS
        if(t == PingTarget.ENTITY) {
            LivingEntity e = (LivingEntity) ping;
            target.text(Component.empty().append(Component.translatable(e.getType().translationKey()).color(TextColor.color(NamedTextColor.DARK_GRAY))));

            e.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,5*20,1,false,false));
            teams.get(type.getValue()).addEntity(e);

            for(int i = 1; i <= 3; i++) {
                isEmpty.add(e.getWorld().getBlockAt(e.getEyeLocation().getBlockX(),e.getEyeLocation().getBlockY()+i, e.getEyeLocation().getBlockZ()).isEmpty());
            }

            int[] taskId = new int[1];

            taskId[0] = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
                int counter = 0;
                @Override
                public void run() {
                    double distText = p.getLocation().distance(e.getLocation());
                    dist.text(Component.text((int) distText+" m"));

                    if(!isEmpty.contains(false)) {
                        l.getWorld().getPlayersSeeingChunk(l.getChunk()).forEach(p -> drawShape(e.getEyeLocation().clone().add(0, 1, 0), p, type));
                    }
                    dist.teleport(e.getEyeLocation().clone().add(0,1.20,0));
                    target.teleport(e.getEyeLocation().clone().add(0,0.95,0));
                    player.teleport(e.getEyeLocation().clone().add(0,0.60,0));

                    p.setCompassTarget(e.getLocation());

                    p.sendActionBar(Component.empty().color(TextColor.color(PingMenu.getChatColor(type.getValue()))).append(
                            Component.text("Alvo: ")
                                    .append(Component.translatable(e.getType().translationKey())
                                            .append(Component.text(" | Distância: "+((int) distText)+" m (X: "+((int) e.getX())+", Y: "+((int) e.getY())+", Z: "+((int) e.getZ())+")")))));

                    counter += 1;
                    if(counter >= 100 || e.isDead()) {
                        Bukkit.getScheduler().cancelTask(taskId[0]);
                        e.removePotionEffect(PotionEffectType.GLOWING);
                        teams.get(type.getValue()).removeEntity(e);
                        pingList.remove(ping);
                        target.remove();
                        player.remove();
                        dist.remove();
                        p.sendActionBar(Component.empty());
                        p.setCompassTarget(oldCompass);
                    }
                }
            },0L,1L);
        } else {
            Block b = (Block) ping;

            boolean isFullBlock = b.getCollisionShape().getBoundingBoxes().stream().allMatch(
                    box -> box.getMinX() == 0.0 && box.getMinY() == 0.0 && box.getMinZ() == 0.0 && box.getMaxX() == 1.0 && box.getMaxY() == 1.0 && box.getMaxZ() == 1.0);

            Entity e;
            double eLoc = 0;
            double yLoc = 0;

            if (isFullBlock && !Tag.PRESSURE_PLATES.isTagged(b.getType()) && !b.getType().toString().contains("CORAL")) {
                e = b.getWorld().spawn(b.getLocation(), Shulker.class);
                e.setGravity(false);
                ((Shulker) e).setAI(false);
                e.setInvulnerable(true);
                yLoc = 0.5;
            } else {
                e = b.getWorld().spawn(b.getLocation().clone().add(0,0,0), BlockDisplay.class);
                ((BlockDisplay) e).setBlock(b.getBlockData());
                eLoc = 0.5;
            }

            e.setGlowing(true);
            e.setInvisible(true);
            teams.get(type.getValue()).addEntity(e);

            target.text(Component.empty().append(Component.translatable(b.getType().translationKey()).color(TextColor.color(NamedTextColor.DARK_GRAY))));

            dist.teleport(e.getLocation().clone().add(eLoc,1.20+0.5+yLoc,eLoc));
            target.teleport(e.getLocation().clone().add(eLoc,0.95+0.5+yLoc,eLoc));
            player.teleport(e.getLocation().clone().add(eLoc,0.60+0.5+yLoc,eLoc));

            for(int i = 1; i <= 3; i++) {
                isEmpty.add(b.getWorld().getBlockAt(b.getX(),b.getY()+i,b.getZ()).isEmpty());
            }

            int[] taskId = new int[1];
            taskId[0] = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
                int counter = 0;
                @Override
                public void run() {
                    double distText = p.getLocation().distance(b.getLocation());
                    dist.text(Component.text((int) distText+" m"));

                    if(e instanceof BlockDisplay)
                        ((BlockDisplay) e).setBlock(b.getBlockData());

                    if(!isEmpty.contains(false))
                        l.getWorld().getPlayersSeeingChunk(l.getChunk()).forEach(p -> drawShape(b.getLocation().clone().add(0.5,2,0.5),p,type));

                    p.sendActionBar(Component.empty().color(TextColor.color(PingMenu.getChatColor(type.getValue()))).append(
                            Component.text("Alvo: ")
                                    .append(Component.translatable(b.getType().translationKey())
                                            .append(Component.text(" | Distância: "+((int) distText)+" m (X: "+b.getX()+", Y: "+b.getY()+", Z: "+b.getZ()+")")))));

                    counter += 1;
                    if(counter >= 100 || b.isEmpty()) {
                        e.remove();
                        pingList.remove(ping);
                        target.remove();
                        player.remove();
                        dist.remove();

                        Bukkit.getScheduler().cancelTask(taskId[0]);
                        p.sendActionBar(Component.empty());
                        p.setCompassTarget(oldCompass);
                    }
                }
            },0L,1L);
        }
    }

    public static Object getTarget(Player player, int range) {
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection();

        for (int i = 0; i < range; i++) {
            Location checkLocation = eyeLocation.clone().add(direction.clone().multiply(i));
            Block b = checkLocation.getBlock();
            if (b.getType() != Material.AIR && b.isSolid() && !Tag.ALL_SIGNS.isTagged(b.getType()) && !b.getType().toString().contains("CHEST")
                    && !Tag.BANNERS.isTagged(b.getType()) && !Tag.BEDS.isTagged(b.getType()) &&
                    !b.getType().toString().equalsIgnoreCase("BAMBOO") && !b.getType().toString().equalsIgnoreCase("POINTED_DRIPSTONE") &&
                    !b.getType().toString().equalsIgnoreCase("SCULK_VEIN")) {
                return checkLocation.getBlock();
            }

            List<Entity> nearbyEntities = (List<Entity>) checkLocation.getWorld().getNearbyEntities(checkLocation, 0.5, 0.5, 0.5);
            for (Entity entity : nearbyEntities) {
                if (entity != player && entity instanceof LivingEntity) {
                    for (Map.Entry<Integer, Team> entry : teams.entrySet()) {
                        Team team = entry.getValue();
                        if (team.hasEntity(entity)) {
                            return null;
                        }
                    }
                    return entity;
                }
            }
        }

        return null;
    }

    private void drawShape(Location location, Player p, PingMenu.PingType type) {
        double space = 0.12;
        double defX = location.getX() - (space * PingMenu.getShape(type.getValue())[0].length / 2) + (space/2);
        double x = defX;
        double y = location.clone().getY() + 2.3;
        double fire = Math.toRadians(p.getYaw());

        for (boolean[] booleans : PingMenu.getShape(type.getValue())) {
            for (boolean aBoolean : booleans) {
                if (aBoolean) {
                    Location target = location.clone();
                    target.setX(x);
                    target.setY(y);

                    Vector v = target.toVector().subtract(location.toVector());
                    v = rotateAroundAxisY(v, fire);

                    location.add(v);
                    p.spawnParticle(Particle.DUST, location.getX(), location.getY(), location.getZ(), 1, PingMenu.getParticleColor(type.getValue()));
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
}
