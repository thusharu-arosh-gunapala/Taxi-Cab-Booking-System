package com.serendib.taxibooking;

import com.serendib.taxibooking.repository.PassengerRepository;
import com.serendib.taxibooking.repository.AdministratorRepository;
import com.serendib.taxibooking.repository.DriverRepository;
import com.serendib.taxibooking.repository.VehicleRepository;
import com.serendib.taxibooking.repository.DriverPasswordResetTokenRepository;
import com.serendib.taxibooking.dto.PassengerRegistrationForm;
import com.serendib.taxibooking.dto.FavouriteLocationForm;
import com.serendib.taxibooking.dto.DriverApplicationForm;
import com.serendib.taxibooking.dto.DriverRegistrationForm;
import com.serendib.taxibooking.dto.VehicleForm;
import com.serendib.taxibooking.entity.Booking;
import com.serendib.taxibooking.entity.FavouriteLocation;
import com.serendib.taxibooking.entity.BookingStatus;
import com.serendib.taxibooking.entity.Driver;
import com.serendib.taxibooking.entity.Administrator;
import com.serendib.taxibooking.entity.PaymentMethod;
import com.serendib.taxibooking.entity.PaymentStatus;
import com.serendib.taxibooking.entity.Promotion;
import com.serendib.taxibooking.entity.PromotionDiscountType;
import com.serendib.taxibooking.repository.BookingRepository;
import com.serendib.taxibooking.repository.PaymentRepository;
import com.serendib.taxibooking.repository.PromotionRepository;
import com.serendib.taxibooking.repository.ReceiptRepository;
import com.serendib.taxibooking.service.BookingIntegrationService;
import com.serendib.taxibooking.service.FavouriteLocationService;
import com.serendib.taxibooking.service.AdminOperationsService;
import com.serendib.taxibooking.service.DriverService;
import com.serendib.taxibooking.service.VehicleService;
import com.serendib.taxibooking.service.PassengerService;
import com.serendib.taxibooking.service.PaymentService;
import com.serendib.taxibooking.service.RoutingProvider;
import com.serendib.taxibooking.service.MailtrapEmailApiClient;
import com.serendib.taxibooking.model.RouteEstimate;
import com.serendib.taxibooking.model.VehicleCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.MailSendException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.Multipart;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;
import java.util.Base64;
import java.util.Properties;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;

