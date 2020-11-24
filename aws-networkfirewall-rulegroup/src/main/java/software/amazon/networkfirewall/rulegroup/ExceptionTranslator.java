package software.amazon.networkfirewall.rulegroup;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.networkfirewall.model.InsufficientCapacityException;
import software.amazon.awssdk.services.networkfirewall.model.InternalServerErrorException;
import software.amazon.awssdk.services.networkfirewall.model.InvalidOperationException;
import software.amazon.awssdk.services.networkfirewall.model.InvalidRequestException;
import software.amazon.awssdk.services.networkfirewall.model.InvalidTokenException;
import software.amazon.awssdk.services.networkfirewall.model.LimitExceededException;
import software.amazon.awssdk.services.networkfirewall.model.ResourceNotFoundException;
import software.amazon.awssdk.services.networkfirewall.model.ThrottlingException;

import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;

public class ExceptionTranslator {

    public static RuntimeException translateToCfnException(final AwsServiceException e) {
        RuntimeException toReturn = null;
        if (e instanceof InvalidRequestException) {
            if (e.getMessage() != null && e.getMessage().contains("A resource with the specified name already exists")) {
                toReturn = new CfnAlreadyExistsException(e);
            } else {
                toReturn = new CfnInvalidRequestException(e);
            }
        } else if (e instanceof ThrottlingException) {
            toReturn = new CfnThrottlingException(e);
        } else if (e instanceof InternalServerErrorException) {
            toReturn = new CfnServiceInternalErrorException(e);
        } else if (e instanceof ResourceNotFoundException) {
            toReturn = new CfnNotFoundException(e);
        } else if (e instanceof InvalidTokenException) {
            toReturn = new CfnInvalidRequestException(e);
        } else if (e instanceof LimitExceededException) {
            toReturn = new CfnServiceLimitExceededException(e);
        } else if (e instanceof InsufficientCapacityException) {
            toReturn = new CfnServiceInternalErrorException(e);
        } else if (e instanceof InvalidOperationException) {
            toReturn = new CfnInvalidRequestException(e);
        } else {
            toReturn = new CfnGeneralServiceException(e.getMessage(), e);
        }
        return toReturn;
    }
}
