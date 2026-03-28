package omnivoxel.client.game.graphics.light;

import omnivoxel.client.game.graphics.light.channel.EmptyLightChannel;
import omnivoxel.client.game.graphics.light.channel.LightChannel;

public class ChunkLightingData {
    public static final ChunkLightingData EMPTY = new ChunkLightingData(new EmptyLightChannel(), new EmptyLightChannel(), new EmptyLightChannel(), new EmptyLightChannel());
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
}