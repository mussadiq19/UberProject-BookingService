package com.example.uberbookingservice.services;

import com.example.uberbookingservice.apis.LocationServiceApi;
import com.example.uberbookingservice.dtos.*;
import com.example.uberbookingservice.repositories.BookingRepository;
import com.example.uberbookingservice.repositories.DriverRepository;
import com.example.uberbookingservice.repositories.PassengerRepository;
import com.example.uberprojectentityservice.models.Booking;
import com.example.uberprojectentityservice.models.BookingStatus;
import com.example.uberprojectentityservice.models.Driver;
import com.example.uberprojectentityservice.models.Passenger;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import javax.swing.text.html.Option;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class BookingServiceImpl implements BookingServices  {


        private final PassengerRepository passengerRepository;
        private final BookingRepository bookingRepository;
        private final RestTemplate restTemplate;
        private final LocationServiceApi locationServiceApi;

        private static  final String LOCATION_SERVICE="http://localhost:7777";
    private final DriverRepository driverRepository;

    public BookingServiceImpl(PassengerRepository passengerRepository, BookingRepository bookingRepository, LocationServiceApi locationServiceApi, DriverRepository driverRepository) {
                this.passengerRepository = passengerRepository;
                this.bookingRepository = bookingRepository;
            this.locationServiceApi = locationServiceApi;
            this.restTemplate = new RestTemplate();
        this.driverRepository = driverRepository;
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
                processNearbyDriversAsync(request);
//                ResponseEntity<DriverLocationDto[]> result= restTemplate.postForEntity(LOCATION_SERVICE+"/api/location/nearby/drivers",request,DriverLocationDto[].class);
////try this with retrofit
//                if(result.getStatusCode().is2xxSuccessful() && result.getBody()!=null){
//                        List<DriverLocationDto> driverLocation = Arrays.asList(result.getBody());
//                        driverLocation.forEach(driverLocationDto -> {
//                                System.out.println(driverLocationDto.getDriverId()+" "+ " lat: "+driverLocationDto.getLatitude()+" "+"long:"+driverLocationDto.getLongitude());
//                        });
//                }

                return CreateBookingResponseDto.builder()
                        .bookingId(newBooking.getId())
                        .bookingStatus(newBooking.getBookingStatus().toString())
                        //.driver(Optional.of(newBooking.getDriver()))
                        .build();


        }

    @Override
    public UpdateBookingResponseDto updateBooking(UpdateBookingRequestDto bookingRequestDto, Long bookingId) {

       Driver driver = driverRepository.findById(bookingRequestDto.getDriverId())
                .orElseThrow(() -> new RuntimeException("Driver not found with id: " + bookingRequestDto.getDriverId()));

        bookingRepository.updateBookingStatusAndDriverById(bookingId, BookingStatus.SCHEDULED, driver);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + bookingId));

        return UpdateBookingResponseDto.builder()
                .bookingId(bookingId)
                .status(booking.getBookingStatus())
                .driver(Optional.ofNullable(booking.getDriver()))
                .build();
    }

    private void processNearbyDriversAsync(NearbyDriverRequestDto requestDto){
            Call<DriverLocationDto[]>call=locationServiceApi.getNearbyDrivers(requestDto);
            call.enqueue(new Callback<DriverLocationDto[]>() {
                @Override
                public void onResponse(Call<DriverLocationDto[]> call, Response<DriverLocationDto[]> response) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    if(response.isSuccessful() && response.body()!=null){
                        List<DriverLocationDto> driverLocation = Arrays.asList(response.body());
                        driverLocation.forEach(driverLocationDto -> {
                                System.out.println(driverLocationDto.getDriverId()+" "+ " lat: "+driverLocationDto.getLatitude()+" "+"long:"+driverLocationDto.getLongitude());
                        });
                    }else {
                     System.out.println("Request failed"+response.message());
                 }
                }

                @Override
                public void onFailure(Call<DriverLocationDto[]> call, Throwable t) {
                    t.printStackTrace();
                }
            });

        }
}
