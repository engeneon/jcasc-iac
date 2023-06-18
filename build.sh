#!/bin/bash
#Simple build script to create deployer image for Azure Terraform deployment pipelines
#
#Recommended: Use a semantic versioning scheme ...
#./build.sh <the semantic vesion id>
#docker pull quay.io/engeneon/jenkins-caf

build_version=$1
docker build -t quay.io/engeneon/jenkins-caf:"$build_version" .
docker push quay.io/engeneon/jenkins-caf:"$build_version"
