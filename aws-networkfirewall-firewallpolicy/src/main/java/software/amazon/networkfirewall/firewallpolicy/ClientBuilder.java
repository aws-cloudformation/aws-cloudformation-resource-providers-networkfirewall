package software.amazon.networkfirewall.firewallpolicy;

import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.cloudformation.LambdaWrapper;

public class ClientBuilder {

  public static NetworkFirewallClient getClient() {
    return NetworkFirewallClient.builder()
            .httpClient(LambdaWrapper.HTTP_CLIENT)
            .build();
  }
}
