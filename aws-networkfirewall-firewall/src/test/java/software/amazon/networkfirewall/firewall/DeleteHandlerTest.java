package software.amazon.networkfirewall.firewall;

import java.time.Duration;

import org.junit.jupiter.api.Assertions;
import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.awssdk.services.networkfirewall.model.DeleteFirewallRequest;
import software.amazon.awssdk.services.networkfirewall.model.DescribeFirewallRequest;
import software.amazon.awssdk.services.networkfirewall.model.InternalServerErrorException;
import software.amazon.awssdk.services.networkfirewall.model.InvalidOperationException;
import software.amazon.awssdk.services.networkfirewall.model.InvalidRequestException;
import software.amazon.awssdk.services.networkfirewall.model.ResourceNotFoundException;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends AbstractTestBase {
    private static ResourceHandlerRequest<ResourceModel> request;
    private DeleteHandler handler;

    @Mock
    private AmazonWebServicesClientProxy proxy;
    @Mock
    private ProxyClient<NetworkFirewallClient> proxyClient;
    @Mock
    NetworkFirewallClient client;

    @BeforeEach
    public void setup() {
        handler = new DeleteHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        client = mock(NetworkFirewallClient.class);
        proxyClient = MOCK_PROXY(proxy, client);

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(ResourceModel.builder().firewallArn("arn-to-delete").build())
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
        // describe returns ResourceNotFoundException stating that it got deleted instantly
        when(proxyClient.client().describeFirewall(any(DescribeFirewallRequest.class)))
                .thenThrow(ResourceNotFoundException.class);

        // call delete handler
        final ProgressEvent<ResourceModel, CallbackContext> handlerResponse =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // validate results
        assertThat(handlerResponse).isNotNull();
        assertThat(handlerResponse.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(handlerResponse.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(handlerResponse.getResourceModel()).isNull();
        assertThat(handlerResponse.getResourceModels()).isNull();
        assertThat(handlerResponse.getMessage()).isNull();
        assertThat(handlerResponse.getErrorCode()).isNull();

        verify(proxyClient.client()).describeFirewall(any(DescribeFirewallRequest.class));
        verify(proxyClient.client()).deleteFirewall(any(DeleteFirewallRequest.class));
    }

    @Test
    public void handleRequest_CoupleIterationsBeforeDeleting() {
        // setup mock requests and responses
        // describe returns ResourceNotFoundException stating that it got deleted instantly
        when(proxyClient.client().describeFirewall(any(DescribeFirewallRequest.class)))
                .thenReturn(commonDescribeResponse()) // still deleting
                .thenReturn(commonDescribeResponse()) // still deleting
                .thenThrow(ResourceNotFoundException.class);  // delete successful

        // call delete handler
        final ProgressEvent<ResourceModel, CallbackContext> handlerResponse =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        // validate results
        assertThat(handlerResponse).isNotNull();
        assertThat(handlerResponse.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(handlerResponse.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(handlerResponse.getResourceModel()).isNull();
        assertThat(handlerResponse.getResourceModels()).isNull();
        assertThat(handlerResponse.getMessage()).isNull();
        assertThat(handlerResponse.getErrorCode()).isNull();

        verify(proxyClient.client(), times(3)).describeFirewall(any(DescribeFirewallRequest.class));
        verify(proxyClient.client(), times(3)).deleteFirewall(any(DeleteFirewallRequest.class));
    }

    @Test
    public void handleRequest_ResourceNotFoundWhileDeleting() {
        // setup mock requests and responses
        when(proxyClient.client().deleteFirewall(any(DeleteFirewallRequest.class)))
                .thenThrow(ResourceNotFoundException.class);

        // call delete handler
        Assertions.assertThrows(CfnNotFoundException.class,
                () -> {handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);});

        verify(proxyClient.client()).deleteFirewall(any(DeleteFirewallRequest.class));
    }

    @Test
    public void deleteInvalidRequest() {
        when(proxyClient.client().deleteFirewall(any(DeleteFirewallRequest.class)))
                .thenThrow(InvalidRequestException.class);
        Assertions.assertThrows(CfnInvalidRequestException.class,
                () -> {handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);});
    }

    @Test
    public void deleteInvalidOperation() {
        when(proxyClient.client().deleteFirewall(any(DeleteFirewallRequest.class)))
                .thenThrow(InvalidOperationException.class);
        Assertions.assertThrows(CfnInvalidRequestException.class,
                () -> {handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);});
    }

    @Test
    public void deleteInternalServiceError() {
        when(proxyClient.client().deleteFirewall(any(DeleteFirewallRequest.class)))
                .thenThrow(InternalServerErrorException.class);
        Assertions.assertThrows(CfnServiceInternalErrorException.class,
                () -> {handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);});
    }
}
