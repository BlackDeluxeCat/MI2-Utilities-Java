package mi2u;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.util.*;
import mi2u.graphics.*;
import mi2u.input.*;
import mi2u.io.*;
import mi2u.ui.*;
import mi2u.ui.elements.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.ui.*;

import java.util.regex.*;

import static mi2u.MI2UVars.*;
import static mindustry.Vars.*;


public class MI2Utilities extends Mod{
    public static final String gitURL = "https://github.com/BlackDeluxeCat/MI2-Utilities-Java";
    public static final String gitRepo = "BlackDeluxeCat/MI2-Utilities-Java";
    public static Mods.LoadedMod MOD;

    public MI2Utilities(){
        Events.on(ClientLoadEvent.class, e -> {
            MOD = mods.getMod(MI2Utilities.class);
            titleButtonSize = 32f;

            Pixmap fade = new Pixmap(128, 128);
            for(int x = 0; x < fade.width; x++){
                for(int y = 0; y < fade.height; y++){
                    FloatFloatf func = f -> Mathf.pow(f, 0.6f);
                    fade.set(x, y, MI2UTmp.c2.set(Color.black).a(0.4f * func.get(
                            Math.min(x, Math.min(y, Math.min(fade.width-x-1, fade.height-y-1))) / (fade.width/2f)
                    )));
                }
            }
            Core.atlas.addRegion("fadeBackground", new TextureRegion(new Texture(fade)));
            fadeBackground = new TextureRegionDrawable(Core.atlas.find("fadeBackground"));

            Mindow2.initMindowStyles();
            MI2USettings.init();
            InputUtils.init();

            maxSchematicSize = MI2USettings.getInt("maxSchematicSize", 64);
            renderer.maxZoom = Strings.parseFloat(MI2USettings.getStr("maxZoom", "6"));
            renderer.minZoom = Strings.parseFloat(MI2USettings.getStr("minZoom", "1.5"));

            Time.runTask(40f, () -> {
                mi2ui.addTo(Core.scene.root);
                mi2ui.visible(() -> state.isGame() && ui.hudfrag.shown);
                if(MI2USettings.getBool("showEmojis")) emojis.addTo(emojis.hasParent() ? emojis.parent : Core.scene.root);
                emojis.visible(() -> !state.isGame() || ui.hudfrag.shown);
                if(MI2USettings.getBool("showCoreInfo")) coreInfo.addTo(coreInfo.hasParent() ? coreInfo.parent : Core.scene.root);
                coreInfo.visible(() -> state.isGame() && ui.hudfrag.shown);
                if(MI2USettings.getBool("showMindowMap")) mindowmap.addTo(mindowmap.hasParent() ? mindowmap.parent : Core.scene.root);
                mindowmap.visible(() -> state.isGame() && ui.hudfrag.shown);
                if(MI2USettings.getBool("showLogicHelper", true)) logicHelper.addTo(logicHelper.hasParent() ? logicHelper.parent : ui.logic);

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
        new PopupTable(){
            int sign = 0;
            Interval in = new Interval();
            String intro = "", version = "" + Iconc.cancel;
            Label introl;
            float delay = 900f;

            {
                setBackground(Styles.black5);
                margin(8f);
                setPositionInScreen((Core.graphics.getWidth() - getPrefWidth()) / 2, (Core.graphics.getHeight() - getPrefHeight()) / 2);

                in.get(1);

                update(() -> {
                    toFront();
                    if(in.check(0, delay)) hide();
                });

                hovered(() -> in.get(1));

                addDragMove();

                this.image().color(Color.coral).growX().height(2f).row();
                this.add("@update.title").growX().with(l -> l.setFontScale(1.2f)).row();
                this.image().color(Color.coral).growX().height(2f);

                this.row();

                Runnable httpreq = () -> {
                    sign = 0;
                    Http.get(gitURL + "/releases/latest", res -> {
                        sign = 1;
                        in.get(1);
                        delay = 1200f;
                        var str = res.getResultAsString();
                        var pp = Pattern.compile("(?<=Release ).*?(?= · BlackDeluxeCat/MI2-Utilities-Java)");
                        var mm = pp.matcher(str);
                        mm.find();
                        version = mm.group();
                        if(version.equals(MOD.meta.version)){
                            this.hide();
                        }else{
                            this.popup();
                        }

                        pp = Pattern.compile("(?<=markdown-body my-3\">)[\\S\\s]*?(?=</div>)");
                        mm = pp.matcher(str);
                        mm.find();
                        intro = mm.group();

                        pp = Pattern.compile("<.*?>");
                        mm = pp.matcher(intro);
                        intro = mm.replaceAll("");
                        if(introl != null) introl.setText(intro);
                    }, e -> {
                        sign = -1;
                        in.get(1);
                        delay = 600f;
                        intro = "";
                        Log.err(e);
                    });
                };

                httpreq.run();

                this.table(t -> {
                    t.label(() -> sign == 0 ? "@update.checking" : sign == -1 ? "@update.failCheck" : MOD.meta.version.equals(version) ? "@update.latest" : "@update.updateAvailable").align(Align.left).growX().pad(5f).get().setColor(0f, 1f, 0.3f, 1f);
                    t.button("" + Iconc.refresh, textb, httpreq).disabled(tb -> sign != -1).size(32f);
                }).growX();

                this.row();

                this.button(gitRepo + "\n" + Iconc.paste + Iconc.github + "(copy url)", textb, () -> {
                    Core.app.setClipboardText(gitURL);
                }).growX().height(50f).get().getLabel().setFontScale(0.5f);

                this.row();

                this.button("", textb, () -> ui.mods.githubImportMod(gitRepo, true)).growX().height(50f).update(b -> {
                    b.setDisabled(() -> sign <= 0);
                    b.getLabelCell().update(l -> {
                        l.setText(Core.bundle.get("update.download") + ": " + MOD.meta.version + " -> " + version);
                    }).get().setColor(1f, 1f, 0.3f, 1f);
                });

                this.row();

                this.button("", textb, this::hide).growX().height(50f).update(b -> {
                    b.setText(Core.bundle.get("update.close") + " (" + Strings.fixed((delay - in.getTime(0)) / 60, 1) + "s)");
                });
                this.row();

                this.pane(t -> {
                    introl = t.add(intro).align(Align.left).growX().get();  //drawing update discription possibly cause font color bug.
                }).width(300f).maxHeight(600f);
            }
            
        };
    }
}
