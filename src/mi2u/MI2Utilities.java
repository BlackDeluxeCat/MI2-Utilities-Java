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


public class MI2Utilities extends Mod{
    public static final String gitURL = "https://github.com/BlackDeluxeCat/MI2-Utilities-Java";
    public static final String gitRepo = "BlackDeluxeCat/MI2-Utilities-Java";
    public static Mods.LoadedMod MOD;

    public MI2Utilities(){

        Events.on(ClientLoadEvent.class, e -> {
            Time.runTask(40f, () -> {
                MI2USettings.init();
                MI2UVars.init();
                mi2ui.addTo(Core.scene.root);
                if(MI2USettings.getBool("showEmojis")) emojis.addTo(emojis.hasParent() ? emojis.parent : Core.scene.root);
                if(MI2USettings.getBool("showCoreInfo")) coreInfo.addTo(coreInfo.hasParent() ? coreInfo.parent : Core.scene.root);
                if(MI2USettings.getBool("showMindowMap")) mindowmap.addTo(mindowmap.hasParent() ? mindowmap.parent : Core.scene.root);
                if(MI2USettings.getBool("showMapInfo")) mapinfo.addTo(mapinfo.hasParent() ? mapinfo.parent : Core.scene.root);
                if(MI2USettings.getBool("showLogicHelper")) logicHelper.addTo(logicHelper.hasParent() ? logicHelper.parent : ui.logic);

                MI2UFuncs.schelogic();
                MI2UFuncs.initBase();
                if(MI2USettings.getBool("modifyBlockBars")) ModifyFuncs.modifyVanillaBlockBars();
            });

            //popup too early will cause font rendering bug.
            Time.runTask(140f, () -> {
                checkUpdate();
            });

        });
    }

    public void checkUpdate(){
        if(!MI2USettings.getBool("enableUpdate", false)) return;
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
                            //t.add(json.getString("body")).align(Align.left).growX();  //drawing update discription possibly cause font color bug.
                        }else{
                            in.get(1);
                            delay = 120f;
                            t.add("Current Is Latest!").align(Align.left).fillX().pad(5f).get().setColor(0.75f, 0.25f, 1f, 1f); 
                        }
                    }).width(400f).maxHeight(600f);
                    //Log.info(json.toString());
                }, e -> {
                    in.get(1);
                    delay = 120f;
                    cont.add("Failed to check update.");
                    Log.err(e);
                });

            }
            
        };
    }
}
