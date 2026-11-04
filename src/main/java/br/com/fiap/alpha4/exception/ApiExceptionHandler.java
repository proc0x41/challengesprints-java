package br.com.fiap.alpha4.exception;

import br.com.fiap.alpha4.model.Inputs.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Message> api(ApiException exception) {
        return ResponseEntity.status(exception.status()).body(new Message(exception.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class})
    ResponseEntity<Message> invalid(Exception exception) {
        return ResponseEntity.badRequest().body(new Message("Dados inválidos. Confira os campos e os limites documentados."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Message> conflict(DataIntegrityViolationException exception) {
        return ResponseEntity.status(409).body(new Message("A operação conflita com um registro existente ou com seu histórico."));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<Message> missing(NoResourceFoundException exception) {
        return ResponseEntity.status(404).body(new Message("Recurso não encontrado."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<Message> method(HttpRequestMethodNotSupportedException exception) {
        return ResponseEntity.status(405).body(new Message("Método HTTP não permitido para este recurso."));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<Message> media(HttpMediaTypeNotSupportedException exception) {
        return ResponseEntity.status(415).body(new Message("Envie o conteúdo no formato JSON."));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Message> unexpected(Exception exception) {
        LOG.error("Falha interna ao processar requisição: {}", exception.getClass().getSimpleName());
        return ResponseEntity.internalServerError().body(new Message("Não foi possível concluir a operação. Tente novamente."));
    }
}
