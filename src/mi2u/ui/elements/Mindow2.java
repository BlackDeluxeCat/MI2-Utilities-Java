package mi2u.ui.elements;

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

import static arc.util.Align.*;
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

    public float fromx = 0, fromy = 0, curx = 0, cury = 0, curw = 100f, curh = 100f;
    boolean dragging = false, resizing = false;
    public boolean minimized = false;
    boolean titleCfg = false;
    public String titleText, helpInfo = "", mindowName;
    protected Table titleBar = new Table(), titlePane = new Table(Styles.black6);
    protected Table cont = new Table();
    protected Seq<SettingEntry> settings = new Seq<>();
    protected MI2Utils.IntervalMillis interval = new MI2Utils.IntervalMillis(2);
    public int edgesnap = Align.center;
    @Nullable public Mindow2 tbSnap, lrSnap;
    public int tbSnapAlign, lrSnapAlign;
    public float tbLeftOff, lrBottomOff;

    public Mindow2(String name, String title, String help){
        init();
        mindowName = name;
        helpInfo = help;
        Events.on(MI2UEvents.FinishSettingInitEvent.class, e -> {
            initSettings();
            loadUISettings();
        });

        Events.on(ResizeEvent.class, e -> Time.run(60f, this::loadUISettings));

        titleText = title;
        registerName();
        Events.on(ClientLoadEvent.class, e -> {
            rebuild();
        });
    }

    public void init(){}

    public void rebuild(){
        clear();
        cont.setBackground(Styles.black3);
        cont.touchable = Touchable.enabled;
        setupCont(cont);
        titleBar.clear();
        setupTitle();

        add(titleBar).growX();
        row();
        if(!minimized){
            add(cont).growX();
        }
        setTransform(true);
    }

    /** called when rebuild Mindow2, should be overrided */
    public void setupCont(Table cont){}

    /** called when click minimize-button, can be overrided */
    public void minimize(){
        rebuild();
    }

    public void setupTitle(){
        titleBar.add(new ScrollPane(titlePane){
            @Override
            public float getPrefWidth(){
                return 0f;
            }
        }).scrollX(true).scrollY(false).with(sp -> {
            sp.update(() -> {
                Element e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
                if(e != null && e.isDescendantOf(sp)){
                    sp.requestScroll();
                }else if(sp.hasScroll()){
                    Core.scene.setScrollFocus(null);
                }
            });
            sp.setFadeScrollBars(true);
            sp.setupFadeScrollBars(0.3f, 0f);
        }).growX().right().get().hovered(() -> titleCfg = false);
        titleBar.image().width(2f).growY().color(Color.white);

        var toast = new Table();
        toast.add(titleText).color(MI2UTmp.c1.set(0.8f,0.9f,1f,1f));
        toast.button("-", textbtoggle, () -> {
            minimized = !minimized;
            cury += (minimized ? 1f : -1f) * cont.getHeight() * scaleY;
            saveUISettings();
            minimize();
        }).size(titleButtonSize).update(b -> b.setChecked(minimized));
        toast.button("" + Iconc.settings, textb, this::showSettings).size(titleButtonSize);
        /*toast.button(b -> b.label(() -> "" + (resizing ? Iconc.move : Iconc.resize)), textb, () -> {
            resizing = !resizing;
            titleCfg = false;
        }).size(titleButtonSize);*/
        toast.setBackground(titleBarbgNormal);

        titleBar.add(new MCollapser(toast, true).setCollapsed(true, () -> {
            if(titleCfg && interval.check(0, 10000)) titleCfg = false;
            return !titleCfg && !minimized;
        }).setDirection(true, true).setDuration(0.2f));

        titleBar.update(() -> {
            titleBar.invalidate();
            titleBar.layout();
        });

        titleBar.button(b -> b.label(() -> "" + (resizing ? Iconc.resize : Iconc.move)), textb, () -> {
            titleCfg = !titleCfg;
            interval.get(0,0);
        }).size(titleButtonSize).get().addListener(new InputListener(){
            Vec2 tmpv = new Vec2();
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
                dragging = MI2UTmp.v2.set(v).sub(fromx, fromy).sub(tmpv).len() > 5f;
                if(resizing){
                    curw = Mathf.clamp(v.x - getX(left), 50f, 800f);
                    curh = Mathf.clamp(v.y - getY(bottom), 50f, 800f);
                }else{
                    curx = v.x;
                    cury = v.y;
                    setSnap(v.x, v.y);
                }
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
    }

    @Override
    public void act(float delta){
        super.act(delta);
        boolean slideAnime = edgeSnap(edgesnap);
        slideAnime = slideAnime | elementSnap(tbSnap, tbSnapAlign, lrSnap == null && !Align.isLeft(edgesnap) && !Align.isRight(edgesnap));
        slideAnime = slideAnime | elementSnap(lrSnap, lrSnapAlign, tbSnap == null && !Align.isBottom(edgesnap) && !Align.isTop(edgesnap));
        if(slideAnime) interval.get(1, 0);

        if(!interval.check(1, 400)){
            setPosition(Mathf.lerp(x, curx, 0.4f), Mathf.lerp(y, cury, 0.4f));
        }else{
            setPosition(curx, cury);
        }
        //Log.info(width + "," + height);
        //setSize(curw, curh);
        keepInStage();
        invalidateHierarchy();
        pack();
    }

    /** Returns the X position of the specified {@link Align alignment}. */
    @Override
    public float getX(int alignment){
        float x = this.x;
        if((alignment & right) != 0)
            x += width * scaleX;
        else if((alignment & left) == 0) //
            x += width * scaleX / 2;
        return x;
    }

    /** Returns the Y position of the specified {@link Align alignment}. */
    @Override
    public float getY(int alignment){
        float y = this.y;
        if((alignment & top) != 0)
            y += height * scaleY;
        else if((alignment & bottom) == 0) //
            y += height * scaleY / 2;
        return y;
    }

    @Override
    public void setPosition(float x, float y, int alignment){
        if((alignment & right) != 0)
            x -= width * scaleX;
        else if((alignment & left) == 0) //
            x -= width * scaleX / 2;

        if((alignment & top) != 0)
            y -= height * scaleY;
        else if((alignment & bottom) == 0) //
            y -= height * scaleY / 2;

        if(this.x != x || this.y != y){
            this.x = x;
            this.y = y;
        }
    }

    protected boolean edgeSnap(int align){
        if(parent == null) return false;
        if(Align.isTop(align)) cury = parent.getHeight() - height * scaleY;
        if(Align.isBottom(align)) cury = 0;
        if(Align.isRight(align)) curx = parent.getWidth() - width * scaleX;
        if(Align.isLeft(align)) curx = 0;
        return align != Align.center;
    }

    protected boolean elementSnap(Element e, int align, boolean off){
        if(e == null) return false;
        if(Align.isTop(align)) cury = e.getY(Align.top);
        if(Align.isBottom(align)) cury = e.getY(Align.bottom) - height * scaleY;

        if(Align.isRight(align)) curx = e.getX(Align.right);
        if(Align.isLeft(align)) curx = e.getX(Align.left) - width * scaleX;

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
        int top = Core.graphics.getHeight() - mindowY - getHeight() * scaleY < dst ? Align.top : 0;
        int bottom = mindowY < dst ? Align.bottom : 0;
        int right = Core.graphics.getWidth() - mindowX - getWidth() * scaleX < dst ? Align.right : 0;
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

            dst = Math.abs(m.getX(Align.left) - (mindowX + getWidth() * scaleX));
            if(dst < 32f && dst <= dst1 && between.get(mindowY, m.y - getHeight() * scaleY, m.y + m.getHeight() * m.scaleY)){
                dst1 = dst;
                lrSnap = m;
                lrSnapAlign = Align.left;
                lrBottomOff = mindowY - m.y;
            };

            dst = Math.abs(m.getX(Align.right) - mindowX);
            if(dst < 32f && dst <= dst1 && between.get(mindowY, m.y - getHeight() * scaleY, m.y + m.getHeight() * m.scaleY)){
                dst1 = dst;
                lrSnap = m;
                lrSnapAlign = Align.right;
                lrBottomOff = mindowY - m.y;
            };

            dst = Math.abs(m.getY(Align.bottom) - (mindowY + getHeight() * scaleY));
            if(dst < 32f && dst <= dst2 && between.get(mindowX, m.x - getWidth() * scaleX, m.x + m.getWidth() * m.scaleX)){
                dst2 = dst;
                tbSnap = m;
                tbSnapAlign = Align.bottom;
                tbLeftOff = mindowX - m.x;
            };

            dst = Math.abs(m.getY(Align.top) - mindowY);
            if(dst < 32f && dst <= dst2 && between.get(mindowX, m.x - getWidth() * scaleX, m.x + m.getWidth() * m.scaleX)){
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
        if(mindowName == null || mindowName.equals("")) return false;
        minimized = MI2USettings.getBool(mindowName + ".minimized");
        edgesnap = MI2USettings.getInt(mindowName + ".edgesnap", -1);
        curx = (float)MI2USettings.getInt(mindowName + ".curx");
        cury = (float)MI2USettings.getInt(mindowName + ".cury");
        //curw = (float)MI2USettings.getInt(mindowName + ".curw");
        //curh = (float)MI2USettings.getInt(mindowName + ".curh");
        setScale(MI2USettings.getInt(mindowName + ".scale", 100) / 100f);
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

    /**
     * Override this method for custom UI settings save
     */
    public void saveUISettings(){
        //it is a not-named mindow2, no settings can be saved.
        if(mindowName == null || mindowName.equals("")) return;
        MI2USettings.putBool(mindowName + ".minimized", minimized);
        MI2USettings.putInt(mindowName + ".edgesnap", edgesnap);
        //edgesnap will disable curx / cury changes, so they shouldn't be saved when edgesnapping.
        if(!Align.isTop(edgesnap) && !Align.isBottom(edgesnap)){
            MI2USettings.putInt(mindowName + ".cury", (int)cury);
        }
        if(!Align.isLeft(edgesnap) && !Align.isRight(edgesnap)){
            MI2USettings.putInt(mindowName + ".curx", (int)curx);
        }
        //MI2USettings.putInt(mindowName + ".curw", (int)curw);
        //MI2USettings.putInt(mindowName + ".curh", (int)curh);
        MI2USettings.putInt(mindowName + ".scale", (int)(scaleX * 100));

        MI2USettings.putStr(mindowName + ".LRsnap", lrSnap == null ? "null" : lrSnap.mindowName);
        MI2USettings.putInt(mindowName + ".LRsnapAlign", lrSnapAlign);
        MI2USettings.putInt(mindowName + ".LRsnapOff", (int)lrBottomOff);
        MI2USettings.putStr(mindowName + ".TBsnap", tbSnap == null ? "null" : tbSnap.mindowName);
        MI2USettings.putInt(mindowName + ".TBsnapAlign", tbSnapAlign);
        MI2USettings.putInt(mindowName + ".TBsnapOff", (int)tbLeftOff);
    }

    public void registerName(){
        if(mindowName != null && !mindowName.equals("") && !mindow2s.contains(m -> m.mindowName.equals(this.mindowName))){
            mindow2s.add(this);
        }
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
        FieldEntry entry6 = new FieldEntry(mindowName + ".scale", "@settings.mindow2.scale", "100", TextField.TextFieldFilter.digitsOnly, str -> Strings.canParseInt(str) && Strings.parseInt(str) >= 5 && Strings.parseInt(str) <= 400, str -> setScale(Strings.parseInt(str, 100)/100f));

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
                                        float mindw = (mind.getWidth()*mind.scaleX/Core.graphics.getWidth())*this.getWidth(),
                                                mindh = (mind.getHeight()*mind.scaleY/Core.graphics.getHeight())*this.getHeight();
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
                t.row();
                t.table(tt -> entry6.build(tt)).growX();
            };
        }
    }
}
