package top.codfish.projecteteam;

import java.io.File;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.common.MinecraftForge;
import top.codfish.projecteteam.network.PacketHandler;
import top.codfish.projecteteam.proxies.IProxy;

@Mod(modid = ProjectETeam.MODID, name = ProjectETeam.MODNAME, version = ProjectETeam.VERSION, dependencies = "required-after:ProjectE")
public class ProjectETeam
{
    public static final String MODID = "ProjectETeam";
    public static final String MODNAME = "ProjectE Team";
    public static final String VERSION = "1.1";

    @Instance(MODID)
    public static ProjectETeam instance;

    @SidedProxy(clientSide = "top.codfish.projecteteam.proxies.ClientProxy", serverSide = "top.codfish.projecteteam.proxies.ServerProxy")
    public static IProxy proxy;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        Config.init(new File(event.getModConfigurationDirectory(), "ProjectETeam.cfg"));
        PacketHandler.registerCommon();
        proxy.registerClientPackets();
        proxy.registerClientOnlyEvents();
        TeamManager eventHandler = new TeamManager();
        MinecraftForge.EVENT_BUS.register(eventHandler);
        FMLCommonHandler.instance().bus().register(eventHandler);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {}

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {}

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event)
    {
        File dir = new File(event.getServer().getEntityWorld().getSaveHandler().getWorldDirectory(), "ProjectETeam");
        if (!dir.exists())
        {
            dir.mkdirs();
        }
        TeamManager.setSaveDir(dir);
        TeamManager.loadAll();
        event.registerServerCommand(new CommandProjectETeam());
    }

    @EventHandler
    public void serverStopping(FMLServerStoppingEvent event)
    {
        TeamManager.saveAll();
    }

    @EventHandler
    public void serverStopped(FMLServerStoppedEvent event)
    {
        TeamManager.clearAll();
    }
}
