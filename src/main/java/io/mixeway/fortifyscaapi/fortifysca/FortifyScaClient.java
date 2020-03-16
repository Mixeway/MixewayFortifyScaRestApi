package io.mixeway.fortifyscaapi.fortifysca;

import io.mixeway.fortifyscaapi.db.entity.FortifyScan;
import io.mixeway.fortifyscaapi.db.repository.FortifyScanRepository;
import io.mixeway.fortifyscaapi.dtrack.DependencyTrackScriptExecutor;
import io.mixeway.fortifyscaapi.pojo.CreateScanRequest;
import io.mixeway.fortifyscaapi.pojo.Project;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Component
public class FortifyScaClient {
    private Logger logger = LoggerFactory.getLogger(FortifyScaClient.class);
    private final FortifyScanRepository fortifyScanRepository;
    private DependencyTrackScriptExecutor dependencyTrackScriptExecutor = new DependencyTrackScriptExecutor();
    @Autowired
    FortifyScaClient (FortifyScanRepository fortifyScanRepository){
        this.fortifyScanRepository = fortifyScanRepository;
    }

    @Value("${code.base.location}")
    private String location;
    @Value("${dependencytrack.script.mvn}")
    private String dTrackMvn;
    @Value("${dependencytrack.script.python}")
    private String dTrackPy;
    @Value("${dependencytrack.script.js}")
    private String dTrackJs;
    @Value("${dependencytrack.script.php}")
    private String dTrackPHP;

    private String sourceAnalyzerForJs(String groupName){
        return "sourceanalyzer -b "+groupName+" **/*.tsx -Dcom.fortify.sca.Phase0HigherOrder.Languages=javascript,typescript -Dcom.fortify.sca.EnableDOMModeling=true -Dcom.fortify.sca.TypeInferenceLanguages=tytypescript,javascript && " +
                "sourceanalyzer -b "+groupName+" **/*.ts -Dcom.fortify.sca.Phase0HigherOrder.Languages=javascript,typescript -Dcom.fortify.sca.EnableDOMModeling=true -Dcom.fortify.sca.TypeInferenceLanguages=tytypescript,javascript && " +
                "sourceanalyzer -b "+groupName+" **/*.js -Dcom.fortify.sca.Phase0HigherOrder.Languages=javascript,typescript -Dcom.fortify.sca.EnableDOMModeling=true -Dcom.fortify.sca.TypeInferenceLanguages=tytypescript,javascript";
    }
    private String sourceAnalyzerForMaven(String groupName){
        return "sourceanalyzer -b "+groupName+" mvn -DskipTests";
    }
    private String sourceAnalyzerForPHP(String groupName){
        return "sourceanalyzer -b "+groupName+" -exclude vendor -php-source-root . \"**/*.php\" && " +
                "sourceanalyzer -b "+groupName+" **/*.js -Dcom.fortify.sca.Phase0HigherOrder.Languages=javascript,typescript -Dcom.fortify.sca.EnableDOMModeling=true -Dcom.fortify.sca.TypeInferenceLanguages=tytypescript,javascript";

    }
    private String sourceAnalyzerForPython(String groupName){
        return "sourceanalyzer -b "+groupName+" -python-version 3 -python-path /usr/lib/python3.6/site-packages **/*.py";
    }
    private String sourceAnalyzerForAnt(String groupName){
        return "sourceanalyzer -b "+groupName+" ant";
    }

