package it.castigo.classes;
/** Per-player cosmetic request budget; never changes combat state. */
public final class AttackVisualGate {
    private long lastId=-1,lastAt=Long.MIN_VALUE;private String weapon="";private int variant;
    public int accept(long id,long now,String held){
        if(id<0||id<=lastId||held==null||held.equals("VANILLA"))return -1;
        if(lastAt!=Long.MIN_VALUE&&now-lastAt<100)return -1;
        boolean reset=lastAt==Long.MIN_VALUE||now-lastAt>1000||!weapon.equals(held);
        lastId=id;lastAt=now;weapon=held;variant=reset?0:1-variant;return variant;
    }
}
