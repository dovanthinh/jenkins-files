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
     build job: 'ADM-deb-upload', parameters: [string(name: 'DEBURL', value: DEBURL)]
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

try {
    stage("Downtime") {
        println "Downtime ..."
    }
    stage("Deloying") {
        deploy_pkg(srv, 'atop=2.3.0-1')
    }
    stage("Rechecking") {
        check_service(srv, pkgName)
    }
}
catch(Exception exp) {
    currentBuild.result = "FAILURE"
}

