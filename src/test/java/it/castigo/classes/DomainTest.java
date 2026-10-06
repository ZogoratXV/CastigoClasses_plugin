package it.castigo.classes;

import it.castigo.classes.model.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DomainTest {
    private Profile player() { return new Profile(UUID.randomUUID(),"mago"); }
    private Skill skill() { return new Skill("bolt","Bolt","",Skill.Effect.BOLT,"minecraft:stick",0,1,8,1500,4,0.5,20,3,20); }
    @Test void levelOneUsesBaseAndGrowthIsPerAdditionalLevel() {
        Stats base=new Stats(2,4,24,100,10,1,3),growth=new Stats(0.2,0.3,2,6,1.5,0.2,0.5);
        assertEquals(base,base.atLevel(growth,1));assertEquals(28,base.atLevel(growth,3).health());
        assertEquals(112,base.atLevel(growth,3).mana());assertEquals(13,base.atLevel(growth,3).intelligence());
    }
    @Test void largeXpGrantCrossesMultipleLevelsAndKeepsRemainder() {
        Profile p=player();new Progression(100,100,2).add(p,750);
        assertEquals(4,p.level);assertEquals(50,p.xp);
    }
    @Test void capDiscardsExtraXpAndCannotOverflow() {
        Profile p=player();new Progression(3,100,1).add(p,Long.MAX_VALUE);
        assertEquals(3,p.level);assertEquals(0,p.xp);
    }
    @Test void loadNormalizationDoesNotLoseXpAtBoundary() {
        Profile p=player();p.xp=100;new Progression(100,100,2).add(p,0);
        assertEquals(2,p.level);assertEquals(0,p.xp);
    }
    @Test void invalidGrowthAndNonFiniteStatsAreRejected() {
        assertThrows(IllegalArgumentException.class,()->new Progression(100,100,Double.NaN));
        assertThrows(IllegalArgumentException.class,()->new Stats(0,0,20,Double.NaN,0,0,0));
    }
    @Test void cooldownAndResourceRemainAuthoritativeAfterReorder() {
        Profile p=player();p.resource=10;Skill s=skill();
        assertEquals(AbilityRules.Result.READY,AbilityRules.check(p,s,1000));
        AbilityRules.commit(p,s,1000,350);assertEquals(2,p.resource);
        assertEquals(AbilityRules.Result.GLOBAL_COOLDOWN,AbilityRules.check(p,s,1100));
        assertEquals(AbilityRules.Result.COOLDOWN,AbilityRules.check(p,s,1500));
        assertEquals(AbilityRules.Result.NO_RESOURCE,AbilityRules.check(p,s,2500));
        p.resource=8;assertEquals(AbilityRules.Result.READY,AbilityRules.check(p,s,2500));
    }
    @Test void resourceRegenerationCapsAndHandlesZeroResourceClass() {
        assertEquals(100,AbilityRules.regenerate(99,100,10,0.25));
        assertEquals(0,AbilityRules.regenerate(0,0,10,1));
    }
    @Test void slotsRequireAnExactPermutation() {
        List<String> ids=List.of("a","b","c","d","e","f","g","h");
        List<String> swap=new ArrayList<>(ids);Collections.swap(swap,0,7);assertTrue(Profile.validOrder(swap,ids));
        swap.set(0,"b");assertFalse(Profile.validOrder(swap,ids));assertFalse(Profile.validOrder(ids.subList(0,7),ids));
    }
    @Test void defenseReducesButNeverInvertsDamage() {
        assertEquals(10,Stats.mitigate(20,100));assertTrue(Stats.mitigate(20,100000)>0);
    }
}
