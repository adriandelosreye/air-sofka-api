package ec.com.airsofka.router;

import ec.com.airsofka.AuthRouter;
import ec.com.airsofka.data.AuthRequestDTO;
import ec.com.airsofka.data.AuthResponseDTO;
import ec.com.airsofka.exceptions.RequestValidationException;
import ec.com.airsofka.handler.AuthHandler;
import ec.com.airsofka.user.values.objects.Role;
import ec.com.airsofka.validator.RequestValidatorShared;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthRouterTest {
    @Mock
    private RequestValidatorShared requestValidator;

    @Mock
    private AuthHandler authHandler;

    @InjectMocks
    private AuthRouter authRouter;

    private WebTestClient webTestClient;

    private AuthRequestDTO authRequestDTO;
    private AuthResponseDTO authResponseDTO;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToRouterFunction(authRouter.authRouters()).build();

        authRequestDTO = new AuthRequestDTO("test@gmail.com","Test123.");
        authResponseDTO = new AuthResponseDTO("test@gmail.com", "ADMIN", "as23n23y2kweb1wel231kwd1l");
    }

    @Test
    @DisplayName("Should login User when the validated credentials")
    void shouldLoginUserSuccessfully() {
        when(authHandler.authenticate(any())).thenReturn(Mono.just(authResponseDTO));

        webTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(authRequestDTO)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.token").isEqualTo(authResponseDTO.getToken());
    }

    @Test
    @DisplayName("Should return validation errors when exist error")
    void shouldReturnValidationErrors() {
        AuthRequestDTO invalidRequest = new AuthRequestDTO(null , "Test123.");

        doThrow(new RequestValidationException(List.of("email cannot be null")))
                .when(requestValidator).validate(any(AuthRequestDTO.class));

        webTestClient.post()
                .uri("/api/auth/login")
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().isEqualTo(500);
    }

}