package software.amazon.networkfirewall.firewall;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Assertions;
import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.awssdk.services.networkfirewall.model.ConfigurationSyncState;
import software.amazon.awssdk.services.networkfirewall.model.CreateFirewallRequest;
import software.amazon.awssdk.services.networkfirewall.model.CreateFirewallResponse;
import software.amazon.awssdk.services.networkfirewall.model.DescribeFirewallRequest;
import software.amazon.awssdk.services.networkfirewall.model.DescribeFirewallResponse;
import software.amazon.awssdk.services.networkfirewall.model.Firewall;
import software.amazon.awssdk.services.networkfirewall.model.FirewallStatusValue;
import software.amazon.awssdk.services.networkfirewall.model.InsufficientCapacityException;
import software.amazon.awssdk.services.networkfirewall.model.InternalServerErrorException;
import software.amazon.awssdk.services.networkfirewall.model.InvalidOperationException;
import software.amazon.awssdk.services.networkfirewall.model.InvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {
    private static ResourceHandlerRequest<ResourceModel> request;
    private CreateHandler handler;

    @Mock
    private AmazonWebServicesClientProxy proxy;
    @Mock
    private ProxyClient<NetworkFirewallClient> proxyClient;
    @Mock
    NetworkFirewallClient client;

    @BeforeEach
    public void setup() {
        handler = new CreateHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        client = mock(NetworkFirewallClient.class);
        proxyClient = MOCK_PROXY(proxy, client);

        final Set<SubnetMapping> subnetMappings = ImmutableSet.of(new SubnetMapping("subnet1"),
                new SubnetMapping("subnet2"));
        ResourceModel model = ResourceModel.builder()
                .firewallName("firewallName").firewallPolicyArn("policyarn")
                .vpcId("vpcId").subnetMappings(subnetMappings).build();

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();
    }

    @AfterEach
    public void tear_down() {
        verify(client, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(client);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        // setup mock requests and responses
        // describe returns a READY status implying that the firewall is created instantly
        when(proxyClient.client().describeFirewall(any(DescribeFirewallRequest.class)))
                .thenReturn(commonDescribeResponse())
                .thenReturn(commonDescribeResponse()); // last step of create

        when(proxyClient.client().createFirewall(any(CreateFirewallRequest.class))).thenReturn(
                CreateFirewallResponse.builder().firewall(Firewall.builder().firewallArn("validarn").build()).build());

        // call create handler
        final ProgressEvent<ResourceModel, CallbackContext> handlerResponse =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // validate results
        assertThat(handlerResponse).isNotNull();
        assertThat(handlerResponse.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(handlerResponse.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(handlerResponse.getResourceModel()).isNotNull();
        assertThat(handlerResponse.getResourceModels()).isNull();
        assertThat(handlerResponse.getMessage()).isNull();
        assertThat(handlerResponse.getErrorCode()).isNull();

        verify(proxyClient.client()).createFirewall(any(CreateFirewallRequest.class));
        verify(proxyClient.client(), times(2)).describeFirewall(any(DescribeFirewallRequest.class));

        final ResourceModel model = handlerResponse.getResourceModel();
        assertThat(model.getFirewallId()).isEqualTo("id");
        assertThat(model.getFirewallArn()).isEqualTo("validarn");
        assertThat(model.getFirewallName()).isEqualTo("firewallName");
        assertThat(model.getFirewallPolicyArn()).isEqualTo("policyarn");
        assertThat(model.getVpcId()).isEqualTo("vpcId");
        assertThat(model.getTags()).isNullOrEmpty();
    }

    @Test
    public void handleRequest_SuccessMultipleTries() {
        // setup mock requests and responses
        // describe returns a READY status implying that the firewall is created instantly
        when(proxyClient.client().describeFirewall(any(DescribeFirewallRequest.class)))
                .thenReturn(commonDescribeResponse(FirewallStatusValue.PROVISIONING.toString()))
                .thenReturn(commonDescribeResponse(FirewallStatusValue.PROVISIONING.toString()))
                .thenReturn(commonDescribeResponse(FirewallStatusValue.PROVISIONING.toString()))
                .thenReturn(commonDescribeResponse(FirewallStatusValue.READY.toString())) // creation successful
                .thenReturn(commonDescribeResponse()); // last step of create

        when(proxyClient.client().createFirewall(any(CreateFirewallRequest.class))).thenReturn(
                CreateFirewallResponse.builder().firewall(Firewall.builder().firewallArn("validarn").build()).build());

        // call create handler
        final ProgressEvent<ResourceModel, CallbackContext> handlerResponse =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // validate results
        assertThat(handlerResponse).isNotNull();
        assertThat(handlerResponse.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(handlerResponse.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(handlerResponse.getResourceModel()).isNotNull();
        assertThat(handlerResponse.getResourceModels()).isNull();
        assertThat(handlerResponse.getMessage()).isNull();
        assertThat(handlerResponse.getErrorCode()).isNull();

        verify(proxyClient.client()).createFirewall(any(CreateFirewallRequest.class));
        verify(proxyClient.client(), times(5)).describeFirewall(any(DescribeFirewallRequest.class));

        final ResourceModel model = handlerResponse.getResourceModel();
        assertThat(model.getFirewallId()).isEqualTo("id");
        assertThat(model.getFirewallArn()).isEqualTo("validarn");
        assertThat(model.getFirewallName()).isEqualTo("firewallName");
        assertThat(model.getFirewallPolicyArn()).isEqualTo("policyarn");
        assertThat(model.getVpcId()).isEqualTo("vpcId");
        assertThat(model.getTags()).isNullOrEmpty();
    }

    @Test
    public void handleRequest_SimpleSuccess_WithFirewallRequestTags() {
        // setup mock requests and responses
        // generate Firewall response with tags
        final Set<software.amazon.awssdk.services.networkfirewall.model.Tag> tags = ImmutableSet.of(
                software.amazon.awssdk.services.networkfirewall.model.Tag.builder().key("1-key").value("1-value").build(),
                software.amazon.awssdk.services.networkfirewall.model.Tag.builder().key("2-key").value("2-value").build(),
                software.amazon.awssdk.services.networkfirewall.model.Tag.builder().key("3-key").value("3-value").build());
        final Firewall firewall = Firewall.builder()
                .firewallId("id")
                .firewallArn("validarn")
                .firewallName("firewallName")
                .firewallPolicyArn("policyarn")
                .vpcId("vpcId")
                .tags(tags)
                .build();
        final software.amazon.awssdk.services.networkfirewall.model.FirewallStatus status =
                software.amazon.awssdk.services.networkfirewall.model.FirewallStatus.builder()
                        .status(FirewallStatusValue.READY.toString())
                        .configurationSyncStateSummary(ConfigurationSyncState.IN_SYNC)
                        .build();

        final DescribeFirewallResponse describeResponseWithTags =
                DescribeFirewallResponse.builder().firewall(firewall).firewallStatus(status).build();

        // generate resource model request
        final Set<Tag> resourceModelTags = ImmutableSet.of(Tag.builder().key("1-key").value("1-value").build(),
                Tag.builder().key("2-key").value("2-value").build(),
                Tag.builder().key("3-key").value("3-value").build());

        final Set<SubnetMapping> subnetMappings = ImmutableSet.of(new SubnetMapping("subnet1"),
                new SubnetMapping("subnet2"));

        final ResourceModel modelWithTags = ResourceModel.builder()
                .firewallName("firewallName").firewallPolicyArn("policyarn")
                .vpcId("vpcId").subnetMappings(subnetMappings)
                .tags(resourceModelTags).build();

        final ResourceHandlerRequest<ResourceModel> requestWithTags = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(modelWithTags)
                .build();

        when(proxyClient.client().createFirewall(any(CreateFirewallRequest.class))).thenReturn(
                CreateFirewallResponse.builder().firewall(Firewall.builder().firewallArn("validarn").build()).build());

        // describe returns a READY status implying that the firewall is created instantly
        when(proxyClient.client().describeFirewall(any(DescribeFirewallRequest.class)))
                .thenReturn(commonDescribeResponse())
                .thenReturn(describeResponseWithTags); // last step of create

        // call create handler
        final ProgressEvent<ResourceModel, CallbackContext> handlerResponse =
                handler.handleRequest(proxy, requestWithTags, new CallbackContext(), proxyClient, logger);

        // validate results
        assertThat(handlerResponse).isNotNull();
        assertThat(handlerResponse.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(handlerResponse.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(handlerResponse.getResourceModel()).isNotNull();
        assertThat(handlerResponse.getResourceModels()).isNull();
        assertThat(handlerResponse.getMessage()).isNull();
        assertThat(handlerResponse.getErrorCode()).isNull();

        verify(proxyClient.client()).createFirewall(any(CreateFirewallRequest.class));
        verify(proxyClient.client(), times(2)).describeFirewall(any(DescribeFirewallRequest.class));

        final ResourceModel model = handlerResponse.getResourceModel();
        assertThat(model.getFirewallId()).isEqualTo("id");
        assertThat(model.getFirewallArn()).isEqualTo("validarn");
        assertThat(model.getFirewallName()).isEqualTo("firewallName");
        assertThat(model.getFirewallPolicyArn()).isEqualTo("policyarn");
        assertThat(model.getVpcId()).isEqualTo("vpcId");
        assertThat(model.getTags().size()).isEqualTo(3);
        assertThat(model.getTags()).isEqualTo(resourceModelTags);
    }

    @Test
    public void handleRequest_SimpleSuccess_WithStackTags() {
        // setup mock requests and responses
        // generate Firewall response with tags
        final Set<software.amazon.awssdk.services.networkfirewall.model.Tag> tags = ImmutableSet.of(
                software.amazon.awssdk.services.networkfirewall.model.Tag.builder().key("Stack-key-1").value("1-value").build(),
                software.amazon.awssdk.services.networkfirewall.model.Tag.builder().key("Stack-key-2").value("2-value").build(),
                software.amazon.awssdk.services.networkfirewall.model.Tag.builder().key("Stack-key-3").value("3-value").build());
        final Firewall firewall = Firewall.builder()
                .firewallId("id")
                .firewallArn("validarn")
                .firewallName("firewallName")
                .firewallPolicyArn("policyarn")
                .vpcId("vpcId")
                .tags(tags)
                .build();
        final software.amazon.awssdk.services.networkfirewall.model.FirewallStatus status =
                software.amazon.awssdk.services.networkfirewall.model.FirewallStatus.builder()
                        .status(FirewallStatusValue.READY.toString())
                        .configurationSyncStateSummary(ConfigurationSyncState.IN_SYNC)
                        .build();

        final DescribeFirewallResponse describeResponseWithTags =
                DescribeFirewallResponse.builder().firewall(firewall).firewallStatus(status).build();

        // generate resource model request
        final Map<String, String> stackTags = ImmutableMap.of("Stack-key-1", "1-value", "Stack-key-2", "2-value",
                "Stack-key-3", "3-value");

        final Set<SubnetMapping> subnetMappings = ImmutableSet.of(new SubnetMapping("subnet1"),
                new SubnetMapping("subnet2"));

        final ResourceModel requestModel = ResourceModel.builder()
                .firewallName("firewallName").firewallPolicyArn("policyarn")
                .vpcId("vpcId").subnetMappings(subnetMappings).build();

        final ResourceHandlerRequest<ResourceModel> requestWithTags = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(requestModel)
                .desiredResourceTags(stackTags)
                .build();

        when(proxyClient.client().createFirewall(any(CreateFirewallRequest.class))).thenReturn(
                CreateFirewallResponse.builder().firewall(Firewall.builder().firewallArn("validarn").build()).build());

        // describe returns a READY status implying that the firewall is created instantly
        when(proxyClient.client().describeFirewall(any(DescribeFirewallRequest.class)))
                .thenReturn(commonDescribeResponse())
                .thenReturn(describeResponseWithTags); // last step of create

        // call create handler
        final ProgressEvent<ResourceModel, CallbackContext> handlerResponse =
                handler.handleRequest(proxy, requestWithTags, new CallbackContext(), proxyClient, logger);

        // validate results
        assertThat(handlerResponse).isNotNull();
        assertThat(handlerResponse.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(handlerResponse.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(handlerResponse.getResourceModel()).isNotNull();
        assertThat(handlerResponse.getResourceModels()).isNull();
        assertThat(handlerResponse.getMessage()).isNull();
        assertThat(handlerResponse.getErrorCode()).isNull();

        verify(proxyClient.client()).createFirewall(any(CreateFirewallRequest.class));
        verify(proxyClient.client(), times(2)).describeFirewall(any(DescribeFirewallRequest.class));

        final ResourceModel model = handlerResponse.getResourceModel();
        assertThat(model.getFirewallId()).isEqualTo("id");
        assertThat(model.getFirewallArn()).isEqualTo("validarn");
        assertThat(model.getFirewallName()).isEqualTo("firewallName");
        assertThat(model.getFirewallPolicyArn()).isEqualTo("policyarn");
        assertThat(model.getVpcId()).isEqualTo("vpcId");
        assertThat(model.getTags().size()).isEqualTo(3);
        assertThat(handlerResponse.getResourceModel().getTags()).isEqualTo(Translator.translateTagsToSdk(tags));
    }

    @Test
    public void handleRequest_SimpleSuccess_WithStackTagsAndRequestTag() {
        // setup mock requests and responses
        // generate Firewall response with tags
        final Set<software.amazon.awssdk.services.networkfirewall.model.Tag> tags = ImmutableSet.of(
                software.amazon.awssdk.services.networkfirewall.model.Tag.builder().key("Stack-key-1").value("1-value").build(),
                software.amazon.awssdk.services.networkfirewall.model.Tag.builder().key("Request-key-2").value("2-value").build(),
                software.amazon.awssdk.services.networkfirewall.model.Tag.builder().key("Stack-key-3").value("3-value").build());
        final Firewall firewall = Firewall.builder()
                .firewallId("id")
                .firewallArn("validarn")
                .firewallName("firewallName")
                .firewallPolicyArn("policyarn")
                .vpcId("vpcId")
                .tags(tags)
                .build();
        final software.amazon.awssdk.services.networkfirewall.model.FirewallStatus status =
                software.amazon.awssdk.services.networkfirewall.model.FirewallStatus.builder()
                        .status(FirewallStatusValue.READY.toString())
                        .configurationSyncStateSummary(ConfigurationSyncState.IN_SYNC)
                        .build();

        final DescribeFirewallResponse describeResponseWithTags =
                DescribeFirewallResponse.builder().firewall(firewall).firewallStatus(status).build();

        // generate resource model request
        final Map<String, String> stackTags = ImmutableMap.of("Stack-key-1", "1-value","Stack-key-3", "3-value");
        final Set<Tag> requestTags = ImmutableSet.of(Tag.builder().key("Request-key-2").value("2-value").build());

        final Set<SubnetMapping> subnetMappings = ImmutableSet.of(new SubnetMapping("subnet1"),
                new SubnetMapping("subnet2"));

        final ResourceModel requestModel = ResourceModel.builder()
                .firewallName("firewallName").firewallPolicyArn("policyarn")
                .vpcId("vpcId").subnetMappings(subnetMappings).tags(requestTags).build();

        final ResourceHandlerRequest<ResourceModel> requestWithTags = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(requestModel)
                .desiredResourceTags(stackTags)
                .build();

        when(proxyClient.client().createFirewall(any(CreateFirewallRequest.class))).thenReturn(
                CreateFirewallResponse.builder().firewall(Firewall.builder().firewallArn("validarn").build()).build());

        // describe returns a READY status implying that the firewall is created instantly
        when(proxyClient.client().describeFirewall(any(DescribeFirewallRequest.class)))
                .thenReturn(commonDescribeResponse())
                .thenReturn(describeResponseWithTags); // last step of create

        // call create handler
        final ProgressEvent<ResourceModel, CallbackContext> handlerResponse =
                handler.handleRequest(proxy, requestWithTags, new CallbackContext(), proxyClient, logger);

        // validate results
        assertThat(handlerResponse).isNotNull();
        assertThat(handlerResponse.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(handlerResponse.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(handlerResponse.getResourceModel()).isNotNull();
        assertThat(handlerResponse.getResourceModels()).isNull();
        assertThat(handlerResponse.getMessage()).isNull();
        assertThat(handlerResponse.getErrorCode()).isNull();

        verify(proxyClient.client()).createFirewall(any(CreateFirewallRequest.class));
        verify(proxyClient.client(), times(2)).describeFirewall(any(DescribeFirewallRequest.class));

        final ResourceModel model = handlerResponse.getResourceModel();
        assertThat(model.getFirewallId()).isEqualTo("id");
        assertThat(model.getFirewallArn()).isEqualTo("validarn");
        assertThat(model.getFirewallName()).isEqualTo("firewallName");
        assertThat(model.getFirewallPolicyArn()).isEqualTo("policyarn");
        assertThat(model.getVpcId()).isEqualTo("vpcId");
        assertThat(model.getTags().size()).isEqualTo(3);
        assertThat(handlerResponse.getResourceModel().getTags()).isEqualTo(Translator.translateTagsToSdk(tags));
    }

    @Test
    public void handleRequest_DescribeReturnsDeleting() {
        // setup mock requests and responses
        when(proxyClient.client().describeFirewall(any(DescribeFirewallRequest.class)))
                .thenReturn(commonDescribeResponse(FirewallStatusValue.DELETING.toString()));

        when(proxyClient.client().createFirewall(any(CreateFirewallRequest.class))).thenReturn(
                CreateFirewallResponse.builder().firewall(Firewall.builder().firewallArn("validarn").build()).build());

        // call create handler
        Assertions.assertThrows(CfnGeneralServiceException.class,
                () -> {handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);});

        verify(proxyClient.client()).createFirewall(any(CreateFirewallRequest.class));
        verify(proxyClient.client()).describeFirewall(any(DescribeFirewallRequest.class));
    }

    @Test
    public void handleRequest_DescribeReturnsNewStatus() {
        // setup mock requests and responses
        when(proxyClient.client().describeFirewall(any(DescribeFirewallRequest.class)))
                .thenReturn(commonDescribeResponse("WARMING"));
        when(proxyClient.client().createFirewall(any(CreateFirewallRequest.class))).thenReturn(
                CreateFirewallResponse.builder().firewall(Firewall.builder().firewallArn("validarn").build()).build());

        // call create handler
        Assertions.assertThrows(CfnGeneralServiceException.class,
                () -> {handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);});

        verify(proxyClient.client()).createFirewall(any(CreateFirewallRequest.class));
        verify(proxyClient.client()).describeFirewall(any(DescribeFirewallRequest.class));
    }

    @Test
    public void handleRequest_InvalidRequestException_AlreadyExists() {
        // setup mock requests and responses
        when(proxyClient.client().createFirewall(any(CreateFirewallRequest.class)))
                .thenThrow(InvalidRequestException.builder().message("A resource with the specified name already exists").build());

        // call create handler
        Assertions.assertThrows(CfnAlreadyExistsException.class,
                () -> {handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);});
    }

    @Test
    public void handleRequest_InvalidRequestException() {
        // setup mock requests and responses
        when(proxyClient.client().createFirewall(any(CreateFirewallRequest.class)))
                .thenThrow(InvalidRequestException.class);

        // call create handler
        Assertions.assertThrows(CfnInvalidRequestException.class,
                () -> {handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);});
    }

    @Test
    public void handleRequest_InvalidOperationException() {
        // setup mock requests and responses
        when(proxyClient.client().createFirewall(any(CreateFirewallRequest.class)))
                .thenThrow(InvalidOperationException.class);

        // call create handler
        Assertions.assertThrows(CfnInvalidRequestException.class,
                () -> {handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);});
    }

    @Test
    public void handleRequest_InsufficientCapacityException() {
        // setup mock requests and responses
        when(proxyClient.client().createFirewall(any(CreateFirewallRequest.class)))
                .thenThrow(InsufficientCapacityException.class);

        // call create handler
        Assertions.assertThrows(CfnServiceInternalErrorException.class,
                () -> {handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);});
    }

    @Test
    public void handleRequest_InternalServerErrorException() {
        // setup mock requests and responses
        when(proxyClient.client().createFirewall(any(CreateFirewallRequest.class)))
                .thenThrow(InternalServerErrorException.class);

        // call create handler
        Assertions.assertThrows(CfnServiceInternalErrorException.class,
                () -> {handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);});
    }
}
