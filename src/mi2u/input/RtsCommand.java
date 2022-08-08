package mi2u.input;

import arc.*;
import arc.struct.*;
import arc.util.Time;
import mi2u.MI2UTmp;
import mi2u.io.MI2USettings;
import mindustry.game.EventType;
import mindustry.gen.*;

import static mindustry.Vars.*;

public class RtsCommand{
    public static Seq<Unit>[] formations = new Seq[10];
    public static boolean creatingFormation = false;

    public static long lastCallTime;
    public static int lastCallId;
    public static long doubleTapInterval = MI2USettings.getInt("rtsFormDoubleTap", 300);


    public static void init(){
        Events.on(EventType.WorldLoadEvent.class, e -> {
            for(var form : formations){
                if(form == null) continue;
                form.clear();
            }
        });
    }

    public static void createFormation(Seq<Unit> formation, int id){
        if(formations[id] == null){
            formations[id] = new Seq<Unit>(formation);
        }else{
            formations[id].clear();
            formations[id].add(formation);
        }
    }

    /** @return whether it is a valid formation*/
    public static boolean checkFormation(int id){
        return countFormation(id) > 0;
    }

    public static int countFormation(int id){
        updateFormation(id);
        if(formations[id] == null) return 0;
        if(formations[id].isEmpty()) return 0;
        return formations[id].size;
    }

    public static void updateFormation(int id){
        if(formations[id] == null) return;
        if(formations[id].isEmpty()) return;
        formations[id].removeAll(unit -> unit == null || !unit.isValid() || unit.team() != player.team() || !unit.isCommandable());
    }

    public static void callFormation(int id){
        if(!checkFormation(id)) return;
        if(lastCallId == id && Time.timeSinceMillis(lastCallTime) < doubleTapInterval && control.input instanceof InputOverwrite iow){
            iow.pan(true, MI2UTmp.v1.set(formations[id].random()));
        }else{
            control.input.selectedUnits.clear();
            control.input.selectedUnits.add(formations[id]);
            lastCallId = id;
        }
        lastCallTime = Time.millis();
    }

}
