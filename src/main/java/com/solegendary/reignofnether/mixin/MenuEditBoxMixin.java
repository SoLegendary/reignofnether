package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.ui.Theme;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

// text fields: dark inset with an orange edge when focused, like the RTS UI's inputs
@Mixin(EditBox.class)
public abstract class MenuEditBoxMixin {

    @ModifyArg(method = "renderWidget", index = 4, require = 1, at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"))
    private int borderColour(int colour) {
        return ((EditBox) (Object) this).isFocused() ? Theme.ORANGE : Theme.FAINT;
    }

    @ModifyArg(method = "renderWidget", index = 4, require = 1, at = @At(value = "INVOKE", ordinal = 1,
            target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"))
    private int fillColour(int colour) {
        return Theme.INK;
    }
}
