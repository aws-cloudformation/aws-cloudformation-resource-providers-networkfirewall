package software.amazon.networkfirewall.rulegroup;

import com.amazonaws.util.CollectionUtils;
import lombok.NonNull;
import org.apache.commons.collections.MapUtils;
import software.amazon.awssdk.services.networkfirewall.model.CreateRuleGroupRequest;
import software.amazon.awssdk.services.networkfirewall.model.DeleteRuleGroupRequest;
import software.amazon.awssdk.services.networkfirewall.model.DescribeRuleGroupRequest;
import software.amazon.awssdk.services.networkfirewall.model.DescribeRuleGroupResponse;
import software.amazon.awssdk.services.networkfirewall.model.ListRuleGroupsRequest;
import software.amazon.awssdk.services.networkfirewall.model.ListRuleGroupsResponse;
import software.amazon.awssdk.services.networkfirewall.model.RuleGroupResponse;
import software.amazon.awssdk.services.networkfirewall.model.TagResourceRequest;
import software.amazon.awssdk.services.networkfirewall.model.UntagResourceRequest;
import software.amazon.awssdk.services.networkfirewall.model.UpdateRuleGroupRequest;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is a centralized placeholder for
 * - api request construction
 * - object translation to/from aws sdk
 * - resource model construction for read/list handlers
 */

public class Translator {

    /**
     * Request to create a resource
     *
     * @param model resource model
     * @return CreateRuleGroupRequest the aws service request to create a resource
     */
    static CreateRuleGroupRequest translateToCreateRequest(final ResourceModel model, final Map<String, String> tags) {
        return CreateRuleGroupRequest.builder()
                .ruleGroupName(model.getRuleGroupName())
                .type(model.getType())
                .capacity(model.getCapacity())
                .description(model.getDescription())
                .tags(translateTagsToSdk(tags))
                .ruleGroup(translateRuleGroupToSdk(model.getRuleGroup()))
                .build();
    }

    /**
     * Request to read a resource
     *
     * @param model resource model
     * @return DescribeRuleGroupRequest the aws service request to describe a resource
     */
    static DescribeRuleGroupRequest translateToReadRequest(final ResourceModel model) {
        return DescribeRuleGroupRequest.builder()
                .ruleGroupArn(model.getRuleGroupArn())
                .ruleGroupName(model.getRuleGroupName())
                .type(model.getType())
                .build();
    }

    /**
     * Request to delete a resource
     *
     * @param model resource model
     * @return DeleteRuleGroupRequest the aws service request to delete a resource
     */
    static DeleteRuleGroupRequest translateToDeleteRequest(final ResourceModel model) {
        return DeleteRuleGroupRequest.builder()
                .ruleGroupArn(model.getRuleGroupArn())
                .ruleGroupName(model.getRuleGroupName())
                .type(model.getType())
                .build();
    }

    /**
     * Request to update a resource
     *
     * @param model       resource model
     * @param updateToken token on which the update operation is conditional on
     * @return UpdateRuleGroupRequest the aws service request to update a resource
     */
    static UpdateRuleGroupRequest translateToUpdateRequest(final ResourceModel model, final String updateToken) {
        return UpdateRuleGroupRequest.builder()
                .ruleGroupArn(model.getRuleGroupArn())
                .ruleGroupName(model.getRuleGroupName())
                .type(model.getType())
                .description(model.getDescription())
                .ruleGroup(translateRuleGroupToSdk(model.getRuleGroup()))
                .updateToken(updateToken)
                .build();
    }

    /**
     * Request to list resources
     *
     * @param nextToken token passed to the aws service list resources request
     * @return ListRuleGroupsRequest the aws service request to list resources within aws account
     */
    static ListRuleGroupsRequest translateToListRequest(final String nextToken) {
        return ListRuleGroupsRequest.builder()
                .nextToken(nextToken)
                .build();
    }

    /**
     * Translates resource object from sdk into a resource model
     *
     * @return model resource model
     */
    static ResourceModel translateFromReadResponse(final DescribeRuleGroupResponse response) {
        final RuleGroupResponse ruleGroupResponse = response.ruleGroupResponse();
        return ResourceModel.builder()
                .capacity(ruleGroupResponse.capacity())
                .description(ruleGroupResponse.description())
                .ruleGroupArn(ruleGroupResponse.ruleGroupArn())
                .ruleGroupId(ruleGroupResponse.ruleGroupId())
                .ruleGroupName(ruleGroupResponse.ruleGroupName())
                .type(ruleGroupResponse.typeAsString())
                .tags(translateTagsFromSdk(ruleGroupResponse.tags()))
                .ruleGroup(translateRuleGroupFromSdk(response.ruleGroup()))
                .build();
    }

