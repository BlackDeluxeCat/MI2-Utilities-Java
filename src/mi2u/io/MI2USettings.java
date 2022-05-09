package mi2u.io;

import arc.*;
import arc.files.*;
import arc.func.Cons;
import arc.func.Prov;
import arc.scene.ui.layout.Table;
import arc.struct.*;
import arc.util.*;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.game.EventType.*;

public class MI2USettings{
    public static Seq<SettingGroup> groups = new Seq<>();

    private static Seq<MI2USetting> map = new Seq<>();
    private static Fi root, dir;
    private static boolean modified = false;
    private static Interval timer = new Interval();

    public static void init(){
        Core.settings.setAppName(Vars.appName);
        root = Vars.dataDirectory.child("mods").child("MI2U_Settings");
        dir = root.child("MI2USettings.mi2u");
        load();
        Events.run(Trigger.update, () -> {
            if(modified && timer.get(60f) && !Vars.state.isGame()){
                save();
                modified = false;
            }
        });
    }
    
    public static MI2USetting getSetting(String name){
        return map.find(s -> {return s.name.equals(name);});
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

    public static void putStr(String name, String value){
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

    public static int getInt(String name, int def){
        MI2USetting obj = map.find(s -> {return s.name.equals(name);});
        if(obj == null) return def;
        return Strings.parseInt(obj.value, 0);
    }

    public static int getInt(String name){
        return getInt(name, 0);
    }

    public static String getStr(String name, String def){
        MI2USetting obj = map.find(s -> {return s.name.equals(name);});
        if(obj == null) return def;
        return obj.value;
    }

    public static String getStr(String name){
        return getStr(name, "");
    }

    public static boolean getBool(String name, boolean def){
        MI2USetting obj = map.find(s -> {return s.name.equals(name);});
        if(obj == null) return def;
        return obj.value.equals("true");
    }

    public static boolean getBool(String name){
        return getBool(name, false);
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
            }while(index++ < 1000);
            Log.infoTag("MI2U", index + " Settings loaded.");
            reads.close();
        }catch(Throwable e){
            Log.errTag("MI2U", "Settings load failed: " + e.toString());
            return false;
        }

        return true;
    }

    public static void save(){
        Log.infoTag("MI2U Settings", "Saved");
        try{
            Fi predir = root.child("MI2USettings.mi2u");
            if(predir.exists()) {
                predir.moveTo(root.child("MI2USettings_backup.mi2u"));
            }
            var writes = dir.writes();
            writes.str("MI2USettingsHead");
            map.each(s -> {
                s.save(writes);
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
        public Prov<String> getter;
        public Cons<String> setter;

        public MI2USetting(){
        }

        public MI2USetting(String name, String value){
            this.name = name;
            this.value = value;
        }

        public void save(Writes writes){
            writes.str(name);
            writes.str(value);
        }
    }

    /** SettingGroup是一系列选项的组合体。创建实例时自动注册到静态类变量groups中，可以通过静态类方法按名字获取指定选项组。
     * 应提供选项组交互界面的生成方法，成员选项的修改与保存应通过交互界面完成。
     * 创建时必须生成相应的MI2USetting对象（如果没有）。
     * Mindow2选项组建议在创建Mindow时即创建，其他选项组在选项影响的相关功能创建时创建，以方便保存副本和设置getter setter
     * 需要在创建地方以外的地方调用选项组，建议用getGroup方法并作检验
     */
    //TODO 调整put和get方法，强制读写MI2USetting时创建
    public static class SettingGroup{
        public String name;
        public String displayName;
        public Seq<MI2USetting> settings = new Seq<>();

        public SettingGroup(String name, String displayName){
            this.name = name;
            this.displayName = displayName;
            groups.add(this);
        }

        public void addMember(MI2USetting setting){
            if(settings.contains(set -> set.name == setting.name)) return;
            settings.add(setting);
        }

        public void build(Table table){

        }
    }
}
