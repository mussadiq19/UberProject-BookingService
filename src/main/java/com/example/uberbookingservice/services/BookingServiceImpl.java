package com.example.uberbookingservice.services;

import com.example.uberbookingservice.dtos.CreateBookingDto;
import com.example.uberbookingservice.dtos.CreateBookingResponseDto;
import com.example.uberbookingservice.dtos.DriverLocationDto;
import com.example.uberbookingservice.dtos.NearbyDriverRequestDto;
import com.example.uberbookingservice.repositories.BookingRepository;
import com.example.uberbookingservice.repositories.PassengerRepository;
import com.example.uberprojectentityservice.models.Booking;
import com.example.uberprojectentityservice.models.BookingStatus;
import com.example.uberprojectentityservice.models.Passenger;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.swing.text.html.Option;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class BookingServiceImpl implements BookingServices  {


        private final PassengerRepository passengerRepository;
        private final BookingRepository bookingRepository;
        private final RestTemplate restTemplate;

        private static  final String LOCATION_SERVICE="http://localhost:7777";

        public BookingServiceImpl(PassengerRepository passengerRepository, BookingRepository bookingRepository) {
                this.passengerRepository = passengerRepository;
                this.bookingRepository = bookingRepository;
                this.restTemplate = new RestTemplate();
        }

        @Override
        public CreateBookingResponseDto createBooking(CreateBookingDto bookingDetails) {
                Optional<Passenger> passenger= passengerRepository.findById(bookingDetails.getPassengerId());
                Booking booking =Booking.builder()
                        .bookingStatus(BookingStatus.ASSIGNING_DRIVER)
                        .startLocation(bookingDetails.getStartLocation())
                        .endLocation(bookingDetails.getEndLocation())
                        .build();
               Booking newBooking= bookingRepository.save(booking);
         //make an api call to location service to fetch nearby drivers
                NearbyDriverRequestDto request = NearbyDriverRequestDto.builder()
                        .latitude(bookingDetails.getStartLocation().getLatitude())
                        .longitude(bookingDetails.getEndLocation().getLongitude())
                        .build();
                ResponseEntity<DriverLocationDto[]> result= restTemplate.postForEntity(LOCATION_SERVICE+"/api/location/nearby/drivers",request,DriverLocationDto[].class);

                if(result.getStatusCode().is2xxSuccessful() && result.getBody()!=null){
                        List<DriverLocationDto> driverLocation = Arrays.asList(result.getBody());
                        driverLocation.forEach(driverLocationDto -> {
                                System.out.println(driverLocationDto.getDriverId()+" "+ " lat: "+driverLocationDto.getLatitude()+" "+"long:"+driverLocationDto.getLongitude());
                        });
                }

                return CreateBookingResponseDto.builder()
                        .bookingId(newBooking.getId())
                        .bookingStatus(newBooking.getBookingStatus().toString())
                        //.driver(Optional.of(newBooking.getDriver()))
                        .build();

        }
}
