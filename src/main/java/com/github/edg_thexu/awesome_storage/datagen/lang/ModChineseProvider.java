package com.github.edg_thexu.awesome_storage.datagen.lang;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

import static com.github.edg_thexu.awesome_storage.AwesomeStorage.MODID;
import static com.github.edg_thexu.awesome_storage.AwesomeStorage.chineseProviders;

public class ModChineseProvider extends LanguageProvider {
    public ModChineseProvider(PackOutput output) {
        super(output, MODID, "zh_cn");
    }

    @Override
    protected void addTranslations() {
        add("container.awesome_storage.magic_storage", "魔法存储");
        add("container.awesome_storage.storage_array", "存储阵列");

        add("creativetab.awesome_storage.cards", "魔法存储");

        add("awesome_storage.missing_ingredient", "缺少材料");
        add("awesome_storage.can_craft", "点击制作");
        add("awesome_storage.magic_storage_screen.storage", "存储");
        add("awesome_storage.magic_storage_screen.craft", "合成");

        add("awesome_storage.message.no_component", "该物品没有组件: ");
        add("awesome_storage.message.too_far", "你离目标太远了!");

        add("awesome_storage.tooltip.controller_range", "通信距离: ");
        add("awesome_storage.tooltip.distance", "距离: ");
        add("awesome_storage.tooltip.block_pos", "位置: ");
        add("awesome_storage.tooltip.error_level", "无信号");

        add("awesome_storage.deposit_btn.tooltip", "存入全部 - 左键: 全部存入, Ctrl+左键: 快速堆叠, 右键: 补货");

        add("awesome_storage.magic_storage_screen.search", "搜索...");
        add("awesome_storage.magic_storage_screen.store_all", "全部存入");
        add("awesome_storage.magic_storage_screen.capacity_format", "容量: %s/%s");
        add("awesome_storage.magic_storage_screen.craftable", "可合成");
        add("awesome_storage.magic_storage_screen.all", "全部");
        add("awesome_storage.magic_storage_screen.favorites_only", "仅收藏");
        add("awesome_storage.auto_stock_btn.tooltip", "自动补货 - 对物品右键设定库存目标");
        add("awesome_storage.magic_storage_screen.max", "最大");
        add("awesome_storage.magic_storage_screen.reset", "重置");
        add("awesome_storage.magic_storage_screen.select_item", "选择一个物品");
        add("awesome_storage.magic_storage_screen.output", "输出");
        add("awesome_storage.magic_storage_screen.ingredients", "材料");
        add("awesome_storage.magic_storage_screen.stations", "工作站");
        add("awesome_storage.magic_storage_screen.in_storage", "存储中");
        add("awesome_storage.magic_storage_screen.controller", "控制器");
        add("awesome_storage.magic_storage_screen.rename", "重命名");
        add("awesome_storage.magic_storage_screen.default", "默认");
        add("awesome_storage.magic_storage_screen.stackable", "可堆叠");
        add("awesome_storage.magic_storage_screen.non_stackable", "不可堆叠");
        add("awesome_storage.magic_storage_screen.all_mods", "所有模组");
        add("awesome_storage.magic_storage_screen.display", "显示");
        add("awesome_storage.magic_storage_screen.filter", "筛选栏");
        add("awesome_storage.magic_storage_screen.leftmenu", "侧边栏");

        add("awesome_storage.magic_storage_screen.queue", "排队合成");
        add("awesome_storage.magic_storage_screen.queue_title", "合成队列");
        add("awesome_storage.magic_storage_screen.queue_idle", "空闲");
        add("awesome_storage.magic_storage_screen.queue_pending", "待合成");
        add("awesome_storage.magic_storage_screen.queue_empty", "队列为空");
        add("awesome_storage.magic_storage_screen.queue_status", "队列槽数: %s");
        add("awesome_storage.magic_storage_screen.queue_clear_all", "清空全部");
        add("awesome_storage.magic_storage_screen.queue_pause_all", "全部暂停");
        add("awesome_storage.magic_storage_screen.queue_resume_all", "全部继续");

        add("awesome_storage.magic_storage_screen.stats_title", "统计");
        add("awesome_storage.magic_storage_screen.stats_overview", "概览");
        add("awesome_storage.magic_storage_screen.stats_unique_types", "物品种类:");
        add("awesome_storage.magic_storage_screen.stats_total_count", "物品总数:");
        add("awesome_storage.magic_storage_screen.stats_top_items", "热门物品");
        add("awesome_storage.magic_storage_screen.stats_empty", "暂无存储物品");
        add("awesome_storage.magic_storage_screen.stats_mods", "模组分布");

        add("awesome_storage.magic_storage_screen.upgrade_title", "升级");
        add("awesome_storage.magic_storage_screen.upgrade_slot", "无线网卡");
        add("awesome_storage.magic_storage_screen.upgrade_range", "范围: %s");
        add("awesome_storage.magic_storage_screen.upgrade_frequency", "频率");
        add("awesome_storage.magic_storage_screen.upgrade_freq_hint", "输入频率...");
        add("awesome_storage.magic_storage_screen.upgrade_set", "设置");
        add("awesome_storage.magic_storage_screen.upgrade_connected", "已连接的核心");
        add("awesome_storage.magic_storage_screen.upgrade_no_cores", "未找到核心");

        add("awesome_storage.tooltip.network_range", "范围: %s");
        add("awesome_storage.magic_storage_screen.craft_info_title", "配方信息");
        add("awesome_storage.magic_storage_screen.craft_info_empty", "未配置合成配方");
        add("awesome_storage.magic_storage_screen.craft_info_unsupported", "不支持的配方类型");
        add("awesome_storage.tooltip.queue_upgrade", "增加 %s 个队列槽");

        add("awesome_storage.magic_storage_screen.queue_upgrade_title", "队列升级");
        add("awesome_storage.magic_storage_screen.queue_upgrade_total", "总队列槽数: %s");

        add("config.jade.plugin_awesome_storage.magic_block", "魔法存储");
        chineseProviders.forEach(a->a.accept(this));
    }

}