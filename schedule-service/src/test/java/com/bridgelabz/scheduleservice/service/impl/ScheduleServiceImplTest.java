package com.bridgelabz.scheduleservice.service.impl;

import com.bridgelabz.scheduleservice.dto.*;
import com.bridgelabz.scheduleservice.model.AvailabilitySlot;
import com.bridgelabz.scheduleservice.repository.SlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceImplTest {

    @Mock
    private SlotRepository slotRepository;

    @InjectMocks
    private ScheduleServiceImpl scheduleService;

    private SlotRequest slotRequest;
    private AvailabilitySlot slot;

    @BeforeEach
    void setUp() {
        slotRequest = SlotRequest.builder()
                .providerId("PROV-101")
                .date(LocalDate.now().plusDays(2))
                .startTime(LocalTime.of(11, 0))
                .endTime(LocalTime.of(11, 45))
                .durationMinutes(45)
                .build();

        slot = AvailabilitySlot.builder()
                .slotId("SLOT-77")
                .providerId("PROV-101")
                .date(LocalDate.now().plusDays(2))
                .startTime(LocalTime.of(11, 0))
                .endTime(LocalTime.of(11, 45))
                .isBooked(false)
                .isBlocked(false)
                .build();
    }

    @Test
    void addSlot_Success() {
        when(slotRepository.save(any())).thenReturn(slot);
        SlotResponse response = scheduleService.addSlot(slotRequest);
        assertNotNull(response);
        assertEquals("PROV-101", response.getProviderId());
    }

    @Test
    void getAvailableSlots_ReturnsList() {
        when(slotRepository.findByProviderIdAndDateAndIsBookedFalseAndIsBlockedFalse(any(), any()))
                .thenReturn(Collections.singletonList(slot));
        List<SlotResponse> results = scheduleService.getAvailableSlots("PROV-101", LocalDate.now().plusDays(2));
        assertFalse(results.isEmpty());
    }

    @Test
    void blockSlot_UpdatesStatus() {
        when(slotRepository.findById("SLOT-77")).thenReturn(Optional.of(slot));
        when(slotRepository.save(any())).thenReturn(slot);
        SlotResponse response = scheduleService.blockSlot("SLOT-77", true);
        verify(slotRepository, times(1)).save(any());
    }

    @Test
    void generateRecurringSlots_WeeklySuccess() {
        RecurringSlotRequest req = RecurringSlotRequest.builder()
                .providerId("PROV-101")
                .startDate(LocalDate.of(2026, 6, 1)) // Monday
                .endDate(LocalDate.of(2026, 6, 7))   // Sunday
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(10, 30))
                .daysOfWeek(Arrays.asList(1, 3, 5)) // Mon, Wed, Fri
                .build();

        when(slotRepository.saveAll(any())).thenReturn(Collections.singletonList(slot));
        
        List<SlotResponse> results = scheduleService.generateRecurringSlots(req);
        
        verify(slotRepository).saveAll(argThat(list -> ((List<?>)list).size() == 3));
        assertNotNull(results);
    }

    @Test
    void updateBookingStatus_Success() {
        when(slotRepository.findById("SLOT-77")).thenReturn(Optional.of(slot));
        when(slotRepository.save(any())).thenReturn(slot);
        SlotResponse response = scheduleService.updateBookingStatus("SLOT-77", true);
        verify(slotRepository).save(argThat(s -> s.getIsBooked()));
    }
}
