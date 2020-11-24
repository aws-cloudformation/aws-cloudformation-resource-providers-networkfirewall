package software.amazon.networkfirewall.rulegroup;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.awssdk.services.networkfirewall.model.DescribeRuleGroupRequest;
import software.amazon.awssdk.services.networkfirewall.model.DescribeRuleGroupResponse;
import software.amazon.awssdk.services.networkfirewall.model.InsufficientCapacityException;
import software.amazon.awssdk.services.networkfirewall.model.InternalServerErrorException;
import software.amazon.awssdk.services.networkfirewall.model.InvalidOperationException;
import software.amazon.awssdk.services.networkfirewall.model.InvalidRequestException;
import software.amazon.awssdk.services.networkfirewall.model.InvalidTokenException;
import software.amazon.awssdk.services.networkfirewall.model.LimitExceededException;
import software.amazon.awssdk.services.networkfirewall.model.ResourceNotFoundException;
import software.amazon.awssdk.services.networkfirewall.model.TagResourceRequest;
import software.amazon.awssdk.services.networkfirewall.model.TagResourceResponse;
import software.amazon.awssdk.services.networkfirewall.model.ThrottlingException;
import software.amazon.awssdk.services.networkfirewall.model.UntagResourceRequest;
import software.amazon.awssdk.services.networkfirewall.model.UntagResourceResponse;
import software.amazon.awssdk.services.networkfirewall.model.UpdateRuleGroupRequest;
import software.amazon.awssdk.services.networkfirewall.model.UpdateRuleGroupResponse;
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
public class UpdateHandlerTest extends AbstractTestBase {
    private UpdateHandler handler;
    ResourceModel previousStatelessModelWithNoTags, previousStatelessModelWithTags, previousStatefulModelWithNoTags, previousStatefulModelWithTags, desiredStatelessModel, desiredStatefulModel;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<NetworkFirewallClient> proxyClient;

    @Mock
    NetworkFirewallClient networkFirewallClient;

    @BeforeEach
    public void setup() {
        handler = new UpdateHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        networkFirewallClient = mock(NetworkFirewallClient.class);
        proxyClient = MOCK_PROXY(proxy, networkFirewallClient);

        setupRuleGroupTest();

        // previous state of the resource
        previousStatelessModelWithNoTags = ResourceModel
                .builder()
                .ruleGroupName(STATELESS_RULEGROUP_NAME)
                .ruleGroup(cfnStatelessRuleGroup1)
                .ruleGroupArn(STATELESS_RULEGROUP_ARN)
                .description(DESCRIPTION)
                .type(STATELESS_RULEGROUP_TYPE)
                .build();
        previousStatelessModelWithTags = ResourceModel
                .builder()
                .ruleGroupName(STATELESS_RULEGROUP_NAME)
                .ruleGroupArn(STATELESS_RULEGROUP_ARN)
                .ruleGroup(cfnStatelessRuleGroup1)
                .description(DESCRIPTION)
                .type(STATELESS_RULEGROUP_TYPE)
                .tags(statelessTags)
                .build();
        previousStatefulModelWithNoTags = ResourceModel
                .builder()
                .ruleGroupName(STATEFUL_RULEGROUP_NAME)
                .ruleGroupArn(STATEFUL_RULEGROUP_ARN)
                .ruleGroup(cfnStatefulRuleGroup1)
                .description(DESCRIPTION)
                .type(STATEFUL_RULEGROUP_TYPE)
                .build();
        previousStatefulModelWithTags = ResourceModel
                .builder()
                .ruleGroupName(STATEFUL_RULEGROUP_NAME)
                .ruleGroup(cfnStatefulRuleGroup1)
                .ruleGroupArn(STATEFUL_RULEGROUP_ARN)
                .description(DESCRIPTION)
                .type(STATEFUL_RULEGROUP_TYPE)
                .tags(statefulTags)
                .build();

        // desired state of the resource
        desiredStatelessModel = ResourceModel
                .builder()
                .ruleGroup(cfnStatelessRuleGroup1)
                .ruleGroupName(STATELESS_RULEGROUP_NAME)
                .ruleGroupArn(STATELESS_RULEGROUP_ARN)
                .type(STATELESS_RULEGROUP_TYPE)
                .description(DESCRIPTION)
                .build();
        desiredStatefulModel = ResourceModel
                .builder()
                .ruleGroup(cfnStatefulRuleGroup1)
                .ruleGroupName(STATEFUL_RULEGROUP_NAME)
                .ruleGroupArn(STATEFUL_RULEGROUP_ARN)
                .type(STATEFUL_RULEGROUP_TYPE)
                .description(DESCRIPTION)
                .build();
    }

