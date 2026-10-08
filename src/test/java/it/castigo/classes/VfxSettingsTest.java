package it.castigo.classes;

import it.castigo.classes.model.Skill;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class VfxSettingsTest {
    @TempDir Path directory;
    Skill skill() { return new Skill("test","Test","",Skill.Effect.BOLT,"minecraft:stick",0xAA77FF,1,8,1500,4,.5,24,3,100); }
    @Test void overridesSurviveRestartAndDoNotChangeOtherClassesOrPhases() throws Exception {
        var file=directory.resolve("vfx.json").toFile();var settings=new VfxSettings(file);var base=SkillPresentation.read(null,skill());
        var draft=VfxSettings.draft(base.cues().get(SkillPresentation.Stage.IMPACT));draft.addProperty("shape","SPIRAL");
        settings.save("mage","test",SkillPresentation.Stage.IMPACT,draft);settings=new VfxSettings(file);
        assertEquals(SkillPresentation.Shape.SPIRAL,settings.resolve("mage",skill(),base).cues().get(SkillPresentation.Stage.IMPACT).shape());
        assertEquals(base,settings.resolve("other",skill(),base));assertEquals(base.cues().get(SkillPresentation.Stage.CAST),settings.resolve("mage",skill(),base).cues().get(SkillPresentation.Stage.CAST));
        settings.save("mage","test",SkillPresentation.Stage.IMPACT,null);assertEquals(base,settings.resolve("mage",skill(),base));
    }
    @Test void invalidOrOversizedPresetCannotReplaceSavedConfiguration() throws Exception {
        var settings=new VfxSettings(directory.resolve("vfx.json").toFile());var base=SkillPresentation.read(null,skill());
        var draft=VfxSettings.draft(base.cues().get(SkillPresentation.Stage.IMPACT));draft.addProperty("count",100000);
        assertThrows(IllegalArgumentException.class,()->settings.save("mage","test",SkillPresentation.Stage.CAST,draft));
        assertEquals(base,settings.resolve("mage",skill(),base));
        draft.addProperty("count",1);draft.addProperty("command","op player");assertThrows(IllegalArgumentException.class,()->VfxSettings.read(draft));
    }
}
