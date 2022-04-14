package com.example.WishAndFish.service;

import com.example.WishAndFish.dto.AddActionDTO;
import com.example.WishAndFish.dto.AdditionalServicesDTO;
import com.example.WishAndFish.dto.AppointmentDTO;
import com.example.WishAndFish.dto.AvailabilityDTO;
import com.example.WishAndFish.model.AdditionalService;
import com.example.WishAndFish.model.Appointment;
import com.example.WishAndFish.model.Client;
import com.example.WishAndFish.model.Cottage;
import com.example.WishAndFish.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private CottageRepository cottageRepository;

    @Autowired
    private AdditionalServiceRepository additionalServiceRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private EmailService emailService;

    public List<AppointmentDTO> getAllByCottage(Long id){
        List<AppointmentDTO> ret = new ArrayList<>();
        for(Appointment as: appointmentRepository.findAll()){
            if(id.equals(as.getCottage().getId()) && !as.getReserved() && !as.isDeleted() && as.getIsAction()){
                ret.add(new AppointmentDTO(as));
            }
        }
        return ret;
    }

    public ResponseEntity<Long> deleteAppointment(Long id){
        for(Appointment a: this.appointmentRepository.findAll()){
            if(a.getId() == id) {
                a.setDeleted(true);
                this.appointmentRepository.save(a);
                return new ResponseEntity<>(id, HttpStatus.OK);
            }
        }
        return new ResponseEntity<>(id, HttpStatus.NOT_FOUND);
    }


    public Appointment editAvailability(AvailabilityDTO dto){
        Appointment a = new Appointment();
        a.setDeleted(false);
        a.setCottage(cottageRepository.findById(dto.getId()).orElseGet(null));
        a.setIsAction(false);
        a.setStartDate(findDate(dto.getStartDate()));
        a.setEndDate(findDate(dto.getEndDate()));
        a.setDuration(Duration.between(findDate(dto.getStartDate()), findDate(dto.getEndDate())));
        a.setReserved(false);
        this.appointmentRepository.save(a);
        return a;
    }

    private LocalDateTime findDate(String start){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
        return LocalDateTime.parse(start, formatter);
    }


    public Appointment addNewAction(AddActionDTO dto) throws MessagingException {
        Appointment a = new Appointment();
        a.setIsAction(true);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        a.setStartDate(LocalDateTime.parse(dto.getStartDate(), formatter));
        a.setEndDate(LocalDateTime.parse(dto.getEndDate(), formatter));
        a.setExpirationDate(LocalDateTime.parse(dto.getExpirationDate(), formatter));
        a.setCottage(cottageRepository.findById(dto.getId()).orElseGet(null));
        a.setMaxPersons(dto.getMaxPersons());
        a.setPrice(dto.getPrice());
        for(Long as: dto.getAdditionalServices()){
            a.getAdditionalServices().add(additionalServiceRepository.findById(as).orElseGet(null));
        }
        appointmentRepository.save(a);

        for(Client c: clientRepository.findAll()){
            for(Cottage cot: c.getCottageSubscriptions()){
                if(dto.getId().equals(cot.getId())){
                    emailService.sendEmailForNewAction(c.getEmail(), cot.getName());
                }
            }
        }

        //TO DO: slanje mejlova!!
        return a;
    }
}
