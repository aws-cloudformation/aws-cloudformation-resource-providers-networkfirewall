package software.amazon.networkfirewall.firewallpolicy;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.awssdk.services.networkfirewall.model.*;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static software.amazon.networkfirewall.firewallpolicy.ExceptionTranslator.translateToCfnException;

public class UpdateHandler extends BaseHandlerStd {
    private Logger logger;
    private AmazonWebServicesClientProxy proxy;
    private ResourceHandlerRequest<ResourceModel> handlerRequest;
    private CallbackContext callbackContext;
    private ProxyClient<NetworkFirewallClient> proxyClient;
    private ResourceModel desiredStateModel;
    private ResourceModel previousStateModel;

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
        this.desiredStateModel = request.getDesiredResourceState();
        this.previousStateModel = request.getPreviousResourceState();

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(this::updateFirewallPolicy)
                .then(this::updateTags)
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    private ProgressEvent<ResourceModel, CallbackContext> updateFirewallPolicy(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {

        return proxy.initiate(
                "AWS-NetworkFirewall-FirewallPolicy::Update", proxyClient, progress.getResourceModel(), callbackContext)
                .translateToServiceRequest((model) -> Translator.translateToUpdateRequest(model,getUpdateToken(proxyClient, model)))
                .makeServiceCall((updateFirewallPolicyRequest, proxyInvocation) -> {
                            {
                                UpdateFirewallPolicyResponse updateFirewallPolicyResponse = null;

                                try {
                                    updateFirewallPolicyResponse = proxyInvocation.injectCredentialsAndInvokeV2(updateFirewallPolicyRequest, proxyInvocation.client()::updateFirewallPolicy);
                                    logger.log(String.format("Firewall policy: %s has successfully been updated.", ResourceModel.TYPE_NAME));
                                } catch (final AwsServiceException e) {
                                    translateToCfnException(e);
                                }

                                return updateFirewallPolicyResponse;
                            }
                        })
                .stabilize(this::isStabilized)
                .progress();
    }

    // tags on resource request overrides tags attached to the CloudFormation stack this resource belongs to
    private ProgressEvent<ResourceModel, CallbackContext> updateTags(ProgressEvent<ResourceModel, CallbackContext> progress) {
        final TagUtils tagUtils = new TagUtils(previousStateModel.getTags(), desiredStateModel.getTags(),
                handlerRequest.getPreviousResourceTags(), handlerRequest.getDesiredResourceTags());
        // Untag resource: get tags to remove from firewall resource
        if (tagUtils.tagsToRemove().size() > 0) {
            progress = proxy.initiate("AWS-NetworkFirewall-FirewallPolicy::Update-UntagResource", proxyClient, progress.getResourceModel(), callbackContext)
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
            progress = proxy.initiate("AWS-NetworkFirewall-FirewallPolicy::Update-TagResource", proxyClient, progress.getResourceModel(), callbackContext)
                    .translateToServiceRequest(model ->
                            Translator.translateToTagRequest(model, tagUtils.tagsToAddOrUpdate()))
                    .makeServiceCall((request, client) ->
                            client.injectCredentialsAndInvokeV2(request, client.client()::tagResource))
                    .progress();
        }

        return progress;
    }

    private String getUpdateToken(final ProxyClient<NetworkFirewallClient> proxyClient, final ResourceModel model) {
        final DescribeFirewallPolicyRequest describeRuleGroupRequest = DescribeFirewallPolicyRequest.builder()
                .firewallPolicyArn(model.getFirewallPolicyArn())
                .firewallPolicyName(model.getFirewallPolicyName())
                .build();
        DescribeFirewallPolicyResponse response = null;
        try {
            response = proxyClient.injectCredentialsAndInvokeV2(describeRuleGroupRequest, proxyClient.client()::describeFirewallPolicy);
        } catch (final AwsServiceException e) {
            translateToCfnException(e);
        }

        if (response == null) {
            throw new CfnGeneralServiceException(String
                    .format("Firewall policy: %s is unable to update", ResourceModel.TYPE_NAME));
        }
        return response.updateToken();
    }

    private boolean isStabilized(final UpdateFirewallPolicyRequest updateFirewallPolicyRequest,
                                                  final UpdateFirewallPolicyResponse updateFirewallPolicyResponse, final ProxyClient<NetworkFirewallClient> client,
                                                  final ResourceModel model, final CallbackContext callbackContext) {
        try {

            final DescribeFirewallPolicyResponse response = client.injectCredentialsAndInvokeV2(
                    Translator.translateToReadRequest(model),
                    client.client()::describeFirewallPolicy);
            return isStabilized(response.firewallPolicyResponse().firewallPolicyStatus());
        } catch (final AwsServiceException e) {
            translateToCfnException(e);
        }
        return false;
    }

    private Boolean isStabilized(ResourceStatus status) {
        switch (status) {
            case ACTIVE:
                return true;
            case DELETING:
                throw new CfnGeneralServiceException(
                        String
                                .format("Firewall policy: %s is marked as Deleting", ResourceModel.TYPE_NAME));
            default:
                throw new CfnGeneralServiceException(
                        String
                                .format("Firewall policy: %s update request accepted but current status is unknown", ResourceModel.TYPE_NAME));
        }
    }
}
