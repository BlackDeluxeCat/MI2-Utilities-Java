package mi2u.io;

import arc.*;
import arc.files.*;
import arc.func.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import mindustry.*;

@Deprecated
public class MI2USettings{
    public static final OrderedMap<String, MI2USetting> map = new OrderedMap<>();
    private static Fi root, dir;

    public static Json json = new Json();

    public static void init(){
        Core.settings.setAppName(Vars.appName);
        root = Vars.dataDirectory.child("mods").child("MI2U_Settings");
        dir = root.child("MI2USettings.mi2u");
        load();
    }

    private static void quietPut(String name, String value){
        MI2USetting ss = map.get(name);
        if(ss != null){
            ss.value = value;
        }else{
            new MI2USetting(name, value);
        }
    }

    public static int getInt(String name, int def){
        MI2USetting obj = map.get(name);
        if(obj == null) return def;
        return Strings.parseInt(obj.value, def);
    }

    public static int getInt(String name){
        return getInt(name, 0);
    }

    public static String getStr(String name, String def){
        MI2USetting obj = map.get(name);
        if(obj == null) return def;
        return obj.value;
    }

    public static String getStr(String name){
        return getStr(name, "");
    }

    public static boolean getBool(String name, boolean def){
        MI2USetting obj = map.get(name);
        if(obj == null) return def;
        return obj.value.equals("true");
    }

    public static boolean getBool(String name){
        return getBool(name, false);
    }

    public static <T> T getJson(String name, Class<T> clazz, Class elementClazz, Prov<T> def){
        MI2USetting obj = map.get(name);
        if(obj == null) return def.get();
        return json.fromJson(clazz, elementClazz, obj.value);
    }

    public static boolean load(){
        if(!dir.exists()){
            Log.warn("MI2U settings file not found, load failed");
            return false;
        }
		try{
            var reads = dir.reads();
            int index = 0;
            String name, value;
            name = reads.str();
            if(!name.equals("MI2USettingsHead")){
                Log.warn("Invaild MI2U settings file head, load failed: " + name);
                return false;
            }
            do{
                name = reads.str();
                if(name.equals("end")) break;
                value = reads.str();
                if(value.equals("end")) break;
                quietPut(name, value);
            }while(index++ < 1000);
            Log.infoTag("MI2U", index + " Settings loaded.");
            reads.close();
        }catch(Throwable e){
            Log.errTag("MI2U", "Settings load failed: " + e.toString());
            return false;
        }

        return true;
    }

    public static class MI2USetting{
        public String name;
        protected String value;

        public MI2USetting(String name, String value){
            this.name = name;
            this.value = value;
            map.put(name, this);
        }

        public String get(){
            return value;
        }
    }
}