    private String sourceAnalyzerClean(String groupName){
        return "sourceanalyzer -b "+groupName+" -clean";
    }
    private String sourceAnalyzerSendToCloudScan(String groupName, int versionId, String cloudCtrlToken, String sscUrl){
        return "cloudscan -sscurl "+sscUrl+" -ssctoken "+cloudCtrlToken+" start -upload -versionid "+versionId+" -b "+groupName+" -uptoken "+cloudCtrlToken+" -scan";
    }
    public void runCleanForProject(CreateScanRequest createScanRequest, FortifyScan fortifyScan) throws IOException {
        executeCommand(createScanRequest, new Project(), sourceAnalyzerClean(createScanRequest.getGroupName()), fortifyScan);
    }
    public void runCloudScanSendScan(CreateScanRequest createScanRequest, FortifyScan fortifyScan) throws IOException {
        executeCommand(createScanRequest,
                new Project(),
                sourceAnalyzerSendToCloudScan(createScanRequest.getGroupName(),
                        createScanRequest.getVersionId(),
                        createScanRequest.getCloudCtrlToken(),
                        createScanRequest.getSscUrl()),
                fortifyScan);
    }
    public void runTranslateForRequest(CreateScanRequest createScanRequest, Project project, FortifyScan fortifyScan) throws IOException, InterruptedException {
        for (String tech : project.getTechnique().split(",")) {
            switch (tech) {
                case "PHP":
                    executeCommand(createScanRequest, project, sourceAnalyzerForPHP(createScanRequest.getGroupName()), fortifyScan);
                    if (project.getdTrackUuid() != null) {
                        dependencyTrackScriptExecutor.runDTrackScript(project,dTrackPHP);
                    }
                    break;
                case "JS":
                    executeCommand(createScanRequest, project, sourceAnalyzerForJs(createScanRequest.getGroupName()), fortifyScan);
                    if (project.getdTrackUuid() != null) {
                        dependencyTrackScriptExecutor.runDTrackScript(project,dTrackJs);
                    }
                    break;
                case "MVN":
                    executeCommand(createScanRequest, project, sourceAnalyzerForMaven(createScanRequest.getGroupName()), fortifyScan);
                    if (project.getdTrackUuid() != null) {
                        dependencyTrackScriptExecutor.runDTrackScript(project,dTrackMvn);
                    }
                    break;
                case "ANT":
                    executeCommand(createScanRequest, project, sourceAnalyzerForAnt(createScanRequest.getGroupName()), fortifyScan);
                    //no dTrack
                    break;
                case "PYTHON":
                    executeCommand(createScanRequest, project, sourceAnalyzerForPython(createScanRequest.getGroupName()), fortifyScan);
                    if (project.getdTrackUuid() != null) {
                        dependencyTrackScriptExecutor.runDTrackScript(project,dTrackPy);
                    }
                    break;
                default:
                    logger.warn("Unknown technique {}", project.getTechnique());
                    break;
            }
        }
    }

    private void executeCommand(CreateScanRequest createScanRequest, Project project, String command, FortifyScan fs) throws IOException {
        Map<String,String> projects  = new HashMap<>();
        projects.put("test",null);
        if (project!=null && project.getParams()!=null && !project.getParams().equals("")){
            projects.clear();
            for (String param : project.getParams().split(",")){
                projects.put(param.split("-")[0],param.split("-")[1]);
            }
        }

        for (Map.Entry<String,String> entry : projects.entrySet()) {
            assert project != null;
            String pathToLocation = location + (project.getProjectName() != null ? project.getProjectName() : "")
                    + (entry.getValue()!=null? "/"+entry.getValue() : "");
            logger.info("Running command '{}' inside '{}' directory ", command, pathToLocation);
            ProcessBuilder builder = new ProcessBuilder();
            builder.command("sh", "-c", command);
            builder.directory(Paths.get(pathToLocation).toFile());
            Process process = builder.start();
            String output = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
            if (output.contains("Submitted job and received token: ")) {
                fs.setScanId(output.split(": {2}")[1]);
                fs.setRunning(false);
                fortifyScanRepository.save(fs);
                process.destroy();
            }
            if (output.contains("Shutting down with errors.")){
                logger.error("Error during cludscansending ...");
                fs.setRunning(false);
                fs.setError(true);
                fortifyScanRepository.save(fs);
                process.destroy();
            }
        }
    }
}
