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
        p.level=7;p.xp=43;p.resource=17.5;Collections.swap(p.slots,0,7);
        p.cooldowns.put("meteora",System.currentTimeMillis()+25000);store.save(p);
        Profile loaded=store.load(uuid,mage,100);loaded.normalize(mage);
        assertEquals(7,loaded.level);assertEquals(43,loaded.xp);assertEquals(17.5,loaded.resource);
        assertEquals("meteora",loaded.slots.getFirst());assertEquals(p.cooldowns,loaded.cooldowns);
    }
    @Test void malformedExistingProfileIsNotReplacedWithFreshData() throws Exception {
        ClassDefinition mage=mage();Path players=Files.createDirectory(directory.resolve("players"));
        UUID uuid=UUID.randomUUID();Path file=players.resolve(uuid+".yml");String broken="class: [invalid\n";Files.writeString(file,broken);
        assertThrows(Exception.class,()->new ProfileStore(players.toFile()).load(uuid,mage,100));
        assertEquals(broken,Files.readString(file));
    }
}
