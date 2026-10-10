package it.castigo.classes.model;

/** Applies class and daily limits before leveling. Only experience actually accepted consumes the budget. */
public final class ExperienceBudget {
    private ExperienceBudget(){}
    public static void rollover(Profile p,String day,String token){if(!day.equals(p.xpDay)||!token.equals(p.xpResetToken)){p.xpDay=day;p.xpResetToken=token;p.dailyXp=0;}}
    public static long award(Profile p,long amount,Progression curve,int cap,long dailyCap){
        cap=Math.min(cap,curve.maxLevel());if(amount<=0||p.level>=cap){if(p.level>=cap)p.xp=0;return 0;}
        long allowed=Math.min(amount,1_000_000_000_000_000L);
        if(dailyCap>0)allowed=Math.min(allowed,Math.max(0,dailyCap-p.dailyXp));
        long remaining=allowed;
        while(remaining>0&&p.level<cap){
            long need=Math.max(0,curve.required(p.level)-p.xp),take=Math.min(remaining,need);
            p.xp+=take;remaining-=take;
            if(p.xp>=curve.required(p.level)){p.xp=0;p.level++;}
        }
        long accepted=allowed-remaining;p.dailyXp=Math.min(1_000_000_000_000_000L,p.dailyXp+accepted);
        if(p.level>=cap)p.xp=0;return accepted;
    }
}
