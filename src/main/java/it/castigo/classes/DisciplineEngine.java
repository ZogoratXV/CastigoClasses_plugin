package it.castigo.classes;

import it.castigo.classes.model.*;
import org.bukkit.*;
import org.bukkit.attribute.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import java.util.*;

/** One scheduler for preparations, finite statuses, zones, dashes and tracked arrows. */
public final class DisciplineEngine implements Listener {
    enum Kind { HOT, DOT, VULNERABLE, FEAR, HEAL_BLOCK, SLOW, GUARD, RECOVER, LINK, EXPOSED, STUDY }
    private record Source(UUID player,UUID world,String classId,long generation) {}
    private record Key(UUID target,Kind kind) {}
    private static final class Buff {
        final Key key;final Source source;final Skill skill;final double power,fraction;final long end;
        long next;
        Buff(Key key,Source source,Skill skill,double power,double fraction,long now) {
            this.key=key;this.source=source;this.skill=skill;this.power=power;this.fraction=fraction;
            end=now+skill.durationTicks();next=now+20;
        }
    }
    private record Preparation(Source source,Skill skill,double power,Location start,ItemStack weapon,long ready,int total,LivingEntity target) {}
    private static final class Sequence {
        final Source source;final Skill skill;final double power;final LivingEntity target;final Location at;
        int left;long next;
        Sequence(Source source,Skill skill,double power,LivingEntity target,Location at,int left,long next) {
            this.source=source;this.skill=skill;this.power=power;this.target=target;this.at=at;this.left=left;this.next=next;
        }
    }
    private record Zone(Source source,Skill skill,double power,Location center,long end) {}
    private static final class Dash {
        final Source source;final Skill skill;final double power;final Vector direction;double left;int ticks;
        Dash(Source source,Skill skill,double power,Vector direction) { this.source=source;this.skill=skill;this.power=power;this.direction=direction;left=skill.range(); }
    }
    private record Shot(Source source,Skill skill,double power,Location origin,long end,Arrow arrow,Location previous) {}
    private static final class Attempt { final LivingEntity target;final Player source;boolean accepted,blocked;Attempt(LivingEntity t,Player p){target=t;source=p;} }
    private record Hit(boolean accepted,double healthLost,boolean blocked) {}
    private final CastigoClasses plugin;
    private final SkillEngine engine;
    private final java.util.function.BiFunction<Player,Double,LivingEntity> targetRay;
    private final Map<UUID,Long> generations=new HashMap<>();
    private final Map<Key,Buff> buffs=new HashMap<>();
    private final Map<UUID,Preparation> preparing=new HashMap<>();
    private final Map<UUID,Sequence> sequences=new HashMap<>();
    private final Map<UUID,Dash> dashes=new HashMap<>();
    private final List<Zone> zones=new ArrayList<>();
    private final Map<UUID,Shot> shots=new HashMap<>();
    private final Map<UUID,Long> counters=new HashMap<>();
    private final Deque<Attempt> attempts=new ArrayDeque<>();
    private long tick;
    private boolean redirecting;
    public DisciplineEngine(CastigoClasses plugin,SkillEngine engine) { this(plugin,engine,DisciplineEngine::ray); }
    DisciplineEngine(CastigoClasses plugin,SkillEngine engine,java.util.function.BiFunction<Player,Double,LivingEntity> targetRay) {
        this.plugin=plugin;this.engine=engine;this.targetRay=targetRay;
    }
    public boolean busy(Player p) { UUID id=p.getUniqueId();return preparing.containsKey(id)||sequences.containsKey(id)||dashes.containsKey(id); }
    public com.google.gson.JsonObject casting(UUID id) {
        var result=new com.google.gson.JsonObject();var prep=preparing.get(id);
        if(prep!=null) { result.addProperty("name",prep.skill().name());result.addProperty("totalMs",prep.total()*50);result.addProperty("remainingMs",Math.max(0,prep.ready()-tick)*50); }
        if(prep!=null&&prep.target()!=null){
            var caster=resolve(prep.source());var target=prep.target();
            result.addProperty("targetName",target.getName());result.addProperty("range",prep.skill().range());
            if(caster!=null&&caster.getWorld().equals(target.getWorld()))result.addProperty("distance",caster.getLocation().distance(target.getLocation()));
        }
        return result;
    }
    private String interruption(Player p,Preparation prep){
        if(!equipped(p,prep.skill())||!p.getInventory().getItemInMainHand().isSimilar(prep.weapon()))return "equipaggiamento cambiato";
        if(p.getLocation().distanceSquared(prep.start())>0.25)return "ti sei mosso";
        var target=prep.target();if(target!=null){
            if(target.isDead()||!target.isValid())return "bersaglio non disponibile";
            if(!p.getWorld().equals(target.getWorld())||p.getLocation().distanceSquared(target.getLocation())>prep.skill().range()*prep.skill().range())return "bersaglio fuori portata";
            if(!p.equals(target)&&!p.hasLineOfSight(target))return "bersaglio non visibile";
        }
        return "azione non consentita qui o bersaglio non valido";
    }
    private boolean endPreparation(UUID id) {
        boolean removed=preparing.remove(id)!=null;
        if(removed) { Player p=Bukkit.getPlayer(id);if(p!=null) { plugin.weaponMotion(p,null,"stop",0);plugin.sync(p); } }
        return removed;
    }
    private Source source(Player p) { return new Source(p.getUniqueId(),p.getWorld().getUID(),plugin.profile(p).classId,generations.getOrDefault(p.getUniqueId(),0L)); }
    private Player resolve(Source source) {
        Player p=Bukkit.getPlayer(source.player());
        return p!=null&&p.isOnline()&&!p.isDead()&&p.getGameMode()!=GameMode.SPECTATOR&&plugin.profile(p)!=null
                &&p.getWorld().getUID().equals(source.world())&&plugin.profile(p).classId.equals(source.classId())
                &&generations.getOrDefault(p.getUniqueId(),0L)==source.generation()?p:null;
    }
    private SkillMechanics mechanics(Skill skill) { return plugin.catalog().mechanics(skill); }
    public boolean cast(Player p,Skill skill,double power) {
        var rules=mechanics(skill);
        if(!equipped(p,skill)) { plugin.feedback(p,"Equipaggiamento richiesto: "+rules.weapon());return false; }
        int arrows=DisciplineRules.arrows(skill.effect());
        if(arrows>0&&(ammo(p)<arrows||shots.size()+arrows>256)) { plugin.feedback(p,"Servono "+arrows+" frecce normali, oppure troppi proiettili attivi.");return false; }
        if(skill.effect()==Skill.Effect.COUNTER&&counters.getOrDefault(p.getUniqueId(),Long.MIN_VALUE)<tick) { plugin.feedback(p,"Serve una parata riuscita negli ultimi 2 secondi.");return false; }
        int delay=rules.preparationTicks();if(buff(p,Kind.FEAR)!=null)delay=Math.min(600,(int)Math.ceil(delay*1.5));
        if(delay>0) {
            LivingEntity selected=preparationTarget(p,skill);
            if(requiresTarget(skill.effect())&&selected==null)return fail(p,"Nessun bersaglio valido entro portata.");
            preparing.put(p.getUniqueId(),new Preparation(source(p),skill,power,p.getLocation().clone(),p.getInventory().getItemInMainHand().clone(),tick+delay,delay,selected));
            var preparation=preparing.get(p.getUniqueId());
            plugin.weaponMotion(p,skill,"prepare",delay);
            engine.loop(p,skill,SkillPresentation.Stage.CAST,p::getLocation,p.getUniqueId(),false,()->preparing.get(p.getUniqueId())==preparation&&resolve(preparation.source())!=null);
            if(selected!=null)engine.targetAura(p,skill,selected,()->preparing.get(p.getUniqueId())==preparation&&resolve(preparation.source())!=null&&validPreparedTarget(p,skill,selected));
            plugin.feedback(p,"Preparazione: "+skill.name()+" — resta fermo");return true;
        }
        return execute(p,skill,power);
    }
    public void tick() {
        tick++;
        for(var entry:List.copyOf(preparing.entrySet())) {
            var prep=entry.getValue();Player p=resolve(prep.source());
            boolean valid=p!=null&&DisciplineRules.prepared(true,true,equipped(p,prep.skill()),p.getLocation().distanceSquared(prep.start()))
                    &&p.getInventory().getItemInMainHand().isSimilar(prep.weapon())&&engine.allowed(p,p.getLocation())
                    &&(prep.target()==null||validPreparedTarget(p,prep.skill(),prep.target()));
            if(!valid) { endPreparation(entry.getKey());if(p!=null)plugin.feedback(p,"Preparazione interrotta: "+interruption(p,prep)+".");continue; }
            if(tick>=prep.ready()) { endPreparation(entry.getKey());if(!execute(p,prep.skill(),prep.power(),prep.target()))plugin.feedback(p,"Tecnica fallita: bersaglio o requisiti non più validi."); }
        }
        for(Buff b:List.copyOf(buffs.values())) {
            if(buffs.get(b.key)!=b)continue;
            LivingEntity target=living(b.key.target());Player caster=resolve(b.source);
            if(tick>b.end||target==null||target.isDead()||caster==null||!target.getWorld().equals(caster.getWorld())) { remove(b);continue; }
            if((b.key.kind()==Kind.GUARD)&&!equipped(caster,b.skill)) { remove(b);continue; }
            if(tick>=b.next) {
                b.next=tick+20;
                if(b.key.kind()==Kind.HOT) { if(ally(caster,target)&&caster.getLocation().distanceSquared(target.getLocation())<=b.skill.range()*b.skill.range()&&(caster.equals(target)||caster.hasLineOfSight(target))&&heal(caster,target,b.power))engine.pulse(caster,b.skill,SkillPresentation.Stage.HIT,target); }
                if(b.key.kind()==Kind.DOT&&engine.targetAllowed(caster,target)&&hit(caster,target,b.power).accepted())engine.pulse(caster,b.skill,SkillPresentation.Stage.HIT,target);
            }
        }
        if(tick%10==0)for(Zone zone:List.copyOf(zones))pulse(zone);
        for(var entry:List.copyOf(sequences.entrySet())) {
            Sequence seq=entry.getValue();Player p=resolve(seq.source);
            if(p==null||!equipped(p,seq.skill)||!engine.allowed(p,p.getLocation())) { sequences.remove(entry.getKey());continue; }
            if(tick<seq.next)continue;
            seq.next=tick+12;
            if(seq.skill.effect()==Skill.Effect.COMBO) {
                if(seq.target==null||!engine.targetAllowed(p,seq.target)||!p.hasLineOfSight(seq.target)||p.getLocation().distanceSquared(seq.target.getLocation())>seq.skill.range()*seq.skill.range()) { sequences.remove(entry.getKey());continue; }
                if(hit(p,seq.target,seq.power).accepted())engine.visual(p,seq.skill,seq.target);
                engine.castVisual(p,seq.skill);
            } else fire(p,seq.skill,seq.power,seq.at);
            if(--seq.left<=0)sequences.remove(entry.getKey());
        }
        for(var entry:List.copyOf(dashes.entrySet()))move(entry.getKey(),entry.getValue());
        for(var entry:List.copyOf(shots.entrySet())) {
            Shot shot=entry.getValue();Arrow arrow=shot.arrow();Player shooter=resolve(shot.source());
            if(shooter!=null&&arrow.isValid()&&arrow.getWorld().getUID().equals(shot.source().world())&&tick%2==0) {
                if(arrow.getLocation().distanceSquared(shot.previous())<=128*128)engine.trail(shooter,shot.skill(),shot.previous(),arrow.getLocation());
                shots.put(entry.getKey(),new Shot(shot.source(),shot.skill(),shot.power(),shot.origin(),shot.end(),arrow,arrow.getLocation().clone()));
            }
            if(tick>=shot.end()||!arrow.isValid()||arrow.isOnGround()||resolve(shot.source())==null||!arrow.getWorld().getUID().equals(shot.source().world())
                    ||arrow.getLocation().distanceSquared(shot.origin())>shot.skill().range()*shot.skill().range()) { arrow.remove();shots.remove(entry.getKey()); }
        }
        counters.entrySet().removeIf(e->e.getValue()<tick);
    }
    private boolean execute(Player p,Skill s,double power) {
        return execute(p,s,power,null);
    }
    private boolean execute(Player p,Skill s,double power,LivingEntity locked) {
        if(!equipped(p,s)||!engine.allowed(p,p.getLocation()))return false;
        if(!s.effect().discipline())return engine.executeLegacy(p,s,power,locked);
        LivingEntity target;Location center;
        switch(s.effect()) {
            case ALLY_HEAL,HOT,CLEANSE,LINK -> {
                target=locked!=null?locked:friend(p,s.range());if(target==null)return fail(p,"Mira a un giocatore oppure abbassati per selezionare te stesso.");
                if(s.effect()==Skill.Effect.ALLY_HEAL) { if(!heal(p,target,power))return fail(p,"Cura non possibile o vita già al massimo."); }
                if(s.effect()==Skill.Effect.HOT)put(p,target,Kind.HOT,s,power,0);
                if(s.effect()==Skill.Effect.CLEANSE)cleanse(target);
                if(s.effect()==Skill.Effect.LINK) {
                    if(target.equals(p)||buff(p,Kind.LINK)!=null||buffs.values().stream().anyMatch(b->b.key.kind()==Kind.LINK&&b.source.player().equals(target.getUniqueId())))return fail(p,"Vincolo non valido: scegli un altro alleato senza catene di protezione.");
                    put(p,target,Kind.LINK,s,0,mechanics(s).fraction());
                }
                engine.visual(p,s,target);
            }
            case REPULSE -> { for(LivingEntity e:engine.area(p,p.getLocation(),s.radius()))push(p,e,e.getLocation().toVector().subtract(p.getLocation().toVector()),0.8); }
            case SANCTUARY,RUIN,VORTEX -> {
                center=s.effect()==Skill.Effect.SANCTUARY?p.getLocation():ground(p,s.range());
                if(center==null||!engine.allowed(p,center)||zones.size()>=64)return fail(p,"Zona non disponibile: mira a una superficie libera.");
                zones.removeIf(z->z.source().player().equals(p.getUniqueId())&&z.skill().id().equals(s.id()));
                Zone zone=new Zone(source(p),s,power,center.clone(),tick+s.durationTicks());zones.add(zone);engine.visual(p,s,center);
                engine.loop(p,s,SkillPresentation.Stage.TELEGRAPH,zone::center,null,false,()->zones.contains(zone)&&tick<zone.end()&&resolve(zone.source())!=null);
            }
            case CURSED_BOLT,VULNERABILITY,DOT,FEAR,HEAL_BLOCK,DRAIN -> {
                target=locked!=null?locked:enemy(p,s.range());if(target==null)return fail(p,"Nessun nemico visibile entro portata.");
                Hit hit=hit(p,target,power);if(hit.accepted())switch(s.effect()) {
                    case CURSED_BOLT -> put(p,target,Kind.DOT,s,Math.max(0.5,power*0.2),0);
                    case VULNERABILITY -> put(p,target,Kind.VULNERABLE,s,0,mechanics(s).fraction());
                    case DOT -> put(p,target,Kind.DOT,s,power,0);
                    case FEAR -> { put(p,target,Kind.FEAR,s,0,0.2);interrupt(target); }
                    case HEAL_BLOCK -> put(p,target,Kind.HEAL_BLOCK,s,0,mechanics(s).fraction());
                    case DRAIN -> heal(p,p,hit.healthLost()*mechanics(s).fraction());
                    default -> { }
                }
                if(hit.accepted()) { engine.trail(p,s,p.getEyeLocation(),target.getEyeLocation());engine.visual(p,s,target); }
            }
            case GUARD,BULWARK -> put(p,p,Kind.GUARD,s,0,mechanics(s).fraction());
            case RECOVER -> {
                for(Kind kind:List.of(Kind.SLOW,Kind.FEAR)) { var b=buff(p,kind);if(b!=null)remove(b); }
                put(p,p,Kind.RECOVER,s,0,mechanics(s).fraction());
            }
            case DASH,HUNTER_STEP,DISENGAGE,CHARGE -> {
                Vector direction=p.getLocation().getDirection().setY(0);if(direction.lengthSquared()<0.001)return false;direction.normalize();
                if(s.effect()==Skill.Effect.DISENGAGE)direction.multiply(-1);
                if(!safe(p.getLocation().clone().add(direction.clone().multiply(0.8))))return fail(p,"Percorso bloccato o terreno non sicuro.");
                if(s.effect()==Skill.Effect.CHARGE)put(p,p,Kind.EXPOSED,s,0,0.15);
                if(s.effect()==Skill.Effect.DASH)put(p,p,Kind.GUARD,s,0,mechanics(s).fraction());
                dashes.put(p.getUniqueId(),new Dash(source(p),s,power,direction));
            }
            case STUDY -> {
                target=locked!=null?locked:enemy(p,s.range());if(target==null)return fail(p,"Nessun bersaglio visibile.");
                put(p,target,Kind.STUDY,s,0,mechanics(s).fraction());plugin.feedback(p,"Studiato: "+target.getName()+". Bonus ai tiri finché è visibile.");
            }
            case PRECISE_SHOT,HINDERING_SHOT,DOUBLE_SHOT,MASTER_SHOT,COVER_FIRE -> {
                int needed=DisciplineRules.arrows(s.effect());if(ammo(p)<needed||shots.size()+needed>256)return false;
                center=s.effect()==Skill.Effect.COVER_FIRE?ground(p,s.range()):locked!=null?locked.getEyeLocation():null;
                if(s.effect()==Skill.Effect.COVER_FIRE&&center==null)return fail(p,"Mira al terreno per il tiro di copertura.");
                consumeAmmo(p,needed);fire(p,s,power,center);
                if(needed>1)sequences.put(p.getUniqueId(),new Sequence(source(p),s,power,null,center,needed-1,tick+12));
            }
            case SWEEP,LOW_SWEEP -> {
                if(s.effect()==Skill.Effect.LOW_SWEEP)put(p,p,Kind.EXPOSED,s,0,0.15);
                for(LivingEntity e:engine.area(p,p.getLocation(),s.radius()))if(hit(p,e,power).accepted()) {
                    engine.visual(p,s,e);if(s.effect()==Skill.Effect.LOW_SWEEP)put(p,e,Kind.SLOW,s,0,0.45);
                }
            }
            case MELEE,SHIELD_BASH,COUNTER,HEAVY_STRIKE,LONG_THRUST,STOP_STRIKE,GUARD_BREAK,COMBO -> {
                if(s.effect()==Skill.Effect.HEAVY_STRIKE)put(p,p,Kind.EXPOSED,s,0,0.2);
                target=locked!=null?locked:enemy(p,s.range());if(target==null)return fail(p,"Il colpo non raggiunge un bersaglio valido.");
                if(s.effect()==Skill.Effect.COUNTER) { if(counters.getOrDefault(p.getUniqueId(),Long.MIN_VALUE)<tick)return false;counters.remove(p.getUniqueId()); }
                Hit strike=hit(p,target,power);
                if(strike.accepted()||(s.effect()==Skill.Effect.GUARD_BREAK&&strike.blocked()))switch(s.effect()) {
                    case SHIELD_BASH -> { push(p,target,target.getLocation().toVector().subtract(p.getLocation().toVector()),0.6);interrupt(target);put(p,target,Kind.SLOW,s,0,0.4); }
                    case STOP_STRIKE -> { interrupt(target);put(p,target,Kind.SLOW,s,0,0.5); }
                    case GUARD_BREAK -> {
                        put(p,target,Kind.VULNERABLE,s,0,mechanics(s).fraction());var guard=buff(target,Kind.GUARD);if(guard!=null)remove(guard);
                        if(target instanceof Player defender)defender.setCooldown(Material.SHIELD,s.durationTicks());
                    }
                    default -> { }
                }
                if(s.effect()==Skill.Effect.COMBO)sequences.put(p.getUniqueId(),new Sequence(source(p),s,power,target,null,2,tick+12));
                if(strike.accepted()||strike.blocked())engine.visual(p,s,target);
            }
            default -> { return false; }
        }
        if(DisciplineRules.arrows(s.effect())==0)engine.castVisual(p,s);
        return true;
    }
    private boolean fail(Player p,String message) { plugin.feedback(p,message);return false; }
    private static boolean friendly(Skill.Effect effect) {
        return switch(effect) { case ALLY_HEAL,HOT,CLEANSE,LINK -> true;default -> false; };
    }
    private static boolean requiresTarget(Skill.Effect effect) {
        return friendly(effect)||switch(effect) {
            case BOLT,LIGHTNING,CURSED_BOLT,VULNERABILITY,DOT,FEAR,HEAL_BLOCK,DRAIN,STUDY,MELEE,SHIELD_BASH,COUNTER,HEAVY_STRIKE,LONG_THRUST,STOP_STRIKE,GUARD_BREAK,COMBO -> true;
            default -> false;
        };
    }
    private LivingEntity preparationTarget(Player p,Skill skill) {
        if(friendly(skill.effect()))return friend(p,skill.range());
        if(requiresTarget(skill.effect())||switch(skill.effect()) { case PRECISE_SHOT,HINDERING_SHOT,DOUBLE_SHOT,MASTER_SHOT -> true;default -> false; })return enemy(p,skill.range());
        return null; // Ground areas and self-centered abilities do not select an entity.
    }
    private boolean validPreparedTarget(Player p,Skill skill,LivingEntity target) {
        return target.isValid()&&!target.isDead()&&p.getWorld().equals(target.getWorld())
                &&p.getLocation().distanceSquared(target.getLocation())<=skill.range()*skill.range()
                &&(target.equals(p)||p.hasLineOfSight(target))
                &&(friendly(skill.effect())?ally(p,target):engine.targetAllowed(p,target));
    }
    private boolean equipped(Player p,Skill skill) { return plugin.equipped(p,skill); }
    private LivingEntity living(UUID id) { Entity e=Bukkit.getEntity(id);return e instanceof LivingEntity l&&l.isValid()?l:null; }
    boolean ally(Player p,LivingEntity e) {
        if(!(e instanceof Player friend)||friend.isDead()||friend.getGameMode()==GameMode.SPECTATOR||!p.getWorld().equals(friend.getWorld())||!engine.allowed(p,friend.getLocation()))return false;
        return p.equals(friend)||p.canSee(friend);
    }
    private static LivingEntity ray(Player p,double range) {
        var result=p.getWorld().rayTrace(p.getEyeLocation(),p.getEyeLocation().getDirection(),range,FluidCollisionMode.NEVER,true,0.25,
                e->e instanceof LivingEntity&&!e.equals(p)&&!(e instanceof Player player&&player.getGameMode()==GameMode.SPECTATOR));
        return result!=null&&result.getHitEntity() instanceof LivingEntity e?e:null;
    }
    private LivingEntity enemy(Player p,double range) { LivingEntity e=targetRay.apply(p,range);return e!=null&&engine.targetAllowed(p,e)?e:null; }
    private LivingEntity friend(Player p,double range) {
        if(p.isSneaking())return p;
        LivingEntity e=targetRay.apply(p,range);return e==null?p:ally(p,e)?e:null;
    }
    private Location ground(Player p,double range) {
        var result=p.getWorld().rayTraceBlocks(p.getEyeLocation(),p.getEyeLocation().getDirection(),range,FluidCollisionMode.NEVER,true);
        return result==null?null:result.getHitPosition().toLocation(p.getWorld()).add(0,0.15,0);
    }
    private Hit hit(Player p,LivingEntity target,double power) {
        if(!engine.targetAllowed(p,target)||power<=0)return new Hit(false,0,false);
        Attempt attempt=new Attempt(target,p);attempts.push(attempt);double before=target.getHealth();
        try { target.damage(Math.min(10000,power),p);return new Hit(attempt.accepted,Math.max(0,before-target.getHealth()),attempt.blocked); }
        finally { attempts.pop(); }
    }
    boolean heal(Player p,LivingEntity target,double power) {
        if(!ally(p,target))return false;
        var attr=target.getAttribute(Attribute.MAX_HEALTH);if(attr==null)return false;
        double missing=attr.getValue()-target.getHealth();if(missing<=0||power<=0)return false;
        var event=new EntityRegainHealthEvent(target,Math.min(missing,power),EntityRegainHealthEvent.RegainReason.CUSTOM);
        plugin.prepareHeal(event,p);
        try{Bukkit.getPluginManager().callEvent(event);}finally{plugin.finishHeal(event);}
        if(event.isCancelled()||event.getAmount()<=0)return false;
        target.setHealth(Math.min(attr.getValue(),target.getHealth()+event.getAmount()));return true;
    }
    private Buff buff(Entity e,Kind kind) { Buff b=buffs.get(new Key(e.getUniqueId(),kind));return b!=null&&b.end>tick?b:null; }
    private void put(Player p,LivingEntity target,Kind kind,Skill s,double power,double fraction) {
        Key key=new Key(target.getUniqueId(),kind);if(buffs.size()>=4096&&!buffs.containsKey(key))return;
        Buff added=new Buff(key,source(p),s,power,fraction,tick);buffs.put(key,added);refresh(target);
        if(kind!=Kind.EXPOSED)engine.loop(p,s,SkillPresentation.Stage.TELEGRAPH,
                ()->target.getLocation().add(0,kind==Kind.LINK?1:0,0),target.getUniqueId(),kind==Kind.LINK,
                ()->buffs.get(key)==added&&tick<added.end&&target.isValid()&&!target.isDead()&&resolve(added.source)!=null&&target.getWorld().equals(p.getWorld()));
    }
    private void remove(Buff b) { if(buffs.remove(b.key,b)) { LivingEntity target=living(b.key.target());if(target!=null)refresh(target); } }
    private void refresh(LivingEntity target) {
        double slow=0,resistance=0;
        for(Kind kind:List.of(Kind.SLOW,Kind.FEAR,Kind.GUARD)) {
            Buff b=buff(target,kind);if(b!=null)slow=Math.max(slow,kind==Kind.GUARD?(b.skill.effect()==Skill.Effect.BULWARK?0.8:0.4):b.fraction);
        }
        Buff recovery=buff(target,Kind.RECOVER);if(recovery!=null)resistance=recovery.fraction;
        modifier(target,Attribute.MOVEMENT_SPEED,"discipline_slow",-slow,AttributeModifier.Operation.ADD_SCALAR);
        modifier(target,Attribute.KNOCKBACK_RESISTANCE,"discipline_resist",resistance,AttributeModifier.Operation.ADD_NUMBER);
    }
    private void modifier(LivingEntity target,Attribute type,String name,double value,AttributeModifier.Operation operation) {
        var attribute=target.getAttribute(type);if(attribute==null)return;
        var key=new NamespacedKey(plugin,name);var old=attribute.getModifier(key);if(old!=null)attribute.removeModifier(old);
        if(value!=0)attribute.addTransientModifier(new AttributeModifier(key,value,operation));
    }
    public double defenseFactor(UUID target) { Buff b=buffs.get(new Key(target,Kind.VULNERABLE));return b!=null&&b.end>tick?1-b.fraction:1; }
    private void cleanse(LivingEntity target) {
        for(Kind kind:List.of(Kind.DOT,Kind.VULNERABLE,Kind.FEAR,Kind.HEAL_BLOCK,Kind.SLOW)) { Buff b=buff(target,kind);if(b!=null)remove(b); }
        for(var effect:List.of(PotionEffectType.POISON,PotionEffectType.WITHER,PotionEffectType.SLOWNESS,PotionEffectType.WEAKNESS))target.removePotionEffect(effect);
    }
    private void pulse(Zone zone) {
        if(!zones.contains(zone))return;
        Player p=resolve(zone.source());Location at=zone.center();
        if(tick>=zone.end()||p==null||!at.getWorld().isChunkLoaded(at.getBlockX()>>4,at.getBlockZ()>>4)||!engine.allowed(p,at)) { zones.remove(zone);return; }
        if(zone.skill().effect()==Skill.Effect.SANCTUARY)return;
        for(LivingEntity target:engine.area(p,at,zone.skill().radius())) {
            if(zone.skill().effect()==Skill.Effect.RUIN&&tick%20==0)hit(p,target,zone.power());
            if(zone.skill().effect()==Skill.Effect.VORTEX)push(p,target,at.toVector().subtract(target.getLocation().toVector()),0.3);
        }
    }
    private boolean inSanctuary(Player caster,LivingEntity target,Zone zone) {
        return ally(caster,target)&&target.getLocation().distanceSquared(zone.center())<=zone.skill().radius()*zone.skill().radius()
                &&target.hasLineOfSight(caster)&&engine.allowed(caster,zone.center());
    }
    private void push(Player caster,LivingEntity target,Vector direction,double force) {
        if(!engine.targetAllowed(caster,target)||direction.lengthSquared()<0.01)return;
        var resistance=target.getAttribute(Attribute.KNOCKBACK_RESISTANCE);double reduced=force*(1-Math.min(1,resistance==null?0:resistance.getValue()));
        if(reduced<=0)return;
        direction.setY(0);if(direction.lengthSquared()<0.01)return;
        Vector velocity=direction.normalize().multiply(reduced).setY(0.12);target.setVelocity(velocity);
    }
    private boolean safe(Location at) {
        World w=at.getWorld();
        for(double x:new double[]{-0.3,0.3})for(double z:new double[]{-0.3,0.3}) {
            Location corner=at.clone().add(x,0,z);
            if(!w.isChunkLoaded(corner.getBlockX()>>4,corner.getBlockZ()>>4)||!w.getWorldBorder().isInside(corner))return false;
            for(double y:new double[]{0,0.9,1.8}) { var block=corner.clone().add(0,y,0).getBlock();if(!block.isPassable()||block.isLiquid()||block.getType()==Material.FIRE||block.getType()==Material.SOUL_FIRE)return false; }
        }
        Material floor=at.clone().add(0,-0.15,0).getBlock().getType();return floor.isSolid()&&floor!=Material.MAGMA_BLOCK&&floor!=Material.CACTUS;
    }
    private void move(UUID id,Dash dash) {
        Player p=resolve(dash.source);
        if(p==null||dash.left<=0||++dash.ticks>30||!equipped(p,dash.skill)) { stopDash(id,p);return; }
        double step=Math.min(0.65,dash.left);Location ahead=p.getLocation().clone().add(dash.direction.clone().multiply(step+0.3));
        if(!safe(ahead)||!engine.allowed(p,ahead)||!engine.allowed(p,p.getLocation())) { stopDash(id,p);return; }
        if(dash.skill.effect()==Skill.Effect.CHARGE) {
            var trace=p.getWorld().rayTrace(p.getEyeLocation(),dash.direction,1.6,FluidCollisionMode.NEVER,true,0.3,e->e instanceof LivingEntity&&!e.equals(p));
            if(trace!=null&&trace.getHitEntity() instanceof LivingEntity target) {
                if(engine.targetAllowed(p,target)&&hit(p,target,dash.power).accepted())engine.visual(p,dash.skill,target);stopDash(id,p);return;
            }
        }
        if(dash.ticks%2==0)engine.trail(p,dash.skill,p.getLocation().clone().subtract(dash.direction.clone().multiply(.7)),p.getLocation());
        p.setVelocity(dash.direction.clone().multiply(step).setY(Math.min(0,p.getVelocity().getY())));dash.left-=step;
    }
    private void stopDash(UUID id,Player p) { if(dashes.remove(id)!=null&&p!=null)p.setVelocity(new Vector(0,Math.min(0,p.getVelocity().getY()),0)); }
    private int ammo(Player p) { int total=0;for(var item:p.getInventory().getStorageContents())if(item!=null&&item.getType()==Material.ARROW)total+=item.getAmount();return total; }
    private void consumeAmmo(Player p,int count) {
        for(int slot=0;slot<p.getInventory().getStorageContents().length&&count>0;slot++) {
            var item=p.getInventory().getItem(slot);if(item==null||item.getType()!=Material.ARROW)continue;
            int used=Math.min(count,item.getAmount());count-=used;item.setAmount(item.getAmount()-used);p.getInventory().setItem(slot,item.getAmount()==0?null:item);
        }
    }
    private void fire(Player p,Skill skill,double power,Location at) {
        if(shots.size()>=256)return;
        Vector direction=at==null?p.getEyeLocation().getDirection():at.toVector().subtract(p.getEyeLocation().toVector());if(direction.lengthSquared()<0.01)return;
        Arrow arrow=p.getWorld().spawnArrow(p.getEyeLocation(),direction.normalize(),2.5f,skill.effect()==Skill.Effect.DOUBLE_SHOT?5:skill.effect()==Skill.Effect.COVER_FIRE?10:0);
        arrow.setShooter(p);arrow.setCritical(false);arrow.setDamage(Math.max(0.1,power/3));arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);arrow.setPersistent(false);
        var event=new EntityShootBowEvent(p,p.getInventory().getItemInMainHand(),new ItemStack(Material.ARROW),arrow,EquipmentSlot.HAND,1,false);
        Bukkit.getPluginManager().callEvent(event);
        if(event.isCancelled()||event.getProjectile()!=arrow) { arrow.remove();return; }
        shots.put(arrow.getUniqueId(),new Shot(source(p),skill,power,p.getEyeLocation().clone(),tick+100,arrow,p.getEyeLocation().clone()));
        engine.castVisual(p,skill);
    }
    private void interrupt(LivingEntity entity) {
        UUID id=entity.getUniqueId();boolean cancelled=endPreparation(id);cancelled|=sequences.remove(id)!=null;
        if(entity instanceof Player p) { stopDash(id,p);if(cancelled)plugin.feedback(p,"Tecnica interrotta."); }
    }
    public void clear(UUID id) {
        generations.merge(id,1L,Long::sum);endPreparation(id);sequences.remove(id);stopDash(id,Bukkit.getPlayer(id));counters.remove(id);
        for(Buff b:List.copyOf(buffs.values()))if(b.key.target().equals(id)||b.source.player().equals(id))remove(b);
        zones.removeIf(z->z.source().player().equals(id));
        for(var entry:List.copyOf(shots.entrySet()))if(entry.getValue().source().player().equals(id)) { entry.getValue().arrow().remove();shots.remove(entry.getKey()); }
    }
    public void shutdown() {
        for(Buff b:List.copyOf(buffs.values()))remove(b);
        for(var id:List.copyOf(dashes.keySet()))stopDash(id,Bukkit.getPlayer(id));
        shots.values().forEach(s->s.arrow().remove());shots.clear();for(var id:List.copyOf(preparing.keySet()))endPreparation(id);sequences.clear();zones.clear();counters.clear();generations.clear();
    }
    @EventHandler(priority=EventPriority.LOWEST,ignoreCancelled=true) public void damageInput(EntityDamageByEntityEvent e) {
        if(e.getDamager() instanceof Player p&&busy(p)&&attempts.isEmpty()) { e.setCancelled(true);return; }
        Shot shot=shots.get(e.getDamager().getUniqueId());
        if(shot!=null) {
            Player p=resolve(shot.source());
            if(p==null||!engine.allowed(p,p.getLocation())||!engine.targetAllowed(p,e.getEntity())||e.getEntity().getLocation().distanceSquared(shot.origin())>shot.skill().range()*shot.skill().range()) { e.setCancelled(true);return; }
            double damage=shot.power();Buff study=buff(e.getEntity(),Kind.STUDY);
            if(study!=null&&study.source.player().equals(p.getUniqueId())&&p.hasLineOfSight(e.getEntity()))damage*=1+study.fraction;
            if(shot.skill().effect()==Skill.Effect.MASTER_SHOT&&(buff(e.getEntity(),Kind.EXPOSED)!=null||buff(e.getEntity(),Kind.VULNERABLE)!=null))damage*=1.25;
            e.setDamage(damage);
        }
        Buff exposed=buff(e.getEntity(),Kind.EXPOSED);if(exposed!=null)e.setDamage(e.getDamage()*(1+exposed.fraction));
        Buff vulnerability=buff(e.getEntity(),Kind.VULNERABLE);
        if(vulnerability!=null&&(!(e.getEntity() instanceof Player p)||plugin.profile(p)==null))e.setDamage(e.getDamage()*(1+vulnerability.fraction));
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void protection(EntityDamageByEntityEvent e) {
        if(!(e.getEntity() instanceof LivingEntity victim))return;
        Buff guard=buff(victim,Kind.GUARD);double reduction=0;Player reductionOwner=null;
        if(guard!=null&&victim instanceof Player player&&equipped(player,guard.skill)&&front(victim,e.getDamager())){reduction=guard.fraction;reductionOwner=player;}
        for(Zone zone:zones) {
            if(zone.skill().effect()!=Skill.Effect.SANCTUARY||zone.end()<=tick)continue;
            Player caster=resolve(zone.source());if(caster!=null&&inSanctuary(caster,victim,zone)&&mechanics(zone.skill()).fraction()>reduction){reduction=mechanics(zone.skill()).fraction();reductionOwner=caster;}
        }
        if(reduction>0){double before=e.getFinalDamage();e.setDamage(e.getDamage()*(1-reduction));plugin.protectedDamage(e,reductionOwner,Math.max(0,before-e.getFinalDamage()));}
        if(redirecting) { e.setDamage(Math.min(e.getDamage(),Math.max(0,victim.getHealth()-1)));return; }
        Buff link=buff(victim,Kind.LINK);
        if(link==null||redirecting)return;
        Player protector=resolve(link.source);
        if(protector==null||!ally(protector,victim)||!protector.hasLineOfSight(victim)||!equipped(protector,link.skill)||protector.getLocation().distanceSquared(victim.getLocation())>link.skill.range()*link.skill.range()||!engine.allowed(protector,protector.getLocation()))return;
        double share=DisciplineRules.transferred(e.getDamage(),link.fraction,protector.getHealth());if(share<=0)return;
        // Never redirect back through another link; health loss confirms a non-cancelled transfer.
        double before=protector.getHealth();redirecting=true;
        try { protector.damage(share,e.getDamager()); }
        finally { redirecting=false; }
        if(protector.getHealth()<before){double original=e.getFinalDamage();e.setDamage(Math.max(0,e.getDamage()-share));plugin.protectedDamage(e,protector,Math.max(0,original-e.getFinalDamage()));}
    }
    private boolean front(LivingEntity target,Entity damager) {
        if(damager instanceof Projectile projectile&&projectile.getShooter() instanceof Entity shooter)damager=shooter;
        Vector delta=damager.getLocation().toVector().subtract(target.getLocation().toVector()).setY(0);
        Vector facing=target.getLocation().getDirection().setY(0);return delta.lengthSquared()>0.001&&facing.lengthSquared()>0.001&&delta.normalize().dot(facing.normalize())>=0.5;
    }
    @EventHandler(priority=EventPriority.MONITOR) public void damageObserved(EntityDamageByEntityEvent e) {
        boolean accepted=!e.isCancelled()&&e.getFinalDamage()>0;
        if(!attempts.isEmpty()) {
            Attempt a=attempts.peek();if(a.target.equals(e.getEntity())&&a.source.equals(e.getDamager())) {
                a.accepted=accepted;
                a.blocked=!e.isCancelled()&&e.isApplicable(EntityDamageEvent.DamageModifier.BLOCKING)&&e.getDamage(EntityDamageEvent.DamageModifier.BLOCKING)<0;
            }
        }
        Shot shot=shots.get(e.getDamager().getUniqueId());
        if(shot!=null&&accepted&&e.getEntity() instanceof LivingEntity target) {
            Player source=resolve(shot.source());
            if(source!=null)engine.visual(source,shot.skill(),target);
            if(source!=null&&(shot.skill().effect()==Skill.Effect.HINDERING_SHOT||shot.skill().effect()==Skill.Effect.COVER_FIRE))put(source,target,Kind.SLOW,shot.skill(),0,0.4);
        }
        if(!e.isCancelled()&&e.getEntity() instanceof Player p) {
            boolean blocked=e.isApplicable(EntityDamageEvent.DamageModifier.BLOCKING)&&e.getDamage(EntityDamageEvent.DamageModifier.BLOCKING)<0;
            if(blocked||(buff(p,Kind.GUARD)!=null&&front(p,e.getDamager())&&accepted)) {
                counters.put(p.getUniqueId(),tick+40);var guard=buff(p,Kind.GUARD);
                if(guard!=null)engine.pulse(p,guard.skill,SkillPresentation.Stage.HIT,p);
            }
        }
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void interruptOnDamage(EntityDamageEvent e) {
        if(e.getFinalDamage()>0&&e.getEntity() instanceof LivingEntity target)interrupt(target);
    }
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true) public void reduceHealing(EntityRegainHealthEvent e) {
        Buff block=buff(e.getEntity(),Kind.HEAL_BLOCK);if(block!=null)e.setAmount(DisciplineRules.healing(e.getAmount(),e.getAmount(),block.fraction));
    }
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true) public void vanillaBow(EntityShootBowEvent e) {
        if(e.getEntity() instanceof Player p&&preparing.containsKey(p.getUniqueId()))e.setCancelled(true);
    }
}
