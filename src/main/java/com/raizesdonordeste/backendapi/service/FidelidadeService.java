package com.raizesdonordeste.backendapi.service;

import com.raizesdonordeste.backendapi.dto.ConsentimentoResponseDTO;
import com.raizesdonordeste.backendapi.dto.SaldoPontosResponseDTO;
import com.raizesdonordeste.backendapi.exception.RecursoNaoEncontradoException;
import com.raizesdonordeste.backendapi.exception.RegraNegocioException;
import com.raizesdonordeste.backendapi.model.ConsentimentoLgpd;
import com.raizesdonordeste.backendapi.model.FinalidadeConsentimento;
import com.raizesdonordeste.backendapi.model.Usuario;
import com.raizesdonordeste.backendapi.repository.ConsentimentoLgpdRepository;
import com.raizesdonordeste.backendapi.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FidelidadeService {

    private static final String USUARIO_NAO_ENCONTRADO = "USUARIO_NAO_ENCONTRADO";
    private static final String EMAIL_INVALIDO = "EMAIL_INVALIDO";
    private static final String PONTOS_INVALIDOS = "PONTOS_INVALIDOS";
    private static final String CONSENTIMENTO_NECESSARIO = "CONSENTIMENTO_FIDELIDADE_NECESSARIO";
    private static final FinalidadeConsentimento FINALIDADE_FIDELIDADE = FinalidadeConsentimento.FIDELIDADE;
    private static final String ORIGEM_AUTOSSERVICO = "API_AUTOSSERVICO";
    private static final String DESCRICAO_FINALIDADE =
            "Autorizar o uso do saldo de pontos para obter desconto em pedidos.";

    private static final BigDecimal VALOR_POR_PONTO = new BigDecimal("0.10");

    private final UsuarioRepository usuarioRepository;
    private final ConsentimentoLgpdRepository consentimentoLgpdRepository;

    @Value("${app.lgpd.termo-fidelidade-versao:1.0}")
    private String versaoTermoFidelidade;

    @Transactional(readOnly = true)
    public SaldoPontosResponseDTO consultarSaldo(String emailUsuario) {
        validarEmailUsuario(emailUsuario);

        Usuario usuario = buscarUsuario(emailUsuario);
        validarSaldoRegistrado(usuario.getPontos());

        SaldoPontosResponseDTO response = new SaldoPontosResponseDTO();
        response.setUsuarioId(usuario.getId());
        response.setEmail(usuario.getEmail());
        response.setSaldoPontos(usuario.getPontos());

        BigDecimal valorEstimado = calcularValorEstimado(usuario.getPontos());
        response.setValorEstimadoEmDesconto(valorEstimado.doubleValue());

        log.info("Saldo consultado | UsuarioId: {} | Pontos: {} | Valor estimado: {}",
                usuario.getId(), usuario.getPontos(), valorEstimado);

        return response;
    }

    @Transactional(readOnly = true)
    public ConsentimentoResponseDTO consultarConsentimento(String emailUsuario) {
        validarEmailUsuario(emailUsuario);
        Usuario usuario = buscarUsuario(emailUsuario);
        Optional<ConsentimentoLgpd> ultimoEvento = buscarUltimoConsentimento(usuario.getId());
        return criarRespostaConsentimento(ultimoEvento.orElse(null));
    }

    @Transactional
    public ConsentimentoResponseDTO concederConsentimento(String emailUsuario) {
        validarEmailUsuario(emailUsuario);
        Usuario usuario = buscarUsuario(emailUsuario);
        ConsentimentoLgpd evento = registrarEvento(usuario, true);
        log.info("Consentimento LGPD registrado | UsuarioId: {} | Finalidade: {} | Versao: {}",
                usuario.getId(), FINALIDADE_FIDELIDADE, versaoTermoFidelidade);
        return criarRespostaConsentimento(evento);
    }

    @Transactional
    public ConsentimentoResponseDTO revogarConsentimento(String emailUsuario) {
        validarEmailUsuario(emailUsuario);
        Usuario usuario = buscarUsuario(emailUsuario);
        ConsentimentoLgpd evento = registrarEvento(usuario, false);
        log.info("Consentimento LGPD revogado | UsuarioId: {} | Finalidade: {} | Versao: {}",
                usuario.getId(), FINALIDADE_FIDELIDADE, versaoTermoFidelidade);
        return criarRespostaConsentimento(evento);
    }

    @Transactional(readOnly = true)
    public void validarConsentimentoParaResgate(Usuario usuario) {
        Objects.requireNonNull(usuario, "Usuario nao pode ser nulo");
        if (usuario.getId() == null || !possuiConsentimentoAtivo(usuario.getId())) {
            throw new RegraNegocioException(CONSENTIMENTO_NECESSARIO,
                    "E necessario aceitar a versao vigente do termo de fidelidade antes de resgatar pontos.");
        }
    }

    private boolean possuiConsentimentoAtivo(Long usuarioId) {
        return buscarUltimoConsentimento(usuarioId)
                .filter(evento -> Boolean.TRUE.equals(evento.getConcedido()))
                .filter(evento -> versaoTermoFidelidade.equals(evento.getVersaoTermo()))
                .isPresent();
    }

    private Optional<ConsentimentoLgpd> buscarUltimoConsentimento(Long usuarioId) {
        return consentimentoLgpdRepository
                .findTopByUsuarioIdAndFinalidadeOrderByRegistradoEmDescIdDesc(usuarioId, FINALIDADE_FIDELIDADE);
    }

    private ConsentimentoLgpd registrarEvento(Usuario usuario, boolean concedido) {
        ConsentimentoLgpd evento = new ConsentimentoLgpd();
        evento.setUsuario(usuario);
        evento.setFinalidade(FINALIDADE_FIDELIDADE);
        evento.setVersaoTermo(versaoTermoFidelidade);
        evento.setConcedido(concedido);
        evento.setRegistradoEm(LocalDateTime.now());
        evento.setOrigem(ORIGEM_AUTOSSERVICO);
        return consentimentoLgpdRepository.save(evento);
    }

    private ConsentimentoResponseDTO criarRespostaConsentimento(ConsentimentoLgpd evento) {
        boolean ativo = evento != null
                && Boolean.TRUE.equals(evento.getConcedido())
                && versaoTermoFidelidade.equals(evento.getVersaoTermo());
        return new ConsentimentoResponseDTO(
                FINALIDADE_FIDELIDADE,
                DESCRICAO_FINALIDADE,
                ativo,
                versaoTermoFidelidade,
                evento != null ? evento.getVersaoTermo() : null,
                evento != null ? evento.getRegistradoEm() : null);
    }

    private void validarEmailUsuario(String emailUsuario) {
        Objects.requireNonNull(emailUsuario, "Email do usuario nao pode ser nulo");

        if (emailUsuario.trim().isEmpty()) {
            throw new RegraNegocioException(EMAIL_INVALIDO,
                    "O email do usuario e obrigatorio.");
        }

        if (!emailUsuario.contains("@") || !emailUsuario.contains(".")) {
            throw new RegraNegocioException(EMAIL_INVALIDO,
                    "O formato do email e invalido.");
        }
    }

    private Usuario buscarUsuario(String emailUsuario) {
        return usuarioRepository.findByEmail(emailUsuario)
                .orElseThrow(() -> new RecursoNaoEncontradoException(USUARIO_NAO_ENCONTRADO,
                        "Usuario nao encontrado."));
    }

    private void validarSaldoRegistrado(Integer pontos) {
        if (pontos == null) {
            throw new RegraNegocioException(PONTOS_INVALIDOS,
                    "O saldo de pontos do usuario e invalido.");
        }
    }

    private BigDecimal calcularValorEstimado(Integer pontos) {
        int pontosDisponiveis = Math.max(pontos, 0);
        return BigDecimal.valueOf(pontosDisponiveis).multiply(VALOR_POR_PONTO);
    }
}
