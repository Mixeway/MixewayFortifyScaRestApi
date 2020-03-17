package io.mixeway.fortifyscaapi.git;

import io.mixeway.fortifyscaapi.pojo.CreateScanRequest;
import io.mixeway.fortifyscaapi.pojo.GitResponse;
import io.mixeway.fortifyscaapi.pojo.Project;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GitClient {
    private Logger logger = LoggerFactory.getLogger(GitClient.class);

    public GitResponse pull(CreateScanRequest createScanRequest, Project project, Path path){
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            CredentialsProvider credentialsProvider = prepareCredentails(createScanRequest);

            Repository repository = builder.setGitDir(Paths.get(path.toString() + "/.git").toFile())
                    .readEnvironment()
                    .findGitDir()
                    .build();
            Git git = new Git(repository);
            git.reset().setMode(ResetCommand.ResetType.HARD).call();
            git.pull()
                    .setRemote("origin")
                    .setStrategy(MergeStrategy.THEIRS)
                    .setCredentialsProvider(credentialsProvider)
                    .setRemoteBranchName(project.getBranch())
                    .call();
            Ref call = git.checkout().setName("origin/" + project.getBranch()).call();
            String commitId = git.log().setMaxCount(1).call().iterator().next().getName();
            logger.info("Successfully fetched repo for {} commid id is {} branch {}", project.getProjectName(), commitId, project.getBranch());
            return new GitResponse(true,commitId);
        } catch (GitAPIException | IOException e){
            logger.error("Error during fetching repo {}", e.getLocalizedMessage());
        }
        return new GitResponse(false,"");
    }
    public GitResponse clone(CreateScanRequest createScanRequest, Project project, Path path){
        try {
            Git git = Git.cloneRepository()
                    .setCredentialsProvider(prepareCredentails(createScanRequest))
                    .setURI(project.getProjectRepoUrl())
                    .setDirectory(path.toFile())
                    .call();
            String commitId = git.log().setMaxCount(1).call().iterator().next().getName();
            return new GitResponse(true, commitId);
        } catch (GitAPIException e){
            logger.error("Error cloning fetching repo {}", e.getLocalizedMessage());
        }
        return new GitResponse(false, "");
    }
    private CredentialsProvider prepareCredentails(CreateScanRequest createScanRequest) {
        if (createScanRequest.getUsername() ==null && createScanRequest.getPassword()!=null) {
            return new UsernamePasswordCredentialsProvider("PRIVATE-TOKEN", createScanRequest.getPassword());
        } else if (createScanRequest.getUsername() !=null && createScanRequest.getPassword()!=null ) {
            return new UsernamePasswordCredentialsProvider(createScanRequest.getUsername(), createScanRequest.getPassword());
        } else {
            return null;
        }
    }


}
