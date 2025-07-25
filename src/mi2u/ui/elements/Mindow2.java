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
import arc.scene.ui.layout.*;
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
import static mindustry.Vars.*;

/**
 * Mindow2 is a dragable Table that partly works like a window.
 * cont is a container for user items.
 * settings is a SettingEntry seq.
 * <p>
 * {@code setupCont(Table cont)}for cont setup, should be overrided.<p>
 * {@code initSettings()}for customize setting entries, should start with settings.clear()
 *
 * @author BlackDeluxeCat
 */

public class Mindow2 extends Table{
    public float fromx = 0, fromy = 0, curx = 0, cury = 0;
    protected boolean dragging = false;
    public boolean hasCloseButton = false;
    public boolean hasTitle = true, minimized = false;

    protected Table titleBar = new Table(), titlePane = new Table(Styles.black6);
    protected Table cont = new Table();

    /** 窗口专属的settings，在构造方法调用{@code useSettings()}以启用*/
    public @Nullable SettingHandler settings;

    public int edgesnap = Align.center;
    public @Nullable Mindow2 tbSnap, lrSnap;
    public int tbSnapAlign, lrSnapAlign;
    public float tbLeftOff, lrBottomOff;
    public InputListener dragListener = new InputListener(){
        final Vec2 tmpv = new Vec2();

        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
            fromx = x;
            fromy = y;
            tmpv.set(curx, cury);
            return true;
        }

        @Override
        public void touchDragged(InputEvent event, float x, float y, int pointer){
            Vec2 v = localToParentCoordinates(MI2UTmp.v1.set(x, y)).sub(fromx, fromy);
            dragging = MI2UTmp.v2.set(v).sub(tmpv).len() > 5f;
            curx = v.x;
            cury = v.y;
            setSnap(v.x, v.y);
        }

        @Override
        public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
            super.touchUp(event, x, y, pointer, button);

            if(dragging){
                Mindow2.this.toFront();
                dragging = false;
            }else{
                minimized = !minimized;
                cury += (minimized ? 1f : -1f) * cont.getHeight() * scaleY;
                minimize();
            }

