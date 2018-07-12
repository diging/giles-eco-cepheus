package edu.asu.diging.gilesecosystem.cepheus.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import edu.asu.diging.gilesecosystem.cepheus.service.progress.IProgressManager;

@Controller
public class ProgressController {
    
    @Autowired
    private IProgressManager progressManager;

    @RequestMapping(value="/admin/requests/progress")
    public String showProgress(Model model) {
        model.addAttribute("request", progressManager.getCurrentRequest());
        model.addAttribute("status", progressManager.getPhase());
        
        int total = progressManager.getTotalPages();
        int current = progressManager.getCurrentPage();
        
        if (total > 0) {
            float progress = new Float(current)/new Float(total) * 100;
            model.addAttribute("progress", Math.round(progress));
        }
        
        return "admin/requests/progress";
    }
}
