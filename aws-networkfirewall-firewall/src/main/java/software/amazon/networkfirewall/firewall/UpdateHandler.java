package software.amazon.networkfirewall.firewall;

import com.google.common.collect.Sets;
import lombok.NonNull;
import org.apache.commons.collections4.CollectionUtils;
import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.awssdk.services.networkfirewall.model.AssociateFirewallPolicyRequest;
import software.amazon.awssdk.services.networkfirewall.model.AssociateFirewallPolicyResponse;
import software.amazon.awssdk.services.networkfirewall.model.AssociateSubnetsRequest;
import software.amazon.awssdk.services.networkfirewall.model.AssociateSubnetsResponse;
import software.amazon.awssdk.services.networkfirewall.model.Attachment;
import software.amazon.awssdk.services.networkfirewall.model.ConfigurationSyncState;
import software.amazon.awssdk.services.networkfirewall.model.DescribeFirewallResponse;
import software.amazon.awssdk.services.networkfirewall.model.DisassociateSubnetsRequest;
import software.amazon.awssdk.services.networkfirewall.model.DisassociateSubnetsResponse;
import software.amazon.awssdk.services.networkfirewall.model.FirewallStatusValue;
import software.amazon.awssdk.services.networkfirewall.model.InternalServerErrorException;
import software.amazon.awssdk.services.networkfirewall.model.InvalidRequestException;
import software.amazon.awssdk.services.networkfirewall.model.ResourceNotFoundException;
import software.amazon.awssdk.services.networkfirewall.model.SyncState;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class UpdateHandler extends BaseHandlerStd {
    private Logger logger;
    private AmazonWebServicesClientProxy proxy;
    private ResourceHandlerRequest<ResourceModel> handlerRequest;
    private CallbackContext callbackContext;
    private ProxyClient<NetworkFirewallClient> proxyClient;
    private ResourceModel desiredStateModel;
    private ResourceModel previousStateModel;
    private Set<String> subnetsToRemove;
    private Set<String> subnetsToAdd;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<NetworkFirewallClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        this.proxy = proxy;
        this.handlerRequest = request;
        this.callbackContext = callbackContext;
        this.proxyClient = proxyClient;
        this.logger = logger;
        this.desiredStateModel = request.getDesiredResourceState();
        this.previousStateModel = request.getPreviousResourceState();
        this.subnetsToAdd = computeSubnetsToAdd(
                previousStateModel.getSubnetMappings(), desiredStateModel.getSubnetMappings());
        this.subnetsToRemove = computeSubnetsToRemove(
                previousStateModel.getSubnetMappings(), desiredStateModel.getSubnetMappings());

        return ProgressEvent.progress(desiredStateModel, callbackContext)
                .then(this::verifyResourceExists)
                .then(this::updateFirewallDescription)
                .then(this::updateDeleteProtection)
                .then(this::updateFirewallPolicyChangeProtection)
                .then(this::updateSubnetChangeProtection)
                .then(this::associateFirewallPolicy)
                .then(this::associateSubnets)
                .then(this::disassociateSubnets)
                .then(this::updateTags)
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    private ProgressEvent<ResourceModel, CallbackContext> verifyResourceExists(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        return proxy.initiate(
                "Firewall::Update-ResourceExists", proxyClient, progress.getResourceModel(), callbackContext)
                .translateToServiceRequest(Translator::translateToDescribeFirewallRequest)
                .makeServiceCall((describeFirewallRequest, client) -> {
                    try {
                        client.injectCredentialsAndInvokeV2(describeFirewallRequest, client.client()::describeFirewall);
                    } catch (final ResourceNotFoundException e) {
                        throw new CfnNotFoundException(e);
                    } catch (final Exception e) {
                        throw new CfnGeneralServiceException(e.getMessage(), e);
                    }
                    // resource we are trying to update exists, return success
                    return ProgressEvent.defaultSuccessHandler(null);
                })
                .progress();
    }

    private ProgressEvent<ResourceModel, CallbackContext> associateSubnets(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        if (CollectionUtils.isEmpty(subnetsToAdd)) {
            // previous state and desired state are same. Nothing to update, so just return.
            return progress;
        }
        return proxy.initiate(
                "Firewall::Update-AssociateSubnets", proxyClient, progress.getResourceModel(), callbackContext)
                .translateToServiceRequest((model) -> Translator.translateToAssociateSubnets(model, subnetsToAdd))
                .makeServiceCall((request, client) ->
                        client.injectCredentialsAndInvokeV2(request, client.client()::associateSubnets))
                .stabilize(this::stabilizeAssociateSubnets)
                .progress();
    }

    private ProgressEvent<ResourceModel, CallbackContext> disassociateSubnets(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        if (CollectionUtils.isEmpty(subnetsToRemove)) {
            // previous state and desired state are same. Nothing to update, so just return success.
            return progress;
        }
        return proxy.initiate(
                "Firewall::Update-DisassociateSubnets", proxyClient, progress.getResourceModel(), callbackContext)
                .translateToServiceRequest((model) -> Translator.translateToDisassociateSubnets(model, subnetsToRemove))
                .makeServiceCall((request, client) ->
                        client.injectCredentialsAndInvokeV2(request, client.client()::disassociateSubnets))
                .stabilize(this::stabilizeDisassociateSubnets)
                .progress();
    }

    private ProgressEvent<ResourceModel, CallbackContext> updateFirewallDescription(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        if (Objects.equals(previousStateModel.getDescription(), desiredStateModel.getDescription())) {
            // previous state and desired state are same. Nothing to update, so just return success.
            return progress;
        }
        return proxy.initiate(
                "Firewall::Update-Description", proxyClient, progress.getResourceModel(), callbackContext)
                .translateToServiceRequest(Translator::translateToUpdateFirewallDescription)
                .makeServiceCall((request, client) ->
                        client.injectCredentialsAndInvokeV2(request, client.client()::updateFirewallDescription))
                .progress();
    }

    private ProgressEvent<ResourceModel, CallbackContext> updateDeleteProtection(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        if (Objects.equals(previousStateModel.getDeleteProtection(), desiredStateModel.getDeleteProtection())) {
            // previous state and desired state are same. Nothing to update, so just return success.
            return progress;
        }
        return proxy.initiate(
                "Firewall::Update-DeleteProtection", proxyClient, progress.getResourceModel(), callbackContext)
                .translateToServiceRequest(Translator::translateToUpdateFirewallDeleteProtection)
                .makeServiceCall((request, client) ->
                        client.injectCredentialsAndInvokeV2(request, client.client()::updateFirewallDeleteProtection))
                .progress();
    }

    private ProgressEvent<ResourceModel, CallbackContext> updateSubnetChangeProtection(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        if (Objects.equals(previousStateModel.getSubnetChangeProtection(), desiredStateModel.getSubnetChangeProtection())) {
            // previous state and desired state are same. Nothing to update, so just return success.
            return progress;
        }
        return proxy.initiate(
                "Firewall::Update-SubnetChangeProtection", proxyClient, progress.getResourceModel(), callbackContext)
                .translateToServiceRequest(Translator::translateToUpdateSubnetChangeProtection)
                .makeServiceCall((request, client) ->
                        client.injectCredentialsAndInvokeV2(request, client.client()::updateSubnetChangeProtection))
                .progress();
    }

    private ProgressEvent<ResourceModel, CallbackContext> updateFirewallPolicyChangeProtection(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        if (Objects.equals(previousStateModel.getFirewallPolicyChangeProtection(), desiredStateModel.getFirewallPolicyChangeProtection())) {
            // previous state and desired state are same. Nothing to update, so just return success.
            return progress;
        }
        return proxy.initiate(
                "Firewall::Update-FirewallPolicyChangeProtection", proxyClient, progress.getResourceModel(), callbackContext)
                .translateToServiceRequest(Translator::translateToUpdateFirewallPolicyChangeProtection)
                .makeServiceCall((request, client) ->
                        client.injectCredentialsAndInvokeV2(request, client.client()::updateFirewallPolicyChangeProtection))
                .progress();
    }

    private ProgressEvent<ResourceModel, CallbackContext> associateFirewallPolicy(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        if (Objects.equals(previousStateModel.getFirewallPolicyArn(), desiredStateModel.getFirewallPolicyArn())) {
            // previous state and desired state are same. Nothing to update, so just return success.
            return progress;
        }
        return proxy.initiate(
                "Firewall::Update-AssociateFirewallPolicy", proxyClient, progress.getResourceModel(), callbackContext)
                .translateToServiceRequest(Translator::translateToAssociateFirewallPolicy)
                .makeServiceCall((request, client) ->
                        client.injectCredentialsAndInvokeV2(request, client.client()::associateFirewallPolicy))
                .stabilize(this::stabilizeFirewallPolicyUpdate)
                .progress();
    }

    private boolean stabilizeFirewallPolicyUpdate(final AssociateFirewallPolicyRequest awsRequest,
            final AssociateFirewallPolicyResponse awsResponse, final ProxyClient<NetworkFirewallClient> client,
            final ResourceModel model, final CallbackContext callbackContext) {
        try {
            final DescribeFirewallResponse response = client.injectCredentialsAndInvokeV2(
                    Translator.translateToDescribeFirewallRequest(model),
                    client.client()::describeFirewall);

            // this can never be empty for active Firewall, but just in case of a bug, we should not mark the update
            // as success.
            if (response.firewallStatus().syncStates().isEmpty()) {
                return false;
            }
            // Its not enough just for the firewallStatus to be in `READY` and the ConfigurationSyncStateSummary to be
            // in `IN_SYNC` because firewall-policy association request is asynchronous. So, there is a
            // chance that when we call describe right after associateFirewallPolicy API, the work might not have
            // been started. So, the firewall status will be 'READY' and 'IN_SYNC' from the previous policy or
            // rule group updates. So, to confirm if the desired firewallPolicy got associated, we have to verify
            // the Config SyncStatus of this particular firewall policy ARN we are trying to associate here.

            // since same policy is associated to all subnets in a firewall, loop through all AZs under SyncStates key.
            for (final Map.Entry<String, SyncState> azSyncState : response.firewallStatus().syncStates().entrySet()) {
                final SyncState syncState = azSyncState.getValue();
                // check if the desired firewallPolicy ARN is part of the syncStates config Map.
                if (syncState.config().containsKey(desiredStateModel.getFirewallPolicyArn())) {
                    switch (syncState.config().get(desiredStateModel.getFirewallPolicyArn()).syncStatus()) {
                        case PENDING:
                            return false;
                        case IN_SYNC:
                            // continue to check the status in remaining AZs
                            continue;
                        default:
                            logger.log(String.format("Invalid/Unsupported syncState found while associating firewall"
                                    + "Policy:  %s", desiredStateModel.getFirewallPolicyArn()));
                            throw new CfnServiceInternalErrorException("FirewallPolicy failed to associate.");
                    }
                } else {
                    // desired firewallPolicy ARN is not yet added to SyncState, so still not stabilized
                    return false;
                }
            }

            // after confirming that firewallPolicy is associated to all subnets, check the firewallStatus and
            // configurationSyncStateSummary because this policy might have brought in new ruleGroups which also should
            // be in sync and that can be verified though the consolidated configurationSyncStateSummary.
            return response.firewallStatus().status() == FirewallStatusValue.READY &&
                    response.firewallStatus().configurationSyncStateSummary() == ConfigurationSyncState.IN_SYNC;
        } catch (final Exception e) {
            throw new CfnGeneralServiceException("FirewallPolicy failed to associate.");
        }
    }

    // tags on resource request overrides tags attached to the CloudFormation stack this resource belongs to
    private ProgressEvent<ResourceModel, CallbackContext> updateTags(ProgressEvent<ResourceModel, CallbackContext> progress) {
        final TagUtils tagUtils = new TagUtils(previousStateModel.getTags(), desiredStateModel.getTags(),
                handlerRequest.getPreviousResourceTags(), handlerRequest.getDesiredResourceTags());

        // Untag resource: get tags to remove from firewall resource
        if (tagUtils.tagsToRemove().size() > 0) {
            progress = proxy.initiate("Firewall::Update-UntagResource", proxyClient, progress.getResourceModel(), callbackContext)
                    .translateToServiceRequest(model ->
                            Translator.translateToUntagRequest(model, tagUtils.tagsToRemove().keySet()))
                    .makeServiceCall((request, client) ->
                            client.injectCredentialsAndInvokeV2(request, client.client()::untagResource))
                    .progress();

            if (progress.isFailed()) {
                return progress;
            }
        }

        // Tag resource: add tags that are added newly and update tags for which the value has been updated
        if (tagUtils.tagsToAddOrUpdate().size() > 0) {
            progress = proxy.initiate("Firewall::Update-TagResource", proxyClient, progress.getResourceModel(), callbackContext)
                    .translateToServiceRequest(model ->
                            Translator.translateToTagRequest(model, tagUtils.tagsToAddOrUpdate()))
                    .makeServiceCall((request, client) ->
                            client.injectCredentialsAndInvokeV2(request, client.client()::tagResource))
                    .progress();
        }

        return progress;
    }

    private enum SubnetOperation {
        ASSOCIATE,
        DISASSOCIATE
    }

    private boolean stabilizeSubnets(final ProxyClient<NetworkFirewallClient> client,
            final ResourceModel model, final CallbackContext callbackContext, final SubnetOperation operation) {
        try {
            final DescribeFirewallResponse response = client.injectCredentialsAndInvokeV2(
                    Translator.translateToDescribeFirewallRequest(model),
                    client.client()::describeFirewall);

            // get all subnets from SyncStates of current firewall
            Set<String> actualSubnets = new HashSet<>();
            for (final Map.Entry<String, SyncState> e : response.firewallStatus().syncStates().entrySet()) {
                final Attachment attachment = e.getValue().attachment();
                if (attachment.subnetId() != null) {
                    actualSubnets.add(attachment.subnetId());
                }
            }

            switch (operation) {
                case ASSOCIATE:
                    if (!Sets.intersection(actualSubnets, subnetsToAdd).equals(subnetsToAdd)) {
                        // some subnet we want added is still not associated, so return false.
                        return false;
                    }
                    break;
                case DISASSOCIATE:
                    if (!Sets.intersection(actualSubnets, subnetsToRemove).isEmpty()) {
                        // some subnet we want removed is still associated, so return false.
                        return false;
                    }
                    break;
            }

            // after confirming (Associate: subnets started CREATING/SCALING)|(Disassociate: subnets are disassociated),
            // check the firewallStatus and configurationSyncStateSummary because it shows a consolidated output
            // of all configs and attachments.
            return response.firewallStatus().status() == FirewallStatusValue.READY &&
                    response.firewallStatus().configurationSyncStateSummary() == ConfigurationSyncState.IN_SYNC;
        } catch (final Exception e) {
            throw new CfnGeneralServiceException("Subnets failed to associate");
        }
    }

    private boolean stabilizeAssociateSubnets(final AssociateSubnetsRequest awsRequest,
            final AssociateSubnetsResponse awsResponse, final ProxyClient<NetworkFirewallClient> client,
            final ResourceModel model, final CallbackContext callbackContext) {
        return stabilizeSubnets(client, model, callbackContext, SubnetOperation.ASSOCIATE);
    }

    private boolean stabilizeDisassociateSubnets(final DisassociateSubnetsRequest awsRequest,
            final DisassociateSubnetsResponse awsResponse, final ProxyClient<NetworkFirewallClient> client,
            final ResourceModel model, final CallbackContext callbackContext) {
        return stabilizeSubnets(client, model, callbackContext, SubnetOperation.DISASSOCIATE);
    }

    Set<String> computeSubnetsToAdd(@NonNull final Set<SubnetMapping> previousSubnets,
            @NonNull final Set<SubnetMapping> desiredSubnets) {
        TreeSet<SubnetMapping> desired = convertToTreeSet(desiredSubnets);
        TreeSet<SubnetMapping> previous = convertToTreeSet(previousSubnets);

        desired.removeAll(previous);

        return desired.stream().map(subnetMapping -> subnetMapping.getSubnetId()).collect(Collectors.toSet());
    }

    Set<String> computeSubnetsToRemove(@NonNull final Set<SubnetMapping> previousSubnets,
            @NonNull final Set<SubnetMapping> desiredSubnets) {
        TreeSet<SubnetMapping> desired = convertToTreeSet(desiredSubnets);
        TreeSet<SubnetMapping> previous = convertToTreeSet(previousSubnets);

        previous.removeAll(desired);

        return previous.stream().map(subnetMapping -> subnetMapping.getSubnetId()).collect(Collectors.toSet());
    }

    private TreeSet<SubnetMapping> convertToTreeSet(@NonNull final Set<SubnetMapping> subnetMappings) {
        TreeSet<SubnetMapping> treeSetSubnetMappings = new TreeSet<>(new SubnetMappingComparator());
        treeSetSubnetMappings.addAll(subnetMappings);

        return treeSetSubnetMappings;
    }
}

/**
 * Custom comparator to compare SubnetMapping items.
 */
class SubnetMappingComparator implements Comparator<SubnetMapping> {
    @Override
    public int compare(SubnetMapping s1, SubnetMapping s2) {
        return s1.getSubnetId().compareTo(s2.getSubnetId());
    }
}
