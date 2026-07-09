# Supplemental Requirement 13: Shorten startup time

1. Upgrade to Java 25, including Dockerfile (Java 25 is now the default JDK installed on this Mac)

2. Utilize AOT cache to cut startup time.  I only deploy this microservice as a container in Kubernetes.  If it makes sense, use the following buildpack approach:
```groovy
bootBuildImage {
    builder = 'paketobuildpacks/builder-noble-java-tiny'
    environment = [
        'BP_JVM_VERSION': '25',
        'BP_JVM_AOTCACHE_ENABLED': 'true'
    ]
}
```

3. Implement Spring AOT processing

Notes:

- I use the file `kbuild.sh` in the root of the project to perform a multiarchitecture build.  
- I deploy in Kubernetes in AWS.  Don't try running locally (on this machine) and expect to connect to a database.
- Don't change resource allocations in the Kubernetes manifests.  
- I will handle testing in Kubernetes and report results.
