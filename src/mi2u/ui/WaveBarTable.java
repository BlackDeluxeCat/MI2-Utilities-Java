package mi2u.ui;

import arc.*;
import arc.graphics.Color;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;

import static mindustry.Vars.*;

public class WaveBarTable extends Table{
    public int curWave = 1;
    public Seq<WaveData> waves = new Seq<WaveData>();
    public Seq<UnitData> allunits = new Seq<UnitData>();
    public Interval timer = new Interval();
    protected Seq<MI2Bar> hpBars = new Seq<MI2Bar>();
    protected int[] prewave = {1,2,3,4,5};

    public WaveBarTable(){
        super();

        Events.on(WorldLoadEvent.class, e -> {
            clearData();
            Time.run(Math.min(state.rules.waveSpacing, 60f), () -> {
                catchWave();
            });
        });

        Events.on(WaveEvent.class, e -> {
            Time.run(Math.min(state.rules.waveSpacing, 60f), () -> {
                catchWave();
            });
        });

        update(() -> {
            if(!timer.get(19f)) return;
            if(!state.isGame()){
                clearData();
                return;
            }
            allunits.remove(unit -> unit.unit == null || unit.unit.dead());
            state.teams.get(state.rules.waveTeam).units.each(u -> {
                if(allunits.contains(data -> data.unit == u)) return;
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
            hpBars.get(i).set(() -> "Wave " + d.wave + ": " + Strings.fixed(d.units.sumf(unitdata -> Math.max(unitdata.unit.health(), 0) + unitdata.unit.shield()), 1) + "/" + Strings.fixed(d.totalHp + d.totalShield, 1),
                () -> (curWave > d.wave || d.units.sumf(unitdata -> Math.max(unitdata.unit.health(), 0) + unitdata.unit.shield()) > 0) ? d.units.sumf(unitdata -> Math.max(unitdata.unit.health(), 0) + unitdata.unit.shield()) / (d.totalHp + d.totalShield) : 1f,
                (curWave > d.wave || d.units.sumf(unitdata -> Math.max(unitdata.unit.health(), 0) + unitdata.unit.shield()) > 0) ? Color.scarlet : Color.cyan
                );
            hpBars.get(i).blink(Color.white);
            add(hpBars.get(i)).growX().height(18f);
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
    }

    public void updateData(){
        waves.each(waved -> {
            waved.removeDead();
        });
        waves.remove(waved -> waved.units.isEmpty());
    }

    public class WaveData{
        int wave = 1;
        float totalHp = 1f, totalShield = 1f;
        Seq<UnitData> units = new Seq<UnitData>();
        public WaveData(int wave){
            this.wave = wave;
            init();
        }

        public void init(){
            totalHp = totalShield = 0f;
            for(SpawnGroup group : state.rules.spawns){
                totalHp += group.type.health * group.getSpawned(wave - 1) * spawner.countSpawns();
                totalShield += group.getShield(wave - 1) * group.getSpawned(wave - 1) * spawner.countSpawns();
            }
        }

        public void removeDead(){
            units.remove(unit -> unit.unit == null || unit.unit.dead() || unit.unit.health <= 0);
        }
    }

    public class UnitData{
        public Unit unit;
        public float spawnTime = 0f;
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
