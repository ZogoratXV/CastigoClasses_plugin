package it.castigo.classes;

import java.util.*;
import java.util.function.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import it.castigo.classes.model.Skill;

/** Server-owned visual lifetimes. No tasks per effect and no client gameplay authority. */
final class VfxLoops {
    private record Loop(UUID id,UUID world,Player owner,Skill skill,SkillPresentation.Stage stage,
                        Supplier<Location> from,Supplier<Location> at,UUID target,boolean link,BooleanSupplier valid,long next,SkillPresentation.Cue custom) {
        Loop next(long time){return new Loop(id,world,owner,skill,stage,from,at,target,link,valid,time,custom);}
    }
    private final CastigoClasses plugin;
    private final Map<UUID,Loop> loops=new LinkedHashMap<>();
    private long tick;
    private long nextWarning;
    VfxLoops(CastigoClasses plugin){this.plugin=plugin;}
    void start(Player owner,Skill skill,SkillPresentation.Stage stage,Supplier<Location> from,Supplier<Location> at,UUID target,boolean link,BooleanSupplier valid) {
        start(owner,skill,stage,from,at,target,link,valid,null);
    }
    void start(Player owner,Skill skill,SkillPresentation.Stage stage,Supplier<Location> from,Supplier<Location> at,UUID target,boolean link,BooleanSupplier valid,SkillPresentation.Cue custom) {
        if(loops.size()>=512||!valid.getAsBoolean())return;
        var fx=plugin.presentation(owner,skill);if(fx==null||!fx.enabled())return;
        var cue=custom==null?fx.cues().get(stage):custom;
        if(cue==null||!cue.enabled()||!cue.hasMesh())return;
        var loop=new Loop(UUID.randomUUID(),owner.getWorld().getUID(),owner,skill,stage,from,at,target,link,valid,tick+20,custom);
        loops.put(loop.id,loop);safeEmit(loop);
    }
    void tick() {
        tick++;
        for(var loop:List.copyOf(loops.values())) {
            if(!loop.valid.getAsBoolean()) { loops.remove(loop.id);plugin.stopEffect(loop.world,loop.id);continue; }
            if(tick>=loop.next) { safeEmit(loop);loops.put(loop.id,loop.next(tick+20)); }
        }
    }
    private void safeEmit(Loop loop) {
        try { emit(loop); }
        catch(RuntimeException e) {
            if(tick>=nextWarning) { nextWarning=tick+1200;plugin.getLogger().warning("VFX persistente ignorato: "+e.getMessage()); }
        }
    }
    private void emit(Loop loop) {
        var fx=plugin.presentation(loop.owner,loop.skill);if(fx==null){plugin.stopEffect(loop.world,loop.id);return;}
        var cue=loop.custom==null?fx.cues().get(loop.stage):loop.custom;
        if(!fx.enabled()||cue==null||!cue.enabled()) { plugin.stopEffect(loop.world,loop.id);return; }
        var from=loop.from.get();var at=loop.at.get();
        if(from==null||at==null||!from.getWorld().equals(at.getWorld())||!at.getWorld().isChunkLoaded(at.getBlockX()>>4,at.getBlockZ()>>4))return;
        if(loop.link) {
            var target=Bukkit.getEntity(loop.target);
            if(target==null||!loop.owner.hasLineOfSight(target)||!plugin.equipped(loop.owner,loop.skill)||loop.owner.getLocation().distanceSquared(target.getLocation())>loop.skill.range()*loop.skill.range()) {
                plugin.stopEffect(loop.world,loop.id);return;
            }
        }
        else from=at.clone();
        var packet=PresentationPlayer.packet(cue,from,at);packet.addProperty("handle",loop.id.toString());
        packet.addProperty("durationTicks",40);packet.getAsJsonObject("sound").addProperty("enabled",false);
        packet.getAsJsonObject("particles").addProperty("enabled",false);
        PresentationPlayer.target(packet,loop.target);
        if(loop.link) { packet.addProperty("source",loop.owner.getUniqueId().toString());packet.addProperty("anchorHeight",1); }
        plugin.broadcastEffect(from,at,packet);
    }
    void clear(){for(var loop:loops.values())plugin.stopEffect(loop.world,loop.id);loops.clear();}
}
