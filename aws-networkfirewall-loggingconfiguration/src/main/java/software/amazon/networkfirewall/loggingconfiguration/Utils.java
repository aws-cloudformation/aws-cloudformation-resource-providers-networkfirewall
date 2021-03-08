package software.amazon.networkfirewall.loggingconfiguration;

import com.amazonaws.util.CollectionUtils;
import software.amazon.awssdk.services.networkfirewall.NetworkFirewallClient;
import software.amazon.awssdk.services.networkfirewall.model.DescribeLoggingConfigurationRequest;
import software.amazon.awssdk.services.networkfirewall.model.DescribeLoggingConfigurationResponse;
import software.amazon.awssdk.services.networkfirewall.model.InternalServerErrorException;
import software.amazon.awssdk.services.networkfirewall.model.InvalidRequestException;
import software.amazon.awssdk.services.networkfirewall.model.LogDestinationPermissionException;
import software.amazon.awssdk.services.networkfirewall.model.ResourceNotFoundException;
import software.amazon.awssdk.services.networkfirewall.model.ThrottlingException;
import software.amazon.awssdk.services.networkfirewall.model.UpdateLoggingConfigurationRequest;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.ProxyClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static software.amazon.networkfirewall.loggingconfiguration.Translator.toModelLoggingConfiguration;
import static software.amazon.networkfirewall.loggingconfiguration.Translator.translateToReadRequest;

