package software.amazon.networkfirewall.rulegroup;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.awssdk.services.networkfirewall.model.CreateRuleGroupRequest;
import software.amazon.awssdk.services.networkfirewall.model.DescribeRuleGroupRequest;
import software.amazon.awssdk.services.networkfirewall.model.DescribeRuleGroupResponse;
import software.amazon.awssdk.services.networkfirewall.model.InsufficientCapacityException;
import software.amazon.awssdk.services.networkfirewall.model.InternalServerErrorException;
import software.amazon.awssdk.services.networkfirewall.model.InvalidOperationException;
import software.amazon.awssdk.services.networkfirewall.model.InvalidRequestException;
import software.amazon.awssdk.services.networkfirewall.model.LimitExceededException;
import software.amazon.awssdk.services.networkfirewall.model.ResourceNotFoundException;
import software.amazon.awssdk.services.networkfirewall.model.ThrottlingException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
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
public class CreateHandlerTest extends AbstractTestBase {
    private static ResourceHandlerRequest<ResourceModel> request;
    private CreateHandler handler;
    ResourceModel model;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<NetworkFirewallClient> proxyClient;

    @Mock
    NetworkFirewallClient networkFirewallClient;

    @BeforeEach
    public void setup() {
        handler = new CreateHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        networkFirewallClient = mock(NetworkFirewallClient.class);
        proxyClient = MOCK_PROXY(proxy, networkFirewallClient);

        setupRuleGroupTest();

        model = ResourceModel
                .builder()
                .ruleGroupName(STATELESS_RULEGROUP_NAME)
                .ruleGroup(cfnStatelessRuleGroup1)
                .description(DESCRIPTION)
                .capacity(CAPACITY)
                .type(STATELESS_RULEGROUP_TYPE)
                .build();
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();
    }

    @AfterEach
    public void tear_down() {
        verify(networkFirewallClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(networkFirewallClient);
    }

    @Test
    public void testHandleRequest_createStatelessRuleGroupSuccess1() {
        // create request with stateless-rulegroup - one StatelessRule
        model = ResourceModel
                .builder()
                .ruleGroupName(STATELESS_RULEGROUP_NAME)
                .ruleGroup(cfnStatelessRuleGroup1)
                .description(DESCRIPTION)
                .capacity(CAPACITY)
                .type(STATELESS_RULEGROUP_TYPE)
                .build();
        request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        when(proxyClient.injectCredentialsAndInvokeV2(createStatelessRuleGroupRequest1, networkFirewallClient::createRuleGroup)).thenReturn(createStatelessRuleGroupResponse1);
        when(proxyClient.injectCredentialsAndInvokeV2(describeCreateStatelessRuleGroupRequest1, networkFirewallClient::describeRuleGroup)).thenReturn(describeCreateStatelessRuleGroupResponse1);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        verify(proxyClient.client(), times(1)).createRuleGroup(any(CreateRuleGroupRequest.class));
        verify(proxyClient.client(), times(2)).describeRuleGroup(any(DescribeRuleGroupRequest.class));

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel().getRuleGroupName()).isEqualTo(request.getDesiredResourceState().getRuleGroupName());
        assertThat(response.getResourceModel().getType()).isEqualTo(request.getDesiredResourceState().getType());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        // Validate ResourceModel
        validateStatelessResourceModel(response.getResourceModel(), cfnStatelessRuleGroup1, new HashSet<Tag>());
    }

