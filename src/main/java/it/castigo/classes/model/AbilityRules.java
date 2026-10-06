package it.castigo.classes.model;

/** Server-authoritative eligibility rules, independent from Bukkit effects. */
public final class AbilityRules {
    public enum Result { READY, LOCKED, GLOBAL_COOLDOWN, COOLDOWN, NO_RESOURCE }
    private AbilityRules() {}
    public static Result check(Profile p,Skill s,long now) {
        if(p.level<s.unlockLevel())return Result.LOCKED;
        if(p.globalReadyAt>now)return Result.GLOBAL_COOLDOWN;
        if(p.cooldowns.getOrDefault(s.id(),0L)>now)return Result.COOLDOWN;
        if(!Double.isFinite(p.resource)||p.resource<s.cost())return Result.NO_RESOURCE;
        return Result.READY;
    }
    public static void commit(Profile p,Skill s,long now,long globalCooldown) {
        p.resource=Math.max(0,p.resource-s.cost());p.cooldowns.put(s.id(),now+s.cooldownMs());
        p.globalReadyAt=now+Math.max(100,globalCooldown);
    }
    public static double regenerate(double current,double maximum,double perSecond,double elapsedSeconds) {
        return Math.max(0,Math.min(maximum,current+perSecond*elapsedSeconds));
    }
}
