package br.com.somosdb.votacao.shared.error;

import java.net.URI;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ResponseEntity<ProblemDetail> handleNotFound(
            RecursoNaoEncontradoException exception,
            HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, exception.getCodigo(), exception.getMessage(), request);
    }

    @ExceptionHandler(RegraNegocioException.class)
    ResponseEntity<ProblemDetail> handleBusinessRule(
            RegraNegocioException exception,
            HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, exception.getCodigo(), exception.getMessage(), request);
    }

    @ExceptionHandler(AssociadoNaoPodeVotarException.class)
    ResponseEntity<ProblemDetail> handleUnableToVote(
            AssociadoNaoPodeVotarException exception,
            HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, "UNABLE_TO_VOTE", exception.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> handleDataIntegrity(HttpServletRequest request) {
        return problem(
                HttpStatus.CONFLICT,
                "CONFLITO_DE_INTEGRIDADE",
                "A operação conflita com um registro já existente",
                request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        ProblemDetail detail = baseProblem(
                HttpStatus.BAD_REQUEST,
                "REQUISICAO_INVALIDA",
                "Um ou mais campos são inválidos",
                request);
        List<Violacao> violacoes = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new Violacao(error.getField(), error.getDefaultMessage()))
                .toList();
        detail.setProperty("violacoes", violacoes);
        return ResponseEntity.badRequest().body(detail);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request) {
        ProblemDetail detail = baseProblem(
                HttpStatus.BAD_REQUEST,
                "REQUISICAO_INVALIDA",
                "Um ou mais parâmetros são inválidos",
                request);
        List<Violacao> violacoes = exception.getConstraintViolations().stream()
                .map(violation -> new Violacao(violation.getPropertyPath().toString(), violation.getMessage()))
                .toList();
        detail.setProperty("violacoes", violacoes);
        return ResponseEntity.badRequest().body(detail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> handleUnreadable(HttpServletRequest request) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "JSON_INVALIDO",
                "O corpo da requisição está ausente ou possui formato inválido",
                request);
    }

    private ResponseEntity<ProblemDetail> problem(
            HttpStatus status,
            String codigo,
            String mensagem,
            HttpServletRequest request) {
        return ResponseEntity.status(status).body(baseProblem(status, codigo, mensagem, request));
    }

    private ProblemDetail baseProblem(
            HttpStatus status,
            String codigo,
            String mensagem,
            HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, mensagem);
        detail.setTitle(status.getReasonPhrase());
        detail.setInstance(URI.create(request.getRequestURI()));
        detail.setProperty("codigo", codigo);
        return detail;
    }

    record Violacao(String campo, String mensagem) {
    }
}
