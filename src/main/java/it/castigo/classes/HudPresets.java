package it.castigo.classes;
import com.google.gson.JsonObject;
import java.util.*;
public final class HudPresets {
    public static final List<String> NAMES=List.of("archangel","arcidemone","popolano","mago_bianco","mago_nero","guerriero_scudo","guerriero_due_mani","arciere","mago","criminale","guardia");
    public static HudTheme theme(String id){
        int n=NAMES.indexOf(id);if(n<0)throw new IllegalArgumentException("Preset inesistente");
        int[][] heads={{16,13},{11,13},{13,12},{16,14},{15,13},{13,13},{15,10},{10,13},{15,14},{12,11},{15,14}};
        int[][] bars={{19,134},{15,142},{13,147},{13,146},{15,142},{13,146},{13,146},{12,148},{16,140},{12,148},{16,140}};
        int[] textXs={44,40,40,43,41,38,43,36,41,39,41};
        int[][] bands={{39,47,55},{38,47,55},{37,46,54},{39,48,57},{37,46,54},{36,45,53},{36,46,55},{37,46,54},{38,46,54},{38,48,57},{39,47,55}};
        String[] colors={"DCC785","B87770","B59864","E7DEBC","B493CF","9BAEC3","CDA275","84AE73","99AED6","BA7675","B5C2D6"};
        var o=new JsonObject();o.addProperty("texture","castigoclasses:textures/gui/hud/"+id+".png");o.addProperty("frame","NONE");o.addProperty("border",colors[n]);
        o.addProperty("width",172);o.addProperty("height",64);o.addProperty("headX",heads[n][0]);o.addProperty("headY",heads[n][1]);o.addProperty("headSize",18);
        o.addProperty("textX",textXs[n]);o.addProperty("nameY",n==6?10:12);o.addProperty("classY",n==6?17:19);o.addProperty("groupY",n==6?24:26);o.addProperty("textScale",65);
        o.addProperty("barsX",bars[n][0]);o.addProperty("barsWidth",bars[n][1]);o.addProperty("barHeight",4);o.addProperty("barTextScale",50);
        o.addProperty("healthY",bands[n][0]);o.addProperty("resourceY",bands[n][1]);o.addProperty("xpY",bands[n][2]);return new HudTheme(o);
    }
}
