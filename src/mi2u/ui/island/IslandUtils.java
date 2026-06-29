package mi2u.ui.island;

import arc.func.*;
import mi2u.ui.island.children.*;

public class IslandUtils {
    // 便捷方法，减少instanceof样板代码
    public static boolean addChild(Island parent, Island child){
        if(parent != null && child != null && parent.content instanceof ChildrenContent cc){
            cc.addChild(child);
            return true;
        }
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

    public static void runRecursive(Island island, Cons<Island> cons){
        if(island == null) return;
        cons.get(island);
        if(island.content instanceof ChildrenContent cc){
            for(Island child : cc.getChildren()){
                runRecursive(child, cons);
            }
        }
    }
}
