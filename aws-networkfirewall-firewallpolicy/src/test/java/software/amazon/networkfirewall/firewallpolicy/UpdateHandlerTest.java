package software.amazon.networkfirewall.firewallpolicy;

import java.time.Duration;

import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.awssdk.services.networkfirewall.model.*;
import software.amazon.awssdk.services.networkfirewall.model.ResourceNotFoundException;
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
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<NetworkFirewallClient> proxyClient;

    @Mock
    NetworkFirewallClient networkFirewallClient;

    private UpdateHandler handler;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        networkFirewallClient = mock(NetworkFirewallClient.class);
        proxyClient = MOCK_PROXY(proxy, networkFirewallClient);
        handler = new UpdateHandler();
    }

    @AfterEach
    public void tear_down() {
        verify(networkFirewallClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(networkFirewallClient);
    }

    @Test
    public void testSuccessState() {
        when(proxyClient.injectCredentialsAndInvokeV2(
                UPDATE_FIREWALL_POLICY_REQUEST,
                networkFirewallClient::updateFirewallPolicy)
        ).thenReturn(UPDATE_FIREWALL_POLICY_RESPONSE);

        when(proxyClient.injectCredentialsAndInvokeV2(
                DESCRIBE_FIREWALL_POLICY_REQUEST,
                networkFirewallClient::describeFirewallPolicy)
        ).thenReturn(DESCRIBE_FIREWALL_POLICY_RESPONSE);

        when(proxyClient.injectCredentialsAndInvokeV2(
                TAG_RESOURCE_REQUEST,
                networkFirewallClient::tagResource)
        ).thenReturn(TAG_RESOURCE_RESPONSE);

        when(proxyClient.injectCredentialsAndInvokeV2(
                UNTAG_RESOURCE_REQUEST,
                networkFirewallClient::untagResource)
        ).thenReturn(UNTAG_RESOURCE_RESPONSE);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(
                proxy,
                RESOURCE_HANDLER_REQUEST,
                new CallbackContext(), proxyClient, logger
        );

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(UPDATE_FIREWALL_POLICY_RESPONSE_RESOURCE);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void testResourceNotFoundException() {
        when(proxyClient.injectCredentialsAndInvokeV2(
                DESCRIBE_FIREWALL_POLICY_REQUEST,
                networkFirewallClient::describeFirewallPolicy)
        ).thenReturn(DESCRIBE_FIREWALL_POLICY_RESPONSE);

        when(proxyClient.injectCredentialsAndInvokeV2(UPDATE_FIREWALL_POLICY_REQUEST, networkFirewallClient::updateFirewallPolicy))
                .thenThrow(ResourceNotFoundException.class);

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, RESOURCE_HANDLER_REQUEST, new CallbackContext(), proxyClient, logger));
    }

    @Test
    void testInvalidRequestException() {
        when(proxyClient.injectCredentialsAndInvokeV2(
                DESCRIBE_FIREWALL_POLICY_REQUEST,
                networkFirewallClient::describeFirewallPolicy)
        ).thenReturn(DESCRIBE_FIREWALL_POLICY_RESPONSE);

        when(proxyClient.injectCredentialsAndInvokeV2(UPDATE_FIREWALL_POLICY_REQUEST, networkFirewallClient::updateFirewallPolicy))
                .thenThrow(InvalidRequestException.class);

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, RESOURCE_HANDLER_REQUEST, new CallbackContext(), proxyClient, logger));
    }

    @Test
    void testThrottlingException() {
        when(proxyClient.injectCredentialsAndInvokeV2(
                DESCRIBE_FIREWALL_POLICY_REQUEST,
                networkFirewallClient::describeFirewallPolicy)
        ).thenReturn(DESCRIBE_FIREWALL_POLICY_RESPONSE);

        when(proxyClient.injectCredentialsAndInvokeV2(UPDATE_FIREWALL_POLICY_REQUEST, networkFirewallClient::updateFirewallPolicy))
                .thenThrow(ThrottlingException.class);

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, RESOURCE_HANDLER_REQUEST, new CallbackContext(), proxyClient, logger));
    }

    @Test
    void testInternalServerError() {
        when(proxyClient.injectCredentialsAndInvokeV2(
                DESCRIBE_FIREWALL_POLICY_REQUEST,
                networkFirewallClient::describeFirewallPolicy)
        ).thenReturn(DESCRIBE_FIREWALL_POLICY_RESPONSE);

        when(proxyClient.injectCredentialsAndInvokeV2(UPDATE_FIREWALL_POLICY_REQUEST, networkFirewallClient::updateFirewallPolicy))
                .thenThrow(InternalServerErrorException.class);

        assertThrows(CfnServiceInternalErrorException.class, () ->
                handler.handleRequest(proxy, RESOURCE_HANDLER_REQUEST, new CallbackContext(), proxyClient, logger));
    }

    @Test
    void testInvalidTokenException() {
        when(proxyClient.injectCredentialsAndInvokeV2(
                DESCRIBE_FIREWALL_POLICY_REQUEST,
                networkFirewallClient::describeFirewallPolicy)
        ).thenReturn(DESCRIBE_FIREWALL_POLICY_RESPONSE);

        when(proxyClient.injectCredentialsAndInvokeV2(UPDATE_FIREWALL_POLICY_REQUEST, networkFirewallClient::updateFirewallPolicy))
                .thenThrow(InvalidTokenException.class);

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, RESOURCE_HANDLER_REQUEST, new CallbackContext(), proxyClient, logger));
    }
}
