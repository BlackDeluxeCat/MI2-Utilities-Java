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
import mi2u.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.SettingsMenuDialog.*;

import static arc.Core.*;
import static mi2u.MI2UVars.*;

public class SettingHandler{
    public static boolean debug;
    public String prefix;
    public Seq<Setting> list = new Seq<>();

    public SettingHandler(String prefix){
        this.prefix = prefix;
    }

    public void add(Setting setting){
        list.add(setting);
    }

    public String prefix(String name){
        return prefix + "." + name;
    }

    public void buildList(Table p){
        p.clearChildren();
        list.each(setting -> p.table(setting::rebuild).growX().margin(2f).row());
    }

    public Setting getSetting(String name){
        return list.find(s -> s.name.equals(prefix(name)));
    }

    public boolean getBool(String name){
        return settings.getBool(prefix(name));
    }

    public int getInt(String name){
        return settings.getInt(prefix(name));
    }

    public int getInt(String name, int def){
        if(!debug){
            return settings.getInt(prefix(name), def);
        }
        return Strings.parseInt(settings.get(prefix(name), def).toString());
    }

    public float getFloat(String name){
        if(!debug){
            return settings.getFloat(prefix(name));
        }
        return Strings.parseFloat(settings.get(prefix(name), "1f").toString());
    }

    public String getStr(String name){
        return settings.getString(prefix(name));
    }

    public Object get(String name, Object def){
        return settings.get(prefix(name), def);
    }

    public void putInt(String name, int v){
        settings.put(prefix(name), v);
    }

    public void putString(String name, String v){
        settings.put(prefix(name), v);
    }

    public void putBool(String name, boolean v){
        settings.put(prefix(name), v);
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
        settings.defaults(prefix(name), def);
        return res;
    }

    public TextFieldSetting textPref(String name, String def){
        return textPref(name, def, null);
    }

    public TextFieldSetting textPref(String name, String def, Cons<String> changed){
        return textPref(name, def, null, null, changed);
    }

    public TextFieldSetting textPref(String name, String def, TextField.TextFieldFilter filter, TextField.TextFieldValidator validator, Cons<String> changed){
        return textPref(name, def, filter, validator, changed, null);
    }

    public TextFieldSetting textPref(String name, String def, TextField.TextFieldFilter filter, TextField.TextFieldValidator validator, Cons<String> changed, Func<String, Object> parser){
        var s = new TextFieldSetting(name, def, filter, validator, changed, parser);
        list.add(s);
        return s;
    }

    public abstract class Setting extends SettingsTable.Setting{
        public boolean restart, reloadWorld, performance;
        public Setting(){
            super("");
        }

        public Setting(String name){
            super("");
            this.name = prefix(name);
            title = bundle.get("settings." + this.name, name);
            description = bundle.getOrNull("settings." + this.name + ".description");
        }

        public Setting tag(boolean restart, boolean reloadWorld, boolean performance){
            this.restart = restart;
            this.reloadWorld = reloadWorld;
            this.performance = performance;
            return this;
        }

        @Override
        public void add(SettingsTable table){
            table.table(this::rebuild).growX().row();
        }

        public void rebuild(Table table){
            table.table(this::build).growX();
            table.table(t -> {
                t.touchable = Touchable.enabled;
                t.fill(Tex.button, i -> i.image(Icon.info).color(description != null ? Color.gray : Color.clear));
                if(performance) t.add("[negstat]PF[]");
                t.row();
                if(restart) t.add("[orange]RS[]");
                if(reloadWorld) t.add("[sky]RW[]");
            }).with(tb -> {
                MI2Utils.tooltip(tb, tooltip ->{
                    tooltip.table(t -> {
                        t.setBackground(Tex.buttonTrans);

                        t.label(() -> "[accent]" + title + "[]").growX().row();
                        t.label(() -> "[lightgray]" + name + "[]").growX().row();
                        t.labelWrap(() -> description).padTop(8f).padBottom(8f).minWidth(300f).growX().row();

                        t.table(warning -> {
                            if (restart) warning.left().label(() -> "[orange]" + bundle.get("settings.tags.restart") + "[]").pad(2).padRight(8f);
                            if (reloadWorld) warning.left().label(() -> "[sky]" + bundle.get("settings.tags.reloadWorld") + "[]").pad(2).padRight(8f);
                            if (performance) warning.left().label(() -> "[negstat]" + bundle.get("settings.tags.performance") + "[]").pad(2).padRight(8f);
                        }).growX().left().bottom();
                    }).growX().maxWidth(500f);
                });
            }).size(40f).pad(2f);
        }

        public abstract void build(Table table);
    }

    public class Title extends Setting{
        public Title(String name){
            this.name = name;
            title = bundle.get("settings." + this.name, name);
            description = bundle.getOrNull("settings." + this.name + ".description");
        }

        @Override
        public void build(Table table){
            table.add(title).growX().color(Pal.accent).style(Styles.outlineLabel).padTop(12f).row();
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
            settings.defaults(this.name, def);
        }

        @Override
        public void build(Table table){
            CheckBox box = new CheckBox(title);

            box.update(() -> box.setChecked(settings.getBool(name)));

            box.changed(() -> {
                settings.put(name, box.isChecked());
                if(changed != null){
                    changed.get(box.isChecked());
                }
            });

            box.left();

            table.add(box).growX().left().padTop(3f);
        }

        public TextButton miniButton(){
            return miniButton(title);
        }

        public TextButton miniButton(String text){
            var b = new TextButton(text);
            b.setStyle(textbtoggle);
            funcSetTextb.get(b);
            b.clicked(() -> {
                boolean v = settings.getBool(name);
                settings.put(name, !v);
                if(changed != null) changed.get(!v);
            });
            MI2Utils.tooltip(b, title);
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
            settings.defaults(this.name, def);
        }

        @Override
        public void build(Table table){
            Slider slider = new Slider(min, max, step, false);

            int i = settings.getInt(name, def);

            slider.setValue(i);

            Label value = new Label(sp.get(i), Styles.outlineLabel);
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

            table.stack(slider, content).growX().padTop(4f);
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
        public void build(Table table){
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
        @Nullable Func<String, Object> parser;

        public TextFieldSetting(String name, String def, TextField.TextFieldFilter filter, TextField.TextFieldValidator validator, Cons<String> changed, Func<String, Object> parser){
            super(name);
            this.def = def;
            this.filter = filter;
            this.validator = validator;
            this.changed = changed;
            this.parser = parser;
            settings.defaults(this.name, parser != null ? parser.get(def) : def);
        }

        @Override
        public void build(Table table){
            table.add(title).growX();

            table.field(String.valueOf(settings.get(name, def)), Styles.nodeField, str -> {
                if(validator != null && !validator.valid(str)) return;
                settings.put(name, parser != null ? parser.get(str) : str);
                if(changed != null) changed.get(str);
            }).with(tf -> {
                tf.setValidator(validator);
                tf.setFilter(filter);
            }).width(150f).right().update(tf -> {
                if(!tf.hasKeyboard()) tf.setText(String.valueOf(settings.get(name, def)));
            });
        }

        public static Func<String, Object> intParser = Strings::parseInt, floatParser = Strings::parseFloat, boolParser = s -> s.equals("true");
    }
}
