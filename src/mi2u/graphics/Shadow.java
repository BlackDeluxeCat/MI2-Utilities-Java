package mi2u.graphics;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Mathf;
import arc.math.geom.Position;
import arc.struct.FloatSeq;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mi2u.MI2UTmp;
import mi2u.MI2Utils;
import mindustry.gen.*;
import mindustry.graphics.Layer;
import mindustry.graphics.LightRenderer;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;

import java.lang.reflect.Field;

import static mindustry.Vars.*;

public class Shadow{
    public static Field fCircles = MI2Utils.getField(LightRenderer.class, "circles"), fSize = MI2Utils.getField(LightRenderer.class, "circleIndex"), fCircleX, fCircleY, fCircleR, fCircleC;
    public static int size = 0;
    public static float[] tmpc = new float[4];

    public static float layer = Layer.block - 2f;

    public static IndexGetterDrawc indexGetter;

    public static void init(){
        indexGetter = new IndexGetterDrawc();
    }

    public static void getIndex(){
        size = Math.min(MI2Utils.getValue(fSize, renderer.lights), 400);
    }

    public static void draw(Seq<Tile> tiles){
        Draw.z(layer);
        for(Tile tile : tiles){
            //draw white/shadow color depending on blend
            Draw.color((!tile.block().hasShadow || (state.rules.fog && tile.build != null && !tile.build.wasVisible)) ? Color.clear : Color.black);
            float bs = tile.block().size * tilesize;
            //Draw.rect(tile.block().region, tile.build == null ? tile.worldx() : tile.build.x, tile.build == null ? tile.worldy() : tile.build.y, bs, bs);
            Fill.rect(tile.build == null ? tile.worldx() : tile.build.x, tile.build == null ? tile.worldy() : tile.build.y, bs, bs);
        }
    }

    public static void applyShader(){
        if(MI2UShaders.shadow == null) return;
        //the layer of block shadow;
        Draw.drawRange(layer, 0.1f, () -> renderer.effectBuffer.begin(Color.clear), () -> {
            renderer.effectBuffer.end();
            renderer.effectBuffer.blit(MI2UShaders.shadow);
        });
    }

    public static void lightsUniformData(FloatSeq data){
        data.clear();
        if(size == 0) return;
        Seq<Object> seq = MI2Utils.getValue(fCircles, renderer.lights);
        if(seq == null) return;

        for(int i = 0; i < size; i++){
            if(i >= seq.size) break;
            pack(deepReflectObject(seq.get(i)));
            data.addAll(MI2UTmp.v3.x, MI2UTmp.v3.y);
        }
    }

    public static void pack(float[] values){
        if(values[0] < 0 || values[1] < 0 || values[2] < 0 || values[3] < 0 || values[3] > 100000f){
            MI2UTmp.v3.set(-10000f, 0f);
            return;
        }
        MI2UTmp.v3.set(Mathf.floor((values[0] + 100f) * 5)
                + Mathf.floor(values[2]) * 50000f,
                Mathf.floor((values[1] + 100f) * 5)
                + Mathf.floor(values[3]) * 50000f);
    }

    public static float[] deepReflectObject(Object circle){
        if(fCircleX == null) fCircleX = MI2Utils.getField(circle.getClass(), "x");
        if(fCircleY == null) fCircleY = MI2Utils.getField(circle.getClass(), "y");
        if(fCircleR == null) fCircleR = MI2Utils.getField(circle.getClass(), "radius");
        if(fCircleC == null) fCircleC = MI2Utils.getField(circle.getClass(), "color");

        tmpc[0] = fCircleX == null ? -1f : MI2Utils.getValue(fCircleX, circle);
        tmpc[1] = fCircleY == null ? -1f : MI2Utils.getValue(fCircleY, circle);
        var tile = world.tileWorld(tmpc[0], tmpc[1]);
        tmpc[2] = tile == null ? 0f : tile.build != null && Mathf.dst(tile.build.x, tile.build.y, tmpc[0], tmpc[1]) < 0.1f ? tile.block().size : 0f;   //whether the light comes from a building
        tmpc[3] = fCircleR == null ? -1f : MI2Utils.getValue(fCircleR, circle);
        tmpc[3] *= fCircleC == null ? 1f : MI2UTmp.c1.abgr8888(MI2Utils.getValue(fCircleC, circle)).a;
        return tmpc;
    }

    //hack way to get circles index
    public static class IndexGetterDrawc implements Drawc{
        public transient boolean added = false;

        @Override
        public float clipSize() {
            return 100000000f;
        }

        @Override
        public void draw() {
            getIndex();
        }

        @Override
        public Floor floorOn() {
            return null;
        }

        @Override
        public Building buildOn() {
            return null;
        }

        @Override
        public boolean onSolid() {
            return false;
        }

        @Override
        public float getX() {
            return 0;
        }

        @Override
        public float getY() {
            return 0;
        }

        @Override
        public float x() {
            return 0;
        }

        @Override
        public float y() {
            return 0;
        }

        @Override
        public int tileX() {
            return 0;
        }

        @Override
        public int tileY() {
            return 0;
        }

        @Override
        public Block blockOn() {
            return null;
        }

        @Override
        public Tile tileOn() {
            return null;
        }

        @Override
        public void set(Position position) {

        }

        @Override
        public void set(float v, float v1) {

        }

        @Override
        public void trns(Position position) {

        }

        @Override
        public void trns(float v, float v1) {

        }

        @Override
        public void x(float v) {

        }

        @Override
        public void y(float v) {

        }

        @Override
        public <T extends Entityc> T self() {
            return null;
        }

        @Override
        public <T> T as() {
            return null;
        }

        @Override
        public boolean isAdded() {
            return added;
        }

        @Override
        public boolean isLocal() {
            return true;
        }

        @Override
        public boolean isNull() {
            return false;
        }

        @Override
        public boolean isRemote() {
            return false;
        }

        @Override
        public boolean serialize() {
            return false;
        }

        @Override
        public int classId() {
            return 0;
        }

        @Override
        public int id() {
            return 0;
        }

        @Override
        public void add() {
            if (!this.added) {
                Groups.draw.add(this);
                this.added = true;
            }
        }

        @Override
        public void afterRead() {

        }

        @Override
        public void id(int i) {

        }

        @Override
        public void read(Reads reads) {

        }

        @Override
        public void remove() {
            if (this.added) {
                Groups.draw.remove(this);
                this.added = false;
            }
        }

        @Override
        public void update() {

        }

        @Override
        public void write(Writes writes) {

        }
    }
}
