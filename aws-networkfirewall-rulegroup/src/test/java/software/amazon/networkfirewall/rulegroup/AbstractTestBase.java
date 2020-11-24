package software.amazon.networkfirewall.rulegroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.awssdk.services.networkfirewall.model.CreateRuleGroupRequest;
import software.amazon.awssdk.services.networkfirewall.model.CreateRuleGroupResponse;
import software.amazon.awssdk.services.networkfirewall.model.DescribeRuleGroupRequest;
import software.amazon.awssdk.services.networkfirewall.model.DescribeRuleGroupResponse;
import software.amazon.awssdk.services.networkfirewall.model.TagResourceRequest;
import software.amazon.awssdk.services.networkfirewall.model.TargetType;
import software.amazon.awssdk.services.networkfirewall.model.UntagResourceRequest;
import software.amazon.awssdk.services.networkfirewall.model.UpdateRuleGroupRequest;
import software.amazon.awssdk.services.networkfirewall.model.UpdateRuleGroupResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Credentials;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.ProxyClient;

public class AbstractTestBase {
    protected static final Credentials MOCK_CREDENTIALS;
    protected static final LoggerProxy logger;

    // TestData setup
    protected static final String STATELESS_RULEGROUP_ARN = "arn:aws:network-firewall:us-west-2:777777777777:stateless-rulegroup/StatelessRuleGroup";
    protected static final String STATELESS_RULEGROUP_NAME = "StatelessRuleGroup";
    protected static final String STATELESS_RULEGROUP_ID = "e123f167-e0ce-4a32-8ccc-04437d8d47d9";
    protected static final String STATEFUL_RULEGROUP_ARN = "arn:aws:network-firewall:us-west-2:777777777777:stateful-rulegroup/StatefulRuleGroup";
    protected static final String STATEFUL_RULEGROUP_NAME = "StatefulRuleGroup";
    protected static final String STATEFUL_RULEGROUP_ID = "b123f167-e0ce-4a32-8ccc-04437d8d47d9";
    protected static final String DESCRIPTION = "This is rulegroup resource";
    protected static final String STATELESS_RULEGROUP_TYPE = "STATELESS";
    protected static final String STATEFUL_RULEGROUP_TYPE = "STATEFUL";
    protected static final int CAPACITY = 1000;
    protected static final String NEXT_UPDATE_TOKEN = "b08ff167-e0ce-4a32-8ccc-04437d8d47d9";
    protected static final String UPDATE_TOKEN = "a77ff167-e0ce-4a32-8ccc-04437d8d47d9";
    private static final String CIDR_IPV4_1 = "1.2.3.4/32";
    private static final String CIDR_IPV4_2 = "10.20.20.0/24";
    private static final String CIDR_IPV4_3 = "10.10.10.0/24";
    private static final int DESTN_PORT_LOWER_PASS = 5201;
    private static final int DESTN_PORT_UPPER_PASS = 5301;
    private static final int DESTN_PORT_LOWER_DROP = 5501;
    private static final int DESTN_PORT_UPPER_DROP = 5601;
    private static final int SOURCE_PORT_LOWER_PASS = 45000;
    private static final int SOURCE_PORT_UPPER_PASS = 45100;
    private static final int SOURCE_PORT_LOWER_DROP = 45200;
    private static final int SOURCE_PORT_UPPER_DROP = 45300;
    private static final int RULE_PRIORITY_ONE = 1;
    private static final int RULE_PRIORITY_TWO = 2;
    private static final int RULE_PRIORITY_THREE = 3;
    private static final int RULE_PRIORITY_FOUR = 4;
    private static final int TCP_PROTOCOL_NUM = 6;
    private static final String CUSTOM_ACTION_NAME1 = "publishMetricAction1";
    private static final String CUSTOM_ACTION_NAME2 = "publishMetricAction2";
    private static final String TCP_FLAG_ACK = "ACK";
    protected static final String PASS_ACTION = "aws:pass";
    protected static final String DROP_ACTION = "aws:drop";
    protected static final String FORWARD_TO_SFE = "aws:forward_to_sfe";
    protected static final String ACTIVE = "ACTIVE";
    protected static final String DELETING = "DELETING";
    protected static final String STATEFUL_PASS_RULE = "pass tcp 10.20.20.0/24 45400:45500 <> 10.10.10.0/24";

    protected RuleGroup cfnStatelessRuleGroup1, cfnStatelessRuleGroup2, cfnStatelessRuleGroup3;
    protected RuleGroup cfnStatefulRuleGroup1, cfnStatefulRuleGroup2, cfnStatefulRuleGroup3, cfnStatefulRuleGroup4;
    protected software.amazon.awssdk.services.networkfirewall.model.RuleGroup statelessSdkRuleGroup1, statelessSdkRuleGroup2, statelessSdkRuleGroup3;
    protected software.amazon.awssdk.services.networkfirewall.model.RuleGroupResponse statelessSdkRuleGroupResponseWithNoTags,
            statelessSdkRuleGroupResponseWithTags;
    protected software.amazon.awssdk.services.networkfirewall.model.RuleGroupResponse statefulSdkRuleGroupResponseWithNoTags,
            statefulSdkRuleGroupResponseWithTags;
    protected software.amazon.awssdk.services.networkfirewall.model.RuleGroup statefulSdkRuleGroup1, statefulSdkRuleGroup2, statefulSdkRuleGroup3, statefulSdkRuleGroup4;
    protected CreateRuleGroupRequest createStatelessRuleGroupRequest1, createStatelessRuleGroupRequest2, createStatelessRuleGroupRequest3;
    protected CreateRuleGroupResponse createStatelessRuleGroupResponse1, createStatelessRuleGroupResponse2, createStatelessRuleGroupResponse3;
    protected CreateRuleGroupRequest createStatefulRuleGroupRequest1, createStatefulRuleGroupRequest2, createStatefulRuleGroupRequest3, createStatefulRuleGroupRequest4;
    protected CreateRuleGroupResponse createStatefulRuleGroupResponse1, createStatefulRuleGroupResponse2, createStatefulRuleGroupResponse3, createStatefulRuleGroupResponse4;
    protected DescribeRuleGroupRequest describeCreateStatelessRuleGroupRequest1, describeCreateStatelessRuleGroupRequest2,
            describeCreateStatelessRuleGroupRequest3, describeStatelessRuleGroupRequestWithArn;
    protected DescribeRuleGroupResponse describeCreateStatelessRuleGroupResponse1, describeCreateStatelessRuleGroupResponse2, describeCreateStatelessRuleGroupResponse3;
    protected DescribeRuleGroupRequest describeUpdateStatelessRuleGroupRequest1, describeUpdateStatelessRuleGroupRequest2, describeUpdateStatelessRuleGroupRequest3;
    protected DescribeRuleGroupResponse describeUpdateStatelessRuleGroupResponse1, describeUpdateStatelessRuleGroupResponse2, describeUpdateStatelessRuleGroupResponse3;
    protected Set<Tag> statelessTags, statefulTags;
    protected DescribeRuleGroupRequest describeCreateStatefulRuleGroupRequest1, describeCreateStatefulRuleGroupRequest2, describeCreateStatefulRuleGroupRequest3,
            describeCreateStatefulRuleGroupRequest4, describeStatefulRuleGroupRequestWithArn;
    protected DescribeRuleGroupResponse describeCreateStatefulRuleGroupResponse1, describeCreateStatefulRuleGroupResponse2,
            describeCreateStatefulRuleGroupResponse3, describeCreateStatefulRuleGroupResponse4;
    protected UpdateRuleGroupRequest updateStatelessRuleGroupRequest1, updateStatelessRuleGroupRequest2, updateStatelessRuleGroupRequest3;
    protected UpdateRuleGroupResponse updateStatelessRuleGroupResponse1, updateStatelessRuleGroupResponse2, updateStatelessRuleGroupResponse3;
    protected UpdateRuleGroupRequest updateStatefulRuleGroupRequest1, updateStatefulRuleGroupRequest2, updateStatefulRuleGroupRequest3;
    protected UpdateRuleGroupResponse updateStatefulRuleGroupResponse1, updateStatefulRuleGroupResponse2, updateStatefulRuleGroupResponse3;
    protected DescribeRuleGroupRequest describeUpdateStatefulRuleGroupRequest1, describeUpdateStatefulRuleGroupRequest2, describeUpdateStatefulRuleGroupRequest3;
    protected DescribeRuleGroupResponse describeUpdateStatefulRuleGroupResponse1, describeUpdateStatefulRuleGroupResponse2, describeUpdateStatefulRuleGroupResponse3;
    protected TagResourceRequest tagStatelessRuleGroupRequest, tagStatefulRuleGroupRequest;

