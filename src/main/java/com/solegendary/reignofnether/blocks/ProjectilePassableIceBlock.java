package com.solegendary.reignofnether.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// identical to packed ice, except projectiles (arrows, fireballs, snowballs, etc.) fly straight through it
// note that AbstractArrow.tick() checks collision with an empty context, so AbstractArrowMixin also needs to
// know about this block to stop arrows sticking in it
public class ProjectilePassableIceBlock extends Block {

    public ProjectilePassableIceBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext entityContext && entityContext.getEntity() instanceof Projectile) {
            return Shapes.empty();
        }
        return super.getCollisionShape(state, level, pos, context);
    }
}
