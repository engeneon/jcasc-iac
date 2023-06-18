pipelineJob("azure-sql-database-devealz000") {
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
              dir("/var/jenkins_home/workspace/azure-sql-database-devealz000/") {
                deleteDir()
              }
              dir("/var/jenkins_home/workspace/azure-sql-database-devealz000/") {
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
           dir("/var/jenkins_home/workspace/azure-sql-database-devealz000/orion/configuration/") {

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
              dir("/var/jenkins_home/workspace/azure-sql-database-devealz000/orion/level-4") {  
                sh '''#!/bin/bash
                set +x
                terraform version
                az version
                git checkout \${CURRENT_BRANCH} 
                pwd
                ls -l
                ./login.sh
                echo "============================================="
                '''
              }
            }
        }

        stage("Testing Dev EA LZ 000 Application 000") {
         steps {
           dir("/var/jenkins_home/workspace/azure-sql-database-devealz000/orion/level-4/applications/azure-sql-database-devealz000") {  
                sh '''#!/bin/bash
                echo "Test dev ea app 000 lz 000 ..."
                pwd
                ls -l
                ./test.sh
                '''           
           }
         }
        }

        stage("Deploying Dev EA LZ 000 Application 000") {
         steps {
           dir("/var/jenkins_home/workspace/azure-sql-database-devealz000/orion/level-4/applications/azure-sql-database-devealz000") {
                sh '''#!/bin/bash
                echo "Deploy dev ea app 000 lz 000 ..."
                pwd
                ls -l
                ./deploy.sh
                '''
           }
         }
        }

        stage("Deployment Summary") {
            steps {
            dir("/var/jenkins_home/workspace/azure-sql-database-devealz000/orion/level-4/applications/azure-sql-database-devealz000") {
               sh '''#!/bin/bash
               echo "================== BEGIN VALIDATION ========="
               pwd
               ./test.sh
               echo "================== DONE VALIDATING =========="
               '''
              }
            }
        }
    }
    post {
        always { 
            dir("/var/jenkins_home/workspace/azure-sql-database-devealz000") {
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
