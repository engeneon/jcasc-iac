# Development Setup for Azure CAF Infrastructure Code

* Guide for setting up basic development environment for the Azure Enterprise Codebase

## Overview

*Scenario #1: Bootstrap/Fresh Start (New Tenant)*
*Scenario #2: Existing setup (multiple developers)*

## Step 1: Obtain Sources

* This guide assumes the `main` branch of the repo `git@github.com:CyberSec-and-Cloud-Platform/azure-enterprise-deployment.git` is used throughout.

```
(base) welcome@Traianos-MacBook-Pro sandbox % git clone git@github.com:CyberSec-and-Cloud-Platform/azure-enterprise-deployment.git
Cloning into 'azure-enterprise-deployment'...
remote: Enumerating objects: 3799, done.
remote: Counting objects: 100% (68/68), done.
remote: Compressing objects: 100% (48/48), done.
remote: Total 3799 (delta 32), reused 44 (delta 19), pack-reused 3731
Receiving objects: 100% (3799/3799), 9.82 MiB | 4.22 MiB/s, done.
Resolving deltas: 100% (2529/2529), done.
```

* Navigate to the `ci-cd/bootstrap`. Our objective is to build the jenkins deployer and pipelines. 

```
(base) welcome@Traianos-MacBook-Pro bootstrap % ls -l
total 24
-rw-r--r--   1 welcome  staff  3302 Feb 13 10:47 README.md
-rwxr-xr-x   1 welcome  staff   308 Feb 13 10:47 deploy.sh
drwxr-xr-x  22 welcome  staff   704 Feb 13 10:47 deployer-image
-rw-r--r--   1 welcome  staff  1617 Feb 13 10:47 deployer.yaml
drwxr-xr-x   5 welcome  staff   160 Feb 13 10:47 media
```

## Step 2: Obtain Credentials and Tenant Settings

The jenkins deployer uses CASC (see `ci-cd/bootstrap/deployer-image/casc.yaml`) to define a jenkins installation "as code" and must be configured with a number of credentials:

* "BITBUCKET_COMMON_CREDS" = username and BitBucket Repo Access Key for the git repository
* "DOCKERHUB_CREDENTIALS" = username and password to a Dockerhub repository
* In the credentials bloc, configure the credentials once you've obtained them via a private channel.

```yaml
credentials:
  system:
    domainCredentials:
    - credentials:
      - usernamePassword:
          id: "BITBUCKET_COMMON_CREDS"
          username: "archmangler"
          password: "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
          description: "BitBucket Repo Access Key"
          scope: GLOBAL
      - usernamePassword:
          id: "DOCKERHUB_CREDENTIALS"
          username: "dockerhub username"
          password: "dockerhub password"
          description: "Dockerhub Repo Access Key"
          scope: GLOBAL
```

* NOTE: for a minimum level of security, don't commit the casc.yaml file to git, just keep the secrets local.

## Step 3: Configure Tenant and Subscriptions

* If you're using a new tenant and new subscriptions and not operating on an existing deployment, then update the details in the following pipelines:

```
-rw-r--r--  1 welcome  staff   4084 Feb 13 10:47 eip-enterprise-caf-level0.groovy
-rw-r--r--  1 welcome  staff   6929 Feb 13 10:47 eip-enterprise-caf-level1.groovy
-rw-r--r--  1 welcome  staff   5385 Feb 13 10:47 eip-enterprise-caf-level2.groovy
-rw-r--r--  1 welcome  staff   2542 Feb 13 10:47 eip-enterprise-caf-level3.groovy
```

## Step 4: Install the development tools

* Instructions below are for Mac OSX

### Docker Desktop

* Install docker desktop on Mac OSX.

### Rancher Desktop

* You may need to configure Rancher Desktop for networking on your local computer. Refer to the Rancher instructions for this.

## Step 5: Build Jenkins Deployer

