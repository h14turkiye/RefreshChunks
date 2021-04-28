package com.vanillage.refreshchunks;

import java.util.Locale;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.destroystokyo.paper.util.misc.PooledLinkedHashSets.PooledObjectLinkedOpenHashSet;

import net.minecraft.server.v1_16_R3.Chunk;
import net.minecraft.server.v1_16_R3.ChunkCoordIntPair;
import net.minecraft.server.v1_16_R3.EntityPlayer;
import net.minecraft.server.v1_16_R3.Packet;
import net.minecraft.server.v1_16_R3.PlayerChunk;
import net.minecraft.server.v1_16_R3.PlayerChunkMap;

public final class RefreshChunks extends JavaPlugin {
    @Override
    public void onEnable() {
        getLogger().info(getDescription().getFullName() + " enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info(getDescription().getFullName() + " disabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().toLowerCase(Locale.ROOT).equals("refreshchunks")) {
            if (args.length == 0) {
                if (sender instanceof Player) {
                    refreshChunks((Player) sender);
                    sender.sendMessage("Chunks refreshed.");
                } else {
                    sender.sendMessage(ChatColor.RED + "This command can only be executed by a player.");
                }

                return true;
            } else if (args.length == 1) {
                if (sender.hasPermission("refreshchunks.command.refreshchunk.player")) {
                    Player player = getServer().getPlayerExact(args[0]);

                    if (player == null) {
                        sender.sendMessage(ChatColor.RED + "The player " + args[0] + " is not online.");
                    } else {
                        refreshChunks(player);
                        sender.sendMessage("Chunks refreshed for " + player.getName() + ".");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "You don't have permissions.");
                }

                return true;
            }
        }

        return false;
    }

    public void refreshChunks(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("player cannot be null");
        }

        Location location = player.getLocation();
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        int viewDistance = player.getWorld().getViewDistance() + 1;
        int chunkXMin = chunkX - viewDistance;
        int chunkXMax = chunkX + viewDistance;
        int chunkZMin = chunkZ - viewDistance;
        int chunkZMax = chunkZ + viewDistance;
        PlayerChunkMap playerChunkMap = ((CraftWorld) player.getWorld()).getHandle().getChunkProvider().playerChunkMap;

        for (int x = chunkXMin; x <= chunkXMax; x++) {
            for (int z = chunkZMin; z <= chunkZMax; z++) {
                PooledObjectLinkedOpenHashSet<EntityPlayer> players = playerChunkMap.playerViewDistanceBroadcastMap.getObjectsInRange(x, z);

                if (players != null && players.contains(((CraftPlayer) player).getHandle())) {
                    PlayerChunk playerChunk = playerChunkMap.getVisibleChunk(new ChunkCoordIntPair(x, z).pair());

                    if (playerChunk != null) {
                        Chunk chunk = playerChunk.getSendingChunk();

                        if (chunk != null) {
                            playerChunkMap.sendChunk(((CraftPlayer) player).getHandle(), new Packet<?>[2], chunk);
                        }
                    }
                }
            }
        }
    }
}
