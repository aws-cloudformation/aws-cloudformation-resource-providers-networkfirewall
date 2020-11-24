package software.amazon.networkfirewall.firewall;

import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.awssdk.services.networkfirewall.model.DeleteFirewallRequest;
import software.amazon.awssdk.services.networkfirewall.model.DeleteFirewallResponse;
import software.amazon.awssdk.services.networkfirewall.model.InternalServerErrorException;
import software.amazon.awssdk.services.networkfirewall.model.InvalidOperationException;
import software.amazon.awssdk.services.networkfirewall.model.InvalidRequestException;
import software.amazon.awssdk.services.networkfirewall.model.ResourceNotFoundException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

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
                proxy.initiate("AWS-NetworkFirewall-Firewall::Delete", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(Translator::translateToDeleteRequest)
                    .makeServiceCall(this::submitDeleteFirewallCall)
                    .stabilize(this::isDeleted)
                    .progress()
            )
            .then(progress -> ProgressEvent.defaultSuccessHandler(null));
    }

    private DeleteFirewallResponse submitDeleteFirewallCall(final DeleteFirewallRequest request,
            final ProxyClient<NetworkFirewallClient> client) {
        DeleteFirewallResponse response;
        try {
            response = client.injectCredentialsAndInvokeV2(request, client.client()::deleteFirewall);
        } catch (final ResourceNotFoundException e) {
            throw new CfnNotFoundException(e);
        } catch (final InvalidRequestException e) {
            throw new CfnInvalidRequestException(e.getMessage(), e);
        } catch (final InvalidOperationException e) {
            throw new CfnInvalidRequestException(e.getMessage(), e);
        } catch (final InternalServerErrorException e) {
            throw new CfnServiceInternalErrorException(e.getMessage(), e);
        }

        logger.log(String.format("%s delete submitted successfully.", ResourceModel.TYPE_NAME));
        return response;
    }

    private boolean isDeleted(final DeleteFirewallRequest awsRequest, final DeleteFirewallResponse awsResponse,
            final ProxyClient<NetworkFirewallClient> client, final ResourceModel model, final CallbackContext callbackContext) {
        try {
            client.injectCredentialsAndInvokeV2(
                    Translator.translateToDescribeFirewallRequest(model), client.client()::describeFirewall);
        } catch (final ResourceNotFoundException e) {
            return true;
        }

        // resource still exists and not deleted.
        return false;
    }
}