    protected void setupRuleGroupTest() {
        setupStatelessRuleGroupTestData();
        setupStatefulRuleGroupTestData();
    }

    private void setupStatelessRuleGroupTestData() {
        cfnStatelessRuleGroup1 = createStatelessRuleGroup1();
        cfnStatelessRuleGroup2 = createStatelessRuleGroup2();
        cfnStatelessRuleGroup3 = createStatelessRuleGroup3();

        statelessSdkRuleGroup1 = translateToStatelessSdkRuleGroup1();
        statelessSdkRuleGroup2 = translateToStatelessSdkRuleGroup2();
        statelessSdkRuleGroup3 = translateToStatelessSdkRuleGroup3();

        statelessTags = Collections.singleton(Tag.builder()
                .key("rule-group")
                .value("stateless")
                .build());

        statelessSdkRuleGroupResponseWithNoTags = software.amazon.awssdk.services.networkfirewall.model.RuleGroupResponse.builder()
                .capacity(CAPACITY)
                .description(DESCRIPTION)
                .ruleGroupArn(STATELESS_RULEGROUP_ARN)
                .ruleGroupId(STATELESS_RULEGROUP_ID)
                .ruleGroupName(STATELESS_RULEGROUP_NAME)
                .ruleGroupStatus(ACTIVE)
                .type(STATELESS_RULEGROUP_TYPE)
                .tags(new ArrayList<software.amazon.awssdk.services.networkfirewall.model.Tag>())
                .build();

        Set<software.amazon.awssdk.services.networkfirewall.model.Tag> sdkStatelessTags = statelessTags
                .stream()
                .map(tag -> software.amazon.awssdk.services.networkfirewall.model.Tag.builder()
                        .key(tag.getKey())
                        .value(tag.getValue())
                        .build())
                .collect(Collectors.toSet());
        statelessSdkRuleGroupResponseWithTags = software.amazon.awssdk.services.networkfirewall.model.RuleGroupResponse.builder()
                .capacity(CAPACITY)
                .description(DESCRIPTION)
                .ruleGroupArn(STATELESS_RULEGROUP_ARN)
                .ruleGroupId(STATELESS_RULEGROUP_ID)
                .ruleGroupName(STATELESS_RULEGROUP_NAME)
                .ruleGroupStatus(ACTIVE)
                .type(STATELESS_RULEGROUP_TYPE)
                .tags(sdkStatelessTags)
                .build();

        tagStatelessRuleGroupRequest = TagResourceRequest.builder()
                .resourceArn(STATELESS_RULEGROUP_ARN)
                .tags(sdkStatelessTags)
                .build();

        // Creates
        // Stateless RuleGroup Request with no tags - request1
        createStatelessRuleGroupRequest1 = CreateRuleGroupRequest.builder()
                .ruleGroup(statelessSdkRuleGroup1)
                .capacity(CAPACITY)
                .description(DESCRIPTION)
                .type(STATELESS_RULEGROUP_TYPE)
                .ruleGroupName(STATELESS_RULEGROUP_NAME)
                .tags(Collections.emptySet())
                .build();

        createStatelessRuleGroupResponse1 = CreateRuleGroupResponse.builder()
                .ruleGroupResponse(statelessSdkRuleGroupResponseWithNoTags)
                .updateToken(UPDATE_TOKEN)
                .build();

        describeCreateStatelessRuleGroupRequest1 = DescribeRuleGroupRequest.builder()
                .ruleGroupName(STATELESS_RULEGROUP_NAME)
                .type(STATELESS_RULEGROUP_TYPE)
                .ruleGroupArn(STATELESS_RULEGROUP_ARN)
                .build();

        describeCreateStatelessRuleGroupResponse1 = DescribeRuleGroupResponse.builder()
                .ruleGroupResponse(statelessSdkRuleGroupResponseWithNoTags)
                .ruleGroup(statelessSdkRuleGroup1)
                .updateToken(UPDATE_TOKEN)
                .build();

        // Stateless RuleGroup Request with tags - request2
        createStatelessRuleGroupRequest2 = CreateRuleGroupRequest.builder()
                .ruleGroup(statelessSdkRuleGroup2)
                .capacity(CAPACITY)
                .description(DESCRIPTION)
                .type(STATELESS_RULEGROUP_TYPE)
                .ruleGroupName(STATELESS_RULEGROUP_NAME)
                .tags(sdkStatelessTags)
                .build();

        createStatelessRuleGroupResponse2 = CreateRuleGroupResponse.builder()
                .ruleGroupResponse(statelessSdkRuleGroupResponseWithTags)
                .updateToken(UPDATE_TOKEN)
                .build();

        describeCreateStatelessRuleGroupRequest2 = DescribeRuleGroupRequest.builder()
                .ruleGroupName(STATELESS_RULEGROUP_NAME)
                .type(STATELESS_RULEGROUP_TYPE)
                .ruleGroupArn(STATELESS_RULEGROUP_ARN)
                .build();

        describeCreateStatelessRuleGroupResponse2 = DescribeRuleGroupResponse.builder()
                .ruleGroupResponse(statelessSdkRuleGroupResponseWithTags)
                .ruleGroup(statelessSdkRuleGroup2)
                .updateToken(UPDATE_TOKEN)
                .build();

        // Stateless RuleGroup Request with tags - request3
        createStatelessRuleGroupRequest3 = CreateRuleGroupRequest.builder()
                .ruleGroup(statelessSdkRuleGroup3)
                .capacity(CAPACITY)
                .description(DESCRIPTION)
                .type(STATELESS_RULEGROUP_TYPE)
                .ruleGroupName(STATELESS_RULEGROUP_NAME)
                .tags(sdkStatelessTags)
                .build();

        createStatelessRuleGroupResponse3 = CreateRuleGroupResponse.builder()
                .ruleGroupResponse(statelessSdkRuleGroupResponseWithTags)
                .updateToken(UPDATE_TOKEN)
                .build();

        describeCreateStatelessRuleGroupRequest3 = DescribeRuleGroupRequest.builder()
                .ruleGroupName(STATELESS_RULEGROUP_NAME)
                .type(STATELESS_RULEGROUP_TYPE)
                .ruleGroupArn(STATELESS_RULEGROUP_ARN)
                .build();

        describeCreateStatelessRuleGroupResponse3 = DescribeRuleGroupResponse.builder()
                .ruleGroupResponse(statelessSdkRuleGroupResponseWithTags)
                .ruleGroup(statelessSdkRuleGroup3)
                .updateToken(UPDATE_TOKEN)
                .build();

        // Updates
        // Stateless RuleGroup Request with no tags - request1
        updateStatelessRuleGroupRequest1 = UpdateRuleGroupRequest.builder()
                .ruleGroup(statelessSdkRuleGroup1)
                .description(DESCRIPTION)
                .type(STATELESS_RULEGROUP_TYPE)
                .ruleGroupName(STATELESS_RULEGROUP_NAME)
                .ruleGroupArn(STATELESS_RULEGROUP_ARN)
                .updateToken(UPDATE_TOKEN)
                .build();

        updateStatelessRuleGroupResponse1 = UpdateRuleGroupResponse.builder()
                .ruleGroupResponse(statelessSdkRuleGroupResponseWithNoTags)
                .updateToken(NEXT_UPDATE_TOKEN)
                .build();

        describeUpdateStatelessRuleGroupRequest1 = DescribeRuleGroupRequest.builder()
                .ruleGroupName(STATELESS_RULEGROUP_NAME)
                .type(STATELESS_RULEGROUP_TYPE)
                .ruleGroupArn(STATELESS_RULEGROUP_ARN)
                .build();

        describeUpdateStatelessRuleGroupResponse1 = DescribeRuleGroupResponse.builder()
                .ruleGroupResponse(statelessSdkRuleGroupResponseWithNoTags)
                .ruleGroup(statelessSdkRuleGroup1)
                .updateToken(NEXT_UPDATE_TOKEN)
                .build();

        // Stateless RuleGroup Request with tags - request2
        updateStatelessRuleGroupRequest2 = UpdateRuleGroupRequest.builder()
                .ruleGroup(statelessSdkRuleGroup2)
                .ruleGroupName(STATELESS_RULEGROUP_NAME)
                .type(STATELESS_RULEGROUP_TYPE)
                .description(DESCRIPTION)
                .ruleGroupArn(STATELESS_RULEGROUP_ARN)
                .updateToken(UPDATE_TOKEN)
                .build();

        updateStatelessRuleGroupResponse2 = UpdateRuleGroupResponse.builder()
                .ruleGroupResponse(statelessSdkRuleGroupResponseWithTags)
                .updateToken(NEXT_UPDATE_TOKEN)
                .build();

        describeUpdateStatelessRuleGroupRequest2 = DescribeRuleGroupRequest.builder()
                .ruleGroupName(STATELESS_RULEGROUP_NAME)
                .ruleGroupArn(STATELESS_RULEGROUP_ARN)
                .type(STATELESS_RULEGROUP_TYPE)
                .build();

        describeUpdateStatelessRuleGroupResponse2 = DescribeRuleGroupResponse.builder()
                .ruleGroupResponse(statelessSdkRuleGroupResponseWithTags)
                .ruleGroup(statelessSdkRuleGroup2)
                .updateToken(NEXT_UPDATE_TOKEN)
                .build();

        // Stateless RuleGroup Request with tags - request3
        updateStatelessRuleGroupRequest3 = UpdateRuleGroupRequest.builder()
                .ruleGroup(statelessSdkRuleGroup3)
                .description(DESCRIPTION)
                .type(STATELESS_RULEGROUP_TYPE)
                .ruleGroupName(STATELESS_RULEGROUP_NAME)
                .ruleGroupArn(STATELESS_RULEGROUP_ARN)
                .updateToken(UPDATE_TOKEN)
                .build();

        updateStatelessRuleGroupResponse3 = UpdateRuleGroupResponse.builder()
                .ruleGroupResponse(statelessSdkRuleGroupResponseWithTags)
                .updateToken(NEXT_UPDATE_TOKEN)
                .build();

        describeStatelessRuleGroupRequestWithArn = DescribeRuleGroupRequest.builder()
                .ruleGroupArn(STATELESS_RULEGROUP_ARN)
                .build();

        describeUpdateStatelessRuleGroupRequest3 = DescribeRuleGroupRequest.builder()
                .ruleGroupName(STATELESS_RULEGROUP_NAME)
                .type(STATELESS_RULEGROUP_TYPE)
                .ruleGroupArn(STATELESS_RULEGROUP_ARN)
                .build();

        describeUpdateStatelessRuleGroupResponse3 = DescribeRuleGroupResponse.builder()
                .ruleGroupResponse(statelessSdkRuleGroupResponseWithTags)
                .ruleGroup(statelessSdkRuleGroup3)
                .updateToken(NEXT_UPDATE_TOKEN)
                .build();
    }

