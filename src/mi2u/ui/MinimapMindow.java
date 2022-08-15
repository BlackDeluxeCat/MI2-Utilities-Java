package mi2u.ui;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.pooling.*;
import mi2u.MI2UTmp;
import mi2u.MI2UVars;
import mi2u.MI2Utils;
import mi2u.input.InputOverwrite;
import mi2u.io.*;
import mi2u.io.MI2USettings.*;
import mindustry.core.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.ui.*;
import mindustry.world.Tile;

import static mindustry.Vars.*;

public class MinimapMindow extends Mindow2{
    public static Minimap2 m = new Minimap2(200f);
    public static WorldFinderTable finderTable = new WorldFinderTable();
    public static PopupTable buttons;
    public MinimapMindow(){
        super("@minimap.MI2U");
    }

    @Override
    public void init() {
        super.init();
        mindowName = "MindowMap";
        update(() -> {
            if(control.input instanceof InputOverwrite && control.input.block != null && Core.input.keyDown(KeyCode.controlLeft) && Core.input.keyDown(KeyCode.f)){
                finderTable.find = control.input.block;
                finderTable.popup();
                finderTable.setPositionInScreen(Core.input.mouseX(), Core.input.mouseY());
            }
        });

        buttons = new PopupTable();
        buttons.button(Iconc.players + "", MI2UVars.textbtoggle, () -> m.drawLabel = !m.drawLabel).update(b -> b.setChecked(m.drawLabel)).size(48f);
        buttons.button(Iconc.blockSpawn + "", MI2UVars.textbtoggle, () -> m.drawSpawn = !m.drawSpawn).update(b -> b.setChecked(m.drawSpawn)).size(48f);
        buttons.button(Iconc.map + "", MI2UVars.textbtoggle, () -> m.drawFog = !m.drawFog).update(b -> b.setChecked(m.drawFog)).size(48f).get().getLabel().setColor(Color.slate);
        buttons.update(() -> buttons.hideWithoutFocusOn(this, buttons));
    }

    @Override
    public void setupCont(Table cont){
        cont.clear();
        m.setMapSize(MI2USettings.getInt(mindowName + ".size", 200));
        cont.add(m).right();
        cont.row();
        cont.table(t -> {
            t.table(tt -> {
                tt.label(() -> Strings.fixed(World.conv(player.x), 1) + ", "+ Strings.fixed(World.conv(player.y), 1)).right();
                tt.row();
                tt.label(() -> Strings.fixed(World.conv(Core.input.mouseWorldX()), 1) + ", "+ Strings.fixed(World.conv(Core.input.mouseWorldY()), 1)).right().color(Color.scarlet);
            }).growX();

            t.table(tt -> {
                tt.button(Iconc.zoom + "", MI2UVars.textb, () -> {
                    finderTable.popup();
                    finderTable.setPositionInScreen(Core.input.mouseX(), Core.input.mouseY());
                }).width(32f).growY();
                tt.button(Iconc.downOpen + "", MI2UVars.textb, () -> {
                    buttons.popup(Align.right);
                    buttons.setPositionInScreen(Core.input.mouseX(), Core.input.mouseY());
                }).width(32f).growY();
            }).fillX().growY();

        }).growX();
    }

    @Override
    public void initSettings(){
        super.initSettings();
        settings.add(new FieldEntry(mindowName + ".size", "@settings.mindowMap.size", String.valueOf(140), TextField.TextFieldFilter.digitsOnly, s -> Strings.canParseInt(s) && Strings.parseInt(s) >= 100 && Strings.parseInt(s) <= 600, s -> rebuild()));
        settings.add(new CollapseGroupEntry("WorldData", ""){
            private CheckEntry check1 = new CheckEntry("worldDataUpdate", "@settings.mindowMap.worldDataUpdate", true, null);
            private FieldEntry field1 = new FieldEntry("worldDataUpdate.interval", "@settings.mindowMap.worldDataUpdate.interval", String.valueOf(10), TextField.TextFieldFilter.digitsOnly, s -> Strings.canParseInt(s) && Strings.parseInt(s) >= 3 && Strings.parseInt(s) <= 60, null);
            {
                collapsep = () -> !check1.value;
                headBuilder = t -> check1.build(t);
                builder = t -> field1.build(t);
            }
        });
    }

    @Override
    public boolean loadUISettingsRaw(){
        if(!super.loadUISettingsRaw()) return false;
        int size = MI2USettings.getInt(mindowName + ".size");
        m.setMapSize(size);
        rebuild();
        return true;
    }
    
    public static class Minimap2 extends Table{
        protected Element map;
        public boolean drawLabel = true, drawSpawn = true, drawFog = true;
        private static final float baseSize = 16f;
        public float zoom = 4;

        public Minimap2(float size){
            float margin = 5f;
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
                    
                    Draw.reset();
                    Draw.rect(getRegion(), x + width / 2f, y + height / 2f, width, height);
    
                    if(renderer.minimap.getTexture() != null){
                        Draw.alpha(parentAlpha);
                        drawEntities(x, y, width, height, 0.75f, drawLabel);
                        drawSpawns(x, y, width, height, 0.75f);
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
                        setZoom(1f + y / height * (max - 1f));
                    }
                }
    
