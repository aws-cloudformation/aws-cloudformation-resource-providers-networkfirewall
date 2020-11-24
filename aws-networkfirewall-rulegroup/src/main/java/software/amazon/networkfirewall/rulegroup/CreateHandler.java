package software.amazon.networkfirewall.rulegroup;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.awssdk.services.networkfirewall.model.CreateRuleGroupRequest;
import software.amazon.awssdk.services.networkfirewall.model.CreateRuleGroupResponse;
import software.amazon.awssdk.services.networkfirewall.model.DescribeRuleGroupResponse;
import software.amazon.awssdk.services.networkfirewall.model.ResourceStatus;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static software.amazon.networkfirewall.rulegroup.ExceptionTranslator.translateToCfnException;

public class CreateHandler extends BaseHandlerStd {
    private Logger logger;
    private ResourceModel resourceModel;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<NetworkFirewallClient> proxyClient,
        final Logger logger) {

        this.logger = logger;
        this.resourceModel = request.getDesiredResourceState();

        // get tags from resource request and CFN stack.
        final TagUtils tagUtils = new TagUtils(null, request.getDesiredResourceState().getTags(),
                null, request.getDesiredResourceTags());

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
            .then(progress ->
                proxy.initiate("AWS-NetworkFirewall-RuleGroup::Create", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                        .translateToServiceRequest(model -> Translator.translateToCreateRequest(model, tagUtils.tagsToAddOrUpdate()))
                        .makeServiceCall(this::createRuleGroupServiceCall)
                        .stabilize(this::isCreated)
                        .progress()
                )
            .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    private CreateRuleGroupResponse createRuleGroupServiceCall(final CreateRuleGroupRequest createRuleGroupRequest,
            final ProxyClient<NetworkFirewallClient> client) {
        final CreateRuleGroupResponse response;
        try {
            response = client.injectCredentialsAndInvokeV2(createRuleGroupRequest, client.client()::createRuleGroup);
            // set the primaryIdentifier which is returned by the create-handler to be used in the stabilize step
            resourceModel.setRuleGroupArn(response.ruleGroupResponse().ruleGroupArn());
        } catch (final AwsServiceException e) {
            throw translateToCfnException(e);
        }
        logger.log(String.format("%s create request made successfully.", ResourceModel.TYPE_NAME));
        return response;
    }

    private boolean isCreated(final CreateRuleGroupRequest createRuleGroupRequest, final CreateRuleGroupResponse createRuleGroupResponse,
            final ProxyClient<NetworkFirewallClient> client, final ResourceModel model, final CallbackContext callbackContext ) {
        try {
            final DescribeRuleGroupResponse describeRuleGroupResponse = client.injectCredentialsAndInvokeV2(
                    Translator.translateToReadRequest(model),
                    client.client()::describeRuleGroup);
            final ResourceStatus status = describeRuleGroupResponse.ruleGroupResponse().ruleGroupStatus();
            final String ruleGroupArn = describeRuleGroupResponse.ruleGroupResponse().ruleGroupArn();
            switch (status) {
                case ACTIVE:
                    logger.log(String.format("%s : %s successfully created.", ResourceModel.TYPE_NAME, ruleGroupArn));
                    return true;
                case DELETING:
                    logger.log(String.format("%s : %s marked for deletion.", ResourceModel.TYPE_NAME, ruleGroupArn));
                    throw new CfnGeneralServiceException(String.format("%s create failed.", ResourceModel.TYPE_NAME));
                default:
                    logger.log(String.format("Invalid/Unsupported RuleGroupStatus found while creating %s : %s",
                            ResourceModel.TYPE_NAME, ruleGroupArn));
                    throw new CfnGeneralServiceException(String.format("%s create failed.", ResourceModel.TYPE_NAME));
            }
        } catch (final AwsServiceException e) {
            throw new CfnGeneralServiceException(String.format("%s create failed.", ResourceModel.TYPE_NAME));
        }
    }
}
