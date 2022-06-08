package mi2u.ui;

import arc.*;
import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mi2u.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class WaveBarTable extends Table{
    public int curWave = 1;
    public Seq<WaveData> waves = new Seq<>();
    public Seq<UnitData> allunits = new Seq<>();
    ObjectSet<Unit> savedunits = new ObjectSet<>();
    public Interval timer = new Interval();

    protected Seq<MI2Bar> hpBars = new Seq<>();
    protected PopupTable detailt = new PopupTable();

    protected int[] prewave = {1,2,3,4,5};
    public int previewedWave = 0;

    //temp
    private static boolean any = false;

    public WaveBarTable(){
        super();

        Events.on(WorldLoadEvent.class, e -> {
            clearData();
            SpawnerData.update();
            Time.run(Math.min(state.rules.waveSpacing, 60f), this::catchWave);
        });

        Events.on(CoreChangeEvent.class, e -> SpawnerData.update());

        Events.on(WaveEvent.class, e -> Time.run(Math.min(state.rules.waveSpacing, 60f), () -> {
            SpawnerData.update();
            catchWave();
        }));

        update(() -> {
            if(!timer.get(19f)) return;
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
        for(int i = 0; i < waves.size; i++){
            if(i >= hpBars.size) hpBars.add(new MI2Bar());
            WaveData d = waves.get(i);
            add(hpBars.get(i)).growX().height(18f).minWidth(200f).with(bar -> {
                bar.clearListeners();
                bar.set(() -> "Wave " + d.wave + ": " + "(" + d.units.size + ") " + UI.formatAmount((long)d.units.sumf(unitData -> Math.max(unitData.unit.health(), 0) + unitData.unit.shield())) + "/" + UI.formatAmount((long)(d.totalHp + d.totalShield)),
                        () -> {
                            float uhp =  d.units.sumf(unitData -> Math.max(unitData.unit.health(), 0) + unitData.unit.shield());
                            if(curWave <= d.wave && uhp <= 0f) return 1f;
                            return uhp / (d.totalHp + d.totalShield);
                        },
                        (curWave > d.wave || d.units.sumf(unitData -> Math.max(unitData.unit.health(), 0) + unitData.unit.shield()) > 0) ? Color.scarlet : Color.cyan);
                bar.blink(Color.white).outline(MI2UTmp.c2.set(0.3f, 0.3f, 0.6f, 0.3f), 1f).setFontScale(0.8f);
                bar.tapped(() -> {
                    if(detailt.shown) detailt.hide();
                    detailt.clear();
                    detailt.background(Styles.black8);
                    detailt.addCloseButton();
                    detailt.addDragMove();
                    buildDetails(detailt, d);
                    detailt.setPositionInScreen(Core.input.mouseX(), Core.input.mouseY());
                    detailt.popup();
                });
            });
            row();
        }
    }

    public void buildDetails(Table t, WaveData data){
        t.add("Wave " + data.wave).growX().minSize(80f, 36f);
        t.row();
        t.pane(p -> {
            p.table(groupt -> {
                int i = 0;
                for(SpawnGroup group : state.rules.spawns){
                    if(group.getSpawned(data.wave - 1) < 1) continue;
                    groupt.table(g -> {
                        g.table(eip -> {
                            if(group.effect != null && group.effect != StatusEffects.none) eip.image(group.effect.uiIcon).size(12f);
                            if(group.items != null) eip.image(group.items.item.uiIcon).size(12f);
                            if(group.payloads!=null && !group.payloads.isEmpty()) eip.add("" + Iconc.units).get().setFontScale(0.7f);
                            eip.image(group.type.uiIcon).size(18f);
                            eip.add("x" + group.getSpawned(data.wave - 1)).get().setFontScale(0.7f);
                        });
                        g.row();
                        g.add("" + group.getShield(data.wave - 1)).get().setFontScale(0.7f);
                    }).pad(2f);
                    if(++i >= 5){
                        i = 0;
                        groupt.row();
                    }
                }
            });

            p.row();

            p.table(t2 -> {
                for(int id = 0; id < data.totalsByType.length; id++){
                    if(data.totalsByType[id] <= 1f) continue;
                    var type = content.unit(id);
                    t2.image(type.uiIcon).size(18f);
                    t2.add(new MI2Bar()).with(bar -> {
                        bar.set(() -> {
                                    Seq<UnitData> units = data.unitsByType[type.id];
                                    if(units == null) return UI.formatAmount((long)data.totalsByType[type.id]);
                                    float hp = data.unitsByType[type.id].sumf(udata -> udata.unit.health + udata.unit.shield);
                                    return units.size + "|" + UI.formatAmount((long)hp) + "/" + UI.formatAmount((long)data.totalsByType[type.id]);
                                },
                                () -> {
                                    Seq<UnitData> units = data.unitsByType[type.id];
                                    if (units == null) return 1f;
                                    float hp = data.unitsByType[type.id].sumf(udata -> udata.unit.health + udata.unit.shield);
                                    return hp / data.totalsByType[type.id];
                                }, Color.scarlet);
                        bar.setFontScale(0.8f).blink(Color.white);
                    }).height(10f).minWidth(100f).growX();
                    t2.row();
                }
            }).growX();
        }).maxHeight(300f).update(p -> {
            Element e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
            if(e != null && e.isDescendantOf(p)){
                p.requestScroll();
            }else if(p.hasScroll()){
                Core.scene.setScrollFocus(null);
            }
        });
    }

    public void catchWave(){
        if(!waves.contains(waveData -> waveData.wave == curWave)) waves.add(new WaveData(curWave)); //curWave is updated first, thus the spawned unit should be signed to wave-1
        Seq<UnitData> toAdd = allunits.select(unitData -> unitData.wave == -1 && unitData.getSurviveTime() < 240f);
        toAdd.each(unitData -> unitData.wave = curWave);
        waves.select(waveData -> waveData.wave == curWave).first().addAll(toAdd);
        curWave = state.wave;
    }

    public void clearData(){
        waves.clear();
        allunits.clear();
        savedunits.clear();
        curWave = 1;
    }

    public void updateData(){
        waves.each(WaveData::removeDead);
        waves.removeAll(waved -> {
            boolean preview = false;
            for(int i : prewave){
                preview = waved.wave == curWave - 1 + i;
                if(preview) break;
            }
            return !preview && waved.wave != previewedWave && waved.units.isEmpty();
        });

        for(int i : prewave){
            if(!waves.contains(waveData -> waveData.wave == curWave - 1 + i)) waves.add(new WaveData(curWave - 1 + i));
        }
        if(!waves.contains(waveData -> waveData.wave == previewedWave)) waves.add(new WaveData((previewedWave)));

        waves.sort(waveData -> (float)waveData.wave);
    }

    public class WaveData{
        int wave;
        float totalHp = 1f, totalShield = 1f;
        float[] totalsByType = new float[content.units().size];
        Seq<UnitData> units = new Seq<>();
        Seq<UnitData>[] unitsByType;
        public WaveData(int wave){
            this.wave = wave;
            init();
        }

        public void init(){

            unitsByType = new Seq[content.units().size];

            totalHp = totalShield = 0f;

            for(SpawnGroup group : state.rules.spawns){
                float spawns = group.type.flying ? SpawnerData.countFlying(group.spawn) : SpawnerData.countGround(group.spawn);

                totalHp += group.type.health * group.getSpawned(wave - 1) * spawns;
                totalShield += group.getShield(wave - 1) * group.getSpawned(wave - 1) * spawns;
                totalsByType[group.type.id] += (group.type.health + group.getShield(wave - 1)) * group.getSpawned(wave - 1) * spawns;
            }
        }

        public void addAll(Seq<UnitData> unit){
            units.addAll(unit);
            unit.each(unitd -> {
                int id = unitd.unit.type().id;
                if(unitsByType[id] == null) unitsByType[id] = new Seq<UnitData>();
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

    public class UnitData{
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

    public static class SpawnerData{
        public static Seq<Integer> spawnPoints = new Seq<>();
        public static Seq<Integer> groundSpawns = new Seq<>();

        public static void update(){
            spawnPoints.clear();
            groundSpawns.clear();

            for(Tile tile : spawner.getSpawns()){
                spawnPoints.add(tile.pos());
                groundSpawns.add(tile.pos());
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

        public static int countGround(int pos){
            return pos == -1 ? groundSpawns.size : groundSpawns.contains(pos) ? 1 : 0;
        }

        public static int countFlying(int pos){
            return pos == -1 ? spawnPoints.size : spawnPoints.contains(pos) ? 1 : 0;
        }
    }
}
