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
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.*;
import mi2u.*;
import mi2u.input.*;
import mi2u.io.*;
import mi2u.ui.elements.*;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.ui.*;
import mindustry.world.*;

import static mi2u.MI2UVars.*;
import static mi2u.io.SettingHandler.TextFieldSetting.*;
import static mindustry.Vars.*;

public class MinimapMindow extends Mindow2{
    public static Minimap2 m = new Minimap2(200f);
    public static WorldFinderTable finderTable = new WorldFinderTable();
    public static PopupTable buttons;

    public boolean catching = false;
    public MinimapMindow(){
        super("MindowMap", "@minimap.MI2U", "");

        update(() -> {
            if(control.input instanceof InputOverwrite && control.input.block != null && Core.input.keyDown(KeyCode.controlLeft) && Core.input.keyDown(KeyCode.f)){
                finderTable.find = control.input.block;
                finderTable.popup();
                finderTable.setPositionInScreen(Core.input.mouseX(), Core.input.mouseY());
            }
        });

        Events.on(EventType.TapEvent.class, e -> {
            if(!catching) return;
            catching = false;
            BuildingStatsPopup.popNew(e.tile.build);
        });

        m.drawLabel = settings.getBool("drawLabel");
        m.drawSpawn = settings.getBool("drawSpawn");
        m.drawFog = settings.getBool("drawFog");
        m.drawIndicator = settings.getBool("drawIndicator");
        m.drawObjective = settings.getBool("drawObjective");
        m.drawUnitColorDifference = settings.getInt("drawUnitColorDiff") / 100f;
        m.drawUnitOutline = settings.getInt("drawUnitOutline") / 100f;

        buttons = new PopupTable();
        buttons.update(() -> buttons.hideWithoutFocusOn(this, buttons));
        buttons.defaults().height(32f).minWidth(100f).fillX();
        buttons.touchable = Touchable.enabled;

        if(settings.getSetting("drawLabel") instanceof SettingHandler.CheckSetting ce) buttons.add(ce.miniButton()).row();
        if(settings.getSetting("drawSpawn") instanceof SettingHandler.CheckSetting ce) buttons.add(ce.miniButton()).row();
        if(settings.getSetting("drawFog") instanceof SettingHandler.CheckSetting ce) buttons.add(ce.miniButton()).row();
        if(settings.getSetting("drawIndicator") instanceof SettingHandler.CheckSetting ce) buttons.add(ce.miniButton()).row();
        if(settings.getSetting("drawObjective") instanceof SettingHandler.CheckSetting ce) buttons.add(ce.miniButton()).row();


        Events.on(EventType.WorldLoadEvent.class, e -> {
            m.setZoom(m.zoom);
        });

        titlePane.table(t -> {
            Cons<Table> b = tb -> {
                tb.table(tt -> {
                    tt.button(Iconc.logic + "", MI2UVars.textbtoggle, () -> {
                        catching = !catching;
                    }).width(32f).growY().checked(bt -> catching);
                    tt.button(Iconc.zoom + "", MI2UVars.textb, () -> {
                        finderTable.popup();
                        finderTable.setPositionInScreen(Core.input.mouseX(), Core.input.mouseY());
                    }).width(32f).growY();
                    tt.button(Iconc.downOpen + "", MI2UVars.textb, () -> {
                        buttons.popup(Align.right);
                        buttons.setPositionInScreen(Core.input.mouseX(), Core.input.mouseY());
                    }).width(32f).growY();
                }).fillX().growY().height(titleButtonSize);
            };

            Cons<Table> l = tl -> {
                tl.table(tt -> {
                    tt.defaults().width(1f);
                    tt.add("").with(c -> {
                        c.update(() -> {
                            c.setText(Strings.fixed(World.conv(player.x), 1) + ", "+ Strings.fixed(World.conv(player.y), 1));
                            c.setFontScale(titlePane.getWidth() > 110f ? 0.8f : 0.5f);
                        });
                        c.setAlignment(Align.right);
                    });
                    tt.row();
                    tt.add("").color(Color.coral).with(c -> {
                        c.update(() -> {
                            c.setText(Strings.fixed(World.conv(Core.input.mouseWorldX()), 1) + ", "+ Strings.fixed(World.conv(Core.input.mouseWorldY()), 1));
                            c.setFontScale(titlePane.getWidth() > 110f ? 0.8f : 0.5f);
                        });
                        c.setAlignment(Align.right);
                    });
                }).right();
            };
            t.add(new MCollapser(b, true).setCollapsed(false, () -> minimized || !hasMouse()).setDuration(0.2f).setDirection(true, false));

            t.add().growX();
            t.table(l).right();
        }).right().growX();
    }

