pipeline {
    agent any

    environment {
        MAIN_SERVER_DIR = 'shieldrone-main-server'
    }


    stages {
        stage('Check Changes') {
            steps {
                script {

                    buildMainServer = false

                    def diffOutput = sh(script: "git diff --name-only HEAD^ HEAD", returnStdout: true).trim()

                    buildMainServer = diffOutput.contains(env.MAIN_SERVER_DIR)

                }
            }
        }

        stage('Build Main Server') {
            when {
                expression {
                    return buildMainServer
                }
            }
            steps {
                echo 'Building Main Server...'
            }
        }

        stage('Deploy Main Server') {
            when {
                expression {
                    return buildMainServer
                }
            }
            steps {
                echo 'Deploying Main Server...'
            }
        }
    }


    post {
        success {
            script {
                def Author_ID = sh(script: "git show -s --pretty=%an", returnStdout: true).trim()
                def Author_Name = sh(script: "git show -s --pretty=%ae", returnStdout: true).trim()
                mattermostSend (color: 'good',
                message: "빌드 성공: ${env.JOB_NAME} #${env.BUILD_NUMBER} by ${Author_ID}(${Author_Name})\n(<${env.BUILD_URL}|Details>)",
                endpoint: 'https://meeting.ssafy.com/hooks/nuxjcb7rgbg5mg11wjtn4xqqfw',
                channel: 'jenkins'
                )
            }
        }
        failure {
            script {
                def Author_ID = sh(script: "git show -s --pretty=%an", returnStdout: true).trim()
                def Author_Name = sh(script: "git show -s --pretty=%ae", returnStdout: true).trim()
                mattermostSend (color: 'danger',
                message: "빌드 실패: ${env.JOB_NAME} #${env.BUILD_NUMBER} by ${Author_ID}(${Author_Name})\n(<${env.BUILD_URL}|Details>)",
                endpoint: 'https://meeting.ssafy.com/hooks/nuxjcb7rgbg5mg11wjtn4xqqfw',
                channel: 'jenkins'
                )
            }
        }
    }
}

