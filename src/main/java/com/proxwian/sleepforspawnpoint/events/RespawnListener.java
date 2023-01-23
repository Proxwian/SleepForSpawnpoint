package com.proxwian.sleepforspawnpoint.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent.WorldTickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
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
    
    public static HashMap<String, Pair<Level, Pair<BlockPos, BlockPos>>> playerBeds = new HashMap<>();
    public static HashMap<Level, List<Pair<Player, BlockPos>>> playersToRespawn = new HashMap<>();

    public static List<Player> playersFarRespawn = new ArrayList<>();
    
    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load e) {
	    Level world = Util.getWorldIfInstanceOfAndNotRemote(e.getWorld());
        if (world == null
            || !world.dimensionType().bedWorks()) {
            return;
        }

        playersToRespawn.put(world, new ArrayList<>());
        Util.loadBedSpawnsFromWorld(world);
    }
    
    @SubscribeEvent
    public void onWorldTick(WorldTickEvent e) {
        Level world = e.world;
        if (world.isClientSide || !world.dimensionType().bedWorks()) {
            return;
        }

        if (playersToRespawn.get(world).size() > 0) {
            Pair<Player, BlockPos> pair = playersToRespawn.get(world).get(0);
            Player player = pair.getFirst();
            BlockPos respawnpos = pair.getSecond();

            if (player instanceof ServerPlayer serverPlayer) {
                if (world.getBlockState(respawnpos).getBlock() instanceof BedBlock) {
                    ServerLevel serverworld = (ServerLevel) world;

                    Vec3 ts = new Vec3(respawnpos.getX(), respawnpos.getY() + 1.0, respawnpos.getZ());

                    serverPlayer.teleportTo(serverworld, ts.x, ts.y, ts.z, player.getYRot(), player.getXRot());
                } else {
                    String playername = player.getName().toString();
                    Util.checkForBedSpawnRemoval(world, playername, respawnpos, false);
                    playerBeds.remove(playername.toLowerCase());
                    if (SleepConfig.SERVER.sendMessageOnSpawnpointMissing.get()) {
                        Util.sendPlayerMessage(player, SleepConfig.SERVER.customMissingMsg.get());
                    }
                }
            }
            playersToRespawn.get(world).remove(0);
        }
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;

        if (!player.getLevel().dimensionType().bedWorks())
            return;

        String playerName = player.getName().getString().toLowerCase();
        if (!playerBeds.containsKey(playerName)) {
            return;
        }

        Pair<Level, Pair<BlockPos, BlockPos>> pair = playerBeds.get(playerName);

        BlockPos spawn = pair.getSecond().getFirst().immutable();

        if (player.getLevel() != pair.getFirst())
            return;

        if (player.blockPosition().distManhattan(new Vec3i(spawn.getX(), spawn.getY(), spawn.getZ())) <= SleepConfig.SERVER.bedRange.get())
            return;

        playersFarRespawn.add(player);
    }
    
    @SubscribeEvent
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        Player player = (Player) e.getEntity();
        Level world = player.getCommandSenderWorld();
        if (world.isClientSide) {
            return;
        }
		
	    String playername = player.getName().getString().toLowerCase();
        if (!playerBeds.containsKey(playername)) {
            return;
        }

        if (playersFarRespawn.contains(player)) {
            if (SleepConfig.SERVER.sendMessageOnSpawnpointFar.get()) {
                Util.sendPlayerMessage(player, SleepConfig.SERVER.customFarMsg.get());
            }
            playersFarRespawn.remove(player);
            return;
        }
		
        Pair<Level, Pair<BlockPos, BlockPos>> pair = playerBeds.get(playername);
        BlockPos spawnPos = pair.getSecond().getFirst().immutable();

        playersToRespawn.get(pair.getFirst()).add(new Pair<>(player, spawnPos));
    }
    
    @SubscribeEvent
    public void onBedBreak(BlockEvent.BreakEvent e) {
        Level world = Util.getWorldIfInstanceOfAndNotRemote(e.getWorld());
        if (world == null) {
            return;
	    }
		
        BlockPos pos = e.getPos();
        if (world.getBlockState(pos).getBlock() instanceof BedBlock) {
            Player player = e.getPlayer();
            String playerName = player.getName().getString().toLowerCase();

            if (Util.checkForBedSpawnRemoval(world, playerName, pos, true)) {
                if (SleepConfig.SERVER.sendMessageOnSpawnpointCreate.get()) {
                    Util.sendPlayerMessage(player, SleepConfig.SERVER.customUnsetMsg.get());
                }
            }
	    }
    }
}
