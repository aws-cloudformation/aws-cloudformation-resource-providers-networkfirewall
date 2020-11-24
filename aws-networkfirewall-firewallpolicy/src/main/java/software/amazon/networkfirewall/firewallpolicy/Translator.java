package software.amazon.networkfirewall.firewallpolicy;

import lombok.NonNull;
import software.amazon.awssdk.services.networkfirewall.model.CreateFirewallPolicyRequest;
import software.amazon.awssdk.services.networkfirewall.model.DescribeFirewallPolicyRequest;
import software.amazon.awssdk.services.networkfirewall.model.DescribeFirewallPolicyResponse;
import software.amazon.awssdk.services.networkfirewall.model.ListFirewallPoliciesRequest;
import software.amazon.awssdk.services.networkfirewall.model.ListFirewallPoliciesResponse;
import software.amazon.awssdk.services.networkfirewall.model.DeleteFirewallPolicyRequest;
import software.amazon.awssdk.services.networkfirewall.model.TagResourceRequest;
import software.amazon.awssdk.services.networkfirewall.model.UntagResourceRequest;
import software.amazon.awssdk.services.networkfirewall.model.FirewallPolicy;
import software.amazon.awssdk.services.networkfirewall.model.CustomAction;
import software.amazon.awssdk.services.networkfirewall.model.ActionDefinition;
import software.amazon.awssdk.services.networkfirewall.model.Dimension;
import software.amazon.awssdk.services.networkfirewall.model.PublishMetricAction;
import software.amazon.awssdk.services.networkfirewall.model.StatefulRuleGroupReference;
import software.amazon.awssdk.services.networkfirewall.model.StatelessRuleGroupReference;
import software.amazon.awssdk.services.networkfirewall.model.UpdateFirewallPolicyRequest;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is a centralized placeholder for
 *  - api request construction
 *  - object translation to/from aws sdk
 *  - resource model construction for read/list handlers
 */

public class Translator {

  /**
   * Request to create a resource
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  static CreateFirewallPolicyRequest translateToCreateRequest(final ResourceModel model, final Map<String, String> tags) {
    return  CreateFirewallPolicyRequest.builder()
            .firewallPolicyName(model.getFirewallPolicyName())
            .firewallPolicy(translatePolicyToSDK(model.getFirewallPolicy()))
            .description(model.getDescription())
            .tags(translateTagsToNetworkFirewallTags(tags))
            .build();
  }

  /**
   * Request to read a resource
   * @param model resource model
   * @return awsRequest the aws service request to describe a resource
   */
  static DescribeFirewallPolicyRequest translateToReadRequest(final ResourceModel model) {
    return DescribeFirewallPolicyRequest.builder()
            .firewallPolicyArn(model.getFirewallPolicyArn())
            .firewallPolicyName(model.getFirewallPolicyName())
            .build();
  }

  static Set<Tag> translateTagsFromSdk(final List<software.amazon.awssdk.services.networkfirewall.model.Tag> tags) {
    Set<Tag> result = new HashSet<>();
    for (software.amazon.awssdk.services.networkfirewall.model.Tag tag: tags) {
      result.add(Tag.builder()
              .key(tag.key())
              .value(tag.value())
              .build());
    }
    return result;
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

  /**
   * Translates resource object from sdk into a resource model
   * @param response the aws service describe resource response
   * @return model resource model
   */
  static ResourceModel translateFromReadResponse(final DescribeFirewallPolicyResponse response) {
    return ResourceModel.builder()
            .firewallPolicyArn(response.firewallPolicyResponse().firewallPolicyArn())
            .firewallPolicyName(response.firewallPolicyResponse().firewallPolicyName())
            .firewallPolicyId(response.firewallPolicyResponse().firewallPolicyId())
            .description(response.firewallPolicyResponse().description())
            .firewallPolicy(translatePolicyFromSDK(response.firewallPolicy()))
            .tags(translateTagsFromSdk(response.firewallPolicyResponse().tags()))
            .build();
  }

  /**
   * Request to update properties of a previously created resource
   * @param model resource model
   * @return awsRequest the aws service request to modify a resource
   */
  static UpdateFirewallPolicyRequest translateToUpdateRequest(final ResourceModel model,final String updateToken) {
    return UpdateFirewallPolicyRequest.builder()
            .updateToken(updateToken)
            .firewallPolicyArn(model.getFirewallPolicyArn())
            .firewallPolicyName(model.getFirewallPolicyName())
            .firewallPolicy(translatePolicyToSDK(model.getFirewallPolicy()))
            .description(model.getDescription())
            .build();
  }

  /**
   * Request to list resources
   * @return awsRequest the aws service request to list resources within aws account
   */
  static ListFirewallPoliciesRequest translateToListRequest(final String nextToken) {
    return ListFirewallPoliciesRequest.builder()
            .maxResults(50)
            .nextToken(nextToken)
            .build();
  }

  /**
   * Request to update properties of a previously created resource
   * @param model resource model
   * @return awsRequest the aws service request to modify a resource
   */
  static DeleteFirewallPolicyRequest translateToDeleteRequest(final ResourceModel model) {
    return DeleteFirewallPolicyRequest.builder()
            .firewallPolicyArn(model.getFirewallPolicyArn())
            .firewallPolicyName(model.getFirewallPolicyName())
            .build();
  }

  static TagResourceRequest translateToTagRequest(final ResourceModel model, @NonNull Map<String, String> tags) {
    return TagResourceRequest.builder()
            .resourceArn(model.getFirewallPolicyArn())
            .tags(translateTagsToNetworkFirewallTags(tags))
            .build();
  }

  static UntagResourceRequest translateToUntagRequest(final ResourceModel model, @NonNull Set<String> tagsToUntag) {
    return UntagResourceRequest.builder()
            .resourceArn(model.getFirewallPolicyArn())
            .tagKeys(tagsToUntag)
            .build();
  }

  /**
   * Translates resource objects from sdk into a resource model (primary identifier only)
   * @param listFirewallPoliciesResponse the aws service describe resource response
   * @return list of resource models
   */
  static List<ResourceModel> translateFromListRequest(final ListFirewallPoliciesResponse listFirewallPoliciesResponse) {
    return streamOfOrEmpty(listFirewallPoliciesResponse.firewallPolicies())
        .map(firewallPolicy -> ResourceModel.builder()
            .firewallPolicyName(firewallPolicy.name())
                .firewallPolicyArn(firewallPolicy.arn())
            .build())
        .collect(Collectors.toList());
  }

  private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
    return Optional.ofNullable(collection)
        .map(Collection::stream)
        .orElseGet(Stream::empty);
  }

