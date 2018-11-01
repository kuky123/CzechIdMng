package eu.bcvsolutions.idm.core.exception;

import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.PersistenceException;

import org.apache.commons.lang.StringUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.dto.ResultModels;
import eu.bcvsolutions.idm.core.api.exception.CoreException;
import eu.bcvsolutions.idm.core.api.exception.DefaultErrorModel;
import eu.bcvsolutions.idm.core.api.exception.ErrorModel;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.security.exception.IdmAuthenticationException;

/**
 * Handles application exceptions and translate them to result codes
 * 
 * @author Radek Tomiška
 *
 */
@ControllerAdvice
public class ExceptionControllerAdvice {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ExceptionControllerAdvice.class);
	
	@ExceptionHandler(ResultCodeException.class)
    ResponseEntity<ResultModels> handle(ResultCodeException ex) {
		if (ex.getStatus().is5xxServerError()) {
			LOG.error("[" + ex.getId() + "] ", ex);
		} else if(ex.getStatus().is2xxSuccessful()) {
			// nothing
			// TODO: refactor restApplicationException to different types or reimplement 2xx event throwing mechanism
		} else {
			LOG.warn("[" + ex.getId() + "] ", ex);
		}
		return new ResponseEntity<>(ex.getError(), new HttpHeaders(), ex.getStatus());
	}
	
	@ExceptionHandler(IdmAuthenticationException.class)
	ResponseEntity<ResultModels> handle(IdmAuthenticationException ex) {
		ErrorModel errorModel = new DefaultErrorModel(CoreResultCode.AUTH_FAILED);
		 // source exception message is shown only in log 
		LOG.warn("[" + errorModel.getId() + "] ", ex);
        return new ResponseEntity<>(new ResultModels(errorModel), new HttpHeaders(), errorModel.getStatus());
    }
	
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	ResponseEntity<ResultModels> handle(HttpRequestMethodNotSupportedException ex) {
		ErrorModel errorModel = new DefaultErrorModel(CoreResultCode.METHOD_NOT_ALLOWED, ex.getMessage(),
				ImmutableMap.of( //
						"errorMethod", ex.getMethod(), //
						"supportedMethods", StringUtils.join(ex.getSupportedMethods(), ", ")));
		LOG.warn("[" + errorModel.getId() + "] ", ex);
		return new ResponseEntity<>(new ResultModels(errorModel), new HttpHeaders(), errorModel.getStatus());
	}
	
	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ResultModels> handle(HttpMessageNotReadableException ex) {
		ErrorModel errorModel = new DefaultErrorModel(CoreResultCode.METHOD_NOT_ALLOWED, ex.getMessage());
		LOG.warn("[" + errorModel.getId() + "] ", ex);
        return new ResponseEntity<>(new ResultModels(errorModel), new HttpHeaders(), errorModel.getStatus());
    }
	
	@ExceptionHandler(RepositoryConstraintViolationException.class)
	ResponseEntity<ResultModels> handle(RepositoryConstraintViolationException ex) {
		List<ErrorModel> errorModels = ex.getErrors().getFieldErrors().stream()
			.map(fieldError -> new FieldErrorModel(fieldError))
			.peek(errorModel -> LOG.warn("[" + errorModel.getId() + "] ", ex))
			.collect(Collectors.toList());
		// TODO: global errors
		// TODO: better errorModel logging - move source exception to errorModel?		
        return new ResponseEntity<>(new ResultModels(errorModels), new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }
	
	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ResultModels> handle(MethodArgumentNotValidException ex) {		
		List<ErrorModel> errorModels = ex.getBindingResult().getFieldErrors().stream()
			.map(fieldError -> new FieldErrorModel(fieldError))
			.peek(errorModel -> LOG.warn("[" + errorModel.getId() + "] ", ex))
			.collect(Collectors.toList());
		// TODO: global errors
		// TODO: better errorModel logging - move source exception to errorModel?	
        return new ResponseEntity<>(new ResultModels(errorModels), new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }
	
	@ExceptionHandler(javax.validation.ConstraintViolationException.class)
	ResponseEntity<ResultModels> handle(javax.validation.ConstraintViolationException ex) {		
		List<ErrorModel> errorModels = ex.getConstraintViolations().stream()
			.map(constraintViolation -> new FieldErrorModel(constraintViolation))
			.peek(errorModel -> LOG.warn("[" + errorModel.getId() + "] ", ex))
			.collect(Collectors.toList());
		// TODO: global errors
		// TODO: better errorModel logging - move source exception to errorModel?
        return new ResponseEntity<>(new ResultModels(errorModels), new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }
	
	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ResultModels> handle(DataIntegrityViolationException ex) {
		ErrorModel errorModel = null;
		//
		if (ex.getCause() != null && ex.getCause() instanceof ConstraintViolationException){
			ConstraintViolationException constraintEx = (ConstraintViolationException) ex.getCause();
			// TODO: registrable constraint error codes
			if (constraintEx.getConstraintName() != null && constraintEx.getConstraintName().contains("name")) {
				errorModel = new DefaultErrorModel(CoreResultCode.NAME_CONFLICT, ImmutableMap.of("name", constraintEx.getConstraintName()));
			} else if (constraintEx.getConstraintName() != null && constraintEx.getConstraintName().contains("code")) {
				errorModel = new DefaultErrorModel(CoreResultCode.CODE_CONFLICT, ImmutableMap.of("name", constraintEx.getConstraintName()));
			} else if (constraintEx.getConstraintName() == null) {
				errorModel = new DefaultErrorModel(CoreResultCode.CONFLICT, ImmutableMap.of("name", "..."));
			} else {
				errorModel = new DefaultErrorModel(CoreResultCode.CONFLICT, ImmutableMap.of("name", StringUtils.trimToEmpty(constraintEx.getConstraintName())));
			}
		} else {
			errorModel = new DefaultErrorModel(CoreResultCode.CONFLICT, ex.getMostSpecificCause().getMessage());
		}
		LOG.error("[" + errorModel.getId() + "] ", ex);
		return new ResponseEntity<>(new ResultModels(errorModel), new HttpHeaders(), errorModel.getStatus());
    }
	
	@ExceptionHandler(PersistenceException.class)
	ResponseEntity<ResultModels> handle(PersistenceException ex) {
		ErrorModel errorModel = null;
		//
		if (ex.getCause() != null && ex.getCause() instanceof ConstraintViolationException){
			ConstraintViolationException constraintEx = (ConstraintViolationException) ex.getCause();
			// TODO: registrable contstrain error codes
			if (constraintEx.getConstraintName().contains("name")) {
				errorModel = new DefaultErrorModel(CoreResultCode.NAME_CONFLICT, ImmutableMap.of("name", constraintEx.getConstraintName()));
			} else if (constraintEx.getConstraintName().contains("code")) {
				errorModel = new DefaultErrorModel(CoreResultCode.CODE_CONFLICT, ImmutableMap.of("name", constraintEx.getConstraintName()));
			} else {
				errorModel = new DefaultErrorModel(CoreResultCode.CONFLICT, ImmutableMap.of("name", constraintEx.getConstraintName()));
			}
		} else {
			errorModel = new DefaultErrorModel(CoreResultCode.CONFLICT, ex.getMessage());
		}
		LOG.error("[" + errorModel.getId() + "] ", ex);
		return new ResponseEntity<>(new ResultModels(errorModel), new HttpHeaders(), errorModel.getStatus());
	}
	
	@ExceptionHandler(AccessDeniedException.class)
	ResponseEntity<ResultModels> handle(AccessDeniedException ex) {	
		ErrorModel errorModel = new DefaultErrorModel(CoreResultCode.FORBIDDEN, ex.getMessage());
		LOG.warn("[" + errorModel.getId() + "] ", ex);
        return new ResponseEntity<>(new ResultModels(errorModel), new HttpHeaders(), errorModel.getStatus());
    }
	
	@ExceptionHandler(CoreException.class)
	ResponseEntity<ResultModels> handle(CoreException ex) {	
		ErrorModel errorModel = new DefaultErrorModel(CoreResultCode.INTERNAL_SERVER_ERROR, ex.getMessage(), ex.getDetails());
		LOG.error("[" + errorModel.getId() + "] ", ex);
        return new ResponseEntity<>(new ResultModels(errorModel), new HttpHeaders(), errorModel.getStatus());
    }
	
	@ExceptionHandler(Exception.class)
	ResponseEntity<ResultModels> handle(Exception ex) {
		Throwable cause = Throwables.getRootCause(ex);
		// If is cause instance of ResultCodeException, then we will log catched exception and throw only ResultCodeException (for better show on frontend)
		if (cause instanceof ResultCodeException){
			LOG.error(ex.getLocalizedMessage(), ex);
			return handle((ResultCodeException)cause);
		} else {
			ErrorModel errorModel = new DefaultErrorModel(CoreResultCode.INTERNAL_SERVER_ERROR, ex.getMessage());
			LOG.error("[" + errorModel.getId() + "] ", ex);
			return new ResponseEntity<>(new ResultModels(errorModel), new HttpHeaders(), errorModel.getStatus());
		}
    }
}