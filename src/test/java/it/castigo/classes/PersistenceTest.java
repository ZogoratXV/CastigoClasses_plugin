package it.castigo.classes;

import it.castigo.classes.config.ClassCatalog;
import it.castigo.classes.model.*;
import it.castigo.classes.storage.ProfileStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PersistenceTest {
    @TempDir Path directory;
    private ClassDefinition mage() throws Exception {
        Path classes=Files.createDirectory(directory.resolve("classes"));
        try(var stream=getClass().getResourceAsStream("/classes/mago.yml")) { Files.copy(stream,classes.resolve("mago.yml")); }
        return new ClassCatalog(classes.toFile()).get("mago");
    }
    @Test void reconnectPreservesProgressResourceOrderAndActiveCooldowns() throws Exception {
        ClassDefinition mage=mage();ProfileStore store=new ProfileStore(directory.resolve("players").toFile());
        UUID uuid=UUID.randomUUID();Profile p=store.load(uuid,mage,100);
        p.level=7;p.xp=43;p.resource=17.5;p.dailyXp=450;p.xpDay="2026-10-10";p.xpResetToken="staff-reset";p.managedClassGroup="mago";Collections.swap(p.slots,0,7);
        p.cooldowns.put("meteora",System.currentTimeMillis()+25000);store.save(p);
        Profile loaded=store.load(uuid,mage,100);loaded.normalize(mage);
        assertEquals(7,loaded.level);assertEquals(43,loaded.xp);assertEquals(17.5,loaded.resource);
        assertEquals(450,loaded.dailyXp);assertEquals("2026-10-10",loaded.xpDay);assertEquals("staff-reset",loaded.xpResetToken);assertEquals("mago",loaded.managedClassGroup);
        assertEquals("meteora",loaded.slots.getFirst());assertEquals(p.cooldowns,loaded.cooldowns);
    }
    @Test void malformedExistingProfileIsNotReplacedWithFreshData() throws Exception {
        ClassDefinition mage=mage();Path players=Files.createDirectory(directory.resolve("players"));
        UUID uuid=UUID.randomUUID();Path file=players.resolve(uuid+".yml");String broken="class: [invalid\n";Files.writeString(file,broken);
        assertThrows(Exception.class,()->new ProfileStore(players.toFile()).load(uuid,mage,100));
        assertEquals(broken,Files.readString(file));
    }
    @Test void legacyProfileReceivesRetroactivePointsOnceAndKeepsInvestmentsAfterReconnect() throws Exception {
        ClassDefinition mage=mage();Path players=Files.createDirectory(directory.resolve("players"));
        UUID uuid=UUID.randomUUID();Files.writeString(players.resolve(uuid+".yml"),"schema: 1\nclass: mago\nlevel: 10\nxp: 17\nresource: 90\n");
        ProfileStore store=new ProfileStore(players.toFile());
        StatPointRules rules=new StatPointRules(2,1,new Stats(1,1,2,5,1,1,1));
        Profile p=store.load(uuid,mage,100);assertEquals(5,rules.reconcile(p));
        assertTrue(rules.allocate(p,mage,StatAttribute.ATTACK,0.5,0.005));
        p.normalize(mage,rules.total(p,mage));store.save(p);
        Profile loaded=store.load(uuid,mage,100);assertEquals(0,rules.reconcile(loaded));
        assertEquals(4,loaded.availableStatPoints());assertEquals(1,loaded.allocatedStats.get(StatAttribute.ATTACK));
        assertEquals(10,loaded.statPointsRewardedThroughLevel);assertEquals(17,loaded.xp);
        assertEquals(rules.total(p,mage),rules.total(loaded,mage));
    }
    @Test void corruptPointLedgerIsRejectedWithoutOverwritingTheFile() throws Exception {
        ClassDefinition mage=mage();Path players=Files.createDirectory(directory.resolve("players"));
        UUID uuid=UUID.randomUUID();Path file=players.resolve(uuid+".yml");
        String bad="class: mago\nlevel: 10\nstat-points:\n  earned: 1\n  allocated:\n    strength: 5\n";
        Files.writeString(file,bad);
        assertThrows(Exception.class,()->new ProfileStore(players.toFile()).load(uuid,mage,100));
        assertEquals(bad,Files.readString(file));
    }
}
