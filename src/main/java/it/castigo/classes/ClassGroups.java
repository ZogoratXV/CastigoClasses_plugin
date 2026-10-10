package it.castigo.classes;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.node.types.InheritanceNode;
import java.util.*;
import java.util.concurrent.*;

/** Loaded only when LuckPerms is enabled. Per-user chains preserve rapid class change ordering. */
public final class ClassGroups {
    private static final Map<UUID,CompletableFuture<Void>> pending=new HashMap<>();
    private ClassGroups(){}
    public static synchronized void assign(CastigoClasses plugin,UUID id,String previous,String next){
        LuckPerms lp=plugin.getServer().getServicesManager().load(LuckPerms.class);if(lp==null)return;
        var future=pending.getOrDefault(id,CompletableFuture.completedFuture(null)).handle((v,e)->null)
            .thenCompose(v->lp.getGroupManager().createAndLoadGroup(next))
            .thenCompose(g->lp.getUserManager().modifyUser(id,user->{
                if(!previous.isBlank()&&!previous.equals(next))user.data().remove(lp.getNodeBuilderRegistry().forInheritance().group(previous).build());
                user.data().add(lp.getNodeBuilderRegistry().forInheritance().group(next).build());
            }));
        pending.put(id,future);
        future.whenComplete((v,error)->{
            synchronized(ClassGroups.class){pending.remove(id,future);}
            if(error!=null)plugin.getLogger().warning("Gruppo classe LuckPerms non sincronizzato per "+id+": "+error.getMessage());
        });
    }
}
