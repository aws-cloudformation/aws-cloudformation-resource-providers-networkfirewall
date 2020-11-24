package software.amazon.networkfirewall.firewallpolicy;

import java.time.Duration;
import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.awssdk.services.networkfirewall.model.*;
import software.amazon.awssdk.services.networkfirewall.model.ResourceNotFoundException;
import software.amazon.awssdk.services.networkfirewall.model.UnsupportedOperationException;
import software.amazon.cloudformation.exceptions.*;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<NetworkFirewallClient> proxyClient;

    @Mock
    NetworkFirewallClient networkFirewallClient;

    private DeleteHandler handler;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        networkFirewallClient = mock(NetworkFirewallClient.class);
        proxyClient = MOCK_PROXY(proxy, networkFirewallClient);
        handler = new DeleteHandler();
    }

    @AfterEach
    public void tear_down() {
        verify(networkFirewallClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(networkFirewallClient);
    }

    @Test
    public void testSuccessState() {
        when(proxyClient.injectCredentialsAndInvokeV2(
                DELETE_FIREWALL_POLICY_REQUEST,
                networkFirewallClient::deleteFirewallPolicy)
        ).thenReturn(DELETE_FIREWALL_POLICY_RESPONSE);

        when(proxyClient.injectCredentialsAndInvokeV2(
                DESCRIBE_FIREWALL_POLICY_REQUEST,
                networkFirewallClient::describeFirewallPolicy)
        ).thenThrow(ResourceNotFoundException.class);

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, RESOURCE_HANDLER_REQUEST, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void testResourceNotFoundException() {
        when(proxyClient.injectCredentialsAndInvokeV2(DELETE_FIREWALL_POLICY_REQUEST, networkFirewallClient::deleteFirewallPolicy))
                .thenThrow(ResourceNotFoundException.class);

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, RESOURCE_HANDLER_REQUEST, new CallbackContext(), proxyClient, logger));
    }

    @Test
    void testUnsupportedOperationException() {
        when(proxyClient.injectCredentialsAndInvokeV2(DELETE_FIREWALL_POLICY_REQUEST, networkFirewallClient::deleteFirewallPolicy))
                .thenThrow(UnsupportedOperationException.class);

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, RESOURCE_HANDLER_REQUEST, new CallbackContext(), proxyClient, logger));
    }

    @Test
    void testThrottlingException() {
        when(proxyClient.injectCredentialsAndInvokeV2(DELETE_FIREWALL_POLICY_REQUEST, networkFirewallClient::deleteFirewallPolicy))
                .thenThrow(ThrottlingException.class);

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, RESOURCE_HANDLER_REQUEST, new CallbackContext(), proxyClient, logger));
    }

    @Test
    void testInternalServerError() {
        when(proxyClient.injectCredentialsAndInvokeV2(DELETE_FIREWALL_POLICY_REQUEST, networkFirewallClient::deleteFirewallPolicy))
                .thenThrow(InternalServerErrorException.class);

        assertThrows(CfnServiceInternalErrorException.class, () ->
                handler.handleRequest(proxy, RESOURCE_HANDLER_REQUEST, new CallbackContext(), proxyClient, logger));
    }

    @Test
    void testInvalidOperationException() {
        when(proxyClient.injectCredentialsAndInvokeV2(DELETE_FIREWALL_POLICY_REQUEST, networkFirewallClient::deleteFirewallPolicy))
                .thenThrow(InvalidOperationException.class);

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, RESOURCE_HANDLER_REQUEST, new CallbackContext(), proxyClient, logger));
    }
}
