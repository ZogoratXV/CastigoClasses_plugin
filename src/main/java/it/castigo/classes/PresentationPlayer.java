package it.castigo.classes;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Location;
import org.bukkit.Particle;

/** One bounded description per cue; all geometry, particles and audio run in the mod. */
public final class PresentationPlayer {
    private final CastigoClasses plugin;
    private long nextWarning;
    public PresentationPlayer(CastigoClasses plugin) { this.plugin=plugin; }
    public void play(SkillPresentation fx,SkillPresentation.Stage stage,Location from,Location at) {
        play(fx,stage,from,at,null);
    }
    public void play(SkillPresentation fx,SkillPresentation.Stage stage,Location from,Location at,java.util.UUID target) {
        if(fx==null||!fx.enabled()||at==null||at.getWorld()==null||!at.getWorld().isChunkLoaded(at.getBlockX()>>4,at.getBlockZ()>>4))return;
        var cue=fx.cues().get(stage);if(cue==null||!cue.enabled())return;
        if(!cue.hasMesh()&&!cue.particles().enabled()&&!cue.sound().enabled())return;
        try {
            JsonObject packet=packet(cue,from,at);
            if(target!=null)packet.addProperty("target",target.toString());
            plugin.broadcastEffect(from,at,packet);
        }
        catch(RuntimeException ex) {
            // A cosmetic failure must not refund a skill that has already dealt damage.
            if(System.currentTimeMillis()>=nextWarning) {
                nextWarning=System.currentTimeMillis()+60000;
                plugin.getLogger().warning("Effetto client non inviato: "+ex.getMessage());
            }
        }
    }
    public static JsonObject packet(SkillPresentation.Cue cue,Location from,Location at) {
        JsonObject o=new JsonObject();
        o.addProperty("world",at.getWorld().getUID().toString());
        o.addProperty("shape",cue.shape().name());o.addProperty("points",cue.points());
        o.addProperty("durationTicks",cue.durationTicks());o.addProperty("radius",cue.radius());
        if(cue.hasMesh())o.add("mesh",cue.mesh().json());
        o.add("from",position(from==null?at:from));o.add("at",position(at));
        JsonObject particles=new JsonObject();var p=cue.particles();
        particles.addProperty("enabled",p.enabled());particles.addProperty("id",p.particle().getKey().toString());
        particles.addProperty("count",p.count());particles.addProperty("spread",p.spread());
        if(p.data() instanceof Particle.DustOptions dust) {
            particles.addProperty("color",dust.getColor().asRGB());particles.addProperty("size",dust.getSize());
        }
        o.add("particles",particles);
        JsonObject audio=new JsonObject();var s=cue.sound();
        audio.addProperty("enabled",s.enabled());audio.addProperty("id",s.id());audio.addProperty("category",s.category().name());
        audio.addProperty("volume",s.volume());audio.addProperty("pitch",s.pitch());o.add("sound",audio);
        return o;
    }
    private static JsonArray position(Location p) {
        JsonArray a=new JsonArray();a.add(p.getX());a.add(p.getY());a.add(p.getZ());return a;
    }
}
