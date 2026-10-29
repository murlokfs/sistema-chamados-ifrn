package br.edu.ifrn.chamados.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "chamados")
public class Chamado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, length = 5000)
    private String descricao;

    @Column(nullable = false, length = 150)
    private String local;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Prioridade prioridade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusChamado status;

    @Column(name = "data_abertura", nullable = false)
    private OffsetDateTime dataAbertura;

    @Column(name = "data_fechamento")
    private OffsetDateTime dataFechamento;

    @Column(length = 5000)
    private String solucao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Usuario solicitante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tecnico_id")
    private Usuario tecnico;

    @OneToMany(mappedBy = "chamado")
    @OrderBy("dataHora ASC, id ASC")
    private List<Interacao> interacoes = new ArrayList<>();
}
