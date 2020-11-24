package software.amazon.networkfirewall.loggingconfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Credentials;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.ProxyClient;

import static org.mockito.Mockito.mock;

public class AbstractTestBase {

  protected CallbackContext context;

  protected final static String firewallName = "firewall01";

  protected final static String firewallArn = "arn:aws:network-firewall:us-east-1:123456789012:firewall/firewall01";

  protected static final Credentials MOCK_CREDENTIALS;

  static {
    MOCK_CREDENTIALS = new Credentials("accessKey", "secretKey", "token");
  }



  @Mock
  protected AmazonWebServicesClientProxy proxy;

  @Mock
  protected LoggerProxy logger;

  @BeforeEach
  public void setup() {
    proxy = mock(AmazonWebServicesClientProxy.class);
    logger = new LoggerProxy();
    context = null;
  }

  protected ResourceModel buildResourceModel(LoggingConfiguration loggingConfiguration) {
    return ResourceModel.builder()
            .firewallName(firewallName)
            .firewallArn(firewallArn)
            .loggingConfiguration(loggingConfiguration)
            .build();
  }

    protected ResourceModel buildResourceModel() {
        return ResourceModel.builder()
                .firewallArn(firewallArn)
                .build();
    }

  static ProxyClient<NetworkFirewallClient> MOCK_PROXY(
          final AmazonWebServicesClientProxy proxy,
          final NetworkFirewallClient networkFirewallClient) {
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
        return networkFirewallClient;
      }
    };
  }

    protected LogDestinationConfig buildLogDestinationConfig(final String logType,
          final String logDestinationType) {
        LogDestinationConfig logDestinationConfig = new LogDestinationConfig();
        logDestinationConfig.setLogType(logType);
        logDestinationConfig.setLogDestinationType(logDestinationType);

        Map<String, String> logDestination = new HashMap<>();
        if (logDestinationType.equals("S3")) {
            logDestination.put("bucketName", "bucket01");
            logDestination.put("prefix", "prefix");
            logDestinationConfig.setLogDestination(logDestination);
        } else if (logDestinationType.equals("KinesisDataFirehose")) {
            logDestination.put("deliveryStream", "streamName");
            logDestinationConfig.setLogDestination(logDestination);
        } else {
            logDestination.put("logGroup", "logGroupName");
            logDestinationConfig.setLogDestination(logDestination);
        }

        return logDestinationConfig;
  }

  protected LoggingConfiguration buildLoggingConfiguration(List<LogDestinationConfig> list) {
      LoggingConfiguration loggingConfiguration = new LoggingConfiguration();
      loggingConfiguration.setLogDestinationConfigs(list);
      return loggingConfiguration;

  }
}
