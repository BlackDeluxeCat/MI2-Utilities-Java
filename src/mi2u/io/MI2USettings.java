package mi2u.io;

import arc.*;
import arc.files.*;
import arc.func.Boolp;
import arc.func.Cons;
import arc.func.Func;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.scene.ui.Button;
import arc.scene.ui.TextButton;
import arc.scene.ui.TextField.*;
import arc.scene.ui.layout.Table;
import arc.struct.*;
import arc.util.*;
import arc.util.io.Writes;
import mi2u.game.MI2UEvents;
import mi2u.ui.elements.Mindow2;
import mindustry.game.EventType.*;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.Vars;

import static mi2u.MI2UVars.*;

public class MI2USettings{

    public static final OrderedMap<String, MI2USetting> map = new OrderedMap<>();
    private static Fi root, dir;
    public static boolean modified = false;
    private static final Interval timer = new Interval();

    public static Seq<SettingEntry> entries = new Seq<>();

    public static void init(){
        Core.settings.setAppName(Vars.appName);
        root = Vars.dataDirectory.child("mods").child("MI2U_Settings");
        dir = root.child("MI2USettings.mi2u");
        load();
        Events.run(Trigger.update, () -> {
            if(modified && timer.get(60f)){
                save();
                modified = false;
            }
        });

        Events.fire(new MI2UEvents.FinishSettingInitEvent());
    }
    
    public static MI2USetting getSetting(String name){
        return map.get(name);
    }

    public static MI2USetting putInt(String name, int value){
        MI2USetting ss = map.get(name);
        if(ss != null){
            ss.value = String.valueOf(value);
        }else{
            ss = new MI2USetting(name, String.valueOf(value));
        }
        modified = true;
        return ss;
    }

    private static void quietPut(String name, String value){
        MI2USetting ss = map.get(name);
        if(ss != null){
            ss.value = value;
        }else{
            new MI2USetting(name, value);
        }
    }

    public static MI2USetting putStr(String name, String value){
        MI2USetting ss = map.get(name);
        if(ss != null){
            ss.value = value;
        }else{
            ss = new MI2USetting(name, value);
        }
        modified = true;
        return ss;
    }

    public static MI2USetting putBool(String name, Boolean value){
        MI2USetting ss = map.get(name);
        if(ss != null){
            ss.value = String.valueOf(value);
        }else{
            ss = new MI2USetting(name, String.valueOf(value));
        }
        modified = true;
        return ss;
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

    public static void save(){
        Log.infoTag("MI2U Settings", map.size + " Saved");
        try{
            Fi predir = root.child("MI2USettings.mi2u");
            if(predir.exists()) {
                predir.moveTo(root.child("MI2USettings_backup.mi2u"));
            }
            var writes = dir.writes();
            writes.str("MI2USettingsHead");
            map.orderedKeys().each(s -> map.get(s).save(writes));
            writes.str("end");
            writes.close();
        }catch(Throwable e){
            Log.err("MI2U Settings save failed", e);
        }
        
    }

    //TODO better validation and default.
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

        public void put(String newStr){
            this.value = newStr;
            modified = true;
        }

        public void save(Writes writes){
            writes.str(name);
            writes.str(value);
        }
    }

    public static SettingEntry getEntry(String name){
        return entries.find(g -> g.name.equals(name));
    }

    /**
     * Entry主要负责选项ui构建,与MI2USetting不同,它应该是可重复的. 并且除了不重复构造的Group以外,不能在Settings类做任何存储
     */
    public static class SettingEntry{
        public String name;
        public String help;

        public SettingEntry(String name, String help){
            this.name = name;
            this.help = help;
            entries.add(this);
        }

        public void build(Table table){}
    }

    public static class SingleEntry extends SettingEntry{
        public @Nullable MI2USetting setting;

        public SingleEntry(String name, String help){
            super(name, help);
            setting = getSetting(name);
        }

        @Override
        public void build(Table table){
            setting = getSetting(name);
            table.labelWrap(() -> this.name + " = " + (setting != null ? setting.get() : "invaild")).left().growX().get().setColor(0, 1, 1, 0.7f);
        }
    }

    public static class CheckEntry extends SingleEntry{
        public Cons<Boolean> changed;
        public boolean value;

        public CheckEntry(String name, String help, Boolean def, Cons<Boolean> changed){
            super(name, help);
            this.changed = changed;
            if(setting == null) setting = putBool(name, def);   //父类构造时已按name尝试抓取，子类构造只需要按def类型处理put
            value = setting.get().equals("true");
        }

        @Override
        public void build(Table table){
            if(table != null){
                table.button(setting.name, textbtoggle, () -> {
                    value = !value;
                    setting.put(String.valueOf(value));
                    if(changed != null) changed.get(value);
                }).update(b -> b.setChecked(value = setting.get().equals("true"))).left().with(c -> {
                    c.getLabelCell().width(200).height(32).padLeft(4f).padRight(4f);
                    c.getLabel().setWrap(true);
                    c.getLabel().setAlignment(Align.left);
                    c.margin(3f);
                });

                table.add(help).right().self(c -> {
                    c.growX();
                    c.get().setWrap(true);
                    c.get().setAlignment(Align.right);
                });
            }
        }