    /**
     * Request to tag resource
     *
     * @param model resource model
     * @param tags Tags to be associated with the resource
     * @return TagResourceRequest the aws service request to tag a resource
     */
    static TagResourceRequest translateToTagRequest(final ResourceModel model, @NonNull Map<String, String> tags) {
        return TagResourceRequest.builder()
                .resourceArn(model.getRuleGroupArn())
                .tags(translateTagsToSdk(tags))
                .build();
    }

    /**
     * Request to untag resource
     *
     * @param model resource model
     * @param tagsToUntag Tags to be disassociated from the resource
     * @return UntagResourceRequest the aws service request to untag a resource
     */
    static UntagResourceRequest translateToUntagRequest(final ResourceModel model, @NonNull Set<String> tagsToUntag) {
        return  UntagResourceRequest.builder()
                .resourceArn(model.getRuleGroupArn())
                .tagKeys(tagsToUntag)
                .build();

    }

    static Set<software.amazon.awssdk.services.networkfirewall.model.Tag> translateTagsToSdk(final Map<String, String> stackTags) {
        if (stackTags == null || stackTags.size() == 0) {
            return null;
        }
        return stackTags.entrySet().stream().map(tag -> software.amazon.awssdk.services.networkfirewall.model.Tag.builder()
                .key(tag.getKey())
                .value(tag.getValue())
                .build())
                .collect(Collectors.toSet());
    }

    static software.amazon.awssdk.services.networkfirewall.model.RuleGroup translateRuleGroupToSdk(final RuleGroup ruleGroup) {
        if (ruleGroup == null) {
            return null;
        }
        return software.amazon.awssdk.services.networkfirewall.model.RuleGroup.builder()
                .rulesSource(translateRuleSourceToSdk(ruleGroup.getRulesSource()))
                .ruleVariables(translateRuleVariablesToSdk(ruleGroup.getRuleVariables()))
                .build();
    }

    static software.amazon.awssdk.services.networkfirewall.model.RulesSource translateRuleSourceToSdk(final RulesSource rulesSource) {
        if (rulesSource == null) {
            return null;
        }
        return software.amazon.awssdk.services.networkfirewall.model.RulesSource.builder()
                .rulesSourceList(translateRuleSourceListToSdk(rulesSource.getRulesSourceList()))
                .rulesString(rulesSource.getRulesString())
                .statefulRules(translateStatefulRulesToSdk(rulesSource.getStatefulRules()))
                .statelessRulesAndCustomActions(translateStatelessRulesAndCustomActionsToSdk(rulesSource.getStatelessRulesAndCustomActions()))
                .build();
    }

    static software.amazon.awssdk.services.networkfirewall.model.RulesSourceList translateRuleSourceListToSdk(final RulesSourceList rulesSourceList) {
        if (rulesSourceList == null) {
            return null;
        }
        return software.amazon.awssdk.services.networkfirewall.model.RulesSourceList.builder()
                .generatedRulesType(rulesSourceList.getGeneratedRulesType())
                .targetTypes(translateTargetTypesToSdk(rulesSourceList.getTargetTypes()))
                .targets(rulesSourceList.getTargets())
                .build();
    }

    static Set<software.amazon.awssdk.services.networkfirewall.model.TargetType> translateTargetTypesToSdk(final Collection<String> targetTypes) {
        if (targetTypes == null) {
            return null;
        }
        return Optional.of(targetTypes).orElse(Collections.emptySet())
                .stream()
                .map(targetType -> software.amazon.awssdk.services.networkfirewall.model.TargetType.fromValue(targetType))
                .collect(Collectors.toSet());
    }

    static Set<software.amazon.awssdk.services.networkfirewall.model.StatefulRule> translateStatefulRulesToSdk(final Collection<StatefulRule> statefulRules) {
        if (statefulRules == null) {
            return null;
        }
        return Optional.of(statefulRules).orElse(Collections.emptySet())
                .stream()
                .map(statefulRule -> translateStatefulRuleToSdk(statefulRule))
                .collect(Collectors.toSet());
    }

    static software.amazon.awssdk.services.networkfirewall.model.StatefulRule translateStatefulRuleToSdk(final StatefulRule statefulRule) {
        if (statefulRule == null) {
            return null;
        }
        return software.amazon.awssdk.services.networkfirewall.model.StatefulRule.builder()
                .action(statefulRule.getAction())
                .header(translateHeaderToSdk(statefulRule.getHeader()))
                .ruleOptions(translateRuleOptionsToSdk(statefulRule.getRuleOptions()))
                .build();
    }

