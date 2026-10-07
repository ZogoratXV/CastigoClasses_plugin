package it.castigo.classes.model;

/** Small shared rules used by combat, preparation and the test suite. */
public final class DisciplineRules {
    private DisciplineRules() {}
    public static int arrows(Skill.Effect effect) {
        return switch(effect) { case DOUBLE_SHOT -> 2;case COVER_FIRE -> 3;case PRECISE_SHOT,HINDERING_SHOT,MASTER_SHOT -> 1;default -> 0; };
    }
    public static double transferred(double incoming,double fraction,double protectorHealth) {
        return Math.max(0,Math.min(incoming*Math.max(0,Math.min(0.8,fraction)),protectorHealth-1));
    }
    public static double healing(double requested,double missing,double reduction) {
        return Math.max(0,Math.min(requested,missing))*(1-Math.max(0,Math.min(0.8,reduction)));
    }
    public static String rank(int level,java.util.List<Skill> skills) {
        if(skills.stream().noneMatch(s->s.effect().discipline()))return "";
        String[] ranks={"Aspirante","Apprendista","Praticante","Esperto","Maestro"};int rank=0;
        for(int pair=0;pair<4;pair++) {
            if(level<Math.max(skills.get(pair*2).unlockLevel(),skills.get(pair*2+1).unlockLevel()))break;
            rank++;
        }
        return ranks[rank];
    }
    public static boolean prepared(boolean sameWorld,boolean alive,boolean equipment,double movedSquared) {
        return sameWorld&&alive&&equipment&&Double.isFinite(movedSquared)&&movedSquared<=0.25;
    }
}
