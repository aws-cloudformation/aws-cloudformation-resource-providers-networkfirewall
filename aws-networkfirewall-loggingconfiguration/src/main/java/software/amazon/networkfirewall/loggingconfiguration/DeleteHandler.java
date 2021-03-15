package software.amazon.networkfirewall.loggingconfiguration;

import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.awssdk.services.networkfirewall.model.InternalServerErrorException;
import software.amazon.awssdk.services.networkfirewall.model.InvalidRequestException;
import software.amazon.awssdk.services.networkfirewall.model.LogDestinationPermissionException;
import software.amazon.awssdk.services.networkfirewall.model.ResourceNotFoundException;
import software.amazon.awssdk.services.networkfirewall.model.ThrottlingException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static software.amazon.networkfirewall.loggingconfiguration.Utils.convertTemplateToUpdateLoggingConfigurationCall;

public class DeleteHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<NetworkFirewallClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();

        Utils.validateResourceExists(Translator.translateToReadRequest(model), proxyClient);

        try {
            convertTemplateToUpdateLoggingConfigurationCall(model, proxyClient, true, false);

            Utils.stablize(proxyClient, model);
            return ProgressEvent.defaultSuccessHandler(null);
        } catch(InvalidRequestException e){
            throw new CfnInvalidRequestException(e);
        } catch (InternalServerErrorException | LogDestinationPermissionException | InterruptedException e) {
            throw new CfnServiceInternalErrorException(ResourceModel.TYPE_NAME, e);
        } catch (ResourceNotFoundException e) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, e.toString());
        } catch (ThrottlingException e) {
            throw new CfnThrottlingException(ResourceModel.TYPE_NAME, e);
        }
    }
}
