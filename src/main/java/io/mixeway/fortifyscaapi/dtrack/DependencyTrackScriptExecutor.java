package io.mixeway.fortifyscaapi.dtrack;

import io.mixeway.fortifyscaapi.pojo.CreateScanRequest;
import io.mixeway.fortifyscaapi.pojo.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

public class DependencyTrackScriptExecutor {
    private Logger logger = LoggerFactory.getLogger(DependencyTrackScriptExecutor.class);

    public void runDTrackScript(CreateScanRequest createScanRequest, Project project, String script, Path path) throws IOException {
        logger.info("Starting to generate BOM for {}", project.getProjectName());

        ProcessBuilder builder = new ProcessBuilder();
        builder.command("/bin/sh", script,project.getdTrackUuid(), createScanRequest.getdTrackUrl(), createScanRequest.getdTrackToken());
        logger.info("Running {} inside {}",String.join(" ",
                Arrays.asList("sh", "-c", script,project.getdTrackUuid(), createScanRequest.getdTrackUrl(), createScanRequest.getdTrackToken())),
                path.toFile());
        builder.directory(path.toFile());
        Process process = builder.start();
    }


}
