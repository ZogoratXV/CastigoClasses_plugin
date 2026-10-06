package it.castigo.classes.model;

public record Skill(String id, String name, String description, Effect effect, String icon,
                    int color, int unlockLevel, double cost, long cooldownMs, double power,
                    double intelligenceScale, double range, double radius, int durationTicks) {
    public enum Effect { BOLT, FIREBALL, FROST_NOVA, BLINK, WARD, HEAL, LIGHTNING, METEOR }
}
