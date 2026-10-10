package it.castigo.classes;

import com.google.gson.*;
import java.util.*;

/** Portable HUD layout in GUI pixels; textures are identifiers in the client's resource pack. */
public final class HudTheme {
    public static final Map<String,Object> DEFAULTS;
    static {
        var d=new LinkedHashMap<String,Object>();
        d.put("texture","");d.put("frame","MEDIEVAL");d.put("background","191510");d.put("border","B49350");
        d.put("text","F3E4C5");d.put("healthColor","A5343E");d.put("resourceColor","class");
        d.put("width",172);d.put("height",64);d.put("headX",8);d.put("headY",8);d.put("headSize",24);
        d.put("textX",40);d.put("nameY",7);d.put("classY",18);d.put("groupY",28);
        d.put("barsX",6);d.put("healthY",39);d.put("resourceY",50);d.put("barsWidth",160);d.put("xpY",60);
        d.put("textScale",100);d.put("barTextScale",100);d.put("barHeight",8);
        DEFAULTS=Collections.unmodifiableMap(d);
    }
    private final JsonObject values;
    public HudTheme(JsonObject input) {
        values=new Gson().toJsonTree(DEFAULTS).getAsJsonObject();
        if(input!=null)for(var e:input.entrySet()) {
            String key=e.getKey();if(!DEFAULTS.containsKey(key))throw new IllegalArgumentException("Campo HUD sconosciuto: "+key);
            if(!e.getValue().isJsonPrimitive())throw new IllegalArgumentException("Valore HUD non valido");
            String value=e.getValue().getAsString();
            if(DEFAULTS.get(key) instanceof Integer) {
                if(!value.matches("[0-9]{1,3}"))throw new IllegalArgumentException("Serve un numero intero");
                int n=Integer.parseInt(value),min=0,max=400;
                if(key.equals("width")){min=100;max=400;}else if(key.equals("height")){min=60;max=240;}
                else if(key.equals("headSize")){min=8;max=64;}else if(key.equals("barsWidth")){min=20;max=380;}
                else if(key.equals("textScale")||key.equals("barTextScale")){min=50;max=100;}else if(key.equals("barHeight")){min=4;max=12;}
                if(n<min||n>max)throw new IllegalArgumentException(key+": da "+min+" a "+max);
                values.addProperty(key,n);
            } else {
                if(key.equals("texture")) {
                    if(value.startsWith("texture:"))value=value.substring(8);
                    if(!value.isEmpty()&&(value.length()>200||!value.matches("[a-z0-9_.-]+:textures/[a-z0-9_./-]+\\.png")||value.contains("..")))throw new IllegalArgumentException("Usa namespace:textures/gui/hud/nome.png");
                } else if(key.equals("frame")) {
                    value=value.toUpperCase(Locale.ROOT);if(!Set.of("MEDIEVAL","FLAT","NONE").contains(value))throw new IllegalArgumentException("Cornice: MEDIEVAL, FLAT, NONE");
                } else if(!(key.equals("resourceColor")&&value.equals("class"))) {
                    value=value.replace("#","").toUpperCase(Locale.ROOT);if(!value.matches("[0-9A-F]{6}"))throw new IllegalArgumentException("Colore RGB: per esempio FFD700");
                }
                values.addProperty(key,value);
            }
        }
    }
    public String text(String key){return values.get(key).getAsString();}
    public int number(String key){return values.get(key).getAsInt();}
    public int color(String key,int fallback){return text(key).equals("class")?fallback:0xff000000|Integer.parseInt(text(key),16);}
    public JsonObject json(){return values.deepCopy();}
    public HudTheme with(String key,String value){var copy=json();copy.addProperty(key,value);return new HudTheme(copy);}
}
