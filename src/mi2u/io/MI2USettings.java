package mi2u.io;

import arc.*;
import arc.files.Fi;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Strings;
import arc.util.Timer;
import mindustry.Vars;

public class MI2USettings {
    private static Seq<MI2USetting> map = new Seq<MI2USetting>();
    private static Fi root, dir;
    private static boolean modified = false;

    public static void init(){
        Core.settings.setAppName(Vars.appName);
        root = Vars.dataDirectory.child("mods").child("MI2U_Settings");
        dir = root.child("MI2USettings.mi2u");
        load();
        Timer.schedule(() -> {
            if(modified && !Vars.state.isGame()){
                save();
                modified = false;
            }
        }, 600f);
    }
    
    public static boolean hasField(String name){
        map.contains(s -> {return s.name.equals(name);});
        return false;
    }

    public static void putInt(String name, int value){
        MI2USetting ss = map.find(s -> {return s.name.equals(name);});
        if(ss != null){
            ss.value = String.valueOf(value);
        }else{
            map.add(new MI2USetting(name, String.valueOf(value)));
        }
        modified = true;
    }

    public static void putString(String name, String value){
        MI2USetting ss = map.find(s -> {return s.name.equals(name);});
        if(ss != null){
            ss.value = value;
        }else{
            map.add(new MI2USetting(name, value));
        }
        modified = true;
    }

    public static void putBool(String name, Boolean value){
        MI2USetting ss = map.find(s -> {return s.name.equals(name);});
        if(ss != null){
            ss.value = String.valueOf(value);
        }else{
            map.add(new MI2USetting(name, String.valueOf(value)));
        }
        modified = true;
    }

    @Nullable
    public static int getInt(String name){
        MI2USetting obj = map.find(s -> {return s.name.equals(name);});
        if(obj == null) return 0;
        return Strings.parseInt(obj.value, 0);
    }

    public static String getStr(String name){
        MI2USetting obj = map.find(s -> {return s.name.equals(name);});
        if(obj == null) return "";
        return obj.value;
    }

    public static boolean getBool(String name){
        MI2USetting obj = map.find(s -> {return s.name.equals(name);});
        if(obj == null) return false;
        return obj.value.equals("true");
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
                map.add(new MI2USetting(name, value));
                Log.info("MI2U loading setting: " + name + "-" + value);
            }while(index++ < 1000);
            reads.close();
        }catch(Throwable e){
            Log.err("MI2U settings load failed", e);
            return false;
        }

        return true;
    }

    public static void save(){
        try{
            Fi predir = root.child("MI2USettings.mi2u");
            if(predir.exists()) {
                predir.moveTo(root.child("MI2USettings_backup.mi2u"));
            }
            var writes = dir.writes();
            writes.str("MI2USettingsHead");
            map.each(s -> {
                writes.str(s.name);
                writes.str(s.value);
            });
            writes.str("end");
            writes.close();
        }catch(Throwable e){
            Log.err("MI2U Settings save failed", e);
        }
        
    }

    public static class MI2USetting{
        public String name;
        public String value;

        public MI2USetting(){

        }

        public MI2USetting(String name, String value){
            this.name = name;
            this.value = value;
        }
    }
}
