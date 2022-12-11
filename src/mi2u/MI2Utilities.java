package mi2u;

import arc.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.serialization.*;
import mi2u.graphics.*;
import mi2u.input.*;
import mi2u.io.*;
import mi2u.ui.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.mod.*;

import java.util.regex.*;

import static mindustry.Vars.*;
import static mi2u.MI2UVars.*;


public class MI2Utilities extends Mod{
    public static final String gitURL = "https://github.com/BlackDeluxeCat/MI2-Utilities-Java";
    public static final String gitRepo = "BlackDeluxeCat/MI2-Utilities-Java";
    public static Mods.LoadedMod MOD;

    public MI2Utilities(){
        Events.on(ClientLoadEvent.class, e -> {
            MOD = mods.getMod(MI2Utilities.class);
            titleButtonSize = 32f;

            Mindow2.initMindowStyles();
            MI2USettings.init();
            InputUtils.init();

            maxSchematicSize = MI2USettings.getInt("maxSchematicSize", 32);
            Time.runTask(40f, () -> {
                mi2ui.addTo(Core.scene.root);
                mi2ui.visible(() -> !state.isGame() || ui.hudfrag.shown);
                if(MI2USettings.getBool("showEmojis")) emojis.addTo(emojis.hasParent() ? emojis.parent : Core.scene.root);
                emojis.visible(() -> !state.isGame() || ui.hudfrag.shown);
                if(MI2USettings.getBool("showCoreInfo")) coreInfo.addTo(coreInfo.hasParent() ? coreInfo.parent : Core.scene.root);
                coreInfo.visible(() -> !state.isGame() || ui.hudfrag.shown);
                if(MI2USettings.getBool("showMindowMap")) mindowmap.addTo(mindowmap.hasParent() ? mindowmap.parent : Core.scene.root);
                mindowmap.visible(() -> !state.isGame() || ui.hudfrag.shown);
                if(MI2USettings.getBool("showMapInfo")) mapinfo.addTo(mapinfo.hasParent() ? mapinfo.parent : Core.scene.root);
                mapinfo.visible(() -> !state.isGame() || ui.hudfrag.shown);
                if(MI2USettings.getBool("showLogicHelper", true)) logicHelper.addTo(logicHelper.hasParent() ? logicHelper.parent : ui.logic);
                if(MI2USettings.getBool("showUIContainer")) container.addTo(container.hasParent() ? container.parent : ui.logic);
                container.visible(() -> !state.isGame() || ui.hudfrag.shown);

                RendererExt.initBase();
                ModifyFuncs.modifyVanilla();
                RtsCommand.init();
                BuildingStatsPopup.init();
            });

            //popup too early will cause font rendering bug.
            Time.runTask(140f, () -> {
                if(MI2USettings.getBool("enableUpdate", true)) checkUpdate();
            });

        });

        Events.on(FileTreeInitEvent.class, e -> Core.app.post(MI2UShaders::load));
    }

    public static void checkUpdate(){
        Pattern pattern = Pattern.compile("^([hH][tT]{2}[pP]://|[hH][tT]{2}[pP][sS]://).*");
        if(!pattern.matcher(MI2USettings.getStr("ghApi", ghApi)).matches()){
            MI2USettings.putStr("ghApi", ghApi);
        }
        new Mindow2("@update.title"){
            Interval in = new Interval();
            String intro = "", version = "" + Iconc.cancel;;
            Label introl;
            float delay = 900f;
            {
                curx = (Core.graphics.getWidth() - getPrefWidth()) / 2;
                cury = (Core.graphics.getHeight() - getPrefHeight()) / 2;
                in.get(1);

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
                cont.label(() -> intro.isEmpty() ? "@update.checking" : MOD.meta.version.equals(version) ? "@update.latest" : "@update.updateAvailable").align(Align.left).fillX().pad(5f).get().setColor(0f, 1f, 0.3f, 1f);
                cont.row();

                cont.button(gitRepo + "\n" + Iconc.paste + Iconc.github, textb, () -> {
                    Core.app.setClipboardText(gitURL);
                }).growX().height(50f);
                cont.row();
                cont.button("", textb, () -> {
                    ui.loadfrag.show("@downloading");
                    ui.loadfrag.setProgress(() -> Reflect.get(ui.mods, "modImportProgress"));
                    Http.get(MI2USettings.getStr("ghApi", ghApi) + "/repos/" + gitRepo + "/releases/latest", res -> {
                        var json = Jval.read(res.getResultAsString());
                        var assets = json.get("assets").asArray();

                        //prioritize dexed jar, as that's what Sonnicon's mod template outputs
                        var dexedAsset = assets.find(j -> j.getString("name").startsWith("dexed") && j.getString("name").endsWith(".jar"));
                        var asset = dexedAsset == null ? assets.find(j -> j.getString("name").endsWith(".jar")) : dexedAsset;

                        if(asset != null){
                            //grab actual file
                            var url = asset.getString("browser_download_url");

                            Http.get(url, result -> {
                                Reflect.invoke(ui.mods, "handleMod", new Object[]{gitRepo, result}, String.class, Http.HttpResponse.class);
                            }, e -> Reflect.invoke(ui.mods, "modError", new Object[]{e}, Throwable.class));
                        }else{
                            throw new ArcRuntimeException("No JAR file found in releases. Make sure you have a valid jar file in the mod's latest Github Release.");
                        }
                    }, e -> Reflect.invoke(ui.mods, "modError", new Object[]{e}, Throwable.class));
                }).growX().height(50f).update(b -> {
                    b.getLabelCell().update(l -> {
                        l.setText(Core.bundle.get("update.download") + ": " + MOD.meta.version + " -> " + version);
                    }).get().setColor(1f, 1f, 0.3f, 1f);
                });
                cont.row();
                cont.button("", textb, () -> this.addTo(null)).growX().height(50f).update(b -> {
                    b.setText(Core.bundle.get("update.close") + " (" + Strings.fixed((delay - in.getTime(0))/60 , 1)+ "s)");
                });
                cont.row();

                cont.pane(t -> {
                    introl = t.add(intro).align(Align.left).growX().get();  //drawing update discription possibly cause font color bug.
                }).width(400f).maxHeight(500f);

                Http.get(MI2USettings.getStr("ghApi", ghApi) + "/repos/" + gitRepo + "/releases/latest", res -> {
                    var json = Jval.read(res.getResultAsString());
                    version = json.getString("name");
                    intro = json.getString("body");
                    if(introl != null) introl.setText(intro);
                    if(!MOD.meta.version.equals(version)) addTo(Core.scene.root);
                }, e -> {
                    in.get(1);
                    delay = 10f;
                    intro = "@update.failCheck";
                    if(introl != null) introl.setText(intro);
                    remove();
                    Log.err(e);
                });

            }
            
        };
    }
}
