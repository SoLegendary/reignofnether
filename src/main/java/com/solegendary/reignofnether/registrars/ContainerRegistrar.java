package com.solegendary.reignofnether.registrars;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.guiscreen.TopdownGuiContainer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ContainerRegistrar {

    public static final DeferredRegister<MenuType<?>> CONTAINERS =
            DeferredRegister.create(BuiltInRegistries.MENU, ReignOfNether.MOD_ID);

    // 1.21.1 made MenuType's constructor private; IMenuTypeExtension#create is the loader that
    // wraps a factory taking the id and the player inventory.
    public static final Supplier<MenuType<TopdownGuiContainer>> TOPDOWNGUI_CONTAINER =
            CONTAINERS.register("topdowngui_container", () -> IMenuTypeExtension.create(
                    (id, inv, buf) -> new TopdownGuiContainer(id, inv)
            ));

    public static void init(ModContainer container) {
        CONTAINERS.register(container.getEventBus());
    }
}
