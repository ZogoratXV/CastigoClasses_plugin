package it.castigo.classes;

import it.castigo.classes.model.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class StatPointsTest {
    private static final Stats GAIN=new Stats(1,1,2,5,1,1,1);
    private static final Stats BASE=new Stats(2,4,24,100,10,1,3);
    private static final Stats GROWTH=new Stats(0.2,0.3,2,6,1.5,0.2,0.5);
    private static final ClassDefinition MAGE=new ClassDefinition("mago","Mago","","",1,"Mana",0,5,0.1,BASE,GROWTH,List.of());
    private Profile player() { return new Profile(UUID.randomUUID(),"mago"); }
    private StatPointRules rules() { return new StatPointRules(2,1,GAIN); }

    @Test void awardsOnlyAtConfiguredMilestonesAndNeverAtStartingLevel() {
        Profile p=player();StatPointRules rules=rules();
        assertEquals(0,rules.reconcile(p));p.level=2;assertEquals(1,rules.reconcile(p));
        p.level=3;assertEquals(0,rules.reconcile(p));p.level=4;assertEquals(1,rules.reconcile(p));
        assertEquals(2,p.availableStatPoints());
        assertEquals(0,new StatPointRules(1,3,GAIN).entitlement(1));
        assertEquals(3,new StatPointRules(1,3,GAIN).entitlement(2));
    }
    @Test void multipleLevelJumpAwardsAllMilestonesExactlyOnce() {
        Profile p=player();p.level=11;
        assertEquals(5,rules().reconcile(p));assertEquals(0,rules().reconcile(p));
        assertEquals(11,p.statPointsRewardedThroughLevel);assertEquals(5,p.earnedStatPoints);
    }
    @Test void scheduleChangesApplyToFutureLevelsWithoutTakingOrDuplicatingPastRewards() {
        Profile p=player();p.level=4;assertEquals(6,new StatPointRules(2,3,GAIN).reconcile(p));
        StatPointRules changed=new StatPointRules(1,1,GAIN);
        assertEquals(0,changed.reconcile(p));p.level=5;assertEquals(1,changed.reconcile(p));
        assertEquals(7,p.earnedStatPoints);
    }
    @Test void loweringAndRegainingLevelsOrChangingClassCannotFarmPoints() {
        Profile p=player();p.level=10;rules().reconcile(p);
        p.level=2;p.classId="piromante";assertEquals(0,rules().reconcile(p));
        p.level=10;assertEquals(0,rules().reconcile(p));
        p.level=12;assertEquals(1,rules().reconcile(p));assertEquals(6,p.earnedStatPoints);
    }
    @Test void zeroRewardDisablesNewAwardsButKeepsExistingPoints() {
        Profile p=player();p.level=4;rules().reconcile(p);
        p.level=10;assertEquals(0,new StatPointRules(2,0,GAIN).reconcile(p));
        assertEquals(2,p.earnedStatPoints);assertEquals(0,rules().reconcile(p));
        p.level=12;assertEquals(1,rules().reconcile(p));
    }
    @Test void classGrowthAndAllocatedBonusesAreAddedForEveryStat() {
        Profile p=player();p.level=3;p.earnedStatPoints=7;
        for(StatAttribute stat:StatAttribute.values())assertTrue(rules().allocate(p,MAGE,stat,0.5,0.005));
        Stats total=rules().total(p,MAGE),base=MAGE.stats(3);
        for(StatAttribute stat:StatAttribute.values())assertEquals(base.value(stat)+GAIN.value(stat),total.value(stat),1e-9);
        assertEquals(0,p.availableStatPoints());assertEquals(7,p.spentStatPoints());
        assertFalse(rules().allocate(p,MAGE,StatAttribute.ATTACK,0.5,0.005));
    }
    @Test void allocatedStatsFeedActualCombatFormulas() {
        Profile p=player();p.level=3;p.earnedStatPoints=7;
        Stats before=rules().total(p,MAGE);
        for(StatAttribute stat:StatAttribute.values())rules().allocate(p,MAGE,stat,0.5,0.005);
        Stats after=rules().total(p,MAGE);
        assertEquals(1.5,CombatMath.meleeBonus(after,0.5)-CombatMath.meleeBonus(before,0.5),1e-9);
        assertEquals(0.005,CombatMath.speedBonus(after,0.005)-CombatMath.speedBonus(before,0.005),1e-9);
        assertTrue(Stats.mitigate(20,after.defense())<Stats.mitigate(20,before.defense()));
        Skill bolt=new Skill("bolt","Bolt","",Skill.Effect.BOLT,"minecraft:stick",0,1,8,1500,4,0.45,20,3,20);
        assertEquals(0.45,CombatMath.skillPower(after,bolt)-CombatMath.skillPower(before,bolt),1e-9);
        assertEquals(2,after.health()-before.health());assertEquals(5,after.mana()-before.mana());
    }
    @Test void automaticGrowthAlsoChangesAttackDefenseSpeedAndSkills() {
        Stats low=MAGE.stats(1),high=MAGE.stats(5);
        assertTrue(CombatMath.meleeBonus(high,0.5)>CombatMath.meleeBonus(low,0.5));
        assertTrue(CombatMath.speedBonus(high,0.005)>CombatMath.speedBonus(low,0.005));
        assertTrue(CombatMath.defenseReduction(high)>CombatMath.defenseReduction(low));
        assertTrue(high.intelligence()>low.intelligence());
    }
    @Test void reloadAndNormalizationKeepResourceAboveOriginalClassCapacity() {
        Profile p=player();p.earnedStatPoints=5;p.allocatedStats.put(StatAttribute.MANA,5);p.resource=120;
        p.normalize(MAGE,rules().total(p,MAGE));assertEquals(120,p.resource);
        assertEquals(125,rules().total(p,MAGE).mana());
    }
    @Test void cappedOrDisabledStatsCannotConsumePoints() {
        Profile p=player();p.earnedStatPoints=1000;p.allocatedStats.put(StatAttribute.HEALTH,500);
        assertFalse(rules().allocate(p,MAGE,StatAttribute.HEALTH,0.5,0.005));
        p.allocatedStats.put(StatAttribute.DEXTERITY,400);
        assertFalse(rules().allocate(p,MAGE,StatAttribute.DEXTERITY,0.5,0.005));
        assertFalse(rules().allocate(p,MAGE,StatAttribute.STRENGTH,0,0.005));
        var disabled=new StatPointRules(2,1,new Stats(1,1,2,0,1,1,1));
        assertFalse(disabled.allocate(p,MAGE,StatAttribute.MANA,0.5,0.005));
        assertEquals(100,p.availableStatPoints());
    }
    @Test void customGainAndSubclassKeepTheInvestedPoints() {
        Profile p=player();p.earnedStatPoints=1;rules().allocate(p,MAGE,StatAttribute.INTELLIGENCE,0.5,0.005);
        StatPointRules custom=new StatPointRules(5,2,new Stats(1,1,2,5,3,1,1));
        var subclass=new ClassDefinition("piromante","Piromante","","mago",10,"Fede",0,5,0.1,new Stats(2,4,30,120,15,1,3),GROWTH,List.of());
        assertEquals(18,custom.total(p,subclass).intelligence());assertEquals(1,p.spentStatPoints());
    }
    @Test void invalidRewardConfigurationAndUnknownAttributeAreRejected() {
        assertThrows(IllegalArgumentException.class,()->new StatPointRules(0,1,GAIN));
        assertThrows(IllegalArgumentException.class,()->new StatPointRules(2,-1,GAIN));
        assertThrows(IllegalArgumentException.class,()->StatAttribute.parse("admin"));
        assertEquals(StatAttribute.STRENGTH,StatAttribute.parse("forza"));
        assertEquals(StatAttribute.MANA,StatAttribute.parse("risorsa"));
    }
}
