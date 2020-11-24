package software.amazon.networkfirewall.firewallpolicy;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.awssdk.services.networkfirewall.model.ListFirewallPoliciesRequest;
import software.amazon.awssdk.services.networkfirewall.model.ListFirewallPoliciesResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.OperationStatus;

import static software.amazon.networkfirewall.firewallpolicy.ExceptionTranslator.translateToCfnException;

public class ListHandler extends BaseHandlerStd {

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<NetworkFirewallClient> proxyClient,
        final Logger logger) {

        final ListFirewallPoliciesRequest listFirewallPoliciesRequest = Translator.translateToListRequest(request.getNextToken());

        ListFirewallPoliciesResponse listFirewallPoliciesResponse = null;

        try {
            listFirewallPoliciesResponse =
                    proxy.injectCredentialsAndInvokeV2(
                            listFirewallPoliciesRequest,
                            proxyClient.client()::listFirewallPolicies
                    );
        } catch (AwsServiceException e) {
            translateToCfnException(e);
        }

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(Translator.translateFromListRequest(listFirewallPoliciesResponse))
                .nextToken(listFirewallPoliciesResponse.nextToken())
                .status(OperationStatus.SUCCESS)
                .build();
    }
}
