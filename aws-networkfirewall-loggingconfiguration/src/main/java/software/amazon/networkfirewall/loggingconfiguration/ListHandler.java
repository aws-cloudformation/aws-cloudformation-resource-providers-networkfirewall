package software.amazon.networkfirewall.loggingconfiguration;

import com.amazonaws.util.CollectionUtils;
import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.awssdk.services.networkfirewall.model.DescribeLoggingConfigurationRequest;
import software.amazon.awssdk.services.networkfirewall.model.DescribeLoggingConfigurationResponse;
import software.amazon.awssdk.services.networkfirewall.model.InternalServerErrorException;
import software.amazon.awssdk.services.networkfirewall.model.InvalidRequestException;
import software.amazon.awssdk.services.networkfirewall.model.ResourceNotFoundException;
import software.amazon.awssdk.services.networkfirewall.model.ThrottlingException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Collections;


public class ListHandler extends BaseHandlerStd {
    private Logger logger;

    // A list handler MUST return an array of primary identifiers.
    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<NetworkFirewallClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        final ResourceModel model = request.getDesiredResourceState();

        Utils.describeLoggingConfigurationCall(Translator.translateToReadRequest(model), proxyClient);

        final DescribeLoggingConfigurationRequest describeLoggingConfigurationRequest = Translator.translateToReadRequest(model);
        DescribeLoggingConfigurationResponse describeLoggingConfigurationResponse;
        try {
            describeLoggingConfigurationResponse = proxyClient.injectCredentialsAndInvokeV2(
                    describeLoggingConfigurationRequest, proxyClient.client()::describeLoggingConfiguration);
        } catch (ResourceNotFoundException e) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, e.toString());
        } catch (InvalidRequestException e) {
            throw new CfnInvalidRequestException(describeLoggingConfigurationRequest.toString(), e);
        } catch (InternalServerErrorException e) {
            throw new CfnServiceInternalErrorException(ResourceModel.TYPE_NAME, e);
        } catch (ThrottlingException e) {
            throw new CfnThrottlingException(ResourceModel.TYPE_NAME, e);
        }

        if (describeLoggingConfigurationResponse.loggingConfiguration() == null ||
                CollectionUtils.isNullOrEmpty(describeLoggingConfigurationResponse.loggingConfiguration().logDestinationConfigs())) {
            return constructResourceModelFromResponse();
        }

        return constructResourceModelFromResponse(describeLoggingConfigurationResponse);
    }

    private ProgressEvent<ResourceModel, CallbackContext> constructResourceModelFromResponse(
            final DescribeLoggingConfigurationResponse describeLoggingConfigurationResponse) {
        ResourceModel resourceModel =
                Translator.translateFromReadResponse(describeLoggingConfigurationResponse);
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(Collections.singletonList(resourceModel))
                .nextToken(null)
                .status(OperationStatus.SUCCESS)
                .build();
    }

    private ProgressEvent<ResourceModel, CallbackContext> constructResourceModelFromResponse() {
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(Collections.emptyList())
                .nextToken(null)
                .status(OperationStatus.SUCCESS)
                .build();
    }
}
