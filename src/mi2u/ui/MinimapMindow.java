package mi2u.ui;

import arc.*;
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
import mi2u.ai.*;
import mi2u.input.*;
import mi2u.io.*;
import mi2u.ui.elements.*;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;

import static mi2u.MI2UVars.*;
import static mi2u.io.SettingHandler.TextFieldSetting.*;
import static mindustry.Vars.*;

public class MinimapMindow extends Mindow2{
    public static Minimap2 m = new Minimap2();
    public static PopupTable buttons;

    public MinimapMindow(){
        super("Minimap", true);
        setVisibleInGame();
        touchable = Touchable.childrenOnly;
        cont.background(Styles.none);

        m.drawLabel = settings.getBool("drawLabel");
        m.drawSpawn = settings.getBool("drawSpawn");
        m.drawFog = settings.getBool("drawFog");
        m.drawIndicator = settings.getBool("drawIndicator");
        m.drawMarkers = settings.getBool("drawObjective");
        m.drawUnitColorDifference = settings.getInt("drawUnitColorDiff") / 100f;
        m.drawUnitOutline = settings.getInt("drawUnitOutline") / 100f;

        buttons = new PopupTable();
        buttons.defaults().height(buttonSize).minWidth(100f).fillX();
        buttons.addCloseButton(48);
        buttons.setBackground(Styles.black3);
        buttons.add().row();

        if(settings.getSetting("drawLabel") instanceof SettingHandler.CheckSetting ce) buttons.add(ce.miniButton()).row();
        if(settings.getSetting("drawSpawn") instanceof SettingHandler.CheckSetting ce) buttons.add(ce.miniButton()).row();
        if(settings.getSetting("drawFog") instanceof SettingHandler.CheckSetting ce) buttons.add(ce.miniButton()).row();
        if(settings.getSetting("drawIndicator") instanceof SettingHandler.CheckSetting ce) buttons.add(ce.miniButton()).row();
        if(settings.getSetting("drawObjective") instanceof SettingHandler.CheckSetting ce) buttons.add(ce.miniButton()).row();


        Events.on(EventType.WorldLoadEvent.class, e -> {
            m.setZoom(m.zoom);
        });

        titlePane.defaults().size(buttonSize);
        titlePane.add().growX();

    }

    @Override
    public void setupCont(Table cont){
        cont.clear();
        cont.touchable = Touchable.childrenOnly;
        int size = settings.getInt("size");
        cont.table(t -> t.background(Styles.black3).add(m).pad(2f).size(size));
        cont.row();
        cont.table(t -> {
            t.table(tt -> {
                tt.defaults().width(0.1f);
                tt.label(() -> Strings.fixed(World.conv(player.x), 1) + ",").labelAlign(Align.right).fontScale(0.7f);
                tt.label(() -> " " + Strings.fixed(World.conv(player.y), 1)).labelAlign(Align.left).fontScale(0.7f);

                tt.row();

                tt.label(() -> Strings.fixed(World.conv(Core.input.mouseWorldX()), 1) + ",").labelAlign(Align.right).fontScale(0.7f).color(Color.scarlet);
                tt.label(() -> " " + Strings.fixed(World.conv(Core.input.mouseWorldY()), 1)).labelAlign(Align.left).fontScale(0.7f).color(Color.scarlet);
            }).growX();
            t.button(Iconc.downOpen + "", MI2UVars.textb, () -> {
                buttons.popup(Align.right);
                buttons.setPositionInScreen(Core.input.mouseX(), Core.input.mouseY());
            }).size(buttonSize).top();
        }).growX();
    }

    @Override
    public void initSettings(){
        super.initSettings();
        settings.checkPref("drawLabel", false, b -> m.drawLabel = b);
        settings.checkPref("drawSpawn", true, b -> m.drawSpawn = b);
        settings.checkPref("drawFog", true, b -> m.drawFog = b);
        settings.checkPref("drawIndicator", true, b -> m.drawIndicator = b);
        settings.checkPref("drawObjective", true, b -> m.drawMarkers = b);
        settings.textPref("size", String.valueOf(140), TextField.TextFieldFilter.digitsOnly, s -> Strings.canParseInt(s) && Strings.parseInt(s) >= 100 && Strings.parseInt(s) <= 3200, s -> setupCont(cont), intParser);
        settings.textPref("drawUnitColorDiff", String.valueOf(10), TextField.TextFieldFilter.digitsOnly, s -> Strings.canParseInt(s) && Strings.parseInt(s) >= 0 && Strings.parseInt(s) <= 100, s -> m.drawUnitColorDifference = Strings.parseInt(s) / 100f, intParser);
        settings.textPref("drawUnitOutline", String.valueOf(10), TextField.TextFieldFilter.digitsOnly, s -> Strings.canParseInt(s) && Strings.parseInt(s) >= 0 && Strings.parseInt(s) <= 100, s -> m.drawUnitOutline = Strings.parseInt(s) / 100f, intParser);
    }
    