    private RuleGroup createStatelessRuleGroup1() {
        final RuleGroup ruleGroup = RuleGroup.builder()
                .rulesSource(RulesSource.builder()
                        .statelessRulesAndCustomActions(StatelessRulesAndCustomActions.builder()
                                .statelessRules(Collections.singleton(StatelessRule.builder()
                                        .priority(RULE_PRIORITY_ONE).ruleDefinition(RuleDefinition.builder()
                                                .actions(Collections.singleton(PASS_ACTION))
                                                .matchAttributes(MatchAttributes.builder()
                                                        .sources(Collections.singleton(Address.builder()
                                                                .addressDefinition(CIDR_IPV4_1)
                                                                .build()))
                                                        .build())
                                                .build())
                                        .build())).build()).build())
                .build();
        return ruleGroup;
    }

    private software.amazon.awssdk.services.networkfirewall.model.RuleGroup translateToStatelessSdkRuleGroup1() {
        software.amazon.awssdk.services.networkfirewall.model.RuleGroup translatedSdkRuleGroup = software.amazon.awssdk.services.networkfirewall.model.RuleGroup.builder()
                .rulesSource(software.amazon.awssdk.services.networkfirewall.model.RulesSource.builder()
                        .statelessRulesAndCustomActions(software.amazon.awssdk.services.networkfirewall.model.StatelessRulesAndCustomActions.builder()
                                .statelessRules(Collections.singletonList(software.amazon.awssdk.services.networkfirewall.model.StatelessRule.builder()
                                        .priority(RULE_PRIORITY_ONE)
                                        .ruleDefinition(software.amazon.awssdk.services.networkfirewall.model.RuleDefinition.builder()
                                                .actions(PASS_ACTION)
                                                .matchAttributes(software.amazon.awssdk.services.networkfirewall.model.MatchAttributes.builder()
                                                        .sources(software.amazon.awssdk.services.networkfirewall.model.Address.builder()
                                                                .addressDefinition(CIDR_IPV4_1)
                                                                .build()).build()).build())
                                        .build()))
                                .build())
                        .build()).build();
        return translatedSdkRuleGroup;
    }

