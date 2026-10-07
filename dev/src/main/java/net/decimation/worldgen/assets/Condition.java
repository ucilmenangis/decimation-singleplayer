package net.decimation.worldgen.assets;

import java.util.HashSet;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * A "when" test on an asset (furniture sets now, parts and loot later):
 * {"kinds": ["apartment", "office", "shop"], "storey": "ground" | "upper",
 * "floors": [min, max]}. Every given test must pass; a missing test passes.
 */
public final class Condition
{
    public static final Condition ALWAYS = new Condition(null, null, 0, Integer.MAX_VALUE);

    private final Set<String> kinds;
    private final String storey;
    private final int minFloors, maxFloors;

    private Condition(Set<String> kinds, String storey, int minFloors, int maxFloors)
    {
        this.kinds = kinds;
        this.storey = storey;
        this.minFloors = minFloors;
        this.maxFloors = maxFloors;
    }

    public static Condition parse(JsonObject o)
    {
        if (o == null)
        {
            return ALWAYS;
        }
        Set<String> kinds = null;
        if (o.has("kinds"))
        {
            kinds = new HashSet<String>();
            for (JsonElement e : o.getAsJsonArray("kinds"))
            {
                kinds.add(e.getAsString());
            }
        }
        String storey = o.has("storey") ? o.get("storey").getAsString() : null;
        int min = 0, max = Integer.MAX_VALUE;
        if (o.has("floors"))
        {
            min = o.getAsJsonArray("floors").get(0).getAsInt();
            max = o.getAsJsonArray("floors").get(1).getAsInt();
        }
        return new Condition(kinds, storey, min, max);
    }

    /** kind: building kind name ("apartment", "office", "shop"); ground: ground storey plan. */
    public boolean test(String kind, boolean ground, int floors)
    {
        if (kinds != null && !kinds.contains(kind))
        {
            return false;
        }
        if (storey != null && !storey.equals(ground ? "ground" : "upper"))
        {
            return false;
        }
        return floors >= minFloors && floors <= maxFloors;
    }
}
