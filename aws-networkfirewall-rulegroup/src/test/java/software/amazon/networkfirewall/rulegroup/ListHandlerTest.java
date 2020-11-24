package software.amazon.networkfirewall.rulegroup;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.AfterEach;
import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.awssdk.services.networkfirewall.model.ListFirewallsRequest;
import software.amazon.awssdk.services.networkfirewall.model.ListRuleGroupsRequest;
import software.amazon.awssdk.services.networkfirewall.model.ListRuleGroupsResponse;
import software.amazon.awssdk.services.networkfirewall.model.RuleGroupMetadata;
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
    NetworkFirewallClient networkFirewallClient;

    @BeforeEach
    public void setup() {
        handler = new ListHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        networkFirewallClient = mock(NetworkFirewallClient.class);
        proxyClient = MOCK_PROXY(proxy, networkFirewallClient);

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(ResourceModel.builder().build())
                .build();
    }

    @AfterEach
    public void tear_down() {
        verifyNoMoreInteractions(networkFirewallClient);
    }

    @Test
    public void testHandleRequest_listRuleGroupSuccess() {
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        RuleGroupMetadata ruleGroupMetadata1 = RuleGroupMetadata.builder().arn(STATELESS_RULEGROUP_ARN).build();
        RuleGroupMetadata ruleGroupMetadata2 = RuleGroupMetadata.builder().arn(STATEFUL_RULEGROUP_ARN).build();
        List<RuleGroupMetadata> rulegroups = ImmutableList.of(ruleGroupMetadata1, ruleGroupMetadata2);

        ListRuleGroupsResponse listResponse = ListRuleGroupsResponse.builder()
                .ruleGroups(rulegroups).build();

        when(proxyClient.client().listRuleGroups(any(ListRuleGroupsRequest.class))).thenReturn(listResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        verify(proxyClient.client()).listRuleGroups(any(ListRuleGroupsRequest.class));

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNotNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}
