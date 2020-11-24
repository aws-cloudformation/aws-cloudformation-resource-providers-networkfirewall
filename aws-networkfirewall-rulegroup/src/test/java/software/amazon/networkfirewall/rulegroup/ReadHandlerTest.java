package software.amazon.networkfirewall.rulegroup;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.awssdk.services.networkfirewall.model.DescribeRuleGroupResponse;
import software.amazon.awssdk.services.networkfirewall.model.ResourceNotFoundException;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends AbstractTestBase {
    private ReadHandler handler;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<NetworkFirewallClient> proxyClient;

    @Mock
    NetworkFirewallClient networkFirewallClient;

    @BeforeEach
    public void setup() {
        handler = new ReadHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        networkFirewallClient = mock(NetworkFirewallClient.class);
        proxyClient = MOCK_PROXY(proxy, networkFirewallClient);
        setupRuleGroupTest();
    }

    @AfterEach
    public void tear_down() {
        verify(networkFirewallClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(networkFirewallClient);
    }

    @Test
    public void testHandleRequest_describeStatelessRuleGroupSuccess1() {
        final ResourceModel model = ResourceModel
                .builder()
                .ruleGroupArn(STATELESS_RULEGROUP_ARN)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        when(proxyClient.injectCredentialsAndInvokeV2(describeStatelessRuleGroupRequestWithArn, networkFirewallClient::describeRuleGroup)).thenReturn(describeUpdateStatelessRuleGroupResponse2);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        // Validate ResourceModel
        validateStatelessResourceModel(response.getResourceModel(), cfnStatelessRuleGroup2, statelessTags);
    }

    @Test
    public void testHandleRequest_describeStatelessRuleGroupSuccess2() {
        final ResourceModel model = ResourceModel
                .builder()
                .ruleGroupName(STATELESS_RULEGROUP_NAME)
                .ruleGroupArn(STATELESS_RULEGROUP_ARN)
                .type(STATELESS_RULEGROUP_TYPE)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.injectCredentialsAndInvokeV2(describeUpdateStatelessRuleGroupRequest3, networkFirewallClient::describeRuleGroup)).thenReturn(describeUpdateStatelessRuleGroupResponse3);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        // Validate ResourceModel
        validateStatelessResourceModel(response.getResourceModel(), cfnStatelessRuleGroup3, statelessTags);
    }

    @Test
    public void testHandleRequest_describeStatelessRuleGroupSuccess3() {
        final ResourceModel model = ResourceModel
                .builder()
                .ruleGroupName(STATELESS_RULEGROUP_NAME)
                .type(STATELESS_RULEGROUP_TYPE)
                .ruleGroupArn(STATELESS_RULEGROUP_ARN)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.injectCredentialsAndInvokeV2(describeUpdateStatelessRuleGroupRequest1, networkFirewallClient::describeRuleGroup)).thenReturn(describeUpdateStatelessRuleGroupResponse1);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        // Validate ResourceModel
        validateStatelessResourceModel(response.getResourceModel(), cfnStatelessRuleGroup1, new HashSet<Tag>());
    }

    @Test
    public void testHandleRequest_describeStatefulRuleGroupSuccess1() {
        final ResourceModel model = ResourceModel
                .builder()
                .ruleGroupArn(STATEFUL_RULEGROUP_ARN)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.injectCredentialsAndInvokeV2(describeStatefulRuleGroupRequestWithArn, networkFirewallClient::describeRuleGroup)).thenReturn(describeUpdateStatefulRuleGroupResponse2);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        // Validate ResourceModel
        validateStatefulResourceModel(response.getResourceModel(), cfnStatefulRuleGroup2, statefulTags);
    }

    @Test
    public void testHandleRequest_describeStatefulRuleGroupSuccess2() {
        final ResourceModel model = ResourceModel
                .builder()
                .ruleGroupArn(STATEFUL_RULEGROUP_ARN)
                .ruleGroupName(STATEFUL_RULEGROUP_NAME)
                .type(STATEFUL_RULEGROUP_TYPE)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.injectCredentialsAndInvokeV2(describeUpdateStatefulRuleGroupRequest3, networkFirewallClient::describeRuleGroup)).thenReturn(describeUpdateStatefulRuleGroupResponse3);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        // Validate ResourceModel
        validateStatefulResourceModel(response.getResourceModel(), cfnStatefulRuleGroup3, statefulTags);
    }

    @Test
    public void testHandleRequest_describeStatefulRuleGroupSuccess3() {
        final ResourceModel model = ResourceModel
                .builder()
                .ruleGroupName(STATEFUL_RULEGROUP_NAME)
                .type(STATEFUL_RULEGROUP_TYPE)
                .ruleGroupArn(STATEFUL_RULEGROUP_ARN)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.injectCredentialsAndInvokeV2(describeUpdateStatefulRuleGroupRequest1, networkFirewallClient::describeRuleGroup)).thenReturn(describeUpdateStatefulRuleGroupResponse1);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        // Validate ResourceModel
        validateStatefulResourceModel(response.getResourceModel(), cfnStatefulRuleGroup1, new HashSet<Tag>());
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

        when(proxyClient.injectCredentialsAndInvokeV2(describeUpdateStatefulRuleGroupRequest1, networkFirewallClient::describeRuleGroup)).thenThrow(ResourceNotFoundException.class);

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
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
