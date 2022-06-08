package mi2u.ui;

import arc.*;
import arc.graphics.*;
import arc.math.geom.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

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

        unitt.update(() -> {
            if(unit == lastUnit) return;
            lastUnit = unit;
            unitt.clear();
            if(unit != null){
                unit.display(unitt);
            }
        });

        buildt.update(() -> {
            if(build == lastBuild) return;
            lastBuild = build;
            buildt.clear();
            if(build != null){
                build.display(buildt);
            }
        });

        tilet.defaults().growX();
        tilet.table(t -> {
            float lw = 55f;
            t.left();
            t.add(new Image(){
                Block last;
                TextureRegionDrawable icon = new TextureRegionDrawable();
                {
                    update(() -> {
                        if(tile == null || tile.floor() == last) return;
                        last = tile.floor();
                        this.setDrawable(tile != null ? icon.set(tile.floor().uiIcon) : null);
                    });
                }
            }).size(8 * 4);
            t.labelWrap(() -> tile != null ? tile.floor().localizedName : "").left().padLeft(5).width(lw);

            t.add(new Image(){
                Block last;
                TextureRegionDrawable icon = new TextureRegionDrawable();
                {
                    update(() -> {
                        if(tile == null || tile.overlay() == last) return;
                        last = tile.overlay();
                        this.setDrawable((tile != null && tile.overlay() != null && tile.overlay() != Blocks.air) ? icon.set(tile.overlay().uiIcon) : null);
                    });
                }
            }).size(8 * 4);
            t.labelWrap(() -> tile != null && tile.overlay() != null && tile.overlay() != Blocks.air ? tile.overlay().localizedName : "").left().padLeft(5).width(lw);

            t.add(new Image(){
                Block last;
                TextureRegionDrawable icon = new TextureRegionDrawable();
                {
                    update(() -> {
                        if(tile == null || tile.block() == last) return;
                        last = tile.block();
                        this.setDrawable((tile != null && tile.block() instanceof Prop || tile.block() instanceof TreeBlock) ? icon.set(tile.block().uiIcon) : null);
                    });
                }
            }).size(8 * 4);
            t.labelWrap(() -> (tile != null && tile.block() instanceof Prop || tile.block() instanceof TreeBlock) ? tile.block().localizedName : "").left().padLeft(5).width(lw);
        }).left();
    }

    public void build(){
        clear();
        table().growX().update(t -> {
            t.clear();
            t.defaults().growX().padBottom(4f);

            if(build != null){
                t.add(buildt);
                t.row();
            }

            if(unit != null){
                t.add(unitt);
                t.row();
            }

            if(tile != null){
                t.add(tilet);
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
        }else{
            build = null;
            tile = null;
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
