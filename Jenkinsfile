// Jenkinsfile (t9/Б18): пайплайн «как код» — версионируется и ревьюится вместе с проектом.
// Полный CI на машине преподавателя не разворачивается — этот файл линтуют/читают,
// а локальным «симулятором» тех же шагов служит ci.sh.
//
// Логика: pipeline (последовательность) → stage (этап) → sh (шаг). Артефакты
// (HTML-отчёт, JSON Allure, XML JUnit) архивируются в post{always} — сохраняются даже
// при падении, иначе падение не разобрать. Allure-результаты копятся между прогонами
// → вкладки History/Trend.
pipeline {
    agent any

    environment {
        // JDK 21 — задаётся на агенте в Global Tool Configuration (у слушателей курса — JDK 21).
    }

    options {
        timestamps()          // время на каждый шаг в логе
        disableConcurrentBuilds()
    }

    stages {
        stage('Сборка и тесты (unit + H2)') {
            steps {
                // docker-теги (@Tag("docker"), integrationTest) в дефолтный прогон не входят:
                // им нужен Docker-агент. На CI с Docker раскомментируй следующую строку.
                sh './gradlew clean test'
                // sh './gradlew integrationTest'   // требует Docker (Testcontainers)
            }
        }
        stage('Allure-отчёт') {
            steps {
                sh './gradlew allureReport'
            }
        }
    }

    post {
        always {
            // Артефакты — и при падении (post{always}): красную сборку разбирают по отчётам.
            archiveArtifacts artifacts: 'build/reports/allure-report/**', allowEmptyArchive: true
            archiveArtifacts artifacts: 'build/allure-results/**',   allowEmptyArchive: true
            archiveArtifacts artifacts: 'build/reports/tests/test/**', allowEmptyArchive: true
            junit 'build/test-results/test/*.xml'
        }
    }
}
