package com.solegendary.reignofnether.registrars;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.commands.rtsapi.argument.BuildingArgument;
import com.solegendary.reignofnether.commands.rtsapi.argument.PlayerNameArgument;
import com.solegendary.reignofnether.commands.rtsapi.argument.UnitArgument;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * The three custom brigadier argument types behind {@code /rtsapi}.
 *
 * <p>Each type has to be known in <em>two</em> places, and they are populated at different times:
 *
 * <ul>
 *   <li>{@code ArgumentTypeInfos.registerByClass} fills a client-local map keyed by argument class.
 *       It is what {@code Commands.validate()} consults, and that runs inside
 *       {@code Bootstrap.validate()} - <em>before</em> NeoForge constructs the mods - so it cannot
 *       be done from the mod constructor or from a setup event. The three argument classes therefore
 *       do it from their own static initialisers, which run while the commands using them are being
 *       registered, i.e. still before validate.
 *   <li>{@code minecraft:command_argument_type} is the registry whose numeric ids go over the wire:
 *       {@code ClientboundCommandsPacket} writes an argument type as {@code registryId} and the
 *       client resolves it with {@code BuiltInRegistries.COMMAND_ARGUMENT_TYPE.byId}. If the id is
 *       unknown the client returns a null node instead of one and the rest of the packet is read
 *       from the wrong offset, which surfaces as
 *       {@code VarIntArray with size 33554431 is bigger than allowed}. So the types must be in this
 *       registry on <em>both</em> sides, which is what the mod-bus DeferredRegister below is for.
 * </ul>
 *
 * <p>The suppliers stay so call sites keep their {@code .get()} shape.
 */
public class CommandArgumentRegistrar {

    public static final DeferredRegister<ArgumentTypeInfo<?, ?>> ARGUMENT_TYPES =
            DeferredRegister.create(BuiltInRegistries.COMMAND_ARGUMENT_TYPE, ReignOfNether.MOD_ID);

    // The Info instances come from the argument classes themselves, so that byClass and the registry
    // hold the very same object - see the comment on BuildingArgument#INFO.
    public static final Supplier<ArgumentTypeInfo<BuildingArgument, ?>> BUILDING_ARGUMENT =
            ARGUMENT_TYPES.register("building", () -> BuildingArgument.INFO);

    public static final Supplier<ArgumentTypeInfo<PlayerNameArgument, ?>> PLAYER_NAME_ARGUMENT =
            ARGUMENT_TYPES.register("player_name", () -> PlayerNameArgument.INFO);

    public static final Supplier<ArgumentTypeInfo<UnitArgument, ?>> UNIT_ARGUMENT =
            ARGUMENT_TYPES.register("unit", () -> UnitArgument.INFO);

    public static void init(ModContainer container) {
        ARGUMENT_TYPES.register(container.getEventBus());
    }
}
