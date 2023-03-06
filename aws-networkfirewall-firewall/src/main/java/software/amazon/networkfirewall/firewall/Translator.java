package software.amazon.networkfirewall.firewall;

import lombok.NonNull;
import software.amazon.awssdk.services.networkfirewall.model.AssociateFirewallPolicyRequest;
import software.amazon.awssdk.services.networkfirewall.model.AssociateSubnetsRequest;
import software.amazon.awssdk.services.networkfirewall.model.CreateFirewallRequest;
import software.amazon.awssdk.services.networkfirewall.model.DeleteFirewallRequest;
import software.amazon.awssdk.services.networkfirewall.model.DescribeFirewallRequest;
import software.amazon.awssdk.services.networkfirewall.model.DescribeFirewallResponse;
import software.amazon.awssdk.services.networkfirewall.model.DisassociateSubnetsRequest;
import software.amazon.awssdk.services.networkfirewall.model.ListFirewallsRequest;
import software.amazon.awssdk.services.networkfirewall.model.ListFirewallsResponse;
import software.amazon.awssdk.services.networkfirewall.model.SyncState;
import software.amazon.awssdk.services.networkfirewall.model.TagResourceRequest;
import software.amazon.awssdk.services.networkfirewall.model.UntagResourceRequest;
import software.amazon.awssdk.services.networkfirewall.model.UpdateFirewallDeleteProtectionRequest;
import software.amazon.awssdk.services.networkfirewall.model.UpdateFirewallDescriptionRequest;
import software.amazon.awssdk.services.networkfirewall.model.UpdateFirewallPolicyChangeProtectionRequest;
import software.amazon.awssdk.services.networkfirewall.model.UpdateSubnetChangeProtectionRequest;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Translator {

    static CreateFirewallRequest translateToCreateRequest(final ResourceModel model, final Map<String, String> tags) {
        return CreateFirewallRequest.builder()
                .firewallName(model.getFirewallName())
                .firewallPolicyArn(model.getFirewallPolicyArn())
                .vpcId(model.getVpcId())
                .subnetMappings(translateSubnetMappingsFromSdk(model.getSubnetMappings()))
                .deleteProtection(model.getDeleteProtection())
                .subnetChangeProtection(model.getSubnetChangeProtection())
                .firewallPolicyChangeProtection(model.getFirewallPolicyChangeProtection())
                .description(model.getDescription())
                .tags(translateTagsToNetworkFirewallTags(tags))
                .build();
    }

    static ResourceModel translateFromDescribeFirewallResponse(final DescribeFirewallResponse response) {
        return ResourceModel.builder()
                .firewallId(response.firewall().firewallId())
                .firewallArn(response.firewall().firewallArn())
                .firewallName(response.firewall().firewallName())
                .firewallPolicyArn(response.firewall().firewallPolicyArn())
                .vpcId(response.firewall().vpcId())
                .subnetMappings(translateSubnetMappingsToSdk(response.firewall().subnetMappings()))
                .deleteProtection(response.firewall().deleteProtection())
                .subnetChangeProtection(response.firewall().subnetChangeProtection())
                .firewallPolicyChangeProtection(response.firewall().firewallPolicyChangeProtection())
                .description(response.firewall().description())
                .tags(translateTagsToSdk(response.firewall().tags()))
                .endpointIds(getAllEndpointIds(response.firewallStatus().syncStates()))
                .build();
    }

    static List<String> getAllEndpointIds(Map<String, SyncState> syncStates) {
        if (syncStates == null) {
            throw new CfnGeneralServiceException("Sync state is not found for this firewall. Firewall creation" +
                    " is not yet complete. Please check the NetworkFirewall AWS console" +
                    " or use CLI to fetch the latest status of this firewall.");
        }

        List<String> endpointIds = new ArrayList<>();
        for (final Map.Entry<String, SyncState> s : syncStates.entrySet()) {
            final String azName = s.getKey();
            final SyncState state = s.getValue();

            // add AZName in the suffix of the endpointID.
            endpointIds.add(String.format("%s:%s", azName, state.attachment().endpointId()));
        }
        Collections.sort(endpointIds);
        return endpointIds;
    }

    static Set<software.amazon.awssdk.services.networkfirewall.model.Tag> translateTagsToNetworkFirewallTags(final Map<String, String> stackTags) {
        if (stackTags == null || stackTags.size() == 0) {
            return null;
        }
        return stackTags.entrySet().stream().map(tag -> software.amazon.awssdk.services.networkfirewall.model.Tag.builder()
                    .key(tag.getKey())
                    .value(tag.getValue())
                    .build())
                .collect(Collectors.toSet());
    }

    static Set<Tag> translateTagsToSdk(final Collection<software.amazon.awssdk.services.networkfirewall.model.Tag> tags) {
        return Optional.ofNullable(tags).orElse(Collections.emptySet())
                .stream()
                .map(tag -> Tag.builder().key(tag.key()).value(tag.value()).build())
                .filter(tag -> !tag.getKey().startsWith("aws:tag")) // filter out system tags in response to match request
                .collect(Collectors.toSet());
    }

    static Set<software.amazon.awssdk.services.networkfirewall.model.SubnetMapping> translateSubnetMappingsFromSdk(
            final Collection<SubnetMapping> subnetMappings) {
        return Optional.ofNullable(subnetMappings).orElse(Collections.emptySet())
                .stream()
                .map(subnetMap -> software.amazon.awssdk.services.networkfirewall.model.SubnetMapping.builder()
                        .subnetId(subnetMap.getSubnetId())
                        .build())
                .collect(Collectors.toSet());
    }

    static Set<software.amazon.awssdk.services.networkfirewall.model.SubnetMapping> translateSubnetMappingsFromSdk(
            final Set<String> subnetMappings) {
        return Optional.ofNullable(subnetMappings).orElse(Collections.emptySet())
                .stream()
                .map(subnetId -> software.amazon.awssdk.services.networkfirewall.model.SubnetMapping.builder()
                        .subnetId(subnetId)
                        .build())
                .collect(Collectors.toSet());
    }

    static DescribeFirewallRequest translateToDescribeFirewallRequest(final ResourceModel model) {
        return DescribeFirewallRequest.builder()
                .firewallArn(model.getFirewallArn())
                .firewallName(model.getFirewallName())
                .build();
    }

    static Set<SubnetMapping> translateSubnetMappingsToSdk(
            final List<software.amazon.awssdk.services.networkfirewall.model.SubnetMapping> subnets) {
        if (subnets == null) {
            return null;
        }
        Set<SubnetMapping> sdkSubnets = new HashSet<>();
        for (final software.amazon.awssdk.services.networkfirewall.model.SubnetMapping s : subnets) {
            sdkSubnets.add(SubnetMapping.builder().subnetId(s.subnetId()).build());
        }

        return sdkSubnets;
    }

    static DeleteFirewallRequest translateToDeleteRequest(final ResourceModel model) {
        return DeleteFirewallRequest.builder()
                .firewallName(model.getFirewallName())
                .firewallArn(model.getFirewallArn())
                .build();
    }

    static ListFirewallsRequest translateToListRequest(final String nextToken) {
        return ListFirewallsRequest.builder()
                .nextToken(nextToken)
                .build();
    }

    static List<ResourceModel> translateFromListRequest(final ListFirewallsResponse response) {
        return streamOfOrEmpty(response.firewalls())
                .map(resource -> ResourceModel.builder()
                        .firewallArn(resource.firewallArn())
                        .build())
                .collect(Collectors.toList());
    }

    private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
        return Optional.ofNullable(collection)
                .map(Collection::stream)
                .orElseGet(Stream::empty);
    }

    // following are translate functions for all firewall update operations

    static AssociateSubnetsRequest translateToAssociateSubnets(final ResourceModel model, final Set<String> subnetsToAssociate) {
        return AssociateSubnetsRequest.builder()
                .firewallArn(model.getFirewallArn())
                .firewallName(model.getFirewallName())
                .subnetMappings(translateSubnetMappingsFromSdk(subnetsToAssociate))
                .build();
    }

    static DisassociateSubnetsRequest translateToDisassociateSubnets(final ResourceModel model, final Set<String> subnetsToDisassociate) {
        return DisassociateSubnetsRequest.builder()
                .firewallArn(model.getFirewallArn())
                .firewallName(model.getFirewallName())
                .subnetIds(subnetsToDisassociate)
                .build();
    }

    static UpdateFirewallDescriptionRequest translateToUpdateFirewallDescription(final ResourceModel model) {
        return UpdateFirewallDescriptionRequest.builder()
                .firewallArn(model.getFirewallArn())
                .firewallName(model.getFirewallName())
                .description(model.getDescription())
                .build();
    }

    static UpdateFirewallDeleteProtectionRequest translateToUpdateFirewallDeleteProtection(final ResourceModel model) {
        return UpdateFirewallDeleteProtectionRequest.builder()
                .firewallArn(model.getFirewallArn())
                .firewallName(model.getFirewallName())
                .deleteProtection(model.getDeleteProtection())
                .build();
    }

    static UpdateSubnetChangeProtectionRequest translateToUpdateSubnetChangeProtection(final ResourceModel model) {
        return UpdateSubnetChangeProtectionRequest.builder()
                .firewallArn(model.getFirewallArn())
                .firewallName(model.getFirewallName())
                .subnetChangeProtection(model.getSubnetChangeProtection())
                .build();
    }

    static UpdateFirewallPolicyChangeProtectionRequest translateToUpdateFirewallPolicyChangeProtection(final ResourceModel model) {
        return UpdateFirewallPolicyChangeProtectionRequest.builder()
                .firewallArn(model.getFirewallArn())
                .firewallName(model.getFirewallName())
                .firewallPolicyChangeProtection(model.getFirewallPolicyChangeProtection())
                .build();
    }

    static AssociateFirewallPolicyRequest translateToAssociateFirewallPolicy(final ResourceModel model) {
        return AssociateFirewallPolicyRequest.builder()
                .firewallArn(model.getFirewallArn())
                .firewallName(model.getFirewallName())
                .firewallPolicyArn(model.getFirewallPolicyArn())
                .build();
    }

    static TagResourceRequest translateToTagRequest(final ResourceModel model, @NonNull Map<String, String> tags) {
        return TagResourceRequest.builder()
                .resourceArn(model.getFirewallArn())
                .tags(translateTagsToNetworkFirewallTags(tags))
                .build();
    }

    static UntagResourceRequest translateToUntagRequest(final ResourceModel model, @NonNull Set<String> tagsToUntag) {
        return UntagResourceRequest.builder()
                .resourceArn(model.getFirewallArn())
                .tagKeys(tagsToUntag)
                .build();
    }
}
