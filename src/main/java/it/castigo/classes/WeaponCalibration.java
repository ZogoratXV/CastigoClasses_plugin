package it.castigo.classes;
import com.google.gson.*;
import it.castigo.core.CoreApi;
import java.nio.file.*;
import java.util.*;
public final class WeaponCalibration {
    public static final List<String> PROFILES=List.of("AUTO","VANILLA","SWORD","TWO_HANDED","AXE","THRUST","SHIELD","BOW","CAST");
    public static String profileLabel(String id){return switch(id){case "AUTO"->"Automatico";case "VANILLA"->"Movimenti vanilla";case "SWORD"->"Spada a una mano";case "TWO_HANDED"->"Spada a due mani";case "AXE"->"Arma pesante";case "THRUST"->"Affondo";case "SHIELD"->"Scudo";case "BOW"->"Arco";case "CAST"->"Lancio magico";default->id;};}
    public static void validateProfile(String value){if(!PROFILES.contains(value))throw new IllegalArgumentException("Profilo animazione sconosciuto");}
    public static final List<String> FIELDS=List.of("firstX","firstY","firstZ","firstPitch","firstYaw","firstRoll","supportX","supportY","supportZ","mainPitch","mainYaw","mainRoll","supportPitch","supportYaw","supportRoll","firstIntensity","thirdIntensity");
    private final Path file;private JsonObject data=new JsonObject();
    public WeaponCalibration(Path file)throws Exception{this.file=file;if(Files.exists(file)){if(Files.size(file)>524288)throw new IllegalArgumentException("File pose troppo grande");data=JsonParser.parseString(Files.readString(file)).getAsJsonObject();if(data.size()>256)throw new IllegalArgumentException("Massimo 256 pose");for(var e:data.entrySet()){SkillEquipment.validateItem(e.getKey());for(var v:e.getValue().getAsJsonObject().entrySet()){if(v.getKey().equals("profile"))validateProfile(v.getValue().getAsString());else validate(v.getKey(),v.getValue().getAsDouble());}}}}
    public JsonObject get(String id){return data.has(id)?data.getAsJsonObject(id).deepCopy():new JsonObject();}
    public static void validate(String key,double value){if(!FIELDS.contains(key)||!Double.isFinite(value))throw new IllegalArgumentException("Valore posa non valido");double max=key.endsWith("Intensity")?2:key.endsWith("X")||key.endsWith("Y")||key.endsWith("Z")?2:180;double min=key.endsWith("Intensity")?0:-max;if(value<min||value>max)throw new IllegalArgumentException("Valore da "+min+" a "+max);}
    public void set(String id,String field,String value)throws Exception{SkillEquipment.validateItem(id);var next=data.deepCopy();if(field.equals("reset"))next.remove(id);else if(field.equals("profile")){validateProfile(value);if(!next.has(id)&&next.size()>=256)throw new IllegalArgumentException("Massimo 256 pose");var o=get(id);o.addProperty(field,value);next.add(id,o);}else {double n=Double.parseDouble(value);validate(field,n);if(!next.has(id)&&next.size()>=256)throw new IllegalArgumentException("Massimo 256 pose");var o=get(id);o.addProperty(field,n);next.add(id,o);}CoreApi.atomicWrite(file,next.toString());data=next;}
}
