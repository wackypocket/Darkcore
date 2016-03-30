package io.darkcraft.darkcore.mod.handlers.packets;

import io.darkcraft.darkcore.mod.DarkcoreMod;
import io.darkcraft.darkcore.mod.abstracts.AbstractEntityDataStore;
import io.darkcraft.darkcore.mod.helpers.ServerHelper;
import io.darkcraft.darkcore.mod.helpers.WorldHelper;
import io.darkcraft.darkcore.mod.interfaces.IDataPacketHandler;
import io.darkcraft.darkcore.mod.network.DataPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.Type;
import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;

public class EntityDataStorePacketHandler implements IDataPacketHandler
{
	{
		FMLCommonHandler.instance().bus().register(this);
	}

	public static final String disc = "core.aeds";

	private static final Queue<AbstractEntityDataStore> queue = new ConcurrentLinkedQueue();

	public static void queueUpdate(AbstractEntityDataStore aeds)
	{
		if((aeds == null) || (aeds.getEntity() == null) || ServerHelper.isClient()) return;
		queue.add(aeds);
	}

	/**
	 * Try to send an update from the aeds
	 * @param aeds
	 * @return
	 */
	public static boolean sendUpdate(AbstractEntityDataStore aeds)
	{
		if(ServerHelper.isClient()) return false;
		EntityLivingBase ent = aeds.getEntity();
		if((ent == null) || ent.isDead) return false;
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setInteger("entID", ent.getEntityId());
		nbt.setInteger("world", WorldHelper.getWorldID(ent));
		nbt.setString("aedsID", aeds.id);
		aeds.writeTransmittable(nbt);
		DataPacket dp = new DataPacket(nbt,disc);
		if(aeds.notifyArea())
			DarkcoreMod.networkChannel.sendToAllAround(dp, new TargetPoint(WorldHelper.getWorldID(ent),ent.posX,ent.posY,ent.posZ,120));
		else if(ent instanceof EntityPlayerMP)
		{
			EntityPlayerMP pl = (EntityPlayerMP)ent;
			if((pl.playerNetServerHandler == null) || (pl.playerNetServerHandler.netManager == null)) return true;
			DarkcoreMod.networkChannel.sendTo(dp, pl);
		}
		return false;
	}

	@SubscribeEvent
	public void tickHandler(TickEvent.ServerTickEvent tick)
	{
		if((tick.phase != Phase.START) || (tick.type != Type.SERVER)) return;
		AbstractEntityDataStore aeds;
		List<AbstractEntityDataStore> toReadd = new ArrayList();
		while((aeds = queue.poll()) != null)
		{
			if(aeds.getEntity() == null) continue;
			if(sendUpdate(aeds))
				toReadd.add(aeds);
		}
		queue.addAll(toReadd);
	}

	@Override
	public void handleData(NBTTagCompound data)
	{
		try
		{
			World w = WorldHelper.getWorld(data.getInteger("world"));
			int eid = data.getInteger("entID");
			Entity e = w.getEntityByID(eid);
			String aid = data.getString("aedsID");
			if(e instanceof EntityLivingBase)
			{
				IExtendedEntityProperties ieep = e.getExtendedProperties(aid);
				if(!(ieep instanceof AbstractEntityDataStore)) return;
				((AbstractEntityDataStore)ieep).readTransmittable(data);
			}
		}
		catch(NullPointerException e){}
	}

}
