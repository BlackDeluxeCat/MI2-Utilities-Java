package mi2u.ui.island;

import arc.func.*;
import arc.struct.*;
import arc.util.*;
import mi2u.io.*;
import mi2u.ui.island.children.*;

public class IslandUtils {
    // 标准方法，减少instanceof样板代码
    public static boolean addChild(Island parent, Island child){
        if(parent != null && child != null && parent != child && !isAscendantOf(child, parent) && parent.content instanceof ChildrenContent cc){
            cc.unsafeAddChild(child);
            return true;
        }
        Log.warn("Cannot add child.");
        return false;
    }

    public static Island getRoot(Island island){
        int i = 0;
        do{
            var parent = island.getParentIsland();
            if(parent == null) return island;
            island = island.getParentIsland();
        }while(i++ < 100);
        return null;
    }

    public static boolean isRoot(Island tree){
        return tree.content instanceof ChildrenContent cc && cc.childrenLayout instanceof RootStackLayout;
    }

    public static boolean hasSameRoot(Island i1, Island i2){
        return getRoot(i1) == getRoot(i2);
    }

    public static boolean isAscendantOf(Island i1, Island i2){
        int i = 0;
        while(i++ < 1000){
            var parent = i2.getParentIsland();
            if(parent == null) return false;
            if(parent == i1) return true;
            i2 = parent;
        }
        throw new RuntimeException("Recursion out of limit");
    }

    public static Island findDescendant(Island parent, Boolf<Island> boolf){
        if(parent == null) return null;
        if(boolf.get(parent)) return parent;
        if(parent.content instanceof ChildrenContent cc){
            for(Island child : cc.getChildren()){
                var result = findDescendant(child, boolf);
                if(result != null) return result;
            }
        }
        return null;
    }

    public static Island findIsland(Island root, int id){
        return findDescendant(root, i -> i.id == id);
    }

    /** 反序列化一条island分支 */
    public static Island json2Island(String json){
        return JsonUtils.json.fromJson(Island.class, json);
    }

    /** 序列化一条island分支 */
    public static String island2Json(Island island){
        return JsonUtils.json.toJson(island);
    }

    public static void regenerateIdsRecursive(Island island){
        runRecursive(island, isle -> isle.id = Island.newId());
    }

    public static void runRecursive(Island island, Cons<Island> cons){
        if(island == null) return;
        cons.get(island);
        if(island.content instanceof ChildrenContent cc){
            for(Island child : cc.getChildren()){
                runRecursive(child, cons);
            }
        }
    }

    /** 检查树拓扑是否无环 */
    public static boolean isAcyclic(Island tree){
        ObjectSet<Island> set = new ObjectSet<>();
        return isAcyclicRecursive(tree, set);
    }

    private static boolean isAcyclicRecursive(Island tree, ObjectSet<Island> set){
        // 自己有环
        if(!set.add(tree)) return false;
        // 子级有环
        if(tree.content instanceof ChildrenContent cc){
            for(Island child : cc.getChildren()){
                if(!isAcyclicRecursive(child, set)){
                    return false;
                }
            }
        }
        return true;
    }

    public static void rebuildLinks(Island island){
        rebuildLinksRecursive(island, null);
    }

    private static void rebuildLinksRecursive(Island island, Island parent){
        island.setParentIsland(parent);

        if(island.content instanceof ChildrenContent cc){
            for(Island child : cc.getChildren()){
                rebuildLinksRecursive(child, island);
            }
        }
    }
}
