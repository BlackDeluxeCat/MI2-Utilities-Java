package mi2u.struct;

import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class WorldData{

    /** block - building positions */
    public static ObjectMap<Block, IntSeq> tiles = new ObjectMap<>();
    private static ObjectSet<Building> scanned = new ObjectSet<>();

    public static void clear(){
        tiles.clear();
    }

    public static int countBlock(Block block, @Nullable Team team){
        if(!tiles.containsKey(block) || tiles.get(block).isEmpty()) return 0;
        return tiles.get(block).size;
    }

    public static void scanWorld(){
        tiles.clear();
        scanned.clear();
        for(var tile : world.tiles){
            putBlock(tile.floor(), tile.pos());
            if(tile.overlay() != tile.floor()) putBlock(tile.overlay(), tile.pos());
            if(tile.block() != tile.floor() && tile.block() != tile.overlay()){
                if(tile.build != null && tile.block().isMultiblock()){
                    if(scanned.contains(tile.build)) continue;
                    scanned.add(tile.build);
                }
                putBlock(tile.block(), tile.pos());
            }
        }
    }

    private static void putBlock(Block block, int pos){
        if(block == null) return;
        if(tiles.get(block) == null) tiles.put(block, new IntSeq());
        tiles.get(block).add(pos);
    }

    public static class WorldFinder{
        public Block findTarget = Blocks.air;
        public int findIndex = 0;
        @Nullable public Team team;

        public int findNext(){
            if(!tiles.containsKey(findTarget)) scanWorld();
            var seq = tiles.get(findTarget);
            if(seq == null || seq.isEmpty()) return -1;

            boolean teamCheck = team != null && findTarget.hasBuilding();
            int pos, count = 0;
            do{
                if(findIndex >= seq.size) findIndex = 0;
                pos = seq.get(findIndex++);
                if(count++ > seq.size) return -1;
            }while(teamCheck && world.tiles.getp(pos).team() != team);
            return pos;
        }
    }
}
