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
          // Load local Maven repository
          sh "mvn package process-test-resources -DskipTests ${mvnOpts}"
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
              //sh "mvn test -o -Dalt.build.dir=target-default ${mvnOpts}"
            }
          }
        }
        stage('JUnit - MongoDB') {
          when {
            expression {
              targetTestEnvironments.contains(DB_MONGO) || targetTestEnvironments.contains(DB_ALL)
            }
          }
          steps {
            container('maven-nuxeo') {
              //sh "mvn test -o -Dalt.build.dir=target-mongo ${mvnOpts}"
            }
          }
        }
        stage('JUnit - PostgreSQL') {
          when {
            expression {
              targetTestEnvironments.contains(DB_PGSQL) || targetTestEnvironments.contains(DB_ALL)
            }
          }
          steps {
            container('maven-nuxeo') {
              //sh "mvn test -o -Dalt.build.dir=target-default ${mvnOpts}"
            }
          }
        }
      }
    }
    stage('Build Docker Image') {
      when {
        expression {
          targetPreviewEnvironments.contains(DB_H2) || !env.BRANCH.startsWith(WORK) || targetPreviewEnvironments.contains(DB_ALL) || targetPreviewEnvironments.contains(DB_MONGO) || targetPreviewEnvironments.contains(DB_PGSQL) 
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
        NAMESPACE = "$APP_NAME-$BRANCH_NAME-$BUILD_NUMBER"
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
        NAMESPACE = "$APP_NAME-$BRANCH_NAME-$BUILD_NUMBER-mongo"
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
        NAMESPACE = "$APP_NAME-$BRANCH_NAME-$BUILD_NUMBER-postgresql"
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
      cleanWs()
    }
  }
}
