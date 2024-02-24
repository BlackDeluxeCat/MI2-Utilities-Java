package mi2u.io;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.math.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.SettingsMenuDialog.*;

import static arc.Core.bundle;
import static arc.Core.settings;
import static mi2u.MI2UVars.funcSetTextb;
import static mi2u.MI2UVars.textbtoggle;

public class SettingHandler{
    public String prefix;
    public Seq<Setting> list = new Seq<>();

    public SettingHandler(String prefix){
        this.prefix = prefix;
    }

    public void add(Setting setting){
        list.add(setting);
    }

    public void rebuild(Table table){
        table.clearChildren();
        list.each(setting -> {
            table.table(setting::add);
            table.table(t -> {
                if(setting.description != null) t.button(Icon.infoSmall, () -> {}).size(32f);
                if(setting.restart) t.add("RS").color(Color.orange);
                t.row();
                if(setting.reloadWorld) t.add("RW").color(Color.cyan);
                if(setting.performance) t.add("PF").color(Color.scarlet);
            }).row();
        });
    }

    public Setting getSetting(String name){
        return list.find(s -> s.name.equals(prefix + "." + name));
    }

    public boolean getBool(String name){
        return settings.getBool(prefix + "." + name);
    }

    public int getInt(String name){
        return settings.getInt(prefix + "." + name);
    }

    public int getInt(String name, int def){
        return settings.getInt(prefix + "." + name, def);
    }

    public float getFloat(String name){
        return settings.getFloat(prefix + "." + name);
    }

    public String getStr(String name){
        return settings.getString(prefix + "." + name);
    }

    public Object get(String name, Object def){
        return settings.get(prefix + "." + name, def);
    }

    public void putInt(String name, int v){
        settings.put(prefix + "." + name, v);
    }

    public void putString(String name, String v){
        settings.put(prefix + "." + name, v);
    }

    public void putBool(String name, boolean v){
        settings.put(prefix + "." + name, v);
    }

    public Title title(String pureName){
        var s = new Title(pureName);
        list.add(s);
        return s;
    }

    public CheckSetting checkPref(String name, boolean def){
        return checkPref(name, def, null);
    }

    public CheckSetting checkPref(String name, boolean def, Boolc changed){
        var s = new CheckSetting(name, def, changed);
        list.add(s);
        settings.defaults(name, def);
        return s;
    }

    public SliderSetting sliderPref(String name, int def, int min, int max, StringProcessor s){
        return sliderPref(name, def, min, max, 1, s);
    }

    public SliderSetting sliderPref(String name, int def, int min, int max, int step, StringProcessor s){
        return sliderPref(name, def, min, max, step, s, null);

    }

    public SliderSetting sliderPref(String name, int def, int min, int max, int step, StringProcessor s, Intc changed){
        SliderSetting res;
        list.add(res = new SliderSetting(name, def, min, max, step, s, changed));
        settings.defaults(name, def);
        return res;
    }

    public TextFieldSetting textPref(String name, String def){
        return textPref(name, def, null);
    }

    public TextFieldSetting textPref(String name, String def, Cons<String> changed){
        return textPref(name, def, null, null, changed);
    }

    public TextFieldSetting textPref(String name, String def, TextField.TextFieldFilter filter, TextField.TextFieldValidator validator, Cons<String> changed){
        var s = new TextFieldSetting(name, def, filter, validator, changed);
        list.add(s);
        settings.defaults(name, def);
        return s;
    }

    public abstract class Setting{
        /** 调用设置项的字符串，接上prefix */
        public String name;
        /** 从bundle读标题的字符串 */
        public String title;
        /** 从bundle读描述的字符串 */
        public @Nullable String description;
        public boolean restart, reloadWorld, performance;
        public Setting(){}

        public Setting(String name){
            this.name = prefix + "." + name;
            title = bundle.get("settings." + this.name, name);
            description = bundle.getOrNull("settings." + this.name + ".description");
        }

        public Setting tag(boolean restart, boolean reloadWorld, boolean performance){
            this.restart = restart;
            this.reloadWorld = reloadWorld;
            this.performance = performance;
            return this;
        }

        public abstract void add(Table table);
    }

    public class Title extends Setting{
        public Title(String name){
            this.name = name;
            title = bundle.get("settings." + this.name, name);
            description = bundle.getOrNull("settings." + this.name + ".description");
        }

