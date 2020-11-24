package software.amazon.networkfirewall.rulegroup;

import java.time.Duration;
import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.awssdk.services.networkfirewall.model.DeleteRuleGroupRequest;
import software.amazon.awssdk.services.networkfirewall.model.DeleteRuleGroupResponse;
import software.amazon.awssdk.services.networkfirewall.model.DescribeRuleGroupRequest;
import software.amazon.awssdk.services.networkfirewall.model.DescribeRuleGroupResponse;
import software.amazon.awssdk.services.networkfirewall.model.InvalidRequestException;
import software.amazon.awssdk.services.networkfirewall.model.ResourceNotFoundException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends AbstractTestBase {
    private DeleteHandler handler;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<NetworkFirewallClient> proxyClient;

    @Mock
    NetworkFirewallClient networkFirewallClient;

    @BeforeEach
    public void setup() {
        handler = new DeleteHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        networkFirewallClient = mock(NetworkFirewallClient.class);
        proxyClient = MOCK_PROXY(proxy, networkFirewallClient);
    }

    @AfterEach
    public void tear_down() {
        verify(networkFirewallClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(networkFirewallClient);
    }

    @Test
    public void testHandleRequest_deleteStatelessRuleGroupSuccess() {
        // Delete succeeds instantly
        final ResourceModel model = ResourceModel
                .builder()
                .ruleGroupArn(STATELESS_RULEGROUP_ARN)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        DeleteRuleGroupRequest deleteStatelessRuleGroupRequest = DeleteRuleGroupRequest.builder()
                .ruleGroupArn(STATELESS_RULEGROUP_ARN)
                .build();
        DeleteRuleGroupResponse deleteStatelessRuleGroupResponse = DeleteRuleGroupResponse.builder()
                .ruleGroupResponse(statelessSdkRuleGroupResponseWithNoTags)
                .build();
        DescribeRuleGroupRequest describeRuleGroupRequest = DescribeRuleGroupRequest.builder()
                .ruleGroupArn(STATELESS_RULEGROUP_ARN)
                .build();

        when(proxyClient.injectCredentialsAndInvokeV2(deleteStatelessRuleGroupRequest, networkFirewallClient::deleteRuleGroup)).thenReturn(deleteStatelessRuleGroupResponse);
        when(proxyClient.injectCredentialsAndInvokeV2(describeRuleGroupRequest, networkFirewallClient::describeRuleGroup)).thenThrow(ResourceNotFoundException.class);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        verify(proxyClient.client(), times(1)).deleteRuleGroup(any(DeleteRuleGroupRequest.class));
        verify(proxyClient.client(), times(1)).describeRuleGroup(any(DescribeRuleGroupRequest.class));

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void testHandleRequest_deleteStatelessRuleGroupSuccess2() {
        // Delete succeeds after few calls
        final ResourceModel model = ResourceModel
                .builder()
                .ruleGroupArn(STATELESS_RULEGROUP_ARN)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        DeleteRuleGroupRequest deleteStatelessRuleGroupRequest = DeleteRuleGroupRequest.builder()
                .ruleGroupArn(STATELESS_RULEGROUP_ARN)
                .build();
        DeleteRuleGroupResponse deleteStatelessRuleGroupResponse = DeleteRuleGroupResponse.builder()
                .ruleGroupResponse(statelessSdkRuleGroupResponseWithNoTags)
                .build();
        DescribeRuleGroupRequest describeRuleGroupRequest = DescribeRuleGroupRequest.builder()
                .ruleGroupArn(STATELESS_RULEGROUP_ARN)
                .build();
        DescribeRuleGroupResponse describeRuleGroupResponse = DescribeRuleGroupResponse.builder()
                .ruleGroupResponse(statelessSdkRuleGroupResponseWithNoTags)
                .build();

        when(proxyClient.injectCredentialsAndInvokeV2(deleteStatelessRuleGroupRequest, networkFirewallClient::deleteRuleGroup)).thenReturn(deleteStatelessRuleGroupResponse);
        when(proxyClient.injectCredentialsAndInvokeV2(describeRuleGroupRequest, networkFirewallClient::describeRuleGroup)).thenReturn(describeRuleGroupResponse).thenReturn(describeRuleGroupResponse).thenThrow(ResourceNotFoundException.class);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        verify(proxyClient.client(), times(1)).deleteRuleGroup(any(DeleteRuleGroupRequest.class));
        verify(proxyClient.client(), times(3)).describeRuleGroup(any(DescribeRuleGroupRequest.class));

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void testHandleRequest_deleteStatefulRuleGroup() {
        final ResourceModel model = ResourceModel
                .builder()
                .ruleGroupName(STATEFUL_RULEGROUP_NAME)
                .type(STATEFUL_RULEGROUP_TYPE)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        DeleteRuleGroupRequest deleteStatefulRuleGroupRequest = DeleteRuleGroupRequest.builder()
                .ruleGroupName(STATEFUL_RULEGROUP_NAME)
                .type(STATEFUL_RULEGROUP_TYPE)
                .build();
        DeleteRuleGroupResponse deleteStatefulRuleGroupResponse = DeleteRuleGroupResponse.builder()
                .ruleGroupResponse(statefulSdkRuleGroupResponseWithTags)
                .build();
        DescribeRuleGroupRequest describeRuleGroupRequest = DescribeRuleGroupRequest.builder()
                .ruleGroupName(STATEFUL_RULEGROUP_NAME)
                .type(STATEFUL_RULEGROUP_TYPE)
                .build();

        when(proxyClient.injectCredentialsAndInvokeV2(deleteStatefulRuleGroupRequest, networkFirewallClient::deleteRuleGroup)).thenReturn(deleteStatefulRuleGroupResponse);
        when(proxyClient.injectCredentialsAndInvokeV2(describeRuleGroupRequest, networkFirewallClient::describeRuleGroup)).thenThrow(ResourceNotFoundException.class);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        verify(proxyClient.client(), times(1)).deleteRuleGroup(any(DeleteRuleGroupRequest.class));
        verify(proxyClient.client(), times(1)).describeRuleGroup(any(DescribeRuleGroupRequest.class));

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void testHandleRequest_throwsResourceNotFoundException() {
        final ResourceModel model = ResourceModel
                .builder()
                .ruleGroupName(STATEFUL_RULEGROUP_NAME)
                .type(STATEFUL_RULEGROUP_TYPE)
                .ruleGroupArn(STATEFUL_RULEGROUP_ARN)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        DeleteRuleGroupRequest deleteStatefulRuleGroupRequest = DeleteRuleGroupRequest.builder()
                .ruleGroupArn(STATEFUL_RULEGROUP_ARN)
                .ruleGroupName(STATEFUL_RULEGROUP_NAME)
                .type(STATEFUL_RULEGROUP_TYPE)
                .build();

        when(proxyClient.injectCredentialsAndInvokeV2(deleteStatefulRuleGroupRequest, networkFirewallClient::deleteRuleGroup)).thenThrow(ResourceNotFoundException.class);

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        verify(proxyClient.client(), times(1)).deleteRuleGroup(any(DeleteRuleGroupRequest.class));
        verify(proxyClient.client(), never()).describeRuleGroup(any(DescribeRuleGroupRequest.class));
    }

    @Test
    public void testHandleRequest_deleteRuleGroup_throwsException() {
        final ResourceModel model = ResourceModel
                .builder()
                .ruleGroupName(STATEFUL_RULEGROUP_NAME)
                .type(STATEFUL_RULEGROUP_TYPE)
                .ruleGroupArn(STATEFUL_RULEGROUP_ARN)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        DeleteRuleGroupRequest deleteStatefulRuleGroupRequest = DeleteRuleGroupRequest.builder()
                .ruleGroupArn(STATEFUL_RULEGROUP_ARN)
                .ruleGroupName(STATEFUL_RULEGROUP_NAME)
                .type(STATEFUL_RULEGROUP_TYPE)
                .build();

        when(proxyClient.injectCredentialsAndInvokeV2(deleteStatefulRuleGroupRequest, networkFirewallClient::deleteRuleGroup)).thenThrow(InvalidRequestException.class);

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        verify(proxyClient.client(), times(1)).deleteRuleGroup(any(DeleteRuleGroupRequest.class));
        verify(proxyClient.client(), never()).describeRuleGroup(any(DescribeRuleGroupRequest.class));
    }

}
