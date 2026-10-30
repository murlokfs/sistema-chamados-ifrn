package br.edu.ifrn.chamados.exception;

import java.time.OffsetDateTime;

public record ErroResponse(OffsetDateTime timestamp, int status, String mensagem, String caminho) {
}