    @AfterEach
    public void tear_down() {
        verify(networkFirewallClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(networkFirewallClient);
    }

    @Test
    public void testHandleRequest_updateStatelessRuleGroupSuccess1() {
        // desired state has tags
        ResourceModel desiredModel = ResourceModel
                .builder()
                .ruleGroup(cfnStatelessRuleGroup2)
                .ruleGroupName(STATELESS_RULEGROUP_NAME)
                .type(STATELESS_RULEGROUP_TYPE)
                .ruleGroupArn(STATELESS_RULEGROUP_ARN)
                .description(DESCRIPTION)
                .tags(statelessTags)
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(previousStatelessModelWithNoTags)
                .desiredResourceState(desiredModel)
                .build();

        // previous state had no tags
        DescribeRuleGroupResponse previousState = DescribeRuleGroupResponse.builder()
                .ruleGroupResponse(statelessSdkRuleGroupResponseWithNoTags)
                .ruleGroup(statelessSdkRuleGroup1)
                .updateToken(UPDATE_TOKEN)
                .build();
        when(proxyClient.injectCredentialsAndInvokeV2(describeUpdateStatelessRuleGroupRequest2, networkFirewallClient::describeRuleGroup)).thenReturn(previousState).thenReturn(previousState).thenReturn(describeUpdateStatelessRuleGroupResponse2);
        when(proxyClient.injectCredentialsAndInvokeV2(updateStatelessRuleGroupRequest2, networkFirewallClient::updateRuleGroup)).thenReturn(updateStatelessRuleGroupResponse2);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        verify(proxyClient.client(), times(1)).updateRuleGroup(any(UpdateRuleGroupRequest.class));
        verify(proxyClient.client(), times(4)).describeRuleGroup(any(DescribeRuleGroupRequest.class));
        verify(proxyClient.client(), times(1)).tagResource(any(TagResourceRequest.class));

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel().getRuleGroupName()).isEqualTo(STATELESS_RULEGROUP_NAME);
        assertThat(response.getResourceModel().getType()).isEqualTo(STATELESS_RULEGROUP_TYPE);
        assertThat(response.getResourceModel().getRuleGroupArn()).isEqualTo(request.getDesiredResourceState().getRuleGroupArn());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        // Validate ResourceModel
        validateStatelessResourceModel(response.getResourceModel(), cfnStatelessRuleGroup2, statelessTags);
    }

