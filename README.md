# Jenkins deployment pipeline for `EIP Multi-Tenant Multi-Subscription CAF Landing Zone Implementation`

* The code in this repo demonstrates an automated pipeline to deploy `EIP Multi-Tenant Multi-Subscription CAF Landing Zone Implementation` to kubernetes or docker.
* A jenkins pipeline image is first deployed to kubernetes hosted locally by rancher-desktop
* The pipeline then interacts consumes  terraform infrastructure-code to deploy a complete multi-tenant, multi-subscription Azure environments based on the Microsoft CAF model

# How to deploy (MacOS):

* Install Rancher desktop on your computer to get a convenient Kubernetes environment for development: (https://github.com/rancher-sandbox/rancher-desktop/releases/download/v1.0.0/Rancher.Desktop-1.0.0.x86_64.dmg)
* Clone down the repo: git clone `EIP Multi-Tenant Multi-Subscription CAF Landing Zone Implementation`
* Run the demo deployer:

```
EIP Multi-Tenant Multi-Subscription CAF Landing Zone Implementation
```

* Browse to http://127.0.0.1:30889/ address and login with `admin`, `admin`:

![alt text](media/jenkins-login.png?raw=true "Deployer Login") 

* All the infrastructure deployment pipelines should be visible:

![alt text](media/jenkins-instance-and-pipeline-jobs.png?raw=true "Deployer Login") 

* select the `level-0` pipeline and click "build"

![alt text](media/level-0-pipeline.png?raw=true "Deploy Pipeline")

# Extending the `EIP Multi-Tenant Multi-Subscription CAF Landing Zone Implementation` deployer pipeline

* The deployer pipeline is defined as a declarative Jenkins pipeline DSL based on groovy and deployed as an immutable docker image.
* To modify and update the pipeline in the docker image, modify `*.groovy` in `deployer-image` folder and rebuild the image:

```
#Note: Use your  specific registry tag below ...
cd deployer-image/
docker build -t archbungle/jenkins:jcasc-0.0.x
docker push archbungle/jenkins:jcasc-0.0.x
```

(Modify to your personal image registry requirements)

# Naming Convention

* To maintain a consistent naming convention from L0 to L3 (at least!) we make use of the azure terraform naming convention provider, example:


```
resource "azurecaf_name" "rg_example" {
  name            = "demogroup"
    resource_type   = "azurerm_resource_group"
    prefixes        = ["a", "b"]
    suffixes        = ["y", "z"]
    random_length   = 5 
    clean_input     = true
}

resource "azurerm_resource_group" "demo" {
  name     = azurecaf_name.rg_example.result
  location = "southeastasia"
}
```

# State Storage

* While we've implemented bootstrap state storage by terraform it can also be done via cli:

```
#!/bin/bash

RESOURCE_GROUP_NAME=tfstate
STORAGE_ACCOUNT_NAME=tfstate$RANDOM
CONTAINER_NAME=tfstate

# Create resource group
az group create --name $RESOURCE_GROUP_NAME --location eastus

# Create storage account
az storage account create --resource-group $RESOURCE_GROUP_NAME --name $STORAGE_ACCOUNT_NAME --sku Standard_LRS --encryption-services blob

# Create blob container
az storage container create --name $CONTAINER_NAME --account-name $STORAGE_ACCOUNT_NAME
```


# Related Repositories:

* Example JCASC Deployer: https://github.com/archmangler/jenkins-jcasc
 
#  Issues

- n/a

# ToDo (Pending)

- Docker Standalone deployer (for lighter deploy footprint)
