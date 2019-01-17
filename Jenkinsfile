def DB_MONGO = "mongodb"
def DB_H2 = "h2"
def DB_PGSQL = "postgresql"
def DB_ALL = "all"
def WORK = "work"
def targetTestEnvironments = [] as Set
def targetPreviewEnvironments = [] as Set
def mvnOpts = ""

def findBranchName() {
  // in case of PR-*, we need to lookup to the CHANGE_BRANCH var to find original branch
  env.CHANGE_BRANCH ? env.CHANGE_BRANCH : env.BRANCH_NAME
}

def formatNamespace(String ns) {
  ns.replaceAll(/\//, "-").toLowerCase()
}

pipeline {
  agent {
    label "builder-maven-nuxeo"
  }
  environment {
    ORG = 'nuxeo-sandbox'
    APP_NAME = 'nuxeo-notification-stream'
    CHARTMUSEUM_CREDS = credentials('jenkins-x-chartmuseum')
    KS_CLUSTER = 'l2it'
    BRANCH = findBranchName()
    PREVIEW_VERSION = "0.0.0-SNAPSHOT-$BRANCH_NAME-$BUILD_NUMBER"
  }
  stages {
    stage('Setup') {
      parallel {
        stage('Branch Naming') {
          steps {
            script {
              String testDef = env.BRANCH.split("/").find({ it.startsWith("test-") })
              if (testDef) {
                targetTestEnvironments.addAll(testDef.substring("test-".length()).split("-"))
              }
              testDef = env.BRANCH.split("/").find({ it.startsWith("preview-") })
              if (testDef) {
                targetPreviewEnvironments.addAll(testDef.substring("preview-".length()).split("-"))
              }
            }
          }
        }
        stage('PR Comment') {
          when {
            branch 'PR-*'
          }
          steps {
            script {
              def regex = /^- \[x\] (\w+)$/
              def splitBody = pullRequest.body.split("\n")
              for (line in splitBody) {
                def group = (line =~ regex)
                if (group) {
                  def env = group[0][1].toLowerCase()
                  targetTestEnvironments.add(env)
                  targetPreviewEnvironments.add(env)
                }
              }
            }
          }
        }
        stage('Changeset Speedup') {
          when {
            expression {
              return env.BRANCH_NAME.startsWith('work/')
            }
          }
          steps {
            script {
              // Add origin/master as ref to compare both branch
              sh script: "git fetch --no-tags --progress https://github.com/${ORG}/${APP_NAME}.git +refs/heads/master:refs/remotes/origin/master"
              def roots = [] as List
              def masterPoint = (sh(returnStdout: true, script: "git log --format=%H origin/master..origin/${env.BRANCH_NAME} --format=\"%H\" | tail -1")).trim()
              (sh(returnStdout: true, script: "git diff ${masterPoint} --name-only")).split("\n").each({
                def segments = it.split("/") as List
                if (segments.size() <= 1) {
                  segments = [".", it]
                }
                // Loop through child directory to find first pom.xml file
                for (int i = segments.size() - 1; i >= 0; i--) {
                  // Cannot use `List#sublist` due to Iterator uses is not allowed: JENKINS-27421
                  String folder = ""
                  for (int j = 0; j < i; j++) {
                    if (j > 0) {
                      folder += "/"
                    }
                    folder += segments.get(j)
                  }
                  if (fileExists("${folder}/pom.xml")) {
                    if (!roots.contains(folder)) {
                      roots.add(folder)
                    }
                    return
                  }
                }
              })
              if (roots.size() > 0) {
                mvnOpts = " -pl ${roots.join(",")} -amd "
              }
            }
          }
        }
      }
    }
    stage('Summary') {
      steps {
        script {
          println("Test environments: ${(targetTestEnvironments as List).join(' ')}")
          println("Preview environments: ${(targetPreviewEnvironments as List).join(' ')}")
          println("Maven Args: ${mvnOpts}")
        }
      }
    }
    stage('Prepare test compile') {
      steps {
        container('maven-nuxeo') {
          // Prepare Helm
          sh "helm init --client-only"
          sh "helm repo add jenkins-x http://jenkins-x-chartmuseum:8080"

          // Load local Maven repository
          sh "echo foo=bar > /root/nuxeo-test-vcs.properties"
          sh "mvn package process-test-resources -Pcustomdb -DskipTests ${mvnOpts}"
        }
      }
    }
    stage('CI Build') {
      parallel {
        stage('JUnit - H2') {
          when {
            expression {
              targetTestEnvironments.contains(DB_H2) || targetTestEnvironments.size() == 0 || targetTestEnvironments.contains(DB_ALL)
            }
          }
          steps {
            container('maven-nuxeo') {
              sh "mvn test -o -Dalt.build.dir=${DB_H2} ${mvnOpts}"
            }
          }
        }
        stage('JUnit - MongoDB') {
          when {
            expression {
              targetTestEnvironments.contains(DB_MONGO) || targetTestEnvironments.contains(DB_ALL)
            }
          }
          environment {
            NAMESPACE = formatNamespace "nss-${BRANCH_NAME}-${DB_MONGO}-${BUILD_NUMBER}"
            APP_NAME = "nss-${DB_MONGO}-${BUILD_NUMBER}"
          }
          steps {
            container('maven-nuxeo') {
              // Prepare nuxeo-test-vcs.properties
              sh "echo nuxeo.test.core=mongodb > /root/nuxeo-test-vcs-${DB_MONGO}.properties"
              sh "echo nuxeo.test.mongodb.server=mongodb://${APP_NAME}.${NAMESPACE}.svc.cluster.local >> /root/nuxeo-test-vcs-${DB_MONGO}.properties"
              sh "echo nuxeo.test.mongodb.dbname=vcstest >> /root/nuxeo-test-vcs-${DB_MONGO}.properties"

              sh "helm install --name ${APP_NAME} --namespace ${NAMESPACE} jenkins-x/nuxeo-mongodb"

              sh "mvn test -Pcustomdb,mongodb -o -Dalt.build.dir=${DB_MONGO} ${mvnOpts}"
            }
          }
          post {
            always {
              container('maven-nuxeo') {
                sh "helm delete $APP_NAME --purge || true"
                sh "kubectl delete ns $NAMESPACE || true"
              }
            }
          }
        }
        stage('JUnit - PostgreSQL') {
          when {
            expression {
              targetTestEnvironments.contains(DB_PGSQL) || targetTestEnvironments.contains(DB_ALL)
            }
          }
          environment {
            NAMESPACE = formatNamespace "nss-${BRANCH_NAME}-${DB_PGSQL}-${BUILD_NUMBER}"
            APP_NAME = "nss-${DB_PGSQL}-${BUILD_NUMBER}"
          }
          steps {
            container('maven-nuxeo') {
              // Prepare nuxeo-test-vcs.properties
              sh "echo nuxeo.test.vcs.db=PostgreSQL > /root/nuxeo-test-vcs-${DB_PGSQL}.properties"
              sh "echo nuxeo.test.vcs.server=${APP_NAME}-postgresql.${NAMESPACE}.svc.cluster.local >> /root/nuxeo-test-vcs-${DB_PGSQL}.properties"
              sh "echo nuxeo.test.vcs.database=nuxeo >> /root/nuxeo-test-vcs-${DB_PGSQL}.properties"
              sh "echo nuxeo.test.vcs.user=nuxeo >> /root/nuxeo-test-vcs-${DB_PGSQL}.properties"
              sh "echo nuxeo.test.vcs.password=nuxeo >> /root/nuxeo-test-vcs-${DB_PGSQL}.properties"

              sh "helm install --name ${APP_NAME} --namespace ${NAMESPACE} jenkins-x/nuxeo-postgresql"

              sh "mvn test -o -Pcustomdb,postgresql -Dalt.build.dir=${DB_PGSQL} ${mvnOpts}"
            }
          }
          post {
            always {
              container('maven-nuxeo') {
                sh "jx step helm delete $APP_NAME --purge || true"
                sh "kubectl delete ns $NAMESPACE || true"
              }
            }
          }
        }
      }
    }
    stage('Build Docker Image') {
      when {
        expression {
          targetPreviewEnvironments.size() > 0
        }
      }
      steps {
        container('maven-nuxeo') {
          sh "export VERSION=$PREVIEW_VERSION && skaffold build -f skaffold.yaml"
          sh "jx step post build --image $DOCKER_REGISTRY/$ORG/$APP_NAME:$PREVIEW_VERSION"
        }
      }
    }
    stage('Preview - H2') {
      environment {
        NAMESPACE = formatNamespace "nss-$BRANCH_NAME-$BUILD_NUMBER"
      }
      when {
        expression {
          targetPreviewEnvironments.contains(DB_H2) || !env.BRANCH.startsWith(WORK) || targetPreviewEnvironments.contains(DB_ALL)
        }
      }
      steps {
        container('maven-nuxeo') {
          dir('charts/preview') {
            sh "make preview"
            sh "jx preview --log-level debug --app $APP_NAME --namespace=${NAMESPACE} --dir ../.."
          }
        }
      }
    }
    stage('Preview - MongoDB') {
      environment {
        NAMESPACE = formatNamespace "nss-$BRANCH_NAME-$BUILD_NUMBER-mongo"
      }
      when {
        expression {
          targetPreviewEnvironments.contains(DB_MONGO) || targetPreviewEnvironments.contains(DB_ALL)
        }
      }
      steps {
        container('maven-nuxeo') {
          dir('charts/preview') {
            sh "make mongodb"
            sh "make preview"
            sh "jx preview --log-level debug --pull-secrets instance-clid --app $APP_NAME --namespace=${NAMESPACE} --dir ../.."
          }
        }
      }
    }
    stage('Preview - PostgreSQL') {
      environment {
        NAMESPACE = formatNamespace "nss-$BRANCH_NAME-$BUILD_NUMBER-postgresql"
      }
      when {
        expression {
          targetPreviewEnvironments.contains(DB_PGSQL) || targetPreviewEnvironments.contains(DB_ALL)
        }
      }
      steps {
        container('maven-nuxeo') {
          dir('charts/preview') {
            sh "make postgresql"
            sh "make preview"
            sh "jx preview --log-level debug --pull-secrets instance-clid --app $APP_NAME --namespace=${NAMESPACE} --dir ../.."
          }
        }
      }
    }

  }
  post {
    always {
      junit testResults: "**/target*/surefire-reports/*.xml, **/target*/failsafe-reports/*.xml, **/target*/failsafe-reports/**/*.xml"
    }
    cleanup {
      cleanWs()
    }
  }
}
