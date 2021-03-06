package emasher.sockets.modules;

import java.util.List;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import emasher.api.SideConfig;
import emasher.api.SocketModule;
import emasher.api.SocketTileAccess;
import emasher.sockets.SocketsMod;

public class ModFluidOutput extends SocketModule
{

	public ModFluidOutput(int id)
	{
		super(id, "sockets:fluidOutput");
	}

	@Override
	public String getLocalizedName()
	{
		return "Fluid Output";
	}
	
	@Override
	public void getToolTip(List l)
	{
		l.add("Outputs fluid from its configured");
		l.add("tank to adjacent pipes/tanks/etc.");
	}
	
	@Override
	public void getIndicatorKey(List l)
	{
		l.add(SocketsMod.PREF_BLUE + "Tank to output from");
		l.add(SocketsMod.PREF_RED + "RS control circuit");
		l.add(SocketsMod.PREF_DARK_PURPLE + "RS control latch");
	}
	
	@Override
	public void addRecipe()
	{
		GameRegistry.addShapedRecipe(new ItemStack(SocketsMod.module, 1, moduleID), "d", "u", "b", Character.valueOf('i'), Item.ingotIron, Character.valueOf('d'), Block.fenceIron,
				Character.valueOf('u'), Block.dropper, Character.valueOf('b'), SocketsMod.blankSide);
	}
	
	@Override
	public void updateSide(SideConfig config, SocketTileAccess ts, ForgeDirection side)
	{
		if(config.tank < 0 || config.tank > 2) return;
		
		boolean allOff = true;
		
		for(int i = 0; i < 3; i++)
		{
			if(config.rsControl[i])
			{
				if(ts.getRSControl(i))
				{
					ts.tryInsertFluid(config.tank, side);
					return;
				}
				allOff = false;
			}
			
			if(config.rsLatch[i])
			{
				if(ts.getRSLatch(i))
				{
					ts.tryInsertFluid(config.tank, side);
					return;
				}
				allOff = false;
			}
		}
		
		if(allOff) ts.tryInsertFluid(config.tank, side);
	}
	
	@Override
	public boolean hasTankIndicator() { return true; }
	
	@Override
	public boolean hasRSIndicator() { return true; }
	
	@Override
	public boolean hasLatchIndicator() { return true; }
	
	@Override
	public boolean isFluidInterface() { return true; }
	
	@Override
	public boolean canExtractFluid() { return true; }
	
	/*@Override
	public ILiquidTank getAssociatedTank(SideConfig config, SocketTileAccess ts)
	{
		if(config.tank == -1) return null;
		return ts.tanks[config.tank];
	}*/
	
	@Override
	public FluidStack fluidExtract(int amount, boolean doExtract, SideConfig config, SocketTileAccess ts)
	{
		if(config.tank != -1) return ts.drainInternal(config.tank, amount, doExtract);
		return null;
	}
}
