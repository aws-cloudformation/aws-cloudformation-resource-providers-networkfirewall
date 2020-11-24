package software.amazon.networkfirewall.firewall;

import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.awssdk.services.networkfirewall.model.DescribeFirewallResponse;
import software.amazon.awssdk.services.networkfirewall.model.InternalServerErrorException;
import software.amazon.awssdk.services.networkfirewall.model.InvalidRequestException;
import software.amazon.awssdk.services.networkfirewall.model.ResourceNotFoundException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ReadHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<NetworkFirewallClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        return proxy.initiate("AWS-NetworkFirewall-Firewall::Read", proxyClient, request.getDesiredResourceState(), callbackContext)
            .translateToServiceRequest(Translator::translateToDescribeFirewallRequest)
            .makeServiceCall((describeFirewallRequest, client) -> {
                DescribeFirewallResponse response;
                try {
                    response = client.injectCredentialsAndInvokeV2(describeFirewallRequest, client.client()::describeFirewall);
                } catch (final ResourceNotFoundException e) {
                    throw new CfnNotFoundException(e);
                } catch (final InvalidRequestException e) {
                    throw new CfnInvalidRequestException(e.getMessage(), e);
                } catch (final InternalServerErrorException e) {
                    throw new CfnServiceInternalErrorException(e.getMessage(), e);
                } catch (final Exception e) {
                    throw new CfnGeneralServiceException(e.getMessage(), e);
                }

                return response;
            })
            .done(response -> ProgressEvent.defaultSuccessHandler(Translator.translateFromDescribeFirewallResponse(response)));
    }
}
