package software.amazon.networkfirewall.rulegroup;

import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.awssdk.services.networkfirewall.model.ListRuleGroupsRequest;
import software.amazon.awssdk.services.networkfirewall.model.ListRuleGroupsResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;

public class ListHandler extends BaseHandlerStd {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<NetworkFirewallClient> proxyClient,
            final Logger logger) {

        final ListRuleGroupsRequest listRuleGroupsRequest = Translator.translateToListRequest(request.getNextToken());
        final ListRuleGroupsResponse response = proxy.injectCredentialsAndInvokeV2(listRuleGroupsRequest, proxyClient.client()::listRuleGroups);

        final List<ResourceModel> models = Translator.translateFromListRequest(response);

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(models)
                .nextToken(response.nextToken())
                .status(OperationStatus.SUCCESS)
                .build();
    }
}
