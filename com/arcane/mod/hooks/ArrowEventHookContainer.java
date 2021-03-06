package com.arcane.mod.hooks;

import java.util.List;

import net.minecraft.command.IEntitySelector;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;

import com.arcane.mod.ArcaneEnchantments;

public class ArrowEventHookContainer 
{	
	// Booleans, my boys
	boolean isExplosive = false;
	boolean isPoison = false;
	boolean isHoming = false;
	boolean isCritical = false;
	boolean isGodly = false;

	// Integers, ya idiot
	int explosiveAmount;
	int poisonAmount;
	int homingAmount;
	int criticalAmount;
	int godlyAmount;

	EntityLiving target;

	@ForgeSubscribe
	public void arrowLooseEvent(ArrowLooseEvent event)
	{
		EntityPlayer player = event.entityPlayer;
		ItemStack stack = player.inventory.getCurrentItem();

		explosiveAmount = EnchantmentHelper.getEnchantmentLevel(ArcaneEnchantments.arrowExplosive.effectId, stack);

		if(explosiveAmount > 0)
		{
			isExplosive = true;
		}

		poisonAmount = EnchantmentHelper.getEnchantmentLevel(ArcaneEnchantments.arrowPoison.effectId, stack);

		if(poisonAmount > 0)
		{
			isPoison = true;
		}

		homingAmount = EnchantmentHelper.getEnchantmentLevel(ArcaneEnchantments.arrowSeeking.effectId, stack);

		if(homingAmount > 0)
		{
			isHoming = true;
		}

		criticalAmount = EnchantmentHelper.getEnchantmentLevel(ArcaneEnchantments.arrowCritical.effectId, stack);

		if(criticalAmount > 0)
		{
			isCritical = true;
		}

		godlyAmount = EnchantmentHelper.getEnchantmentLevel(ArcaneEnchantments.arrowLightning.effectId, stack);

		if(godlyAmount > 0)
		{
			isGodly = true;
		}
	}

	@ForgeSubscribe
	public void entityAttacked(LivingAttackEvent event)
	{
		EntityLivingBase ent = event.entityLiving;

		if(event.source.isProjectile() && isExplosive == true)
		{
			EntityArrow entArrow = null;
			this.createExplosionOnEntityWithModifier(entArrow, ent.worldObj, explosiveAmount, ent);
		} else if(event.source.isProjectile() && isPoison == true)
		{
			int duration = poisonAmount * 20;
			int amplifier = poisonAmount + 8;
			float damage = poisonAmount + 4;
			ent.addPotionEffect(new PotionEffect(Potion.poison.id, duration, amplifier));
			ent.attackEntityFrom(DamageSource.generic, damage);
		} else if(event.source.isProjectile() && isHoming == true)
		{
			float damage = 6;
			ent.attackEntityFrom(DamageSource.generic, damage);
		} else if(event.source.isProjectile() && isCritical == true)
		{
			float damage = criticalAmount + 4;
			ent.attackEntityFrom(DamageSource.generic, damage);
		} else if(event.source.isProjectile() && isGodly == true)
		{
			EntityArrow entArrow = null;
			this.spawnLightningOnEntityWithModifier(entArrow, ent.worldObj, godlyAmount, ent);
		} else
		{
			return;
		}
	} 

