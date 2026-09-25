package com.cryrius.turretcontroller;

import com.cryrius.turretcontroller.common.CommonProxy;
import com.cryrius.turretcontroller.network.ControllerMessage;
import com.cryrius.turretcontroller.tileentity.TileEntityTurretController;
import com.cryrius.turretcontroller.block.BlockTurretController;
import com.cryrius.turretcontroller.item.ItemTurretLinker;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Entry point for the standalone turret controller.
 *
 * <p>There is intentionally no reference to an HBM class in this class (or in
 * the rest of the common code).  Optional fields and methods are discovered
 * at runtime after a player links a tile entity; the adapter can also probe
 * for the optional base class with {@code Class.forName}.  As a result this
 * mod can be installed and loaded on its own.</p>
 */
@Mod(modid = TurretControllerMod.MOD_ID,
     name = "Turret Controller",
     version = TurretControllerMod.VERSION,
     acceptedMinecraftVersions = "[1.7.10]")
public final class TurretControllerMod {

    public static final String MOD_ID = "turretcontroller";
    public static final String VERSION = "1.0.0";
    public static final int GUI_CONTROLLER = 1;

    @Mod.Instance(MOD_ID)
    public static TurretControllerMod INSTANCE;

    @SidedProxy(clientSide = "com.cryrius.turretcontroller.client.ClientProxy",
                serverSide = "com.cryrius.turretcontroller.common.CommonProxy")
    public static CommonProxy proxy;

    public static SimpleNetworkWrapper NETWORK;
    public static Block CONTROLLER_BLOCK;
    public static Item LINKER_ITEM;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        CONTROLLER_BLOCK = new BlockTurretController()
                .setBlockName("turretController")
                .setBlockTextureName(MOD_ID + ":controller")
                .setCreativeTab(CreativeTabs.tabRedstone);
        GameRegistry.registerBlock(CONTROLLER_BLOCK, "turret_controller");

        LINKER_ITEM = new ItemTurretLinker()
                .setUnlocalizedName("turretLinker")
                .setTextureName(MOD_ID + ":linker")
                .setCreativeTab(CreativeTabs.tabTools);
        GameRegistry.registerItem(LINKER_ITEM, "turret_linker");
        GameRegistry.registerTileEntity(TileEntityTurretController.class, MOD_ID + ":controller");

        GameRegistry.addRecipe(new ItemStack(CONTROLLER_BLOCK),
                "IRI", "RCR", "IRI", 'I', Items.iron_ingot,
                'R', Items.redstone, 'C', Blocks.iron_block);
        GameRegistry.addRecipe(new ItemStack(LINKER_ITEM),
                " RI", "SIR", "  R", 'I', Items.iron_ingot,
                'R', Items.redstone, 'S', Items.stick);

        NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel(MOD_ID);
        NETWORK.registerMessage(ControllerMessage.Handler.class, ControllerMessage.class, 0,
                cpw.mods.fml.relauncher.Side.SERVER);

        proxy.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(this, proxy);
        proxy.init();
    }
}