  private static software.amazon.networkfirewall.firewallpolicy.FirewallPolicy translatePolicyFromSDK(
          final FirewallPolicy responsePolicy) {

    return software.amazon.networkfirewall.firewallpolicy.FirewallPolicy.builder()
            .statefulRuleGroupReferences(responsePolicy.statefulRuleGroupReferences() != null && responsePolicy.statefulRuleGroupReferences().size() != 0
                    ? translateStatefulRuleGroupReferenceFromSDK(responsePolicy.statefulRuleGroupReferences()): null)
            .statelessRuleGroupReferences(responsePolicy.statelessRuleGroupReferences() != null && responsePolicy.statelessRuleGroupReferences().size() != 0
                    ? translateStatelessRuleGroupReferenceFromSDK(responsePolicy.statelessRuleGroupReferences()): null)
            .statelessDefaultActions(responsePolicy.statelessDefaultActions().size() != 0
                    ? translateDefaultActionFromSDK(responsePolicy.statelessDefaultActions()) : null)
            .statelessFragmentDefaultActions(responsePolicy.statelessFragmentDefaultActions().size() != 0
                    ? translateDefaultActionFromSDK(responsePolicy.statelessFragmentDefaultActions()) : null)
            .statelessCustomActions(responsePolicy.statelessCustomActions() != null && responsePolicy.statelessCustomActions().size() != 0 ? translateCustomActionsFromSDK(responsePolicy.statelessCustomActions()): null)
            .build();
  }

  private static Set<software.amazon.networkfirewall.firewallpolicy.StatefulRuleGroupReference> translateStatefulRuleGroupReferenceFromSDK (
          final List<StatefulRuleGroupReference> references) {
    Set<software.amazon.networkfirewall.firewallpolicy.StatefulRuleGroupReference> result = new HashSet<>();

    for (StatefulRuleGroupReference reference : references) {
      software.amazon.networkfirewall.firewallpolicy.StatefulRuleGroupReference tempRef =
              software.amazon.networkfirewall.firewallpolicy.StatefulRuleGroupReference.builder()
                      .resourceArn(reference.resourceArn()).build();
      result.add(tempRef);
    }
    return result;
  }

  private static Set<software.amazon.networkfirewall.firewallpolicy.StatelessRuleGroupReference> translateStatelessRuleGroupReferenceFromSDK (
          final List<StatelessRuleGroupReference> references) {
    Set<software.amazon.networkfirewall.firewallpolicy.StatelessRuleGroupReference> result = new HashSet<>();

    for (StatelessRuleGroupReference reference : references) {
      software.amazon.networkfirewall.firewallpolicy.StatelessRuleGroupReference tempRef =
              software.amazon.networkfirewall.firewallpolicy.StatelessRuleGroupReference.builder()
                      .resourceArn(reference.resourceArn())
                      .priority(reference.priority())
                      .build();
      result.add(tempRef);
    }
    return result;
  }

