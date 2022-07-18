package mi2u.input;

import arc.Events;
import arc.struct.*;
import mindustry.game.EventType;
import mindustry.gen.*;

import static mindustry.Vars.*;

public class RtsCommand{
    public static Seq<Unit>[] formations = new Seq[10];
    public static boolean creatingFormation = false;

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
        updateFormation(id);
        if(formations[id] == null) return false;
        if(formations[id].isEmpty()) return false;
        return true;
    }

    public static void updateFormation(int id){
        if(formations[id] == null) return;
        if(formations[id].isEmpty()) return;
        formations[id].removeAll(unit -> unit == null || !unit.isValid() || unit.team() != player.team() || !unit.isCommandable());
    }

    public static void callFormation(int id){
        if(!checkFormation(id)) return;
        control.input.selectedUnits.clear();
        control.input.selectedUnits.add(formations[id]);
    }

}
