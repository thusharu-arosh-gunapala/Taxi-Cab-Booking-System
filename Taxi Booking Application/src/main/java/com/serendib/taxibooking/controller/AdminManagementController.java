package com.serendib.taxibooking.controller;

import com.serendib.taxibooking.dto.DriverApplicationForm;
import com.serendib.taxibooking.dto.AdministratorForm;
import com.serendib.taxibooking.dto.VehicleForm;
import com.serendib.taxibooking.entity.Driver;
import com.serendib.taxibooking.entity.DriverApplicationStatus;
import com.serendib.taxibooking.entity.RefundStatus;
import com.serendib.taxibooking.repository.AdministratorRepository;
import com.serendib.taxibooking.repository.BookingRepository;
import com.serendib.taxibooking.repository.PassengerRepository;
import com.serendib.taxibooking.repository.PaymentRepository;
import com.serendib.taxibooking.repository.RefundRepository;
import com.serendib.taxibooking.service.AdminOperationsService;
import com.serendib.taxibooking.model.VehicleCategory;
import com.serendib.taxibooking.service.DriverService;
import com.serendib.taxibooking.service.DriverDocumentStorageService;
import com.serendib.taxibooking.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminManagementController {

    private final DriverService driverService;
    private final VehicleService vehicleService;
    private final BookingRepository bookingRepository;
    private final PassengerRepository passengerRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final AdminOperationsService adminOperationsService;
    private final AdministratorRepository administratorRepository;
    private final DriverDocumentStorageService driverDocumentStorageService;

    public AdminManagementController(DriverService driverService,
                                     VehicleService vehicleService,
                                     BookingRepository bookingRepository,
                                     PassengerRepository passengerRepository,
                                     PaymentRepository paymentRepository,
                                     RefundRepository refundRepository,
                                     AdminOperationsService adminOperationsService,
                                     AdministratorRepository administratorRepository,
                                     DriverDocumentStorageService driverDocumentStorageService) {
        this.driverService = driverService;
        this.vehicleService = vehicleService;
        this.bookingRepository = bookingRepository;
        this.passengerRepository = passengerRepository;
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.adminOperationsService = adminOperationsService;
        this.administratorRepository = administratorRepository;
        this.driverDocumentStorageService = driverDocumentStorageService;
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        model.addAttribute("pendingDrivers", driverService.countPending());
        model.addAttribute("approvedDrivers", driverService.countApproved());
        model.addAttribute("onlineDrivers", driverService.countOnline());
        model.addAttribute("availableVehicles", vehicleService.countAvailable());
        model.addAttribute("passengerCount", passengerRepository.count());
        model.addAttribute("bookingCount", bookingRepository.count());
        model.addAttribute("paymentCount", paymentRepository.count());
        model.addAttribute("refundCount", refundRepository.countByStatus(RefundStatus.REQUESTED));
        return "admin/dashboard";
    }

    @GetMapping("/admin/passengers")
    public String passengers(Model model) {
        var administratorEmails = administratorRepository.findAllByOrderByCreatedAtAsc().stream()
                .map(administrator -> administrator.getEmail().toLowerCase(java.util.Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());
        model.addAttribute("passengers", passengerRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(passenger -> !administratorEmails.contains(passenger.getEmail().toLowerCase(java.util.Locale.ROOT)))
                .toList());
        return "admin/passengers";
    }

    @GetMapping("/admin/operators")
    public String operators(Model model) {
        model.addAttribute("operators", administratorRepository.findAllByOrderByCreatedAtAsc());
        model.addAttribute("administratorForm", new AdministratorForm());
        return "admin/operators";
    }

    @PostMapping("/admin/operators")
    public String createOperator(@Valid @ModelAttribute("administratorForm") AdministratorForm form,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            form.setPassword("");
            model.addAttribute("operators", administratorRepository.findAllByOrderByCreatedAtAsc());
            return "admin/operators";
        }
        try {
            adminOperationsService.createAdministrator(form);
            redirectAttributes.addFlashAttribute("notice", "Administrator account created.");
            return "redirect:/admin/operators";
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("administrator.invalid", exception.getMessage());
            form.setPassword("");
            model.addAttribute("operators", administratorRepository.findAllByOrderByCreatedAtAsc());
            return "admin/operators";
        }
    }

    @PostMapping("/admin/operators/{id}/remove")
    public String removeOperator(@PathVariable Long id,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            adminOperationsService.removeAdministrator(id, authentication.getName());
            redirectAttributes.addFlashAttribute("notice", "Administrator access removed.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/operators";
    }

    @GetMapping("/admin/bookings")
    public String bookings(Model model) {
        model.addAttribute("bookings", bookingRepository.findAllByOrderByCreatedAtDesc());
        return "admin/bookings";
    }

    @GetMapping("/admin/payments")
    public String payments(Model model) {
        model.addAttribute("payments", paymentRepository.findAllByOrderByCreatedAtDesc());
        return "admin/payments";
    }

    @GetMapping("/admin/refunds")
    public String refunds(Model model) {
        model.addAttribute("refunds", refundRepository.findAllByOrderByRequestedAtDesc());
        return "admin/refunds";
    }

    @GetMapping("/admin/drivers")
    public String drivers(@org.springframework.web.bind.annotation.RequestParam(required = false) DriverApplicationStatus status,
                          @org.springframework.web.bind.annotation.RequestParam(required = false) String search,
                          Model model) {
        model.addAttribute("drivers", driverService.searchApplications(status, search));
        model.addAttribute("statuses", DriverApplicationStatus.values());
        model.addAttribute("selectedStatus", status == null ? "" : status.name());
        model.addAttribute("search", search == null ? "" : search);
        return "admin/drivers";
    }

    @GetMapping("/admin/drivers/{id}")
    public String driverApplication(@PathVariable Long id, Model model) {
        Driver driver = driverService.findById(id);
        model.addAttribute("driverApplication", driver);
        model.addAttribute("vehicles", vehicleService.findByDriverId(id));
        return "admin/driver-application-detail";
    }

    @GetMapping("/admin/drivers/{id}/documents/{kind}")
    public ResponseEntity<Resource> driverDocument(@PathVariable Long id, @PathVariable String kind) {
        Driver driver = driverService.findById(id);
        var vehicles = vehicleService.findByDriverId(id);
        String key = switch (kind) {
            case "profile" -> driver.getProfilePhotoKey();
            case "license" -> driver.getLicenseImageKey();
            case "vehicle" -> vehicles.stream()
                    .findFirst().map(com.serendib.taxibooking.entity.Vehicle::getPhotoKey).orElse(null);
            case "registration" -> vehicles.stream().findFirst()
                    .map(com.serendib.taxibooking.entity.Vehicle::getRegistrationDocumentKey).orElse(null);
            default -> throw new IllegalArgumentException("The requested driver image is unavailable.");
        };
        Resource resource = driverDocumentStorageService.load(key);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        org.springframework.http.ContentDisposition.inline().filename(key).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .contentType(driverDocumentStorageService.mediaType(key))
                .body(resource);
    }

    @GetMapping("/admin/drivers/new")
    public String newDriver(Model model) {
        if (!model.containsAttribute("driverApplication")) {
            model.addAttribute("driverApplication", new DriverApplicationForm());
        }
        model.addAttribute("editing", false);
        return "admin/driver-form";
    }

    @GetMapping("/admin/drivers/{id}/edit")
    public String editDriver(@PathVariable Long id, Model model) {
        Driver driver = driverService.findById(id);
        DriverApplicationForm form = new DriverApplicationForm();
        form.setName(driver.getName());
        form.setPhone(driver.getPhone());
        form.setEmail(driver.getEmail());
        form.setLicenseNumber(driver.getLicenseNumber());
        model.addAttribute("driverApplication", form);
        model.addAttribute("driverId", id);
        model.addAttribute("editing", true);
        return "admin/driver-form";
    }

    @PostMapping("/admin/drivers")
    public String createDriver(@Valid @ModelAttribute("driverApplication") DriverApplicationForm form,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("editing", false);
            return "admin/driver-form";
        }
        try {
            driverService.apply(form);
            redirectAttributes.addFlashAttribute("notice", "Driver added as pending verification.");
            return "redirect:/admin/drivers";
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("driver.invalid", exception.getMessage());
            model.addAttribute("editing", false);
            return "admin/driver-form";
        }
    }

    @PostMapping("/admin/drivers/{id}")
    public String updateDriver(@PathVariable Long id,
                               @Valid @ModelAttribute("driverApplication") DriverApplicationForm form,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("driverId", id);
            model.addAttribute("editing", true);
            return "admin/driver-form";
        }
        try {
            driverService.update(id, form);
            redirectAttributes.addFlashAttribute("notice", "Driver details updated.");
            return "redirect:/admin/drivers";
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("driver.invalid", exception.getMessage());
            model.addAttribute("driverId", id);
            model.addAttribute("editing", true);
            return "admin/driver-form";
        }
    }

    @PostMapping("/admin/drivers/{id}/approve")
    public String approveDriver(@PathVariable Long id,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            var administrator = administratorRepository.findByEmailIgnoreCase(authentication.getName())
                    .orElseThrow(() -> new IllegalStateException("Administrator account was not found."));
            driverService.approve(id, administrator.getId());
            redirectAttributes.addFlashAttribute("notice", "Driver application approved.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/drivers";
    }

    @PostMapping("/admin/drivers/{id}/reject")
    public String rejectDriver(@PathVariable Long id,
                               @org.springframework.web.bind.annotation.RequestParam String reason,
                               RedirectAttributes redirectAttributes) {
        try {
            driverService.reject(id, reason);
            redirectAttributes.addFlashAttribute("notice", "Application rejected.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/drivers/" + id;
    }

    @PostMapping("/admin/drivers/{id}/resubmission")
    public String requestDriverResubmission(@PathVariable Long id,
                                            @org.springframework.web.bind.annotation.RequestParam String reason,
                                            RedirectAttributes redirectAttributes) {
        try {
            driverService.requestResubmission(id, reason);
            redirectAttributes.addFlashAttribute("notice", "Corrections requested from the driver.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/drivers/" + id;
    }

    @PostMapping("/admin/drivers/{id}/notes")
    public String updateDriverNotes(@PathVariable Long id,
                                    @org.springframework.web.bind.annotation.RequestParam(required = false) String notes,
                                    RedirectAttributes redirectAttributes) {
        try {
            driverService.updateInternalNotes(id, notes);
            redirectAttributes.addFlashAttribute("notice", "Internal notes saved.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/drivers/" + id;
    }

    @PostMapping("/admin/drivers/{id}/suspension")
    public String setDriverSuspension(@PathVariable Long id,
                                      @org.springframework.web.bind.annotation.RequestParam boolean suspended,
                                      RedirectAttributes redirectAttributes) {
        try {
            driverService.setSuspended(id, suspended);
            redirectAttributes.addFlashAttribute("notice",
                    suspended ? "Driver account suspended." : "Driver account reactivated.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/drivers/" + id;
    }

    @PostMapping("/admin/drivers/{id}/online")
    public String toggleDriverOnline(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            driverService.toggleOnline(id);
            redirectAttributes.addFlashAttribute("notice", "Driver availability updated.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/drivers";
    }

    @PostMapping("/admin/drivers/{id}/delete")
    public String deleteDriver(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            driverService.delete(id);
            redirectAttributes.addFlashAttribute("notice", "Driver deleted.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/drivers";
    }

    @GetMapping("/admin/vehicles")
    public String vehicles(Model model) {
        model.addAttribute("vehicles", vehicleService.findAll());
        return "admin/vehicles";
    }

    @GetMapping("/admin/vehicles/new")
    public String newVehicle(Model model) {
        prepareVehicleForm(model, new VehicleForm(), null);
        return "admin/vehicle-form";
    }

    @GetMapping("/admin/vehicles/{id}/edit")
    public String editVehicle(@PathVariable Long id, Model model) {
        var vehicle = vehicleService.findById(id);
        VehicleForm form = new VehicleForm();
        form.setCategory(vehicle.getCategory());
        form.setModel(vehicle.getModel());
        form.setPlateNumber(vehicle.getPlateNumber());
        form.setColor(vehicle.getColor());
        form.setDriverId(vehicle.getDriver() == null ? null : vehicle.getDriver().getId());
        prepareVehicleForm(model, form, id);
        return "admin/vehicle-form";
    }

    @PostMapping("/admin/vehicles")
    public String createVehicle(@Valid @ModelAttribute("vehicleForm") VehicleForm form,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareVehicleForm(model, form, null);
            return "admin/vehicle-form";
        }
        try {
            vehicleService.save(form);
            redirectAttributes.addFlashAttribute("notice", "Vehicle added to the fleet.");
            return "redirect:/admin/vehicles";
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("vehicle.invalid", exception.getMessage());
            prepareVehicleForm(model, form, null);
            return "admin/vehicle-form";
        }
    }

    @PostMapping("/admin/vehicles/{id}")
    public String updateVehicle(@PathVariable Long id,
                                @Valid @ModelAttribute("vehicleForm") VehicleForm form,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareVehicleForm(model, form, id);
            return "admin/vehicle-form";
        }
        try {
            vehicleService.update(id, form);
            redirectAttributes.addFlashAttribute("notice", "Vehicle details updated.");
            return "redirect:/admin/vehicles";
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("vehicle.invalid", exception.getMessage());
            prepareVehicleForm(model, form, id);
            return "admin/vehicle-form";
        }
    }

    @PostMapping("/admin/vehicles/{id}/availability")
    public String toggleVehicleAvailability(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            vehicleService.toggleAvailability(id);
            redirectAttributes.addFlashAttribute("notice", "Vehicle availability updated.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/vehicles";
    }

    @PostMapping("/admin/vehicles/{id}/delete")
    public String deleteVehicle(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            vehicleService.delete(id);
            redirectAttributes.addFlashAttribute("notice", "Vehicle removed from the fleet.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/vehicles";
    }

    private void prepareVehicleForm(Model model, VehicleForm form, Long vehicleId) {
        model.addAttribute("vehicleForm", form);
        model.addAttribute("vehicleCategories", VehicleCategory.values());
        model.addAttribute("drivers", driverService.findApproved());
        model.addAttribute("vehicleId", vehicleId);
    }

}
