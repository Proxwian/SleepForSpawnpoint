package com.proxwian.sleepforspawnpoint.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent.WorldTickEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import com.mojang.datafixers.util.Pair;
import com.proxwian.sleepforspawnpoint.config.SleepConfig;
import com.proxwian.sleepforspawnpoint.functions.Util;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.event.world.BlockEvent;

public class RespawnListener {
    
    public static HashMap<String, Pair<Level, Pair<BlockPos, BlockPos>>> playerbeds = new HashMap<>();
    public static HashMap<Level, List<Pair<Player, BlockPos>>> playerstorespawn = new HashMap<>();
    
    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load e) {
	    Level world = Util.getWorldIfInstanceOfAndNotRemote(e.getWorld());
        if (world == null
            || !world.dimensionType().bedWorks()) {
            return;
        }

        playerstorespawn.put(world, new ArrayList<>());
        Util.loadBedSpawnsFromWorld(world);
    }
    
    @SubscribeEvent
    public void onWorldTick(WorldTickEvent e) {
        Level world = e.world;
        if (world.isClientSide || !world.dimensionType().bedWorks()) {
            return;
        }

        if (playerstorespawn.get(world).size() > 0) {
            Pair<Player, BlockPos> pair = playerstorespawn.get(world).get(0);
            Player player = pair.getFirst();
            BlockPos respawnpos = pair.getSecond();

            if (player instanceof ServerPlayer serverPlayer) {
                if (world.getBlockState(respawnpos).getBlock() instanceof BedBlock) {
                    ServerPlayer serverplayer = serverPlayer;
                    ServerLevel serverworld = (ServerLevel) world;

                    Vec3 ts = new Vec3(respawnpos.getX() + 1.5, respawnpos.getY(), respawnpos.getZ() + 0.5);

                    serverplayer.teleportTo(serverworld, ts.x, ts.y, ts.z, player.getYRot(), player.getXRot());
                } else {
                    String playername = player.getName().toString();
                    playerbeds.remove(playername.toLowerCase());
                    if (SleepConfig.SERVER.sendMessageOnSpawnpointMissing.get()) {
                        Util.sendPlayerMessage(player, SleepConfig.SERVER.customMissingMsg.get());
                    }
                }
            }
            playerstorespawn.get(world).remove(0);
        }
    }
    
    @SubscribeEvent
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        Player player = (Player) e.getEntity();
        Level world = player.getCommandSenderWorld();
        if (world.isClientSide) {
            return;
        }
		
	    String playername = player.getName().getString().toLowerCase();
        if (!playerbeds.containsKey(playername)) {
                return;
        }
		
        Pair<Level, Pair<BlockPos, BlockPos>> pair = playerbeds.get(playername);
        playerstorespawn.get(pair.getFirst()).add(new Pair<>(player, pair.getSecond().getFirst().immutable()));
    }
    
    @SubscribeEvent
    public void onCampfireBreak(BlockEvent.BreakEvent e) {
        Level world = Util.getWorldIfInstanceOfAndNotRemote(e.getWorld());
        if (world == null) {
                return;
	    }
		
        BlockPos pos = e.getPos();
        if (world.getBlockState(pos).getBlock() instanceof BedBlock) {
            Player player = e.getPlayer();
            String playername = player.getName().getString().toLowerCase();

            if (Util.checkForBedSpawnRemoval(world, playername, pos)) {
                if (SleepConfig.SERVER.sendMessageOnSpawnpointCreate.get()) {
                    Util.sendPlayerMessage(player, SleepConfig.SERVER.customUnsetMsg.get());
                }
            }
	}
    }
}
