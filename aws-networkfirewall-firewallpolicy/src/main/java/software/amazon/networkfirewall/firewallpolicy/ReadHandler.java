package software.amazon.networkfirewall.firewallpolicy;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.awssdk.services.networkfirewall.model.DescribeFirewallPolicyRequest;
import software.amazon.awssdk.services.networkfirewall.model.DescribeFirewallPolicyResponse;
import software.amazon.awssdk.services.networkfirewall.model.InvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static software.amazon.networkfirewall.firewallpolicy.ExceptionTranslator.translateToCfnException;

public class ReadHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<NetworkFirewallClient> proxyClient,
        final Logger logger) {

        this.logger = logger;
        return proxy.initiate("AWS-NetworkFirewall-FirewallPolicy::Read", proxyClient, request.getDesiredResourceState(), callbackContext)
            .translateToServiceRequest(Translator::translateToReadRequest)
            .makeServiceCall(this::describeFirewallPolicy)
            .done(describeFirewallPolicyResponse -> ProgressEvent.defaultSuccessHandler(Translator.translateFromReadResponse(describeFirewallPolicyResponse)));
    }

    private DescribeFirewallPolicyResponse describeFirewallPolicy(
            final DescribeFirewallPolicyRequest describeFirewallPolicyRequest,
            final ProxyClient<NetworkFirewallClient> client) {

        final NetworkFirewallClient networkFirewallClient = client.client();

        DescribeFirewallPolicyResponse describeFirewallPolicyResponse = null;
        try {

            describeFirewallPolicyResponse =
                    client.injectCredentialsAndInvokeV2(describeFirewallPolicyRequest, networkFirewallClient::describeFirewallPolicy);
            logger.log(
                    String.format(
                            "Firewall policy: %s is successfully described.",
                            ResourceModel.TYPE_NAME
                    )
            );
        } catch (final InvalidRequestException e) {
            throw new CfnNotFoundException(e);
        }
        catch (final AwsServiceException e) {
            translateToCfnException(e);
        }

        return describeFirewallPolicyResponse;
    }
}