    @Override
    public void setupCont(Table cont){
        cont.clear();
        int size = settings.getInt("size");
        m.setMapSize(size);
        cont.add(m);
    }

    @Override
    public void initSettings(){
        super.initSettings();
        settings.checkPref("drawLabel", false, b -> m.drawLabel = b);
        settings.checkPref("drawSpawn", true, b -> m.drawSpawn = b);
        settings.checkPref("drawFog", true, b -> m.drawFog = b);
        settings.checkPref("drawIndicator", true, b -> m.drawIndicator = b);
        settings.checkPref("drawObjective", true, b -> m.drawObjective = b);
        settings.textPref("size", String.valueOf(140), TextField.TextFieldFilter.digitsOnly, s -> Strings.canParseInt(s) && Strings.parseInt(s) >= 100 && Strings.parseInt(s) <= 3200, s -> rebuild(), intParser);
        settings.textPref("drawUnitColorDiff", String.valueOf(10), TextField.TextFieldFilter.digitsOnly, s -> Strings.canParseInt(s) && Strings.parseInt(s) >= 0 && Strings.parseInt(s) <= 100, s -> m.drawUnitColorDifference = Strings.parseInt(s) / 100f, intParser);
        settings.textPref("drawUnitOutline", String.valueOf(10), TextField.TextFieldFilter.digitsOnly, s -> Strings.canParseInt(s) && Strings.parseInt(s) >= 0 && Strings.parseInt(s) <= 100, s -> m.drawUnitOutline = Strings.parseInt(s) / 100f, intParser);
    }

    @Override
    public boolean loadUISettingsRaw(){
        if(!super.loadUISettingsRaw()) return false;
        int size = settings.getInt("size");
        m.setMapSize(size);
        rebuild();
        return true;
    }
    
    public static class Minimap2 extends Table{
        protected Element map;
        public Rect rect = new Rect();
        private static final float baseSize = 16f;
        public float zoom = 4;

        public boolean drawLabel = false, drawSpawn = true, drawFog = true, drawIndicator = true, drawObjective = true;
        public float drawUnitOutline = 0f;
        public float drawUnitColorDifference = 0.9f;

        public Minimap2(float size){
            float margin = 0f;
            this.touchable = Touchable.enabled;
            map = new Element(){
                {
                    setSize(Scl.scl(size));
                }
    
                @Override
                public void act(float delta){
                    setPosition(Scl.scl(margin), Scl.scl(margin));
    
                    super.act(delta);
                }
    
                @Override
                public void draw(){
                    if(renderer.minimap.getRegion() == null) return;
                    if(!clipBegin()) return;
                    setRect();
                    
                    Draw.reset();
                    Draw.rect(getRegion(), x + width / 2f, y + height / 2f, width, height);
    
                    if(renderer.minimap.getTexture() != null){
                        Draw.alpha(parentAlpha);
                        drawEntities(x, y, width, height, 0.75f, drawLabel);
                        if(drawSpawn) drawSpawns(x, y, width, height, 0.75f);
                    }
    
                    clipEnd();
                }
            };

            margin(margin);

            addListener(new InputListener(){
                @Override
                public boolean scrolled(InputEvent event, float x, float y, float amountx, float amounty){
                    zoomBy(amounty);
                    return true;
                }
            });
    
            addListener(new ClickListener(){
                {
                    tapSquareSize = Scl.scl(11f);
                }
    
                @Override
                public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                    if(inTapSquare()){
                        super.touchUp(event, x, y, pointer, button);
                    }else{
                        pressed = false;
                        pressedPointer = -1;
                        pressedButton = null;
                        cancelled = false;
                    }
                }
    
                @Override
                public void touchDragged(InputEvent event, float x, float y, int pointer){
                    if(!inTapSquare(x, y)){
                        invalidateTapSquare();
                    }
                    super.touchDragged(event, x, y, pointer);
    
                    if(mobile){
                        float max = Math.min(world.width(), world.height()) / 16f / 2f;
                        setZoom(1f + (y * 0.8f + 0.2f ) / height * (max - 1f));
                    }else{
                        this.clicked(null, x, y);
                    }
                }

                @Override
                public void clicked(InputEvent event, float x, float y){
                    setRect();
                    untransform(MI2UTmp.v1.set(x, y));
                    if(control.input instanceof DesktopInput || control.input instanceof InputOverwrite){
                        try{
                            if(control.input instanceof InputOverwrite ino){
                                ino.pan(true, MI2UTmp.v1);
                            }else if(control.input instanceof DesktopInput inp){
                                inp.panning = true;
                                Core.camera.position.set(MI2UTmp.v1);
                            }
                        }catch(Exception e){
                            Log.err("Minimap", e);
                        }
                    }else{
                        ui.minimapfrag.toggle();
                    }
                }

                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                    if(button == KeyCode.mouseRight && control.input.commandMode){
                        setRect();
                        untransform(MI2UTmp.v1.set(x, y));
                        Core.camera.project(MI2UTmp.v1);
                        control.input.commandTap(MI2UTmp.v1.x, MI2UTmp.v1.y);
                    }

                    return super.touchDown(event, x, y, pointer, button);
                }
            });
    
