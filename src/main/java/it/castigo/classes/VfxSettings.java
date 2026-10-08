package it.castigo.classes;

import com.google.gson.*;
import it.castigo.classes.model.Skill;
import org.bukkit.Particle;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.nio.file.*;
import java.util.*;

/** Admin-authored cosmetic overrides, separate from class/gameplay definitions. */
public final class VfxSettings {
    private final Path file;
    private JsonObject data=new JsonObject();
    private final Map<String,SkillPresentation> cache=new HashMap<>();
    public VfxSettings(File file) throws Exception {
        this.file=file.toPath();
        if(file.exists()) {
            if(Files.size(this.file)>8_000_000)throw new IllegalArgumentException("File VFX troppo grande");
            data=JsonParser.parseString(Files.readString(this.file)).getAsJsonObject();
            if(data.size()>10240)throw new IllegalArgumentException("Troppi preset VFX");
            for(var e:data.entrySet()) {
                String[] key=e.getKey().split("/");if(key.length!=3)throw new IllegalArgumentException("Chiave VFX non valida");
                key(key[0],key[1],SkillPresentation.Stage.valueOf(key[2]));read(e.getValue().getAsJsonObject());
            }
        }
    }
    private static String key(String c,String s,SkillPresentation.Stage stage) {
        if(!c.matches("[a-z0-9_]{1,40}")||!s.matches("[a-z0-9_]{1,40}"))throw new IllegalArgumentException("ID VFX non valido");
        return c+"/"+s+"/"+stage.name();
    }
    public SkillPresentation resolve(String c,Skill skill,SkillPresentation base) {
        return cache.computeIfAbsent(c+"/"+skill.id(),unused->{
            var cues=new EnumMap<SkillPresentation.Stage,SkillPresentation.Cue>(SkillPresentation.Stage.class);cues.putAll(base.cues());boolean modified=false;
            for(var stage:SkillPresentation.Stage.values()) { var raw=data.get(key(c,skill.id(),stage));if(raw!=null) { cues.put(stage,read(raw.getAsJsonObject()));modified=true; } }
            return modified?new SkillPresentation(true,cues):base;
        });
    }
    public void save(String c,String skill,SkillPresentation.Stage stage,JsonObject draft) throws Exception {
        if(draft!=null)read(draft);var next=data.deepCopy();String key=key(c,skill,stage);
        if(draft==null)next.remove(key);else next.add(key,draft.deepCopy());
        if(next.size()>10240||next.toString().length()>7_000_000)throw new IllegalArgumentException("Archivio VFX pieno");
        Files.createDirectories(file.getParent());Path temp=Files.createTempFile(file.getParent(),"vfx-",".tmp");
        try {
            Files.writeString(temp,new GsonBuilder().setPrettyPrinting().create().toJson(next));
            try { Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
            catch(AtomicMoveNotSupportedException e) { Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING); }
            data=next;cache.clear();
        } finally { Files.deleteIfExists(temp); }
    }
    public static SkillPresentation.Cue read(JsonObject draft) {
        Set<String> keys=Set.of("enabled","shape","points","duration","radius","particlesEnabled","particle","count","spread","color","size","soundEnabled","sound","volume","pitch");
        if(!draft.keySet().equals(keys))throw new IllegalArgumentException("Campi del preset VFX non validi");
        var yaml=new YamlConfiguration();
        for(String k:List.of("enabled","particlesEnabled","soundEnabled"))if(!draft.get(k).isJsonPrimitive()||!draft.get(k).getAsJsonPrimitive().isBoolean())throw new IllegalArgumentException("Booleano richiesto: "+k);
        for(String k:List.of("points","duration","radius","count","spread","size","volume","pitch"))if(!draft.get(k).isJsonPrimitive()||!draft.get(k).getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("Numero richiesto: "+k);
        yaml.set("impact.enabled",draft.get("enabled").getAsBoolean());yaml.set("impact.shape",draft.get("shape").getAsString());
        yaml.set("impact.points",draft.get("points").getAsDouble());yaml.set("impact.duration-ticks",draft.get("duration").getAsDouble());yaml.set("impact.radius",draft.get("radius").getAsDouble());
        yaml.set("impact.particles.enabled",draft.get("particlesEnabled").getAsBoolean());yaml.set("impact.particles.particle",draft.get("particle").getAsString());
        yaml.set("impact.particles.count",draft.get("count").getAsDouble());yaml.set("impact.particles.spread",draft.get("spread").getAsDouble());
        if(!draft.get("color").getAsString().matches("[0-9a-fA-F]{6}"))throw new IllegalArgumentException("Colore RGB a sei cifre richiesto");
        yaml.set("impact.particles.color",draft.get("color").getAsString());yaml.set("impact.particles.size",draft.get("size").getAsDouble());
        yaml.set("impact.sound.enabled",draft.get("soundEnabled").getAsBoolean());yaml.set("impact.sound.id",draft.get("sound").getAsString());
        yaml.set("impact.sound.volume",draft.get("volume").getAsDouble());yaml.set("impact.sound.pitch",draft.get("pitch").getAsDouble());
        Skill sample=new Skill("preview","Preview","",Skill.Effect.BOLT,"minecraft:stick",0xFFFFFF,1,0,100,0,0,10,1,20);
        return SkillPresentation.read(yaml,sample).cues().get(SkillPresentation.Stage.IMPACT);
    }
    public static JsonObject draft(SkillPresentation.Cue c) {
        var o=new JsonObject();o.addProperty("enabled",c.enabled());o.addProperty("shape",c.shape().name());
        o.addProperty("points",c.points());o.addProperty("duration",c.durationTicks());o.addProperty("radius",c.radius());
        o.addProperty("particlesEnabled",c.particles().enabled());o.addProperty("particle",c.particles().particle().name());
        o.addProperty("count",c.particles().count());o.addProperty("spread",c.particles().spread());
        var dust=c.particles().data() instanceof Particle.DustOptions d?d:null;
        o.addProperty("color",String.format(Locale.ROOT,"%06X",dust==null?0xFFFFFF:dust.getColor().asRGB()));o.addProperty("size",dust==null?1:c.particles().data() instanceof Particle.DustOptions d?d.getSize():1);
        o.addProperty("soundEnabled",c.sound().enabled());o.addProperty("sound",c.sound().id());o.addProperty("volume",c.sound().volume());o.addProperty("pitch",c.sound().pitch());return o;
    }
}
