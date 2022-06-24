package mi2u.struct;

import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class WorldData{

    /** block - team.id - building positions
     * key team.id == 256 is for blocks that block.hasBuilding() is false, or all positions ignoring team check.
     * */
    public static ObjectMap<Block, IntSeq[]> tiles = new ObjectMap<>();
    private static ObjectSet<Building> scanned = new ObjectSet<>();

    public static void clear(){
        scanned.clear();
        tiles.each((b, array) -> {
            for(var intSeq : array){
                if(intSeq != null) intSeq.clear();
            }
        });
    }

    @Nullable
    public static IntSeq getSeq(Block block, @Nullable Team team){
        if(tiles.get(block) == null) return null;
        if(team != null && block.hasBuilding()){
            return tiles.get(block)[team.id];
        }else{
            return tiles.get(block)[256];
        }
    }

    public static int countBlock(Block block, @Nullable Team team){
        var seq = getSeq(block, team);
        return seq == null ? 0 : seq.size;
    }

    public static void scanWorld(){
        clear();
        for(var tile : world.tiles){
            putBlock(tile.floor(), tile.pos(), 256);
            if(tile.overlay() != tile.floor()) putBlock(tile.overlay(), tile.pos(), 256);
            if(tile.block() != tile.floor() && tile.block() != tile.overlay()){
                if(tile.build != null && tile.block().isMultiblock()){
                    if(scanned.contains(tile.build)) continue;
                    scanned.add(tile.build);
                }
                putBlock(tile.block(), tile.pos(), 256);
                if(tile.build != null) putBlock(tile.block(), tile.pos(), tile.build.team.id);
            }
        }
    }

    //TODO null checks cost large amount of memory.
    private static void putBlock(Block block, int pos, int team){
        if(block == null) return;
        if(tiles.get(block) == null) tiles.put(block, new IntSeq[257]);
        if(tiles.get(block)[team] == null) tiles.get(block)[team] = new IntSeq();
        tiles.get(block)[team].add(pos);
    }

    public static class WorldFinder{
        public Block findTarget = Blocks.air;
        public int findIndex = 0;
        @Nullable public Team team;

        public int findNext(){
            if(getSeq(findTarget, team) == null){
                findIndex = 0;
                return -1;
            }
            int size = getSeq(findTarget, team).size;
            if(size == 0) return -1;
            if(findIndex >= size) findIndex = 0;
            return getSeq(findTarget, team).get(findIndex++);
        }
    }
}
