pipelineJob("landing-zone-provisioner-test") {
  definition {
    cps {
         sandbox(true)
script("""
pipeline {
    agent any

    options {
        ansiColor('xterm')
    }

    environment {
     BITBUCKET_COMMON_CREDS=credentials('BITBUCKET_COMMON_CREDS')
      CURRENT_BRANCH="main"
    }

     stages {
       stage("Getting infrastructure code") {
            steps {

              dir("/var/jenkins_home/workspace/landing-zone-provisioner-test/") {
                deleteDir()
              }

              dir("/var/jenkins_home/workspace/landing-zone-provisioner-test/") {
                sh '''#!/bin/bash
                set +ex
                git clone "https://\${BITBUCKET_COMMON_CREDS_USR}:\${BITBUCKET_COMMON_CREDS_PSW}@github.com/engeneon/orion.git"
                cd orion/
                git checkout \${CURRENT_BRANCH}
                terraform version
                az version
                ls -l
                pwd
                echo "============================================="
                '''
             }
         }
      }

      stage("Extracting global environment variables") {
        steps {
           dir("/var/jenkins_home/workspace/landing-zone-provisioner-test/orion/configuration/") {

              script {
                 env.orgCode = input message: 'Please enter the organisation code',
                             parameters: [string(defaultValue: 'acme',
                                          description: 'short naming code for the organisation e.g eip, contoso',
                                          name: 'orgCode')]
                 env.environmentDomain = input message: 'Please enter the environmentDomain',
                             parameters: [string(defaultValue: 'nprd',
                                          description: 'Environment Domain for deployment e.g nprd/prod',
                                          name: 'environmentDomain')]
                 env.deployIndex = input message: 'Please enter the deploy index',
                             parameters: [string(defaultValue: '001',
                                          description: 'The index of this tenant deployment e.g 000, 001, 002 etc...',
                                          name: 'deployIndex')]
              }

              sh '''
               echo "Getting deployment configuration ..." 
               pwd
               ls
              '''

             script {
                def readpropscontent = readProperties file: "\${environmentDomain}/\${orgCode}/\${deployIndex}/\${orgCode}.conf"
                echo 'Got deployment parameters for tenant: readpropscontent = '+readpropscontent

                echo 'setting TENANT_ID = '+readpropscontent['TENANT_ID']
                echo 'setting TENANT_ORGANISATION_NAME = '+readpropscontent['TENANT_ORGANISATION_NAME']
                echo 'setting TENANT_ORGANISATION_SHORT = '+readpropscontent['TENANT_ORGANISATION_SHORT']
                echo 'setting MANAGEMENT_SUBSCRIPTION_ID = '+readpropscontent['MANAGEMENT_SUBSCRIPTION_ID']
                echo 'setting CONNECTIVITY_SUBSCRIPTION_ID = '+readpropscontent['CONNECTIVITY_SUBSCRIPTION_ID']
                echo 'setting TENANT_DEPLOY_INDEX = '+readpropscontent['TENANT_DEPLOY_INDEX']
                echo 'setting DEPLOYMENT_LOCATION_SHORT = '+readpropscontent['DEPLOYMENT_LOCATION_SHORT']
                echo 'setting CURRENT_BRANCH = '+readpropscontent['CURRENT_BRANCH']
                echo 'setting TENANT_ENVIRONMENT_DOMAIN = '+readpropscontent['TENANT_ENVIRONMENT_DOMAIN']
                echo 'setting IDENTITY_SUBSCRIPTION_ID = '+readpropscontent['IDENTITY_SUBSCRIPTION_ID']

                env['IDENTITY_SUBSCRIPTION_ID'] = readpropscontent['IDENTITY_SUBSCRIPTION_ID']
                env['TENANT_ENVIRONMENT_DOMAIN'] = readpropscontent['TENANT_ENVIRONMENT_DOMAIN']
                env['TENANT_ID'] = readpropscontent['TENANT_ID']
                env['TENANT_ORGANISATION_NAME'] = readpropscontent['TENANT_ORGANISATION_NAME']
                env['TENANT_ORGANISATION_SHORT'] = readpropscontent['TENANT_ORGANISATION_SHORT']
                env['MANAGEMENT_SUBSCRIPTION_ID'] = readpropscontent['MANAGEMENT_SUBSCRIPTION_ID']
                env['CONNECTIVITY_SUBSCRIPTION_ID'] = readpropscontent['CONNECTIVITY_SUBSCRIPTION_ID']
                env['TENANT_DEPLOY_INDEX'] = readpropscontent['TENANT_DEPLOY_INDEX']
                env['DEPLOYMENT_LOCATION_SHORT'] = readpropscontent['DEPLOYMENT_LOCATION_SHORT']
                env['CURRENT_BRANCH'] = readpropscontent['CURRENT_BRANCH']

             }

           }
         }
        }

        stage("Configuring Azure Access") {
          
            steps {

              dir("/var/jenkins_home/workspace/landing-zone-provisioner-test") {
                deleteDir()
              }

              dir("/var/jenkins_home/workspace/landing-zone-provisioner-test") {  
                sh '''#!/bin/bash
                set +ex
                git clone "https://\${BITBUCKET_COMMON_CREDS_USR}:\${BITBUCKET_COMMON_CREDS_PSW}@github.com/engeneon/orion.git"
                cd orion/
                terraform version
                az version
                az login --tenant \${TENANT_ID}
                ls -l
                pwd
                git checkout \${CURRENT_BRANCH}
                echo "============================================="
                '''
              }            
            }
        }

        //BEGIN process configuration
        stage("Deploying RBAC Roles for Landing Zone") {
          steps {
            dir("/var/jenkins_home/workspace/landing-zone-provisioner-test/orion/level-0/aad-rbac") {  
              sh '''#!/bin/bash
              set +ex
              ls -l
              pwd
              ./test.sh
              echo "============================================="
              '''
            }
          }
        }
        //END process configuration

        //BEGIN process configuration
        stage("Deploying Landing Zone Automation Credentials") {
          steps {
            
            dir("/var/jenkins_home/workspace/landing-zone-provisioner-test/orion/level-0/aad-rbac") {  
              sh '''#!/bin/bash
              set +ex
              ls -l
              pwd
              ./test.sh
              echo "============================================="
              '''
            }
            
            dir("/var/jenkins_home/workspace/landing-zone-provisioner-test/orion/level-0/secrets") {  
              sh '''#!/bin/bash
              set +ex
              ls -l
              pwd
              ./test.sh
              echo "============================================="
              '''
            }

            dir("/var/jenkins_home/workspace/landing-zone-provisioner-test/orion/level-0/keyvaults") {  
              sh '''#!/bin/bash
              set +ex
              ls -l
              pwd
              ./test.sh
              echo "============================================="
              '''
            }

            dir("/var/jenkins_home/workspace/landing-zone-provisioner-test/orion/level-0/storage") {  
              sh '''#!/bin/bash
              set +ex
              ls -l
              pwd
              ./test.sh
              echo "============================================="
              '''
            }


          }
        }
        //END process configuration

        //BEGIN process configuration
        stage("Creating Landing Zone State Storage") {
          steps {
            dir("/var/jenkins_home/workspace/landing-zone-provisioner-test/orion/level-0/storage") {  
              sh '''#!/bin/bash
              set +ex
              ls -l
              pwd
              ./test.sh
              echo "============================================="
              '''
            }
          }
        }
        //END process configuration

        //BEGIN process configuration
        stage("Deploying Landing Zone Spoke VNETs") {
          steps {
            dir("/var/jenkins_home/workspace/landing-zone-provisioner-test/orion/level-2/caf-spokes") {  
              sh '''#!/bin/bash
              set +ex
              ls -l
              pwd
              ./test.sh
              echo "============================================="
              '''
            }
          }
        }
        //END process configuration

        //BEGIN process configuration
        stage("Configuring Central Firewall Access") {
          steps {
            dir("/var/jenkins_home/workspace/landing-zone-provisioner-test/orion/level-2/afw-policy") {  
              sh '''#!/bin/bash
              set +ex
              ls -l
              pwd
              ./test.sh
              echo "============================================="
              '''
            }
          }
        }
        //END process configuration


        //BEGIN process configuration
        stage("Configuring Landing Zone Hub-Spoke Peering Link") {
          steps {
            dir("/var/jenkins_home/workspace/landing-zone-provisioner-test/orion/level-1/azure-caf-implementation") {  
              sh '''#!/bin/bash
              set +ex
              ls -l
              pwd
              ./test.sh
              echo "============================================="
              '''
            }
          }
        }
        //END process configuration

        //BEGIN process configuration
        stage("Configuring Landing Zone Namespace") {
          steps {

            dir("/var/jenkins_home/workspace/landing-zone-provisioner-test/orion/level-1/azure-caf-implementation") {  
              sh '''#!/bin/bash
              set +ex
              ls -l
              pwd
              ./test.sh
              echo "============================================="
              '''
            }

          }
        }
        //END process configuration

        //BEGIN process configuration
        stage("Customising Landing Zone Policy") {
          steps {
            dir("/var/jenkins_home/workspace/landing-zone-provisioner-test/orion/level-3/custom-landing-zones") {  
              sh '''#!/bin/bash
              set +ex
              ls -l
              pwd
              ./test.sh
              echo "============================================="
              '''
            }
          }
        }
        //END process configuration

        //BEGIN process configuration
        stage("Provisioning Requested Landing Zone DNS Zones") {
          steps {

            dir("/var/jenkins_home/workspace/landing-zone-provisioner-test/orion/level-1/azure-caf-implementation") {  
              sh '''#!/bin/bash
              set +ex
              ls -l
              pwd
              ./test.sh
              echo "============================================="
              '''
            }

          }
        }
        //END process configuration

        //BEGIN process configuration
        stage("Deploying Landing Zone DNS Configuration") {
          steps {

            dir("/var/jenkins_home/workspace/landing-zone-provisioner-test/orion/level-3/dns-zone-records") {  
              sh '''#!/bin/bash
              set +ex
              ls -l
              pwd
              ./test.sh
              echo "============================================="
              '''
            }

          }
        }
        //END process configuration

        //BEGIN process configuration
        stage("Summarising Landing Zone Deliverables") {
          steps {

            dir("/var/jenkins_home/workspace/landing-zone-provisioner-test/orion/configuration") {  
              sh '''#!/bin/bash
              set +ex
              ls -l
              pwd
              ./summary.sh
              echo "============================================="
              '''
            }

          }
        }
        //END process configuration

    }
    post {
        always { 
            dir("/var/jenkins_home/workspace/level-0/") {  
               deleteDir()
            }
            cleanWs()
        }
    }
}
""")
}
}
}
