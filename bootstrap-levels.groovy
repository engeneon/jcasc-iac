pipelineJob("bootstrap-levels") {
 definition {
    cps {
         sandbox(true)
script("""

pipeline {
    agent any

    environment {
      BITBUCKET_COMMON_CREDS=credentials('BITBUCKET_COMMON_CREDS')
      CURRENT_BRANCH="main"
    }

     stages {
        stage("Configuring Azure Access") {
            steps {

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

              sh '''#!/bin/bash
              set +x
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

        stage("Extracting global environment variables") {
         steps {
           dir("/var/jenkins_home/workspace/bootstrap-levels/orion/configuration/") {

             sh '''
              echo "Getting deployment configuration ..." 
              pwd
              ls
             '''

             script {
                def readpropscontent = readProperties file: "\${environmentDomain}/\${orgCode}/\${deployIndex}/\${orgCode}.conf"
                echo "Reading config from file: \${environmentDomain}/\${orgCode}/\${deployIndex}/\${orgCode}.conf"
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

        stage("Deploying bootstrap state storage") {
         steps {
           dir("/var/jenkins_home/workspace/bootstrap-levels/orion/level-0/bootstrap") {  
             script {
               sh '''#!/bin/bash
               echo "================== BEGIN BOOTSTRAP ========="
                set -x
                pwd
                ls -l
                az login --use-device-code --tenant \${TENANT_ID}
                ./deploy.sh
               echo "================== FINISH BOOTSTRAP =========="
               '''           
             }
           }
         }
        }

        stage("Deployment Validation Checks") {
          steps {
            dir("/var/jenkins_home/workspace/bootstrap-levels/orion/level-0") {  
              sh '''#!/bin/bash
               echo "================== BEGIN VALIDATION ========="
               pwd
               echo "Running in TENANT_ID = \${TENANT_ID}"
               ls -l
               echo "================== DONE VALIDATING =========="
               '''
              }
            }
        }

        stage("Deployment Summary") {
            steps {
             dir("/var/jenkins_home/workspace/bootstrap-levels/orion") {  
               sh '''#!/bin/bash
               echo "================== BEGIN SUMMARY ========="
               pwd
               echo "Deployed to TENANT_ID = \${TENANT_ID}"
               ls -l
               echo "================== FINISH SUMMARY ========="
               '''
              }
            }
        }

    }
    post {
        always { 
            dir("/var/jenkins_home/workspace/bootstrap-levels") {  
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
