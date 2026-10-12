package it.castigo.classes;

import com.google.gson.*;
import it.castigo.classes.model.Skill;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Model identity travels with the skill, so inherited skills keep the same artwork. */
final class BlockbenchVfx {
    private static final JsonObject MODELS=load();
    private static JsonObject load(){try(var in=BlockbenchVfx.class.getResourceAsStream("/blockbench-models.json")){return JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(in),StandardCharsets.UTF_8)).getAsJsonObject();}catch(IOException ex){throw new UncheckedIOException(ex);}}
    static Set<String> skills(){return MODELS.keySet();}
    static void decorate(JsonObject packet,Skill skill,SkillPresentation.Stage stage,boolean persistent){
        if(skill==null||!MODELS.has(skill.id()))return;
        var entry=MODELS.getAsJsonArray(skill.id());String model=entry.get(0).getAsString();int n=Integer.parseInt(model.substring(0,2));
        boolean projectile=n==2||n==9||n==33||n>=37;
        boolean loop=Set.of(3,5,8,9,10,11,12,13,14,16,19,21,23,24,35).contains(n);
        String phase=null;
        if(persistent&&stage==SkillPresentation.Stage.TELEGRAPH&&loop)phase="loop";
        else if(!persistent){
            if(projectile&&stage==SkillPresentation.Stage.TRAIL)phase="flight";
            else if(projectile&&stage==SkillPresentation.Stage.IMPACT)phase="impact";
            else if(n==6&&stage==SkillPresentation.Stage.CAST)phase="full";
            else if(!loop&&n<=16&&n!=6&&stage==SkillPresentation.Stage.IMPACT)phase="full";
            else if(!loop&&n>=17&&!projectile&&stage==SkillPresentation.Stage.CAST)phase="full";
        }
        packet.addProperty("model",phase==null?"-":model+":"+phase);
        if(phase==null)return;
        int ticks=persistent?40:entry.get(1).getAsInt();
        if(phase.equals("impact")&&projectile)ticks=n==2?20:n==9?36:20;
        if(phase.equals("flight")){
            var from=packet.getAsJsonArray("from");var at=packet.getAsJsonArray("at");var facing=new JsonArray();double distance=0;
            for(int i=0;i<3;i++){double d=at.get(i).getAsDouble()-from.get(i).getAsDouble();facing.add(d);distance+=d*d;}
            if(distance>.0001)packet.add("direction",facing);
            ticks=distance>1&&n<17?8:2;
        }
        packet.addProperty("modelTicks",Math.clamp(ticks,1,600));
        if((n==2||n==9)&&phase.equals("impact"))packet.addProperty("modelDelay",8);
        if(n==5||n==21)packet.addProperty("modelAnchorHeight",0);
        if(projectile&&phase.equals("impact")&&packet.has("target")){
            var target=org.bukkit.Bukkit.getEntity(java.util.UUID.fromString(packet.get("target").getAsString()));
            packet.addProperty("modelAnchorHeight",target==null?1:Math.clamp(target.getHeight()*.6,.1,10));
        }
    }
}
