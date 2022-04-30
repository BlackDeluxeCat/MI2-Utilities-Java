package mi2u.ui;

import arc.*;
import arc.graphics.Color;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mi2u.MI2UTmp;
import mi2u.input.InputOverwrite;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;

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
            hpBars.get(i).blink(Color.white);
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
        waves.each(data -> {
            data.removeDead();
        });
        waves.removeAll(waved -> waved.units.isEmpty());
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
                totalHp += group.type.health * group.getSpawned(wave - 1) * (group.type.flying ? spawner.countFlyerSpawns() : spawner.countGroundSpawns());
                totalShield += group.getShield(wave - 1) * group.getSpawned(wave - 1) * (group.type.flying ? spawner.countFlyerSpawns() : spawner.countGroundSpawns());
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
