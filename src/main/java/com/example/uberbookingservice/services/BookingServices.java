package com.example.uberbookingservice.services;

import com.example.uberbookingservice.dtos.CreateBookingDto;
import com.example.uberbookingservice.dtos.CreateBookingResponseDto;
import com.example.uberprojectentityservice.models.Booking;
import org.springframework.stereotype.Service;

@Service
public interface BookingServices {
    CreateBookingResponseDto createBooking(CreateBookingDto bookingDetails);
}
