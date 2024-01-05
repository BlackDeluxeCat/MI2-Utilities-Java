package mi2u.struct;

import arc.func.*;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.util.*;
import mi2u.MI2UTmp;

import java.util.*;

public class FloatDataRecorder{
    private float[] values;
    public Floatp getter = null;
    //the position of last data
    private int head = 0;
    private int size = 0;
    public boolean disable = false;
    public Boolf<FloatDataRecorder> disableF = null;
    public Prov<String> titleGetter = null;

    public FloatDataRecorder(){
        values = new float[16];
    }

    public FloatDataRecorder(int initsize){
        values = new float[initsize];
    }

    //max bit size
    public int cap(){
        return values.length;
    }

    //used bit size
    public int size(){
        return size;
    }

    public void reset(){
        head = 0;
        size = 0;
        Arrays.fill(values, 0f);
    }

    public float get(int before){
        if(before >= size) return size == 0 ? values[head] : values[Mathf.mod(head + 1, size)];
        if(before < 0) return values[head];
        return values[head - before + ((head - before) < 0 ? values.length : 0)];
    }

    public void add(float value){
        if(++head >= values.length) head = 0;
        values[head] = value;
        size = Math.min(++size, values.length);
    }

    //starts from the latest record.
    public void each(Cons<Float> cons){
        for(int i = size - 1; i >= 0; i--){
            cons.get(get(i));
        }
    }

    public float min(){
        return min(f -> f);
    }

    public float max(){
        return max(f -> f);
    }

    public float min(FloatFloatf func){
        float min = get(0), check = func.get(min), result;
        for(int i = size - 1; i >= 1; i--){
            result = func.get(get(i));
            if(check > result){
                min = get(i);
                check = result;
            }
        }
        return min;
    }

    public float max(FloatFloatf func){
        float max = get(0), check = func.get(max), result;
        for(int i = size - 1; i >= 1; i--){
            result = func.get(get(i));
            if(check < result){
                max = get(i);
                check = result;
            }
        }
        return max;
    }

    public float avg(){
        if(size == 0) return 0f;
        float result = 0f;
        for(int i = size - 1; i >= 1; i--){
            result += get(i);
        }
        return result / (float)size;
    }

    public void update(){
        if(disableF != null) disable = disableF.get(this);
        if(disable) return;
        if(getter == null) return;
        add(getter.get());
    }

    public void resize(int newSize){
        float[] newArray = new float[newSize];
        if(newSize > values.length){
            System.arraycopy(values, 0, newArray, 0, head + 1);
            System.arraycopy(values, head + 1, newArray, head + 1 + newSize - values.length, values.length - (head + 1));
        }else{
            //i don't want to make it, just clean data
            head = 0;
        }
        this.values = newArray;
    }

    public String getTitle(){
        return titleGetter != null ? titleGetter.get():"";
    }

    public void defaultDraw(float x, float y, float width, float height, boolean lineChart, float min, float max){
        if(lineChart){
            //折线图
            Lines.beginLine();
            for(int i = 0; i < size; i++){
                Lines.linePoint(x + (size - i) * width/(float)size, y + height*Mathf.clamp((get(i)-min+1f)/(max-min+1f)));
            }
            Lines.endLine();
        }else{
            //柱状图
            for(int i = 0; i < size; i++){
                Fill.rect(MI2UTmp.r1.set(x + (size - i) * width/(float)size, y,  width/(float)size, height*Mathf.clamp((get(i)-min+1f)/(max-min+1f))));
            }
        }
    }

    public void defaultDraw(float x, float y, float width, float height, boolean lineChart){
        defaultDraw(x, y, width, height, lineChart, min(), max());
    }
}
