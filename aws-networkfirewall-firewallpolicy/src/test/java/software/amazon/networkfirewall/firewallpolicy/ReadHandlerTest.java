package software.amazon.networkfirewall.firewallpolicy;

import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.awssdk.services.networkfirewall.model.*;
import software.amazon.awssdk.services.networkfirewall.model.ResourceNotFoundException;
import software.amazon.cloudformation.exceptions.*;
import software.amazon.cloudformation.proxy.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends AbstractTestBase {

    @Mock
    private Logger logger;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<NetworkFirewallClient> proxyClient;

    @Mock
    NetworkFirewallClient networkFirewallClient;

    private ReadHandler handler;

    @BeforeEach
    public void setup() {
        proxy = getAmazonWebServicesClientProxy();
        networkFirewallClient = mock(NetworkFirewallClient.class);
        proxyClient = MOCK_PROXY(proxy, networkFirewallClient);
        logger = mock(Logger.class);
        handler = new ReadHandler();
    }

    @AfterEach
    public void tear_down() {
        verify(networkFirewallClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(networkFirewallClient);
    }

    @Test
    public void testSuccessState() {

        // Mock
        when(proxyClient
                .injectCredentialsAndInvokeV2(DESCRIBE_FIREWALL_POLICY_REQUEST, networkFirewallClient::describeFirewallPolicy))
                .thenReturn(DESCRIBE_FIREWALL_POLICY_RESPONSE);

        // Call
        final ProgressEvent<ResourceModel, CallbackContext> response
                =handler.handleRequest(proxy, RESOURCE_HANDLER_REQUEST, new CallbackContext(), proxyClient, logger);


        // Assert
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(DESCRIBE_FIREWALL_POLICY_RESPONSE_RESOURCE_MODEL);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void testInvalidRequestException() {
        when(proxyClient.injectCredentialsAndInvokeV2(DESCRIBE_FIREWALL_POLICY_REQUEST, networkFirewallClient::describeFirewallPolicy))
                .thenThrow(InvalidRequestException.class);

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, RESOURCE_HANDLER_REQUEST, new CallbackContext(), proxyClient, logger));
    }

    @Test
    void testResourceNotFoundException() {
        when(proxyClient.injectCredentialsAndInvokeV2(DESCRIBE_FIREWALL_POLICY_REQUEST, networkFirewallClient::describeFirewallPolicy))
                .thenThrow(ResourceNotFoundException.class);

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, RESOURCE_HANDLER_REQUEST, new CallbackContext(), proxyClient, logger));
    }

    @Test
    void testThrottlingException() {
        when(proxyClient.injectCredentialsAndInvokeV2(DESCRIBE_FIREWALL_POLICY_REQUEST, networkFirewallClient::describeFirewallPolicy))
                .thenThrow(ThrottlingException.class);

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, RESOURCE_HANDLER_REQUEST, new CallbackContext(), proxyClient, logger));
    }

    @Test
    void testInternalServerError() {
        when(proxyClient.injectCredentialsAndInvokeV2(DESCRIBE_FIREWALL_POLICY_REQUEST, networkFirewallClient::describeFirewallPolicy))
                .thenThrow(InternalServerErrorException.class);

        assertThrows(CfnServiceInternalErrorException.class, () ->
                handler.handleRequest(proxy, RESOURCE_HANDLER_REQUEST, new CallbackContext(), proxyClient, logger));
    }
}