    @Test
    public void testHandleRequest_updateStatelessRuleGroupSuccess2() {
        // desired state has tags
        ResourceModel desiredModel = ResourceModel
                .builder()
                .ruleGroup(cfnStatelessRuleGroup3)
                .ruleGroupName(STATELESS_RULEGROUP_NAME)
                .ruleGroupArn(STATELESS_RULEGROUP_ARN)
                .type(STATELESS_RULEGROUP_TYPE)
                .tags(statelessTags)
                .description(DESCRIPTION)
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(previousStatelessModelWithNoTags)
                .desiredResourceState(desiredModel)
                .build();

        // previous state has no tags
        DescribeRuleGroupResponse previousState = DescribeRuleGroupResponse.builder()
                .ruleGroupResponse(statelessSdkRuleGroupResponseWithNoTags)
                .ruleGroup(statelessSdkRuleGroup1)
                .updateToken(UPDATE_TOKEN)
                .build();
        when(proxyClient.injectCredentialsAndInvokeV2(describeUpdateStatelessRuleGroupRequest3, networkFirewallClient::describeRuleGroup)).thenReturn(previousState).thenReturn(previousState).thenReturn(describeUpdateStatelessRuleGroupResponse3);
        when(proxyClient.injectCredentialsAndInvokeV2(updateStatelessRuleGroupRequest3, networkFirewallClient::updateRuleGroup)).thenReturn(updateStatelessRuleGroupResponse3);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        verify(proxyClient.client(), times(1)).updateRuleGroup(any(UpdateRuleGroupRequest.class));
        verify(proxyClient.client(), times(4)).describeRuleGroup(any(DescribeRuleGroupRequest.class));
        verify(proxyClient.client(), times(1)).tagResource(any(TagResourceRequest.class));

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel().getRuleGroupName()).isEqualTo(STATELESS_RULEGROUP_NAME);
        assertThat(response.getResourceModel().getType()).isEqualTo(STATELESS_RULEGROUP_TYPE);
        assertThat(response.getResourceModel().getRuleGroupArn()).isEqualTo(STATELESS_RULEGROUP_ARN);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        // Validate ResourceModel
        validateStatelessResourceModel(response.getResourceModel(), cfnStatelessRuleGroup3, statelessTags);
    }

    @Test
    public void testHandleRequest_updateStatelessRuleGroupSuccess3() {
        // desired state has no tags
        ResourceModel desiredModel = ResourceModel
                .builder()
                .ruleGroup(cfnStatelessRuleGroup1)
                .ruleGroupName(STATELESS_RULEGROUP_NAME)
                .ruleGroupArn(STATELESS_RULEGROUP_ARN)
                .type(STATELESS_RULEGROUP_TYPE)
                .description(DESCRIPTION)
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(previousStatelessModelWithTags)
                .desiredResourceState(desiredModel)
                .build();

        // previous state has tags
        DescribeRuleGroupResponse previousState = DescribeRuleGroupResponse.builder()
                .ruleGroupResponse(statelessSdkRuleGroupResponseWithTags)
                .ruleGroup(statelessSdkRuleGroup3)
                .updateToken(UPDATE_TOKEN)
                .build();
        UntagResourceRequest untagStatelessRuleGroupRequest = UntagResourceRequest.builder()
                .tagKeys("rule-group")
                .resourceArn(STATELESS_RULEGROUP_ARN)
                .build();
        when(proxyClient.injectCredentialsAndInvokeV2(describeUpdateStatelessRuleGroupRequest1, networkFirewallClient::describeRuleGroup)).thenReturn(previousState).thenReturn(previousState).thenReturn(describeUpdateStatelessRuleGroupResponse1);
        when(proxyClient.injectCredentialsAndInvokeV2(updateStatelessRuleGroupRequest1, networkFirewallClient::updateRuleGroup)).thenReturn(updateStatelessRuleGroupResponse1);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        verify(proxyClient.client(), times(1)).updateRuleGroup(any(UpdateRuleGroupRequest.class));
        verify(proxyClient.client(), times(4)).describeRuleGroup(any(DescribeRuleGroupRequest.class));
        verify(proxyClient.client(), times(1)).untagResource(any(UntagResourceRequest.class));

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel().getRuleGroupName()).isEqualTo(STATELESS_RULEGROUP_NAME);
        assertThat(response.getResourceModel().getType()).isEqualTo(STATELESS_RULEGROUP_TYPE);
        assertThat(response.getResourceModel().getRuleGroupArn()).isEqualTo(request.getDesiredResourceState().getRuleGroupArn());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        // Validate ResourceModel
        validateStatelessResourceModel(response.getResourceModel(), cfnStatelessRuleGroup1, new HashSet<Tag>());
    }

    @Test
    public void testHandleRequest_updateStatefulRuleGroupSuccess1() {
        // desired state has tags
        ResourceModel desiredModel = ResourceModel
                .builder()
                .ruleGroup(cfnStatefulRuleGroup2)
                .ruleGroupName(STATEFUL_RULEGROUP_NAME)
                .type(STATEFUL_RULEGROUP_TYPE)
                .ruleGroupArn(STATEFUL_RULEGROUP_ARN)
                .description(DESCRIPTION)
                .tags(statefulTags)
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(previousStatefulModelWithNoTags)
                .desiredResourceState(desiredModel)
                .build();

        // previous state has no tags
        DescribeRuleGroupResponse previousState = DescribeRuleGroupResponse.builder()
                .ruleGroupResponse(statefulSdkRuleGroupResponseWithNoTags)
                .ruleGroup(statefulSdkRuleGroup1)
                .updateToken(UPDATE_TOKEN)
                .build();
        when(proxyClient.injectCredentialsAndInvokeV2(describeUpdateStatefulRuleGroupRequest2, networkFirewallClient::describeRuleGroup)).thenReturn(previousState).thenReturn(previousState).thenReturn(describeUpdateStatefulRuleGroupResponse2);
        when(proxyClient.injectCredentialsAndInvokeV2(updateStatefulRuleGroupRequest2, networkFirewallClient::updateRuleGroup)).thenReturn(updateStatefulRuleGroupResponse2);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        verify(proxyClient.client(), times(1)).updateRuleGroup(any(UpdateRuleGroupRequest.class));
        verify(proxyClient.client(), times(4)).describeRuleGroup(any(DescribeRuleGroupRequest.class));
        verify(proxyClient.client(), times(1)).tagResource(any(TagResourceRequest.class));

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel().getRuleGroupName()).isEqualTo(STATEFUL_RULEGROUP_NAME);
        assertThat(response.getResourceModel().getType()).isEqualTo(STATEFUL_RULEGROUP_TYPE);
        assertThat(response.getResourceModel().getRuleGroupArn()).isEqualTo(request.getDesiredResourceState().getRuleGroupArn());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        // Validate ResourceModel
        validateStatefulResourceModel(response.getResourceModel(), cfnStatefulRuleGroup2, statefulTags);
    }

    @Test
    public void testHandleRequest_updateStatefulRuleGroupSuccess2() {
        // desired state has tags
        ResourceModel desiredModel = ResourceModel
                .builder()
                .ruleGroup(cfnStatefulRuleGroup3)
                .ruleGroupName(STATEFUL_RULEGROUP_NAME)
                .ruleGroupArn(STATEFUL_RULEGROUP_ARN)
                .type(STATEFUL_RULEGROUP_TYPE)
                .description(DESCRIPTION)
                .tags(statefulTags)
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(previousStatefulModelWithNoTags)
                .desiredResourceState(desiredModel)
                .build();

        // previous state with no tags
        DescribeRuleGroupResponse previousState = DescribeRuleGroupResponse.builder()
                .ruleGroupResponse(statefulSdkRuleGroupResponseWithNoTags)
                .ruleGroup(statefulSdkRuleGroup1)
                .updateToken(UPDATE_TOKEN)
                .build();
        when(proxyClient.injectCredentialsAndInvokeV2(describeUpdateStatefulRuleGroupRequest3, networkFirewallClient::describeRuleGroup)).thenReturn(previousState).thenReturn(previousState).thenReturn(describeUpdateStatefulRuleGroupResponse3);
        when(proxyClient.injectCredentialsAndInvokeV2(updateStatefulRuleGroupRequest3, networkFirewallClient::updateRuleGroup)).thenReturn(updateStatefulRuleGroupResponse3);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        verify(proxyClient.client(), times(1)).updateRuleGroup(any(UpdateRuleGroupRequest.class));
        verify(proxyClient.client(), times(4)).describeRuleGroup(any(DescribeRuleGroupRequest.class));
        verify(proxyClient.client(), times(1)).tagResource(any(TagResourceRequest.class));

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel().getRuleGroupName()).isEqualTo(STATEFUL_RULEGROUP_NAME);
        assertThat(response.getResourceModel().getType()).isEqualTo(STATEFUL_RULEGROUP_TYPE);
        assertThat(response.getResourceModel().getRuleGroupArn()).isEqualTo(STATEFUL_RULEGROUP_ARN);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        // Validate ResourceModel
        validateStatefulResourceModel(response.getResourceModel(), cfnStatefulRuleGroup3, statefulTags);
    }

    @Test
    public void testHandleRequest_updateStatefulRuleGroupSuccess3() {
        // previous state with tags
        previousStatefulModelWithTags = ResourceModel
                .builder()
                .ruleGroupArn(STATEFUL_RULEGROUP_ARN)
                .ruleGroup(cfnStatefulRuleGroup3)
                .description(DESCRIPTION)
                .tags(statefulTags)
                .build();

        // desired state with no tags
        ResourceModel desiredModel = ResourceModel
                .builder()
                .ruleGroup(cfnStatefulRuleGroup1)
                .ruleGroupName(STATEFUL_RULEGROUP_NAME)
                .ruleGroupArn(STATEFUL_RULEGROUP_ARN)
                .type(STATEFUL_RULEGROUP_TYPE)
                .description(DESCRIPTION)
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(previousStatefulModelWithTags)
                .desiredResourceState(desiredModel)
                .build();

        DescribeRuleGroupResponse previousState = DescribeRuleGroupResponse.builder()
                .ruleGroupResponse(statefulSdkRuleGroupResponseWithTags)
                .ruleGroup(statefulSdkRuleGroup3)
                .updateToken(UPDATE_TOKEN)
                .build();

        when(proxyClient.injectCredentialsAndInvokeV2(describeUpdateStatefulRuleGroupRequest1, networkFirewallClient::describeRuleGroup)).thenReturn(previousState).thenReturn(previousState).thenReturn(describeUpdateStatefulRuleGroupResponse1);
        when(proxyClient.injectCredentialsAndInvokeV2(updateStatefulRuleGroupRequest1, networkFirewallClient::updateRuleGroup)).thenReturn(updateStatefulRuleGroupResponse1);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        verify(proxyClient.client(), times(1)).updateRuleGroup(any(UpdateRuleGroupRequest.class));
        verify(proxyClient.client(), times(4)).describeRuleGroup(any(DescribeRuleGroupRequest.class));
        verify(proxyClient.client(), times(1)).untagResource(any(UntagResourceRequest.class));

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel().getRuleGroupName()).isEqualTo(STATEFUL_RULEGROUP_NAME);
        assertThat(response.getResourceModel().getType()).isEqualTo(STATEFUL_RULEGROUP_TYPE);
        assertThat(response.getResourceModel().getRuleGroupArn()).isEqualTo(request.getDesiredResourceState().getRuleGroupArn());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        // Validate ResourceModel
        validateStatefulResourceModel(response.getResourceModel(), cfnStatefulRuleGroup1, new HashSet<Tag>());
    }

    @Test
    public void testHandleRequest_updateStatefulRuleGroupSuccess_withDifferentTags() {
        Set<Tag> newStatefulTags = Collections.singleton(Tag.builder()
                .key("test")
                .value("change")
                .build());
        // desired state has tags
        ResourceModel desiredModel = ResourceModel
                .builder()
                .ruleGroup(cfnStatefulRuleGroup3)
                .ruleGroupName(STATEFUL_RULEGROUP_NAME)
                .ruleGroupArn(STATEFUL_RULEGROUP_ARN)
                .type(STATEFUL_RULEGROUP_TYPE)
                .description(DESCRIPTION)
                .tags(newStatefulTags)
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(previousStatefulModelWithTags)
                .desiredResourceState(desiredModel)
                .build();

        // previous state with different tags (rule-group=stateful)
        DescribeRuleGroupResponse previousState = DescribeRuleGroupResponse.builder()
                .ruleGroupResponse(statefulSdkRuleGroupResponseWithTags)
                .ruleGroup(statefulSdkRuleGroup2)
                .updateToken(UPDATE_TOKEN)
                .build();

        Set<software.amazon.awssdk.services.networkfirewall.model.Tag> newSdkStatefulTags = newStatefulTags
                .stream()
                .map(tag -> software.amazon.awssdk.services.networkfirewall.model.Tag.builder()
                        .key(tag.getKey())
                        .value(tag.getValue())
                        .build())
                .collect(Collectors.toSet());
        software.amazon.awssdk.services.networkfirewall.model.RuleGroupResponse statefulSdkRuleGroupResponseWithNewTags = software.amazon.awssdk.services.networkfirewall.model.RuleGroupResponse.builder()
                .capacity(CAPACITY)
                .description(DESCRIPTION)
                .ruleGroupArn(STATEFUL_RULEGROUP_ARN)
                .ruleGroupId(STATEFUL_RULEGROUP_ID)
                .ruleGroupName(STATEFUL_RULEGROUP_NAME)
                .ruleGroupStatus(ACTIVE)
                .type(STATEFUL_RULEGROUP_TYPE)
                .tags(newSdkStatefulTags)
                .build();
        DescribeRuleGroupResponse newState = DescribeRuleGroupResponse.builder()
                .ruleGroupResponse(statefulSdkRuleGroupResponseWithNewTags)
                .ruleGroup(statefulSdkRuleGroup3)
                .updateToken(NEXT_UPDATE_TOKEN)
                .build();
        UpdateRuleGroupResponse updateResponse = UpdateRuleGroupResponse.builder()
                .ruleGroupResponse(statefulSdkRuleGroupResponseWithNewTags)
                .updateToken(NEXT_UPDATE_TOKEN)
                .build();

        TagResourceRequest tagRequest = TagResourceRequest.builder()
                .resourceArn(STATEFUL_RULEGROUP_ARN)
                .tags(newSdkStatefulTags).build();

        when(proxyClient.injectCredentialsAndInvokeV2(describeUpdateStatefulRuleGroupRequest3, networkFirewallClient::describeRuleGroup)).thenReturn(previousState).thenReturn(previousState).thenReturn(newState);
        when(proxyClient.injectCredentialsAndInvokeV2(updateStatefulRuleGroupRequest3, networkFirewallClient::updateRuleGroup)).thenReturn(updateResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        verify(proxyClient.client(), times(1)).updateRuleGroup(any(UpdateRuleGroupRequest.class));
        verify(proxyClient.client(), times(4)).describeRuleGroup(any(DescribeRuleGroupRequest.class));
        verify(proxyClient.client(), times(1)).untagResource(any(UntagResourceRequest.class));
        verify(proxyClient.client(), times(1)).tagResource(any(TagResourceRequest.class));

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel().getRuleGroupName()).isEqualTo(STATEFUL_RULEGROUP_NAME);
        assertThat(response.getResourceModel().getType()).isEqualTo(STATEFUL_RULEGROUP_TYPE);
        assertThat(response.getResourceModel().getRuleGroupArn()).isEqualTo(STATEFUL_RULEGROUP_ARN);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        // Validate ResourceModel
        validateStatefulResourceModel(response.getResourceModel(), cfnStatefulRuleGroup3, newStatefulTags);
    }

    @Test
    public void testHandleRequest_throwsInvalidRequestException() {
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(previousStatelessModelWithNoTags)
                .desiredResourceState(desiredStatelessModel)
                .build();
        DescribeRuleGroupResponse previousState = DescribeRuleGroupResponse.builder()
                .ruleGroupResponse(statelessSdkRuleGroupResponseWithNoTags)
                .ruleGroup(statelessSdkRuleGroup1)
                .updateToken(UPDATE_TOKEN)
                .build();

        when(proxyClient.injectCredentialsAndInvokeV2(describeUpdateStatelessRuleGroupRequest1, networkFirewallClient::describeRuleGroup)).thenReturn(previousState);
        when(proxyClient.injectCredentialsAndInvokeV2(updateStatelessRuleGroupRequest1, networkFirewallClient::updateRuleGroup)).thenThrow(InvalidRequestException.class);

        verify(proxyClient.client(), never()).updateRuleGroup(updateStatelessRuleGroupRequest1);
        verify(proxyClient.client(), never()).describeRuleGroup(describeUpdateStatelessRuleGroupRequest1);

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void testHandleRequest_throwsThrottlingException() {
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(previousStatefulModelWithNoTags)
                .desiredResourceState(desiredStatefulModel)
                .build();
        DescribeRuleGroupResponse previousState = DescribeRuleGroupResponse.builder()
                .ruleGroupResponse(statefulSdkRuleGroupResponseWithNoTags)
                .ruleGroup(statefulSdkRuleGroup1)
                .updateToken(UPDATE_TOKEN)
                .build();
        when(proxyClient.injectCredentialsAndInvokeV2(describeUpdateStatefulRuleGroupRequest1, networkFirewallClient::describeRuleGroup)).thenReturn(previousState).thenReturn(previousState);
        when(proxyClient.injectCredentialsAndInvokeV2(updateStatefulRuleGroupRequest1, networkFirewallClient::updateRuleGroup)).thenThrow(ThrottlingException.class);

        verify(proxyClient.client(), never()).updateRuleGroup(updateStatefulRuleGroupRequest1);
        verify(proxyClient.client(), never()).describeRuleGroup(describeUpdateStatelessRuleGroupRequest1);

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void testHandleRequest_throwsInvalidOperationException() {
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(previousStatelessModelWithNoTags)
                .desiredResourceState(desiredStatelessModel)
                .build();
        DescribeRuleGroupResponse previousState = DescribeRuleGroupResponse.builder()
                .ruleGroupResponse(statelessSdkRuleGroupResponseWithNoTags)
                .ruleGroup(statelessSdkRuleGroup1)
                .updateToken(UPDATE_TOKEN)
                .build();

        when(proxyClient.injectCredentialsAndInvokeV2(describeUpdateStatelessRuleGroupRequest1, networkFirewallClient::describeRuleGroup)).thenReturn(previousState);
        when(proxyClient.injectCredentialsAndInvokeV2(updateStatelessRuleGroupRequest1, networkFirewallClient::updateRuleGroup)).thenThrow(InvalidOperationException.class);

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        verify(proxyClient.client(), never()).updateRuleGroup(updateStatelessRuleGroupRequest1);
        verify(proxyClient.client(), never()).describeRuleGroup(describeUpdateStatelessRuleGroupRequest1);
    }

    @Test
    public void testHandleRequest_throwsResourceNotFoundException() {
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(previousStatelessModelWithNoTags)
                .desiredResourceState(desiredStatelessModel)
                .build();
        DescribeRuleGroupResponse previousState = DescribeRuleGroupResponse.builder()
                .ruleGroupResponse(statelessSdkRuleGroupResponseWithNoTags)
                .ruleGroup(statelessSdkRuleGroup1)
                .updateToken(UPDATE_TOKEN)
                .build();

        when(proxyClient.injectCredentialsAndInvokeV2(describeUpdateStatelessRuleGroupRequest1, networkFirewallClient::describeRuleGroup)).thenReturn(previousState);
        when(proxyClient.injectCredentialsAndInvokeV2(updateStatelessRuleGroupRequest1, networkFirewallClient::updateRuleGroup)).thenThrow(ResourceNotFoundException.class);

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void testHandleRequest_throwsLimitExceededException() {
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(previousStatelessModelWithNoTags)
                .desiredResourceState(desiredStatelessModel)
                .build();
        DescribeRuleGroupResponse previousState = DescribeRuleGroupResponse.builder()
                .ruleGroupResponse(statelessSdkRuleGroupResponseWithNoTags)
                .ruleGroup(statelessSdkRuleGroup1)
                .updateToken(UPDATE_TOKEN)
                .build();

        when(proxyClient.injectCredentialsAndInvokeV2(describeUpdateStatelessRuleGroupRequest1, networkFirewallClient::describeRuleGroup)).thenReturn(previousState);
        when(proxyClient.injectCredentialsAndInvokeV2(updateStatelessRuleGroupRequest1, networkFirewallClient::updateRuleGroup)).thenThrow(LimitExceededException.class);

        verify(proxyClient.client(), never()).updateRuleGroup(updateStatelessRuleGroupRequest1);
        verify(proxyClient.client(), never()).describeRuleGroup(describeUpdateStatelessRuleGroupRequest1);

        assertThrows(CfnServiceLimitExceededException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void testHandleRequest_throwsInsufficientCapacityException() {
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(previousStatelessModelWithNoTags)
                .desiredResourceState(desiredStatelessModel)
                .build();
        DescribeRuleGroupResponse previousState = DescribeRuleGroupResponse.builder()
                .ruleGroupResponse(statelessSdkRuleGroupResponseWithNoTags)
                .ruleGroup(statelessSdkRuleGroup1)
                .updateToken(UPDATE_TOKEN)
                .build();

        when(proxyClient.injectCredentialsAndInvokeV2(describeUpdateStatelessRuleGroupRequest1, networkFirewallClient::describeRuleGroup)).thenReturn(previousState).thenReturn(previousState);
        when(proxyClient.injectCredentialsAndInvokeV2(updateStatelessRuleGroupRequest1, networkFirewallClient::updateRuleGroup)).thenThrow(InsufficientCapacityException.class);

        assertThrows(CfnServiceInternalErrorException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        verify(proxyClient.client(), never()).updateRuleGroup(updateStatelessRuleGroupRequest1);
        verify(proxyClient.client(), never()).describeRuleGroup(describeUpdateStatelessRuleGroupRequest1);
    }

    @Test
    public void testHandleRequest_throwsInvalidTokenException() {
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(previousStatelessModelWithNoTags)
                .desiredResourceState(desiredStatelessModel)
                .build();
        DescribeRuleGroupResponse previousState = DescribeRuleGroupResponse.builder()
                .ruleGroupResponse(statelessSdkRuleGroupResponseWithNoTags)
                .ruleGroup(statelessSdkRuleGroup1)
                .updateToken(UPDATE_TOKEN)
                .build();

        when(proxyClient.injectCredentialsAndInvokeV2(describeUpdateStatelessRuleGroupRequest1, networkFirewallClient::describeRuleGroup)).thenReturn(previousState).thenReturn(previousState);
        when(proxyClient.injectCredentialsAndInvokeV2(updateStatelessRuleGroupRequest1, networkFirewallClient::updateRuleGroup)).thenThrow(InvalidTokenException.class);

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void testHandleRequest_throwsInternalServerErrorException() {
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(previousStatefulModelWithNoTags)
                .desiredResourceState(desiredStatefulModel)
                .build();
        DescribeRuleGroupResponse previousState = DescribeRuleGroupResponse.builder()
                .ruleGroupResponse(statefulSdkRuleGroupResponseWithNoTags)
                .ruleGroup(statefulSdkRuleGroup1)
                .updateToken(UPDATE_TOKEN)
                .build();

        when(proxyClient.injectCredentialsAndInvokeV2(describeUpdateStatefulRuleGroupRequest1, networkFirewallClient::describeRuleGroup)).thenReturn(previousState).thenReturn(previousState);
        when(proxyClient.injectCredentialsAndInvokeV2(updateStatefulRuleGroupRequest1, networkFirewallClient::updateRuleGroup)).thenThrow(InternalServerErrorException.class);

        assertThrows(CfnServiceInternalErrorException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        verify(proxyClient.client(), never()).updateRuleGroup(updateStatefulRuleGroupRequest1);
        verify(proxyClient.client(), never()).describeRuleGroup(describeUpdateStatelessRuleGroupRequest1);
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
