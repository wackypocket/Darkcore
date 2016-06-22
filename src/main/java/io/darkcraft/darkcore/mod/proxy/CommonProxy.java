package io.darkcraft.darkcore.mod.proxy;

import io.darkcraft.darkcore.mod.abstracts.AbstractBlock;
import io.darkcraft.darkcore.mod.abstracts.AbstractItem;
import io.darkcraft.darkcore.mod.helpers.WorldHelper;
import net.minecraft.world.World;

public class CommonProxy
{
	public void init()
	{

	}

	public World getWorld(int id)
	{
		return WorldHelper.getWorldServer(id);
	}

	public void postInit()
	{
	}

	public void register(AbstractBlock b){}

	public void register(AbstractItem i){}
}
