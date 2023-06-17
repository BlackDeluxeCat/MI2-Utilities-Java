package mi2u.ui;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
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
    public static Drawable titleBarbgNormal, titleBarbgSnapped, white, gray2;

    public float fromx = 0, fromy = 0, curx = 0, cury = 0;
    boolean dragging = false;
    public boolean minimized = false;
    public String titleText, helpInfo = "", mindowName;
    protected Table titleBar = new Table();
    protected Table cont = new Table();
    protected Seq<SettingEntry> settings = new Seq<>();
    protected MI2Utils.IntervalMillis interval = new MI2Utils.IntervalMillis(2);
    public int edgesnap = Align.center;
    @Nullable public Mindow2 tbSnap, lrSnap;
    public int tbSnapAlign, lrSnapAlign;
    public float tbLeftOff, lrBottomOff;

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
            static Vec2 tmpv = new Vec2();
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                fromx = x;
                fromy = y;
                tmpv.set(curx, cury);
                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer){
                Vec2 v = localToStageCoordinates(MI2UTmp.v1.set(x, y));
                v.sub(fromx, fromy);
                curx = v.x;
                cury = v.y;

                setSnap(v.x, v.y);
                dragging = v.sub(tmpv).len() > 5f;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                super.touchUp(event, x, y, pointer, button);
                if(!dragging) interval.get(0, 0);
                dragging = false;
                Mindow2.this.toFront();
                saveUISettings();
            }
        });

        if(!minimized){
            titleBar.add(title).pad(0, 1, 0, 1);

            titleBar.button("" + Iconc.settings, textb, this::showSettings).size(titleButtonSize);

            titleBar.button("-", textbtoggle, () -> {
                minimized = !minimized;
                cury += (minimized ? 1f : -1f) * cont.getHeight();
                saveUISettings();
                minimize();
            }).size(titleButtonSize).update(b -> b.setChecked(minimized));
        }else{
            titleBar.button(titleText != null ? titleText : "-", textbtoggle, () -> {
                minimized = !minimized;
                cury += (minimized ? 1f : -1f) * cont.getHeight();
                saveUISettings();
                minimize();
            }).height(titleButtonSize).update(b -> b.setChecked(minimized)).with(funcSetTextb);
        }

        titleBar.update(() -> {
            cont.touchable = Touchable.enabled;
            //TODO add a abovesnap listener
            titleBar.setBackground(titleBarbgNormal);
            title.color.set(MI2UTmp.c1.set(0.8f,0.9f,1f,1f));

            boolean slideAnime = edgeSnap(edgesnap);
            slideAnime = slideAnime | elementSnap(tbSnap, tbSnapAlign, lrSnap == null && !Align.isLeft(edgesnap) && !Align.isRight(edgesnap));
            slideAnime = slideAnime | elementSnap(lrSnap, lrSnapAlign, tbSnap == null && !Align.isBottom(edgesnap) && !Align.isTop(edgesnap));
            if(slideAnime) interval.reset(1, 0);

            if(!interval.check(1, 400)){
                setPosition(Mathf.lerpDelta(x, curx, 0.4f), Mathf.lerpDelta(y, cury, 0.4f));
            }else{
                setPosition(curx, cury);
            }

            keepInStage();
            invalidateHierarchy();
            pack();
        });

        var coll = new Collapser(titleBar, false);
        coll.setCollapsed(true, () -> !(cont.getPrefHeight() < 20f || minimized || (hasMouse() && interval.check(0, 3000))));
        coll.setDuration(0.1f);
        coll.update(() -> {
            float w = titleBar.getPrefWidth(), h = titleBar.getPrefHeight();
            coll.setSize(w, h);
            coll.toFront();
            coll.setPosition(0f,getHeight() - h);
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

    protected boolean edgeSnap(int align){
        if(parent == null) return false;
        if(Align.isTop(align)) cury = parent.getHeight() - getPrefHeight();
        if(Align.isBottom(align)) cury = 0;
        if(Align.isRight(align)) curx = parent.getWidth() - getPrefWidth();
        if(Align.isLeft(align)) curx = 0;
        return align != Align.center;
    }

    protected boolean elementSnap(Element e, int align, boolean off){
        if(e == null) return false;
        if(Align.isTop(align)) cury = e.getY(Align.top);
        if(Align.isBottom(align)) cury = e.getY(Align.bottom) - getHeight();

        if(Align.isRight(align)) curx = e.getX(Align.right);
        if(Align.isLeft(align)) curx = e.getX(Align.left) - getWidth();

        if(off){
            if(Align.isTop(align) || Align.isBottom(align)){
                curx = e.x + tbLeftOff;
            }
            if(Align.isRight(align) || Align.isLeft(align)){
                cury = e.y + lrBottomOff;
            }
        }
        return true;
    }

    public int computeEdgeSnap(float mindowX, float mindowY, float dst){
        int top = Core.graphics.getHeight() - mindowY - getHeight() < dst ? Align.top : 0;
        int bottom = mindowY < dst ? Align.bottom : 0;
        int right = Core.graphics.getWidth() - mindowX - getWidth() < dst ? Align.right : 0;
        int left = mindowX < dst ? Align.left : 0;
        return top | left | right | bottom;
    }

    public void setSnap(float mindowX, float mindowY){
        edgesnap = computeEdgeSnap(mindowX, mindowY, 32f);
        if(edgesnap == 0) edgesnap = Align.center;

        Func3<Float, Float, Float, Boolean> between = (v, min, max) -> max >= min && v <= max && v >= min;

        lrSnap = null;
        tbSnap = null;

        float dst1 = Float.MAX_VALUE, dst2 = Float.MAX_VALUE, dst;
        for(Mindow2 m : mindow2s){
            if(m == this || m.mindowName.equals("")) continue;
            if(m.lrSnap == this || m.tbSnap == this) continue;
            if(!m.visible || !m.hasParent()) continue;

            dst = Math.abs(m.getX(Align.left) - (mindowX + getWidth()));
            if(dst < 32f && dst <= dst1 && between.get(mindowY, m.y - getHeight(), m.y + m.getHeight())){
                dst1 = dst;
                lrSnap = m;
                lrSnapAlign = Align.left;
                lrBottomOff = mindowY - m.y;
            };

            dst = Math.abs(m.getX(Align.right) - mindowX);
            if(dst < 32f && dst <= dst1 && between.get(mindowY, m.y - getHeight(), m.y + m.getHeight())){
                dst1 = dst;
                lrSnap = m;
                lrSnapAlign = Align.right;
                lrBottomOff = mindowY - m.y;
            };

            dst = Math.abs(m.getY(Align.bottom) - (mindowY + getHeight()));
            if(dst < 32f && dst <= dst2 && between.get(mindowX, m.x - getWidth(), m.x + m.getWidth())){
                dst2 = dst;
                tbSnap = m;
                tbSnapAlign = Align.bottom;
                tbLeftOff = mindowX - m.x;
            };

            dst = Math.abs(m.getY(Align.top) - mindowY);
            if(dst < 32f && dst <= dst2 && between.get(mindowX, m.x - getWidth(), m.x + m.getWidth())){
                dst2 = dst;
                tbSnap = m;
                tbSnapAlign = Align.top;
                tbLeftOff = mindowX - m.x;
            };
        }
        testSnaps();
    }

    public void testSnaps(){
        ObjectSet<Mindow2> set = new ObjectSet<>();
        set.add(this);
        if(lrSnap != null && !lrSnap.testSnap(set)) lrSnap = null;
        set.clear();
        set.add(this);
        if(tbSnap != null && !tbSnap.testSnap(set)) tbSnap = null;
    }

    //circular snapping
    public boolean testSnap(ObjectSet<Mindow2> set){
        set.add(this);
        if(lrSnap != null && !set.add(lrSnap)){
            return false;
        }

        if(tbSnap != null && !set.add(tbSnap)){
            return false;
        }

        return (lrSnap == null || lrSnap.testSnap(set)) && (tbSnap == null || tbSnap.testSnap(set));
    }

    public boolean addTo(Group newParent){
        if(newParent == null){
            return !this.remove();
        }
        this.remove();
        newParent.addChild(this);
        return true;
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
                    t.button("@mindow2.settings.help", Icon.info, () -> showHelp()).width(200f).get().setStyle(textb);
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
        edgesnap = MI2USettings.getInt(mindowName + ".edgesnap", -1);
        curx = (float)MI2USettings.getInt(mindowName + ".curx");
        cury = (float)MI2USettings.getInt(mindowName + ".cury");
        mindow2s.each(m -> {
            if(m == this) return;
            if(m.mindowName.equals(MI2USettings.getStr(mindowName + ".LRsnap", "null"))){
                lrSnap = m;
            }
            if(m.mindowName.equals(MI2USettings.getStr(mindowName + ".TBsnap", "null"))){
                tbSnap = m;
            }
        });
        lrSnapAlign = MI2USettings.getInt(mindowName + ".LRsnapAlign");
        lrBottomOff = MI2USettings.getInt(mindowName + ".LRsnapOff");
        tbSnapAlign = MI2USettings.getInt(mindowName + ".TBsnapAlign");
        tbLeftOff = MI2USettings.getInt(mindowName + ".TBsnapOff");
        testSnaps();
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
        MI2USettings.putInt(mindowName + ".edgesnap", edgesnap);
        //edgesnap will disable curx / cury changes, so they shouldn't be saved when edgesnapping.
        if(!Align.isTop(edgesnap) && !Align.isBottom(edgesnap)){
            MI2USettings.putInt(mindowName + ".cury", (int)cury);
        }
        if(!Align.isLeft(edgesnap) && !Align.isRight(edgesnap)){
            MI2USettings.putInt(mindowName + ".curx", (int)curx);
        }

        MI2USettings.putStr(mindowName + ".LRsnap", lrSnap == null ? "null" : lrSnap.mindowName);
        MI2USettings.putInt(mindowName + ".LRsnapAlign", lrSnapAlign);
        MI2USettings.putInt(mindowName + ".LRsnapOff", (int)lrBottomOff);
        MI2USettings.putStr(mindowName + ".TBsnap", tbSnap == null ? "null" : tbSnap.mindowName);
        MI2USettings.putInt(mindowName + ".TBsnapAlign", tbSnapAlign);
        MI2USettings.putInt(mindowName + ".TBsnapOff", (int)tbLeftOff);
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
                    tt.stack(
                        new Element(){
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
                        },
                        new Table(){{
                            this.add(new Element(){
                                {
                                    Element el = this;
                                    el.addListener(new InputListener(){
                                        @Override
                                        public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                                            return true;
                                        }

                                        @Override
                                        public void touchDragged(InputEvent event, float x, float y, int pointer){
                                            el.isDescendantOf(e -> {
                                                if(e instanceof ScrollPane p){
                                                    p.cancel();
                                                    return true;
                                                }
                                                return false;
                                            });

                                            float tx = x / el.getWidth() * Core.graphics.getWidth(), ty = y / el.getHeight() * Core.graphics.getHeight();
                                            curx = tx;
                                            cury = ty;
                                            setSnap(tx, ty);
                                            super.touchDragged(event, x, y, pointer);
                                        }

                                        @Override
                                        public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                                            saveUISettings();
                                            super.touchUp(event, x, y, pointer, button);
                                        }
                                    });
                                }
                                @Override
                                public void draw(){
                                    super.draw();
                                    Draw.color(Color.grays(0.1f));
                                    Draw.alpha(parentAlpha * 0.8f);
                                    Fill.rect(x + this.getWidth()/2f, y + this.getHeight()/2f, this.getWidth(), this.getHeight());

                                    mindow2s.each(mind -> {
                                        if(mind.parent != Mindow2.this.parent) return;
                                        Draw.color(mind == Mindow2.this ? Color.coral : Color.grays(0.4f));
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
                            entry3.build(ttt);
                            ttt.row();
                            entry4.build(ttt);
                            ttt.row();
                            entry5.build(ttt);
                        }).growX();
                    }).minWidth(220f).pad(4f);
                });
            };
        }
    }
}
