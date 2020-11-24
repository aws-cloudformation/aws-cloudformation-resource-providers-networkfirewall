package software.amazon.networkfirewall.firewall;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.AfterEach;
import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.awssdk.services.networkfirewall.model.CreateFirewallRequest;
import software.amazon.awssdk.services.networkfirewall.model.FirewallMetadata;
import software.amazon.awssdk.services.networkfirewall.model.ListFirewallsRequest;
import software.amazon.awssdk.services.networkfirewall.model.ListFirewallsResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
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

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends AbstractTestBase {
    private static ResourceHandlerRequest<ResourceModel> request;
    private ListHandler handler;

    @Mock
    private AmazonWebServicesClientProxy proxy;
    @Mock
    private ProxyClient<NetworkFirewallClient> proxyClient;
    @Mock
    NetworkFirewallClient client;

    @BeforeEach
    public void setup() {
        handler = new ListHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        client = mock(NetworkFirewallClient.class);
        proxyClient = MOCK_PROXY(proxy, client);

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(ResourceModel.builder().build())
                .build();
    }

    @AfterEach
    public void tear_down() {
        verifyNoMoreInteractions(proxyClient.client());
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final List<FirewallMetadata> metadataList = ImmutableList.of(FirewallMetadata.builder().firewallArn("arn1").build(),
                FirewallMetadata.builder().firewallArn("arn2").build(),
                FirewallMetadata.builder().firewallArn("arn3").build());
        // setup mock requests and responses
        final ListFirewallsResponse firewalls = ListFirewallsResponse.builder().firewalls(metadataList).build();
        when(proxyClient.client().listFirewalls(any(ListFirewallsRequest.class))).thenReturn(firewalls);

        // call list handler
        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // validate results
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNotNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).listFirewalls(any(ListFirewallsRequest.class));
    }
}
