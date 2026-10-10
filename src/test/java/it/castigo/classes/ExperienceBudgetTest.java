package it.castigo.classes;
import it.castigo.classes.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class ExperienceBudgetTest {
    @TempDir Path temp;
    @Test void classCapDiscardsExcessWithoutSpendingDailyAllowance(){
        var p=new Profile(UUID.randomUUID(),"a");p.level=9;p.xp=90;
        assertEquals(10,ExperienceBudget.award(p,10000,new Progression(100,100,1),10,1000));
        assertEquals(10,p.level);assertEquals(0,p.xp);assertEquals(10,p.dailyXp);
        assertEquals(0,ExperienceBudget.award(p,99,new Progression(100,100,1),10,1000));
        p.classId="a_esperto";assertEquals(50,ExperienceBudget.award(p,50,new Progression(100,100,1),25,1000));assertEquals(60,p.dailyXp);
    }
    @Test void dailyCapPersistsAcrossClassChangesAndResetDoesNotTouchLevel(){
        var p=new Profile(UUID.randomUUID(),"a");ExperienceBudget.rollover(p,"2026-10-10","");
        assertEquals(150,ExperienceBudget.award(p,900,new Progression(100,100,1),10,150));
        p.classId="b";assertEquals(0,ExperienceBudget.award(p,10,new Progression(100,100,1),10,150));
        ExperienceBudget.rollover(p,"2026-10-10","");assertEquals(150,p.dailyXp);
        ExperienceBudget.rollover(p,"2026-10-10","reset");assertEquals(0,p.dailyXp);assertEquals(2,p.level);assertEquals(50,p.xp);
    }
    @Test void italianResetSupportsSummerWinterAndRepeatedHour()throws Exception{
        var s=new ProgressionSettings(temp.resolve("settings.yml"));s.set(null,"reset-time","04:30");
        assertEquals("2026-07-09",s.day(Instant.parse("2026-07-10T02:29:59Z")));
        assertEquals("2026-07-10",s.day(Instant.parse("2026-07-10T02:30:00Z")));
        assertEquals("2026-01-10",s.day(Instant.parse("2026-01-10T03:30:00Z")));
        s.set(null,"reset-time","02:30");
        assertEquals("2026-10-25",s.day(Instant.parse("2026-10-25T00:40:00Z")));
        assertEquals("2026-10-25",s.day(Instant.parse("2026-10-25T01:10:00Z")));
        assertThrows(Exception.class,()->s.set(null,"reset-time","25:00"));
        assertEquals("02:30",new ProgressionSettings(temp.resolve("settings.yml")).resetTime().toString());
    }
    @Test void damageRewardIsProportionalConservesTotalAndCannotBeClaimedTwice(){
        var d=new DamageContributions();UUID mob=UUID.randomUUID(),a=UUID.randomUUID(),b=UUID.randomUUID();
        d.record(mob,a,75,1000);d.record(mob,b,25,2000);
        var rewards=d.take(mob,11,2000,Set.of(a,b));assertEquals(8,rewards.get(a));assertEquals(3,rewards.get(b));
        assertTrue(d.take(mob,11,2000,Set.of(a,b)).isEmpty());
    }
    @Test void expiredAndIneligibleHitsReceiveNothing(){
        var d=new DamageContributions();UUID mob=UUID.randomUUID(),a=UUID.randomUUID(),b=UUID.randomUUID();
        d.record(mob,a,100,0);d.record(mob,b,1,40000);
        assertEquals(Map.of(b,10L),d.take(mob,10,40000,Set.of(a,b)));
    }
}
