package com.proxwian.sleepforspawnpoint.events;

import com.mojang.datafixers.util.Pair;
import com.proxwian.sleepforspawnpoint.config.SleepConfig;
import com.proxwian.sleepforspawnpoint.config.Values;
import com.proxwian.sleepforspawnpoint.functions.Util;
import java.io.File;
import java.io.PrintWriter;
import java.util.Objects;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerSetSpawnEvent;
import net.minecraftforge.event.entity.player.SleepingTimeCheckEvent;
import net.minecraftforge.event.world.SleepFinishedTimeEvent;

public class SleepListener {
    
    public static final String MOD_ID = "sleepforspawnpoint";

    @SubscribeEvent
    public void onPlayerSetSpawn(final PlayerSetSpawnEvent event) {
        Player p = (Player) event.getEntity();
        if (!canSetSpawn(p, event.getNewSpawn())) {
            event.setCanceled(true);
        }
    }
    
    @SubscribeEvent
    public void onSleepingCheckEvent(SleepingTimeCheckEvent event) {
        if (!SleepConfig.SERVER.modBehavior.get().equals(Values.Modes.REST) 
                || event.getPlayer().getSleepTimer() != 2) return;
        
        ServerPlayer p = (ServerPlayer) event.getPlayer();
        this.setPlayerRespawnPosition(p);
        this.sendPlayerWakeMessage(p);
    }
	
    @SubscribeEvent
    public void wakeUpSetRespawn(SleepFinishedTimeEvent event) {
        if (!SleepConfig.SERVER.modBehavior.get().equals(Values.Modes.WAKE))
            return;
        
        event.getWorld().players().stream().filter(LivingEntity::isSleeping).toList().forEach((player) -> {
            ServerPlayer p = (ServerPlayer) player;
            this.setPlayerRespawnPosition(p);
            this.sendPlayerWakeMessage(p);
        });
    }
    
    private void sendPlayerWakeMessage(Player player) {
        Util.sendPlayerMessage(player, SleepConfig.SERVER.customCreateMsg.get());
    }
    
    private void setPlayerRespawnPosition(ServerPlayer player) {
        if (!player.getLevel().dimensionType().bedWorks())
            return;

        BlockPos secondBlockPos = player.blockPosition();
        switch (Objects.requireNonNull(player.getBedOrientation())) {
            case NORTH -> secondBlockPos = player.blockPosition().offset(0,0,1);
            case WEST -> secondBlockPos = player.blockPosition().offset(1,0,0);
            case EAST -> secondBlockPos = player.blockPosition().offset(-1,0,0);
            case SOUTH -> secondBlockPos = player.blockPosition().offset(0,0,-1);
        }
        Util.setBedSpawn(player.level, player.getName().getString(), player.blockPosition(), secondBlockPos);
        player.setRespawnPosition(player.level.dimension(), player.blockPosition(), player.getYRot(), true, false);
    }
    
    public static boolean canSetSpawn(Player player, BlockPos pos) {
        final Level level = player.getLevel();

        if (pos != null && !player.getLevel().isClientSide) {
            final Block block = level.getBlockState(pos).getBlock();
            return !(block instanceof BedBlock);
        }
        return true;
    }

}