    @Test
    public void testHandleRequest_createStatelessRuleGroupSuccess2() {
        // create request with stateless-rulegroup - Multiple StatelessRules with almost all the attributes set
        model = ResourceModel
                .builder()
                .ruleGroupName(STATELESS_RULEGROUP_NAME)
                .ruleGroup(cfnStatelessRuleGroup2)
                .description(DESCRIPTION)
                .capacity(CAPACITY)
                .type(STATELESS_RULEGROUP_TYPE)
                .tags(statelessTags)
                .build();
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.injectCredentialsAndInvokeV2(createStatelessRuleGroupRequest2, networkFirewallClient::createRuleGroup)).thenReturn(createStatelessRuleGroupResponse2);
        when(proxyClient.injectCredentialsAndInvokeV2(describeCreateStatelessRuleGroupRequest2, networkFirewallClient::describeRuleGroup)).thenReturn(describeCreateStatelessRuleGroupResponse2);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        verify(proxyClient.client(), times(1)).createRuleGroup(any(CreateRuleGroupRequest.class));
        verify(proxyClient.client(), times(2)).describeRuleGroup(any(DescribeRuleGroupRequest.class));

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel().getRuleGroupName()).isEqualTo(request.getDesiredResourceState().getRuleGroupName());
        assertThat(response.getResourceModel().getType()).isEqualTo(request.getDesiredResourceState().getType());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        // Validate ResourceModel
        validateStatelessResourceModel(response.getResourceModel(), cfnStatelessRuleGroup2, statelessTags);
    }

    @Test
    public void testHandleRequest_createStatelessRuleGroupSuccess3() {
        // create request with stateless-rulegroup - StatelessRule & CustomActions
        model = ResourceModel
                .builder()
                .ruleGroupName(STATELESS_RULEGROUP_NAME)
                .ruleGroup(cfnStatelessRuleGroup3)
                .description(DESCRIPTION)
                .capacity(CAPACITY)
                .type(STATELESS_RULEGROUP_TYPE)
                .tags(statelessTags)
                .build();
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.injectCredentialsAndInvokeV2(createStatelessRuleGroupRequest3, networkFirewallClient::createRuleGroup)).thenReturn(createStatelessRuleGroupResponse3);
        when(proxyClient.injectCredentialsAndInvokeV2(describeCreateStatelessRuleGroupRequest3, networkFirewallClient::describeRuleGroup)).thenReturn(describeCreateStatelessRuleGroupResponse3);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        verify(proxyClient.client(), times(1)).createRuleGroup(any(CreateRuleGroupRequest.class));
        verify(proxyClient.client(), times(2)).describeRuleGroup(any(DescribeRuleGroupRequest.class));

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel().getRuleGroupName()).isEqualTo(request.getDesiredResourceState().getRuleGroupName());
        assertThat(response.getResourceModel().getType()).isEqualTo(request.getDesiredResourceState().getType());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        // Validate ResourceModel
        validateStatelessResourceModel(response.getResourceModel(), cfnStatelessRuleGroup3, statelessTags);
    }

    @Test
    public void testHandleRequest_deletingStateThrowsCfnGeneralServiceException() {
        model = ResourceModel
                .builder()
                .ruleGroupName(STATELESS_RULEGROUP_NAME)
                .ruleGroup(cfnStatelessRuleGroup1)
                .description(DESCRIPTION)
                .capacity(CAPACITY)
                .type(STATELESS_RULEGROUP_TYPE)
                .build();
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        software.amazon.awssdk.services.networkfirewall.model.RuleGroupResponse statelessSdkRuleGroupResponse = software.amazon.awssdk.services.networkfirewall.model.RuleGroupResponse.builder()
                .capacity(CAPACITY)
                .description(DESCRIPTION)
                .ruleGroupArn(STATELESS_RULEGROUP_ARN)
                .ruleGroupId(STATELESS_RULEGROUP_ID)
                .ruleGroupName(STATELESS_RULEGROUP_NAME)
                .ruleGroupStatus(DELETING) // resource is in DELETING state
                .type(STATELESS_RULEGROUP_TYPE)
                .build();

        DescribeRuleGroupResponse describeResponse = DescribeRuleGroupResponse.builder()
                .ruleGroupResponse(statelessSdkRuleGroupResponse)
                .ruleGroup(statelessSdkRuleGroup1)
                .updateToken(UPDATE_TOKEN)
                .build();

        when(proxyClient.injectCredentialsAndInvokeV2(createStatelessRuleGroupRequest1, networkFirewallClient::createRuleGroup)).thenReturn(createStatelessRuleGroupResponse1);
        when(proxyClient.injectCredentialsAndInvokeV2(describeCreateStatelessRuleGroupRequest1, networkFirewallClient::describeRuleGroup)).thenReturn(describeResponse);

        assertThrows(CfnGeneralServiceException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        verify(proxyClient.client(), times(1)).createRuleGroup(any(CreateRuleGroupRequest.class));
        verify(proxyClient.client(), times(1)).describeRuleGroup(any(DescribeRuleGroupRequest.class));
    }

    @Test
    public void testHandleRequest_createStatefulRuleGroupSuccess1() {
        // create request with stateful-rulegroup - with RuleString
        model = ResourceModel
                .builder()
                .ruleGroupName(STATEFUL_RULEGROUP_NAME)
                .ruleGroup(cfnStatefulRuleGroup1)
                .description(DESCRIPTION)
                .capacity(CAPACITY)
                .type(STATEFUL_RULEGROUP_TYPE)
                .build();
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.injectCredentialsAndInvokeV2(createStatefulRuleGroupRequest1, networkFirewallClient::createRuleGroup)).thenReturn(createStatefulRuleGroupResponse1);
        when(proxyClient.injectCredentialsAndInvokeV2(describeCreateStatefulRuleGroupRequest1, networkFirewallClient::describeRuleGroup)).thenReturn(describeCreateStatefulRuleGroupResponse1);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        verify(proxyClient.client(), times(1)).createRuleGroup(any(CreateRuleGroupRequest.class));
        verify(proxyClient.client(), times(2)).describeRuleGroup(any(DescribeRuleGroupRequest.class));

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel().getRuleGroupName()).isEqualTo(request.getDesiredResourceState().getRuleGroupName());
        assertThat(response.getResourceModel().getType()).isEqualTo(request.getDesiredResourceState().getType());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        // Validate ResourceModel
        validateStatefulResourceModel(response.getResourceModel(), cfnStatefulRuleGroup1, new HashSet<Tag>());
    }

    @Test
    public void testHandleRequest_createStatefulRuleGroupSuccess2() {
        // create request with stateful-rulegroup - with RuleSourceList
        model = ResourceModel
                .builder()
                .ruleGroupName(STATEFUL_RULEGROUP_NAME)
                .ruleGroup(cfnStatefulRuleGroup2)
                .description(DESCRIPTION)
                .capacity(CAPACITY)
                .type(STATEFUL_RULEGROUP_TYPE)
                .tags(statefulTags)
                .build();
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.injectCredentialsAndInvokeV2(createStatefulRuleGroupRequest2, networkFirewallClient::createRuleGroup)).thenReturn(createStatefulRuleGroupResponse2);
        when(proxyClient.injectCredentialsAndInvokeV2(describeCreateStatefulRuleGroupRequest2, networkFirewallClient::describeRuleGroup)).thenReturn(describeCreateStatefulRuleGroupResponse2);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        verify(proxyClient.client(), times(1)).createRuleGroup(any(CreateRuleGroupRequest.class));
        verify(proxyClient.client(), times(2)).describeRuleGroup(any(DescribeRuleGroupRequest.class));

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel().getRuleGroupName()).isEqualTo(request.getDesiredResourceState().getRuleGroupName());
        assertThat(response.getResourceModel().getType()).isEqualTo(request.getDesiredResourceState().getType());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        // Validate ResourceModel
        validateStatefulResourceModel(response.getResourceModel(), cfnStatefulRuleGroup2, statefulTags);
    }

    @Test
    public void testHandleRequest_createStatefulRuleGroupSuccess3() {
        // create request with stateful-rulegroup - with StatefulRule
        model = ResourceModel
                .builder()
                .ruleGroupName(STATEFUL_RULEGROUP_NAME)
                .ruleGroup(cfnStatefulRuleGroup3)
                .description(DESCRIPTION)
                .capacity(CAPACITY)
                .type(STATEFUL_RULEGROUP_TYPE)
                .tags(statefulTags)
                .build();
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.injectCredentialsAndInvokeV2(createStatefulRuleGroupRequest3, networkFirewallClient::createRuleGroup)).thenReturn(createStatefulRuleGroupResponse3);
        when(proxyClient.injectCredentialsAndInvokeV2(describeCreateStatefulRuleGroupRequest3, networkFirewallClient::describeRuleGroup)).thenReturn(describeCreateStatefulRuleGroupResponse3);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        verify(proxyClient.client(), times(1)).createRuleGroup(any(CreateRuleGroupRequest.class));
        verify(proxyClient.client(), times(2)).describeRuleGroup(any(DescribeRuleGroupRequest.class));

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel().getRuleGroupName()).isEqualTo(request.getDesiredResourceState().getRuleGroupName());
        assertThat(response.getResourceModel().getType()).isEqualTo(request.getDesiredResourceState().getType());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        // Validate ResourceModel
        validateStatefulResourceModel(response.getResourceModel(), cfnStatefulRuleGroup3, statefulTags);
    }

    @Test
    public void testHandleRequest_createStatefulRuleGroupSuccess4() {
        // create request with stateful-rulegroup - with RuleVariables
        model = ResourceModel
                .builder()
                .ruleGroupName(STATEFUL_RULEGROUP_NAME)
                .ruleGroup(cfnStatefulRuleGroup4)
                .description(DESCRIPTION)
                .capacity(CAPACITY)
                .type(STATEFUL_RULEGROUP_TYPE)
                .tags(statefulTags)
                .build();
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.injectCredentialsAndInvokeV2(createStatefulRuleGroupRequest4, networkFirewallClient::createRuleGroup)).thenReturn(createStatefulRuleGroupResponse4);
        when(proxyClient.injectCredentialsAndInvokeV2(describeCreateStatefulRuleGroupRequest4, networkFirewallClient::describeRuleGroup)).thenReturn(describeCreateStatefulRuleGroupResponse4);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        verify(proxyClient.client(), times(1)).createRuleGroup(any(CreateRuleGroupRequest.class));
        verify(proxyClient.client(), times(2)).describeRuleGroup(any(DescribeRuleGroupRequest.class));

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel().getRuleGroupName()).isEqualTo(request.getDesiredResourceState().getRuleGroupName());
        assertThat(response.getResourceModel().getType()).isEqualTo(request.getDesiredResourceState().getType());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        // Validate ResourceModel
        validateStatefulResourceModel(response.getResourceModel(), cfnStatefulRuleGroup4, statefulTags);
    }

    @Test
    public void testHandleRequest_throwsInvalidRequestException() {
        when(proxyClient.injectCredentialsAndInvokeV2(createStatelessRuleGroupRequest1, networkFirewallClient::createRuleGroup)).thenThrow(InvalidRequestException.class);

        verify(proxyClient.client(), never()).createRuleGroup(createStatelessRuleGroupRequest1);
        verify(proxyClient.client(), never()).describeRuleGroup(describeCreateStatelessRuleGroupRequest1);

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void testHandleRequest_throwsThrottlingException() {
        when(proxyClient.injectCredentialsAndInvokeV2(createStatelessRuleGroupRequest1, networkFirewallClient::createRuleGroup)).thenThrow(ThrottlingException.class);

        verify(proxyClient.client(), never()).createRuleGroup(createStatelessRuleGroupRequest1);
        verify(proxyClient.client(), never()).describeRuleGroup(describeCreateStatelessRuleGroupRequest1);

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void testHandleRequest_throwsInvalidOperationException() {
        when(proxyClient.injectCredentialsAndInvokeV2(createStatelessRuleGroupRequest1, networkFirewallClient::createRuleGroup)).thenThrow(InvalidOperationException.class);

        verify(proxyClient.client(), never()).createRuleGroup(createStatelessRuleGroupRequest1);
        verify(proxyClient.client(), never()).describeRuleGroup(describeCreateStatelessRuleGroupRequest1);

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void testHandleRequest_throwsResourceNotFoundException() {
        when(proxyClient.injectCredentialsAndInvokeV2(createStatelessRuleGroupRequest1, networkFirewallClient::createRuleGroup)).thenThrow(ResourceNotFoundException.class);

        verify(proxyClient.client(), never()).createRuleGroup(createStatelessRuleGroupRequest1);
        verify(proxyClient.client(), never()).describeRuleGroup(describeCreateStatelessRuleGroupRequest1);

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void testHandleRequest_throwsLimitExceededException() {
        when(proxyClient.injectCredentialsAndInvokeV2(createStatelessRuleGroupRequest1, networkFirewallClient::createRuleGroup)).thenThrow(LimitExceededException.class);

        verify(proxyClient.client(), never()).createRuleGroup(createStatelessRuleGroupRequest1);
        verify(proxyClient.client(), never()).describeRuleGroup(describeCreateStatelessRuleGroupRequest1);

        assertThrows(CfnServiceLimitExceededException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void testHandleRequest_throwsInsufficientCapacityException() {
        when(proxyClient.injectCredentialsAndInvokeV2(createStatelessRuleGroupRequest1, networkFirewallClient::createRuleGroup)).thenThrow(InsufficientCapacityException.class);

        verify(proxyClient.client(), never()).createRuleGroup(createStatelessRuleGroupRequest1);
        verify(proxyClient.client(), never()).describeRuleGroup(describeCreateStatelessRuleGroupRequest1);

        assertThrows(CfnServiceInternalErrorException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void testHandleRequest_throwsInternalServerErrorException() {
        when(proxyClient.injectCredentialsAndInvokeV2(createStatelessRuleGroupRequest1, networkFirewallClient::createRuleGroup)).thenThrow(InternalServerErrorException.class);

        assertThrows(CfnServiceInternalErrorException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        verify(proxyClient.client(), never()).createRuleGroup(createStatelessRuleGroupRequest1);
        verify(proxyClient.client(), never()).describeRuleGroup(describeCreateStatelessRuleGroupRequest1);
    }

    @Test
    public void testHandleRequest_throwsAlreadyExistsException() {
        InvalidRequestException exception = InvalidRequestException.builder().message("A resource with the specified name already exists").build();
        when(proxyClient.injectCredentialsAndInvokeV2(createStatelessRuleGroupRequest1, networkFirewallClient::createRuleGroup)).thenThrow(exception);

        assertThrows(CfnAlreadyExistsException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        verify(proxyClient.client(), never()).createRuleGroup(createStatelessRuleGroupRequest1);
        verify(proxyClient.client(), never()).describeRuleGroup(describeCreateStatelessRuleGroupRequest1);
    }

    private void validateStatelessResourceModel(final ResourceModel outputModel, final RuleGroup ruleGroup, final Set<Tag> tags) {
        assertThat(outputModel.getCapacity()).isEqualTo(CAPACITY);
        assertThat(outputModel.getDescription()).isEqualTo(DESCRIPTION);
        assertThat(outputModel.getRuleGroupArn()).isEqualTo(STATELESS_RULEGROUP_ARN);
        assertThat(outputModel.getRuleGroup()).isEqualTo(ruleGroup);
        assertThat(outputModel.getRuleGroupName()).isEqualTo(STATELESS_RULEGROUP_NAME);
        assertThat(outputModel.getType()).isEqualTo(STATELESS_RULEGROUP_TYPE);
        assertThat(outputModel.getDescription()).isEqualTo(DESCRIPTION);
        assertThat(outputModel.getRuleGroupId()).isEqualTo(STATELESS_RULEGROUP_ID);
        assertThat(outputModel.getTags()).isEqualTo(tags);
    }

    private void validateStatefulResourceModel(final ResourceModel outputModel, final RuleGroup ruleGroup, final Set<Tag> tags) {
        assertThat(outputModel.getCapacity()).isEqualTo(CAPACITY);
        assertThat(outputModel.getDescription()).isEqualTo(DESCRIPTION);
        assertThat(outputModel.getRuleGroupArn()).isEqualTo(STATEFUL_RULEGROUP_ARN);
        assertThat(outputModel.getRuleGroup()).isEqualTo(ruleGroup);
        assertThat(outputModel.getRuleGroupName()).isEqualTo(STATEFUL_RULEGROUP_NAME);
        assertThat(outputModel.getType()).isEqualTo(STATEFUL_RULEGROUP_TYPE);
        assertThat(outputModel.getDescription()).isEqualTo(DESCRIPTION);
        assertThat(outputModel.getRuleGroupId()).isEqualTo(STATEFUL_RULEGROUP_ID);
        assertThat(outputModel.getTags()).isEqualTo(tags);
    }
}
