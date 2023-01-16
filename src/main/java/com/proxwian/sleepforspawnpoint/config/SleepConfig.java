package com.proxwian.sleepforspawnpoint.config;

import com.proxwian.sleepforspawnpoint.SleepForSpawnpoint;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.apache.commons.lang3.tuple.Pair;

public class SleepConfig {

    public static class Server {

        public final BooleanValue sendMessageOnSpawnpointCreate;
        public final BooleanValue sendMessageOnSpawnpointUnset;
        public final BooleanValue sendMessageOnSpawnpointMissing;
        public final BooleanValue sendMessageOnSpawnpointFar;
        
        public final ConfigValue<? extends String> modBehavior;

        public final IntValue bedRange;
        
        public final ConfigValue<? extends String> customCreateMsg;
        public final ConfigValue<? extends String> customUnsetMsg;
        public final ConfigValue<? extends String> customMissingMsg;
        public final ConfigValue<? extends String> customFarMsg;

        Server(ForgeConfigSpec.Builder builder)
        {
            builder.comment("SleepForSpawnPoint",
                            "Change sleep mechanic to set bed spawnpoint only when player well rested in bed")
                    .push("general");
            
            modBehavior = builder
                    .comment("WAKE (default) - sets spawn point on player wake in his bed, REST - sets spawn point in bed if player was in bed, DISABLE - disable bed spawnpoint set")
                    .define("modBehavior", "WAKE", o -> (o instanceof String));

            bedRange = builder
                    .comment("The maximum range at which a player can be respawn back to his bed.",
                            "Default: 1024")
                    .defineInRange("bedRange", 1024, 0, Integer.MAX_VALUE);

            builder.pop();
            
            builder.comment("Control custom messages for bed spawn logic")
                    .push("messages");
           
            sendMessageOnSpawnpointCreate = builder
                    .comment("Send custom message when spawn point is created")
                    .define("sendMessageOnSpawnpointCreate", true);
            
            customCreateMsg = builder
                    .define("customWakeMsg", "You sleep well and your spawnpoint has been changed.", o -> (o instanceof String));

            sendMessageOnSpawnpointUnset = builder
                    .comment("Send custom message when spawn point is unset")
                    .define("sendMessageOnSpawnpointUnset", true);
            
            customUnsetMsg = builder
                    .define("customUnsetMsg", "Your bed spawnpoint has been removed.", o -> (o instanceof String));

            sendMessageOnSpawnpointMissing = builder
                    .comment("Send custom message when spawn point is missing")
                    .define("sendMessageOnSpawnpointMissing", true);
            
            customMissingMsg = builder
                    .define("customMissingMsg", "Your bed is destroyed or unreachable.", o -> (o instanceof String));
                       
            sendMessageOnSpawnpointFar = builder
                    .comment("Send custom message when spawn point is too far from player")
                    .define("sendMessageOnSpawnpointFar", true);
            
            customFarMsg = builder
                    .define("customFarMsg", "Your bed is too far away from you.", o -> (o instanceof String));

            builder.pop();

        }
    }

    public static final ForgeConfigSpec serverSpec;
    public static final SleepConfig.Server SERVER;

    static {
        final Pair<Server, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Server::new);
        serverSpec = specPair.getRight();
        SERVER = specPair.getLeft();
    }

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent.Loading configEvent)
    {
        SleepForSpawnpoint.logger.debug("Loaded SleepForSpawnpoint's config file {}", configEvent.getConfig().getFileName());
    }

    @SubscribeEvent
    public static void onFileChange(final ModConfigEvent.Reloading configEvent)
    {
        SleepForSpawnpoint.logger.debug("SleepForSpawnpoint Chat's config just got changed on the file system!");
    }
}