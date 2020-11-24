package software.amazon.networkfirewall.firewall;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerUtilsTest {

    final UpdateHandler handler = new UpdateHandler();

    @Test
    public void computeSubnetsTest() {
        final UpdateHandler handler = new UpdateHandler();

        // only 1 add subnet
        Set<SubnetMapping> previous = subnetMappingsBuilder(ImmutableSet.of("a"));
        Set<SubnetMapping> desired = subnetMappingsBuilder(ImmutableSet.of("a", "b"));
        assertThat(handler.computeSubnetsToAdd(previous, desired)).isEqualTo(ImmutableSet.of("b"));
        assertThat(handler.computeSubnetsToRemove(previous, desired)).isEqualTo(ImmutableSet.of());

        // 1 add subnet and 1 remove subnet
        previous = subnetMappingsBuilder(ImmutableSet.of("a"));
        desired = subnetMappingsBuilder(ImmutableSet.of("b"));
        assertThat(handler.computeSubnetsToAdd(previous, desired)).isEqualTo(ImmutableSet.of("b"));
        assertThat(handler.computeSubnetsToRemove(previous, desired)).isEqualTo(ImmutableSet.of("a"));

        // no change
        previous = subnetMappingsBuilder(ImmutableSet.of("a"));
        desired = subnetMappingsBuilder(ImmutableSet.of("a"));
        assertThat(handler.computeSubnetsToAdd(previous, desired)).isEqualTo(ImmutableSet.of());
        assertThat(handler.computeSubnetsToRemove(previous, desired)).isEqualTo(ImmutableSet.of());

        // 1 add subnet and 0 remove subnet
        previous = subnetMappingsBuilder(ImmutableSet.of("a", "b", "c"));
        desired = subnetMappingsBuilder(ImmutableSet.of("a", "b", "c", "d"));
        assertThat(handler.computeSubnetsToAdd(previous, desired)).isEqualTo(ImmutableSet.of("d"));
        assertThat(handler.computeSubnetsToRemove(previous, desired)).isEqualTo(ImmutableSet.of());

        // 1 add subnet and 3 remove subnet
        previous = subnetMappingsBuilder(ImmutableSet.of("a", "b", "c"));
        desired = subnetMappingsBuilder(ImmutableSet.of("d"));
        assertThat(handler.computeSubnetsToAdd(previous, desired)).isEqualTo(ImmutableSet.of("d"));
        assertThat(handler.computeSubnetsToRemove(previous, desired)).isEqualTo(ImmutableSet.of("a", "b", "c"));

        // 0 add subnet and 3 remove subnet
        previous = subnetMappingsBuilder(ImmutableSet.of("a", "b", "c", "d"));
        desired = subnetMappingsBuilder(ImmutableSet.of("a"));
        assertThat(handler.computeSubnetsToAdd(previous, desired)).isEqualTo(ImmutableSet.of());
        assertThat(handler.computeSubnetsToRemove(previous, desired)).isEqualTo(ImmutableSet.of("b", "c", "d"));
    }

    private Set<SubnetMapping> subnetMappingsBuilder(Set<String> subnets) {
        Set<SubnetMapping> subnetMappings = new HashSet<>();
        for (final String s : subnets) {
            subnetMappings.add(SubnetMapping.builder().subnetId(s).build());
        }
        return subnetMappings;
    }

}
