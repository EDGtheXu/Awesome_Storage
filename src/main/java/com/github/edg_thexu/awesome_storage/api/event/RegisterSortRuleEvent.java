package com.github.edg_thexu.awesome_storage.api.event;

import com.github.edg_thexu.awesome_storage.api.filter.FilterRuleRegistry;
import com.github.edg_thexu.awesome_storage.api.filter.SortRule;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

/**
 * Register for screen item sort rule in client.
 */
public class RegisterSortRuleEvent extends Event implements IModBusEvent {
    public void register(SortRule rule) {
        FilterRuleRegistry.registerSortRule(rule);
    }
}
