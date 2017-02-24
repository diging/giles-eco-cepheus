package edu.asu.diging.gilesecosystem.cepheus.web;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import edu.asu.diging.gilesecosystem.cepheus.exceptions.CepheusPropertiesStorageException;
import edu.asu.diging.gilesecosystem.cepheus.service.IPropertiesManager;
import edu.asu.diging.gilesecosystem.cepheus.web.pages.SystemConfigPage;
import edu.asu.diging.gilesecosystem.cepheus.web.validators.SystemConfigValidator;

@Controller
public class EditPropertiesController {
    
    @Autowired
    private IPropertiesManager propertyManager;
    
    @InitBinder
    protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder, WebDataBinder validateBinder) {
        validateBinder.addValidators(new SystemConfigValidator());
    }

    @RequestMapping(value = "/admin/system/config", method = RequestMethod.GET)
    public String getConfigPage(Model model) {
        SystemConfigPage page = new SystemConfigPage();
        
        page.setGilesAccessToken(propertyManager.getProperty(IPropertiesManager.GILES_ACCESS_TOKEN));
        page.setBaseUrl(propertyManager.getProperty(IPropertiesManager.CEPHEUS_URL));
        page.setNepomukAccessToken(propertyManager.getProperty(IPropertiesManager.NEPOMUK_ACCESS_TOKEN));
        
        model.addAttribute("systemConfigPage", page);
        return "admin/system/config";
    }
    
    @RequestMapping(value = "/admin/system/config", method = RequestMethod.POST)
    public String storeSystemConfig(@Validated @ModelAttribute SystemConfigPage systemConfigPage, BindingResult results, Model model, RedirectAttributes redirectAttrs) {
        model.addAttribute("systemConfigPage", systemConfigPage);
        
        if (results.hasErrors()) {
            model.addAttribute("show_alert", true);
            model.addAttribute("alert_type", "danger");
            model.addAttribute("alert_msg", "System Configuration could not be saved. Please check the error messages below.");
            return "admin/system/config";
        }
        
        Map<String, String> propertiesMap = new HashMap<String, String>();
        propertiesMap.put(IPropertiesManager.GILES_ACCESS_TOKEN, systemConfigPage.getGilesAccessToken());
        propertiesMap.put(IPropertiesManager.CEPHEUS_URL, systemConfigPage.getBaseUrl());
        propertiesMap.put(IPropertiesManager.NEPOMUK_ACCESS_TOKEN, systemConfigPage.getNepomukAccessToken());
        
        try {
            propertyManager.updateProperties(propertiesMap);
        } catch (CepheusPropertiesStorageException e) {
            model.addAttribute("show_alert", true);
            model.addAttribute("alert_type", "danger");
            model.addAttribute("alert_msg", "An unexpected error occurred. System Configuration could not be saved.");
            return "admin/system/config";
        }
        
        redirectAttrs.addFlashAttribute("show_alert", true);
        redirectAttrs.addFlashAttribute("alert_type", "success");
        redirectAttrs.addFlashAttribute("alert_msg", "System Configuration was successfully saved.");
        
        return "redirect:/admin/system/config";
    }
}
