package it.castigo.classes;
import com.google.gson.*;
import it.castigo.classes.model.Skill;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class BlockbenchVfxTest {
    @Test void comboSendsOneSegmentForEachServerConfirmedStrike(){
        var s=new Skill("guerriero_due_mani_sequenza","Test","",Skill.Effect.COMBO,"minecraft:stick",0xffffff,1,1,1,1,1,24,3,100);
        for(int strike=0;strike<3;strike++){
            var p=packet();BlockbenchVfx.decorate(p,s,SkillPresentation.Stage.CAST,false);BlockbenchVfx.combo(p,s,SkillPresentation.Stage.CAST,strike);
            assertEquals(strike,p.get("modelStrike").getAsInt());assertEquals(strike==2?36:12,p.get("modelTicks").getAsInt());
        }
    }
    @TempDir Path dir;
    private Skill skill(String id){return new Skill(id,"Test","",Skill.Effect.ALLY_HEAL,"minecraft:stick",0xffffff,1,1,1,1,1,24,3,100);}
    private JsonObject packet(){var p=new JsonObject();var pos=new JsonArray();pos.add(0);pos.add(64);pos.add(0);p.add("from",pos);p.add("at",pos.deepCopy());return p;}
    @Test void everySkillHasAReachableModelPhaseAndStableIdentity(){
        assertEquals(40,BlockbenchVfx.skills().size());
        for(String id:BlockbenchVfx.skills()){
            boolean found=false;for(var stage:SkillPresentation.Stage.values())for(boolean loop:new boolean[]{false,true}){
                var p=packet();BlockbenchVfx.decorate(p,skill(id),stage,loop);assertTrue(p.has("model"));
                if(!p.get("model").getAsString().equals("-")){found=true;assertTrue(p.get("modelTicks").getAsInt()<=600);}
            }assertTrue(found,id);
        }
    }
    @Test void orisonAppearsOnImpactAndDoesNotReplacePreparationAura(){
        var p=packet();var s=skill("mago_bianco_orison");BlockbenchVfx.decorate(p,s,SkillPresentation.Stage.IMPACT,false);
        assertEquals("01_orison_di_cura:full",p.get("model").getAsString());assertEquals(32,p.get("modelTicks").getAsInt());
        p=packet();BlockbenchVfx.decorate(p,s,SkillPresentation.Stage.CAST,true);assertEquals("-",p.get("model").getAsString());
    }
    @Test void customHudWinsAndAutomaticRoleThenClassFallbackIsBundled()throws Exception{
        var settings=new HudSettings(dir.resolve("hud.json"));
        assertTrue(settings.resolve(List.of("default"),"default","arciere").text("texture").endsWith("/arciere.png"));
        assertTrue(settings.resolve(List.of("admin","arciere"),"arciere","arciere").text("texture").endsWith("/archangel.png"));
        settings.set("admin","texture","custom:textures/myhud.png");assertEquals("custom:textures/myhud.png",settings.resolve(List.of("admin"),"admin","arciere").text("texture"));
        settings.set("old","texture","castigo:textures/gui/hud/mago_nero.png");assertEquals(HudPresets.theme("mago_nero").json(),settings.theme("old").json());
    }
}
