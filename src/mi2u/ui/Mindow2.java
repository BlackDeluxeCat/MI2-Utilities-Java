package mi2u.ui;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.Label.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mi2u.*;
import mi2u.game.MI2UEvents;
import mi2u.io.*;
import mi2u.io.MI2USettings.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mi2u.MI2UVars.*;
/**  
 * Mindow2 is a dragable Table that partly works like a window. 
 * titleText is text shown on titleBar, set in constructor.
 * mindowName is inner name, used for window-specific settings, set in overrided constructor.
 * helpInfo is text shown in window-specific help dialog, set in constructor.
 * cont is a container for user items.
 * settings is a SettingEntry seq.
 * <p>
 * {@code setupCont(Table cont)}for cont rebuild, should be overrided.<p>
 * {@code initSettings()}for customize settings, should start with settings.clear()
 * @author BlackDeluxeCat
 */

public class Mindow2 extends Table{
    @Nullable public static Mindow2 currTopmost = null;
    public static Drawable titleBarbgNormal, titleBarbgSnapped, white, gray2;

    public float fromx = 0, fromy = 0, curx = 0, cury = 0;
    public boolean topmost = false, minimized = false;
    public String titleText, helpInfo = "", mindowName;
    protected Table titleBar = new Table();
    protected Table cont = new Table();
    protected Seq<SettingEntry> settings = new Seq<>();
    protected MI2Utils.IntervalMillis interval = new MI2Utils.IntervalMillis(1);
    @Nullable public Element aboveSnap; public int edgesnap = Align.center;

    public Mindow2(String title){
        init();

        Events.on(MI2UEvents.FinishSettingInitEvent.class, e -> {
            initSettings();
            loadUISettings();
        });

        Events.on(ResizeEvent.class, e -> Time.run(60f, this::loadUISettings));

        titleText = title;
        registerName();
        rebuild();
    }

    public Mindow2(String title, String help){
        this(title);
        helpInfo = help;
    }

    public void init(){}

    public void rebuild(){
        clear();
        setupTitle();
        row();
        if(!minimized){
            cont.setBackground(Styles.black3);
            setupCont(cont);
            add(cont);
        }
    }

    /** called when rebuild Mindow2, should be overrided */
    public void setupCont(Table cont){}

    /** called when click minimize-button, can be overrided */
    public void minimize(){
        rebuild();
    }

    public void setupTitle(){
        titleBar.clear();
        var title = new Label(titleText);
        title.name = "Mindow2Title";
        title.setAlignment(Align.left);
        title.addListener(new InputListener(){
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                fromx = x;
                fromy = y;
                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer){
                Vec2 v = localToStageCoordinates(MI2UTmp.v1.set(x, y));
                Element hit = Core.scene.hit(v.x + title.x + title.parent.x, v.y + title.y + title.parent.y, false);
                if(hit != null && hit.name != null && hit.name.equals("Mindow2Title") && !hit.isDescendantOf(Mindow2.this)){
                    aboveSnap = hit.parent.parent;
                    return;
                }
                aboveSnap = null;
                curx = v.x - fromx;
                cury = v.y - fromy;
            }
        });

        if(!minimized){
            titleBar.add(title).pad(0, 1, 0, 1);

            titleBar.button("" + Iconc.settings, textb, this::showSettings).size(titleButtonSize);

            titleBar.button("-", textbtoggle, () -> {
                minimized = !minimized;
                if(minimized){
                    cury += cont.getHeight();
                }else{
                    cury -= cont.getHeight();
                }
                minimize();
            }).size(titleButtonSize).update(b -> {
                b.setChecked(minimized);
            });
        }else{
            titleBar.button(titleText != null ? titleText : "-", textbtoggle, () -> {
                minimized = !minimized;
                if(minimized){
                    cury += cont.getHeight();
                }else{
                    cury -= cont.getHeight();
                }
                minimize();
            }).height(titleButtonSize).update(b -> {
                b.setChecked(minimized);
            }).with(funcSetTextb);
        }

        interval.get(0, 1);
        titleBar.update(() -> {
            cont.touchable = Touchable.enabled;
            //TODO add a abovesnap listener
            titleBar.setBackground(aboveSnap == null ? titleBarbgNormal : titleBarbgSnapped);
            title.color.set(aboveSnap == null ? MI2UTmp.c1.set(0.8f,0.9f,1f,1f) : MI2UTmp.c1.set(0.1f,0.6f,0.6f,1f));
            edgesnap = MI2USettings.getInt(mindowName + ".edgesnap", Align.center);
            if(this == currTopmost || shouldTopMost()) setZIndex(1000);

            if(aboveSnap != null){
                setPosition(aboveSnap.x, aboveSnap.y, Align.topLeft);
            }else if(edgesnap != Align.center && hasParent()){
                Vec2 vec = MI2UTmp.v1;
                vec.set(curx, cury);
                edgeSnap(edgesnap, vec);
                setPosition(vec.x, vec.y);
            }else{
                setPosition(curx, cury);
            }
            keepInStage();
            invalidateHierarchy();
            pack();
        });

