package com.proxwian.sleepforspawnpoint.functions;

import com.mojang.datafixers.util.Pair;
import com.proxwian.sleepforspawnpoint.config.SleepConfig;
import com.proxwian.sleepforspawnpoint.events.RespawnListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.storage.LevelResource;

public class Util {
    public static String BED_SPAWN_FOLDER_PATH = File.separator + "data" + File.separator + "sleepforspawnpoint" + File.separator;
    public static String SPAWN_FILE_SUFFIX = ".bed";
    
    public static void loadBedSpawnsFromWorld(Level world) {
        String bedspawnfolder = getWorldPath((ServerLevel)world) + BED_SPAWN_FOLDER_PATH + getSimpleDimensionString(world);
        File dir = new File(bedspawnfolder);
        dir.mkdirs();

        File[] listOfFiles = dir.listFiles();
        if (listOfFiles == null) {
            return;
        }
		
        for (File listOfFile : listOfFiles) {
            String filename = listOfFile.getName();
            if (filename.endsWith(SPAWN_FILE_SUFFIX)) {
                String playername = filename.toLowerCase().replace(SPAWN_FILE_SUFFIX, "");
                
                String filecontent;
                try {
                    filecontent = new String(Files.readAllBytes(Paths.get(bedspawnfolder + File.separator + filename, new String[0])));
                } catch (IOException e) {
                    System.out.println("[Error] SleepForSpawnpoint: Something went wrong while loading a bed spawn location for player " + playername + ".");
                    continue;
                }
                
                if (sequenceCount(filecontent, "_") < 4) {
                    continue;
                }
                
                String[] cs = filecontent.trim().split("_");
                if (cs.length == 6) {
                    if (isNumeric(cs[0]) && isNumeric(cs[1]) && isNumeric(cs[2]) && isNumeric(cs[3]) && isNumeric(cs[4]) && isNumeric(cs[5])) {
                        int x = Integer.parseInt(cs[0]);
                        int y = Integer.parseInt(cs[1]);
                        int z = Integer.parseInt(cs[2]);
                        int x2 = Integer.parseInt(cs[3]);
                        int y2 = Integer.parseInt(cs[4]);
                        int z2 = Integer.parseInt(cs[5]);
                        
                        BlockPos spawnpos = new BlockPos(x, y, z);
                        BlockPos spawnpos2 = new BlockPos(x2, y2, z2);
                        RespawnListener.playerBeds.put(playername, new Pair<>(world, new Pair<>(spawnpos.immutable(), spawnpos2.immutable())));
                    }
                }
            }
        }
    }
    public static boolean setBedSpawn(Level world, String playername, BlockPos bedpos, BlockPos secondpos) {
        String bedSpawnFolder = Util.getWorldPath((ServerLevel)world) + Util.BED_SPAWN_FOLDER_PATH + Util.getSimpleDimensionString(world);
        File dir = new File(bedSpawnFolder);
        dir.mkdirs();

        String filename = playername.toLowerCase() + Util.SPAWN_FILE_SUFFIX;
        try {
            try (PrintWriter writer = new PrintWriter(bedSpawnFolder + File.separator + filename, "UTF-8")) {
                writer.println(bedpos.getX() + "_" + bedpos.getY() + "_" + bedpos.getZ() + "_" + secondpos.getX() + "_" + secondpos.getY() + "_" + secondpos.getZ());
                writer.println();
            }
        } catch (Exception e) {
            System.out.println("[Error] SleepForSpawnpoint: Something went wrong while saving a bed spawn location for player " + playername + ".");
            return false;
        }

        RespawnListener.playerBeds.put(playername.toLowerCase(), new Pair<>(world, new Pair<>(bedpos.immutable(), secondpos.immutable())));
        return true;
    }
    
    public static boolean checkForBedSpawnRemoval(Level world, String playername, BlockPos bedpos, Boolean message) {
        List<String> fromplayernames = new ArrayList<>();
        for (String pname : RespawnListener.playerBeds.keySet()) {
            Pair<Level, Pair<BlockPos, BlockPos>> pair = RespawnListener.playerBeds.get(pname);
            if (bedpos.equals(pair.getSecond().getFirst()) || bedpos.equals(pair.getSecond().getSecond())) {
                fromplayernames.add(pname);
            }
	    }
		
        boolean removed = false;
        for (String fromplayername : fromplayernames) {
            if (RespawnListener.playerBeds.containsKey(fromplayername)) {
                for (Level loopworld : RespawnListener.playersToRespawn.keySet()) {
                    String bedspawnfolder = getWorldPath((ServerLevel)loopworld) + BED_SPAWN_FOLDER_PATH + getSimpleDimensionString(loopworld);
                    File dir = new File(bedspawnfolder);
                    dir.mkdirs();

                    String filename = fromplayername.toLowerCase() + SPAWN_FILE_SUFFIX;
                    try {
                        try (PrintWriter writer = new PrintWriter(bedspawnfolder + File.separator + filename, "UTF-8")) {
                            writer.print("");
                        }
                    } catch (Exception e) {
                        System.out.println("[Error] SleepForSpawnpoint: Something went wrong while removing a bed spawn location for player " + playername + ".");
                    }
                }
				
                RespawnListener.playerBeds.remove(fromplayername);
                removed = true;

                if (message && SleepConfig.SERVER.sendMessageOnSpawnpointUnset.get()) {
                    if (fromplayername.equalsIgnoreCase(playername)) {
                        continue;
                    }

                    Player target = matchPlayer(world, fromplayername);
                    if (target == null) {
                        continue;
                    }

                    sendPlayerMessage(target, SleepConfig.SERVER.customUnsetMsg.get());
                }
            }
	    }
        return removed && fromplayernames.contains(playername.toLowerCase());
    }
    
    public static String getSimpleDimensionString(Level world) {
        String dimensionfolder = getWorldDimensionName(world).toLowerCase();
        if (dimensionfolder.contains(":")) {
                dimensionfolder = dimensionfolder.split(":")[1];
        }
        return dimensionfolder;
    }
    
    public static String getWorldDimensionName(Level world) {
	return world.dimension().location().toString();
    }
    
    public static String getWorldPath(ServerLevel ServerLevel) {
        String worldpath = ServerLevel.getServer().getWorldPath(LevelResource.ROOT).toString();
        return worldpath.substring(0, worldpath.length() - 2);
    }
    
    public static void sendPlayerMessage(Player player, String message) {
        player.displayClientMessage(new TextComponent(message), false);
    }
    
    public static Player matchPlayer(Level world, String other) {
        List<? extends Player> players = world.players();

        for (Player onlineplayer : players) {
                if (onlineplayer.getName().getString().toLowerCase().equals(other)) {
            return onlineplayer;
                }
        }
        return null;
    }
    
    // IWorld functions
    public static Level getWorldIfInstanceOfAndNotRemote(LevelAccessor iworld) {
        if (iworld.isClientSide()) {
                return null;
        }
        if (iworld instanceof Level level) {
                return level;
        }
        return null;
    }
    
    public static boolean isNumeric(String string) {
        if (string == null) {
            return false;
	    }
        try {
            Double.parseDouble(string);
        } catch (NumberFormatException ex) {
            return false;
        }
        return true;
    }
    
    public static int sequenceCount(String text, String sequence) {
        Pattern pattern = Pattern.compile(sequence);
        Matcher matcher = pattern.matcher(text);

        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
    
}
