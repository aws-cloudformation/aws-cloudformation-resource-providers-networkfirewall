package software.amazon.networkfirewall.firewallpolicy;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.awssdk.services.networkfirewall.model.DeleteFirewallPolicyRequest;
import software.amazon.awssdk.services.networkfirewall.model.DeleteFirewallPolicyResponse;
import software.amazon.awssdk.services.networkfirewall.model.DescribeFirewallPolicyRequest;
import software.amazon.awssdk.services.networkfirewall.model.ResourceNotFoundException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static software.amazon.networkfirewall.firewallpolicy.ExceptionTranslator.translateToCfnException;

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
                proxy.initiate("AWS-NetworkFirewall-FirewallPolicy::Delete", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                        .translateToServiceRequest(Translator::translateToDeleteRequest)
                        .makeServiceCall(this::deleteFirewallPolicy)
                        .stabilize(this::isDeleteStabilized)
                        .progress())
                .then(progress -> ProgressEvent.defaultSuccessHandler(null));
    }

    /**
     * @param deleteFirewallPolicyRequest {@link DeleteFirewallPolicyRequest}.
     * @param client                  ProxyClient
     * @return DeleteSchemaVersionsResponse
     */
    private DeleteFirewallPolicyResponse deleteFirewallPolicy(
            final DeleteFirewallPolicyRequest deleteFirewallPolicyRequest,
            final ProxyClient<NetworkFirewallClient> client) {

        final NetworkFirewallClient networkFirewallClient = client.client();

        DeleteFirewallPolicyResponse deleteFirewallPolicyResponse = null;
        try {

            deleteFirewallPolicyResponse =
                    client.injectCredentialsAndInvokeV2(deleteFirewallPolicyRequest, networkFirewallClient::deleteFirewallPolicy);
            logger.log(
                    String.format(
                            "Firewall policy: %s is successfully deleted.",
                            ResourceModel.TYPE_NAME
                    )
            );
        }
        catch (final AwsServiceException e) {
            translateToCfnException(e);
        }

        return deleteFirewallPolicyResponse;
    }

    private Boolean isDeleteStabilized(
            final DeleteFirewallPolicyRequest deleteFirewallPolicyRequest,
            final DeleteFirewallPolicyResponse deleteFirewallPolicyResponse,
            final ProxyClient<NetworkFirewallClient> proxyClient,
            final ResourceModel resourceModel,
            final CallbackContext callbackContext) {
        try {
            final NetworkFirewallClient networkFirewallClient = proxyClient.client();
            DescribeFirewallPolicyRequest describeFirewallPolicyRequest = DescribeFirewallPolicyRequest.builder()
                    .firewallPolicyName(deleteFirewallPolicyRequest.firewallPolicyName())
                    .firewallPolicyArn(deleteFirewallPolicyRequest.firewallPolicyArn())
                    .build();
            proxyClient
                    .injectCredentialsAndInvokeV2(
                            describeFirewallPolicyRequest,
                            networkFirewallClient::describeFirewallPolicy
                    );

            logger.log(
                    String.format("Firewall policy: %s is not deleted yet",
                            ResourceModel.TYPE_NAME
                    )
            );
            return false;
        } catch (final ResourceNotFoundException e) {
            return true;
        } catch (final AwsServiceException e) {
            throw new CfnGeneralServiceException(
                    String.format("Firewall policy: %s deletion status couldn't be retrieved: %s",
                            ResourceModel.TYPE_NAME,
                            e.getMessage()),
                    e);
        }
    }
}
