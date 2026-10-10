package it.castigo.classes.model;

import java.util.*;

/** Bounded recent damage ledger; no Bukkit objects are retained. Integer rewards preserve the total. */
public final class DamageContributions {
    private record Hit(UUID player,double damage,long at){}
    private final Map<UUID,ArrayDeque<Hit>> victims=new HashMap<>();
    public void prune(long now){victims.values().forEach(q->q.removeIf(h->now-h.at>30000));victims.values().removeIf(Deque::isEmpty);}
    public void record(UUID victim,UUID player,double damage,long now){
        if(!Double.isFinite(damage)||damage<=0)return;
        if(victims.size()>=4096&&!victims.containsKey(victim)){prune(now);if(victims.size()>=4096)return;}
        var q=victims.computeIfAbsent(victim,k->new ArrayDeque<>());q.removeIf(h->now-h.at>30000);
        if(q.size()>=1024)q.removeFirst();q.addLast(new Hit(player,damage,now));
    }
    public Map<UUID,Long> take(UUID victim,long xp,long now,Set<UUID> eligible){
        var q=victims.remove(victim);if(q==null||xp<=0)return Map.of();
        var sums=new TreeMap<UUID,Double>();for(var h:q)if(now-h.at<=30000&&eligible.contains(h.player))sums.merge(h.player,h.damage,Double::sum);
        double total=sums.values().stream().mapToDouble(Double::doubleValue).sum();if(total<=0)return Map.of();
        var result=new LinkedHashMap<UUID,Long>();var fractions=new HashMap<UUID,Double>();long used=0;
        for(var e:sums.entrySet()){double share=xp*(e.getValue()/total);long n=(long)Math.floor(share);result.put(e.getKey(),n);used+=n;fractions.put(e.getKey(),share-n);}
        var order=new ArrayList<>(sums.keySet());order.sort(Comparator.<UUID>comparingDouble(fractions::get).reversed().thenComparing(Comparator.naturalOrder()));
        for(long i=0;i<xp-used;i++){var id=order.get((int)(i%order.size()));result.put(id,result.get(id)+1);}
        return result;
    }
}
