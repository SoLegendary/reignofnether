package com.solegendary.reignofnether.rtsmap;

import com.google.gson.Gson;
import com.mojang.serialization.DataResult;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the join-message code against the 1.21.1 translatable-argument trap.
 *
 * <p>{@code TranslatableContents.ARG_CODEC} serialises every argument as a Number, Boolean, String
 * or Component. Anything else (a StringBuilder, a ResourceLocation, an NbtPath...) is rejected
 * while the system chat packet is encoded, and the resulting {@code EncoderException} disconnects
 * the player one tick after joining. That is exactly how every packaged 1.20.1 map failed in the
 * test round - the world loaded fine, then the client was kicked.
 *
 * <p>The test encodes through {@code TranslatableContents.CODEC}, the element the packet's
 * component codec delegates to (going through {@code ComponentSerialization.CODEC} instead would
 * need the NeoForge mod-loading bootstrap, which a unit test JVM does not have). It therefore
 * reproduces the production failure rather than only checking that the code compiles.
 */
class RtsMapInfoMessageTest {

    private static RTSMapInfo pyroclasmLike() {
        String json = """
            {
              "name": "Pyroclasm",
              "author": ["Killerbee47"],
              "description": "A lava island.",
              "version": "1.0",
              "startPositions": [{"id": 1, "x": 0, "y": 64, "z": 0}],
              "modes": {"ffa_3": [[1]], "1v1": [[1]]},
              "defaultMode": "ffa_3"
            }
            """;
        return new Gson().fromJson(json, RTSMapInfo.class);
    }

    private static DataResult<?> encodeArgs(Component component) {
        return TranslatableContents.CODEC.codec().encodeStart(NbtOps.INSTANCE, (TranslatableContents) component.getContents());
    }

    @Test
    void modesLabelIsAStringAndEncodes() {
        RTSMapInfo info = pyroclasmLike();
        String label = RTSMapInfoServerEvents.modesLabel(info);
        assertEquals("[ffa_3], 1v1", label);

        DataResult<?> encoded = encodeArgs(Component.translatable("message.reignofnether.rts_map_info_modes", label));
        assertTrue(encoded.result().isPresent(),
                "the modes join line must encode; error was: " + encoded.error().map(DataResult.Error::message).orElse("-"));
    }

    /**
     * The bug that was actually shipped: the same line built straight from the StringBuilder is
     * rejected. Dev-transformed vanilla rejects the argument eagerly in the constructor; the
     * shipping jar instead failed while encoding the chat packet ("This value needs to be parsed
     * as component"). Assert on whichever of the two the current classpath produces.
     */
    @Test
    void stringBuilderArgumentIsRejected() {
        TranslatableContents contents;
        try {
            contents = (TranslatableContents) Component
                    .translatable("message.reignofnether.rts_map_info_modes", new StringBuilder("[ffa_3], 1v1"))
                    .getContents();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("must be either a Component, Number, Boolean, or a String"),
                    "unexpected failure reason: " + e.getMessage());
            return;
        }

        DataResult<?> encoded = TranslatableContents.CODEC.codec().encodeStart(NbtOps.INSTANCE, contents);
        assertTrue(encoded.error().isPresent(), "a StringBuilder argument must not encode");
        assertTrue(encoded.error().get().message().contains("needs to be parsed as component"),
                "unexpected failure reason: " + encoded.error().get().message());
    }
}
