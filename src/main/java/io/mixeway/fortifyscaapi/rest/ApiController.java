package io.mixeway.fortifyscaapi.rest;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import io.mixeway.fortifyscaapi.db.entity.FortifyScan;
import io.mixeway.fortifyscaapi.db.repository.FortifyScanRepository;
import io.mixeway.fortifyscaapi.pojo.CreateScanRequest;
import io.mixeway.fortifyscaapi.service.FortifyRestApiService;

import java.io.IOException;
import java.util.List;

@Controller
public class ApiController {
    @Autowired
    FortifyRestApiService fortifyRestApiService;
    @Autowired
    FortifyScanRepository fortifyScanRepository;

    @RequestMapping(value="/initialize", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity initializeScanner(){

        return fortifyRestApiService.initialize();
    }

    @ResponseBody
    @RequestMapping(value="/createscan", method=RequestMethod.PUT)
    public FortifyScan createScan(@RequestBody CreateScanRequest createScanRequest) throws IOException, GitAPIException, InterruptedException {
        List<FortifyScan> fortifyScanList = fortifyScanRepository.findByGroupNameAndRunning(createScanRequest.getGroupName(),true);
        if(fortifyScanList.size()>0){
            FortifyScan foultyScan = new FortifyScan();
            foultyScan.setError(true);
            return foultyScan;
        } else {
            FortifyScan fs = fortifyRestApiService.createFortifyScan(createScanRequest);
            fortifyRestApiService.createScanProcess(createScanRequest, fs);
            return fs;
        }
    }
    @ResponseBody
    @RequestMapping(value="/check/{requestId}", method = RequestMethod.GET)
    public FortifyScan check(@PathVariable("requestId") String requestId){
        return fortifyRestApiService.check(requestId);
    }
}
