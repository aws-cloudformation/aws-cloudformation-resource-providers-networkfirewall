package software.amazon.networkfirewall.rulegroup;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TagUtils {
    private Set<Tag> desiredFirewallRequestTags;
    private Set<Tag> previousFirewallRequestTags;
    private Map<String, String> desiredStackTags;
    private Map<String, String> previousStackTags;

    private final MapDifference<String, String> tagsDiff;

    public TagUtils(final Set<Tag> previousFirewallRequestTags, final Set<Tag> desiredFirewallRequestTags,
            Map<String, String> previousStackTags, final Map<String, String> desiredStackTags) {
        this.desiredFirewallRequestTags = desiredFirewallRequestTags;
        this.previousFirewallRequestTags = previousFirewallRequestTags;
        this.desiredStackTags = desiredStackTags;
        this.previousStackTags = previousStackTags;

        this.tagsDiff = computeTagsDiff();
    }

    // tags to add contains both new tags and existing tag with new value
    public Map<String, String> tagsToAddOrUpdate() {
        Map<String, String> tagsToAdd = new HashMap<>(tagsDiff.entriesOnlyOnRight());
        // get the tags for those value has changed
        tagsDiff.entriesDiffering().forEach((k, v) -> tagsToAdd.put(k, v.rightValue()));

        return tagsToAdd;
    }

    public Map<String, String> tagsToRemove() {
        return tagsDiff.entriesOnlyOnLeft();
    }

    private MapDifference<String, String> computeTagsDiff() {
        // desired resource request tags and stack tags.
        Map<String, String> desiredTags = convertTags(desiredStackTags);
        desiredTags.putAll(convertTags(desiredFirewallRequestTags));

        // previous resource request tags and stack tags.
        Map<String, String> previousTags = convertTags(previousStackTags);
        previousTags.putAll(convertTags(previousFirewallRequestTags));

        return Maps.difference(previousTags, desiredTags);
    }

    private Map<String, String> convertTags(final Set<Tag> tags) {
        Map<String, String> convertedTags = new HashMap<>();
        if (tags != null) {
            tags.stream().forEach(t -> convertedTags.put(t.getKey(), t.getValue()));
        }
        return convertedTags;
    }

    // return empty hashMap if null or return a shallow copy of provided map
    private Map<String, String> convertTags(final Map<String, String> tags) {
        return tags == null ? new HashMap<>() : new HashMap<>(tags);
    }
}
