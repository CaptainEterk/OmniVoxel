package omnivoxel.client.game.graphics.light.channel;

public class EmptyLightChannel implements LightChannel {
    @Override
    public byte getLighting(int index) {
        return 0;
    }
}