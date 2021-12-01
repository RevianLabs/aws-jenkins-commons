AWS_ECS_ACCOUNT_ARN = "arn:aws:ecs:${REGION}:${ENVIRONMENT_ID}"
ENVIRONMENT_ROLE = "arn:aws:iam::${ENVIRONMENT_ID}:role/ADFS-VT-DevOps"

ENVIRONMENT_AWS_CREDENTIALS_MAPPING = "aws_jenkins_user

ECS_CLUSTER = "${AWS_ECS_ACCOUNT_ARN}:cluster/${CLUSTER_NAME}"

ADFS_CLI_DOCKER_IMAGE = "revianlabs/aws-adfs:latest"

node {
    catchError {
        stage 'Update ECS Services'
        updateEcsServices()
    }
    
    catchError {
        stage 'Cleanup'
        deleteDir()
    }
}

void updateEcsServices() {
    withCredentials([usernamePassword(credentialsId: ENVIRONMENT_AWS_CREDENTIALS_MAPPING[ENVIRONMENT], passwordVariable: 'aws_pass', usernameVariable: 'aws_user')]) {
        docker.image(ADFS_CLI_DOCKER_IMAGE).inside { shCall ->
            sh """#!/bin/bash
                set -x
                adfs-cli -u "\$aws_user" -p "\$aws_pass" -r "${ENVIRONMENT_ROLE}"
                aws ecs list-services --cluster ${ECS_CLUSTER} | jq .serviceArns | jq .[] | xargs -i aws ecs update-service --cluster ${ECS_CLUSTER} --service '{}' --desired-count 0
            """.stripIndent()
        }
    }
}
