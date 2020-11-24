package software.amazon.networkfirewall.firewall;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Assertions;
import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.awssdk.services.networkfirewall.model.Attachment;
import software.amazon.awssdk.services.networkfirewall.model.ConfigurationSyncState;
import software.amazon.awssdk.services.networkfirewall.model.DescribeFirewallRequest;
import software.amazon.awssdk.services.networkfirewall.model.DescribeFirewallResponse;
import software.amazon.awssdk.services.networkfirewall.model.Firewall;
import software.amazon.awssdk.services.networkfirewall.model.FirewallStatusValue;
import software.amazon.awssdk.services.networkfirewall.model.InternalServerErrorException;
import software.amazon.awssdk.services.networkfirewall.model.InvalidRequestException;
import software.amazon.awssdk.services.networkfirewall.model.ResourceNotFoundException;
import software.amazon.awssdk.services.networkfirewall.model.SyncState;
import software.amazon.awssdk.services.networkfirewall.model.Tag;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends AbstractTestBase {
    private static ResourceHandlerRequest<ResourceModel> request;
    private ReadHandler handler;

    @Mock
    private AmazonWebServicesClientProxy proxy;
    @Mock
    private ProxyClient<NetworkFirewallClient> proxyClient;
    @Mock
    NetworkFirewallClient client;

    @BeforeEach
    public void setup() {
        handler = new ReadHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        client = mock(NetworkFirewallClient.class);
        proxyClient = MOCK_PROXY(proxy, client);

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(new ResourceModel())
                .build();
    }

    @AfterEach
    public void tear_down() {
        verify(client, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(client);
    }

    @Test
    public void simpleDescribeSuccess() {
        // setup mock requests and responses
        when(proxyClient.client().describeFirewall(any(DescribeFirewallRequest.class))).thenReturn(commonDescribeResponse());

        // call handler function
        final ProgressEvent<ResourceModel, CallbackContext> handlerResponse =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // validate results
        assertThat(handlerResponse).isNotNull();
        assertThat(handlerResponse.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(handlerResponse.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(handlerResponse.getResourceModels()).isNull();
        assertThat(handlerResponse.getMessage()).isNull();
        assertThat(handlerResponse.getErrorCode()).isNull();

        final ResourceModel model = handlerResponse.getResourceModel();
        assertThat(model.getFirewallId()).isEqualTo("id");
        assertThat(model.getFirewallArn()).isEqualTo("validarn");
        assertThat(model.getFirewallName()).isEqualTo("firewallName");
        assertThat(model.getFirewallPolicyArn()).isEqualTo("policyarn");
        assertThat(model.getVpcId()).isEqualTo("vpcId");

        verify(proxyClient.client()).describeFirewall(any(DescribeFirewallRequest.class));
    }

    @Test
    public void allFieldsDescribeSuccess() {
        // setup mock requests and responses
        // generate Firewall response with tags
        final Set<Tag> tags = ImmutableSet.of(
                software.amazon.awssdk.services.networkfirewall.model.Tag.builder().key("1-key").value("1-value").build(),
                software.amazon.awssdk.services.networkfirewall.model.Tag.builder().key("2-key").value("2-value").build(),
                software.amazon.awssdk.services.networkfirewall.model.Tag.builder().key("3-key").value("3-value").build());
        final Firewall firewall = Firewall.builder()
                .firewallId("id").firewallArn("validarn").firewallName("firewallName").firewallPolicyArn("policyarn")
                .vpcId("vpcId").tags(tags).build();
        final Map<String, SyncState> syncStates = ImmutableMap.of(
            "us-west-2a", SyncState.builder().attachment(Attachment.builder().endpointId("endpoint-1").build()).build(),
            "us-west-2b", SyncState.builder().attachment(Attachment.builder().endpointId("endpoint-2").build()).build(),
            "us-west-2c", SyncState.builder().attachment(Attachment.builder().endpointId("endpoint-3").build()).build());

        final software.amazon.awssdk.services.networkfirewall.model.FirewallStatus status =
                software.amazon.awssdk.services.networkfirewall.model.FirewallStatus.builder()
                        .status(FirewallStatusValue.READY.toString())
                        .configurationSyncStateSummary("summary")
                        .syncStates(syncStates)
                        .build();

        final DescribeFirewallResponse allFieldsDescribeResponse =
                DescribeFirewallResponse.builder().firewall(firewall).firewallStatus(status).build();

        when(proxyClient.client().describeFirewall(any(DescribeFirewallRequest.class))).thenReturn(allFieldsDescribeResponse);

        // call handler function
        final ProgressEvent<ResourceModel, CallbackContext> handlerResponse =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // validate results
        // generate resource model fields for assertion
        final Set<software.amazon.networkfirewall.firewall.Tag> resourceModelTags = ImmutableSet.of(
                software.amazon.networkfirewall.firewall.Tag.builder().key("1-key").value("1-value").build(),
                software.amazon.networkfirewall.firewall.Tag.builder().key("2-key").value("2-value").build(),
                software.amazon.networkfirewall.firewall.Tag.builder().key("3-key").value("3-value").build());

        assertThat(handlerResponse).isNotNull();
        assertThat(handlerResponse.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(handlerResponse.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(handlerResponse.getResourceModels()).isNull();
        assertThat(handlerResponse.getMessage()).isNull();
        assertThat(handlerResponse.getErrorCode()).isNull();

        final ResourceModel model = handlerResponse.getResourceModel();
        assertThat(model.getFirewallId()).isEqualTo("id");
        assertThat(model.getFirewallArn()).isEqualTo("validarn");
        assertThat(model.getFirewallName()).isEqualTo("firewallName");
        assertThat(model.getFirewallPolicyArn()).isEqualTo("policyarn");
        assertThat(model.getVpcId()).isEqualTo("vpcId");
        assertThat(model.getTags().size()).isEqualTo(3);
        assertThat(model.getTags()).isEqualTo(resourceModelTags);
        assertThat(model.getEndpointIds().get(0)).isEqualTo("us-west-2a:endpoint-1");
        assertThat(model.getEndpointIds().get(1)).isEqualTo("us-west-2b:endpoint-2");
        assertThat(model.getEndpointIds().get(2)).isEqualTo("us-west-2c:endpoint-3");

        verify(proxyClient.client()).describeFirewall(any(DescribeFirewallRequest.class));
    }

    @Test
    public void describeResourceNotFound() {
        when(proxyClient.client().describeFirewall(any(DescribeFirewallRequest.class)))
                .thenThrow(ResourceNotFoundException.class);
        Assertions.assertThrows(CfnNotFoundException.class,
                () -> {handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);});
    }

    @Test
    public void describeInvalidRequest() {
        when(proxyClient.client().describeFirewall(any(DescribeFirewallRequest.class)))
                .thenThrow(InvalidRequestException.class);
        Assertions.assertThrows(CfnInvalidRequestException.class,
                () -> {handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);});
    }

    @Test
    public void describeInternalServiceError() {
        when(proxyClient.client().describeFirewall(any(DescribeFirewallRequest.class)))
                .thenThrow(InternalServerErrorException.class);
        Assertions.assertThrows(CfnServiceInternalErrorException.class,
                () -> {handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);});
    }
}
