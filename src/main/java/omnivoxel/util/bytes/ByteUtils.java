package omnivoxel.util.bytes;

import io.netty.buffer.ByteBuf;
import omnivoxel.common.annotations.NotNull;

public class ByteUtils {
    public static String bytesToHex(ByteBuf byteBuf, int start, int length) {
        StringBuilder hex = new StringBuilder();
        byte[] clientIDBytes = getBytes(byteBuf, start, length);
        for (int i = 0; i < 32; i++) {
            hex.append(String.format("%02X", clientIDBytes[i]));
        }
        return hex.toString();
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02X", b));
        }
        return hex.toString();
    }

    public static byte[] getBytes(ByteBuf byteBuf, int i, int length) {
        byte[] bytes = new byte[length];
        byteBuf.getBytes(i, bytes);
        return bytes;
    }

    public static void addFloat(byte @NotNull [] bytes, float f, int index) {
        int i = Float.floatToIntBits(f);
        addInt(bytes, i, index);
    }

    public static void addInt(byte @NotNull [] bytes, int i, int index) {
        bytes[index] = (byte) (i >> 24);
        bytes[index + 1] = (byte) (i >> 16);
        bytes[index + 2] = (byte) (i >> 8);
        bytes[index + 3] = (byte) (i);
    }

    public static void addFloats(byte @NotNull [] bytes, int i, float... floats) {
        for (int j = 0; j < floats.length; j++) {
            addFloat(bytes, floats[j], i + j * Float.BYTES);
        }
    }

    public static void addDouble(byte[] bytes, double d, int index) {
        long l = Double.doubleToLongBits(d);
        for (int i = 0; i < 8; i++) {
            bytes[index + i] = (byte) (l >>> (56 - (i * 8)));
        }
    }
}