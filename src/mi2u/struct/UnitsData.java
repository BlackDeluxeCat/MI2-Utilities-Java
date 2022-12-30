package mi2u.struct;

import arc.struct.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;

import static mindustry.Vars.*;

public class UnitsData{
    public static Seq<WaveData> allwaves = new Seq<>();
    public static Seq<UnitData> allunits = new Seq<>();
    static ObjectSet<Unit> savedunits = new ObjectSet<>();

    public static void clearData(){
        allwaves.clear();
        allunits.clear();
        savedunits.clear();
    }

    public static void updateData(){
        if(!state.isGame()){
            clearData();
            return;
        }

        //clear dead units
        allunits.remove(unit -> {
            boolean remove = unit.unit == null || !unit.unit.isValid();
            if(remove) savedunits.remove(unit.unit);
            return remove;
        });

        //catch new units
        Groups.unit.each(u -> {
            if(!savedunits.add(u)) return;
            allunits.add(new UnitData(u, -1));
        });

        //wave data cleanup
        allwaves.each(WaveData::removeDead);
        allwaves.removeAll(waved -> {
            boolean preview = false;
            for(int i = 0;i < 200; i++){
                preview = waved.wave == state.wave + i;
                if(preview) break;
            }
            return !preview && waved.units.isEmpty();
        });

        for(int i = 0;i < 200; i++){
            int ii = i;
            if(!allwaves.contains(waveData -> waveData.wave == state.wave + ii)) allwaves.add(new WaveData(state.wave + ii));
        }

        allwaves.sort(waveData -> waveData.wave);
    }

    public static void catchWave(){
        int wa = state.wave - 1;//state.wave is updated before WaveEvent, thus the spawned unit should be signed to wave-1
        if(!allwaves.contains(waveData -> waveData.wave == wa)) allwaves.add(new WaveData(wa));
        Seq<UnitData> toAdd = allunits.select(unitData -> unitData.wave == -1 && unitData.unit.team == state.rules.waveTeam && unitData.getSurviveTime() < 240f);
        toAdd.each(unitData -> unitData.wave = wa);
        allwaves.select(waveData -> waveData.wave == wa).first().addAll(toAdd);
    }

    public static class WaveData{
        public int wave;
        public float totalHp = 1f, totalShield = 1f;
        public float[] totalsByType = new float[content.units().size];
        public Seq<UnitData> units = new Seq<>();
        public Seq<UnitData>[] unitsByType;
        Interval time = new Interval();
        float sumHp;
        public WaveData(int wave){
            this.wave = wave;
            init();
        }

        public void init(){
            unitsByType = new Seq[content.units().size];
            totalHp = totalShield = 0f;

            for(SpawnGroup group : state.rules.spawns){
                int spawns = group.type.flying ? WorldData.countFlyingSpawner(group.spawn) : WorldData.countGroundSpawner(group.spawn);

                totalHp += group.type.health * group.getSpawned(wave - 1) * spawns;
                totalShield += group.getShield(wave - 1) * group.getSpawned(wave - 1) * spawns;
                totalsByType[group.type.id] += (group.type.health + group.getShield(wave - 1)) * group.getSpawned(wave - 1) * spawns;
            }
        }

        public float sumHp(){
            if(time.get(1)) sumHp = units.sumf(unitData -> Math.max(unitData.unit.health(), 0) + unitData.unit.shield());
            return sumHp;
        }

        public void addAll(Seq<UnitData> unit){
            if(units == null) units = new Seq<>();
            units.addAll(unit);
            unit.each(unitd -> {
                int id = unitd.unit.type().id;
                if(unitsByType[id] == null) unitsByType[id] = new Seq<>();
                unitsByType[id].add(unitd);
            });
        }

        public void removeDead(){
            units.removeAll(unit -> unit.unit == null || !unit.unit.isValid() || unit.unit.dead() || unit.unit.health <= 0f);
            for(var seq : unitsByType){
                if(seq != null) seq.removeAll(unit -> unit.unit == null || !unit.unit.isValid() || unit.unit.dead() || unit.unit.health <= 0f); //this remove method may cause lagging
            }
        }
    }

    public static class UnitData{
        public Unit unit;
        public float spawnTime;
        public int wave;

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
