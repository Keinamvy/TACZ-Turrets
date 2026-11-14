package com.entropy.tacz_turrets.mixin;

import com.entropy.tacz_turrets.config.TurretEnabledCondition;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tacz.guns.resource.manager.TableRecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(value = TableRecipeManager.class, remap = false)
public class TableRecipeManagerMixin {
    @Inject(method = "isGunSmithTableRecipe", at = @At("RETURN"), cancellable = true, remap = false)
    private static void filterDisabledTurretRecipes(JsonElement element, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || element == null || !element.isJsonObject()) {
            return;
        }
        JsonObject root = element.getAsJsonObject();
        if (!tacz_turrets$checkConditions(root, "fabric:load_conditions", "condition")
                || !tacz_turrets$checkConditions(root, "neoforge:conditions", "type")
                || !tacz_turrets$checkConditions(root, "conditions", "type")) {
            cir.setReturnValue(false);
        }
    }

    @Unique
    private static boolean tacz_turrets$checkConditions(JsonObject root, String arrayKey, String typeKey) {
        if (!root.has(arrayKey) || !root.get(arrayKey).isJsonArray()) {
            return true;
        }
        JsonArray conditions = root.getAsJsonArray(arrayKey);
        for (JsonElement condEl : conditions) {
            if (!condEl.isJsonObject()) continue;
            JsonObject cond = condEl.getAsJsonObject();
            if (!cond.has(typeKey)) continue;
            String type = cond.get(typeKey).getAsString();
            if ("tacz_turrets:turret_enabled".equals(type) && cond.has("turret")) {
                String turret = cond.get("turret").getAsString();
                Optional<Boolean> identical = cond.has("identical")
                        ? Optional.of(cond.get("identical").getAsBoolean())
                        : Optional.empty();
                if (!TurretEnabledCondition.of(turret, identical).test(null)) {
                    return false;
                }
            }
        }
        return true;
    }
}
