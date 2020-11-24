package software.amazon.networkfirewall.rulegroup;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.awssdk.services.networkfirewall.model.CreateRuleGroupRequest;
import software.amazon.awssdk.services.networkfirewall.model.CreateRuleGroupResponse;
import software.amazon.awssdk.services.networkfirewall.model.DescribeRuleGroupRequest;
import software.amazon.awssdk.services.networkfirewall.model.DescribeRuleGroupResponse;
import software.amazon.awssdk.services.networkfirewall.model.InvalidRequestException;
import software.amazon.awssdk.services.networkfirewall.model.ResourceNotFoundException;
import software.amazon.awssdk.services.networkfirewall.model.ResourceStatus;
import software.amazon.awssdk.services.networkfirewall.model.UpdateRuleGroupRequest;
import software.amazon.awssdk.services.networkfirewall.model.UpdateRuleGroupResponse;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static software.amazon.networkfirewall.rulegroup.ExceptionTranslator.translateToCfnException;

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
                .then(this::verifyResourceExists)
                .then(this::updateRuleGroup)
                .then(this::updateTags)
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    private ProgressEvent<ResourceModel, CallbackContext> verifyResourceExists(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        return proxy.initiate(
                "RuleGroup::Update-ResourceExists", proxyClient, progress.getResourceModel(), callbackContext)
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((describeRuleGroupRequest, client) -> {
                    try {
                        DescribeRuleGroupResponse describeResponse = client.injectCredentialsAndInvokeV2(describeRuleGroupRequest, client.client()::describeRuleGroup);
                        final Integer actualCapacityFromCreate = describeResponse.ruleGroupResponse().capacity();
                        if (desiredStateModel.getCapacity() == null) {
                            desiredStateModel.setCapacity(actualCapacityFromCreate);
                        } else {
                            if (!actualCapacityFromCreate.equals(desiredStateModel.getCapacity())) {
                                throw new CfnInvalidRequestException("RuleGroup capacity cannot be updated.");
                            }
                        }
                    } catch (final ResourceNotFoundException e) {
                        throw new CfnNotFoundException(e);
                    } catch (final AwsServiceException e) {
                        translateToCfnException(e);
                    }
                    // resource we are trying to update exists, return success
                    return ProgressEvent.defaultSuccessHandler(null);
                })
                .progress();
    }

    private ProgressEvent<ResourceModel, CallbackContext>  updateRuleGroup(final ProgressEvent<ResourceModel, CallbackContext> progress) {
        return proxy.initiate("AWS-NetworkFirewall-RuleGroup::Update", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                .translateToServiceRequest((model) -> Translator.translateToUpdateRequest(model, getUpdateToken(proxyClient, model)))
                .makeServiceCall(this::updateRuleGroupServiceCall)
                .stabilize(this::isStabilized)
                .progress();
    }

    private UpdateRuleGroupResponse  updateRuleGroupServiceCall(final UpdateRuleGroupRequest updateRuleGroupRequest, final ProxyClient<NetworkFirewallClient> client) {
        final UpdateRuleGroupResponse response;
        try {
            response = client.injectCredentialsAndInvokeV2(updateRuleGroupRequest, client.client()::updateRuleGroup);
            // set the primaryIdentifier to be used in the tagging step
            desiredStateModel.setRuleGroupArn(response.ruleGroupResponse().ruleGroupArn());
        } catch (final AwsServiceException e) {
            throw translateToCfnException(e);
        }
        logger.log(String.format("%s successfully updated.", ResourceModel.TYPE_NAME));
        return response;
    }

    private String getUpdateToken(final ProxyClient<NetworkFirewallClient> proxyClient, final ResourceModel model) {
        final DescribeRuleGroupRequest describeRuleGroupRequest = DescribeRuleGroupRequest.builder()
                .ruleGroupArn(model.getRuleGroupArn())
                .ruleGroupName(model.getRuleGroupName())
                .type(model.getType())
                .build();
        final DescribeRuleGroupResponse response;
        try {
            response = proxyClient.injectCredentialsAndInvokeV2(describeRuleGroupRequest, proxyClient.client()::describeRuleGroup);
        } catch (final ResourceNotFoundException e) {
            throw new CfnNotFoundException(e);
        } catch (final AwsServiceException e) {
            throw translateToCfnException(e);
        }
        return response.updateToken();
    }

    private boolean isStabilized(final UpdateRuleGroupRequest updateRuleGroupRequest, final UpdateRuleGroupResponse updateRuleGroupResponse,
            final ProxyClient<NetworkFirewallClient> client, final ResourceModel model, final CallbackContext callbackContext ) {
        try {
            final DescribeRuleGroupResponse describeRuleGroupResponse = client.injectCredentialsAndInvokeV2(
                    Translator.translateToReadRequest(model),
                    client.client()::describeRuleGroup);
            final ResourceStatus status = describeRuleGroupResponse.ruleGroupResponse().ruleGroupStatus();
            final String ruleGroupArn = describeRuleGroupResponse.ruleGroupResponse().ruleGroupArn();
            switch (status) {
                case ACTIVE:
                    logger.log(String.format("%s : %s successfully updated.", ResourceModel.TYPE_NAME, ruleGroupArn));
                    return true;
                case DELETING:
                    logger.log(String.format("%s : %s marked for deletion.", ResourceModel.TYPE_NAME, ruleGroupArn));
                    throw new CfnGeneralServiceException(String.format("%s update failed.", ResourceModel.TYPE_NAME));
                default:
                    logger.log(String.format("Invalid/Unsupported RuleGroupStatus found while updating %s : %s",
                            ResourceModel.TYPE_NAME, ruleGroupArn));
                    throw new CfnGeneralServiceException(String.format("%s update failed.", ResourceModel.TYPE_NAME));
            }
        } catch (final AwsServiceException e) {
            throw new CfnGeneralServiceException(String.format("%s update failed.", ResourceModel.TYPE_NAME));
        }
    }

    // tags on resource request overrides tags attached to the CloudFormation stack this resource belongs to
    private ProgressEvent<ResourceModel, CallbackContext> updateTags(ProgressEvent<ResourceModel, CallbackContext> progress) {
        final TagUtils tagUtils = new TagUtils(previousStateModel.getTags(), desiredStateModel.getTags(),
                handlerRequest.getPreviousResourceTags(), handlerRequest.getDesiredResourceTags());

        // Untag resource: get tags to remove from firewall resource
        if (tagUtils.tagsToRemove().size() > 0) {
            progress = proxy.initiate("RuleGroup::Update-UntagResource", proxyClient, progress.getResourceModel(), callbackContext)
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
            progress = proxy.initiate("RuleGroup::Update-TagResource", proxyClient, progress.getResourceModel(), callbackContext)
                    .translateToServiceRequest(model ->
                            Translator.translateToTagRequest(model, tagUtils.tagsToAddOrUpdate()))
                    .makeServiceCall((request, client) ->
                            client.injectCredentialsAndInvokeV2(request, client.client()::tagResource))
                    .progress();
        }

        return progress;
    }
}
