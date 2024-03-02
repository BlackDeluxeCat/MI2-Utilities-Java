package mi2u.input;

import arc.*;
import arc.struct.*;
import arc.util.*;
import mi2u.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.input.*;
import mindustry.world.blocks.units.*;
import mindustry.world.meta.*;

import static mi2u.MI2UVars.*;
import static mindustry.Vars.*;

public class RtsCommand{
    public static Seq<Formation> formations = new Seq<>(10);
    public static boolean creatingFormation = false;

    public static long lastCallTime;
    public static int lastCallId;
    public static long doubleTapInterval = mi2ui.settings.getInt("rtsFormDoubleTap", 300);


    public static boolean lowHealthBack = false;    //TODO formation auto control

    public static void init(){
        for(int i = 0; i < 10; i++){
            formations.add(new Formation(i));
        }
        Events.on(EventType.WorldLoadEvent.class, e -> {
            formations.each(Formation::clear);
        });

        Events.run(EventType.Trigger.update, () -> {
            formations.each(Formation::update);
        });
    }

    public static void createFormation(Seq<Unit> formation, int id){
        formations.get(id).newFormation(formation);
    }

    /** @return whether it is a valid formation*/
    public static boolean checkFormation(int id){
        return countFormation(id) > 0;
    }

    public static int countFormation(int id){
        return formations.get(id).count();
    }

    public static void callFormation(int id){
        if(!checkFormation(id)) return;
        var form = formations.get(id);
        if(lastCallId == id && Time.timeSinceMillis(lastCallTime) < doubleTapInterval){
            if(control.input instanceof InputOverwrite iow){
                iow.pan(true, MI2UTmp.v1.set(form.all.random()));
            }else{
                Core.camera.position.set(MI2UTmp.v1.set(form.all.random()));
            }

        }else{
            control.input.selectedUnits.clear();
            control.input.selectedUnits.add(form.all.select(u -> !form.lowHps.contains(u)));
            lastCallId = id;
        }
        lastCallTime = Time.millis();
    }

    public static void desktopFormation(){
        if(control.input.commandMode){
            if(Core.input.keyDown(Binding.control)) creatingFormation = true;
            if(Core.input.keyRelease(Binding.control)) creatingFormation = false;
            //force block selection short-cut to switch category
            MI2Utils.setValue(ui.hudfrag.blockfrag, "blockSelectEnd", true);
            //cancel any stored block selections
            ObjectMap selectBlocks = MI2Utils.getValue(ui.hudfrag.blockfrag, "selectedBlocks");
            selectBlocks.each((cat, block) -> selectBlocks.put(cat, null));
            for(int ki = 0; ki < DesktopInputExt.numKey.length; ki++){
                if(Core.input.keyTap(DesktopInputExt.numKey[ki])){
                    if(creatingFormation){
                        createFormation(control.input.selectedUnits, ki);
                    }else{
                        callFormation(ki);
                    }
                }
            }
        }
    }

    public static class Formation{
        private static Seq<Unit> tmplows = new Seq<>(), tmpselect;
        public int id;
        protected Seq<Unit> all = new Seq<>();
        protected OrderedSet<Unit> lowHps = new OrderedSet<>();
        protected MI2Utils.IntervalMillis timer = new MI2Utils.IntervalMillis();

        public Formation(int id){
            this.id = id;
        }

        public void newFormation(Seq<Unit> units){
            clear();
            all.add(units);
        }

        public void clear(){
            all.clear();
            lowHps.clear();
        }

        public int count(){
            return all.size;
        }

        public void update(){
            all.removeAll(unit -> {
                boolean remove = unit == null || !unit.isValid() || unit.team() != player.team() || !unit.isCommandable();
                if(!remove) return false;
                lowHps.remove(unit);
                return true;
            });

            if(lowHealthBack){
                if(timer.get(500)){
                    tmpselect = control.input.selectedUnits;
                    tmplows.clear();

                    //标记低血量
                    all.each(unit -> {
                        if(unit.health / unit.maxHealth > 0.99f){
                            if(lowHps.remove(unit)) control.input.selectedUnits.add(unit);
                        }
                        if(unit.health / unit.maxHealth > 0.5f) return;
                        if(!lowHps.add(unit)) return;
                        tmplows.add(unit);
                    });

                    //低血量召回核心一次
                    if(!tmplows.isEmpty()){
                        control.input.selectedUnits = tmplows;

                        var unit = tmplows.first();
                        var build = Units.closestBuilding(unit.team, unit.x, unit.y, 800f, b -> b.block.flags.contains(BlockFlag.repair) || b.block instanceof RepairTower);
                        if(build == null){
                            build = indexer.findClosestFlag(unit.x, unit.y, unit.team(), BlockFlag.core);
                        }
                        if(build != null){
                            Core.camera.project(MI2UTmp.v2.set(build.x, build.y));
                            control.input.commandTap(MI2UTmp.v2.x, MI2UTmp.v2.y);

                            tmpselect.remove(u -> tmplows.contains(u));
                        }
                        control.input.selectedUnits = tmpselect;
                    }
                }
            }else{
                lowHps.clear();
            }
        }
    }
}
