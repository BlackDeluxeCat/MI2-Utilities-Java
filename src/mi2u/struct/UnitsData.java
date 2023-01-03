package mi2u.struct;

import arc.struct.*;
import arc.util.*;
import mi2u.ui.*;
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

        allwaves.removeAll(waved -> waved.units.isEmpty() && Math.abs(state.wave - 1 - waved.wave) > 3 && (waved.wave - MapInfoTable.curWave < 0 || waved.wave - MapInfoTable.curWave > 50));
        int cs = 0;
        while(cs < 50){
            int css = cs;
            if(!allwaves.contains(waveData -> waveData.wave == MapInfoTable.curWave + css)) allwaves.add(new WaveData(MapInfoTable.curWave + css));
            cs++;
        }

        allwaves.sort(waveData -> waveData.wave);
    }

    public static void catchWave(){
        int wa = Math.max(state.wave - 1 - 1, 0);//state.wave is updated before WaveEvent, thus the spawned unit should be signed to wave-1
        if(!allwaves.contains(waveData -> waveData.wave == wa)) allwaves.add(new WaveData(wa));
        Seq<UnitData> toAdd = allunits.select(unitData -> unitData.wave == -1 && unitData.unit.team.id == state.rules.waveTeam.id && unitData.getSurviveTime() < 240f);
        toAdd.each(unitData -> unitData.wave = wa);
        allwaves.select(waveData -> waveData.wave == wa).first().addAll(toAdd);
    }

    public static class WaveData{
        public int wave;
        public float totalHp = 1f, totalShield = 1f;
        public float[] totalsByType = new float[content.units().size];
        public Seq<UnitData> units = new Seq<>();
        public Seq<UnitData>[] unitsByType = new Seq[content.units().size];
        Interval time = new Interval();
        float sumHp;
        public WaveData(int wave){
            this.wave = wave;
            updateSpawnInfo();
        }

        public void updateSpawnInfo(){
            totalHp = totalShield = 0f;

            for(SpawnGroup group : state.rules.spawns){
                int countSpawns = group.type.flying ? WorldData.countFlyingSpawner(group.spawn) : WorldData.countGroundSpawner(group.spawn);
                int countUnits = group.getSpawned(wave) * countSpawns;
                totalHp += group.type.health * countUnits;
                totalShield += group.getShield(wave ) * countUnits;
                totalsByType[group.type.id] += (group.type.health + group.getShield(wave)) * countUnits;
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
            units.removeAll(unit -> unit.unit == null || !unit.unit.isValid());
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