        var coll = new Collapser(titleBar, false);
        coll.setCollapsed(true, () -> !(cont.getPrefHeight() < 20f || minimized || !interval.check(0, 3000)));
        coll.setDuration(0.1f);
        coll.update(() -> {
            float w = titleBar.getPrefWidth(), h = titleBar.getPrefHeight();
            coll.setSize(w, h);
            coll.setPosition(0f,getHeight() - h);
            coll.toFront();
            if(hasMouse()) interval.get(0, 1);
        });

        addChild(coll);
    }

    @Override
    public float getPrefHeight(){
        return Math.max(super.getPrefHeight(), titleBar.getPrefHeight());
    }

    @Override
    public float getPrefWidth(){
        return Math.max(super.getPrefWidth(), titleBar.getPrefWidth());
    }

    protected void edgeSnap(int align, Vec2 vec){
        if(parent == null) return;
        switch(align){
            case Align.topLeft://lefttop
                vec.x = 0;
                vec.y = parent.getHeight() - getPrefHeight();
                break;
            case Align.top://top
                //vec.x = (parent.getWidth() - getWidth())/2f;
                vec.y = (parent.getHeight() - getPrefHeight());
                break;
            case Align.topRight://righttop
                vec.x = (parent.getWidth() - getPrefWidth());
                vec.y = (parent.getHeight() - getPrefHeight());
                break;
            case Align.right://right
                vec.x = (parent.getWidth() - getPrefWidth());
                //vec.y = (parent.getHeight() -getRealHeight())/2f;
                break;
            case Align.bottomRight://rightbottom
                vec.x = (parent.getWidth() - getPrefWidth());
                vec.y = 0;
                break;
            case Align.bottom://bottom
                //vec.x = (parent.getWidth() - getWidth())/2f;
                vec.y = 0;
                break;
            case Align.bottomLeft://leftbottom
                vec.x = 0;
                vec.y = 0;
                break;
            case Align.left://left
                vec.x = 0;
                //vec.y = (parent.getHeight() -getRealHeight())/2f;
                break;
        }
    }

    public boolean addTo(Group newParent){
        if(newParent == null){
            return !this.remove();
        }
        this.remove();
        newParent.addChild(this);
        return true;
    }

    //TODO snap chain causing StackOverflow
    public boolean shouldTopMost(){
        return (topmost || (aboveSnap != null && aboveSnap != this && aboveSnap instanceof Mindow2 m && m.topmost));
    }
    
    public void showHelp(){
        new BaseDialog("@mindow2.helpInfoTitle"){
            {
                addCloseButton();
                this.cont.pane(t -> {
                    t.add(helpInfo).padBottom(60f).left().width(Core.graphics.getWidth() / 1.5f).get().setWrap(true);
                    t.row();
                    t.add("@mindow2.uiHelp").left().width(Core.graphics.getWidth() / 1.5f).get().setWrap(true);
                });
                show();
            }
        };
    }

    /** Settings shoulded be set in Seq: settings, will be shown and configurable in SettingsDialog 
     * UISetting will be shown to, but not configurable
    */
    public void showSettings(){
        new BaseDialog("@mindow2.settings.title"){
            {
                addCloseButton();
                this.cont.pane(t -> {
                    t.add(mindowName != null && !mindowName.equals("") ? Core.bundle.format("mindow2.settings.curMindowName") + mindowName: "@mindow2.settings.noMindowNameWarning").fontScale(1.2f).get().setAlignment(Align.center);
                    t.row();
                    t.button("Help", Icon.info, () -> showHelp()).width(200f).get().setStyle(textb);
                    t.row();
                    settings.each(st -> {
                        t.table(st::build).width(Math.min(600, Core.graphics.getWidth())).left();
                        t.row();
                    });
                }).grow();
                show();
            }
        };
    }

    /** can be overrided, should use super.initSettings(), called in rebuild() */
    public void initSettings(){
        settings.clear();
        if(mindowName == null || mindowName.equals("")) return;

        settings.add(new MindowUIGroupEntry(mindowName + ".Mindow", ""));
    }

    /** Override this method for custom UI settings load
     * rebuild() called once finished loading
     */
    public boolean loadUISettingsRaw(){
        //it is a no-named mindow2, no settings can be loaded.
        if(mindowName == null || mindowName.equals("")) return false;
        minimized = MI2USettings.getBool(mindowName + ".minimized");
        topmost = MI2USettings.getBool(mindowName + ".topmost");
        if(topmost) currTopmost = this;
        edgesnap = MI2USettings.getInt(mindowName + ".edgesnap", -1);
        curx = (float)MI2USettings.getInt(mindowName + ".curx");
        cury = (float)MI2USettings.getInt(mindowName + ".cury");
        if(MI2USettings.getStr(mindowName + ".abovesnapTarget").equals("null")){
            aboveSnap = null;
        }else{
            mindow2s.each(m -> {
                if(m.mindowName.equals(MI2USettings.getStr(mindowName + ".abovesnapTarget"))){
                    aboveSnap = m;
                    Log.info(mindowName + " snaps to " + m.mindowName);
                }
            });
        }
        return true;
    }

    public void loadUISettings(){
        loadUISettingsRaw();
        rebuild();
    }

    /** Override this method for custom UI settings save
     */
    public boolean saveUISettings(){
        //it is a not-named mindow2, no settings can be saved.
        if(mindowName == null || mindowName.equals("")) return false;
        MI2USettings.putBool(mindowName + ".minimized", minimized);
        MI2USettings.putBool(mindowName + ".topmost", topmost);
        MI2USettings.putInt(mindowName + ".edgesnap", edgesnap);
        //edgesnap will disable curx / cury changes, so they shouldn't be saved when edgesnapping.
        if(!Align.isTop(edgesnap) && !Align.isBottom(edgesnap)){
            MI2USettings.putInt(mindowName + ".cury", (int)cury);
        }
        if(!Align.isLeft(edgesnap) && !Align.isRight(edgesnap)){
            MI2USettings.putInt(mindowName + ".curx", (int)curx);
        }
        return true;
    }

    public boolean registerName(){
        if(mindowName != null && !mindowName.equals("") && !mindow2s.contains(m -> m.mindowName.equals(this.mindowName))){
            mindow2s.add(this);
            return true;
        }
        return false;
    }

    public static void initMindowStyles(){
        var whiteui = (TextureRegionDrawable)Tex.whiteui;
        titleBarbgNormal = whiteui.tint(1f, 0.1f, 0.2f, 0.8f);
        titleBarbgSnapped = whiteui.tint(1f, 0.1f, 0.2f, 0.2f);
        white = whiteui.tint(1f, 1f, 1f, 1f);
        gray2 = whiteui.tint(0.2f, 0.2f, 0.2f, 1f);
    }

    public class MindowUIGroupEntry extends SettingGroupEntry{
        SingleEntry entry1 = new SingleEntry(mindowName + ".minimized", "");
        CheckEntry entry2 = new CheckEntry(mindowName + ".topmost", "", false, b -> {
            topmost = b;
            if(topmost){
                currTopmost = Mindow2.this;
            }else{
                if(currTopmost == Mindow2.this) currTopmost = null;
            }
        });
        SingleEntry entry3 = new SingleEntry(mindowName + ".curx", "");
        SingleEntry entry4 = new SingleEntry(mindowName + ".cury", "");
        SingleEntry entry5 = new SingleEntry(mindowName + ".edgesnap", ""){
            @Override
            public void build(Table table) {
                setting = MI2USettings.getSetting(name);
                table.labelWrap(() -> this.name + " = " + (setting != null ? Align.toString(Strings.parseInt(setting.get())) : "invaild")).left().growX().get().setColor(0, 1, 1, 0.7f);
            }
        };
        Table buildTarget;
        public MindowUIGroupEntry(String name, String help) {
            super(name, help);
            Events.on(ResizeEvent.class, e -> {
                if(buildTarget != null && buildTarget.hasParent()) {
                    build(buildTarget);
                }else if(buildTarget != null) buildTarget = null;
            });

            builder = t -> {
                buildTarget = t;
                t.clear();
                t.defaults().pad(15f);
                t.margin(10f);
                t.background(Styles.flatDown);
                t.table(tt -> {
                    tt.stack(new Element(){
                        @Override
                        public void draw(){
                            super.draw();
                            Draw.color(Color.darkGray);
                            Draw.alpha(parentAlpha);
                            float divw = this.getWidth()/3f, divh = this.getHeight()/3f;
                            Fill.rect(x + this.getWidth()/2f, y + this.getHeight()/2f, this.getWidth(), this.getHeight());
                            Draw.color(Color.olive);
                            Draw.alpha(parentAlpha);
                            float drawx = Align.isRight(edgesnap) ? 2*divw : Align.isCenterHorizontal(edgesnap) ? divw : 0f, drawy = Align.isTop(edgesnap) ? 2*divh : Align.isCenterVertical(edgesnap) ? divh : 0f;
                            Fill.rect(x + this.getWidth()/6f + drawx, y + this.getHeight()/6f + drawy, divw, divh);
                            Draw.reset();
                        }
                        {
                            Element el = this;
                            addListener(new InputListener(){
                                @Override
                                public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                                    int top = y/el.getHeight()*3f > 2f ? Align.top : 0;
                                    int bottom = y/el.getHeight()*3f < 1f ? Align.bottom : 0;
                                    int left = x/el.getWidth()*3f < 1f ? Align.left : 0;
                                    int right = x/el.getWidth()*3f > 2f ? Align.right : 0;
                                    edgesnap = top | left | right | bottom;
                                    if(edgesnap == 0) edgesnap = Align.center;
                                    MI2USettings.putInt(mindowName + ".edgesnap", edgesnap);
                                    return super.touchDown(event, x, y, pointer, button);
                                }});
                        }}, new Table(){{
                            this.add(new Element(){
                                {this.touchable = Touchable.disabled;}
                                @Override
                                public void draw(){
                                    super.draw();
                                    Draw.color(Color.grays(0.1f));
                                    Draw.alpha(parentAlpha * 0.8f);
                                    Fill.rect(x + this.getWidth()/2f, y + this.getHeight()/2f, this.getWidth(), this.getHeight());

                                    mindow2s.each(mind -> {
                                        if(mind.parent != Mindow2.this.parent) return;
                                        Draw.color(mind == Mindow2.this ? Color.coral : mind == aboveSnap ? Color.royal : Color.grays(0.4f));
                                        Draw.alpha(0.8f * parentAlpha * 0.8f);
                                        float mindw = (mind.getWidth()/Core.graphics.getWidth())*this.getWidth(),
                                                mindh = (mind.getHeight()/Core.graphics.getHeight())*this.getHeight();
                                        float mindx = x + (mind.x/Core.graphics.getWidth())*this.getWidth() + mindw/2f, mindy = y + (mind.y/Core.graphics.getHeight())*this.getHeight() + mindh/2f;
                                        Fill.rect(mindx, mindy, mindw, mindh);
                                        Draw.reset();
                                    });
                                }
                            }).self(c -> c.pad(10f).size(300f, 300f*Core.graphics.getHeight()/Core.graphics.getWidth()));
                        }}
                    ).fill().size(320f, 300f*Core.graphics.getHeight()/Core.graphics.getWidth() + 20f);

                    tt.table(rightt -> {
                        rightt.add(Mindow2.this.mindowName);
                        rightt.row();
                        rightt.table(ttt -> {
                            entry1.build(ttt);
                            ttt.row();
                            ttt.add(entry2.newTextButton(name + ".topMost")).growX();
                            ttt.row();
                            entry3.build(ttt);
                            ttt.row();
                            entry4.build(ttt);
                            ttt.row();
                            entry5.build(ttt);
                            ttt.row();
                            ttt.labelWrap("@settings.mindow.edgeSnap.tip").left().growX();
                        }).growX();
                        rightt.row();

                        rightt.table(ttt -> {
                            ttt.button("@mindow2.settings.reloadUI", textb, Mindow2.this::loadUISettings).with(c -> {
                                c.getLabel().setColor(1, 1, 0, 1);
                                funcSetTextb.get(c);
                            }).growX();

                            ttt.button("@mindow2.settings.cacheUI", textb, Mindow2.this::saveUISettings).with(c -> {
                                c.getLabel().setColor(0, 0, 1, 1);
                                funcSetTextb.get(c);
                            }).growX();
                        }).tooltip(tooltip -> {
                            tooltip.setBackground(Styles.black9);
                            tooltip.labelWrap("@mindow2.settingHelp").width(200f);
                        });

                        rightt.row();
                        rightt.table(t3 -> {
                            var b = t3.button("@settings.mindow.abovesnapTarget", textb, null).growX().get();
                            b.clicked(() -> {
                                new PopupTable(){{
                                    this.setBackground(Styles.black5);
                                    this.defaults().growX().height(40f);
                                    for(var m : mindow2s){
                                        if(m == Mindow2.this || m.parent != Mindow2.this.parent) continue;
                                        this.button(Core.bundle.get(new StringBuilder(m.titleText).substring(1)) + "(" + m.mindowName + ")", textbtoggle, () -> {
                                            MI2USettings.putStr(mindowName + ".abovesnapTarget", m.mindowName);
                                            this.hide();
                                        }).with(funcSetTextb).get().setChecked(aboveSnap == m);
                                        this.row();
                                    }
                                    this.button(Iconc.cancel + "null", textbtoggle, () -> {
                                        MI2USettings.putStr(mindowName + ".abovesnapTarget", "null");
                                        this.hide();
                                    }).with(funcSetTextb).get().getLabel().setColor(Color.royal);
                                    this.snapTo(b);
                                    this.update(() -> this.hideWithoutFocusOn(this, buildTarget));
                                    this.addCloseButton();
                                    this.popup();
                                }};
                            });
                            b.getLabelCell().pad(5f,2f,5f,2f);
                        }).growX();
                    }).minWidth(220f).pad(4f);
                });
            };
        }
    }
}