    public static class Minimap2 extends Element{
        //minimap camera rect
        public Rect rect = new Rect();
        private static final float baseSize = 16f;
        public float zoom = 8f;

        public boolean drawLabel = false, drawSpawn = true, drawFog = true, drawIndicator = true, drawMarkers = true;
        public float drawUnitOutline = 0f;
        public float drawUnitColorDifference = 0.9f;

        public Mat transWorldUnit = new Mat(), transUI = new Mat();

        public Minimap2(){

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
                    if(!mobile){
                        InputUtils.panStable(MI2UTmp.v1);
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
        }

        @Override
        public void act(float delta){
            super.act(delta);
            funcSetScrollFocus.get(this);
        }

        @Override
        public void draw(){
            if(renderer.minimap.getRegion() == null) return;
            if(!clipBegin()) return;
            setRect();

            Draw.reset();
            Draw.color(color);
            Draw.rect(getRegion(), x + width / 2f, y + height / 2f, width, height);

            if(renderer.minimap.getTexture() != null){
                drawEntities(x, y, width, height, 0.75f, drawLabel);
            }

            clipEnd();
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

        /** set the world area corresponding to minimap region. */
        public void setRect(){
            float sz = baseSize * zoom;
            float szh = sz * height / width;
            float cx = (Core.camera.position.x / tilesize);
            float cy = (Core.camera.position.y / tilesize);
            cx = (2 * sz) <= world.width() ? Mathf.clamp(cx, sz, world.width() - sz) : world.width() / 2f;
            cy = (2 * szh) <= world.height() ? Mathf.clamp(cy, szh, world.height() - szh) : world.height() / 2f;
            rect.set((cx - sz) * tilesize, (cy - szh) * tilesize, sz * 2 * tilesize, szh * 2 * tilesize);
        }

        /** world 2 map ui */
        public Vec2 transform(Vec2 position){
            return position.sub(rect.x, rect.y).scl(width / rect.width, height / rect.height);
        }
        /** map 2 world */
        public Vec2 untransform(Vec2 position){
            return position.scl(rect.width / width, rect.height / height).add(rect.x, rect.y);
        }

        public void drawEntities(float x, float y, float w, float h, float scaling, boolean withLabels){
            transUI.idt().set(Draw.trans());

            float scaleFactor;
            transWorldUnit.set(transUI);
            transWorldUnit.translate(x, y);
            transWorldUnit.scale(scaleFactor = w / rect.width, h / rect.height);
            transWorldUnit.translate(-rect.x, -rect.y);
            Draw.trans(transWorldUnit);
            Draw.reset();

            scaleFactor = 1f / scaleFactor;
            scaleFactor *= scaling;
            float iconScaleFactor = scaleFactor * 24f;

            Draw.color(Color.white, 0.5f);
            Lines.stroke(scaleFactor);
            Lines.rect(MI2UTmp.r1.setCentered(Core.camera.position.x, Core.camera.position.y, Core.graphics.getWidth() / renderer.camerascale, Core.graphics.getHeight() / renderer.camerascale));

            Draw.color();
            Draw.alpha(color.a);
            //just render unit group
            for(var unit : Groups.unit){
                if(unit.inFogTo(player.team()) || !unit.type.drawMinimap) continue;
                var region = unit.icon();
                if(drawUnitOutline > 0.01f){
                    Draw.mixcol(Color.white, 1f);
                    Draw.rect(region, unit.x, unit.y, (1f + drawUnitOutline) * iconScaleFactor, (1f + drawUnitOutline) * iconScaleFactor * (float)region.height / region.width, unit.rotation() - 90);
                }
                //color difference between block and unit in setting
                Draw.mixcol(MI2UTmp.c1.set(unit.team().color).mul(Mathf.clamp(1f - drawUnitColorDifference)), 1f);
                Draw.rect(region, unit.x, unit.y, iconScaleFactor, iconScaleFactor * (float)region.height / region.width, unit.rotation() - 90);
                Draw.mixcol();
            }

            //display labels
            if(withLabels){
                for(Player player : Groups.player){
                    if(!player.dead()){
                        drawLabel(player.x, player.y, player.name, player.team().color, scaleFactor);
                    }
                }
            }

            Draw.reset();

            if(state.rules.fog && drawFog){
                Draw.shader(Shaders.fog);
                Texture staticTex = renderer.fog.getStaticTexture(), dynamicTex = renderer.fog.getDynamicTexture();

                //crisp pixels
                dynamicTex.setFilter(Texture.TextureFilter.nearest);

                Tmp.tr1.set(dynamicTex);
                Tmp.tr1.set(0f, 1f, 1f, 0);

                Draw.color(state.rules.dynamicColor, state.rules.dynamicColor.a * color.a);
                float ww = world.width() * tilesize;
                float wh = world.height() * tilesize;
                Draw.rect(Tmp.tr1, ww / 2f, wh / 2f, ww, wh);

                if(state.rules.staticFog){
                    staticTex.setFilter(Texture.TextureFilter.nearest);

                    Tmp.tr1.texture = staticTex;
                    //must be black to fit with borders
                    Draw.color(0f, 0f, 0f, state.rules.staticColor.a);
                    Draw.rect(Tmp.tr1, ww / 2f, wh / 2f, ww, wh);
                }

                Draw.color();
                Draw.shader();
            }

            if(drawSpawn) drawSpawns(scaleFactor * 0.75f);

            if(drawIndicator){
                LongSeq indicators = control.indicators.list();
                float fin = ((Time.globalTime / 30f) % 1f);
                float rad = fin * 5f + tilesize - 2f;
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

                    Draw.color(Color.orange, Color.scarlet, Mathf.clamp(time / 70f) * color.a);
                    Lines.square((ix + 0.5f + offset) * tilesize, (iy + 0.5f + offset) * tilesize, rad);
                }

                Draw.reset();
            }

            //could be buggy
            if(drawMarkers){
                state.rules.objectives.eachRunning(obj -> {
                    for(var marker : obj.markers){
                        marker.draw(1f);
                    }
                });

                for(var marker : state.markers){
                    if(marker.minimap){
                        marker.draw(1f);
                    }
                }

                for(var marker : FullAI.LogicMode.markers){
                    if(marker.minimap){
                        marker.draw(1);
                    }
                }
            }

            Draw.trans(transUI);
            Draw.reset();
        }

        public void drawSpawns(float scaling){
            if(!state.hasSpawns() || !state.rules.waves) return;

            TextureRegion icon = Icon.units.getRegion();

            Lines.stroke(2f);

            Draw.color(state.rules.waveTeam.color, Tmp.c2.set(state.rules.waveTeam.color).value(1.2f), Mathf.absin(Time.time, 16f, 1f) * color.a);

            float curve = Mathf.curve(Time.time % 240f, 120f, 240f);

            float rad = state.rules.dropZoneRadius;

            for(Tile tile : spawner.getSpawns()){
                if(!rect.contains(tile.worldx(), tile.worldy())) continue;
                float tx = tile.worldx() + 4f;
                float ty = tile.worldy() + 4f;

                Draw.rect(icon, tx, ty, icon.width * scaling / zoom, icon.height * scaling / zoom);
                Lines.circle(tx, ty, rad);
                if(curve > 0f) Lines.circle(tx, ty, rad * Interp.pow3Out.apply(curve));
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

        public void drawLabel(float x, float y, String text, Color color, float scaleFactor){
            Font font = Fonts.outline;
            GlyphLayout l = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
            boolean ints = font.usesIntegerPositions();
            font.getData().setScale(1 / 1.25f / Scl.scl(1f) * scaleFactor * 1f);
            font.setUseIntegerPositions(false);

            l.setText(font, text, color, 90f * scaleFactor, Align.left, true);
            float yOffset = 20f;
            float margin = 3f * scaleFactor;

            Draw.color(0f, 0f, 0f, 0.2f * this.color.a);
            Fill.rect(x, y + yOffset - l.height/2f, l.width + margin, l.height + margin);
            Draw.color();
            font.setColor(color);
            font.draw(text, x - l.width/2f, y + yOffset,90f * scaleFactor, Align.left, true);
            font.setUseIntegerPositions(ints);
            font.getData().setScale(1f);
            Pools.free(l);
        }
    }
}
