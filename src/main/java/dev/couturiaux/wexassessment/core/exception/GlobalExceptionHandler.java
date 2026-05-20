package dev.couturiaux.wexassessment.core.exception;

import dev.couturiaux.wexassessment.core.currency.ExchangeRateNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private static final String TIMESTAMP_PROPERTY = "timestamp";

  @ExceptionHandler(ResourceNotFoundException.class)
  public ProblemDetail handleNotFoundException(ResourceNotFoundException ex) {
    logger.warn("SERVICE: Lookup failed: {}", ex.getMessage());

    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    problemDetail.setTitle("Resource Not Found");
    problemDetail.setProperty(TIMESTAMP_PROPERTY, Instant.now());
    return problemDetail;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleValidationExceptions(MethodArgumentNotValidException ex) {
    logger.warn(
        "API: Request failed validation check. Input error count: {}",
        ex.getBindingResult().getErrorCount());

    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "Your request contains invalid fields.");
    problemDetail.setTitle("Validation Failed");
    problemDetail.setProperty(TIMESTAMP_PROPERTY, Instant.now());

    List<Map<String, String>> invalidParams =
        ex.getBindingResult().getFieldErrors().stream()
            .map(
                error ->
                    Map.of(
                        "field",
                        error.getField(),
                        "reason",
                        error.getDefaultMessage() != null
                            ? error.getDefaultMessage()
                            : "Invalid Value"))
            .toList();

    problemDetail.setProperty("invalid_params", invalidParams);
    return problemDetail;
  }

  @ExceptionHandler(Exception.class)
  public ProblemDetail handleGenericException(Exception ex) {
    logger.error("SYSTEM: Unhandled system error", ex);

    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected internal server error occurred. Please contact support.");
    problemDetail.setTitle("Internal Server Error");
    problemDetail.setProperty(TIMESTAMP_PROPERTY, Instant.now());
    return problemDetail;
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ProblemDetail handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
    logger.warn("API: Malformed HTTP request payload: {}", ex.getMostSpecificCause().getMessage());

    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "The request body is malformed or invalid JSON syntax.");
    problemDetail.setTitle("Malformed JSON payload");
    problemDetail.setProperty(TIMESTAMP_PROPERTY, Instant.now());

    return problemDetail;
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ProblemDetail handleArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
    String paramName = ex.getName();
    String rejectedValue = String.valueOf(ex.getValue());
    Class<?> requiredTypeClass = ex.getRequiredType();
    String requiredType =
        (requiredTypeClass != null) ? requiredTypeClass.getSimpleName() : "unknown";

    logger.warn(
        "API: Parameter type mismatch: Field {} received value [{}] but requires type [{}]",
        paramName,
        rejectedValue,
        requiredType);

    String detailedMessage =
        "The paramter '%s' expects a valid %s format. Value provided: '%s'"
            .formatted(paramName, requiredType, rejectedValue);

    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detailedMessage);
    problemDetail.setTitle("Invalid Parameter Type");
    problemDetail.setProperty(TIMESTAMP_PROPERTY, Instant.now());

    return problemDetail;
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  ProblemDetail handleMissingParameterExcception(MissingServletRequestParameterException ex) {
    String paramName = ex.getParameterName();
    String paramType = ex.getParameterType();

    logger.warn(
        "API: Required request paramter missing: Name '{}' of type [{}] was omitted from the"
            + " request URL",
        paramName,
        paramType);

    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "The required query parameter '%s' (%s) is missing from this request"
                .formatted(paramName, paramType));

    problemDetail.setTitle("Missing Request Parameter");
    problemDetail.setProperty(TIMESTAMP_PROPERTY, Instant.now());

    return problemDetail;
  }

  @ExceptionHandler(UnsupportedCountryCurrencyException.class)
  ProblemDetail handleUnsupportedCountryCurrencyException(UnsupportedCountryCurrencyException ex) {
    logger.warn("SERVICE: Unsupported country currency violation: {}", ex.getMessage());

    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    problemDetail.setTitle("Unsupported Currency Configuration");

    return problemDetail;
  }

  @ExceptionHandler(ExchangeRateNotFoundException.class)
  ProblemDetail handleExchangeRateNotFound(ExchangeRateNotFoundException ex) {
    logger.warn("INTEGRATION: Historical rate gap encountered: {}", ex.getMessage());

    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    problemDetail.setTitle("Exchange Rate Data Missing");

    return problemDetail;
  }

  @ExceptionHandler(TreasuryApiUnavailableException.class)
  ProblemDetail handleTreasuryUnavailable(TreasuryApiUnavailableException ex) {
    logger.error("INTEGRATION: Treasury API integration crash intercepted: ", ex);

    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());

    problemDetail.setTitle("Downstream Provider Offline");
    problemDetail.setProperty(TIMESTAMP_PROPERTY, Instant.now());

    return problemDetail;
  }
}
