package com.cargorent.booking.client;

import com.cargorent.booking.common.ApiException;
import com.cargorent.booking.dto.VehicleInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class VehicleClient {

    private final RestClient rest;

    public VehicleClient(RestClient.Builder builder, @Value("${services.vehicle.url}") String baseUrl) {
        this.rest = builder.baseUrl(baseUrl).build();
    }

    public VehicleInfo get(Long vehicleId) {
        try {
            return rest.get().uri("/vehicles/{id}", vehicleId).retrieve().body(VehicleInfo.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw ApiException.notFound("Vehicle not found");
        } catch (RestClientException e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Vehicle service is unavailable. Please try again.");
        }
    }
}
