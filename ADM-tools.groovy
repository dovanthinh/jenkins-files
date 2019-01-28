@Library('admin-groovy-libs@master') _

/***VARIABLES***/
def pkg = URLDecoder.decode(DEBURL)
def srv = SRV
String pkgName = ""
String pkgVer = ""
pkgInfo = pkgInfo.getPkgInfo(pkg)
pkgName = pkgInfo[0]
pkgVer = pkgInfo[1]

pkgPermission.checkPkgPermission(pkgName, srv)

/***FUNCTIONS***/
def upload_pkg() {
    node("master") {
        sleep(5)
        println "Calling ADM-deb-upload to upload deb pkg to Repo"
        build job: 'ADM-deb-upload', parameters: [string(name: 'DEBURL', value: DEBURL)]
    }
}

def deploy_pkg(srv, pkg) {
    node("master") {
        sleep(5)
        sh(script: "python /sysadmin/jenkins-script/jdeploy.py -m ${srv} -p ${pkg}")
    }
}

def check_service(srv, pkgName) {
    node("monitoring") {
        sleep(5)
        sh(script: "python /sysadmin/jenkins-script/application_check.py ${pkgName} ${srv}")
        sh(script: "python /sysadmin/jenkins-script/service_check.py ${pkgName} ${srv}")
    }
}

def loadtest(glClass, jobName) {
    node("master") {
        sleep(5)
        sh(script: "/bin/bash /sysadmin/gatling/bin/gatling.sh -s sysadmin.${glClass} -rf /sysadmin/gatling/results/${jobName}/")
    }
}

/***JOBNAME***/
pkg = pkgName + "=" +  pkgVer
currentBuild.displayName = "#" + currentBuild.number + " - " + pkg + " - " + SRV

/***WORKFLOW***/
try {
    stage("Uploading") {
		upload_pkg()
    }
    stage("Deloy to DEV") {
        deploy_pkg(srv, pkg)
    }
    stage("Loadtest") {
		loadtest(pkgName, pkgName)
    }
    stage("Deploy to Prod") {
        println("deploying.. to Prod")
    }
    stage("Rechecking") {
        check_service(srv, pkgName)
    }
    currentBuild.result = "SUCCESS"
}
catch(Exception exp) {
    println(exp.toString());
    println(exp.getMessage());
    println(exp.getStackTrace());
    currentBuild.result = "FAILURE"
}
finally {
    telegramNotify.notify("${env.JOB_NAME}", pkg, srv, currentBuild.result)
}

