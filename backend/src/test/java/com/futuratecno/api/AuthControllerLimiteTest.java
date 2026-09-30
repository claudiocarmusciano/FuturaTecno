package com.futuratecno.api;

import com.futuratecno.api.dto.AuthResponse;
import com.futuratecno.api.dto.ForgotPasswordRequest;
import com.futuratecno.api.dto.LoginRequest;
import com.futuratecno.application.AuthService;
import com.futuratecno.application.LimitadorIntentos;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerLimiteTest {

    private final AuthService auth = mock(AuthService.class);
    private final AuthController controller = new AuthController(auth, new LimitadorIntentos(), "https://www.futuratecno.com.ar");

    private static LoginRequest login(String email, String password) {
        LoginRequest r = new LoginRequest();
        r.setEmail(email);
        r.setPassword(password);
        return r;
    }

    private static MockHttpServletRequest desde(String ip) {
        MockHttpServletRequest r = new MockHttpServletRequest();
        r.setRemoteAddr(ip);
        return r;
    }

    @Test
    void despuesDeCincoContraseniasMalasLaCuentaQuedaBloqueadaAunqueCambieLaIp() {
        when(auth.login(argThat(r -> r != null && "mala".equals(r.getPassword())))).thenThrow(new IllegalArgumentException("Credenciales inválidas"));
        when(auth.login(argThat(r -> r != null && "buena".equals(r.getPassword())))).thenReturn(new AuthResponse("t", "admin@x.com", "Admin", "ADMIN"));

        for (int i = 0; i < AuthController.LOGIN_FALLOS_POR_EMAIL; i++) {
            assertEquals(401, controller.login(login("admin@x.com", "mala"), desde("1.1.1." + i)).getStatusCode().value());
        }
        // Bloqueada: ni con la contraseña correcta, ni desde otra IP, ni escrita con mayúsculas.
        assertEquals(429, controller.login(login(" ADMIN@x.com ", "buena"), desde("9.9.9.9")).getStatusCode().value());
        verify(auth, times(AuthController.LOGIN_FALLOS_POR_EMAIL)).login(any());

        // Otra cuenta no se ve afectada.
        when(auth.login(argThat(r -> r != null && "otra@x.com".equals(r.getEmail())))).thenReturn(new AuthResponse("t", "otra@x.com", "Otra", "USUARIO"));
        assertEquals(200, controller.login(login("otra@x.com", "x"), desde("9.9.9.9")).getStatusCode().value());
    }

    @Test
    void unLoginCorrectoReiniciaLaCuentaDeFallos() {
        when(auth.login(argThat(r -> r != null && "mala".equals(r.getPassword())))).thenThrow(new IllegalArgumentException("Credenciales inválidas"));
        when(auth.login(argThat(r -> r != null && "buena".equals(r.getPassword())))).thenReturn(new AuthResponse("t", "ana@x.com", "Ana", "USUARIO"));
        for (int ronda = 0; ronda < 3; ronda++) {
            for (int i = 0; i < AuthController.LOGIN_FALLOS_POR_EMAIL - 1; i++) controller.login(login("ana@x.com", "mala"), desde("2.2.2." + ronda));
            assertEquals(200, controller.login(login("ana@x.com", "buena"), desde("2.2.2." + ronda)).getStatusCode().value());
        }
    }

    @Test
    void recuperarContraseniaMandaComoMaximoTresMailsPorHoraYResponde200Igual() {
        ForgotPasswordRequest r = new ForgotPasswordRequest();
        r.setEmail("ana@x.com");
        for (int i = 0; i < 4; i++) {
            assertEquals(200, controller.forgotPassword(r, desde("3.3.3." + i)).getStatusCode().value());
        }
        verify(auth, times(3)).solicitarReset(eq("ana@x.com"), anyString());

        // Y por IP corta con 429 a las 5 por hora.
        ForgotPasswordRequest otro = new ForgotPasswordRequest();
        for (int i = 0; i < 5; i++) { otro.setEmail("u" + i + "@x.com"); controller.forgotPassword(otro, desde("4.4.4.4")); }
        otro.setEmail("u9@x.com");
        assertEquals(429, controller.forgotPassword(otro, desde("4.4.4.4")).getStatusCode().value());
        verify(auth, never()).solicitarReset(eq("u9@x.com"), anyString());
    }
}
