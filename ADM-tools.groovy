@Library('admin-groovy-libs@master') _

//Variables
def pkg = URLDecoder.decode(DEBURL)
def srv = SRV
String pkgName = ""
String pkgVer = ""
pkgInfo = pkgInfo.getPkgInfo(pkg)
pkgName = pkgInfo[0]
pkgVer = pkgInfo[1]

pkgPermission.checkPkgPermission(pkgName, srv)


def upload_pkg() {
    node("master") {
        sleep(5)
        println "Call ADM-deb-upload to upload deb pkg to Repo"
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

currentBuild.displayName = "#" + currentBuild.number + " - " + pkgName + "=" + pkgVer + " - " + SRV

pkg = pkgName + "=" +  pkgVer
try {
    stage("Downtime") {
        println "Downtime ..."
    }
    stage("Uploading") {
		upload_pkg()
    }
    stage("Deloying") {
        deploy_pkg(srv, pkg)
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
    telegramNotify.notify(${env.JOB_NAME}, pkg, srv, currentBuild.result)
}

