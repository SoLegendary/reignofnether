package com.solegendary.reignofnether.network;

import net.minecraft.network.FriendlyByteBuf;

import static net.minecraftforge.network.HandshakeMessages.C2SAcknowledge;

public class S2CReset extends C2SAcknowledge {

    private int loginIndex;

    public S2CReset() {
        super();
    }

    public void encode(FriendlyByteBuf buffer) {

    }

    public static S2CReset decode(FriendlyByteBuf buffer) {
        return new S2CReset();
    }

    public void setLoginIndex(final int loginIndex) {
        this.loginIndex = loginIndex;
    }

    public int getLoginIndex() {
        return loginIndex;
    }
}