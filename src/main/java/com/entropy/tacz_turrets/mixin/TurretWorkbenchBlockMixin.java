package com.entropy.tacz_turrets.mixin;

import com.entropy.tacz_turrets.util.TurretWorkbench;
import com.tacz.guns.block.AbstractGunSmithTableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractGunSmithTableBlock.class)
public abstract class TurretWorkbenchBlockMixin extends BaseEntityBlock {
    private TurretWorkbenchBlockMixin(Properties properties) {
        super(properties);
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (!TurretWorkbench.is(level, pos, state)) return super.getDestroyProgress(state, player, level, pos);
        return player.getDestroySpeed(Blocks.IRON_BLOCK.defaultBlockState()) / 5.0F / 30;
    }
}
