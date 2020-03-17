package io.mixeway.fortifyscaapi.service;

import io.mixeway.fortifyscaapi.db.repository.FortifyScanRepository;
import io.mixeway.fortifyscaapi.fortifysca.FortifyScaClient;
import io.mixeway.fortifyscaapi.git.GitClient;
import io.mixeway.fortifyscaapi.pojo.CreateScanRequest;
import io.mixeway.fortifyscaapi.pojo.GitResponse;
import io.mixeway.fortifyscaapi.pojo.Project;
;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import io.mixeway.fortifyscaapi.db.entity.FortifyScan;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class FortifyRestApiService {
    private Logger logger = LoggerFactory.getLogger(FortifyRestApiService.class);

    private final FortifyScanRepository fortifyScanRepository;
    private final FortifyScaClient fortifyScaClient;
    private GitClient gitClient = new GitClient();
    @Autowired
    FortifyRestApiService (FortifyScanRepository fortifyScanRepository, FortifyScaClient fortifyScaClient){
        this.fortifyScanRepository = fortifyScanRepository;
        this.fortifyScaClient = fortifyScaClient;
    }

    @Value("${code.base.location}")
    private String location;

    public ResponseEntity<?> initialize(){
        Path path = Paths.get(location);
        if (Files.exists(path)) {
            logger.info("Location exists");
            return new ResponseEntity<>(null,HttpStatus.OK);
        } else{
            File file = new File(location);
            if (!file.exists()) {
                if (file.mkdir()) {
                    logger.info("Directory is created! {}", location);
                    return new ResponseEntity<>(null,HttpStatus.CREATED);
                } else {
                    logger.warn("Failed to create directory!");
                    return new ResponseEntity<>(null,HttpStatus.CONFLICT);                }
            }
        }
        return null;
    }

    @Async
    public void createScanProcess(CreateScanRequest createScanRequest, FortifyScan fortifyScan) {
        GitResponse gitResponse = null;
        try {
            if (fortifyScanRepository.findByGroupNameAndRunning(createScanRequest.getGroupName(), true).size() != 0) {
                //fortifyScaClient.runCleanForProject(createScanRequest,fortifyScan);
                for (Project project : createScanRequest.getProjects()) {
                    logger.info("Starting processing of app {} in {} dtrackuuid is {} ", project.getProjectName(), project.getTechnique(),
                            project.getdTrackUuid()!=null ? project.getdTrackUuid() : "empty");
                    Path path = Paths.get(location + project.getProjectName());
                    if (Files.exists(path)) {
                        //git fetch
                        gitResponse = gitClient.pull(createScanRequest,project, path);
                        if (!gitResponse.getStatus())
                            throw new Exception("Some kind of error during pulling repo for " + project.getProjectName());
                    } else {
                        //git clone
                        gitResponse = gitClient.clone(createScanRequest,project, path);
                        if (!gitResponse.getStatus())
                            throw new Exception("Some kind of error during cloning repo for " + project.getProjectName());
                    }
                    fortifyScaClient.runTranslateForRequest(createScanRequest,project,fortifyScan);
                }
                assert gitResponse != null;
                fortifyScan.setCommitid(gitResponse.getCommitId());
                fortifyScanRepository.save(fortifyScan);
                fortifyScaClient.runCloudScanSendScan(createScanRequest,fortifyScan);
            } else {
                logger.warn("Trying to create scan for already running group {} aborting.", createScanRequest.getGroupName());
            }
        } catch (Exception ex){
            ex.printStackTrace();
            logger.error("Somekind of error during scan for {} {}", fortifyScan.getGroupName(), ex.getLocalizedMessage());
            fortifyScan.setError(true);
            fortifyScanRepository.save(fortifyScan);
        }
    }

    public FortifyScan createFortifyScan(CreateScanRequest csr) {
        FortifyScan fs = new FortifyScan();
        fs.setError(false);
        fs.setGroupName(csr.getGroupName());
        fs.setRunning(true);
        if (csr.getProjects().size() == 1 && csr.getSingle()) {
            fs.setProjectName(csr.getProjects().get(0).getProjectName());
            logger.info("Get single request for {}", fs.getProjectName());
        }
        fs.setRequestId(UUID.randomUUID().toString());
        fortifyScanRepository.save(fs);

        return fs;
    }
    public FortifyScan check(String requestid){
        Optional<FortifyScan> fs = fortifyScanRepository.findByRequestId(requestid);
        if ( fs.isPresent() && fs.get().getScanId() != null && !fs.get().getScanId().equals("")) {
            fs.get().setRunning(false);
            fortifyScanRepository.save(fs.get());
            fortifyScanRepository.delete(fs.get());
            logger.info("Mixer is asking for done job {} - removing",requestid);
        } else if (fs.isPresent() && fs.get().getError()){
            fortifyScanRepository.delete(fs.get());
            logger.info("Mixer is asking for errored job {} - removing",requestid);
        }
        FortifyScan foultyScan = new FortifyScan();
        foultyScan.setError(true);
        return fs.orElse(foultyScan);
    }
}
