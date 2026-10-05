package com.example.musinsaPointSystem.data.apiController;
import org.springframework.web.bind.annotation.*;
import com.example.musinsaPointSystem.data.apiService.ObservationAreaService;
@RestController
@RequestMapping("/v1/mobility")
public class ObservationAreaController {
    private final ObservationAreaService service;
    public ObservationAreaController(ObservationAreaService service){this.service=service;}
    @GetMapping("/observation-areas")
    public ObservationAreaService.ObservationAreasResponse areas(){return service.get();}
}
