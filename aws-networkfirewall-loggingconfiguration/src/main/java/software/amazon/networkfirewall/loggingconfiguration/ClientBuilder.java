package software.amazon.networkfirewall.loggingconfiguration;

import java.util.function.Supplier;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.cloudformation.LambdaWrapper;


public class ClientBuilder {

  public static NetworkFirewallClient getClient() {

    return NetworkFirewallClient.builder()
            .httpClient(LambdaWrapper.HTTP_CLIENT)
            .build();
  }

  public static Supplier<NetworkFirewallClient> getClient(final String region) {
    if (region.equals("us-gov-west-1") || region.equals("us-gov-east-1")) {
      return () -> (NetworkFirewallClient) NetworkFirewallClient.builder()
              .httpClient(LambdaWrapper.HTTP_CLIENT)
              .region(Region.of("fips-" + region))
              .build();
    }
    return () -> (NetworkFirewallClient) NetworkFirewallClient.builder()
            .httpClient(LambdaWrapper.HTTP_CLIENT)
            .build();
  }
}
