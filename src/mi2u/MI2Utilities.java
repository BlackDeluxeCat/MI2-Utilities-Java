package mi2u;

import arc.*;
import arc.util.*;
import mi2u.io.*;
import mindustry.game.EventType.*;
import mindustry.mod.*;

import static mindustry.Vars.*;
import static mi2u.MI2UVars.*;

public class MI2Utilities extends Mod{

    public MI2Utilities(){
        Log.info("Loaded MI2Utilities constructor.");

        //listen for game load event
        Events.on(ClientLoadEvent.class, e -> {
            Time.runTask(20f, () -> {
                MI2USettings.init();
                MI2UVars.init();
                mi2ui.addTo(Core.scene.root);
                logicHelper.addTo(ui.logic);
            });
        });

        Events.run(Trigger.draw, () -> {
            if(!state.isGame()) return;
            state.teams.getActive().each(data -> {
                data.units.each(unit -> {
                    drawUnit(unit);
                });
            }); 
        });
    }
}
