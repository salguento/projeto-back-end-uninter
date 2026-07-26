package com.raizesdonordeste.backendapi.service;

import com.raizesdonordeste.backendapi.dto.AceiteDocumentoResponseDTO;
import com.raizesdonordeste.backendapi.dto.AceiteTermosRequestDTO;
import com.raizesdonordeste.backendapi.dto.CadastroUsuarioDTO;
import com.raizesdonordeste.backendapi.dto.DocumentoLegalResponseDTO;
import com.raizesdonordeste.backendapi.exception.RegraNegocioException;
import com.raizesdonordeste.backendapi.model.AceiteDocumento;
import com.raizesdonordeste.backendapi.model.TipoDocumentoLegal;
import com.raizesdonordeste.backendapi.model.Usuario;
import com.raizesdonordeste.backendapi.repository.AceiteDocumentoRepository;
import com.raizesdonordeste.backendapi.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentoLegalService - aceite contratual versionado")
class DocumentoLegalServiceTest {

    @Mock
    private AceiteDocumentoRepository aceiteDocumentoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    private DocumentoLegalService service;
    private Usuario usuario;
    private DocumentoLegalResponseDTO termos;

    @BeforeEach
    void configurar() {
        service = new DocumentoLegalService(aceiteDocumentoRepository, usuarioRepository,
                new DefaultResourceLoader());
        ReflectionTestUtils.setField(service, "versaoTermosUso", "1.0");
        ReflectionTestUtils.setField(service, "recursoTermosUso", "classpath:legal/termos-uso-v1.0.md");
        ReflectionTestUtils.setField(service, "versaoAvisoPrivacidade", "1.0");
        ReflectionTestUtils.setField(service, "recursoAvisoPrivacidade",
                "classpath:legal/aviso-privacidade-v1.0.md");
        service.carregarDocumentos();
        termos = service.obterTermosUso();

        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("cliente@raizes.com");
    }

    @Test
    @DisplayName("Deve publicar termos e aviso com versão, conteúdo e hash SHA-256")
    void devePublicarDocumentosVigentes() {
        DocumentoLegalResponseDTO aviso = service.obterAvisoPrivacidade();

        assertEquals(TipoDocumentoLegal.TERMOS_USO, termos.tipo());
        assertEquals("1.0", termos.versao());
        assertEquals(64, termos.hashSha256().length());
        assertTrue(termos.conteudo().contains("Termos de Uso"));
        assertEquals(TipoDocumentoLegal.AVISO_PRIVACIDADE, aviso.tipo());
        assertEquals(64, aviso.hashSha256().length());
    }

    @Test
    @DisplayName("Deve rejeitar ausência de aceite explícito")
    void deveRejeitarAusenciaDeAceiteExplicito() {
        CadastroUsuarioDTO dto = cadastroValido();
        dto.setAceiteTermosUso(false);

        RegraNegocioException erro = assertThrows(RegraNegocioException.class,
                () -> service.validarDadosAceiteCadastro(dto));

        assertEquals("TERMOS_USO_NAO_ACEITOS", erro.getErrorCode());
    }

    @Test
    @DisplayName("Deve rejeitar versão ou hash diferentes do documento vigente")
    void deveRejeitarDocumentoDesatualizado() {
        CadastroUsuarioDTO dto = cadastroValido();
        dto.setVersaoTermosUso("0.9");

        RegraNegocioException erro = assertThrows(RegraNegocioException.class,
                () -> service.validarDadosAceiteCadastro(dto));

        assertEquals("DOCUMENTO_LEGAL_DESATUALIZADO", erro.getErrorCode());
    }

    @Test
    @DisplayName("Deve registrar aceite atual para usuário autenticado")
    void deveRegistrarAceiteAtual() {
        AceiteTermosRequestDTO dto = new AceiteTermosRequestDTO();
        dto.setAceito(true);
        dto.setVersao(termos.versao());
        dto.setHashSha256(termos.hashSha256());
        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(aceiteDocumentoRepository.findByUsuarioIdAndTipoDocumentoAndVersao(
                1L, TipoDocumentoLegal.TERMOS_USO, "1.0")).thenReturn(Optional.empty());
        when(aceiteDocumentoRepository.save(any(AceiteDocumento.class))).thenAnswer(invocation -> {
            AceiteDocumento aceite = invocation.getArgument(0);
            aceite.setId(10L);
            return aceite;
        });

        AceiteDocumentoResponseDTO response = service.registrarAceiteAtual(usuario.getEmail(), dto);

        assertTrue(response.vigente());
        assertNotNull(response.aceitoEm());
        verify(aceiteDocumentoRepository).save(argThat(aceite ->
                aceite.getUsuario() == usuario
                        && "API_AUTOSSERVICO".equals(aceite.getOrigem())
                        && termos.hashSha256().equals(aceite.getHashDocumento())));
    }

    @Test
    @DisplayName("Deve tratar repetição do aceite como operação idempotente")
    void deveTratarAceiteRepetidoComoIdempotente() {
        AceiteDocumento existente = new AceiteDocumento();
        existente.setUsuario(usuario);
        existente.setTipoDocumento(TipoDocumentoLegal.TERMOS_USO);
        existente.setVersao(termos.versao());
        existente.setHashDocumento(termos.hashSha256());
        when(aceiteDocumentoRepository.findByUsuarioIdAndTipoDocumentoAndVersao(
                1L, TipoDocumentoLegal.TERMOS_USO, "1.0")).thenReturn(Optional.of(existente));

        AceiteDocumentoResponseDTO response = service.registrarAceiteSeed(usuario);

        assertTrue(response.vigente());
        verify(aceiteDocumentoRepository, never()).save(any());
    }

    private CadastroUsuarioDTO cadastroValido() {
        CadastroUsuarioDTO dto = new CadastroUsuarioDTO();
        dto.setAceiteTermosUso(true);
        dto.setVersaoTermosUso(termos.versao());
        dto.setHashTermosUso(termos.hashSha256());
        return dto;
    }
}
