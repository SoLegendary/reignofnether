package com.solegendary.reignofnether.registrars;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.blocks.RTSStructureBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class BlockEntityRegistrar {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ReignOfNether.MOD_ID);

    public static final RegistryObject<BlockEntityType<RTSStructureBlockEntity>> RTS_STRUCTURE_BLOCK_ENTITY =
            register("rts_structure_block_entity",
                    () -> BlockEntityType.Builder.of(RTSStructureBlockEntity::new,
                            BlockRegistrar.RTS_STRUCTURE_BLOCK.get()).build(null)
            );

    private static <T extends BlockEntity> RegistryObject<BlockEntityType<T>> register(String name, Supplier<BlockEntityType<T>> blockEntity) {
        return BLOCK_ENTITIES.register(name, blockEntity);
    }

    public static void init(FMLJavaModLoadingContext context) {
        BLOCK_ENTITIES.register(context.getModEventBus());
    }
}
