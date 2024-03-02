package mi2u.ui.elements;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mi2u.*;
import mi2u.io.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static arc.Core.*;
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
    public SettingHandler settings;
    protected MI2Utils.IntervalMillis interval = new MI2Utils.IntervalMillis(3);
    public int edgesnap = Align.center;
    @Nullable public Mindow2 tbSnap, lrSnap;
    public int tbSnapAlign, lrSnapAlign;
    public float tbLeftOff, lrBottomOff;

    public Mindow2(String name, String title, String help){
        init();
        mindowName = name;
        helpInfo = help;
        registerName();
        initSettings();
        loadUISettings();

        Events.on(ResizeEvent.class, e -> Time.run(60f, () -> {
            this.loadUISettings();
            this.rebuild();
        }));

        titleText = title;
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
        titlePane.touchable = Touchable.enabled;
        titleBar.add(new MCollapser(titlePane, false).setCollapsed(() -> minimized)).growX().get().hovered(() -> titleCfg = false);
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
            if(titleCfg && interval.check(0, 5000)) titleCfg = false;
            return !titleCfg && !minimized;
        }).setDirection(true, true).setDuration(0.2f));

        titleBar.update(() -> {
            titleBar.invalidate();
            titleBar.layout();
        });

        titleBar.button(b -> b.label(() -> "" + (resizing ? Iconc.resize : Iconc.move)), textb, () -> {
            titleCfg = !titleCfg;
            interval.get(0,0);
        }).size(titleButtonSize).with(b -> {
            b.addListener(new InputListener(){
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
                    Vec2 v = localToStageCoordinates(MI2UTmp.v1.set(x, y)).sub(fromx, fromy);
                    dragging = MI2UTmp.v2.set(v).sub(tmpv).len() > 5f;
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
        });
    }

    @Override
    public void act(float delta){
        super.act(delta);
        if(interval.get(1, 300)){
            boolean slideAnime = edgeSnap(edgesnap);
            slideAnime = slideAnime | elementSnap(tbSnap, tbSnapAlign, lrSnap == null && !Align.isLeft(edgesnap) && !Align.isRight(edgesnap));
            slideAnime = slideAnime | elementSnap(lrSnap, lrSnapAlign, tbSnap == null && !Align.isBottom(edgesnap) && !Align.isTop(edgesnap));
            if(slideAnime && MI2UTmp.v1.set(curx, cury).sub(x, y).len() >= 3f) interval.reset(2, 0);
        }

        if(!interval.check(2, 400)){
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
        interval.reset(1, 10000);
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
        new BaseDialog("@settings.meta.dialogTitle"){
            {
                addCloseButton();
                this.cont.pane(t -> {
                    t.button("@settings.meta.mindowHelp", Icon.info, () -> showHelp()).width(300f).height(150f).get().setStyle(textb);
                    t.row();
                    t.table(tt -> settings.buildList(tt)).width(Math.min(600, Core.graphics.getWidth()));
                }).grow();
                this.cont.labelWrap("@settings.meta.oldVersion").fill();
                this.cont.addChild(new Table(){
                    {
                        settings.buildDescription(this);
                        touchable = Touchable.disabled;
                        update(() -> {
                            cont.stageToLocalCoordinates(MI2UTmp.v3.set(Core.input.mouse()));
                            this.setPosition(MI2UTmp.v3.x, MI2UTmp.v3.y);
                        });
                    }
                });
                show();
            }
        };
    }

    /** can be overrided, should use super.initSettings(), called in rebuild() */
    public void initSettings(){
        if(mindowName == null || mindowName.equals("")) return;
        if(settings == null) settings = new SettingHandler(mindowName);
        settings.list.clear();
        var sScl = settings.sliderPref("scale", 100, 20, 400, 10, s -> s + "%", scl -> setScale(scl / 100f));
        sScl.title = bundle.get("settings.mindow.scale");
        sScl.description = bundle.getOrNull("settings.mindow.scale.description");
    }

    /** Override this method for custom UI settings load
     * rebuild() called once finished loading
     */
    public boolean loadUISettingsRaw(){
        if(mindowName == null || mindowName.equals("")) return false;
        minimized = settings.getBool("minimized");
        edgesnap = settings.getInt("edgesnap");
        curx = settings.getInt("curx");
        cury = settings.getInt("cury");
        setScale(settings.getInt("scale", 100) / 100f);
        mindow2s.each(m -> {
            if(m == this) return;
            if(m.mindowName.equals(settings.getStr("LRsnap"))){
                lrSnap = m;
            }
            if(m.mindowName.equals(settings.getStr("TBsnap"))){
                tbSnap = m;
            }
        });
        lrSnapAlign = settings.getInt("LRsnapAlign");
        lrBottomOff = settings.getInt("LRsnapOff");
        tbSnapAlign = settings.getInt("TBsnapAlign");
        tbLeftOff = settings.getInt("TBsnapOff");
        testSnaps();
        return true;
    }

    public void loadUISettings(){
        loadUISettingsRaw();
    }

    /**
     * Override this method for custom UI settings save
     */
    public void saveUISettings(){
        //it is a not-named mindow2, no settings can be saved.
        if(mindowName == null || mindowName.equals("")) return;
        settings.putBool("minimized", minimized);
        settings.putInt("edgesnap", edgesnap);
        //edgesnap will disable curx / cury changes, so they shouldn't be saved when edgesnapping.
        if(!Align.isTop(edgesnap) && !Align.isBottom(edgesnap)){
            settings.putInt("cury", (int)cury);
        }
        if(!Align.isLeft(edgesnap) && !Align.isRight(edgesnap)){
            settings.putInt("curx", (int)curx);
        }
        settings.putInt("scale", (int)(scaleX * 100));

        settings.putString("LRsnap", lrSnap == null ? "" : lrSnap.mindowName);
        settings.putInt("LRsnapAlign", lrSnapAlign);
        settings.putInt("LRsnapOff", (int)lrBottomOff);
        settings.putString("TBsnap", tbSnap == null ? "" : tbSnap.mindowName);
        settings.putInt("TBsnapAlign", tbSnapAlign);
        settings.putInt("TBsnapOff", (int)tbLeftOff);
    }

    public void registerName(){
        if(mindowName != null && !mindowName.equals("") && !mindow2s.contains(m -> m.mindowName.equals(this.mindowName))){
            mindow2s.add(this);
        }
    }
}