                @Override
                public void clicked(InputEvent event, float x, float y){
                    if(control.input instanceof DesktopInput || control.input instanceof InputOverwrite){
                        try{
                            float sz = baseSize * zoom;
                            float dx = (Core.camera.position.x / tilesize);
                            float dy = (Core.camera.position.y / tilesize);
                            dx = (2 * sz) <= world.width() ? Mathf.clamp(dx, sz, world.width() - sz) : world.width() / 2f;
                            dy = (2 * sz) <= world.height() ? Mathf.clamp(dy, sz, world.height() - sz) : world.height() / 2f;
                            if(control.input instanceof InputOverwrite ino){
                                ino.pan(true, MI2UTmp.v1.set((x / width - 0.5f) * 2f * sz * tilesize + dx * tilesize, (y / height - 0.5f) * 2f * sz * tilesize + dy * tilesize));
                            }else if(control.input instanceof DesktopInput inp){
                                inp.panning = true;
                                Core.camera.position.set(
                                        ((x / width - 0.5f) * 2f * sz * tilesize + dx * tilesize),
                                        ((y / height - 0.5f) * 2f * sz * tilesize + dy * tilesize));
                            }
                        }catch(Exception e){
                            Log.err("Minimap", e);
                        }
                    }else{
                        ui.minimapfrag.toggle();
                    }
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
        public void zoomBy(float amount){
            zoom += amount;
            setZoom(zoom);
        }

        public void setZoom(float amount){
            zoom = Mathf.clamp(amount, 1f, Math.max(world.width(), world.height()) / baseSize / 2f);
        }

        public void drawEntities(float x, float y, float w, float h, float scaling, boolean withLabels){
            float sz = baseSize * zoom;
            float dx = (Core.camera.position.x / tilesize);
            float dy = (Core.camera.position.y / tilesize);
            dx = (2 * sz) <= world.width() ? Mathf.clamp(dx, sz, world.width() - sz) : world.width() / 2f;
            dy = (2 * sz) <= world.height() ? Mathf.clamp(dy, sz, world.height() - sz) : world.height() / 2f;

            Rect rect = MI2UTmp.r1.set((dx - sz) * tilesize, (dy - sz) * tilesize, sz * 2 * tilesize, sz * 2 * tilesize);

            //draw a linerect of view area
            Lines.stroke(1f, new Color(1f, 1f, 1f, 0.5f));
            float cx = (Core.camera.position.x - rect.x) / rect.width * w;
            float cy = (Core.camera.position.y - rect.y) / rect.width * h;
            Lines.rect(x + cx - Core.graphics.getWidth() / rect.width * w / renderer.getScale() / 2f,
                    y + cy - Core.graphics.getHeight() / rect.width * h / renderer.getScale() / 2f,
                    Core.graphics.getWidth() / rect.width * w / renderer.getScale() ,
                    Core.graphics.getHeight() / rect.width * h / renderer.getScale());
            Draw.color();
            //just render unit group
            Groups.unit.each(unit -> {
                float rx = (unit.x - rect.x) / rect.width * w;
                float ry = (unit.y - rect.y) / rect.width * h;

                float scale = Scl.scl(1f) / 2f * scaling * 32f;
                var region = unit.icon();
                //color difference between block and unit in setting
                Draw.mixcol(new Color(unit.team().color.r * 0.9f, unit.team().color.g * 0.9f, unit.team().color.b * 0.9f, 1f), 1f);
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

                if(withLabels){
                    region.set(0f, 0f, 1f, 1f);
                }

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
        }

        public void drawSpawns(float x, float y, float w, float h, float scaling){
            if(!state.hasSpawns() || !state.rules.waves) return;

            TextureRegion icon = Icon.units.getRegion();

            Lines.stroke(Scl.scl(1f));

            Draw.color(state.rules.waveTeam.color, Tmp.c2.set(state.rules.waveTeam.color).value(1.2f), Mathf.absin(Time.time, 16f, 1f));

            float curve = Mathf.curve(Time.time % 240f, 120f, 240f);

            float sz = baseSize * zoom;
            float dx = (Core.camera.position.x / tilesize);
            float dy = (Core.camera.position.y / tilesize);
            dx = (2 * sz) <= world.width() ? Mathf.clamp(dx, sz, world.width() - sz) : world.width() / 2f;
            dy = (2 * sz) <= world.height() ? Mathf.clamp(dy, sz, world.height() - sz) : world.height() / 2f;
            float rad = state.rules.dropZoneRadius / (2 * sz * tilesize) * w;

            Rect rect = MI2UTmp.r1.set((dx - sz) * tilesize, (dy - sz) * tilesize, sz * 2 * tilesize, sz * 2 * tilesize);

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
            //2 * sz = map width/height in tiles
            float sz = baseSize * zoom;
            float dx = (Core.camera.position.x / tilesize);
            float dy = (Core.camera.position.y / tilesize);
            dx = (2 * sz) <= world.width() ? Mathf.clamp(dx, sz, world.width() - sz) : world.width() / 2f;
            dy = (2 * sz) <= world.height() ? Mathf.clamp(dy, sz, world.height() - sz) : world.height() / 2f;
            float invTexWidth = 1f / texture.width;
            float invTexHeight = 1f / texture.height;
            float x = dx - sz, y = world.height() - dy - sz, width = sz * 2, height = sz * 2;
            region.set(x * invTexWidth, y * invTexHeight, (x + width) * invTexWidth, (y + height) * invTexHeight);
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
