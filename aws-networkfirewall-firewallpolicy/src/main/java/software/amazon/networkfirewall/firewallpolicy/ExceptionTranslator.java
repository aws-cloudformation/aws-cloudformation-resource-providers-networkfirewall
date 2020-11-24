package software.amazon.networkfirewall.firewallpolicy;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.networkfirewall.model.*;
import software.amazon.awssdk.services.networkfirewall.model.ResourceNotFoundException;
import software.amazon.awssdk.services.networkfirewall.model.UnsupportedOperationException;
import software.amazon.cloudformation.exceptions.*;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;

public class ExceptionTranslator {

    private ExceptionTranslator() { }

    public static void translateToCfnException(final AwsServiceException exception) {
        if (exception instanceof InvalidRequestException) {
            if (exception.getMessage() != null && exception.getMessage().contains("A resource with the specified name already exists")) {
                throw new CfnAlreadyExistsException(exception);
            } else {
                throw new CfnInvalidRequestException(exception);
            }
        }
        if (exception instanceof ThrottlingException) {
            throw new CfnThrottlingException(exception);
        }
        if (exception instanceof InternalServerErrorException) {
            throw new CfnServiceInternalErrorException(exception);
        }
        if (exception instanceof ResourceNotFoundException) {
            throw new CfnNotFoundException(exception);
        }
        if (exception instanceof InvalidTokenException) {
            throw new CfnInvalidRequestException(exception);
        }
        if (exception instanceof LimitExceededException) {
            throw new CfnServiceLimitExceededException(exception);
        }
        if (exception instanceof InsufficientCapacityException) {
            throw new CfnServiceLimitExceededException(exception);
        }
        if (exception instanceof UnsupportedOperationException) {
            throw new CfnInvalidRequestException(exception);
        }
        if (exception instanceof InvalidOperationException) {
            throw new CfnInvalidRequestException(exception);
        }
        throw new CfnGeneralServiceException(exception.getMessage(), exception);
    }

}
