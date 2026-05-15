package com.bridgelabz.providerservice.service.impl;

import com.bridgelabz.providerservice.dto.ProviderRequest;
import com.bridgelabz.providerservice.dto.ProviderResponse;
import com.bridgelabz.providerservice.exception.ProviderAlreadyExistsException;
import com.bridgelabz.providerservice.exception.ProviderNotFoundException;
import com.bridgelabz.providerservice.model.Provider;
import com.bridgelabz.providerservice.repository.ProviderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProviderServiceImplTest {

    @Mock
    private ProviderRepository providerRepository;

    @InjectMocks
    private ProviderServiceImpl providerService;

    private ProviderRequest providerRequest;
    private Provider provider;

    @BeforeEach
    void setUp() {
        providerRequest = ProviderRequest.builder()
                .userId("DOC-445")
                .fullName("Dr. Amit Verma")
                .specialization("General Medicine")
                .consultationFee(500.0)
                .clinicName("City Health Clinic")
                .build();

        provider = Provider.builder()
                .providerId("PROV-101")
                .userId("DOC-445")
                .fullName("Dr. Amit Verma")
                .specialization("General Medicine")
                .isVerified(true)
                .build();
    }

    @Test
    void registerProvider_Success() {
        when(providerRepository.findByUserId("DOC-445")).thenReturn(Optional.empty());
        when(providerRepository.save(any())).thenReturn(provider);

        ProviderResponse response = providerService.registerProvider(providerRequest);

        assertNotNull(response);
        assertEquals("Dr. Amit Verma", response.getFullName());
        verify(providerRepository, times(1)).save(any());
    }

    @Test
    void registerProvider_AlreadyExists() {
        when(providerRepository.findByUserId("DOC-445")).thenReturn(Optional.of(provider));

        assertThrows(ProviderAlreadyExistsException.class, () -> {
            providerService.registerProvider(providerRequest);
        });
    }

    @Test
    void getProviderById_Success() {
        when(providerRepository.findById("PROV-101")).thenReturn(Optional.of(provider));

        ProviderResponse response = providerService.getProviderById("PROV-101");

        assertEquals("PROV-101", response.getProviderId());
    }

    @Test
    void verifyProvider_Success() {
        when(providerRepository.findById("PROV-101")).thenReturn(Optional.of(provider));
        when(providerRepository.save(any())).thenReturn(provider);

        ProviderResponse response = providerService.verifyProvider("PROV-101", true);

        verify(providerRepository, times(1)).save(any());
    }

    @Test
    void searchProviders_ReturnsList() {
        when(providerRepository.findByFullNameContainingIgnoreCaseOrSpecializationContainingIgnoreCase(any(), any()))
                .thenReturn(Collections.singletonList(provider));

        List<ProviderResponse> results = providerService.searchProviders("General Medicine");

        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
    }
}
