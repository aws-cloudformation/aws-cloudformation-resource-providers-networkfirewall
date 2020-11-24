package software.amazon.networkfirewall.loggingconfiguration;

import software.amazon.awssdk.services.networkfirewall.model.DescribeLoggingConfigurationRequest;
import software.amazon.awssdk.services.networkfirewall.model.DescribeLoggingConfigurationResponse;
import software.amazon.awssdk.services.networkfirewall.model.UpdateLoggingConfigurationRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is a centralized placeholder for
 *  - api request construction
 *  - object translation to/from aws sdk
 *  - resource model construction for read/list handlers
 */

public class Translator {

  /**
   * Request to read a resource
   * @param model resource model
   * @return awsRequest the aws service request to describe a resource
   */
  static DescribeLoggingConfigurationRequest translateToReadRequest(final ResourceModel model) {
      return DescribeLoggingConfigurationRequest.builder()
              .firewallArn(model.getFirewallArn())
              .firewallName(model.getFirewallName())
              .build();
  }

  /**
   * Translates resource object from sdk into a resource model
   * @param describeLoggingConfigurationResponse the networkFirewall describeLoggingConfiguration response
   * @return model resource model
   */
  static ResourceModel translateFromReadResponse(
          final DescribeLoggingConfigurationResponse describeLoggingConfigurationResponse) {
      return ResourceModel.builder()
          .firewallArn(describeLoggingConfigurationResponse.firewallArn())
          .loggingConfiguration(Translator.toModelLoggingConfiguration(
                  describeLoggingConfigurationResponse.loggingConfiguration()))
          .build();
  }

  /**
   * Request to update properties of a previously created resource
   * @param model resource model
   * @return awsRequest the aws service request to modify a resource
   */
  static UpdateLoggingConfigurationRequest translateToUpdateRequest(final ResourceModel model,
          final LoggingConfiguration loggingConfiguration) {
      return UpdateLoggingConfigurationRequest.builder()
              .firewallArn(model.getFirewallArn())
              .firewallName(model.getFirewallName())
              .loggingConfiguration(toSdkLoggingConfiguration(loggingConfiguration))
              .build();
  }

  static software.amazon.awssdk.services.networkfirewall.model.LoggingConfiguration toSdkLoggingConfiguration(
          final LoggingConfiguration loggingConfiguration) {

      if (loggingConfiguration == null) {
        return null;
      }
      software.amazon.awssdk.services.networkfirewall.model.LoggingConfiguration.Builder modelLoggingConfiguration =
              software.amazon.awssdk.services.networkfirewall.model.LoggingConfiguration.builder();

      List<software.amazon.awssdk.services.networkfirewall.model.LogDestinationConfig> modelLogDestinationConfig =
              new ArrayList<>();
      loggingConfiguration.getLogDestinationConfigs().forEach(logDestinationConfig ->
              modelLogDestinationConfig.add(toSdkLogDestinationConfig(logDestinationConfig)));
      modelLoggingConfiguration.logDestinationConfigs(modelLogDestinationConfig);

      return modelLoggingConfiguration.build();
  }

  static software.amazon.awssdk.services.networkfirewall.model.LogDestinationConfig toSdkLogDestinationConfig(
          final LogDestinationConfig logDestinationConfig) {

      software.amazon.awssdk.services.networkfirewall.model.LogDestinationConfig.Builder modelLogDestinationConfig =
              software.amazon.awssdk.services.networkfirewall.model.LogDestinationConfig.builder();
      modelLogDestinationConfig.logType(logDestinationConfig.getLogType());
      modelLogDestinationConfig.logDestinationType(logDestinationConfig.getLogDestinationType());
      modelLogDestinationConfig.logDestination((logDestinationConfig.getLogDestination()));

      return modelLogDestinationConfig.build();
  }

  static LoggingConfiguration toModelLoggingConfiguration(
          final software.amazon.awssdk.services.networkfirewall.model.LoggingConfiguration loggingConfiguration) {

      LoggingConfiguration modelLoggingConfiguration = new LoggingConfiguration();

      if (loggingConfiguration == null) {
          modelLoggingConfiguration.setLogDestinationConfigs(new ArrayList<>());
          return modelLoggingConfiguration;
      }

      List<LogDestinationConfig> modelLogDestinationConfig = new ArrayList<>();
      loggingConfiguration.logDestinationConfigs().forEach(logDestinationConfig ->
              modelLogDestinationConfig.add(toModelLogDestinationConfig(logDestinationConfig)));
      modelLoggingConfiguration.setLogDestinationConfigs(modelLogDestinationConfig);

      return modelLoggingConfiguration;
  }

  static LogDestinationConfig toModelLogDestinationConfig(
          final software.amazon.awssdk.services.networkfirewall.model.LogDestinationConfig logDestinationConfig) {

      LogDestinationConfig modelLogDestinationConfig = new LogDestinationConfig();
      modelLogDestinationConfig.setLogType(logDestinationConfig.logType().toString());
      modelLogDestinationConfig.setLogDestinationType(logDestinationConfig.logDestinationTypeAsString());
      modelLogDestinationConfig.setLogDestination(logDestinationConfig.logDestination());

      return modelLogDestinationConfig;
  }
}