	@ForgeSubscribe
	public void arrowInAir(EntityEvent event)
	{
		if(event.entity instanceof EntityArrow)
		{
			EntityArrow arrow = (EntityArrow) event.entity;

			// To whomever reads this, other than myself, I am terribly sorry for the mess of code below...ugh...
			if(isHoming == true)// && (arrow.shootingEntity instanceof EntityPlayer) && arrow.getDistanceToEntitySq(arrow.shootingEntity) > (float) (7 - homingAmount)) 
			{	
				if(target == null || target.velocityChanged || !target.canEntityBeSeen(arrow))
				{
					double posX = arrow.posX;
					double posY = arrow.posY;
					double posZ = arrow.posZ;
					double size = 6 * homingAmount;
					double d = -1D;
					EntityLiving entityliving = null;
					List list = arrow.worldObj.getEntitiesWithinAABB(net.minecraft.entity.EntityLiving.class, arrow.boundingBox.expand(size, size, size));

					for(int id = 0; id < list.size(); id++)
					{
						EntityLiving tempEnt = (EntityLiving) list.get(id);

						if(tempEnt == arrow.shootingEntity)
						{
							continue;
						}

						double distance = tempEnt.getDistance(posX, posY, posZ);

						if((size < 0.0D || distance < size * size) && (d == -1D || distance < d) && tempEnt.canEntityBeSeen(arrow))
						{
							d = distance;
							entityliving = tempEnt;
						}
					}

					target = entityliving;
				}

				if(target != null)
				{
					// All these fancy calculations guarantee that it will hit an entity dead on
					double dirX = target.posX - arrow.posX;
					double dirY = target.boundingBox.minY + (double) (target.height / 2.0F) - (arrow.posY + (double) (arrow.height / 2.0F));
		            double dirZ = target.posZ - arrow.posZ;
					arrow.setThrowableHeading(dirX, dirY, dirZ, 1.5F, 0.0F);
				}
			} else if(isCritical == true)
			{
				arrow.setIsCritical(true);
			} else if(isGodly == true || isExplosive == true)
			{
				//arrow = this.entArrow;
			} 
		}
	}

	@ForgeSubscribe
	// We declared the EntityLiving, but it was null, so every 
	// living update, we set it equal to the EntityLivingBase class
	public void onLivingUpdate(LivingUpdateEvent event)
	{
		EntityLivingBase living = event.entityLiving;
		living = target;
	}

	public void createExplosionOnEntityWithModifier(EntityArrow arrow, World world, int modifierAmount, Entity entity)
	{
		if((arrow.shootingEntity instanceof EntityPlayer) && arrow.getDistanceToEntity(arrow.shootingEntity) > 5F + (float) (modifierAmount * 2))
		{
			world.createExplosion(arrow, arrow.posX, arrow.posY, arrow.posZ, 2.0F * (float) modifierAmount, true);
			arrow.setDead();
		} else if(entity.getDistance(arrow.posX, arrow.posY, arrow.posZ) > (double) (5F + (float) (modifierAmount * 2)))
		{
			world.createExplosion(arrow, arrow.posX, arrow.posY, arrow.posZ, 2.0F * (float) modifierAmount, true);
			arrow.setDead();
		}
	}

	public void spawnLightningOnEntityWithModifier(EntityArrow arrow, World world, int modifierAmount, Entity entity)
	{
		if(!arrow.isInWater())
		{
			if(entity == null)
			{
				if((arrow.shootingEntity instanceof EntityPlayer) && arrow.getDistanceToEntity(arrow.shootingEntity) > 5F + (float) (modifierAmount * 2))
				{
					for(int j = 0; j < modifierAmount; j++)
					{
						int l = world.rand.nextInt(4);
						world.spawnEntityInWorld(new EntityLightningBolt(world, arrow.posX + (double)l, arrow.posY, arrow.posZ + (double)l));
					}
					if(modifierAmount > 1)
					{
						this.createExplosionOnEntityWithModifier(arrow, world, modifierAmount - 1, null);
					}
				}
			} else if(entity.getDistance(arrow.posX, arrow.posY, arrow.posZ) > (double) (5F + (float) ((modifierAmount + 1) * 2)))
			{
				for(int k = 0; k < modifierAmount; k++)
				{
					int i1 = world.rand.nextInt(4);
					world.spawnEntityInWorld(new EntityLightningBolt(world, arrow.posX + (double)i1, arrow.posY, arrow.posZ + (double)i1));
				}
				if(modifierAmount > 1)
				{
					this.createExplosionOnEntityWithModifier(arrow, world, modifierAmount - 1, entity);
				}
			}
		}
	}
}