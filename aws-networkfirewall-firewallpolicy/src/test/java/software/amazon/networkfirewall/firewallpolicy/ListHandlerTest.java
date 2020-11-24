package software.amazon.networkfirewall.firewallpolicy;

import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.awssdk.services.networkfirewall.model.ListFirewallPoliciesResponse;
import software.amazon.awssdk.services.networkfirewall.model.*;
import software.amazon.cloudformation.exceptions.*;
import software.amazon.awssdk.services.networkfirewall.model.FirewallPolicyMetadata;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private ProxyClient<NetworkFirewallClient> proxyClient;

    @Mock
    NetworkFirewallClient networkFirewallClient;

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        networkFirewallClient = mock(NetworkFirewallClient.class);
        proxyClient = MOCK_PROXY(proxy, networkFirewallClient);
        logger = mock(Logger.class);
    }

    @Test
    public void testSuccessState() {
        // Prepare inputs
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(ResourceModel.builder().build())
                .build();
        FirewallPolicyMetadata policy1 = FirewallPolicyMetadata.builder().name("policyOne")
                .build();
        FirewallPolicyMetadata policy2 = FirewallPolicyMetadata.builder().name("policyTwo")
                .build();

        List<FirewallPolicyMetadata> firewallPolicies = new ArrayList<>();
        firewallPolicies.add(policy1);
        firewallPolicies.add(policy2);

        // Mock
        doReturn(
                ListFirewallPoliciesResponse.builder()
                        .nextToken("nextToken")
                        .firewallPolicies(firewallPolicies)
                        .build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        // Call
        final ProgressEvent<ResourceModel, CallbackContext> response
                = new ListHandler().handleRequest(proxy, request, null, proxyClient, logger);

        // Assert
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels().size()).isEqualTo(firewallPolicies.size());
        assertThat(response.getResourceModels().get(0).getFirewallPolicyName()).isEqualTo(firewallPolicies.get(0).name());
        assertThat(response.getResourceModels().get(1).getFirewallPolicyName()).isEqualTo(firewallPolicies.get(1).name());
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    void testInvalidRequestException() {
        // Prepare inputs
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(ResourceModel.builder().build())
                .build();

        // Mock
        doThrow(InvalidRequestException.builder().build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        // Call
        assertThrows(CfnInvalidRequestException.class, () ->
                new ListHandler().handleRequest(proxy, request, null, proxyClient, logger));
    }

    @Test
    void testThrottlingException() {
        // Prepare inputs
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(ResourceModel.builder().build())
                .build();

        // Mock
        doThrow(ThrottlingException.builder().build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        // Call
        assertThrows(CfnThrottlingException.class, () ->
                new ListHandler().handleRequest(proxy, request, null, proxyClient, logger));
    }

    @Test
    void testInternalServerError() {
        // Prepare inputs
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(ResourceModel.builder().build())
                .build();

        // Mock
        doThrow(CfnServiceInternalErrorException.class)
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        // Call
        assertThrows(CfnServiceInternalErrorException.class, () ->
                new ListHandler().handleRequest(proxy, request, null, proxyClient, logger));
    }
}