    static software.amazon.awssdk.services.networkfirewall.model.Header translateHeaderToSdk(final Header header) {
        if (header == null) {
            return null;
        }
        return software.amazon.awssdk.services.networkfirewall.model.Header.builder()
                .source(header.getSource())
                .destination(header.getDestination())
                .destinationPort(header.getDestinationPort())
                .sourcePort(header.getSourcePort())
                .direction(header.getDirection())
                .protocol(header.getProtocol())
                .build();
    }

    static Set<software.amazon.awssdk.services.networkfirewall.model.RuleOption> translateRuleOptionsToSdk(final Collection<RuleOption> ruleOptions) {
        if (ruleOptions == null) {
            return null;
        }
        return Optional.of(ruleOptions).orElse(Collections.emptySet())
                .stream()
                .map(ruleOption -> translateRuleOptionToSdk(ruleOption))
                .collect(Collectors.toSet());
    }

    static software.amazon.awssdk.services.networkfirewall.model.RuleOption translateRuleOptionToSdk(final RuleOption ruleOption) {
        if (ruleOption == null) {
            return null;
        }
        return software.amazon.awssdk.services.networkfirewall.model.RuleOption.builder()
                .keyword(ruleOption.getKeyword())
                .settings(ruleOption.getSettings())
                .build();
    }

    static software.amazon.awssdk.services.networkfirewall.model.RuleVariables translateRuleVariablesToSdk(final RuleVariables ruleVariables) {
        if (ruleVariables == null) {
            return null;
        }
        return software.amazon.awssdk.services.networkfirewall.model.RuleVariables.builder()
                .ipSets(translateIpSetsToSdk(ruleVariables.getIPSets()))
                .portSets(translatePortSetsToSdk(ruleVariables.getPortSets()))
                .build();
    }

