package mi2u.ui;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.struct.Seq;
import arc.util.*;
import arc.util.pooling.*;
import mi2u.io.*;
import mindustry.core.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.input.*;
import mindustry.io.*;
import mindustry.ui.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class MinimapMindow extends Mindow2{
    public static Minimap2 m = new Minimap2(200f);
    public static MinimapRenderer2 renderer2 = new MinimapRenderer2();
    public MinimapMindow(){
        super("MindowMap");
        mindowName = "MindowMap";
    }

    @Override
    public void setupCont(Table cont){
        cont.clear();
        m.setMapSize(MI2USettings.getInt(mindowName + ".size", 200));
        cont.add(m).fill();
        cont.row();
        cont.label(() -> Strings.fixed(World.conv(player.x), 1) + ", "+ Strings.fixed(World.conv(player.y), 1));
        cont.row();
        cont.label(() -> Iconc.commandAttack + "  " + Strings.fixed(World.conv(Core.input.mouseWorldY()), 1) + ", "+ Strings.fixed(World.conv(Core.input.mouseWorldY()), 1));
    }

    @Override
    public void initSettings(){
        super.initSettings();
        settings.add(new FieldSettingEntry(SettingType.Int, mindowName + ".size", s -> {
            return Strings.canParseInt(s) && Strings.parseInt(s) >= 100 && Strings.parseInt(s) <= 600;
        }, "@settings.mindowMap.size", s -> rebuild()));
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
                    if(renderer2.getRegion() == null) return;
                    if(!clipBegin()) return;
                    
                    Draw.reset();
                    Draw.rect(renderer2.getRegion(), x + width / 2f, y + height / 2f, width, height);
    
                    if(renderer2.getTexture() != null){
                        Draw.alpha(parentAlpha);
                        renderer2.drawEntities(x, y, width, height, 0.75f, true);
                    }
    
                    clipEnd();
                }
            };

            margin(margin);

            addListener(new InputListener(){
                @Override
                public boolean scrolled(InputEvent event, float x, float y, float amountx, float amounty){
                    renderer2.zoomBy(amounty);
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
                        renderer2.setZoom(1f + y / height * (max - 1f));
                    }
                }
    
                @Override
                public void clicked(InputEvent event, float x, float y){
                    if(control.input instanceof DesktopInput inp){
                        try{
                            float sz = 16f * renderer2.getZoom();
                            float dx = (Core.camera.position.x / tilesize);
                            float dy = (Core.camera.position.y / tilesize);
                            dx = (2 * sz) <= world.width() ? Mathf.clamp(dx, sz, world.width() - sz) : world.width() / 2;
                            dy = (2 * sz) <= world.height() ? Mathf.clamp(dy, sz, world.height() - sz) : world.height() / 2;

                            inp.panning = true;
                            Core.camera.position.set(
                                ((x / width - 0.5f) * 2f * sz * tilesize + dx * tilesize), 
                                ((y / height - 0.5f) * 2f * sz * tilesize + dy * tilesize));
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
    }

    //may cause some problems about ui color
    public static class MinimapRenderer2{
        public static final Color tmpc = new Color();
        private static final float baseSize = 16f;
        private final Seq<Unit> units = new Seq<>();
        private Pixmap pixmap;
        private Texture texture;
        private TextureRegion region;
        private Rect rect = new Rect();
        private float zoom = 4;
    
        public MinimapRenderer2(){
            Events.on(WorldLoadEvent.class, event -> {
                reset();
                updateAll();
            });
            Events.on(TileChangeEvent.class, event -> {
                //TODO don't update when the minimap is off?
                if(!ui.editor.isShown()){
                    update(event.tile);
                }
            });
    
            Events.on(BuildTeamChangeEvent.class, event -> update(event.build.tile));
        }
    
        public Pixmap getPixmap(){
            return pixmap;
        }
    
        public @Nullable Texture getTexture(){
            return texture;
        }
    
        public void zoomBy(float amount){
            zoom += amount;
            setZoom(zoom);
        }
    
        public void setZoom(float amount){
            zoom = Mathf.clamp(amount, 1f, Math.max(world.width(), world.height()) / baseSize / 2f);
        }
    
        public float getZoom(){
            return zoom;
        }
    
        public void reset(){
            if(pixmap != null){
                pixmap.dispose();
                texture.dispose();
            }
            setZoom(4f);
            pixmap = new Pixmap(world.width(), world.height());
            texture = new Texture(pixmap);
            region = new TextureRegion(texture);
        }
    
        public void drawEntities(float x, float y, float w, float h, float scaling, boolean withLabels){
            if(!withLabels){
                updateUnitArray();
            }else{
                units.clear();
                Groups.unit.each(units::add);
            }
    
            float sz = baseSize * zoom;
            float dx = (Core.camera.position.x / tilesize);
            float dy = (Core.camera.position.y / tilesize);
            dx = (2 * sz) <= world.width() ? Mathf.clamp(dx, sz, world.width() - sz) : world.width() / 2;
            dy = (2 * sz) <= world.height() ? Mathf.clamp(dy, sz, world.height() - sz) : world.height() / 2;
    
            rect.set((dx - sz) * tilesize, (dy - sz) * tilesize, sz * 2 * tilesize, sz * 2 * tilesize);
    
            //draw a linerect of view area
            Lines.stroke(1f, new Color(1f, 1f, 1f, 0.5f));
            float cx = withLabels ? (Core.camera.position.x - rect.x) / rect.width * w : Core.camera.position.x / (world.width() * tilesize) * w;
            float cy = withLabels ? (Core.camera.position.y - rect.y) / rect.width * h : Core.camera.position.y / (world.height() * tilesize) * h;
            Lines.rect(x + cx - Core.graphics.getWidth() / rect.width * w / renderer.getScale() / 2f, 
                y + cy - Core.graphics.getHeight() / rect.width * h / renderer.getScale() / 2f, 
                Core.graphics.getWidth() / rect.width * w / renderer.getScale() , 
                Core.graphics.getHeight() / rect.width * h / renderer.getScale());
            Draw.color();
    
            for(Unit unit : units){
                float rx = withLabels ? (unit.x - rect.x) / rect.width * w : unit.x / (world.width() * tilesize) * w;
                float ry = withLabels ? (unit.y - rect.y) / rect.width * h : unit.y / (world.height() * tilesize) * h;
    
                float scale = Scl.scl(1f) / 2f * scaling * 32f;
                var region = unit.icon();
                //color difference between block and unit in setting
                Draw.mixcol(new Color(unit.team().color.r * 0.9f, unit.team().color.g * 0.9f, unit.team().color.b * 0.9f, 1f), 1f);
                Draw.rect(region, x + rx, y + ry, scale, scale * (float)region.height / region.width, unit.rotation() - 90);
                Draw.reset();
            }
    
            //always display labels
            if(true){
                for(Player player : Groups.player){
                    if(!player.dead()){
                        //float rx = player.x / (world.width() * tilesize) * w;
                        //float ry = player.y / (world.height() * tilesize) * h;
                        float rx = withLabels ? (player.x - rect.x) / rect.width * w : player.x / (world.width() * tilesize) * w;
                        float ry = withLabels ? (player.y - rect.y) / rect.width * h : player.y / (world.height() * tilesize) * h;
    
                        drawLabel(x + rx, y + ry, player.name, player.team().color);
                    }
                }
            }
    
            Draw.reset();
        }
    
        public void drawEntities(float x, float y, float w, float h){
            drawEntities(x, y, w, h, 1f, true);
        }
    
        public @Nullable TextureRegion getRegion(){
            if(texture == null) return null;
    
            //2 * sz = map width/height in tiles
            float sz = baseSize * zoom;
            float dx = (Core.camera.position.x / tilesize);
            float dy = (Core.camera.position.y / tilesize);
            dx = (2 * sz) <= world.width() ? Mathf.clamp(dx, sz, world.width() - sz) : world.width() / 2;
            dy = (2 * sz) <= world.height() ? Mathf.clamp(dy, sz, world.height() - sz) : world.height() / 2;
            float invTexWidth = 1f / texture.width;
            float invTexHeight = 1f / texture.height;
            float x = dx - sz, y = world.height() - dy - sz, width = sz * 2, height = sz * 2;
            region.set(x * invTexWidth, y * invTexHeight, (x + width) * invTexWidth, (y + height) * invTexHeight);
            return region;
        }
    
        public void updateAll(){
            for(Tile tile : world.tiles){
                pixmap.set(tile.x, pixmap.height - 1 - tile.y, colorFor(tile));
            }
            texture.draw(pixmap);
        }
    
        public void update(Tile tile){
            if(world.isGenerating() || !state.isGame()) return;
    
            if(tile.build != null && tile.isCenter()){
                tile.getLinkedTiles(other -> {
                    if(!other.isCenter()){
                        update(other);
                    }
                });
            }
    
            int color = colorFor(tile);
            pixmap.set(tile.x, pixmap.height - 1 - tile.y, color);
    
            Pixmaps.drawPixel(texture, tile.x, pixmap.height - 1 - tile.y, color);
        }
    
        public void updateUnitArray(){
            float sz = baseSize * zoom;
            float dx = (Core.camera.position.x / tilesize);
            float dy = (Core.camera.position.y / tilesize);
            dx = Mathf.clamp(dx, sz, world.width() - sz);
            dy = Mathf.clamp(dy, sz, world.height() - sz);
    
            units.clear();
            Units.nearby((dx - sz) * tilesize, (dy - sz) * tilesize, sz * 2 * tilesize, sz * 2 * tilesize, units::add);
        }
    
        private int colorFor(Tile tile){
            if(tile == null) return 0;
            int bc = tile.block().minimapColor(tile);
            Color color = tmpc.set(bc == 0 ? MapIO.colorFor(tile.block(), tile.floor(), tile.overlay(), tile.team()) : bc);
            color.mul(1f - Mathf.clamp(world.getDarkness(tile.x, tile.y) / 4f));
    
            return color.rgba();
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
