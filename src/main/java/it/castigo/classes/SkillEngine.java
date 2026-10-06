package it.castigo.classes;

import it.castigo.classes.model.*;
import it.castigo.core.Protection;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.potion.*;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import java.util.*;

public final class SkillEngine implements Listener {
    private record Ward(double remaining,long expires) {}
    private final CastigoClasses plugin;
    private final Map<UUID,Ward> wards=new HashMap<>();
    private LivingEntity damageTarget;
    private Player damageSource;
    private boolean acceptedDamage;
    public SkillEngine(CastigoClasses plugin) { this.plugin=plugin; }
    public void clear(UUID id) { wards.remove(id); }

    public void cast(Player p,Skill skill) {
        Profile data=plugin.profile(p); long now=System.currentTimeMillis();
        if(skill==null||data==null||p.isDead()||p.getGameMode()==GameMode.SPECTATOR)return;
        if(!Protection.interact(p,p.getLocation())) { plugin.feedback(p,"Non puoi usare abilità qui.");return; }
        switch(AbilityRules.check(data,skill,now)) {
            case LOCKED -> { plugin.feedback(p,"Abilità sbloccata al livello "+skill.unlockLevel());return; }
            case GLOBAL_COOLDOWN -> { return; }
            case COOLDOWN -> { plugin.feedback(p,"Ricarica: "+String.format(Locale.ITALIAN,"%.1f",(data.cooldowns.get(skill.id())-now)/1000.0)+" s");return; }
            case NO_RESOURCE -> { plugin.feedback(p,plugin.definition(data).resourceName()+" insufficiente");return; }
            case READY -> { }
        }
        double power=skill.power()+plugin.stats(p).intelligence()*skill.intelligenceScale();
        double previousResource=data.resource;long previousGlobal=data.globalReadyAt;
        Long previousCooldown=data.cooldowns.get(skill.id());
        AbilityRules.commit(data,skill,now,plugin.getConfig().getLong("combat.global-cooldown-ms",350));
        boolean accepted=false;
        try { accepted=execute(p,skill,power); }
        finally {
            if(!accepted) {
                data.resource=previousResource;data.globalReadyAt=previousGlobal;
                if(previousCooldown==null)data.cooldowns.remove(skill.id());else data.cooldowns.put(skill.id(),previousCooldown);
            }
            plugin.sync(p);
        }
    }
    private boolean targetAllowed(Player p,Entity target) {
        if(!(target instanceof LivingEntity living)||target.equals(p)||living.isDead()||target instanceof ArmorStand||target.isInvulnerable()||target.hasMetadata("NPC"))return false;
        if(target instanceof Tameable tame&&tame.isTamed())return false;
        if(target instanceof Player other) {
            if(!plugin.getConfig().getBoolean("combat.pvp",false)||!p.getWorld().getPVP()||other.getGameMode()==GameMode.CREATIVE||other.getGameMode()==GameMode.SPECTATOR)return false;
            var team=p.getScoreboard().getEntryTeam(p.getName());
            if(team!=null&&!team.allowFriendlyFire()&&team.hasEntry(other.getName()))return false;
        }
        return Protection.interact(p,target.getLocation());
    }
    private RayTraceResult ray(Player p,double range) {
        return p.getWorld().rayTrace(p.getEyeLocation(),p.getEyeLocation().getDirection(),range,FluidCollisionMode.NEVER,true,0.3,e->targetAllowed(p,e));
    }
    private Location point(Player p,Skill skill) {
        RayTraceResult ray=ray(p,skill.range());
        return ray==null?p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(skill.range())):ray.getHitPosition().toLocation(p.getWorld());
    }
    private void trail(Location from,Location to,int color) {
        double distance=from.distance(to); int steps=Math.max(1,(int)(distance*3));
        Vector step=to.toVector().subtract(from.toVector()).multiply(1.0/steps);
        Location cursor=from.clone();
        for(int i=0;i<=steps;i++) { dust(cursor,color,1);cursor.add(step); }
    }
    private void dust(Location at,int color,int count) {
        at.getWorld().spawnParticle(Particle.DUST,at,count,0.08,0.08,0.08,0,new Particle.DustOptions(Color.fromRGB(color),1.2f));
    }
    private void ring(Location at,double radius,int color) {
        for(int i=0;i<48;i++) { double angle=i*Math.PI/24;dust(at.clone().add(Math.cos(angle)*radius,0.15,Math.sin(angle)*radius),color,1); }
    }
    private List<LivingEntity> area(Player p,Location center,double radius) {
        List<LivingEntity> result=new ArrayList<>();
        for(Entity e:center.getWorld().getNearbyEntities(center,radius,radius,radius)) {
            if(!targetAllowed(p,e)||e.getLocation().distanceSquared(center)>radius*radius)continue;
            LivingEntity living=(LivingEntity)e;
            Location origin=center.clone().add(0,0.25,0);Vector delta=living.getEyeLocation().toVector().subtract(origin.toVector());
            double distance=delta.length();
            if(distance<0.1||center.getWorld().rayTraceBlocks(origin,delta.normalize(),distance,FluidCollisionMode.NEVER,true)==null)result.add(living);
        }
        return result;
    }
    private boolean execute(Player p,Skill s,double power) {
        Location from=p.getEyeLocation();
        switch(s.effect()) {
            case BOLT,LIGHTNING -> {
                RayTraceResult hit=ray(p,s.range());
                if(hit==null||!(hit.getHitEntity() instanceof LivingEntity target)) { plugin.feedback(p,"Nessun bersaglio valido nella linea di mira.");return false; }
                trail(from,hit.getHitPosition().toLocation(p.getWorld()),s.color());
                if(s.effect()==Skill.Effect.LIGHTNING) {
                    p.getWorld().strikeLightningEffect(target.getLocation());
                    p.getWorld().playSound(target.getLocation(),Sound.ENTITY_LIGHTNING_BOLT_IMPACT,0.6f,1.5f);
                } else p.getWorld().playSound(from,Sound.BLOCK_AMETHYST_BLOCK_CHIME,0.8f,1.5f);
                target.damage(power,p);
            }
            case FIREBALL -> {
                Location center=point(p,s);
                if(!Protection.interact(p,center))return false;
                trail(from,center,s.color());
                center.getWorld().spawnParticle(Particle.FLAME,center,45,0.7,0.7,0.7,0.06);
                center.getWorld().playSound(center,Sound.ENTITY_GENERIC_EXPLODE,0.6f,1.3f);
                for(LivingEntity target:area(p,center,s.radius()))target.damage(power,p);
            }
            case FROST_NOVA -> {
                Location center=p.getLocation(); ring(center,s.radius(),s.color());
                center.getWorld().spawnParticle(Particle.SNOWFLAKE,center.clone().add(0,1,0),70,s.radius()/2,0.4,s.radius()/2,0.02);
                for(LivingEntity target:area(p,center,s.radius())) {
                    if(damageAccepted(target,p,power))
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,s.durationTicks(),1));
                }
                p.getWorld().playSound(center,Sound.BLOCK_GLASS_BREAK,0.8f,0.6f);
            }
            case BLINK -> {
                Vector direction=p.getLocation().getDirection().setY(0);
                if(direction.lengthSquared()<0.001)return false;
                direction.normalize();Location start=p.getLocation(),best=null;
                for(double distance=0.5;distance<=s.range();distance+=0.5) {
                    Location candidate=start.clone().add(direction.clone().multiply(distance));
                    if(!candidate.getWorld().isChunkLoaded(candidate.getBlockX()>>4,candidate.getBlockZ()>>4)||!candidate.getWorld().getWorldBorder().isInside(candidate))break;
                    if(!candidate.getBlock().isPassable()||!candidate.clone().add(0,1,0).getBlock().isPassable())break;
                    if(candidate.getBlock().isLiquid()||candidate.clone().add(0,1,0).getBlock().isLiquid()||candidate.getBlock().getType()==Material.FIRE||candidate.getBlock().getType()==Material.SOUL_FIRE)break;
                    Material ground=candidate.clone().add(0,-0.1,0).getBlock().getType();
                    if(ground.isSolid()&&ground!=Material.MAGMA_BLOCK&&ground!=Material.CACTUS&&Protection.interact(p,candidate))best=candidate;
                }
                if(best==null||best.distanceSquared(start)<1||!p.teleport(best,org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN)) {
                    plugin.feedback(p,"Nessuna destinazione sicura.");return false;
                }
                trail(start.clone().add(0,1,0),best.clone().add(0,1,0),s.color());
                p.getWorld().playSound(best,Sound.ENTITY_ENDERMAN_TELEPORT,0.6f,1.5f);
            }
            case WARD -> {
                wards.put(p.getUniqueId(),new Ward(power,System.currentTimeMillis()+s.durationTicks()*50L));
                ring(p.getLocation(),1.2,s.color());p.getWorld().playSound(p.getLocation(),Sound.BLOCK_BEACON_ACTIVATE,0.6f,1.5f);
            }
            case HEAL -> {
                double missing=p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()-p.getHealth();
                if(missing<=0) { plugin.feedback(p,"La vita è già al massimo.");return false; }
                EntityRegainHealthEvent event=new EntityRegainHealthEvent(p,Math.min(missing,power),EntityRegainHealthEvent.RegainReason.CUSTOM);
                Bukkit.getPluginManager().callEvent(event);if(event.isCancelled())return false;
                p.setHealth(Math.min(p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue(),p.getHealth()+Math.max(0,event.getAmount())));
                p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,p.getLocation().add(0,1,0),20,0.5,0.8,0.5,0);
                p.getWorld().playSound(p.getLocation(),Sound.BLOCK_ENCHANTMENT_TABLE_USE,0.7f,1.3f);
            }
            case METEOR -> {
                var block=p.getWorld().rayTraceBlocks(from,from.getDirection(),s.range(),FluidCollisionMode.NEVER,true);
                if(block==null) { plugin.feedback(p,"Mira a una superficie entro "+(int)s.range()+" blocchi.");return false; }
                Location center=block.getHitPosition().toLocation(p.getWorld()).add(0,0.1,0);
                if(!Protection.interact(p,center))return false;
                ring(center,s.radius(),s.color());String classId=plugin.profile(p).classId;
                plugin.getServer().getScheduler().runTaskLater(plugin,()-> {
                    if(!p.isOnline()||p.isDead()||plugin.profile(p)==null||!plugin.profile(p).classId.equals(classId)||!p.getWorld().equals(center.getWorld())||!center.getWorld().isChunkLoaded(center.getBlockX()>>4,center.getBlockZ()>>4)||!Protection.interact(p,center))return;
                    trail(center.clone().add(0,10,0),center,s.color());
                    center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER,center,1);
                    center.getWorld().playSound(center,Sound.ENTITY_GENERIC_EXPLODE,0.9f,0.65f);
                    for(LivingEntity target:area(p,center,s.radius()))target.damage(power,p);
                },20);
            }
        }
        return true;
    }
    private boolean damageAccepted(LivingEntity target,Player source,double amount) {
        damageTarget=target;damageSource=source;acceptedDamage=false;
        try { target.damage(amount,source);return acceptedDamage; }
        finally { damageTarget=null;damageSource=null; }
    }
    @EventHandler(priority=EventPriority.MONITOR) public void observeDamage(EntityDamageByEntityEvent e) {
        if(e.getEntity().equals(damageTarget)&&e.getDamager().equals(damageSource))acceptedDamage=!e.isCancelled()&&e.getFinalDamage()>0;
    }
    @EventHandler(ignoreCancelled=true,priority=EventPriority.HIGHEST) public void absorb(EntityDamageEvent e) {
        if(!(e.getEntity() instanceof Player p)||e.getCause()==EntityDamageEvent.DamageCause.VOID)return;
        Ward ward=wards.get(p.getUniqueId());if(ward==null)return;
        if(ward.expires()<System.currentTimeMillis()) { wards.remove(p.getUniqueId());return; }
        double absorbed=Math.min(e.getDamage(),ward.remaining());e.setDamage(e.getDamage()-absorbed);
        if(absorbed>=ward.remaining())wards.remove(p.getUniqueId());else wards.put(p.getUniqueId(),new Ward(ward.remaining()-absorbed,ward.expires()));
        dust(p.getLocation().add(0,1,0),0x7799FF,12);
    }
}
