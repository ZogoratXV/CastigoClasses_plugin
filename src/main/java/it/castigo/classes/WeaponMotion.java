package it.castigo.classes;

import it.castigo.classes.model.Skill;

/** Cosmetic gesture only: no attack, item use, damage or projectile is generated here. */
public final class WeaponMotion {
    private WeaponMotion() {}
    public static String style(Skill skill){return skill.id().startsWith("guerriero_scudo_")&&skill.effect()==Skill.Effect.LINK?"SHIELD":style(skill.effect());}
    public static String gesture(Skill skill){
        if(skill.id().startsWith("guerriero_scudo_")&&skill.effect()==Skill.Effect.LINK)return "SHIELD";
        return switch(skill.effect()){
            case ALLY_HEAL,HOT,CLEANSE -> "BLESS";
            case LINK,SANCTUARY,REPULSE -> "CHANNEL";
            case CURSED_BOLT,VULNERABILITY,DOT,FEAR,RUIN,HEAL_BLOCK,DRAIN,VORTEX -> "CURSE";
            case DASH,CHARGE -> "DASH";
            case RECOVER -> "RALLY";
            case SWEEP,COMBO -> "SPIN";
            case LOW_SWEEP -> "LOW_SWEEP";
            default -> "";
        };
    }
    public static String style(Skill.Effect effect) {
        return switch(effect) {
            case PRECISE_SHOT,HINDERING_SHOT,DOUBLE_SHOT,MASTER_SHOT,COVER_FIRE,HUNTER_STEP,STUDY,DISENGAGE -> "BOW";
            case GUARD,BULWARK,SHIELD_BASH,DASH,RECOVER -> "SHIELD";
            case MELEE,LONG_THRUST -> "THRUST";
            case HEAVY_STRIKE,GUARD_BREAK,CHARGE -> "HEAVY";
            case COUNTER,SWEEP,LOW_SWEEP,COMBO,STOP_STRIKE -> "SLASH";
            default -> "CAST";
        };
    }
}
