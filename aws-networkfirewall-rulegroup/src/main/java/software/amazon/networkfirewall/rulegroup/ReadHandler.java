package software.amazon.networkfirewall.rulegroup;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.awssdk.services.networkfirewall.model.DescribeRuleGroupResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static software.amazon.networkfirewall.rulegroup.ExceptionTranslator.translateToCfnException;

public class ReadHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<NetworkFirewallClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        return proxy.initiate("AWS-NetworkFirewall-RuleGroup::Read", proxyClient, request.getDesiredResourceState(), callbackContext)
            .translateToServiceRequest(Translator::translateToReadRequest)
            .makeServiceCall((describeRuleGroupRequest, client) -> {
                final DescribeRuleGroupResponse response;
                try {
                    response = client.injectCredentialsAndInvokeV2(describeRuleGroupRequest, client.client()::describeRuleGroup);
                } catch (final AwsServiceException e) {
                    throw translateToCfnException(e);
                }
                logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
                return response;
            })
            .done(awsResponse -> ProgressEvent.defaultSuccessHandler(Translator.translateFromReadResponse(awsResponse)));
    }
}
