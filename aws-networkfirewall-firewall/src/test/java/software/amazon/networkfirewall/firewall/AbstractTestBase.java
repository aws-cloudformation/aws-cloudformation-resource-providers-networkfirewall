package software.amazon.networkfirewall.firewall;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.awssdk.services.networkfirewall.model.ConfigurationSyncState;
import software.amazon.awssdk.services.networkfirewall.model.DescribeFirewallResponse;
import software.amazon.awssdk.services.networkfirewall.model.Firewall;
import software.amazon.awssdk.services.networkfirewall.model.FirewallStatusValue;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Credentials;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.ProxyClient;

public class AbstractTestBase {
    protected static final Credentials MOCK_CREDENTIALS;
    protected static final LoggerProxy logger;

    static {
        MOCK_CREDENTIALS = new Credentials("accessKey", "secretKey", "token");
        logger = new LoggerProxy();
    }
    static ProxyClient<NetworkFirewallClient> MOCK_PROXY(
            final AmazonWebServicesClientProxy proxy,
            final NetworkFirewallClient NetworkFirewallClient) {
        return new ProxyClient<NetworkFirewallClient>() {
            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseT
            injectCredentialsAndInvokeV2(RequestT request, Function<RequestT, ResponseT> requestFunction) {
                return proxy.injectCredentialsAndInvokeV2(request, requestFunction);
            }

            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse>
            CompletableFuture<ResponseT>
            injectCredentialsAndInvokeV2Async(RequestT request, Function<RequestT, CompletableFuture<ResponseT>> requestFunction) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse, IterableT extends SdkIterable<ResponseT>>
            IterableT
            injectCredentialsAndInvokeIterableV2(RequestT request, Function<RequestT, IterableT> requestFunction) {
                return proxy.injectCredentialsAndInvokeIterableV2(request, requestFunction);
            }

            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseInputStream<ResponseT>
            injectCredentialsAndInvokeV2InputStream(RequestT requestT, Function<RequestT, ResponseInputStream<ResponseT>> function) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseBytes<ResponseT>
            injectCredentialsAndInvokeV2Bytes(RequestT requestT, Function<RequestT, ResponseBytes<ResponseT>> function) {
                throw new UnsupportedOperationException();
            }

            @Override
            public NetworkFirewallClient client() {
                return NetworkFirewallClient;
            }
        };
    }

    DescribeFirewallResponse commonDescribeResponse(String firewallStatus) {
        final Firewall firewall = Firewall.builder()
                .firewallId("id")
                .firewallArn("validarn")
                .firewallName("firewallName")
                .firewallPolicyArn("policyarn")
                .vpcId("vpcId")
                .build();
        final software.amazon.awssdk.services.networkfirewall.model.FirewallStatus status =
                software.amazon.awssdk.services.networkfirewall.model.FirewallStatus.builder()
                        .status(firewallStatus)
                        .configurationSyncStateSummary(ConfigurationSyncState.IN_SYNC)
                        .build();

        return DescribeFirewallResponse.builder().firewall(firewall).firewallStatus(status).build();
    }

    DescribeFirewallResponse commonDescribeResponse() {
        return commonDescribeResponse(FirewallStatusValue.READY.toString());
    }
}
