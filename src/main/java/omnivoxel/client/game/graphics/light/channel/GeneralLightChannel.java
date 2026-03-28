package omnivoxel.client.game.graphics.light.channel;

public class GeneralLightChannel implements LightChannel {
    private final byte[] channel;

    public GeneralLightChannel(byte[] channel) {
        this.channel = channel;
    }

    public byte getLighting(int index) {
        return channel[index];
    }
}