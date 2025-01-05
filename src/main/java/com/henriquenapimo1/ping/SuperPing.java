package com.henriquenapimo1.ping;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public final class SuperPing extends JavaPlugin implements Listener {

    private static Team team;
    private final List<Object> pingList = new ArrayList<>();
    private final List<UUID> playerPingList = new ArrayList<>();
    private ProtocolManager protocolManager;

    @Override
    public void onEnable() {

        if (getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
            Logger.getLogger("Minecraft").severe("[SuperPing] - O plugin ProtocolLib não foi encontrado. Este plugin será desabilitado");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        protocolManager = ProtocolLibrary.getProtocolManager();

        getServer().getPluginManager().registerEvents(this, this);

        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        if (board.getTeam("pingColorBlue") == null) {
            team = board.registerNewTeam("pingColorBlue");
        } else {
            team = board.getTeam("pingColorBlue");
        }

        if (team != null) {
            team.color(NamedTextColor.AQUA);
        }
    }

    @Override
    public void onDisable() {
        if(team != null)
            team.unregister();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteracEntity(PlayerInteractEntityEvent event) {
        if(event.getPlayer().getInventory().getItemInMainHand().getType() != Material.AIR &&  event.getPlayer().getInventory().getItemInMainHand().getType() == Material.COMPASS) {
            event.setCancelled(true);
            handlePingEvent(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if(event.getAction().isRightClick() && event.getItem() != null && event.getItem().getType() == Material.COMPASS) {
            event.setCancelled(true);
            handlePingEvent(event.getPlayer());
        }
    }

    private void handlePingEvent(Player p) {
        if(playerPingList.contains(p.getUniqueId())) {
            p.sendActionBar(Component.text("§cEspere alguns segundos antes de pingar novamente."));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_SNARE, 0.5F, 0.5F);
            return;
        }

        Object target = getTarget(p, 50);
        playerPingList.add(p.getUniqueId());
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> playerPingList.remove(p.getUniqueId()),20L);

        if (target instanceof Entity) {
            handlePing(target, PingType.ENTITY, ((LivingEntity) target).getEyeLocation(),p);
        } else if (target instanceof org.bukkit.block.Block) {
            handlePing(target, PingType.BLOCK, ((Block) target).getLocation(),p);
        } else {
            p.sendActionBar(Component.text("§cNenhum bloco ou entidade encontrado num raio de 50 blocos."));
            p.playSound(p.getLocation(),Sound.BLOCK_NOTE_BLOCK_SNARE,0.5F,0.5F);
        }
    }

    private enum PingType { ENTITY, BLOCK }

    final Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(85, 255, 255), 1);

    private void handlePing(Object ping, PingType t, Location l, Player p) {
        if(pingList.contains(ping)) {
            p.sendActionBar(Component.text("§cEsta localização já foi pingada recentemente."));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_SNARE, 0.5F, 0.5F);
            return;
        }

        pingList.add(ping);

        // SOM
        l.getWorld().playSound(l, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 1F, 1F);
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 0.5F, 0.5F);

        // PARTÍCULAS TRAIL
        double distance = p.getLocation().distance(l);

        Vector p1 = p.getLocation().add(0,1,0).toVector();
        Vector p2 = l.toVector();

        if(t==PingType.BLOCK)
            p2 = l.clone().add(0.5, 0.5, 0.5).toVector();

        Vector vector = p2.clone().subtract(p1).normalize().multiply(0.2);

        double covered = 0;

        for (; covered < distance; p1.add(vector)) {
            l.getWorld().spawnParticle(Particle.DUST, p1.getX(), p1.getY(), p1.getZ(), 1, dust);
            covered += 0.2;
        }

        TextDisplay target = l.getWorld().spawn(l, TextDisplay.class);
        target.setBillboard(Display.Billboard.CENTER);
        target.setBackgroundColor(Color.AQUA);
        target.setBrightness(new Display.Brightness(8,15));

        TextDisplay player = l.getWorld().spawn(l, TextDisplay.class);
        player.setBillboard(Display.Billboard.CENTER);
        player.setBrightness(new Display.Brightness(5,8));
        player.text(Component.empty().append(Component.text("§o⚑ "+p.getName()).color(TextColor.color(224,224,224))));

        TextDisplay dist = l.getWorld().spawn(l,TextDisplay.class);
        dist.setBillboard(Display.Billboard.CENTER);
        dist.setBrightness(new Display.Brightness(8,15));

        // CASOS ESPECÍFICOS
        if(t == PingType.ENTITY) {
            LivingEntity e = (LivingEntity) ping;
            target.text(Component.empty().append(Component.translatable(e.getType().translationKey()).color(TextColor.color(NamedTextColor.DARK_GRAY))));

            e.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,5*20,1,false,false));
            team.addEntity(e);

            int[] taskId = new int[1];
            taskId[0] = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
                int counter = 0;
                @Override
                public void run() {
                    double distText = p.getLocation().distance(e.getLocation());
                    //dist.text(Component.text(distText+" m"));

                    l.getWorld().getPlayersSeeingChunk(l.getChunk()).forEach(p -> {
                        setDistanceText(p,e.getLocation(),dist);
                        drawArrow(e.getEyeLocation().clone().add(0,1,0),p);
                    });

                    dist.teleport(e.getEyeLocation().clone().add(0,1.20,0));
                    target.teleport(e.getEyeLocation().clone().add(0,0.95,0));
                    player.teleport(e.getEyeLocation().clone().add(0,0.60,0));

                    p.sendActionBar(Component.empty().append(
                            Component.text("§bAlvo: ")
                                    .append(Component.translatable(e.getType().translationKey()).color(TextColor.color(NamedTextColor.AQUA))
                                            .append(Component.text(" | Distância: "+((int) distText)+" m (X: "+((int) e.getX())+", Y: "+((int) e.getY())+", Z: "+((int) e.getZ())+")")))));

                    counter += 1;
                    if(counter >= 100) {
                        Bukkit.getScheduler().cancelTask(taskId[0]);
                        e.removePotionEffect(PotionEffectType.GLOWING);
                        team.removeEntity(e);
                        pingList.remove(ping);
                        target.remove();
                        player.remove();
                        dist.remove();
                        p.sendActionBar(Component.empty());
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
            team.addEntity(e);

            target.text(Component.empty().append(Component.translatable(b.getType().translationKey()).color(TextColor.color(NamedTextColor.DARK_GRAY))));

            dist.teleport(e.getLocation().clone().add(eLoc,1.20+0.5+yLoc,eLoc));
            target.teleport(e.getLocation().clone().add(eLoc,0.95+0.5+yLoc,eLoc));
            player.teleport(e.getLocation().clone().add(eLoc,0.60+0.5+yLoc,eLoc));

            int[] taskId = new int[1];
            taskId[0] = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
                int counter = 0;
                @Override
                public void run() {
                    double distText = p.getLocation().distance(b.getLocation());
                    //dist.text(Component.text(distText+" m"));
                    l.getWorld().getPlayersSeeingChunk(l.getChunk()).forEach(p -> setDistanceText(p,b.getLocation(),dist));

                    p.sendActionBar(Component.empty().append(
                            Component.text("§bAlvo: ")
                                    .append(Component.translatable(b.getType().translationKey()).color(TextColor.color(NamedTextColor.AQUA))
                                            .append(Component.text(" | Distância: "+((int) distText)+" m (X: "+b.getX()+", Y: "+b.getY()+", Z: "+b.getZ()+")")))));

                    counter += 1;
                    if(counter >= 100) {
                        e.remove();
                        pingList.remove(ping);
                        target.remove();
                        player.remove();
                        dist.remove();

                        Bukkit.getScheduler().cancelTask(taskId[0]);
                        p.sendActionBar(Component.empty());
                    }
                }
            },0L,1L);
        }
    }

    private void setDistanceText(Player p, Location l, TextDisplay display) {
        int dist = (int) p.getLocation().distance(l);
        String distText = String.format("%d m", dist);
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);

        packet.getIntegers().write(0, display.getEntityId());

        WrappedDataWatcher dataWatcher = new WrappedDataWatcher();

        WrappedDataWatcher.WrappedDataWatcherObject textObject = new WrappedDataWatcher.WrappedDataWatcherObject(23, WrappedDataWatcher.Registry.getChatComponentSerializer(true));

        // Convertendo o texto simples para o formato ChatComponent
        dataWatcher.setObject(textObject, Optional.of(WrappedChatComponent.fromText(distText).getHandle()));

        // Modificando o pacote para incluir o texto
        packet.getWatchableCollectionModifier().write(0, dataWatcher.getWatchableObjects());
        try {
            protocolManager.sendServerPacket(p, packet);
        } catch (Exception ignored) {}
    }

    /*private void setDistanceText(Player p, Location l, TextDisplay display) {
        // Criação do Armor Stand
        int entityId = 123; // ID único para a entidade

        PacketContainer spawnPacket = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
        spawnPacket.getIntegers().write(0, entityId); // ID da entidade
        spawnPacket.getUUIDs().write(0, UUID.randomUUID()); // UUID único
        spawnPacket.getDoubles()
                .write(0, l.getX())
                .write(1, l.getY())
                .write(2, l.getZ());
        spawnPacket.getEntityTypeModifier().write(0, EntityType.ARMOR_STAND); // Tipo de entidade

        try {
            protocolManager.sendServerPacket(p, spawnPacket);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        int dist = (int) p.getLocation().distance(l);
        String distText = String.format("%d m", dist);

        PacketContainer metadataPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        metadataPacket.getIntegers().write(0, entityId);

        WrappedDataWatcher dataWatcher = new WrappedDataWatcher();

        WrappedDataWatcher.WrappedDataWatcherObject nameObject =
                new WrappedDataWatcher.WrappedDataWatcherObject(2, WrappedDataWatcher.Registry.getChatComponentSerializer(true));
        dataWatcher.setObject(nameObject, Optional.of(WrappedChatComponent.fromText(distText).getHandle()));

        WrappedDataWatcher.WrappedDataWatcherObject showNameObject =
                new WrappedDataWatcher.WrappedDataWatcherObject(3, WrappedDataWatcher.Registry.get(Boolean.class));
        dataWatcher.setObject(showNameObject, true);

        WrappedDataWatcher.WrappedDataWatcherObject invisibleObject =
                new WrappedDataWatcher.WrappedDataWatcherObject(0, WrappedDataWatcher.Registry.get(Byte.class));
        dataWatcher.setObject(invisibleObject, (byte) 0x20);

        WrappedDataWatcher.WrappedDataWatcherObject noGravityObject =
                new WrappedDataWatcher.WrappedDataWatcherObject(5, WrappedDataWatcher.Registry.get(Boolean.class));
        dataWatcher.setObject(noGravityObject, true);

        metadataPacket.getWatchableCollectionModifier().write(0, dataWatcher.getWatchableObjects());
        try {
            protocolManager.sendServerPacket(p, metadataPacket);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }*/

    public static Object getTarget(Player player, int range) {
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection();

        for (int i = 0; i < range; i++) {
            Location checkLocation = eyeLocation.clone().add(direction.clone().multiply(i));
            Block b = checkLocation.getBlock();
            if (b.getType() != Material.AIR && b.isSolid() && !Tag.ALL_SIGNS.isTagged(b.getType()) && !b.getType().toString().contains("CHEST")
            && !Tag.BANNERS.isTagged(b.getType()) && !Tag.BEDS.isTagged(b.getType()) &&
            !b.getType().toString().equalsIgnoreCase("BAMBOO") && !b.getType().toString().equalsIgnoreCase("POINTED_DRIPSTONE")) {
                return checkLocation.getBlock();
            }

            List<Entity> nearbyEntities = (List<Entity>) checkLocation.getWorld().getNearbyEntities(checkLocation, 0.5, 0.5, 0.5);
            for (Entity entity : nearbyEntities) {
                if (!team.hasEntity(entity) && entity != player && entity instanceof LivingEntity) {
                    return entity;
                }
            }
        }

        return null;
    }

    private void drawArrow(Location location, Player p) {
        double space = 0.20;
        double defX = location.getX() - (space * arrow[0].length / 2) + (space/2);
        double x = defX;
        double y = location.clone().getY() + 3.5;
        double fire = Math.toRadians(p.getYaw());

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