* Pre-requisite: You should have your own Dockerhub account. Alternatively you may reconfigure the jenkins pipelines and all code in `ci-cd` to use an alternative repo.
* Update the Dockerhub details in the yaml and the jenkins pipelines

```sh

# for ARM-based Macs only
#export DOCKER_DEFAULT_PLATFORM=linux/amd64

#Note: Use your specific registry tag below ...
cd deployer-image/
docker build -t $YOUR_DOCKERHUB_NAMESPACE/jenkins:jcasc-0.0.x .
docker push $YOUR_DOCKERHUB_NAMESPACE/jenkins:jcasc-0.0.x
```

* Having built your dockerized jenkins deployer, update the image version in the kubernetes yaml:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: jenkins
  namespace: ragnarok
spec:
  selector:
    matchLabels:
      app: jenkins
  replicas: 1
  template:
    metadata:
      labels:
        app: jenkins
    spec:
      serviceAccountName: internal-kubectl
      containers:
      - name: jenkins
        image: archbungle/jenkins-eip:0.0.x # update the version
        imagePullPolicy: IfNotPresent
      env:
        # change Dockerhub credentials
```

* deploy the kubernetes `Deployment` to the rancher desktop kubernetes service:

```
(base) welcome@Traianos-MacBook-Pro bootstrap % ./deploy.sh 
deleted
rolebinding.rbac.authorization.k8s.io "modify-pods-to-sa" deleted
role.rbac.authorization.k8s.io "modify-pods" deleted
serviceaccount "internal-kubectl" deleted
NAME                                     READY   STATUS        RESTARTS   AGE
load-sink-66cf5c8bb7-jc24b               0/1     Pending       0          182d
prometheus-deployment-599bbd9457-f7t6n   1/1     Running       8          182d
grafana-64c89f57f7-qccrl                 1/1     Running       13         234d
jenkins-77dfdbfb7b-24n5v                 1/1     Terminating   0          9d
NAME                                     READY   STATUS        RESTARTS   AGE
load-sink-66cf5c8bb7-jc24b               0/
.
.
.
NAME                 TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)          AGE
consumer-service     NodePort    10.43.239.133   <none>        80:30081/TCP     412d
nfs-service          ClusterIP   10.43.210.174   <none>        2049/TCP         411d
producer-service     NodePort    10.43.100.123   <none>        80:30082/TCP     411d
loader-service       NodePort    10.43.75.174    <none>        80:30084/TCP     411d
grafana              ClusterIP   10.43.125.19    <none>        80/TCP           412d
ingestor-service     ClusterIP   10.43.119.108   <none>        80/TCP           279d
fxconsumer-service   NodePort    10.43.18.39     <none>        80:30079/TCP     239d
prometheus-service   NodePort    10.43.173.106   <none>        8080:30000/TCP   182d
sink-service         ClusterIP   10.43.169.23    <none>        80/TCP           182d
jenkins-service      NodePort    10.43.119.90    <none>        8080:30889/TCP   8s
```

## Step 6: Run Jenkins Deployer

* Browse to http://127.0.0.1:30889/ address and login with `admin`, `admin` (password is configured in the `casc.yaml`):

![alt text](media/jenkins-login.png?raw=true "Deployer Login") 

* All the infrastructure deployment pipelines should be visible:

![alt text](media/jenkins-instance-and-pipeline-jobs.png?raw=true "Deployer Login") 

## Step 7: Run the Test pipelines

* Each deploy pipeline (levels 0 - 3) has a test pipeline which only runs terraform apply for each stage for safe testing.
* You should run the test pipeline to check the impact of your changes from a terraform  point of view before running the actual deploy pipeline
* select the `level-xxx-test` pipeline and click "build"
* repeat for level-1-test,2,3 etc ...

## Step 8: Run the Deploy Pipelines

* select the `level-0` pipeline and click "build"

![alt text](media/level-0-pipeline.png?raw=true "Deploy Pipeline")

* repeat for level-1,2,3 etc ...
