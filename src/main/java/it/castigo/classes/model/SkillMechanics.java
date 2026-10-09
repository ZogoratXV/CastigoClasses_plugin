package it.castigo.classes.model;

import it.castigo.classes.config.ClassCatalog;
import org.bukkit.configuration.ConfigurationSection;
import java.util.Locale;

public record SkillMechanics(Weapon weapon,int preparationTicks,double strengthScale,double dexterityScale,
                             double attackScale,double fraction) {
    public enum Weapon { ANY, SHIELD, TWO_HANDED, BOW }
    public static SkillMechanics read(ConfigurationSection c) {
        String key="preparation-seconds";
        for(String alias:new String[]{"casting-time","casting-time-seconds","cast-time-seconds"})if(c.contains(alias)) {
            if(!key.equals("preparation-seconds"))throw new IllegalArgumentException("Usa un solo alias per il casting time");
            key=alias;
        }
        double seconds=ClassCatalog.number(c,key,0,0,30);
        return new SkillMechanics(Weapon.valueOf(c.getString("weapon","ANY").toUpperCase(Locale.ROOT)),(int)Math.round(seconds*20),
                ClassCatalog.number(c,"strength-scale",0,0,100),ClassCatalog.number(c,"dexterity-scale",0,0,100),
                ClassCatalog.number(c,"attack-scale",0,0,100),ClassCatalog.number(c,"fraction",0.3,0,0.8));
    }
    public double power(Stats stats,Skill skill) {
        return Math.min(10000,CombatMath.skillPower(stats,skill)+stats.strength()*strengthScale+stats.dexterity()*dexterityScale+stats.attack()*attackScale);
    }
}
