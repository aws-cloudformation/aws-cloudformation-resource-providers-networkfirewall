package software.amazon.networkfirewall.firewallpolicy;

import java.lang.UnsupportedOperationException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.services.networkfirewall.model.*;
import software.amazon.cloudformation.proxy.*;

public class AbstractTestBase {
  protected static final Credentials MOCK_CREDENTIALS;
  protected static final LoggerProxy logger;

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

  static AmazonWebServicesClientProxy getAmazonWebServicesClientProxy() {
    final int remainingTimeToExecuteInMillis = 600;
    return new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS,
            () -> Duration.ofSeconds(remainingTimeToExecuteInMillis).toMillis());
  }

  private static final String policyName = "Policy";
  private static final String policyArn = "arn:aws:network-firewall:us-east-1:123456789012:firewall-policy/Policy";
  private static final String statefulRuleGroupArn = "arn:aws:network-firewall:us-east-1:123456789012:stateful-rulegroup/RuleGroup";
  private static final String statelessRuleGroupArn = "arn:aws:network-firewall:us-east-1:123456789012:stateless-rulegroup/RuleGroup2";
  private static final String actionName = "action";
  private static final String dimension = "dimension";
  private static final String statelessAction = "aws:pass";
  private static final String fragmentStatelessAction = "aws:drop";
  private static final String description = "Sample description";
  private static final String status = "ACTIVE";
  private static final String deleting = "DELETING";
  private static final String key = "key";
  private static final String key2 = "key2";
  private static final String value = "value";

  public final static ResourceModel RESOURCE_MODEL = ResourceModel
          .builder()
          .firewallPolicyName(policyName)
          .firewallPolicyArn(policyArn)
          .firewallPolicy(SetFirewallPolicy())
          .description(description)
          .tags(TagSet())
          .build();

  public final static ResourceHandlerRequest<ResourceModel> RESOURCE_HANDLER_REQUEST =
          ResourceHandlerRequest.<ResourceModel>builder()
                  .desiredResourceState(RESOURCE_MODEL)
                  .previousResourceState(baseModel().build())
                  .build();

  private static ResourceModel.ResourceModelBuilder baseModel() {
    return ResourceModel.builder()
            .firewallPolicyName(policyName)
            .firewallPolicyArn(policyArn)
            .tags(PreviousTagSet());
  }

  public final static CreateFirewallPolicyRequest CREATE_FIREWALL_POLICY_REQUEST =
          CreateFirewallPolicyRequest.builder()
                  .firewallPolicyName(policyName)
                  .description(description)
                  .firewallPolicy(software.amazon.awssdk.services.networkfirewall.model.FirewallPolicy.builder()
                          .statefulRuleGroupReferences(
                                  software.amazon.awssdk.services.networkfirewall.model.StatefulRuleGroupReference.builder()
                                          .resourceArn(statefulRuleGroupArn)
                                          .build())
                          .statelessRuleGroupReferences(
                                  software.amazon.awssdk.services.networkfirewall.model.StatelessRuleGroupReference.builder()
                                          .resourceArn(statelessRuleGroupArn)
                                          .priority(1)
                                          .build())
                          .statelessCustomActions(software.amazon.awssdk.services.networkfirewall.model.CustomAction.builder()
                                  .actionName(actionName)
                                  .actionDefinition(software.amazon.awssdk.services.networkfirewall.model.ActionDefinition.builder()
                                          .publishMetricAction(software.amazon.awssdk.services.networkfirewall.model.PublishMetricAction.builder()
                                                  .dimensions(software.amazon.awssdk.services.networkfirewall.model.Dimension.builder()
                                                          .value(dimension)
                                                          .build())
                                                  .build())
                                          .build())
                                  .build())
                          .statelessDefaultActions(statelessAction,actionName)
                          .statelessFragmentDefaultActions(fragmentStatelessAction)
                          .build()
                  )
                  .tags(TagList())
                  .build();

  public final static CreateFirewallPolicyResponse CREATE_FIREWALL_POLICY_RESPONSE =
          CreateFirewallPolicyResponse.builder()
                  .firewallPolicyResponse(software.amazon.awssdk.services.networkfirewall.model.FirewallPolicyResponse.builder()
                          .firewallPolicyName(policyName)
                          .firewallPolicyArn(policyArn)
                          .description(description)
                          .tags(TagList())
                          .build())
                  .build();

  public final static ResourceModel CREATE_FIREWALL_POLICY_RESPONSE_RESOURCE =
          ResourceModel.builder()
                  .firewallPolicyName(policyName)
                  .firewallPolicyArn(policyArn)
                  .firewallPolicy(SetFirewallPolicy())
                  .description(description)
                  .tags(TagSet())
                  .build();


  public final static DescribeFirewallPolicyRequest DESCRIBE_FIREWALL_POLICY_REQUEST =
          DescribeFirewallPolicyRequest.builder()
                  .firewallPolicyName(policyName)
                  .firewallPolicyArn(policyArn)
                  .build();

  public final static DescribeFirewallPolicyResponse DESCRIBE_FIREWALL_POLICY_RESPONSE =
          DescribeFirewallPolicyResponse.builder()
                  .firewallPolicyResponse(software.amazon.awssdk.services.networkfirewall.model.FirewallPolicyResponse.builder()
                          .firewallPolicyName(policyName)
                          .firewallPolicyArn(policyArn)
                          .description(description)
                          .firewallPolicyStatus(status)
                          .tags(TagList())
                          .build())
                  .firewallPolicy(software.amazon.awssdk.services.networkfirewall.model.FirewallPolicy.builder()
                          .statefulRuleGroupReferences(
                                  software.amazon.awssdk.services.networkfirewall.model.StatefulRuleGroupReference.builder()
                                          .resourceArn(statefulRuleGroupArn)
                                          .build())
                          .statelessRuleGroupReferences(
                                  software.amazon.awssdk.services.networkfirewall.model.StatelessRuleGroupReference.builder()
                                          .resourceArn(statelessRuleGroupArn)
                                          .priority(1)
                                          .build())
                          .statelessCustomActions(software.amazon.awssdk.services.networkfirewall.model.CustomAction.builder()
                                  .actionName(actionName)
                                  .actionDefinition(software.amazon.awssdk.services.networkfirewall.model.ActionDefinition.builder()
                                          .publishMetricAction(software.amazon.awssdk.services.networkfirewall.model.PublishMetricAction.builder()
                                                  .dimensions(software.amazon.awssdk.services.networkfirewall.model.Dimension.builder()
                                                          .value(dimension)
                                                          .build())
                                                  .build())
                                          .build())
                                  .build())
                          .statelessDefaultActions(statelessAction,actionName)
                          .statelessFragmentDefaultActions(fragmentStatelessAction)
                          .build())
                  .build();

  public final static ResourceModel DESCRIBE_FIREWALL_POLICY_RESPONSE_RESOURCE_MODEL =
          ResourceModel
                  .builder()
                  .firewallPolicyName(policyName)
                  .firewallPolicyArn(policyArn)
                  .firewallPolicy(SetFirewallPolicy())
                  .description(description)
                  .tags(TagSet())
                  .build();

  public final static UpdateFirewallPolicyRequest UPDATE_FIREWALL_POLICY_REQUEST =
          UpdateFirewallPolicyRequest.builder()
                  .firewallPolicyArn(policyArn)
                  .firewallPolicyName(policyName)
                  .description(description)
                  .firewallPolicy(software.amazon.awssdk.services.networkfirewall.model.FirewallPolicy.builder()
                          .statefulRuleGroupReferences(
                                  software.amazon.awssdk.services.networkfirewall.model.StatefulRuleGroupReference.builder()
                                          .resourceArn(statefulRuleGroupArn)
                                          .build())
                          .statelessRuleGroupReferences(
                                  software.amazon.awssdk.services.networkfirewall.model.StatelessRuleGroupReference.builder()
                                          .resourceArn(statelessRuleGroupArn)
                                          .priority(1)
                                          .build())
                          .statelessCustomActions(software.amazon.awssdk.services.networkfirewall.model.CustomAction.builder()
                                  .actionName(actionName)
                                  .actionDefinition(software.amazon.awssdk.services.networkfirewall.model.ActionDefinition.builder()
                                          .publishMetricAction(software.amazon.awssdk.services.networkfirewall.model.PublishMetricAction.builder()
                                                  .dimensions(software.amazon.awssdk.services.networkfirewall.model.Dimension.builder()
                                                          .value(dimension)
                                                          .build())
                                                  .build())
                                          .build())
                                  .build())
                          .statelessDefaultActions(statelessAction,actionName)
                          .statelessFragmentDefaultActions(fragmentStatelessAction)
                          .build()
                  )
                  .build();

  public final static UpdateFirewallPolicyResponse UPDATE_FIREWALL_POLICY_RESPONSE =
          UpdateFirewallPolicyResponse.builder()
                  .firewallPolicyResponse(software.amazon.awssdk.services.networkfirewall.model.FirewallPolicyResponse.builder()
                          .firewallPolicyName(policyName)
                          .firewallPolicyArn(policyArn)
                          .description(description)
                          .firewallPolicyStatus(status)
                          .build())
                  .build();

  public final static ResourceModel UPDATE_FIREWALL_POLICY_RESPONSE_RESOURCE =
          ResourceModel.builder()
                  .firewallPolicyName(policyName)
                  .firewallPolicyArn(policyArn)
                  .firewallPolicy(SetFirewallPolicy())
                  .description(description)
                  .tags(TagSet())
                  .build();


  public final static UntagResourceRequest UNTAG_RESOURCE_REQUEST =
          UntagResourceRequest.builder()
                  .resourceArn(policyArn)
                  .tagKeys(key2)
                  .build();

  public final static UntagResourceResponse UNTAG_RESOURCE_RESPONSE =
          UntagResourceResponse.builder()
                  .build();

  public final static TagResourceRequest TAG_RESOURCE_REQUEST =
          TagResourceRequest.builder()
                  .resourceArn(policyArn)
                  .tags(TagList())
                  .build();

  public final static TagResourceResponse TAG_RESOURCE_RESPONSE =
          TagResourceResponse.builder()
                  .build();

  public final static DeleteFirewallPolicyRequest DELETE_FIREWALL_POLICY_REQUEST =
          DeleteFirewallPolicyRequest.builder()
                  .firewallPolicyName(policyName)
                  .firewallPolicyArn(policyArn)
                  .build();

  public final static DeleteFirewallPolicyResponse DELETE_FIREWALL_POLICY_RESPONSE =
          DeleteFirewallPolicyResponse.builder()
                  .firewallPolicyResponse(FirewallPolicyResponse.builder()
                          .firewallPolicyName(policyName)
                          .firewallPolicyArn(policyArn)
                          .firewallPolicyStatus(deleting)
                          .build())
                  .build();

  public static software.amazon.networkfirewall.firewallpolicy.FirewallPolicy SetFirewallPolicy () {
    Set<StatelessRuleGroupReference> statelessReferences = new HashSet<>();
    Set<StatefulRuleGroupReference> statefulReferences = new HashSet<>();
    Set<CustomAction> customAction = new HashSet<>();
    Set<Dimension> dimensionSet = new HashSet<>();
    Set<String> statelessDefaultAction = new HashSet<>();
    Set<String> fragmentStatelessDefaultAction = new HashSet<>();
    statelessReferences.add(StatelessRuleGroupReference.builder()
            .priority(1)
            .resourceArn(statelessRuleGroupArn)
            .build());
    statefulReferences.add(StatefulRuleGroupReference.builder()
            .resourceArn(statefulRuleGroupArn)
            .build());
    dimensionSet.add(Dimension.builder()
            .value(dimension)
            .build());
    customAction.add(CustomAction.builder()
            .actionName(actionName)
            .actionDefinition(ActionDefinition.builder()
                    .publishMetricAction(PublishMetricAction.builder()
                            .dimensions(dimensionSet)
                            .build())
                    .build())
            .build());
    statelessDefaultAction.add(statelessAction);
    statelessDefaultAction.add(actionName);
    fragmentStatelessDefaultAction.add(fragmentStatelessAction);

    return FirewallPolicy.builder()
            .statefulRuleGroupReferences(statefulReferences)
            .statelessRuleGroupReferences(statelessReferences)
            .statelessCustomActions(customAction)
            .statelessDefaultActions(statelessDefaultAction)
            .statelessFragmentDefaultActions(fragmentStatelessDefaultAction)
            .build();
  }

  public static HashSet<software.amazon.networkfirewall.firewallpolicy.Tag> TagSet() {
    HashSet<software.amazon.networkfirewall.firewallpolicy.Tag> tags = new HashSet<>();
    tags.add(software.amazon.networkfirewall.firewallpolicy.Tag.builder()
            .key(key)
            .value(value)
            .build());
    return tags;
  }

  public static HashSet<software.amazon.networkfirewall.firewallpolicy.Tag> PreviousTagSet() {
    HashSet<software.amazon.networkfirewall.firewallpolicy.Tag> tags = new HashSet<>();
    tags.add(software.amazon.networkfirewall.firewallpolicy.Tag.builder()
            .key(key2)
            .build());
    return tags;
  }

  public static List<software.amazon.awssdk.services.networkfirewall.model.Tag> TagList() {
    List<software.amazon.awssdk.services.networkfirewall.model.Tag> tags = new ArrayList<>();
    tags.add(software.amazon.awssdk.services.networkfirewall.model.Tag.builder()
            .key(key)
            .value(value)
            .build());
    return tags;
  }

}
