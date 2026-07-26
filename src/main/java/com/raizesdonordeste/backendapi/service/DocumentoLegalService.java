package com.raizesdonordeste.backendapi.service;

import com.raizesdonordeste.backendapi.dto.AceiteDocumentoResponseDTO;
import com.raizesdonordeste.backendapi.dto.AceiteTermosRequestDTO;
import com.raizesdonordeste.backendapi.dto.CadastroUsuarioDTO;
import com.raizesdonordeste.backendapi.dto.DocumentoLegalResponseDTO;
import com.raizesdonordeste.backendapi.exception.RecursoNaoEncontradoException;
import com.raizesdonordeste.backendapi.exception.RegraNegocioException;
import com.raizesdonordeste.backendapi.model.AceiteDocumento;
import com.raizesdonordeste.backendapi.model.TipoDocumentoLegal;
import com.raizesdonordeste.backendapi.model.Usuario;
import com.raizesdonordeste.backendapi.repository.AceiteDocumentoRepository;
import com.raizesdonordeste.backendapi.repository.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DocumentoLegalService {

    public static final String TERMOS_USO_NAO_ACEITOS = "TERMOS_USO_NAO_ACEITOS";
    public static final String MENSAGEM_TERMOS_NAO_ACEITOS =
            "E necessario aceitar a versao vigente dos termos de uso para continuar.";

    private static final String DOCUMENTO_DESATUALIZADO = "DOCUMENTO_LEGAL_DESATUALIZADO";
    private static final String USUARIO_NAO_ENCONTRADO = "USUARIO_NAO_ENCONTRADO";
    private static final String ORIGEM_CADASTRO = "CADASTRO_PUBLICO";
    private static final String ORIGEM_AUTOSSERVICO = "API_AUTOSSERVICO";
    private static final String ORIGEM_SEED = "DATA_SEED";

    private final AceiteDocumentoRepository aceiteDocumentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ResourceLoader resourceLoader;

    @Value("${app.documentos.termos-uso.versao:1.0}")
    private String versaoTermosUso;

    @Value("${app.documentos.termos-uso.recurso:classpath:legal/termos-uso-v1.0.md}")
    private String recursoTermosUso;

    @Value("${app.documentos.aviso-privacidade.versao:1.0}")
    private String versaoAvisoPrivacidade;

    @Value("${app.documentos.aviso-privacidade.recurso:classpath:legal/aviso-privacidade-v1.0.md}")
    private String recursoAvisoPrivacidade;

    private DocumentoVigente termosUso;
    private DocumentoVigente avisoPrivacidade;

    @PostConstruct
    void carregarDocumentos() {
        termosUso = carregar(TipoDocumentoLegal.TERMOS_USO, versaoTermosUso, recursoTermosUso);
        avisoPrivacidade = carregar(TipoDocumentoLegal.AVISO_PRIVACIDADE,
                versaoAvisoPrivacidade, recursoAvisoPrivacidade);
    }

    public DocumentoLegalResponseDTO obterTermosUso() {
        return termosUso.toResponse();
    }

    public DocumentoLegalResponseDTO obterAvisoPrivacidade() {
        return avisoPrivacidade.toResponse();
    }

    public void validarDadosAceiteCadastro(CadastroUsuarioDTO dto) {
        Objects.requireNonNull(dto, "DTO de cadastro nao pode ser nulo");
        validarAceiteExplicito(dto.getAceiteTermosUso());
        validarVersaoEHash(dto.getVersaoTermosUso(), dto.getHashTermosUso());
    }

    @Transactional
    public AceiteDocumentoResponseDTO registrarAceiteCadastro(Usuario usuario, CadastroUsuarioDTO dto) {
        validarDadosAceiteCadastro(dto);
        return registrarAceite(usuario, dto.getVersaoTermosUso(), dto.getHashTermosUso(), ORIGEM_CADASTRO);
    }

    @Transactional
    public AceiteDocumentoResponseDTO registrarAceiteAtual(String email, AceiteTermosRequestDTO dto) {
        Objects.requireNonNull(dto, "DTO de aceite nao pode ser nulo");
        validarAceiteExplicito(dto.getAceito());
        validarVersaoEHash(dto.getVersao(), dto.getHashSha256());
        Usuario usuario = buscarUsuario(email);
        return registrarAceite(usuario, dto.getVersao(), dto.getHashSha256(), ORIGEM_AUTOSSERVICO);
    }

    @Transactional
    public AceiteDocumentoResponseDTO registrarAceiteSeed(Usuario usuario) {
        return registrarAceite(usuario, termosUso.versao(), termosUso.hashSha256(), ORIGEM_SEED);
    }

    @Transactional(readOnly = true)
    public AceiteDocumentoResponseDTO consultarAceiteAtual(String email) {
        Usuario usuario = buscarUsuario(email);
        return aceiteDocumentoRepository.findByUsuarioIdAndTipoDocumentoAndVersao(
                        usuario.getId(), TipoDocumentoLegal.TERMOS_USO, termosUso.versao())
                .map(this::toResponse)
                .orElse(new AceiteDocumentoResponseDTO(TipoDocumentoLegal.TERMOS_USO,
                        termosUso.versao(), termosUso.hashSha256(), null, false));
    }

    @Transactional(readOnly = true)
    public boolean possuiAceiteVigente(String email) {
        Usuario usuario = buscarUsuario(email);
        return aceiteDocumentoRepository.existsByUsuarioIdAndTipoDocumentoAndVersaoAndHashDocumento(
                usuario.getId(), TipoDocumentoLegal.TERMOS_USO, termosUso.versao(), termosUso.hashSha256());
    }

    private AceiteDocumentoResponseDTO registrarAceite(Usuario usuario, String versao, String hash, String origem) {
        Objects.requireNonNull(usuario, "Usuario nao pode ser nulo");
        Objects.requireNonNull(usuario.getId(), "Usuario deve estar persistido antes do aceite");

        return aceiteDocumentoRepository.findByUsuarioIdAndTipoDocumentoAndVersao(
                        usuario.getId(), TipoDocumentoLegal.TERMOS_USO, versao)
                .map(this::validarAceiteExistente)
                .orElseGet(() -> salvarAceite(usuario, versao, hash, origem));
    }

    private AceiteDocumentoResponseDTO validarAceiteExistente(AceiteDocumento aceite) {
        if (!termosUso.hashSha256().equals(aceite.getHashDocumento())) {
            throw new RegraNegocioException(DOCUMENTO_DESATUALIZADO,
                    "O conteúdo dos termos foi alterado sem mudança de versão.");
        }
        return toResponse(aceite);
    }

    private AceiteDocumentoResponseDTO salvarAceite(Usuario usuario, String versao, String hash, String origem) {
        AceiteDocumento aceite = new AceiteDocumento();
        aceite.setUsuario(usuario);
        aceite.setTipoDocumento(TipoDocumentoLegal.TERMOS_USO);
        aceite.setVersao(versao);
        aceite.setHashDocumento(hash);
        aceite.setAceitoEm(LocalDateTime.now());
        aceite.setOrigem(origem);
        return toResponse(aceiteDocumentoRepository.save(aceite));
    }

    private AceiteDocumentoResponseDTO toResponse(AceiteDocumento aceite) {
        boolean vigente = termosUso.versao().equals(aceite.getVersao())
                && termosUso.hashSha256().equals(aceite.getHashDocumento());
        return new AceiteDocumentoResponseDTO(aceite.getTipoDocumento(), aceite.getVersao(),
                aceite.getHashDocumento(), aceite.getAceitoEm(), vigente);
    }

    private void validarVersaoEHash(String versao, String hash) {
        if (!termosUso.versao().equals(versao) || !termosUso.hashSha256().equalsIgnoreCase(hash)) {
            throw new RegraNegocioException(DOCUMENTO_DESATUALIZADO,
                    "A versao ou o hash informado nao corresponde aos termos de uso vigentes.");
        }
    }

    private void validarAceiteExplicito(Boolean aceito) {
        if (!Boolean.TRUE.equals(aceito)) {
            throw new RegraNegocioException(TERMOS_USO_NAO_ACEITOS, MENSAGEM_TERMOS_NAO_ACEITOS);
        }
    }

    private Usuario buscarUsuario(String email) {
        Objects.requireNonNull(email, "Email do usuario nao pode ser nulo");
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException(USUARIO_NAO_ENCONTRADO,
                        "Usuario autenticado nao encontrado."));
    }

    private DocumentoVigente carregar(TipoDocumentoLegal tipo, String versao, String localizacao) {
        if (versao == null || versao.isBlank()) {
            throw new IllegalStateException("A versao do documento legal e obrigatoria.");
        }
        Resource resource = resourceLoader.getResource(localizacao);
        try {
            String conteudo = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            if (conteudo.isBlank()) {
                throw new IllegalStateException("O documento legal nao pode estar vazio: " + localizacao);
            }
            return new DocumentoVigente(tipo, versao, sha256(conteudo), conteudo);
        } catch (IOException e) {
            throw new IllegalStateException("Nao foi possivel carregar o documento legal: " + localizacao, e);
        }
    }

    private String sha256(String conteudo) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(conteudo.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 indisponivel.", e);
        }
    }

    private record DocumentoVigente(
            TipoDocumentoLegal tipo,
            String versao,
            String hashSha256,
            String conteudo) {

        private DocumentoLegalResponseDTO toResponse() {
            return new DocumentoLegalResponseDTO(tipo, versao, hashSha256, conteudo);
        }
    }
}
