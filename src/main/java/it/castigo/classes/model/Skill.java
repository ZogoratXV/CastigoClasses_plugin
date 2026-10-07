package it.castigo.classes.model;

public record Skill(String id, String name, String description, Effect effect, String icon,
                    int color, int unlockLevel, double cost, long cooldownMs, double power,
                    double intelligenceScale, double range, double radius, int durationTicks) {
    public enum Effect {
        BOLT, FIREBALL, FROST_NOVA, BLINK, WARD, HEAL, LIGHTNING, METEOR,
        ALLY_HEAL, HOT, CLEANSE, LINK, REPULSE, SANCTUARY,
        CURSED_BOLT, VULNERABILITY, DOT, FEAR, RUIN, HEAL_BLOCK, DRAIN, VORTEX,
        MELEE, SHIELD_BASH, GUARD, DASH, COUNTER, RECOVER, BULWARK,
        HEAVY_STRIKE, LONG_THRUST, CHARGE, STOP_STRIKE, SWEEP, LOW_SWEEP, GUARD_BREAK, COMBO,
        PRECISE_SHOT, HUNTER_STEP, STUDY, DISENGAGE, HINDERING_SHOT, COVER_FIRE, DOUBLE_SHOT, MASTER_SHOT;
        public boolean discipline() { return ordinal()>=ALLY_HEAL.ordinal(); }
    }
}
