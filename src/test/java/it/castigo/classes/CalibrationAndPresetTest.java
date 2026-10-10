package it.castigo.classes;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
class CalibrationAndPresetTest {
    @TempDir Path dir;
    @BeforeEach void setup(){org.mockbukkit.mockbukkit.MockBukkit.mock();}
    @AfterEach void cleanup(){org.mockbukkit.mockbukkit.MockBukkit.unmock();}
    @Test void calibrationPersistsAndRejectsInvalidValuesAtomically()throws Exception{
        var f=dir.resolve("poses.json");var c=new WeaponCalibration(f);c.set("minecraft:bow","firstY","-0.3");
        assertThrows(IllegalArgumentException.class,()->c.set("minecraft:bow","firstY","NaN"));assertThrows(IllegalArgumentException.class,()->c.set("minecraft:bow","thirdIntensity","3"));
        assertEquals(-.3,new WeaponCalibration(f).get("minecraft:bow").get("firstY").getAsDouble());c.set("minecraft:bow","reset","");assertTrue(new WeaponCalibration(f).get("minecraft:bow").isEmpty());
    }
    @Test void presetKeepsPriorityAndAllLayoutsStayInsideCanvas()throws Exception{
        var s=new HudSettings(dir.resolve("hud.json"));s.set("admin","priority","500");
        for(var id:HudPresets.NAMES){s.preset("admin",id);var t=s.theme("admin");assertEquals(500,s.priority("admin"));assertEquals(172,t.number("width"));assertTrue(t.number("barsX")+t.number("barsWidth")<172);assertTrue(t.number("xpY")+2<64);assertTrue(t.number("healthY")+t.number("barHeight")<t.number("resourceY"));}
        assertThrows(IllegalArgumentException.class,()->s.set("admin","textScale","49"));assertEquals(70,s.theme("admin").number("textScale"));
    }
    @Test void nextResetHandlesBothDstTransitions()throws Exception{
        var s=new ProgressionSettings(dir.resolve("progression.yml"));s.set(null,"reset-time","02:30");
        var before=java.time.Instant.parse("2026-10-25T00:00:00Z");assertEquals(java.time.Instant.parse("2026-10-25T00:30:00Z"),s.nextReset(before));
        assertEquals(java.time.Instant.parse("2026-10-26T01:30:00Z"),s.nextReset(java.time.Instant.parse("2026-10-25T01:00:00Z")));
        assertEquals(java.time.Instant.parse("2026-03-29T01:30:00Z"),s.nextReset(java.time.Instant.parse("2026-03-29T00:00:00Z")));
    }
}
