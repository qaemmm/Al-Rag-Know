package org.gwh.api;

import org.gwh.api.response.Response;

import java.util.concurrent.CompletableFuture;

public interface IGitService {

    CompletableFuture<Response<String>> analyzeGitRepositoryAsync(String repoUrl, String userName, String token) ;
}
