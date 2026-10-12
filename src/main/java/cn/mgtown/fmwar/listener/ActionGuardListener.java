package cn.mgtown.fmwar.listener;

import cn.mgtown.fmwar.config.ActionBlockedConfig;
import cn.mgtown.fmwar.service.AlertService;
import cn.mgtown.fmwar.service.ConfigService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import java.util.Map;

/**
 * 行为限制：在配置的限制区域内禁止骑乘（含骑在其他玩家头上）。
 *
 * <p>需求：“在提示接收范围内，玩家不能躺下、坐下、趴下、骑在其他玩家头上，判定到直接取消动作”。</p>
 *
 * <p>实现边界：原版协议没有“坐下/躺下/趴下”姿态，这些动作由坐姿类插件（如 GSit）提供，
 * 其事件不在 paper-api 里，因此本插件只能拦下**原版范围内**可判定的部分：
 * 骑乘生物/载具/其他玩家，以及潜行。坐姿插件的拦截需要按其 API 另接一个监听器。</p>
 *
 * <p>限制区域不再从 config.yml 的 {@code regions.notify} 取，而是来自**独立配置**
 * {@code action-blocked.yml}（见 {@link ActionBlockedConfig}）：那里可以列出任意多片区域，
 * 每片区域可位于不同世界。这样「提示可见范围」与「动作受限范围」两件事就不再彼此绑死——
 * 前者的判定口径是一个坐标盒子，后者才是真正需要按区域维护的运维配置。</p>
 *
 * <p><b>为什么不叠加“本局玩家”判定</b>：这些区域不一定属于本插件的场地（典型如主城），
 * 也不受本插件管理。服主把它写进 {@code action-blocked.yml}，就是声明“这片地方谁都不许骑”，
 * 适用范围由清单本身表达；插件再自作主张只拦本局玩家，反而会让配置与行为对不上。
 * 因此判定只看坐标是否命中，与玩家身份无关。</p>
 */
public final class ActionGuardListener implements Listener {

    private final ConfigService config;
    private final AlertService alerts;

    public ActionGuardListener(ConfigService config, AlertService alerts) {
        this.config = config;
        this.alerts = alerts;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onMount(EntityMountEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!restricted(player)) {
            return;
        }
        event.setCancelled(true);
        alerts.sendActionBarTo(player, "action-blocked", Map.of());
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!event.isSneaking()) {
            return;
        }
        // 默认关闭：原版协议没有“坐下/躺下”姿态，潜行只是最接近的可判定动作。
        // 若服上确实要求连潜行一起禁，把 config.yml 的 start.block-sneak 打开即可。
        if (!config.settings().start().blockSneak()) {
            return;
        }
        if (!restricted(player)) {
            return;
        }
        event.setCancelled(true);
    }

    /**
     * 这名玩家此刻是否该受行为限制——即是否落在 {@code action-blocked.yml} 列出的
     * 任一片区域里（世界名也参与判定，因此限制区可以位于游戏世界之外，例如主城）。
     *
     * <p>不做玩家身份过滤：区域清单来自服主，它声明的是“这片区域内的动作一律取消”，
     * 与这名玩家是否参与本局无关（详见类注释）。config 的 {@code isParticipant}
     * 守卫只用于**本插件自己产生副作用**的那些操作，不适用于这里。</p>
     */
    private boolean restricted(Player player) {
        return config.actionBlocked().contains(player.getLocation());
    }
}
