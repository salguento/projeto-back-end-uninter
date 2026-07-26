package com.raizesdonordeste.backendapi.config;

import com.raizesdonordeste.backendapi.model.*;
import com.raizesdonordeste.backendapi.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import com.raizesdonordeste.backendapi.service.DocumentoLegalService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataSeed implements CommandLineRunner {

    private static final String SENHA_CLIENTE = "cliente123";
    private static final String SENHA_COZINHA = "cozinha123";
    private static final String SENHA_ATENDENTE = "atendente123";
    private static final String SENHA_GERENTE = "gerente123";
    private static final String SENHA_ADMIN = "admin123";

    private static final String EMAIL_CLIENTE = "cliente@raizes.com";
    private static final String EMAIL_COZINHA = "cozinha@raizes.com";
    private static final String EMAIL_ATENDENTE = "atendente@raizes.com";
    private static final String EMAIL_GERENTE = "gerente@raizes.com";
    private static final String EMAIL_ADMIN = "admin@raizes.com";

    private static final String UNIDADE_CENTRO = "Unidade Centro";
    private static final String UNIDADE_ZONA_SUL = "Unidade Zona Sul";

    private static final String CAMPANHA_TACA_TAPIOCA = "Terca da Tapioca";
    private static final String CUPOM_NORDESTE10 = "NORDESTE10";
    private static final String CUPOM_PROMO5 = "PROMO5";

    private final UsuarioRepository usuarioRepository;
    private final UnidadeRepository unidadeRepository;
    private final ProdutoRepository produtoRepository;
    private final EstoqueRepository estoqueRepository;
    private final UsuarioUnidadeRepository usuarioUnidadeRepository;
    private final CampanhaRepository campanhaRepository;
    private final CupomRepository cupomRepository;
    private final PasswordEncoder passwordEncoder;
    private final DocumentoLegalService documentoLegalService;

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            log.info("Seed ja executado. Pulando inicializacao.");
            return;
        }

        log.info("Executando DataSeed...");

        Unidade unidade1 = criarUnidade(UNIDADE_CENTRO, "Rua A, 123", "Sao Paulo", "SP", "01000-000", "1133334444");
        Unidade unidade2 = criarUnidade(UNIDADE_ZONA_SUL, "Av. B, 456", "Sao Paulo", "SP", "04000-000", "1144445555");

        Usuario cliente = criarUsuario("Cliente Teste", EMAIL_CLIENTE, SENHA_CLIENTE, Perfil.CLIENTE, 100, "11999999999", "12345678901");
        Usuario cozinha = criarUsuario("Cozinha Teste", EMAIL_COZINHA, SENHA_COZINHA, Perfil.COZINHA, 0, "11888888888", "22345678901");
        Usuario atendente = criarUsuario("Atendente Teste", EMAIL_ATENDENTE, SENHA_ATENDENTE, Perfil.ATENDENTE, 0, "11777777777", "32345678901");
        Usuario gerente = criarUsuario("Gerente Teste", EMAIL_GERENTE, SENHA_GERENTE, Perfil.GERENTE, 0, "11666666666", "42345678901");
        Usuario administrador = criarUsuario("Administrador Sistema", EMAIL_ADMIN, SENHA_ADMIN, Perfil.ADMIN, 0, "11555555555", "52345678901");

        registrarAceitesIniciais(cliente, cozinha, atendente, gerente, administrador);
        
        vincularUsuarioUnidade(cozinha, unidade1);
        vincularUsuarioUnidade(atendente, unidade1);
        vincularUsuarioUnidade(atendente, unidade2);
        vincularUsuarioUnidade(gerente, unidade1);
        vincularUsuarioUnidade(gerente, unidade2);

        Produto produto1 = criarProduto("Tapioca Rendada com Queijo Coalho", "Tapioca artesanal com queijo coalho derretido", "14.50");
        Produto produto2 = criarProduto("Cuscuz Completo com Carne de Sol", "Cuscuz nordestino com carne de sol, manteiga de garrafa e queijo", "19.90");
        Produto produto3 = criarProduto("Suco de Cajá", "Suco natural de caju, 500ml", "8.00");
        Produto produto4 = criarProduto("Carne de Sol Acebolada", "Carne de sol macia com cebolas douradas, acompanha arroz e farofa", "25.00");

        criarEstoque(unidade1, produto1, 50);
        criarEstoque(unidade1, produto2, 30);
        criarEstoque(unidade1, produto3, 100);
        criarEstoque(unidade1, produto4, 20);

        criarEstoque(unidade2, produto1, 40);
        criarEstoque(unidade2, produto2, 25);
        criarEstoque(unidade2, produto3, 80);
        criarEstoque(unidade2, produto4, 15);

        criarCampanha(unidade1, produto1);
        criarCupons(unidade1);

        log.info("Seed executado | Admin e funcionarios alocados");
    }

    private void registrarAceitesIniciais(Usuario... usuarios) {
        for (Usuario usuario : usuarios) {
            documentoLegalService.registrarAceiteSeed(usuario);
        }
    }

    private Unidade criarUnidade(String nome, String endereco, String cidade, String estado, String cep, String telefone) {
        Unidade unidade = new Unidade();
        unidade.setNome(nome);
        unidade.setEndereco(endereco);
        unidade.setCidade(cidade);
        unidade.setEstado(estado);
        unidade.setCep(cep);
        unidade.setTelefone(telefone);
        return unidadeRepository.save(unidade);
    }

    private Usuario criarUsuario(String nome, String email, String senha, Perfil perfil, int pontos, String telefone, String cpf) {
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode(senha));
        usuario.setPerfil(perfil);
        usuario.setPontos(pontos);
        usuario.setTelefone(telefone);
        usuario.setCpf(cpf);
        return usuarioRepository.save(usuario);
    }

    private void vincularUsuarioUnidade(Usuario usuario, Unidade unidade) {
        UsuarioUnidade vinculo = new UsuarioUnidade();
        vinculo.setUsuario(usuario);
        vinculo.setUnidade(unidade);
        usuarioUnidadeRepository.save(vinculo);
    }

    private Produto criarProduto(String nome, String descricao, String preco) {
        Produto produto = new Produto();
        produto.setNome(nome);
        produto.setDescricao(descricao);
        produto.setPrecoVigente(new BigDecimal(preco));
        produto.setAtivo(true);
        return produtoRepository.save(produto);
    }

    private void criarEstoque(Unidade unidade, Produto produto, int quantidade) {
        Estoque estoque = new Estoque();
        estoque.setUnidade(unidade);
        estoque.setProduto(produto);
        estoque.setQuantidade(quantidade);
        estoqueRepository.save(estoque);
    }

    private void criarCampanha(Unidade unidade, Produto produto) {
        Campanha campanha = new Campanha();
        campanha.setNome(CAMPANHA_TACA_TAPIOCA);
        campanha.setProduto(produto);
        campanha.setUnidade(unidade);
        campanha.setPrecoPromocional(new BigDecimal("9.90"));
        campanha.setDataInicio(LocalDateTime.now().minusDays(1));
        campanha.setDataFim(LocalDateTime.now().plusDays(30));
        campanha.setAtiva(true);
        campanhaRepository.save(campanha);
        log.info("Campanha criada | Nome: {} | Preco: 14.50 -> 9.90", CAMPANHA_TACA_TAPIOCA);
    }

    private void criarCupons(Unidade unidade) {
        criarCupomPercentual(CUPOM_NORDESTE10, "10", "30", unidade, 100);
        criarCupomNominal(CUPOM_PROMO5, "5", "20");
        log.info("Cupons criados | {} (10%), {} (R$ 5 fixo)", CUPOM_NORDESTE10, CUPOM_PROMO5);
    }

    private void criarCupomPercentual(String codigo, String valor, String valorMinimo, Unidade unidade, int usoMaximo) {
        Cupom cupom = new Cupom();
        cupom.setCodigo(codigo);
        cupom.setTipoDesconto(TipoDesconto.PERCENTUAL);
        cupom.setValor(new BigDecimal(valor));
        cupom.setValorMinimoPedido(new BigDecimal(valorMinimo));
        cupom.setUnidade(unidade);
        cupom.setDataInicio(LocalDateTime.now().minusDays(1));
        cupom.setDataFim(LocalDateTime.now().plusDays(60));
        cupom.setAtivo(true);
        cupom.setUsoMaximo(usoMaximo);
        cupomRepository.save(cupom);
    }

    private void criarCupomNominal(String codigo, String valor, String valorMinimo) {
        Cupom cupom = new Cupom();
        cupom.setCodigo(codigo);
        cupom.setTipoDesconto(TipoDesconto.NOMINAL);
        cupom.setValor(new BigDecimal(valor));
        cupom.setValorMinimoPedido(new BigDecimal(valorMinimo));
        cupom.setDataInicio(LocalDateTime.now().minusDays(1));
        cupom.setDataFim(LocalDateTime.now().plusDays(60));
        cupom.setAtivo(true);
        cupomRepository.save(cupom);
    }
}
