package edu.asu.diging.gilesecosystem.cepheus.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import edu.asu.diging.gilesecosystem.cepheus.service.IKafkaListenerManager;

@Controller
public class KafkaListenerController {
    
    @Autowired
    private IKafkaListenerManager listenerManager;

    @RequestMapping(value="/admin/kafka/listeners")
    public String showPage(Model model) {
        model.addAttribute("listenerStatus", listenerManager.isListening());
        return "admin/kafka/listeners";
    }
    
    @RequestMapping(value="/admin/kafka/listeners/start", method=RequestMethod.POST)
    public String startListeners() {
        listenerManager.startListeners();
        return "redirect:/admin/kafka/listeners";
    }
    
    @RequestMapping(value="/admin/kafka/listeners/stop", method=RequestMethod.POST)
    public String stopListeners() {
        listenerManager.shutdownListeners();
        return "redirect:/admin/kafka/listeners";
    }
}
