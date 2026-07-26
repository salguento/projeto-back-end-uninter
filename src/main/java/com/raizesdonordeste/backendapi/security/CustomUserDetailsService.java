package com.raizesdonordeste.backendapi.security;

import com.raizesdonordeste.backendapi.model.Usuario;
import com.raizesdonordeste.backendapi.repository.UsuarioRepository;
import com.raizesdonordeste.backendapi.util.LogPseudonymizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private static final String PREFIXO_ROLE = "ROLE_";
    private static final String USUARIO_NAO_ENCONTRADO_MSG = "Credenciais invalidas.";

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Objects.requireNonNull(email, "Email nao pode ser nulo");

        if (email.trim().isEmpty()) {
            throw new UsernameNotFoundException(USUARIO_NAO_ENCONTRADO_MSG);
        }

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Tentativa de login com conta inexistente | Ator: {}", LogPseudonymizer.id(email));
                    return new UsernameNotFoundException(USUARIO_NAO_ENCONTRADO_MSG);
                });

        if (Boolean.TRUE.equals(usuario.getAnonimizado())) {
            log.warn("Tentativa de login com conta anonimizada | UsuarioId: {}", usuario.getId());
            throw new UsernameNotFoundException(USUARIO_NAO_ENCONTRADO_MSG);
        }

        String roleName = PREFIXO_ROLE + usuario.getPerfil().name().toUpperCase();
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(roleName)
        );

        log.info("Login bem-sucedido | UsuarioId: {} | Perfil: {}", usuario.getId(), usuario.getPerfil());

        return new User(usuario.getEmail(), usuario.getSenha(), authorities);
    }
}