public class Utils {
    final static Set<String> logTypeSet = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("FLOW", "ALERT")));

    static DescribeLoggingConfigurationResponse validateResourceExists(
            final DescribeLoggingConfigurationRequest describeLoggingConfigurationRequest,
            final ProxyClient<NetworkFirewallClient> proxyClient) {

        DescribeLoggingConfigurationResponse describeResult = describeLoggingConfigurationCall(
                describeLoggingConfigurationRequest, proxyClient);

        if (describeResult.loggingConfiguration() == null ||
                CollectionUtils.isNullOrEmpty(describeResult.loggingConfiguration().logDestinationConfigs())) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME,
                    describeLoggingConfigurationRequest.firewallArn());
        }

        return describeResult;
    }

    static DescribeLoggingConfigurationResponse validateResourceNotExists(
            final DescribeLoggingConfigurationRequest describeLoggingConfigurationRequest,
            final ProxyClient<NetworkFirewallClient> proxyClient) {

        DescribeLoggingConfigurationResponse describeResult = describeLoggingConfigurationCall(
                describeLoggingConfigurationRequest, proxyClient);

        if (describeResult.loggingConfiguration() != null &&
                !CollectionUtils.isNullOrEmpty(describeResult.loggingConfiguration().logDestinationConfigs())) {
            throw new CfnAlreadyExistsException(ResourceModel.TYPE_NAME,
                    describeLoggingConfigurationRequest.firewallArn());
        }

        return describeResult;
    }

    static DescribeLoggingConfigurationResponse describeLoggingConfigurationCall(
            final DescribeLoggingConfigurationRequest describeLoggingConfigurationRequest,
            final ProxyClient<NetworkFirewallClient> proxyClient) {
        DescribeLoggingConfigurationResponse describeResult;
        try{
            describeResult = proxyClient.injectCredentialsAndInvokeV2(describeLoggingConfigurationRequest,
                    proxyClient.client()::describeLoggingConfiguration);

        } catch(InvalidRequestException e){
            throw new CfnInvalidRequestException(e.getMessage(), e);
        } catch (InternalServerErrorException e) {
            throw new CfnServiceInternalErrorException(ResourceModel.TYPE_NAME, e);
        } catch (ResourceNotFoundException e) {
            throw new CfnNotFoundException("AWS::NetworkFirewall::Firewall", e.toString());
        } catch (ThrottlingException e) {
            throw new CfnThrottlingException(ResourceModel.TYPE_NAME, e);
        }
        return describeResult;
    }

    static void convertTemplateToUpdateLoggingConfigurationCall(
            final ResourceModel model, final ProxyClient<NetworkFirewallClient> proxyClient) {
        convertTemplateToUpdateLoggingConfigurationCall(model, proxyClient, false, false);
    }

    //TODO: The signatrue of this method is not reasonable. Instead of using booleans,
    // we should use some operation enums to reflect the operations.
    static void convertTemplateToUpdateLoggingConfigurationCall(
            final ResourceModel model, final ProxyClient<NetworkFirewallClient> proxyClient, boolean isDeleteRequest, boolean isCreateRequest) {
        LoggingConfiguration trackingLoggingConfiguration;

        if (!isCreateRequest) {
            final DescribeLoggingConfigurationRequest readRequest = translateToReadRequest(model);
            final DescribeLoggingConfigurationResponse describeLoggingConfigurationResponse = proxyClient
                    .injectCredentialsAndInvokeV2(readRequest,
                            proxyClient.client()::describeLoggingConfiguration);
            trackingLoggingConfiguration = toModelLoggingConfiguration(
                    describeLoggingConfigurationResponse.loggingConfiguration());
        } else {
            trackingLoggingConfiguration = toModelLoggingConfiguration(null);
        }

        final Map<String, LogDestinationConfig> newConfigMap = isDeleteRequest? new HashMap<>(): convertToMap(model.getLoggingConfiguration());
        final Map<String, LogDestinationConfig> currentConfigMap = convertToMap(trackingLoggingConfiguration);


        for (String logType: logTypeSet) {
            final LogDestinationConfig currentConfig = currentConfigMap.get(logType);
            final LogDestinationConfig newConfig = newConfigMap.get(logType);

            if (newConfig != null) {
                if (currentConfig == null) {
                    // case: we add a new logType
                    final List<LogDestinationConfig> trackingLogDestinationConfig =
                            trackingLoggingConfiguration.getLogDestinationConfigs();
                    trackingLogDestinationConfig.add(newConfig);
                    executeUpdateLoggingConfigurationRequest(model, proxyClient,
                            trackingLoggingConfiguration,trackingLogDestinationConfig);
                } else if (newConfig.getLogDestinationType().equals(currentConfig.getLogDestinationType())) {
                    if (newConfig.getLogDestination().equals(currentConfig.getLogDestination())) {
                        continue;
                    }
                    // case: we just need to update the loggingDestination
                    final List<LogDestinationConfig> trackingLogDestinationConfig =
                            trackingLoggingConfiguration.getLogDestinationConfigs();
                    for (LogDestinationConfig logDestinationConfig: trackingLoggingConfiguration.
                            getLogDestinationConfigs()) {
                        if (logDestinationConfig.getLogType().equals(logType)) {
                            logDestinationConfig.setLogDestination(newConfig.getLogDestination());
                        }
                    }
                    executeUpdateLoggingConfigurationRequest(model, proxyClient,
                            trackingLoggingConfiguration, trackingLogDestinationConfig);
                } else {
                    // case: we need to swap the loggingDestinationType
                    final List<LogDestinationConfig> trackingLogDestinationConfig = new ArrayList<>();
                    for (LogDestinationConfig logDestinationConfig: trackingLoggingConfiguration.
                            getLogDestinationConfigs()) {
                        if (!logDestinationConfig.getLogType().equals(logType)) {
                            trackingLogDestinationConfig.add(logDestinationConfig);
                        }
                    }
                    executeUpdateLoggingConfigurationRequest(model, proxyClient,
                            trackingLoggingConfiguration, trackingLogDestinationConfig);
                    trackingLogDestinationConfig.add(newConfig);
                    executeUpdateLoggingConfigurationRequest(model, proxyClient,
                            trackingLoggingConfiguration, trackingLogDestinationConfig);
                }
            } else {
                if (currentConfig != null) {
                    //case: we remove the current loggingDestinationConfig
                    final List<LogDestinationConfig> trackingLogDestinationConfig = new ArrayList<>();
                    for (LogDestinationConfig logDestinationConfig: trackingLoggingConfiguration.
                            getLogDestinationConfigs()) {
                        if (!logDestinationConfig.getLogType().equals(logType)) {
                            trackingLogDestinationConfig.add(logDestinationConfig);
                        }
                    }
                    executeUpdateLoggingConfigurationRequest(model, proxyClient,
                            trackingLoggingConfiguration, trackingLogDestinationConfig);
                }
            }
        }
    }

    static Map<String, LogDestinationConfig> convertToMap(final LoggingConfiguration loggingConfiguration) {
        if (loggingConfiguration == null ||
                CollectionUtils.isNullOrEmpty(loggingConfiguration.getLogDestinationConfigs())) {
            return new HashMap<>();
        }
        Map<String, LogDestinationConfig> map = new HashMap<>();
        for (LogDestinationConfig logDestinationConfig: loggingConfiguration.getLogDestinationConfigs()) {
            map.put(logDestinationConfig.getLogType(), logDestinationConfig);
        }
        return map;
    }

    static void executeUpdateLoggingConfigurationRequest(final ResourceModel model,
            final ProxyClient<NetworkFirewallClient> proxyClient,
            final LoggingConfiguration trackingLoggingConfiguration,
            final List<LogDestinationConfig> trackingLogDestinationConfig) {
        trackingLoggingConfiguration.setLogDestinationConfigs(trackingLogDestinationConfig);
        UpdateLoggingConfigurationRequest trackingUpdateLoggingConfigurationRequest =
                Translator.translateToUpdateRequest(model, trackingLoggingConfiguration);

        try {
            proxyClient.injectCredentialsAndInvokeV2(trackingUpdateLoggingConfigurationRequest,
                    proxyClient.client()::updateLoggingConfiguration);
        } catch (InvalidRequestException e) {
            throw new CfnInvalidRequestException(e.getMessage(), e);
        } catch (ResourceNotFoundException e) {
            throw new CfnNotFoundException(e);
        } catch (LogDestinationPermissionException e) {
            throw new CfnAccessDeniedException(e);
        } catch (InternalServerErrorException e) {
            throw new CfnServiceInternalErrorException(ResourceModel.TYPE_NAME, e);
        } catch (ThrottlingException e) {
            throw new CfnThrottlingException(ResourceModel.TYPE_NAME, e);
        }
    }

    /**
     * To validate customer's loggingConfiguration.
     */
    static void validateInputModel(final ResourceModel model) {
        final List<LogDestinationConfig> list = model.getLoggingConfiguration().getLogDestinationConfigs();
        if (list.size() == 2){
            if (list.get(0).getLogType().equals(list.get(1).getLogType())) {
                throw new CfnInvalidRequestException(
                        "We don't support publishing 1 type of log to multiple destinations.");
            }
        }
    }

    /**
     * To stablize the update of loggingConfiguration for 10 seconds.
     */
    static void stablize(ProxyClient<NetworkFirewallClient> client, ResourceModel model) throws InterruptedException {
        int time = 0;
        do {
            if (isStable(client, model)) {
                return;
            }
            time++;
            Thread.sleep(Duration.ofSeconds(2).toMillis());
        } while (time < 5);
        throw new CfnNotStabilizedException(ResourceModel.TYPE_NAME, model.getFirewallArn());
    }

    static boolean isStable(ProxyClient<NetworkFirewallClient> client, ResourceModel model) {
        final DescribeLoggingConfigurationRequest describeLoggingConfigurationRequest = Translator.translateToReadRequest(model);
        DescribeLoggingConfigurationResponse describeLoggingConfigurationResponse;
        try {
            describeLoggingConfigurationResponse = client.injectCredentialsAndInvokeV2(
                    describeLoggingConfigurationRequest, client.client()::describeLoggingConfiguration);

            return (model.getLoggingConfiguration() == null && describeLoggingConfigurationResponse.loggingConfiguration() == null)
                    || describeLoggingConfigurationResponse.loggingConfiguration()
                    .equals(Translator.toSdkLoggingConfiguration(model.getLoggingConfiguration()));
        } catch (final Exception e) {
            throw new CfnGeneralServiceException("Failed to retrieve loggingConfiguration definition.");
        }
    }
}
