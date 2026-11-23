package com.taskhive.backend;

import com.taskhive.backend.dto.AuthDtos.AuthResponse;
import com.taskhive.backend.dto.AuthDtos.LoginRequest;
import com.taskhive.backend.dto.AuthDtos.RegisterRequest;
import com.taskhive.backend.dto.BoardDtos.BoardDetailDto;
import com.taskhive.backend.dto.BoardDtos.BoardSummaryDto;
import com.taskhive.backend.dto.BoardDtos.CardDto;
import com.taskhive.backend.dto.BoardDtos.CreateCardRequest;
import com.taskhive.backend.dto.BoardDtos.InviteMemberRequest;
import com.taskhive.backend.dto.BoardDtos.MoveCardRequest;
import com.taskhive.backend.dto.BoardDtos.UpdateCardRequest;
import com.taskhive.backend.service.LoginRateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class TaskHiveIntegrationTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("app.redis.pubsub.enabled", () -> "false");
    }

    @LocalServerPort
    private int port;

    private final RestTemplate rest = new RestTemplate(new JdkClientHttpRequestFactory());

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    void endToEndCollaborationAuthorizationAndFractionalMove() {
        AuthResponse alice = rest.postForObject(
                url("/api/v1/auth/register"),
                new RegisterRequest("Alice", "alice-" + UUID.randomUUID() + "@example.com", "Password123!"),
                AuthResponse.class
        );
        String bobEmail = "bob-" + UUID.randomUUID() + "@example.com";
        AuthResponse bob = rest.postForObject(
                url("/api/v1/auth/register"),
                new RegisterRequest("Bob", bobEmail, "Password123!"),
                AuthResponse.class
        );
        assertThat(alice).isNotNull();
        assertThat(bob).isNotNull();

        ResponseEntity<BoardSummaryDto[]> aliceBoardsRes = rest.exchange(
                url("/api/v1/boards"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(alice.accessToken())),
                BoardSummaryDto[].class
        );
        BoardSummaryDto aliceBoard = aliceBoardsRes.getBody()[0];

        // Bob is blocked from accessing Alice's board (IDOR check -> 403)
        assertThatThrownBy(() -> rest.exchange(
                url("/api/v1/boards/" + aliceBoard.id()),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(bob.accessToken())),
                BoardDetailDto.class
        )).isInstanceOf(HttpClientErrorException.Forbidden.class);

        // Alice invites Bob to her workspace
        ResponseEntity<String> inviteRes = rest.exchange(
                url("/api/v1/workspaces/" + aliceBoard.workspaceId() + "/members"),
                HttpMethod.POST,
                new HttpEntity<>(new InviteMemberRequest(bobEmail, "MEMBER"), authHeaders(alice.accessToken())),
                String.class
        );
        assertThat(inviteRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Bob can now open Alice's shared board
        ResponseEntity<BoardDetailDto> sharedBoardRes = rest.exchange(
                url("/api/v1/boards/" + aliceBoard.id()),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(bob.accessToken())),
                BoardDetailDto.class
        );
        assertThat(sharedBoardRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        BoardDetailDto detail = sharedBoardRes.getBody();
        assertThat(detail).isNotNull();
        UUID todoListId = detail.lists().get(0).id();

        // Create two more cards and reorder the 3rd card between 1st and 2nd (midpoint fractional index)
        CardDto card2 = rest.exchange(
                url("/api/v1/boards/" + aliceBoard.id() + "/cards"),
                HttpMethod.POST,
                new HttpEntity<>(new CreateCardRequest(todoListId, "Second Card", "Desc 2"), authHeaders(bob.accessToken())),
                CardDto.class
        ).getBody();
        CardDto card3 = rest.exchange(
                url("/api/v1/boards/" + aliceBoard.id() + "/cards"),
                HttpMethod.POST,
                new HttpEntity<>(new CreateCardRequest(todoListId, "Third Card", "Desc 3"), authHeaders(bob.accessToken())),
                CardDto.class
        ).getBody();
        assertThat(card2).isNotNull();
        assertThat(card3).isNotNull();

        UUID card1Id = detail.cards().get(0).id();
        CardDto moved = rest.exchange(
                url("/api/v1/boards/" + aliceBoard.id() + "/cards/" + card3.id() + "/move"),
                HttpMethod.PATCH,
                new HttpEntity<>(
                        new MoveCardRequest(card3.id(), todoListId, null, card1Id, card2.id()),
                        authHeaders(bob.accessToken())
                ),
                CardDto.class
        ).getBody();
        assertThat(moved).isNotNull();
        assertThat(moved.position()).isBetween(detail.cards().get(0).position(), card2.position());

        // Edit card title & description
        CardDto edited = rest.exchange(
                url("/api/v1/boards/" + aliceBoard.id() + "/cards/" + card3.id()),
                HttpMethod.PATCH,
                new HttpEntity<>(new UpdateCardRequest("Edited Title", "Updated Desc"), authHeaders(alice.accessToken())),
                CardDto.class
        ).getBody();
        assertThat(edited).isNotNull();
        assertThat(edited.title()).isEqualTo("Edited Title");
        assertThat(edited.description()).isEqualTo("Updated Desc");
    }

    @Test
    void loginRateLimiterBlocksAfterTenFailures() {
        String email = "ratelimit-" + UUID.randomUUID() + "@example.com";
        rest.postForObject(
                url("/api/v1/auth/register"),
                new RegisterRequest("Rate Limit User", email, "CorrectPass123!"),
                AuthResponse.class
        );

        for (int i = 0; i < LoginRateLimiter.MAX_ATTEMPTS; i++) {
            assertThatThrownBy(() -> rest.postForEntity(
                    url("/api/v1/auth/login"),
                    new LoginRequest(email, "WrongPassword!"),
                    String.class
            )).isInstanceOf(HttpClientErrorException.Unauthorized.class);
        }

        assertThatThrownBy(() -> rest.postForEntity(
                url("/api/v1/auth/login"),
                new LoginRequest(email, "CorrectPass123!"),
                String.class
        )).isInstanceOf(HttpClientErrorException.TooManyRequests.class);
    }
}