    private RuleGroup createStatelessRuleGroup2() {
        PortRange srcPortRangePass = PortRange.builder()
                .fromPort(SOURCE_PORT_LOWER_PASS)
                .toPort(SOURCE_PORT_UPPER_PASS)
                .build();
        PortRange destPortRangePass = PortRange.builder()
                .fromPort(DESTN_PORT_LOWER_PASS)
                .toPort(DESTN_PORT_UPPER_PASS)
                .build();
        PortRange destPortRangeDrop = PortRange.builder()
                .fromPort(DESTN_PORT_LOWER_DROP)
                .toPort(DESTN_PORT_UPPER_DROP)
                .build();
        PortRange srcPortRangeDrop = PortRange.builder()
                .fromPort(SOURCE_PORT_LOWER_DROP)
                .toPort(SOURCE_PORT_UPPER_DROP)
                .build();
        TCPFlagField tcpFlagField = TCPFlagField.builder()
                .flags(Collections.singleton(TCP_FLAG_ACK))
                .masks(Collections.singleton(TCP_FLAG_ACK))
                .build();
        Set<PortRange> sourcePortsPass = Collections.singleton(srcPortRangePass);
        Set<PortRange> destPortsPass = Collections.singleton(destPortRangePass);
        Set<PortRange> destPortsDrop = Collections.singleton(destPortRangeDrop);
        Set<PortRange> sourcePortsDrop = Collections.singleton(srcPortRangeDrop);
        Address destinationAddress = Address.builder()
                .addressDefinition(CIDR_IPV4_2).build();
        Address sourceDestination = Address.builder()
                .addressDefinition(CIDR_IPV4_3).build();
        MatchAttributes passAttributesFwd = MatchAttributes.builder()
                .sources(Collections.singleton(destinationAddress))
                .sourcePorts(sourcePortsPass)
                .destinations(Collections.singleton(sourceDestination))
                .destinationPorts(destPortsPass)
                .protocols(Collections.singleton(TCP_PROTOCOL_NUM))
                .build();
        MatchAttributes passAttributesBack = MatchAttributes.builder()
                .sources(Collections.singleton(sourceDestination))
                .sourcePorts(destPortsPass)
                .destinations(Collections.singleton(destinationAddress))
                .destinationPorts(sourcePortsPass)
                .protocols(Collections.singleton(TCP_PROTOCOL_NUM))
                .build();
        MatchAttributes dropAttributesFwd = MatchAttributes.builder()
                .sources(Collections.singleton(destinationAddress))
                .sourcePorts(sourcePortsDrop)
                .destinations(Collections.singleton(sourceDestination))
                .destinationPorts(destPortsDrop)
                .protocols(Collections.singleton(TCP_PROTOCOL_NUM))
                .tCPFlags(Collections.singleton(tcpFlagField))
                .build();
        MatchAttributes dropAttributesBack = MatchAttributes.builder()
                .sources(Collections.singleton(sourceDestination))
                .sourcePorts(destPortsDrop)
                .destinations(Collections.singleton(destinationAddress))
                .destinationPorts(sourcePortsDrop)
                .protocols(Collections.singleton(TCP_PROTOCOL_NUM))
                .tCPFlags(Collections.singleton(tcpFlagField))
                .build();
        RuleDefinition passDefinition1 = RuleDefinition.builder()
                .matchAttributes(passAttributesFwd)
                .actions(Collections.singleton(PASS_ACTION))
                .build();
        RuleDefinition passDefinition2 = RuleDefinition.builder()
                .matchAttributes(passAttributesBack)
                .actions(Collections.singleton(FORWARD_TO_SFE))
                .build();
        RuleDefinition dropDefinition1 = RuleDefinition.builder()
                .matchAttributes(dropAttributesFwd)
                .actions(Collections.singleton(DROP_ACTION))
                .build();
        RuleDefinition dropDefinition2 = RuleDefinition.builder()
                .matchAttributes(dropAttributesBack)
                .actions(Collections.singleton(DROP_ACTION))
                .build();
        StatelessRule passRule = StatelessRule.builder()
                .ruleDefinition(passDefinition1)
                .priority(RULE_PRIORITY_ONE)
                .build();
        StatelessRule passRuleBack = StatelessRule.builder()
                .ruleDefinition(passDefinition2)
                .priority(RULE_PRIORITY_TWO)
                .build();
        StatelessRule dropRule = StatelessRule.builder()
                .ruleDefinition(dropDefinition1)
                .priority(RULE_PRIORITY_THREE)
                .build();
        StatelessRule dropRuleBack = StatelessRule.builder()
                .ruleDefinition(dropDefinition2)
                .priority(RULE_PRIORITY_FOUR)
                .build();
        StatelessRulesAndCustomActions rulesAndCustomActions = StatelessRulesAndCustomActions.builder()
                .statelessRules(ImmutableSet.of(passRule, passRuleBack, dropRule, dropRuleBack)).build();

        RulesSource rulesSource = RulesSource.builder()
                .statelessRulesAndCustomActions(rulesAndCustomActions)
                .build();
        return RuleGroup.builder()
                .rulesSource(rulesSource).build();
    }

