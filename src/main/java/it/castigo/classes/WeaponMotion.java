package it.castigo.classes;

import it.castigo.classes.model.Skill;

/** Cosmetic gesture only: no attack, item use, damage or projectile is generated here. */
public final class WeaponMotion {
    private WeaponMotion() {}
    public static String style(Skill.Effect effect) {
        return switch(effect) {
            case PRECISE_SHOT,HINDERING_SHOT,DOUBLE_SHOT,MASTER_SHOT,COVER_FIRE -> "BOW";
            case GUARD,BULWARK,SHIELD_BASH -> "SHIELD";
            case MELEE,LONG_THRUST -> "THRUST";
            case HEAVY_STRIKE,GUARD_BREAK -> "HEAVY";
            case COUNTER,SWEEP,LOW_SWEEP,COMBO,STOP_STRIKE -> "SLASH";
            default -> "CAST";
        };
    }
}
