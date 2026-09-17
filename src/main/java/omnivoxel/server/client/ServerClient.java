package omnivoxel.server.client;

import io.netty.channel.ChannelHandlerContext;
import omnivoxel.client.game.hitbox.Hitbox;
import omnivoxel.server.client.block.ServerBlockAndPosition;
import omnivoxel.server.entity.Entity;
import omnivoxel.server.entity.EntityType;
import omnivoxel.server.entity.mob.MobEntity;
import omnivoxel.server.entity.mob.PlayerEntity;
import omnivoxel.util.bytes.ByteUtils;

import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;

public class ServerClient {
    public final Set<String> registeredIDs;
    private final String clientID;
    private final ChannelHandlerContext ctx;
    private final byte[] playerID;
    private final Queue<ServerBlockAndPosition> replacedBlocks = new ArrayDeque<>();
    private final PlayerEntity playerEntity;
    private final Consumer<ServerClient> disconnect;

    public ServerClient(String clientID, ChannelHandlerContext ctx, PlayerEntity playerEntity, Consumer<ServerClient> disconnect) {
        this.clientID = clientID;
        this.ctx = ctx;
        this.playerEntity = playerEntity;
        this.disconnect = disconnect;
        playerID = new byte[32];
        new SecureRandom().nextBytes(playerID);
        registeredIDs = new HashSet<>();
    }

    public ChannelHandlerContext getCTX() {
        return ctx;
    }

    public byte[] getPlayerID() {
        return playerID;
    }

    public String getClientID() {
        return clientID;
    }

    public boolean registerBlockID(String id) {
        return registeredIDs.add(id);
    }

    public void queueReplacedBlocks(ServerBlockAndPosition serverBlockAndPosition) {
        replacedBlocks.add(serverBlockAndPosition);
    }

    public Queue<ServerBlockAndPosition> getReplacedBlocks() {
        return replacedBlocks;
    }

    public PlayerEntity getPlayerEntity() {
        return playerEntity;
    }

    public void disconnect() {
        disconnect.accept(this);
    }

    public boolean isOpen() {
        return ctx.channel().isOpen();
    }
}