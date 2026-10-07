package it.castigo.classes;

import it.castigo.classes.model.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DisciplineRulesTest {
    @Test void physicalAndMagicalAttributesBothAffectConfiguredSkills() {
        var skill=new Skill("test","Test","",Skill.Effect.MELEE,"minecraft:stick",0,1,1,1000,5,0.5,3,2,20);
        var rules=new SkillMechanics(SkillMechanics.Weapon.ANY,0,2,3,4,0.3);
        assertEquals(5+6*0.5+2*2+3*3+7*4,rules.power(new Stats(2,3,20,10,6,7,0),skill));
        assertEquals(10000,rules.power(new Stats(1000000,1000000,20,10,1000000,1000000,0),skill));
    }
    @Test void transferCannotKillProtectorAndNeverAmplifiesIncomingDamage() {
        assertEquals(3,DisciplineRules.transferred(10,0.3,20));
        assertEquals(1,DisciplineRules.transferred(100,0.8,2));
        assertEquals(0,DisciplineRules.transferred(100,0.8,1));
        assertEquals(8,DisciplineRules.transferred(10,5,100));
    }
    @Test void healingIsBoundedByMissingHealthAndReductionLimit() {
        assertEquals(4,DisciplineRules.healing(100,8,0.5));
        assertEquals(2,DisciplineRules.healing(10,20,1),1e-9);
        assertEquals(0,DisciplineRules.healing(10,0,0));
    }
    @Test void movementEquipmentAndWorldInvalidatePreparation() {
        assertTrue(DisciplineRules.prepared(true,true,true,0.25));
        assertFalse(DisciplineRules.prepared(true,true,true,0.251));
        assertFalse(DisciplineRules.prepared(false,true,true,0));
        assertFalse(DisciplineRules.prepared(true,false,true,0));
        assertFalse(DisciplineRules.prepared(true,true,false,0));
        assertFalse(DisciplineRules.prepared(true,true,true,Double.NaN));
    }
    @Test void BurstsRequireTheirFullAmmunition() {
        assertEquals(1,DisciplineRules.arrows(Skill.Effect.MASTER_SHOT));
        assertEquals(2,DisciplineRules.arrows(Skill.Effect.DOUBLE_SHOT));
        assertEquals(3,DisciplineRules.arrows(Skill.Effect.COVER_FIRE));
        assertEquals(0,DisciplineRules.arrows(Skill.Effect.HUNTER_STEP));
    }
}
