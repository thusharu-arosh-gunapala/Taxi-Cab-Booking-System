package taxicab.taxicab.controller;

import taxicab.taxicab.model.Vehicle;
import taxicab.taxicab.service.VehicleService;
import taxicab.taxicab.service.DriverService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;
    private final DriverService driverService;

    public VehicleController(
            VehicleService vehicleService,
            DriverService driverService) {
        this.vehicleService = vehicleService;
        this.driverService = driverService;
    }

    @GetMapping
    public String listVehicles(Model model) {
        model.addAttribute(
                "vehicles", vehicleService.getAllVehicles());

        return "driver/vehicle-list";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute(
                "drivers", driverService.getAllDrivers());

        model.addAttribute("vehicle", null);

        return "driver/vehicle-form";
    }

    @PostMapping("/save")
    public String saveVehicle(
            @RequestParam String category,
            @RequestParam String vehicleNumber,
            @RequestParam String brand,
            @RequestParam String vehicleModel,
            @RequestParam int seats,
            @RequestParam(required = false) Long driverId) {

        vehicleService.createVehicle(
                category,
                vehicleNumber,
                brand,
                vehicleModel,
                seats,
                driverId
        );

        return "redirect:/vehicles";
    }

    @GetMapping("/edit/{id}")
    public String editVehicle(
            @PathVariable Long id, Model model) {

        model.addAttribute(
                "vehicle", vehicleService.getVehicleById(id));

        model.addAttribute(
                "drivers", driverService.getAllDrivers());

        return "driver/vehicle-form";
    }

    @PostMapping("/update/{id}")
    public String updateVehicle(
            @PathVariable Long id,
            @RequestParam String vehicleNumber,
            @RequestParam String brand,
            @RequestParam String vehicleModel,
            @RequestParam int seats,
            @RequestParam(defaultValue = "true") boolean available,
            @RequestParam(required = false) Long driverId) {

        Vehicle oldVehicle =
                vehicleService.getVehicleById(id);

        oldVehicle.setVehicleNumber(vehicleNumber);
        oldVehicle.setBrand(brand);
        oldVehicle.setModel(vehicleModel);
        oldVehicle.setSeats(seats);
        oldVehicle.setAvailable(available);

        vehicleService.updateVehicle(
                id, oldVehicle, driverId);

        return "redirect:/vehicles";
    }

    @PostMapping("/delete/{id}")
    public String deleteVehicle(@PathVariable Long id) {
        vehicleService.deleteVehicle(id);
        return "redirect:/vehicles";
    }
}
