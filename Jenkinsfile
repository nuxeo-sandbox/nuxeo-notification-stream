pipeline {
  agent {
    label "builder-maven-nuxeo"
  }
  environment {
    ORG = 'nuxeo-sandbox'
    APP_NAME = 'nuxeo-notification-stream'
    CHARTMUSEUM_CREDS = credentials('jenkins-x-chartmuseum')
    KS_CLUSTER = 'l2it'
  }
  stages {
    stage('Prepare test compile') {
      steps {
        container('maven-nuxeo') {
          // Load local Maven repository
          sh "mvn package process-test-resources -DskipTests"
        }
      }
    }
    stage('CI Build') {
      when {
        branch 'test-*'
      }
      environment {
        PREVIEW_VERSION = "0.0.0-SNAPSHOT-$BRANCH_NAME-$BUILD_NUMBER"
        PREVIEW_NAMESPACE = "$APP_NAME-$BRANCH_NAME".toLowerCase()
        HELM_RELEASE = "$PREVIEW_NAMESPACE".toLowerCase()
      }
      parallel {
        stage('JUnit - Default') {
          steps {
            container('maven-nuxeo') {
              sh "mvn test -o -Dalt.build.dir=target-default"
            }
          }
        }
        stage('JUnit - MongoDB') {
          environment {
            TEST_NAMESPACE = "$HELM_RELEASE-mongo"
          }
          steps {
            container('maven-nuxeo') {
              sh "kubectl create ns $TEST_NAMESPACE || true"
              sh "jx step helm delete $TEST_NAMESPACE --namespace $TEST_NAMESPACE --purge || true"
              sh "helm init --client-only"
              sh "jx step helm install --name $TEST_NAMESPACE stable/mongodb --set persistence.enabled=false --set usePassword=false --namespace $TEST_NAMESPACE"
              sh "mvn test -o -Dalt.build.dir=target-mongo"
            }
          }
          post {
            always {
              container('maven-nuxeo') {
                sh "jx step helm delete $TEST_NAMESPACE --namespace $TEST_NAMESPACE --purge || true"
                sh "kubectl delete ns $TEST_NAMESPACE"
              }
            }
          }
        }
      }
    }
    stage('Deploy Preview') {
      when {
        branch 'test-*'
      }
      environment {
        PREVIEW_VERSION = "0.0.0-SNAPSHOT-$BRANCH_NAME-$BUILD_NUMBER"
        PREVIEW_NAMESPACE = "$APP_NAME-$BRANCH_NAME".toLowerCase()
        HELM_RELEASE = "$PREVIEW_NAMESPACE".toLowerCase()
      }
      steps {
        container('maven-nuxeo') {
          // XXX Not possible to set a version when inherited
          // sh "mvn versions:set -DnewVersion=$PREVIEW_VERSION"
          sh "export VERSION=$PREVIEW_VERSION && skaffold build -f skaffold.yaml"
          sh "jx step post build --image $DOCKER_REGISTRY/$ORG/$APP_NAME:$PREVIEW_VERSION"
          dir('charts/preview') {
            sh "make preview"
            sh "jx preview --log-level debug --pull-secrets instance-clid --app $APP_NAME --dir ../.."
          }
        }
      }
    }
    stage('Build Release') {
      when {
        branch 'master'
      }
      steps {
        container('maven') {

          // ensure we're not on a detached head
          sh "git checkout master"
          sh "git config --global credential.helper store"
          sh "jx step git credentials"

          // so we can retrieve the version in later steps
          sh "echo \$(jx-release-version) > VERSION"
          sh "mvn versions:set -DnewVersion=\$(cat VERSION)"
          sh "jx step tag --version \$(cat VERSION)"
          sh "mvn clean deploy"
          sh "export VERSION=`cat VERSION` && skaffold build -f skaffold.yaml"
          sh "jx step post build --image $DOCKER_REGISTRY/$ORG/$APP_NAME:\$(cat VERSION)"
        }
      }
    }
    stage('Promote to Environments') {
      when {
        branch 'master'
      }
      steps {
        container('maven') {
          dir('charts/nuxeo-notification-stream') {
            sh "jx step changelog --version v\$(cat ../../VERSION)"

            // release the helm chart
            sh "jx step helm release"

            // promote through all 'Auto' promotion Environments
            sh "jx promote -b --all-auto --timeout 1h --version \$(cat ../../VERSION)"
          }
        }
      }
    }
  }
  post {
    always {
      cleanWs()
    }
  }
}
