package it.castigo.classes;
import it.castigo.classes.model.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class SupportContributionsTest {
    final UUID mob=UUID.randomUUID(),tank=UUID.randomUUID(),healer=UUID.randomUUID();
    @Test void onlyOutstandingMobWoundsFundHealing(){var s=new SupportContributions();s.wound(tank,mob,10,0);s.heal(healer,tank,100,1);s.heal(healer,tank,100,2);assertEquals(10,s.take(mob,100,1,3,Set.of(healer)).get(healer));}
    @Test void selfOrNaturalHealingConsumesDebtWithoutCredit(){var s=new SupportContributions();s.wound(tank,mob,10,0);s.heal(tank,tank,10,1);s.heal(healer,tank,100,2);assertTrue(s.take(mob,100,1,3,Set.of(healer,tank)).isEmpty());}
    @Test void expiredAndIneligibleSupportCannotEarnXp(){var s=new SupportContributions();s.wound(tank,mob,10,0);s.heal(healer,tank,10,30001);s.protect(mob,tank,50,30001);assertTrue(s.take(mob,100,1,30002,Set.of(healer)).isEmpty());}
    @Test void boundedSupportSharesExistingXpAndLedgerIsConsumed(){var s=new SupportContributions();var d=new DamageContributions();d.record(mob,tank,100,0);s.protect(mob,healer,1000,0);var eligible=Set.of(tank,healer);var credit=s.take(mob,d.total(mob,1,eligible),.35,1,eligible);assertEquals(35,credit.get(healer));var xp=d.take(mob,135,1,eligible,credit);assertEquals(100,xp.get(tank));assertEquals(35,xp.get(healer));assertEquals(135,xp.values().stream().mapToLong(Long::longValue).sum());assertTrue(s.take(mob,100,.35,2,eligible).isEmpty());}
    @Test void supportCannotEarnXpWithoutDamageOrWhenDisabled(){var s=new SupportContributions();s.protect(mob,healer,100,0);assertTrue(s.take(mob,0,1,1,Set.of(healer)).isEmpty());s.protect(mob,healer,100,2);assertTrue(s.take(mob,100,0,3,Set.of(healer)).isEmpty());}
}
