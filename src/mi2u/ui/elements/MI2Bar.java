package mi2u.ui.elements;
import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.ui.layout.*;
import arc.util.pooling.*;
import mindustry.ui.*;

public class MI2Bar extends Element{
    private boolean vertical = false;
    private Floatp fraction;
    private CharSequence name = "";
    private float value, lastValue, blink, outlineRadius, fontScaleX = 1, fontScaleY = 1;
    private Color blinkColor = new Color(), outlineColor = new Color();

    public MI2Bar(String name, Color color, Floatp fraction){
        this.fraction = fraction;
        this.name = Core.bundle.get(name, name);
        this.blinkColor.set(color);
        lastValue = value = fraction.get();
        setColor(color);
    }

    public MI2Bar(Prov<String> name, Prov<Color> color, Floatp fraction){
        this.fraction = fraction;
        this.blinkColor.set(color.get());
        try{
            lastValue = value = Mathf.clamp(fraction.get());
        }catch(Exception e){ //getting the fraction may involve referring to invalid data
            lastValue = value = 0f;
        }
        update(() -> {
            try{
                this.name = name.get();
                setColor(color.get());
            }catch(Exception e){ //getting the fraction may involve referring to invalid data
                this.name = "";
            }
        });
    }

    public MI2Bar(){

    }

    public void reset(float value){
        this.value = lastValue = blink = value;
    }

    public void set(Prov<String> name, Floatp fraction, Color color){
        this.fraction = fraction;
        this.lastValue = fraction.get();
        this.blinkColor.set(color);
        setColor(color);
        update(() -> this.name = name.get());
    }

    public void set(Prov<String> name, Floatp fraction, Prov<Color> color){
        this.fraction = fraction;
        this.lastValue = fraction.get();
        this.blinkColor.set(color.get());
        update(() -> {
            this.name = name.get();
            setColor(color.get());
        });
    }

    public MI2Bar setVertical(boolean v){
        vertical = v; 
        return this;
    }

    public void snap(){
        lastValue = value = fraction.get();
    }

    public MI2Bar outline(Color color, float stroke){
        outlineColor.set(color);
        outlineRadius = Scl.scl(stroke);
        return this;
    }

    public void flash(){
        blink = 1f;
    }

    public MI2Bar blink(Color color){
        blinkColor.set(color);
        return this;
    }

    @Override
    public void draw(){
        if(fraction == null) return;

        float computed;
        try{
            computed = Mathf.clamp(fraction.get());
        }catch(Exception e){ //getting the fraction may involve referring to invalid data
            computed = 0f;
        }

        if(lastValue > computed) blink = 1f;
        lastValue = computed;

        if(Float.isNaN(lastValue)) lastValue = 0;
        if(Float.isInfinite(lastValue)) lastValue = 1f;
        if(Float.isNaN(value)) value = 0;
        if(Float.isInfinite(value)) value = 1f;
        if(Float.isNaN(computed)) computed = 0;
        if(Float.isInfinite(computed)) computed = 1f;

        blink = Mathf.lerpDelta(blink, 0f, 0.2f);
        value = Mathf.lerpDelta(value, computed, 0.15f);


        float innerWidth = width - 2*outlineRadius;
        float innerHeight = height - 2*outlineRadius;

        //外框
        Draw.color(outlineColor);
        Draw.alpha(parentAlpha);
        Fill.rect(centerX(), centerY(), width, height);

        Draw.color(Color.black);
        Draw.alpha(parentAlpha);
        Fill.rect(centerX(), centerY(), innerWidth, innerHeight);

        //水平竖直条
        if(!vertical){
            Draw.color(color, blinkColor, blink);
            Draw.alpha(parentAlpha);
            float topWidth = innerWidth * value;
            Fill.rect(centerX() - innerWidth / 2 + topWidth / 2, centerY(), topWidth, innerHeight);
        }else{
            Draw.color(color, blinkColor, blink);
            Draw.alpha(parentAlpha);
            float topHeight = innerHeight * value;
            Fill.rect(centerX(), centerY() - innerHeight / 2 + topHeight / 2, innerWidth, topHeight);
        }


        Draw.color();

        Font font = Fonts.outline;
        float oldScaleX = font.getScaleX();
        float oldScaleY = font.getScaleY();
        font.getData().setScale(fontScaleX, fontScaleY);

        GlyphLayout lay = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
        lay.setText(font, name);

        font.setColor(1f, 1f, 1f, 1f);
        font.getCache().clear();
        font.getCache().addText(name, centerX() - lay.width / 2f, centerY() + lay.height / 2f + 1);
        font.getCache().draw(parentAlpha);

        font.getData().setScale(oldScaleX, oldScaleY);

        Pools.free(lay);
    }

    public float centerX(){
        return x + width / 2;
    }

    public float centerY(){
        return y + height / 2;
    }


    public MI2Bar setFontScaleX(float scaleX){
        fontScaleX = scaleX;
        return this;
    }

    public MI2Bar setFontScaleY(float scaleY){
        fontScaleY = scaleY;
        return this;
    }

    public MI2Bar setFontScale(float scale){
        fontScaleX = scale;
        fontScaleY = scale;
        return this;
    }
    public float getFontScaleX() {
        return fontScaleX;
    }

    public float getFontScaleY() {
        return fontScaleY;
    }
}