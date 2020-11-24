package software.amazon.networkfirewall.rulegroup;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.awssdk.services.networkfirewall.model.DeleteRuleGroupRequest;
import software.amazon.awssdk.services.networkfirewall.model.DeleteRuleGroupResponse;
import software.amazon.awssdk.services.networkfirewall.model.ResourceNotFoundException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static software.amazon.networkfirewall.rulegroup.ExceptionTranslator.translateToCfnException;

public class DeleteHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<NetworkFirewallClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
            .then(progress ->
                proxy.initiate("AWS-NetworkFirewall-RuleGroup::Delete", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                        .translateToServiceRequest(Translator::translateToDeleteRequest)
                        .makeServiceCall(this::deleteRuleGroupServiceCall)
                        .stabilize(this::isDeleted)
                        .progress()
            )
            .then(progress -> ProgressEvent.defaultSuccessHandler(null));
    }

    private DeleteRuleGroupResponse deleteRuleGroupServiceCall(final DeleteRuleGroupRequest deleteRuleGroupRequest,
            final ProxyClient<NetworkFirewallClient> client) {
        final DeleteRuleGroupResponse response;
        try {
            response = client.injectCredentialsAndInvokeV2(deleteRuleGroupRequest, client.client()::deleteRuleGroup);
        } catch (final AwsServiceException e) {
            throw translateToCfnException(e);
        }
        logger.log(String.format("%s delete request made successfully.", ResourceModel.TYPE_NAME));
        return response;
    }

    private boolean isDeleted(final DeleteRuleGroupRequest deleteRuleGroupRequest, final DeleteRuleGroupResponse deleteRuleGroupResponse,
            final ProxyClient<NetworkFirewallClient> client, final ResourceModel model, final CallbackContext callbackContext) {
        final String ruleGroupIdentifier = model.getRuleGroupArn() != null ? model.getRuleGroupArn() : model.getRuleGroupName();
        try {
            client.injectCredentialsAndInvokeV2(Translator.translateToReadRequest(model), client.client()::describeRuleGroup);
        } catch (final ResourceNotFoundException e) {
            logger.log(String.format("%s : %s successfully deleted.", ResourceModel.TYPE_NAME, ruleGroupIdentifier));
            return true;
        } catch (final AwsServiceException e) {
            logger.log(String.format("Failed to delete %s : %s", ResourceModel.TYPE_NAME, ruleGroupIdentifier));
            throw new CfnGeneralServiceException(String.format("%s delete failed.", ResourceModel.TYPE_NAME));
        }
        return false;
    }
}
