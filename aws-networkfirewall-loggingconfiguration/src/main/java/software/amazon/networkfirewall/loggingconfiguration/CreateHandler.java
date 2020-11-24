package software.amazon.networkfirewall.loggingconfiguration;

import com.amazonaws.util.CollectionUtils;

import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.awssdk.services.networkfirewall.model.DescribeLoggingConfigurationRequest;
import software.amazon.awssdk.services.networkfirewall.model.DescribeLoggingConfigurationResponse;
import software.amazon.awssdk.services.networkfirewall.model.InternalServerErrorException;
import software.amazon.awssdk.services.networkfirewall.model.InvalidRequestException;
import software.amazon.awssdk.services.networkfirewall.model.LogDestinationPermissionException;
import software.amazon.awssdk.services.networkfirewall.model.ResourceNotFoundException;
import software.amazon.awssdk.services.networkfirewall.model.ThrottlingException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static software.amazon.networkfirewall.loggingconfiguration.Utils.convertTemplateToUpdateLoggingConfigurationCall;
import static software.amazon.networkfirewall.loggingconfiguration.Utils.validateInputModel;


public class CreateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<NetworkFirewallClient> proxyClient,
        final Logger logger) {

        this.logger = logger;
        final ResourceModel model = request.getDesiredResourceState();

        Utils.validateResourceNotExists(Translator.translateToReadRequest(model), proxyClient);
        validateInputModel(model);

        try {
            convertTemplateToUpdateLoggingConfigurationCall(model, proxyClient, false, true);
            return new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger);
        } catch(InvalidRequestException e){
            throw new CfnInvalidRequestException(e);
        } catch (InternalServerErrorException | LogDestinationPermissionException | ResourceNotFoundException e) {
            throw new CfnServiceInternalErrorException(ResourceModel.TYPE_NAME, e);
        } catch (ThrottlingException e) {
            throw new CfnThrottlingException(ResourceModel.TYPE_NAME, e);
        }
    }
}
