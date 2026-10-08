package it.castigo.classes;
import it.castigo.classes.model.Skill;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class WeaponMotionTest {
    @Test void gesturesCoverEveryEffectAndDistinguishWeaponFamilies() {
        for(var e:Skill.Effect.values())assertNotNull(WeaponMotion.style(e));
        assertEquals("BOW",WeaponMotion.style(Skill.Effect.MASTER_SHOT));
        assertEquals("SHIELD",WeaponMotion.style(Skill.Effect.SHIELD_BASH));
        assertEquals("THRUST",WeaponMotion.style(Skill.Effect.LONG_THRUST));
        assertEquals("CAST",WeaponMotion.style(Skill.Effect.ALLY_HEAL));
        assertEquals("HEAVY",WeaponMotion.style(Skill.Effect.HEAVY_STRIKE));
    }
}