        public Button newTextButton(String text){
            var b = new TextButton(text);
            b.setStyle(textbtoggle);
            funcSetTextb.get(b);
            b.clicked(() -> {
                value = !value;
                setting.put(String.valueOf(value));
                if(changed != null) changed.get(value);
            });
            b.update(() -> b.setChecked(value = setting.get().equals("true")));
            return b;
        }
    }

    /**
     * 文本框选项，只处理String或Integer的选项。
     * 当选项是Int类型，构造方法的def需用String.valueOf转成String，validator需做Strings.canParseInt检查(或利用intValidator)，自定义的changed需用String.parseInt转换为整数使用。
     */
    public static class FieldEntry extends SingleEntry{
        public Cons<String> changed;
        public TextFieldFilter filter;
        public TextFieldValidator validator;

        public FieldEntry(String name, String help, String def, TextFieldFilter filter, TextFieldValidator validator, Cons<String> changed){
            super(name, help);
            this.changed = changed;
            this.filter = filter;
            this.validator = validator;
            if(setting == null) setting = putStr(name, def);    //父类构造时已按name尝试抓取，子类构造只需要按def类型处理put
            if(this.validator != null && !validator.valid(setting.value)) setting.value = def;
        }

        @Override
        public void build(Table table){
            if(table != null){
                table.table(t -> {
                    t.field(setting.value, Styles.nodeField, str -> {
                        if(validator != null && !validator.valid(str)) return;
                        setting.put(str);
                        if(changed != null) changed.get(str);
                    }).with(tf -> {
                        tf.setValidator(validator);
                        tf.setFilter(filter);
                    }).width(200f).left().padRight(20);

                    t.row();
                    t.label(() -> name + " = " + setting.get()).get().setColor(0, 1, 1, 0.7f);
                });

                table.add(help).right().self(c -> {
                    c.growX();
                    c.get().setWrap(true);
                    c.get().setAlignment(Align.right);
                });
            }
        }
    }

    //Still don't know how to fit integer, boolean items array
    public static class ChooseEntry extends SingleEntry{
        public String[] items;
        /** process item to display on TextButton */
        public Func<String, String> buttonTextFunc;
        public Cons<String> changed;
        public ChooseEntry(String name, String help, String[] items, Func<String, String> buttonTextFunc, Cons<String> changed){
            super(name, help);
            this.items = items;
            this.buttonTextFunc = buttonTextFunc;
            this.changed = changed;
            if(setting == null) setting = putStr(name, items[0]);
        }

        @Override
        public void build(Table table){
            if(table != null){
                if(items != null){
                    table.table(t -> {
                        t.table(it -> {
                            it.defaults().growX().uniform();
                            int i = 0;
                            for(var item : items){
                                it.button(buttonTextFunc != null ? buttonTextFunc.get(item) : item, textbtoggle, () -> {
                                    setting.put(item);
                                    if(changed != null) changed.get(item);
                                }).with(funcSetTextb).update(b -> b.setChecked(setting.get().equals(item)));
                                if(Mathf.mod(++i, 4) == 0){
                                    it.row();
                                    i = 0;
                                }
                            }

                        }).growX();

                        t.row();
                        t.label(() -> name + " = " + setting.get()).left().get().setColor(0, 1, 1, 0.7f);
                    }).width(200f).left();
                }

                table.add(help).right().self(c -> {
                    c.growX();
                    c.get().setWrap(true);
                    c.get().setAlignment(Align.right);
                });
            }
        }
    }

    /** SettingGroup主要用于自定义交互界面。创建实例时自动注册到静态类变量groups中，可以通过静态类方法按名字获取指定选项组。
     * 应提供选项组交互界面的生成方法，选项的修改与保存通过界面的自定义操作完成。
     * //创建时必须新建所需的MI2USetting字段。
     * Mindow2选项组建议在创建Mindow时即创建，其他选项组在选项影响的相关功能创建时创建，以方便保存副本和设置getter setter
     * 需要在创建地方以外的地方调用选项组，建议用getGroup方法并作检验
     */
    public static class SettingGroupEntry extends SettingEntry{
        public Cons<Table> builder = null;

        public SettingGroupEntry(String name, String help) {
            super(name, help);
        }

        @Override
        public void build(Table table){
            if(builder == null || table == null) return;
            builder.get(table);
        }
    }

    public static class CollapseGroupEntry extends SettingGroupEntry{
        public Boolp collapsep = () -> false;
        public Cons<Table> headBuilder = null;

        public CollapseGroupEntry(String name, String help) {
            super(name, help);
        }

        @Override
        public void build(Table table) {
            if(headBuilder == null || builder == null || table == null) return;
            table.margin(2f);
            table.table(t -> {
                t.setBackground(Mindow2.gray2);
                t.margin(2f,4f,2f,4f);
                headBuilder.get(t);
            }).growX();
            table.row();
            table.collapser(t -> {
                t.margin(5f);
                builder.get(t);
                t.image(Tex.whiteui).growY().width(10f).color(Color.grays(0.8f));
            }, collapsep).growX().get().setCollapsed(true, collapsep).setDuration(0.1f);
        }

        public void setDefaultHeader(String title){
            headBuilder = t -> {
                var b = t.button(title, textbtoggle, null).growX().pad(0f,20f,0f,20f).height(36f).get();
                collapsep = () -> !b.isChecked();
            };
        }
    }
}