    private software.amazon.awssdk.services.networkfirewall.model.RuleGroup translateToStatelessSdkRuleGroup2() {
        software.amazon.awssdk.services.networkfirewall.model.PortRange srcPortRangePass = software.amazon.awssdk.services.networkfirewall.model.PortRange.builder()
                .fromPort(SOURCE_PORT_LOWER_PASS)
                .toPort(SOURCE_PORT_UPPER_PASS)
                .build();
        software.amazon.awssdk.services.networkfirewall.model.PortRange destPortRangePass = software.amazon.awssdk.services.networkfirewall.model.PortRange.builder()
                .fromPort(DESTN_PORT_LOWER_PASS)
                .toPort(DESTN_PORT_UPPER_PASS)
                .build();
        software.amazon.awssdk.services.networkfirewall.model.PortRange destPortRangeDrop = software.amazon.awssdk.services.networkfirewall.model.PortRange.builder()
                .fromPort(DESTN_PORT_LOWER_DROP)
                .toPort(DESTN_PORT_UPPER_DROP)
                .build();
        software.amazon.awssdk.services.networkfirewall.model.PortRange srcPortRangeDrop = software.amazon.awssdk.services.networkfirewall.model.PortRange.builder()
                .fromPort(SOURCE_PORT_LOWER_DROP)
                .toPort(SOURCE_PORT_UPPER_DROP)
                .build();
        software.amazon.awssdk.services.networkfirewall.model.TCPFlagField tcpFlagField = software.amazon.awssdk.services.networkfirewall.model.TCPFlagField.builder()
                .flagsWithStrings(Collections.singleton(TCP_FLAG_ACK))
                .masksWithStrings(Collections.singleton(TCP_FLAG_ACK))
                .build();
        software.amazon.awssdk.services.networkfirewall.model.Address destinationAddress = software.amazon.awssdk.services.networkfirewall.model.Address.builder()
                .addressDefinition(CIDR_IPV4_2).build();
        software.amazon.awssdk.services.networkfirewall.model.Address sourceDestination = software.amazon.awssdk.services.networkfirewall.model.Address.builder()
                .addressDefinition(CIDR_IPV4_3).build();
        Set<software.amazon.awssdk.services.networkfirewall.model.PortRange> sourcePortsPass = Collections.singleton(srcPortRangePass);
        Set<software.amazon.awssdk.services.networkfirewall.model.PortRange> destPortsPass = Collections.singleton(destPortRangePass);
        Set<software.amazon.awssdk.services.networkfirewall.model.PortRange> destPortsDrop = Collections.singleton(destPortRangeDrop);
        Set<software.amazon.awssdk.services.networkfirewall.model.PortRange> sourcePortsDrop = Collections.singleton(srcPortRangeDrop);

        software.amazon.awssdk.services.networkfirewall.model.MatchAttributes passAttributesFwd = software.amazon.awssdk.services.networkfirewall.model.MatchAttributes.builder()
                .destinationPorts(destPortsPass)
                .sourcePorts(sourcePortsPass)
                .sources(Collections.singleton(destinationAddress))
                .destinations(Collections.singleton(sourceDestination))
                .protocols(Collections.singleton(TCP_PROTOCOL_NUM))
                .build();
        software.amazon.awssdk.services.networkfirewall.model.MatchAttributes passAttributesBck = software.amazon.awssdk.services.networkfirewall.model.MatchAttributes.builder()
                .destinationPorts(sourcePortsPass)
                .sourcePorts(destPortsPass)
                .sources(Collections.singleton(sourceDestination))
                .destinations(Collections.singleton(destinationAddress))
                .protocols(Collections.singleton(TCP_PROTOCOL_NUM))
                .build();
        software.amazon.awssdk.services.networkfirewall.model.MatchAttributes dropAttributesFwd = software.amazon.awssdk.services.networkfirewall.model.MatchAttributes.builder()
                .destinationPorts(destPortsDrop)
                .sourcePorts(sourcePortsDrop)
                .sources(Collections.singleton(destinationAddress))
                .destinations(Collections.singleton(sourceDestination))
                .protocols(Collections.singleton(TCP_PROTOCOL_NUM))
                .tcpFlags(Collections.singleton(tcpFlagField))
                .build();
        software.amazon.awssdk.services.networkfirewall.model.MatchAttributes dropAttributesBck = software.amazon.awssdk.services.networkfirewall.model.MatchAttributes.builder()
                .destinationPorts(sourcePortsDrop)
                .sourcePorts(destPortsDrop)
                .sources(Collections.singleton(sourceDestination))
                .destinations(Collections.singleton(destinationAddress))
                .protocols(Collections.singleton(TCP_PROTOCOL_NUM))
                .tcpFlags(Collections.singleton(tcpFlagField))
                .build();

        software.amazon.awssdk.services.networkfirewall.model.RuleDefinition ruleDefinition1 = software.amazon.awssdk.services.networkfirewall.model.RuleDefinition.builder()
                .matchAttributes(passAttributesFwd)
                .actions(PASS_ACTION)
                .build();
        software.amazon.awssdk.services.networkfirewall.model.RuleDefinition ruleDefinition2 = software.amazon.awssdk.services.networkfirewall.model.RuleDefinition.builder()
                .matchAttributes(passAttributesBck)
                .actions(FORWARD_TO_SFE)
                .build();
        software.amazon.awssdk.services.networkfirewall.model.RuleDefinition ruleDefinition3 = software.amazon.awssdk.services.networkfirewall.model.RuleDefinition.builder()
                .matchAttributes(dropAttributesFwd)
                .actions(DROP_ACTION)
                .build();
        software.amazon.awssdk.services.networkfirewall.model.RuleDefinition ruleDefinition4 = software.amazon.awssdk.services.networkfirewall.model.RuleDefinition.builder()
                .matchAttributes(dropAttributesBck)
                .actions(DROP_ACTION)
                .build();
        software.amazon.awssdk.services.networkfirewall.model.StatelessRule statelessRule1 = software.amazon.awssdk.services.networkfirewall.model.StatelessRule.builder()
                .ruleDefinition(ruleDefinition1)
                .priority(RULE_PRIORITY_ONE)
                .build();
        software.amazon.awssdk.services.networkfirewall.model.StatelessRule statelessRule2 = software.amazon.awssdk.services.networkfirewall.model.StatelessRule.builder()
                .ruleDefinition(ruleDefinition2)
                .priority(RULE_PRIORITY_TWO)
                .build();
        software.amazon.awssdk.services.networkfirewall.model.StatelessRule statelessRule3 = software.amazon.awssdk.services.networkfirewall.model.StatelessRule.builder()
                .ruleDefinition(ruleDefinition3)
                .priority(RULE_PRIORITY_THREE)
                .build();
        software.amazon.awssdk.services.networkfirewall.model.StatelessRule statelessRule4 = software.amazon.awssdk.services.networkfirewall.model.StatelessRule.builder()
                .ruleDefinition(ruleDefinition4)
                .priority(RULE_PRIORITY_FOUR)
                .build();
        software.amazon.awssdk.services.networkfirewall.model.StatelessRulesAndCustomActions statelessRulesAndCustomActions =
                software.amazon.awssdk.services.networkfirewall.model.StatelessRulesAndCustomActions.builder()
                        .statelessRules(statelessRule4, statelessRule1, statelessRule2, statelessRule3).build();
        software.amazon.awssdk.services.networkfirewall.model.RulesSource rulesSource = software.amazon.awssdk.services.networkfirewall.model.RulesSource.builder()
                .statelessRulesAndCustomActions(statelessRulesAndCustomActions)
                .build();
        software.amazon.awssdk.services.networkfirewall.model.RuleGroup translatedSdkRuleGroup = software.amazon.awssdk.services.networkfirewall.model.RuleGroup.builder()
                .rulesSource(rulesSource).build();
        return translatedSdkRuleGroup;
    }

    private RuleGroup createStatelessRuleGroup3() {
        Address address = Address.builder()
                .addressDefinition(CIDR_IPV4_1).build();
        MatchAttributes matchAttributes = MatchAttributes.builder()
                .sources(Collections.singleton(address))
                .protocols(Collections.singleton(TCP_PROTOCOL_NUM))
                .build();
        RuleDefinition ruleDefinition = RuleDefinition.builder()
                .matchAttributes(matchAttributes)
                .actions(Collections.singleton(PASS_ACTION))
                .build();
        StatelessRule statelessRule = StatelessRule.builder()
                .ruleDefinition(ruleDefinition)
                .priority(RULE_PRIORITY_ONE)
                .build();
        CustomAction customAction1 = CustomAction.builder()
                .actionName(CUSTOM_ACTION_NAME1)
                .actionDefinition(ActionDefinition.builder()
                        .publishMetricAction(PublishMetricAction.builder().
                                dimensions(Collections.singleton(Dimension.builder().value("test1")
                                        .build())).
                                build())
                        .build())
                .build();
        CustomAction customAction2 = CustomAction.builder()
                .actionName(CUSTOM_ACTION_NAME2)
                .actionDefinition(ActionDefinition.builder()
                        .publishMetricAction(PublishMetricAction.builder().
                                dimensions(Collections.singleton(Dimension.builder().value("test2")
                                        .build())).
                                build())
                        .build())
                .build();
        StatelessRulesAndCustomActions rulesAndCustomActions = StatelessRulesAndCustomActions.builder()
                .statelessRules(ImmutableSet.of(statelessRule))
                .customActions(ImmutableSet.of(customAction1, customAction2))
                .build();
        RulesSource rulesSource = RulesSource.builder()
                .statelessRulesAndCustomActions(rulesAndCustomActions)
                .build();
        return RuleGroup.builder()
                .rulesSource(rulesSource).build();
    }

