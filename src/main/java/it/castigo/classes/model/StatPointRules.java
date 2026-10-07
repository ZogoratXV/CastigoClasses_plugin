package it.castigo.classes.model;

public record StatPointRules(int everyLevels,int pointsPerAward,Stats perPoint) {
    public StatPointRules {
        if(everyLevels<1||everyLevels>1000||pointsPerAward<0||pointsPerAward>1000)
            throw new IllegalArgumentException("Frequenza/quantità punti attributo non valida");
        for(StatAttribute stat:StatAttribute.values())
            if(perPoint.value(stat)>1000)throw new IllegalArgumentException("Bonus per punto troppo alto: "+stat.id());
    }
    public int entitlement(int level) {
        // Award at positive multiples of the interval, excluding starting level 1.
        return (Math.max(1,Math.min(1000,level))/everyLevels-1/everyLevels)*pointsPerAward;
    }
    public int reconcile(Profile p) {
        int level=Math.max(1,Math.min(1000,p.level));
        if(level<=p.statPointsRewardedThroughLevel)return 0;
        int awarded=entitlement(level)-entitlement(p.statPointsRewardedThroughLevel);
        p.earnedStatPoints+=awarded;
        p.statPointsRewardedThroughLevel=level;
        return awarded;
    }
    public Stats total(Profile p,ClassDefinition definition) {
        return definition.stats(p.level).addAllocated(p.allocatedStats,perPoint);
    }
    public boolean canAllocate(Profile p,ClassDefinition definition,StatAttribute attribute,
                               double strengthFactor,double dexterityFactor) {
        if(p.availableStatPoints()<1||perPoint.value(attribute)<=0)return false;
        Stats before=total(p,definition);
        var allocation=new java.util.EnumMap<>(p.allocatedStats);
        allocation.merge(attribute,1,Integer::sum);
        Stats after=definition.stats(p.level).addAllocated(allocation,perPoint);
        if(after.value(attribute)<=before.value(attribute))return false;
        return switch(attribute) {
            case STRENGTH, ATTACK -> CombatMath.meleeBonus(after,strengthFactor)>CombatMath.meleeBonus(before,strengthFactor);
            case DEXTERITY -> CombatMath.speedBonus(after,dexterityFactor)>CombatMath.speedBonus(before,dexterityFactor);
            default -> true;
        };
    }
    public boolean allocate(Profile p,ClassDefinition definition,StatAttribute attribute,
                            double strengthFactor,double dexterityFactor) {
        if(!canAllocate(p,definition,attribute,strengthFactor,dexterityFactor))return false;
        p.allocatedStats.merge(attribute,1,Integer::sum);
        return true;
    }
}
