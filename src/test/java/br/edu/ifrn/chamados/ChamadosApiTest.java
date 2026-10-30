package br.edu.ifrn.chamados;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ChamadosApiTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @DynamicPropertySource
    static void bancoDeTeste(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void prepararDados() {
        jdbc.execute("TRUNCATE interacoes, chamados, usuarios, categorias, setores RESTART IDENTITY CASCADE");
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V2__inserir_dados_iniciais.sql"))
                .execute(dataSource);
    }

    @Test
    void abreComValoresPadraoEDevolveLocation() throws Exception {
        enviarChamado(1, 1, null)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/chamados/1"))
                .andExpect(jsonPath("$.status").value("ABERTO"))
                .andExpect(jsonPath("$.prioridade").value("MEDIA"))
                .andExpect(jsonPath("$.dataAbertura").isNotEmpty())
                .andExpect(jsonPath("$.dataFechamento").isEmpty())
                .andExpect(jsonPath("$.tecnico").isEmpty());
    }

    @Test
    void percorreAtendimentoComHistorico() throws Exception {
        long id = abrir(1, 1, "ALTA");
        postJson("/chamados/" + id + "/interacoes", """
                {"autorId":1,"texto":"O problema começou na primeira aula."}
                """).andExpect(status().isCreated());
        assumir(id, 3).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ATENDIMENTO"))
                .andExpect(jsonPath("$.tecnico.id").value(3));
        postJson("/chamados/" + id + "/interacoes", """
                {"autorId":3,"texto":"Estou verificando o ponto de acesso."}
                """).andExpect(status().isCreated());
        patchJson("/chamados/" + id + "/resolver", """
                {"tecnicoId":3,"solucao":"Ponto de acesso reiniciado e conexão testada."}
                """).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVIDO"))
                .andExpect(jsonPath("$.dataFechamento").isNotEmpty())
                .andExpect(jsonPath("$.solucao").isNotEmpty())
                .andExpect(jsonPath("$.interacoes.length()").value(2));
        mvc.perform(get("/chamados/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.interacoes[0].autor.id").value(1))
                .andExpect(jsonPath("$.interacoes[1].autor.id").value(3));
    }

    @Test
    void tecnicoNaoPodeAbrirChamado() throws Exception {
        enviarChamado(3, 1, null).andExpect(status().isConflict());
    }

    @Test
    void solicitanteNaoPodeAssumirChamado() throws Exception {
        assumir(abrir(1, 1, null), 1).andExpect(status().isConflict());
    }

    @Test
    void tecnicoDeOutroSetorNaoPodeAssumir() throws Exception {
        long id = abrir(1, 1, null);
        assumir(id, 4).andExpect(status().isConflict());
        mvc.perform(get("/chamados/{id}", id)).andExpect(jsonPath("$.status").value("ABERTO"));
    }

    @Test
    void naoPermiteAssumirNovamenteNemResolverSemAssumir() throws Exception {
        long id = abrir(1, 1, null);
        patchJson("/chamados/" + id + "/resolver", """
                {"tecnicoId":3,"solucao":"Ajustado."}
                """).andExpect(status().isConflict());
        assumir(id, 3).andExpect(status().isOk());
        assumir(id, 3).andExpect(status().isConflict());
    }

    @Test
    void somenteTecnicoAtribuidoPodeResolver() throws Exception {
        long id = abrir(1, 1, null);
        assumir(id, 3).andExpect(status().isOk());
        patchJson("/chamados/" + id + "/resolver", """
                {"tecnicoId":4,"solucao":"Ajustado."}
                """).andExpect(status().isConflict());
    }

    @Test
    void solucaoEmBrancoRetorna400() throws Exception {
        long id = abrir(1, 1, null);
        assumir(id, 3).andExpect(status().isOk());
        patchJson("/chamados/" + id + "/resolver", """
                {"tecnicoId":3,"solucao":"   "}
                """).andExpect(status().isBadRequest());
    }

    @Test
    void somenteProprioSolicitantePodeCancelarEnquantoAberto() throws Exception {
        long id = abrir(1, 1, null);
        patchJson("/chamados/" + id + "/cancelar", "{\"solicitanteId\":2}")
                .andExpect(status().isConflict());
        patchJson("/chamados/" + id + "/cancelar", "{\"solicitanteId\":1}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"))
                .andExpect(jsonPath("$.dataFechamento").isNotEmpty());
        long outro = abrir(1, 1, null);
        assumir(outro, 3).andExpect(status().isOk());
        patchJson("/chamados/" + outro + "/cancelar", "{\"solicitanteId\":1}")
                .andExpect(status().isConflict());
    }

    @ParameterizedTest
    @ValueSource(strings = {"RESOLVIDO", "CANCELADO"})
    void encerradosRecusamTodasAsAcoes(String estado) throws Exception {
        long id = abrir(1, 1, null);
        if (estado.equals("RESOLVIDO")) {
            assumir(id, 3).andExpect(status().isOk());
            patchJson("/chamados/" + id + "/resolver", "{\"tecnicoId\":3,\"solucao\":\"Conexão restabelecida.\"}")
                    .andExpect(status().isOk());
        } else {
            patchJson("/chamados/" + id + "/cancelar", "{\"solicitanteId\":1}")
                    .andExpect(status().isOk());
        }
        assumir(id, 3).andExpect(status().isConflict());
        patchJson("/chamados/" + id + "/resolver", "{\"tecnicoId\":3,\"solucao\":\"Ajustado.\"}")
                .andExpect(status().isConflict());
        patchJson("/chamados/" + id + "/cancelar", "{\"solicitanteId\":1}")
                .andExpect(status().isConflict());
        postJson("/chamados/" + id + "/interacoes", "{\"autorId\":1,\"texto\":\"Mais detalhes.\"}")
                .andExpect(status().isConflict());
    }

    @Test
    void interacaoExigeParticipacaoNoChamado() throws Exception {
        long id = abrir(1, 1, null);
        postJson("/chamados/" + id + "/interacoes", "{\"autorId\":2,\"texto\":\"Teste\"}")
                .andExpect(status().isConflict());
        postJson("/chamados/" + id + "/interacoes", "{\"autorId\":3,\"texto\":\"Teste\"}")
                .andExpect(status().isConflict());
        postJson("/chamados/" + id + "/interacoes", "{\"autorId\":1,\"texto\":\"Detalhes\"}")
                .andExpect(status().isCreated());
    }

    @Test
    void filtrosDeChamadosSaoCombinaveisEPaginados() throws Exception {
        long id = abrir(1, 1, "ALTA");
        abrir(2, 3, "BAIXA");
        assumir(id, 3).andExpect(status().isOk());
        String hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo")).toString();
        mvc.perform(get("/chamados").param("status", "EM_ATENDIMENTO")
                        .param("prioridade", "ALTA").param("setorId", "1").param("categoriaId", "1")
                        .param("solicitanteId", "1").param("tecnicoId", "3")
                        .param("dataInicio", hoje).param("dataFim", hoje)
                        .param("size", "1").param("sort", "dataAbertura,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(id))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
        mvc.perform(get("/chamados")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/chamados").param("dataFim", "2000-01-01"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/chamados").param("dataInicio", "2099-01-01"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void periodoIncluiTodoUltimoDiaNoFusoDoCampus() throws Exception {
        long primeiro = abrir(1, 1, null);
        long segundo = abrir(1, 1, null);
        jdbc.update("UPDATE chamados SET data_abertura = '2026-10-30T23:59:59-03:00' WHERE id = ?", primeiro);
        jdbc.update("UPDATE chamados SET data_abertura = '2026-10-31T00:00:00-03:00' WHERE id = ?", segundo);
        mvc.perform(get("/chamados").param("dataInicio", "2026-10-30").param("dataFim", "2026-10-30"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(primeiro));
    }

    @Test
    void listaChamadosConformePapelDoUsuario() throws Exception {
        long id = abrir(1, 1, null);
        abrir(2, 1, null);
        assumir(id, 3).andExpect(status().isOk());
        mvc.perform(get("/usuarios/1/chamados")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/usuarios/3/chamados").param("status", "EM_ATENDIMENTO"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(id));
        mvc.perform(get("/usuarios/3/chamados").param("status", "ABERTO"))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/usuarios/999/chamados")).andExpect(status().isNotFound());
    }

    @Test
    void cadastrosAceitamFiltrosSemDiferenciarMaiusculas() throws Exception {
        mvc.perform(get("/setores").param("nome", "tecnologia"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/categorias").param("nome", "REDE").param("setorId", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/usuarios").param("nome", "MARCOS").param("tipo", "TECNICO").param("setorId", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/setores/1/categorias"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/setores/999/categorias")).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/setores", "/categorias", "/usuarios", "/chamados", "/setores/1/categorias", "/usuarios/1/chamados"})
    void todaListaTemTamanhoPadraoELimite(String rota) throws Exception {
        mvc.perform(get(rota)).andExpect(status().isOk()).andExpect(jsonPath("$.size").value(10));
        mvc.perform(get(rota).param("size", "999"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.size").value(50));
    }

    @Test
    void paginaEOrdenacaoFuncionam() throws Exception {
        mvc.perform(get("/usuarios").param("size", "2").param("page", "1").param("sort", "id,desc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].id").value(2));
    }

    @ParameterizedTest
    @ValueSource(strings = {"setor.nome,asc", "setor.nome,desc", "setor.id,asc", "setor.id,desc"})
    void ordenarPeloSetorNaoEscondeSolicitantes(String ordenacao) throws Exception {
        mvc.perform(get("/usuarios").param("sort", ordenacao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.content.length()").value(4));
        mvc.perform(get("/usuarios").param("sort", ordenacao).param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.totalPages").value(4));
    }

    @ParameterizedTest
    @ValueSource(strings = {"%", "_", "\\"})
    void filtroDeNomeProcuraCaracteresLiterais(String nome) throws Exception {
        for (String rota : new String[]{"/setores", "/categorias", "/usuarios", "/setores/1/categorias"}) {
            mvc.perform(get(rota).param("nome", nome))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    @Test
    void filtrosDeNomeEncontramPercentualESublinhadoNoCadastro() throws Exception {
        postJson("/categorias", """
                {"nome":"Rede_100%","descricao":"Categoria de teste","setorId":1}
                """).andExpect(status().isCreated());
        postJson("/usuarios", """
                {"nome":"Usuário_100%","email":"literal@ifrn.edu.br","matricula":"20260001","tipo":"SOLICITANTE"}
                """).andExpect(status().isCreated());
        for (String rota : new String[]{"/categorias", "/usuarios"}) {
            mvc.perform(get(rota).param("nome", "_100%"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/setores", "/categorias", "/usuarios", "/chamados", "/setores/1/categorias", "/usuarios/1/chamados"})
    void paginaAcimaDoLimiteRetorna400(String rota) throws Exception {
        mvc.perform(get(rota).param("page", "2147483647").param("size", "50"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.caminho").value(rota));
    }

    @Test
    void errosDeProtocoloTambemDevolvemJsonPadronizado() throws Exception {
        mvc.perform(get("/setores").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(406))
                .andExpect(jsonPath("$.mensagem").isNotEmpty())
                .andExpect(jsonPath("$.caminho").value("/setores"));
        mvc.perform(delete("/usuarios/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
        mvc.perform(post("/setores").contentType(MediaType.TEXT_PLAIN).content("texto"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
        postJson("/setores", "{json inválido}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void protegeEmailESiglaContraDuplicacao() throws Exception {
        postJson("/usuarios", usuario("ANA.BEATRIZ@ESCOLAR.IFRN.EDU.BR", "SOLICITANTE", "null"))
                .andExpect(status().isConflict());
        postJson("/setores", "{\"nome\":\"Outro setor\",\"sigla\":\"cotic\"}")
                .andExpect(status().isConflict());
        mvc.perform(put("/usuarios/2").contentType(MediaType.APPLICATION_JSON)
                        .content(usuario("ana.beatriz@escolar.ifrn.edu.br", "SOLICITANTE", "null")))
                .andExpect(status().isConflict());
    }

    @Test
    void tecnicoDeveTerSetorESolicitanteNaoPodeTerSetor() throws Exception {
        postJson("/usuarios", usuario("teste@ifrn.edu.br", "TECNICO", "null"))
                .andExpect(status().isConflict());
        postJson("/usuarios", usuario("teste@ifrn.edu.br", "SOLICITANTE", "1"))
                .andExpect(status().isConflict());
    }

    @Test
    void naoExcluiSetorOuCategoriaComChamadosMesmoEncerrados() throws Exception {
        long id = abrir(1, 1, null);
        patchJson("/chamados/" + id + "/cancelar", "{\"solicitanteId\":1}")
                .andExpect(status().isOk());
        mvc.perform(delete("/categorias/1")).andExpect(status().isConflict());
        mvc.perform(delete("/setores/1")).andExpect(status().isConflict());
    }

    @Test
    void atualizacaoNaoPodeDesfazerVinculosDeAtendimento() throws Exception {
        long id = abrir(1, 1, null);
        assumir(id, 3).andExpect(status().isOk());
        mvc.perform(put("/categorias/1").contentType(MediaType.APPLICATION_JSON).content("""
                {"nome":"Rede","descricao":"Rede do campus","setorId":2}
                """)).andExpect(status().isConflict());
        mvc.perform(put("/usuarios/3").contentType(MediaType.APPLICATION_JSON)
                        .content(usuario("marcos.silva@ifrn.edu.br", "TECNICO", "2")))
                .andExpect(status().isConflict());
        mvc.perform(put("/usuarios/1").contentType(MediaType.APPLICATION_JSON)
                        .content(usuario("ana.beatriz@escolar.ifrn.edu.br", "TECNICO", "1")))
                .andExpect(status().isConflict());
    }

    @Test
    void crudDosCadastros() throws Exception {
        long setor = id(postJson("/setores", "{\"nome\":\"Biblioteca\",\"sigla\":\"BIB\"}")
                .andExpect(status().isCreated()));
        mvc.perform(put("/setores/{id}", setor).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Biblioteca do campus\",\"sigla\":\"BIB\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/setores/{id}", setor)).andExpect(status().isOk());
        long categoria = id(postJson("/categorias", """
                {"nome":"Acervo","descricao":"Atendimento ao acervo","setorId":%d}
                """.formatted(setor)).andExpect(status().isCreated()));
        mvc.perform(put("/categorias/{id}", categoria).contentType(MediaType.APPLICATION_JSON).content("""
                {"nome":"Consulta ao acervo","descricao":"Suporte ao acervo","setorId":%d}
                """.formatted(setor))).andExpect(status().isOk());
        mvc.perform(get("/categorias/{id}", categoria)).andExpect(status().isOk());
        mvc.perform(delete("/setores/{id}", setor)).andExpect(status().isConflict());
        mvc.perform(delete("/categorias/{id}", categoria)).andExpect(status().isNoContent());
        mvc.perform(delete("/setores/{id}", setor)).andExpect(status().isNoContent());
        mvc.perform(get("/setores/{id}", setor)).andExpect(status().isNotFound());

        long usuario = id(postJson("/usuarios", usuario("novo@ifrn.edu.br", "SOLICITANTE", "null"))
                .andExpect(status().isCreated()));
        mvc.perform(put("/usuarios/{id}", usuario).contentType(MediaType.APPLICATION_JSON)
                        .content(usuario("alterado@ifrn.edu.br", "SOLICITANTE", "null")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("alterado@ifrn.edu.br"));
        mvc.perform(get("/usuarios/{id}", usuario)).andExpect(status().isOk());
    }

    @Test
    void errosTemFormatoPadronizado() throws Exception {
        mvc.perform(get("/chamados/999")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensagem").isNotEmpty())
                .andExpect(jsonPath("$.caminho").value("/chamados/999"));
        postJson("/usuarios", usuario("email-invalido", "SOLICITANTE", "null"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        postJson("/setores", "{\"nome\":\"  \",\"sigla\":\"X\"}")
                .andExpect(status().isBadRequest());
        mvc.perform(get("/chamados").param("status", "INEXISTENTE"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/chamados").param("sort", "campoInexistente,asc"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/chamados").param("dataInicio", "2026-10-31").param("dataFim", "2026-10-01"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/rota-inexistente")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.caminho").value("/rota-inexistente"));
        postJson("/chamados", "{\"status\":\"RESOLVIDO\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void referenciasInexistentesRetornam404() throws Exception {
        enviarChamado(999, 1, null).andExpect(status().isNotFound());
        enviarChamado(1, 999, null).andExpect(status().isNotFound());
        assumir(abrir(1, 1, null), 999).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @CsvSource({
            "status,ABERTO,2",
            "status,EM_ATENDIMENTO,1",
            "status,RESOLVIDO,1",
            "prioridade,ALTA,2",
            "prioridade,MEDIA,1",
            "prioridade,BAIXA,1",
            "setorId,1,2",
            "setorId,2,2",
            "categoriaId,2,1",
            "categoriaId,4,1",
            "solicitanteId,1,2",
            "solicitanteId,2,2",
            "tecnicoId,3,1",
            "tecnicoId,4,1"
    })
    void cadaFiltroDeChamadoFuncionaSeparadamente(String campo, String valor, int quantidade) throws Exception {
        long primeiro = abrir(1, 1, "ALTA");
        abrir(2, 2, "BAIXA");
        long terceiro = abrir(2, 3, "MEDIA");
        abrir(1, 4, "ALTA");
        assumir(primeiro, 3).andExpect(status().isOk());
        assumir(terceiro, 4).andExpect(status().isOk());
        patchJson("/chamados/" + terceiro + "/resolver", "{\"tecnicoId\":4,\"solucao\":\"Iluminação corrigida.\"}")
                .andExpect(status().isOk());
        mvc.perform(get("/chamados").param(campo, valor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(quantidade));
    }

    @Test
    void filtrosDeCadastrosSaoIndependentesECombinados() throws Exception {
        mvc.perform(get("/usuarios").param("tipo", "SOLICITANTE"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/usuarios").param("setorId", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(4))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/usuarios").param("nome", "marcos").param("setorId", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/categorias").param("setorId", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/categorias").param("nome", "rede").param("setorId", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void paginacaoLimitaQuantidadeRealDeRegistros() throws Exception {
        jdbc.update("""
                INSERT INTO chamados (titulo, descricao, local, prioridade, status, data_abertura, solicitante_id, categoria_id)
                SELECT 'Chamado ' || n, 'Problema de teste', 'Sala 1', 'MEDIA', 'ABERTO', CURRENT_TIMESTAMP, 1, 1
                FROM generate_series(1, 61) AS n
                """);
        mvc.perform(get("/chamados").param("size", "100").param("sort", "id,asc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.size").value(50))
                .andExpect(jsonPath("$.content.length()").value(50))
                .andExpect(jsonPath("$.totalElements").value(61))
                .andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(get("/chamados").param("size", "50").param("page", "1").param("sort", "id,asc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(11))
                .andExpect(jsonPath("$.content[0].id").value(51));
        mvc.perform(get("/chamados").param("size", "50").param("page", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(61));
    }

    @Test
    void ordenarPeloTecnicoNaoOcultaChamadosSemTecnico() throws Exception {
        long id = abrir(1, 1, null);
        abrir(2, 1, null);
        assumir(id, 3).andExpect(status().isOk());
        mvc.perform(get("/chamados").param("sort", "tecnico.id,asc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(2));
        mvc.perform(get("/usuarios/2/chamados").param("sort", "tecnico.id,asc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void idsObrigatoriosCamposLongosEEnumsInvalidosRetornam400() throws Exception {
        postJson("/chamados", """
                {"titulo":"Teste","descricao":"Descrição","local":"Sala 1","categoriaId":1}
                """).andExpect(status().isBadRequest());
        postJson("/chamados", """
                {"titulo":"Teste","descricao":"Descrição","local":"Sala 1","solicitanteId":0,"categoriaId":1}
                """).andExpect(status().isBadRequest());
        postJson("/chamados", """
                {"titulo":"Teste","descricao":"Descrição","local":"Sala 1","solicitanteId":1,"categoriaId":1,"prioridade":"URGENTE"}
                """).andExpect(status().isBadRequest());
        postJson("/setores", "{\"nome\":\"" + "a".repeat(101) + "\",\"sigla\":\"TESTE\"}")
                .andExpect(status().isBadRequest());
        long id = abrir(1, 1, null);
        postJson("/chamados/" + id + "/interacoes", "{\"autorId\":1,\"texto\":\"  \"}")
                .andExpect(status().isBadRequest());
        patchJson("/chamados/" + id + "/assumir", "{}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void clienteNaoPodeDefinirCamposControladosPeloSistema() throws Exception {
        for (String campo : new String[]{"status", "dataAbertura", "dataFechamento", "tecnicoId", "solucao"}) {
            postJson("/chamados", """
                    {"titulo":"Teste","descricao":"Descrição","local":"Sala 1","solicitanteId":1,"categoriaId":1,"%s":"valor"}
                    """.formatted(campo)).andExpect(status().isBadRequest());
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM chamados", Long.class)).isZero();
    }

    @Test
    void apenasUmaRequisicaoConcorrenteConsegueAssumir() throws Exception {
        long id = abrir(1, 1, null);
        CountDownLatch inicio = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var primeira = executor.submit(() -> {
                inicio.await();
                return assumir(id, 3).andReturn().getResponse().getStatus();
            });
            var segunda = executor.submit(() -> {
                inicio.await();
                return assumir(id, 3).andReturn().getResponse().getStatus();
            });
            inicio.countDown();
            assertThat(new Integer[]{primeira.get(15, TimeUnit.SECONDS), segunda.get(15, TimeUnit.SECONDS)})
                    .containsExactlyInAnyOrder(200, 409);
        }
    }

    private ResultActions enviarChamado(long solicitante, long categoria, String prioridade) throws Exception {
        return postJson("/chamados", """
                {"titulo":"Wi-Fi sem conexão","descricao":"A rede caiu durante a aula.",
                 "local":"Laboratório 2","solicitanteId":%d,"categoriaId":%d,"prioridade":%s}
                """.formatted(solicitante, categoria, prioridade == null ? "null" : "\"" + prioridade + "\""));
    }

    private long abrir(long solicitante, long categoria, String prioridade) throws Exception {
        return id(enviarChamado(solicitante, categoria, prioridade).andExpect(status().isCreated()));
    }

    private long id(ResultActions resultado) throws Exception {
        return JsonPath.parse(resultado.andReturn().getResponse().getContentAsString()).read("$.id", Long.class);
    }

    private ResultActions assumir(long id, long tecnico) throws Exception {
        return patchJson("/chamados/" + id + "/assumir", "{\"tecnicoId\":" + tecnico + "}");
    }

    private ResultActions postJson(String rota, String corpo) throws Exception {
        return mvc.perform(post(rota).contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private ResultActions patchJson(String rota, String corpo) throws Exception {
        return mvc.perform(patch(rota).contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private String usuario(String email, String tipo, String setorId) {
        return """
                {"nome":"Usuário de teste","email":"%s","matricula":"20269999","tipo":"%s","setorId":%s}
                """.formatted(email, tipo, setorId);
    }
}
