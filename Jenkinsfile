def DB_MONGO = "mongodb"
def DB_h2 = "h2"
def DB_PGSQL = "postgresql"
def DB_DEFAULT = "default"
def DB_ALL = "all"

List dbs = [DB_DEFAULT]

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
//          sh "mvn package process-test-resources -DskipTests"
        }
      }
    }
    stage('Prepare CI') {
      steps {
        script {
          println env.BRANCH_NAME
          println env
          List DB = [DB_h2, DB_MONGO, DB_PGSQL]

          String testdbs = env.BRANCH_NAME.split("/").find({ it.startsWith("testdb") })
          if (testdbs) {
            dbs = testdbs.substring("testdb-".length()).split("-")
          }

          if (dbs.contains(DB_ALL)) {
            dbs = DB
          }

          println dbs
        }
      }
    }
    stage('CI Build') {
      when {
        expression {
          return env.BRANCH_NAME.startsWith('test-')
        }
      }
      environment {
        PREVIEW_VERSION = "0.0.0-SNAPSHOT-$BRANCH_NAME-$BUILD_NUMBER"
        PREVIEW_NAMESPACE = "$APP_NAME-$BRANCH_NAME".toLowerCase()
        HELM_RELEASE = "$PREVIEW_NAMESPACE".toLowerCase()
      }
      parallel {
        stage('JUnit - Default') {
          when {
            expression {
              return dbs.contains(DB_DEFAULT) || dbs.contains(DB_h2)
            }
          }
          steps {
            container('maven-nuxeo') {
//                sh "mvn test -o -Dalt.build.dir=target-default"
            }
          }
        }
        stage('JUnit - MongoDB') {
          when {
            expression {
              return dbs.contains(DB_MONGO)
            }
          }
          environment {
            TEST_NAMESPACE = "$HELM_RELEASE-mongo"
          }
          steps {
            container('maven-nuxeo') {
              sh "kubectl create ns $TEST_NAMESPACE || true"
              sh "jx step helm delete $TEST_NAMESPACE --namespace $TEST_NAMESPACE --purge || true"
              sh "helm init --client-only"
              sh "helm repo add jenkins-x-l2it http://chartmuseum.l2it.35.231.200.170.nip.io"
              sh "jx step helm install --name $TEST_NAMESPACE --namespace $TEST_NAMESPACE jenkins-x-l2it/nuxeo-tests-mongo"
//                sh "mvn test -o -Dalt.build.dir=target-mongo"
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
        stage('JUnit - PostgreSQL') {
          when {
            expression {
              return dbs.contains(DB_PGSQL)
            }
          }
          steps {
            container('maven-nuxeo') {
//                sh "mvn test -o -Dalt.build.dir=target-postgres"
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
        container('maven-nuxeo') {
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
