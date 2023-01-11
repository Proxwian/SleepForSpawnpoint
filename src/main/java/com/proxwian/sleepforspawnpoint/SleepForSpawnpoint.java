package com.proxwian.sleepforspawnpoint;

import com.proxwian.sleepforspawnpoint.config.SleepConfig;
import com.proxwian.sleepforspawnpoint.events.RespawnListener;
import com.proxwian.sleepforspawnpoint.events.SleepListener;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.IExtensionPoint.DisplayTest;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(SleepForSpawnpoint.MODID)
public class SleepForSpawnpoint {
    public static final String MODID = "sleepforspawnpoint";
    public static final Logger logger = LogManager.getLogger(SleepForSpawnpoint.MODID);

    public SleepForSpawnpoint() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SleepConfig.serverSpec);
        eventBus.register(SleepConfig.class);

        MinecraftForge.EVENT_BUS.register(new SleepListener());
            MinecraftForge.EVENT_BUS.register(new RespawnListener());

        //Make sure the client does not complain if it's not installed
        ModLoadingContext.get().registerExtensionPoint(DisplayTest.class,()->
                new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY,
            (s,b)->true));
    }

}