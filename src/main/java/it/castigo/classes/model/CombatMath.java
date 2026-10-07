package it.castigo.classes.model;

/** All combat formulas consume the same combined class + level + allocated attributes. */
public final class CombatMath {
    private CombatMath() {}
    public static double meleeBonus(Stats s,double strengthFactor) {
        return Math.min(2047,s.attack()+s.strength()*strengthFactor);
    }
    public static double speedBonus(Stats s,double dexterityFactor) { return Math.min(2,s.dexterity()*dexterityFactor); }
    public static double defenseReduction(Stats s) { return 100*s.defense()/(100+s.defense()); }
    public static double skillPower(Stats s,Skill skill) { return skill.power()+s.intelligence()*skill.intelligenceScale(); }
}
