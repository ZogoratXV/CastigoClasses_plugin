package it.castigo.classes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class HudSettingsTest {
    @TempDir Path dir;
    @Test void priorityPrimaryTieAndFallbackPersist()throws Exception{
        var file=dir.resolve("hud.json");var settings=new HudSettings(file);
        settings.set("default","border","AAAAAA");settings.set("nobile","border","FFD700");settings.set("mago","border","8844FF");
        settings.set("mago","priority","20");settings.set("nobile","priority","10");
        assertEquals("8844FF",settings.resolve(List.of("nobile","mago"),"nobile").text("border"));
        settings.set("nobile","priority","20");assertEquals("FFD700",settings.resolve(List.of("mago","nobile"),"nobile").text("border"));
        var restored=new HudSettings(file);assertEquals("AAAAAA",restored.resolve(List.of("unknown"),"unknown").text("border"));
        restored.reset("mago");assertEquals("AAAAAA",restored.resolve(List.of("mago"),"mago").text("border"));
    }
    @Test void civilianThemeIgnoresClassDefaultsAndKeepsCustomPopolanoSettings()throws Exception{
        var s=new HudSettings(dir.resolve("hud.json"));s.preset("default","mago_nero");s.preset("mago_nero","mago_nero");s.set("mago_nero","priority","100");
        assertEquals("castigoclasses:textures/gui/hud/popolano.png",s.civilianTheme().text("texture"));
        s.preset("popolano","popolano");s.set("popolano","texture","regno:textures/gui/civile.png");
        assertEquals("regno:textures/gui/civile.png",new HudSettings(dir.resolve("hud.json")).civilianTheme().text("texture"));
    }
    @Test void luckPermsWeightWinsOverLegacyPriorityAndClasslessFallback()throws Exception{
        var s=new HudSettings(dir.resolve("hud.json"));s.preset("guardia","guardia");s.preset("mago_nero","mago_nero");
        s.set("mago_nero","priority","9999");
        var groups=java.util.Map.of("guardia",100,"mago_nero",10,"permission_only",900);
        assertTrue(s.resolveWeighted(groups,"mago_nero","mago_nero",java.util.Map.of()).text("texture").endsWith("guardia.png"));
        assertTrue(s.resolveWeighted(groups,"mago_nero","",java.util.Map.of()).text("texture").endsWith("guardia.png"));
        assertTrue(s.resolveWeighted(java.util.Map.of("guardia",100,"mago_nero",100),"mago_nero","",java.util.Map.of()).text("texture").endsWith("mago_nero.png"));
        assertTrue(s.resolveWeighted(java.util.Map.of(),"default","",java.util.Map.of()).text("texture").endsWith("popolano.png"));
        assertTrue(s.resolveWeighted(java.util.Map.of("elite",50),"elite","",java.util.Map.of("elite","arciere")).text("texture").endsWith("arciere.png"));
    }
    @Test void invalidUpdatesCannotReplaceSavedSettings()throws Exception{
        var s=new HudSettings(dir.resolve("hud.json"));s.set("test","texture","castigo:textures/gui/hud/test.png");
        assertThrows(IllegalArgumentException.class,()->s.set("test","texture","https://example.com/file.png"));
        assertThrows(IllegalArgumentException.class,()->s.set("test","width","99999"));
        assertThrows(IllegalArgumentException.class,()->s.set("test","texture","castigo:textures/../secret.png"));
        assertEquals("castigo:textures/gui/hud/test.png",s.theme("test").text("texture"));
    }
}
