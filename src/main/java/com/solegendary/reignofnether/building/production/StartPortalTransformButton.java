package com.solegendary.reignofnether.building.production;

import com.solegendary.reignofnether.building.buildings.placements.PortalPlacement;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.keybinds.Keybinding;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.function.Supplier;

public class StartPortalTransformButton extends StartProductionButton {
    public StartPortalTransformButton(String name, ResourceLocation rl, Keybinding hotkey, Supplier<Boolean> isHidden, Supplier<Boolean> isEnabled, List<FormattedCharSequence> tooltipLines, ProductionPlacement placement, ProductionItem production) {
        super(name, rl, hotkey, isHidden, isEnabled, tooltipLines, production);

        Runnable startTransform = onLeftClick;
        onLeftClick = () -> {
            if (placement.productionQueue.isEmpty()) {
                startTransform.run();
            }
        };
    }

    public StartPortalTransformButton(String name, ResourceLocation iconRl, ResourceLocation frameRl, Keybinding hotkey, Supplier<Boolean> isHidden, Supplier<Boolean> isEnabled, List<FormattedCharSequence> tooltipLines, ProductionPlacement placement, ProductionItem production) {
        super(name, iconRl, frameRl, hotkey, isHidden, isEnabled, tooltipLines, production);

        Runnable startTransform = onLeftClick;
        onLeftClick = () -> {
            if (placement.productionQueue.isEmpty()) {
                startTransform.run();
            }
        };
    }
}
