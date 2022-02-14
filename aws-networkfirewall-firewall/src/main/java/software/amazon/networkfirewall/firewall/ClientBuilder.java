package software.amazon.networkfirewall.firewall;

import java.util.function.Supplier;
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
                    .fipsEnabled(true))
              .build();
        }
        return () -> (NetworkFirewallClient) NetworkFirewallClient.builder()
                .httpClient(LambdaWrapper.HTTP_CLIENT)
                .build();
    }
}
