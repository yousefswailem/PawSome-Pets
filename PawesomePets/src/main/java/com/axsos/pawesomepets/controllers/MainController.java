package com.axsos.pawesomepets.controllers;

import java.security.Principal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.axsos.pawesomepets.models.Appointment;
import com.axsos.pawesomepets.models.PService;
import com.axsos.pawesomepets.models.ServicehasPet;
import com.axsos.pawesomepets.models.User;
import com.axsos.pawesomepets.services.AppointmentService;
import com.axsos.pawesomepets.services.CategoryService;
import com.axsos.pawesomepets.services.PServiceService;
import com.axsos.pawesomepets.services.ServicehasPetService;
import com.axsos.pawesomepets.services.ServicehasPethasAppointmentService;
import com.axsos.pawesomepets.services.UserService;
import com.axsos.pawesomepets.validator.UserValidator;

@Controller
public class MainController {
	private UserService userService;
	private UserValidator userValidator;
	private final CategoryService categoryService;
	private final PServiceService pserviceService;
	private final ServicehasPetService servicehasPetService;
	private final AppointmentService appointmentService;
	private final ServicehasPethasAppointmentService servicehasPethasAppointmentService;

	public MainController(UserService userService, UserValidator userValidator, CategoryService categoryService,
			PServiceService pserviceService, ServicehasPetService servicehasPetService,
			AppointmentService appointmentService,
			ServicehasPethasAppointmentService servicehasPethasAppointmentService) {
		this.userService = userService;
		this.userValidator = userValidator;
		this.categoryService = categoryService;
		this.pserviceService = pserviceService;
		this.servicehasPetService = servicehasPetService;
		this.appointmentService = appointmentService;
		this.servicehasPethasAppointmentService = servicehasPethasAppointmentService;
	}

	// ******************************************************************************
	// This method is only commented when you want to comment out the following
	// method
//	@PostMapping("/registration")
//	public String registration(@Valid @ModelAttribute("user") User user, BindingResult result, Model model) {
//		userValidator.validate(user, result);
//		if (result.hasErrors()) {
//			return "logreg.jsp";
//		}
//
//		userService.saveWithUserRole(user);
//		return "redirect:/login";
//	}
	// ******************************************************************************
	// ******************************************************************************
	// This method is only commented out when you want to add an admin, and the
	// previous method shall be commented
	@PostMapping("/registration")
	public String registration(@Valid @ModelAttribute("user") User user, BindingResult result, Model model) {
		userValidator.validate(user, result);
		if (result.hasErrors()) {
			return "logreg.jsp";
		}
		userService.saveUserWithAdminRole(user);
		return "redirect:/login";
	}
	// ******************************************************************************

	@RequestMapping("/login")
	public String login(@RequestParam(value = "error", required = false) String error,
			@RequestParam(value = "logout", required = false) String logout, Model model,
			@Valid @ModelAttribute(value = "user") User user, BindingResult result) {
		if (error != null) {
			model.addAttribute("errorMessage", "Invalid Credentials, Please try again.");
		}
		if (logout != null) {
			model.addAttribute("logoutMessage", "Logout Successful!");
		}
		return "logreg.jsp";
	}

	@RequestMapping("/admin")
	public String adminPage(Principal principal, Model model) {
		String username = principal.getName();
		model.addAttribute("currentUser", userService.findByUsername(username));
		return "adminPage.jsp";
	}

	@RequestMapping(value = "/admin/createCategory", method = RequestMethod.POST)
	public String createCategoryProcess(Model model, @RequestParam(value = "type") String type) {
		if (type.length() < 2 || type.length() > 10) {
			model.addAttribute("addingCategoriesErrorMessage", "Category must be between 2 and 10");
			return "adminPage.jsp";
		} else {
			categoryService.createCategory(type);
			return "redirect:/admin";
		}
	}

	@RequestMapping(value = "/admin/createPService", method = RequestMethod.POST)
	public String createServiceProcess(Model model, @RequestParam("name") String name) {
		if (name.length() < 2 || name.length() > 10) {
			model.addAttribute("addingPServicesErrorMessage", "Service must be between 2 and 10");
			return "adminPage.jsp";
		} else {
			pserviceService.createPService(name);
			return "redirect:/admin";
		}
	}

	@RequestMapping(value = "/admin/createAppointment", method = RequestMethod.POST)
	public String createAppointmentProcess(Model model, @RequestParam("appointment") Date appointment)
			throws ParseException {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Calendar cal = Calendar.getInstance();
		String today = dateFormat.format(cal.getTime());
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		Date todayDate = formatter.parse(today);
		if (appointment.compareTo(todayDate) < 0) {
			model.addAttribute("addingAppointmentsErrorMessage", "Appointment must be a future date!");
			return "adminPage.jsp";
		} else {
			appointmentService.createAppointment(appointment);
			return "redirect:/admin";
		}
	}

	@InitBinder
	protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		dateFormat.setLenient(false);
		binder.registerCustomEditor(Date.class, null, new CustomDateEditor(dateFormat, true));
	}

	@RequestMapping("/apply")
	public String apply(Model model, Principal principal) {
		String currentEmail = principal.getName();
		User currentUser = userService.findByUsername(currentEmail);
		model.addAttribute("pets", currentUser.getPets());

		List<PService> allPServices = pserviceService.findAll();
		model.addAttribute("allPServices", allPServices);

		List<Appointment> allAppointments = appointmentService.findAll();
		model.addAttribute("allAppointments", allAppointments);
		return "apply.jsp";
	}

	@RequestMapping(value = "/applyProcess", method = RequestMethod.POST)
	public String applyProcess(@RequestParam("petId") Long petId, @RequestParam("serviceId") Long serviceId,
			@RequestParam("appointmentId") Long appointmentId) {
		servicehasPetService.fillTable(petId, serviceId);
		ServicehasPet myServiceHasPetService = servicehasPetService.findByTwoIds(petId, serviceId);
		servicehasPethasAppointmentService.fillTable(myServiceHasPetService.getId(), appointmentId);
		return "redirect:/apply";
	}

	@RequestMapping(value = { "/", "/home" })
	public String home(Principal principal, Model model) {
		String username = principal.getName();
		model.addAttribute("currentUser", userService.findByUsername(username));
		return "homePage.jsp";
	}

	@RequestMapping("/about")
	public String aboutUs() {
		return "aboutus.jsp";
	}

	@RequestMapping("/ourteam")
	public String ourTeam() {
		return "ourteam.jsp";
	}

	@RequestMapping("/editservice")
	public String editService() {
		return "editService.jsp";
	}

	@RequestMapping("/editcategory")
	public String editCategory() {
		return "editCategory.jsp";
	}

	@RequestMapping("/editappointment")
	public String editAppointment() {
		return "editAppointment.jsp";
	}

	@RequestMapping("/services")
	public String services() {
		return "services.jsp";
	}
	
//	@RequestMapping("/test")
//	public String test() {
//		return "serviceInfo.jsp";
//	}
	
}