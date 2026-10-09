package it.castigo.classes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class BundledIconsTest {
    @TempDir Path dir;
    @Test void allFortyDisciplineSkillsHaveDistinctPackIcons()throws Exception{
        var classes=List.of("mago_bianco","mago_nero","guerriero_scudo","guerriero_due_mani","arciere");
        for(String id:classes)try(var in=getClass().getResourceAsStream("/classes/"+id+".yml")){Files.copy(in,dir.resolve(id+".yml"));}
        var catalog=new it.castigo.classes.config.ClassCatalog(dir.toFile());var icons=new HashSet<String>();
        for(String id:classes)for(var skill:catalog.get(id).skills()){
            String icon=SkillEquipment.defaultIcon(skill);SkillEquipment.validateIcon(icon);
            assertTrue(icon.startsWith("texture:classi_regno:textures/item/"),skill.name());assertTrue(icons.add(icon));
        }
        assertEquals(40,icons.size());
    }
}
