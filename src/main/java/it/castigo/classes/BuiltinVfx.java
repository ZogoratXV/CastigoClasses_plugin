package it.castigo.classes;

import com.google.gson.*;
import it.castigo.classes.model.Skill;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Authored defaults for all shipped abilities; user YAML and editor overrides win. */
final class BuiltinVfx {
    private static final JsonObject PRESETS=load();
    private static JsonObject load() {
        try(var stream=BuiltinVfx.class.getResourceAsStream("/skill-vfx.json")) {
            if(stream==null)throw new IllegalStateException("Missing skill-vfx.json");
            return JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
        } catch(IOException e) { throw new IllegalStateException(e); }
    }
    static void apply(YamlConfiguration target,Skill skill) {
        var preset=PRESETS.getAsJsonObject(skill.id());if(preset==null||!preset.get("effect").getAsString().equals(skill.effect().name()))return;
        copy(target,"",preset.getAsJsonObject("presentation"));
        switch(skill.effect()) {
            case SANCTUARY,RUIN,VORTEX,METEOR -> { target.set("telegraph.radius",skill.radius());target.set("impact.radius",skill.radius()); }
            case REPULSE,SWEEP,LOW_SWEEP,FROST_NOVA -> {
                target.set("cast.radius",skill.radius());
                if("MESH_WAVE".equals(target.getString("impact.shape")))target.set("impact.radius",skill.radius());
            }
            default -> { }
        }
    }
    private static void copy(YamlConfiguration target,String prefix,JsonObject object) {
        for(var entry:object.entrySet()) {
            String path=prefix+entry.getKey();var v=entry.getValue();
            if(v.isJsonObject())copy(target,path+".",v.getAsJsonObject());
            else { var p=v.getAsJsonPrimitive();target.set(path,p.isBoolean()?p.getAsBoolean():p.isNumber()?p.getAsDouble():p.getAsString()); }
        }
    }
}
