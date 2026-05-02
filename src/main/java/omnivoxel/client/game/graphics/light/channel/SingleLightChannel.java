package omnivoxel.client.game.graphics.light.channel;

public class SingleLightChannel implements LightChannel {
    private final byte light;

    public SingleLightChannel(byte light) {
        this.light = light;
    }

    @Override
    public byte getLighting(int index) {
        return light;
    }

    @Override
    public LightChannel setLighting(int idx, byte newLight) {
        if (newLight == light) {
            return this;
        }
        return new ModifiedLightChannel(idx, newLight, this);
    }
}