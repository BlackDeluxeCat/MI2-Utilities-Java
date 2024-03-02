package mi2u;

import arc.struct.*;
import arc.util.*;

import java.lang.reflect.*;
import java.util.*;

public class MI2Utils{
    public static ObjectMap<String, Field> fields = new ObjectMap<>();

    public static Field getField(Class<?> clazz, String name){
        Field f = fields.get(clazz.getName() + "." + name);
        if(f == null){
            try{
                Field f2 = clazz.getDeclaredField(name);
                f2.setAccessible(true);
                fields.put(clazz.getName() + "." + name, f2);
                return f2;
            }catch(Exception e){
                return null;
            }
        }
        return f;
    }

    public static <T> T getValue(Class<?> clazz, String name){
        try{
            return (T)getField(clazz, name).get(clazz);
        }catch(Exception ignored){
            return null;
        }
    }

    public static <T> T getValue(Object obj, String name){
        try{
            return (T)getField(obj.getClass(), name).get(obj);
        }catch(Exception ignored){
            return null;
        }
    }

    public static <T> T getValue(Field field, Object obj){
        try{
            return (T)field.get(obj);
        }catch(Exception ignored){
            return null;
        }
    }

    public static <T> T getValue(Field field, Class<?> clazz){
        try{
            return (T)field.get(clazz);
        }catch(Exception ignored){
            return null;
        }
    }

    public static void setValue(Object obj, String name, Object value){
        if(obj == null || value == null) return;
        try{
            getField(obj.getClass(), name).set(obj, value);
        }catch(Exception ignored){}
    }

    public static class IntervalMillis{
        long[] times;

        public IntervalMillis(int capacity){
            times = new long[capacity];
        }

        public IntervalMillis(){
            this(1);
        }

        public boolean get(long time){
            return get(0, time);
        }

        public boolean get(int id, long time){
            if(id >= times.length) throw new RuntimeException("Out of bounds! Max timer size is " + times.length + "!");

            boolean got = check(id, time);
            if(got) times[id] = Time.millis();
            return got;
        }

        public boolean check(int id, long time){
            return Time.millis() - times[id] >= time || Time.millis() < times[id];
        }

        public void reset(int id, long time){
            times[id] = Time.millis() - time;
        }

        public void clear(){
            Arrays.fill(times, 0);
        }

        public long getTime(int id){
            return Time.millis() - times[id];
        }

        public long[] getTimes(){
            return times;
        }
    }
}