    private software.amazon.awssdk.services.networkfirewall.model.RuleGroup translateToStatelessSdkRuleGroup3() {
        software.amazon.awssdk.services.networkfirewall.model.Address address = software.amazon.awssdk.services.networkfirewall.model.Address.builder()
                .addressDefinition(CIDR_IPV4_1).build();
        software.amazon.awssdk.services.networkfirewall.model.MatchAttributes matchAttributes = software.amazon.awssdk.services.networkfirewall.model.MatchAttributes.builder()
                .sources(Collections.singleton(address))
                .protocols(Collections.singleton(TCP_PROTOCOL_NUM))
                .build();
        software.amazon.awssdk.services.networkfirewall.model.RuleDefinition ruleDefinition = software.amazon.awssdk.services.networkfirewall.model.RuleDefinition.builder()
                .matchAttributes(matchAttributes)
                .actions(PASS_ACTION)
                .build();
        software.amazon.awssdk.services.networkfirewall.model.StatelessRule statelessRule = software.amazon.awssdk.services.networkfirewall.model.StatelessRule.builder()
                .ruleDefinition(ruleDefinition)
                .priority(RULE_PRIORITY_ONE)
                .build();
        software.amazon.awssdk.services.networkfirewall.model.CustomAction customAction1 = software.amazon.awssdk.services.networkfirewall.model.CustomAction.builder()
                .actionDefinition(software.amazon.awssdk.services.networkfirewall.model.ActionDefinition.builder()
                        .publishMetricAction(software.amazon.awssdk.services.networkfirewall.model.PublishMetricAction.builder()
                                .dimensions(software.amazon.awssdk.services.networkfirewall.model.Dimension.builder()
                                        .value("test1")
                                        .build())
                                .build())
                        .build())
                .actionName(CUSTOM_ACTION_NAME1)
                .build();
        software.amazon.awssdk.services.networkfirewall.model.CustomAction customAction2 = software.amazon.awssdk.services.networkfirewall.model.CustomAction.builder()
                .actionDefinition(software.amazon.awssdk.services.networkfirewall.model.ActionDefinition.builder()
                        .publishMetricAction(software.amazon.awssdk.services.networkfirewall.model.PublishMetricAction.builder()
                                .dimensions(software.amazon.awssdk.services.networkfirewall.model.Dimension.builder()
                                        .value("test2")
                                        .build())
                                .build())
                        .build())
                .actionName(CUSTOM_ACTION_NAME2)
                .build();
        software.amazon.awssdk.services.networkfirewall.model.StatelessRulesAndCustomActions statelessRulesAndCustomActions =
                software.amazon.awssdk.services.networkfirewall.model.StatelessRulesAndCustomActions.builder()
                        .statelessRules(statelessRule)
                        .customActions(customAction1, customAction2)
                        .build();
        software.amazon.awssdk.services.networkfirewall.model.RulesSource rulesSource = software.amazon.awssdk.services.networkfirewall.model.RulesSource.builder()
                .statelessRulesAndCustomActions(statelessRulesAndCustomActions)
                .build();
        software.amazon.awssdk.services.networkfirewall.model.RuleGroup translatedSdkRuleGroup = software.amazon.awssdk.services.networkfirewall.model.RuleGroup.builder()
                .rulesSource(rulesSource).build();
        return translatedSdkRuleGroup;

    }

