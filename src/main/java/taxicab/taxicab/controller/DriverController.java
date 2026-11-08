package taxicab.taxicab.controller;

import taxicab.taxicab.model.Driver;
import taxicab.taxicab.service.DriverService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/drivers")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @GetMapping
    public String listDrivers(Model model) {
        model.addAttribute(
                "drivers", driverService.getAllDrivers());
        return "driver/list";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("driver", new Driver());
        return "driver/form";
    }

    @PostMapping("/save")
    public String saveDriver(@ModelAttribute Driver driver) {
        driverService.saveDriver(driver);
        return "redirect:/drivers";
    }

    @GetMapping("/edit/{id}")
    public String editDriver(
            @PathVariable Long id, Model model) {

        model.addAttribute(
                "driver", driverService.getDriverById(id));

        return "driver/form";
    }

    @PostMapping("/update/{id}")
    public String updateDriver(
            @PathVariable Long id,
            @ModelAttribute Driver driver) {

        driverService.updateDriver(id, driver);
        return "redirect:/drivers";
    }

    @PostMapping("/delete/{id}")
    public String deleteDriver(@PathVariable Long id) {
        driverService.deleteDriver(id);
        return "redirect:/drivers";
    }
}
