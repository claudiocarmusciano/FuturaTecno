package com.futuratecno.application;

import com.futuratecno.api.dto.AuthResponse;
import com.futuratecno.domain.Usuario;
import com.futuratecno.infrastructure.UsuarioRepository;
import com.futuratecno.infrastructure.security.GoogleTokenVerifier;
import com.futuratecno.infrastructure.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * La vinculación de Google a una cuenta existente es una toma de cuenta si sale mal: el vínculo no
 * se corta al cambiar la contraseña, y el registro no exige verificar el email, así que la
 * contraseña de una cuenta sin verificar la pudo haber elegido cualquiera.
 */
class AuthServiceGoogleTest {

    private static <T> T stub(Class<T> type) {
        return mock(type, withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS));
    }

    private final UsuarioRepository repo = stub(UsuarioRepository.class);
    private final GoogleTokenVerifier verifier = stub(GoogleTokenVerifier.class);
    private final JwtService jwt = stub(JwtService.class);
    private final AuthService service = new AuthService(repo, stub(PasswordEncoder.class), jwt, verifier,
            stub(EmailService.class));

    @BeforeEach
    void setUp() {
        when(verifier.verificar(anyString()))
                .thenReturn(new GoogleTokenVerifier.GoogleUser("sub-google", "persona@example.test", "Persona"));
        when(repo.findByGoogleSub(anyString())).thenReturn(Optional.empty());
        when(jwt.generarToken(anyString(), anyString())).thenReturn("jwt");
    }

    private static Usuario usuario(String rol, boolean verificado, String googleSub) {
        Usuario u = new Usuario();
        u.setEmail("persona@example.test");
        u.setRol(rol);
        u.setActivo(true);
        u.setEmailVerificado(verificado);
        u.setPassword("hash-elegido-por-quien-registro");
        u.setGoogleSub(googleSub);
        return u;
    }

    @Test void noVinculaGoogleALaCuentaAdmin() {
        Usuario admin = usuario("ADMIN", true, null);
        when(repo.findByEmailIgnoreCase("persona@example.test")).thenReturn(Optional.of(admin));

        assertThrows(IllegalArgumentException.class, () -> service.loginConGoogle("cred"));
        assertNull(admin.getGoogleSub());
        verify(repo, never()).save(any());
    }

    /** Una cuenta ADMIN que ya tenía Google vinculado (de antes de esta regla) tampoco entra por ahí. */
    @Test void rechazaAdminYaVinculado() {
        when(repo.findByGoogleSub("sub-google")).thenReturn(Optional.of(usuario("ADMIN", true, "sub-google")));

        assertThrows(IllegalArgumentException.class, () -> service.loginConGoogle("cred"));
        verify(jwt, never()).generarToken(anyString(), anyString());
    }

    @Test void noPisaUnGoogleYaVinculado() {
        Usuario u = usuario("USUARIO", true, "sub-de-otra-cuenta");
        when(repo.findByEmailIgnoreCase("persona@example.test")).thenReturn(Optional.of(u));

        assertThrows(IllegalArgumentException.class, () -> service.loginConGoogle("cred"));
        assertEquals("sub-de-otra-cuenta", u.getGoogleSub());
    }

    /** Pre-registro: quien creó la cuenta con un email ajeno pierde su contraseña al vincularse el dueño real. */
    @Test void borraLaContrasenaDeUnaCuentaSinVerificar() {
        Usuario u = usuario("USUARIO", false, null);
        when(repo.findByEmailIgnoreCase("persona@example.test")).thenReturn(Optional.of(u));

        AuthResponse r = service.loginConGoogle("cred");

        assertEquals("USUARIO", r.getRol());
        assertEquals("sub-google", u.getGoogleSub());
        assertNull(u.getPassword());
        assertTrue(u.getEmailVerificado());
    }

    @Test void conservaLaContrasenaDeUnaCuentaVerificada() {
        Usuario u = usuario("USUARIO", true, null);
        when(repo.findByEmailIgnoreCase("persona@example.test")).thenReturn(Optional.of(u));

        service.loginConGoogle("cred");

        assertEquals("sub-google", u.getGoogleSub());
        assertEquals("hash-elegido-por-quien-registro", u.getPassword());
    }
}
