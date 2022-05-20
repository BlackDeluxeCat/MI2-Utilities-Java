package mi2u.ui;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import arc.math.geom.Vec2;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import arc.util.Nullable;
import mindustry.content.Blocks;
import mindustry.entities.Units;
import mindustry.gen.Tex;
import mindustry.ui.Displayable;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Prop;
import mindustry.world.blocks.environment.StaticWall;
import mindustry.world.blocks.environment.TreeBlock;

import static mindustry.Vars.*;

public class HoverTopTable extends Table {
    public static HoverTopTable hoverInfo = new HoverTopTable();

    public Displayable unit, build, lastUnit, lastBuild; Tile tile, lastTile;
    public Table unitt, buildt, tilet;

    public HoverTopTable(){
        initChild();
        build();
    }

    public void initChild(){
        unitt = new Table();
        buildt = new Table();
        tilet = new Table();

        //TODO 更节约性能的update
        unitt.update(() -> {
            if(unit == lastUnit) return;
            lastUnit = unit;
            Log.info("new unit");
            unitt.clear();
            if(unit != null){
                unit.display(unitt);
            }
        });

        buildt.update(() -> {
            if(build == lastBuild) return;
            lastBuild = build;
            Log.info("new building");
            buildt.clear();
            if(build != null){
                build.display(buildt);
            }
        });

        tilet.defaults().growX();
        tilet.table(t -> {
            t.left();
            t.add(new Image(){
                Block last;
                {
                    update(() -> {
                        if(tile == null || tile.floor() == last) return;
                        last = tile.floor();
                        this.setDrawable(tile != null ? new TextureRegionDrawable(tile.floor().uiIcon) : null);
                    });
                }
            }).size(8 * 4);
            t.labelWrap(() -> tile != null ? tile.floor().localizedName : "").left().padLeft(5);
        }).left();

        tilet.table(t -> {
            t.left();
            t.add(new Image(){
                Block last;
                {
                    update(() -> {
                        if(tile == null || tile.overlay() == last) return;
                        last = tile.overlay();
                        this.setDrawable((tile != null && tile.overlay() != null && tile.overlay() != Blocks.air) ? new TextureRegionDrawable(tile.overlay().uiIcon) : null);
                    });
                }
            }).size(8 * 4);
            t.labelWrap(() -> tile != null && tile.overlay() != null && tile.overlay() != Blocks.air ? tile.overlay().localizedName : "").left().padLeft(5);
        }).left();

        tilet.table(t -> {
            t.left();
            t.add(new Image(){
                Block last;
                {
                    update(() -> {
                        if(tile == null || tile.block() == last) return;
                        last = tile.block();
                        this.setDrawable((tile != null && tile.block() instanceof Prop || tile.block() instanceof TreeBlock) ? new TextureRegionDrawable(tile.block().uiIcon) : null);
                    });
                }
            }).size(8 * 4);
            t.labelWrap(() -> (tile != null && tile.block() instanceof Prop || tile.block() instanceof TreeBlock) ? tile.block().localizedName : "").left().width(90f).padLeft(5);
        }).left();
    }

    public void build(){
        clear();
        table().growX().update(t -> {
            t.clear();
            t.defaults().growX().padBottom(2f);
            if(unit != null){
                t.add(unitt);
                t.row();
            }

            if(tile != null){
                t.add(tilet);
                t.row();
            }

            if(build != null){
                t.add(buildt);
                t.row();
            }
            addColorBar(t);
            t.row();
        });
    }

    /** Returns the thing being hovered over. */
    @Nullable
    public void hovered(){
        Vec2 v = this.stageToLocalCoordinates(Core.input.mouse());

        //if the mouse intersects the table or the UI has the mouse, no hovering can occur
        if(Core.scene.hasMouse() || this.hit(v.x, v.y, false) != null) return;

        //check for a unit
        unit = Units.closestOverlap(null, Core.input.mouseWorldX(), Core.input.mouseWorldY(), 5f, u -> true);

        //check tile being hovered over
        Tile hoverTile = world.tileWorld(Core.input.mouseWorld().x, Core.input.mouseWorld().y);
        if(hoverTile != null){
            //if the tile has a building, display it
            build = hoverTile.build;

            //if the tile has a drop, display the drop
            //if(hoverTile.drop() != null || hoverTile.wallDrop() != null){
                tile = hoverTile;
            //}
        }
    }

    public boolean hasInfo(){
        hovered();
        return unit != null || tile != null || build != null;
    }

    public void addColorBar(Table table){
        table.table(Tex.whiteui).height(4f).growX().pad(4f,0f,4f,0f).color(Color.grays(0.2f));
    }
}