    private void setupStatefulRuleGroupTestData() {
        cfnStatefulRuleGroup1 = createStatefulRuleGroup1();
        cfnStatefulRuleGroup2 = createStatefulRuleGroup2();
        cfnStatefulRuleGroup3 = createStatefulRuleGroup3();
        cfnStatefulRuleGroup4 = createStatefulRuleGroup4();

        statefulSdkRuleGroup1 = translateToStatefulSdkRuleGroup1();
        statefulSdkRuleGroup2 = translateToStatefulSdkRuleGroup2();
        statefulSdkRuleGroup3 = translateToStatefulSdkRuleGroup3();
        statefulSdkRuleGroup4 = translateToStatefulSdkRuleGroup4();

        statefulTags = Collections.singleton(Tag.builder()
                .key("rule-group")
                .value("stateful")
                .build());

        statefulSdkRuleGroupResponseWithNoTags = software.amazon.awssdk.services.networkfirewall.model.RuleGroupResponse.builder()
                .capacity(CAPACITY)
                .description(DESCRIPTION)
                .ruleGroupArn(STATEFUL_RULEGROUP_ARN)
                .ruleGroupId(STATEFUL_RULEGROUP_ID)
                .ruleGroupName(STATEFUL_RULEGROUP_NAME)
                .ruleGroupStatus(ACTIVE)
                .type(STATEFUL_RULEGROUP_TYPE)
                .tags(new ArrayList<software.amazon.awssdk.services.networkfirewall.model.Tag>())
                .build();

        Set<software.amazon.awssdk.services.networkfirewall.model.Tag> sdkStatefulTags = statefulTags
                .stream()
                .map(tag -> software.amazon.awssdk.services.networkfirewall.model.Tag.builder()
                        .key(tag.getKey())
                        .value(tag.getValue())
                        .build())
                .collect(Collectors.toSet());
        statefulSdkRuleGroupResponseWithTags = software.amazon.awssdk.services.networkfirewall.model.RuleGroupResponse.builder()
                .capacity(CAPACITY)
                .description(DESCRIPTION)
                .ruleGroupArn(STATEFUL_RULEGROUP_ARN)
                .ruleGroupId(STATEFUL_RULEGROUP_ID)
                .ruleGroupName(STATEFUL_RULEGROUP_NAME)
                .ruleGroupStatus(ACTIVE)
                .type(STATEFUL_RULEGROUP_TYPE)
                .tags(sdkStatefulTags)
                .build();

        tagStatefulRuleGroupRequest = TagResourceRequest.builder()
                .resourceArn(STATEFUL_RULEGROUP_ARN)
                .tags(sdkStatefulTags)
                .build();

        // Stateful RuleGroup Request with no tags - request1
        createStatefulRuleGroupRequest1 = CreateRuleGroupRequest.builder()
                .ruleGroup(statefulSdkRuleGroup1)
                .capacity(CAPACITY)
                .description(DESCRIPTION)
                .type(STATEFUL_RULEGROUP_TYPE)
                .ruleGroupName(STATEFUL_RULEGROUP_NAME)
                .tags(Collections.emptySet())
                .build();

        createStatefulRuleGroupResponse1 = CreateRuleGroupResponse.builder()
                .ruleGroupResponse(statefulSdkRuleGroupResponseWithNoTags)
                .updateToken(UPDATE_TOKEN)
                .build();

        describeCreateStatefulRuleGroupRequest1 = DescribeRuleGroupRequest.builder()
                .ruleGroupName(STATEFUL_RULEGROUP_NAME)
                .type(STATEFUL_RULEGROUP_TYPE)
                .ruleGroupArn(STATEFUL_RULEGROUP_ARN)
                .build();

        describeCreateStatefulRuleGroupResponse1 = DescribeRuleGroupResponse.builder()
                .ruleGroupResponse(statefulSdkRuleGroupResponseWithNoTags)
                .ruleGroup(statefulSdkRuleGroup1)
                .updateToken(UPDATE_TOKEN)
                .build();

        // Stateful RuleGroup Request with tags - request2
        createStatefulRuleGroupRequest2 = CreateRuleGroupRequest.builder()
                .ruleGroup(statefulSdkRuleGroup2)
                .capacity(CAPACITY)
                .description(DESCRIPTION)
                .type(STATEFUL_RULEGROUP_TYPE)
                .ruleGroupName(STATEFUL_RULEGROUP_NAME)
                .tags(sdkStatefulTags)
                .build();

        createStatefulRuleGroupResponse2 = CreateRuleGroupResponse.builder()
                .ruleGroupResponse(statefulSdkRuleGroupResponseWithTags)
                .updateToken(UPDATE_TOKEN)
                .build();

        describeCreateStatefulRuleGroupRequest2 = DescribeRuleGroupRequest.builder()
                .ruleGroupName(STATEFUL_RULEGROUP_NAME)
                .type(STATEFUL_RULEGROUP_TYPE)
                .ruleGroupArn(STATEFUL_RULEGROUP_ARN)
                .build();

        describeCreateStatefulRuleGroupResponse2 = DescribeRuleGroupResponse.builder()
                .ruleGroupResponse(statefulSdkRuleGroupResponseWithTags)
                .ruleGroup(statefulSdkRuleGroup2)
                .updateToken(UPDATE_TOKEN)
                .build();

        // Stateful RuleGroup Request with tags - request3
        createStatefulRuleGroupRequest3 = CreateRuleGroupRequest.builder()
                .ruleGroup(statefulSdkRuleGroup3)
                .capacity(CAPACITY)
                .description(DESCRIPTION)
                .type(STATEFUL_RULEGROUP_TYPE)
                .ruleGroupName(STATEFUL_RULEGROUP_NAME)
                .tags(sdkStatefulTags)
                .build();

        createStatefulRuleGroupResponse3 = CreateRuleGroupResponse.builder()
                .ruleGroupResponse(statefulSdkRuleGroupResponseWithTags)
                .updateToken(UPDATE_TOKEN)
                .build();

        describeCreateStatefulRuleGroupRequest3 = DescribeRuleGroupRequest.builder()
                .ruleGroupName(STATEFUL_RULEGROUP_NAME)
                .type(STATEFUL_RULEGROUP_TYPE)
                .ruleGroupArn(STATEFUL_RULEGROUP_ARN)
                .build();

        describeCreateStatefulRuleGroupResponse3 = DescribeRuleGroupResponse.builder()
                .ruleGroupResponse(statefulSdkRuleGroupResponseWithTags)
                .ruleGroup(statefulSdkRuleGroup3)
                .updateToken(UPDATE_TOKEN)
                .build();

        // Stateful RuleGroup Request with tags - request4
        createStatefulRuleGroupRequest4 = CreateRuleGroupRequest.builder()
                .ruleGroup(statefulSdkRuleGroup4)
                .capacity(CAPACITY)
                .description(DESCRIPTION)
                .type(STATEFUL_RULEGROUP_TYPE)
                .ruleGroupName(STATEFUL_RULEGROUP_NAME)
                .tags(sdkStatefulTags)
                .build();

        createStatefulRuleGroupResponse4 = CreateRuleGroupResponse.builder()
                .ruleGroupResponse(statefulSdkRuleGroupResponseWithTags)
                .updateToken(UPDATE_TOKEN)
                .build();

        describeCreateStatefulRuleGroupRequest4 = DescribeRuleGroupRequest.builder()
                .ruleGroupName(STATEFUL_RULEGROUP_NAME)
                .type(STATEFUL_RULEGROUP_TYPE)
                .ruleGroupArn(STATEFUL_RULEGROUP_ARN)
                .build();

        describeCreateStatefulRuleGroupResponse4 = DescribeRuleGroupResponse.builder()
                .ruleGroupResponse(statefulSdkRuleGroupResponseWithTags)
                .ruleGroup(statefulSdkRuleGroup4)
                .updateToken(UPDATE_TOKEN)
                .build();

        // Updates
        // Stateful RuleGroup Request with no tags - request1
        updateStatefulRuleGroupRequest1 = UpdateRuleGroupRequest.builder()
                .ruleGroup(statefulSdkRuleGroup1)
                .description(DESCRIPTION)
                .type(STATEFUL_RULEGROUP_TYPE)
                .ruleGroupName(STATEFUL_RULEGROUP_NAME)
                .ruleGroupArn(STATEFUL_RULEGROUP_ARN)
                .updateToken(UPDATE_TOKEN)
                .build();

        updateStatefulRuleGroupResponse1 = UpdateRuleGroupResponse.builder()
                .ruleGroupResponse(statefulSdkRuleGroupResponseWithNoTags)
                .updateToken(NEXT_UPDATE_TOKEN)
                .build();

        describeUpdateStatefulRuleGroupRequest1 = DescribeRuleGroupRequest.builder()
                .ruleGroupName(STATEFUL_RULEGROUP_NAME)
                .type(STATEFUL_RULEGROUP_TYPE)
                .ruleGroupArn(STATEFUL_RULEGROUP_ARN)
                .build();

        describeUpdateStatefulRuleGroupResponse1 = DescribeRuleGroupResponse.builder()
                .ruleGroupResponse(statefulSdkRuleGroupResponseWithNoTags)
                .ruleGroup(statefulSdkRuleGroup1)
                .updateToken(NEXT_UPDATE_TOKEN)
                .build();

        // Stateful RuleGroup Request with tags - request2
        updateStatefulRuleGroupRequest2 = UpdateRuleGroupRequest.builder()
                .ruleGroup(statefulSdkRuleGroup2)
                .description(DESCRIPTION)
                .ruleGroupArn(STATEFUL_RULEGROUP_ARN)
                .ruleGroupName(STATEFUL_RULEGROUP_NAME)
                .type(STATEFUL_RULEGROUP_TYPE)
                .updateToken(UPDATE_TOKEN)
                .build();

        updateStatefulRuleGroupResponse2 = UpdateRuleGroupResponse.builder()
                .ruleGroupResponse(statefulSdkRuleGroupResponseWithTags)
                .updateToken(NEXT_UPDATE_TOKEN)
                .build();

        describeUpdateStatefulRuleGroupRequest2 = DescribeRuleGroupRequest.builder()
                .ruleGroupName(STATEFUL_RULEGROUP_NAME)
                .type(STATEFUL_RULEGROUP_TYPE)
                .ruleGroupArn(STATEFUL_RULEGROUP_ARN)
                .build();

        describeUpdateStatefulRuleGroupResponse2 = DescribeRuleGroupResponse.builder()
                .ruleGroupResponse(statefulSdkRuleGroupResponseWithTags)
                .ruleGroup(statefulSdkRuleGroup2)
                .updateToken(NEXT_UPDATE_TOKEN)
                .build();

        // Stateful RuleGroup Request with tags - request3
        updateStatefulRuleGroupRequest3 = UpdateRuleGroupRequest.builder()
                .ruleGroup(statefulSdkRuleGroup3)
                .description(DESCRIPTION)
                .type(STATEFUL_RULEGROUP_TYPE)
                .ruleGroupName(STATEFUL_RULEGROUP_NAME)
                .ruleGroupArn(STATEFUL_RULEGROUP_ARN)
                .updateToken(UPDATE_TOKEN)
                .build();

        updateStatefulRuleGroupResponse3 = UpdateRuleGroupResponse.builder()
                .ruleGroupResponse(statefulSdkRuleGroupResponseWithTags)
                .updateToken(NEXT_UPDATE_TOKEN)
                .build();

        describeStatefulRuleGroupRequestWithArn = DescribeRuleGroupRequest.builder()
                .ruleGroupArn(STATEFUL_RULEGROUP_ARN)
                .build();

        describeUpdateStatefulRuleGroupRequest3 = DescribeRuleGroupRequest.builder()
                .ruleGroupName(STATEFUL_RULEGROUP_NAME)
                .type(STATEFUL_RULEGROUP_TYPE)
                .ruleGroupArn(STATEFUL_RULEGROUP_ARN)
                .build();

        describeUpdateStatefulRuleGroupResponse3 = DescribeRuleGroupResponse.builder()
                .ruleGroupResponse(statefulSdkRuleGroupResponseWithTags)
                .ruleGroup(statefulSdkRuleGroup3)
                .updateToken(NEXT_UPDATE_TOKEN)
                .build();
    }

