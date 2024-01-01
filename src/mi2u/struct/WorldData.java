package mi2u.struct;

import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.struct.*;
import arc.util.*;
import mi2u.MI2UTmp;
import mi2u.io.MI2USettings;
import mindustry.content.*;
import mindustry.core.World;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class WorldData{

    /** block - team.id - building positions
     * key team.id == 256 is for blocks that block.hasBuilding() is false, or all positions ignoring team check.
     * */
    public static ObjectMap<Block, IntSeq[]> tiles, tiles2;//尊贵的离屏更新模式
    static ObjectMap<Block, IntSeq[]> backTiles1 = new ObjectMap<>(), backTiles2 = new ObjectMap<>();
    private static ObjectSet<Building> scanned = new ObjectSet<>();

    public static IntSeq spawnPoints = new IntSeq();
    public static IntSeq groundSpawns = new IntSeq();
    public static Seq<Integer> usedSpawns = new Seq<>();

    //temp
    static boolean any = false;
    static int index;

    public static void clear(){
        index = 0;
        scanned.clear();
        tiles = backTiles1;
        tiles2 = backTiles2;
        tiles.each((b, array) -> {
            for(var intSeq : array){
                if(intSeq != null) intSeq.clear();
            }
        });
        tiles2.each((b, array) -> {
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

    public static void scanWorld(int times){
        for(int i = 0; i < times; i++){
            scanTile(world.tiles.geti(index++));
            if(index >= world.tiles.width * world.tiles.height){
                 index = 0;
                if(tiles == backTiles1){
                    tiles = backTiles2;
                    tiles2 = backTiles1;
                }else{
                    tiles = backTiles1;
                    tiles2 = backTiles2;
                }
                scanned.clear();
                tiles2.each((b, array) -> {
                    for(var intSeq : array){
                        if(intSeq != null) intSeq.clear();
                    }
                });
                break;
            }
        }
    }

    public static void scanTile(Tile tile){
        putBlock(tile.floor(), tile.pos(), 256);
        if(tile.overlay() != tile.floor()) putBlock(tile.overlay(), tile.pos(), 256);
        if(tile.block() != tile.floor() && tile.block() != tile.overlay()){
            if(tile.build != null && tile.block().isMultiblock()){
                if(scanned.contains(tile.build)) return;
                scanned.add(tile.build);
            }
            putBlock(tile.block(), tile.pos(), 256);
            if(tile.build != null){
                putBlock(tile.block(), tile.pos(), tile.build.team.id);
            }
        }
    }

    //null checks cost large amount of memory.
    private static void putBlock(Block block, int pos, int team){
        if(block == null) return;
        if(tiles2.get(block) == null) tiles2.put(block, new IntSeq[257]);
        if(tiles2.get(block)[team] == null) tiles2.get(block)[team] = new IntSeq();
        tiles2.get(block)[team].add(pos);
    }

    public static void updateSpanwer(){
        spawnPoints.clear();
        groundSpawns.clear();
        usedSpawns.clear();

        for(Tile tile : spawner.getSpawns()){
            spawnPoints.add(tile.pos());
            groundSpawns.add(tile.pos());
        }

        for(var group : state.rules.spawns){
            if(group.spawn != -1 && !usedSpawns.contains(group.spawn)) usedSpawns.add(group.spawn);
        }

        //rewrite Anuke's, as invoking private method "each.*Spawn" with private interface param is too hard for me.
        if(state.rules.attackMode && state.teams.isActive(state.rules.waveTeam) && !state.teams.playerCores().isEmpty()){
            Building firstCore = state.teams.playerCores().first();
            for(Building core : state.rules.waveTeam.cores()){
                spawnPoints.add(core.pos());

                MI2UTmp.v1.set(firstCore).sub(core).limit(16f + core.block.size * tilesize /2f * Mathf.sqrt2);

                boolean valid = false;
                int steps = 0;

                //keep moving forward until the max step amount is reached
                while(steps++ < 30f){
                    int tx = World.toTile(core.x + MI2UTmp.v1.x), ty = World.toTile(core.y + MI2UTmp.v1.y);
                    any = false;
                    Geometry.circle(tx, ty, world.width(), world.height(), 3, (x, y) -> {
                        if(world.solid(x, y)){
                            any = true;
                        }
                    });

                    //nothing is in the way, spawn it
                    if(!any){
                        valid = true;
                        break;
                    }else{
                        //make the vector longer
                        MI2UTmp.v1.setLength(MI2UTmp.v1.len() + tilesize*1.1f);
                    }
                }

                if(valid) groundSpawns.add(core.pos());
            }
        }
    }

    public static int countGroundSpawner(int pos){
        return pos == -1 ? groundSpawns.size : groundSpawns.contains(pos) ? 1 : 0;
    }

    public static int countFlyingSpawner(int pos){
        return pos == -1 ? spawnPoints.size : spawnPoints.contains(pos) ? 1 : 0;
    }

    public static class WorldFinder{
        public Block findTarget = Blocks.air;
        public int findIndex = 0, lastPos = -1;
        @Nullable public Team team;

        public int findNext(){
            return findNext(1);
        }

        public int findNext(int step){
            var seq = getSeq(findTarget, team);
            if(seq == null){
                findIndex = 0;
                return -1;
            }
            int size = seq.size;
            if(size == 0) return -1;
            lastPos = seq.get(findIndex = Mathf.mod(findIndex + step, size));
            return lastPos;
        }
    }
}
