package omnivoxel.client.game.graphics.light;

import omnivoxel.client.game.graphics.light.channel.LightChannel;
import omnivoxel.client.game.graphics.light.channel.LightChannels;
import omnivoxel.client.game.graphics.light.channel.SingleLightChannel;

public class ChunkLightingData {
    public static final ChunkLightingData EMPTY = new ChunkLightingData(new SingleLightChannel((byte) 0), new SingleLightChannel((byte) 0), new SingleLightChannel((byte) 0), new SingleLightChannel((byte) 0));
    private LightChannel redChannel;
    private LightChannel greenChannel;
    private LightChannel blueChannel;
    private LightChannel skylightChannel;

    public ChunkLightingData(LightChannel redChannel, LightChannel greenChannel, LightChannel blueChannel, LightChannel skylightChannel) {
        this.redChannel = redChannel;
        this.greenChannel = greenChannel;
        this.blueChannel = blueChannel;
        this.skylightChannel = skylightChannel;
    }

    public LightChannel getRedChannel() {
        return redChannel;
    }

    public void setRedChannel(LightChannel redChannel) {
        this.redChannel = redChannel;
    }

    public LightChannel getGreenChannel() {
        return greenChannel;
    }

    public void setGreenChannel(LightChannel greenChannel) {
        this.greenChannel = greenChannel;
    }

    public LightChannel getBlueChannel() {
        return blueChannel;
    }

    public void setBlueChannel(LightChannel blueChannel) {
        this.blueChannel = blueChannel;
    }

    public LightChannel getSkylightChannel() {
        return skylightChannel;
    }

    public void setSkylightChannel(LightChannel skylightChannel) {
        this.skylightChannel = skylightChannel;
    }

    public LightChannel getChannel(LightChannels channel) {
        return switch (channel) {
            case LightChannels.RED -> redChannel;
            case LightChannels.GREEN -> greenChannel;
            case LightChannels.BLUE -> blueChannel;
            case LightChannels.SKYLIGHT -> skylightChannel;
        };
    }
}