        @Override
        public void add(Table table){
            table.add(name).growX().color(Pal.accent).style(Styles.outlineLabel).row();
            table.image().growX().height(2f).color(Pal.accent);
        }
    }

    public class CheckSetting extends Setting{
        boolean def;
        Boolc changed;
        public CheckSetting(String name, boolean def, Boolc changed){
            super(name);
            this.def = def;
            this.changed = changed;
        }

        @Override
        public void add(Table table){
            CheckBox box = new CheckBox(title);

            box.update(() -> box.setChecked(settings.getBool(name)));

            box.changed(() -> {
                settings.put(name, box.isChecked());
                if(changed != null){
                    changed.get(box.isChecked());
                }
            });

            box.left();

            table.add(box).left().padTop(3f);
        }

        public TextButton miniButton(){
            var b = new TextButton(title);
            b.setStyle(textbtoggle);
            funcSetTextb.get(b);
            b.clicked(() -> {
                boolean v = settings.getBool(name);
                settings.put(name, !v);
                if(changed != null) changed.get(!v);
            });
            b.update(() -> b.setChecked(settings.getBool(name)));
            return b;
        }
    }

    public class SliderSetting extends Setting{
        int def, min, max, step;
        StringProcessor sp;
        Intc changed;

        public SliderSetting(String name, int def, int min, int max, int step, StringProcessor s, Intc changed){
            super(name);
            this.def = def;
            this.min = min;
            this.max = max;
            this.step = step;
            this.sp = s;
            this.changed = changed;
        }

        @Override
        public void add(Table table){
            Slider slider = new Slider(min, max, step, false);

            slider.setValue(settings.getInt(name, def));

            Label value = new Label("", Styles.outlineLabel);
            Table content = new Table();
            content.add(title, Styles.outlineLabel).left().growX().wrap();
            content.add(value).padLeft(10f).right();
            content.margin(3f, 33f, 3f, 33f);
            content.touchable = Touchable.disabled;

            slider.changed(() -> {
                settings.put(name, (int)slider.getValue());
                if(changed != null) changed.get((int)slider.getValue());
                value.setText(sp.get((int)slider.getValue()));
            });

            slider.change();

            table.stack(slider, content).width(Math.min(Core.graphics.getWidth() / 1.2f, 460f)).left().padTop(4f);
        }
    }

    public class ChooseSetting extends Setting{
        String def;
        String[] values;
        Func<String, String> textf;
        Cons<String> changed;

        public ChooseSetting(String name, String def, String[] values, Func<String, String> buttonTextFunc, Cons<String> changed){
            super(name);
            this.def = def;
            this.values = values;
            this.textf = buttonTextFunc == null ? s -> s : buttonTextFunc;
            this.changed = changed;
        }

        @Override
        public void add(Table table){
            if(values != null){
                ButtonGroup<Button> group = new ButtonGroup<>();

                table.table(t -> {
                    t.defaults().uniform().growX();
                    int i = 0;
                    for(var item : values){
                        t.button(textf.get(item), textbtoggle, () -> {
                            group.uncheckAll();
                            group.setChecked(textf.get(item));
                            settings.put(name, item);
                            if(changed != null) changed.get(item);
                        }).with(funcSetTextb).with(group::add);

                        if(Mathf.mod(++i, 4) == 0){
                            t.row();
                            i = 0;
                        }
                    }
                }).growX().left().maxWidth(Math.min(Core.graphics.getWidth() / 1.2f, 460f));

                group.setChecked(textf.get(settings.getString(name, def)));

                table.add(title).row();
            }
        }
    }

    public class TextFieldSetting extends Setting{
        String def;
        Cons<String> changed;
        @Nullable TextField.TextFieldFilter filter;
        @Nullable TextField.TextFieldValidator validator;

        public TextFieldSetting(String name, String def, TextField.TextFieldFilter filter, TextField.TextFieldValidator validator, Cons<String> changed){
            super(name);
            this.def = def;
            this.filter = filter;
            this.validator = validator;
            this.changed = changed;
        }

        @Override
        public void add(Table table){
            table.field(settings.getString(name, def), Styles.nodeField, str -> {
                if(validator != null && !validator.valid(str)) return;
                settings.put(name, str);
                if(changed != null) changed.get(str);
            }).with(tf -> {
                tf.setValidator(validator);
                tf.setFilter(filter);
            }).width(150f).right().update(tf -> {
                if(!tf.hasKeyboard()) tf.setText(settings.getString(name, def));
            });

            table.add(title).growX();
        }
    }
}
