package mi2u.ui;

import arc.*;
import arc.func.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mi2u.*;
import mi2u.ui.stats.*;
import mindustry.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mi2u.MI2UVars.*;
public class MonitorCanvas extends WidgetGroup{
    public Seq<Monitor> monitors = new Seq<>();

    /**camera position left bottom.*/
    public Vec2 camera = new Vec2();
    public static float unitSize = 32f;

    public Table buttons = new Table();
    public BaseDialog addDialog;

    public MonitorCanvas(){
        visible(() -> Vars.state.isGame() && Vars.ui.hudfrag.shown);
        Events.run(EventType.Trigger.update, () -> {
            monitors.each(Monitor::update);
        });

        addDialog = new BaseDialog(""){
            {
                addCloseButton();
                cont.pane(t -> {
                    Monitors.all.each(meta -> {
                        t.button(b -> {
                            b.add("" + switch(meta.type){
                                case building -> Iconc.production;
                                case teamdata -> Iconc.units;
                                case none -> Iconc.none;
                            });
                            b.add(Core.bundle.get(meta.name)).growX();
                        }, textb, () -> {
                            monitors.add(meta.prov.get());
                            hide();
                            rebuild();
                        }).growX().height(buttonSize);
                        t.row();
                    });
                }).growY();
            }
        };

        buttons.background(titleBarBackground);
        buttons.defaults().size(3 * unitSize, 2 * unitSize).growX();
        buttons.button("" + Iconc.add, textb, () -> addDialog.show()).with(funcSetTextb);
        buttons.button("清空", textb, () -> {
            monitors.clear();
            rebuild();
        }).with(funcSetTextb);

        rebuild();

        this.setTransform(true);

        addCaptureListener(new InputListener(){
            float lx, ly;

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                if(Core.input.isTouched(1) || button == KeyCode.mouseRight){
                    lx = x;
                    ly = y;
                    return true;
                }
                return false;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer){
                camera.sub(x - lx, y - ly);
                lx = x;
                ly = y;
            }
        });
    }

    public void rebuild(){
        clear();
        addChild(new Table(shell -> {
            shell.add(buttons).bottom();
            shell.setFillParent(true);
        }));
        monitors.each(m -> addChild(new MonitorTable(m)));
        children.each(e -> {
            if(e instanceof MonitorTable t) e.setPosition(-camera.x + t.m.x, -camera.y + t.m.y);
        });
    }

    @Override
    public void act(float delta){
        super.act(delta);
        buttons.setPosition(Core.graphics.getWidth() / 2f, 0, Align.bottom);
    }

    public class MonitorTable extends Table{
        public Monitor m;
        public Table title = new Table(), display = new Table(), config = new Table(), fetch = new Table();

        public final static int tabDisplay = 0, tabConfig = 1, tabFetch = 2;
        public transient int tab = tabDisplay;

        public MonitorTable(Monitor m){
            this.m = m;

            title.background(titleBarBackground);
            title.defaults().size(unitSize);
            title.button(b -> b.label(() -> "" + (tab == tabDisplay ? Iconc.resize : Iconc.blockLogicDisplay)), textb, () -> {
                tab = tabDisplay;
                setup();
            }).disabled(tb -> tab == tabDisplay).touchable(() -> tab != tabDisplay ? Touchable.enabled : Touchable.childrenOnly).get().addListener(new InputListener(){
                boolean resizing;
                float lx, ly, lw, lh, cx, cy;
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                    resizing = false;
                    if(tab == tabDisplay){
                        resizing = true;
                        cx = x;
                        cy = y;
                        lx = m.x;
                        ly = m.y;
                        lw = m.w;
                        lh = m.h;
                        return true;
                    }
                    return super.touchDown(event, x, y, pointer, button);
                }

                @Override
                public void touchDragged(InputEvent event, float x, float y, int pointer){
                    super.touchDragged(event, x, y, pointer);
                    if(resizing){
                        //左上角拖拽
                        float dx = localToAscendantCoordinates(MonitorCanvas.this, MI2UTmp.v1.set(x, y)).x - lx;
                        m.w = Math.max(unit(lw - dx, unitSize), 5 * unitSize);
                        m.x = lx - (m.w - lw) * Scl.scl();
                        MonitorTable.this.setPosition(-camera.x + m.x, MonitorTable.this.y);
                        m.h = unit(y + m.h, unitSize);
                    }
                }

                @Override
                public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                    rebuildTabs();
                    setup();
                    super.touchUp(event, x, y, pointer, button);
                }
            });

            title.button("" + Iconc.settings, textb, () -> {
                tab = tabConfig;
                setup();
            }).disabled(tb -> tab == tabConfig).touchable(() -> tab != tabConfig ? Touchable.enabled : Touchable.disabled);

            title.button("" + Iconc.trash, textb, () -> {
                monitors.remove(m);
                remove();
            });

            title.button(b -> b.label(m::title).labelAlign(Align.center).growX(), textb, () -> {
                tab = tabFetch;
                setup();
            }).growX().maxWidth(9999).disabled(tb -> tab == tabFetch).touchable(() -> tab != tabFetch ? Touchable.enabled : Touchable.childrenOnly);

            title.add("" + Iconc.move).with(l -> {
                l.setAlignment(Align.center);
                l.addListener(new InputListener(){
                    float fromx1, fromy1;

                    @Override
                    public boolean touchDown(InputEvent event, float x1, float y1, int pointer, KeyCode button){
                        fromx1 = x1;
                        fromy1 = y1;
                        return true;
                    }

                    @Override
                    public void touchDragged(InputEvent event, float x, float y, int pointer){
                        super.touchDragged(event, x, y, pointer);
                        Vec2 v = localToParentCoordinates(MI2UTmp.v1.set(x, y));
                        setPosition(-camera.x + (MonitorTable.this.m.x = unit(v.x - fromx1, Scl.scl(unitSize))), -camera.y + (MonitorTable.this.m.y = unit(v.y - fromy1, Scl.scl(unitSize))));
                    }
                });
            }).size(unitSize);

            rebuildTabs();

            setup();
        }

        public Table tab(int tab){
            return switch(tab){
                case tabConfig -> config;
                case tabFetch -> fetch;
                case tabDisplay -> display;
                default -> display;
            };
        }

        public void setup(){
            clear();
            table(shell -> {
                shell.background(Styles.black3);
                shell.add(title).growX().height(unitSize);
                shell.row();
                shell.add(tab(tab)).grow();
            }).self(c -> c.update(t -> c.size(m.w, m.h)));
            update(() -> setSize(Scl.scl() * m.w, Scl.scl() * m.h));
        }

        public void rebuildTabs(){
            m.build(display);
            m.buildCfg(config);
            m.buildFetch(fetch);
        }

        public static float unit(float f, float unit){
            return Mathf.floor(f / unit) * unit;
        }
    }
}
