package it.castigo.classes;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AttackVisualGateTest {
    @Test void rejectsReplaysAndFloodsWithoutConsumingNextVariant(){var g=new AttackVisualGate();assertEquals(0,g.accept(1,0,"sword"));assertEquals(-1,g.accept(1,200,"sword"));assertEquals(-1,g.accept(2,50,"sword"));assertEquals(1,g.accept(2,100,"sword"));assertEquals(0,g.accept(3,200,"sword"));}
    @Test void resetsAfterPauseOrWeaponChange(){var g=new AttackVisualGate();assertEquals(0,g.accept(1,0,"sword"));assertEquals(1,g.accept(2,100,"sword"));assertEquals(0,g.accept(3,1200,"sword"));assertEquals(0,g.accept(4,1300,"axe"));}
    @Test void invalidRequestsDoNotClaimSequence(){var g=new AttackVisualGate();assertEquals(-1,g.accept(-1,0,"sword"));assertEquals(-1,g.accept(1,100,"VANILLA"));assertEquals(0,g.accept(1,100,"sword"));}
}