            update(() -> {
                Element e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
                if(e != null && e.isDescendantOf(this)){
                    requestScroll();
                }else if(hasScroll()){
                    Core.scene.setScrollFocus(null);
                }
            });

            add(map).size(size);
        }

        public void setMapSize(float size){
            clearChildren();
            map.setSize(Scl.scl(size));
            add(map).size(size);
        }

        /** these methods below are replacing vanilla minimap renderer method*/
        /** zoom is private in renderer */
        public void zoomBy(float amount){
            zoom += amount;
            setZoom(zoom);
        }

        public void setZoom(float amount){
            zoom = Mathf.clamp(amount, 1f, Math.max(world.width(), world.height()) / baseSize / 2f);
        }

        /** set the world area corresponding to map camera. */
        public void setRect(){
            float sz = baseSize * zoom;
            float cx = (Core.camera.position.x / tilesize);
            float cy = (Core.camera.position.y / tilesize);
            cx = (2 * sz) <= world.width() ? Mathf.clamp(cx, sz, world.width() - sz) : world.width() / 2f;
            cy = (2 * sz) <= world.height() ? Mathf.clamp(cy, sz, world.height() - sz) : world.height() / 2f;
            rect.set((cx - sz) * tilesize, (cy - sz) * tilesize, sz * 2 * tilesize, sz * 2 * tilesize);
        }

        /** world 2 map ui */
        public Vec2 transform(Vec2 position){
            return position.sub(rect.x, rect.y).scl(width / rect.width, height / rect.height);
        }
        /** map 2 world */
        public Vec2 untransform(Vec2 position){
            return position.scl(rect.width / width, rect.height / height).add(rect.x, rect.y);
        }

        public float scale(float radius){
            return width / rect.width * radius;
        }

        public void drawEntities(float x, float y, float w, float h, float scaling, boolean withLabels){
            //draw a linerect of view area
            Lines.stroke(1f, MI2UTmp.c1.set(Color.white).a(0.5f));
            float cx = (Core.camera.position.x - rect.x) / rect.width * w;
            float cy = (Core.camera.position.y - rect.y) / rect.width * h;
            Lines.rect(x + cx - Core.graphics.getWidth() / rect.width * w / renderer.getScale() / 2f,
                    y + cy - Core.graphics.getHeight() / rect.width * h / renderer.getScale() / 2f,
                    Core.graphics.getWidth() / rect.width * w / renderer.getScale() ,
                    Core.graphics.getHeight() / rect.width * h / renderer.getScale());
            Draw.color();

            //just render unit group
            Groups.unit.each(unit -> {
                if(unit.inFogTo(player.team()) || !unit.type.drawMinimap) return;
                float rx = (unit.x - rect.x) / rect.width * w;
                float ry = (unit.y - rect.y) / rect.width * h;

                float scale = Scl.scl(1f) / 2f * scaling * 32f;
                var region = unit.icon();
                if(drawUnitOutline > 0.01f){
                    Draw.mixcol(Color.white, 1f);
                    Draw.rect(region, x + rx, y + ry, (1f + drawUnitOutline) * scale, (1f + drawUnitOutline) * scale * (float)region.height / region.width, unit.rotation() - 90);
                }
                //color difference between block and unit in setting
                Draw.mixcol(MI2UTmp.c1.set(unit.team().color).mul(Mathf.clamp(1f - drawUnitColorDifference)), 1f);
                Draw.rect(region, x + rx, y + ry, scale, scale * (float)region.height / region.width, unit.rotation() - 90);
                Draw.reset();
            });

            //display labels
            if(withLabels){
                for(Player player : Groups.player){
                    if(!player.dead()){
                        float rx = (player.x - rect.x) / rect.width * w;
                        float ry = (player.y - rect.y) / rect.width * h;

                        drawLabel(x + rx, y + ry, player.name, player.team().color);
                    }
                }
            }

            Draw.reset();

            if(state.rules.fog && drawFog){
                Draw.shader(Shaders.fog);
                Texture staticTex = renderer.fog.getStaticTexture(), dynamicTex = renderer.fog.getDynamicTexture();

                //crisp pixels
                dynamicTex.setFilter(Texture.TextureFilter.nearest);
                var region = getRegion();

                Tmp.tr1.set(dynamicTex);
                Tmp.tr1.set(region.u, 1f - region.v, region.u2, 1f - region.v2);

                Draw.color(state.rules.dynamicColor);
                Draw.rect(Tmp.tr1, x + w/2f, y + h/2f, w, h);

                if(state.rules.staticFog){
                    staticTex.setFilter(Texture.TextureFilter.nearest);

                    Tmp.tr1.texture = staticTex;
                    //must be black to fit with borders
                    Draw.color(0f, 0f, 0f, state.rules.staticColor.a);
                    Draw.rect(Tmp.tr1, x + w/2f, y + h/2f, w, h);
                }

                Draw.color();
                Draw.shader();
            }

            if(drawIndicator){
                LongSeq indicators = control.indicators.list();
                float fin = ((Time.globalTime / 30f) % 1f);
                float rad = 2f + scale(fin * 5f + tilesize - 2f);
                Lines.stroke(Scl.scl((1f - fin) * 4f + 0.5f));

                for(int i = 0; i < indicators.size; i++){
                    long ind = indicators.items[i];
                    int
                            pos = Indicator.pos(ind),
                            ix = Point2.x(pos),
                            iy = Point2.y(pos);
                    float time = Indicator.time(ind), offset = 0f;

                    //fix multiblock offset - this is suboptimal
                    Building build = world.build(pos);
                    if(build != null){
                        offset = build.block.offset / tilesize;
                    }

                    Vec2 v = transform(Tmp.v1.set((ix + 0.5f + offset) * tilesize, (iy + 0.5f + offset) * tilesize));
                    //v.sub(getMarginBottom(), getMarginBottom());

                    Draw.color(Color.orange, Color.scarlet, Mathf.clamp(time / 70f));
                    Lines.square(x + v.x, y + v.y, rad);
                }

                Draw.reset();
            }

            //could be buggy
            if(drawObjective){
                state.rules.objectives.eachRunning(obj -> {
                    for(var marker : obj.markers){
                        marker.drawMinimap(renderer.minimap);
                    }
                });
            }

            Draw.reset();
        }

        public void drawSpawns(float x, float y, float w, float h, float scaling){
            if(!state.hasSpawns() || !state.rules.waves) return;

            TextureRegion icon = Icon.units.getRegion();

            Lines.stroke(Scl.scl(1f));

            Draw.color(state.rules.waveTeam.color, Tmp.c2.set(state.rules.waveTeam.color).value(1.2f), Mathf.absin(Time.time, 16f, 1f));

            float curve = Mathf.curve(Time.time % 240f, 120f, 240f);

            float rad = scale(state.rules.dropZoneRadius);

            for(Tile tile : spawner.getSpawns()){
                if(!rect.contains(tile.worldx(), tile.worldy())) continue;
                float tx = (tile.worldx() - rect.x) / rect.width * w;
                float ty = (tile.worldy() - rect.y) / rect.height * w;

                Draw.rect(icon, x + tx, y + ty, icon.width * scaling / zoom, icon.height * scaling / zoom);
                Lines.circle(x + tx, y + ty, rad);
                if(curve > 0f) Lines.circle(x + tx, y + ty, rad * Interp.pow3Out.apply(curve));
            }

            Draw.reset();
        }

        /**
         * get texture, region in {@link MinimapRenderer}*/
        public @Nullable TextureRegion getRegion(){
            var texture = renderer.minimap.getTexture();
            if(texture == null) return null;
            var region = renderer.minimap.getRegion();
            if(region == null) return null;

            float invTexWidth = 1f / texture.width / tilesize;
            float invTexHeight = 1f / texture.height / tilesize;
            float pixmapy = world.height() * tilesize - (rect.y + rect.height);

            region.set(rect.x * invTexWidth, pixmapy * invTexHeight, (rect.x + rect.width) * invTexWidth, (pixmapy + rect.height) * invTexHeight);
            return region;
        }

        public void drawLabel(float x, float y, String text, Color color){
            Font font = Fonts.outline;
            GlyphLayout l = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
            boolean ints = font.usesIntegerPositions();
            font.getData().setScale(1 / 1.5f / Scl.scl(1f));
            font.setUseIntegerPositions(false);

            l.setText(font, text, color, 90f, Align.left, true);
            float yOffset = 20f;
            float margin = 3f;

            Draw.color(0f, 0f, 0f, 0.2f);
            Fill.rect(x, y + yOffset - l.height/2f, l.width + margin, l.height + margin);
            Draw.color();
            font.setColor(color);
            font.draw(text, x - l.width/2f, y + yOffset, 90f, Align.left, true);
            font.setUseIntegerPositions(ints);
            font.getData().setScale(1f);
            Pools.free(l);
        }
    }
}
