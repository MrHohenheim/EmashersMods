package emasher.sockets;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.OutputStream;

import emasher.sockets.pipes.TileDirectionChanger;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.liquids.LiquidDictionary;
import net.minecraftforge.liquids.LiquidStack;
import cpw.mods.fml.common.network.IPacketHandler;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;
import emasher.api.SideConfig;
import emasher.api.SocketTileAccess;
import emasher.sockets.pipes.TileAdapterBase;
import emasher.sockets.pipes.TilePipeBase;

public class PacketHandler implements IPacketHandler
{
	public static PacketHandler instance = new PacketHandler();
	public final static String networkChannel = "Emasher_Sockets";
	
	@Override
	public void onPacketData(INetworkManager manager, Packet250CustomPayload packet, Player player)
	{
		int dimID;
		try
		{
			if(packet.channel.equals(networkChannel))
			{
				if(packet.data[0] == 0)
				{
					int x = toInteger(packet.data, 1);
					int y = toInteger(packet.data, 5);
					int z = toInteger(packet.data, 9);
					int side = packet.data[17];
					dimID = toInteger(packet.data, 13);
					
					World world = ((EntityPlayer) player).worldObj;
					TileEntity te = world.getBlockTileEntity(x, y, z);
					if(te != null && te instanceof TileSocket)
					{
						TileSocket ts = (TileSocket)te;
						
						SendClientSideState(ts, (byte)side);
					}
				}
				else if(packet.data[0] == 1)
				{
					int x = toInteger(packet.data, 1);
					int y = toInteger(packet.data, 5);
					int z = toInteger(packet.data, 9);
					int inventory = packet.data[17];
					dimID = toInteger(packet.data, 13);
					
					World world = ((EntityPlayer) player).worldObj;
					TileEntity te = world.getBlockTileEntity(x, y, z);
					if(te != null && te instanceof TileSocket)
					{
						TileSocket ts = (TileSocket)te;
						
						SendClientInventorySlot(ts, (byte)inventory);
					}
				}
				else if(packet.data[0] == 2)
				{
					int x = toInteger(packet.data, 1);
					int y = toInteger(packet.data, 5);
					int z = toInteger(packet.data, 9);
					int tank = packet.data[17];
					dimID = toInteger(packet.data, 13);
					
					World world = ((EntityPlayer) player).worldObj;
					TileEntity te = world.getBlockTileEntity(x, y, z);
					if(te != null && te instanceof TileSocket)
					{
						TileSocket ts = (TileSocket)te;
						
						SendClientTankSlot(ts, (byte)tank);
					}
				}
				else if(packet.data[0] == 3)
				{
					int x = toInteger(packet.data, 1);
					int y = toInteger(packet.data, 5);
					int z = toInteger(packet.data, 9);
					
					World world = ((EntityPlayer) player).worldObj;
					TileEntity te = world.getBlockTileEntity(x, y, z);
					if(te != null && te instanceof TilePipeBase)
					{
						TilePipeBase p = (TilePipeBase)te;
						
						this.sendClientPipeColour(p);
					}
				}
				else if(packet.data[0] == 4)
				{
					int x = toInteger(packet.data, 1);
					int y = toInteger(packet.data, 5);
					int z = toInteger(packet.data, 9);
					
					World world = ((EntityPlayer) player).worldObj;
					TileEntity te = world.getBlockTileEntity(x, y, z);
					if(te != null && te instanceof TileAdapterBase)
					{
						TileAdapterBase t = (TileAdapterBase)te;
						
						for(int i = 0; i < 6; i++)
						{
							this.sendClientAdapterSide(t, i);
						}
					}
				}
                else if(packet.data[0] == 5)
                {
                    int x = toInteger(packet.data, 1);
                    int y = toInteger(packet.data, 5);
                    int z = toInteger(packet.data, 9);

                    World world = ((EntityPlayer) player).worldObj;
                    TileEntity te = world.getBlockTileEntity(x, y, z);
                    if(te != null && te instanceof TileDirectionChanger)
                    {
                        TileDirectionChanger t = (TileDirectionChanger)te;

                        for(int i = 0; i < 6; i++)
                        {
                            this.sendClientChangerSide(t, i);
                        }
                    }
                }
			}
		}
		catch (Exception e)
		{
			System.err.println("[Engineer's Toolbox] Network Error");
			
		}
	}
	
	public void SendClientSideState(TileSocket ts, byte side)
	{
		SideConfig c = ts.configs[side];
		
		byte[] out = new byte[36];
		
		out[0] = 0;
		out[2] = (byte)c.tank;
		out[3] = (byte)c.inventory;
		out[4] = (byte)ts.rsIndicatorIndex(side);
		out[5] = (byte)ts.latchIndicatorIndex(side);
		
		toByte(out, c.meta, 6);
		toByte(out, ts.xCoord, 10);
		toByte(out, ts.yCoord, 14);
		toByte(out, ts.zCoord, 18);
		toByte(out, ts.facID[side], 27);
		toByte(out, ts.facMeta[side], 31);
		
		out[1] = (byte)side;
		toByte(out, ts.sides[side], 22);
		out[26] = boolToByte(ts.sideLocked[side]);
		
		//PacketDispatcher.sendPacketToAllPlayers(new Packet250CustomPayload(networkChannel, out));
		//PacketDispatcher.sendPacketToAllInDimension(new Packet250CustomPayload(networkChannel, out), ts.worldObj.provider.dimensionId);
		PacketDispatcher.sendPacketToAllAround(ts.xCoord, ts.yCoord, ts.zCoord, 160, ts.worldObj.provider.dimensionId, new Packet250CustomPayload(networkChannel, out));
	}
	
