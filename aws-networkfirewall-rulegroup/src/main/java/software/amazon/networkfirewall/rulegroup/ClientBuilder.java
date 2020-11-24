package software.amazon.networkfirewall.rulegroup;

import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.cloudformation.LambdaWrapper;

public class ClientBuilder {
  public static NetworkFirewallClient getClient() {

    // hard coded URI to contact beta endpoint for testing
    return NetworkFirewallClient.builder()
            .httpClient(LambdaWrapper.HTTP_CLIENT)
            .build();
  }
}
