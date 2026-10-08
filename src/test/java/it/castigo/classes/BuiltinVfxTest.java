package it.castigo.classes;

import com.google.gson.*;
import it.castigo.classes.config.ClassCatalog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.InputStreamReader;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class BuiltinVfxTest {
    @TempDir Path directory;
    @Test void archerSkillsUseDistinctColorsConsistentAcrossEveryStage() {
        var presets=JsonParser.parseReader(new InputStreamReader(getClass().getResourceAsStream("/skill-vfx.json"),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        var colors=new java.util.HashSet<String>();
        for(var value:presets.asMap().values()) {
            var preset=value.getAsJsonObject();if(!preset.get("class").getAsString().equals("arciere"))continue;
            var stages=preset.getAsJsonObject("presentation");String color=null;
            for(var stage:stages.asMap().values()) {
                var cue=stage.getAsJsonObject();if(!cue.get("enabled").getAsBoolean())continue;
                String tint=cue.getAsJsonObject("mesh").get("tint").getAsString();
                if(color==null)color=tint;else assertEquals(color,tint);
                assertEquals(color,cue.getAsJsonObject("particles").get("color").getAsString());
            }
            assertTrue(colors.add(color),preset.get("name").getAsString());
        }
        assertEquals(8,colors.size());
    }
    @Test void all48RealSkillsHaveValidatedMeshPresetsAndPortableEditorRoundTrips() throws Exception {
        var classes=java.util.List.of("mago","mago_bianco","mago_nero","guerriero_scudo","guerriero_due_mani","arciere");
        for(String c:classes)try(var stream=getClass().getResourceAsStream("/classes/"+c+".yml")){Files.copy(stream,directory.resolve(c+".yml"));}
        var catalog=new ClassCatalog(directory.toFile());
        var json=JsonParser.parseReader(new InputStreamReader(getClass().getResourceAsStream("/skill-vfx.json"),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals(48,json.size());int count=0;
        for(String c:classes)for(var skill:catalog.get(c).skills()) {
            count++;assertTrue(json.has(skill.id()),skill.id());
            var fx=catalog.presentation(skill);assertTrue(fx.cues().values().stream().anyMatch(v->v.enabled()&&v.hasMesh()),skill.id());
            for(var cue:fx.cues().values())if(cue.enabled()) {
                var restored=VfxSettings.read(VfxSettings.draft(cue));
                assertEquals(cue.shape(),restored.shape());assertEquals(cue.mesh(),restored.mesh());
            }
        }
        assertEquals(48,count);
    }
    @Test void areaRadiusUsesCurrentClassConfigurationAndExplicitYamlStillWins() throws Exception {
        var skill=new it.castigo.classes.model.Skill("mago_bianco_santuario","Santuario","",it.castigo.classes.model.Skill.Effect.SANCTUARY,"minecraft:stick",0xffffff,1,1,1,1,1,18,7,200);
        // Use the actual shipped ID, not a guessed alias.
        var json=JsonParser.parseReader(new InputStreamReader(getClass().getResourceAsStream("/skill-vfx.json"),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        String id=json.entrySet().stream().filter(e->e.getValue().getAsJsonObject().get("effect").getAsString().equals("SANCTUARY")).findFirst().orElseThrow().getKey();
        skill=new it.castigo.classes.model.Skill(id,skill.name(),"",skill.effect(),skill.icon(),skill.color(),1,1,1,1,1,18,7,200);
        assertEquals(7,SkillPresentation.read(null,skill).cues().get(SkillPresentation.Stage.TELEGRAPH).radius());
        var override=new org.bukkit.configuration.file.YamlConfiguration();override.set("telegraph.radius",3);
        assertEquals(3,SkillPresentation.read(override,skill).cues().get(SkillPresentation.Stage.TELEGRAPH).radius());
    }
}
