package it.castigo.classes;

import it.castigo.classes.config.ClassCatalog;
import it.castigo.classes.model.Skill;
import it.castigo.core.CoreApi;
import it.castigo.core.ParticleStyle;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.lang.reflect.Proxy;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PresentationTest {
    @TempDir Path directory;
    private Skill skill(Skill.Effect effect) { return new Skill("test","Test","",effect,"minecraft:stick",0xBB88FF,1,8,1500,4,0.5,24,3,100); }
    private YamlConfiguration yaml(String value) throws Exception { var y=new YamlConfiguration();y.loadFromString(value);return y; }
    @Test void oldYamlGetsBoundedDefaultsForEveryEffect() {
        for(var effect:Skill.Effect.values()) {
            var fx=SkillPresentation.read(null,skill(effect));
            assertTrue(fx.enabled());assertEquals(5,fx.cues().size());
            for(var cue:fx.cues().values()) { assertTrue(cue.points()<=32);assertTrue(cue.particles().count()<=32); }
        }
    }
    @Test void partialOverrideKeepsUnspecifiedPropertiesAndCustomAudio() throws Exception {
        var fx=SkillPresentation.read(yaml("trail:\n  particles:\n    color: '123456'\nimpact:\n  sound:\n    id: castigo:arcane.impact\n    volume: 0.4\n"),skill(Skill.Effect.BOLT));
        var dust=(Particle.DustOptions)fx.cues().get(SkillPresentation.Stage.TRAIL).particles().data();
        assertEquals(0x123456,dust.getColor().asRGB());assertEquals(24,fx.cues().get(SkillPresentation.Stage.TRAIL).points());
        assertEquals("castigo:arcane.impact",fx.cues().get(SkillPresentation.Stage.IMPACT).sound().id());
    }
    @Test void canDisableWholePresentationAndIndividualChannels() throws Exception {
        var fx=SkillPresentation.read(yaml("enabled: false\ntrail:\n  particles:\n    enabled: false\nimpact:\n  sound:\n    enabled: false\n"),skill(Skill.Effect.BOLT));
        assertFalse(fx.enabled());assertFalse(fx.cues().get(SkillPresentation.Stage.TRAIL).particles().enabled());
        assertFalse(fx.cues().get(SkillPresentation.Stage.IMPACT).sound().enabled());
    }
    @Test void rejectsExpensiveMalformedAndUnsupportedEffectsBeforeReload() throws Exception {
        for(String value:new String[]{"trail:\n  points: 10000", "trail:\n  duration-ticks: 0", "trail:\n  particles:\n    count: 1.5", "trail:\n  particles:\n    spread: .NaN", "trail:\n  particles:\n    particle: ITEM", "impact:\n  sound:\n    id: 'bad id'", "impact:\n  sound:\n    pitch: 10", "unknown-stage: {}", "enabled: 'yes'"}) {
            var config=yaml(value);assertThrows(RuntimeException.class,()->SkillPresentation.read(config,skill(Skill.Effect.BOLT)),value);
        }
    }
    @Test void inheritedSkillsKeepTheirPresentation() throws Exception {
        try(var stream=getClass().getResourceAsStream("/classes/mago.yml")) { Files.copy(stream,directory.resolve("mago.yml")); }
        Files.writeString(directory.resolve("child.yml"),"id: child\nparent: mago\n");
        var catalog=new ClassCatalog(directory.toFile());
        assertSame(catalog.presentation(catalog.get("mago").skills().getFirst()),catalog.presentation(catalog.get("child").skills().getFirst()));
    }
    @Test void wirePacketContainsOnlyPortableValues() {
        UUID id=UUID.fromString("11111111-1111-1111-1111-111111111111");
        World world=(World)Proxy.newProxyInstance(World.class.getClassLoader(),new Class<?>[]{World.class},(proxy,method,args)->method.getName().equals("getUID")?id:null);
        var cue=SkillPresentation.read(null,skill(Skill.Effect.BOLT)).cues().get(SkillPresentation.Stage.TRAIL);
        var packet=PresentationPlayer.packet(cue,new Location(world,0,64,0),new Location(world,10,64,0));
        assertEquals("minecraft:dust",packet.getAsJsonObject("particles").get("id").getAsString());
        assertEquals(0xBB88FF,packet.getAsJsonObject("particles").get("color").getAsInt());
        assertEquals("LINE",packet.get("shape").getAsString());assertEquals(8,packet.get("durationTicks").getAsInt());
        assertEquals(id.toString(),packet.get("world").getAsString());assertFalse(packet.toString().contains("org.bukkit"));
    }
    @Test void linkedCoreExposesRequiredBeta9Apis() throws Exception {
        assertNotNull(ParticleStyle.class.getMethod("read",org.bukkit.configuration.ConfigurationSection.class));
        assertNotNull(CoreApi.class.getMethod("atomicWrite",Path.class,String.class));
        assertNotNull(it.castigo.core.Reflect.class.getMethod("call",Object.class,String.class,Object[].class));
        assertThrows(NoSuchMethodException.class,()->it.castigo.core.Protection.class.getMethod("interact",org.bukkit.entity.Player.class,Location.class));
    }
}
