package software.amazon.networkfirewall.firewall;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.mockito.Mockito;
import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.awssdk.services.networkfirewall.model.AssociateFirewallPolicyRequest;
import software.amazon.awssdk.services.networkfirewall.model.AssociateSubnetsRequest;
import software.amazon.awssdk.services.networkfirewall.model.Attachment;
import software.amazon.awssdk.services.networkfirewall.model.AttachmentStatus;
import software.amazon.awssdk.services.networkfirewall.model.ConfigurationSyncState;
import software.amazon.awssdk.services.networkfirewall.model.DescribeFirewallRequest;
import software.amazon.awssdk.services.networkfirewall.model.DescribeFirewallResponse;
import software.amazon.awssdk.services.networkfirewall.model.DisassociateSubnetsRequest;
import software.amazon.awssdk.services.networkfirewall.model.Firewall;
import software.amazon.awssdk.services.networkfirewall.model.FirewallStatus;
import software.amazon.awssdk.services.networkfirewall.model.FirewallStatusValue;
import software.amazon.awssdk.services.networkfirewall.model.PerObjectStatus;
import software.amazon.awssdk.services.networkfirewall.model.PerObjectSyncStatus;
import software.amazon.awssdk.services.networkfirewall.model.SyncState;
import software.amazon.awssdk.services.networkfirewall.model.TagResourceRequest;
import software.amazon.awssdk.services.networkfirewall.model.UntagResourceRequest;
import software.amazon.awssdk.services.networkfirewall.model.UpdateFirewallDeleteProtectionRequest;
import software.amazon.awssdk.services.networkfirewall.model.UpdateFirewallDescriptionRequest;
import software.amazon.awssdk.services.networkfirewall.model.UpdateFirewallPolicyChangeProtectionRequest;
import software.amazon.awssdk.services.networkfirewall.model.UpdateSubnetChangeProtectionRequest;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase {
    private static ResourceHandlerRequest<ResourceModel> request;
    private UpdateHandler handler;

    @Mock
    private AmazonWebServicesClientProxy proxy;
    @Mock
    private ProxyClient<NetworkFirewallClient> proxyClient;
    @Mock
    NetworkFirewallClient client;

    @BeforeEach
    public void setup() {
        handler = new UpdateHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        client = mock(NetworkFirewallClient.class);
        proxyClient = MOCK_PROXY(proxy, client);
    }

    @AfterEach
    public void tear_down() {
         verify(client, atLeastOnce()).serviceName();
         verifyNoMoreInteractions(client);
    }

    @Test
    public void verifyUpdateSubnetChangeProtection() {
        // setup mock requests and responses
        when(proxyClient.client().describeFirewall(any(DescribeFirewallRequest.class)))
                .thenReturn(commonDescribeResponse());

        // I. switch from false to true
        // call update handler with proper request parameters: switch from false to true
        ResourceModel desiredModel = baseModel().subnetChangeProtection(true).build();
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredModel)
                .previousResourceState(baseModel().build())
                .build();
        ProgressEvent<ResourceModel, CallbackContext> handlerResponse =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // validate result
        validateCommonParameters(handlerResponse);
        verify(proxyClient.client()).updateSubnetChangeProtection(any(UpdateSubnetChangeProtectionRequest.class));
        verify(proxyClient.client(), times(2)).describeFirewall(any(DescribeFirewallRequest.class));

        // II. switch from true to false
        // call update handler with proper request parameters: switch from true to false
        desiredModel = baseModel().subnetChangeProtection(false).build();
        ResourceModel previousModel = baseModel().subnetChangeProtection(true).build();
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredModel)
                .previousResourceState(previousModel)
                .build();
        handlerResponse = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // validate result
        validateCommonParameters(handlerResponse);
        verify(proxyClient.client(), times(2)).updateSubnetChangeProtection(any(UpdateSubnetChangeProtectionRequest.class));

        // III. no switch
        // call update handler with proper request parameters: switch from true to true
        desiredModel = baseModel().subnetChangeProtection(true).build();
        previousModel = baseModel().subnetChangeProtection(true).build();
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredModel)
                .previousResourceState(previousModel)
                .build();
        handlerResponse = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // validate result
        validateCommonParameters(handlerResponse);
        verify(proxyClient.client(), times(2)).updateSubnetChangeProtection(any(UpdateSubnetChangeProtectionRequest.class));

        // validate none of the other update APIs are called
        verify(proxyClient.client(), never()).associateSubnets(any(AssociateSubnetsRequest.class));
        verify(proxyClient.client(), never()).disassociateSubnets(any(DisassociateSubnetsRequest.class));
        verify(proxyClient.client(), never()).updateFirewallDescription(any(UpdateFirewallDescriptionRequest.class));
        verify(proxyClient.client(), never()).updateFirewallPolicyChangeProtection(any(UpdateFirewallPolicyChangeProtectionRequest.class));
        verify(proxyClient.client(), never()).updateFirewallDeleteProtection(any(UpdateFirewallDeleteProtectionRequest.class));
        verify(proxyClient.client(), never()).associateFirewallPolicy(any(AssociateFirewallPolicyRequest.class));
        verify(proxyClient.client(), never()).tagResource(any(TagResourceRequest.class));
        verify(proxyClient.client(), never()).untagResource(any(UntagResourceRequest.class));
    }

    @Test
    public void verifyUpdateDeleteProtection() {
        // setup mock requests and responses
        when(proxyClient.client().describeFirewall(any(DescribeFirewallRequest.class)))
                .thenReturn(commonDescribeResponse());

        // I. switch from false to true
        // call update handler with proper request parameters: switch from false to true
        ResourceModel desiredModel = baseModel().deleteProtection(true).build();
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredModel)
                .previousResourceState(baseModel().build())
                .build();
        ProgressEvent<ResourceModel, CallbackContext> handlerResponse =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // validate result
        validateCommonParameters(handlerResponse);
        verify(proxyClient.client()).updateFirewallDeleteProtection(any(UpdateFirewallDeleteProtectionRequest.class));
        verify(proxyClient.client(), times(2)).describeFirewall(any(DescribeFirewallRequest.class));

        // II. switch from true to false
        // call update handler with proper request parameters: switch from true to false
        desiredModel = baseModel().deleteProtection(false).build();
        ResourceModel previousModel = baseModel().deleteProtection(true).build();
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredModel)
                .previousResourceState(previousModel)
                .build();
        handlerResponse = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // validate result
        validateCommonParameters(handlerResponse);
        verify(proxyClient.client(), times(2)).updateFirewallDeleteProtection(any(UpdateFirewallDeleteProtectionRequest.class));

        // III. no switch
        // call update handler with proper request parameters: switch from true to true
        desiredModel = baseModel().deleteProtection(true).build();
        previousModel = baseModel().deleteProtection(true).build();
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredModel)
                .previousResourceState(previousModel)
                .build();
        handlerResponse = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // validate result
        validateCommonParameters(handlerResponse);
        verify(proxyClient.client(), times(2)).updateFirewallDeleteProtection(any(UpdateFirewallDeleteProtectionRequest.class));

        // validate none of the other update APIs are called
        verify(proxyClient.client(), never()).associateSubnets(any(AssociateSubnetsRequest.class));
        verify(proxyClient.client(), never()).disassociateSubnets(any(DisassociateSubnetsRequest.class));
        verify(proxyClient.client(), never()).updateFirewallDescription(any(UpdateFirewallDescriptionRequest.class));
        verify(proxyClient.client(), never()).updateFirewallPolicyChangeProtection(any(UpdateFirewallPolicyChangeProtectionRequest.class));
        verify(proxyClient.client(), never()).associateFirewallPolicy(any(AssociateFirewallPolicyRequest.class));
        verify(proxyClient.client(), never()).tagResource(any(TagResourceRequest.class));
        verify(proxyClient.client(), never()).untagResource(any(UntagResourceRequest.class));
        verify(proxyClient.client(), never()).updateSubnetChangeProtection(any(UpdateSubnetChangeProtectionRequest.class));
    }

    @Test
    public void verifyUpdateFirewallPolicyChangeProtection() {
        // setup mock requests and responses
        when(proxyClient.client().describeFirewall(any(DescribeFirewallRequest.class)))
                .thenReturn(commonDescribeResponse());

        // I. switch from false to true
        // call update handler with proper request parameters: switch from false to true
        ResourceModel desiredModel = baseModel().firewallPolicyChangeProtection(true).build();
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredModel)
                .previousResourceState(baseModel().build())
                .build();
        ProgressEvent<ResourceModel, CallbackContext> handlerResponse =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // validate result
        validateCommonParameters(handlerResponse);
        verify(proxyClient.client()).updateFirewallPolicyChangeProtection(any(UpdateFirewallPolicyChangeProtectionRequest.class));
        verify(proxyClient.client(), times(2)).describeFirewall(any(DescribeFirewallRequest.class));

        // II. switch from true to false
        // call update handler with proper request parameters: switch from true to false
        desiredModel = baseModel().firewallPolicyChangeProtection(false).build();
        ResourceModel previousModel = baseModel().firewallPolicyChangeProtection(true).build();
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredModel)
                .previousResourceState(previousModel)
                .build();
        handlerResponse = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // validate result
        validateCommonParameters(handlerResponse);
        verify(proxyClient.client(), times(2)).updateFirewallPolicyChangeProtection(any(UpdateFirewallPolicyChangeProtectionRequest.class));

        // III. no switch
        // call update handler with proper request parameters: switch from true to true
        desiredModel = baseModel().firewallPolicyChangeProtection(true).build();
        previousModel = baseModel().firewallPolicyChangeProtection(true).build();
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredModel)
                .previousResourceState(previousModel)
                .build();
        handlerResponse = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // validate result
        validateCommonParameters(handlerResponse);
        verify(proxyClient.client(), times(2)).updateFirewallPolicyChangeProtection(any(UpdateFirewallPolicyChangeProtectionRequest.class));

        // validate none of the other update APIs are called
        verify(proxyClient.client(), never()).associateSubnets(any(AssociateSubnetsRequest.class));
        verify(proxyClient.client(), never()).disassociateSubnets(any(DisassociateSubnetsRequest.class));
        verify(proxyClient.client(), never()).updateFirewallDescription(any(UpdateFirewallDescriptionRequest.class));
        verify(proxyClient.client(), never()).associateFirewallPolicy(any(AssociateFirewallPolicyRequest.class));
        verify(proxyClient.client(), never()).tagResource(any(TagResourceRequest.class));
        verify(proxyClient.client(), never()).untagResource(any(UntagResourceRequest.class));
        verify(proxyClient.client(), never()).updateSubnetChangeProtection(any(UpdateSubnetChangeProtectionRequest.class));
        verify(proxyClient.client(), never()).updateFirewallDeleteProtection(any(UpdateFirewallDeleteProtectionRequest.class));
    }

    @Test
    public void verifyAssociateFirewallPolicy() {
        // setup mock requests and responses
        SyncState syncState = SyncState.builder().config(ImmutableMap.of(
                "previousArn",
                PerObjectStatus.builder().syncStatus(PerObjectSyncStatus.IN_SYNC).build())).build();
        FirewallStatus firewallStatus = FirewallStatus.builder()
                .status(FirewallStatusValue.READY)
                .configurationSyncStateSummary(ConfigurationSyncState.IN_SYNC)
                .syncStates(ImmutableMap.of("east-1a", syncState)).build();
        DescribeFirewallResponse desiredFirewallPolicyMissing = DescribeFirewallResponse.builder()
                .firewall(commonDescribeResponse().firewall())
                .firewallStatus(firewallStatus).build();

        // build syncStatus with desiredArn in PENDING
        syncState = SyncState.builder().config(ImmutableMap.of(
                "previousArn", PerObjectStatus.builder().syncStatus(PerObjectSyncStatus.IN_SYNC).build(),
                "desiredArn", PerObjectStatus.builder().syncStatus(PerObjectSyncStatus.PENDING).build())).build();
        firewallStatus = FirewallStatus.builder()
                .status(FirewallStatusValue.READY)
                .configurationSyncStateSummary(ConfigurationSyncState.PENDING)
                .syncStates(ImmutableMap.of("east-1a", syncState)).build();
        DescribeFirewallResponse desiredFirewallPolicyPending = DescribeFirewallResponse.builder()
                .firewall(commonDescribeResponse().firewall())
                .firewallStatus(firewallStatus).build();

        // build syncStatus with desiredArn in IN_SYNC, but overall status in PENDING
        syncState = SyncState.builder().config(ImmutableMap.of(
                "previousArn", PerObjectStatus.builder().syncStatus(PerObjectSyncStatus.IN_SYNC).build(),
                "desiredArn", PerObjectStatus.builder().syncStatus(PerObjectSyncStatus.IN_SYNC).build())).build();
        firewallStatus = FirewallStatus.builder()
                .status(FirewallStatusValue.READY)
                .configurationSyncStateSummary(ConfigurationSyncState.PENDING)
                .syncStates(ImmutableMap.of("east-1a", syncState)).build();
        DescribeFirewallResponse desiredFirewallPolicyInSyncOverallPending = DescribeFirewallResponse.builder()
                .firewall(commonDescribeResponse().firewall())
                .firewallStatus(firewallStatus).build();

        // build syncStatus with desiredArn in IN_SYNC, and overall status in IN_SYNC
        syncState = SyncState.builder().config(ImmutableMap.of(
                "previousArn", PerObjectStatus.builder().syncStatus(PerObjectSyncStatus.IN_SYNC).build(),
                "desiredArn", PerObjectStatus.builder().syncStatus(PerObjectSyncStatus.IN_SYNC).build())).build();
        firewallStatus = FirewallStatus.builder()
                .status(FirewallStatusValue.READY)
                .configurationSyncStateSummary(ConfigurationSyncState.IN_SYNC)
                .syncStates(ImmutableMap.of("east-1a", syncState)).build();
        DescribeFirewallResponse desiredFirewallPolicyInSync = DescribeFirewallResponse.builder()
                .firewall(commonDescribeResponse().firewall())
                .firewallStatus(firewallStatus).build();

        when(proxyClient.client().describeFirewall(any(DescribeFirewallRequest.class)))
                .thenReturn(commonDescribeResponse()) // validation if resource exists
                .thenReturn(desiredFirewallPolicyMissing) // Stabilize 1: desiredArn not present on azSyncState
                .thenReturn(desiredFirewallPolicyPending) // Stabilize 2: desiredArn in PENDING STATE
                .thenReturn(desiredFirewallPolicyInSyncOverallPending) // Stabilize 3: desiredArn in IN_SYNC STATE, but overall in PENDING
                .thenReturn(desiredFirewallPolicyInSync) // Stabilize 4: desiredArn and overall in IN_SYNC STATE
                .thenReturn(commonDescribeResponse()); // last step of update operation

        // switch from previousArn to desiredArn
        // call update handler with proper request parameters
        ResourceModel desiredModel = baseModel().firewallPolicyArn("desiredArn").build();
        ResourceModel previousModel = baseModel().firewallPolicyArn("previousArn").build();
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredModel)
                .previousResourceState(previousModel)
                .build();
        ProgressEvent<ResourceModel, CallbackContext> handlerResponse =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // validate result
        validateCommonParameters(handlerResponse);
        verify(proxyClient.client(), times(4)).associateFirewallPolicy(any(AssociateFirewallPolicyRequest.class)); // TODO check
        verify(proxyClient.client(), times(6)).describeFirewall(any(DescribeFirewallRequest.class));

        // validate none of the other update APIs are called
        verify(proxyClient.client(), never()).associateSubnets(any(AssociateSubnetsRequest.class));
        verify(proxyClient.client(), never()).disassociateSubnets(any(DisassociateSubnetsRequest.class));
        verify(proxyClient.client(), never()).updateFirewallDescription(any(UpdateFirewallDescriptionRequest.class));
        verify(proxyClient.client(), never()).tagResource(any(TagResourceRequest.class));
        verify(proxyClient.client(), never()).untagResource(any(UntagResourceRequest.class));
        verify(proxyClient.client(), never()).updateSubnetChangeProtection(any(UpdateSubnetChangeProtectionRequest.class));
        verify(proxyClient.client(), never()).updateFirewallDeleteProtection(any(UpdateFirewallDeleteProtectionRequest.class));
        verify(proxyClient.client(), never()).updateFirewallPolicyChangeProtection(any(UpdateFirewallPolicyChangeProtectionRequest.class));
    }

    @Test
    public void verifyAssociateFirewallPolicyMultipleAZ() {
        // setup mock requests and responses
        SyncState desiredMissing = SyncState.builder().config(ImmutableMap.of(
                "previousArn",
                PerObjectStatus.builder().syncStatus(PerObjectSyncStatus.IN_SYNC).build())).build();
        SyncState desiredPending = SyncState.builder().config(ImmutableMap.of(
                "previousArn", PerObjectStatus.builder().syncStatus(PerObjectSyncStatus.IN_SYNC).build(),
                "desiredArn", PerObjectStatus.builder().syncStatus(PerObjectSyncStatus.PENDING).build())).build();
        SyncState desiredInSync = SyncState.builder().config(ImmutableMap.of(
                "previousArn", PerObjectStatus.builder().syncStatus(PerObjectSyncStatus.IN_SYNC).build(),
                "desiredArn", PerObjectStatus.builder().syncStatus(PerObjectSyncStatus.IN_SYNC).build())).build();

        // build syncStatus with desiredArn missing in 1a and 1b
        FirewallStatus firewallStatus = FirewallStatus.builder()
                .status(FirewallStatusValue.READY)
                .configurationSyncStateSummary(ConfigurationSyncState.IN_SYNC)
                .syncStates(ImmutableMap.of(
                        "east-1a", desiredMissing,
                        "east-1b", desiredMissing)).build();
        DescribeFirewallResponse d1 = DescribeFirewallResponse.builder()
                .firewall(commonDescribeResponse().firewall())
                .firewallStatus(firewallStatus).build();

        // build syncStatus with desiredArn in PENDING in 1a, but 1b does not have desiredArn yet
        firewallStatus = FirewallStatus.builder()
                .status(FirewallStatusValue.READY)
                .configurationSyncStateSummary(ConfigurationSyncState.PENDING)
                .syncStates(ImmutableMap.of(
                        "east-1a", desiredPending,
                        "east-1b", desiredMissing)).build();
        DescribeFirewallResponse d2 = DescribeFirewallResponse.builder()
                .firewall(commonDescribeResponse().firewall())
                .firewallStatus(firewallStatus).build();

        // build syncStatus with desiredArn in IN_SYNC in 1a, but 1b does not have desiredArn yet
        firewallStatus = FirewallStatus.builder()
                .status(FirewallStatusValue.READY)
                .configurationSyncStateSummary(ConfigurationSyncState.PENDING)
                .syncStates(ImmutableMap.of(
                        "east-1a", desiredInSync,
                        "east-1b", desiredMissing)).build();
        DescribeFirewallResponse d3 = DescribeFirewallResponse.builder()
                .firewall(commonDescribeResponse().firewall())
                .firewallStatus(firewallStatus).build();

        // build syncStatus with desiredArn in IN_SYNC in 1a, 1b has desiredArn in PENDING
        firewallStatus = FirewallStatus.builder()
                .status(FirewallStatusValue.READY)
                .configurationSyncStateSummary(ConfigurationSyncState.PENDING)
                .syncStates(ImmutableMap.of(
                        "east-1a", desiredInSync,
                        "east-1b", desiredPending)).build();
        DescribeFirewallResponse d4 = DescribeFirewallResponse.builder()
                .firewall(commonDescribeResponse().firewall())
                .firewallStatus(firewallStatus).build();

        // build syncStatus with desiredArn in IN_SYNC in 1a, 1b has desiredArn in IN_SYNC
        firewallStatus = FirewallStatus.builder()
                .status(FirewallStatusValue.READY)
                .configurationSyncStateSummary(ConfigurationSyncState.IN_SYNC)
                .syncStates(ImmutableMap.of(
                        "east-1a", desiredInSync,
                        "east-1b", desiredInSync)).build();
        DescribeFirewallResponse d5 = DescribeFirewallResponse.builder()
                .firewall(commonDescribeResponse().firewall())
                .firewallStatus(firewallStatus).build();

        when(proxyClient.client().describeFirewall(any(DescribeFirewallRequest.class)))
                .thenReturn(commonDescribeResponse()) // validation if resource exists
                .thenReturn(d1) // Stabilize 1: desiredArn not present in 1a and 1b
                .thenReturn(d2) // Stabilize 2: desiredArn in PENDING in 1a, missing in 1b
                .thenReturn(d3) // Stabilize 3: desiredArn in IN_SYNC in 1a, missing in 1b
                .thenReturn(d4) // Stabilize 4: desiredArn in IN_SYNC in 1a, PENDING in 1b
                .thenReturn(d5) // Stabilize 5: desiredArn in IN_SYNC in 1a, IN_SYNC in 1b
                .thenReturn(commonDescribeResponse()); // last step of update operation

        // switch from previousArn to desiredArn
        // call update handler with proper request parameters
        ResourceModel desiredModel = baseModel().firewallPolicyArn("desiredArn").build();
        ResourceModel previousModel = baseModel().firewallPolicyArn("previousArn").build();
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredModel)
                .previousResourceState(previousModel)
                .build();
        ProgressEvent<ResourceModel, CallbackContext> handlerResponse =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // validate result
        validateCommonParameters(handlerResponse);
        verify(proxyClient.client(), times(5)).associateFirewallPolicy(any(AssociateFirewallPolicyRequest.class)); // TODO check
        verify(proxyClient.client(), times(7)).describeFirewall(any(DescribeFirewallRequest.class));

        // validate none of the other update APIs are called
        verify(proxyClient.client(), never()).associateSubnets(any(AssociateSubnetsRequest.class));
        verify(proxyClient.client(), never()).disassociateSubnets(any(DisassociateSubnetsRequest.class));
        verify(proxyClient.client(), never()).updateFirewallDescription(any(UpdateFirewallDescriptionRequest.class));
        verify(proxyClient.client(), never()).tagResource(any(TagResourceRequest.class));
        verify(proxyClient.client(), never()).untagResource(any(UntagResourceRequest.class));
        verify(proxyClient.client(), never()).updateSubnetChangeProtection(any(UpdateSubnetChangeProtectionRequest.class));
        verify(proxyClient.client(), never()).updateFirewallDeleteProtection(any(UpdateFirewallDeleteProtectionRequest.class));
        verify(proxyClient.client(), never()).updateFirewallPolicyChangeProtection(any(UpdateFirewallPolicyChangeProtectionRequest.class));
    }

    @Test
    public void verifyAssociateFirewallPolicyNoSwitch() {
        // setup mock requests and responses
        when(proxyClient.client().describeFirewall(any(DescribeFirewallRequest.class)))
                .thenReturn(commonDescribeResponse());

        // call update handler with proper request parameters
        ResourceModel desiredModel = baseModel().firewallPolicyArn("desiredArn").build();
        ResourceModel previousModel = baseModel().firewallPolicyArn("desiredArn").build();
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredModel)
                .previousResourceState(previousModel)
                .build();
        ProgressEvent<ResourceModel, CallbackContext> handlerResponse =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // validate result
        validateCommonParameters(handlerResponse);
        verify(proxyClient.client(), never()).associateFirewallPolicy(any(AssociateFirewallPolicyRequest.class));
        verify(proxyClient.client(), times(2)).describeFirewall(any(DescribeFirewallRequest.class));

        // validate none of the other update APIs are called
        verify(proxyClient.client(), never()).associateSubnets(any(AssociateSubnetsRequest.class));
        verify(proxyClient.client(), never()).disassociateSubnets(any(DisassociateSubnetsRequest.class));
        verify(proxyClient.client(), never()).updateFirewallDescription(any(UpdateFirewallDescriptionRequest.class));
        verify(proxyClient.client(), never()).tagResource(any(TagResourceRequest.class));
        verify(proxyClient.client(), never()).untagResource(any(UntagResourceRequest.class));
        verify(proxyClient.client(), never()).updateSubnetChangeProtection(any(UpdateSubnetChangeProtectionRequest.class));
        verify(proxyClient.client(), never()).updateFirewallDeleteProtection(any(UpdateFirewallDeleteProtectionRequest.class));
        verify(proxyClient.client(), never()).updateFirewallPolicyChangeProtection(any(UpdateFirewallPolicyChangeProtectionRequest.class));
    }

    @Test
    public void verifyUpdateFirewallDescription() {
        // setup mock requests and responses
        when(proxyClient.client().describeFirewall(any(DescribeFirewallRequest.class)))
                .thenReturn(commonDescribeResponse());

        // I. switch from false to true
        // call update handler with proper request parameters: switch from false to true
        ResourceModel desiredModel = baseModel().description("desired description").build();
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredModel)
                .previousResourceState(baseModel().build())
                .build();
        ProgressEvent<ResourceModel, CallbackContext> handlerResponse =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // validate result
        validateCommonParameters(handlerResponse);
        verify(proxyClient.client()).updateFirewallDescription(any(UpdateFirewallDescriptionRequest.class));
        verify(proxyClient.client(), times(2)).describeFirewall(any(DescribeFirewallRequest.class));

        // II. switch from true to false
        // call update handler with proper request parameters: switch from true to false
        desiredModel = baseModel().description("desired description").build();
        ResourceModel previousModel = baseModel().description("previous description").build();
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredModel)
                .previousResourceState(previousModel)
                .build();
        handlerResponse = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // validate result
        validateCommonParameters(handlerResponse);
        verify(proxyClient.client(), times(2)).updateFirewallDescription(any(UpdateFirewallDescriptionRequest.class));

        // III. no switch
        // call update handler with proper request parameters: switch from true to true
        desiredModel = baseModel().description("desired description").build();
        previousModel = baseModel().description("desired description").build();
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredModel)
                .previousResourceState(previousModel)
                .build();
        handlerResponse = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // validate result
        validateCommonParameters(handlerResponse);
        verify(proxyClient.client(), times(2)).updateFirewallDescription(any(UpdateFirewallDescriptionRequest.class));

        // validate none of the other update APIs are called
        verify(proxyClient.client(), never()).associateSubnets(any(AssociateSubnetsRequest.class));
        verify(proxyClient.client(), never()).disassociateSubnets(any(DisassociateSubnetsRequest.class));
        verify(proxyClient.client(), never()).updateFirewallPolicyChangeProtection(any(UpdateFirewallPolicyChangeProtectionRequest.class));
        verify(proxyClient.client(), never()).associateFirewallPolicy(any(AssociateFirewallPolicyRequest.class));
        verify(proxyClient.client(), never()).tagResource(any(TagResourceRequest.class));
        verify(proxyClient.client(), never()).untagResource(any(UntagResourceRequest.class));
        verify(proxyClient.client(), never()).updateSubnetChangeProtection(any(UpdateSubnetChangeProtectionRequest.class));
        verify(proxyClient.client(), never()).updateFirewallDeleteProtection(any(UpdateFirewallDeleteProtectionRequest.class));
    }

    private ResourceModel.ResourceModelBuilder baseModel() {
        final Set<SubnetMapping> subnetMappings = ImmutableSet.of(new SubnetMapping("subnet-A"),
                new SubnetMapping("subnet-B"));
        return ResourceModel.builder()
                .firewallArn("validarn").firewallPolicyArn("policyArn").vpcId("vpcId").subnetMappings(subnetMappings);
    }

    @Test
    public void verifyAssociateSubnets() {
        // setup mock requests and responses
        SyncState subnet1ASyncState = SyncState.builder().attachment(
                Attachment.builder().subnetId("subnet-A").status(AttachmentStatus.READY).build()).build();
        FirewallStatus firewallStatus = FirewallStatus.builder()
                .status(FirewallStatusValue.READY)
                .configurationSyncStateSummary(ConfigurationSyncState.IN_SYNC)
                .syncStates(ImmutableMap.of("east-1a", subnet1ASyncState)).build();
        DescribeFirewallResponse d1 = DescribeFirewallResponse.builder()
                .firewall(commonDescribeResponse().firewall())
                .firewallStatus(firewallStatus).build();

        // new syncState created but without subnetID populated
        SyncState subnet1BSyncState = SyncState.builder().attachment(
                Attachment.builder().status(AttachmentStatus.CREATING).build()).build();
        firewallStatus = FirewallStatus.builder()
                .status(FirewallStatusValue.PROVISIONING)
                .configurationSyncStateSummary(ConfigurationSyncState.IN_SYNC)
                .syncStates(ImmutableMap.of("east-1a", subnet1ASyncState,"east-1b", subnet1BSyncState)).build();
        DescribeFirewallResponse d2 = DescribeFirewallResponse.builder()
                .firewall(commonDescribeResponse().firewall())
                .firewallStatus(firewallStatus).build();

        // sync state for 1B is populated with subnet name and status scaling
        subnet1BSyncState = SyncState.builder().attachment(
                Attachment.builder().subnetId("subnet-B").status(AttachmentStatus.SCALING).build()).build();
        firewallStatus = FirewallStatus.builder()
                .status(FirewallStatusValue.PROVISIONING)
                .configurationSyncStateSummary(ConfigurationSyncState.PENDING)
                .syncStates(ImmutableMap.of("east-1a", subnet1ASyncState,"east-1b", subnet1BSyncState)).build();
        DescribeFirewallResponse d3 = DescribeFirewallResponse.builder()
                .firewall(commonDescribeResponse().firewall())
                .firewallStatus(firewallStatus).build();

        // 1B is attached but the config sync summary is pending
        subnet1BSyncState = SyncState.builder().attachment(
                Attachment.builder().subnetId("subnet-B").status(AttachmentStatus.READY).build()).build();
        firewallStatus = FirewallStatus.builder()
                .status(FirewallStatusValue.READY)
                .configurationSyncStateSummary(ConfigurationSyncState.PENDING)
                .syncStates(ImmutableMap.of("east-1a", subnet1ASyncState,"east-1b", subnet1BSyncState)).build();
        DescribeFirewallResponse d4 = DescribeFirewallResponse.builder()
                .firewall(commonDescribeResponse().firewall())
                .firewallStatus(firewallStatus).build();

        // all is well for subnet-b but subnet-c is still not created
        subnet1BSyncState = SyncState.builder().attachment(
                Attachment.builder().subnetId("subnet-B").status(AttachmentStatus.READY).build()).build();
        firewallStatus = FirewallStatus.builder()
                .status(FirewallStatusValue.READY)
                .configurationSyncStateSummary(ConfigurationSyncState.IN_SYNC)
                .syncStates(ImmutableMap.of("east-1a", subnet1ASyncState,"east-1b", subnet1BSyncState)).build();
        DescribeFirewallResponse d5 = DescribeFirewallResponse.builder()
                .firewall(commonDescribeResponse().firewall())
                .firewallStatus(firewallStatus).build();

        // subnet-C is created and in SCALING state
        SyncState subnet1CSyncState = SyncState.builder().attachment(
                Attachment.builder().subnetId("subnet-C").status(AttachmentStatus.SCALING).build()).build();
        firewallStatus = FirewallStatus.builder()
                .status(FirewallStatusValue.PROVISIONING)
                .configurationSyncStateSummary(ConfigurationSyncState.IN_SYNC)
                .syncStates(ImmutableMap.of("east-1a", subnet1ASyncState,"east-1b", subnet1BSyncState,
                        "east-1c", subnet1CSyncState)).build();
        DescribeFirewallResponse d6 = DescribeFirewallResponse.builder()
                .firewall(commonDescribeResponse().firewall())
                .firewallStatus(firewallStatus).build();

        // all subnets created and in READY state
        subnet1CSyncState = SyncState.builder().attachment(
                Attachment.builder().subnetId("subnet-C").status(AttachmentStatus.READY).build()).build();
        firewallStatus = FirewallStatus.builder()
                .status(FirewallStatusValue.READY)
                .configurationSyncStateSummary(ConfigurationSyncState.IN_SYNC)
                .syncStates(ImmutableMap.of("east-1a", subnet1ASyncState,"east-1b", subnet1BSyncState,
                        "east-1c", subnet1CSyncState)).build();
        DescribeFirewallResponse d7 = DescribeFirewallResponse.builder()
                .firewall(commonDescribeResponse().firewall())
                .firewallStatus(firewallStatus).build();

        when(proxyClient.client().describeFirewall(any(DescribeFirewallRequest.class)))
                .thenReturn(commonDescribeResponse()) // validation if resource exists
                .thenReturn(d1) // Stabilize 1: subnet-B is not yet added to describe
                .thenReturn(d2) // Stabilize 2: synState record is created, but subnetId is not populated
                .thenReturn(d3) // Stabilize 3: subnet-B is in scaling state
                .thenReturn(d4) // Stabilize 4: subnet-B is in READY state, overallConfigSync is PENDING
                .thenReturn(d5) // Stabilize 5: subnet-B all in READY and IN_SYNC state | subnet-C not yet started
                .thenReturn(d6) // Stabilize 6: subnet-B all in READY and IN_SYNC state | subnet-C in SCALING
                .thenReturn(d7) // Stabilize 7: subnet-B | subnet-C all in READY and IN_SYNC state
                .thenReturn(commonDescribeResponse()); // last step of update operation

        // call update handler with proper request parameters
        final Set<SubnetMapping> previousSubnets = ImmutableSet.of(new SubnetMapping("subnet-A"));
        final Set<SubnetMapping> desiredSubnets = ImmutableSet.of(
                new SubnetMapping("subnet-A"), new SubnetMapping("subnet-B"),
                new SubnetMapping("subnet-C"));
        ResourceModel desiredModel = baseModel().subnetMappings(desiredSubnets).build();
        ResourceModel previousModel = baseModel().subnetMappings(previousSubnets).build();
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredModel)
                .previousResourceState(previousModel)
                .build();
        ProgressEvent<ResourceModel, CallbackContext> handlerResponse =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // validate result
        validateCommonParameters(handlerResponse);
        verify(proxyClient.client(), times(7)).associateSubnets(any(AssociateSubnetsRequest.class)); // TODO check

        // validate none of the other update APIs are called
        verify(proxyClient.client(), never()).associateFirewallPolicy(any(AssociateFirewallPolicyRequest.class));
        verify(proxyClient.client(), never()).disassociateSubnets(any(DisassociateSubnetsRequest.class));
        verify(proxyClient.client(), never()).updateFirewallDescription(any(UpdateFirewallDescriptionRequest.class));
        verify(proxyClient.client(), never()).tagResource(any(TagResourceRequest.class));
        verify(proxyClient.client(), never()).untagResource(any(UntagResourceRequest.class));
        verify(proxyClient.client(), never()).updateSubnetChangeProtection(any(UpdateSubnetChangeProtectionRequest.class));
        verify(proxyClient.client(), never()).updateFirewallDeleteProtection(any(UpdateFirewallDeleteProtectionRequest.class));
        verify(proxyClient.client(), never()).updateFirewallPolicyChangeProtection(any(UpdateFirewallPolicyChangeProtectionRequest.class));
    }

    @Test
    public void verifyDisassociateSubnets() {
        // setup mock requests and responses
        SyncState subnet1ASyncState = SyncState.builder().attachment(
                Attachment.builder().subnetId("subnet-A").status(AttachmentStatus.READY).build()).build();
        SyncState subnet1BSyncState = SyncState.builder().attachment(
                Attachment.builder().subnetId("subnet-B").status(AttachmentStatus.READY).build()).build();
        SyncState subnet1CSyncState = SyncState.builder().attachment(
                Attachment.builder().subnetId("subnet-C").status(AttachmentStatus.READY).build()).build();
        FirewallStatus firewallStatus = FirewallStatus.builder()
                .status(FirewallStatusValue.READY)
                .configurationSyncStateSummary(ConfigurationSyncState.IN_SYNC)
                .syncStates(ImmutableMap.of("east-1a", subnet1ASyncState, "east-1b", subnet1BSyncState,
                        "east-1c", subnet1CSyncState)).build();
        DescribeFirewallResponse d1 = DescribeFirewallResponse.builder()
                .firewall(commonDescribeResponse().firewall())
                .firewallStatus(firewallStatus).build();

        // Subnet-A: started to delete | Subnet-B: Not started
        subnet1ASyncState = SyncState.builder().attachment(
                Attachment.builder().subnetId("subnet-A").status(AttachmentStatus.SCALING).build()).build();
        firewallStatus = FirewallStatus.builder()
                .status(FirewallStatusValue.PROVISIONING)
                .configurationSyncStateSummary(ConfigurationSyncState.IN_SYNC)
                .syncStates(ImmutableMap.of("east-1a", subnet1ASyncState, "east-1b", subnet1BSyncState,
                        "east-1c", subnet1CSyncState)).build();
        DescribeFirewallResponse d2 = DescribeFirewallResponse.builder()
                .firewall(commonDescribeResponse().firewall())
                .firewallStatus(firewallStatus).build();

        // Subnet-A: deleting | Subnet-B: Not started
        subnet1ASyncState = SyncState.builder().attachment(
                Attachment.builder().subnetId("subnet-A").status(AttachmentStatus.DELETING).build()).build();
        firewallStatus = FirewallStatus.builder()
                .status(FirewallStatusValue.PROVISIONING)
                .configurationSyncStateSummary(ConfigurationSyncState.PENDING)
                .syncStates(ImmutableMap.of("east-1a", subnet1ASyncState, "east-1b", subnet1BSyncState,
                        "east-1c", subnet1CSyncState)).build();
        DescribeFirewallResponse d3 = DescribeFirewallResponse.builder()
                .firewall(commonDescribeResponse().firewall())
                .firewallStatus(firewallStatus).build();

        // Subnet-A: deleted | Subnet-B: Not started
        firewallStatus = FirewallStatus.builder()
                .status(FirewallStatusValue.READY)
                .configurationSyncStateSummary(ConfigurationSyncState.IN_SYNC)
                .syncStates(ImmutableMap.of("east-1b", subnet1BSyncState, "east-1c", subnet1CSyncState)).build();
        DescribeFirewallResponse d4 = DescribeFirewallResponse.builder()
                .firewall(commonDescribeResponse().firewall())
                .firewallStatus(firewallStatus).build();

        // Subnet-A: deleted | Subnet-B: deleting
        subnet1BSyncState = SyncState.builder().attachment(
                Attachment.builder().subnetId("subnet-B").status(AttachmentStatus.DELETING).build()).build();
        firewallStatus = FirewallStatus.builder()
                .status(FirewallStatusValue.PROVISIONING)
                .configurationSyncStateSummary(ConfigurationSyncState.IN_SYNC)
                .syncStates(ImmutableMap.of("east-1b", subnet1BSyncState, "east-1c", subnet1CSyncState)).build();
        DescribeFirewallResponse d5 = DescribeFirewallResponse.builder()
                .firewall(commonDescribeResponse().firewall())
                .firewallStatus(firewallStatus).build();

        // all subnets deleted and in READY state
        firewallStatus = FirewallStatus.builder()
                .status(FirewallStatusValue.READY)
                .configurationSyncStateSummary(ConfigurationSyncState.IN_SYNC)
                .syncStates(ImmutableMap.of("east-1c", subnet1CSyncState)).build();
        DescribeFirewallResponse d6 = DescribeFirewallResponse.builder()
                .firewall(commonDescribeResponse().firewall())
                .firewallStatus(firewallStatus).build();

        when(proxyClient.client().describeFirewall(any(DescribeFirewallRequest.class)))
                .thenReturn(commonDescribeResponse()) // validation if resource exists
                .thenReturn(d1) // Stabilize 1: Subnet-A: not started deleting | Subnet-B: not started deleting
                .thenReturn(d2) // Stabilize 2: Subnet-A: started to delete | Subnet-B: Not started
                .thenReturn(d3) // Stabilize 3: Subnet-A: deleting | Subnet-B: Not started
                .thenReturn(d4) // Stabilize 4: Subnet-A: deleted | Subnet-B: Not started
                .thenReturn(d5) // Stabilize 5: Subnet-A: deleted | Subnet-B: deleting
                .thenReturn(d6) // Stabilize 6: all subnets deleted and in READY state
                .thenReturn(commonDescribeResponse()); // last step of update operation

        // call update handler with proper request parameters
        final Set<SubnetMapping> previousSubnets = ImmutableSet.of(
                new SubnetMapping("subnet-A"), new SubnetMapping("subnet-B"), new SubnetMapping("subnet-C"));
        final Set<SubnetMapping> desiredSubnets = ImmutableSet.of(new SubnetMapping("subnet-C"));
        ResourceModel desiredModel = baseModel().subnetMappings(desiredSubnets).build();
        ResourceModel previousModel = baseModel().subnetMappings(previousSubnets).build();
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredModel)
                .previousResourceState(previousModel)
                .build();
        ProgressEvent<ResourceModel, CallbackContext> handlerResponse =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // validate result
        validateCommonParameters(handlerResponse);
        verify(proxyClient.client(), times(6)).disassociateSubnets(any(DisassociateSubnetsRequest.class)); // TODO check

        // validate none of the other update APIs are called
        verify(proxyClient.client(), never()).associateFirewallPolicy(any(AssociateFirewallPolicyRequest.class));
        verify(proxyClient.client(), never()).associateSubnets(any(AssociateSubnetsRequest.class));
        verify(proxyClient.client(), never()).updateFirewallDescription(any(UpdateFirewallDescriptionRequest.class));
        verify(proxyClient.client(), never()).tagResource(any(TagResourceRequest.class));
        verify(proxyClient.client(), never()).untagResource(any(UntagResourceRequest.class));
        verify(proxyClient.client(), never()).updateSubnetChangeProtection(any(UpdateSubnetChangeProtectionRequest.class));
        verify(proxyClient.client(), never()).updateFirewallDeleteProtection(any(UpdateFirewallDeleteProtectionRequest.class));
        verify(proxyClient.client(), never()).updateFirewallPolicyChangeProtection(any(UpdateFirewallPolicyChangeProtectionRequest.class));
    }

    @Test
    public void verifyTagUpdates() {
        // setup mock requests and responses
        when(proxyClient.client().describeFirewall(any(DescribeFirewallRequest.class)))
                .thenReturn(commonDescribeResponse());

        // I. add 1 new set of tags
        // call update handler with proper request parameters
        Map<String, String> previousTags = ImmutableMap.of("key1", "value1", "key2", "value2");
        Map<String, String> desiredTags = ImmutableMap.of("key1", "value1", "key2", "value2", "key3", "value3");
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceTags(previousTags)
                .desiredResourceTags(desiredTags)
                .previousResourceState(baseModel().build())
                .desiredResourceState(baseModel().build())
                .build();
        ProgressEvent<ResourceModel, CallbackContext> handlerResponse =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // validate result
        validateCommonParameters(handlerResponse);
        verify(proxyClient.client()).tagResource(any(TagResourceRequest.class));
        verify(proxyClient.client(), times(2)).describeFirewall(any(DescribeFirewallRequest.class));

        // clear before next test
        Mockito.clearInvocations(client);

        // II. remove 2 tags and add 1 tag
        // call update handler with proper request parameters
        previousTags = ImmutableMap.of("key1", "value1", "key2", "value2");
        desiredTags = ImmutableMap.of("key3", "value3");
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceTags(previousTags)
                .desiredResourceTags(desiredTags)
                .previousResourceState(baseModel().build())
                .desiredResourceState(baseModel().build())
                .build();
        handlerResponse = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // validate result
        validateCommonParameters(handlerResponse);
        verify(proxyClient.client()).tagResource(any(TagResourceRequest.class));
        verify(proxyClient.client()).untagResource(any(UntagResourceRequest.class));
        verify(proxyClient.client(), times(2)).describeFirewall(any(DescribeFirewallRequest.class));

        // clear before next test
        Mockito.clearInvocations(client);

        // III. no change
        // call update handler with proper request parameters
        previousTags = ImmutableMap.of("key1", "value1", "key2", "value2");
        desiredTags = ImmutableMap.of("key1", "value1", "key2", "value2");
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceTags(previousTags)
                .desiredResourceTags(desiredTags)
                .previousResourceState(baseModel().build())
                .desiredResourceState(baseModel().build())
                .build();
        handlerResponse = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // validate result
        validateCommonParameters(handlerResponse);
        verify(proxyClient.client(), never()).tagResource(any(TagResourceRequest.class));
        verify(proxyClient.client(), never()).untagResource(any(UntagResourceRequest.class));
        verify(proxyClient.client(), times(2)).describeFirewall(any(DescribeFirewallRequest.class));

        // clear before next test
        Mockito.clearInvocations(client);

        // IV. no new tag, but updated value for an existing tag
        // call update handler with proper request parameters
        previousTags = ImmutableMap.of("key1", "value1", "key2", "value2");
        desiredTags = ImmutableMap.of("key1", "value1", "key2", "changeValue1");
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceTags(previousTags)
                .desiredResourceTags(desiredTags)
                .previousResourceState(baseModel().build())
                .desiredResourceState(baseModel().build())
                .build();
        handlerResponse = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // validate result
        validateCommonParameters(handlerResponse);
        verify(proxyClient.client()).tagResource(any(TagResourceRequest.class));
        verify(proxyClient.client(), never()).untagResource(any(UntagResourceRequest.class));
        verify(proxyClient.client(), times(2)).describeFirewall(any(DescribeFirewallRequest.class));

        // clear before next test
        Mockito.clearInvocations(client);

        // V. only resource request tags
        // no stack tags | add 1 and remove 1 of resource request tag
        Set<Tag> previousResourceTags = ImmutableSet.of(Tag.builder().key("Request-key-2").value("2-value").build());
        Set<Tag> desiredResourceTags = ImmutableSet.of(Tag.builder().key("Request-key-1").value("1-value").build());
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceTags(null)
                .desiredResourceTags(null)
                .previousResourceState(baseModel().tags(previousResourceTags).build())
                .desiredResourceState(baseModel().tags(desiredResourceTags).build())
                .build();
        handlerResponse = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // validate result
        validateCommonParameters(handlerResponse);
        verify(proxyClient.client()).tagResource(any(TagResourceRequest.class));
        verify(proxyClient.client()).untagResource(any(UntagResourceRequest.class));
        verify(proxyClient.client(), times(2)).describeFirewall(any(DescribeFirewallRequest.class));

        // clear before next test
        Mockito.clearInvocations(client);

        // VI. only resource request tags
        // 1 resource request tag overrides a stack key
        previousTags = ImmutableMap.of("stackKey1", "value1");
        desiredTags = ImmutableMap.of("stackKey1", "value1");

        previousResourceTags = ImmutableSet.of();
        desiredResourceTags = ImmutableSet.of(Tag.builder().key("stackKey1").value("overridden").build());
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceTags(previousTags)
                .desiredResourceTags(desiredTags)
                .previousResourceState(baseModel().tags(previousResourceTags).build())
                .desiredResourceState(baseModel().tags(desiredResourceTags).build())
                .build();
        handlerResponse = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // validate result
        validateCommonParameters(handlerResponse);
        verify(proxyClient.client()).tagResource(any(TagResourceRequest.class));
        verify(proxyClient.client(), never()).untagResource(any(UntagResourceRequest.class));
        verify(proxyClient.client(), times(2)).describeFirewall(any(DescribeFirewallRequest.class));

        // clear before next test
        Mockito.clearInvocations(client);

        // VII. only resource request tags
        // add a tag because we remove 1 resource request tag | remove 1 resource request tag
        previousTags = ImmutableMap.of("stackKey1", "value1", "newOneAdd", "value1");
        desiredTags = ImmutableMap.of("stackKey1", "value1", "newOneAdd", "value1");

        previousResourceTags = ImmutableSet.of(Tag.builder().key("newOneAdd").value("overriddenValue").build());
        desiredResourceTags = ImmutableSet.of();
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceTags(previousTags)
                .desiredResourceTags(desiredTags)
                .previousResourceState(baseModel().tags(previousResourceTags).build())
                .desiredResourceState(baseModel().tags(desiredResourceTags).build())
                .build();
        handlerResponse = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // validate result
        validateCommonParameters(handlerResponse);
        verify(proxyClient.client()).tagResource(any(TagResourceRequest.class));
        verify(proxyClient.client(), never()).untagResource(any(UntagResourceRequest.class));
        verify(proxyClient.client(), times(2)).describeFirewall(any(DescribeFirewallRequest.class));

        // validate none of the other update APIs are called
        verify(proxyClient.client(), never()).associateSubnets(any(AssociateSubnetsRequest.class));
        verify(proxyClient.client(), never()).disassociateSubnets(any(DisassociateSubnetsRequest.class));
        verify(proxyClient.client(), never()).updateFirewallDescription(any(UpdateFirewallDescriptionRequest.class));
        verify(proxyClient.client(), never()).updateFirewallPolicyChangeProtection(any(UpdateFirewallPolicyChangeProtectionRequest.class));
        verify(proxyClient.client(), never()).updateFirewallDeleteProtection(any(UpdateFirewallDeleteProtectionRequest.class));
        verify(proxyClient.client(), never()).associateFirewallPolicy(any(AssociateFirewallPolicyRequest.class));
        verify(proxyClient.client(), never()).updateSubnetChangeProtection(any(UpdateSubnetChangeProtectionRequest.class));
    }

    private void validateCommonParameters(final ProgressEvent<ResourceModel, CallbackContext> handlerResponse) {
        assertThat(handlerResponse).isNotNull();
        assertThat(handlerResponse.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(handlerResponse.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(handlerResponse.getResourceModel()).isNotNull();
        assertThat(handlerResponse.getResourceModels()).isNull();
        assertThat(handlerResponse.getMessage()).isNull();
        assertThat(handlerResponse.getErrorCode()).isNull();
    }
}