    static Map<String, software.amazon.awssdk.services.networkfirewall.model.IPSet> translateIpSetsToSdk(final Map<String, IPSet> ipSets) {
        if (ipSets == null) {
            return null;
        }
        return Optional.of(ipSets).orElse(Collections.emptyMap()).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> translateIpSetToSdk(entry.getValue())));
    }

    static software.amazon.awssdk.services.networkfirewall.model.IPSet translateIpSetToSdk(final IPSet ipSet) {
        if (ipSet == null) {
            return null;
        }
        return software.amazon.awssdk.services.networkfirewall.model.IPSet.builder()
                .definition(ipSet.getDefinition())
                .build();
    }

    static Map<String, software.amazon.awssdk.services.networkfirewall.model.PortSet> translatePortSetsToSdk(final Map<String, PortSet> portSets) {
        if (portSets == null) {
            return null;
        }
        return Optional.of(portSets).orElse(Collections.emptyMap()).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> translatePortSetToSdk(entry.getValue())));
    }

    static software.amazon.awssdk.services.networkfirewall.model.PortSet translatePortSetToSdk(final PortSet portSet) {
        if (portSet == null) {
            return null;
        }
        return software.amazon.awssdk.services.networkfirewall.model.PortSet.builder()
                .definition(portSet.getDefinition())
                .build();
    }

    static software.amazon.awssdk.services.networkfirewall.model.StatelessRulesAndCustomActions translateStatelessRulesAndCustomActionsToSdk(final StatelessRulesAndCustomActions statelessRulesAndCustomActions) {
        if (statelessRulesAndCustomActions == null) {
            return null;
        }
        return software.amazon.awssdk.services.networkfirewall.model.StatelessRulesAndCustomActions.builder()
                .statelessRules(translateStatelessRulesToSdk(statelessRulesAndCustomActions.getStatelessRules()))
                .customActions(translateCustomActionsToSdk(statelessRulesAndCustomActions.getCustomActions()))
                .build();
    }

    static Set<software.amazon.awssdk.services.networkfirewall.model.StatelessRule> translateStatelessRulesToSdk(final Collection<StatelessRule> statelessRules) {
        if (statelessRules == null) {
            return null;
        }
        return Optional.of(statelessRules).orElse(Collections.emptySet())
                .stream()
                .map(statelessRule -> translateStatelessRuleToSdk(statelessRule))
                .collect(Collectors.toSet());
    }

    static software.amazon.awssdk.services.networkfirewall.model.StatelessRule translateStatelessRuleToSdk(final StatelessRule statelessRule) {
        if (statelessRule == null) {
            return null;
        }
        return software.amazon.awssdk.services.networkfirewall.model.StatelessRule.builder()
                .ruleDefinition(translateRuleDefinitionToSdk(statelessRule.getRuleDefinition()))
                .priority(statelessRule.getPriority())
                .build();
    }

    static software.amazon.awssdk.services.networkfirewall.model.RuleDefinition translateRuleDefinitionToSdk(final RuleDefinition ruleDefinition) {
        if (ruleDefinition == null) {
            return null;
        }
        return software.amazon.awssdk.services.networkfirewall.model.RuleDefinition.builder()
                .actions(translateActionsToSdk(ruleDefinition.getActions()))
                .matchAttributes(translateMatchAttributesToSdk(ruleDefinition.getMatchAttributes()))
                .build();
    }

    static Set<String> translateActionsToSdk(final Collection<String> actions) {
        if (actions == null) {
            return null;
        }
        return Optional.of(actions).orElse(Collections.emptySet())
                .stream()
                .collect(Collectors.toSet());
    }

    static software.amazon.awssdk.services.networkfirewall.model.MatchAttributes translateMatchAttributesToSdk(final MatchAttributes matchAttributes) {
        if (matchAttributes == null) {
            return null;
        }
        return software.amazon.awssdk.services.networkfirewall.model.MatchAttributes.builder()
                .sources(translateSourcesToSdk(matchAttributes.getSources()))
                .destinations(translateDestinationsToSdk(matchAttributes.getDestinations()))
                .sourcePorts(translateSourcePortsToSdk(matchAttributes.getSourcePorts()))
                .destinationPorts(translateDestinationPortsToSdk(matchAttributes.getDestinationPorts()))
                .protocols(matchAttributes.getProtocols())
                .tcpFlags(translateTCPFlagFieldsToSdk(matchAttributes.getTCPFlags()))
                .build();
    }

    static Set<software.amazon.awssdk.services.networkfirewall.model.Address> translateSourcesToSdk(final Collection<Address> sources) {
        if (sources == null) {
            return null;
        }
        return Optional.of(sources).orElse(Collections.emptySet())
                .stream()
                .map(source -> translateAddressToSdk(source))
                .collect(Collectors.toSet());
    }

    static Set<software.amazon.awssdk.services.networkfirewall.model.Address> translateDestinationsToSdk(final Collection<Address> destinations) {
        if (destinations == null) {
            return null;
        }
        return Optional.of(destinations).orElse(Collections.emptySet())
                .stream()
                .map(destination -> translateAddressToSdk(destination))
                .collect(Collectors.toSet());
    }

    static Set<software.amazon.awssdk.services.networkfirewall.model.PortRange> translateSourcePortsToSdk(final Collection<PortRange> sourcePorts) {
        if (sourcePorts == null) {
            return null;
        }
        return Optional.of(sourcePorts).orElse(Collections.emptySet())
                .stream()
                .map(sourcePort -> translatePortRangeToSdk(sourcePort))
                .collect(Collectors.toSet());
    }

    static Set<software.amazon.awssdk.services.networkfirewall.model.PortRange> translateDestinationPortsToSdk(final Collection<PortRange> destinationPorts) {
        if (destinationPorts == null) {
            return null;
        }
        return Optional.of(destinationPorts).orElse(Collections.emptySet())
                .stream()
                .map(destinationPort -> translatePortRangeToSdk(destinationPort))
                .collect(Collectors.toSet());
    }

    static Set<software.amazon.awssdk.services.networkfirewall.model.TCPFlagField> translateTCPFlagFieldsToSdk(final Collection<TCPFlagField> tcpFlagFields) {
        if (tcpFlagFields == null) {
            return null;
        }
        return Optional.of(tcpFlagFields).orElse(Collections.emptySet())
                .stream()
                .map(tcpFlagField -> translateTCPFlagFieldToSdk(tcpFlagField))
                .collect(Collectors.toSet());
    }

    static software.amazon.awssdk.services.networkfirewall.model.TCPFlagField translateTCPFlagFieldToSdk(final TCPFlagField tcpFlagField) {
        if (tcpFlagField == null) {
            return null;
        }
        return software.amazon.awssdk.services.networkfirewall.model.TCPFlagField.builder()
                .flags(translateTCPFlagsToSdk(tcpFlagField.getFlags()))
                .masks(translateTCPFlagsToSdk(tcpFlagField.getMasks()))
                .build();
    }

    static Set<software.amazon.awssdk.services.networkfirewall.model.TCPFlag> translateTCPFlagsToSdk(final Collection<String> tcpFlags) {
        if (tcpFlags == null) {
            return null;
        }
        return Optional.of(tcpFlags).orElse(Collections.emptySet())
                .stream()
                .map(tcpFlag -> software.amazon.awssdk.services.networkfirewall.model.TCPFlag.fromValue(tcpFlag))
                .collect(Collectors.toSet());
    }

    static software.amazon.awssdk.services.networkfirewall.model.Address translateAddressToSdk(final Address address) {
        if (address == null) {
            return null;
        }
        return software.amazon.awssdk.services.networkfirewall.model.Address.builder()
                .addressDefinition(address.getAddressDefinition())
                .build();
    }

    static software.amazon.awssdk.services.networkfirewall.model.PortRange translatePortRangeToSdk(final PortRange portRange) {
        if (portRange == null) {
            return null;
        }
        return software.amazon.awssdk.services.networkfirewall.model.PortRange.builder()
                .fromPort(portRange.getFromPort())
                .toPort(portRange.getToPort())
                .build();
    }

    static Set<software.amazon.awssdk.services.networkfirewall.model.CustomAction> translateCustomActionsToSdk(final Collection<CustomAction> customActions) {
        if (customActions == null) {
            return null;
        }
        return Optional.of(customActions).orElse(Collections.emptySet())
                .stream()
                .map(customAction -> translateCustomActionToSdk(customAction))
                .collect(Collectors.toSet());
    }

    static software.amazon.awssdk.services.networkfirewall.model.CustomAction translateCustomActionToSdk(final CustomAction customAction) {
        if (customAction == null) {
            return null;
        }
        return software.amazon.awssdk.services.networkfirewall.model.CustomAction.builder()
                .actionName(customAction.getActionName())
                .actionDefinition(translateActionDefinitionToSdk(customAction.getActionDefinition()))
                .build();
    }

    static software.amazon.awssdk.services.networkfirewall.model.ActionDefinition translateActionDefinitionToSdk(final ActionDefinition actionDefinition) {
        if (actionDefinition == null) {
            return null;
        }
        return software.amazon.awssdk.services.networkfirewall.model.ActionDefinition.builder()
                .publishMetricAction(translatePublishMetricActionToSdk(actionDefinition.getPublishMetricAction()))
                .build();
    }

    static software.amazon.awssdk.services.networkfirewall.model.PublishMetricAction translatePublishMetricActionToSdk(final PublishMetricAction publishMetricAction) {
        if (publishMetricAction == null) {
            return null;
        }
        return software.amazon.awssdk.services.networkfirewall.model.PublishMetricAction.builder()
                .dimensions(translateDimensionsToSdk(publishMetricAction.getDimensions()))
                .build();
    }

    static Set<software.amazon.awssdk.services.networkfirewall.model.Dimension> translateDimensionsToSdk(final Collection<Dimension> dimensions) {
        if (dimensions == null) {
            return null;
        }
        return Optional.of(dimensions).orElse(Collections.emptySet())
                .stream()
                .map(dimension -> translateDimensionToSdk(dimension))
                .collect(Collectors.toSet());
    }

    static software.amazon.awssdk.services.networkfirewall.model.Dimension translateDimensionToSdk(final Dimension dimension) {
        if (dimension == null) {
            return null;
        }
        return software.amazon.awssdk.services.networkfirewall.model.Dimension.builder()
                .value(dimension.getValue())
                .build();
    }

    static Set<Tag> translateTagsFromSdk(final Collection<software.amazon.awssdk.services.networkfirewall.model.Tag> tags) {
        // tags is an exception for the above case, since empty tags also will be displayed as emptySet to the customer
        return Optional.ofNullable(tags).orElse(Collections.emptySet())
                .stream()
                .filter(tag -> !tag.key().startsWith("aws:tag"))
                .map(tag -> Tag.builder().key(tag.key()).value(tag.value()).build())
                .collect(Collectors.toSet());
    }

    static RuleGroup translateRuleGroupFromSdk(final software.amazon.awssdk.services.networkfirewall.model.RuleGroup ruleGroup) {
        if (ruleGroup == null) {
            return null;
        }
        return RuleGroup.builder()
                .rulesSource(translateRuleSourceFromSdk(ruleGroup.rulesSource()))
                .ruleVariables(translateRuleVariablesFromSdk(ruleGroup.ruleVariables()))
                .build();
    }

    static RulesSource translateRuleSourceFromSdk(final software.amazon.awssdk.services.networkfirewall.model.RulesSource rulesSource) {
        return RulesSource.builder()
                .rulesSourceList(translateRuleSourceListFromSdk(rulesSource.rulesSourceList()))
                .rulesString(rulesSource.rulesString())
                .statefulRules(translateStatefulRulesFromSdk(rulesSource.statefulRules()))
                .statelessRulesAndCustomActions(translateStatelessRulesAndCustomActionsFromSdk(rulesSource.statelessRulesAndCustomActions()))
                .build();
    }

    static RulesSourceList translateRuleSourceListFromSdk(final software.amazon.awssdk.services.networkfirewall.model.RulesSourceList rulesSourceList) {
        if (rulesSourceList == null) {
            return null;
        }
        return RulesSourceList.builder()
                .generatedRulesType(rulesSourceList.generatedRulesTypeAsString())
                .targets(convertToSetFromSdk(rulesSourceList.targets()))
                .targetTypes(convertToSetFromSdk(rulesSourceList.targetTypesAsStrings()))
                .build();
    }

    static Set<StatefulRule> translateStatefulRulesFromSdk(final Collection<software.amazon.awssdk.services.networkfirewall.model.StatefulRule> statefulRules) {
        if (CollectionUtils.isNullOrEmpty(statefulRules)) {
            return null;
        }
        return Optional.of(statefulRules).orElse(Collections.emptySet())
                .stream()
                .map(statefulRule -> translateStatefulRuleFromSdk(statefulRule))
                .collect(Collectors.toSet());
    }

    static StatefulRule translateStatefulRuleFromSdk(final software.amazon.awssdk.services.networkfirewall.model.StatefulRule statefulRule) {
        if (statefulRule == null) {
            return null;
        }
        return StatefulRule.builder()
                .action(statefulRule.actionAsString())
                .header(translateHeaderFromSdk(statefulRule.header()))
                .ruleOptions(translateRuleOptionsFromSdk(statefulRule.ruleOptions()))
                .build();
    }

    static Header translateHeaderFromSdk(final software.amazon.awssdk.services.networkfirewall.model.Header header) {
        if (header == null) {
            return null;
        }
        return Header.builder()
                .destination(header.destination())
                .destinationPort(header.destinationPort())
                .direction(header.directionAsString())
                .protocol(header.protocolAsString())
                .source(header.source())
                .sourcePort(header.sourcePort())
                .build();
    }

    static Set<RuleOption> translateRuleOptionsFromSdk(final Collection<software.amazon.awssdk.services.networkfirewall.model.RuleOption> ruleOptions) {
        if (CollectionUtils.isNullOrEmpty(ruleOptions)) {
            return null;
        }
        return Optional.of(ruleOptions).orElse(Collections.emptySet())
                .stream()
                .map(ruleOption -> translateRuleOptionFromSdk(ruleOption))
                .collect(Collectors.toSet());
    }

    static RuleOption translateRuleOptionFromSdk(final software.amazon.awssdk.services.networkfirewall.model.RuleOption ruleOption) {
        if (ruleOption == null) {
            return null;
        }
        return RuleOption.builder()
                .keyword(ruleOption.keyword())
                .settings(convertToSetFromSdk(ruleOption.settings()))
                .build();
    }

    static StatelessRulesAndCustomActions translateStatelessRulesAndCustomActionsFromSdk(final software.amazon.awssdk.services.networkfirewall.model.StatelessRulesAndCustomActions statelessRulesAndCustomActions) {
        if (statelessRulesAndCustomActions == null) {
            return null;
        }
        return StatelessRulesAndCustomActions.builder()
                .statelessRules(translateStatelessRulesFromSdk(statelessRulesAndCustomActions.statelessRules()))
                .customActions(translateCustomActionsFromSdk(statelessRulesAndCustomActions.customActions()))
                .build();
    }

    static Set<StatelessRule> translateStatelessRulesFromSdk(final Collection<software.amazon.awssdk.services.networkfirewall.model.StatelessRule> statelessRules) {
        if (CollectionUtils.isNullOrEmpty(statelessRules)) {
            return null;
        }
        return Optional.of(statelessRules).orElse(Collections.emptySet())
                .stream()
                .map(statelessRule -> translateStatelessRuleFromSdk(statelessRule))
                .collect(Collectors.toSet());
    }

    static StatelessRule translateStatelessRuleFromSdk(final software.amazon.awssdk.services.networkfirewall.model.StatelessRule statelessRule) {
        if (statelessRule == null) {
            return null;
        }
        return StatelessRule.builder()
                .priority(statelessRule.priority())
                .ruleDefinition(translateRuleDefinitionFromSdk(statelessRule.ruleDefinition()))
                .build();
    }

    static RuleDefinition translateRuleDefinitionFromSdk(final software.amazon.awssdk.services.networkfirewall.model.RuleDefinition ruleDefinition) {
        if (ruleDefinition == null) {
            return null;
        }
        return RuleDefinition.builder()
                .actions(convertToSetFromSdk(ruleDefinition.actions()))
                .matchAttributes(translateMatchAttributesFromSdk(ruleDefinition.matchAttributes()))
                .build();
    }

    static MatchAttributes translateMatchAttributesFromSdk(final software.amazon.awssdk.services.networkfirewall.model.MatchAttributes matchAttributes) {
        if (matchAttributes == null) {
            return null;
        }
        return MatchAttributes.builder()
                .destinationPorts(translateDestinationPortsFromSdk(matchAttributes.destinationPorts()))
                .destinations(translateDestinationsFromSdk(matchAttributes.destinations()))
                .protocols(convertToSetFromSdk(matchAttributes.protocols()))
                .sourcePorts(translateSourcePortsFromSdk(matchAttributes.sourcePorts()))
                .sources(translateSourcesFromSdk(matchAttributes.sources()))
                .tCPFlags(translateTCPFlagFieldsFromSdk(matchAttributes.tcpFlags()))
                .build();
    }

    static Set<PortRange> translateDestinationPortsFromSdk(final Collection<software.amazon.awssdk.services.networkfirewall.model.PortRange> portRanges) {
        if (CollectionUtils.isNullOrEmpty(portRanges)) {
            return null;
        }
        return Optional.of(portRanges).orElse(Collections.emptySet())
                .stream()
                .map(portRange -> translatePortRangeFromSdk(portRange))
                .collect(Collectors.toSet());
    }

    static Set<Address> translateDestinationsFromSdk(final Collection<software.amazon.awssdk.services.networkfirewall.model.Address> destinations) {
        if (CollectionUtils.isNullOrEmpty(destinations)) {
            return null;
        }
        return Optional.of(destinations).orElse(Collections.emptySet())
                .stream()
                .map(destination -> translateAddressFromSdk(destination))
                .collect(Collectors.toSet());
    }

    static Set<PortRange> translateSourcePortsFromSdk(final Collection<software.amazon.awssdk.services.networkfirewall.model.PortRange> portRanges) {
        if (CollectionUtils.isNullOrEmpty(portRanges)) {
            return null;
        }
        return Optional.of(portRanges).orElse(Collections.emptySet())
                .stream()
                .map(portRange -> translatePortRangeFromSdk(portRange))
                .collect(Collectors.toSet());
    }

    static Set<Address> translateSourcesFromSdk(final Collection<software.amazon.awssdk.services.networkfirewall.model.Address> sources) {
        if (CollectionUtils.isNullOrEmpty(sources)) {
            return null;
        }
        return Optional.of(sources).orElse(Collections.emptySet())
                .stream()
                .map(source -> translateAddressFromSdk(source))
                .collect(Collectors.toSet());
    }

    static PortRange translatePortRangeFromSdk(final software.amazon.awssdk.services.networkfirewall.model.PortRange portRange) {
        if (portRange == null) {
            return null;
        }
        return PortRange.builder()
                .fromPort(portRange.fromPort())
                .toPort(portRange.toPort())
                .build();
    }

    static Address translateAddressFromSdk(final software.amazon.awssdk.services.networkfirewall.model.Address address) {
        if (address == null) {
            return null;
        }
        return Address.builder()
                .addressDefinition(address.addressDefinition())
                .build();
    }

    static Set<TCPFlagField> translateTCPFlagFieldsFromSdk(final Collection<software.amazon.awssdk.services.networkfirewall.model.TCPFlagField> tcpFlagFields) {
        if (CollectionUtils.isNullOrEmpty(tcpFlagFields)) {
            return null;
        }
        return Optional.of(tcpFlagFields).orElse(Collections.emptySet())
                .stream()
                .map(tcpFlagField -> translateTCPFlagFieldFromSdk(tcpFlagField))
                .collect(Collectors.toSet());
    }

    static TCPFlagField translateTCPFlagFieldFromSdk(final software.amazon.awssdk.services.networkfirewall.model.TCPFlagField tcpFlagField) {
        if (tcpFlagField == null) {
            return null;
        }
        return TCPFlagField.builder()
                .flags(convertToSetFromSdk(tcpFlagField.flagsAsStrings()))
                .masks(convertToSetFromSdk(tcpFlagField.masksAsStrings()))
                .build();
    }

    static Set<CustomAction> translateCustomActionsFromSdk(final Collection<software.amazon.awssdk.services.networkfirewall.model.CustomAction> customActions) {
        if (CollectionUtils.isNullOrEmpty(customActions)) {
            return null;
        }
        return Optional.of(customActions).orElse(Collections.emptySet())
                .stream()
                .map(customAction -> translateCustomActionFromSdk(customAction))
                .collect(Collectors.toSet());
    }

    static CustomAction translateCustomActionFromSdk(final software.amazon.awssdk.services.networkfirewall.model.CustomAction customAction) {
        if (customAction == null) {
            return null;
        }
        return CustomAction.builder()
                .actionName(customAction.actionName())
                .actionDefinition(translateActionDefinitionFromSdk(customAction.actionDefinition()))
                .build();
    }

    static ActionDefinition translateActionDefinitionFromSdk(final software.amazon.awssdk.services.networkfirewall.model.ActionDefinition actionDefinition) {
        if (actionDefinition == null) {
            return null;
        }
        return ActionDefinition.builder()
                .publishMetricAction(translatePublishMetricActionFromSdk(actionDefinition.publishMetricAction()))
                .build();
    }

    static PublishMetricAction translatePublishMetricActionFromSdk(final software.amazon.awssdk.services.networkfirewall.model.PublishMetricAction publishMetricAction) {
        if (publishMetricAction == null) {
            return null;
        }
        return PublishMetricAction.builder()
                .dimensions(translateDimensionsFromSdk(publishMetricAction.dimensions()))
                .build();
    }

    static Set<Dimension> translateDimensionsFromSdk(final Collection<software.amazon.awssdk.services.networkfirewall.model.Dimension> dimensions) {
        if (CollectionUtils.isNullOrEmpty(dimensions)) {
            return null;
        }
        return Optional.of(dimensions).orElse(Collections.emptySet())
                .stream()
                .map(dimension -> translateDimensionFromSdk(dimension))
                .collect(Collectors.toSet());
    }

    static Dimension translateDimensionFromSdk(final software.amazon.awssdk.services.networkfirewall.model.Dimension dimension) {
        if (dimension == null) {
            return null;
        }
        return Dimension.builder()
                .value(dimension.value())
                .build();
    }

    static RuleVariables translateRuleVariablesFromSdk(final software.amazon.awssdk.services.networkfirewall.model.RuleVariables ruleVariables) {
        if (ruleVariables == null) {
            return null;
        }
        return RuleVariables.builder()
                .iPSets(translateIpSetsFromSdk(ruleVariables.ipSets()))
                .portSets(translatePortSetsFromSdk(ruleVariables.portSets()))
                .build();
    }

    static Map<String, IPSet> translateIpSetsFromSdk(final Map<String, software.amazon.awssdk.services.networkfirewall.model.IPSet> stringIPSetMap) {
        if (MapUtils.isEmpty(stringIPSetMap)) {
            return null;
        }
        return Optional.of(stringIPSetMap).orElse(Collections.emptyMap()).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> translateIpSetFromSdk(entry.getValue())));

    }

    static IPSet translateIpSetFromSdk(final software.amazon.awssdk.services.networkfirewall.model.IPSet value) {
        if (value == null) {
            return null;
        }
        return IPSet.builder()
                .definition(convertToSetFromSdk(value.definition()))
                .build();
    }

    static Map<String, PortSet> translatePortSetsFromSdk(final Map<String, software.amazon.awssdk.services.networkfirewall.model.PortSet> stringPortSetMap) {
        if (MapUtils.isEmpty(stringPortSetMap)) {
            return null;
        }
        return Optional.of(stringPortSetMap).orElse(Collections.emptyMap()).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> translatePortSetFromSdk(entry.getValue())));

    }

    static PortSet translatePortSetFromSdk(final software.amazon.awssdk.services.networkfirewall.model.PortSet value) {
        if (value == null) {
            return null;
        }
        return PortSet.builder()
                .definition(convertToSetFromSdk(value.definition()))
                .build();
    }

    /**
     * Translates resource objects from sdk into a resource model (primary identifier only)
     *
     * @return list of resource models
     */
    static List<ResourceModel> translateFromListRequest(final ListRuleGroupsResponse listRuleGroupsResponse) {
        // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L75-L82
        return streamOfOrEmpty(listRuleGroupsResponse.ruleGroups())
                .map(resource -> ResourceModel.builder()
                        .ruleGroupArn(resource.arn())
                        .build())
                .collect(Collectors.toList());
    }

    private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
        return Optional.ofNullable(collection)
                .map(Collection::stream)
                .orElseGet(Stream::empty);
    }

    private static <T> Set<T> convertToSetFromSdk(final Collection<T> collection) {
        if (CollectionUtils.isNullOrEmpty(collection)) {
            return null;
        }
        return Optional.of(collection).orElse(Collections.emptySet())
                .stream()
                .collect(Collectors.toSet());
    }
}