@SpringBootTest(properties = {
        "app.integrations.driver-token=test-integration-token",
        "app.driver.upload-directory=${java.io.tmpdir}/taxi-booking-driver-tests",
        "app.mail.credential-encryption-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "spring.mail.password=test-only-password",
        "spring.mail.host=smtp.test",
        "spring.profiles.active=dev"
})
@AutoConfigureMockMvc
class TaxiBookingWebApplicationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PassengerRepository passengerRepository;

    @Autowired
    private AdministratorRepository administratorRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private DriverPasswordResetTokenRepository driverPasswordResetTokenRepository;

    @Autowired
    private DriverService driverService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private AdminOperationsService adminOperationsService;

    @Autowired
    private PassengerService passengerService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingIntegrationService bookingIntegrationService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private ReceiptRepository receiptRepository;

    @Autowired
    private FavouriteLocationService favouriteLocationService;

    @MockBean
    private RoutingProvider routingProvider;

    @MockBean
    private JavaMailSender mailSender;

    @MockBean
    private MailtrapEmailApiClient mailtrapEmailApiClient;

    @BeforeEach
    void provideRoadRoute() {
        reset(mailSender);
        when(mailSender.createMimeMessage())
                .thenAnswer(ignored -> new MimeMessage(Session.getInstance(new Properties())));
        when(routingProvider.route(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(new RouteEstimate(2_500, 600, List.of(
                        List.of(79.8428, 6.9344), List.of(79.85, 6.928), List.of(79.8562, 6.9219))));
    }

    @Test
    void approvedDriverMustChangeEmailedTemporaryPasswordBeforeDashboardAccess() throws Exception {
        DriverRegistrationForm form = newDriverRegistrationForm("ACT");
        Driver driver = driverService.applyWithVehicle(form);

        mockMvc.perform(post("/admin/drivers/{id}/approve", driver.getId())
                        .with(user("activation-admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        MimeMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getSubject())
                .isEqualTo("Your driver account has been approved");
        String emailText = extractEmailText(sentMessage);
        String temporaryPassword = emailText.lines()
                .filter(line -> line.startsWith("Temporary password: "))
                .map(line -> line.substring("Temporary password: ".length()).trim())
                .findFirst()
                .orElseThrow();
        assertThat(passwordEncoder.matches(temporaryPassword,
                driverRepository.findById(driver.getId()).orElseThrow().getPassword())).isTrue();
        Driver pendingChangeDriver = driverRepository.findById(driver.getId()).orElseThrow();
        assertThat(pendingChangeDriver.isMustChangePassword()).isTrue();
        assertThat(pendingChangeDriver.getCredentialEmailStatus().name()).isEqualTo("SENT");
        assertThatThrownBy(() -> driverService.approve(driver.getId()))
                .isInstanceOf(IllegalStateException.class);

        mockMvc.perform(get("/driver/dashboard").with(user(pendingChangeDriver)))
                .andExpect(status().isForbidden());

        var loginResult = mockMvc.perform(post("/login")
                        .param("username", form.getEmail())
                        .param("password", temporaryPassword)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/driver/change-password"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        mockMvc.perform(post("/driver/change-password")
                        .session(session)
                        .param("password", "SecureDriverPass123")
                        .param("confirmPassword", "SecureDriverPass123")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/driver/dashboard"));

        mockMvc.perform(get("/driver/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("driver/dashboard"));
    }

    @Test
    void driverPasswordResetLinkIsOneTimeAndHashedAtRest() throws Exception {
        Driver driver = driverService.applyWithVehicle(newDriverRegistrationForm("RESET"));
        driverService.approve(driver.getId());
        driverService.changeTemporaryPassword(
                driverRepository.findById(driver.getId()).orElseThrow(), "OriginalDriverPass123");

        reset(mailSender);
        when(mailSender.createMimeMessage())
                .thenAnswer(ignored -> new MimeMessage(Session.getInstance(new Properties())));
        mockMvc.perform(post("/driver/forgot-password")
                        .with(csrf())
                        .param("email", driver.getEmail()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/driver/forgot-password"));

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        String resetUrl = extractEmailText(messageCaptor.getValue()).lines()
                .filter(line -> line.startsWith("http://localhost:8081/driver/reset-password?token="))
                .findFirst()
                .orElseThrow();
        String token = java.net.URI.create(resetUrl).getRawQuery().substring("token=".length());
        assertThat(driverPasswordResetTokenRepository.findAll())
                .hasSize(1)
                .allSatisfy(storedToken -> assertThat(storedToken.getTokenHash()).isNotEqualTo(token));

        mockMvc.perform(post("/driver/reset-password")
                        .with(csrf())
                        .param("token", token)
                        .param("password", "ReplacementDriverPass123")
                        .param("confirmPassword", "ReplacementDriverPass123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?driver"));

        Driver updatedDriver = driverRepository.findById(driver.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("ReplacementDriverPass123", updatedDriver.getPassword())).isTrue();
        assertThat(updatedDriver.isMustChangePassword()).isFalse();
        assertThat(driverPasswordResetTokenRepository.findAll()).isEmpty();
        mockMvc.perform(post("/driver/reset-password")
                        .with(csrf())
                        .param("token", token)
                        .param("password", "AnotherReplacementPass123")
                        .param("confirmPassword", "AnotherReplacementPass123"))
                .andExpect(status().isOk())
                .andExpect(view().name("driver/reset-password"));
    }

    @Test
    void rejectedDriverApplicationDoesNotCreateAnAccount() throws Exception {
        Driver driver = driverService.applyWithVehicle(newDriverRegistrationForm("REJECT"));
        driverService.rejectAndNotify(driver.getId(), "Please provide a current licence image.");

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        String rejectionEmail = extractEmailText(messageCaptor.getValue());
        assertThat(rejectionEmail).contains("REJECTED")
                .contains("Please provide a current licence image.");

        Driver rejected = driverRepository.findById(driver.getId()).orElseThrow();
        assertThat(rejected.getApplicationStatus().name()).isEqualTo("REJECTED");
        assertThat(rejected.getRejectionReason()).isEqualTo("Please provide a current licence image.");
        assertThat(rejected.isApproved()).isFalse();
        assertThat(rejected.isAccountActive()).isFalse();
        assertThat(passwordEncoder.matches("anything", rejected.getPassword())).isFalse();
        assertThatThrownBy(() -> driverService.approve(driver.getId()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void smtpFailureFallsBackToMailtrapApiAndRetryDoesNotRotateCredentials() {
        Driver driver = driverService.applyWithVehicle(newDriverRegistrationForm("MAILFAIL"));
        doThrow(new MailSendException("SMTP unavailable")).when(mailSender).send(any(MimeMessage.class));
        when(mailtrapEmailApiClient.isConfigured()).thenReturn(true);
        when(mailtrapEmailApiClient.send(any(), any(), any(), any())).thenReturn(true);

        driverService.approve(driver.getId());

        Driver deliveredDriver = driverRepository.findById(driver.getId()).orElseThrow();
        assertThat(deliveredDriver.getCredentialEmailStatus().name()).isEqualTo("SENT");
        assertThat(deliveredDriver.isMustChangePassword()).isTrue();
        verify(mailtrapEmailApiClient).send(
                org.mockito.ArgumentMatchers.eq(driver.getEmail()),
                org.mockito.ArgumentMatchers.contains("approved"),
                org.mockito.ArgumentMatchers.contains("Temporary password:"),
                org.mockito.ArgumentMatchers.contains("Temporary password:"));

        String originalPasswordHash = deliveredDriver.getPassword();
        assertThatThrownBy(() -> driverService.resendCredentials(driver.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("delivered");
        assertThat(driverRepository.findById(driver.getId()).orElseThrow().getPassword())
                .isEqualTo(originalPasswordHash);
    }

    @Test
    void failedApprovalEmailRetriesWithTheSamePasswordAndDoesNotCreateAnotherDriver() throws Exception {
        Driver driver = driverService.applyWithVehicle(newDriverRegistrationForm("MAILRETRY"));
        doThrow(new MailSendException("SMTP unavailable")).when(mailSender).send(any(MimeMessage.class));

        driverService.approve(driver.getId());

        ArgumentCaptor<MimeMessage> firstMessage = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(firstMessage.capture());
        String firstPassword = extractTemporaryPassword(firstMessage.getValue());
        Driver pendingDriver = driverRepository.findById(driver.getId()).orElseThrow();
        String storedHash = pendingDriver.getPassword();
        assertThat(pendingDriver.getCredentialEmailStatus().name()).isEqualTo("PENDING");
        long driverCount = driverRepository.count();

        reset(mailSender);
        when(mailSender.createMimeMessage())
                .thenAnswer(ignored -> new MimeMessage(Session.getInstance(new Properties())));
        driverService.resendCredentials(driver.getId());

        ArgumentCaptor<MimeMessage> retryMessage = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(retryMessage.capture());
        Driver retriedDriver = driverRepository.findById(driver.getId()).orElseThrow();
        assertThat(extractTemporaryPassword(retryMessage.getValue())).isEqualTo(firstPassword);
        assertThat(retriedDriver.getPassword()).isEqualTo(storedHash);
        assertThat(retriedDriver.getCredentialEmailStatus().name()).isEqualTo("SENT");
        assertThat(driverRepository.count()).isEqualTo(driverCount);
    }

    @Test
    void developmentEmailTestEndpointRequiresAdminAndSendsValidatedRequest() throws Exception {
        mockMvc.perform(post("/api/email/test")
                        .with(user("email-admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"to":"mailtest@example.com","subject":"Mailtrap SMTP Test","message":"Testing email integration."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("sent"));

        mockMvc.perform(post("/api/email/test")
                        .with(user("email-passenger").roles("PASSENGER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"to":"mailtest@example.com","subject":"Mailtrap SMTP Test","message":"Testing email integration."}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/email/test")
                        .with(user("email-admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"to":"not-an-email","subject":"Mailtrap SMTP Test","message":"Testing email integration."}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void driverApplicationsRejectDuplicateEmailAndNic() {
        DriverRegistrationForm original = newDriverRegistrationForm("DUP");
        driverService.applyWithVehicle(original);

        DriverRegistrationForm duplicateEmail = newDriverRegistrationForm("DUPMAIL");
        duplicateEmail.setEmail(original.getEmail());
        assertThatThrownBy(() -> driverService.applyWithVehicle(duplicateEmail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");

        DriverRegistrationForm duplicateNic = newDriverRegistrationForm("DUPNIC");
        duplicateNic.setNicNumber(original.getNicNumber());
        assertThatThrownBy(() -> driverService.applyWithVehicle(duplicateNic))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("NIC");
    }

    @Test
    void onlyAdminsCanViewPrivateDriverDocuments() throws Exception {
        Driver driver = driverService.applyWithVehicle(newDriverRegistrationForm("DOC"));

        mockMvc.perform(get("/admin/drivers/{id}", driver.getId())
                        .with(user("document-admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/driver-application-detail"));

        mockMvc.perform(get("/admin/drivers/{id}/documents/profile", driver.getId())
                        .with(user("document-admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Cache-Control", "private, no-store"));

        mockMvc.perform(get("/admin/drivers/{id}/documents/profile", driver.getId())
                        .with(user("document-passenger").roles("PASSENGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void driverPortalIsNotAvailableToPassengerAccounts() throws Exception {
        mockMvc.perform(get("/driver/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));

        mockMvc.perform(get("/driver/dashboard").with(user("passenger").roles("PASSENGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void homePageLoads() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    void bookingPagesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/bookings"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void authenticatedPassengerPagesRender() throws Exception {
        PassengerRegistrationForm registration = new PassengerRegistrationForm();
        registration.setFullName("Page Test Passenger");
        registration.setEmail("pages-" + UUID.randomUUID() + "@example.test");
        registration.setPhone("+94771234567");
        registration.setAddress("Colombo");
        registration.setPassword("test-password-123");
        registration.setConfirmPassword("test-password-123");
        var passenger = passengerService.register(registration);

        mockMvc.perform(get("/bookings/new").with(user(passenger)))
                .andExpect(status().isOk())
                .andExpect(view().name("booking/new"));
        mockMvc.perform(get("/passenger/favourites").with(user(passenger)))
                .andExpect(status().isOk())
                .andExpect(view().name("passenger/favourites"));
        mockMvc.perform(get("/trips").with(user(passenger)))
                .andExpect(status().isOk())
                .andExpect(view().name("trip/list"));
        mockMvc.perform(get("/payments").with(user(passenger)))
                .andExpect(status().isOk())
                .andExpect(view().name("payment/list"));
        mockMvc.perform(get("/refunds").with(user(passenger)))
                .andExpect(status().isOk())
                .andExpect(view().name("payment/refunds"));
    }

    @Test
    void passengerCanSaveOnlyOneHomeLocation() {
        PassengerRegistrationForm registration = new PassengerRegistrationForm();
        registration.setFullName("Location Test Passenger");
        registration.setEmail("location-" + UUID.randomUUID() + "@example.test");
        registration.setPhone("+94771234567");
        registration.setAddress("Colombo");
        registration.setPassword("test-password-123");
        registration.setConfirmPassword("test-password-123");
        var passenger = passengerService.register(registration);

        FavouriteLocationForm home = new FavouriteLocationForm();
        home.setType(FavouriteLocation.Type.HOME);
        home.setAddress("Home");
        home.setLatitude(6.9271);
        home.setLongitude(79.8612);
        favouriteLocationService.add(passenger, home);

        assertThatThrownBy(() -> favouriteLocationService.add(passenger, home))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already have a HOME location");
    }

    @Test
    void driverApplicationRequiresAdminVerificationAndAdminRoutesAreProtected() throws Exception {
        String email = "driver-" + UUID.randomUUID() + "@example.test";
        mockMvc.perform(get("/driver/apply"))
                .andExpect(status().isOk())
                .andExpect(view().name("driver/application"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Vehicle details")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("vehicleCategory")));

        mockMvc.perform(driverApplicationRequest("Test Driver", email,
                        "LIC-" + UUID.randomUUID().toString().substring(0, 8),
                        "DRV-" + UUID.randomUUID().toString().substring(0, 8))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/driver/apply"));

        Driver driver = driverRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertThat(driver.isApproved()).isFalse();
        assertThat(driver.isOnline()).isFalse();
        var registeredVehicles = vehicleRepository.findAllByDriverId(driver.getId());
        assertThat(registeredVehicles).hasSize(1);
        assertThat(registeredVehicles.get(0).getModel()).isEqualTo("Toyota Prius");
        assertThat(registeredVehicles.get(0).getCategory()).isEqualTo(VehicleCategory.CAR);
        assertThat(registeredVehicles.get(0).getDriver().getId()).isEqualTo(driver.getId());
        assertThatThrownBy(() -> driverService.toggleOnline(driver.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("account setup");

        mockMvc.perform(get("/admin").with(user("passenger").roles("PASSENGER")))
                .andExpect(status().isForbidden());

        var admin = user("fleet-admin").roles("ADMIN");
        mockMvc.perform(get("/admin").with(admin))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"));
        mockMvc.perform(get("/admin/drivers").with(admin))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/drivers"));
        mockMvc.perform(get("/admin/vehicles/new").with(admin))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/vehicle-form"));

        mockMvc.perform(post("/admin/drivers/{id}/approve", driver.getId())
                        .with(admin)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/drivers"));
        Driver approvedDriver = driverRepository.findById(driver.getId()).orElseThrow();
        driverService.changeTemporaryPassword(approvedDriver, "test-driver-password");
        mockMvc.perform(post("/admin/drivers/{id}/online", driver.getId())
                        .with(admin)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(driverRepository.findById(driver.getId()).orElseThrow().isOnline()).isTrue();
    }

    @Test
    void nearbyDriverMapRequiresPassengerAndUsesFreshAuthenticatedLocations() throws Exception {
        DriverRegistrationForm form = newDriverRegistrationForm("LIVE");
        Driver driver = driverService.applyWithVehicle(form);
        driverService.approve(driver.getId());
        Driver approvedDriver = driverRepository.findById(driver.getId()).orElseThrow();
        driverService.changeTemporaryPassword(approvedDriver, "test-driver-password");
        driverService.toggleOnline(driver.getId());

        mockMvc.perform(post("/api/integrations/drivers/{driverId}/location", driver.getId())
                        .header("X-Integration-Token", "test-integration-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"latitude":6.9271,"longitude":79.8612}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/maps/drivers")
                        .param("latitude", "6.9275")
                        .param("longitude", "79.8615")
                        .param("category", "CAR")
                        .with(user("map-passenger").roles("PASSENGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].latitude").value(6.9271))
                .andExpect(jsonPath("$[0].longitude").value(79.8612))
                .andExpect(jsonPath("$[0].distanceMetres").isNumber());

        mockMvc.perform(get("/api/maps/drivers")
                        .param("latitude", "6.9275")
                        .param("longitude", "79.8615")
                        .param("category", "CAR")
                        .with(user("map-admin").roles("ADMIN")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/integrations/drivers/{driverId}/location", driver.getId())
                        .header("X-Integration-Token", "wrong-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"latitude":6.9271,"longitude":79.8612}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void driverApplicationRejectsDuplicateVehiclePlateWithoutCreatingDriver() throws Exception {
        String plateNumber = "DUP-" + UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(driverApplicationRequest("First Driver", "first-" + UUID.randomUUID() + "@example.test",
                        "LIC-" + UUID.randomUUID().toString().substring(0, 8), plateNumber)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        String secondEmail = "second-" + UUID.randomUUID() + "@example.test";
        mockMvc.perform(driverApplicationRequest("Second Driver", secondEmail,
                        "LIC-" + UUID.randomUUID().toString().substring(0, 8), plateNumber.toLowerCase())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("driver/application"));

        assertThat(driverRepository.findByEmailIgnoreCase(secondEmail)).isEmpty();
    }

    @Test
    void adminCanAssignAndManageVehiclesOnlyForVerifiedDrivers() throws Exception {
        DriverApplicationForm application = new DriverApplicationForm();
        application.setName("Fleet Test Driver");
        application.setPhone("+94771234567");
        application.setEmail("fleet-" + UUID.randomUUID() + "@example.test");
        application.setLicenseNumber("FLEET-" + UUID.randomUUID().toString().substring(0, 8));
        Driver driver = driverService.apply(application);

        VehicleForm vehicleForm = new VehicleForm();
        vehicleForm.setCategory(VehicleCategory.CAR);
        vehicleForm.setModel("Toyota Prius");
        vehicleForm.setPlateNumber("CAR-" + UUID.randomUUID().toString().substring(0, 8));
        vehicleForm.setColor("Silver");
        vehicleForm.setDriverId(driver.getId());

        assertThatThrownBy(() -> vehicleService.save(vehicleForm))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("approved");

        driverService.approve(driver.getId());
        Driver approvedDriver = driverRepository.findById(driver.getId()).orElseThrow();
        driverService.changeTemporaryPassword(approvedDriver, "test-driver-password");
        var admin = user("fleet-admin").roles("ADMIN");
        mockMvc.perform(post("/admin/vehicles")
                        .with(admin)
                        .with(csrf())
                        .param("category", "CAR")
                        .param("model", vehicleForm.getModel())
                        .param("plateNumber", vehicleForm.getPlateNumber())
                        .param("color", vehicleForm.getColor())
                        .param("driverId", driver.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/vehicles"));

        var vehicle = vehicleRepository.findAllByOrderByIdDesc().stream()
                .filter(item -> item.getPlateNumber().equalsIgnoreCase(vehicleForm.getPlateNumber()))
                .findFirst()
                .orElseThrow();
        assertThat(vehicle.isAvailable()).isTrue();
        assertThat(vehicleService.findAvailableForCategory(VehicleCategory.CAR))
                .extracting(item -> item.getId())
                .doesNotContain(vehicle.getId());

        driverService.toggleOnline(driver.getId());
        assertThat(vehicleService.findAvailableForCategory(VehicleCategory.CAR))
                .extracting(item -> item.getId())
                .contains(vehicle.getId());
        assertThatThrownBy(() -> driverService.delete(driver.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("vehicles");

        mockMvc.perform(post("/admin/vehicles/{id}/availability", vehicle.getId())
                        .with(admin)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(vehicleService.findAvailableForCategory(VehicleCategory.CAR))
                .extracting(item -> item.getId())
                .doesNotContain(vehicle.getId());

        mockMvc.perform(get("/admin/vehicles").with(admin))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/vehicles"));
    }

    @Test
    void adminCanReviewOperationsAndManageSeparateOperatorAccounts() throws Exception {
        PassengerRegistrationForm registration = new PassengerRegistrationForm();
        registration.setFullName("Admin Panel Customer");
        registration.setEmail("admin-panel-" + UUID.randomUUID() + "@example.test");
        registration.setPhone("+94771234567");
        registration.setAddress("Colombo");
        registration.setPassword("test-password-123");
        registration.setConfirmPassword("test-password-123");
        var passenger = passengerService.register(registration);
        var admin = user("site-admin").roles("ADMIN");

        mockMvc.perform(get("/admin").with(admin))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"));
        mockMvc.perform(get("/admin/passengers").with(admin))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/passengers"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(passenger.getEmail())));
        mockMvc.perform(get("/admin/bookings").with(admin))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/bookings"));
        mockMvc.perform(get("/admin/payments").with(admin))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/payments"));
        mockMvc.perform(get("/admin/refunds").with(admin))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/refunds"));

        mockMvc.perform(get("/admin/operators").with(admin))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/operators"));

        assertThat(passenger.getAuthorities()).extracting(Object::toString)
                .containsExactly("ROLE_PASSENGER");
        String operatorEmail = "operator-" + UUID.randomUUID() + "@example.test";
        mockMvc.perform(post("/admin/operators")
                        .with(admin)
                        .with(csrf())
                        .param("fullName", "Second Site Operator")
                        .param("email", operatorEmail)
                        .param("password", "new-operator-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/operators"));
        assertThat(administratorRepository.findByEmailIgnoreCase(operatorEmail)).isPresent();
        assertThat(passengerRepository.findById(passenger.getId()).orElseThrow().getRole().name())
                .isEqualTo("PASSENGER");
        Administrator operator = administratorRepository.findByEmailIgnoreCase(operatorEmail).orElseThrow();
        assertThat(operator.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_ADMIN");
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", operatorEmail)
                        .param("password", "new-operator-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", operatorEmail)
                        .param("password", "incorrect-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
        mockMvc.perform(get("/bookings").with(user(operator)))
                .andExpect(status().isForbidden());
        assertThatThrownBy(() -> adminOperationsService.removeAdministrator(
                operator.getId(), "different-operator@example.test"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least one administrator");
        assertThatThrownBy(() -> adminOperationsService.removeAdministrator(
                operator.getId(), operator.getEmail()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("your own");
    }

    @Test
    void passengerAndAdministratorUseTheSameSignInPage() throws Exception {
        mockMvc.perform(get("/admin/login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sign in to Serendib Ride")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Admin sign in"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Administrator sign in"))));
    }

    @Test
    @Transactional
    void firstAdministratorCanBeCreatedFromOneTimeLocalSetupPage() throws Exception {
        administratorRepository.deleteAllInBatch();

        mockMvc.perform(get("/admin/setup"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/admin-setup"));

        String email = "first-admin-" + UUID.randomUUID() + "@example.test";
        mockMvc.perform(post("/admin/setup")
                        .with(csrf())
                        .param("name", "First Admin")
                        .param("email", email)
                        .param("password", "SecureAdminPass123")
                        .param("confirmPassword", "SecureAdminPass123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        var administrator = administratorRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertThat(administrator.getPassword()).isNotEqualTo("SecureAdminPass123");
        assertThat(passwordEncoder.matches("SecureAdminPass123", administrator.getPassword())).isTrue();

        mockMvc.perform(get("/admin/setup"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void firstAdministratorSetupRejectsNonLocalRequests() throws Exception {
        mockMvc.perform(get("/admin/setup").with(request -> {
                    request.setRemoteAddr("203.0.113.10");
                    return request;
                }))
                .andExpect(status().isNotFound());
    }

    @Test
    void mapConfigurationUsesServerProxiedGeoapifyTilesWithoutExposingAnApiKey() throws Exception {
        mockMvc.perform(get("/api/maps/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.tileUrl").value("/api/maps/tiles/{z}/{x}/{y}.png"))
                .andExpect(jsonPath("$.apiKey").doesNotExist());
        mockMvc.perform(get("/api/maps/search").param("q", "Colombo"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/api/maps/tiles/10/800/500.png"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void validPassengerRegistrationPersistsAccount() throws Exception {
        String email = "test-" + UUID.randomUUID() + "@example.test";
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "Test Passenger")
                        .param("email", email)
                        .param("phone", "+94771234567")
                        .param("address", "Colombo")
                        .param("password", "test-password-123")
                        .param("confirmPassword", "test-password-123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        assertThat(passengerRepository.findByEmailIgnoreCase(email)).isPresent();
        assertThat(passengerRepository.findByEmailIgnoreCase(email).orElseThrow().getAddress())
                .isEqualTo("Colombo");
    }

    @Test
    void registrationRejectsMismatchedPasswords() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "Test Passenger")
                        .param("email", "mismatch@example.test")
                        .param("phone", "+94771234567")
                        .param("address", "Colombo")
                        .param("password", "test-password-123")
                        .param("confirmPassword", "different-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));

        assertThat(passengerRepository.findByEmailIgnoreCase("mismatch@example.test")).isEmpty();
    }

    @Test
    void passengerCanSignInWithRegisteredCredentials() throws Exception {
        String email = "login-" + UUID.randomUUID() + "@example.test";
        PassengerRegistrationForm registration = new PassengerRegistrationForm();
        registration.setFullName("Login Test Passenger");
        registration.setEmail(email);
        registration.setPhone("+94771234567");
        registration.setAddress("Colombo");
        registration.setPassword("test-password-123");
        registration.setConfirmPassword("test-password-123");
        passengerService.register(registration);

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", email)
                        .param("password", "test-password-123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings"));
    }

    @Test
    void passengerCanCreateAndCancelOwnBooking() throws Exception {
        PassengerRegistrationForm registration = new PassengerRegistrationForm();
        registration.setFullName("Booking Test Passenger");
        registration.setEmail("booking-" + UUID.randomUUID() + "@example.test");
        registration.setPhone("+94771234567");
        registration.setAddress("Colombo");
        registration.setPassword("test-password-123");
        registration.setConfirmPassword("test-password-123");
        var passenger = passengerService.register(registration);

        mockMvc.perform(get("/bookings/new").with(user(passenger)))
                .andExpect(status().isOk())
                .andExpect(view().name("booking/new"));

        mockMvc.perform(post("/bookings")
                        .with(user(passenger))
                        .with(csrf())
                        .param("pickupAddress", "Colombo Fort")
                        .param("destinationAddress", "Galle Face")
                        .param("pickupLatitude", "6.9344")
                        .param("pickupLongitude", "79.8428")
                        .param("destinationLatitude", "6.9219")
                        .param("destinationLongitude", "79.8562")
                        .param("vehicleCategory", "CAR"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings"));

        var bookings = bookingRepository.findAllByPassengerIdOrderByCreatedAtDesc(passenger.getId());
        assertThat(bookings).hasSize(1);
        assertThat(bookings.get(0).getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(bookings.get(0).getEstimatedFare()).isPositive();
        assertThat(bookings.get(0).getBookingReference()).startsWith("RIDE-");
        assertThat(bookings.get(0).getEstimatedDurationMinutes()).isEqualTo(10);
        mockMvc.perform(get("/bookings").with(user(passenger)))
                .andExpect(status().isOk())
                .andExpect(view().name("booking/list"));

        mockMvc.perform(post("/bookings/{id}/cancel", bookings.get(0).getId())
                        .with(user(passenger))
                        .with(csrf())
                        .param("reason", "Plans changed"))
                .andExpect(status().is3xxRedirection())
                        .andExpect(redirectedUrl("/bookings/" + bookings.get(0).getId()));

        assertThat(bookingRepository.findById(bookings.get(0).getId()).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.CANCELLED);
        assertThat(bookingRepository.findById(bookings.get(0).getId()).orElseThrow().getCancellationReason())
                .isEqualTo("Plans changed");
    }

    @Test
    void completedBookingSupportsDiscountedCashPaymentReceiptAndRefundRequest() throws Exception {
        PassengerRegistrationForm registration = new PassengerRegistrationForm();
        registration.setFullName("Payment Test Passenger");
        registration.setEmail("payment-" + UUID.randomUUID() + "@example.test");
        registration.setPhone("+94771234567");
        registration.setAddress("Colombo");
        registration.setPassword("test-password-123");
        registration.setConfirmPassword("test-password-123");
        var passenger = passengerService.register(registration);
        var booking = bookingRepository.save(new Booking(
                "RIDE-" + UUID.randomUUID(),
                passenger,
                "Colombo Fort",
                "Galle Face",
                6.9344,
                79.8428,
                6.9219,
                79.8562,
                new BigDecimal("2.500"),
                new BigDecimal("3500.00"),
                10,
                "LKR",
                VehicleCategory.CAR,
                false,
                LocalDateTime.now()));
        bookingIntegrationService.beginSearch(booking.getId());
        bookingIntegrationService.assignDriver(booking.getId(), "driver-test-1");
        bookingIntegrationService.respondToAssignment(booking.getId(), true);
        bookingIntegrationService.startTrip(booking.getId());
        bookingIntegrationService.completeTrip(booking.getId());
        mockMvc.perform(get("/bookings/{id}/payment", booking.getId()).with(user(passenger)))
                .andExpect(status().isOk())
                .andExpect(view().name("payment/checkout"));

        String promotionCode = "P" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        promotionRepository.save(new Promotion(
                promotionCode,
                PromotionDiscountType.PERCENTAGE,
                new BigDecimal("10"),
                new BigDecimal("1000"),
                new BigDecimal("500"),
                null,
                1L));
        var payment = paymentService.startPayment(passenger, booking.getId(), PaymentMethod.CASH, promotionCode);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getOriginalAmount()).isEqualByComparingTo("3500.00");
        assertThat(payment.getDiscountAmount()).isEqualByComparingTo("350.00");
        assertThat(payment.getAmount()).isEqualByComparingTo("3150.00");
        assertThatThrownBy(() -> paymentService.startPayment(passenger, booking.getId(), PaymentMethod.CASH, null))
                .isInstanceOf(IllegalStateException.class);
        assertThat(promotionRepository.findById(promotionCode).orElseThrow().getReservedCount()).isEqualTo(1);

        mockMvc.perform(post("/api/integrations/payments/{reference}/cash-collected",
                        payment.getPaymentReference())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"collectionReference\":\"CASH-001\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/integrations/payments/{reference}/cash-collected",
                        payment.getPaymentReference())
                        .header("X-Integration-Token", "test-integration-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"collectionReference\":\"CASH-001\"}"))
                .andExpect(status().isOk());

        var settledPayment = paymentRepository.findByPaymentReference(payment.getPaymentReference()).orElseThrow();
        assertThat(settledPayment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(promotionRepository.findById(promotionCode).orElseThrow().getUsageCount()).isEqualTo(1);
        var receipt = receiptRepository.findByPaymentId(settledPayment.getId()).orElseThrow();
        assertThat(receipt.getReceiptReference()).isNotBlank();
        mockMvc.perform(get("/payments/{reference}", payment.getPaymentReference()).with(user(passenger)))
                .andExpect(status().isOk())
                .andExpect(view().name("payment/detail"));

        var refund = paymentService.requestRefund(passenger, payment.getPaymentReference(), "Duplicate charge");
        assertThat(refund.getStatus().name()).isEqualTo("REQUESTED");
        assertThat(paymentRepository.findByPaymentReference(payment.getPaymentReference()).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.REFUND_REQUESTED);
        mockMvc.perform(get("/receipts/{reference}", receipt.getReceiptReference()).with(user(passenger)))
                .andExpect(status().isOk())
                .andExpect(view().name("payment/receipt"));

        PassengerRegistrationForm otherRegistration = new PassengerRegistrationForm();
        otherRegistration.setFullName("Other Passenger");
        otherRegistration.setEmail("other-" + UUID.randomUUID() + "@example.test");
        otherRegistration.setPhone("+94771234567");
        otherRegistration.setAddress("Colombo");
        otherRegistration.setPassword("test-password-123");
        otherRegistration.setConfirmPassword("test-password-123");
        var otherPassenger = passengerService.register(otherRegistration);
        mockMvc.perform(get("/payments/{reference}", payment.getPaymentReference()).with(user(otherPassenger)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/receipts/{reference}", receipt.getReceiptReference()).with(user(otherPassenger)))
                .andExpect(status().isNotFound());
    }

    @Test
    void fareEstimateApiReturnsEstimateAndValidationMessage() throws Exception {
        mockMvc.perform(post("/api/fares/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pickupLatitude": 6.9344,
                                  "pickupLongitude": 79.8428,
                                  "destinationLatitude": 6.9219,
                                  "destinationLongitude": 79.8562,
                                  "vehicleCategory": "CAR",
                                  "peakHour": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("LKR"))
                .andExpect(jsonPath("$.durationMinutes").value(10))
                .andExpect(jsonPath("$.routeCoordinates").isArray())
                .andExpect(jsonPath("$.estimatedFare").isNumber());

        mockMvc.perform(post("/api/fares/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pickupLatitude": 91,
                                  "pickupLongitude": 0,
                                  "destinationLatitude": 0,
                                  "destinationLongitude": 1,
                                  "vehicleCategory": "CAR",
                                  "peakHour": false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"message\":\"must be less than or equal to 90.0\"}"));
    }

    @Test
    void externalDriverIntegrationEnforcesTokenAndBookingTransitions() throws Exception {
        PassengerRegistrationForm registration = new PassengerRegistrationForm();
        registration.setFullName("Integration Test Passenger");
        registration.setEmail("integration-" + UUID.randomUUID() + "@example.test");
        registration.setPhone("+94771234567");
        registration.setAddress("Colombo");
        registration.setPassword("test-password-123");
        registration.setConfirmPassword("test-password-123");
        var passenger = passengerService.register(registration);

        mockMvc.perform(post("/bookings")
                        .with(user(passenger))
                        .with(csrf())
                        .param("pickupAddress", "Colombo Fort")
                        .param("destinationAddress", "Galle Face")
                        .param("pickupLatitude", "6.9344")
                        .param("pickupLongitude", "79.8428")
                        .param("destinationLatitude", "6.9219")
                        .param("destinationLongitude", "79.8562")
                        .param("vehicleCategory", "CAR"))
                .andExpect(status().is3xxRedirection());

        var booking = bookingRepository.findAllByPassengerIdOrderByCreatedAtDesc(passenger.getId()).get(0);
        String path = "/api/integrations/bookings/" + booking.getId();

        mockMvc.perform(post(path + "/searching"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(path + "/searching")
                        .header("X-Integration-Token", "test-integration-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SEARCHING_FOR_DRIVER"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(path + "/assignment")
                        .header("X-Integration-Token", "test-integration-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"driverReference\":\"external-driver-42\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRIVER_ASSIGNED"));

        mockMvc.perform(post(path + "/driver-response")
                        .header("X-Integration-Token", "test-integration-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accepted\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(path + "/assignment")
                        .header("X-Integration-Token", "test-integration-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"driverReference\":\"external-driver-43\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRIVER_ASSIGNED"));

        mockMvc.perform(post(path + "/driver-response")
                        .header("X-Integration-Token", "test-integration-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accepted\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        mockMvc.perform(post(path + "/arriving")
                        .header("X-Integration-Token", "test-integration-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARRIVING"));

        mockMvc.perform(post(path + "/start")
                        .header("X-Integration-Token", "test-integration-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(post(path + "/complete")
                        .header("X-Integration-Token", "test-integration-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    private DriverRegistrationForm newDriverRegistrationForm(String prefix) {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        DriverRegistrationForm form = new DriverRegistrationForm();
        form.setName(prefix + " Test Driver");
        form.setPhone("+94771234567");
        form.setEmail(prefix.toLowerCase() + "-" + unique + "@example.test");
        form.setNicNumber(randomNic());
        form.setAddress("12 Main Street, Colombo");
        form.setLicenseNumber(prefix + "-" + unique);
        form.setLicenseExpiryDate(java.time.LocalDate.now().plusYears(2));
        form.setVehicleCategory(VehicleCategory.CAR);
        form.setVehicleBrand("Toyota");
        form.setVehicleModel("Test car");
        form.setVehicleYear(2022);
        form.setPlateNumber(prefix + "-" + unique);
        form.setVehicleColor("Blue");
        byte[] image = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/l2cAAAAASUVORK5CYII=");
        form.setProfilePhoto(new MockMultipartFile("profilePhoto", "profile.png", "image/png", image));
        form.setLicenseImage(new MockMultipartFile("licenseImage", "license.png", "image/png", image));
        form.setVehiclePhoto(new MockMultipartFile("vehiclePhoto", "vehicle.png", "image/png", image));
        return form;
    }

    private MockHttpServletRequestBuilder driverApplicationRequest(
            String name, String email, String licenseNumber, String plateNumber) {
        byte[] image = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/l2cAAAAASUVORK5CYII=");
        return multipart("/driver/apply")
                .file(new MockMultipartFile("profilePhoto", "profile.png", "image/png", image))
                .file(new MockMultipartFile("licenseImage", "license.png", "image/png", image))
                .file(new MockMultipartFile("vehiclePhoto", "vehicle.png", "image/png", image))
                .param("name", name)
                .param("phone", "+94771234567")
                .param("email", email)
                .param("nicNumber", randomNic())
                .param("address", "12 Main Street, Colombo")
                .param("licenseNumber", licenseNumber)
                .param("licenseExpiryDate", java.time.LocalDate.now().plusYears(2).toString())
                .param("vehicleCategory", "CAR")
                .param("vehicleBrand", "Toyota")
                .param("vehicleModel", "Toyota Prius")
                .param("vehicleYear", "2022")
                .param("plateNumber", plateNumber)
                .param("vehicleColor", "Silver");
    }

    private String randomNic() {
        long numericValue = Math.abs(UUID.randomUUID().getLeastSignificantBits() % 1_000_000_000_000L);
        return String.format("%012d", numericValue);
    }

    private String extractEmailText(MimeMessage message) throws Exception {
        String text = extractText(message.getContent());
        if (text == null) {
            throw new IllegalStateException("Driver approval email did not contain a readable text body.");
        }
        return text;
    }

    private String extractTemporaryPassword(MimeMessage message) throws Exception {
        return extractEmailText(message).lines()
                .filter(line -> line.startsWith("Temporary password: "))
                .map(line -> line.substring("Temporary password: ".length()).trim())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Driver email did not contain a temporary password."));
    }

    private String extractText(Object content) throws Exception {
        if (content instanceof String text) {
            return text;
        }
        if (content instanceof Multipart multipartContent) {
            for (int index = 0; index < multipartContent.getCount(); index++) {
                String text = extractText(multipartContent.getBodyPart(index).getContent());
                if (text != null) {
                    return text;
                }
            }
        }
        return null;
    }
}
