package it.castigo.classes;
import net.luckperms.api.*;
import net.luckperms.api.model.user.*;
import net.luckperms.api.model.group.*;
import net.luckperms.api.model.data.*;
import net.luckperms.api.node.*;
import org.bukkit.plugin.ServicePriority;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;
class ClassGroupsTest {
    @SuppressWarnings("unchecked") static <T>T proxy(Class<T> type,InvocationHandler h){return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},h);}
    @Test void rapidChangesAreOrderedAndKeepUnrelatedGroups(){
        var server=MockBukkit.mock();try{
            server.addSimpleWorld("world");MockBukkit.createMockPlugin("CastigoCore");
            var plugin=MockBukkit.loadWith(DisciplineIntegrationTest.TestClasses.class,getClass().getResourceAsStream("/plugin.yml"));
            var nodes=new HashSet<>(Set.of("group.admin","group.mago_bianco"));
            NodeMap map=proxy(NodeMap.class,(o,m,a)->{if(m.getName().equals("add"))nodes.add(((Node)a[0]).getKey());if(m.getName().equals("remove"))nodes.remove(((Node)a[0]).getKey());return null;});
            User user=proxy(User.class,(o,m,a)->m.getName().equals("data")?map:null);
            UserManager users=proxy(UserManager.class,(o,m,a)->{if(m.getName().equals("modifyUser")){((Consumer<User>)a[1]).accept(user);return CompletableFuture.completedFuture(null);}return null;});
            CompletableFuture<Group> first=new CompletableFuture<>();int[] calls={0};
            GroupManager groups=proxy(GroupManager.class,(o,m,a)->m.getName().equals("createAndLoadGroup")?(++calls[0]==1?first:CompletableFuture.completedFuture(null)):null);
            NodeBuilderRegistry builders=proxy(NodeBuilderRegistry.class,(o,m,a)->{
                String[] name={""};return proxy(net.luckperms.api.node.types.InheritanceNode.Builder.class,(builder,method,args)->{
                    if(method.getName().equals("group")){name[0]=(String)args[0];return builder;}
                    if(method.getName().equals("build")){String key="group."+name[0];return proxy(net.luckperms.api.node.types.InheritanceNode.class,(n,method2,args2)->method2.getName().equals("getKey")?key:null);}return null;
                });
            });
            LuckPerms api=proxy(LuckPerms.class,(o,m,a)->switch(m.getName()){case "getUserManager"->users;case "getGroupManager"->groups;case "getNodeBuilderRegistry"->builders;default->null;});
            server.getServicesManager().register(LuckPerms.class,api,plugin,ServicePriority.Normal);UUID id=UUID.randomUUID();
            ClassGroups.assign(plugin,id,"mago_bianco","mago_bianco_esperto");ClassGroups.assign(plugin,id,"mago_bianco_esperto","mago_bianco_maestro");
            assertEquals(1,calls[0]);first.complete(null);
            assertEquals(Set.of("group.admin","group.mago_bianco_maestro"),nodes);
        }finally{MockBukkit.unmock();}
    }
}
