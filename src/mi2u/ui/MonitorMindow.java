package mi2u.ui;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.geom.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mi2u.*;
import mi2u.ui.elements.*;
import mi2u.ui.stats.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mi2u.MI2UVars.*;

public class MonitorMindow extends Mindow2{
    public Seq<Monitor> monitors = new Seq<>();

    public boolean cfg = false, cfgMove = false;

    public MonitorCanvas group = new MonitorCanvas();

    public MonitorMindow(){
        super("Monitor");
        hasCloseButton = true;
        setVisibleInGame();

        titlePane.defaults().height(buttonSize).growX();

        titlePane.button("设置监视器", textbtoggle, () -> {
            cfg = !cfg;
            rebuild();
        }).with(funcSetTextb);

        titlePane.button("设置界面", textbtoggle, () -> {
            cfgMove = !cfgMove;
            rebuild();
        }).with(funcSetTextb);

        titlePane.button("" + Iconc.add, textb, () -> new BaseDialog(""){
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
                show();
            }
        }).with(funcSetTextb);

        Events.run(EventType.Trigger.update, () -> {
            monitors.each(Monitor::update);
        });
    }

    @Override
    public void setupCont(Table cont){
        cont.clear();

        group.clearChildren();

        monitors.each(m -> group.addChild(new MonitorTable(m)));

        cont.add(group).grow().minSize(300f);
    }

    public class MonitorCanvas extends WidgetGroup{
        /**
         * camera position left bottom.
         */
        public Vec2 camera = new Vec2();

        public MonitorCanvas(){
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

        @Override
        public void act(float delta){
            children.each(e -> {
                if(e instanceof MonitorTable t) e.setPosition(-camera.x + t.m.x, -camera.y + t.m.y);
            });
            super.act(delta);
        }

        @Override
        public void draw(){
            if(transform) applyTransform(computeTransform());

            if(clipBegin(0, 0, getWidth(), getHeight())){
                Draw.color(Color.white, 0.5f);
                Fill.rect(-camera.x, -camera.y, 42, 42);
                drawChildren();
                Draw.flush();
                clipEnd();
            }

            if(transform) resetTransform();
        }
    }

    public class MonitorTable extends Table{
        public Monitor m;
        public MonitorTable(Monitor m){
            this.m = m;
            rebuild();
        }

        public void rebuild(){
            clear();

            table(t -> {
                t.background(Styles.black6);
                if(cfgMove){
                    t.defaults().growX().height(buttonSize);
                    t.button("刷新" + Iconc.refresh, textb, () -> {
                        m.reset();
                        rebuild();
                    });
                    t.button("删除" + Iconc.trash, textb, () -> {
                        monitors.remove(m);
                        remove();
                    });
                    t.row();
                    t.add("" + Iconc.move).colspan(2).with(l -> {
                        l.setAlignment(Align.center);
                        l.addListener(new InputListener(){
                            float fromx1, fromy1;

                            @Override
                            public boolean touchDown(InputEvent event, float x1, float y1, int pointer, KeyCode button){
                                fromx1 = x1;
                                fromy1 = y1;
                                l.setColor(Pal.accent);
                                return true;
                            }

                            @Override
                            public void touchDragged(InputEvent event, float x1, float y1, int pointer){
                                Vec2 v = localToParentCoordinates(MI2UTmp.v1.set(x1, y1));
                                setPosition(MonitorTable.this.m.x = v.x - fromx1 + group.camera.x, MonitorTable.this.m.y = v.y - fromy1 + group.camera.y);
                            }

                            @Override
                            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                                super.touchUp(event, x, y, pointer, button);
                                l.setColor(Color.white);
                            }
                        });
                    });
                }else if(cfg) this.m.buildCfg(t);
                else this.m.build(t);
            }).self(c -> c.get().update(() -> c.size(m.w * buttonSize, m.h * buttonSize)));
        }
    }
}
