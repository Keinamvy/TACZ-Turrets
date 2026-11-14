package com.entropy.tacz_turrets.client.model;

import com.entropy.tacz_turrets.TACZTurrets;
import com.entropy.tacz_turrets.turret.TurretEntity;
import com.entropy.tacz_turrets.turret.TurretState;
import com.entropy.tacz_turrets.turret.TurretType;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;

public class TurretModel extends DefaultedEntityGeoModel<TurretEntity> {
    public static final DataTicket<TurretType> TURRET_TYPE = DataTicket.create("tacz_turrets_type", TurretType.class);
    public static final DataTicket<TurretState> TURRET_STATE = DataTicket.create("tacz_turrets_state", TurretState.class);
    public static final DataTicket<TurretEntity> TURRET = DataTicket.create("tacz_turrets_turret", TurretEntity.class);

    public TurretModel() {
        super(TACZTurrets.id("turret"));
    }

    @Override
    public void addAdditionalStateData(TurretEntity animatable, Object relatedObject, GeoRenderState renderState) {
        renderState.addGeckolibData(TURRET_TYPE, animatable.getTurretType());
        renderState.addGeckolibData(TURRET_STATE, TurretState.getState(animatable));
        renderState.addGeckolibData(TURRET, animatable);
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        TurretType type = renderState.getOrDefaultGeckolibData(TURRET_TYPE, TurretType.MODERN);
        return type.model();
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        TurretType type = renderState.getOrDefaultGeckolibData(TURRET_TYPE, TurretType.MODERN);
        TurretState state = renderState.getOrDefaultGeckolibData(TURRET_STATE, TurretState.NO_GUN);
        return type.texture(state);
    }
}
