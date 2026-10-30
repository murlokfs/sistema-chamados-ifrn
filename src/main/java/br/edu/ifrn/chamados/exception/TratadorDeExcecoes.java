package br.edu.ifrn.chamados.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class TratadorDeExcecoes {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException ex, HttpServletRequest request) {
        return resposta(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResponse> regraNegocio(RegraNegocioException ex, HttpServletRequest request) {
        return resposta(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(DadosInvalidosException.class)
    public ResponseEntity<ErroResponse> dadosInvalidos(DadosInvalidosException ex, HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .sorted().distinct().collect(Collectors.joining("; "));
        return resposta(HttpStatus.BAD_REQUEST, mensagem, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> jsonInvalido(HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST,
                "Corpo da requisição inválido. Confira os campos, os tipos e os valores dos enums.", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> parametroInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "Valor inválido para o parâmetro " + ex.getName() + ".", request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErroResponse> parametroAusente(MissingServletRequestParameterException ex, HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "Informe o parâmetro " + ex.getParameterName() + ".", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> integridade(HttpServletRequest request) {
        return resposta(HttpStatus.CONFLICT,
                "A operação conflita com um cadastro existente ou com um vínculo que precisa ser mantido.", request);
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ErroResponse> concorrencia(HttpServletRequest request) {
        return resposta(HttpStatus.CONFLICT, "O chamado está sendo atualizado. Tente novamente.", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResponse> rotaInexistente(HttpServletRequest request) {
        return resposta(HttpStatus.NOT_FOUND, "Rota não encontrada.", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResponse> metodoInvalido(HttpServletRequest request) {
        return resposta(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP não permitido para esta rota.", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErroResponse> formatoInvalido(HttpServletRequest request) {
        return resposta(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Envie o corpo com Content-Type application/json.", request);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErroResponse> respostaIncompativel(HttpServletRequest request) {
        return resposta(HttpStatus.NOT_ACCEPTABLE, "Esta API devolve respostas em application/json.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> inesperado(Exception ex, HttpServletRequest request) {
        log.error("Erro inesperado em {}", request.getRequestURI(), ex);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível concluir a operação.", request);
    }

    private ResponseEntity<ErroResponse> resposta(HttpStatus status, String mensagem, HttpServletRequest request) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(new ErroResponse(
                OffsetDateTime.now(ZoneOffset.UTC), status.value(), mensagem, request.getRequestURI()));
    }
}
