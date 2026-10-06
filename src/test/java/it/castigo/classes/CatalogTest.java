package it.castigo.classes;

import it.castigo.classes.config.ClassCatalog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class CatalogTest {
    @TempDir Path directory;
    private void mage() throws Exception {
        try(var stream=getClass().getResourceAsStream("/classes/mago.yml")) { Files.copy(stream,directory.resolve("mago.yml")); }
    }
    @Test void shippedMageHasEightPlayableSkills() throws Exception {
        mage();var mage=new ClassCatalog(directory.toFile()).get("mago");
        assertEquals(8,mage.skills().size());assertEquals(100,mage.base().mana());
        assertTrue(mage.skills().stream().allMatch(s->s.unlockLevel()==1));
    }
    @Test void subclassInheritsSkillsAndOverridesIndividualAttributesAndResource() throws Exception {
        mage();Files.writeString(directory.resolve("paladino.yml"),"id: paladino\nparent: mago\nresource:\n  name: Fede\nattributes:\n  base:\n    health: 40\n");
        var child=new ClassCatalog(directory.toFile()).get("paladino");
        assertEquals("Fede",child.resourceName());assertEquals(40,child.base().health());
        assertEquals(100,child.base().mana());assertEquals(8,child.skills().size());assertEquals(1.5,child.growth().intelligence());
    }
    @Test void cyclesAndMissingParentsAreRejected() throws Exception {
        Files.writeString(directory.resolve("a.yml"),"id: a\nparent: b\n");
        assertThrows(IllegalArgumentException.class,()->new ClassCatalog(directory.toFile()));
        Files.writeString(directory.resolve("b.yml"),"id: b\nparent: a\n");
        assertThrows(IllegalArgumentException.class,()->new ClassCatalog(directory.toFile()));
    }
    @Test void malformedSkillCountCannotReplaceLiveCatalog() throws Exception {
        mage();Files.writeString(directory.resolve("bad.yml"),"id: bad\nparent: mago\nskills:\n  one:\n    name: One\n");
        assertThrows(IllegalArgumentException.class,()->new ClassCatalog(directory.toFile()));
    }
}
