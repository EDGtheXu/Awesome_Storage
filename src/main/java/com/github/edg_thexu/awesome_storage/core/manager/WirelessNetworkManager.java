package com.github.edg_thexu.awesome_storage.core.manager;

import com.github.edg_thexu.awesome_storage.core.block.MagicStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class WirelessNetworkManager {

    private static final WirelessNetworkManager INSTANCE = new WirelessNetworkManager();

    public static WirelessNetworkManager getInstance() {
        return INSTANCE;
    }

    private record Node(BlockPos pos, ResourceKey<Level> dimension, int range) {}

    // dimension -> frequency -> list of nodes
    private final Map<ResourceKey<Level>, Map<Integer, List<Node>>> network = new ConcurrentHashMap<>();
    // pos -> frequency (for quick unregister)
    private final Map<BlockPos, Integer> posToFreq = new ConcurrentHashMap<>();

    private WirelessNetworkManager() {}

    public void register(BlockPos pos, ResourceKey<Level> dimension, int frequency, int range) {
        if (frequency == 0) return;
        unregister(pos);
        Map<Integer, List<Node>> byFreq = network.computeIfAbsent(dimension, k -> new ConcurrentHashMap<>());
        byFreq.computeIfAbsent(frequency, k -> Collections.synchronizedList(new ArrayList<>())).add(new Node(pos, dimension, range));
        posToFreq.put(pos, frequency);
    }

    public void unregister(BlockPos pos) {
        Integer freq = posToFreq.remove(pos);
        if (freq == null) return;
        for (var entry : network.entrySet()) {
            Map<Integer, List<Node>> byFreq = entry.getValue();
            List<Node> nodes = byFreq.get(freq);
            if (nodes != null) {
                nodes.removeIf(n -> n.pos().equals(pos));
                if (nodes.isEmpty()) byFreq.remove(freq);
            }
        }
    }

    public List<BlockPos> findConnectedCores(BlockPos pos, ResourceKey<Level> dimension, int frequency, int range) {
        if (frequency == 0) return List.of();
        Map<Integer, List<Node>> byFreq = network.get(dimension);
        if (byFreq == null) return List.of();
        List<Node> nodes = byFreq.get(frequency);
        if (nodes == null) return List.of();
        return nodes.stream()
                .filter(n -> !n.pos().equals(pos))
                .filter(n -> n.pos().distManhattan(pos) <= range)
                .map(Node::pos)
                .collect(Collectors.toList());
    }

    public int getFrequency(BlockPos pos) {
        return posToFreq.getOrDefault(pos, 0);
    }

    public void clear() {
        network.clear();
        posToFreq.clear();
    }
}
