package br.edu.ifrn.chamados.exception;

public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String recurso, Long id) {
        super(recurso + " de id " + id + " não encontrado.");
    }
}
