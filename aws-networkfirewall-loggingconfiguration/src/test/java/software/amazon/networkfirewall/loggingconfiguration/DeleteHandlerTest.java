package software.amazon.networkfirewall.loggingconfiguration;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;

import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.awssdk.services.networkfirewall.model.DescribeLoggingConfigurationRequest;
import software.amazon.awssdk.services.networkfirewall.model.DescribeLoggingConfigurationResponse;
import software.amazon.awssdk.services.networkfirewall.model.UpdateLoggingConfigurationRequest;
import software.amazon.awssdk.services.networkfirewall.model.UpdateLoggingConfigurationResponse;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;
import static software.amazon.networkfirewall.loggingconfiguration.Translator.toSdkLoggingConfiguration;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<NetworkFirewallClient> proxyClient;

    @Mock
    NetworkFirewallClient networkFirewallClient;

    private DeleteHandler handler;
    private ResourceModel model;

    @BeforeEach
    public void setup() {
        handler = new DeleteHandler();
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
        model = buildResourceModel();

        final DescribeLoggingConfigurationResponse preCheckLoggingConfigurationResponse = DescribeLoggingConfigurationResponse.builder()
                .firewallArn(firewallArn)
                .loggingConfiguration(toSdkLoggingConfiguration(buildLoggingConfiguration(Collections.singletonList(config1))))
                .build();

        final DescribeLoggingConfigurationResponse finalLoggingConfigurationResponse = DescribeLoggingConfigurationResponse.builder()
                .firewallArn(firewallArn)
                .build();

        final UpdateLoggingConfigurationResponse updateLoggingConfigurationResponse = UpdateLoggingConfigurationResponse.builder()
                .firewallArn(firewallArn)
                .firewallName(firewallName)
                .build();

        when(proxyClient.client().describeLoggingConfiguration(any(DescribeLoggingConfigurationRequest.class)))
                .thenReturn(preCheckLoggingConfigurationResponse)
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
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client(), times(1)).updateLoggingConfiguration(any(UpdateLoggingConfigurationRequest.class));
    }

    @Test
    public void handleRequest_SimpleSuccessWith2LogDestination() {
        LogDestinationConfig config1 = buildLogDestinationConfig("FLOW", "S3");
        LogDestinationConfig config2 = buildLogDestinationConfig("ALERT", "CloudWatchLogs");
        model = buildResourceModel();

        final DescribeLoggingConfigurationResponse preCheckLoggingConfigurationResponse = DescribeLoggingConfigurationResponse.builder()
                .firewallArn(firewallArn)
                .loggingConfiguration(toSdkLoggingConfiguration(buildLoggingConfiguration(Arrays.asList(config1, config2))))
                .build();

        final DescribeLoggingConfigurationResponse finalLoggingConfigurationResponse = DescribeLoggingConfigurationResponse.builder()
                .firewallArn(firewallArn)
                .build();

        final UpdateLoggingConfigurationResponse updateLoggingConfigurationResponse1 = UpdateLoggingConfigurationResponse.builder()
                .firewallArn(firewallArn)
                .firewallName(firewallName)
                .loggingConfiguration(toSdkLoggingConfiguration(buildLoggingConfiguration(Collections.singletonList(config2))))
                .build();

        final UpdateLoggingConfigurationResponse updateLoggingConfigurationResponse2 = UpdateLoggingConfigurationResponse.builder()
                .firewallArn(firewallArn)
                .firewallName(firewallName)
                .build();

        when(proxyClient.client().describeLoggingConfiguration(any(DescribeLoggingConfigurationRequest.class)))
                .thenReturn(preCheckLoggingConfigurationResponse)
                .thenReturn(preCheckLoggingConfigurationResponse)
                .thenReturn(finalLoggingConfigurationResponse);

        when(proxyClient.client().updateLoggingConfiguration(any(UpdateLoggingConfigurationRequest.class)))
                .thenReturn(updateLoggingConfigurationResponse1)
                .thenReturn(updateLoggingConfigurationResponse2);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client(), times(2)).updateLoggingConfiguration(any(UpdateLoggingConfigurationRequest.class));
    }

    @Test
    public void handleRequest_ResourceNotFound() {
        model = buildResourceModel();


        when(proxyClient.client().describeLoggingConfiguration(any(DescribeLoggingConfigurationRequest.class)))
                .thenThrow(CfnNotFoundException.class);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnNotFoundException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        verify(proxyClient.client(), times(1)).describeLoggingConfiguration(any(DescribeLoggingConfigurationRequest.class));
    }

    @Test
    public void handleRequest_InternalServerError() {
        model = buildResourceModel();


        when(proxyClient.client().describeLoggingConfiguration(any(DescribeLoggingConfigurationRequest.class)))
                .thenThrow(CfnServiceInternalErrorException.class);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnServiceInternalErrorException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        verify(proxyClient.client(), times(1)).describeLoggingConfiguration(any(DescribeLoggingConfigurationRequest.class));
    }
}
