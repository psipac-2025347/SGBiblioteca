package com.biblioteca.loan_service.client;

import com.biblioteca.loan_service.exception.BusinessRuleException;
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
public class CatalogClient {

    private final RestClient restClient;

    public CatalogClient(@Value("${services.catalog.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    /** cambio = -1 al prestar, +1 al devolver. */
    public void ajustarStock(Long libroId, int cambio, String authorization) {
        try {
            restClient.patch()
                    .uri("/api/v1/libros/{id}/stock", libroId)
                    .header("Authorization", authorization)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("cambio", cambio))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Libro no encontrado con id " + libroId);
        } catch (HttpClientErrorException.BadRequest e) {
            throw new BusinessRuleException(cambio < 0
                    ? "El libro no tiene stock disponible"
                    : "No se pudo devolver el ejemplar: el stock disponible superaría el stock total");
        } catch (RestClientResponseException e) {
            throw new ServiceUnavailableException(
                    "catalog-service respondió con error " + e.getStatusCode().value());
        } catch (ResourceAccessException e) {
            throw new ServiceUnavailableException("catalog-service no está disponible");
        }
    }
}