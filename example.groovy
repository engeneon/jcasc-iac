pipelineJob("projectname") {
  definition {
    cps {
         sandbox(true)
script("""
pipeline {
    agent any

environment {
   AKS_CLUSTER_NAME = ""
   AKS_RESOURCE_GROUP = ""
   AWS_ACCESS_KEY_ID= ""
   AWS_CLUSTER_NAME = ""
   AWS_DEPLOY_REGION=""
   AWS_ID=""
   AWS_SECRET_ACCESS_KEY=""
   AWS_SESSION_TOKEN=""
   DOCKERHUB_SERVER="https://index.docker.io/v1/"
   BITBUCKET_COMMON_CREDS=credentials('BITBUCKET_COMMON_CREDS')
   DOCKERHUB_COMMON_CREDS=credentials('DOCKERHUB_CREDENTIALS')
}
   parameters {
     string(name: 'MFA_TOKEN', defaultValue: 'null', description: 'Enter MFA Token')
     string(name: 'AWS_ACCOUNT_NUMBER', defaultValue: 'null', description: 'Enter AWS_ACCOUNT_NUMBER')
   }
    
    stages {
        stage("Configuring Access") {
            steps {

                sh '''#!/bin/bash
                set +x
                rm -rf trading-system-load-engine/
                #git clone "https://\${BITBUCKET_COMMON_CREDS_USR}:\${BITBUCKET_COMMON_CREDS_PSW}@bitbucket.org/equos_exchange/trading-system-load-engine.git"
                git clone "https://github.com/archmangler/trading-system-load-engine.git"
                cd trading-system-load-engine/
                echo "============================================="
                echo "I AM A JENKINSFILE ..."
                aws --version
                env
                echo "============================================="
                git checkout batch-test-automation-execution-engeneon
                source env.sh \${MFA_TOKEN} \${AWS_ACCOUNT_NUMBER}
                pwd
                ls -l
                echo \$HOME
                cd  ~/
                pwd
                echo "============== start saving environment variables ========================"

                touch settings.sh
                echo export AWS_ACCESS_KEY_ID=\${AWS_ACCESS_KEY_ID} >/var/jenkins_home/workspace/projectname/trading-system-load-engine/kubernetes/eks-deploy/settings.sh
                echo export AWS_ACCOUNT_NUMBER=\${AWS_ACCOUNT_NUMBER} >>/var/jenkins_home/workspace/projectname/trading-system-load-engine/kubernetes/eks-deploy/settings.sh
                echo export AWS_CLUSTER_NAME=\${AWS_CLUSTER_NAME} >>/var/jenkins_home/workspace/projectname/trading-system-load-engine/kubernetes/eks-deploy/settings.sh
                echo export AWS_DEPLOY_REGION=\${AWS_DEPLOY_REGION} >>/var/jenkins_home/workspace/projectname/trading-system-load-engine/kubernetes/eks-deploy/settings.sh
                echo export AWS_ID=\${AWS_ID} >>/var/jenkins_home/workspace/projectname/trading-system-load-engine/kubernetes/eks-deploy/settings.sh
                echo export AWS_SECRET_ACCESS_KEY=\${AWS_SECRET_ACCESS_KEY} >>/var/jenkins_home/workspace/projectname/trading-system-load-engine/kubernetes/eks-deploy/settings.sh
                echo export AWS_SESSION_TOKEN=\${AWS_SESSION_TOKEN} >>/var/jenkins_home/workspace/projectname/trading-system-load-engine/kubernetes/eks-deploy/settings.sh
                cat /var/jenkins_home/workspace/projectname/trading-system-load-engine/kubernetes/eks-deploy/settings.sh

                echo "============== done saving environment variables ========================"
                '''

            }
        }

        stage("Deploying Kubernetes") {
         steps {
           dir("/var/jenkins_home/workspace/projectname/trading-system-load-engine/kubernetes/eks-deploy") {  
                sh '''#!/bin/bash
                echo "================== begin restoring aws access settings ========="
                ls -l
                source settings.sh
                echo "================== initialising remote state storage  ==========================="
                ./bootstrap-remote-state.sh
                echo "================== BEGIN running terraform ==========================="
                ./deploy.sh
                echo "Scaling up EKS cluster to initial size ..."
                eksctl get nodegroup --cluster \${AWS_CLUSTER_NAME} --region \${AWS_DEPLOY_REGION} | awk '{print \$2}'| egrep -v "NODEGROUP" | xargs -Inodename eksctl scale nodegroup --cluster \${AWS_CLUSTER_NAME} --region \${AWS_DEPLOY_REGION} --name nodename --nodes 6 --nodes-max 18 --nodes-min 3
                echo "================== DONE running terraform ==========================="
                '''           
           }
         }
        }

       stage("Configuring Microservices") {
            steps {

              dir("/var/jenkins_home/workspace/projectname/trading-system-load-engine/") {  
                sh '''#!/bin/bash
                echo "================== begin restoring aws access settings ========="
                source /var/jenkins_home/workspace/projectname/trading-system-load-engine/kubernetes/eks-deploy/settings.sh
                echo "logging in to Docker registry (dockerhub) ..."
                echo "================== done restoring aws access settings =========="

                echo "================== begin microservice deployments =========="
                ./deploy.sh
                echo "================== end microservice deployments =========="
                for i in `seq 1 20`;do echo "\$i - waiting to get ready ..." - \$(sleep 1);done
                '''
               }

               dir("/var/jenkins_home/workspace/projectname/trading-system-load-engine/") {  

                sh '''#!/bin/bash
                echo "================== begin restoring aws access settings ========="
                source /var/jenkins_home/workspace/projectname/trading-system-load-engine/kubernetes/eks-deploy/settings.sh
                echo "================== done restoring aws access settings =========="
                echo "creating core service namespaces"
                for ns in projectname redis pulsar; do echo "creating namespace \$ns" kubectl create ns \$ns;done
                echo "done creating core namespaces ..."
                '''

               }

               dir("/var/jenkins_home/workspace/projectname/trading-system-load-engine/networking/eks-deploy") {  

                sh '''#!/bin/bash
                echo "================== begin restoring aws access settings ========="
                source /var/jenkins_home/workspace/projectname/trading-system-load-engine/kubernetes/eks-deploy/settings.sh
                echo "================== done restoring aws access settings =========="
                echo "begin deploying ingress controllers ..."
                ./deploy.sh
                echo "done deploying ingress controllers ..."
                '''

               }


               dir("/var/jenkins_home/workspace/projectname/trading-system-load-engine/microservices/ingress/eks-deploy") {  

                sh '''#!/bin/bash
                echo "================== begin restoring aws access settings ========="
                source /var/jenkins_home/workspace/projectname/trading-system-load-engine/kubernetes/eks-deploy/settings.sh
                echo "================== done restoring aws access settings =========="
                echo "begin deploying ingress configuration ..."
                ./deploy.sh
                echo "done deploying ingress configuration ..."
                '''

               }

               dir("/var/jenkins_home/workspace/projectname/trading-system-load-engine/monitoring/prometheus/common") {  

                sh '''#!/bin/bash
                echo "================== begin restoring aws access settings ========="
                source /var/jenkins_home/workspace/projectname/trading-system-load-engine/kubernetes/eks-deploy/settings.sh
                echo "================== done restoring aws access settings =========="
                echo "begin deploying monitoring - prometheus ..."
                ./deploy.sh
                echo "done deploying monitoring - prometheus ..."
                '''

               }

               dir("/var/jenkins_home/workspace/projectname/trading-system-load-engine/monitoring/grafana") {  

                sh '''#!/bin/bash
                echo "================== begin restoring aws access settings ========="
                source /var/jenkins_home/workspace/projectname/trading-system-load-engine/kubernetes/eks-deploy/settings.sh
                echo "================== done restoring aws access settings =========="
                echo "begin deploying monitoring - grafana ..."
                ./deploy.sh
                echo "done deploying monitoring - grafana ..."
                '''

               }


               dir("/var/jenkins_home/workspace/projectname/trading-system-load-engine/microservices/storage/redis-storage/eks-deploy") {  
                 sh '''#!/bin/bash
                 echo "================== begin restoring aws access settings ========="
                 source /var/jenkins_home/workspace/projectname/trading-system-load-engine/kubernetes/eks-deploy/settings.sh
                 echo "================== done restoring aws access settings =========="
                 echo "begin deploying redis storage ..."
                 ./deploy.sh
                 echo "done deploying redis storage  ..."
                 '''
               }

               dir("/var/jenkins_home/workspace/projectname/trading-system-load-engine/microservices/storage/redis-storage/eks-deploy") {  
               
                 sh '''#!/bin/bash
                 echo "================== begin restoring aws access settings ========="
                 source /var/jenkins_home/workspace/projectname/trading-system-load-engine/kubernetes/eks-deploy/settings.sh
                 echo "================== done restoring aws access settings =========="
                 echo "begin deploying redis storage ..."
                 ./deploy.sh
                 echo "done deploying redis storage  ..."
                 '''
               
               }

               dir("/var/jenkins_home/workspace/projectname/trading-system-load-engine/") {  
               
                 sh '''#!/bin/bash
                 echo "================== begin restoring aws access settings ========="
                 source /var/jenkins_home/workspace/projectname/trading-system-load-engine/kubernetes/eks-deploy/settings.sh
                 echo "================== done restoring aws access settings =========="
                 echo "begin deploying ..."
                 ./deploy.sh
                 echo "done deploying ..."
                 '''
               
               }


               dir("/var/jenkins_home/workspace/projectname/trading-system-load-engine/microservices/producer/build/") {  
                sh '''#!/bin/bash
                echo "================== begin restoring aws access settings ========="
                source /var/jenkins_home/workspace/projectname/trading-system-load-engine/kubernetes/eks-deploy/settings.sh
                echo "================== done restoring aws access settings =========="
                echo "================== begin creating secrets =========="
                ./create-secret.sh
                echo "================== done creating secrets =========="
                for i in `seq 1 10`;do echo "\$i - waiting to get ready ..." - \$(sleep 1);done
                '''
               }


               dir("/var/jenkins_home/workspace/projectname/trading-system-load-engine/microservices/load-sink/eks-deploy") {  
                sh '''#!/bin/bash
                 echo "================== begin restoring aws access settings ========="
                 source /var/jenkins_home/workspace/projectname/trading-system-load-engine/kubernetes/eks-deploy/settings.sh
                 echo "================== done restoring aws access settings =========="
                 echo "deploying load sink service ..."
                 ./deploy.sh
                '''
               }

  dir("/var/jenkins_home/workspace/projectname/trading-system-load-engine/microservices/producer/eks-deploy/") {  
   sh '''#!/bin/bash
                 echo "================== begin restoring aws access settings ========="
                 source /var/jenkins_home/workspace/projectname/trading-system-load-engine/kubernetes/eks-deploy/settings.sh
                 echo "================== done restoring aws access settings =========="
    echo "deploying load sink service ..."
    ./deploy.sh
   '''
  }

 dir("/var/jenkins_home/workspace/projectname/trading-system-load-engine/microservices/consumer/eks-deploy/") {  
  sh '''#!/bin/bash
                echo "================== begin restoring aws access settings ========="
                source /var/jenkins_home/workspace/projectname/trading-system-load-engine/kubernetes/eks-deploy/settings.sh
                echo "================== done restoring aws access settings =========="
   ./deploy.sh
  '''
 }

 dir("/var/jenkins_home/workspace/projectname/trading-system-load-engine/microservices/fix-consumer/eks-deploy/") {  
  sh '''#!/bin/bash
                echo "================== begin restoring aws access settings ========="
                source /var/jenkins_home/workspace/projectname/trading-system-load-engine/kubernetes/eks-deploy/settings.sh
                echo "================== done restoring aws access settings =========="
   ./deploy.sh
  '''
 }


  dir("/var/jenkins_home/workspace/projectname/trading-system-load-engine/microservices/loader/eks-deploy/") {  
   sh '''#!/bin/bash
                echo "================== begin restoring aws access settings ========="
                source /var/jenkins_home/workspace/projectname/trading-system-load-engine/kubernetes/eks-deploy/settings.sh
                echo "================== done restoring aws access settings =========="
   pwd
   ./destroy.sh
   ./deploy.sh
   '''
  }

 dir("/var/jenkins_home/workspace/projectname/trading-system-load-engine/microservices/loader/eks-deploy/rbac-config") {  
   sh '''#!/bin/bash
                echo "================== begin restoring aws access settings ========="
                source /var/jenkins_home/workspace/projectname/trading-system-load-engine/kubernetes/eks-deploy/settings.sh
                echo "================== done restoring aws access settings =========="
   pwd
   echo "deploying rbac config for management service ..."
   ./deploy.sh
   '''
 }

 dir("/var/jenkins_home/workspace/projectname/trading-system-load-engine/microservices/order-dumper/eks-deploy") {  
   sh '''#!/bin/bash
                echo "================== begin restoring aws access settings ========="
                source /var/jenkins_home/workspace/projectname/trading-system-load-engine/kubernetes/eks-deploy/settings.sh
                echo "================== done restoring aws access settings =========="
   pwd
   echo "deploying rbac config for management service ..."
   ./deploy.sh
   '''
 }

dir("/var/jenkins_home/workspace/projectname/trading-system-load-engine/storage/eks-deploy/") {  
    sh '''#!/bin/bash
    echo "================== begin restoring aws access settings ========="
    source /var/jenkins_home/workspace/projectname/trading-system-load-engine/kubernetes/eks-deploy/settings.sh
    echo "================== done restoring aws access settings =========="
    pwd
    ./deploy.sh
    ./eksS3access.sh
    '''
}

        dir("/var/jenkins_home/workspace/projectname/trading-system-load-engine/microservices/ingestor/eks-deploy/") {  
               sh '''#!/bin/bash
                echo "================== begin restoring aws access settings ========="
                source /var/jenkins_home/workspace/projectname/trading-system-load-engine/kubernetes/eks-deploy/settings.sh
                echo "================== done restoring aws access settings =========="
                pwd
               ./deploy.sh
               '''  
        }

        dir("/var/jenkins_home/workspace/projectname/trading-system-load-engine/microservices/serialin/eks-deploy/") {  
               sh '''#!/bin/bash
                echo "================== begin restoring aws access settings ========="
                source /var/jenkins_home/workspace/projectname/trading-system-load-engine/kubernetes/eks-deploy/settings.sh
                echo "================== done restoring aws access settings =========="
                pwd
               ./deploy.sh
               '''  
        }

        dir("/var/jenkins_home/workspace/projectname/trading-system-load-engine/microservices/serialout/eks-deploy/") {  

               sh '''#!/bin/bash
                echo "================== begin restoring aws access settings ========="
                source /var/jenkins_home/workspace/projectname/trading-system-load-engine/kubernetes/eks-deploy/settings.sh
                echo "================== done restoring aws access settings =========="
                pwd
               ./deploy.sh
               '''  
        
        }
       
    }





    }

    stage("Deployment Health Checks") {
          steps {
            dir("/var/jenkins_home/workspace/projectname/trading-system-load-engine/") {  
              sh '''#!/bin/bash
              echo "================== begin restoring aws access settings ========="
               source /var/jenkins_home/workspace/projectname/trading-system-load-engine/kubernetes/eks-deploy/settings.sh
               echo "================== done restoring aws access settings =========="
               echo "================== Running Deployment Health Check =========="
               ./health.sh
               echo "================== DONE Running Deployment Health Check =========="
               '''
              }
            }
        }

        stage("Deployment Summary") {
            steps {
             dir("/var/jenkins_home/workspace/projectname/trading-system-load-engine/") {  
               sh '''#!/bin/bash
               echo "================== begin restoring aws access settings ========="
               source /var/jenkins_home/workspace/projectname/trading-system-load-engine/kubernetes/eks-deploy/settings.sh
               echo "================== done restoring aws access settings =========="
               echo "================== Deployment Details =========="
               management_ui=\$(kubectl get ingress -n projectname -o json| jq -r '.items|.[]|.status|.loadBalancer|.ingress|.[]|.hostname')
               echo "1) Access the Load Testing Management U.I          : \$management_ui/loader-admin"
               echo "2) Acess the Monitoring Dashboard    : \$management_ui/"
               echo "================== DONE =========="
               '''
              }
            }
        }

    }
}
""")
}
}
}
