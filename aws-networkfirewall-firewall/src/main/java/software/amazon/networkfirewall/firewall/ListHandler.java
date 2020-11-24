package software.amazon.networkfirewall.firewall;

import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.awssdk.services.networkfirewall.model.ListFirewallsRequest;
import software.amazon.awssdk.services.networkfirewall.model.ListFirewallsResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ListHandler extends BaseHandlerStd {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<NetworkFirewallClient> proxyClient,
        final Logger logger) {

        final ListFirewallsRequest listFirewallsRequest = Translator.translateToListRequest(request.getNextToken());
        final ListFirewallsResponse response = proxy.injectCredentialsAndInvokeV2(listFirewallsRequest, proxyClient.client()::listFirewalls);

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
            .resourceModels(Translator.translateFromListRequest(response))
            .nextToken(response.nextToken())
            .status(OperationStatus.SUCCESS)
            .build();
    }
}
