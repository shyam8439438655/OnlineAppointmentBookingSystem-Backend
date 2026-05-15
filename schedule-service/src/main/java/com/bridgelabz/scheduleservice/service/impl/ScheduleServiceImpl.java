package com.bridgelabz.scheduleservice.service.impl;

import com.bridgelabz.scheduleservice.dto.*;
import com.bridgelabz.scheduleservice.exception.SlotNotFoundException;
import com.bridgelabz.scheduleservice.model.AvailabilitySlot;
import com.bridgelabz.scheduleservice.repository.SlotRepository;
import com.bridgelabz.scheduleservice.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final SlotRepository slotRepository;

    private static final String SLOT_NOT_FOUND_ID = "Slot not found with id: ";

    @Override
    public SlotResponse addSlot(SlotRequest request) {
        AvailabilitySlot slot = AvailabilitySlot.builder()
                .providerId(request.getProviderId())
                .date(request.getDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .durationMinutes(request.getDurationMinutes())
                .recurrence(request.getRecurrence())
                .isBooked(false)
                .isBlocked(false)
                .build();

        return mapToResponse(slotRepository.save(slot));
    }

    @Override
    public List<SlotResponse> addBulkSlots(List<SlotRequest> requests) {
        return requests.stream()
                .map(this::addSlot)
                .toList();
    }

    @Override
    public List<SlotResponse> getSlotsByProvider(String providerId) {
        return slotRepository.findByProviderId(providerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<SlotResponse> getAvailableSlots(String providerId, LocalDate date) {
        return slotRepository
                .findByProviderIdAndDateAndIsBookedFalseAndIsBlockedFalse(providerId, date)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public SlotResponse getSlotById(String slotId) {
        AvailabilitySlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new SlotNotFoundException(SLOT_NOT_FOUND_ID + slotId));

        return mapToResponse(slot);
    }

    @Override
    public SlotResponse blockSlot(String slotId, Boolean isBlocked) {
        AvailabilitySlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new SlotNotFoundException(SLOT_NOT_FOUND_ID + slotId));

        slot.setIsBlocked(isBlocked);
        return mapToResponse(slotRepository.save(slot));
    }

    @Override
    public SlotResponse updateSlot(String slotId, SlotRequest request) {
        AvailabilitySlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new SlotNotFoundException(SLOT_NOT_FOUND_ID + slotId));

        slot.setDate(request.getDate());
        slot.setStartTime(request.getStartTime());
        slot.setEndTime(request.getEndTime());
        slot.setDurationMinutes(request.getDurationMinutes());

        return mapToResponse(slotRepository.save(slot));
    }

    @Override
    public void deleteSlot(String slotId) {
        AvailabilitySlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new SlotNotFoundException(SLOT_NOT_FOUND_ID + slotId));

        slotRepository.delete(slot);
    }

    @Override
    public List<SlotResponse> generateRecurringSlots(RecurringSlotRequest request) {
        LocalDate current = request.getStartDate();
        List<AvailabilitySlot> slots = new java.util.ArrayList<>();

        while (!current.isAfter(request.getEndDate())) {
            // Check if current day is in requested daysOfWeek (if provided)
            boolean shouldCreate = true;
            if (request.getDaysOfWeek() != null && !request.getDaysOfWeek().isEmpty()) {
                shouldCreate = request.getDaysOfWeek().contains(current.getDayOfWeek().getValue());
            }

            if (shouldCreate) {
                AvailabilitySlot slot = AvailabilitySlot.builder()
                        .providerId(request.getProviderId())
                        .date(current)
                        .startTime(request.getStartTime())
                        .endTime(request.getEndTime())
                        .durationMinutes(request.getDurationMinutes())
                        .recurrence(request.getRecurrence())
                        .isBooked(false)
                        .isBlocked(false)
                        .build();

                slots.add(slot);
            }

            current = current.plusDays(1);
        }

        return slotRepository.saveAll(slots)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public SlotResponse updateBookingStatus(String slotId, Boolean isBooked) {
        AvailabilitySlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new SlotNotFoundException(SLOT_NOT_FOUND_ID + slotId));
        
        slot.setIsBooked(isBooked);
        return mapToResponse(slotRepository.save(slot));
    }

    @Override
    public List<SlotResponse> getAllSlots() {
        return slotRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private SlotResponse mapToResponse(AvailabilitySlot slot) {
        return SlotResponse.builder()
                .slotId(slot.getSlotId())
                .providerId(slot.getProviderId())
                .date(slot.getDate())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .durationMinutes(slot.getDurationMinutes())
                .isBooked(slot.getIsBooked())
                .isBlocked(slot.getIsBlocked())
                .recurrence(slot.getRecurrence())
                .createdAt(slot.getCreatedAt())
                .build();
    }
}