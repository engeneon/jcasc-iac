FROM jenkins/jenkins:2.346.1-jdk17-preview
ENV JAVA_OPTS -Djenkins.install.runSetupWizard=false
ENV CASC_JENKINS_CONFIG /var/jenkins_home/casc.yaml
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN /usr/local/bin/install-plugins.sh < /usr/share/jenkins/ref/plugins.txt
COPY casc.yaml /var/jenkins_home/casc.yaml
RUN mkdir -p /var/jenkins_home/.aws/
USER root
RUN apt-get clean && apt-get update && apt-get install -y gnupg software-properties-common
RUN apt-get clean && apt-get update
RUN apt-get -y install kubernetes-client
RUN apt-get -y install netcat
RUN apt-get -y install jq
RUN apt-get -y install bash
RUN apt-get -y install unzip
RUN apt-get -y install curl
RUN curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
RUN unzip awscliv2.zip
RUN ./aws/install
RUN aws --version
RUN apt-get -y install docker
RUN curl -fsSL https://apt.releases.hashicorp.com/gpg | apt-key add -
RUN apt-get -y install ca-certificates curl apt-transport-https lsb-release gnupg
RUN curl -sL https://packages.microsoft.com/keys/microsoft.asc | gpg --dearmor | tee /etc/apt/trusted.gpg.d/microsoft.gpg > /dev/null
RUN echo "deb [arch=amd64] https://packages.microsoft.com/repos/azure-cli/ bionic main" | tee /etc/apt/sources.list.d/azure-cli.list
RUN apt-add-repository "deb [arch=amd64] https://apt.releases.hashicorp.com bionic main"
#BROKEN Upstream at microsoft. Those assholes.
#RUN apt-get update && apt-get install azure-cli
RUN curl -sL https://aka.ms/InstallAzureCLIDeb | bash
RUN apt-get install terraform && terraform -version docker
RUN curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 && chmod 700 get_helm.sh && ./get_helm.sh
USER jenkins
COPY bootstrap-levels.groovy /usr/local/bootstrap-levels.groovy
COPY eip-enterprise-caf-level0-certificate-rotation.groovy /usr/local/eip-enterprise-caf-level0-certificate-rotation.groovy
COPY eip-enterprise-caf-level0.groovy /usr/local/eip-enterprise-caf-level0.groovy
COPY eip-enterprise-destroy-level0.groovy /usr/local/eip-enterprise-destroy-level0.groovy
COPY eip-enterprise-caf-level1.groovy /usr/local/eip-enterprise-caf-level1.groovy
COPY eip-enterprise-destroy-level1.groovy /usr/local/eip-enterprise-destroy-level1.groovy
COPY eip-enterprise-caf-level2.groovy /usr/local/eip-enterprise-caf-level2.groovy
COPY eip-enterprise-caf-level3.groovy /usr/local/eip-enterprise-caf-level3.groovy
COPY eip-enterprise-caf-cust000-app001.groovy /usr/local/eip-enterprise-caf-cust000-app001.groovy
COPY eip-enterprise-caf-cust001-app000.groovy /usr/local/eip-enterprise-caf-cust001-app000.groovy
COPY eip-enterprise-caf-rbac-management.groovy /usr/local/eip-enterprise-caf-rbac-management.groovy
COPY eip-enterprise-caf-level0-test.groovy /usr/local/eip-enterprise-caf-level0-test.groovy
COPY eip-enterprise-caf-level1-test.groovy /usr/local/eip-enterprise-caf-level1-test.groovy
COPY eip-enterprise-caf-level2-test.groovy /usr/local/eip-enterprise-caf-level2-test.groovy
COPY eip-enterprise-caf-level3-test.groovy /usr/local/eip-enterprise-caf-level3-test.groovy
COPY eip-enterprise-caf-lz-provisioner-deploy.groovy /usr/local/eip-enterprise-caf-lz-provisioner-deploy.groovy
COPY eip-enterprise-caf-lz-provisioner-test.groovy /usr/local/eip-enterprise-caf-lz-provisioner-test.groovy
COPY eip-enterprise-caf-devealz000.groovy /usr/local/eip-enterprise-caf-devealz000.groovy
