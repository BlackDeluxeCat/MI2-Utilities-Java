package mi2u.io;

import arc.*;
import arc.graphics.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import mi2u.*;
import mi2u.ui.elements.*;
import mindustry.core.*;
import mindustry.gen.*;
import mindustry.ui.*;

import java.util.regex.*;

import static mi2u.MI2Utilities.MOD;
import static mindustry.Vars.*;

public class ModUpdateChecker {
    public static final String GIT_URL = "https://github.com/BlackDeluxeCat/MI2-Utilities-Java";
    public static final String API_URL = "https://api.github.com/repos/BlackDeluxeCat/MI2-Utilities-Java/releases";
    public static final String REPO_NAME = "BlackDeluxeCat/MI2-Utilities-Java";

    // 附件信息类
    public static class ReleaseAsset {
        public String fileName;
        public String modVersion;        // 从文件名提取的mod版本
        public String mindustryVersion;  // 从文件名提取的Mindustry版本
        public String downloadUrl;
        public String releaseName;       // release的tag_name
        public String releaseBody;       // release的描述
        public String releaseUrl;        // 用于调用release下载方法
        public boolean exactMatch;       // 是否与当前版本完全匹配
        public boolean compatible;       // 是否兼容当前Mindustry版本

        public ReleaseAsset(String fileName, String modVersion, String mindustryVersion,
                            String downloadUrl, String releaseName, String releaseBody,
                            boolean exactMatch, boolean compatible) {
            this.fileName = fileName;
            this.modVersion = modVersion;
            this.mindustryVersion = mindustryVersion;
            this.downloadUrl = downloadUrl;
            this.releaseName = releaseName;
            this.releaseBody = releaseBody;
            this.exactMatch = exactMatch;
            this.compatible = compatible;
        }

        @Override
        public String toString() {
            return formatVersion(modVersion, mindustryVersion) +
                       (exactMatch ? " [已安装]" : "") +
                       (!compatible ? " [版本不兼容]" : "");
        }
    }

    public PopupTable dialog;
    public Table versionListTable;
    public int sign = 0; // 0:检查中, 1:成功, -1:失败
    public final Interval timer = new Interval();
    public float autoCloseDelay = 900f;
    public Seq<ReleaseAsset> allAssets = new Seq<>();

    public void show() {
        dialog = new PopupTable() {
            {
                setBackground(Styles.black5);
                margin(8f);
                setPositionInScreen((Core.graphics.getWidth() - getPrefWidth()) / 2,
                    (Core.graphics.getHeight() - getPrefHeight()) / 2);

                timer.get(1);

                update(() -> {
                    //toFront();
                    if (timer.check(0, autoCloseDelay)) hide();
                });

                hovered(() -> timer.get(1));
                addDragMove();

                // 标题栏
                this.image().color(Color.coral).growX().height(2f).row();
                this.add("@update.title").growX().with(l -> l.setFontScale(1.2f)).row();
                this.image().color(Color.coral).growX().height(2f).row();

                // 已安装信息
                this.table(infoTable -> {
                    infoTable.table(sub -> {
                        sub.add("[gray]已安装版本: []" + formatVersion(MOD.meta.version, MOD.meta.minGameVersion)).left();
                    }).padLeft(10f);
                }).growX().padTop(5f).row();

                // 查询到的版本列表
                this.table(git -> {
                    git.add(Iconc.github + "可选版本").color(Color.lightGray).left().pad(4f);
                    git.button("" + Iconc.refresh, MI2UVars.textb, ModUpdateChecker.this::fetchReleases)
                        .disabled(b -> sign != -1).size(MI2UVars.buttonSize);
                    git.button(Iconc.link + REPO_NAME,
                            MI2UVars.textb, () -> Core.app.openURI(GIT_URL)).grow()
                        .get().getLabel().setFontScale(0.7f);
                }).growX().padTop(5f).row();

                this.pane(scroll -> {
                    versionListTable = scroll;
                    scroll.defaults().growX();
                    scroll.add("@update.checking").color(Color.gray).left();
                }).minWidth(400f).maxHeight(300f).row();

                // 底部按钮
                this.table(bottomTable -> {
                    bottomTable.defaults().growX().height(50f);

                    bottomTable.button("", MI2UVars.textb, this::hide)
                        .update(b -> b.setText(Core.bundle.get("update.close") + " (" +
                                                   Strings.fixed((autoCloseDelay - timer.getTime(0)) / 60, 1) + "s)"));
                }).growX().row();

                // 初始获取数据
                fetchReleases();
            }
        };

        dialog.popup();
    }

    private void buildVersionList(Table table) {
        table.clear();

        // 分组：精确匹配、兼容版本、不兼容版本
        Seq<ReleaseAsset> exactMatches = allAssets.select(a -> a.exactMatch);
        Seq<ReleaseAsset> compatibleVersions = allAssets.select(a -> a.compatible && !a.exactMatch);
        Seq<ReleaseAsset> incompatibleVersions = allAssets.select(a -> !a.compatible && !a.exactMatch);

        // 显示精确匹配
        if (!exactMatches.isEmpty()) {
            for (ReleaseAsset asset : exactMatches) {
                addVersionButton(table, asset, Color.green, "已安装");
            }
        }

        // 显示兼容版本
        if (!compatibleVersions.isEmpty()) {
            for (ReleaseAsset asset : compatibleVersions) {
                addVersionButton(table, asset, Color.white, "可能兼容");
            }
        }

        // 显示不兼容版本
        if (!incompatibleVersions.isEmpty()) {
            for (ReleaseAsset asset : incompatibleVersions) {
                addVersionButton(table, asset, Color.gray, "版本不兼容");
            }
        }
    }

