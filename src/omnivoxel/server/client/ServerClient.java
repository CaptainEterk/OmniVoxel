package omnivoxel.server.client;

import io.netty.channel.ChannelHandlerContext;
import omnivoxel.client.game.hitbox.Hitbox;
import omnivoxel.server.entity.EntityType;
import omnivoxel.server.entity.mob.MobEntity;
import omnivoxel.util.bytes.ByteUtils;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;

public class ServerClient extends MobEntity implements ServerItem {
    public final Set<String> registeredIDs;
    private final String clientID;
    private final ChannelHandlerContext ctx;
    private final byte[] playerID;

    public ServerClient(String clientID, ChannelHandlerContext ctx) {
        super(clientID, new Hitbox(0, 0, 0, 1, 2, 1, 2, 3, 2));
        this.clientID = clientID;
        this.ctx = ctx;
        playerID = new byte[32];
        new SecureRandom().nextBytes(playerID);
        registeredIDs = new HashSet<>();
    }

    @Override
    public byte[] getBytes() {
        byte[] nameBytes = clientID.getBytes();
        int totalSize = Integer.BYTES
                + playerID.length
                + Integer.BYTES
                + Double.BYTES * 5
                + Integer.BYTES
                + nameBytes.length;

        byte[] out = new byte[totalSize];
        int offset = 0;

        ByteUtils.addInt(out, playerID.length, offset);
        offset += Integer.BYTES;

        System.arraycopy(playerID, 0, out, offset, playerID.length);
        offset += playerID.length;

        ByteUtils.addInt(out, EntityType.Type.PLAYER.ordinal(), offset);
        offset += Integer.BYTES;

        ByteUtils.addDouble(out, x, offset);
        offset += Double.BYTES;
        ByteUtils.addDouble(out, y, offset);
        offset += Double.BYTES;
        ByteUtils.addDouble(out, z, offset);
        offset += Double.BYTES;
        ByteUtils.addDouble(out, pitch, offset);
        offset += Double.BYTES;
        ByteUtils.addDouble(out, yaw, offset);
        offset += Double.BYTES;

        ByteUtils.addInt(out, nameBytes.length, offset);
        offset += Integer.BYTES;

        System.arraycopy(nameBytes, 0, out, offset, nameBytes.length);

        return out;
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
}