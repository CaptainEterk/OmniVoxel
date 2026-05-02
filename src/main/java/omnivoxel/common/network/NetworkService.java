package omnivoxel.common.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import omnivoxel.server.PackageID;
import omnivoxel.util.log.Logger;

public class NetworkService {
    private static void flush(Channel channel, ByteBuf byteBuf) {
        channel.writeAndFlush(byteBuf).addListener(f -> {
            if (!f.isSuccess()) {
                Logger.error(Logger.Priority.HIGH, "Failed to send packet: " + f.cause());
                f.cause().printStackTrace();
            }
        });
    }

    private static boolean checkChannel(Channel channel, PackageID id) {
        if (channel == null) {
            Logger.error(String.format("Failed to send PackageID.%s because channel is null. You may not be connected.", id.toString()));
            return false;
        }
        if (!channel.isActive()) {
            Logger.error("Channel is closed. Cannot send " + id + " package.");
            return false;
        }
        return true;
    }

    public static boolean checkChannel(Channel channel) {
        return channel != null && channel.isActive();
    }

    public static void sendDoubles(Channel channel, PackageID id, byte[] clientID, double... numbers) {
        if (checkChannel(channel, id)) {
            ByteBuf buffer = Unpooled.buffer();
            buffer.writeInt(Integer.BYTES + clientID.length + numbers.length * Double.BYTES);
            buffer.writeInt(id.ordinal());
            buffer.writeBytes(clientID);
            for (double i : numbers) {
                buffer.writeDouble(i);
            }
            flush(channel, buffer);
        }
    }

    public static void sendBytes(Channel channel, PackageID id, byte[] clientID, byte[]... bytes) {
        if (bytes.length == 0) {
            Logger.warn("NetworkService", "No bytes to send for PackageID." + id.toString());
        }
        if (checkChannel(channel, id)) {
            ByteBuf buffer = Unpooled.buffer();
            int length = Integer.BYTES + (clientID == null ? 0 : clientID.length);
            for (byte[] bites : bytes) {
                length += bites.length;
            }
            buffer.writeInt(length);
            buffer.writeInt(id.ordinal());
            if (clientID != null) {
                buffer.writeBytes(clientID);
            }
            for (byte[] bites : bytes) {
                buffer.writeBytes(bites);
            }
            flush(channel, buffer);
        }
    }

    public static void sendBytes3D(Channel channel, PackageID id, int x, int y, int z, byte[]... bytes) {
        if (checkChannel(channel, id)) {
            ByteBuf buffer = Unpooled.buffer();
            int length = Integer.BYTES * 5;
            for (byte[] bites : bytes) {
                length += bites.length + Integer.BYTES;
            }
            buffer.writeInt(length);
            buffer.writeInt(id.ordinal());
            buffer.writeInt(x);
            buffer.writeInt(y);
            buffer.writeInt(z);
            for (byte[] bites : bytes) {
                buffer.writeInt(bites.length);
                buffer.writeBytes(bites);
            }
            flush(channel, buffer);
        }
    }

    public static void sendBytes2D(Channel channel, PackageID id, int x, int z, byte[]... bytes) {
        if (checkChannel(channel, id)) {
            ByteBuf buffer = Unpooled.buffer();
            int length = Integer.BYTES * 4;
            for (byte[] bites : bytes) {
                length += bites.length;
            }
            buffer.writeInt(length);
            buffer.writeInt(id.ordinal());
            buffer.writeInt(x);
            buffer.writeInt(z);
            for (byte[] bites : bytes) {
                buffer.writeInt(bites.length);
                buffer.writeBytes(bites);
            }
            flush(channel, buffer);
        }
    }

    public static void sendInts(Channel channel, PackageID id, byte[] clientID, int... numbers) {
        if (checkChannel(channel, id)) {
            ByteBuf buffer = Unpooled.buffer();
            buffer.writeInt(Integer.BYTES + clientID.length + numbers.length * Integer.BYTES);
            buffer.writeInt(id.ordinal());
            buffer.writeBytes(clientID);
            for (int i : numbers) {
                buffer.writeInt(i);
            }
            flush(channel, buffer);
        }
    }
}