	public void SendClientInventorySlot(TileSocket ts, int inventory)
	{
		ItemStack s = ts.inventory.getStackInSlot(inventory);
		byte[] NBTData = new byte[]{};
		
		try
		{
			if(s != null && s.getTagCompound() != null) NBTData = CompressedStreamTools.compress(s.getTagCompound());
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		byte[] out = new byte[27 + NBTData.length];
		
		out[0] = 1;
		
		if(s != null)
		{
			toByte(out, s.itemID, 1);
			toByte(out, s.getItemDamage(), 5);
			toByte(out, s.stackSize, 23);
		}
		else
		{
			toByte(out, -1, 1);
		}
		
		toByte(out, ts.xCoord, 9);
		toByte(out, ts.yCoord, 13);
		toByte(out, ts.zCoord, 17);
		out[22] = (byte)inventory;
		
		for(int i = 0; i < NBTData.length; i++)
		{
			out[27 + i] = NBTData[i];
		}
		
		
		//PacketDispatcher.sendPacketToAllPlayers(new Packet250CustomPayload(networkChannel, out));
		//PacketDispatcher.sendPacketToAllInDimension(new Packet250CustomPayload(networkChannel, out), ts.worldObj.provider.dimensionId);
		PacketDispatcher.sendPacketToAllAround(ts.xCoord, ts.yCoord, ts.zCoord, 160, ts.worldObj.provider.dimensionId, new Packet250CustomPayload(networkChannel, out));
	}
	
	public void SendClientTankSlot(TileSocket ts, int tank)
	{
		FluidStack l = ts.tanks[tank].getFluid();
		
		int id = -1;
		int meta = 0;
		int amnt = 0;
		
		byte[] out = new byte[27];
		out[0] = 2;
		
		if(l != null)
		{
			id = l.fluidID;
			meta = 0;
			amnt = l.amount;
		}
		
		toByte(out, ts.xCoord, 1);
		toByte(out, ts.yCoord, 5);
		toByte(out, ts.zCoord, 9);
		toByte(out, id, 13);
		toByte(out, meta, 17);
		toByte(out, amnt, 22);
		out[26] = (byte)tank;
		
		//PacketDispatcher.sendPacketToAllPlayers(new Packet250CustomPayload(networkChannel, out));
		//PacketDispatcher.sendPacketToAllInDimension(new Packet250CustomPayload(networkChannel, out), ts.worldObj.provider.dimensionId);
		PacketDispatcher.sendPacketToAllAround(ts.xCoord, ts.yCoord, ts.zCoord, 160, ts.worldObj.provider.dimensionId, new Packet250CustomPayload(networkChannel, out));
	}
	
	public void sendClientPipeColour(TilePipeBase p)
	{
		byte[] out = new byte[18];
		
		out[0] = 3;
		toByte(out, p.xCoord, 1);
		toByte(out, p.yCoord, 5);
		toByte(out, p.zCoord, 9);
		toByte(out, p.worldObj.provider.dimensionId, 13);
		out[17] = (byte)p.colour;
		
		PacketDispatcher.sendPacketToAllInDimension(new Packet250CustomPayload(networkChannel, out), p.worldObj.provider.dimensionId);
	}
	
	public void sendClientAdapterSide(TileAdapterBase t, int side)
	{
		byte[] out = new byte[19];
		
		out[0] = 4;
		toByte(out, t.xCoord, 1);
		toByte(out, t.yCoord, 5);
		toByte(out, t.zCoord, 9);
		toByte(out, t.worldObj.provider.dimensionId, 13);
		if(t.outputs[side]) out[17] = 1;
		else out[17] = 0;
		out[18] = (byte)side;
		
		PacketDispatcher.sendPacketToAllInDimension(new Packet250CustomPayload(networkChannel, out), t.worldObj.provider.dimensionId);
	}

    public void sendClientChangerSide(TileDirectionChanger t, int side)
    {
        byte[] out = new byte[19];

        out[0] = 5;
        toByte(out, t.xCoord, 1);
        toByte(out, t.yCoord, 5);
        toByte(out, t.zCoord, 9);
        toByte(out, t.worldObj.provider.dimensionId, 13);
        out[17] = (byte)t.directions[side].ordinal();
        out[18] = (byte)side;

        PacketDispatcher.sendPacketToAllInDimension(new Packet250CustomPayload(networkChannel, out), t.worldObj.provider.dimensionId);
    }
	
	private void toByte(byte[] out, int in, int start)
	{
		out[start++] = (byte) (in >> 24);
		out[start++] = (byte) (in >> 16);
		out[start++] = (byte) (in >> 8);
		out[start++] = (byte) in;
	}
	
	private static int toInteger(byte[] in, int start)
	{
		int value = 0;
		for (int i = start; i < start + 4; i++) {
			value = (value << 8) + (in[i] & 0xFF);
		}
		return value;
	}
	
	private static byte boolToByte(boolean b)
	{
		if(b) return (byte)1;
		return (byte) 0;
	}
}