            saveUISettings();
        }
    };

    static short timerSnapAnime = 0, timerDragSnapping = 1;
    protected MI2Utils.IntervalMillis interval = new MI2Utils.IntervalMillis(2);

    public Mindow2(String name, boolean hasSettings){
        this.name = name == null ? "" : name;
        if(!mindow2s.contains(m -> m.name.equals(this.name))) mindow2s.add(this);
        if(name != null && !name.isEmpty() && hasSettings) useSettings();

        if(name != null && !name.isEmpty() && settings != null) initSettings();

        cont.setBackground(Styles.black3);
        titleBar.setBackground(mindowTitleBarBackground);

        Events.on(ResizeEvent.class, e -> Time.run(60f, () -> {
            loadUISettings();
            rebuild();
        }));

        Events.on(ClientLoadEvent.class, e -> {
            loadUISettings();
            rebuild();
        });
    }

    public Mindow2(String name){
        this(name, false);
    }

    public void rebuild(){
        clear();

        setupTitle();

        if(hasTitle) add(titleBar).growX().row();


        if(!minimized){
            cont.touchable = Touchable.enabled;
            setupCont(cont);
            add(cont).growX();
        }

        setTransform(true);
    }

    /**
     * called when setup Mindow2, should be overridden
     */
    public void setupCont(Table cont){}

    /**
     * called when click minimize-button, can be overridden
     */
    public void minimize(){
        rebuild();
    }

    public void close(){
        remove();
    }

    public boolean closed(){
        return !hasParent();
    }

    public void setVisibleInGame(){
        visible(() -> state.isGame() && ui.hudfrag.shown);
    }

    public void setupTitle(){
        titleBar.clear();
        if(!minimized){
            titlePane.touchable = Touchable.enabled;
            titleBar.add(titlePane).growX();
            if(settings != null) titleBar.button("" + Iconc.settings, Styles.nonet, this::showSettings).size(buttonSize);
            if(hasCloseButton) titleBar.button("" + Iconc.cancel, Styles.nonet, this::close).size(buttonSize);
        }

        titleBar.table(t -> {
            if(minimized) t.add(bundle.get(name + ".MI2U"));
            t.add(new CombinationIcon(tt -> tt.label(() -> dragging ? Iconc.move + "" : minimized ? "□" : "-").labelAlign(center).grow()).topRight(tt -> tt.label(() -> !dragging ? Iconc.move + "" : "").fontScale(0.6f))).size(buttonSize);
        }).get().addListener(dragListener);
    }

    @Override
    public void act(float delta){
        super.act(delta);
        if(interval.get(timerDragSnapping, 120)){
            boolean slideAnime = edgeSnap(edgesnap) | elementSnap(tbSnap, tbSnapAlign, lrSnap == null && !Align.isLeft(edgesnap) && !Align.isRight(edgesnap)) | elementSnap(lrSnap, lrSnapAlign, tbSnap == null && !Align.isBottom(edgesnap) && !Align.isTop(edgesnap));
            if(slideAnime && MI2UTmp.v1.set(curx, cury).sub(x, y).len() >= 3f) interval.reset(timerSnapAnime, 0);
        }

        if(!interval.check(timerSnapAnime, 240)){
            setPosition(Mathf.lerp(x, curx, 0.3f), Mathf.lerp(y, cury, 0.3f));
        }else{
            setPosition(curx, cury);
        }
        keepInStage();
        invalidate();
        pack();
    }

    @Override
    public void draw(){
        super.draw();
        if(dragging){
            var r = MI2UTmp.r1;
            float fraction = Math.floorMod(Time.millis(), 800)/800f;
            float w = Interp.fastSlow.apply(fraction) * 16f;

            applyTransform(computeTransform());
            Draw.color(Color.cyan, 1 - fraction);
            Lines.stroke(w);
            Lines.rect(r.set(0, 0, width, height).grow(w * 2));
            resetTransform();

            if(lrSnap != null){
                lrSnap.applyTransform(lrSnap.computeTransform());
                Draw.color(Color.scarlet, 1 - fraction);
                Lines.rect(r.set(0, 0, lrSnap.width, lrSnap.height).grow(w * 2));
                lrSnap.resetTransform();
            }

            if(tbSnap != null){
                tbSnap.applyTransform(tbSnap.computeTransform());

                Draw.color(Color.gold, 1 - fraction);
                Lines.rect(r.set(0, 0, tbSnap.width, tbSnap.height).grow(w * 2));
                tbSnap.resetTransform();
            }

            Draw.color(Color.yellow, 1 - fraction);
            Lines.stroke(w * 2);
            if(Align.isTop(edgesnap)) Lines.lineAngle(0, scene.getHeight(), 0, scene.getWidth());
            if(Align.isBottom(edgesnap)) Lines.lineAngle(0, 0, 0, scene.getWidth());

            if(Align.isLeft(edgesnap)) Lines.lineAngle(0, 0, 90, scene.getHeight());
            if(Align.isRight(edgesnap)) Lines.lineAngle(scene.getWidth(), 0, 90, scene.getHeight());

        }
    }

    /**
     * Returns the X position of the specified {@link Align alignment}.
     */
    @Override
    public float getX(int alignment){
        float x = this.x;
        if((alignment & right) != 0)
            x += width * scaleX;
        else if((alignment & left) == 0) //
            x += width * scaleX / 2;
        return x;
    }

    /**
     * Returns the Y position of the specified {@link Align alignment}.
     */
    @Override
    public float getY(int alignment){
        float y = this.y;
        if((alignment & top) != 0)
            y += height * scaleY;
        else if((alignment & bottom) == 0) //
            y += height * scaleY / 2;
        return y;
    }

    public void forceSetPosition(float x, float y){
        curx = x;
        cury = y;
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

    public void setSnap(float x, float y){
        edgesnap = computeEdgeSnap(x, y, 32f);
        if(edgesnap == 0) edgesnap = Align.center;

        Func3<Float, Float, Float, Boolean> between = (v, min, max) -> max >= min && v <= max && v >= min;

        lrSnap = null;
        tbSnap = null;

        float dst1 = Float.MAX_VALUE, dst2 = Float.MAX_VALUE, dst;
        for(Mindow2 m : mindow2s){
            if(m == this) continue;
            if(m.lrSnap == this || m.tbSnap == this) continue;
            if(!m.visible || !m.hasParent()) continue;

            dst = Math.abs(m.getX(Align.left) - (x + getWidth() * scaleX));
            if(dst < 32f && dst <= dst1 && between.get(y, m.y - getHeight() * scaleY, m.y + m.getHeight() * m.scaleY)){
                dst1 = dst;
                lrSnap = m;
                lrSnapAlign = Align.left;
                lrBottomOff = y - m.y;
            }

            dst = Math.abs(m.getX(Align.right) - x);
            if(dst < 32f && dst <= dst1 && between.get(y, m.y - getHeight() * scaleY, m.y + m.getHeight() * m.scaleY)){
                dst1 = dst;
                lrSnap = m;
                lrSnapAlign = Align.right;
                lrBottomOff = y - m.y;
            }

            dst = Math.abs(m.getY(Align.bottom) - (y + getHeight() * scaleY));
            if(dst < 32f && dst <= dst2 && between.get(x, m.x - getWidth() * scaleX, m.x + m.getWidth() * m.scaleX)){
                dst2 = dst;
                tbSnap = m;
                tbSnapAlign = Align.bottom;
                tbLeftOff = x - m.x;
            }

            dst = Math.abs(m.getY(Align.top) - y);
            if(dst < 32f && dst <= dst2 && between.get(x, m.x - getWidth() * scaleX, m.x + m.getWidth() * m.scaleX)){
                dst2 = dst;
                tbSnap = m;
                tbSnapAlign = Align.top;
                tbLeftOff = x - m.x;
            }

            if(Math.abs(lrBottomOff) < 8) lrBottomOff = 0f;
            if(Math.abs(tbLeftOff) < 8) tbLeftOff = 0f;
            if(Math.abs(getHeight() + lrBottomOff - m.getHeight()) < 8) lrBottomOff = m.getHeight() - getHeight();
            if(Math.abs(getWidth() + tbLeftOff - m.getWidth()) < 8) tbLeftOff = m.getWidth() - getWidth();
        }

        testSnaps();
        interval.reset(timerDragSnapping, 10000);
    }

    public void testSnaps(){
        if(lrSnap != null && !lrSnap.testSnap(this)) lrSnap = null;
        if(tbSnap != null && !tbSnap.testSnap(this)) tbSnap = null;
    }

    //circular snapping
    public boolean testSnap(Mindow2 requester){
        if(lrSnap == requester){
            return false;
        }

        if(tbSnap == requester){
            return false;
        }

        return (lrSnap == null || lrSnap.testSnap(requester)) && (tbSnap == null || tbSnap.testSnap(requester));
    }

    public void addTo(Group newParent){
        if(newParent == null){
            this.remove();
            return;
        }
        newParent.addChild(this);
    }

    /**
     * Settings shoulded be set in Seq: settings, will be shown and configurable in SettingsDialog
     */
    public void showSettings(){
        new BaseDialog("@settings.meta.dialogTitle"){
            {
                addCloseButton();
                this.cont.pane(t -> {
                    t.button("@settings.meta.mindowHelp", Icon.info, () -> new BaseDialog(""){
                        {
                            addCloseButton();
                            this.cont.pane(t1 -> t1.add("@" + Mindow2.this.name + ".help").padBottom(60f).left().width(graphics.getWidth() / 1.5f).get().setWrap(true));
                            show();
                        }
                    }).disabled(!bundle.has(Mindow2.this.name + ".help")).width(300f).height(150f).get().setStyle(textb);
                    t.row();
                    t.table(tt -> settings.buildList(tt)).width(Math.min(600, Core.graphics.getWidth()));
                }).grow().row();
                this.cont.addChild(new Table(){
                    {
                        touchable = Touchable.disabled;
                        update(() -> {
                            cont.stageToLocalCoordinates(MI2UTmp.v3.set(Core.input.mouse()));
                            this.setPosition(MI2UTmp.v3.x, MI2UTmp.v3.y + 100);
                        });
                    }
                });
                show();
            }
        };
    }

    public void useSettings(){
        if(settings == null) settings = new SettingHandler(name);
    }
    /**
     * can be overrided, should use super.initSettings()
     */
    public void initSettings(){
        settings.list.clear();
        var sScl = settings.sliderPref("scale", 100, 20, 400, 10, s -> s + "%", scl -> setScale(scl / 100f));
        sScl.title = bundle.get("settings.mindow.scale");
        sScl.description = bundle.getOrNull("settings.mindow.scale.description");
        var sTitle = settings.checkPref("hastitle", true, b -> {
            hasTitle = b;
            this.rebuild();
        });
        sTitle.title = bundle.get("settings.mindow.hastitle");
        sTitle.description = bundle.getOrNull("settings.mindow.hastitle.description");
        settings.title("");
    }

    public void loadUISettings(){
        if(name == null || name.isEmpty() || settings == null) return;
        hasTitle = settings.getBool("hastitle", true);
        minimized = settings.getBool("minimized");
        edgesnap = settings.getInt("edgesnap");
        curx = settings.getInt("curx");
        cury = settings.getInt("cury");
        setScale(settings.getInt("scale", 100) / 100f);
        mindow2s.each(m -> {
            if(m == this) return;
            if(m.name.equals(settings.getStr("LRsnap"))){
                lrSnap = m;
            }
            if(m.name.equals(settings.getStr("TBsnap"))){
                tbSnap = m;
            }
        });
        lrSnapAlign = settings.getInt("LRsnapAlign");
        lrBottomOff = settings.getInt("LRsnapOff");
        tbSnapAlign = settings.getInt("TBsnapAlign");
        tbLeftOff = settings.getInt("TBsnapOff");
        testSnaps();
    }

    /**
     * Override this method for custom UI settings save
     */
    public void saveUISettings(){
        if(name == null || name.isEmpty() || settings == null) return;
        settings.putBool("hastitle", hasTitle);
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

        settings.putString("LRsnap", lrSnap == null || lrSnap.name == null ? "" : lrSnap.name);
        settings.putInt("LRsnapAlign", lrSnapAlign);
        settings.putInt("LRsnapOff", (int)lrBottomOff);
        settings.putString("TBsnap", tbSnap == null || tbSnap.name == null ? "" : tbSnap.name);
        settings.putInt("TBsnapAlign", tbSnapAlign);
        settings.putInt("TBsnapOff", (int)tbLeftOff);
    }
}
