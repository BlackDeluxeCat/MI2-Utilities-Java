package mi2u.ui;

import arc.*;
import arc.func.Floatc;
import arc.func.Floatc2;
import arc.graphics.Color;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mi2u.MI2UTmp;
import mi2u.input.InputOverwrite;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.world.Tile;

import java.util.Set;

import static mindustry.Vars.*;

public class WaveBarTable extends Table{
    public int curWave = 1;
    public Seq<WaveData> waves = new Seq<>();
    public Seq<UnitData> allunits = new Seq<>();
    ObjectSet<Unit> savedunits = new ObjectSet<>();
    public Interval timer = new Interval(2);
    protected Seq<MI2Bar> hpBars = new Seq<>();
    protected int[] prewave = {1,2,3,4,5};

    private int tmpCount = 0;
    private boolean any = false;

    public WaveBarTable(){
        super();

        Events.on(WorldLoadEvent.class, e -> {
            clearData();
            Time.run(Math.min(state.rules.waveSpacing, 60f), this::catchWave);
        });

        Events.on(WaveEvent.class, e -> Time.run(Math.min(state.rules.waveSpacing, 60f), this::catchWave));

        update(() -> {
            if(!timer.get(0, 19f)) return;
            if(!state.isGame()){
                clearData();
                return;
            }
            allunits.remove(unit -> {
                boolean remove = unit.unit == null || !unit.unit.isValid();
                if(remove) savedunits.remove(unit.unit);
                return remove;
            });
            state.teams.get(state.rules.waveTeam).units.each(u -> {
                if(!savedunits.add(u)) return;
                allunits.add(new UnitData(u, -1));
            });
            updateData();
            setupBars();
        });
    }

    public void setupBars(){
        clear();
        for(int i : prewave){
            if(!waves.contains(wavedata -> wavedata.wave == curWave - 1 + i)) waves.add(new WaveData(curWave - 1 + i));
        }
        waves.sort(wavedata -> (float)wavedata.wave);
        for(int i = 0; i < waves.size; i++){
            if(i >= hpBars.size) hpBars.add(new MI2Bar());
            WaveData d = waves.get(i);
            hpBars.get(i).set(() -> "Wave " + d.wave + ": " + "(" + d.units.size + ") " + UI.formatAmount((long)d.units.sumf(unitdata -> Math.max(unitdata.unit.health(), 0) + unitdata.unit.shield())) + "/" + UI.formatAmount((long)(d.totalHp + d.totalShield)),
                () -> (curWave > d.wave || d.units.sumf(unitdata -> Math.max(unitdata.unit.health(), 0) + unitdata.unit.shield()) > 0) ? d.units.sumf(unitdata -> Math.max(unitdata.unit.health(), 0) + unitdata.unit.shield()) / (d.totalHp + d.totalShield) : 1f,
                (curWave > d.wave || d.units.sumf(unitdata -> Math.max(unitdata.unit.health(), 0) + unitdata.unit.shield()) > 0) ? Color.scarlet : Color.cyan
                );
            hpBars.get(i).blink(Color.white).outline(MI2UTmp.c2.set(0.3f, 0.3f, 0.6f, 0.3f), 1f);
            add(hpBars.get(i)).growX().height(18f).minWidth(200f);
            row();
        }
    }

    public void catchWave(){
        if(!waves.contains(wavedata -> wavedata.wave == curWave)) waves.add(new WaveData(curWave)); //curWave is updated first, thus the spawned unit should be signed to wave-1
        Seq<UnitData> toAdd = allunits.select(unitdata -> unitdata.wave == -1 && unitdata.getSurviveTime() < 240f);
        toAdd.each(unitdata -> unitdata.wave = curWave);
        waves.select(wavedata -> wavedata.wave == curWave).first().units.addAll(toAdd);
        curWave = state.wave;
    }

    public void clearData(){
        waves.clear();
        allunits.clear();
        savedunits.clear();
        curWave = state.wave;
    }

    public void updateData(){
        waves.each(WaveData::removeDead);
        waves.removeAll(waved -> {
            boolean preview = false;
            for(int i : prewave){
                if(preview = waved.wave == curWave - 1 + i) break;
            }
            return !preview && waved.units.isEmpty();
        });
    }

    public class WaveData{
        int wave;
        float totalHp = 1f, totalShield = 1f;
        Seq<UnitData> units = new Seq<>();
        public WaveData(int wave){
            this.wave = wave;
            init();
        }

        public void init(){
            totalHp = totalShield = 0f;
            for(SpawnGroup group : state.rules.spawns){
                tmpCount = 0;
                int filterPos = group.spawn;

                //rewrite Anuke's, as invoking private method "eachGroundSpawn" with private interface param is too hard for me.
                if(group.type.flying){
                    for(Tile tile : spawner.getSpawns()){
                        if(filterPos != -1 && filterPos != tile.pos()) continue;
                        tmpCount++;
                    }

                    if(state.rules.attackMode && state.teams.isActive(state.rules.waveTeam)){
                        for(Building core : state.rules.waveTeam.data().cores){
                            if(filterPos != -1 && filterPos != core.pos()) continue;
                            tmpCount++;
                        }
                    }
                }else{
                    if(state.hasSpawns()){
                        for(Tile spawn : spawner.getSpawns()){
                            if(filterPos != -1 && filterPos != spawn.pos()) continue;
                            tmpCount++;
                        }
                    }

                    if(state.rules.attackMode && state.teams.isActive(state.rules.waveTeam) && !state.teams.playerCores().isEmpty()){
                        Building firstCore = state.teams.playerCores().first();
                        for(Building core : state.rules.waveTeam.cores()){
                            if(filterPos != -1 && filterPos != core.pos()) continue;

                            Tmp.v1.set(firstCore).sub(core).limit(16f + core.block.size * tilesize /2f * Mathf.sqrt2);

                            boolean valid = false;
                            int steps = 0;

                            //keep moving forward until the max step amount is reached
                            while(steps++ < 30f){
                                int tx = World.toTile(core.x + Tmp.v1.x), ty = World.toTile(core.y + Tmp.v1.y);
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
                                    Tmp.v1.setLength(Tmp.v1.len() + tilesize*1.1f);
                                }
                            }

                            if(valid) tmpCount++;
                        }
                    }
                }

                totalHp += group.type.health * group.getSpawned(wave - 1) * tmpCount;
                totalShield += group.getShield(wave - 1) * group.getSpawned(wave - 1) * tmpCount;
            }
        }

        public Seq<UnitData> removeDead(){
            return units.removeAll(unit -> unit.unit == null || !unit.unit.isValid() || unit.unit.dead() || unit.unit.health <= 0);
        }
    }

    public class UnitData{
        public Unit unit;
        public float spawnTime;
        public int wave = 1;

        public UnitData(Unit unit, int wave){
            this.unit = unit;
            this.wave = wave;
            spawnTime = Time.time;
        }

        public float getSurviveTime(){
            return Time.time - spawnTime;
        }
    }
}
