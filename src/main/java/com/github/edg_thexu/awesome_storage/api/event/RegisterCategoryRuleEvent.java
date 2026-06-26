package com.github.edg_thexu.awesome_storage.api.event;

import com.github.edg_thexu.awesome_storage.api.filter.CategoryRule;
import com.github.edg_thexu.awesome_storage.api.filter.FilterRuleRegistry;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

/**
 * Register for screen item filter rule in client.
 */
public class RegisterCategoryRuleEvent extends Event implements IModBusEvent {
    public void register(CategoryRule rule) {
        FilterRuleRegistry.registerCategoryRule(rule);
    }
}
