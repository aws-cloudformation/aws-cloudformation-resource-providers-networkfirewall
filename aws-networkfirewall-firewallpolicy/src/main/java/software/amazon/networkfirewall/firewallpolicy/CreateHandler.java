package software.amazon.networkfirewall.firewallpolicy;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.awssdk.services.networkfirewall.model.*;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static software.amazon.networkfirewall.firewallpolicy.ExceptionTranslator.translateToCfnException;

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

        final TagUtils tagUtils = new TagUtils(null, request.getDesiredResourceState().getTags(), null, request.getDesiredResourceTags());
        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
            .then(progress ->
                proxy.initiate("AWS-NetworkFirewall-FirewallPolicy::Create", proxyClient,progress.getResourceModel(), progress.getCallbackContext())
                        .translateToServiceRequest(model -> Translator.translateToCreateRequest(model, tagUtils.tagsToAddOrUpdate()))
                        .makeServiceCall(this::createFirewallPolicy)
                        .stabilize(this::isStabilized)
                        .progress()
            )
            .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    private CreateFirewallPolicyResponse createFirewallPolicy (CreateFirewallPolicyRequest createFirewallPolicyRequest, final ProxyClient<NetworkFirewallClient> proxyClient) {
        CreateFirewallPolicyResponse createFirewallPolicyResponse = null;
        try {
            createFirewallPolicyResponse = proxyClient.injectCredentialsAndInvokeV2(createFirewallPolicyRequest, proxyClient.client()::createFirewallPolicy);
            logger.log(String.format("Firewall policy: %s successfully created.", ResourceModel.TYPE_NAME));
            resourceModel.setFirewallPolicyArn(createFirewallPolicyResponse.firewallPolicyResponse().firewallPolicyArn());
        } catch (final AwsServiceException e) {
            translateToCfnException(e);
        }
        return createFirewallPolicyResponse;
    }

    private boolean isStabilized(final CreateFirewallPolicyRequest createFirewallPolicyRequest,
                                 final CreateFirewallPolicyResponse createFirewallPolicyResponse, final ProxyClient<NetworkFirewallClient> proxyClient,
                                 final ResourceModel model, final CallbackContext callbackContext) {
        final NetworkFirewallClient networkFirewallClient = proxyClient.client();
        final DescribeFirewallPolicyRequest describeFirewallPolicyRequest = DescribeFirewallPolicyRequest
                .builder()
                .firewallPolicyArn(model.getFirewallPolicyArn())
                .firewallPolicyName(model.getFirewallPolicyName())
                .build();
        final DescribeFirewallPolicyResponse describeFirewallPolicyResponse =
                proxyClient.injectCredentialsAndInvokeV2(
                        describeFirewallPolicyRequest, networkFirewallClient::describeFirewallPolicy);
        return isStabilized(describeFirewallPolicyResponse.firewallPolicyResponse().firewallPolicyStatus());
    }

    private Boolean isStabilized(ResourceStatus status) {
        switch (status) {
            case ACTIVE:
                return true;
            case DELETING:
                throw new CfnGeneralServiceException(
                        String
                                .format("Firewall policy: %s is marked as Deleting", ResourceModel.TYPE_NAME));
            default:
                throw new CfnGeneralServiceException(
                        String
                                .format("Firewall policy: %s creation request accepted but current status is unknown", ResourceModel.TYPE_NAME));
        }
    }
}
