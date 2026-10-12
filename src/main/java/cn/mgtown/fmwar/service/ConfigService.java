package cn.mgtown.fmwar.service;

import cn.mgtown.fmwar.config.ActionBlockedConfig;
import cn.mgtown.fmwar.config.ConfigManager;
import cn.mgtown.fmwar.config.Settings;

/**
 * 运行期配置访问入口。
 *
 * <p>{@link #settings()} 每次都读取 {@link ConfigManager} 的 volatile 快照，
 * 因此 /fmwar reload 之后所有服务立刻看到新配置，无需重启或重新注册监听器。</p>
 */
public final class ConfigService {

    private final ConfigManager configManager;

    public ConfigService(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public Settings settings() {
        return configManager.settings();
    }

    /**
     * 行为限制区域（独立配置 action-blocked.yml）。
     *
     * <p>与 {@link #settings()} 一样是 volatile 快照，reload 后立即生效。</p>
     */
    public ActionBlockedConfig actionBlocked() {
        return configManager.actionBlocked();
    }

    public ConfigManager manager() {
        return configManager;
    }

    public boolean reload() {
        return configManager.load();
    }
}
