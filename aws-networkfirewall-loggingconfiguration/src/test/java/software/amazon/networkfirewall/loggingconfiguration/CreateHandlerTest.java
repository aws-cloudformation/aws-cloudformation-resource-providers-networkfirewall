package software.amazon.networkfirewall.loggingconfiguration;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;

import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.awssdk.services.networkfirewall.model.DescribeLoggingConfigurationRequest;
import software.amazon.awssdk.services.networkfirewall.model.DescribeLoggingConfigurationResponse;
import software.amazon.awssdk.services.networkfirewall.model.InvalidRequestException;
import software.amazon.awssdk.services.networkfirewall.model.UpdateLoggingConfigurationRequest;
import software.amazon.awssdk.services.networkfirewall.model.UpdateLoggingConfigurationResponse;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static software.amazon.networkfirewall.loggingconfiguration.Translator.toSdkLoggingConfiguration;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<NetworkFirewallClient> proxyClient;

    @Mock
    NetworkFirewallClient networkFirewallClient;

    private CreateHandler handler;
    private ResourceModel model;

    @BeforeEach
    public void setup() {
        handler = new CreateHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        networkFirewallClient = mock(NetworkFirewallClient.class);
        proxyClient = MOCK_PROXY(proxy, networkFirewallClient);
    }

    @AfterEach
    public void tear_down() {
        verifyNoMoreInteractions(networkFirewallClient);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        LogDestinationConfig config1 = buildLogDestinationConfig("FLOW", "S3");
        model = buildResourceModel(buildLoggingConfiguration(Collections.singletonList(config1)));

        final DescribeLoggingConfigurationResponse preCheckLoggingConfigurationResponse = DescribeLoggingConfigurationResponse.builder()
                .firewallArn(firewallArn)
                .build();

        final DescribeLoggingConfigurationResponse finalLoggingConfigurationResponse = DescribeLoggingConfigurationResponse.builder()
                .firewallArn(firewallArn)
                .loggingConfiguration(toSdkLoggingConfiguration(buildLoggingConfiguration(Collections.singletonList(config1))))
                .build();

        final UpdateLoggingConfigurationResponse updateLoggingConfigurationResponse = UpdateLoggingConfigurationResponse.builder()
                .loggingConfiguration(toSdkLoggingConfiguration(buildLoggingConfiguration(Collections.singletonList(config1))))
                .firewallArn(firewallArn)
                .firewallName(firewallName)
                .build();

        when(proxyClient.client().describeLoggingConfiguration(any(DescribeLoggingConfigurationRequest.class)))
                .thenReturn(preCheckLoggingConfigurationResponse)
                .thenReturn(finalLoggingConfigurationResponse);

        when(proxyClient.client().updateLoggingConfiguration(any(UpdateLoggingConfigurationRequest.class)))
            .thenReturn(updateLoggingConfigurationResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel().getLoggingConfiguration()).isEqualTo(request.getDesiredResourceState().getLoggingConfiguration());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client(), times(1)).updateLoggingConfiguration(any(UpdateLoggingConfigurationRequest.class));
    }

    @Test
    public void handleRequest_SimpleSuccessWith2LogDestinationConfig() {
        LogDestinationConfig config1 = buildLogDestinationConfig("FLOW", "S3");
        LogDestinationConfig config2 = buildLogDestinationConfig("ALERT", "CloudWatchLogs");
        model = buildResourceModel(buildLoggingConfiguration(Arrays.asList(config1, config2)));

        final DescribeLoggingConfigurationResponse preCheckLoggingConfigurationResponse = DescribeLoggingConfigurationResponse.builder()
                .firewallArn(firewallArn)
                .build();

        final DescribeLoggingConfigurationResponse finalLoggingConfigurationResponse = DescribeLoggingConfigurationResponse.builder()
                .firewallArn(firewallArn)
                .loggingConfiguration(toSdkLoggingConfiguration(buildLoggingConfiguration(Arrays.asList(config1, config2))))
                .build();

        final UpdateLoggingConfigurationResponse updateLoggingConfigurationResponse = UpdateLoggingConfigurationResponse.builder()
                .loggingConfiguration(toSdkLoggingConfiguration(buildLoggingConfiguration(Arrays.asList(config1, config2))))
                .firewallArn(firewallArn)
                .firewallName(firewallName)
                .build();

        when(proxyClient.client().describeLoggingConfiguration(any(DescribeLoggingConfigurationRequest.class)))
                .thenReturn(preCheckLoggingConfigurationResponse)
                .thenReturn(finalLoggingConfigurationResponse);

        when(proxyClient.client().updateLoggingConfiguration(any(UpdateLoggingConfigurationRequest.class)))
                .thenReturn(updateLoggingConfigurationResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel().getLoggingConfiguration()).isEqualTo(request.getDesiredResourceState().getLoggingConfiguration());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client(), times(2)).updateLoggingConfiguration(any(UpdateLoggingConfigurationRequest.class));
    }

    @Test
    public void handleRequest_SimpleSuccessWith2LogDestinationConfig_InTheOtherOrder() {
        LogDestinationConfig config1 = buildLogDestinationConfig("FLOW", "S3");
        LogDestinationConfig config2 = buildLogDestinationConfig("ALERT", "CloudWatchLogs");
        model = buildResourceModel(buildLoggingConfiguration(Arrays.asList(config1, config2)));

        final DescribeLoggingConfigurationResponse preCheckLoggingConfigurationResponse = DescribeLoggingConfigurationResponse.builder()
                .firewallArn(firewallArn)
                .build();

        final DescribeLoggingConfigurationResponse finalLoggingConfigurationResponse = DescribeLoggingConfigurationResponse.builder()
                .firewallArn(firewallArn)
                .loggingConfiguration(toSdkLoggingConfiguration(buildLoggingConfiguration(Arrays.asList(config2, config1))))
                .build();

        final UpdateLoggingConfigurationResponse updateLoggingConfigurationResponse = UpdateLoggingConfigurationResponse.builder()
                .loggingConfiguration(toSdkLoggingConfiguration(buildLoggingConfiguration(Arrays.asList(config1, config2))))
                .firewallArn(firewallArn)
                .firewallName(firewallName)
                .build();

        when(proxyClient.client().describeLoggingConfiguration(any(DescribeLoggingConfigurationRequest.class)))
                .thenReturn(preCheckLoggingConfigurationResponse)
                .thenReturn(finalLoggingConfigurationResponse);

        when(proxyClient.client().updateLoggingConfiguration(any(UpdateLoggingConfigurationRequest.class)))
                .thenReturn(updateLoggingConfigurationResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client(), times(2)).updateLoggingConfiguration(any(UpdateLoggingConfigurationRequest.class));
    }

    @Test
    public void handleRequest_ResourceAlreadyExists() {
        LogDestinationConfig config1 = buildLogDestinationConfig("FLOW", "S3");
        LogDestinationConfig config2 = buildLogDestinationConfig("ALERT", "CloudWatchLogs");
        model = buildResourceModel(buildLoggingConfiguration(Arrays.asList(config1, config2)));

        final DescribeLoggingConfigurationResponse preCheckLoggingConfigurationResponse = DescribeLoggingConfigurationResponse.builder()
                .firewallArn(firewallArn)
                .loggingConfiguration(toSdkLoggingConfiguration(buildLoggingConfiguration(Arrays.asList(config1, config2))))
                .build();

        when(proxyClient.client().describeLoggingConfiguration(any(DescribeLoggingConfigurationRequest.class)))
                .thenReturn(preCheckLoggingConfigurationResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnAlreadyExistsException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        verify(proxyClient.client(), times(0)).updateLoggingConfiguration(any(UpdateLoggingConfigurationRequest.class));
    }

    @Test
    public void handleRequest_DestinationNotExists() {
        LogDestinationConfig config1 = buildLogDestinationConfig("FLOW", "S3");
        LogDestinationConfig config2 = buildLogDestinationConfig("ALERT", "CloudWatchLogs");
        model = buildResourceModel(buildLoggingConfiguration(Arrays.asList(config1, config2)));

        final DescribeLoggingConfigurationResponse preCheckLoggingConfigurationResponse = DescribeLoggingConfigurationResponse.builder()
                .firewallArn(firewallArn)
                .build();

        when(proxyClient.client().describeLoggingConfiguration(any(DescribeLoggingConfigurationRequest.class)))
                .thenReturn(preCheckLoggingConfigurationResponse);

        when(proxyClient.client().updateLoggingConfiguration(any(UpdateLoggingConfigurationRequest.class)))
                .thenThrow(CfnNotFoundException.class);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnNotFoundException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        verify(proxyClient.client(), times(1)).updateLoggingConfiguration(any(UpdateLoggingConfigurationRequest.class));
    }

    @Test
    public void handleRequest_InvalidRequest1() {
        LogDestinationConfig config1 = buildLogDestinationConfig("FLOW", "S3");
        LogDestinationConfig config2 = buildLogDestinationConfig("FLOW", "CloudWatchLogs");
        model = buildResourceModel(buildLoggingConfiguration(Arrays.asList(config1, config2)));

        final DescribeLoggingConfigurationResponse preCheckLoggingConfigurationResponse = DescribeLoggingConfigurationResponse.builder()
                .firewallArn(firewallArn)
                .build();

        when(proxyClient.client().describeLoggingConfiguration(any(DescribeLoggingConfigurationRequest.class)))
                .thenReturn(preCheckLoggingConfigurationResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnInvalidRequestException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        verify(proxyClient.client(), times(0)).updateLoggingConfiguration(any(UpdateLoggingConfigurationRequest.class));
    }

    @Test
    public void handleRequest_InvalidRequest2() {
        LogDestinationConfig config1 = buildLogDestinationConfig("FLOW", "S3");
        LogDestinationConfig config2 = buildLogDestinationConfig("ALERT", "CloudWatchLogs");
        model = buildResourceModel(buildLoggingConfiguration(Arrays.asList(config1, config2)));

        when(proxyClient.client().describeLoggingConfiguration(any(DescribeLoggingConfigurationRequest.class)))
                .thenThrow(InvalidRequestException.class);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnInvalidRequestException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        verify(proxyClient.client(), times(0)).updateLoggingConfiguration(any(UpdateLoggingConfigurationRequest.class));
    }
}
