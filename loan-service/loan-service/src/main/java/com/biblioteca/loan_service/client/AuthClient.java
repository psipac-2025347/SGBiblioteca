package com.biblioteca.loan_service.client;

import com.biblioteca.loan_service.exception.ResourceNotFoundException;
import com.biblioteca.loan_service.exception.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Component
public class AuthClient {

    private final RestClient restClient;

    public AuthClient(@Value("${services.auth.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public UsuarioRemoto obtenerUsuario(Long id, String authorization) {
        try {
            return restClient.get()
                    .uri("/api/v1/usuarios/{id}", id)
                    .header("Authorization", authorization)
                    .retrieve()
                    .body(UsuarioRemoto.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Usuario no encontrado con id " + id);
        } catch (RestClientResponseException e) {
            throw new ServiceUnavailableException(
                    "auth-service respondió con error " + e.getStatusCode().value());
        } catch (ResourceAccessException e) {
            throw new ServiceUnavailableException("auth-service no está disponible");
        }
    }

    public void sancionar(Long id, String authorization) {
        try {
            restClient.patch()
                    .uri("/api/v1/usuarios/{id}/estado", id)
                    .header("Authorization", authorization)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("estado", "SANCIONADO"))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw new ServiceUnavailableException(
                    "auth-service respondió con error " + e.getStatusCode().value());
        } catch (ResourceAccessException e) {
            throw new ServiceUnavailableException("auth-service no está disponible");
        }
    }
}