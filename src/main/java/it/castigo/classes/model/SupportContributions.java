package it.castigo.classes.model;

import java.util.*;

/** Only real mob-inflicted wounds can fund healing credit. Support weight per mob is bounded. */
public final class SupportContributions {
    private record Wound(UUID mob,double amount,long at){}
    private record Credit(UUID player,double amount,long at){}
    private final Map<UUID,ArrayDeque<Wound>> wounds=new HashMap<>();
    private final Map<UUID,ArrayDeque<Credit>> credits=new HashMap<>();
    public void clearWounds(UUID player){wounds.remove(player);}
    public void prune(long now){wounds.values().forEach(q->q.removeIf(w->now-w.at>30000));wounds.values().removeIf(Deque::isEmpty);credits.values().forEach(q->q.removeIf(c->now-c.at>30000));credits.values().removeIf(Deque::isEmpty);}
    public void wound(UUID player,UUID mob,double amount,long now){
        if(!Double.isFinite(amount)||amount<=0)return;if(wounds.size()>=1024&&!wounds.containsKey(player))return;
        var q=wounds.computeIfAbsent(player,k->new ArrayDeque<>());if(q.size()>=128)q.removeFirst();q.addLast(new Wound(mob,amount,now));
    }
    public void heal(UUID healer,UUID target,double amount,long now){
        var q=wounds.get(target);if(q==null||amount<=0||!Double.isFinite(amount))return;
        while(amount>0&&!q.isEmpty()){
            var w=q.removeFirst();if(now-w.at>30000)continue;double n=Math.min(amount,w.amount);amount-=n;
            if(!healer.equals(target))protect(w.mob,healer,n,now);
            if(n<w.amount)q.addFirst(new Wound(w.mob,w.amount-n,w.at));
        }
        if(q.isEmpty())wounds.remove(target);
    }
    public void protect(UUID mob,UUID player,double amount,long now){
        if(!Double.isFinite(amount)||amount<=0||credits.size()>=4096&&!credits.containsKey(mob))return;
        var q=credits.computeIfAbsent(mob,k->new ArrayDeque<>());if(q.size()>=256)q.removeFirst();q.addLast(new Credit(player,amount,now));
    }
    public Map<UUID,Double> take(UUID mob,double directDamage,double maxRatio,long now,Set<UUID> eligible){
        var q=credits.remove(mob);for(var debts:wounds.values())debts.removeIf(w->w.mob.equals(mob));
        if(q==null||directDamage<=0||maxRatio<=0)return Map.of();
        var sums=new HashMap<UUID,Double>();for(var c:q)if(now-c.at<=30000&&eligible.contains(c.player))sums.merge(c.player,c.amount,Double::sum);
        double total=sums.values().stream().mapToDouble(Double::doubleValue).sum(),factor=total<=0?0:Math.min(1,directDamage*maxRatio/total);
        sums.replaceAll((id,n)->n*factor);return sums;
    }
}
