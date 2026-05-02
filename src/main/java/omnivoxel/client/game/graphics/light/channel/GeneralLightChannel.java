package omnivoxel.client.game.graphics.light.channel;

import omnivoxel.common.settings.ConstantCommonSettings;

public class GeneralLightChannel implements LightChannel {
    private final byte[] channel;

    public GeneralLightChannel(byte[] channel) {
        this.channel = channel;
    }

    public GeneralLightChannel(LightChannel lightChannel) {
        channel = new byte[ConstantCommonSettings.BLOCKS_IN_CHUNK];
        for (int i = 0; i < ConstantCommonSettings.BLOCKS_IN_CHUNK; i++) {
            channel[i] = lightChannel.getLighting(i);
        }
    }

    public byte getLighting(int index) {
        return channel[index];
    }

    @Override
    public LightChannel setLighting(int idx, byte newLight) {
        channel[idx] = newLight;
        return this;
    }
}