  private static Set<software.amazon.networkfirewall.firewallpolicy.CustomAction> translateCustomActionsFromSDK (
          final List<CustomAction> actions) {
    Set<software.amazon.networkfirewall.firewallpolicy.CustomAction> result = new HashSet<>();

    for (CustomAction action : actions) {
      Set <software.amazon.networkfirewall.firewallpolicy.Dimension> dimensions = new HashSet<>();
      PublishMetricAction publishActionFromResponse = action.actionDefinition().publishMetricAction();
      if (publishActionFromResponse != null) {
        for (Dimension dim :publishActionFromResponse.dimensions()) {
          dimensions.add(new software.amazon.networkfirewall.firewallpolicy.Dimension(dim.value()));
        }
      }
      software.amazon.networkfirewall.firewallpolicy.PublishMetricAction publishAction =
              software.amazon.networkfirewall.firewallpolicy.PublishMetricAction.builder()
                      .dimensions(dimensions)
                      .build();
      software.amazon.networkfirewall.firewallpolicy.ActionDefinition definition =
              software.amazon.networkfirewall.firewallpolicy.ActionDefinition.builder()
                      .publishMetricAction(publishAction)
                      .build();
      software.amazon.networkfirewall.firewallpolicy.CustomAction tempRef =
              software.amazon.networkfirewall.firewallpolicy.CustomAction.builder()
                      .actionName(action.actionName())
                      .actionDefinition(definition)
                      .build();

      result.add(tempRef);
    }
    return result;
  }

  private static Set<String> translateDefaultActionFromSDK (final List<String> actions) {
    return new HashSet<>(actions);
  }

  private static FirewallPolicy translatePolicyToSDK(
          final software.amazon.networkfirewall.firewallpolicy.FirewallPolicy policy) {
    return FirewallPolicy.builder()
            .statefulRuleGroupReferences(policy.getStatefulRuleGroupReferences() != null && policy.getStatefulRuleGroupReferences().size() != 0
                    ? translateStatefulRuleGroupReferenceToSDK(policy.getStatefulRuleGroupReferences()): null)
            .statelessRuleGroupReferences(policy.getStatelessRuleGroupReferences() != null && policy.getStatelessRuleGroupReferences().size() != 0
                    ? translateRuleGroupReferenceToSDK(policy.getStatelessRuleGroupReferences()): null)
            .statelessDefaultActions(policy.getStatelessDefaultActions().size() != 0
                    ? translateDefaultActionToSDK(policy.getStatelessDefaultActions()) : null)
            .statelessFragmentDefaultActions(policy.getStatelessFragmentDefaultActions().size() != 0
                    ? translateDefaultActionToSDK(policy.getStatelessFragmentDefaultActions()) : null)
            .statelessCustomActions( policy.getStatelessCustomActions() != null && policy.getStatelessCustomActions().size() != 0 ? translateCustomActionsToSDK(policy.getStatelessCustomActions()): null)
            .build();
  }

  private static List<StatefulRuleGroupReference> translateStatefulRuleGroupReferenceToSDK (
          final Set<software.amazon.networkfirewall.firewallpolicy.StatefulRuleGroupReference>  references) {
    List<StatefulRuleGroupReference> result = new ArrayList<>();

    for (software.amazon.networkfirewall.firewallpolicy.StatefulRuleGroupReference reference : references) {
      StatefulRuleGroupReference tempRef = StatefulRuleGroupReference.builder()
                      .resourceArn(reference.getResourceArn()).build();
      result.add(tempRef);
    }
    return result;
  }

  private static List<StatelessRuleGroupReference> translateRuleGroupReferenceToSDK (
          final Set<software.amazon.networkfirewall.firewallpolicy.StatelessRuleGroupReference> references) {
    List<StatelessRuleGroupReference> result = new ArrayList<>();

    for (software.amazon.networkfirewall.firewallpolicy.StatelessRuleGroupReference reference : references) {
      StatelessRuleGroupReference tempRef = StatelessRuleGroupReference.builder()
                      .resourceArn(reference.getResourceArn())
                      .priority(reference.getPriority())
                      .build();
      result.add(tempRef);
    }
    return result;
  }

  private static List<CustomAction> translateCustomActionsToSDK (
          final Set<software.amazon.networkfirewall.firewallpolicy.CustomAction> actions) {
    List<CustomAction> result = new ArrayList<>();

    for (software.amazon.networkfirewall.firewallpolicy.CustomAction action : actions) {
      List<Dimension> dimensions = new ArrayList<>();
      software.amazon.networkfirewall.firewallpolicy.PublishMetricAction publishActionFromResponse =
              action.getActionDefinition().getPublishMetricAction();
      if (publishActionFromResponse != null) {
        for (software.amazon.networkfirewall.firewallpolicy.Dimension dim : publishActionFromResponse.getDimensions()) {
          dimensions.add(Dimension.builder()
                  .value(dim.getValue())
                  .build());
        }
      }

      PublishMetricAction publishAction = PublishMetricAction.builder()
                      .dimensions(dimensions)
                      .build();
      ActionDefinition definition = ActionDefinition.builder()
                      .publishMetricAction(publishAction)
                      .build();
      CustomAction tempRef = CustomAction.builder()
                      .actionName(action.getActionName())
                      .actionDefinition(definition)
                      .build();

      result.add(tempRef);
    }
    return result;
  }

  private static List<String> translateDefaultActionToSDK (final Set<String> actions) {
    return new ArrayList<>(actions);
  }
}
