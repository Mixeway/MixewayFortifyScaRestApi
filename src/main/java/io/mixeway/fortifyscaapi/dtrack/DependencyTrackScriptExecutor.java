package io.mixeway.fortifyscaapi.dtrack;

import io.mixeway.fortifyscaapi.pojo.CreateScanRequest;
import io.mixeway.fortifyscaapi.pojo.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class DependencyTrackScriptExecutor {
    private Logger logger = LoggerFactory.getLogger(DependencyTrackScriptExecutor.class);

    @Value("${code.base.location}")
    private String location;

    public void runDTrackScript(CreateScanRequest createScanRequest, Project project, String script) throws IOException {
        logger.info("Starting to generate BOM for {}", project.getProjectName());
        Map<String,String> projects  = new HashMap<>();
        projects.put("test",null);
        if (project.getParams() != null && project.getParams() != ""){
            projects.clear();
            for (String param : project.getParams().split(",")){
                projects.put(param.split("-")[0],param.split("-")[1]);
            }
        }
        for (Map.Entry<String,String> entry : projects.entrySet()) {
            String pathToLocation = location + (project.getProjectName() != null ? project.getProjectName() : "")
                    + (entry.getValue()!=null? "/"+entry.getValue() : "");
            ProcessBuilder builder = new ProcessBuilder();
            builder.command("sh", "-c", script,project.getdTrackUuid(), createScanRequest.getdTrackUrl(), createScanRequest.getdTrackUrl());
            builder.directory(Paths.get(pathToLocation).toFile());
            Process process = builder.start();
        }
    }


}
