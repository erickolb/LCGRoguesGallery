package net.chateaulore.roguesgallery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Select membership independently of playback order. Never downloads images. */
final class CollectionSelection {
    static final int DEFAULT_MAXIMUM = 1000;

    static List<Cover> select(List<Cover> collection, Set<String> excluded, int maximum,
                              boolean randomSelection, List<String> previous, boolean keepPrevious,
                              Random random) {
        if (maximum < 1) throw new IllegalArgumentException("Maximum must be positive");
        LinkedHashMap<String, Cover> eligible = new LinkedHashMap<>();
        for (Cover cover : collection) {
            if (!excluded.contains(cover.id)) eligible.putIfAbsent(cover.id, cover);
        }
        Set<String> chosen = new HashSet<>();
        if (keepPrevious) {
            for (String id : previous) {
                if (chosen.size() >= maximum) break;
                if (eligible.containsKey(id)) chosen.add(id);
            }
        }
        List<String> candidates = new ArrayList<>();
        for (String id : eligible.keySet()) if (!chosen.contains(id)) candidates.add(id);
        if (randomSelection) Collections.shuffle(candidates, random);
        for (String id : candidates) {
            if (chosen.size() >= maximum) break;
            chosen.add(id);
        }
        // Canonical LCG order makes sequential display independent of sampling.
        List<Cover> selected = new ArrayList<>();
        for (Cover cover : eligible.values()) if (chosen.contains(cover.id)) selected.add(cover);
        return selected;
    }

    static List<String> ids(List<Cover> covers) {
        List<String> ids = new ArrayList<>();
        for (Cover cover : covers) ids.add(cover.id);
        return ids;
    }

    static List<Cover> displayOrder(List<Cover> selected, boolean randomOrder, Random random) {
        List<Cover> deck = new ArrayList<>(selected);
        if (randomOrder) Collections.shuffle(deck, random);
        return deck;
    }
}
