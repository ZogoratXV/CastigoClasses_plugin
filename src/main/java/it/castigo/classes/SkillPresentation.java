package it.castigo.classes;

import it.castigo.classes.config.ClassCatalog;
import it.castigo.classes.model.Skill;
import it.castigo.core.ParticleStyle;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import java.util.*;

/** Server-only cosmetic definitions: never serialized into the gameplay protocol. */
public record SkillPresentation(boolean enabled, Map<Stage,Cue> cues) {
    public enum Stage { CAST, TRAIL, IMPACT, TELEGRAPH, HIT }
    public enum Shape { BURST, LINE, RING, SPIRAL }
    public record Audio(boolean enabled,String id,SoundCategory category,float volume,float pitch) {}
    public record Cue(boolean enabled,Shape shape,int points,int durationTicks,double radius,ParticleStyle particles,Audio sound) {}
    public SkillPresentation { cues=Map.copyOf(cues); }

    public static SkillPresentation read(ConfigurationSection section,Skill skill) {
        YamlConfiguration defaults=new YamlConfiguration();
        String color=String.format(Locale.ROOT,"%06X",skill.color());
        cue(defaults,"cast",false,"BURST","DUST",1,color,null);
        cue(defaults,"trail",false,"LINE","DUST",1,color,null);
        cue(defaults,"impact",false,"BURST","DUST",12,color,null);
        cue(defaults,"telegraph",false,"RING","DUST",1,color,null);
        cue(defaults,"hit",false,"BURST","DUST",12,color,null);
        switch(skill.effect()) {
            case BOLT -> { enable(defaults,"trail"); sound(defaults,"impact","minecraft:block.amethyst_block.chime",0.8,1.5); }
            case LIGHTNING -> { enable(defaults,"trail"); enable(defaults,"impact"); sound(defaults,"impact","minecraft:entity.lightning_bolt.impact",0.6,1.5); }
            case FIREBALL -> { enable(defaults,"trail"); burst(defaults,"impact","FLAME",32,0.7); sound(defaults,"impact","minecraft:entity.generic.explode",0.6,1.3); }
            case FROST_NOVA -> { enable(defaults,"trail"); defaults.set("trail.shape","RING"); burst(defaults,"impact","SNOWFLAKE",32,1); sound(defaults,"impact","minecraft:block.glass.break",0.8,0.6); }
            case BLINK -> { enable(defaults,"trail"); sound(defaults,"impact","minecraft:entity.enderman.teleport",0.6,1.5); }
            case WARD -> { enable(defaults,"impact"); defaults.set("impact.shape","RING"); defaults.set("impact.radius",1.2); defaults.set("impact.particles.count",1); enable(defaults,"hit"); sound(defaults,"impact","minecraft:block.beacon.activate",0.6,1.5); }
            case HEAL -> { burst(defaults,"impact","HAPPY_VILLAGER",20,0.5); sound(defaults,"impact","minecraft:block.enchantment_table.use",0.7,1.3); }
            case METEOR -> { enable(defaults,"telegraph"); enable(defaults,"trail"); burst(defaults,"impact","EXPLOSION",1,0); sound(defaults,"impact","minecraft:entity.generic.explode",0.9,0.65); }
            default -> { burst(defaults,"impact","DUST",6,0.15); }
        }
        // Overlay only explicitly supplied keys, so a color-only edit retains all other defaults.
        if(section!=null) {
            for(String key:section.getKeys(false))
                if(!Set.of("enabled","cast","trail","impact","telegraph","hit").contains(key))throw new IllegalArgumentException("Fase presentation sconosciuta: "+key);
            for(String key:section.getKeys(true))if(!section.isConfigurationSection(key))defaults.set(key,section.get(key));
        }
        Map<Stage,Cue> result=new EnumMap<>(Stage.class);
        for(Stage stage:Stage.values()) {
            String path=stage.name().toLowerCase(Locale.ROOT);
            ConfigurationSection c=Objects.requireNonNull(defaults.getConfigurationSection(path),"Fase effetti non valida: "+path);
            ConfigurationSection particles=Objects.requireNonNull(c.getConfigurationSection("particles"),"Sezione particles richiesta");
            ConfigurationSection audio=Objects.requireNonNull(c.getConfigurationSection("sound"),"Sezione sound richiesta");
            integer(particles,"count",1,1,32);
            ClassCatalog.number(particles,"spread",0.08,0,2);
            ClassCatalog.number(particles,"size",1.2,0.05,4);
            bool(particles,"enabled",true);
            String soundId=audio.getString("id","");
            if(!soundId.matches("[a-z0-9_.-]+:[a-z0-9/._-]+")||soundId.length()>160)throw new IllegalArgumentException("ID suono non valido: "+soundId);
            result.put(stage,new Cue(bool(c,"enabled",true),Shape.valueOf(c.getString("shape","BURST").toUpperCase(Locale.ROOT)),
                    integer(c,"points",24,2,32),integer(c,"duration-ticks",8,1,40),ClassCatalog.number(c,"radius",skill.radius(),0.1,12),ParticleStyle.read(particles),
                    new Audio(bool(audio,"enabled",false),soundId,SoundCategory.valueOf(audio.getString("category","PLAYERS").toUpperCase(Locale.ROOT)),
                            (float)ClassCatalog.number(audio,"volume",0.7,0,2),(float)ClassCatalog.number(audio,"pitch",1,0.5,2))));
        }
        return new SkillPresentation(bool(defaults,"enabled",true),result);
    }
    private static boolean bool(ConfigurationSection c,String path,boolean fallback) {
        if(c.contains(path)&&!(c.get(path) instanceof Boolean))throw new IllegalArgumentException("Booleano richiesto: "+path);
        return c.getBoolean(path,fallback);
    }
    private static int integer(ConfigurationSection c,String path,int fallback,int min,int max) {
        double n=ClassCatalog.number(c,path,fallback,min,max);
        if(n!=Math.rint(n))throw new IllegalArgumentException("Intero richiesto: "+path);
        return (int)n;
    }
    private static void cue(YamlConfiguration c,String path,boolean enabled,String shape,String particle,int count,String color,String sound) {
        c.set(path+".enabled",true);c.set(path+".shape",shape);
        c.set(path+".particles.enabled",enabled);c.set(path+".particles.particle",particle);
        c.set(path+".particles.count",count);c.set(path+".particles.color",color);c.set(path+".particles.size",1.2);
        c.set(path+".sound.enabled",sound!=null);c.set(path+".sound.id",sound==null?"minecraft:block.amethyst_block.chime":sound);
    }
    private static void enable(YamlConfiguration c,String path) { c.set(path+".particles.enabled",true); }
    private static void burst(YamlConfiguration c,String path,String particle,int count,double spread) {
        enable(c,path);c.set(path+".particles.particle",particle);c.set(path+".particles.count",count);c.set(path+".particles.spread",spread);
    }
    private static void sound(YamlConfiguration c,String path,String id,double volume,double pitch) {
        c.set(path+".sound.enabled",true);c.set(path+".sound.id",id);c.set(path+".sound.volume",volume);c.set(path+".sound.pitch",pitch);
    }
}
