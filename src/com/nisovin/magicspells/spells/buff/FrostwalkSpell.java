package com.nisovin.magicspells.spells.buff;

import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.BlockPlatform;
import com.nisovin.magicspells.util.MagicConfig;

public class FrostwalkSpell extends BuffSpell {
	
	private int size;
	private boolean leaveFrozen;
	
	private HashMap<String,BlockPlatform> frostwalkers;

	public FrostwalkSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		size = config.getInt("spells." + spellName + ".size", 2);
		leaveFrozen = config.getBoolean("spells." + spellName + ".leave-frozen", false);
		
		frostwalkers = new HashMap<String,BlockPlatform>();
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (frostwalkers.containsKey(player.getName())) {
			turnOff(player);
			return PostCastAction.ALREADY_HANDLED;
		} else if (state == SpellCastState.NORMAL) {
			frostwalkers.put(player.getName(), new BlockPlatform(Material.ICE, Material.STATIONARY_WATER, player.getLocation().getBlock().getRelative(0,-1,0), size, !leaveFrozen, "square"));
			startSpellDuration(player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerMove(PlayerMoveEvent event) {
		if (frostwalkers.containsKey(event.getPlayer().getName())) {
			Player player = event.getPlayer();
			if (isExpired(player)) {
				turnOff(player);
			} else {
				Block block;
				boolean teleportUp = false;
				if (event.getTo().getY() > event.getFrom().getY() && event.getTo().getY() % 1 > .62 && event.getTo().getBlock().getType() == Material.STATIONARY_WATER && event.getTo().getBlock().getRelative(0,1,0).getType() == Material.AIR) {
					block = event.getTo().getBlock();
					teleportUp = true;
				} else {
					block = event.getTo().getBlock().getRelative(0,-1,0);
				}
				boolean moved = frostwalkers.get(player.getName()).movePlatform(block);
				if (moved) {
					addUse(player);
					chargeUseCost(player);
					if (teleportUp) {
						Location loc = player.getLocation().clone();
						loc.setY(event.getTo().getBlockY()+1);
						player.teleport(loc);
					}
				}
			}
		}
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.isCancelled()) return;
		if (frostwalkers.size() > 0 && event.getBlock().getType() == Material.ICE) {
			for (BlockPlatform platform : frostwalkers.values()) {
				if (platform.blockInPlatform(event.getBlock())) {
					event.setCancelled(true);
					break;
				}
			}
		}
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {
		turnOff(event.getPlayer());
	}
	
	@Override
	public void turnOff(Player player) {
		BlockPlatform platform = frostwalkers.get(player.getName());
		if (platform != null) {
			super.turnOff(player);
			platform.destroyPlatform();
			frostwalkers.remove(player.getName());
			sendMessage(player, strFade);
		}
	}
	
	@Override
	protected void turnOff() {
		for (BlockPlatform platform : frostwalkers.values()) {
			platform.destroyPlatform();
		}
		frostwalkers.clear();
	}

}
