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
                echo '********** shieldrone-main-server Build Start **********'
                dir(env.MAIN_SERVER_DIR) {
                    sh 'docker build -t main-server .'
                }
                echo '********** shieldrone-main-server Build End **********'
            }
        }

        stage('Deploy Main Server') {
            when {
                expression {
                    return buildMainServer
                }
            }
            steps {
                script {
                    echo '********** shieldrone-main-server Deploy Start **********'
                    sh 'docker-compose -f main-server-compose.yml stop'
                    sh 'docker rm -f main-server || true'
                    sh 'docker rm -f mysql || true'
                    sh 'docker-compose -f mysql-compose.yml up -d'
                    sh 'docker-compose -f main-server-compose.yml up -d'
                    echo '********** shieldrone-main-server Deploy End **********'
                }
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