    private RuleGroup createStatefulRuleGroup1() {
        return RuleGroup.builder()
                .rulesSource(RulesSource.builder()
                        .rulesString(STATEFUL_PASS_RULE)
                        .build())
                .build();
    }

    private software.amazon.awssdk.services.networkfirewall.model.RuleGroup translateToStatefulSdkRuleGroup1() {
        return software.amazon.awssdk.services.networkfirewall.model.RuleGroup.builder()
                .rulesSource(software.amazon.awssdk.services.networkfirewall.model.RulesSource.builder()
                        .rulesString(STATEFUL_PASS_RULE)
                        .build())
                .build();
    }

    private RuleGroup createStatefulRuleGroup2() {
        return RuleGroup.builder()
                .rulesSource(RulesSource.builder()
                        .rulesSourceList(RulesSourceList.builder()
                                .targetTypes(Collections.singleton("TLS_SNI"))
                                .generatedRulesType("ALLOWLIST")
                                .targets(Collections.singleton(".amazon.com"))
                                .build())
                        .build())
                .build();
    }

    private software.amazon.awssdk.services.networkfirewall.model.RuleGroup translateToStatefulSdkRuleGroup2() {
        return software.amazon.awssdk.services.networkfirewall.model.RuleGroup.builder()
                .rulesSource(software.amazon.awssdk.services.networkfirewall.model.RulesSource.builder()
                        .rulesSourceList(software.amazon.awssdk.services.networkfirewall.model.RulesSourceList.builder()
                                .targets(Collections.singleton(".amazon.com"))
                                .targetTypes(TargetType.TLS_SNI)
                                .generatedRulesType("ALLOWLIST")
                                .build())
                        .build())
                .build();
    }

    private RuleGroup createStatefulRuleGroup3() {
        return RuleGroup.builder()
                .rulesSource(RulesSource.builder()
                        .statefulRules(Collections.singleton(StatefulRule.builder()
                                .action("PASS")
                                .header(Header.builder()
                                        .sourcePort("any")
                                        .destinationPort("any")
                                        .source("$HOME_NET")
                                        .destination("$EXTERNAL_NET")
                                        .protocol("TCP")
                                        .direction("FORWARD")
                                        .build())
                                .ruleOptions(Collections.singleton(RuleOption.builder()
                                        .keyword("sid")
                                        .settings(Collections.singleton("100"))
                                        .build()))
                                .build()))
                        .build())
                .build();
    }

    private software.amazon.awssdk.services.networkfirewall.model.RuleGroup translateToStatefulSdkRuleGroup3() {
        return software.amazon.awssdk.services.networkfirewall.model.RuleGroup.builder()
                .rulesSource(software.amazon.awssdk.services.networkfirewall.model.RulesSource.builder()
                        .statefulRules(Collections.singleton(software.amazon.awssdk.services.networkfirewall.model.StatefulRule.builder()
                                .action("PASS")
                                .header(software.amazon.awssdk.services.networkfirewall.model.Header.builder()
                                        .protocol("TCP")
                                        .source("$HOME_NET")
                                        .direction("FORWARD")
                                        .sourcePort("any")
                                        .destinationPort("any")
                                        .destination("$EXTERNAL_NET")
                                        .build())
                                .ruleOptions(Collections.singleton(software.amazon.awssdk.services.networkfirewall.model.RuleOption.builder()
                                        .settings("100")
                                        .keyword("sid")
                                        .build()))
                                .build()))
                        .build())
                .build();
    }

    private RuleGroup createStatefulRuleGroup4() {
        return RuleGroup.builder()
                .rulesSource(RulesSource.builder()
                        .rulesString(STATEFUL_PASS_RULE)
                        .build())
                .ruleVariables(RuleVariables.builder()
                        .iPSets(ImmutableMap.of("test", IPSet.builder()
                                .definition(Collections.singleton("test-definition-1, test-definition-2"))
                                .build()))
                        .build())
                .build();
    }

    private software.amazon.awssdk.services.networkfirewall.model.RuleGroup translateToStatefulSdkRuleGroup4() {
        return software.amazon.awssdk.services.networkfirewall.model.RuleGroup.builder()
                .rulesSource(software.amazon.awssdk.services.networkfirewall.model.RulesSource.builder()
                        .rulesString(STATEFUL_PASS_RULE)
                        .build())
                .ruleVariables(software.amazon.awssdk.services.networkfirewall.model.RuleVariables.builder()
                        .ipSets(ImmutableMap.of("test", software.amazon.awssdk.services.networkfirewall.model.IPSet.builder()
                                .definition(Collections.singleton("test-definition-1, test-definition-2"))
                                .build()))
                        .build())
                .build();
    }

    static {
        MOCK_CREDENTIALS = new Credentials("accessKey", "secretKey", "token");
        logger = new LoggerProxy();
    }

    static ProxyClient<NetworkFirewallClient> MOCK_PROXY(
            final AmazonWebServicesClientProxy proxy,
            final NetworkFirewallClient networkFirewallClient) {
        return new ProxyClient<NetworkFirewallClient>() {
            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseT
            injectCredentialsAndInvokeV2(RequestT request, Function<RequestT, ResponseT> requestFunction) {
                return proxy.injectCredentialsAndInvokeV2(request, requestFunction);
            }

            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse>
            CompletableFuture<ResponseT>
            injectCredentialsAndInvokeV2Async(RequestT request, Function<RequestT, CompletableFuture<ResponseT>> requestFunction) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse, IterableT extends SdkIterable<ResponseT>>
            IterableT
            injectCredentialsAndInvokeIterableV2(RequestT request, Function<RequestT, IterableT> requestFunction) {
                return proxy.injectCredentialsAndInvokeIterableV2(request, requestFunction);
            }

            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseInputStream<ResponseT>
            injectCredentialsAndInvokeV2InputStream(RequestT requestT, Function<RequestT, ResponseInputStream<ResponseT>> function) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseBytes<ResponseT>
            injectCredentialsAndInvokeV2Bytes(RequestT requestT, Function<RequestT, ResponseBytes<ResponseT>> function) {
                throw new UnsupportedOperationException();
            }

            @Override
            public NetworkFirewallClient client() {
                return networkFirewallClient;
            }
        };
    }
}
