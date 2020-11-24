package software.amazon.networkfirewall.firewall;

import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.awssdk.services.networkfirewall.model.ConfigurationSyncState;
import software.amazon.awssdk.services.networkfirewall.model.CreateFirewallRequest;
import software.amazon.awssdk.services.networkfirewall.model.CreateFirewallResponse;
import software.amazon.awssdk.services.networkfirewall.model.DescribeFirewallResponse;
import software.amazon.awssdk.services.networkfirewall.model.InsufficientCapacityException;
import software.amazon.awssdk.services.networkfirewall.model.InternalServerErrorException;
import software.amazon.awssdk.services.networkfirewall.model.InvalidOperationException;
import software.amazon.awssdk.services.networkfirewall.model.InvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

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

        // get tags from resource request and CFN stack.
        final TagUtils tagUtils = new TagUtils(null, request.getDesiredResourceState().getTags(),
                null, request.getDesiredResourceTags());

        return ProgressEvent.progress(resourceModel, callbackContext)
            .then(progress ->
                proxy.initiate("AWS-NetworkFirewall-Firewall::Create", proxyClient, progress.getResourceModel(), callbackContext)
                    .translateToServiceRequest(model -> Translator.translateToCreateRequest(model, tagUtils.tagsToAddOrUpdate()))
                    .makeServiceCall(this::submitCreateFirewallCall)
                    .stabilize(this::isCreated)
                    .progress())
            .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    private CreateFirewallResponse submitCreateFirewallCall(final CreateFirewallRequest request,
            final ProxyClient<NetworkFirewallClient> client) {
        CreateFirewallResponse response;
        try {
            response = client.injectCredentialsAndInvokeV2(request, client.client()::createFirewall);

            // set the primaryIdentifier which is returned by the created handler
            resourceModel.setFirewallArn(response.firewall().firewallArn());
            resourceModel.setFirewallId(response.firewall().firewallId()); // also, set firewallId
        } catch (final InvalidRequestException e) {
            if (e.getMessage() != null && e.getMessage().contains("A resource with the specified name already exists")) {
                throw new CfnAlreadyExistsException(ResourceModel.TYPE_NAME, e.getMessage(), e);
            }
            throw new CfnInvalidRequestException(e.getMessage(), e);
        } catch (final InvalidOperationException e) {
            throw new CfnInvalidRequestException(e.getMessage(), e);
        } catch (final InsufficientCapacityException | InternalServerErrorException e) {
            throw new CfnServiceInternalErrorException(e.getMessage(), e);
        }
        return response;
    }

    private Boolean isCreated(CreateFirewallRequest request, CreateFirewallResponse createResponse,
            ProxyClient<NetworkFirewallClient> client, ResourceModel model,
            CallbackContext callbackContext) {
        try {
            final DescribeFirewallResponse response = client.injectCredentialsAndInvokeV2(
                    Translator.translateToDescribeFirewallRequest(model),
                    client.client()::describeFirewall);

            switch (response.firewallStatus().status()) {
                case READY:
                    return response.firewallStatus().configurationSyncStateSummary() == ConfigurationSyncState.IN_SYNC;
                case PROVISIONING:
                    return false;
                case DELETING:
                    logger.log(String.format("Firewall '%s' is in DELETING status during create workflow.",
                            model.getFirewallArn()));
                    throw new CfnGeneralServiceException("Firewall failed to create.");
                default:
                    logger.log(String.format("Invalid/Unsupported firewallStatus found while creating firewall %s",
                            model.getFirewallArn()));
                    throw new CfnGeneralServiceException("Firewall failed to create");
            }
        } catch (final Exception e) {
            throw new CfnGeneralServiceException("Firewall failed to create");
        }
    }
}
