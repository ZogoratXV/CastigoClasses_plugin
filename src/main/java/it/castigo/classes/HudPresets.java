package it.castigo.classes;
import com.google.gson.JsonObject;
import java.util.*;
public final class HudPresets {
    public static final List<String> NAMES=List.of("archangel","arcidemone","popolano","mago_bianco","mago_nero","guerriero_scudo","guerriero_due_mani","arciere","mago","criminale","guardia");
    public static HudTheme theme(String id){
        int n=NAMES.indexOf(id);if(n<0)throw new IllegalArgumentException("Preset inesistente");
        int[][] bands={{39,47,55},{38,47,55},{37,46,54},{39,48,57},{37,46,54},{36,45,53},{36,46,55},{37,46,54},{38,46,54},{38,48,57},{39,47,55}};
        String[] colors={"DCC785","B87770","B59864","E7DEBC","B493CF","9BAEC3","CDA275","84AE73","99AED6","BA7675","B5C2D6"};
        var o=new JsonObject();o.addProperty("texture","castigo:textures/gui/hud/"+id+".png");o.addProperty("frame","NONE");o.addProperty("border",colors[n]);
        o.addProperty("width",172);o.addProperty("height",64);o.addProperty("headX",15);o.addProperty("headY",13);o.addProperty("headSize",18);
        o.addProperty("textX",41);o.addProperty("nameY",13);o.addProperty("classY",20);o.addProperty("groupY",27);o.addProperty("textScale",70);
        o.addProperty("barsX",15);o.addProperty("barsWidth",142);o.addProperty("barHeight",4);o.addProperty("barTextScale",50);
        o.addProperty("healthY",bands[n][0]);o.addProperty("resourceY",bands[n][1]);o.addProperty("xpY",bands[n][2]);return new HudTheme(o);
    }
}
