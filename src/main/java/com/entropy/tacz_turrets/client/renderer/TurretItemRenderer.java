package com.entropy.tacz_turrets.client.renderer;

import com.entropy.tacz_turrets.TACZTurrets;
import com.entropy.tacz_turrets.item.TurretItem;
import com.entropy.tacz_turrets.turret.TurretState;
import com.entropy.tacz_turrets.turret.TurretType;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public class TurretItemRenderer extends GeoItemRenderer<TurretItem> {
    public static final GeoRenderProvider RENDER_PROVIDER = new GeoRenderProvider() {
        private TurretItemRenderer renderer;

        @Override
        public GeoItemRenderer<?> getGeoItemRenderer() {
            if (renderer == null) renderer = new TurretItemRenderer();
            return renderer;
        }
    };
    public static final GeoRenderProvider EXTENSIONS = RENDER_PROVIDER;

    public TurretItemRenderer() {
        super(new GeoModel<TurretItem>() {
            private TurretType getType(GeoRenderState renderState) {
                Object data = renderState.getGeckolibData(GeoItemRenderer.CURRENT_ITEM);
                if (data instanceof TurretItem ti) return ti.getType();
                if (data instanceof ItemStack stack && stack.getItem() instanceof TurretItem ti) return ti.getType();
                return TurretType.MODERN;
            }

            @Override
            public Identifier getModelResource(GeoRenderState renderState) {
                return getType(renderState).model();
            }

            @Override
            public Identifier getTextureResource(GeoRenderState renderState) {
                return getType(renderState).texture(TurretState.NO_GUN);
            }

            @Override
            public Identifier getAnimationResource(TurretItem item) {
                return TACZTurrets.id("entity/turret");
            }
        });
        withRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}
