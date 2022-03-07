package mi2u;

import arc.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.serialization.*;
import mi2u.io.*;
import mi2u.ui.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.mod.*;

import static mindustry.Vars.*;
import static mi2u.MI2UVars.*;
import static mi2u.MI2UFuncs.*;

public class MI2Utilities extends Mod{
    public static final String gitURL = "https://github.com/BlackDeluxeCat/MI2-Utilities-Java";
    public static final String gitRepo = "BlackDeluxeCat/MI2-Utilities-Java";
    public static Mods.LoadedMod MOD;

    public MI2Utilities(){
        Log.info("Loaded MI2Utilities constructor.");

        //listen for game load event
        Events.on(ClientLoadEvent.class, e -> {
            Time.runTask(20f, () -> {
                MI2USettings.init();
                MI2UVars.init();
                mi2ui.addTo(Core.scene.root);
                logicHelper.addTo(ui.logic);
                if(MI2USettings.getBool("showEmojis")) emojis.addTo(emojis.hasParent() ? emojis.parent : Core.scene.root);
                if(MI2USettings.getBool("showCoreInfo")) coreInfo.addTo(coreInfo.hasParent() ? coreInfo.parent : Core.scene.root);
                if(MI2USettings.getBool("showMindowMap")) mindowmap.addTo(coreInfo.hasParent() ? coreInfo.parent : Core.scene.root);
                if(MI2USettings.getBool("showMapInfo")) mapinfo.addTo(coreInfo.hasParent() ? coreInfo.parent : Core.scene.root);
            });

            Time.runTask(140f, () -> {
                checkUpdate();
            });
        });

        Events.run(Trigger.draw, () -> {
            if(!state.isGame()) return;
            state.teams.getActive().each(data -> {
                data.units.each(unit -> {
                    drawUnit(unit);
                });
            }); 
            if(enDistributionReveal) drawBlackboxBuilding();
        });
    }

    public void checkUpdate(){
        MOD = mods.getMod(getClass());
        new Mindow2("Update Check"){
            Interval in = new Interval();
            float delay = 900f;
            {
                addTo(Core.scene.root);
                curx = (Core.graphics.getWidth() - getPrefWidth()) / 2;
                cury = (Core.graphics.getHeight() - getRealHeight()) / 2;
                
                update(() -> {
                    toFront();
                    if(in.check(0, delay)) addTo(null);
                });
                
                hovered(() -> {
                    in.get(1);
                });
            }
            @Override
            public void setupCont(Table cont){
                cont.table(t -> {
                    t.add(gitRepo);
                    t.button("" + Iconc.paste, textb, () -> {
                        Core.app.setClipboardText(gitRepo);
                    }).size(titleButtonSize);
                    t.button("" + Iconc.github, textb, () -> {
                        Core.app.setClipboardText(gitURL);
                    }).size(titleButtonSize);
                });
                cont.row();
                cont.button("", textb, () -> this.addTo(null)).growX().height(50f).update(b -> {
                    b.setText("Close At " + Strings.fixed((delay - in.getTime(0))/60 , 1)+ "s");
                });
                cont.row();
                Http.get(ghApi + "/repos/" + gitRepo + "/releases/latest", res -> {
                    var json = Jval.read(res.getResultAsString());
                    cont.pane(t -> {
                        if(!MOD.meta.version.equals(json.getString("name"))){
                            t.add("New Release Available!").align(Align.left).fillX().pad(5f).get().setColor(0f, 1f, 0.3f, 1f);
                            t.row(); 
                            t.add(json.getString("name")).align(Align.left).fillX().pad(5f).get().setColor(1f, 1f, 0.3f, 1f);
                            t.row();
                            t.labelWrap(json.getString("body")).align(Align.left).growX();
                        }else{
                            delay = 150f;
                            t.add("Current Is Latest!").align(Align.left).fillX().pad(5f).get().setColor(0.75f, 0.25f, 1f, 1f); 
                        }
                    }).width(400f).maxHeight(600f);
                    //Log.info(json.toString());
                }, e -> {
                    delay = 150f;
                    cont.add("Failed to check update.");
                    Log.err(e);
                });

            }
            
        };
    }
}
