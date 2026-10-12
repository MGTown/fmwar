package cn.mgtown.fmwar.config;

import org.bukkit.Location;

import java.util.List;

/**
 * 行为限制区域（独立配置 action-blocked.yml）。
 *
 * <p>为什么单独拆一个文件：这些区域是**纯运维配置**——服主随时可能加一片主城区域、
 * 调一片限制区的边界，而它们与玩法（场地、决斗圈、奖励箱、计时）毫无关系。
 * 塞进 config.yml 会让那份文件越滚越大，改错一处还容易连带影响对局；
 * 拆出来之后，区域的增删可以在一个只有几十行的文件里完成。</p>
 *
 * <p>本配置与 config.yml 的 world **互不影响**：限制区域可以位于任意世界
 * （例如主城所在的 SurvivalWorld）。</p>
 *
 * <p><b>不区分“本局玩家”</b>：这里列出的区域由服主声明为“谁都不许骑乘”的地方，
 * 它们不一定属于本插件的场地，也不受本插件管理——因此判定只看坐标是否命中，
 * 不再叠加 {@code GameEngine#isParticipant}。这是刻意的：区域清单本身就是
 * 服主的显式意图，插件不该替他去缩小适用范围。</p>
 *
 * <p>不可变对象：reload 时整体重建再原子替换，进行中的判定不会被半份配置污染。</p>
 *
 * @param areas 全部限制区域；为空表示不限制任何区域
 */
public record ActionBlockedConfig(List<Area> areas) {

    /** 未配置任何限制区域。 */
    public static final ActionBlockedConfig EMPTY = new ActionBlockedConfig(List.of());

    /**
     * 单个限制区域。
     *
     * @param name   区域名（仅用于日志与自检；未命名时由解析器补 "#序号"）
     * @param region 区域几何范围（含世界名）
     */
    public record Area(String name, Region region) {
    }

    public boolean isEmpty() {
        return areas.isEmpty();
    }

    /** 位置是否落在任一限制区域内（世界名也会参与判定）。 */
    public boolean contains(Location location) {
        return find(location) != null;
    }

    /**
     * 返回命中该位置的区域；未命中返回 {@code null}。
     *
     * <p>返回命中的**区域本身**而不只是布尔值：日志与自检需要报出是哪一片区域生效。</p>
     */
    public Area find(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        return find(location.getWorld().getName(), location.getX(), location.getY(), location.getZ());
    }

    /** 纯几何判定版本（不接触任何 Bukkit 类型，便于自动化断言覆盖）。 */
    public Area find(String worldName, double x, double y, double z) {
        if (worldName == null) {
            return null;
        }
        for (Area area : areas) {
            if (area.region().contains(worldName, x, y, z)) {
                return area;
            }
        }
        return null;
    }

    /** 供日志/自检使用的一行摘要。 */
    public String describe() {
        if (areas.isEmpty()) {
            return "0 个（行为限制整体不生效）";
        }
        StringBuilder builder = new StringBuilder();
        builder.append(areas.size()).append(" 个 [");
        for (int index = 0; index < areas.size(); index++) {
            if (index > 0) {
                builder.append(", ");
            }
            Area area = areas.get(index);
            builder.append(area.name()).append('@').append(area.region().world());
        }
        builder.append(']');
        return builder.toString();
    }
}
