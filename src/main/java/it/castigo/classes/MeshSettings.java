package it.castigo.classes;

import com.google.gson.*;
import java.util.Set;

/** Bounded, portable settings. Kept identical to the server-side schema. */
public record MeshSettings(double height,double opacity,double rotation,double scroll,double fadeIn,double fadeOut,
                           int rings,double ringGap,double columnRadius,String ringTexture,String columnTexture,String tint) {
    public static final MeshSettings DEFAULT=new MeshSettings(2.8,.65,90,.65,.15,.3,2,.38,.42,
            "castigoclasses:textures/vfx/rune_ring.png","castigoclasses:textures/vfx/healing_column.png","auto");
    private static final Set<String> KEYS=Set.of("height","opacity","rotation","scroll","fadeIn","fadeOut","rings","ringGap","columnRadius","ringTexture","columnTexture","tint");
    public static MeshSettings read(JsonObject o) {
        if(o==null)return DEFAULT;
        if(!KEYS.containsAll(o.keySet()))throw new IllegalArgumentException("Campo mesh sconosciuto");
        double rings=n(o,"rings",2,0,4);
        if(rings!=Math.rint(rings))throw new IllegalArgumentException("Numero anelli intero richiesto");
        String tint=o.has("tint")?o.get("tint").getAsString():"auto";
        if(!tint.equals("auto")&&!tint.matches("[0-9a-fA-F]{6}"))throw new IllegalArgumentException("Colore mesh: RGB o auto");
        var result=new MeshSettings(n(o,"height",2.8,.1,12),n(o,"opacity",.65,0,1),n(o,"rotation",90,-720,720),
                n(o,"scroll",.65,-4,4),n(o,"fadeIn",.15,0,.5),n(o,"fadeOut",.3,0,.5),(int)rings,
                n(o,"ringGap",.38,0,3),n(o,"columnRadius",.42,.05,1),
                texture(o,"ringTexture",DEFAULT.ringTexture),texture(o,"columnTexture",DEFAULT.columnTexture),tint);
        return result;
    }
    private static double n(JsonObject o,String key,double fallback,double min,double max) {
        if(!o.has(key))return fallback;
        var v=o.get(key);if(!v.isJsonPrimitive()||!v.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("Numero richiesto: "+key);
        double n=v.getAsDouble();if(!Double.isFinite(n)||n<min||n>max)throw new IllegalArgumentException("Mesh: "+key+" fuori limite");return n;
    }
    private static String texture(JsonObject o,String key,String fallback) {
        if(!o.has(key))return fallback;
        var v=o.get(key);if(!v.isJsonPrimitive()||!v.getAsJsonPrimitive().isString())throw new IllegalArgumentException("Texture non valida");
        String id=v.getAsString();
        if(id.length()>160||!id.matches("[a-z0-9_.-]+:textures/[a-z0-9/_.-]+\\.png")||id.contains(".."))throw new IllegalArgumentException("ID texture non valido");return id;
    }
    public JsonObject json() { return new Gson().toJsonTree(this).getAsJsonObject(); }
    /** Fractions of lifetime, independent of frame rate. */
    public double alpha(double life) {
        if(life<0||life>=1)return 0;
        double a=fadeIn==0?1:Math.min(1,life/fadeIn),b=fadeOut==0?1:Math.min(1,(1-life)/fadeOut);
        return opacity*a*b;
    }
}

