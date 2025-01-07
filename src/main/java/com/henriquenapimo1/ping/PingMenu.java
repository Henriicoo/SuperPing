package com.henriquenapimo1.ping;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;


import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class PingMenu {

    public static Inventory getPingMenu() {
        Inventory i = Bukkit.createInventory(null,9,Component.text("Selecione o tipo de ping"));

        ItemStack nada = getItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE,Component.text("§7 "));
        for (int a = 0; a < i.getSize(); a++)
            i.setItem(a,nada);

        i.setItem(2,getItem(Material.RED_STAINED_GLASS_PANE,Component.text("PERIGO").decorate(TextDecoration.BOLD).color(NamedTextColor.RED),Component.text("§7Clique para pingar como §c§lPERIGO")));
        i.setItem(3,getItem(Material.YELLOW_STAINED_GLASS_PANE,Component.text("ALERTA").decorate(TextDecoration.BOLD).color(NamedTextColor.YELLOW),Component.text("§7Clique para pingar como §e§lALERTA")));
        i.setItem(4,getItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE,Component.text("PADRÃO").decorate(TextDecoration.BOLD).color(NamedTextColor.AQUA),Component.text("§7Clique para pingar como §b§lPADRÃO")));
        i.setItem(5,getItem(Material.LIME_STAINED_GLASS_PANE,Component.text("SEGURO").decorate(TextDecoration.BOLD).color(NamedTextColor.GREEN),Component.text("§7Clique para pingar como §a§lSEGURO")));
        i.setItem(6,getItem(Material.PURPLE_STAINED_GLASS_PANE,Component.text("ESPECIAL").decorate(TextDecoration.BOLD).color(NamedTextColor.DARK_PURPLE),Component.text("§7Clique para pingar como §5§lESPECIAL")));

        return i;
    }

    private static ItemStack getItem(Material material, Component nome, Component... lore) {
        ItemStack i = new ItemStack(material);
        ItemMeta m = i.getItemMeta();

        assert m != null;

        m.displayName(nome);

        if(lore != null)
            m.lore(Arrays.asList(lore));

        i.setItemMeta(m);

        return i;
    }

    private Scoreboard board;

    public Map<Integer, Team> getColorTeams(Scoreboard board) {
        this.board = board;

        Map<Integer, Team> teams = new HashMap<>();
        teams.put(0,createTeam("Red",0));
        teams.put(1,createTeam("Yell",1));
        teams.put(2,createTeam("Blue",2));
        teams.put(3,createTeam("Green",3));
        teams.put(4,createTeam("Purp",4));
        return teams;
    }

    private Team createTeam(String nome, int i) {
        Team team = board.getTeam("PingColor"+nome);

        if (team == null)
            team = board.registerNewTeam("PingColor"+nome);

        team.color(getChatColor(i));

        return team;
    }


    public enum PingType {
        PERIGO(0),
        ALERTA(1),
        PADRAO(2),
        SEGURO(3),
        ESPECIAL(4);

        private final int value;

        PingType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static PingType getByValue(int value) {
            for (PingType e : PingType.values()) {
                if (e.value == value) {
                    return e;
                }
            }
            throw new IllegalArgumentException("Não existe o ping com o valor: " + value);
        }
    }

    private static final Map<Integer, Particle.DustOptions> particleColorMap = Map.of(
            0, new Particle.DustOptions(Color.fromRGB(255, 85, 85), 0.6F),
            1, new Particle.DustOptions(Color.fromRGB(255, 255, 85), 0.6F),
            2, new Particle.DustOptions(Color.fromRGB(85, 255, 255), 0.6F),
            3, new Particle.DustOptions(Color.fromRGB(85, 255, 85), 0.6F),
            4, new Particle.DustOptions(Color.fromRGB(170, 0, 170), 0.6F)
    );

    public static Particle.DustOptions getParticleColor(int i) {
        return particleColorMap.get(i);
    }

    private static final boolean o = false;
    private static final boolean x = true;

    private static final Map<Integer, boolean[][]> shapeMap = Map.of(
            0, new boolean[][]{
                    {o, o, o, o, x, x, x, x, x, x, x, x, o, o, o, o},
                    { o, o, o, x, x, x, x, x, x, x, x, x, x, o, o, o},
                    { o, x, x, x, x, x, x, x, x, x, x, x, x, x, x, o},
                    { o, x, x, x, x, x, x, x, x, x, x, x, x, x, x, o},
                    { o, x, x, x, o, o, o, x, x, o, o, o, x, x, x, o},
                    { x, x, x, x, o, o, o, x, x, o, o, o, x, x, x, x},
                    { x, x, o, o, o, o, o, x, x, o, o, o, o, o, x, x},
                    { x, x, o, o, o, x, x, x, x, x, x, o, o, o, x, x},
                    { x, x, o, o, o, x, x, x, x, x, x, o, o, o, x, x},
                    { o, x, x, x, x, x, x, x, x, x, x, x, x, x, x, o},
                    { o, x, x, x, x, x, o, o, o, o, x, x, x, x, x, o},
                    { o, x, x, x, x, x, x, o, o, x, x, x, x, x, x, o},
                    { o, o, o, x, x, x, x, x, x, x, x, x, x, o, o, o},
                    { o, o, o, x, x, o, o, x, x, o, o, x, x, o, o, o},
                    { o, o, o, x, x, o, o, x, x, o, o, x, x, o, o, o}},
            1, new boolean[][]{
                    { o, o, o, o, o, o, o, x, x, o, o, o, o, o, o, o},
                    { o, o, o, o, o, o, o, x, x, o, o, o, o, o, o, o},
                    { o, o, o, o, o, o, x, x, x, x, o, o, o, o, o, o},
                    { o, o, o, o, o, o, x, x, x, x, o, o, o, o, o, o},
                    { o, o, o, o, o, x, x, o, o, x, x, o, o, o, o, o},
                    { o, o, o, o, o, x, x, o, o, x, x, o, o, o, o, o},
                    { o, o, o, o, x, x, o, x, x, o, x, x, o, o, o, o},
                    { o, o, o, o, x, x, o, x, x, o, x, x, o, o, o, o},
                    { o, o, o, x, x, x, o, x, x, o, x, x, x, o, o, o},
                    { o, o, o, x, x, o, o, x, x, o, o, x, x, o, o, o},
                    { o, o, o, x, x, o, o, x, x, o, o, x, x, o, o, o},
                    { o, o, x, x, x, o, o, o, o, o, o, x, x, x, o, o},
                    { o, o, x, x, o, o, o, x, x, o, o, o, x, x, o, o},
                    { o, x, x, o, o, o, o, o, o, o, o, o, o, x, x, o},
                    { o, x, x, x, x, x, x, x, x, x, x, x, x, x, x, o}},
            2, new boolean[][]{
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
                    { o, o, o, o, o, o, o, x, x, o, o, o, o, o, o, o}},
            3, new boolean[][]{
                    { o, x, x, o, o, o, o, o, o, o, o, o, x, x, x, o},
                    { o, x, x, x, x, x, x, o, o, o, o, x, x, x, x, o},
                    { o, x, x, x, x, x, x, x, o, o, x, x, x, x, x, o},
                    { o, x, x, x, x, x, x, x, x, x, x, x, x, x, o, o},
                    { o, x, x, x, x, x, x, x, x, x, x, x, x, x, o, o},
                    { o, x, x, x, x, x, x, x, x, x, x, x, x, x, x, o},
                    { o, x, x, x, x, x, x, x, x, x, x, x, x, x, x, o},
                    { o, x, x, x, x, x, x, x, x, x, x, x, x, x, o, o},
                    { o, x, x, o, o, x, x, x, x, x, x, x, o, o, o, o},
                    { o, x, x, o, o, o, o, x, x, x, o, o, o, o, o, o},
                    { o, x, x, o, o, o, o, o, o, o, o, o, o, o, o, o},
                    { o, x, x, o, o, o, o, o, o, o, o, o, o, o, o, o},
                    { o, x, x, o, o, o, o, o, o, o, o, o, o, o, o, o},
                    { o, x, x, o, o, o, o, o, o, o, o, o, o, o, o, o},
                    { o, x, x, o, o, o, o, o, o, o, o, o, o, o, o, o}},
            4, new boolean[][]{
                    { o, o, o, o, o, o, o, x, x, o, o, o, o, o, o, o},
                    { o, o, o, o, o, o, x, x, x, x, o, o, o, o, o, o},
                    { o, o, o, o, o, o, x, x, x, x, o, o, o, o, o, o},
                    { o, o, o, o, o, o, x, x, x, x, o, o, o, o, o, o},
                    { o, o, o, o, o, x, x, o, o, x, x, o, o, o, o, o},
                    { o, o, o, o, o, x, x, o, o, x, x, o, o, o, o, o},
                    { x, x, x, x, x, x, o, o, o, o, x, x, x, x, x, x},
                    { x, x, x, x, o, o, o, o, o, o, o, o, x, x, x, x},
                    { o, x, x, x, x, o, o, o, o, o, o, x, x, x, x, o},
                    { o, o, o, x, x, o, o, x, x, o, o, x, x, o, o, o},
                    { o, o, o, o, x, o, x, x, x, x, o, x, o, o, o, o},
                    { o, o, o, x, x, x, x, x, x, x, x, x, x, o, o, o},
                    { o, o, x, x, x, x, x, o, o, x, x, x, x, x, o, o},
                    { o, o, x, x, x, x, o, o, o, o, x, x, x, x, o, o},
                    { o, o, x, x, o, o, o, o, o, o, o, o, x, x, o, o}}
    );

    public static boolean[][] getShape(int i) {
        return shapeMap.get(i);
    }

    public static NamedTextColor getChatColor(int i) {
        return switch (i) {
            case 0 -> NamedTextColor.RED;
            case 1 -> NamedTextColor.YELLOW;
            case 2 -> NamedTextColor.AQUA;
            case 3 -> NamedTextColor.GREEN;
            case 4 -> NamedTextColor.DARK_PURPLE;
            default -> NamedTextColor.WHITE;
        };
    }

}