    private void addVersionButton(Table table, ReleaseAsset asset, Color color, String status) {
        table.button(b -> {
                b.table(inner -> {
                    inner.add(asset.modVersion + " (Mindustry " + asset.mindustryVersion + ")")
                        .color(color).left().growX();
                    inner.add("[" + status + "]").color(color.cpy().mul(0.8f)).right().padLeft(10f);
                }).growX();
            }, Styles.flatBordert, () -> showDownloadConfirm(asset))
            .growX().height(40f).padBottom(2f).disabled(b -> !asset.compatible).row();
    }

    private void showDownloadConfirm(ReleaseAsset asset) {
        ui.showConfirm("@confirm",
            "确定要下载版本 " + asset.modVersion + " 吗？\n" +
                "Mindustry版本要求: " + asset.mindustryVersion + "\n\n" +
                (asset.releaseBody.length() > 200 ? asset.releaseBody.substring(0, 200) + "..." : asset.releaseBody),
            () -> {
                // 调用mod安装方法
                ui.mods.githubImportMod(REPO_NAME, true, asset.releaseUrl.substring(asset.releaseUrl.lastIndexOf("/") + 1));
                dialog.hide();
            });
    }

    private void fetchReleases() {
        sign = 0;
        allAssets.clear();

        Http.get(API_URL, res -> {
            sign = 1;
            timer.get(1);
            autoCloseDelay = 1200f;

            try {
                var json = new JsonReader().parse(res.getResultAsString());
                parseReleases(json);

                // 在主线程中更新UI
                Core.app.post(() -> {
                    buildVersionList(versionListTable);
                    dialog.keepInScreen();
                });
            } catch (Exception e) {
                Log.err("解析GitHub响应失败", e);
                sign = -1;
                Core.app.post(() -> {
                    if (versionListTable != null) {
                        versionListTable.clear();
                        versionListTable.add("@update.failCheck").color(Color.red).left();
                    }
                });
            }
        }, e -> {
            sign = -1;
            timer.get(1);
            autoCloseDelay = 600f;
            Log.err("获取release失败", e);
            Core.app.post(() -> {
                if (versionListTable != null) {
                    versionListTable.clear();
                    versionListTable.add("@update.failCheck").color(Color.red).left();
                }
            });
        });
    }

    private void parseReleases(JsonValue releasesJson) {
        String currentModVersion = MOD.meta.version;

        for (JsonValue release : releasesJson) {
            String releaseName = release.getString("tag_name", "");
            String releaseBody = release.getString("body", "");
            String url = release.getString("url", "");
            JsonValue assets = release.get("assets");

            if (assets != null) {
                for (JsonValue asset : assets) {
                    ReleaseAsset parsed = parseAsset(asset, releaseName, releaseBody, currentModVersion);
                    if (parsed != null) {
                        parsed.releaseUrl = url;
                        allAssets.add(parsed);
                    }
                }
            }
        }

        // 按版本号降序排序
        allAssets.sort((a, b) -> compareModVersions(b.modVersion, a.modVersion));
    }

    private ReleaseAsset parseAsset(JsonValue assetJson, String releaseName, String releaseBody, String currentModVersion) {
        String fileName = assetJson.getString("name", "");

        // 解析文件名格式: mi2-utilities-java.1.10.0-154.2.jar
        Pattern pattern = Pattern.compile("^(.+?)\\.([\\w.-]+)-([\\w.-]+)\\.jar$");
        Matcher matcher = pattern.matcher(fileName);

        if (matcher.matches()) {
            String repoName = matcher.group(1);
            String modVersion = matcher.group(2);
            String mindustryVersion = matcher.group(3);

            boolean exactMatch = modVersion.equals(currentModVersion);
            boolean compatible = isCompatibleMindustryVersion(mindustryVersion);

            return new ReleaseAsset(
                fileName,
                modVersion,
                mindustryVersion,
                assetJson.getString("browser_download_url", ""),
                releaseName,
                releaseBody,
                exactMatch,
                compatible
            );
        } else {
            Log.warn("MI2U Update Check", "发布中存在非标准命名格式的模组文件: " + fileName);
        }
        return null;
    }

    private boolean isCompatibleMindustryVersion(String requiredVersion) {
        try {
            // 使用Mindustry的Version.isAtLeast方法检查兼容性
            // 这里简化处理：如果当前版本 >= 要求版本，则认为兼容
            return Version.isAtLeast(requiredVersion);
        } catch (Exception e) {
            Log.err("检查Mindustry版本兼容性失败", e);
            return false;
        }
    }

    // 简单的版本号比较
    private int compareModVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int num1 = i < parts1.length ? Strings.parseInt(parts1[i]) : 0;
            int num2 = i < parts2.length ? Strings.parseInt(parts2[i]) : 0;

            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }
        return 0;
    }

    public static String formatVersion(String modVersion, String minGameVersion) {
        return (modVersion + " (Mindustry " + minGameVersion + ")");
    }
}